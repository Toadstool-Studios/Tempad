package earth.terrarium.tempad.client.screen.tempad

import com.mojang.blaze3d.platform.InputConstants
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.BufferUploader
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.Tesselator
import com.mojang.blaze3d.vertex.VertexFormat
import earth.terrarium.tempad.api.context.ContextHolder
import earth.terrarium.tempad.client.widgets.TimelineEntry
import earth.terrarium.tempad.common.data.HistoricalLocation
import earth.terrarium.tempad.common.network.c2s.BackTrackLocation
import earth.terrarium.tempad.common.utils.component
import earth.terrarium.tempad.common.utils.sendToServer
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.network.chat.CommonComponents
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.Mth
import org.joml.Vector2f
import java.util.Date
import kotlin.collections.toList
import kotlin.let
import kotlin.ranges.until

class TimeTwisterScreen(history: Map<Date, HistoricalLocation>, val ctx: ContextHolder<*>) : Screen(CommonComponents.EMPTY) {
    companion object {
        private const val MAX_ENTRIES = 8
        private const val START = 25
        private const val END = 80
        private const val END_HOVERED = 85

        private const val TEXT_LENGTH = (START + END) * 0.4f
        private const val TEXT_LENGTH_HOVERED = (START + END + 5) * 0.4f

        private const val ICON_LENGTH = (START + END) * 0.575f
        private const val ICON_LENGTH_HOVERED = (START + END + 5) * 0.575f

        private const val TEXT_COLOR = 0xAAAAAA
        private const val TEXT_COLOR_DISABLED = 0xFF5555

        private const val COLOR = 0.2f
        private const val COLOR_ODD = 0.25f
        private const val COLOR_HOVER = 1.2f
        private const val COLOR_ODD_HOVER = 1.25f

        var lastSelect: Long = 0

        fun toPoint(angle: Float, length: Float): Vector2f {
            val radians = Math.toRadians(angle.toDouble()).toFloat()
            val x = Mth.sin(radians) * length
            val y = -Mth.cos(radians) * length
            return Vector2f(x, y)
        }

        fun indexFromAngle(angle: Float): Int {
            return (Math.round(angle * MAX_ENTRIES / 360f) % MAX_ENTRIES)
        }

        fun angleFromIndex(index: Int): Float {
            return (360f / MAX_ENTRIES) * index
        }

        fun drawXYRGBA(builder: BufferBuilder, x: Float, y: Float, r: Float, g: Float, b: Float, a: Float) {
            builder.addVertex(x, y, 0f).setColor(r, g, b, a)
        }
    }

    private val locations = history.toSortedMap().reversed().entries.toList()

    private fun getIndex(): Int {
        minecraft?.let { mc ->
            val window = mc.window
            val mouse = mc.mouseHandler
            val mdx: Double
            val mdy: Double
            val minRadius: Double
            if (Minecraft.ON_OSX) {
                mdx = mouse.xpos() - window.width / 4f
                mdy = mouse.ypos() - window.height / 4f

                minRadius = window.guiScale * START / 2
            } else {
                mdx = mouse.xpos() - window.width / 2f
                mdy = mouse.ypos() - window.height / 2f

                minRadius = window.guiScale * START
            }
            if (mdx * mdx + mdy * mdy < minRadius * minRadius) return -1
            var angle = Math.toDegrees(Math.atan2(mdx, -mdy)).toFloat()
            if (angle < 0) angle += 360
            return indexFromAngle(angle)
        }
        return -1
    }

    override fun isPauseScreen(): Boolean = false

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        return if (button == 0) select(getIndex()) else super.mouseClicked(mouseX, mouseY, button)
    }

    override fun keyReleased(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        return when (keyCode) {
            InputConstants.KEY_1 -> select(0)
            InputConstants.KEY_2 -> select(1)
            InputConstants.KEY_3 -> select(2)
            InputConstants.KEY_4 -> select(3)
            InputConstants.KEY_5 -> select(4)
            InputConstants.KEY_6 -> select(5)
            InputConstants.KEY_7 -> select(6)
            InputConstants.KEY_8 -> select(7)
            else -> false
        }
    }

    private fun select(index: Int): Boolean {
        locations.getOrNull(index)?.let { (date, _) ->
            BackTrackLocation(date, ctx).sendToServer()
        }
        onClose()
        lastSelect = System.currentTimeMillis()
        return true
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTicks: Float) {
        super.render(graphics, mouseX, mouseY, partialTicks)
        RenderSystem.disableCull()
        RenderSystem.enableBlend()
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f)

        RenderSystem.setShader(GameRenderer::getPositionColorShader)
        val builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR)

        val hoveredIndex = getIndex()
        val scaledHeight = Minecraft.getInstance().window.guiScaledHeight
        val scaledWidth = Minecraft.getInstance().window.guiScaledWidth

        val middleX = scaledWidth / 2f
        val middleY = scaledHeight / 2f

        for (angle in 0 until 360) {
            val index = indexFromAngle(angle.toFloat())

            val hovered = hoveredIndex == index && index < locations.size

            val length = if (hovered) END_HOVERED else END
            val color = when {
                index % 2 == 0 -> if (hovered) COLOR_HOVER else COLOR
                else -> if (hovered) COLOR_ODD_HOVER else COLOR_ODD
            }

            val radians1 = Math.toRadians(angle.toDouble())
            val radians2 = Math.toRadians((angle + 1).toDouble())

            val x1 = Math.sin(radians1).toFloat()
            val y1 = -Math.cos(radians1).toFloat()
            val x2 = Math.sin(radians2).toFloat()
            val y2 = -Math.cos(radians2).toFloat()

            drawXYRGBA(builder, middleX + x1 * length, middleY + y1 * length, color, color, color, 0.8f)
            drawXYRGBA(builder, middleX + x2 * length, middleY + y2 * length, color, color, color, 0.8f)
            drawXYRGBA(builder, middleX + x2 * START, middleY + y2 * START, color, color, color, 0.56f)
            drawXYRGBA(builder, middleX + x1 * START, middleY + y1 * START, color, color, color, 0.56f)

            val innerColor = if (index % 2 == 0) 1f else 0.5f

            val start = length - 2

            drawXYRGBA(builder, middleX + x1 * length, middleY + y1 * length, innerColor, innerColor, innerColor, 1f)
            drawXYRGBA(builder, middleX + x2 * length, middleY + y2 * length, innerColor, innerColor, innerColor, 1f)
            drawXYRGBA(builder, middleX + x2 * start, middleY + y2 * start, innerColor, innerColor, innerColor, 1f)
            drawXYRGBA(builder, middleX + x1 * start, middleY + y1 * start, innerColor, innerColor, innerColor, 1f)
        }

        builder.build()?.let { BufferUploader.drawWithShader(it) }

        for (index in 0 until MAX_ENTRIES) {
            val hovered = hoveredIndex == index && index < locations.size
            val angle = angleFromIndex(index)
            var pos = toPoint(angle, if (hovered) TEXT_LENGTH_HOVERED else TEXT_LENGTH)

            graphics.drawCenteredString(
                font,
                (index + 1).toString(),
                (scaledWidth / 2f + pos.x()).toInt(),
                (scaledHeight / 2f + pos.y()).toInt(),
                getTextColor(index)
            )

            pos = toPoint(angle, if (hovered) ICON_LENGTH_HOVERED else ICON_LENGTH)

            if (index < locations.size) {
                val (date, history) = locations[index]
                graphics.blitSprite(
                    ResourceLocation.fromNamespaceAndPath(history.marker!!.namespace, "marker/" + history.marker.path),
                    (scaledWidth / 2f + pos.x() - 8).toInt(),
                    (scaledHeight / 2f + pos.y() - 8).toInt(),
                    16,
                    16
                )
                if (hovered) {
                    setTooltipForNextRenderPass(
                        listOf(
                            Component.translatable(history.marker.toLanguageKey("marker")).apply {
                                append(CommonComponents.SPACE)
                                append(history.dimension.component)
                            }.visualOrderText,
                            history.pos.component.visualOrderText,
                            Component.literal(TimelineEntry.dateFormat.format(date)).visualOrderText
                        )
                    )
                }
            }
        }
    }

    private fun getTextColor(index: Int): Int {
        return if (index >= locations.size) {
            TEXT_COLOR_DISABLED
        } else {
            TEXT_COLOR
        }
    }
}
