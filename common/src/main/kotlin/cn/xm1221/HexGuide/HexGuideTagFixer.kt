package cn.xm1221.HexGuide

import at.petrak.hexcasting.api.casting.ActionRegistryEntry
import at.petrak.hexcasting.common.lib.HexRegistries
import at.petrak.hexcasting.common.lib.hex.HexActions
import com.google.gson.JsonParser
import dev.architectury.registry.ReloadListenerRegistry
import net.minecraft.core.Holder
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.PackType
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.server.packs.resources.SimplePreparableReloadListener
import net.minecraft.tags.TagKey
import net.minecraft.util.profiling.ProfilerFiller
import java.io.InputStreamReader

/**
 * 互联（Connector：Forge 环境跑 Fabric 版 HexMod）时，hexcasting:action 的 tag 路径不一致：
 * - Fabric：data/hexcasting/tags/action/xxx.json
 * - Forge：data/hexcasting/tags/hexcasting/action/xxx.json
 * 导致 Forge 的 TagLoader 读不到 Fabric 版 HexMod 的 tag → tag 全部丢失。
 *
 * 本监听器在服务端数据重载后，从 Fabric 路径（tags/action/）读取 tag，
 * 合并回 hexcasting:action 注册表（Registry.bindTags）。
 * 服务端 tag 完整后再同步给客户端，从而跨平台补全。
 */
object HexGuideTagFixer {
    fun init() {
        ReloadListenerRegistry.register(PackType.SERVER_DATA, object : SimplePreparableReloadListener<Unit>() {
            override fun prepare(rm: ResourceManager, profiler: ProfilerFiller): Unit {}

            override fun apply(prepared: Unit, rm: ResourceManager, profiler: ProfilerFiller) = fixTags(rm)
        })
    }

    private fun fixTags(rm: ResourceManager) {
        try {
            val reg = HexActions.REGISTRY

            // 1. 收集 Fabric 路径的 tag：data/<ns>/tags/action/*.json（tag 名 = 文件名）
            val fabric = mutableMapOf<ResourceLocation, List<String>>()
            for ((rl, _) in rm.listResources("hexcasting") {
                it.path.startsWith("tags/action/") && it.path.endsWith(".json")
            }) {
                val res = rm.getResource(rl).orElse(null) ?: continue
                res.open().use { stream ->
                    val json = JsonParser.parseReader(InputStreamReader(stream)).asJsonObject
                    val ids = json.getAsJsonArray("values").mapNotNull { e ->
                        val s = e.asString
                        if (s.startsWith("#")) null else s // 跳过对其他 tag 的引用
                    }
                    if (ids.isNotEmpty()) {
                        val name = rl.path.removePrefix("tags/action/").removeSuffix(".json")
                        fabric[ResourceLocation("hexcasting", name)] = ids
                    }
                }
            }
            if (fabric.isEmpty()) return

            // 2. 现有 tag（Forge 路径已加载的） + Fabric 路径补的，合并去重
            val merged = mutableMapOf<TagKey<ActionRegistryEntry>, List<Holder<ActionRegistryEntry>>>()
            for (tk in reg.getTagNames()) {
                merged[tk] = reg.getTag(tk).map { it.stream().toList() }.orElse(emptyList())
            }
            for ((name, ids) in fabric) {
                val tk = TagKey.create(HexRegistries.ACTION, name)
                val existing = merged[tk] ?: emptyList()
                // 已存在的 key（按 location 去重）
                val existingLocs = existing.mapNotNull { e -> e.unwrapKey().orElse(null)?.location()?.toString() }.toSet()
                val extra = ids.mapNotNull { id ->
                    try {
                        reg.getHolder(ResourceKey.create(HexRegistries.ACTION, ResourceLocation(id))).orElse(null)
                    } catch (_: Exception) {
                        null
                    }
                }.filter { h ->
                    !existingLocs.contains(h.unwrapKey().orElse(null)?.location()?.toString())
                }
                if (extra.isNotEmpty()) merged[tk] = existing + extra
            }

            // 3. 重新绑定（Registry.bindTags 是 public）
            reg.bindTags(merged)
        } catch (_: Exception) {
            // 非 HexMod 环境或解析失败时静默跳过
        }
    }
}
