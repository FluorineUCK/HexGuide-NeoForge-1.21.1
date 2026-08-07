package cn.xm1221.HexGuide.client.screen

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.font.TextFieldHelper
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.resources.ResourceLocation
import org.lwjgl.glfw.GLFW

/**
 * 占位材质按钮：功能全部沿用原生 Button（点击/焦点/音效/叙述）。
 * 材质：hexguide:textures/gui/note_buttons.png（200×60，三态各 200×20：normal/hovered/disabled）。
 * 纹理缺失时回退纯色占位。
 */
class PlaceholderButton(
    x: Int, y: Int, w: Int, h: Int,
    msg: Component,
    onPress: Button.OnPress,
) : Button(x, y, w, h, msg, onPress, Button.DEFAULT_NARRATION) {

    override fun renderWidget(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTicks: Float) {
        val font = Minecraft.getInstance().font
        val hasTex = Minecraft.getInstance().resourceManager.getResource(BTN_TEX).isPresent
        if (hasTex) {
            // 三态：normal y=0 / hovered y=20 / disabled y=40（200×20 每态，拉伸到按钮尺寸）
            val v = if (!active) 40 else if (isHoveredOrFocused) 20 else 0
            graphics.blit(BTN_TEX, getX(), getY(), 0f, v.toFloat(), width, height, 200, 60)
        } else {
            // 占位背景（纯色；不画原版按钮材质）
            val bg = if (isHoveredOrFocused) 0xFF_555555.toInt() else 0xFF_3a3a3a.toInt()
            graphics.fill(getX(), getY(), getX() + width, getY() + height, bg)
            graphics.renderOutline(getX(), getY(), width, height, 0xFF_999999.toInt())
        }
        // 文字（居中）
        graphics.drawCenteredString(font, message, getX() + width / 2, getY() + (height - 8) / 2, 0xFF_FFFFFF.toInt())
    }

    companion object {
        /** 按钮纹理：assets/hexguide/textures/gui/note_buttons.png */
        val BTN_TEX = ResourceLocation("hexguide", "textures/gui/note_buttons.png")
    }
}

/**
 * 笔记编辑器（仿 MC 原版"书与笔" BookEditScreen）：
 * - 每页一段文本（TextFieldHelper 多行编辑：光标/选择/剪贴板/退格/方向键）
 * - 精确光标：getCursorPos() + 行/列定位（plainIndexAtWidth），光标常亮；点击文本区可按行列移动光标
 * - 正文用默认字体（与书页同字宽，每行字符数一致）+ 1.25× 放大渲染（视觉与书内文字接近）
 * - 文本区水平居中；翻页/保存/取消用【占位材质按钮】（PlaceholderButton）
 * - 点击文本区外取消选中；回车换行（keyPressed + charTyped 双路）
 * - 背景材质【留空】（仅纯色占位矩形，方便 modpack 绘制替换）
 */
class NoteEditorScreen(
    private val playerName: String,
    private val initialTitle: String = "",
    initialPages: List<String> = emptyList(),
    private val onSave: (title: String, pages: List<String>) -> Unit,
) : net.minecraft.client.gui.screens.Screen(Component.translatable("hexguide.notes.editor_title")) {

    private val pages: MutableList<String> = initialPages.toMutableList()
    private var currPage: Int = 0
    private var titleBox: EditBox? = null
    private lateinit var helper: TextFieldHelper

    /** 文本区水平居中 X（按视觉宽度） */
    private var editX: Int = 0
    private var editY: Int = 40

    /** 文本区视觉尺寸（逻辑尺寸 × SCALE） */
    private val editVW: Int get() = (EDIT_WIDTH * SCALE).toInt()
    private val editVH: Int get() = (EDIT_HEIGHT * SCALE).toInt()

    override fun init() {
        if (pages.isEmpty()) pages.add("")
        if (currPage >= pages.size) currPage = pages.size - 1
        editX = width / 2 - editVW / 2

        // 标题（单行，居中；点击可聚焦输入）
        titleBox = EditBox(font, width / 2 - 70, 18, 140, 16, Component.translatable("hexguide.notes.title")).also {
            it.setMaxLength(64)
            it.setValue(initialTitle)
            addWidget(it)
        }

        // 文本编辑助手（getter/setter 读写当前页；剪贴板用 MC 键盘管理器）
        helper = TextFieldHelper(
            { pages[currPage] },
            { s -> pages[currPage] = s },
            { Minecraft.getInstance().keyboardHandler.clipboard },
            { s -> Minecraft.getInstance().keyboardHandler.clipboard = s },
            { s -> s.length <= 2048 },
        )

        // 按钮：文本区正下方（原生 Button 功能 + 占位材质）
        val btnY = editY + editVH + 8
        addRenderableWidget(PlaceholderButton(editX - 80, btnY, 60, 16, Component.translatable("hexguide.notes.prev_page"), Button.OnPress { prevPage() }))
        addRenderableWidget(PlaceholderButton(editX + editVW + 20, btnY, 60, 16, Component.translatable("hexguide.notes.next_page"), Button.OnPress { nextPage() }))
        addRenderableWidget(PlaceholderButton(width / 2 - 65, btnY + 22, 60, 16, Component.translatable("hexguide.notes.save"), Button.OnPress { saveAndClose() }))
        addRenderableWidget(PlaceholderButton(width / 2 + 5, btnY + 22, 60, 16, Component.translatable("hexguide.notes.cancel"), Button.OnPress { onClose() }))
    }

    // ---- 页面操作 ----

    private fun prevPage() {
        if (currPage > 0) {
            currPage--
            if (currPage < pages.size - 1 && pages[currPage].isBlank()) {
                pages.removeAt(currPage)
            }
            helper.setCursorPos(0)
            helper.setSelectionPos(0)
        }
    }

    private fun nextPage() {
        if (pages[currPage].isNotBlank() && currPage == pages.size - 1) {
            pages.add("")
        }
        if (currPage < pages.size - 1) {
            currPage++
            helper.setCursorPos(0)
            helper.setSelectionPos(0)
        }
    }

    private fun saveAndClose() {
        val title = titleBox?.value?.trim().orEmpty()
        val nonEmpty = pages.map { it.trimEnd() }.filter { it.isNotBlank() }
        if (nonEmpty.isEmpty()) {
            onClose()
            return
        }
        onSave(title, nonEmpty)
        onClose()
    }

    // ---- 键盘 ----

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (getFocused() === titleBox) { // Screen 级焦点在标题框 → 转发给标题框
            return super.keyPressed(keyCode, scanCode, modifiers)
        }
        when (keyCode) {
            GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                helper.insertText("\n")
                return true
            }
            GLFW.GLFW_KEY_PAGE_UP -> { prevPage(); return true }
            GLFW.GLFW_KEY_PAGE_DOWN -> { nextPage(); return true }
        }
        if (helper.keyPressed(keyCode)) return true
        return super.keyPressed(keyCode, scanCode, modifiers)
    }

    override fun charTyped(code: Char, modifiers: Int): Boolean {
        if (getFocused() === titleBox) return super.charTyped(code, modifiers)
        // 某些环境（输入法/特殊键盘）Enter 走 charTyped 而非 keyPressed —— 这里补上换行
        if (code == '\n' || code == '\r') {
            helper.insertText("\n")
            return true
        }
        if (helper.charTyped(code)) return true
        return super.charTyped(code, modifiers)
    }

    // ---- 鼠标 ----

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button == 0) {
            val tb = titleBox
            // 标题框优先：点击 → widget 聚焦 + Screen 级焦点（否则键盘不转发给标题框）
            if (tb != null && tb.isMouseOver(mouseX, mouseY)) {
                tb.mouseClicked(mouseX, mouseY, button)
                setFocused(tb)
                return true
            }
            // 点击文本区：换算回逻辑坐标后按 (行, 列) 精确移动光标
            if (isInEditArea(mouseX, mouseY)) {
                setFocused(null) // 键盘回到文本区
                val lx = ((mouseX - editX) / SCALE).toInt().coerceAtLeast(0)
                val ly = ((mouseY - editY) / SCALE).toInt().coerceAtLeast(0)
                val row = (ly / LINE_HEIGHT).toInt()
                val text = pages[currPage]
                val lines = wrapLines(text)
                val lineText = lines.getOrNull(row) ?: ""
                val col = font.splitter.plainIndexAtWidth(lineText, lx, Style.EMPTY)
                val pos = (rowToTextOffset(lines, row) + col).coerceAtMost(text.length)
                helper.setCursorPos(pos)
                helper.setSelectionPos(pos) // 取消选中
                return true
            }
            // 点击文本区外：取消选中 + 键盘回文本区
            val c = helper.getCursorPos()
            helper.setSelectionPos(c)
            setFocused(null)
        }
        return super.mouseClicked(mouseX, mouseY, button)
    }

    /** 文本区视觉区域判定 */
    private fun isInEditArea(mx: Double, my: Double): Boolean {
        return mx >= editX && mx < editX + editVW && my >= editY && my < editY + editVH
    }

    // ---- 渲染 ----

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTicks: Float) {
        // 背景：hexguide:textures/gui/note_editor.png（640×360，1:1 对应 GUI 逻辑坐标，拉伸到屏幕；缺失时回退默认背景）
        if (Minecraft.getInstance().resourceManager.getResource(BG_TEX).isPresent) {
            graphics.blit(BG_TEX, 0, 0, 0f, 0f, width, height, 640, 360)
        } else {
            renderBackground(graphics) // Screen 默认背景
        }
        titleBox?.render(graphics, mouseX, mouseY, partialTicks)
        graphics.drawCenteredString(font, Component.translatable("hexguide.notes.title"), width / 2, 4, 0xFF777777.toInt())

        // 文本区占位背景（视觉尺寸；若背景纹理已画好文本区可自行删掉这层）
        graphics.fill(editX - 3, editY - 3, editX + editVW + 3, editY + editVH + 3, 0x33_3a3a3a.toInt())

        val text = pages[currPage]
        val lines = wrapLines(text)

        // 每页最多 MAX_LINES 行，超出滚动（显示最后 MAX_LINES 行）
        val start = (lines.size - MAX_LINES).coerceAtLeast(0)

        // 正文 + 光标：uniform 字体 + SCALE 放大渲染（逻辑坐标绘制，整体放大）
        graphics.pose().pushPose()
        graphics.pose().translate(editX.toFloat(), editY.toFloat(), 0f)
        graphics.pose().scale(SCALE, SCALE, 1f)
        val uf = font
        for (i in start until lines.size) {
            val y = (i - start) * LINE_HEIGHT
            graphics.drawString(uf, lines[i], 0, y, 0xFFE0E0E0.toInt(), false)
        }
        // 光标（常亮）
        val cursor = helper.getCursorPos().coerceIn(0, text.length)
        val (row, col) = cursorToRowCol(lines, cursor)
        val visRow = row - start
        if (visRow in 0 until MAX_LINES) {
            val lineText = lines.getOrNull(row) ?: ""
            val caretX = uf.width(lineText.substring(0, col.coerceIn(0, lineText.length)))
            val caretY = visRow * LINE_HEIGHT
            graphics.fill(caretX, caretY, caretX + 1, caretY + LINE_HEIGHT, 0xFFFFFFFF.toInt())
        }
        graphics.pose().popPose()

        // 页码（文本区下方，居中）
        val pageText = Component.translatable("hexguide.notes.page_indicator", currPage + 1, pages.size).string
        graphics.drawString(font, pageText, width / 2 - font.width(pageText) / 2, editY + editVH + 46, 0xFF777777.toInt(), false)

        // 关键：渲染 renderables（占位按钮等）——不调 super 则按钮不显示
        super.render(graphics, mouseX, mouseY, partialTicks)
    }

    // ---- 行拆分与光标定位 ----

    /** 按宽度 + 换行符拆行（与渲染/光标同一套逻辑，宽度用 uniform 字体），返回每行文本。
     *  以 \n 结尾时末尾补一个空行——否则光标定位（cursorToRowCol）无法落到"换行后的新行"行首。 */
    private fun wrapLines(text: String): List<String> {
        if (text.isEmpty()) return listOf("")
        val splitter = font.splitter
        val out = mutableListOf<String>()
        var remaining = text
        while (remaining.isNotEmpty()) {
            val nl = remaining.indexOf('\n')
            val limit = splitter.plainIndexAtWidth(remaining, EDIT_WIDTH, Style.EMPTY)
            val end = if (nl in 0 until limit) nl else limit.coerceAtMost(remaining.length)
            out.add(remaining.substring(0, end))
            val consumed = if (nl in 0 until limit) end + 1 else end
            remaining = remaining.substring(consumed)
            // 已处理完且原文本以换行符结尾 → 追加空行（Enter 后光标停在新行行首）
            if (remaining.isEmpty() && consumed > 0 && text.endsWith('\n')) {
                out.add("")
            }
        }
        return out
    }

    /** 行 → 该行行首的字符偏移 */
    private fun rowToTextOffset(lines: List<String>, row: Int): Int {
        var offset = 0
        for (i in 0 until row.coerceAtMost(lines.size - 1)) {
            offset += lines[i].length + 1 // +1 换行符
        }
        return offset
    }

    /** 字符偏移 → (行, 列)，基于 wrapLines 的行拆分 */
    private fun cursorToRowCol(lines: List<String>, cursor: Int): Pair<Int, Int> {
        var remaining = cursor
        for ((i, line) in lines.withIndex()) {
            val len = line.length
            if (remaining <= len) return i to remaining
            remaining -= len + 1 // +1 换行符
        }
        return (lines.size - 1).coerceAtLeast(0) to remaining.coerceAtLeast(0)
    }

    companion object {
        /** 背景纹理：assets/hexguide/textures/gui/note_editor.png（640×360，1:1 对应 GUI 逻辑坐标） */
        val BG_TEX = ResourceLocation("hexguide", "textures/gui/note_editor.png")

        /** 正文放大倍数（视觉与咒术笔记书内文字接近） */
        const val SCALE = 1.25f
        const val MAX_LINES = 14
        const val LINE_HEIGHT = 9
        /** 逻辑宽度 = 书页宽（GuiBook.PAGE_WIDTH=116）——编辑器每行与书页等宽，所见即所得 */
        const val EDIT_WIDTH = 116
        const val EDIT_HEIGHT = MAX_LINES * LINE_HEIGHT
    }
}
