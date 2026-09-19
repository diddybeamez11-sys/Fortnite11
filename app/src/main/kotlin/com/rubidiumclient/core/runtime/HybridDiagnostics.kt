package com.rubidiumclient.core.runtime

import com.rubidiumclient.events.PacketEvent
import com.rubidiumclient.events.PacketEventBus
import java.util.concurrent.atomic.AtomicLong

/**
 * Read-only diagnostics for proving which data path is alive.
 * It never changes or blocks network traffic.
 */
object HybridDiagnostics : PacketEventBus.PacketListener {
    private val packetsReceived = AtomicLong(0)
    private val clientToServer = AtomicLong(0)
    private val serverToClient = AtomicLong(0)
    private val lastPacketAt = AtomicLong(0)

    @Volatile var packetObservationEnabled: Boolean = true
        private set

    fun start() {
        PacketEventBus.register(this)
    }

    fun stop() {
        PacketEventBus.unregister(this)
    }

    fun resetPacketCounters() {
        packetsReceived.set(0)
        clientToServer.set(0)
        serverToClient.set(0)
        lastPacketAt.set(0)
    }

    fun setPacketObservationEnabled(enabled: Boolean) {
        packetObservationEnabled = enabled
    }

    override val priority: Int get() = Int.MIN_VALUE

    override fun onPacket(event: PacketEvent) {
        if (!packetObservationEnabled) return
        packetsReceived.incrementAndGet()
        lastPacketAt.set(System.currentTimeMillis())
        if (event.isClientToServer) clientToServer.incrementAndGet()
        else serverToClient.incrementAndGet()
    }

    fun snapshot(): Snapshot = Snapshot(
        packetsReceived = packetsReceived.get(),
        clientToServer = clientToServer.get(),
        serverToClient = serverToClient.get(),
        lastPacketAt = lastPacketAt.get(),
        packetObservationEnabled = packetObservationEnabled
    )

    data class Snapshot(
        val packetsReceived: Long,
        val clientToServer: Long,
        val serverToClient: Long,
        val lastPacketAt: Long,
        val packetObservationEnabled: Boolean
    )
}
