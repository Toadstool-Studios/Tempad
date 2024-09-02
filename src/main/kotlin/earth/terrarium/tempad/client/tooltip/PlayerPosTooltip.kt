package earth.terrarium.tempad.client.tooltip

import com.mojang.authlib.GameProfile
import earth.terrarium.tempad.Tempad
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.PlayerFaceRenderer
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.network.chat.Component
import org.joml.Matrix4f
import sun.java2d.cmm.Profile

class PlayerPosTooltip(val profile: GameProfile): ClientTooltipComponent {
    val text = Component.literal(profile.name)

    val playerTexture get() = Minecraft.getInstance().skinManager.getInsecureSkin(profile).texture()

    override fun getHeight(): Int = 10
    override fun getWidth(font: Font): Int = font.width(text) + 13

    override fun renderText(
        font: Font,
        mouseX: Int,
        mouseY: Int,
        matrix: Matrix4f,
        bufferSource: MultiBufferSource.BufferSource,
    ) {
        super.renderText(font, mouseX, mouseY, matrix, bufferSource)
        font.drawInBatch(
            text,
            mouseX.toFloat() + 13,
            mouseY.toFloat() + 1,
            Tempad.ORANGE.value,
            true,
            matrix,
            bufferSource,
            Font.DisplayMode.NORMAL,
            0,
            15728880
        )
    }

    override fun renderImage(font: Font, x: Int, y: Int, guiGraphics: GuiGraphics) {
        super.renderImage(font, x, y, guiGraphics)
        PlayerFaceRenderer.draw(guiGraphics, playerTexture, x, y, 9)
    }
}