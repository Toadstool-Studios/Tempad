package earth.terrarium.tempad.common.network.s2c

import com.teamresourceful.bytecodecs.base.ByteCodec
import com.teamresourceful.bytecodecs.base.`object`.ObjectByteCodec
import com.teamresourceful.resourcefullib.common.network.Packet
import com.teamresourceful.resourcefullib.common.network.base.NetworkHandle
import com.teamresourceful.resourcefullib.common.network.base.PacketType
import com.teamresourceful.resourcefullib.common.network.defaults.CodecPacketType
import earth.terrarium.tempad.api.context.ContextHolder
import earth.terrarium.tempad.client.screen.tempad.TimeTwisterScreen
import earth.terrarium.tempad.common.data.HistoricalLocation
import earth.terrarium.tempad.common.utils.DATE_BYTE_CODEC
import earth.terrarium.tempad.tempadId
import net.minecraft.client.Minecraft
import java.util.Date

class OpenTimeTwister(val history: Map<Date, HistoricalLocation>, val ctx: ContextHolder<*>): Packet<OpenTimeTwister> {
    companion object {
        val type = CodecPacketType.Client.create(
            "open_time_twister".tempadId,
            ObjectByteCodec.create(
                ByteCodec.mapOf(DATE_BYTE_CODEC, HistoricalLocation.BYTE_CODEC).fieldOf(OpenTimeTwister::history),
                ContextHolder.codec.fieldOf(OpenTimeTwister::ctx),
                ::OpenTimeTwister
            ),
            NetworkHandle.handle { message ->
                Minecraft.getInstance().setScreen(TimeTwisterScreen(message.history, message.ctx))
            }
        )
    }

    override fun type(): PacketType<OpenTimeTwister> = type
}