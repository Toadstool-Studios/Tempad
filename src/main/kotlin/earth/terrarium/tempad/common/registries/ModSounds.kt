package earth.terrarium.tempad.common.registries

import com.teamresourceful.resourcefullib.common.registry.ResourcefulRegistries
import earth.terrarium.tempad.Tempad
import earth.terrarium.tempad.tempadId
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.sounds.SoundEvent
import com.teamresourceful.resourcefullibkt.common.getValue

object ModSounds {
    val registry = ResourcefulRegistries.create(BuiltInRegistries.SOUND_EVENT, Tempad.MOD_ID)

    val timedoorOpen by registry.register("entity.timedoor.open") {
        SoundEvent.createVariableRangeEvent("entity.timedoor.open".tempadId)
    }
}