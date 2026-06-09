package cn.xm1221.HexGuide

import cn.xm1221.HexGuide.config.HexGuideClientConfig
import me.shedaniel.autoconfig.AutoConfig
import net.minecraft.client.gui.screens.Screen

object HexGuideClient {
    fun init() {
        HexGuideClientConfig.init()
    }

    fun getConfigScreen(parent: Screen): Screen {
        return AutoConfig.getConfigScreen(HexGuideClientConfig.GlobalConfig::class.java, parent).get()
    }
}
