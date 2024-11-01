package earth.terrarium.tempad.client.block

import com.mojang.blaze3d.vertex.PoseStack
import earth.terrarium.tempad.common.block.WorkstationBE
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer

class WorkstationRenderer: BlockEntityRenderer<WorkstationBE> {
    override fun render(
        blockEntity: WorkstationBE,
        partialTick: Float,
        poseStack: PoseStack,
        bufferSource: MultiBufferSource,
        packedLight: Int,
        packedOverlay: Int,
    ) {
        TODO("Not yet implemented")
    }
}