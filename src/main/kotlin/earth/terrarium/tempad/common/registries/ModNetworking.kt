package earth.terrarium.tempad.common.registries

import com.teamresourceful.resourcefullib.common.network.Network
import earth.terrarium.tempad.tempadId
import earth.terrarium.tempad.common.network.c2s.*
import earth.terrarium.tempad.common.network.s2c.*

object ModNetworking {
    val channel: Network = Network("main".tempadId, 0, false)

    fun init() {
        channel.register(BackTrackLocation.type)
        channel.register(CreateLocationPacket.type)
        channel.register(DeleteLocationPacket.type)
        channel.register(OpenAppPacket.type)
        channel.register(OpenFavoritePacket.type)
        channel.register(OpenTimedoorPacket.type)
        channel.register(SaveSettingsPacket.type)
        channel.register(SetFavoritePacket.type)
        channel.register(WriteToCardPacket.type)
        channel.register(UpdateAnchorPacket)

        channel.register(RotatePlayerMomentumPacket.type)
        channel.register(OpenTimeTwister.type)
        channel.register(OpenSpatialAnchor)
    }
}