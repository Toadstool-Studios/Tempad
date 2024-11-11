package earth.terrarium.tempad.data.client

import earth.terrarium.tempad.Tempad
import earth.terrarium.tempad.tempadId
import earth.terrarium.tempad.common.registries.ModItems
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.data.PackOutput
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.Item
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder
import net.neoforged.neoforge.client.model.generators.ItemModelProvider
import net.neoforged.neoforge.client.model.generators.ModelFile
import net.neoforged.neoforge.common.data.ExistingFileHelper

class ModItemModelData(output: PackOutput, fileHelper: ExistingFileHelper) : ItemModelProvider(output, Tempad.MOD_ID, fileHelper) {
    override fun registerModels() {
        getBuilder("tempad").apply {
            parent(ModelFile.UncheckedModelFile("item/generated"))
            texture("layer0", "tempad:item/tempad/base")
            for (attached in arrayOf(0f, 1f)) {
                for (inUse in arrayOf(0f, 1f)) {
                    for ((index, charge) in arrayOf(0f, 0.33f, 0.66f, 1f).withIndex()) {
                        var path = "tempad:item/tempad/"
                        path += (if (attached == 1f) "attached" else "base")
                        path += "_"
                        path += (if (inUse == 1f) "in_use" else "idle")
                        path += "_$index"

                        override().apply {
                            predicate("attached".tempadId, attached)
                            predicate("in_use".tempadId, inUse)
                            predicate("charge".tempadId, charge)

                            model(getBuilder(path).apply {
                                parent(ModelFile.UncheckedModelFile("item/generated"))

                                texture("layer0", "item/tempad/base${if (attached == 1f) "_with_twister" else ""}".tempadId)
                                if (inUse == 1f) {
                                    texture("layer1", "item/tempad/screen_on".tempadId)
                                }
                                if (charge > 0) {
                                    texture("layer2", "item/tempad/charge_${index}")
                                }
                            })
                        }
                    }
                }
            }
        }

        basicItem(ModItems.chronometer).apply {
            for ((index, charge) in arrayOf(0.33f, 0.66f, 1f).withIndex()) {
                override().apply {
                    predicate("charge".tempadId, charge)

                    model(getBuilder("tempad:chronometer_${index + 1}").apply {
                        parent(ModelFile.UncheckedModelFile("item/generated"))

                        texture("layer0", "tempad:item/chronometer")
                        texture("layer1", "tempad:item/chronometer/charge_${index + 1}")
                    })
                }
            }
        }

        enablableItem(ModItems.statusEmitter)

        basicItem(ModItems.timeTwister)
        basicItem(ModItems.newLocationUpgrade)
        basicItem(ModItems.playerTeleportUpgrade)
    }

    fun enablableItem(item: Item): ItemModelBuilder {
        val itemId = BuiltInRegistries.ITEM.getKey(item)
        return getBuilder(itemId.toString())
            .parent(ModelFile.UncheckedModelFile("item/generated"))
            .texture("layer0", ResourceLocation.fromNamespaceAndPath(itemId.namespace, "item/" + itemId.path))
            .override()
            .predicate("enabled".tempadId, 1f)
            .model(getBuilder(itemId.toString() + "_on").apply {
                parent(ModelFile.UncheckedModelFile("item/generated"))
                texture("layer0", ResourceLocation.fromNamespaceAndPath(itemId.namespace, "item/${itemId.path}_on"))
            })
            .end()
    }
}