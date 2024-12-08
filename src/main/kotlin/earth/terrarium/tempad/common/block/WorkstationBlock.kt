package earth.terrarium.tempad.common.block

import com.mojang.serialization.MapCodec
import earth.terrarium.tempad.api.tva_device.chronons
import earth.terrarium.tempad.api.tva_device.upgrades
import earth.terrarium.tempad.common.registries.ModBlocks
import earth.terrarium.tempad.common.utils.get
import earth.terrarium.tempad.common.utils.set
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.ItemInteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.BaseEntityBlock
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.phys.BlockHitResult

class WorkstationBlock : BaseEntityBlock(Properties.of().noOcclusion()) {
    val codec: MapCodec<out BaseEntityBlock?> = simpleCodec { ModBlocks.workstation }

    init {
        this.registerDefaultState(
            stateDefinition.any().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH)
        )
    }

    override fun useItemOn(
        stack: ItemStack,
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hand: InteractionHand,
        hitResult: BlockHitResult,
    ): ItemInteractionResult {
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS
        }

        val blockEntity = level.getBlockEntity(pos) as? WorkstationBE
            ?: return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
        if (blockEntity.inventory[0].isEmpty) {
            blockEntity.inventory[0] = stack
            blockEntity.setChanged()
            player.setItemInHand(hand, ItemStack.EMPTY)
            return ItemInteractionResult.SUCCESS
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
    }

    override fun useWithoutItem(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hitResult: BlockHitResult,
    ): InteractionResult {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS
        }

        val blockEntity = level.getBlockEntity(pos) as? WorkstationBE
            ?: return InteractionResult.PASS
        player.inventory.placeItemBackInInventory(blockEntity.inventory[0])
        blockEntity.inventory[0] = ItemStack.EMPTY
        blockEntity.setChanged()
        return InteractionResult.SUCCESS
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState {
        return defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, context.horizontalDirection)
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block?, BlockState?>) {
        builder.add(BlockStateProperties.HORIZONTAL_FACING)
    }

    override fun codec(): MapCodec<out BaseEntityBlock?> = codec

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity = WorkstationBE(pos, state)

    override fun getRenderShape(state: BlockState): RenderShape = RenderShape.MODEL
}