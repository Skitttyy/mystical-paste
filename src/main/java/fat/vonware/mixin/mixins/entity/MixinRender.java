package fat.vonware.mixin.mixins.entity;

import fat.vonware.features.modules.render.NoRender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
   value = {Render.class},
   priority = 1000
)
public abstract class MixinRender<T extends Entity> {
   @Inject(
      method = {"renderShadow"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void renderShadowHook(Entity entityIn, double x, double y, double z, float shadowAlpha, float partialTicks, CallbackInfo ci) {
      if (entityIn instanceof EntityPlayer) {
         if (entityIn != Minecraft.getMinecraft().player) {
            NoRender noRender = NoRender.getInstance();
            if (noRender != null && noRender.isEnabled() && (Boolean)noRender.optimizePlayerEntity.getValue()) {
               ci.cancel();
            }

         }
      }
   }
}
