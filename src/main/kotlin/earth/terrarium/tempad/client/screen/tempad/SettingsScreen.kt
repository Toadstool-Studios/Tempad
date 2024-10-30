package earth.terrarium.tempad.client.screen.tempad

import earth.terrarium.olympus.client.components.Widgets
import earth.terrarium.olympus.client.components.base.ListWidget
import earth.terrarium.olympus.client.components.buttons.Button
import earth.terrarium.olympus.client.components.dropdown.DropdownState
import earth.terrarium.olympus.client.components.renderers.WidgetRenderers
import earth.terrarium.olympus.client.constants.MinecraftColors
import earth.terrarium.tempad.Tempad
import earth.terrarium.tempad.api.app.AppRegistry
import earth.terrarium.tempad.api.macro.MacroRegistry
import earth.terrarium.tempad.client.TempadUI
import earth.terrarium.tempad.client.TempadUI.style
import earth.terrarium.tempad.common.network.c2s.SaveSettingsPacket
import earth.terrarium.tempad.common.registries.ModMenus
import earth.terrarium.tempad.common.registries.defaultApp
import earth.terrarium.tempad.common.registries.defaultMacro
import earth.terrarium.tempad.common.utils.sendToServer
import earth.terrarium.tempad.common.utils.toLanguageKey
import earth.terrarium.tempad.common.utils.translatable
import net.minecraft.client.gui.layouts.FrameLayout
import net.minecraft.network.chat.CommonComponents
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Inventory

class SettingsScreen(menu: ModMenus.SettingsMenu, inv: Inventory, title: Component) :
    AbstractTempadScreen<ModMenus.SettingsMenu>(null, menu, inv, title) {

    var defaultMacro: DropdownState<ResourceLocation> = DropdownState.of(menu.ctx.stack.defaultMacro)
    var defaultApp: DropdownState<ResourceLocation> = DropdownState.of(menu.ctx.stack.defaultApp)

    override fun init() {
        super.init()

        val settings = addRenderableWidget(ListWidget(190, 118)).apply {
            withGap(2)
            setPosition(localLeft + 4, localTop + 20)
        }

        settings.add(Widgets.labelled(font, "settings.default_macro".toLanguageKey("app"), Tempad.ORANGE, Widgets.dropdown(
            defaultMacro,
            MacroRegistry.getIds().toList(),
            { Component.translatable(it.toLanguageKey("macro")) },
            {
                it.withTexture(TempadUI.button)
                it.withSize(80, 20)
                it.withRenderer(defaultMacro.withRenderer { macro, enabled ->
                    (macro?.let { WidgetRenderers.textWithChevron<Button>(it.toLanguageKey("macro").translatable, enabled) } ?: WidgetRenderers.ellpsisWithChevron(enabled)).withColor(MinecraftColors.BLACK)
                }.withPadding(0, 4))
            },
            { it.style { it.toLanguageKey("macro").translatable } }
        )))

        settings.add(Widgets.labelled(font, "settings.default_app".toLanguageKey("app"), Tempad.ORANGE, Widgets.dropdown(
            defaultApp,
            AppRegistry.getIds().toList(),
            { Component.translatable(it.toLanguageKey("app")) },
            {
                it.withTexture(TempadUI.button)
                it.withSize(80, 20)
                it.withRenderer(defaultApp.withRenderer { app, enabled ->
                    (app?.let { WidgetRenderers.textWithChevron<Button>(it.toLanguageKey("app").translatable, enabled) } ?: WidgetRenderers.ellpsisWithChevron(enabled)).withColor(MinecraftColors.BLACK)
                }.withPadding(0, 4))
            },
            { it.style { it.toLanguageKey("app").translatable } }
        )))

        val layout = FrameLayout(localLeft, localTop, 198, 118).setMinDimensions(198, 118)

        layout.addChild(Widgets.button {
            it.withSize(font.width(CommonComponents.GUI_DONE) + 12, 16)
            it.withTexture(TempadUI.button)
            it.withRenderer(WidgetRenderers.text<Button>(CommonComponents.GUI_DONE).withColor(MinecraftColors.BLACK))
            it.withCallback {
                SaveSettingsPacket(menu.ctxHolder, defaultApp.get(), defaultMacro.get()).sendToServer()
            }
        }) { it.alignHorizontallyRight().alignVerticallyBottom().padding(4) }

        layout.arrangeElements()
        layout.visitWidgets { addRenderableWidget(it) }
    }
}