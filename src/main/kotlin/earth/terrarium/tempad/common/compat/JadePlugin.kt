package earth.terrarium.tempad.common.compat

import earth.terrarium.tempad.common.entity.TimedoorEntity
import earth.terrarium.tempad.tempadId
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import snownee.jade.api.*
import snownee.jade.api.config.IPluginConfig

@WailaPlugin
class JadePlugin: IWailaPlugin {
    override fun registerClient(registration: IWailaClientRegistration) {
        registration.registerEntityComponent(TimedoorComponentProvider, TimedoorEntity::class.java)
    }
}

object TimedoorComponentProvider: IEntityComponentProvider {
    override fun getUid(): ResourceLocation = "timedoor".tempadId

    override fun appendTooltip(tooltip: ITooltip, accessor: EntityAccessor, config: IPluginConfig) {
        val timedoorEntity = accessor.entity as? TimedoorEntity ?: return
        if (timedoorEntity.closingTime > 0 && timedoorEntity.closingTime > timedoorEntity.tickCount) {
            tooltip.add(Component.translatable("jade.tempad.will_close", Math.round((timedoorEntity.closingTime - timedoorEntity.tickCount) / 20f)))
        } else if (timedoorEntity.closingTime != -1) {
            tooltip.add(Component.translatable("jade.tempad.closing"))
        }
        tooltip.add(Component.translatable("jade.tempad.pos", Component.translatable(timedoorEntity.targetDimension.location().toLanguageKey("dimension")), "${timedoorEntity.targetPos.x().toInt()}, ${timedoorEntity.targetPos.y().toInt()}, ${timedoorEntity.targetPos.z().toInt()}"))
    }
}