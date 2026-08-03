package cn.xm1221.HexGuide

import cn.xm1221.HexGuide.compat.inline.IotaInlineRender
import cn.xm1221.HexGuide.compat.inline.IotaMatcher
import cn.xm1221.HexGuide.config.HexGuideClientConfig
import cn.xm1221.HexGuide.patchouli.PageComponentText
import cn.xm1221.HexGuide.patchouli.SpellcastingPage
import cn.xm1221.HexGuide.scrying.ScryingBookOverlay
import com.samsthenerd.inline.api.client.InlineClientAPI
import me.shedaniel.autoconfig.AutoConfig
import net.minecraft.client.gui.screens.Screen
import net.minecraft.resources.ResourceLocation
import vazkii.patchouli.client.book.ClientBookRegistry

object HexGuideClient {
    fun init() {
        HexGuideClientConfig.init()
        ScryingBookOverlay.registerSlateOverlay()
        InlineClientAPI.INSTANCE.addMatcher(IotaMatcher.INSTANCE)
        InlineClientAPI.INSTANCE.addRenderer(IotaInlineRender())

        // 注册自定义 Patchouli 页面类型 hexguide:component_text
        ClientBookRegistry.INSTANCE.pageTypes[ResourceLocation("hexguide", "component_text")] =
            PageComponentText::class.java
        // hexguide:spellcasting —— 页面内嵌可交互法阵绘制区
        ClientBookRegistry.INSTANCE.pageTypes[ResourceLocation("hexguide", "spellcasting")] =
            SpellcastingPage::class.java
    }

    fun getConfigScreen(parent: Screen): Screen {
        return AutoConfig.getConfigScreen(HexGuideClientConfig.GlobalConfig::class.java, parent).get()
    }
}
