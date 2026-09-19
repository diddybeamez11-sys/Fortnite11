package com.rubidiumclient.module.movement

import com.rubidiumclient.events.PacketEvent
import com.rubidiumclient.module.BaseModule
import com.rubidiumclient.module.ModuleCategory
import org.cloudburstmc.math.vector.Vector3f
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket

/** Ported from ProtoHax ModuleNoFall (Cubecraft packet mode). */
class EClientNoFall : BaseModule("EClientNoFall", ModuleCategory.MOVEMENT, "ProtoHax NoFall") {
    private val mode = enum("Mode", EClientNoFallMode.CUBECRAFT)

    override fun onPacket(event: PacketEvent) {
        if (!isEnabled || !event.isClientToServer) return
        val packet = event.packet as? PlayerAuthInputPacket ?: return
        when (mode.value) {
            EClientNoFallMode.CUBECRAFT -> if (packet.delta.y < -0.3f) {
                packet.delta = Vector3f.from(packet.delta.x, 0f, packet.delta.z)
            }
            EClientNoFallMode.PACKET -> if (packet.delta.y < -0.3f) {
                packet.delta = Vector3f.ZERO
            }
        }
    }
}

enum class EClientNoFallMode { CUBECRAFT, PACKET }
