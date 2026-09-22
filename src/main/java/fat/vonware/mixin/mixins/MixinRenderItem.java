package fat.vonware.mixin.mixins;

import fat.vonware.Vonware;
import fat.vonware.features.modules.render.GlintModify;
import fat.vonware.util.shader.FrameBufferShader;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin({RenderItem.class})
public class MixinRenderItem {
   @Inject(
      method = {"renderItemModel"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/renderer/RenderItem;renderItem(Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/renderer/block/model/IBakedModel;)V",
   shift = At.Shift.BEFORE
)}
   )
   private void renderItemModel(ItemStack stack, IBakedModel bakedModel, ItemCameraTransforms.TransformType transform, boolean leftHanded, CallbackInfo ci) {
      FrameBufferShader.applyCurrentHeldItemOcclusionState();
   }

   @ModifyArg(
      method = {"renderEffect"},
      at = @At(
   value = "INVOKE",
   target = "net/minecraft/client/renderer/RenderItem.renderModel(Lnet/minecraft/client/renderer/block/model/IBakedModel;I)V"
)
   )
   private int renderEffect(int glintVal) {
      GlintModify GLINT_MODIFY = (GlintModify)Vonware.moduleManager.getModuleByClass(GlintModify.class);
      return GLINT_MODIFY.isEnabled() ? GLINT_MODIFY.getEnchantColor().getRGB() : glintVal;
   }

   @ModifyArgs(
      method = {"renderEffect"},
      at = @At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/renderer/GlStateManager;scale(FFF)V"
)
   )
   public void scaleArgsHook(Args args) {
      GlintModify GLINT_MODIFY = (GlintModify)Vonware.moduleManager.getModuleByClass(GlintModify.class);
      float scale = GLINT_MODIFY.getGlintScale();
      if (GLINT_MODIFY.isEnabled() && scale != 0.0F) {
         args.set(0, scale);
         args.set(1, scale);
         args.set(2, scale);
      }

   }

   @ModifyArgs(
      method = {"renderEffect"},
      at = @At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/renderer/GlStateManager;translate(FFF)V"
)
   )
   public void translateHook(Args args) {
      GlintModify GLINT_MODIFY = (GlintModify)Vonware.moduleManager.getModuleByClass(GlintModify.class);
      if (GLINT_MODIFY.isEnabled()) {
         args.set(0, (Float)args.get(0) * GLINT_MODIFY.getFactor());
      }

   }

   @ModifyArgs(
      method = {"renderEffect"},
      at = @At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/renderer/GlStateManager;rotate(FFFF)V"
)
   )
   public void rotateHook(Args args) {
      GlintModify GLINT_MODIFY = (GlintModify)Vonware.moduleManager.getModuleByClass(GlintModify.class);
      if (GLINT_MODIFY.isEnabled()) {
         args.set(0, (Float)args.get(0) * GLINT_MODIFY.getGlintRotate());
      }

   }
}
