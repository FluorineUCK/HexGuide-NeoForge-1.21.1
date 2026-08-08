package cn.xm1221.HexGuide.mixin;

import at.petrak.hexcasting.api.casting.eval.ResolvedPattern;
import at.petrak.hexcasting.api.casting.eval.ResolvedPatternType;
import at.petrak.hexcasting.api.casting.iota.IotaType;
import at.petrak.hexcasting.api.casting.iota.PatternIota;
import at.petrak.hexcasting.api.casting.math.HexCoord;
import at.petrak.hexcasting.api.casting.math.HexPattern;
import at.petrak.hexcasting.client.gui.GuiSpellcasting;
import at.petrak.hexcasting.common.msgs.IMessage;
import at.petrak.hexcasting.common.msgs.MsgNewSpellPatternC2S;
import at.petrak.hexcasting.xplat.IClientXplatAbstractions;
import cn.xm1221.HexGuide.patchouli.BookSpellcastingAccess;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec2;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 让书页能内嵌真实 GuiSpellcasting：
 * - 拦截 drawEnd 的 sendPacketToServer（书页练习不施法）
 * - 只绘制不施法模式：完成的图案标 ESCAPED（黄色）
 * - 把新图案加入 cachedStack 并重算栈显示（服务端不回包，需本地维护）
 * - 覆盖 hexSize（点间距可配置）、清屏、读取栈
 * 注：Screen 的 minecraft/width/height 用公共 Screen.init(mc, w, h) 设置。
 */
@Mixin(GuiSpellcasting.class)
public abstract class MixinGuiSpellcasting implements BookSpellcastingAccess {

    @Shadow(remap = false) private List<ResolvedPattern> patterns;
    @Shadow(remap = false) private Set<HexCoord> usedSpots;
    @Shadow(remap = false) private List<CompoundTag> cachedStack;
    @Shadow(remap = false) private void calculateIotaDisplays() { }

    // 默认不拦截、不写模式——只有书页内嵌时才由页面显式开启，正常法杖施法不受影响
    @Unique private boolean blockSending$hexguide = false;
    @Unique private boolean writeMode$hexguide = false;
    @Unique private float hexSizeOverride$hexguide = Float.NaN;
    @Unique private Vec2 coordsOffsetOverride$hexguide;
    // 写模式记录的、尚未推送到法杖栈的图案（切到施法模式时补发，不执行）
    @Unique private List<CompoundTag> pendingSync$hexguide = new ArrayList<>();
    // 演示页面：图案下标 → 自定义颜色（ARGB）
    @Unique private Map<Integer, Integer> demoColors$hexguide = new HashMap<>();
    // ── 演示执行状态（透传：服务端 CastingVM 更新 → demoSetImageNbt 读回 → demoGetImageNbt 写回）──
    @Unique private int demoOpenParens$hexguide = 0;               // 内省计数
    @Unique private boolean demoEscapeNext$hexguide = false;       // 考察待转义
    @Unique private CompoundTag demoParenthesized$hexguide = new CompoundTag(); // 内省中的列表
    @Unique private int demoOpsConsumed$hexguide = 0;
    @Unique private CompoundTag demoUserData$hexguide = new CompoundTag();
    @Unique private int demoEscapedColor$hexguide = 0xFFFFD93D;    // 转义图案颜色（默认黄）

    @WrapWithCondition(
        method = "drawEnd",
        at = @At(value = "INVOKE",
            target = "Lat/petrak/hexcasting/xplat/IClientXplatAbstractions;sendPacketToServer(Lat/petrak/hexcasting/common/msgs/IMessage;)V",
            remap = false),
        remap = false
    )
    private boolean blockPacket$hexguide(IClientXplatAbstractions inst, IMessage msg) {
        if (msg instanceof MsgNewSpellPatternC2S s) {
            // 写模式：图案标 ESCAPED（黄色）+ 本地维护栈（记录）；施法模式（原版行为）放行发包
            if (writeMode$hexguide) {
                int idx = s.resolvedPatterns().size() - 1;
                if (idx >= 0 && idx < patterns.size()) {
                    patterns.get(idx).setType(ResolvedPatternType.ESCAPED);
                }
                CompoundTag rec = IotaType.serialize(new PatternIota(s.pattern()));
                List<CompoundTag> newStack = new ArrayList<>(cachedStack);
                newStack.add(rec);
                cachedStack = newStack;
                pendingSync$hexguide.add(rec);
                this.calculateIotaDisplays();
            }
        }
        return !blockSending$hexguide;
    }

    @ModifyReturnValue(method = "hexSize", at = @At("RETURN"), remap = false)
    private float overrideHexSize$hexguide(float original) {
        return Float.isNaN(hexSizeOverride$hexguide) ? original : hexSizeOverride$hexguide;
    }

    @ModifyReturnValue(method = "coordsOffset", at = @At("RETURN"), remap = false)
    private Vec2 overrideCoordsOffset$hexguide(Vec2 original) {
        return coordsOffsetOverride$hexguide != null ? coordsOffsetOverride$hexguide : original;
    }

    @Override public void setBlockSending$hexguide(boolean block) { blockSending$hexguide = block; }
    @Override public void setHexSizeOverride$hexguide(float hexSize) { hexSizeOverride$hexguide = hexSize; }
    @Override public void setCoordsOffset$hexguide(Vec2 offset) { coordsOffsetOverride$hexguide = offset; }
    @Override public void setWriteMode$hexguide(boolean write) { writeMode$hexguide = write; }
    @Override public void clearPatterns$hexguide() {
        patterns.clear();
        usedSpots.clear();
        // 不清除 cachedStack：转义的栈记录保留（栈显示继续累积）
        this.calculateIotaDisplays();
    }

    /**
     * 施法结果栈清空时调用：清空栈显示（不关闭 GUI）。
     * 原版 recvServerUpdate 在 isStackClear 时会 setScreen(null)，书页内嵌不能关书。
     */
    @Override public void setStackClear$hexguide() {
        cachedStack = new ArrayList<>();
        this.calculateIotaDisplays();
    }

    /**
     * 切到施法模式时调用：把尚未同步到服务端的本地图案补发（MsgNewSpellPatternC2S），
     * 让服务端施法 VM 从与本地相同的栈继续执行——本地栈与施法栈是同一个。
     */
    /**
     * 演示页面：把图案加入网格（带可选自定义颜色）。
     * 转义/内省等状态由服务端 CastingVM 原版逻辑处理——execute 步骤全部上传服务端执行，
     * 本地只负责状态透传（demoSetImageNbt 读回 / demoGetImageNbt 写回）与转义结果标色。
     */
    @Override public void demoAddPattern$hexguide(HexPattern pat, HexCoord origin, int color) {
        int idx = patterns.size();
        patterns.add(new ResolvedPattern(pat, origin, ResolvedPatternType.EVALUATED));
        usedSpots.addAll(pat.positions(origin));
        if (color != -1) {
            demoColors$hexguide.put(idx, color);
        }
        this.calculateIotaDisplays();
    }

    /** 设置转义图案颜色（ARGB） */
    @Override public void setDemoEscapedColor$hexguide(int color) {
        demoEscapedColor$hexguide = color;
    }

    /** 重置本地执行状态（转义/内省等，页面切换或重播时清空） */
    @Override public void resetDemoParenState$hexguide() {
        demoOpenParens$hexguide = 0;
        demoEscapeNext$hexguide = false;
        demoParenthesized$hexguide = new CompoundTag();
        demoOpsConsumed$hexguide = 0;
        demoUserData$hexguide = new CompoundTag();
    }

    /** 演示页面：清空画布（网格 + usedSpots + 颜色覆盖），不清空本地栈 */
    @Override public void demoClearCanvas$hexguide() {
        patterns.clear();
        usedSpots.clear();
        demoColors$hexguide.clear();
        this.calculateIotaDisplays();
    }

    /** 演示页面：给指定下标的图案设置自定义颜色（如执行 ERRORED 染红） */
    @Override public void demoColor$hexguide(int index, int color) {
        demoColors$hexguide.put(index, color);
    }

    /** 演示页面"入栈"步骤：把配置的自定义 iota 压入本地栈 */
    @Override public void demoPushIota$hexguide(CompoundTag iotaNbt) {
        List<CompoundTag> newStack = new ArrayList<>(cachedStack);
        newStack.add(iotaNbt);
        cachedStack = newStack;
        this.calculateIotaDisplays();
    }

    /**
     * 演示页面"peek"步骤：移除本地栈指定位置的 iota。
     * 用户下标以栈顶为 0（栈顶 = cachedStack 末尾）；越界时忽略（限界保护）。
     */
    @Override public void demoPeek$hexguide(int userIndex) {
        int size = cachedStack.size();
        int realIndex = size - 1 - userIndex; // 栈顶(用户0) = cachedStack 最后
        if (realIndex < 0 || realIndex >= size) return; // 越界保护
        List<CompoundTag> newStack = new ArrayList<>(cachedStack);
        newStack.remove(realIndex);
        cachedStack = newStack;
        this.calculateIotaDisplays();
    }

    /** 上传用：把本地栈 + 执行状态打包成 CastingImage 的 NBT（服务端 loadFromNbt 后按原版逻辑运行） */
    @Override public CompoundTag demoGetImageNbt$hexguide() {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (CompoundTag c : cachedStack) list.add(c);
        tag.put("stack", list);
        // 写回本地保存的执行状态（服务端 CastingVM 用它们处理转义/内省）
        tag.putInt("open_parens", demoOpenParens$hexguide);
        tag.putBoolean("escape_next", demoEscapeNext$hexguide);
        tag.putInt("ops_consumed", demoOpsConsumed$hexguide);
        tag.put("parenthesized", demoParenthesized$hexguide);
        tag.put("userdata", demoUserData$hexguide);
        return tag;
    }

    /** 执行结果：用返回的 CastingImage 更新本地栈显示 + 读回执行状态（供下次上传继续转义/内省） */
    @Override public void demoSetImageNbt$hexguide(CompoundTag imageNbt) {
        List<CompoundTag> newStack = new ArrayList<>();
        ListTag list = imageNbt.getList("stack", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            newStack.add(list.getCompound(i));
        }
        cachedStack = newStack;
        // 读回服务端更新过的执行状态（原版 CastingVM 处理转义/内省后回传）
        if (imageNbt.contains("open_parens")) demoOpenParens$hexguide = imageNbt.getInt("open_parens");
        if (imageNbt.contains("escape_next")) demoEscapeNext$hexguide = imageNbt.getBoolean("escape_next");
        if (imageNbt.contains("ops_consumed")) demoOpsConsumed$hexguide = imageNbt.getInt("ops_consumed");
        if (imageNbt.contains("parenthesized")) demoParenthesized$hexguide = imageNbt.getCompound("parenthesized");
        if (imageNbt.contains("userdata")) demoUserData$hexguide = imageNbt.getCompound("userdata");
        this.calculateIotaDisplays();
    }

    @Override public int patternCount$hexguide() { return patterns.size(); }
    @Override public HexPattern getPattern$hexguide(int index) { return patterns.get(index).getPattern(); }
    @Override public List<CompoundTag> getStack$hexguide() { return cachedStack; }

    /**
     * 渲染时按图案下标替换自定义颜色。drawPatternFromPoints 的最后参数 seed 即网格循环的 idx。
     * WIP 图案的 seed = patterns.size()，不在覆盖表内，不受影响。
     */
    @WrapOperation(
        // render 是 override Screen.render：
        // - 开发/Forge mojmap：render
        // - Forge 发布（SRG）：m_88315_
        // - Fabric 发布（HexMod jar 是 intermediary）：method_25394
        method = {"render", "m_88315_", "method_25394"},
        at = @At(value = "INVOKE",
            target = "Lat/petrak/hexcasting/client/render/RenderLib;drawPatternFromPoints(Lorg/joml/Matrix4f;Ljava/util/List;Ljava/util/Set;ZIIFFFD)V",
            remap = false),
        remap = false
    )
    private void wrapDemoColor$hexguide(Matrix4f mat, List<Vec2> points, Set<Integer> dupIndices,
                                        boolean drawLast, int tail, int head, float flowIrregular,
                                        float readabilityOffset, float lastSegmentLen, double seed,
                                        Operation<Void> original) {
        int idx = (int) Math.round(seed);
        Integer col = demoColors$hexguide.get(idx);
        if (col != null) {
            tail = col.intValue();
            head = col.intValue();
        }
        original.call(mat, points, dupIndices, drawLast, tail, head, flowIrregular, readabilityOffset,
            lastSegmentLen, seed);
    }

    /** 取走待同步到法杖栈的写模式记录（并清空） */
    @Override public List<CompoundTag> takePendingSync$hexguide() {
        List<CompoundTag> out = new ArrayList<>(pendingSync$hexguide);
        pendingSync$hexguide = new ArrayList<>();
        return out;
    }
}
