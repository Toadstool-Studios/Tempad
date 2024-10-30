package earth.terrarium.tempad.common.compat

import com.mojang.authlib.GameProfile
import earth.terrarium.argonauts.api.teams.guild.GuildApi
import earth.terrarium.argonauts.api.teams.party.PartyApi
import earth.terrarium.tempad.api.visibility.AnchorAccess
import earth.terrarium.tempad.api.visibility.AnchorAccessApi
import earth.terrarium.tempad.tempadId
import net.minecraft.world.level.Level
import kotlin.jvm.optionals.getOrNull

enum class ArgonautsAccess: AnchorAccess {
    Guild {
        override fun canAccess(level: Level, owner: GameProfile, accessor: GameProfile): Boolean {
            return GuildApi.API.getPlayerGuild(level, owner.id).getOrNull()?.let {
                it.isAllied(accessor.id) || it.isMember(accessor.id)
            } == true
        }
    },
    Party {
        override fun canAccess(level: Level, owner: GameProfile, accessor: GameProfile): Boolean {
            return PartyApi.API.getPlayerParty(owner.id).getOrNull()?.isMember(accessor.id) == true
        }
    },
    GuildOrParty {
        override fun canAccess(level: Level, owner: GameProfile, accessor: GameProfile): Boolean {
            return Guild.canAccess(level, owner, accessor) || Party.canAccess(level, owner, accessor)
        }
    }
}

fun initArgonautsAccess() {
    AnchorAccessApi["guild".tempadId] = ArgonautsAccess.Guild
    AnchorAccessApi["party".tempadId] = ArgonautsAccess.Party
    AnchorAccessApi["guild_or_party".tempadId] = ArgonautsAccess.GuildOrParty
}