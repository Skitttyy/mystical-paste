package fat.vonware.mixin.mixins;

import fat.vonware.Vonware;
import fat.vonware.features.modules.player.FreeLook;
import net.minecraft.client.renderer.culling.Frustum;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Frustum.class})
public class MixinFrustum {
   @Inject(
      method = {"isBoxInFrustum"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void onIsBoxInFrustum(double minX, double minY, double minZ, double maxX, double maxY, double maxZ, CallbackInfoReturnable<Boolean> cir) {
      FreeLook freeLook = (FreeLook)Vonware.moduleManager.getModuleByClass(FreeLook.class);
      if (freeLook != null && freeLook.isEnabled() && freeLook.mode.getValue() == FreeLook.Mode.FreeLook) {
         cir.setReturnValue(true);
      }

   }
}
