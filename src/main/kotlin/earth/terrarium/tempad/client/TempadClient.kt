@file:Suppress("INACCESSIBLE_TYPE")

package earth.terrarium.tempad.client

import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.VertexFormat
import com.mojang.datafixers.util.Either
import com.teamresourceful.resourcefullib.client.fluid.data.ClientFluidProperties
import com.teamresourceful.resourcefullib.client.fluid.registry.ResourcefulClientFluidRegistry
import earth.terrarium.tempad.Tempad
import earth.terrarium.tempad.client.block.SpatialAnchorRenderer
import earth.terrarium.tempad.client.entity.TimedoorRenderer
import earth.terrarium.tempad.client.screen.anchor.SpatialAnchorScreen
import earth.terrarium.tempad.client.screen.tempad.NewLocationScreen
import earth.terrarium.tempad.client.screen.tempad.SettingsScreen
import earth.terrarium.tempad.client.screen.tempad.TeleportScreen
import earth.terrarium.tempad.client.screen.tempad.TimelineScreen
import earth.terrarium.tempad.client.tooltip.*
import earth.terrarium.tempad.common.compat.ArsCompat
import earth.terrarium.tempad.common.config.ClientConfig
import earth.terrarium.tempad.common.data.InstalledUpgradesComponent
import earth.terrarium.tempad.common.menu.AbstractTempadMenu
import earth.terrarium.tempad.common.network.s2c.OpenSpatialAnchor
import earth.terrarium.tempad.common.registries.*
import earth.terrarium.tempad.common.utils.get
import earth.terrarium.tempad.common.utils.safeLet
import earth.terrarium.tempad.common.utils.vanillaId
import earth.terrarium.tempad.tempadId
import net.minecraft.client.Minecraft
import net.minecraft.client.color.block.BlockColor
import net.minecraft.client.color.item.ItemColor
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.client.renderer.RenderStateShard
import net.minecraft.client.renderer.RenderStateShard.ShaderStateShard
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.RenderType.*
import net.minecraft.client.renderer.ShaderInstance
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers
import net.minecraft.client.renderer.entity.EntityRenderers
import net.minecraft.client.renderer.item.ClampedItemPropertyFunction
import net.minecraft.client.renderer.item.ItemProperties
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.tooltip.TooltipComponent
import net.minecraft.world.level.BlockGetter
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.ModList
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent
import net.neoforged.neoforge.capabilities.Capabilities
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent
import net.neoforged.neoforge.client.event.RegisterShadersEvent
import net.neoforged.neoforge.client.event.RenderTooltipEvent
import net.neoforged.neoforge.common.NeoForge
import java.io.IOException

@EventBusSubscriber(modid = Tempad.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = [Dist.CLIENT])
object TempadClient {
    var timedoorShader: ShaderInstance? = null
    fun renderType(textureId: ResourceLocation?): RenderType = CompositeState.builder()
        .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
        .setCullState(NO_CULL)
        .setLayeringState(NO_LAYERING)
        .setShaderState(ShaderStateShard {
            textureId?.let { GameRenderer.getPositionTexColorShader() } ?: timedoorShader
        })
        .apply {
            if (textureId != null) {
                setTextureState(RenderStateShard.TextureStateShard(textureId, false, true))
            }
        }
        .setOutputState(PARTICLES_TARGET)
        .createCompositeState(true)
        .let {
            create(
                "timedoor",
                textureId?.let { DefaultVertexFormat.POSITION_TEX_COLOR } ?: DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,
                VertexFormat.Mode.QUADS,
                256,
                false,
                true,
                it
            )
        }

    val enabledProperty = BooleanItemPropertyFunction { stack, level, entity, seed -> stack.enabled }

    val twisterAttachedProperty = BooleanItemPropertyFunction { stack, level, entity, seed -> stack.twisterEquipped }

    val inUseProperty = BooleanItemPropertyFunction { stack, level, entity, seed ->
        if (entity !is Player) return@BooleanItemPropertyFunction false
        val menu = entity.containerMenu
        if (menu !is AbstractTempadMenu<*>) return@BooleanItemPropertyFunction false
        return@BooleanItemPropertyFunction menu.ctx.stack === stack
    }

    val chargeProperty = ClampedItemPropertyFunction { stack, level, entity, seed ->
        val tank = stack[Capabilities.FluidHandler.ITEM] ?: return@ClampedItemPropertyFunction 0f
        step(tank.getFluidInTank(0).amount.toFloat() / tank.getTankCapacity(0), 0.33f)
    }

    val writtenProperty = BooleanItemPropertyFunction { stack, level, entity, seed -> stack.targetPos != null }

    val clientFluidRegistry = ResourcefulClientFluidRegistry(Tempad.MOD_ID)

    val chrononRenderer = ClientFluidProperties.builder().apply {
        still("block/water_still".vanillaId)
        flowing("block/water_flow".vanillaId)
        overlay("block/water_overlay".vanillaId)
        screenOverlay("textures/misc/underwater.png".vanillaId)
        tintColor(Tempad.ORANGE.value)
    }

    val blockColor: BlockColor = BlockColor { _, maybeLevel, maybePos, _ ->
        safeLet(maybeLevel as? BlockGetter, maybePos) { level, pos ->
            val blockEntity = level.getBlockEntity(pos)
            (blockEntity?.color)?.value?.let { return@BlockColor it }
        } ?: Tempad.ORANGE.value
    }

    val itemColor: ItemColor = ItemColor { stack, _ ->
        stack.color?.value ?: Tempad.ORANGE.value
    }

    fun step(value: Float, step: Float): Float {
        if (value >= 0.9) return 1f
        return value - value % step
    }

    init {
        clientFluidRegistry.register("chronon", chrononRenderer)
        NeoForge.EVENT_BUS.addListener(::appendTooltip)
        Tempad.CONFIGURATOR.register(ClientConfig::class.java)
    }

    @SubscribeEvent
    @JvmStatic
    fun init(event: FMLClientSetupEvent) {
        EntityRenderers.register(ModEntities.TIMEDOOR_ENTITY, ::TimedoorRenderer)
        ItemProperties.register(ModItems.tempad, "in_use".tempadId, inUseProperty)
        ItemProperties.register(ModItems.tempad, "attached".tempadId, twisterAttachedProperty)
        ItemProperties.register(ModItems.tempad, "charge".tempadId, chargeProperty)
        ItemProperties.register(ModItems.chronometer, "charge".tempadId, chargeProperty)
        ItemProperties.register(ModItems.statusEmitter, "enabled".tempadId, enabledProperty)
        ItemProperties.register(ModItems.locationCard, "written".tempadId, writtenProperty)
        ItemProperties.register(ModItems.rudimentaryTempad, "has_card".tempadId, writtenProperty)
        BlockEntityRenderers.register(ModBlocks.spatialAnchorBE) { SpatialAnchorRenderer(it.blockRenderDispatcher) }

        if (ModList.get().isLoaded("ars_nouveau")) {
            ArsCompat.init()
        }
    }

    @SubscribeEvent
    @JvmStatic
    fun registerScreens(event: RegisterMenuScreensEvent) {
        event.register(ModMenus.TELEPORT_MENU, ::TeleportScreen)
        event.register(ModMenus.NEW_LOCATION_MENU, ::NewLocationScreen)
        event.register(ModMenus.SETTINGS_MENU, ::SettingsScreen)
        event.register(ModMenus.TIMELINE_MENU, ::TimelineScreen)
    }

    @SubscribeEvent
    @JvmStatic
    @Throws(IOException::class)
    fun registerShaders(event: RegisterShadersEvent) {
        event.registerShader(
            ShaderInstance(
                event.resourceProvider,
                "rendertype_timedoor".vanillaId,
                DefaultVertexFormat.POSITION_COLOR
            )
        ) { shaderInstance: ShaderInstance ->
            timedoorShader = shaderInstance
        }
    }

    @SubscribeEvent
    @JvmStatic
    fun registerTooltip(event: RegisterClientTooltipComponentFactoriesEvent) {
        event.register(ChrononData::class.java, ::ChrononTooltip)
        event.register(InstalledUpgradesComponent::class.java, ::UpgradesTooltip)
    }

    @SubscribeEvent
    @JvmStatic
    fun registerBlockColors(event: RegisterColorHandlersEvent.Block) {
        event.register(blockColor, ModBlocks.spatialAnchor)
    }

    @SubscribeEvent
    @JvmStatic
    fun registerItemColors(event: RegisterColorHandlersEvent.Item) {
        event.register(itemColor, ModItems.spatialAnchor)
    }

    fun appendTooltip(event: RenderTooltipEvent.GatherComponents) {
        val stack = event.itemStack
        if (stack.item === ModItems.tempad && stack.installedUpgrades.upgrades.isNotEmpty()) {
            event.tooltipElements.add(2, Either.right(stack.installedUpgrades))
        }

        if (stack.item === ModItems.rudimentaryTempad && stack.targetPos != null) {
            (stack.targetPos as? TooltipComponent)?.let {
                event.tooltipElements.add(2, Either.right(it))
            }
        }
    }

    fun openSpatialAnchorScreen(packet: OpenSpatialAnchor) {
        Minecraft.getInstance().setScreen(SpatialAnchorScreen(packet.blockPos, packet.name, packet.color, packet.access))
    }
}