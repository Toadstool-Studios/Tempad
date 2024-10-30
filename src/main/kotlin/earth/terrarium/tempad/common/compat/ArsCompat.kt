package earth.terrarium.tempad.common.compat

import com.hollingsworth.arsnouveau.common.light.LightManager
import earth.terrarium.tempad.common.entity.TimedoorEntity
import earth.terrarium.tempad.common.registries.ModEntities
import net.minecraft.util.Mth

object ArsCompat {
    fun init() {
        LightManager.register(ModEntities.TIMEDOOR_ENTITY) {
            if (it.tickCount < TimedoorEntity.IDLE_BEFORE_START) {
                return@register Mth.lerpInt(it.tickCount / TimedoorEntity.IDLE_BEFORE_START.toFloat(), 0, 5)
            }
            5
        }
    }
}