package earth.terrarium.tempad.common.registries

import earth.terrarium.tempad.api.Priority
import earth.terrarium.tempad.api.PriorityId
import earth.terrarium.tempad.api.context.ContextRegistry
import earth.terrarium.tempad.api.context.InventoryContext
import earth.terrarium.tempad.common.compat.initCuriosCompat
import earth.terrarium.tempad.common.menu.CarriedMenuCtx
import earth.terrarium.tempad.tempadId
import net.neoforged.fml.ModList

object ModContext {
    fun init() {
        ContextRegistry.register(InventoryContext.type, ::InventoryContext)
        ContextRegistry.register(CarriedMenuCtx.type) { player, _ -> CarriedMenuCtx(player) }

        if (ModList.get().isLoaded("curios")) {
            initCuriosCompat()
        }

        ContextRegistry.registerLocator(PriorityId("inventory".tempadId, Priority.NORMAL)) { player, filter ->
            for (index in 0 until player.inventory.containerSize) {
                val stack = player.inventory.getItem(index)
                if (filter(stack)) {
                    return@registerLocator InventoryContext(player, index)
                }
            }
            return@registerLocator null
        }
    }
}