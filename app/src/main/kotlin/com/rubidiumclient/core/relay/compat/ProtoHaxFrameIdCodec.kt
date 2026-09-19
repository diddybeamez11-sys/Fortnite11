package com.rubidiumclient.core.relay.compat

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToMessageCodec
import io.netty.util.internal.StringUtil
import org.cloudburstmc.netty.channel.raknet.RakReliability
import org.cloudburstmc.netty.channel.raknet.packet.RakMessage
import org.cloudburstmc.protocol.bedrock.netty.initializer.BedrockChannelInitializer

/**
 * ProtoHax-compatible RakNet frame wrapper.
 *
 * ProtoHax uses this layer to force the Minecraft frame payload onto a
 * predictable RakNet reliability class. Rubidium keeps the same behavior,
 * while leaving all existing module packet handling untouched.
 */
@Sharable
class ProtoHaxFrameIdCodec(
    private val frameId: Int = BedrockChannelInitializer.RAKNET_MINECRAFT_ID,
    private val reliability: RakReliability? = RakReliability.RELIABLE_ORDERED
) : MessageToMessageCodec<Any, ByteBuf>() {

    override fun encode(ctx: ChannelHandlerContext, msg: ByteBuf, out: MutableList<Any>) {
        val buf = ctx.alloc().compositeDirectBuffer(2)
        try {
            buf.addComponent(true, ctx.alloc().ioBuffer(1).writeByte(frameId))
            buf.addComponent(true, msg.retainedSlice())
            if (reliability == null) {
                out.add(buf.retain())
            } else {
                out.add(RakMessage(buf.retain(), reliability))
            }
        } finally {
            buf.release()
        }
    }

    override fun decode(ctx: ChannelHandlerContext, msg: Any, out: MutableList<Any>) {
        val content = when (msg) {
            is RakMessage -> msg.content()
            is ByteBuf -> msg
            else -> throw UnsupportedOperationException(
                "unsupported message type: ${StringUtil.simpleClassName(msg)}"
            )
        }

        if (!content.isReadable) return

        val id = content.readUnsignedByte().toInt()
        check(id == frameId) { "Invalid frame ID: $id" }
        out.add(content.readRetainedSlice(content.readableBytes()))
    }

    companion object {
        const val NAME = "protohax-frame-id-codec"
    }
}
