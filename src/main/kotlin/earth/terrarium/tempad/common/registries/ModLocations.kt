package earth.terrarium.tempad.common.registries

import earth.terrarium.tempad.api.locations.*
import earth.terrarium.tempad.api.visibility.DefaultVisbility
import earth.terrarium.tempad.api.visibility.VisbilityApi
import earth.terrarium.tempad.common.location_handlers.DefaultLocationHandler
import earth.terrarium.tempad.common.location_handlers.PlayerHandler
import earth.terrarium.tempad.common.location_handlers.AnchorPointsHandler
import earth.terrarium.tempad.tempadId

object ModLocations {
    fun init() {
        TempadLocations[DefaultLocationHandler.ID] = { player, _ -> DefaultLocationHandler(player) }
        TempadLocations["spatial_anchors".tempadId] = { player, _ -> AnchorPointsHandler(player) }
        TempadLocations[PlayerHandler.ID] = ::PlayerHandler

        VisbilityApi["private".tempadId] = DefaultVisbility.PRIVATE
        VisbilityApi["public".tempadId] = DefaultVisbility.PUBLIC
    }
}