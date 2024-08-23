package earth.terrarium.tempad.client.screen

import earth.terrarium.tempad.Tempad
import earth.terrarium.tempad.api.locations.ClientDisplay
import earth.terrarium.tempad.client.widgets.InformationPanel
import earth.terrarium.tempad.client.widgets.ModWidgets
import earth.terrarium.tempad.client.widgets.buttons.EnumButton
import earth.terrarium.tempad.client.widgets.buttons.ToggleButton
import earth.terrarium.tempad.client.widgets.colored.ColoredButton
import earth.terrarium.tempad.client.widgets.location_panel.PanelWidget
import earth.terrarium.tempad.common.config.ClientConfig
import earth.terrarium.tempad.common.data.FavoriteLocationAttachment
import earth.terrarium.tempad.common.network.c2s.DeleteLocationPacket
import earth.terrarium.tempad.common.network.c2s.OpenTimedoorPacket
import earth.terrarium.tempad.common.network.c2s.SetFavoritePacket
import earth.terrarium.tempad.common.network.c2s.WriteToCardPacket
import earth.terrarium.tempad.common.registries.ModMenus.TeleportMenu
import earth.terrarium.tempad.common.utils.btnSprites
import earth.terrarium.tempad.common.utils.display
import earth.terrarium.tempad.common.utils.sendToServer
import earth.terrarium.tempad.common.utils.toLanguageKey
import earth.terrarium.tempad.tempadId
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.components.ImageButton
import net.minecraft.client.gui.components.Tooltip
import net.minecraft.client.gui.layouts.FrameLayout
import net.minecraft.client.gui.layouts.LinearLayout
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Inventory
import org.lwjgl.glfw.GLFW
import java.util.*

class TeleportScreen(menu: TeleportMenu, inv: Inventory, title: Component) :
    AbstractTempadScreen<TeleportMenu>(SPRITE, menu, inv, title) {
    companion object {
        val SPRITE = "screen/teleport".tempadId

        fun imgBtn(key: String, onClick: (btn: Button) -> Unit): ImageButton {
            val imageButton = ImageButton(10, 10, key.btnSprites(), onClick, key.toLanguageKey("button"))
            imageButton.tooltip = Tooltip.create(Component.translatable("app.${Tempad.MOD_ID}.teleport.$key"))
            return imageButton
        }

        val teleportText = Component.translatable("app.${Tempad.MOD_ID}.teleport")

        var sortMode: Sorting
            get() = Sorting.valueOf(ClientConfig.sortingMode)
            set(value) {
                ClientConfig.sortingMode = value.name
                Tempad.CONFIGURATOR.saveConfig(ClientConfig::class.java)
            }
    }

    var selected: Triple<ResourceLocation, UUID, ClientDisplay>? = null
        set(value) {
            field = value
            teleportBtn.active = value != null
            favBtn.toggled = favorite?.matches(value?.first, value?.second) == true

            infoLayout.visitWidgets { widget ->
                widget.visible = value != null
            }

            value?.third?.let { infoTextWidget.update(it.pos.display) }
        }

    private lateinit var locationButtons: LinearLayout
    private lateinit var favBtn: ToggleButton
    private lateinit var search: EditBox
    private var favorite: FavoriteLocationAttachment? = menu.appContent.favoriteLocation
    private lateinit var locationList: PanelWidget
    private lateinit var teleportBtn: ColoredButton

    private lateinit var infoLayout: FrameLayout
    private lateinit var infoTextWidget: InformationPanel

    override fun init() {
        super.init()

        val searchValue = if (::search.isInitialized) search.value else ""
        this.search = addRenderableWidget(
            ModWidgets.search(
                localLeft + 20,
                localTop + 24,
                64,
                12
            ) { _ -> if (::locationList.isInitialized) locationList.update() }
        )

        search.value = searchValue

        this.locationList = addRenderableWidget(PanelWidget(
            menu.appContent.locations,
            this::selected,
            { selected = it },
            { search.value },
            { provider, locationId -> favorite?.matches(provider, locationId) ?: false }
        ))

        locationList.setPosition(localLeft + 4, localTop + 39)
        locationList.update()
        locationList.sortBy(sortMode)

        val sortBtn = addRenderableWidget(EnumButton(Sorting.entries) {
            sortMode = it
            locationList.sortBy(it)
        })

        sortBtn.setPosition(localLeft + 100, localTop + 20)
        sortBtn.selected = sortMode

        infoLayout = FrameLayout(74, 74)
        infoLayout.setPosition(localLeft + 119, localTop + 22)

        infoTextWidget = infoLayout.addChild(InformationPanel(font, 74, 74))
        infoTextWidget.setPosition(119, 21)

        locationButtons = infoLayout.addChild(LinearLayout(0, 0, LinearLayout.Orientation.HORIZONTAL).spacing(2)) {
            it.alignHorizontallyRight()
            it.alignVerticallyBottom()
            it.padding(4)
        }

        favBtn = locationButtons.addChild(
            ToggleButton("unpin".btnSprites(), "pin".btnSprites()) {
                if (minecraft == null || selected == null) return@ToggleButton
                val (provider, locationId, _) = selected!!
                if (favorite?.matches(provider, locationId) == true) {
                    SetFavoritePacket(null).sendToServer()
                    favorite = null
                } else {
                    SetFavoritePacket(provider, locationId).sendToServer()
                    favorite = FavoriteLocationAttachment(provider, locationId)
                }
                locationList.update()
            },
        )

        favBtn.tooltipFun = { selected ->
            Tooltip.create(
                Component.translatable(
                    if (selected) "app.${Tempad.MOD_ID}.teleport.unfavorite" else "app.${Tempad.MOD_ID}.teleport.favorite"
                )
            )
        }

        locationButtons.addChild(
            imgBtn("delete") {
                if (minecraft == null || selected == null) return@imgBtn
                val (provider, locationId, _) = selected!!
                DeleteLocationPacket(menu.ctxHolder, provider, locationId).sendToServer()
                locationList.deleteSelected()
            },
        )

        infoLayout.visitWidgets { widget ->
            addRenderableWidget(widget)
            widget.visible = false
        }
        infoLayout.arrangeElements()

        teleportBtn = addRenderableWidget(
            ColoredButton(teleportText, width = 76, height = 16, x = localLeft + 118, y = localTop + 98) {
                if (minecraft == null || selected == null) return@ColoredButton
                val (provider, locationId, _) = selected!!
                OpenTimedoorPacket(provider, locationId, menu.ctxHolder).sendToServer()
                this.onClose()
            },
        )

        teleportBtn.active = false
    }

    override fun mouseDragged(pMouseX: Double, pMouseY: Double, pButton: Int, pDragX: Double, pDragY: Double): Boolean {
        super.mouseDragged(pMouseX, pMouseY, pButton, pDragX, pDragY)
        if (locationList.isScrolling) {
            locationList.mouseDragged(pMouseX, pMouseY, pButton, pDragX, pDragY)
        }
        return true
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, pButton: Int): Boolean {
        if (!search.isMouseOver(mouseX, mouseY)) {
            search.isFocused = false
        }
        if (mouseX >= localLeft + 118 && mouseX <= localLeft + 193 && mouseY >= localTop + 20 && mouseY <= localTop + 95 && !menu.carried.isEmpty) {
            selected?.let { (provider, locationId, _) ->
                WriteToCardPacket(provider, locationId, menu.ctxHolder).sendToServer()
                return true
            }
        }
        return super.mouseClicked(mouseX, mouseY, pButton)
    }

    override fun keyPressed(pKeyCode: Int, pScanCode: Int, pModifiers: Int): Boolean {
        if (search.isFocused) {
            if (pKeyCode == GLFW.GLFW_KEY_ESCAPE) {
                search.isFocused = false
                return true
            }
            return search.keyPressed(pKeyCode, pScanCode, pModifiers)
        }
        return super.keyPressed(pKeyCode, pScanCode, pModifiers)
    }

    override fun renderBg(graphics: GuiGraphics, partialTick: Float, mouseX: Int, mouseY: Int) {
        super.renderBg(graphics, partialTick, mouseX, mouseY)
        if (mouseX >= localLeft + 118 && mouseX <= localLeft + 193 && mouseY >= localTop + 20 && mouseY <= localTop + 95 && !menu.carried.isEmpty && selected != null) {
            graphics.renderOutline(localLeft + 118, localTop + 20, 76, 76, Tempad.HIGHLIGHTED_ORANGE.value)
        }
    }
}

enum class Sorting {
    DIMENSION, ALPHABETICAL, TYPE
}