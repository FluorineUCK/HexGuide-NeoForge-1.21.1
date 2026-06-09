package cn.xm1221.HexGuide.fabric

import cn.xm1221.HexGuide.HexGuide
import net.fabricmc.api.ModInitializer

object FabricHexGuide : ModInitializer {
    override fun onInitialize() {
        HexGuide.init()
    }
}
