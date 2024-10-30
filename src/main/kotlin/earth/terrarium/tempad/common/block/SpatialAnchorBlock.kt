package earth.terrarium.tempad.common.block

import com.mojang.serialization.MapCodec
import com.teamresourceful.resourcefullib.common.color.ConstantColors
import earth.terrarium.tempad.Tempad
import earth.terrarium.tempad.common.network.s2c.OpenSpatialAnchor
import earth.terrarium.tempad.common.registries.*
import earth.terrarium.tempad.common.utils.contains
import earth.terrarium.tempad.common.utils.sendToClient
import earth.terrarium.tempad.common.utils.stack
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.component.DataComponents
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.ItemInteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.BaseEntityBlock
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.storage.loot.LootParams
import net.minecraft.world.level.storage.loot.parameters.LootContextParams
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape
import net.neoforged.neoforge.common.Tags

class SpatialAnchorBlock : BaseEntityBlock(Properties.of()) {
    companion object {
        val codec: MapCodec<out BaseEntityBlock> = simpleCodec { ModBlocks.spatialAnchor }
        val shape = Shapes.or(
            box(0.0, 0.0, 0.0, 16.0, 2.0, 16.0),
            box(3.0, 3.0, 3.0, 13.0, 4.0, 13.0)
        )
    }

    init {
        this.registerDefaultState(
            stateDefinition.any().setValue(BlockStateProperties.FACING, Direction.NORTH)
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
        if (stack.item === ModItems.locationCard) {
            if (level.isClientSide) return ItemInteractionResult.sidedSuccess(level.isClientSide)
            val blockEntity = level.getBlockEntity(pos) as SpatialAnchorBE
            /* // TODO implement dynamic providers
            level.anchorPointData[blockEntity.posId!!]?.let {
                stack.targetPos = it
            }
             */
            return ItemInteractionResult.sidedSuccess(level.isClientSide)
        }

        val color = when (stack) {
            in Tags.Items.DYES_RED -> ConstantColors.red
            in Tags.Items.DYES_GREEN -> ConstantColors.green
            in Tags.Items.DYES_BLUE -> ConstantColors.blue
            in Tags.Items.DYES_YELLOW -> ConstantColors.yellow
            in Tags.Items.DYES_BLACK -> ConstantColors.black
            in Tags.Items.DYES_BROWN -> ConstantColors.brown
            in Tags.Items.DYES_CYAN -> ConstantColors.cyan
            in Tags.Items.DYES_GRAY -> ConstantColors.gray
            in Tags.Items.DYES_LIGHT_BLUE -> ConstantColors.powderblue
            in Tags.Items.DYES_LIGHT_GRAY -> ConstantColors.lightgray
            in Tags.Items.DYES_LIME -> ConstantColors.lime
            in Tags.Items.DYES_MAGENTA -> ConstantColors.magenta
            in Tags.Items.DYES_ORANGE -> Tempad.ORANGE
            in Tags.Items.DYES_PINK -> ConstantColors.pink
            in Tags.Items.DYES_PURPLE -> ConstantColors.purple
            in Tags.Items.DYES_WHITE -> ConstantColors.white
            else -> return super.useItemOn(stack, state, level, pos, player, hand, hitResult)
        }

        (level as? ServerLevel)?.getBlockEntity(pos)?.let {
            it as SpatialAnchorBE
            it.color = color
            level.setBlockAndUpdate(pos, state)
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide)
    }

    override fun useWithoutItem(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hitResult: BlockHitResult,
    ): InteractionResult {
        if (level.isClientSide) return InteractionResult.sidedSuccess(level.isClientSide)
        (level.getBlockEntity(pos) as? SpatialAnchorBE)?.let {
            OpenSpatialAnchor(pos, it.color, it.name.string, it.accessId).sendToClient(player)
        }
        return InteractionResult.sidedSuccess(level.isClientSide)
    }

    override fun getDrops(state: BlockState, params: LootParams.Builder): MutableList<ItemStack> {
        return mutableListOf(
            ModItems.spatialAnchor.stack {
                (params.getParameter(LootContextParams.BLOCK_ENTITY) as? SpatialAnchorBE)?.let {
                    color = it.color
                    set(DataComponents.CUSTOM_NAME, it.name)
                    anchorId = it.id
                }
            }
        )
    }

    override fun onPlace(state: BlockState, level: Level, pos: BlockPos, oldState: BlockState, moved: Boolean) {
        super.onPlace(state, level, pos, oldState, moved)
        if (!level.isClientSide) (level.getBlockEntity(pos) as? SpatialAnchorBE)?.let { anchorPoints += it }
    }

    override fun onRemove(state: BlockState, level: Level, pos: BlockPos, newState: BlockState, moved: Boolean) {
        level.getBlockEntity(pos)?.let {
            (it as? SpatialAnchorBE)?.id?.let { posId -> anchorPoints -= posId }
        }
        super.onRemove(state, level, pos, newState, moved)
    }

    override fun codec(): MapCodec<out BaseEntityBlock> {
        return codec
    }

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity = SpatialAnchorBE(pos, state)

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block?, BlockState?>) {
        builder.add(BlockStateProperties.FACING)
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState {
        return defaultBlockState().setValue(BlockStateProperties.FACING, context.horizontalDirection.opposite)
    }

    override fun getShape(
        state: BlockState,
        level: BlockGetter,
        pos: BlockPos,
        context: CollisionContext,
    ): VoxelShape {
        return shape
    }
}