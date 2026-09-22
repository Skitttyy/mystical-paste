package fat.vonware.mixin.mixins;

import fat.vonware.features.modules.render.NoRender;
import net.minecraft.client.gui.GuiBossOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
   value = {GuiBossOverlay.class},
   priority = 10000
)
public class MixinGuiBossOverlay {
   @Inject(
      method = {"renderBossHealth"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void renderBossHealth(CallbackInfo ci) {
      if (NoRender.getInstance().isEnabled() && (Boolean)NoRender.getInstance().boss.getValue()) {
         ci.cancel();
      }

   }
}
