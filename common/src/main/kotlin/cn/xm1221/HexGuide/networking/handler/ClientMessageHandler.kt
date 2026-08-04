package cn.xm1221.HexGuide.networking.handler

import cn.xm1221.HexGuide.config.HexGuideServerConfig
import cn.xm1221.HexGuide.networking.msg.*
import cn.xm1221.HexGuide.patchouli.SpellcastDemoPage
import cn.xm1221.HexGuide.registry.HexGuideCreativeTab
import dev.architectury.networking.NetworkManager.PacketContext
import net.minecraft.client.Minecraft

fun HexGuideMessageS2C.applyOnClient(ctx: PacketContext) = ctx.queue {
    when (this) {
        is MsgSyncConfigS2C -> HexGuideServerConfig.onSyncConfig(serverConfig)

        // 演示"真执行"结果：路由到当前显示中的演示页
        is MsgBookExecDemoS2C -> {
            Minecraft.getInstance().execute {
                for (page in SpellcastDemoPage.ACTIVE) page.onExecResult(image, resolutionType)
            }
        }

        // 演示配置内容：路由到对应演示页
        is MsgBookLoadSpellplayS2C -> {
            Minecraft.getInstance().execute {
                for (page in SpellcastDemoPage.ACTIVE) {
                    if (page.matches(ns, name)) page.onSpellplayLoaded(json)
                }
            }
        }

        // 创造标签页排除列表（服务端 tag 完整）
        is MsgExcludedPatternsS2C -> {
            Minecraft.getInstance().execute {
                HexGuideCreativeTab.setExcludedPatterns(ids.toSet())
            }
        }
    }
}
