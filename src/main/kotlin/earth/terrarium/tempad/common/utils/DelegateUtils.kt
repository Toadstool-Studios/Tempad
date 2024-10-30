package earth.terrarium.tempad.common.utils

import earth.terrarium.common_storage_lib.data.network.BlockEntitySyncPacket
import earth.terrarium.common_storage_lib.data.network.EntitySyncPacket
import earth.terrarium.common_storage_lib.data.sync.DataSyncSerializer
import earth.terrarium.tempad.Tempad
import net.minecraft.core.component.DataComponentType
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializer
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.Container
import net.minecraft.world.entity.Entity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.block.entity.BlockEntity
import net.neoforged.neoforge.attachment.AttachmentHolder
import net.neoforged.neoforge.attachment.AttachmentType
import net.neoforged.neoforge.common.MutableDataComponentHolder
import net.neoforged.neoforge.network.PacketDistributor
import java.util.*
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

operator fun <T : Any> AttachmentHolder.get(key: AttachmentType<T>): T = this.getData(key)
operator fun <T : Any> AttachmentHolder.set(key: AttachmentType<T>, value: T) = this.setData(key, value)
operator fun <T : Any> AttachmentHolder.minusAssign(key: AttachmentType<T>) {
    this.removeData(key)
}

operator fun <T : Any> AttachmentType<T>.getValue(thisRef: AttachmentHolder, property: KProperty<*>): T = thisRef[this]
operator fun <T : Any> AttachmentType<T>.setValue(thisRef: AttachmentHolder, property: KProperty<*>, value: T) {
    thisRef[this] = value
}

class OptionalAttachmentDelegate<T : Any>(private val key: AttachmentType<T>) :
    ReadWriteProperty<AttachmentHolder, T?> {
    override operator fun getValue(thisRef: AttachmentHolder, property: KProperty<*>): T? =
        thisRef.getExistingData(key).orElse(null)

    override operator fun setValue(thisRef: AttachmentHolder, property: KProperty<*>, value: T?) {
        if (value == null) {
            thisRef -= key
        } else {
            thisRef[key] = value
        }
    }
}

class SyncedOptionalAttachmentDelegate<T : Any>(private val key: AttachmentType<T>, private val sync: DataSyncSerializer<T>) {
    operator fun getValue(thisRef: AttachmentHolder, property: KProperty<*>): T? =
        thisRef.getExistingData(key).orElse(null)

    operator fun setValue(thisRef: AttachmentHolder, property: KProperty<*>, value: T?) {
        if (value == null) {
            thisRef -= key
        } else {
            thisRef[key] = value
            // Sync the data here
        }
        syncData(thisRef, sync, value)
    }
}

class SyncedAttachmentDelegate<T : Any>(private val key: AttachmentType<T>, private val sync: DataSyncSerializer<T>) :
    ReadWriteProperty<AttachmentHolder, T> {
    override operator fun getValue(thisRef: AttachmentHolder, property: KProperty<*>): T =
        thisRef.getData(key)

    override operator fun setValue(thisRef: AttachmentHolder, property: KProperty<*>, value: T) {
        thisRef[key] = value
        syncData(thisRef, sync, value)
    }
}



private fun <T : Any> syncData(thisRef: AttachmentHolder, sync: DataSyncSerializer<T>, value: T?) {
    if (thisRef is Entity && !thisRef.level().isClientSide) {
        PacketDistributor.sendToPlayersTrackingEntity(thisRef, EntitySyncPacket.of(thisRef, sync, value))
    }

    if (thisRef is BlockEntity && !thisRef.level!!.isClientSide) {
        PacketDistributor.sendToPlayersTrackingChunk(
            thisRef.level as ServerLevel, ChunkPos(thisRef.blockPos),
            BlockEntitySyncPacket.of(thisRef, sync, value)
        )
    }
}

fun <T : Any> AttachmentType<T>.optional() = OptionalAttachmentDelegate(this)
fun <T : Any> AttachmentType<T>.syncedOptional(sync: DataSyncSerializer<T>) = SyncedOptionalAttachmentDelegate(this, sync)
fun <T : Any> AttachmentType<T>.synced(sync: DataSyncSerializer<T>) = SyncedAttachmentDelegate(this, sync)

val <T : Any> AttachmentType<T>.serverData get() = ServerDataDelegate(this)
val <T : Any> AttachmentType<T>.optionalServerData get() = OptionalServerDataDelegate(this)

class ServerDataDelegate<T : Any>(private val key: AttachmentType<T>) : ReadWriteProperty<Any?, T> {
    override operator fun getValue(thisRef: Any?, property: KProperty<*>): T = Tempad.server?.overworld()?.getData(key)!!
    override operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        Tempad.server?.overworld()?.setData(key, value)
    }
}

class OptionalServerDataDelegate<T : Any>(private val key: AttachmentType<T>) : ReadWriteProperty<Any?, T?> {
    override operator fun getValue(thisRef: Any?, property: KProperty<*>): T? =
        Tempad.server?.overworld()?.getExistingData(key)?.orElse(null)

    override operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T?) {
        if (value == null) {
            Tempad.server?.overworld()?.removeData(key)
        } else {
            Tempad.server?.overworld()?.setData(key, value)
        }
    }
}

class ComponentDelegate<T : Any>(private val key: DataComponentType<T>, private val default: T) {
    operator fun getValue(thisRef: MutableDataComponentHolder, property: KProperty<*>): T = thisRef[key] ?: default
    operator fun setValue(thisRef: MutableDataComponentHolder, property: KProperty<*>, value: T) {
        thisRef[key] = value
    }
}

operator fun <T : Any> DataComponentType<T>.getValue(thisRef: MutableDataComponentHolder, property: KProperty<*>): T? =
    thisRef[this]

operator fun <T : Any> DataComponentType<T>.setValue(
    thisRef: MutableDataComponentHolder,
    property: KProperty<*>,
    value: T?,
) {
    if (value != null) {
        thisRef[this] = value
    } else {
        thisRef.remove(this)
    }
}

fun <T : Any> DataComponentType<T>.withDefault(default: T) = ComponentDelegate(this, default)

inline fun <reified T : Entity, U> createDataKey(serializer: EntityDataSerializer<U>): EntityDataAccessor<U> =
    SynchedEntityData.defineId(T::class.java, serializer)

class DataDelegate<T : Any>(private val key: EntityDataAccessor<T>) : ReadWriteProperty<Entity, T> {
    override operator fun getValue(thisRef: Entity, property: KProperty<*>): T = thisRef.entityData[key]
    override operator fun setValue(thisRef: Entity, property: KProperty<*>, value: T) =
        thisRef.entityData.set(key, value)
}

operator fun <T : Any> EntityDataAccessor<T>.getValue(thisRef: Entity, property: KProperty<*>): T =
    thisRef.entityData[this]

operator fun <T : Any> EntityDataAccessor<T>.setValue(thisRef: Entity, property: KProperty<*>, value: T) {
    thisRef.entityData[this] = value
}

class OptionalDataDelegate<T : Any>(private val key: EntityDataAccessor<Optional<T>>) : ReadWriteProperty<Entity, T?> {
    override operator fun getValue(thisRef: Entity, property: KProperty<*>): T? = thisRef.entityData[key].orElse(null)
    override operator fun setValue(thisRef: Entity, property: KProperty<*>, value: T?) =
        thisRef.entityData.set(key, Optional.ofNullable(value))
}

class ContainerDelegate(private val container: Container, private val slot: Int) : ReadWriteProperty<Any?, ItemStack> {
    override operator fun getValue(thisRef: Any?, property: KProperty<*>): ItemStack = container.getItem(slot)
    override operator fun setValue(thisRef: Any?, property: KProperty<*>, value: ItemStack) =
        container.setItem(slot, value)
}

fun Container.slot(slot: Int) = ContainerDelegate(this, slot)