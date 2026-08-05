package cn.xm1221.HexGuide

import at.petrak.hexcasting.api.casting.ActionRegistryEntry
import at.petrak.hexcasting.common.lib.HexRegistries
import at.petrak.hexcasting.common.lib.hex.HexActions
import cn.xm1221.HexGuide.config.HexGuideServerConfig
import com.google.gson.JsonParser
import dev.architectury.event.events.common.LifecycleEvent
import dev.architectury.platform.Platform
import net.minecraft.core.Holder
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.tags.TagKey
import java.io.InputStreamReader
import java.nio.file.Files
import java.util.zip.ZipFile

/**
 * 互联（Connector：Forge 环境跑 Fabric 版 HexMod / Fabric 附属）时，
 * hexcasting:action 的 tag 路径不一致：
 * - Fabric：data/hexcasting/tags/action/xxx.json（几乎所有附属都放这里，含 Forge 版附属）
 * - Forge：data/hexcasting/tags/hexcasting/action/xxx.json（仅 hexcasting 本体等少数）
 * 导致 Forge 的 TagLoader 只读到 Forge 路径的 tag，所有附属的 tag 丢失。
 *
 * 本修复在服务器启动完成后（TagLoader 已跑完）：
 * 1. 从服务端资源管理器扫描（原生 Forge 环境有效）
 * 2. 直接遍历 mods 目录 jar 读取 data/hexcasting/tags/action/ 下的 json
 *    （Connector 环境下 Fabric 附属的 data 资源不暴露给 ResourceManager，必须直接读 jar）
 * 合并回 hexcasting:action 注册表（Registry.bindTags）。服务端 tag 完整后再同步给客户端。
 */
object HexGuideTagFixer {
    fun init() {
        // 可在配置文件中关闭（hexguide 配置 → server → fixTags），默认开启
        if (!HexGuideServerConfig.config.fixTags) {
            HexGuide.LOGGER.info("[HexGuideTagFixer] 已在配置中关闭 tag 修复")
            return
        }
        // 服务器启动完成后触发（此时 TagLoader 已绑定完 tag，我们随后补全 Fabric 路径的）
        LifecycleEvent.SERVER_STARTED.register { server ->
            fixTags(server.getResourceManager())
        }
    }

    private fun fixTags(rm: ResourceManager) {
        try {
            val reg = HexActions.REGISTRY

            // 1a. 从服务端资源管理器扫描（原生环境有效）
            val fabric = mutableMapOf<ResourceLocation, List<String>>()
            for (ns in rm.getNamespaces()) {
                for ((rl, _) in rm.listResources(ns) {
                    it.path.startsWith("tags/action/") && it.path.endsWith(".json")
                }) {
                    val res = rm.getResource(rl).orElse(null) ?: continue
                    res.open().use { stream ->
                        collectTag(fabric, rl, stream)
                    }
                }
            }

            // 1b. 直接遍历 mods 目录 jar（Connector 环境下 ResourceManager 读不到 Fabric 附属的 data）
            collectFromModsDir(fabric)

            HexGuide.LOGGER.info("[HexGuideTagFixer] 读取到 Fabric 路径 tag: {}", fabric.keys)

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
            HexGuide.LOGGER.info(
                "[HexGuideTagFixer] 绑定完成，注册表现有 tag: {}",
                reg.getTagNames().map { it.location() }
            )
        } catch (e: Exception) {
            HexGuide.LOGGER.error("[HexGuideTagFixer] 修复 tag 失败", e)
        }
    }

    /** 解析单个 tag 文件：data/<ns>/tags/action/<name>.json → tagKey <ns>:<name> */
    private fun collectTag(
        out: MutableMap<ResourceLocation, List<String>>,
        rl: ResourceLocation,
        stream: java.io.InputStream,
    ) {
        try {
            val json = JsonParser.parseReader(InputStreamReader(stream)).asJsonObject
            val ids = json.getAsJsonArray("values").mapNotNull { e ->
                val s = e.asString
                if (s.startsWith("#")) null else s // 跳过对其他 tag 的引用
            }
            if (ids.isNotEmpty()) {
                val name = rl.path.removePrefix("tags/action/").removeSuffix(".json")
                // 合并同 tag 的多个来源（不同 mod 往同一 tag 追加）
                out.merge(ResourceLocation(rl.namespace, name), ids) { a, b -> (a + b).distinct() }
            }
        } catch (_: Exception) {}
    }

    /** 直接读 mods 目录 jar 里的 data/hexcasting/tags/action/ 下的 json（Connector 兜底） */
    private fun collectFromModsDir(out: MutableMap<ResourceLocation, List<String>>) {
        try {
            val modsDir = Platform.getGameFolder().resolve("mods")
            if (!Files.isDirectory(modsDir)) return
            Files.list(modsDir).use { stream ->
                stream.filter { it.toString().endsWith(".jar") }.forEach { jar ->
                    try {
                        ZipFile(jar.toFile()).use { zf ->
                            val entries = zf.entries()
                            while (entries.hasMoreElements()) {
                                val e = entries.nextElement()
                                if (e.isDirectory) continue
                                if (!e.name.startsWith("data/hexcasting/tags/action/")) continue
                                if (!e.name.endsWith(".json")) continue
                                val name = e.name.removePrefix("data/hexcasting/tags/action/").removeSuffix(".json")
                                zf.getInputStream(e).use { stream2 ->
                                    val json = JsonParser.parseReader(InputStreamReader(stream2)).asJsonObject
                                    val ids = json.getAsJsonArray("values").mapNotNull { el ->
                                        val s = el.asString
                                        if (s.startsWith("#")) null else s
                                    }
                                    if (ids.isNotEmpty()) {
                                        out.merge(ResourceLocation("hexcasting", name), ids) { a, b -> (a + b).distinct() }
                                    }
                                }
                            }
                        }
                    } catch (_: Exception) {}
                }
            }
        } catch (_: Exception) {}
    }
}
