package earth.terrarium.tempad.client.screen.tempad

import com.teamresourceful.resourcefullib.common.color.Color
import com.teamresourceful.resourcefullib.common.color.ConstantColors
import earth.terrarium.olympus.client.components.Widgets
import earth.terrarium.olympus.client.components.buttons.Button
import earth.terrarium.olympus.client.components.map.MapRenderer
import earth.terrarium.olympus.client.components.renderers.WidgetRenderers
import earth.terrarium.olympus.client.ui.UIIcons
import earth.terrarium.tempad.Tempad
import earth.terrarium.tempad.client.TempadUI
import earth.terrarium.tempad.client.TempadUI.colored
import earth.terrarium.tempad.client.state.AppearanceState
import earth.terrarium.tempad.client.state.MutableState
import earth.terrarium.tempad.common.network.c2s.CreateLocationPacket
import earth.terrarium.tempad.common.registries.ModMenus
import earth.terrarium.tempad.common.utils.globalPos
import earth.terrarium.tempad.common.utils.sendToServer
import earth.terrarium.tempad.common.utils.toLanguageKey
import net.minecraft.client.gui.components.StringWidget
import net.minecraft.client.gui.layouts.FrameLayout
import net.minecraft.client.gui.layouts.LinearLayout
import net.minecraft.core.Vec3i
import net.minecraft.network.chat.CommonComponents
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory

class NewLocationScreen(menu: ModMenus.NewLocationMenu, inv: Inventory, title: Component) :
    AbstractTempadScreen<ModMenus.NewLocationMenu>(null, menu, inv, title) {
    companion object {
        val NAME_FIELD = "new_location.name".toLanguageKey("app")
        val COLOR_FIELD = "new_location.color".toLanguageKey("app")

        val COLORS = listOf(
            Tempad.ORANGE,
            ConstantColors.yellow,
            ConstantColors.lime,
            ConstantColors.cyan,
            ConstantColors.purple,
            ConstantColors.red,
            ConstantColors.white,
            ConstantColors.black,
            Color.RAINBOW
        )

        fun getEntries(pos: Vec3i): List<Component> {
            return listOf(
                Component.literal("X: ${pos.x}"),
                Component.literal("Y: ${pos.y}"),
                Component.literal("Z: ${pos.z}"),
            )
        }
    }

    var currentColor = MutableState.of(Color(0xff6f00))
    var doneBtn: AppearanceState = AppearanceState()
    var textState = MutableState.of("")

    var mapState = MutableState.of<MapRenderer>()

    override fun init() {
        super.init()

        addRenderableOnly(Widgets.map(mapState, {
            it.withPosition(localLeft + 100, localTop + 20)
            it.withSize(94)
            it.withTexture(TempadUI.element.get(true, false))
         }))

        val mapFrameLayout = FrameLayout(localLeft + 101, localTop + 21, 92, 92).setMinDimensions(92, 92)
        val posInfoLayout = LinearLayout(0,0, LinearLayout.Orientation.VERTICAL).spacing(2)
        mapFrameLayout.addChild(posInfoLayout) { it.alignVerticallyBottom().alignHorizontallyRight().padding(4) }

        minecraft?.player?.globalPos?.let { pos ->
            mapFrameLayout.addChild(StringWidget(Component.translatable(pos.dimension().location().toLanguageKey("dimension")), font).setColor(Tempad.ORANGE.value)) {
                it.alignVerticallyTop()
                it.alignHorizontallyLeft()
                it.padding(4)
            }

            getEntries(pos.pos).forEach { component ->
                posInfoLayout.addChild(StringWidget(component, font).setColor(Tempad.ORANGE.value)) { it.alignHorizontallyRight() }
            }
        }

        mapFrameLayout.arrangeElements()
        mapFrameLayout.visitWidgets { addRenderableWidget(it) }

        val layout = LinearLayout(0, 0, LinearLayout.Orientation.VERTICAL).spacing(4)

        layout.addChild(
            StringWidget(
                this.localLeft + 4,
                this.localTop + 32,
                100,
                8,
                COLOR_FIELD,
                font
            ).setColor(Tempad.ORANGE.value).alignLeft()
        )

        val colorOptions = layout.addChild(LinearLayout(0, 0, LinearLayout.Orientation.HORIZONTAL).spacing(2))

        colorOptions.addChild(Widgets.colorPicker(currentColor, false, { btn ->
            btn.withSize(16, 16)
            btn.withTexture(TempadUI.button)
            btn.withPosition(localLeft + 4, localTop + 44)
            btn.withRenderer(currentColor.withRenderer{ color -> WidgetRenderers.solid<Button>().withoutAlpha().withColor(color).withPadding(2) })
        }, { overlay ->
            overlay.withBackground(TempadUI.modal)
            overlay.withInset(TempadUI.element.get(true, false))
            overlay.withEyedropperSettings {
                it.withSize(16)
                it.withTexture(TempadUI.button)
                it.withRenderer(WidgetRenderers.icon<Button>(UIIcons.EYE_DROPPER).withColor(ConstantColors.black).withCentered(12, 12))
            }
            overlay.withDropdownButtonSettings { btn, state ->
                btn.withSize(82, 16)
                btn.withRenderer(state.withRenderer {preset, isOpen ->
                    WidgetRenderers.textWithChevron<Button>(Component.translatable(preset!!.translationKey), isOpen).colored()
                })
            }
            overlay.withDropdownSettings { dropdown, state ->
                dropdown.withTexture(TempadUI.modal)
                dropdown.withEntrySprites(null)
                dropdown.withEntryHeight(14)
                dropdown.withEntryRenderer {
                    WidgetRenderers.text<Button>(Component.translatable(it.translationKey)).withLeftAlignment().colored().withPadding(0, 3)
                }
            }
        }))

        colorOptions.addChild(Widgets.colorInput(currentColor) {
            it.withSize(76, 16)
            it.withTexture(TempadUI.element)
            it.withTextColor(Tempad.ORANGE)
            it.withPosition(localLeft + 22, localTop + 44)
        })

        layout.addChild(
            StringWidget(
                this.localLeft + 4,
                this.localTop + 60,
                100,
                8,
                NAME_FIELD,
                font
            ).setColor(Tempad.ORANGE.value).alignLeft()
        )

        layout.addChild(Widgets.textInput(textState) {
            it.withSize(94, 16)
            it.withTexture(TempadUI.element)
            it.withTextColor(Tempad.ORANGE)
            it.withPosition(localLeft + 4, localTop + 70)
            it.withMaxLength(32)
        })

        layout.arrangeElements()
        FrameLayout.alignInRectangle(layout, localLeft + 4, localTop + 20, 94, 78, 0f, 0.5f)
        layout.visitWidgets { addRenderableWidget(it) }

        doneBtn += addRenderableWidget(Widgets.button {
            it.withSize(48, 16)
            it.withTexture(TempadUI.button)
            it.withPosition(localLeft + 98 - 48, localTop + 98)
            it.withRenderer(WidgetRenderers.text<Button>(CommonComponents.GUI_DONE).withColor(ConstantColors.black))
            it.withCallback {
                CreateLocationPacket(textState.value, currentColor.value, menu.ctxHolder).sendToServer()
                this.onClose()
            }
        })
    }
}
