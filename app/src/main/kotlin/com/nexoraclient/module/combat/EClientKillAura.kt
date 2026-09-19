package com.rubidiumclient.module.combat

import com.rubidiumclient.core.proxy.EntityTracker
import com.rubidiumclient.events.PacketEvent
import com.rubidiumclient.events.PacketEventBus
import com.rubidiumclient.module.BaseModule
import com.rubidiumclient.module.ModuleCategory
import com.rubidiumclient.module.social.isFriendEntity
import com.rubidiumclient.utils.MathUtil
import com.rubidiumclient.utils.PacketUtil
import com.rubidiumclient.utils.RotationUtil
import org.cloudburstmc.math.vector.Vector3f
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * Fresh Eclient KillAura implementation inspired by the supplied module's
 * capabilities, but written for Eclient's own module/event API.
 */
class EClientKillAura : BaseModule(
    name = "EClientKillAura",
    category = ModuleCategory.COMBAT,
    description = "Multi-target Bedrock aura with predictive aim and configurable rotations"
), PacketEventBus.PacketListener {

    private val range = float("Range", 8.0f, 2.0f, 32.0f)
    private val fov = float("FOV", 360.0f, 30.0f, 360.0f)
    private val cps = float("CPS", 12.0f, 1.0f, 25.0f)
    private val maxTargets = int("Max Targets", 1, 1, 6)
    private val multiTarget = bool("Multi Target", false)
    private val playersOnly = bool("Players Only", true)
    private val ignoreFriends = bool("Ignore Friends", true)
    private val antiBot = bool("Anti Bot", true)
    private val requireInRange = bool("Require In Range", true)

    private val targetMode = enum("Target Mode", TargetMode.DISTANCE)
    private val aimPoint = enum("Aim Point", AimPoint.CENTER)
    private val rotationMode = enum("Rotation", RotationMode.PREDICTIVE)
    private val silentRotation = bool("Silent Rotation", true)
    private val rotationSpeed = float("Rotation Speed", 0.55f, 0.05f, 1.0f)
    private val rotationJitter = float("Rotation Jitter", 0.0f, 0.0f, 2.0f)

    private val prediction = bool("Prediction", true)
    private val predictionTime = float("Prediction Time", 0.10f, 0.0f, 0.50f)
    private val predictionScale = float("Prediction Scale", 1.0f, 0.0f, 2.0f)

    private val attacksPerCycle = int("Attacks", 1, 1, 3)
    private val attackSpread = float("Attack Spread", 0.0f, 0.0f, 0.35f)
    private val switchDelay = int("Switch Delay", 120, 0, 1000)

    private var currentTargetId = Long.MIN_VALUE
    private var lastAttackNs = 0L
    private var lastSwitchMs = 0L
    private var lastScanMs = 0L
    private var cachedTargets: List<EntityTracker.TrackedEntity> = emptyList()
    private var smoothYaw = 0f
    private var smoothPitch = 0f
    private var initializedRotation = false

    override fun onEnable() {
        super.onEnable()
        currentTargetId = Long.MIN_VALUE
        lastAttackNs = 0L
        lastSwitchMs = 0L
        lastScanMs = 0L
        cachedTargets = emptyList()
        smoothYaw = EntityTracker.selfYaw
        smoothPitch = EntityTracker.selfPitch
        initializedRotation = false
    }

    override fun onDisable() {
        cachedTargets = emptyList()
        currentTargetId = Long.MIN_VALUE
        initializedRotation = false
        super.onDisable()
    }

    override fun onPacket(event: PacketEvent) {
        if (!isEnabled || event.direction != PacketEvent.Direction.CLIENT_TO_SERVER) return
        val packet = event.packet as? PlayerAuthInputPacket ?: return
        val nowMs = System.currentTimeMillis()

        if (nowMs - lastScanMs >= 50L || cachedTargets.isEmpty()) {
            cachedTargets = findTargets()
            lastScanMs = nowMs
        }

        val primary = choosePrimary(cachedTargets, nowMs) ?: run {
            initializedRotation = false
            return
        }

        val aim = predictedAim(primary)
        val desired = RotationUtil.toPoint(aim.x, aim.y, aim.z)
        val rotation = makeRotation(desired)

        packet.rotation = Vector3f.from(rotation.pitch, rotation.yaw, rotation.yaw)
        if (!silentRotation.value) {
            EntityTracker.selfYaw = rotation.yaw
            EntityTracker.selfPitch = rotation.pitch
        }

        if (!canAttack(System.nanoTime())) {
            event.cancelAndReplace(packet)
            return
        }

        val targets = if (multiTarget.value) cachedTargets.take(maxTargets.value) else listOf(primary)
        val slot = EntityTracker.selfHotbarSlot.coerceIn(0, 8)
        var attacked = false

        repeat(attacksPerCycle.value) {
            for (target in targets) {
                if (!isStillAttackable(target)) continue
                val click = attackPoint(target)
                val spread = attackSpread.value
                val adjusted = if (spread > 0f) {
                    Vector3f.from(
                        click.x + randomOffset(spread),
                        click.y + randomOffset(spread * 0.5f),
                        click.z + randomOffset(spread)
                    )
                } else click

                PacketUtil.sendSwing(event.session)
                PacketUtil.sendAttack(event.session, target.runtimeId, slot, adjusted)
                attacked = true
            }
        }

        if (attacked) lastAttackNs = System.nanoTime()
        event.cancelAndReplace(packet)
    }

    private fun findTargets(): List<EntityTracker.TrackedEntity> {
        val sx = EntityTracker.selfX
        val sy = EntityTracker.selfY
        val sz = EntityTracker.selfZ

        return EntityTracker.getEntitiesInRange(range.value)
            .asSequence()
            .filter { it.runtimeId != EntityTracker.selfRuntimeId }
            .filter { isValidTarget(it, sx, sy, sz) }
            .filter { RotationUtil.fovCheck(it, fov.value) }
            .sortedWith(compareBy { score(it, sx, sy, sz) })
            .toList()
    }

    private fun isValidTarget(entity: EntityTracker.TrackedEntity, sx: Float, sy: Float, sz: Float): Boolean {
        if (playersOnly.value && !entity.isPlayer) return false
        if (ignoreFriends.value && entity.isFriendEntity) return false
        if (antiBot.value && (entity.name.isBlank() || entity.uniqueId == 0L)) return false
        if (requireInRange.value && MathUtil.dist3(sx, sy, sz, entity.x, entity.y, entity.z) > range.value) return false
        return true
    }

    private fun score(entity: EntityTracker.TrackedEntity, sx: Float, sy: Float, sz: Float): Float {
        val distance = MathUtil.dist3(sx, sy, sz, entity.x, entity.y, entity.z)
        return when (targetMode.value) {
            TargetMode.DISTANCE -> distance
            TargetMode.LOW_HEALTH -> entity.healthPercent * 100f + distance * 0.02f
            TargetMode.ANGLE -> RotationUtil.angleDiff(RotationUtil.toEntity(entity).yaw, EntityTracker.selfYaw) + distance * 0.01f
        }
    }

    private fun choosePrimary(targets: List<EntityTracker.TrackedEntity>, nowMs: Long): EntityTracker.TrackedEntity? {
        if (targets.isEmpty()) {
            currentTargetId = Long.MIN_VALUE
            return null
        }

        val locked = targets.firstOrNull { it.runtimeId == currentTargetId }
        if (locked != null && nowMs - lastSwitchMs < switchDelay.value) return locked

        val selected = targets.first()
        if (selected.runtimeId != currentTargetId) {
            currentTargetId = selected.runtimeId
            lastSwitchMs = nowMs
        }
        return selected
    }

    private fun predictedAim(target: EntityTracker.TrackedEntity): Vector3f {
        val baseY = when (aimPoint.value) {
            AimPoint.FEET -> target.y + 0.10f
            AimPoint.CENTER -> target.y + 0.85f
            AimPoint.HEAD -> target.y + 1.55f
        }

        if (!prediction.value) return Vector3f.from(target.x, baseY, target.z)

        val t = predictionTime.value * predictionScale.value
        return Vector3f.from(
            target.x + target.velX * t,
            baseY + target.velY * t,
            target.z + target.velZ * t
        )
    }

    private fun makeRotation(target: RotationUtil.Rotation): RotationUtil.Rotation {
        if (!initializedRotation) {
            initializedRotation = true
            smoothYaw = EntityTracker.selfYaw
            smoothPitch = EntityTracker.selfPitch
        }

        val desiredYaw = RotationUtil.normalize(target.yaw)
        val desiredPitch = target.pitch.coerceIn(-90f, 90f)

        return when (rotationMode.value) {
            RotationMode.INSTANT -> RotationUtil.Rotation(
                RotationUtil.normalize(desiredYaw + randomOffset(rotationJitter.value)),
                (desiredPitch + randomOffset(rotationJitter.value * 0.5f)).coerceIn(-90f, 90f)
            )
            RotationMode.SMOOTH -> {
                val factor = rotationSpeed.value
                smoothYaw = RotationUtil.normalize(smoothYaw + wrapDelta(desiredYaw, smoothYaw) * factor)
                smoothPitch += (desiredPitch - smoothPitch) * factor
                RotationUtil.Rotation(smoothYaw, smoothPitch.coerceIn(-90f, 90f))
            }
            RotationMode.PREDICTIVE -> {
                val factor = min(1f, rotationSpeed.value + 0.20f)
                smoothYaw = RotationUtil.normalize(smoothYaw + wrapDelta(desiredYaw, smoothYaw) * factor)
                smoothPitch += (desiredPitch - smoothPitch) * factor
                RotationUtil.Rotation(
                    RotationUtil.normalize(smoothYaw + randomOffset(rotationJitter.value)),
                    (smoothPitch + randomOffset(rotationJitter.value * 0.35f)).coerceIn(-90f, 90f)
                )
            }
        }
    }

    private fun attackPoint(target: EntityTracker.TrackedEntity): Vector3f = when (aimPoint.value) {
        AimPoint.FEET -> Vector3f.from(target.x, target.y + 0.1f, target.z)
        AimPoint.CENTER -> Vector3f.from(target.x, target.y + 0.85f, target.z)
        AimPoint.HEAD -> Vector3f.from(target.x, target.y + 1.55f, target.z)
    }

    private fun isStillAttackable(target: EntityTracker.TrackedEntity): Boolean {
        if (target.runtimeId == EntityTracker.selfRuntimeId) return false
        if (!isValidTarget(target, EntityTracker.selfX, EntityTracker.selfY, EntityTracker.selfZ)) return false
        return MathUtil.dist3(EntityTracker.selfX, EntityTracker.selfY, EntityTracker.selfZ, target.x, target.y, target.z) <= range.value
    }

    private fun canAttack(nowNs: Long): Boolean {
        val delay = (1_000_000_000.0 / max(1.0, cps.value.toDouble())).toLong()
        return nowNs - lastAttackNs >= delay
    }

    private fun wrapDelta(target: Float, current: Float): Float {
        var d = target - current
        while (d > 180f) d -= 360f
        while (d < -180f) d += 360f
        return d
    }

    private fun randomOffset(maxAbs: Float): Float =
        if (maxAbs <= 0f) 0f else Random.nextFloat() * maxAbs * 2f - maxAbs

    enum class TargetMode { DISTANCE, LOW_HEALTH, ANGLE }
    enum class AimPoint { FEET, CENTER, HEAD }
    enum class RotationMode { INSTANT, SMOOTH, PREDICTIVE }
}
