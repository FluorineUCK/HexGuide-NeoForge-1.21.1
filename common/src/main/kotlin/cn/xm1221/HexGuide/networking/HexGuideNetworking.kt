package cn.xm1221.HexGuide.networking

import dev.architectury.networking.NetworkChannel
import cn.xm1221.HexGuide.HexGuide
import cn.xm1221.HexGuide.networking.msg.HexGuideMessageCompanion

object HexGuideNetworking {
    val CHANNEL: NetworkChannel = NetworkChannel.create(HexGuide.id("networking_channel"))

    fun init() {
        for (subclass in HexGuideMessageCompanion::class.sealedSubclasses) {
            subclass.objectInstance?.register(CHANNEL)
        }
    }
}
