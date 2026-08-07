package cn.xm1221.HexGuide.networking.handler

import at.petrak.hexcasting.api.casting.eval.vm.CastingImage
import at.petrak.hexcasting.api.casting.eval.vm.CastingVM
import at.petrak.hexcasting.api.casting.iota.IotaType
import at.petrak.hexcasting.api.casting.iota.PatternIota
import at.petrak.hexcasting.api.casting.math.HexDir
import at.petrak.hexcasting.api.casting.math.HexPattern
import at.petrak.hexcasting.api.mod.HexTags
import at.petrak.hexcasting.common.lib.hex.HexActions
import at.petrak.hexcasting.xplat.IXplatAbstractions
import cn.xm1221.HexGuide.HexGuide
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import dev.architectury.networking.NetworkManager.PacketContext
import cn.xm1221.HexGuide.networking.msg.*
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand

/**
 * 读取全局 pattern_vector 默认 origin 表：
 * data/hexguide/pattern_vector/ 目录下所有 .json，按文件名顺序加载并合并（后加载覆盖先加载）。
 * 每个文件可记录多个 action：{"<action id>": {"origin": [q, r]}, ...}
 */
private fun readPatternVector(player: ServerPlayer): String? {
    return try {
        val rm = player.server.resourceManager
        // 诊断：hexguide 命名空间全部资源（不过滤），确认 pattern_vector 是否在资源管理器里
        val all = rm.listResources(HexGuide.MODID) { true }.keys.map { it.path }
        HexGuide.LOGGER.info("[Spellplay] hexguide 命名空间全部资源({}): {}", all.size, all)
        val direct = rm.getResource(ResourceLocation(HexGuide.MODID, "pattern_vector/a_hexguide.json"))
        HexGuide.LOGGER.info("[Spellplay] 直接 getResource pattern_vector/a_hexguide.json: {}", direct.isPresent)
        val merged = JsonObject()
        val files = rm
            .listResources(HexGuide.MODID) { it.path.startsWith("pattern_vector/") && it.path.endsWith(".json") }
            .keys
            .sortedBy { it.path } // 按文件名顺序：后加载覆盖先加载的同名 action
        HexGuide.LOGGER.info("[Spellplay] pattern_vector 扫描到 {} 个文件: {}", files.size, files.map { it.path })
        for (rl in files) {
            val json = rm.getResource(rl)
                .map { it.open().use { r -> r.readBytes().toString(Charsets.UTF_8) } }
                .orElse(null) ?: continue
            try {
                val obj = JsonParser.parseString(json).asJsonObject
                for ((k, v) in obj.entrySet()) merged.add(k, v)
                HexGuide.LOGGER.info("[Spellplay] pattern_vector 文件 {}: {}", rl.path, json)
            } catch (e: Exception) {
                HexGuide.LOGGER.warn("[Spellplay] 解析 pattern_vector 文件失败: {}", rl, e)
            }
        }
        HexGuide.LOGGER.info("[Spellplay] pattern_vector 合并结果: {}", if (merged.size() > 0) merged.toString() else "(空)")
        if (merged.size() > 0) merged.toString() else null
    } catch (e: Exception) {
        HexGuide.LOGGER.warn("[Spellplay] readPatternVector 异常", e)
        null
    }
}

fun HexGuideMessageC2S.applyOnServer(ctx: PacketContext) = ctx.queue {
    when (this) {
        // 把写模式记录的图案推入法杖栈（不执行），让法杖栈与本地栈同步
        is MsgBookSyncStackC2S -> {
            val player = ctx.player as? ServerPlayer ?: return@queue
            val vm = IXplatAbstractions.INSTANCE.getStaffcastVM(player, InteractionHand.MAIN_HAND)
            val newStack = ArrayList(vm.image.stack)
            for (tag in patterns) {
                try {
                    newStack.add(IotaType.deserialize(tag, player.serverLevel()) ?: continue)
                } catch (e: Exception) {
                    // 忽略无法解析的图案
                }
            }
            IXplatAbstractions.INSTANCE.setStaffcastImage(player, vm.image.copy(stack = newStack))
        }

        // 演示"真执行"：上传的本地 CastingImage + 图案 → 新 VM 运行（不碰玩家法杖栈）→ 结果传回
        is MsgBookExecDemoC2S -> {
            val player = ctx.player as? ServerPlayer ?: return@queue
            val world = player.serverLevel()
            val image = try {
                CastingImage.loadFromNbt(image, world)
            } catch (e: Exception) {
                CastingImage()
            }
            val pattern = try {
                HexPattern.fromAngles(sig, HexDir.valueOf(startDir))
            } catch (e: Exception) {
                return@queue
            }
            val env = DemoCastEnv(player, InteractionHand.MAIN_HAND)
            val vm = CastingVM(image, env)
            val result = vm.queueExecuteAndWrapIota(PatternIota(pattern), world)
            MsgBookExecDemoS2C(vm.image.serializeToNbt(), result.resolutionType.name)
                .sendToPlayer(player)
        }

        // 读取演示配置 data/<ns>/spellplays/<name>.json + 全局 pattern_vector（默认 origin 表）并传回客户端
        is MsgBookLoadSpellplayC2S -> {
            val player = ctx.player as? ServerPlayer ?: return@queue
            val rl = ResourceLocation(ns, "spellplays/$name.json")
            try {
                val opt = player.server.resourceManager.getResource(rl)
                val pvJson = readPatternVector(player)
                if (opt.isPresent) {
                    val json = opt.get().open().use { it.readBytes().toString(Charsets.UTF_8) }
                    MsgBookLoadSpellplayS2C(ns, name, json, pvJson).sendToPlayer(player)
                } else {
                    MsgBookLoadSpellplayS2C(ns, name, null, pvJson).sendToPlayer(player)
                }
            } catch (e: Exception) {
                MsgBookLoadSpellplayS2C(ns, name, null, null).sendToPlayer(player)
            }
        }

        // 返回创造标签页应排除的图案 id（服务端 tag 数据完整；客户端 Forge 静态 registry 查不到 tag）
        is MsgRequestExcludedPatternsC2S -> {
            val player = ctx.player as? ServerPlayer ?: return@queue
            // 用 registry.getTag 而非 getHolder(key).is(tag)：getHolder 会创建 STAND_ALONE holder，
            // 而 bindTags 跳过 STAND_ALONE，导致 holder 无 tag 且污染注册表
            val greatTag = HexActions.REGISTRY.getTag(HexTags.Actions.REQUIRES_ENLIGHTENMENT)
            val perWorldTag = HexActions.REGISTRY.getTag(HexTags.Actions.PER_WORLD_PATTERN)
            val excluded = HexActions.REGISTRY.entrySet()
                .filter { (key, _) ->
                    val inGreat = greatTag.isPresent && greatTag.get().stream().anyMatch { it.`is`(key) }
                    val inPerWorld = perWorldTag.isPresent && perWorldTag.get().stream().anyMatch { it.`is`(key) }
                    inGreat || inPerWorld
                }
                .map { (key, _) -> key.location().toString() }
            MsgExcludedPatternsS2C(excluded).sendToPlayer(player)
        }

        else -> {}
    }
}
