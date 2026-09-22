package fat.vonware.mixin.mixins.entity;

import fat.vonware.features.modules.render.NoRender;
import net.minecraft.client.renderer.entity.RenderArmorStand;
import net.minecraft.entity.item.EntityArmorStand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({RenderArmorStand.class})
public class MixinRenderArmorStand {
   @Inject(
      method = {"doRender"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onDoRender(EntityArmorStand entity, double x, double y, double z, float entityYaw, float partialTicks, CallbackInfo ci) {
      if (NoRender.getInstance().noArmorStands()) {
         ci.cancel();
      }

   }
}
