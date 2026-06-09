package cn.xm1221.HexGuide.fabric

import cn.xm1221.HexGuide.HexGuideClient
import net.fabricmc.api.ClientModInitializer

object FabricHexGuideClient : ClientModInitializer {
    override fun onInitializeClient() {
        HexGuideClient.init()
    }
}
