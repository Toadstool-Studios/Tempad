package earth.terrarium.tempad.api.visibility

import com.mojang.authlib.GameProfile
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.Level

interface AnchorAccess {
    fun canAccess(level: Level, owner: GameProfile, accessor: GameProfile): Boolean
}

object AnchorAccessApi {
    val visbility: Map<ResourceLocation, AnchorAccess>
        field = mutableMapOf()

    @JvmStatic
    @JvmName("register")
    operator fun set(id: ResourceLocation, visibility: AnchorAccess) {
        visbility[id] = visibility
    }

    @JvmStatic
    operator fun get(id: ResourceLocation): AnchorAccess = visbility[id] ?: DefaultAccess.Private
}