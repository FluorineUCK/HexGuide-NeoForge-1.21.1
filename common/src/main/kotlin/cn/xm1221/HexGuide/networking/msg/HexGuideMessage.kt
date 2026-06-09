package cn.xm1221.HexGuide.networking.msg

import dev.architectury.networking.NetworkChannel
import dev.architectury.networking.NetworkManager.PacketContext
import cn.xm1221.HexGuide.HexGuide
import cn.xm1221.HexGuide.networking.HexGuideNetworking
import cn.xm1221.HexGuide.networking.handler.applyOnClient
import cn.xm1221.HexGuide.networking.handler.applyOnServer
import net.fabricmc.api.EnvType
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.server.level.ServerPlayer
import java.util.function.Supplier

sealed interface HexGuideMessage

sealed interface HexGuideMessageC2S : HexGuideMessage {
    fun sendToServer() {
        HexGuideNetworking.CHANNEL.sendToServer(this)
    }
}

sealed interface HexGuideMessageS2C : HexGuideMessage {
    fun sendToPlayer(player: ServerPlayer) {
        HexGuideNetworking.CHANNEL.sendToPlayer(player, this)
    }

    fun sendToPlayers(players: Iterable<ServerPlayer>) {
        HexGuideNetworking.CHANNEL.sendToPlayers(players, this)
    }
}

sealed interface HexGuideMessageCompanion<T : HexGuideMessage> {
    val type: Class<T>

    fun decode(buf: FriendlyByteBuf): T

    fun T.encode(buf: FriendlyByteBuf)

    fun apply(msg: T, supplier: Supplier<PacketContext>) {
        val ctx = supplier.get()
        when (ctx.env) {
            EnvType.SERVER, null -> {
                HexGuide.LOGGER.debug("Server received packet from {}: {}", ctx.player.name.string, this)
                when (msg) {
                    is HexGuideMessageC2S -> msg.applyOnServer(ctx)
                    else -> HexGuide.LOGGER.warn("Message not handled on server: {}", msg::class)
                }
            }
            EnvType.CLIENT -> {
                HexGuide.LOGGER.debug("Client received packet: {}", this)
                when (msg) {
                    is HexGuideMessageS2C -> msg.applyOnClient(ctx)
                    else -> HexGuide.LOGGER.warn("Message not handled on client: {}", msg::class)
                }
            }
        }
    }

    fun register(channel: NetworkChannel) {
        channel.register(type, { msg, buf -> msg.encode(buf) }, ::decode, ::apply)
    }
}
