package cn.xm1221.HexGuide.compat.inline

import cn.xm1221.HexGuide.HexGuide
import com.mojang.blaze3d.vertex.PoseStack
import com.samsthenerd.inline.api.client.InlineRenderer
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Style
import net.minecraft.resources.ResourceLocation

class IotaInlineRender : InlineRenderer<IotaInlineData> {

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

        // 1. 获取要显示的 Component（已包含 Iota 的显示样式）
        val component = d.asText(false)  // false 或 true 均可，asText 未使用该参数

        // 2. 获得格式化的渲染序列（保留样式）
        val seq = component.visualOrderText
        context.drawString(Minecraft.getInstance().font,component,0,0,0)
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
        val component = d.asText(false)
        return Minecraft.getInstance().font.width(component.visualOrderText)
    }
}