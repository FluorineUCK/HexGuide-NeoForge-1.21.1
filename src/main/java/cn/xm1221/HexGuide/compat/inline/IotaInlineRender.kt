package cn.xm1221.HexGuide.compat.inline

import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.iota.ListIota
import at.petrak.hexcasting.api.casting.iota.PatternIota
import cn.xm1221.HexGuide.HexGuide
import com.mojang.blaze3d.vertex.PoseStack
import com.samsthenerd.inline.api.client.InlineRenderer
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.resources.ResourceLocation

class IotaInlineRender : InlineRenderer<IotaInlineData> {

    /** 强制换行时每行最大宽度（px），书页正文可用宽度内 */
    private val WRAP_WIDTH = 100

    override fun getId(): ResourceLocation? =
        ResourceLocation.tryBuild(HexGuide.MODID, "iota")

    override fun render(
        data: IotaInlineData?,
        context: GuiGraphics?,
        index: Int,
        style: Style?,
        codepoint: Int,
        trContext: InlineRenderer.TextRenderingContext?
    ): Int {
        // 所有参数在正常调用时均非空，但为安全做防御
        val d = data ?: return 0
        val ctx = context ?: return 0
        val tr = trContext ?: return 0
        val font = Minecraft.getInstance().font

        // 1. 获取要显示的 Component（已包含 Iota 的显示样式）
        val component = d.asText(false)  // false 或 true 均可，asText 未使用该参数

        // 2. 颜色：*b / *w / *RRGGBB 强制色；否则继承当前文本样式颜色
        //    （trContext.usableColor 与 HexMod 的 InlinePatternRenderer 一致，书页/tooltip 自适应）
        val color = if (d.getForcedColor() != -1) d.getForcedColor()
                    else tr.usableColor()

        // 3. **n 强制换行：ListIota 遍历 Iota 对象逐元素布局换行；非列表单行渲染
        if (d.isWrapNewline()) {
            val iota = d.getOrDeserialize()
            if (iota is ListIota) {
                return renderListIota(font, context, iota, color)
            }
            context.drawString(font, component, 0, 0, color)
            return font.width(component.visualOrderText)
        }

        // 4. 常规渲染（颜色参数只在样式无颜色时生效）
        context.drawString(Minecraft.getInstance().font, component, 0, 0, color)
        // 5. 返回渲染宽度（必须与 charWidth 一致）
        return Minecraft.getInstance().font.width(component.visualOrderText)
    }

    override fun charWidth(
        data: IotaInlineData?,
        style: Style?,
        codepoint: Int
    ): Int {
        // 使用同样的方法计算宽度
        val d = data ?: return 0
        val font = Minecraft.getInstance().font
        if (d.isWrapNewline()) {
            val iota = d.getOrDeserialize()
            if (iota is ListIota) {
                val lines = layoutListLines(font, iota)
                if (lines.isEmpty()) return font.width("[]") // 空列表
                return lines.mapIndexed { li, line ->
                    var w = 0
                    if (li == 0) w += font.width("[")        // 首行括号
                    for (c in line) w += font.width(c)
                    if (li == lines.size - 1) w += font.width("]") // 末行括号
                    w
                }.maxOrNull() ?: 0
            }
        }
        val component = d.asText(false)
        return font.width(component.visualOrderText)
    }

    /**
     * 渲染 ListIota：遍历 Iota 对象逐元素布局换行。
     * 遵循 HexMod ListIota.display() 的规则：
     * - 逗号只在相邻元素任一非 PatternIota 时添加（图案之间无逗号）
     * - 首尾保留 [ ]（hexcasting.tooltip.list_contents = "[%s]"），括号用列表色暗紫
     */
    private fun renderListIota(font: Font, context: GuiGraphics, listIota: ListIota, color: Int): Int {
        val lines = layoutListLines(font, listIota)
        // 括号/空列表用 HexMod ListIota.TYPE.color()（0xffaa00aa 暗紫），元素保持各自 display 颜色
        val listColor = 0xFFAA00AA.toInt()
        if (lines.isEmpty()) {
            // 空列表 []
            context.drawString(font, "[]", 0, 0, listColor)
            return font.width("[]")
        }
        var y = 0
        var maxW = 0
        for ((li, line) in lines.withIndex()) {
            var x = 0
            if (li == 0) {
                context.drawString(font, "[", x, y, listColor)
                x += font.width("[")
            }
            for (c in line) {
                context.drawString(font, c, x, y, color)
                x += font.width(c)
            }
            if (li == lines.size - 1) {
                context.drawString(font, "]", x, y, listColor)
                x += font.width("]")
            }
            maxW = maxOf(maxW, x)
            y += font.lineHeight
        }
        return maxW
    }

    /** 把 ListIota 的元素按宽度布局成多行；元素尾随逗号（相邻任一 usesListCommas 才加） */
    private fun layoutListLines(font: Font, listIota: ListIota): List<List<Component>> {
        val iotas = ArrayList<Iota>()
        for (el in listIota.getList()) iotas.add(el) // SpellList → 常规列表
        if (iotas.isEmpty()) return emptyList()
        // 每个元素 + 尾部逗号：与 HexMod 0.11.3 display() 一致——相邻任一非 PatternIota 则加逗号
        // （0.11.3 用 getTypeFromTag != PatternIota.TYPE 判断，图案之间无逗号）
        val units = ArrayList<Component>()
        for (i in iotas.indices) {
            val c = iotas[i].display()
            val needComma = i < iotas.size - 1 &&
                (iotas[i].getType() !== PatternIota.TYPE || iotas[i + 1].getType() !== PatternIota.TYPE)
            units.add(if (needComma) c.copy().append(", ") else c)
        }
        // 按宽度布局：每行塞尽量多的元素，超宽在元素边界换行
        val lines = ArrayList<List<Component>>()
        val cur = ArrayList<Component>()
        var w = 0
        for (u in units) {
            val uw = font.width(u)
            if (cur.isNotEmpty() && w + uw > WRAP_WIDTH) {
                lines.add(ArrayList(cur))
                cur.clear()
                w = 0
            }
            cur.add(u)
            w += uw
        }
        if (cur.isNotEmpty()) lines.add(ArrayList(cur))
        return lines
    }
}
