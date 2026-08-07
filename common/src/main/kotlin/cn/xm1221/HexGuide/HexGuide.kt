package cn.xm1221.HexGuide

import cn.xm1221.HexGuide.compat.inline.InlineHexGuide
import net.minecraft.resources.ResourceLocation
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import cn.xm1221.HexGuide.config.HexGuideServerConfig
import cn.xm1221.HexGuide.networking.HexGuideNetworking
import cn.xm1221.HexGuide.registry.HexGuideActions
import cn.xm1221.HexGuide.registry.HexGuideCreativeTab
import cn.xm1221.HexGuide.registry.HexGuideIotaTypes
import cn.xm1221.HexGuide.registry.HexGuideItems
import dev.architectury.event.events.common.LifecycleEvent
import dev.architectury.event.events.common.PlayerEvent

object HexGuide {
    const val MODID = "hexguide"

    @JvmField
    val LOGGER: Logger = LogManager.getLogger(MODID)

    @JvmStatic
    fun id(path: String) = ResourceLocation(MODID, path)

    fun init() {
        HexGuideServerConfig.init()
        initRegistries(
            HexGuideActions,
            HexGuideCreativeTab,
            HexGuideIotaTypes,
            HexGuideItems
        )
        HexGuideNetworking.init()
        InlineHexGuide.init()
        HexGuideTagFixer.init() // 互联时把 Fabric 路径的 hexcasting:action tag 补全
        // 玩家进服时下发其笔记（手册笔记页）
        PlayerEvent.PLAYER_JOIN.register { player ->
            val notes = cn.xm1221.HexGuide.api.notes.PlayerNotes.get(player.serverLevel())
            cn.xm1221.HexGuide.networking.handler.syncNotes(player, notes)
        }
        //HexGuideCreativeTab.register()
    }

    fun initServer() {
        HexGuideServerConfig.initServer()
    }
}
