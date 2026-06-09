package cn.xm1221.HexGuide.forge

import dev.architectury.platform.forge.EventBuses
import cn.xm1221.HexGuide.HexGuide
import cn.xm1221.HexGuide.forge.datagen.ForgeHexGuideDatagen
import net.minecraftforge.fml.common.Mod
import thedarkcolour.kotlinforforge.forge.MOD_BUS

@Mod(HexGuide.MODID)
class ForgeHexGuide {
    init {
        MOD_BUS.apply {
            EventBuses.registerModEventBus(HexGuide.MODID, this)
            addListener(ForgeHexGuideClient::init)
            addListener(ForgeHexGuideDatagen::init)
            addListener(ForgeHexGuideServer::init)
        }
        HexGuide.init()
    }
}
