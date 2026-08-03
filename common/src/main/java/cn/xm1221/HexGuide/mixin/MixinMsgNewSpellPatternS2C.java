package cn.xm1221.HexGuide.mixin;

import at.petrak.hexcasting.common.msgs.MsgNewSpellPatternS2C;
import cn.xm1221.HexGuide.patchouli.SpellcastingPage;
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
public abstract class MixinMsgNewSpellPatternS2C {

    @Inject(method = "handle", at = @At("HEAD"), remap = false)
    private static void routeToEmbedded$hexguide(MsgNewSpellPatternS2C self, CallbackInfo ci) {
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
