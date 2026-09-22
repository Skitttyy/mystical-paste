package fat.vonware.mixin.mixins;

import fat.vonware.features.modules.client.Optimizer;
import fat.vonware.features.modules.render.NoRender;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.EntityPotion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({RenderManager.class})
public class MixinRenderManager {
   @Inject(
      method = {"renderEntity"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void onRenderEntity(Entity entity, double x, double y, double z, float yaw, float partialTicks, boolean debug, CallbackInfo ci) {
      if (entity instanceof EntityPotion) {
         NoRender noRender = NoRender.getInstance();
         if (!noRender.shouldRenderHealthPotion((EntityPotion)entity)) {
            ci.cancel();
         }
      }

      if (Optimizer.shouldCullEntity(entity)) {
         ci.cancel();
      }

   }
}
