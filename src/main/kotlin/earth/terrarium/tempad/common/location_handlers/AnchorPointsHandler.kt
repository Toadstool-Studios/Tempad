package earth.terrarium.tempad.common.location_handlers

import com.mojang.authlib.GameProfile
import com.mojang.serialization.Codec
import earth.terrarium.tempad.Tempad
import earth.terrarium.tempad.api.locations.LocationHandler
import earth.terrarium.tempad.api.locations.NamedGlobalVec3
import earth.terrarium.tempad.common.block.SpatialAnchorBE
import earth.terrarium.tempad.common.registries.anchorPoints
import earth.terrarium.tempad.common.registries.id
import earth.terrarium.tempad.common.utils.get
import earth.terrarium.tempad.common.utils.safeLet
import net.minecraft.core.GlobalPos
import net.minecraft.core.UUIDUtil
import java.util.*

class AnchorPointsData(anchors: Map<UUID, GlobalPos>) {
    companion object {
        val codec = Codec.unboundedMap(UUIDUtil.STRING_CODEC, GlobalPos.CODEC).xmap<AnchorPointsData>(::AnchorPointsData, AnchorPointsData::anchors)
    }

    val anchors = mutableMapOf<UUID, GlobalPos>()

    init {
        this.anchors.putAll(anchors)
    }

    fun getPostions(accessor: GameProfile): Map<UUID, NamedGlobalVec3> {
        return anchors.mapValues { getBlockEntity(it.value) }.filterValues { it != null && it.canAccess(accessor) }.mapValues { it.value!!.namedGlobalVec3 }
    }

    operator fun plusAssign(block: SpatialAnchorBE) {
        if (block.id == null) block.id = UUID.randomUUID()
        safeLet(block.id, block.level) { id, level ->
            anchors[id] = GlobalPos.of(level.dimension(), block.blockPos)
        }
    }

    operator fun minusAssign(locationId: UUID) {
        anchors.remove(locationId)
    }
}

fun getBlockEntity(pos: GlobalPos): SpatialAnchorBE? {
    val level = Tempad.server?.get(pos.dimension())
    return level?.getBlockEntity(pos.pos)?.let { it as? SpatialAnchorBE }
}

class AnchorPointsHandler(val gameProfile: GameProfile): LocationHandler {
    override val locations: Map<UUID, NamedGlobalVec3> get() = anchorPoints.getPostions(gameProfile)
    override fun minusAssign(locationId: UUID) {}
}