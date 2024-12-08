package earth.terrarium.tempad.client.block

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import earth.terrarium.tempad.common.block.WorkstationBE
import earth.terrarium.tempad.common.utils.get
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer
import net.minecraft.client.renderer.entity.ItemRenderer
import net.minecraft.world.item.ItemDisplayContext

class WorkstationRenderer(val itemRenderer: ItemRenderer): BlockEntityRenderer<WorkstationBE> {
    override fun render(
        block: WorkstationBE,
        partialTick: Float,
        poseStack: PoseStack,
        bufferSource: MultiBufferSource,
        packedLight: Int,
        packedOverlay: Int,
    ) {
        val item = block.inventory[0]
        if(item.isEmpty) return

        poseStack.pushPose()
        poseStack.translate(0.5625, 0.125 + 0.03125, 0.375)
        poseStack.scale(0.625f, 0.625f, 0.625f)
        poseStack.mulPose(Axis.XP.rotation((Math.PI * 0.5).toFloat()))
        itemRenderer.renderStatic(item, ItemDisplayContext.FIXED, packedLight, packedOverlay, poseStack, bufferSource, block.level, 0)
        poseStack.popPose()
    }
}