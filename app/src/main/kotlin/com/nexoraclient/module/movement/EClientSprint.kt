package com.rubidiumclient.module.movement

import com.rubidiumclient.core.proxy.EntityTracker
import com.rubidiumclient.events.PacketEvent
import com.rubidiumclient.module.BaseModule
import com.rubidiumclient.module.ModuleCategory
import org.cloudburstmc.math.vector.Vector3i
import org.cloudburstmc.protocol.bedrock.data.PlayerActionType
import org.cloudburstmc.protocol.bedrock.packet.PlayerActionPacket
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket

/** Ported from ProtoHax ModuleSprint, adapted to Rubidium's relay session. */
class EClientSprint : BaseModule("EClientSprint", ModuleCategory.MOVEMENT, "ProtoHax Sprint") {
    override fun onPacket(event: PacketEvent) {
        if (!isEnabled || !event.isClientToServer || event.packet !is PlayerAuthInputPacket) return
        val moving = EntityTracker.selfSpeedXZ > 0.001f
        if (!moving) return
        event.session.serverBound(PlayerActionPacket().apply {
            runtimeEntityId = EntityTracker.selfRuntimeId
            action = PlayerActionType.START_SPRINT
            blockPosition = Vector3i.ZERO
            resultPosition = Vector3i.ZERO
            face = 0
        })
    }
}
