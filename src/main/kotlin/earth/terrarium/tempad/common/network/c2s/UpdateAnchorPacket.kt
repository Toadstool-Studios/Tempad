package earth.terrarium.tempad.common.network.c2s

import com.teamresourceful.bytecodecs.base.ByteCodec
import com.teamresourceful.bytecodecs.base.`object`.ObjectByteCodec
import com.teamresourceful.resourcefullib.common.bytecodecs.ExtraByteCodecs
import com.teamresourceful.resourcefullib.common.color.Color
import com.teamresourceful.resourcefullib.common.network.Packet
import com.teamresourceful.resourcefullib.common.network.base.PacketType
import earth.terrarium.tempad.common.block.SpatialAnchorBE
import earth.terrarium.tempad.common.network.ServerPacketCompanion
import earth.terrarium.tempad.common.registries.accessId
import earth.terrarium.tempad.common.registries.color
import earth.terrarium.tempad.tempadId
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Player

data class UpdateAnchorPacket(val blockPos: BlockPos, val color: Color, val name: String, val access: ResourceLocation): Packet<UpdateAnchorPacket> {
    override fun type(): PacketType<UpdateAnchorPacket> = Companion

    companion object: ServerPacketCompanion<UpdateAnchorPacket> {
        override val id: ResourceLocation = "update_anchor".tempadId
        override val byteCodec: ByteCodec<UpdateAnchorPacket> = ObjectByteCodec.create(
            ExtraByteCodecs.BLOCK_POS.fieldOf(UpdateAnchorPacket::blockPos),
            Color.BYTE_CODEC.fieldOf(UpdateAnchorPacket::color),
            ByteCodec.STRING.fieldOf(UpdateAnchorPacket::name),
            ExtraByteCodecs.RESOURCE_LOCATION.fieldOf(UpdateAnchorPacket::access),
            ::UpdateAnchorPacket
        )

        override fun onReceive(packet: UpdateAnchorPacket, player: Player) {
            (player.level().getBlockEntity(packet.blockPos) as? SpatialAnchorBE)?.let {
                it.color = packet.color
                it.name = Component.literal(packet.name)
                it.accessId = packet.access
                it.setChanged()
            }
        }
    }
}