package earth.terrarium.tempad.client.screen.tempad

import com.teamresourceful.resourcefullib.common.color.ConstantColors
import com.teamresourceful.resourcefullib.common.utils.TriState
import earth.terrarium.olympus.client.components.Widgets
import earth.terrarium.olympus.client.components.base.renderer.WidgetRenderer
import earth.terrarium.olympus.client.components.buttons.Button
import earth.terrarium.olympus.client.components.compound.LayoutWidget
import earth.terrarium.olympus.client.components.dropdown.DropdownState
import earth.terrarium.olympus.client.components.renderers.WidgetRenderers
import earth.terrarium.olympus.client.components.string.MultilineTextWidget
import earth.terrarium.olympus.client.constants.MinecraftColors
import earth.terrarium.olympus.client.ui.ClearableGridLayout
import earth.terrarium.olympus.client.utils.ListenableState
import earth.terrarium.olympus.client.utils.Translatable
import earth.terrarium.tempad.Tempad
import earth.terrarium.tempad.api.locations.NamedGlobalVec3
import earth.terrarium.tempad.client.TempadUI
import earth.terrarium.tempad.client.TempadUI.colored
import earth.terrarium.tempad.client.TempadUI.style
import earth.terrarium.tempad.client.state.AppearanceState
import earth.terrarium.tempad.client.state.MutableState
import earth.terrarium.tempad.common.config.ClientConfig
import earth.terrarium.tempad.common.data.FavoriteLocationAttachment
import earth.terrarium.tempad.common.network.c2s.DeleteLocationPacket
import earth.terrarium.tempad.common.network.c2s.OpenTimedoorPacket
import earth.terrarium.tempad.common.network.c2s.SetFavoritePacket
import earth.terrarium.tempad.common.network.c2s.WriteToCardPacket
import earth.terrarium.tempad.common.registries.ModMenus.TeleportMenu
import earth.terrarium.tempad.common.utils.safeLet
import earth.terrarium.tempad.common.utils.sendToServer
import earth.terrarium.tempad.common.utils.translatable
import earth.terrarium.tempad.tempadId
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.StringWidget
import net.minecraft.client.gui.layouts.FrameLayout
import net.minecraft.client.gui.layouts.LinearLayout
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Inventory
import java.util.*

class TeleportScreen(menu: TeleportMenu, inv: Inventory, title: Component) :
    AbstractTempadScreen<TeleportMenu>(null, menu, inv, title) {
    companion object {
        val teleportText = Component.translatable("app.${Tempad.MOD_ID}.teleport")
        val noSelectionText = Component.translatable("app.${Tempad.MOD_ID}.teleport.no_selection")
        val deleteText = Component.translatable("app.${Tempad.MOD_ID}.teleport.delete")
        val sectionVisibility = mutableMapOf<Component, MutableState<Boolean>>()
        val sortMode = DropdownState.of(Sorting.valueOf(ClientConfig.sortingMode))

        val scrollbarYRenderer: WidgetRenderer<LayoutWidget<ClearableGridLayout>> =
            WidgetRenderer { graphics, context, partialTick ->
                val widget = context.getWidget();
                val scrollHeight =
                    (context.height.toFloat() * (context.height.toFloat() / widget.contentHeight)).toInt() + (widget.viewHeight - context.height)
                val scrollY =
                    ((widget.yScroll.toFloat() + widget.overscrollY) / widget.contentHeight.toFloat() * context.height.toFloat()).toInt()
                graphics.fill(
                    context.x - 2,
                    context.y - 2,
                    context.x + context.width + 2,
                    context.y + context.height + 4,
                    0x4aff6f00.toInt()
                )
                graphics.fill(
                    context.x + 2,
                    context.y,
                    context.x + context.width - 2,
                    context.y + context.height,
                    Tempad.ORANGE.value
                )
                graphics.blitSprite(
                    "button/normal".tempadId,
                    context.x,
                    context.y + scrollY,
                    context.width,
                    scrollHeight
                )
            }
    }
    var selected: Triple<ResourceLocation, UUID, NamedGlobalVec3>? = null
        set(value) {
            field = value
            teleportBtn.active = value != null
            locationButtons.visible = value != null
        }

    var search = ListenableState.of("").apply {
        registerListener {
            updateLocationPanel()
        }
    }

    // make map of ids to locations from menu.appContent.locations mutable
    private val locations: Map<ResourceLocation, MutableMap<UUID, NamedGlobalVec3>> =
        menu.appContent.locations.mapValues { (_, value) ->
            value.toMutableMap()
        }

    private var locationButtons: AppearanceState = AppearanceState().apply { visible = false }
    private var teleportBtn = AppearanceState().apply { active = false }

    private var favorite: FavoriteLocationAttachment? = menu.appContent.favoriteLocation
    private val isSelectedPinned: Boolean get() = safeLet(selected?.first, selected?.second) { provider, id -> favorite?.matches(provider, id) == true } == true

    private lateinit var locationList: LayoutWidget<ClearableGridLayout>
    private lateinit var infoTextWidget: LayoutWidget<ClearableGridLayout>

    fun ClearableGridLayout.initLocationPanel() {
        var list = this.rows(0, 1)

        for ((category, locations) in sortMode.get().reorganize(locations)) {
            val filtered = locations.filter { (_, _, display) ->
                display.name.string.contains(search.get(), ignoreCase = true)
            }.sortedBy { it.third.name.string.lowercase(Locale.ROOT) }.sortedBy { (provider, id, _) ->
                // sort by favorite
                favorite?.matches(provider, id) == false
            }

            if (filtered.isEmpty()) continue

            list.addChild(Widgets.button {
                it.withSize(110, 12)
                it.withTexture(null)
                it.withRenderer(
                    sectionVisibility.getOrDefault(category, MutableState.of(true)).withRenderer { enabled ->
                        WidgetRenderers
                            .textWithChevron<Button>(category, enabled)
                            .colored()
                            .withPadding(0, 2, 0, 4)
                    }
                )
                it.withCallback {
                    sectionVisibility[category] =
                        sectionVisibility.getOrDefault(category, MutableState.of(true)).apply { set(!get()) }
                    updateLocationPanel()
                }
            })

            if (!sectionVisibility.getOrDefault(category, MutableState.of(true)).get()) continue
            for ((provider, locationId, display) in filtered) {
                val entry = Widgets.button {
                    it.withSize(110, 12)
                    it.withTexture(null)
                    it.withRenderer(WidgetRenderer<Button> { graphics, ctx, _ ->
                        val isSelected = selected?.second == locationId && selected?.first == provider
                        val isHovered = ctx.widget.isHoveredOrFocused
                        val isFavorite = favorite?.matches(provider, locationId) == true

                        if (isSelected) {
                            graphics.fill(ctx.x, ctx.y, ctx.x + ctx.width, ctx.y + ctx.height, Tempad.ORANGE.value)
                        } else {
                            graphics.fill(ctx.x, ctx.y, ctx.x + ctx.width, ctx.y + ctx.height, 0x3aff6f00.toInt())
                        }

                        if (isFavorite) {
                            WidgetRenderers.textWithIcon<Button>(display.name, "icons/mini/pin".tempadId)
                                .withColor(if (isSelected) ConstantColors.black else if (isHovered) Tempad.HIGHLIGHTED_ORANGE else Tempad.ORANGE)
                                .withIconSize(7)
                                .withTextLeftIconLeft()
                                .render(graphics, ctx, 0f)
                        } else {
                            WidgetRenderers.text<Button>(display.name)
                                .withColor(if (isSelected) ConstantColors.black else if (isHovered) Tempad.HIGHLIGHTED_ORANGE else Tempad.ORANGE)
                                .withLeftAlignment()
                                .withPadding(0, 0, 0, 2)
                                .render(graphics, ctx, 0f)
                        }
                    }.withPadding(0, 2))
                    it.withCallback {
                        selected = Triple(provider, locationId, display)
                        updateInfoPanel()
                    }
                }
                list.addChild(entry)
            }
        }
    }

    fun ClearableGridLayout.initInfoPanel() {
        this.clear()
        var list = this.rows(0, 1)

        var selected = selected
        if (selected == null) {
            list.addChild(MultilineTextWidget(62, noSelectionText, font)).apply {
                setColor(Tempad.ORANGE.value)
                alignLeft()
            }
        } else {
            list.addChild(StringWidget(selected.third.dimensionText, font)).setColor(Tempad.ORANGE.value)
            list.addChild(StringWidget(Component.literal("X: ${selected.third.x}"), font)).setColor(Tempad.ORANGE.value)
            list.addChild(StringWidget(Component.literal("Y: ${selected.third.y}"), font)).setColor(Tempad.ORANGE.value)
            list.addChild(StringWidget(Component.literal("Z: ${selected.third.z}"), font)).setColor(Tempad.ORANGE.value)
        }
    }

    fun updateLocationPanel() {
        locationList.withContents {
            it.clear()
            it.initLocationPanel()
        }
    }

    fun updateInfoPanel() {
        infoTextWidget.withContents {
            it.clear()
            it.initInfoPanel()
        }
    }

    override fun init() {
        super.init()

        addRenderableWidget(Widgets.textInput(search) {
            it.withTexture(TempadUI.element)
            it.withTextColor(Tempad.ORANGE)
            it.withPlaceholder("Search...")
            it.withPlaceholderColor(Tempad.DARK_ORANGE)
            it.withPosition(localLeft + 4, localTop + 20)
            it.withSize(94, 16)
        })

        addRenderableWidget(Widgets.dropdown(sortMode, Sorting::class.java, {
            it.withSize(100, 64)
            it.withCallback {
                ClientConfig.sortingMode = it.name
                Tempad.CONFIGURATOR.saveConfig(ClientConfig::class.java)
                updateLocationPanel()
                sortMode.button?.withTooltip(Translatable.toComponent(sortMode.get()))
            }
            it.style { Translatable.toComponent(it) }
        }, {
            it.withPosition(localLeft + 100, localTop + 20)
            it.withSize(16, 16)
            it.withTexture(TempadUI.button)
            it.withRenderer(sortMode.withRenderer { mode ->
                WidgetRenderers.icon<Button>("icons/${mode.toString().lowercase()}".tempadId).withColor(MinecraftColors.BLACK).withCentered(12, 12)
            })
            it.withTooltip(Translatable.toComponent(sortMode.get()))
        }))

        this.locationList = addRenderableWidget(LayoutWidget(ClearableGridLayout())).apply {
            withPosition(localLeft + 4, localTop + 38)
            withSize(112, 76)
            withContentFillWidth()
            withScrollableY(TriState.UNDEFINED)
            withScrollbarYRenderer(scrollbarYRenderer)
            withTexture("element/normal".tempadId)
            withOverscrollY(2)
            withContentMargin(1)
        }

        updateLocationPanel()

        var infoLayout = FrameLayout(74, 74)
        infoLayout.setPosition(localLeft + 119, localTop + 22)

        infoTextWidget = addRenderableWidget(LayoutWidget(ClearableGridLayout())).apply {
            withPosition(localLeft + 118, localTop + 20)
            withSize(76, 76)
            withTexture("element/normal".tempadId)
            withContentMargin(6)
        }

        updateInfoPanel()

        var options = infoLayout.addChild(LinearLayout(0, 0, LinearLayout.Orientation.HORIZONTAL).spacing(2)) {
            it.alignHorizontallyRight()
            it.alignVerticallyBottom()
            it.padding(4)
        }

        locationButtons += options.addChild(Widgets.button {
            it.withTexture(null)
            it.withSize(7, 7)
            it.withRenderer { graphics, ctx, dunno ->
                WidgetRenderers.icon<Button>(("icons/mini/${if(isSelectedPinned) "unpin" else "pin"}").tempadId)
                    .colored()
                    .withCentered(7, 7)
                    .render(graphics, ctx, dunno)
            }
            it.withCallback {
                if (minecraft == null || selected == null) return@withCallback
                val (provider, locationId, _) = selected!!
                if (favorite?.matches(provider, locationId) == true) {
                    SetFavoritePacket(null).sendToServer()
                    favorite = null
                } else {
                    SetFavoritePacket(provider, locationId).sendToServer()
                    favorite = FavoriteLocationAttachment(provider, locationId)
                }
                updateLocationPanel()
                it.withTooltip("app.tempad.teleport.${if(isSelectedPinned) "unpin" else "pin"}".translatable)
            }
            it.withTooltip("app.tempad.teleport.${if(isSelectedPinned) "unpin" else "pin"}".translatable)
        })

        locationButtons += options.addChild(
            Widgets.button {
                it.withTexture(null)
                it.withRenderer(WidgetRenderers.icon<Button>("icons/mini/x".tempadId).colored().withCentered(7, 7))
                it.withSize(7, 7)
                it.withCallback {
                    if (minecraft == null || selected == null) return@withCallback
                    val (provider, locationId, _) = selected!!
                    DeleteLocationPacket(menu.ctxHolder, provider, locationId).sendToServer()

                    locations[provider]?.remove(locationId)
                    selected = null
                    updateLocationPanel()
                    updateInfoPanel()
                }
                it.withTooltip(deleteText)
            },
        )

        infoLayout.arrangeElements()
        infoLayout.visitWidgets(::addRenderableWidget)

        teleportBtn += addRenderableWidget(
            Widgets.button {
                it.withTexture(TempadUI.button)
                it.withRenderer(WidgetRenderers.text<Button>(teleportText).withColor(ConstantColors.black))
                it.withSize(76, 16)
                it.withCallback {
                    if (minecraft == null || selected == null) return@withCallback
                    val (provider, locationId, _) = selected!!
                    OpenTimedoorPacket(provider, locationId, menu.ctxHolder).sendToServer()
                    this.onClose()
                }
                it.withPosition(localLeft + 118, localTop + 98)
            },
        )
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, pButton: Int): Boolean {
        if (mouseX >= localLeft + 118 && mouseX <= localLeft + 193 && mouseY >= localTop + 20 && mouseY <= localTop + 95 && !menu.carried.isEmpty) {
            selected?.let { (provider, locationId, _) ->
                WriteToCardPacket(provider, locationId, menu.ctxHolder).sendToServer()
                return true
            }
        }
        return super.mouseClicked(mouseX, mouseY, pButton)
    }

    override fun mouseDragged(pMouseX: Double, pMouseY: Double, pButton: Int, pDragX: Double, pDragY: Double): Boolean {
        super.mouseDragged(pMouseX, pMouseY, pButton, pDragX, pDragY)
        if (locationList.isDragging) { // todo fix olympus
            locationList.mouseDragged(pMouseX, pMouseY, pButton, pDragX, pDragY)
        }
        return true
    }

    override fun renderBg(graphics: GuiGraphics, partialTick: Float, mouseX: Int, mouseY: Int) {
        super.renderBg(graphics, partialTick, mouseX, mouseY)
        if (mouseX >= localLeft + 118 && mouseX <= localLeft + 193 && mouseY >= localTop + 20 && mouseY <= localTop + 95 && !menu.carried.isEmpty && selected != null) {
            graphics.renderOutline(localLeft + 118, localTop + 20, 76, 76, Tempad.HIGHLIGHTED_ORANGE.value)
        }
    }
}

enum class Sorting: Translatable {
    Dimension, Alphabetical, Type;

    override fun getTranslationKey(): String? {
        return "button.tempad.${this.toString().lowercase()}"
    }

    fun reorganize(values: Map<ResourceLocation, Map<UUID, NamedGlobalVec3>>): Map<Component, List<Triple<ResourceLocation, UUID, NamedGlobalVec3>>> {
        when (this) {
            Dimension -> {
                // provider to map of id to pos -> dimension to list of provider to id to pos
                val dimensionMap = values.flatMap { (provider, locations) ->
                    locations.map { (id, pos) -> Triple(provider, id, pos) }
                }.groupBy { (_, _, pos) -> Component.translatable(pos.dimension.location().toLanguageKey("dimension")) }
                return dimensionMap.mapValues { (_, value) -> value.sortedBy { it.third.toString() } }
            }
            Alphabetical -> {
                // provider to map of id to pos -> alphabetical letter to list of provider to id to pos
                val alphabetMap = values.flatMap { (provider, locations) ->
                    locations.map { (id, pos) -> Triple(provider, id, pos) }
                }.sortedBy { it.third.name.string }
                    .groupBy { Component.literal(it.third.name.string.first().uppercaseChar().toString()) }
                return alphabetMap.mapValues { (_, value) -> value.sortedBy { it.third.name.string } }
            }
            Type -> {
                // provider to map of id to pos -> type to list of provider to id to pos
                val typeMap = values.flatMap { (provider, locations) ->
                    locations.map { (id, pos) -> Triple(provider, id, pos) }
                }.groupBy { Component.translatable(it.first.toLanguageKey("provider")) }
                return typeMap.mapValues { (_, value) -> value.sortedBy { it.third.toString() } }
            }
        }
    }
}