package cn.xm1221.HexGuide.api.notes

import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.iota.IotaType
import net.minecraft.ChatFormatting
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel

/**
 * 玩家笔记 Iota：一页笔记 = 一个 NoteIota。
 * payload 是 CompoundTag：{ title, body, author, id, time }。
 * 可作为一等公民 iota 入栈、内联渲染（iota:xxx）、悬停查看、跨玩家交换。
 */
class NoteIota(data: CompoundTag) : Iota(TYPE, data) {

    val data: CompoundTag
        get() = payload as CompoundTag

    val title: String get() = data.getString(KEY_TITLE)
    val body: String get() = data.getString(KEY_BODY)
    val author: String get() = data.getString(KEY_AUTHOR)
    val id: String get() = data.getString(KEY_ID)
    val time: Long get() = data.getLong(KEY_TIME)

    override fun isTruthy(): Boolean = body.isNotEmpty() || title.isNotEmpty()

    override fun toleratesOther(that: Iota): Boolean = typesMatch(this, that) && that is NoteIota

    override fun serialize(): Tag = data.copy()

    fun make(title: String, body: String,author: String,id: String,time: Long): NoteIota {
        return NoteIota(makeData(title, body, author, id, time))
    }

    companion object {
        const val KEY_TITLE = "title"
        const val KEY_BODY = "body"
        const val KEY_AUTHOR = "author"
        const val KEY_ID = "id"
        const val KEY_TIME = "time"

        /** 构造 payload CompoundTag */
        fun makeData(title: String, body: String, author: String, id: String, time: Long): CompoundTag {
            val c = CompoundTag()
            c.putString(KEY_TITLE, title)
            c.putString(KEY_BODY, body)
            c.putString(KEY_AUTHOR, author)
            c.putString(KEY_ID, id)
            c.putLong(KEY_TIME, time)
            return c
        }

        val TYPE: IotaType<NoteIota> = object : IotaType<NoteIota>() {
            override fun deserialize(tag: Tag, world: ServerLevel?): NoteIota? {
                if (tag is CompoundTag) return NoteIota(tag.copy())
                return null
            }

            override fun display(tag: Tag): Component {
                if (tag is CompoundTag) {
                    val title = tag.getString(KEY_TITLE)
                    val author = tag.getString(KEY_AUTHOR)
                    if (title.isNotEmpty()) {
                        val t: Component = Component.literal(title).withStyle(ChatFormatting.AQUA)
                        return if (author.isEmpty()) t
                        else t.copy().append(Component.literal(" — $author").withStyle(ChatFormatting.GRAY))
                    }
                }
                return Component.translatable("hexguide.notes.unnamed").withStyle(ChatFormatting.GRAY)
            }

            override fun color(): Int = 0xff_55ffff.toInt()
        }
    }
}
