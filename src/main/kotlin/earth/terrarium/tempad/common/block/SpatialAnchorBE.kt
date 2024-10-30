package earth.terrarium.tempad.common.block

import com.mojang.authlib.GameProfile
import earth.terrarium.tempad.api.locations.NamedGlobalVec3
import earth.terrarium.tempad.api.visibility.AnchorAccessApi
import earth.terrarium.tempad.common.registries.ModAttachments
import earth.terrarium.tempad.common.registries.ModBlocks
import earth.terrarium.tempad.common.registries.accessId
import earth.terrarium.tempad.common.registries.color
import earth.terrarium.tempad.common.registries.owner
import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3
import kotlin.jvm.optionals.getOrNull

class SpatialAnchorBE(pos: BlockPos, state: BlockState): BlockEntity(ModBlocks.spatialAnchorBE, pos, state) {
    var name: Component get() = getExistingData(ModAttachments.name).getOrNull() ?: ModBlocks.spatialAnchor.name
        set(value) {
            setData(ModAttachments.name, value)
        }

    val namedGlobalVec3 get() = NamedGlobalVec3(name, Vec3.atCenterOf(worldPosition), level!!.dimension(), 0f, color)

    fun canAccess(player: GameProfile): Boolean {
        return AnchorAccessApi[this.accessId].canAccess(level!!, owner!!, player)
    }

    override fun getUpdateTag(registries: HolderLookup.Provider): CompoundTag {
        return CompoundTag().apply { saveAdditional(this, registries) }
    }
}