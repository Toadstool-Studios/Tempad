package earth.terrarium.tempad.common.config

import earth.terrarium.tempad.common.registries.ModNetworking

object CommonConfigCache {
    val CACHE = ConfigCache("tempad", ModNetworking.channel)

    val expPerCharge by CACHE.ofInt(CommonConfig::expPerCharge)
    val energyPerCharge by CACHE.ofInt(CommonConfig::energyPerCharge)

    object Tempad {
        val fuelType by CACHE.ofString(CommonConfig.Tempad::fuelType)
        val capacity by CACHE.ofInt(CommonConfig.Tempad::capacity)
        val cooldownTime by CACHE.ofInt(CommonConfig.Tempad::cooldownTime)
    }

    object AdvancedTempad {
        val fuelType by CACHE.ofString(CommonConfig.AdvancedTempad::fuelType)
        val capacity by CACHE.ofInt(CommonConfig.AdvancedTempad::capacity)
        val cooldownTime by CACHE.ofInt(CommonConfig.AdvancedTempad::cooldownTime)
    }
}