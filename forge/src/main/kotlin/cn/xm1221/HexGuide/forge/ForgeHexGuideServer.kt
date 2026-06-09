package cn.xm1221.HexGuide.forge

import cn.xm1221.HexGuide.HexGuide
import net.minecraftforge.fml.event.lifecycle.FMLDedicatedServerSetupEvent

object ForgeHexGuideServer {
    @Suppress("UNUSED_PARAMETER")
    fun init(event: FMLDedicatedServerSetupEvent) {
        HexGuide.initServer()
    }
}
