package earth.terrarium.tempad.common.compat

import com.teamresourceful.bytecodecs.base.ByteCodec
import com.teamresourceful.bytecodecs.base.`object`.ObjectByteCodec
import earth.terrarium.tempad.api.Priority
import earth.terrarium.tempad.api.PriorityId
import earth.terrarium.tempad.tempadId
import earth.terrarium.tempad.api.context.ContextRegistry
import earth.terrarium.tempad.api.context.ContextType
import earth.terrarium.tempad.api.context.SyncableContext
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import top.theillusivec4.curios.api.CuriosApi
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler

class CuriosContext(val player: Player, override val data: CuriosSlotInfo) : SyncableContext<CuriosContext.CuriosSlotInfo> {
    val inventory: MutableMap<String, ICurioStacksHandler> = CuriosApi.getCuriosInventory(player).orElseThrow().curios

    override val type: ContextType<CuriosSlotInfo> = Companion.type

    override var stack: ItemStack
        get() = inventory[data.identifier]!!.stacks.getStackInSlot(data.index)
        set(value) { inventory[data.identifier]!!.stacks.setStackInSlot(data.index, value) }

    override fun addStack(stack: ItemStack) {
        player.inventory.placeItemBackInInventory(stack)
    }

    companion object {
        val id = "curios".tempadId
        val type = ContextType(id, CuriosSlotInfo.codec)
    }

    class CuriosSlotInfo(val identifier: String, val index: Int) {
        companion object {
            val codec: ByteCodec<CuriosSlotInfo> = ObjectByteCodec.create(
                ByteCodec.STRING.fieldOf(CuriosSlotInfo::identifier),
                ByteCodec.INT.fieldOf(CuriosSlotInfo::index),
                ::CuriosSlotInfo
            )
        }
    }
}

fun initCuriosCompat() {
    ContextRegistry.register(CuriosContext.type, ::CuriosContext)
    ContextRegistry.registerLocator(PriorityId(CuriosContext.id, Priority.HIGH)) { player, filter ->
        val inventory = CuriosApi.getCuriosInventory(player).orElseThrow().curios
        for ((identifier, handler) in inventory) {
            for (index in 0 until handler.stacks.slots) {
                val stack = handler.stacks.getStackInSlot(index)
                if (filter(stack)) {
                    return@registerLocator CuriosContext(player, CuriosContext.CuriosSlotInfo(identifier, index))
                }
            }
        }
        return@registerLocator null
    }
}