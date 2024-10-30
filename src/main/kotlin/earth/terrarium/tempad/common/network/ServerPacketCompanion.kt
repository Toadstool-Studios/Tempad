package earth.terrarium.tempad.common.network

import com.teamresourceful.bytecodecs.base.ByteCodec
import com.teamresourceful.resourcefullib.common.network.Packet
import com.teamresourceful.resourcefullib.common.network.base.ServerboundPacketType
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Player
import java.util.function.Consumer

interface ServerPacketCompanion<T: Packet<T>>: ServerboundPacketType<T> {
    val id: ResourceLocation
    val byteCodec: ByteCodec<T>
    fun onReceive(packet: T, player: Player)

    override fun id(): ResourceLocation? {
        return id
    }

    override fun encode(p0: T?, p1: RegistryFriendlyByteBuf?) {
        byteCodec.encode(p0, p1)
    }

    override fun decode(p0: RegistryFriendlyByteBuf?): T? {
        return byteCodec.decode(p0)
    }

    override fun handle(p0: T?): Consumer<Player> {
        return Consumer { player -> p0?.let { onReceive(it, player) } }
    }
}