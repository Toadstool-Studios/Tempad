package earth.terrarium.tempad.client.screen.tempad

import earth.terrarium.olympus.client.components.Widgets
import earth.terrarium.olympus.client.components.buttons.Button
import earth.terrarium.olympus.client.components.renderers.WidgetRenderers
import earth.terrarium.olympus.client.constants.MinecraftColors
import earth.terrarium.tempad.Tempad
import earth.terrarium.tempad.client.TempadUI
import earth.terrarium.tempad.tempadId
import earth.terrarium.tempad.client.widgets.TimelineWidget
import earth.terrarium.tempad.client.widgets.TimelineEntry
import earth.terrarium.tempad.common.network.c2s.BackTrackLocation
import earth.terrarium.tempad.common.registries.ModMenus
import earth.terrarium.tempad.common.utils.sendToServer
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.StringWidget
import net.minecraft.client.gui.screens.inventory.InventoryScreen
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory

class TimelineScreen(menu: ModMenus.TimelineMenu, inv: Inventory, title: Component): AbstractTempadScreen<ModMenus.TimelineMenu>(
    sprite, menu, inv, title) {
    companion object {
        val trackingText = Component.translatable("app.tempad.timeline.tracking")
        val teleportButton = Component.translatable("app.tempad.timeline.teleport")
        val sprite = "screen/timeline".tempadId
    }

    override fun init() {
        super.init()

        val timeline = TimelineWidget(125, 72)
        timeline.setPosition(localLeft + 5, localTop + 21)

        for ((dateTime, location) in menu.appContent.history.toSortedMap()) {
            timeline.add(TimelineEntry(timeline.width, font, dateTime, location))
        }

        timeline.scrollToBottom()
        addRenderableWidget(timeline)

        val teleportBtn = addRenderableWidget(Widgets.button {
            it.withSize(font.width(teleportButton) + 12, 16)
            it.withTexture(TempadUI.button)
            it.withRenderer(WidgetRenderers.text<Button>(teleportButton).withColor(MinecraftColors.BLACK))
            it.withCallback {
                (timeline.current as? TimelineEntry)?.let {
                    BackTrackLocation(it.date, menu.ctxHolder).sendToServer()
                    onClose()
                }
            }
        })

        teleportBtn.x = localLeft + 66 - teleportBtn.width / 2
        teleportBtn.y = localTop + 94

        addRenderableWidget(StringWidget(localLeft + 133, localTop + 23, 61, 12, trackingText, font)).setColor(Tempad.ORANGE.value)
        addRenderableWidget(StringWidget(localLeft + 133, localTop + 98, 61, 17, inv.player.name, font)).setColor(Tempad.ORANGE.value)
    }

    override fun renderBg(graphics: GuiGraphics, partialTick: Float, mouseX: Int, mouseY: Int) {
        super.renderBg(graphics, partialTick, mouseX, mouseY)
        minecraft?.player?.let {
            InventoryScreen.renderEntityInInventoryFollowsMouse(
                graphics, localLeft + 134, localTop + 37, localLeft + 192, localTop + 94, 50, 0.6f,
                mouseX.toFloat(),
                mouseY.toFloat(),
                it
            )
        }
    }
}