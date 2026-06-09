package cn.xm1221.HexGuide.fabric

import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi
import cn.xm1221.HexGuide.HexGuideClient

object FabricHexGuideModMenu : ModMenuApi {
    override fun getModConfigScreenFactory() = ConfigScreenFactory(HexGuideClient::getConfigScreen)
}
