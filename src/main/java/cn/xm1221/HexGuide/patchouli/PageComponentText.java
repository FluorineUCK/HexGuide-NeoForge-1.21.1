package cn.xm1221.HexGuide.patchouli;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.resources.ResourceLocation;

import vazkii.patchouli.api.IVariable;
import vazkii.patchouli.client.book.BookPage;
import vazkii.patchouli.client.book.gui.BookTextRenderer;
import vazkii.patchouli.client.book.gui.GuiBook;
import vazkii.patchouli.client.book.gui.GuiBookEntry;

import java.util.Map;

/**
 * 以 Component 形式渲染的文本页面。
 * 与 patchouli:text 类似（标题+正文），但 text 字段直接作为 Component 渲染，
 * 保留 translate/with/extra/color/click/hover 等完整结构，并自动换行。
 *
 * JSON 用法：
 * {
 *   "type": "hexguide:component_text",
 *   "title": "可选标题（翻译键）",
 *   "text": {
 *     "translate": "hexguide.some.text",
 *     "with": [ {"text": "参数"} ]
 *   }
 * }
 */
public class PageComponentText extends BookPage {

    String title;
    IVariable text;

    transient BookTextRenderer textRender;

    @Override
    public void onDisplayed(GuiBookEntry parent, int left, int top) {
        super.onDisplayed(parent, left, top);
        if (text == null) text = IVariable.wrap("");
        // 自定义解析：递归替换 iota: 前缀字符串 + 修复 with 为数组 + 翻译键处理
        Component component = buildComponent(text.unwrap());
        textRender = new BookTextRenderer(parent, component, 0, getTextHeight());
    }

    /** 把 IVariable 的 JSON 转成 Component；iota: 前缀字符串保留原文，交给 Inline 渲染 */
    private Component buildComponent(JsonElement elem) {
        if (elem == null || elem.isJsonNull()) return Component.empty();
        if (elem.isJsonPrimitive() && elem.getAsJsonPrimitive().isString()) {
            String s = elem.getAsString();
            if (s.startsWith("iota:")) {
                // 保留原始字符串，Inline 的字体钩子会渲染对应 iota
                return Component.literal(s);
            }
            // 翻译键 → 立即解析为字面量并隔离 iota，保证 Inline 匹配
            return book.i18n ? Component.literal(isolateIotas(I18n.get(s))) : Component.literal(s);
        }
        // 深拷贝并修复 with 为数组（不解析 iota，留给 Inline）
        JsonElement resolved = fixWith(elem.deepCopy());
        // 对象只有 with 没有内容键（text/translate/...）→ with 本身就是要显示的内容
        if (resolved.isJsonObject()) {
            JsonObject obj = resolved.getAsJsonObject();
            if (obj.has("with") && !hasContentKey(obj)) {
                JsonArray with = obj.getAsJsonArray("with");
                if (!with.isEmpty()) resolved = with.get(0);
            }
        }
        try {
            var registries = mc.level != null ? mc.level.registryAccess() : RegistryAccess.EMPTY;
            Component c = Component.Serializer.fromJson(resolved, registries);
            // translate 对象（含 with 参数）→ 压平成字面量并隔离 iota
            if (c.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents) {
                c = Component.literal(isolateIotas(c.getString()));
            } else if (c.getContents() instanceof PlainTextContents.LiteralContents lc
                && lc.text().contains("iota:")) {
                // 字面量文本里夹着 iota: → 隔离，保证 Inline 匹配
                c = Component.literal(isolateIotas(lc.text()));
            }
            return c;
        } catch (Exception e) {
            return Component.literal(resolved.toString());
        }
    }

    /**
     * 隔离 iota:...：前后补空格，让 BookTextParser 的 BreakIterator 把它当独立词。
     * iota 保持行内不强制换行；引用短（iota:hash.json ≈ 96px < 116px）时整词换行不截断。
     */
    private static String isolateIotas(String s) {
        if (s == null || s.isEmpty() || !s.contains("iota:")) return s;
        // 前面无空白 → 补空格
        s = s.replaceAll("(?<![\\s])iota:", " iota:");
        // 后面无空白 → 补空格
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("iota:[!-~]+").matcher(s);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            m.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(m.group()));
            int end = m.end();
            if (end < s.length() && !Character.isWhitespace(s.charAt(end))) sb.append(' ');
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static boolean hasContentKey(JsonObject o) {
        return o.has("text") || o.has("translate") || o.has("keybind")
            || o.has("score") || o.has("selector") || o.has("nbt");
    }

    /** 递归遍历 JSON，把字符串形式的 with 转成数组（其余保持不变，iota: 字符串原样保留） */
    private JsonElement fixWith(JsonElement elem) {
        if (elem.isJsonArray()) {
            JsonArray arr = elem.getAsJsonArray();
            for (int i = 0; i < arr.size(); i++) arr.set(i, fixWith(arr.get(i)));
            return arr;
        }
        if (elem.isJsonObject()) {
            JsonObject obj = elem.getAsJsonObject();
            if (obj.has("with") && obj.get("with").isJsonPrimitive()) {
                JsonArray arr = new JsonArray();
                arr.add(obj.get("with"));
                obj.add("with", arr);
            }
            for (Map.Entry<String, JsonElement> e : obj.entrySet()) {
                if (!e.getKey().equals("with")) obj.add(e.getKey(), fixWith(e.getValue()));
            }
            return obj;
        }
        return elem;
    }

    private int getTextHeight() {
        if (pageNum == 0) return 22;
        if (title != null && !title.isEmpty()) return 12;
        return -4;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float pticks) {
        if (textRender != null) textRender.render(graphics, mouseX, mouseY, pticks);

        if (pageNum == 0) {
            boolean renderedSmol = false;
            String smolText = "";
            if (mc.options.advancedItemTooltips) {
                ResourceLocation res = parent.getEntry().getId();
                smolText = res.toString();
            } else if (entry.getAddedBy() != null) {
                smolText = I18n.get("patchouli.gui.lexicon.added_by", entry.getAddedBy());
            }

            if (!smolText.isEmpty()) {
                graphics.pose().scale(0.5F, 0.5F, 1F);
                parent.drawCenteredStringNoShadow(graphics, smolText, GuiBook.PAGE_WIDTH, 12, book.headerColor);
                graphics.pose().scale(2F, 2F, 1F);
                renderedSmol = true;
            }

            parent.drawCenteredStringNoShadow(graphics, parent.getEntry().getName().getVisualOrderText(),
                GuiBook.PAGE_WIDTH / 2, renderedSmol ? -3 : 0, book.headerColor);
            GuiBook.drawSeparator(graphics, book, 0, 12);
        } else if (title != null && !title.isEmpty()) {
            parent.drawCenteredStringNoShadow(graphics, I18n.get(title), GuiBook.PAGE_WIDTH / 2, 0, book.headerColor);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        return textRender != null && textRender.click(mouseX, mouseY, mouseButton);
    }
}
