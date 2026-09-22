package fat.vonware.mixin.mixins.entity;

import fat.vonware.Vonware;
import fat.vonware.features.modules.render.NoRender;
import net.minecraft.client.renderer.entity.RenderItemFrame;
import net.minecraft.entity.item.EntityItemFrame;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({RenderItemFrame.class})
public class MixinRenderItemFrame {
   @Inject(
      method = {"doRender"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void onDoRender(EntityItemFrame entity, double x, double y, double z, float entityYaw, float partialTicks, CallbackInfo ci) {
      NoRender noRender = (NoRender)Vonware.moduleManager.getModuleByClass(NoRender.class);
      if (noRender != null && noRender.isEnabled() && !noRender.shouldRenderItemFrame(entity)) {
         ci.cancel();
      }

   }
}
