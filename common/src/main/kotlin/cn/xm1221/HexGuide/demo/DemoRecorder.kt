package cn.xm1221.HexGuide.demo

import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.iota.IotaType
import at.petrak.hexcasting.api.casting.iota.ListIota
import at.petrak.hexcasting.api.casting.iota.PatternIota
import com.google.gson.GsonBuilder
import net.minecraft.nbt.NbtIo
import java.nio.file.Files
import java.nio.file.Path

/**
 * 将 List<PatternIota> 导出为 demo 的 json + nbt 文件对。
 */
object DemoRecorder {

    private val GSON = GsonBuilder().setPrettyPrinting().create()

    data class JsonMeta(
        val duration: Double,
        val hexSize: Float,
        val pauses: DoubleArray,
        val nbt: String
    )

    fun save(
        ns: String, name: String,
        duration: Double, hexSize: Float,
        pauses: DoubleArray,
        iotas: List<PatternIota>,
        resRoot: Path
    ) {
        val dir = resRoot.resolve("assets/$ns/demo")
        Files.createDirectories(dir)

        // NBT
        val nbtPath = dir.resolve("$name.nbt")
        val tag = IotaType.serialize(ListIota(iotas))
        NbtIo.writeCompressed(tag, Files.newOutputStream(nbtPath))

        // JSON
        val jsonPath = dir.resolve("$name.json")
        val json = JsonMeta(duration, hexSize, pauses, "$ns:$name")
        Files.writeString(jsonPath, GSON.toJson(json))
    }
}
