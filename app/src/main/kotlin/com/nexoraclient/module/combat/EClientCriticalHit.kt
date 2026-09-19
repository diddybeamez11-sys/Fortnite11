package com.rubidiumclient.module.combat

import com.rubidiumclient.events.PacketEvent
import com.rubidiumclient.module.BaseModule
import com.rubidiumclient.module.ModuleCategory
import org.cloudburstmc.math.vector.Vector3f
import org.cloudburstmc.protocol.bedrock.packet.MovePlayerPacket

enum class EClientCriticalMode { EASECATION, VANILLA }

/** Ported from ProtoHax ModuleCriticalHit, adapted to the Rubidium packet bus. */
class EClientCriticalHit : BaseModule("EClientCriticalHit", ModuleCategory.COMBAT, "ProtoHax CriticalHit") {
    private val mode = enum("Mode", EClientCriticalMode.EASECATION)
    private var height = 1.2f

    override fun onEnable() { height = 1.2f; super.onEnable() }

    override fun onPacket(event: PacketEvent) {
        if (!isEnabled || !event.isClientToServer) return
        val packet = event.packet as? MovePlayerPacket ?: return
        when (mode.value) {
            EClientCriticalMode.EASECATION -> {
                packet.position = Vector3f.from(packet.position.x, packet.position.y + height, packet.position.z)
                height -= 0.1f
                if (height <= 0.3f) height = 1.2f
            }
            EClientCriticalMode.VANILLA -> {
                packet.position = packet.position.add(0.2, 0.2, 0.2)
                packet.isOnGround = false
            }
        }
    }
}
