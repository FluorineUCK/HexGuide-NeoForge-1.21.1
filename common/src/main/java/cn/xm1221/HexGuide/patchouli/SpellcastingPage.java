package cn.xm1221.HexGuide.patchouli;

import at.petrak.hexcasting.api.casting.eval.ExecutionClientView;
import at.petrak.hexcasting.api.casting.math.HexCoord;
import at.petrak.hexcasting.client.gui.GuiSpellcasting;
import at.petrak.hexcasting.common.lib.HexSounds;
import cn.xm1221.HexGuide.networking.msg.MsgBookSyncStackC2S;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec2;
import vazkii.patchouli.api.IVariable;
import vazkii.patchouli.client.book.BookPage;
import vazkii.patchouli.client.book.gui.GuiBook;
import vazkii.patchouli.client.book.gui.GuiBookEntry;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 页面类型 hexguide:spellcasting —— 在书页内嵌真实 GuiSpellcasting（交互式法阵绘制）。
 *
 * 实现（参考 Almightly Staff 的 IDE 屏幕）：
 * - 创建真实 GuiSpellcasting，用公共 Screen.init(mc, w, h) 初始化为全屏尺寸（随后停掉环境音效）
 * - mixin：覆盖 hexSize（点间距）、覆盖 coordsOffset（网格居中于画布）、写模式标 ESCAPED（黄色）并本地维护栈；
 *   施法模式（原版行为）放行发包，回包经 MixinMsgNewSpellPatternS2C 路由到本页更新栈
 * - 渲染：把 drawPage 与 bookLeft/bookTop 的变换抵消回屏幕坐标
 *   · 网格点铺满画布 + 主画布 scissor 裁剪 + 深色边框
 *   · 栈框：把 GuiSpellcasting 左上角 (10,10) 的栈框平移叠加到页面顶部显示
 * - 输入：SpellcastingWidget 转发画布事件；Cast/Write 切换、Clr 用 SpellcastingButtonWidget（Screen children，屏幕坐标）
 *
 * JSON 用法：
 * {
 *   "type": "hexguide:spellcasting",
 *   "hex_size": 28
 * }
 */
public class SpellcastingPage extends BookPage {

    /** 当前显示中的书页（供施法回包路由） */
    public static final Set<SpellcastingPage> ACTIVE = new HashSet<>();

    IVariable hex_size;

    transient GuiSpellcasting spellcasting;
    transient SpellcastingWidget widget;
    transient SpellcastingButtonWidget castButton;

    /** true = 施法模式（原版行为：图案发服务端真施法）；false = 写模式（只绘制，ESCAPED 黄） */
    transient boolean castMode;

    // 底部按钮（页面相对坐标）
    private static final int BTN_Y = 138, BTN_H = 14;
    private static final int CAST_X = 4, CAST_W = 52;
    private static final int CLR_X = 60, CLR_W = 52;
    // 画布（页面相对）：上移，给底部按钮留足空间
    private static final int CANVAS_X = 0, CANVAS_Y = 22;
    private static final int CANVAS_W = GuiBook.PAGE_WIDTH;
    private static final int CANVAS_H = BTN_Y - CANVAS_Y - 10;

    // 左上角栈框平移到页面后的显示区域
    private static final int STACK_W = 110, STACK_H = 56;
    private static final int STACK_X = 2, STACK_Y = CANVAS_Y + 2;

    @SuppressWarnings("unchecked")
    private static <X> X as(Object o) { return (X) o; }

    @Override
    public void onDisplayed(GuiBookEntry parent, int left, int top) {
        super.onDisplayed(parent, left, top);
        ACTIVE.add(this);
        if (spellcasting == null) {
            spellcasting = new GuiSpellcasting(InteractionHand.MAIN_HAND, new ArrayList<>(), List.of(), null, 1);
            // 公共 Screen.init 设置 minecraft/width/height（会触发 GuiSpellcasting.init 的环境音效，随后停掉）
            // 不用 parent.width/height（经 Patchouli 继承的 MC Screen 字段，发布 remap 后不一致），改用窗口 GUI 尺寸
            var win = Minecraft.getInstance().getWindow();
            spellcasting.init(Minecraft.getInstance(), win.getGuiScaledWidth(), win.getGuiScaledHeight());
            Minecraft.getInstance().getSoundManager().stop(HexSounds.CASTING_AMBIANCE.getLocation(), null);

            BookSpellcastingAccess access = as(spellcasting);
            access.setBlockSending$hexguide(!castMode);
            access.setWriteMode$hexguide(!castMode);
            float hs = hex_size != null ? hex_size.asNumber(28f).floatValue() : 28f;
            access.setHexSizeOverride$hexguide(hs);
            // 网格居中于画布（屏幕坐标），消除空隙
            access.setCoordsOffset$hexguide(new Vec2(
                parent.bookLeft + left + CANVAS_W / 2f,
                parent.bookTop + top + CANVAS_H / 2f));
        }
        // 每次新建 widget：Screen.init 会 clearWidgets，旧实例被清；新实例只被 addWidget 偏移一次
        int y = GuiBook.TOP_PADDING + CANVAS_Y; // addWidget 的 Y 偏移是 bookTop（不含 TOP_PADDING）
        this.widget = new SpellcastingWidget(0, y, CANVAS_W, CANVAS_H, spellcasting);
        parent.addWidget(this.widget, pageNum);
        // 按钮：AbstractWidget 走 Screen children，屏幕坐标点击，无偏移问题
        this.castButton = new SpellcastingButtonWidget(CAST_X, GuiBook.TOP_PADDING + BTN_Y, CAST_W, BTN_H, castMode ? "Write" : "Cast", this::toggleCastMode);
        parent.addWidget(this.castButton, pageNum);
        parent.addWidget(new SpellcastingButtonWidget(CLR_X, GuiBook.TOP_PADDING + BTN_Y, CLR_W, BTN_H, "Clr", this::clearAll), pageNum);
    }

    @Override
    public void onHidden(GuiBookEntry parent) {
        super.onHidden(parent);
        ACTIVE.remove(this);
    }

    /** 服务端回包（施法模式）：更新内嵌 spellcasting 的图案类型与栈（像原版 GUI 的 recvServerUpdate） */
    public void onCastResult(ExecutionClientView info, int index) {
        if (spellcasting == null) return;
        if (info.isStackClear()) {
            // 原版 recvServerUpdate 在栈清空时会 setScreen(null) 关闭当前 GUI——书页内嵌不能关书，
            // 手动清空栈显示即可
            BookSpellcastingAccess access = as(spellcasting);
            access.setStackClear$hexguide();
        } else {
            spellcasting.recvServerUpdate(info, index);
        }
    }

    // ─── 按钮动作 ────────────────────────────────────────────

    /** Cast/Write 切换：只影响之后绘制行为——进入施法模式（原版行为：新图案发服务端真施法）
     *  或返回写模式（只绘制，ESCAPED 黄）。切换本身绝不执行已绘制的图案。
     *  进入施法模式时把写模式记录的图案补发到法杖栈（不执行），使法杖栈与本地栈时刻同步。 */
    private void toggleCastMode() {
        castMode = !castMode;
        if (spellcasting != null) {
            BookSpellcastingAccess access = as(spellcasting);
            access.setBlockSending$hexguide(!castMode);
            access.setWriteMode$hexguide(!castMode);
            if (castMode) {
                // 补发写模式记录到法杖栈（不执行，只同步）
                List<CompoundTag> pending = access.takePendingSync$hexguide();
                if (pending != null && !pending.isEmpty()) {
                    new MsgBookSyncStackC2S(pending).sendToServer();
                }
            }
        }
        if (castButton != null) {
            castButton.setLabel(castMode ? "Write" : "Cast");
        }
    }

    private void clearAll() {
        if (spellcasting != null) {
            BookSpellcastingAccess access = as(spellcasting);
            access.clearPatterns$hexguide();
        }
    }

    // ─── 渲染 ────────────────────────────────────────────────

    /** 把六角格点铺满整个画布（延展网格），图案/栈画在其上 */
    private void renderGridDots(GuiGraphics g, GuiSpellcasting sc, int sx, int sy, int sw, int sh) {
        HexCoord c0 = sc.pxToCoord(new Vec2(sx, sy));
        HexCoord c1 = sc.pxToCoord(new Vec2(sx + sw, sy + sh));
        int q0 = Math.min(c0.getQ(), c1.getQ()), q1 = Math.max(c0.getQ(), c1.getQ());
        int r0 = Math.min(c0.getR(), c1.getR()), r1 = Math.max(c0.getR(), c1.getR());
        for (int q = q0 - 1; q <= q1 + 1; q++) {
            for (int r = r0 - 1; r <= r1 + 1; r++) {
                Vec2 px = sc.coordToPx(new HexCoord(q, r));
                if (px.x >= sx && px.x < sx + sw && px.y >= sy && px.y < sy + sh) {
                    int x = Math.round(px.x), y = Math.round(px.y);
                    g.fill(x - 1, y - 1, x + 1, y + 1, 0x33_aaaaaa);
                }
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float pticks) {
        if (spellcasting == null) return;

        // 页面标题（顶部居中）
        var font = Minecraft.getInstance().font;
        graphics.drawCenteredString(font, i18n("hexguide.entry.guide.spellcasting"),
            CANVAS_W / 2, 2, book.textColor);

        var pose = graphics.pose();
        // drawPage 已 translate(left, top)，外层还有 translate(bookLeft, bookTop)。抵消回屏幕坐标。
        pose.pushPose();
        pose.translate(-(parent.bookLeft + left), -(parent.bookTop + top), 0);

        int screenMouseX = mouseX + left;
        int screenMouseY = mouseY + top;

        int sx = parent.bookLeft + left + CANVAS_X;
        int sy = parent.bookTop + top + CANVAS_Y;

        // 网格点铺满画布（先画，图案在其上）
        renderGridDots(graphics, spellcasting, sx, sy, CANVAS_W, CANVAS_H);

        // 主画布：scissor 裁剪到画布区域
        graphics.enableScissor(sx, sy, sx + CANVAS_W, sy + CANVAS_H);
        spellcasting.render(graphics, screenMouseX, screenMouseY, pticks);
        graphics.disableScissor();

        // 画布边框（深色）
        graphics.fill(sx, sy, sx + CANVAS_W, sy + 1, 0xAA_666666);
        graphics.fill(sx, sy + CANVAS_H - 1, sx + CANVAS_W, sy + CANVAS_H, 0xAA_666666);
        graphics.fill(sx, sy, sx + 1, sy + CANVAS_H, 0xAA_666666);
        graphics.fill(sx + CANVAS_W - 1, sy, sx + CANVAS_W, sy + CANVAS_H, 0xAA_666666);

        // 栈框：把 GuiSpellcasting 左上角 (10,10) 的栈框平移到页面顶部
        // （平移后再渲染一遍，scissor 只留栈框区域；网格/背景被裁掉）
        int stx = parent.bookLeft + left + STACK_X;
        int sty = parent.bookTop + top + STACK_Y;
        pose.pushPose();
        pose.translate(stx - 10, sty - 10, 0);
        graphics.enableScissor(stx, sty, stx + STACK_W, sty + STACK_H);
        spellcasting.render(graphics, screenMouseX, screenMouseY, pticks);
        graphics.disableScissor();
        pose.popPose();

        pose.popPose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        // 按钮由 SpellcastingButtonWidget（children）处理，画布由 SpellcastingWidget（children）处理，
        // 页面不拦截
        return false;
    }
}
