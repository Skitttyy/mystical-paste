package fat.vonware.mixin.mixins.Gui;

import fat.vonware.Vonware;
import fat.vonware.features.modules.render.BetterHotbar;
import fat.vonware.features.modules.render.NoRender;
import fat.vonware.mixin.mixins.accessor.IGuiAccessor;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.gui.ScaledResolution;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({GuiIngame.class})
public abstract class MixinGuiIngame {
   @Inject(
      method = {"renderPortal"},
      at = {@At("HEAD")},
      cancellable = true
   )
   protected void renderPortalHook(float timeInPortal, ScaledResolution scaledResolution, CallbackInfo info) {
      NoRender noRender = (NoRender)Vonware.moduleManager.getModuleByClass(NoRender.class);
      if (noRender != null && noRender.noPortal()) {
         info.cancel();
      }

   }

   @Redirect(
      method = {"renderHotbar"},
      at = @At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/gui/GuiIngame;drawTexturedModalRect(IIIIII)V"
)
   )
   private void vonware$redirectHotbarDraw(GuiIngame instance, int x, int y, int textureX, int textureY, int width, int height) {
      BetterHotbar betterHotbar = (BetterHotbar)Vonware.moduleManager.getModuleByClass(BetterHotbar.class);
      boolean isSelector = textureX == 0 && textureY == 22 && width == 24 && height == 22;
      if (!isSelector || betterHotbar == null || !betterHotbar.isEnabled()) {
         ((IGuiAccessor)instance).vonware$drawTexturedModalRect(x, y, textureX, textureY, width, height);
      }
   }
}
