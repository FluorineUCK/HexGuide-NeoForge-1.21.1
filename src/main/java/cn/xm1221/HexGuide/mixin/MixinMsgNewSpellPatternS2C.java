package cn.xm1221.HexGuide.mixin;

import at.petrak.hexcasting.common.msgs.MsgNewSpellPatternS2C;
import cn.xm1221.HexGuide.patchouli.SpellcastingPage;
import cn.xm1221.HexGuide.patchouli.EmbeddedSpellResultAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 施法模式（原版行为）下，HexMod 的 MsgNewSpellPatternS2C 只会把回包交给
 * `screen instanceof GuiSpellcasting`——书页内嵌的 spellcasting 收不到。
 * 这里在 HEAD 额外路由到 ACTIVE 的书页（原代码对书页无副作用，两不相干）。
 */
@Mixin(MsgNewSpellPatternS2C.class)
public abstract class MixinMsgNewSpellPatternS2C implements EmbeddedSpellResultAccess {

    @Inject(method = "handle()V", at = @At("HEAD"), remap = false)
    private void routeToEmbedded$hexguide(CallbackInfo ci) {
        MsgNewSpellPatternS2C self = (MsgNewSpellPatternS2C) (Object) this;
        Minecraft.getInstance().execute(() -> {
            Screen screen = Minecraft.getInstance().screen;
            if (screen instanceof vazkii.patchouli.client.book.gui.GuiBookEntry) {
                for (SpellcastingPage page : SpellcastingPage.ACTIVE) {
                    page.onCastResult(self.info(), self.index());
                }
            }
        });
    }
}
