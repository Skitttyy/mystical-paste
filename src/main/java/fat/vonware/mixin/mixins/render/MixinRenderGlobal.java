package fat.vonware.mixin.mixins.render;

import fat.vonware.Vonware;
import fat.vonware.features.modules.render.Environment;
import fat.vonware.util.shader.SkyShaderRenderer;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.vertex.VertexBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({RenderGlobal.class})
public class MixinRenderGlobal {
   @Shadow
   private VertexBuffer sky2VBO;

   @Inject(
      method = {"renderSky(FI)V"},
      at = {@At("TAIL")}
   )
   private void renderSkyHook(float partialTicks, int pass, CallbackInfo ci) {
      Environment env = (Environment)Vonware.moduleManager.getModuleByClass(Environment.class);
      if (env != null && env.isEnabled()) {
         env.renderSkyGlint(partialTicks);
      }

   }

   @Inject(
      method = {"renderSky(FI)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void vonware$renderSky(float partialTicks, int pass, CallbackInfo ci) {
      Environment env = Environment.INSTANCE;
      if (env != null && env.isEnabled()) {
         if ((Boolean)env.skyShader.getValue()) {
            SkyShaderRenderer.INSTANCE.render(partialTicks);
            ci.cancel();
         }
      }
   }

   @Redirect(
      method = {"renderSky(FI)V"},
      at = @At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/renderer/vertex/VertexBuffer;drawArrays(I)V"
)
   )
   private void vonware$skipSkybox(VertexBuffer vbo, int mode) {
      Environment env = Environment.INSTANCE;
      if (env == null || !env.isEnabled() || !(Boolean)env.noSkybox.getValue() || vbo != this.sky2VBO) {
         vbo.drawArrays(mode);
      }
   }
}
