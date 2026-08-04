package cn.xm1221.HexGuide

import cn.xm1221.HexGuide.compat.inline.InlineHexGuide
import net.minecraft.resources.ResourceLocation
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import cn.xm1221.HexGuide.config.HexGuideServerConfig
import cn.xm1221.HexGuide.networking.HexGuideNetworking
import cn.xm1221.HexGuide.registry.HexGuideActions
import cn.xm1221.HexGuide.registry.HexGuideCreativeTab

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
            HexGuideCreativeTab
        )
        HexGuideNetworking.init()
        InlineHexGuide.init()
        HexGuideTagFixer.init() // 互联时把 Fabric 路径的 hexcasting:action tag 补全
        //HexGuideCreativeTab.register()
    }

    fun initServer() {
        HexGuideServerConfig.initServer()
    }
}
