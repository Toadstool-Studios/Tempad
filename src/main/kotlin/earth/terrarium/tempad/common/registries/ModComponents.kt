package earth.terrarium.tempad.common.registries

import com.mojang.serialization.Codec
import com.teamresourceful.bytecodecs.base.ByteCodec
import com.teamresourceful.resourcefullib.common.bytecodecs.ExtraByteCodecs
import com.teamresourceful.resourcefullib.common.registry.ResourcefulRegistries
import com.teamresourceful.resourcefullib.common.registry.ResourcefulRegistry
import com.teamresourceful.resourcefullibkt.common.getValue
import earth.terrarium.tempad.Tempad
import earth.terrarium.tempad.api.locations.StaticNamedGlobalPos
import earth.terrarium.tempad.common.data.OrganizationMethod
import earth.terrarium.tempad.common.utils.*
import net.minecraft.core.component.DataComponentType
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.neoforged.neoforge.common.MutableDataComponentHolder

object ModComponents {
    val registry: ResourcefulRegistry<DataComponentType<*>> =
        ResourcefulRegistries.create(BuiltInRegistries.DATA_COMPONENT_TYPE, Tempad.MOD_ID)

    val defaultApp: DataComponentType<ResourceLocation> by registry.register("default_app") {
        componentType {
            serialize = ResourceLocation.CODEC
            networkSerialize = ExtraByteCodecs.RESOURCE_LOCATION
        }
    }

    val defaultMacro: DataComponentType<ResourceLocation> by registry.register("default_macro") {
        componentType {
            serialize = ResourceLocation.CODEC
            networkSerialize = ExtraByteCodecs.RESOURCE_LOCATION
        }
    }

    val organizationMethod: DataComponentType<OrganizationMethod> by registry.register("organization_method") {
        componentType {
            serialize = OrganizationMethod.CODEC
            networkSerialize = OrganizationMethod.BYTE_CODEC
        }
    }

    val chrononContent: DataComponentType<Int> by registry.register("chronon_content") {
        componentType {
            serialize = Codec.INT
            networkSerialize = ByteCodec.INT
        }
    }

    val chrononLimit: DataComponentType<Int> by registry.register("chronon_limit") {
        componentType {
            serialize = Codec.INT
            networkSerialize = ByteCodec.INT
        }
    }

    val staticLocation: DataComponentType<StaticNamedGlobalPos> by registry.register("static_location") {
        componentType {
            serialize = StaticNamedGlobalPos.CODEC
            networkSerialize = StaticNamedGlobalPos.BYTE_CODEC
        }
    }

    val enabled: DataComponentType<Boolean> by registry.register("enabled") {
        componentType {
            serialize = Codec.BOOL
            networkSerialize = ByteCodec.BOOLEAN
        }
    }

    val twisterEquipped: DataComponentType<Boolean> by registry.register("twister_equipped") {
        componentType {
            serialize = Codec.BOOL
            networkSerialize = ByteCodec.BOOLEAN
        }
    }
}

var MutableDataComponentHolder.defaultApp by ModComponents.defaultApp.withDefault(ModApps.teleport)

var MutableDataComponentHolder.defaultMacro by ModComponents.defaultMacro.withDefault(ModMacros.teleportToPinned)

var MutableDataComponentHolder.organizationMethod by ModComponents.organizationMethod.withDefault(OrganizationMethod.DIMENSION)

var MutableDataComponentHolder.chrononContent by ModComponents.chrononContent.withDefault(0)

var MutableDataComponentHolder.chrononLimit by ModComponents.chrononLimit.withDefault(0)

var MutableDataComponentHolder.staticLocation by ModComponents.staticLocation

var MutableDataComponentHolder.enabled by ModComponents.enabled.withDefault(true)

var MutableDataComponentHolder.twisterEquipped by ModComponents.twisterEquipped.withDefault(false)