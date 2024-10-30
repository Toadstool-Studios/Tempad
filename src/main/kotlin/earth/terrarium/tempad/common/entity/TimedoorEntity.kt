package earth.terrarium.tempad.common.entity

import com.mojang.authlib.GameProfile
import com.mojang.datafixers.util.Either
import com.teamresourceful.resourcefullib.common.color.Color
import earth.terrarium.tempad.Tempad
import earth.terrarium.tempad.api.ActionType
import earth.terrarium.tempad.api.context.SyncableContext
import earth.terrarium.tempad.api.context.drain
import earth.terrarium.tempad.api.event.TimedoorEvent
import earth.terrarium.tempad.api.locations.NamedGlobalVec3
import earth.terrarium.tempad.api.locations.offsetLocation
import earth.terrarium.tempad.api.sizing.DefaultSizing
import earth.terrarium.tempad.api.sizing.DoorType
import earth.terrarium.tempad.api.sizing.TimedoorSizing
import earth.terrarium.tempad.api.tva_device.chronons
import earth.terrarium.tempad.common.config.CommonConfig
import earth.terrarium.tempad.common.network.s2c.RotatePlayerMomentumPacket
import earth.terrarium.tempad.common.registries.ModEntities
import earth.terrarium.tempad.common.registries.ModSounds
import earth.terrarium.tempad.common.registries.ModTags
import earth.terrarium.tempad.common.registries.ageUntilAllowedThroughTimedoor
import earth.terrarium.tempad.common.registries.chrononContent
import earth.terrarium.tempad.common.utils.*
import net.minecraft.core.particles.DustParticleOptions
import net.minecraft.core.particles.ParticleOptions
import net.minecraft.core.particles.ParticleType
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.core.registries.Registries
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.resources.ResourceKey
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.*
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.portal.DimensionTransition
import net.minecraft.world.phys.Vec3
import java.util.*
import kotlin.jvm.optionals.getOrNull

class TimedoorEntity(type: EntityType<*>, level: Level) : Entity(type, level) {
    companion object {
        internal const val IDLE_BEFORE_START = 26
        internal const val ANIMATION_LENGTH = 5
        private val CLOSING_TIME = createDataKey<TimedoorEntity, Int>(EntityDataSerializers.INT)
        private val COLOR = createDataKey<TimedoorEntity, Color>(ModEntities.colorSerializer)
        private val TARGET_POS = createDataKey<TimedoorEntity, Vec3>(ModEntities.vec3Serializer)
        private val TARGET_DIMENSION =
            createDataKey<TimedoorEntity, ResourceKey<Level>>(ModEntities.dimensionKeySerializer)
        private val SIZING = createDataKey<TimedoorEntity, TimedoorSizing>(ModEntities.sizingSerializer)
        private val GLITCHING = createDataKey<TimedoorEntity, Boolean>(EntityDataSerializers.BOOLEAN)

        //feedback
        val fail = Component.translatable("entity.tempad.timedoor.fail")
        val posFail = Component.translatable("entity.tempad.timedoor.fail.pos")
        val interDimFail = Component.translatable("entity.tempad.timedoor.fail.interdimensional")
        val intraDimAllFail = Component.translatable("entity.tempad.timedoor.fail.interdimensional_all")
        val intraDimFail = Component.translatable("entity.tempad.timedoor.fail.interdimensional")
        val enteringFail = Component.translatable("entity.tempad.timedoor.fail.entering")
        val leavingFail = Component.translatable("entity.tempad.timedoor.fail.leaving")
        val noChrononsFail = Component.translatable("entity.tempad.timedoor.fail.no_chronons")

        fun openTimedoor(
            player: Player,
            ctx: SyncableContext<*>,
            location: NamedGlobalVec3,
            onOpen: (TimedoorEntity) -> Unit = {},
        ): Component? {
            val stack = ctx.stack
            if (!player.isCreative && stack.chrononContent < 1000) return noChrononsFail

            val result = getTimedoor(player.level(), location)
            result.right().getOrNull()?.let { return it }

            val timedoor = result.left().getOrNull() ?: return fail

            timedoor.owner = player.uuid
            timedoor.sizing = if (player.xRot > 45) DefaultSizing.FLOOR else DefaultSizing.DEFAULT
            timedoor.sizing.placeTimedoor(DoorType.ENTRY, player.position(), player.yRot, timedoor)

            val event = TimedoorEvent.OpenWithItem(timedoor, player, ctx).post()
            if (event.isCanceled) return event.errorMessage ?: fail
            else logTimedoorOpen(player.name.string, location, timedoor)

            if (!player.isCreative) {
                ctx.drain(1000)
                player.cooldowns.addCooldown(stack.item, 40)
            }

            player.level().addFreshEntity(timedoor)
            onOpen(timedoor)
            timedoor.tryInitReceivingPortal()
            return null
        }

        fun openTimedoor(
            player: GameProfile,
            block: BlockEntity,
            location: NamedGlobalVec3,
            onOpen: (TimedoorEntity) -> Unit = {},
        ): Component? {
            if (block.chronons!!.power < 1000) return noChrononsFail
            val result = getTimedoor(block.level!!, location)
            result.right().getOrNull()?.let { return it }

            val timedoor = result.left().getOrNull() ?: return fail
            timedoor.owner = player.id
            timedoor.sizing = DefaultSizing.DEFAULT
            timedoor.sizing.placeTimedoor(
                DoorType.ENTRY,
                Vec3.atCenterOf(block.blockPos).add(0.0, -1.5, 0.0),
                block.blockState.getValue(BlockStateProperties.HORIZONTAL_FACING).toYRot() + 180,
                timedoor
            )

            val event = TimedoorEvent.OpenWithBlock(timedoor, player, block).post()
            if (event.isCanceled) return event.errorMessage ?: fail
            else logTimedoorOpen(player.name, location, timedoor)

            onOpen(timedoor)
            block.level!!.addFreshEntity(timedoor)
            block.chronons?.extract(1000, ActionType.Execute)
            return null
        }

        fun getTimedoor(
            level: Level,
            location: NamedGlobalVec3,
            ignoreRestrictions: Boolean = false,
        ): Either<TimedoorEntity, Component> {
            val targetDimension = location.dimension ?: return Either.right(posFail)
            location.pos ?: return Either.right(posFail)
            val lookup = level.registryAccess().lookup(Registries.DIMENSION)
            val targetHolder = lookup.get().get(targetDimension).getOrNull() ?: return Either.right(posFail)
            val sourceHolder = lookup.get().get(level.dimension()).getOrNull() ?: return Either.right(posFail)
            if (!ignoreRestrictions) {
                level.dimension().let {
                    if (it != location.dimension) {
                        if (!CommonConfig.allowInterdimensionalTravel) return Either.right(interDimFail)
                        if (sourceHolder in ModTags.leavingNotSupported) return Either.right(leavingFail)
                        if (targetHolder in ModTags.enteringNotSupported) return Either.right(enteringFail)
                    } else {
                        if (!CommonConfig.allowIntradimensionalTravel) return Either.right(intraDimAllFail)
                        if (sourceHolder in ModTags.intradimensionalTravelNotSupported) return Either.right(intraDimFail)
                    }
                }
            }

            return Either.left(TimedoorEntity(ModEntities.TIMEDOOR_ENTITY, level).apply {
                setLocation(location)
            })
        }

        private fun logTimedoorOpen(player: String, location: NamedGlobalVec3, timedoor: TimedoorEntity) {
            if (CommonConfig.TimeDoor.logWhenOpen) {
                Tempad.logger.debug(
                    "Player {} opened a timedoor at {} in dimension {} to {} in dimension {}",
                    player,
                    timedoor.blockPosition(),
                    timedoor.level().dimension(),
                    location.name,
                    timedoor.targetDimension
                )
            }
        }
    }

    private fun canTeleport(entity: Entity, targetLevel: Level): Boolean {
        with(sizing) {
            return entity !is TimedoorEntity
                    && isInside(entity)
                    && entity !in ModTags.teleportingNotSupport
                    && entity.canChangeDimensions(level(), targetLevel)
                    && !entity.isPassenger
                    && entity.ageUntilAllowedThroughTimedoor?.let { entity.tickCount > it } ?: true
        }
    }

    var targetAngle = 0f

    var targetPos by DataDelegate(TARGET_POS)
    var targetDimension by DataDelegate(TARGET_DIMENSION)
    var color by DataDelegate(COLOR)
    var closingTime by DataDelegate(CLOSING_TIME)
    var sizing: TimedoorSizing
        get() = entityData.get(SIZING)
        set(value) {
            entityData.set(SIZING, value)
            this.fixupDimensions()
        }

    var owner: UUID? = null
    var glitching: Boolean by DataDelegate(GLITCHING)

    private var linkedPortalEntity: TimedoorEntity? = null

    private val targetLevel: ServerLevel?
        get() = targetDimension.let { level().server[it] }

    private val selfLocation: NamedGlobalVec3
        get() = NamedGlobalVec3(
            name,
            offsetLocation(this.pos, this.yRot),
            level().dimension(),
            yRot,
            color
        )

    override fun defineSynchedData(pBuilder: SynchedEntityData.Builder) {
        pBuilder.define(CLOSING_TIME, CommonConfig.TimeDoor.idleAfterEnter)
        pBuilder.define(COLOR, Tempad.ORANGE)
        pBuilder.define(TARGET_POS, Vec3.ZERO)
        pBuilder.define(TARGET_DIMENSION, Level.OVERWORLD)
        pBuilder.define(SIZING, DefaultSizing.DEFAULT)
        pBuilder.define(GLITCHING, false)
    }

    override fun getDimensions(pose: Pose): EntityDimensions = sizing.dimensions

    override fun isAlwaysTicking() = true

    override fun tick() {
        super.tick()
        if (level().isClientSide()) {
            if (sizing.dimensions.width != bbWidth || sizing.dimensions.height != bbHeight) {
                this.fixupDimensions()
                this.boundingBox = makeBoundingBox()
            }
            var percent = (tickCount / IDLE_BEFORE_START.toDouble())
            if (tickCount < IDLE_BEFORE_START && random.nextDouble() < percent) {
                percent *= 0.25
                val y = this.y + bbHeight / 2.0
                level().addParticle(
                    DustParticleOptions(color.vec3f, 1.0f),
                    true,
                    x + random.nextDouble() * percent - percent / 2,
                    y + random.nextDouble() * percent - percent / 2,
                    z + random.nextDouble() * percent - percent / 2,
                    0.0,
                    0.0,
                    0.0,
                )
            }
            return
        }
        if (tickCount < IDLE_BEFORE_START + ANIMATION_LENGTH) {
            return
        }
        if (tickCount > closingTime && closingTime != -1) {
            tryClose()
            return
        }
        val targetLevel = targetLevel ?: return tryClose()
        val entities = level().getEntities<Entity>(boundingBox) { canTeleport(it, targetLevel) }
        if (entities.isEmpty()) {
            tryClose()
            return
        }
        tryInitReceivingPortal()
        for (entity in entities) {
            val event = TimedoorEvent.Enter(this, entity).post()
            if (event.isCanceled) continue

            if (entity.level().dimension() == targetLevel.dimension()) {
                entity.deltaMovement = entity.deltaMovement.yRot(this.yRot - targetAngle)
                entity.teleportTo(
                    targetLevel,
                    targetPos.x,
                    targetPos.y,
                    targetPos.z,
                    RelativeMovement.ALL,
                    targetAngle,
                    entity.xRot
                )
                entity.hasImpulse = true
                if (entity is Player) {
                    RotatePlayerMomentumPacket(this.yRot - targetAngle).sendToClient(entity)
                }
                entity.ageUntilAllowedThroughTimedoor = entity.tickCount + 30
            } else {
                entity.changeDimension(
                    DimensionTransition(
                        targetLevel,
                        targetPos,
                        entity.deltaMovement,
                        targetAngle,
                        0.0F,
                        false,
                        DimensionTransition.DO_NOTHING
                    )
                )
                entity.ageUntilAllowedThroughTimedoor = entity.tickCount + 60
            }

            linkedPortalEntity?.let { TimedoorEvent.Exit(it, entity).post() }
        }
        tryClose()
    }

    private fun tryInitReceivingPortal() {
        val targetLevel = targetLevel ?: return
        linkedPortalEntity?.let {
            it.closingTime = this.closingTime - this.tickCount
            return
        }
        val targetPortal = TimedoorEntity(ModEntities.TIMEDOOR_ENTITY, targetLevel)
        targetPortal.linkedPortalEntity = this
        targetPortal.closingTime = this.closingTime - this.tickCount
        targetPortal.setLocation(selfLocation)
        targetPortal.sizing = this.sizing
        targetPortal.glitching = this.glitching
        sizing.placeTimedoor(DoorType.EXIT, targetPos, targetAngle + 180f, targetPortal)
        linkedPortalEntity = targetPortal
        targetLevel.addFreshEntity(targetPortal)
    }

    private fun tryClose() {
        if (tickCount > closingTime + ANIMATION_LENGTH && closingTime != -1) {
            TimedoorEvent.Close(this).post()
            this.linkedPortalEntity?.linkedPortalEntity = null
            this.discard()
        }
    }

    override fun onRemovedFromLevel() {
        super.onRemovedFromLevel()
        if (this.linkedPortalEntity != null) this.linkedPortalEntity!!.linkedPortalEntity = null
        this.linkedPortalEntity = null
    }

    fun setLocation(location: NamedGlobalVec3) {
        this.targetPos = location.pos!!
        this.targetDimension = location.dimension!!
        this.customName = location.name
        this.targetAngle = location.angle
        this.color = location.color
    }

    override fun fireImmune(): Boolean = true

    override fun readAdditionalSaveData(pCompound: CompoundTag) {}
    override fun addAdditionalSaveData(pCompound: CompoundTag) {}

    override fun onAddedToLevel() {
        super.onAddedToLevel()
        if (level().isClientSide) return
        level().playSound(null, blockPosition(), ModSounds.timedoorOpen, soundSource, 1.0f, 1.0f)
    }
}
