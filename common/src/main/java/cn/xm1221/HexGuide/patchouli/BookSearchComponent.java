package cn.xm1221.HexGuide.patchouli;

import at.petrak.hexcasting.api.HexAPI;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
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
 * Patchouli 自定义组件 —— 全文搜索《咒术笔记》并跳转。
 * 布局：输入框(顶) → 按钮行 → 结果列表 → 翻页(底)
 */
public class BookSearchComponent implements ICustomComponent {

    private static final int W = 108, PAGE_H = 156;
    private static final int INPUT_H = 12, BTN_H = 10, ROW_H = 10, NAV_H = 10;
    private static final int BTN_Y = INPUT_H + 1;
    private static final int LIST_Y = BTN_Y + BTN_H + 1;
    private static final int MAX_ROWS = (PAGE_H - LIST_Y - NAV_H - 2) / ROW_H;

    private transient int x, y, pageNum;
    private transient EditBox input;
    private transient List<Hit> all, filtered = List.of();
    private transient int scrollOff;

    private record Hit(ResourceLocation eid, int page, String name, String text) {}

    @Override public void onVariablesAvailable(UnaryOperator<IVariable> l) {}

    @Override public void build(int cx, int cy, int pn) {
        this.x = cx == -1 ? (116 - W) / 2 : cx; this.y = cy; this.pageNum = pn;
    }

    @Override public void onDisplayed(IComponentRenderContext c) {
        if (all == null) { all = buildIndex(); filtered = all; }
        if (input == null) {
            input = new EditBox(Minecraft.getInstance().font, x, y, W, INPUT_H,
                Component.translatable("hexguide.search.placeholder"));
            input.setMaxLength(80); input.setBordered(true);
            input.setResponder(this::onInput);
        }
        input.setX(x); input.setY(y);
        scrollOff = 0;
    }

    @Override
    public void render(GuiGraphics g, IComponentRenderContext c, float pt, int mx, int my) {
        if (input == null) return;
        var f = Minecraft.getInstance().font; int total = filtered.size();

        input.render(g, mx, my, pt);

        int by = y + BTN_Y;
        int bw = f.width("back") + 4;
        boolean hBS = c.isAreaHovered(mx, my, x, by, bw, BTN_H);
        g.fill(x, by, x + bw, by + BTN_H, hBS ? 0x44_666666 : 0x44_333333);
        g.drawString(f, "back", x + 2, by, hBS ? 0xFF_FF4444 : 0xFF_888888);
        int cw = f.width("clear") + 4, cxx = x + W - cw;
        boolean hCL = c.isAreaHovered(mx, my, cxx, by, cw, BTN_H);
        g.fill(cxx, by, cxx + cw, by + BTN_H, hCL ? 0x44_666666 : 0x44_333333);
        g.drawString(f, "clear", cxx + 2, by, hCL ? 0xFF_FFCC00 : 0xFF_888888);

        int ly = y + LIST_Y;
        for (int i = scrollOff; i < total && i < scrollOff + MAX_ROWS; i++) {
            var h = filtered.get(i);
            boolean hover = c.isAreaHovered(mx, my, x, ly, W, ROW_H);
            g.drawString(f, clip(f, h.name(), W - f.width(" [99]") - 2) + " ["+(h.page()+1)+"]",
                x, ly, hover ? 0xFFFF_D700 : 0xFF_CC_CCCC);
            ly += ROW_H;
        }

        int ny = y + PAGE_H - NAV_H;
        boolean hUp = c.isAreaHovered(mx, my, x, ny, 20, NAV_H);
        g.drawString(f, hUp ? "▲" : "△", x, ny, hUp ? 0xFF_FFFF00 : 0xFF_888888);
        boolean hDn = c.isAreaHovered(mx, my, x + W - 20, ny, 20, NAV_H);
        g.drawString(f, hDn ? "▼" : "▽", x + W - 10, ny, hDn ? 0xFF_FFFF00 : 0xFF_888888);
        String info = total == 0 ? "0" : total <= MAX_ROWS ? total + "" :
            (scrollOff+1)+"-"+Math.min(scrollOff+MAX_ROWS,total)+"/"+total;
        g.drawString(f, info, x + (W - f.width(info))/2, ny, 0xFF_888888);
    }

    @Override
    public boolean mouseClicked(IComponentRenderContext c, double mx, double my, int btn) {
        if (input == null) return false;
        int total = filtered.size(); var f = Minecraft.getInstance().font;

        if (c.isAreaHovered((int) mx, (int) my, x, y, W, INPUT_H)) {
            input.mouseClicked(mx, my, btn); focusScreenOn(input); return true;
        }
        int by = y + BTN_Y, bw = f.width("back") + 4;
        if (c.isAreaHovered((int) mx, (int) my, x, by, bw, BTN_H)) {
            String v = input.getValue(); if (!v.isEmpty()) {
                int p = input.getCursorPosition();
                if (p > 0) { input.setValue(v.substring(0, p-1)+v.substring(p)); input.setCursorPosition(p-1); }
            } return true;
        }
        int cw = f.width("clear") + 4, cxx = x + W - cw;
        if (c.isAreaHovered((int) mx, (int) my, cxx, by, cw, BTN_H)) { input.setValue(""); return true; }
        int ly = y + LIST_Y;
        for (int i = scrollOff; i < total && i < scrollOff + MAX_ROWS; i++) {
            if (c.isAreaHovered((int) mx, (int) my, x, ly, W, ROW_H)) {
                c.navigateToEntry(filtered.get(i).eid(), filtered.get(i).page(), false); return true;
            } ly += ROW_H;
        }
        int ny = y + PAGE_H - NAV_H;
        if (total > MAX_ROWS && c.isAreaHovered((int) mx, (int) my, x, ny, 20, NAV_H))
            { scrollOff = Math.max(0, scrollOff - MAX_ROWS); return true; }
        if (total > MAX_ROWS && c.isAreaHovered((int) mx, (int) my, x + W - 20, ny, 20, NAV_H))
            { scrollOff = Math.min(total - MAX_ROWS, scrollOff + MAX_ROWS); return true; }
        return false;
    }

    private void onInput(String t) {
        filtered = t.isEmpty() ? all :
            all.stream().filter(r->r.text().toLowerCase(Locale.ROOT).contains(t.toLowerCase(Locale.ROOT))).toList();
        scrollOff = 0;
    }

    private static List<Hit> buildIndex() {
        List<Hit> out = new ArrayList<>();
        Book b = BookRegistry.INSTANCE.books.get(HexAPI.modLoc("thehexbook"));
        if (b == null) return out;
        for (var cat : b.getContents().categories.values())
            for (BookEntry e : cat.getEntries()) {
                if (e.isLocked()) continue;
                String en = e.getName().getString();
                for (int pi = 0; pi < e.getPages().size(); pi++) {
                    JsonObject r = e.getPages().get(pi).sourceObject;
                    if (r == null) continue;
                    StringBuilder sb = new StringBuilder(en).append(' ');
                    String ty = r.has("type") ? r.get("type").getAsString() : "";
                    if ("hexcasting:pattern".equals(ty)) {
                        if (r.has("op_id")) {
                            String op = r.get("op_id").getAsString();
                            sb.append(op).append(' ');
                            sb.append(resolve("hexcasting.action." + op)).append(' ');
                        }
                        if (r.has("input")) sb.append(jsonText(r.get("input"))).append(' ');
                        if (r.has("output")) sb.append(jsonText(r.get("output"))).append(' ');
                    }
                    if (r.has("text")) sb.append(jsonText(r.get("text"))).append(' ');
                    out.add(new Hit(e.getId(), pi, en, sb.toString()));
                }
            }
        return out;
    }

    private static String resolve(String k) {
        try { return Component.translatable(k).getString(); } catch (Exception ig) { return k; }
    }

    /** 安全提取 JSON 中的文本：字符串→翻译键解析；对象→取 translate/text 字段，否则序列化 */
    private static String jsonText(JsonElement el) {
        if (el == null || el.isJsonNull()) return "";
        if (el.isJsonPrimitive()) {
            JsonPrimitive p = el.getAsJsonPrimitive();
            return p.isString() ? resolve(p.getAsString()) : p.toString();
        }
        if (el.isJsonObject()) {
            JsonObject o = el.getAsJsonObject();
            if (o.has("translate")) return resolve(o.get("translate").getAsString());
            if (o.has("text") && o.get("text").isJsonPrimitive()) return o.get("text").getAsString();
            return o.toString();
        }
        return el.toString();
    }
    private static String clip(net.minecraft.client.gui.Font f, String s, int w) {
        while (f.width(s) > w && s.length() > 1) s = s.substring(0, s.length() - 1);
        return s;
    }
    private static void focusScreenOn(GuiEventListener w) {
        var s = Minecraft.getInstance().screen;
        if (s == null) return;
        try { Screen.class.getMethod("setFocused", GuiEventListener.class).invoke(s, w); } catch (Exception ig) {}
    }
}
