package earth.terrarium.tempad.common.registries

import earth.terrarium.tempad.api.locations.*
import earth.terrarium.tempad.api.visibility.DefaultAccess
import earth.terrarium.tempad.api.visibility.AnchorAccessApi
import earth.terrarium.tempad.common.compat.initArgonautsAccess
import earth.terrarium.tempad.common.compat.initFTBTeamsAccess
import earth.terrarium.tempad.common.location_handlers.DefaultLocationHandler
import earth.terrarium.tempad.common.location_handlers.PlayerHandler
import earth.terrarium.tempad.common.location_handlers.AnchorPointsHandler
import earth.terrarium.tempad.tempadId
import net.neoforged.fml.ModList

object ModLocations {
    fun init() {
        TempadLocations[DefaultLocationHandler.ID] = { player, _, _ -> DefaultLocationHandler(player) }
        TempadLocations["spatial_anchors".tempadId] = { player, _, _ -> AnchorPointsHandler(player) }
        TempadLocations[PlayerHandler.ID] = { player, upgrades, _ -> PlayerHandler(player, upgrades) }

        AnchorAccessApi["private".tempadId] = DefaultAccess.Private
        AnchorAccessApi["public".tempadId] = DefaultAccess.Public
        if(ModList.get().isLoaded("argonauts")) initArgonautsAccess()
        if(ModList.get().isLoaded("ftbteams")) initFTBTeamsAccess()
    }
}