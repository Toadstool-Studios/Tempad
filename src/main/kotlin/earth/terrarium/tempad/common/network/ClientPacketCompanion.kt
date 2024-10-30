package earth.terrarium.tempad.common.network

import com.teamresourceful.bytecodecs.base.ByteCodec
import com.teamresourceful.resourcefullib.common.network.Packet
import com.teamresourceful.resourcefullib.common.network.base.ClientboundPacketType
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.resources.ResourceLocation

interface ClientPacketCompanion<T: Packet<T>>: ClientboundPacketType<T> {
    val id: ResourceLocation
    val byteCodec: ByteCodec<T>
    fun onReceive(packet: T)

    override fun id(): ResourceLocation? {
        return id
    }

    override fun encode(p0: T?, p1: RegistryFriendlyByteBuf?) {
        byteCodec.encode(p0, p1)
    }

    override fun decode(p0: RegistryFriendlyByteBuf?): T? {
        return byteCodec.decode(p0)
    }

    override fun handle(p0: T?): Runnable? {
        return Runnable { p0?.let { onReceive(it) } }
    }
}