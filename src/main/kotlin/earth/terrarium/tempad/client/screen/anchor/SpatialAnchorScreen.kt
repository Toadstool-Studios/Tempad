package earth.terrarium.tempad.client.screen.anchor

import com.teamresourceful.resourcefullib.client.screens.BaseCursorScreen
import com.teamresourceful.resourcefullib.common.color.Color
import earth.terrarium.olympus.client.components.Widgets
import earth.terrarium.olympus.client.components.buttons.Button
import earth.terrarium.olympus.client.components.dropdown.DropdownState
import earth.terrarium.olympus.client.components.renderers.WidgetRenderers
import earth.terrarium.olympus.client.components.string.TextWidget
import earth.terrarium.olympus.client.constants.MinecraftColors
import earth.terrarium.olympus.client.layouts.Layouts
import earth.terrarium.olympus.client.ui.UIConstants
import earth.terrarium.tempad.api.visibility.AnchorAccessApi
import earth.terrarium.tempad.client.screen.tempad.NewLocationScreen
import earth.terrarium.tempad.client.state.MutableState
import earth.terrarium.tempad.common.network.c2s.UpdateAnchorPacket
import earth.terrarium.tempad.common.utils.sendToServer
import earth.terrarium.tempad.tempadId
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.layouts.FrameLayout
import net.minecraft.client.gui.layouts.SpacerElement
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation

class SpatialAnchorScreen(val pos: BlockPos, name: String, color: Color, access: ResourceLocation): BaseCursorScreen(Component.translatable("screen.tempad.spatial_anchor")) {
    companion object {
        private const val bgWidth = 200

        private val accessField = Component.translatable("screen.tempad.spatial_anchor.access_field")
    }

    val name: MutableState<String> = MutableState.of(name)
    val color: MutableState<Color> = MutableState.of(color)
    val access: DropdownState<ResourceLocation> = DropdownState.of(access)

    var bgHeight = 0

    override fun init() {
        super.init()

        fun TextWidget.configure(): TextWidget = this.withColor(MinecraftColors.GRAY).withShadow()
        val fields = Layouts.column().withGap(4)

        fields.withChild(FrameLayout(190, 10).apply {
            addChild(Widgets.text(title).withColor(MinecraftColors.WHITE).withShadow()) {
                it.align(0f, 0f)
            }

            addChild(Widgets.button {
                it.withSize(7)
                it.withTexture(null)
                it.withRenderer(
                    WidgetRenderers.withColors(
                        WidgetRenderers.icon<Button>("icons/mini/x".tempadId).withShadow(),
                        MinecraftColors.DARK_GRAY,
                        MinecraftColors.WHITE,
                        MinecraftColors.RED
                    ).withCentered(7, 7)
                )
                it.withCallback {
                    onClose()
                }
            }) {
                it.align(1f, 0f)
            }
        })

        fields.withChild(SpacerElement(0, 0))

        fields.withChild(Widgets.text(NewLocationScreen.NAME_FIELD).configure())
        fields.withChild(Widgets.textInput(name) {
            it.withSize(180, 20)
        })

        fields.withChild(Widgets.text(NewLocationScreen.COLOR_FIELD).configure())
        fields.withChild(Layouts.row().apply {
            withChild(Widgets.colorPicker(color, false, {
                it.withSize(20, 20)
            }, {}))

            withChild(Widgets.colorInput(color) {
                it.withSize(80, 20)
            })
        })

        fields.withChild(Widgets.text(accessField).configure())
        fields.withChild(Widgets.dropdown(access, AnchorAccessApi.visbility.keys.toList(), { Component.translatable(it.toLanguageKey("access")) },
            { it.withSize(100, 20) },
            {}
        ))

        fields.build(this::addRenderableWidget)
        bgHeight = fields.height + 12
        fields.setPosition((width - bgWidth) / 2 + 5, (height - bgHeight) / 2 + 5)
    }

    override fun renderBackground(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick)
        graphics.blitSprite(UIConstants.MODAL, (width - bgWidth) / 2, (height - bgHeight) / 2, bgWidth, bgHeight)
        graphics.blitSprite(UIConstants.MODAL_HEADER, (width - bgWidth) / 2 + 1, (height - bgHeight) / 2 + 1, bgWidth - 2, 15)
    }

    override fun onClose() {
        super.onClose()
        UpdateAnchorPacket(pos, color.value, name.value, access.get()).sendToServer()
    }

    override fun isPauseScreen(): Boolean = false
}