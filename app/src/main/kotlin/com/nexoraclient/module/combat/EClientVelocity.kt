package com.rubidiumclient.module.combat

import com.rubidiumclient.events.PacketEvent
import com.rubidiumclient.module.BaseModule
import com.rubidiumclient.module.ModuleCategory
import org.cloudburstmc.math.vector.Vector3f
import org.cloudburstmc.protocol.bedrock.packet.SetEntityMotionPacket

enum class EClientVelocityMode { CANCEL, SCALE }

class EClientVelocity : BaseModule("EClientVelocity", ModuleCategory.COMBAT, "ProtoHax-style knockback control") {
    private val mode = enum("Mode", EClientVelocityMode.CANCEL)
    private val horizontal = float("Horizontal", 0f, 0f, 1f)
    private val vertical = float("Vertical", 0f, 0f, 1f)
    override fun onPacket(event: PacketEvent) {
        if (!isEnabled || !event.isServerToClient) return
        val p = event.packet as? SetEntityMotionPacket ?: return
        if (mode.value == EClientVelocityMode.CANCEL) event.cancel()
        else p.motion = Vector3f.from(p.motion.x * horizontal.value, p.motion.y * vertical.value, p.motion.z * horizontal.value)
    }
}
