package cn.xm1221.HexGuide

import cn.xm1221.HexGuide.compat.inline.IotaInlineRender
import cn.xm1221.HexGuide.compat.inline.IotaMatcher
import cn.xm1221.HexGuide.config.HexGuideClientConfig
import cn.xm1221.HexGuide.scrying.ScryingBookOverlay
import com.samsthenerd.inline.api.client.InlineClientAPI
import me.shedaniel.autoconfig.AutoConfig
import net.minecraft.client.gui.screens.Screen

object HexGuideClient {
    fun init() {
        HexGuideClientConfig.init()
        ScryingBookOverlay.registerSlateOverlay()
        InlineClientAPI.INSTANCE.addMatcher(IotaMatcher.INSTANCE)
        InlineClientAPI.INSTANCE.addRenderer(IotaInlineRender())
    }

    fun getConfigScreen(parent: Screen): Screen {
        return AutoConfig.getConfigScreen(HexGuideClientConfig.GlobalConfig::class.java, parent).get()
    }
}
