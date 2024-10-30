package earth.terrarium.tempad.common.block

import earth.terrarium.tempad.common.registries.ModBlocks
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState

class WorkstationBE(pos: BlockPos, state: BlockState): BlockEntity(ModBlocks.workstationBE, pos, state) {
}