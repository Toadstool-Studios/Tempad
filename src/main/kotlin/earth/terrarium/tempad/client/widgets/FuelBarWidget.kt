package earth.terrarium.tempad.client.widgets

import earth.terrarium.tempad.Tempad.Companion.tempadId
import earth.terrarium.tempad.api.fuel.FuelHandler
import earth.terrarium.tempad.common.fuel.EmptyFuel
import earth.terrarium.tempad.common.utils.get
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.components.Renderable
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.network.chat.CommonComponents
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory

class FuelBarWidget(val inv: Inventory, val slot: Int, x: Int, y: Int): AbstractWidget(x, y, WIDTH, HEIGHT, CommonComponents.EMPTY) {
    companion object {
        const val WIDTH = 6
        const val HEIGHT = 54

        val SPRITE = "misc/bar".tempadId
    }

    val fuelHandler get() = inv[slot][FuelHandler.CAPABILITY] ?: EmptyFuel

    override fun renderWidget(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        val fuelAmount = fuelHandler.charges
        val capacity = fuelHandler.totalCharges
        val percentage = fuelAmount.toFloat() / capacity.toFloat()
        val barHeight = (percentage * HEIGHT).toInt()
        graphics.blitSprite(SPRITE, WIDTH, HEIGHT, 0, 0, x, y + HEIGHT - barHeight, WIDTH, barHeight)
    }

    override fun updateWidgetNarration(pNarrationElementOutput: NarrationElementOutput) {}
}
