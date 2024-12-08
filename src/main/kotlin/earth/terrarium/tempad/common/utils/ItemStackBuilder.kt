package earth.terrarium.tempad.common.utils

import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack

fun Item.stack(count: Int = 1, components: (ItemStack.() -> Unit)? = null): ItemStack {
    return ItemStack(this, count).also { stack -> components?.invoke(stack) }
}
