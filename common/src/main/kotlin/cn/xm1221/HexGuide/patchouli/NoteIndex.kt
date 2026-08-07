package cn.xm1221.HexGuide.patchouli

import cn.xm1221.HexGuide.api.notes.ClientNotes
import cn.xm1221.HexGuide.api.notes.NoteIota
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import vazkii.patchouli.client.book.BookPage
import vazkii.patchouli.client.book.gui.GuiBook
import vazkii.patchouli.client.book.gui.GuiBookEntry

/**
 * 笔记目录页（hexguide:note_index）。
 * 列出当前玩家所有节（笔记）：标题 + 页数；点击某节 → 设为当前节并跳转到显示条目。
 * 新建笔记用【紫水晶笔】（副手纸右键）。
 */
class NoteIndex : BookPage() {
    @Transient
    private var sections: List<List<NoteIota>> = emptyList()

    override fun onDisplayed(parent: GuiBookEntry, left: Int, top: Int) {
        super.onDisplayed(parent, left, top)
        sections = ClientNotes.sections()
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, pticks: Float) {
        val font = Minecraft.getInstance().font

        if (sections.isEmpty()) {
            graphics.drawString(font, "还没有笔记", 4, LIST_Y, META_COLOR, false)
        } else {
            // 只渲染页面可视范围内的行（超出页面部分不画，避免溢出/判定错位）
            val visible = minOf(sections.size, MAX_ROWS)
            var y = LIST_Y
            for (i in 0 until visible) {
                val sec = sections[i]
                val title = sec.firstOrNull()?.title?.ifEmpty { "（无标题）" } ?: "（空节）"
                val pages = sec.size
                // 行文字限宽（避免溢出页面右侧）
                val full = "▸ $title  [$pages]"
                val text = if (font.width(full) > GuiBook.PAGE_WIDTH - 8)
                    font.splitter.plainHeadByWidth(full, GuiBook.PAGE_WIDTH - 16, Style.EMPTY) + "…"
                else full
                val color = if (i == ClientNotes.currentSection) HIGHLIGHT_COLOR else ROW_COLOR
                graphics.drawString(font, text, 4, y, color, false)
                y += ROW_H
            }
            // 超出部分提示
            if (sections.size > MAX_ROWS) {
                graphics.drawString(font, "… ${sections.size - MAX_ROWS} 条更多", 4, LIST_Y + MAX_ROWS * ROW_H, META_COLOR, false)
            }
        }
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, mouseButton: Int): Boolean {
        if (mouseButton != 0) return false
        // 照抄 BookSearchComponent 的判定：用 isAreaHovered 逐行命中（渲染坐标与判定坐标一致）
        val visible = minOf(sections.size, MAX_ROWS)
        for (i in 0 until visible) {
            if (parent.isAreaHovered(mouseX.toInt(), mouseY.toInt(), ROW_X, LIST_Y + i * ROW_H, GuiBook.PAGE_WIDTH - ROW_X * 2, ROW_H)) {
                ClientNotes.setCurrent(i)
                // 跳到显示条目（替换渲染的 NoteIota 列表；动态找 entry id）
                val notesEntry = parent.book.contents.entries.values.firstOrNull { it.getId().path == "guide/notes" }
                if (notesEntry != null) {
                    parent.navigateToEntry(notesEntry.getId(), 0, false)
                    return true
                }
            }
        }
        return false
    }

    companion object {
        const val ROW_X = 4
        const val LIST_Y = 10
        const val ROW_H = 12
        /** 页面内最多显示的行数（PAGE_HEIGHT≈156，留底部提示空间） */
        const val MAX_ROWS = (GuiBook.PAGE_HEIGHT - LIST_Y - 12) / ROW_H
        const val ROW_COLOR = 0xFF0044AA.toInt()
        const val HIGHLIGHT_COLOR = 0xFF00AA77.toInt()
        const val META_COLOR = 0xFF777777.toInt()
    }
}
