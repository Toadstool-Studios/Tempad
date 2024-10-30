package earth.terrarium.tempad.api.visibility

import com.mojang.authlib.GameProfile
import net.minecraft.world.level.Level

enum class DefaultAccess: AnchorAccess {
    Public {
        override fun canAccess(level: Level, owner: GameProfile, accessor: GameProfile): Boolean = true
    },
    Private {
        override fun canAccess(level: Level, owner: GameProfile, accessor: GameProfile): Boolean = owner == accessor
    }
}