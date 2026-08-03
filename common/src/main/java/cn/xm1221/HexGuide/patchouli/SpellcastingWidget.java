package cn.xm1221.HexGuide.patchouli;

import at.petrak.hexcasting.client.gui.GuiSpellcasting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/**
 * 书页内嵌 GuiSpellcasting 的事件代理。
 * 经 GuiBookEntry.addWidget 注册进 Screen.children，收到 mouseClicked/mouseDragged/mouseReleased
 * （屏幕坐标），直接转发给真实 GuiSpellcasting（其宽度/高度已初始化为全屏，坐标即屏幕坐标）。
 * 渲染不做（由 SpellcastingPage 在页面变换下用 scissor 裁剪渲染）。
 */
public class SpellcastingWidget extends AbstractWidget {

    private final GuiSpellcasting spellcasting;

    public SpellcastingWidget(int x, int y, int w, int h, GuiSpellcasting spellcasting) {
        super(x, y, w, h, Component.literal("spellcasting"));
        this.spellcasting = spellcasting;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isMouseOver(mouseX, mouseY)) return false;
        spellcasting.mouseClicked(mouseX, mouseY, button);
        // Screen.mouseDragged/mouseReleased 只转发给 focused child；
        // GuiBook.mouseClickedScaled 不 setFocused，必须手动设置，拖拽才会到达这里。
        if (Minecraft.getInstance().screen != null) {
            Minecraft.getInstance().screen.setFocused(this);
        }
        return true; // 让 GuiBook setDragging
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        spellcasting.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        spellcasting.mouseReleased(mouseX, mouseY, button);
        return false;
    }

    @Override
    public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float pt) {
        // 画布由 SpellcastingPage.render 渲染，这里不画
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        // 画布无需朗读
    }
}
