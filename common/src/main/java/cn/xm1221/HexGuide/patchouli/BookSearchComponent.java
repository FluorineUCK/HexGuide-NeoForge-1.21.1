package cn.xm1221.HexGuide.patchouli;

import at.petrak.hexcasting.api.HexAPI;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import vazkii.patchouli.api.ICustomComponent;
import vazkii.patchouli.api.IComponentRenderContext;
import vazkii.patchouli.api.IVariable;
import vazkii.patchouli.client.book.BookEntry;
import vazkii.patchouli.client.book.BookPage;
import vazkii.patchouli.common.book.Book;
import vazkii.patchouli.common.book.BookRegistry;

import java.util.*;
import java.util.function.UnaryOperator;

/**
 * Patchouli 自定义组件 —— 在《咒术笔记》中全文搜索并直接跳转。
 * <p>
 * 布局（页面 116×156）：
 * [⌫] [输入框_________] [X]   ← 顶部行：退格+输入框+清空
 * 结果列表 ... 右侧滑条
 * [▲] 1-11 / 45 [▼]           ← 底部行：翻页+计数
 */
public class BookSearchComponent implements ICustomComponent {

    private static final int PAGE_W = 116, PAGE_H = 156;
    private static final int BTN_SZ = 10;       // 按钮大小
    private static final int INPUT_H = 12;
    private static final int INPUT_W = 108 - BTN_SZ * 2 - 4; // 减去两个按钮+间距
    private static final int ROW_H = 10;
    private static final int MAX_ROWS = 11;     // (156-14-14)/10 ≈ 12.8，取11留余量
    private static final int GAP = 1;

    private transient int x, y, pageNum;
    private transient EditBox input;
    private transient List<Hit> all, filtered = List.of();
    private transient int scrollOff;
    private transient IComponentRenderContext ctx;

    private record Hit(ResourceLocation eid, int page, String name, String text) {}

    // ─── 生命周期 ────────────────────────────────────────────

    @Override public void onVariablesAvailable(UnaryOperator<IVariable> l) {}

    @Override
    public void build(int cx, int cy, int pn) {
        this.x = cx == -1 ? (PAGE_W - 108) / 2 : cx;  // 108=INPUT_W+BTN_SZ*2+4
        this.y = cy;
        this.pageNum = pn;
    }

    @Override
    public void onDisplayed(IComponentRenderContext c) {
        this.ctx = c;
        if (all == null) { all = buildIndex(); filtered = all; }
        if (input == null) {
            var f = Minecraft.getInstance().font;
            int ix = x + BTN_SZ + 1;  // 给退格键留空间
            input = new EditBox(f, ix, y, INPUT_W, INPUT_H,
                Component.translatable("hexguide.search.placeholder"));
            input.setMaxLength(80);
            input.setBordered(true);
            input.setResponder(this::onInput);
        }
        input.setX(x + BTN_SZ + 1);
        input.setY(y);
        c.addWidget(input, pageNum);
        input.setFocused(false);
        scrollOff = 0;
    }

    @Override
    public void render(GuiGraphics g, IComponentRenderContext c, float pt, int mx, int my) {
        if (input == null) return;
        var f = Minecraft.getInstance().font;
        int total = filtered.size();

        // ── 顶部按钮行 ──
        int bx = x, by = y + 1;
        // 退格 [⌫]
        boolean hBS = c.isAreaHovered(mx, my, bx, by, BTN_SZ, BTN_SZ);
        g.fill(bx, by, bx + BTN_SZ, by + BTN_SZ, 0x44_000000 + (hBS ? 0x44_666666 : 0));
        g.drawString(f, "⌫", bx + 1, by + 1, hBS ? 0xFF_FF4444 : 0xFF_888888);
        // 清空 [X]
        int cxBtn = bx + BTN_SZ + INPUT_W + 2;
        boolean hCL = c.isAreaHovered(mx, my, cxBtn, by, BTN_SZ, BTN_SZ);
        g.fill(cxBtn, by, cxBtn + BTN_SZ, by + BTN_SZ, 0x44_000000 + (hCL ? 0x44_666666 : 0));
        g.drawString(f, "X", cxBtn + 2, by + 1, hCL ? 0xFF_FFCC00 : 0xFF_888888);

        // ── 结果列表 ──
        int listY = y + INPUT_H + GAP;
        for (int i = scrollOff; i < total && i < scrollOff + MAX_ROWS; i++) {
            var h = filtered.get(i);
            boolean hover = c.isAreaHovered(mx, my, x, listY, 108, ROW_H);
            g.drawString(f, clip(f, h.name(), 108 - f.width(" [99]") - 2) + " [" + (h.page() + 1) + "]",
                x, listY, hover ? 0xFFFF_D700 : 0xFF_CC_CCCC);
            listY += ROW_H;
        }

        // ── 底部导航栏 ──
        int navY = y + PAGE_H - 14;
        int mxW = 108;
        // 左箭头 ▲
        boolean hUp = c.isAreaHovered(mx, my, x, navY, 20, 10);
        g.drawString(f, hUp ? "\u25B2" : "\u25B3", x, navY, hUp ? 0xFF_FFFF00 : 0xFF_888888);
        // 计数
        String info;
        if (total == 0) info = "0 results";
        else if (total <= MAX_ROWS) info = total + " results";
        else info = (scrollOff + 1) + "-" + Math.min(scrollOff + MAX_ROWS, total) + " / " + total;
        int infoW = f.width(info);
        g.drawString(f, info, x + (mxW - infoW) / 2, navY, 0xFF_888888);
        // 右箭头 ▼
        boolean hDn = c.isAreaHovered(mx, my, x + mxW - 20, navY, 20, 10);
        g.drawString(f, hDn ? "\u25BC" : "\u25BD", x + mxW - 10, navY, hDn ? 0xFF_FFFF00 : 0xFF_888888);
    }

    @Override
    public boolean mouseClicked(IComponentRenderContext c, double mx, double my, int btn) {
        if (input == null) return false;
        int total = filtered.size();

        // ── 退格按钮 ──
        int bx = x, by = y + 1;
        if (c.isAreaHovered((int) mx, (int) my, bx, by, BTN_SZ, BTN_SZ)) {
            String v = input.getValue();
            if (!v.isEmpty()) {
                int p = input.getCursorPosition();
                if (p > 0) {
                    input.setValue(v.substring(0, p - 1) + v.substring(p));
                    input.setCursorPosition(p - 1);
                }
            }
            return true;
        }
        // ── 清空按钮 ──
        int cxBtn = bx + BTN_SZ + INPUT_W + 2;
        if (c.isAreaHovered((int) mx, (int) my, cxBtn, by, BTN_SZ, BTN_SZ)) {
            input.setValue("");
            return true;
        }
        // ── 输入框 → 聚焦 ──
        int ix = x + BTN_SZ + 1;
        if (c.isAreaHovered((int) mx, (int) my, ix, y, INPUT_W, INPUT_H)) {
            input.setFocused(true);
            focusScreenOn(input);
            return true;
        }

        // ── 结果行 ──
        int ry = y + INPUT_H + GAP;
        for (int i = scrollOff; i < total && i < scrollOff + MAX_ROWS; i++) {
            if (c.isAreaHovered((int) mx, (int) my, x, ry, 108, ROW_H)) {
                c.navigateToEntry(filtered.get(i).eid(), filtered.get(i).page(), false);
                return true;
            }
            ry += ROW_H;
        }

        // ── 上翻 ▲ ──
        int navY = y + PAGE_H - 14;
        if (total > MAX_ROWS && c.isAreaHovered((int) mx, (int) my, x, navY, 20, 10)) {
            scrollOff = Math.max(0, scrollOff - MAX_ROWS);
            return true;
        }
        // ── 下翻 ▼ ──
        if (total > MAX_ROWS && c.isAreaHovered((int) mx, (int) my, x + 108 - 20, navY, 20, 10)) {
            scrollOff = Math.min(total - MAX_ROWS, scrollOff + MAX_ROWS);
            return true;
        }
        return false;
    }

    // ─── 搜索 ────────────────────────────────────────────────

    private void onInput(String t) {
        filtered = t.isEmpty() ? all :
            all.stream().filter(r -> r.text().toLowerCase(Locale.ROOT).contains(t.toLowerCase(Locale.ROOT))).toList();
        scrollOff = 0;
    }

    private static List<Hit> buildIndex() {
        List<Hit> out = new ArrayList<>();
        Book b = BookRegistry.INSTANCE.books.get(HexAPI.modLoc("thehexbook"));
        if (b == null) return out;
        for (var cat : b.getContents().categories.values())
            for (BookEntry e : cat.getEntries()) {
                String en = e.getName().getString();
                for (int pi = 0; pi < e.getPages().size(); pi++) {
                    JsonObject r = e.getPages().get(pi).sourceObject;
                    if (r == null) continue;
                    StringBuilder sb = new StringBuilder(en).append(' ');
                    String ty = r.has("type") ? r.get("type").getAsString() : "";
                    if ("hexcasting:pattern".equals(ty)) {
                        if (r.has("op_id")) sb.append(r.get("op_id").getAsString()).append(' ');
                        if (r.has("input")) sb.append(r.get("input").getAsString()).append(' ');
                        if (r.has("output")) sb.append(r.get("output").getAsString()).append(' ');
                    }
                    if (r.has("text")) sb.append(resolve(r.get("text").getAsString())).append(' ');
                    out.add(new Hit(e.getId(), pi, en, sb.toString()));
                }
            }
        return out;
    }

    private static String resolve(String k) {
        try { return Component.translatable(k).getString(); } catch (Exception ignored) { return k; }
    }

    private static String clip(net.minecraft.client.gui.Font f, String s, int w) {
        while (f.width(s) > w && s.length() > 1) s = s.substring(0, s.length() - 1);
        return s;
    }

    private static void focusScreenOn(GuiEventListener w) {
        var s = Minecraft.getInstance().screen;
        if (s == null) return;
        try { Screen.class.getMethod("setFocused", GuiEventListener.class).invoke(s, w); } catch (Exception ignored) {}
    }
}
