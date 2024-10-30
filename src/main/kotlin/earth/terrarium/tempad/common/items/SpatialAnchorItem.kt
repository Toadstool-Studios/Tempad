package earth.terrarium.tempad.common.items

import earth.terrarium.tempad.common.block.SpatialAnchorBE
import earth.terrarium.tempad.common.registries.*
import net.minecraft.core.BlockPos
import net.minecraft.core.component.DataComponents
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState

class SpatialAnchorItem: BlockItem(ModBlocks.spatialAnchor, Properties()) {
    override fun updateCustomBlockEntityTag(
        pos: BlockPos,
        level: Level,
        player: Player?,
        stack: ItemStack,
        state: BlockState,
    ): Boolean {
        (level.getBlockEntity(pos) as? SpatialAnchorBE)?.let { blockEntity ->
            stack.color?.let { blockEntity.color = it }
            stack.get(DataComponents.CUSTOM_NAME)?.let { blockEntity.name = it }
            stack.anchorId?.let { blockEntity.id = it }
            player?.let { blockEntity.owner = it.gameProfile }
        }
        return super.updateCustomBlockEntityTag(pos, level, player, stack, state)
    }
}