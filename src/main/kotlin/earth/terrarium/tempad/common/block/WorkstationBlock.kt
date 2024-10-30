package earth.terrarium.tempad.common.block

import com.mojang.serialization.MapCodec
import earth.terrarium.tempad.common.registries.ModBlocks
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.BaseEntityBlock
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState

class WorkstationBlock : BaseEntityBlock(Properties.of()) {
    val codec: MapCodec<out BaseEntityBlock?> = simpleCodec { ModBlocks.workstation }

    override fun codec(): MapCodec<out BaseEntityBlock?> = codec

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity = WorkstationBE(pos, state)

    override fun getRenderShape(state: BlockState): RenderShape = RenderShape.MODEL
}