package com.rubidiumclient.module.movement

import com.rubidiumclient.events.PacketEvent
import com.rubidiumclient.events.PacketEventBus
import com.rubidiumclient.module.BaseModule
import com.rubidiumclient.module.ModuleCategory
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket

/** ProtoHax ModuleBlink semantics adapted to Rubidium's packet bus. */
class EClientBlink : BaseModule(
    name = "EClientBlink",
    category = ModuleCategory.MOVEMENT,
    description = "Buffers outbound packets while enabled"
) {
    private val buffered = ArrayList<BedrockPacket>()

    override fun onPacket(event: PacketEvent) {
        if (!isEnabled || !event.isClientToServer) return
        buffered += event.packet
        event.cancel()
    }

    override fun onDisable() {
        val session = PacketEventBus.currentSession
        val copy = buffered.toList()
        buffered.clear()
        if (session != null) copy.forEach(session::serverBound)
        super.onDisable()
    }
}
