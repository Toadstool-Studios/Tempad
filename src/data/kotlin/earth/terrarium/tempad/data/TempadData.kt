package earth.terrarium.tempad.data

import earth.terrarium.tempad.Tempad
import earth.terrarium.tempad.data.client.ModItemModelData
import net.neoforged.bus.api.IEventBus
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.Mod
import net.neoforged.neoforge.data.event.GatherDataEvent

@Mod(Tempad.MOD_ID)
class TempadData(bus: IEventBus) {
    init {
        bus.addListener(::gatherData)
    }

    @SubscribeEvent
    fun gatherData(event: GatherDataEvent) {
        val generator = event.generator
        val output = generator.packOutput
        val existingFileHelper = event.existingFileHelper
        val lookupProvider = event.lookupProvider

        generator.addProvider(event.includeClient(), ModItemModelData(output, existingFileHelper))
    }
}