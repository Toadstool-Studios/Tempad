package earth.terrarium.tempad.common.network.s2c

import com.teamresourceful.bytecodecs.base.ByteCodec
import com.teamresourceful.bytecodecs.base.`object`.ObjectByteCodec
import com.teamresourceful.resourcefullib.common.bytecodecs.ExtraByteCodecs
import com.teamresourceful.resourcefullib.common.color.Color
import com.teamresourceful.resourcefullib.common.network.Packet
import com.teamresourceful.resourcefullib.common.network.base.ClientboundPacketType
import earth.terrarium.tempad.client.TempadClient
import earth.terrarium.tempad.common.network.ClientPacketCompanion
import earth.terrarium.tempad.tempadId
import net.minecraft.core.BlockPos
import net.minecraft.resources.ResourceLocation

data class OpenSpatialAnchor(val blockPos: BlockPos, val color: Color, val name: String, val access: ResourceLocation): Packet<OpenSpatialAnchor> {
    override fun type(): ClientboundPacketType<OpenSpatialAnchor> = Companion

    companion object: ClientPacketCompanion<OpenSpatialAnchor> {
        override val id: ResourceLocation = "open_spatial_anchor".tempadId

        override val byteCodec: ByteCodec<OpenSpatialAnchor> = ObjectByteCodec.create(
            ExtraByteCodecs.BLOCK_POS.fieldOf(OpenSpatialAnchor::blockPos),
            Color.BYTE_CODEC.fieldOf(OpenSpatialAnchor::color),
            ByteCodec.STRING.fieldOf(OpenSpatialAnchor::name),
            ExtraByteCodecs.RESOURCE_LOCATION.fieldOf(OpenSpatialAnchor::access),
            ::OpenSpatialAnchor
        )

        override fun onReceive(packet: OpenSpatialAnchor) {
            TempadClient.openSpatialAnchorScreen(packet)
        }
    }
}