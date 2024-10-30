package earth.terrarium.tempad

import com.teamresourceful.resourcefulconfig.api.loader.Configurator
import com.teamresourceful.resourcefullib.common.color.Color
import earth.terrarium.tempad.api.tva_device.ChrononHandler
import earth.terrarium.tempad.api.tva_device.UpgradeHandler
import earth.terrarium.tempad.api.tva_device.impl.BlockChrononHandler
import earth.terrarium.tempad.api.tva_device.impl.InfiniteChrononHandler
import earth.terrarium.tempad.api.tva_device.impl.ItemChrononHandler
import earth.terrarium.tempad.api.tva_device.impl.ItemUpgradeHandler
import earth.terrarium.tempad.api.tva_device.impl.TempadChrononHandler
import earth.terrarium.tempad.common.config.CommonConfig
import earth.terrarium.tempad.common.data.TravelHistoryAttachment
import earth.terrarium.tempad.common.registries.*
import earth.terrarium.tempad.common.utils.register
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.MinecraftServer
import net.minecraft.world.entity.player.Player
import net.neoforged.bus.api.EventPriority
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.common.Mod
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerRespawnEvent
import net.neoforged.neoforge.event.tick.PlayerTickEvent
import net.neoforged.neoforge.server.ServerLifecycleHooks
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

val String.tempadId: ResourceLocation
    get() = ResourceLocation.fromNamespaceAndPath(Tempad.MOD_ID, this)

@Mod(Tempad.MOD_ID)
class Tempad(bus: IEventBus) {
    companion object {

        const val MOD_ID = "tempad"
        val DARK_ORANGE: Color = Color(0x91, 0x45, 0x0d, 255)
        val ORANGE: Color = Color(0xFF, 0x6f, 0, 255)
        val HIGHLIGHTED_ORANGE: Color = Color(0xFF, 0xcc, 0x54, 255)
        val GREEN = Color(0xFF65fb01.toInt())

        val CONFIGURATOR = Configurator(MOD_ID)

        val server: MinecraftServer?
            get() = ServerLifecycleHooks.getCurrentServer();

        val playerUpgrade: ResourceLocation = "player".tempadId

        val logger: Logger = LogManager.getLogger(MOD_ID)
    }

    init {
        CONFIGURATOR.register(CommonConfig::class.java)

        ModApps.init()
        ModSizing.init()
        ModAttachments.registry.init()
        ModAttachments.syncer.init()
        ModComponents.registry.init()
        ModContext.init()
        ModEntities.entities.init()
        ModEntities.serializers.init()
        ModBlocks.blocks.init()
        ModBlocks.blockEntities.init()
        ModItems.registry.init()
        ModItems.creativeTabs.init()
        ModMacros.init()
        ModMenus.registry.init()
        ModNetworking.init()
        ModRecipes.serializers.init()
        ModFluids.dataRegistry.init()
        ModFluids.registry.init()
        ModSounds.registry.init()
        ModLocations.init()

        bus.addListener { event: RegisterCapabilitiesEvent ->
            val chrononBlocks = event.register(ChrononHandler.block)
            val chrononItems = event.register(ChrononHandler.item)
            val upgradeItems = event.register(UpgradeHandler.item)

            chrononBlocks[ModBlocks.rudimentaryTempadBE] = { it, _ ->
                BlockChrononHandler(it, 4000)
            }

            chrononItems[ModItems.rudimentaryTempad] = { it, _ ->
                ItemChrononHandler(it, 4000)
            }

            chrononItems[ModItems.timeTwister] = { it, _ ->
                ItemChrononHandler(it, 4000)
            }

            chrononItems[ModItems.tempad] = { it, _ ->
                TempadChrononHandler(it, 8000, 4000)
            }

            chrononItems[ModItems.chronometer] = { it, _ ->
                ItemChrononHandler(it, 32000)
            }

            chrononItems[ModItems.sacredChronometer] = { _, _ ->
                InfiniteChrononHandler
            }

            upgradeItems[ModItems.tempad] = { it, _ ->
                ItemUpgradeHandler(it)
            }
        }

        bus.addListener { event: BuildCreativeModeTabContentsEvent ->
            if (event.tab !== ModItems.tab) return@addListener
            for (entry in ModItems.registry.entries) {
                event.accept(entry.get())
            }
        }

        NeoForge.EVENT_BUS.addListener { event: PlayerTickEvent.Post ->
            if (event.entity.tickCount % 1200 != 0) return@addListener
            event.entity.travelHistory.logLocation(event.entity)
        }

        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST) { event: LivingDeathEvent ->
            if (event.isCanceled || event.entity !is Player) return@addListener
            event.entity.travelHistory.logLocation(event.entity, TravelHistoryAttachment.DEATH_MARKER)
        }

        NeoForge.EVENT_BUS.addListener { event: PlayerRespawnEvent ->
            event.entity.travelHistory.logLocation(event.entity, TravelHistoryAttachment.RESPAWN_MARKER)
        }

        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST) { event: EntityTravelToDimensionEvent ->
            val entity = event.entity
            if (event.isCanceled || entity !is Player) return@addListener
            entity.travelHistory.logLocation(entity, TravelHistoryAttachment.DIM_EXIT_MARKER)
        }

        NeoForge.EVENT_BUS.addListener { event: PlayerChangedDimensionEvent ->
            event.entity.travelHistory.logLocation(event.entity, TravelHistoryAttachment.DIM_ENTER_MARKER)
        }
    }
}