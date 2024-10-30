package earth.terrarium.tempad.common.registries

import com.teamresourceful.resourcefullib.common.registry.ResourcefulRegistries
import com.teamresourceful.resourcefullib.common.registry.ResourcefulRegistry
import com.teamresourceful.resourcefullibkt.common.getValue
import earth.terrarium.tempad.Tempad
import earth.terrarium.tempad.common.items.*
import earth.terrarium.tempad.common.utils.creativeModeTab
import earth.terrarium.tempad.common.utils.stack
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.Item

object ModItems {
    val registry: ResourcefulRegistry<Item> = ResourcefulRegistries.create(BuiltInRegistries.ITEM, Tempad.MOD_ID)
    val creativeTabs: ResourcefulRegistry<CreativeModeTab> = ResourcefulRegistries.create(BuiltInRegistries.CREATIVE_MODE_TAB, Tempad.MOD_ID)

    val tab: CreativeModeTab by creativeTabs.register("main") {
        creativeModeTab {
            title(Component.translatable("category.tempad"))
            icon {
                tempad.stack {
                    twisterEquipped = true
                }
            }
        }
    }

    val tempad: TempadItem by registry.register("tempad") { TempadItem() }
    val chronometer: ChronometerItem by registry.register("chronometer") { ChronometerItem() }
    val statusEmitter: Item by registry.register("status_emitter") { StatusEmitterItem() }
    val timeTwister: Item by registry.register("time_twister") { TimeTwisterItem() }
    val sacredChronometer: Item by registry.register("sacred_chronometer") { SacredChronometerItem() }
    val rudimentaryTempad: Item by registry.register("rudimentary_tempad") { RudimentaryTempadItem() }
    val spatialAnchor: Item by registry.register("spatial_anchor") { SpatialAnchorItem() }
    val workstation: Item by registry.register("workstation") { BlockItem(ModBlocks.workstation, Item.Properties()) }

    val locationCard: Item by registry.register("location_card") { LocationCardItem() }
    val inexorableAlloy: Item by registry.register("inexorable_alloy") { Item(Item.Properties()) }
    val newLocationUpgrade: Item by registry.register("new_location_upgrade") { Item(Item.Properties()) }
    val playerTeleportUpgrade: Item by registry.register("player_teleport_upgrade") { Item(Item.Properties()) }
}