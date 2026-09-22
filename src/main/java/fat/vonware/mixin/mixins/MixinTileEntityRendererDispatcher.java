package fat.vonware.mixin.mixins;

import fat.vonware.features.modules.render.NoRender;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.tileentity.TileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({TileEntityRendererDispatcher.class})
public class MixinTileEntityRendererDispatcher {
   @Inject(
      method = {"render(Lnet/minecraft/tileentity/TileEntity;DDDFIF)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void onRenderTileEntity(TileEntity tileEntity, double x, double y, double z, float partialTicks, int destroyStage, float alpha, CallbackInfo ci) {
      NoRender noRender = NoRender.getInstance();
      if (!noRender.shouldRenderTileEntity(tileEntity)) {
         ci.cancel();
      } else {
         if (!noRender.shouldRenderBed(tileEntity)) {
            ci.cancel();
         }

      }
   }
}
