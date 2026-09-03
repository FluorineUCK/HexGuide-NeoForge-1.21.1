package cn.xm1221.HexGuide.api.notes

import cn.xm1221.HexGuide.hexcompat.deserializeIotaCompat
import cn.xm1221.HexGuide.hexcompat.serializeIota
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.Tag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.saveddata.SavedData
import java.util.UUID

class PlayerNotes : SavedData() {
    private val notes: MutableMap<UUID, MutableList<MutableList<NoteIota>>> = mutableMapOf()
    private val authority: MutableMap<UUID, Boolean> = mutableMapOf()

    fun setAuthority(uuid: UUID, allowed: Boolean) {
        authority[uuid] = allowed
        setDirty()
    }

    fun isAllowed(uuid: UUID): Boolean = authority[uuid] ?: true
    fun sections(uuid: UUID): MutableList<MutableList<NoteIota>> =
        notes.getOrPut(uuid) { mutableListOf() }

    fun appendIota(uuid: UUID, sectionIndex: Int, iota: NoteIota) {
        val sections = sections(uuid)
        if (sectionIndex in sections.indices) {
            sections[sectionIndex].add(iota)
            setDirty()
        }
    }

    fun newSection(uuid: UUID, iotas: List<NoteIota>) {
        sections(uuid).add(iotas.toMutableList())
        setDirty()
    }

    fun removeSection(uuid: UUID, index: Int): Boolean {
        val sections = sections(uuid)
        if (index !in sections.indices) return false
        sections.removeAt(index)
        setDirty()
        return true
    }

    override fun save(tag: CompoundTag, registries: HolderLookup.Provider): CompoundTag {
        val players = ListTag()
        for ((uuid, sections) in notes) {
            val player = CompoundTag()
            player.putUUID("uuid", uuid)
            val sectionsTag = ListTag()
            for (section in sections) {
                val sectionTag = ListTag()
                section.forEach { sectionTag.add(serializeIota(it)) }
                sectionsTag.add(sectionTag)
            }
            player.put("sections", sectionsTag)
            players.add(player)
        }
        tag.put("players", players)

        val authTag = ListTag()
        for ((uuid, allowed) in authority) {
            authTag.add(CompoundTag().also {
                it.putUUID("uuid", uuid)
                it.putBoolean("allowed", allowed)
            })
        }
        tag.put("authority", authTag)
        return tag
    }

    private fun readFrom(tag: CompoundTag) {
        notes.clear()
        authority.clear()
        val players = tag.getList("players", Tag.TAG_COMPOUND.toInt())
        for (i in 0 until players.size) {
            val player = players.getCompound(i)
            if (!player.hasUUID("uuid")) continue
            val sections = mutableListOf<MutableList<NoteIota>>()
            val sectionsTag = player.getList("sections", Tag.TAG_LIST.toInt())
            for (j in 0 until sectionsTag.size) {
                val section = mutableListOf<NoteIota>()
                val sectionTag = sectionsTag.getList(j)
                for (k in 0 until sectionTag.size) {
                    (deserializeIotaCompat(sectionTag.get(k)) as? NoteIota)?.let(section::add)
                }
                sections.add(section)
            }
            notes[player.getUUID("uuid")] = sections
        }

        val authTag = tag.getList("authority", Tag.TAG_COMPOUND.toInt())
        for (i in 0 until authTag.size) {
            val auth = authTag.getCompound(i)
            if (auth.hasUUID("uuid")) authority[auth.getUUID("uuid")] = auth.getBoolean("allowed")
        }
    }

    companion object {
        const val NAME = "hexguide_notes"

        private val FACTORY = SavedData.Factory(
            ::PlayerNotes,
            { tag: CompoundTag, _: HolderLookup.Provider -> PlayerNotes().also { it.readFrom(tag) } },
            null,
        )

        fun get(level: ServerLevel): PlayerNotes =
            level.dataStorage.computeIfAbsent(FACTORY, NAME)
    }
}
