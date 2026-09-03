package cn.xm1221.HexGuide

import cn.xm1221.HexGuide.compat.inline.IotaInlineRender
import cn.xm1221.HexGuide.compat.inline.IotaMatcher
import cn.xm1221.HexGuide.patchouli.PageComponentText
import cn.xm1221.HexGuide.patchouli.SpellcastDemoPage
import cn.xm1221.HexGuide.patchouli.SpellcastingPage
import cn.xm1221.HexGuide.scrying.ScryingBookOverlay
import com.samsthenerd.inline.api.client.InlineClientAPI
import net.minecraft.resources.ResourceLocation
import vazkii.patchouli.client.book.ClientBookRegistry

object HexGuideClient {
    fun init() {
        ScryingBookOverlay.registerSlateOverlay()
        InlineClientAPI.INSTANCE.addMatcher(IotaMatcher.INSTANCE)
        InlineClientAPI.INSTANCE.addRenderer(IotaInlineRender())

        // 注册自定义 Patchouli 页面类型 hexguide:component_text
        ClientBookRegistry.INSTANCE.pageTypes[ResourceLocation.fromNamespaceAndPath(HexGuide.MODID, "component_text")] =
            PageComponentText::class.java
        // hexguide:spellcasting —— 页面内嵌可交互法阵绘制区
        ClientBookRegistry.INSTANCE.pageTypes[ResourceLocation.fromNamespaceAndPath(HexGuide.MODID, "spellcasting")] =
            SpellcastingPage::class.java
        // hexguide:spellcast_demo —— 手册内演示图案绘制与栈变化
        ClientBookRegistry.INSTANCE.pageTypes[ResourceLocation.fromNamespaceAndPath(HexGuide.MODID, "spellcast_demo")] =
            SpellcastDemoPage::class.java
        // hexguide:note_page —— 笔记显示页（当前节第 N 个 NoteIota）
        ClientBookRegistry.INSTANCE.pageTypes[ResourceLocation.fromNamespaceAndPath(HexGuide.MODID, "note_page")] =
            cn.xm1221.HexGuide.patchouli.NotePage::class.java
        // hexguide:note_index —— 笔记目录页（列出节并跳转）
        ClientBookRegistry.INSTANCE.pageTypes[ResourceLocation.fromNamespaceAndPath(HexGuide.MODID, "note_index")] =
            cn.xm1221.HexGuide.patchouli.NoteIndex::class.java
    }
}

