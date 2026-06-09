package cn.xm1221.HexGuide.fabric

import cn.xm1221.HexGuide.HexGuide
import net.fabricmc.api.DedicatedServerModInitializer

object FabricHexGuideServer : DedicatedServerModInitializer {
    override fun onInitializeServer() {
        HexGuide.initServer()
    }
}
