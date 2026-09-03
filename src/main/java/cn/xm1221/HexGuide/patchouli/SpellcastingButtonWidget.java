package cn.xm1221.HexGuide.patchouli;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/**
 * 书页内嵌 GUI 的按钮（Cast/Write 切换、Clr）。
 * 经 GuiBookEntry.addWidget 注册进 Screen.children，用屏幕坐标点击（与画布 widget 同机制），
 * 无页面 mouseClicked 的 bookLeft 偏移问题。
 */
public class SpellcastingButtonWidget extends AbstractWidget {

    private String label;
    private final Runnable onClick;

    public SpellcastingButtonWidget(int x, int y, int w, int h, String label, Runnable onClick) {
        super(x, y, w, h, Component.literal(label));
        this.label = label;
        this.onClick = onClick;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isMouseOver(mouseX, mouseY)) return false;
        if (button == 0) {
            onClick.run();
            return true;
        }
        return false;
    }

    @Override
    public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float pt) {
        boolean hover = isHoveredOrFocused();
        g.fill(getX(), getY(), getX() + width, getY() + height, hover ? 0xBB_ff8844 : 0x44_333333);
        g.drawCenteredString(Minecraft.getInstance().font, label, getX() + width / 2, getY() + 3, 0xFFFFFFFF);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        // 无需朗读
    }
}
