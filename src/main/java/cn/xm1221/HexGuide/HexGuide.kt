package cn.xm1221.HexGuide

import net.minecraft.resources.ResourceLocation
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

object HexGuide {
    const val MODID = "hexguide"

    @JvmField
    val LOGGER: Logger = LogManager.getLogger(MODID)

    @JvmStatic
    fun id(path: String) = ResourceLocation.fromNamespaceAndPath(MODID, path)
}
