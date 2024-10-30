package earth.terrarium.tempad.client.screen.tempad

import com.mojang.blaze3d.systems.RenderSystem
import com.teamresourceful.resourcefullib.client.components.selection.SelectionList
import com.teamresourceful.resourcefullib.client.screens.AbstractContainerCursorScreen
import earth.terrarium.olympus.client.components.textbox.TextBox
import earth.terrarium.tempad.Tempad
import earth.terrarium.tempad.tempadId
import earth.terrarium.tempad.api.app.AppRegistry
import earth.terrarium.tempad.client.widgets.buttons.AppButton
import earth.terrarium.tempad.common.menu.AbstractTempadMenu
import earth.terrarium.tempad.common.network.c2s.OpenAppPacket
import earth.terrarium.tempad.common.utils.sendToServer
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Inventory
import org.lwjgl.glfw.GLFW

abstract class AbstractTempadScreen<T: AbstractTempadMenu<*>>(val appSprite: ResourceLocation?, menu: T, val inv: Inventory, title: Component): AbstractContainerCursorScreen<T>(menu, inv, title) {
    init {
        this.imageWidth = 256
        this.imageHeight = 256
        this.titleLabelX = 34
        this.titleLabelY = 27
    }

    var localLeft: Int = 0
    var localTop: Int = 0

    companion object {
        val SPRITE = "screen/tempad".tempadId
    }

    override fun init() {
        super.init()
        this.localTop = this.topPos + 20
        this.localLeft = this.leftPos + 30

        val appList = SelectionList<AppButton>(localLeft - 16, localTop + 1, 16, 116, 15) { button ->
            OpenAppPacket(button!!.appId, menu.ctxHolder).sendToServer()
        }

        for ((id, app) in AppRegistry.getAll(menu.ctxHolder.getCtx(minecraft!!.player!!))) {
            appList.addEntry(AppButton(app, id))
        }

        addRenderableWidget(appList)
    }

    override fun renderBg(graphics: GuiGraphics, partialTick: Float, mouseX: Int, mouseY: Int) {
        graphics.blitSprite(SPRITE, this.leftPos, this.topPos, this.imageWidth, this.imageHeight)
        appSprite?.let {
            RenderSystem.enableBlend()
            graphics.blitSprite(it, this.leftPos + 30, this.topPos + 20, 198, 118)
        }
    }

    override fun keyPressed(pKeyCode: Int, pScanCode: Int, pModifiers: Int): Boolean {
        (focused as? TextBox)?.let {
            if (pKeyCode == GLFW.GLFW_KEY_ESCAPE) {
                focused = null
                return true
            } else if ((!it.keyPressed(pKeyCode, pScanCode, pModifiers) && !it.isFocused && !it.isActive && !it.isVisible) || pKeyCode == GLFW.GLFW_KEY_TAB) {
                return super.keyPressed(pKeyCode, pScanCode, pModifiers)
            } else {
                return true
            }
        }
        return super.keyPressed(pKeyCode, pScanCode, pModifiers)
    }

    override fun renderLabels(pGuiGraphics: GuiGraphics, pMouseX: Int, pMouseY: Int) {
        pGuiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, Tempad.ORANGE.value, true)
    }

    override fun render(pGuiGraphics: GuiGraphics, pMouseX: Int, pMouseY: Int, pPartialTick: Float) {
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick)
        this.renderTooltip(pGuiGraphics, pMouseX, pMouseY)
    }
}