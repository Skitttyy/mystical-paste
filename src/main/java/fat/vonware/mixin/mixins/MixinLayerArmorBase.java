package fat.vonware.mixin.mixins;

import fat.vonware.Vonware;
import fat.vonware.features.modules.render.GlintModify;
import fat.vonware.features.modules.render.NoRender;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.layers.LayerArmorBase;
import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({LayerArmorBase.class})
public class MixinLayerArmorBase {
   @Inject(
      method = {"doRenderLayer"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void doRenderLayer(EntityLivingBase entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, float scale, CallbackInfo ci) {
      if (NoRender.getInstance().isEnabled() && (Boolean)NoRender.getInstance().noArmor.getValue()) {
         ci.cancel();
      }

   }

   @Redirect(
      method = {"renderEnchantedGlint"},
      at = @At(
   value = "INVOKE",
   target = "net/minecraft/client/renderer/GlStateManager.color(FFFF)V",
   ordinal = 1
)
   )
   private static void renderEnchantedGlint(float red, float green, float blue, float alpha) {
      GlintModify GLINT_MODIFY = (GlintModify)Vonware.moduleManager.getModuleByClass(GlintModify.class);
      if (GLINT_MODIFY.isArmorGlint()) {
         GlStateManager.color((float)GLINT_MODIFY.getEnchantColor().getRed() / 255.0F, (float)GLINT_MODIFY.getEnchantColor().getGreen() / 255.0F, (float)GLINT_MODIFY.getEnchantColor().getBlue() / 255.0F, (float)GLINT_MODIFY.getEnchantColor().getAlpha() / 255.0F);
      } else {
         GlStateManager.color(red, green, blue, alpha);
      }

   }
}
