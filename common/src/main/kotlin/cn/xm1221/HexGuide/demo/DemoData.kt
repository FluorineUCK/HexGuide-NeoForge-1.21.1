package cn.xm1221.HexGuide.demo

import at.petrak.hexcasting.api.casting.iota.IotaType
import at.petrak.hexcasting.api.casting.iota.ListIota
import at.petrak.hexcasting.api.casting.iota.PatternIota
import at.petrak.hexcasting.api.casting.math.HexPattern
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import net.minecraft.client.Minecraft
import net.minecraft.nbt.NbtIo
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.Resource
import java.io.InputStreamReader

/**
 * 加载 demo 文件对：json 元数据 + nbt ListIota(PatternIota...)
 */
class DemoData(
    val id: String,
    val duration: Double,
    val hexSize: Float,
    val pauses: DoubleArray,
    val patterns: List<HexPattern>
) {
    companion object {
        private val GSON = GsonBuilder().setPrettyPrinting().create()
        private data class JsonMeta(
            val duration: Double = 20.0,
            val hexSize: Float = 28f,
            val pauses: DoubleArray = doubleArrayOf(),
            val nbt: String = ""
        )

        fun load(ns: String, name: String): DemoData? {
            return try {
                val mgr = Minecraft.getInstance().resourceManager
                val jsonRl = ResourceLocation(ns, "demo/$name.json")
                val jsonRes = mgr.getResource(jsonRl).orElse(null) ?: return null

                val meta: JsonMeta
                jsonRes.open().use { meta = GSON.fromJson(InputStreamReader(it), JsonMeta::class.java) }

                // 解析 nbt 引用 "ns:name"
                val colon = meta.nbt.lastIndexOf(':')
                val nbtNs = if (colon > 0) meta.nbt.substring(0, colon) else ns
                val nbtName = if (colon > 0) meta.nbt.substring(colon + 1) else meta.nbt
                val nbtRl = ResourceLocation(nbtNs, "demo/$nbtName.nbt")
                val nbtRes = mgr.getResource(nbtRl).orElse(null) ?: return null

                val tag = nbtRes.open().use { NbtIo.readCompressed(it) } ?: return null
                val iota = IotaType.deserialize(tag, null)
                if (iota !is ListIota) return null

                val patterns = iota.list
                    .filterIsInstance<PatternIota>()
                    .map { it.pattern }

                DemoData("$ns:$name", meta.duration, meta.hexSize, meta.pauses, patterns)
            } catch (_: Exception) { null }
        }
    }
}
