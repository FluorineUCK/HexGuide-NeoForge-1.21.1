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
import dev.architectury.networking.NetworkManager.PacketContext
import cn.xm1221.HexGuide.networking.msg.*
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand

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

        // 读取演示配置 data/<ns>/spellplays/<name>.json（服务端数据包资源）并传回客户端
        is MsgBookLoadSpellplayC2S -> {
            val player = ctx.player as? ServerPlayer ?: return@queue
            val rl = ResourceLocation(ns, "spellplays/$name.json")
            try {
                val opt = player.server.resourceManager.getResource(rl)
                if (opt.isPresent) {
                    val json = opt.get().open().use { it.readBytes().toString(Charsets.UTF_8) }
                    MsgBookLoadSpellplayS2C(ns, name, json).sendToPlayer(player)
                } else {
                    MsgBookLoadSpellplayS2C(ns, name, null).sendToPlayer(player)
                }
            } catch (e: Exception) {
                MsgBookLoadSpellplayS2C(ns, name, null).sendToPlayer(player)
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
