package fat.vonware.mixin.mixins.player;

import fat.vonware.util.LogoutSpots.GhostRenderState;
import fat.vonware.util.shader.FrameBufferShader;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.layers.LayerCape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({LayerCape.class})
public class MixinLayerCape {
   @Inject(
      method = {"doRenderLayer"},
      at = {@At("HEAD")}
   )
   private void vonware$beginCapeOcclusionLayer(AbstractClientPlayer player, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, float scale, CallbackInfo ci) {
      FrameBufferShader.applyPlayerLayerOcclusionState();
   }

   @Inject(
      method = {"doRenderLayer"},
      at = {@At("RETURN")}
   )
   private void vonware$endCapeOcclusionLayer(AbstractClientPlayer player, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, float scale, CallbackInfo ci) {
      FrameBufferShader.applyPlayerLayerOcclusionState();
   }

   @Redirect(
      method = {"doRenderLayer"},
      at = @At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/renderer/GlStateManager;color(FFFF)V"
)
   )
   private void vonware$applyGhostCapeAlpha(float red, float green, float blue, float alpha) {
      GlStateManager.color(red, green, blue, GhostRenderState.capeAlpha);
   }
}
