package fat.vonware.mixin.mixins.entity;

import fat.vonware.util.shader.FrameBufferShader;
import net.minecraft.client.renderer.entity.layers.LayerHeldItem;
import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({LayerHeldItem.class})
public class MixinLayerHeldItem {
   @Inject(
      method = {"doRenderLayer"},
      at = {@At("HEAD")}
   )
   private void vonware$beginHeldItemOcclusionLayer(EntityLivingBase entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, float scale, CallbackInfo ci) {
      if (FrameBufferShader.isPlayerOcclusionCapture()) {
         FrameBufferShader.beginHeldItemOcclusionLayer();
      }

   }

   @Inject(
      method = {"doRenderLayer"},
      at = {@At("RETURN")}
   )
   private void vonware$endHeldItemOcclusionLayer(EntityLivingBase entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, float scale, CallbackInfo ci) {
      if (FrameBufferShader.isPlayerOcclusionCapture()) {
         FrameBufferShader.endHeldItemOcclusionLayer();
      }

   }
}
