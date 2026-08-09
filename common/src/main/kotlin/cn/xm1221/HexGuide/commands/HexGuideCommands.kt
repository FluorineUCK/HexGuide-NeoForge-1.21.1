package cn.xm1221.HexGuide.commands

import cn.xm1221.HexGuide.api.notes.NoteIota
import cn.xm1221.HexGuide.api.notes.PlayerNotes
import cn.xm1221.HexGuide.networking.handler.syncNotes
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.BoolArgumentType
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import dev.architectury.event.events.common.CommandRegistrationEvent
import dev.architectury.platform.Platform
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.commands.arguments.EntityArgument
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import java.nio.file.Files
import java.util.UUID

/**
 * /hexguide 指令：
 * - /hexguide export <索引>      把执行者第 <索引> 节笔记导出为 JSON（<gameDir>/hexguide/notes/export_<索引>.json）
 * - /hexguide import <文件名>    从 <gameDir>/hexguide/notes/<文件名>.json 导入一节笔记
 * - /hexguide authority <玩家> <true|false>   单独配置某玩家的 export/import 权限（OP 2 级；默认开）
 */
object HexGuideCommands {

    private val notesDir get() = Platform.getGameFolder().resolve("hexguide").resolve("notes")

    fun register() {
        CommandRegistrationEvent.EVENT.register { dispatcher: CommandDispatcher<CommandSourceStack>, _, _ ->
            dispatcher.register(
                Commands.literal("hexguide")
                    // export
                    .then(Commands.literal("export")
                        .requires { it.player != null }
                        .then(Commands.argument("index", IntegerArgumentType.integer(0))
                            .executes { ctx -> exportNote(ctx) }))
                    // import（文件名 Tab 联想 notes 目录里的 .json）
                    .then(Commands.literal("import")
                        .requires { it.player != null }
                        .then(Commands.argument("file", StringArgumentType.greedyString())
                            .suggests { _, builder ->
                                try {
                                    val dir = notesDir
                                    if (Files.isDirectory(dir)) {
                                        Files.list(dir).use { stream ->
                                            stream.filter { it.fileName.toString().endsWith(".json") }
                                                .forEach { builder.suggest(it.fileName.toString()) }
                                        }
                                    }
                                } catch (e: Exception) {}
                                builder.buildFuture()
                            }
                            .executes { ctx -> importNote(ctx) }))
                    // authority（OP 2）
                    .then(Commands.literal("authority")
                        .requires { it.hasPermission(2) }
                        .then(Commands.argument("player", EntityArgument.player())
                            .then(Commands.argument("allowed", BoolArgumentType.bool())
                                .executes { ctx -> setAuthority(ctx) })))
            )
        }
    }

    /** /hexguide export <索引>：导出第 <索引> 节为 JSON 文件 */
    private fun exportNote(ctx: CommandContext<CommandSourceStack>): Int {
        val player = ctx.source.player ?: return 0
        val index = IntegerArgumentType.getInteger(ctx, "index")
        val notes = PlayerNotes.get(player.serverLevel())
        if (!notes.isAllowed(player.uuid)) {
            ctx.source.sendFailure(Component.literal("你没有使用笔记指令的权限（由管理员用 /hexguide authority 配置）"))
            return 0
        }
        val secs = notes.sections(player.uuid)
        if (index !in secs.indices) {
            ctx.source.sendFailure(Component.literal("索引越界：你只有 ${secs.size} 节笔记（0..${secs.size - 1})"))
            return 0
        }
        return try {
            Files.createDirectories(notesDir)
            // 文件名 = 该节第一个 title（清洗非法字符）；无标题则回退 export_<索引>.json
            val firstTitle = secs[index].firstOrNull()?.title ?: ""
            val safe = sanitize(firstTitle)
            val fileName = if (safe.isEmpty()) "export_$index.json" else "$safe.json"
            val file = notesDir.resolve(fileName)
            Files.writeString(file, buildJson(secs[index]))
            ctx.source.sendSuccess({ Component.literal("已导出第 $index 节（${secs[index].size} 页）为 $fileName（hexguide/notes/ 目录）") }, false)
            1
        } catch (e: Exception) {
            ctx.source.sendFailure(Component.literal("导出失败：" + e.message))
            0
        }
    }

    /** /hexguide import <文件名>：从 JSON 文件导入一节笔记 */
    private fun importNote(ctx: CommandContext<CommandSourceStack>): Int {
        val player = ctx.source.player ?: return 0
        val name = StringArgumentType.getString(ctx, "file")
        val notes = PlayerNotes.get(player.serverLevel())
        if (!notes.isAllowed(player.uuid)) {
            ctx.source.sendFailure(Component.literal("你没有使用笔记指令的权限（由管理员用 /hexguide authority 配置）"))
            return 0
        }
        val file = notesDir.resolve(name)
        if (!Files.exists(file)) {
            ctx.source.sendFailure(Component.literal("找不到文件 " + file))
            return 0
        }
        val iotas = try {
            parseJson(Files.readString(file))
        } catch (e: Exception) {
            null
        }
        if (iotas == null || iotas.isEmpty()) {
            ctx.source.sendFailure(Component.literal("文件格式错误或没有页面：" + name))
            return 0
        }
        notes.newSection(player.uuid, iotas)
        syncNotes(player, notes)
        ctx.source.sendSuccess({ Component.literal("已导入 ${iotas.size} 页笔记（来自 $name）") }, false)
        return 1
    }

    /** /hexguide authority <玩家> <true|false>：单独配置某玩家权限 */
    private fun setAuthority(ctx: CommandContext<CommandSourceStack>): Int {
        val target = EntityArgument.getPlayer(ctx, "player")
        val allowed = BoolArgumentType.getBool(ctx, "allowed")
        val notes = PlayerNotes.get(target.serverLevel())
        notes.setAuthority(target.uuid, allowed)
        ctx.source.sendSuccess({
            Component.literal("已设置 ${target.name.string} 的笔记指令权限：${if (allowed) "允许" else "禁止"}（默认开）")
        }, true)
        return 1
    }

    // ---- JSON 序列化 ----

    /** 一节 → {"version":1,"pages":[{title,body,author,id,time},...]} */
    private fun buildJson(sec: List<NoteIota>): String {
        val pages = JsonArray()
        for (iota in sec) {
            val p = JsonObject()
            p.addProperty("title", iota.title)
            p.addProperty("body", iota.body)
            p.addProperty("author", iota.author)
            p.addProperty("id", iota.id)
            p.addProperty("time", iota.time)
            pages.add(p)
        }
        val root = JsonObject()
        root.addProperty("version", 1)
        root.add("pages", pages)
        // 格式化（带换行缩进），便于阅读/编辑
        return com.google.gson.GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(root)
    }

    /** 清洗文件名非法字符（Windows: \ / : * ? " < > |），并限制长度 */
    private fun sanitize(s: String): String =
        s.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim().take(64)

    /** JSON → NoteIota 列表 */
    private fun parseJson(json: String): List<NoteIota>? {
        val root = JsonParser.parseString(json).asJsonObject
        val pages = root.getAsJsonArray("pages")
        val now = System.currentTimeMillis()
        return pages.map { el ->
            val p = el.asJsonObject
            NoteIota(NoteIota.makeData(
                title = p.get("title")?.asString ?: "",
                body = p.get("body")?.asString ?: "",
                author = p.get("author")?.asString ?: "",
                id = p.get("id")?.asString ?: UUID.randomUUID().toString(),
                time = p.get("time")?.asLong ?: now,
            ))
        }
    }
}
