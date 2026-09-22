package fat.vonware.mixin.mixins;

import fat.vonware.Vonware;
import fat.vonware.event.events.RenderItemEvent;
import fat.vonware.event.events.UpdateEquippedItemEvent;
import fat.vonware.features.modules.render.HandVisuals;
import fat.vonware.features.modules.render.NoRender;
import fat.vonware.util.shader.FrameBufferShader;
import java.nio.FloatBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumHandSide;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.common.MinecraftForge;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ItemRenderer.class})
public abstract class MixinItemRenderer {
   @Shadow
   @Final
   public Minecraft mc;
   private boolean injection = true;

   @Shadow
   public abstract void renderItemInFirstPerson(AbstractClientPlayer var1, float var2, float var3, EnumHand var4, float var5, ItemStack var6, float var7);

   @Inject(
      method = {"renderItemInFirstPerson(Lnet/minecraft/client/entity/AbstractClientPlayer;FFLnet/minecraft/util/EnumHand;FLnet/minecraft/item/ItemStack;F)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void renderItemInFirstPersonHook(AbstractClientPlayer player, float p_187457_2_, float p_187457_3_, EnumHand hand, float p_187457_5_, ItemStack stack, float p_187457_7_, CallbackInfo info) {
      if (this.injection) {
         info.cancel();
         HandVisuals handVisuals = HandVisuals.getInstance();
         float xOffset = 0.0F;
         float yOffset = 0.0F;
         this.injection = false;
         if (hand == EnumHand.MAIN_HAND) {
            if (handVisuals.isEnabled() && (Boolean)handVisuals.smallShield.getValue() && !player.getHeldItemMainhand().isEmpty()) {
               xOffset = (Float)handVisuals.mainX.getValue();
               yOffset = (Float)handVisuals.mainY.getValue();
            }
         } else if (handVisuals.isEnabled() && (Boolean)handVisuals.smallShield.getValue() && !player.getHeldItemOffhand().isEmpty()) {
            xOffset = (Float)handVisuals.offX.getValue();
            yOffset = (Float)handVisuals.offY.getValue();
         }

         boolean viewModelActive = handVisuals.isEnabled() && (Boolean)handVisuals.viewModel.getValue() && ((Boolean)handVisuals.viewModelLeft.getValue() || (Boolean)handVisuals.viewModelRight.getValue());
         boolean projectionPushed = false;
         if (viewModelActive) {
            float fov = (Float)handVisuals.viewModelFov.getValue();
            float aspect = (float)this.mc.displayWidth / (float)this.mc.displayHeight;
            GlStateManager.matrixMode(5889);
            GlStateManager.pushMatrix();
            GlStateManager.loadIdentity();
            this.vonware$applyPerspective(fov, aspect, 0.05F, 10.0F);
            GlStateManager.matrixMode(5888);
            projectionPushed = true;
         }

         this.renderItemInFirstPerson(player, p_187457_2_, p_187457_3_, hand, p_187457_5_ + xOffset, stack, p_187457_7_ + yOffset);
         if (projectionPushed) {
            GlStateManager.matrixMode(5889);
            GlStateManager.popMatrix();
            GlStateManager.matrixMode(5888);
         }

         this.injection = true;
      }

   }

   @Redirect(
      method = {"renderItemSide"},
      at = @At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/renderer/RenderItem;renderItem(Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/EntityLivingBase;Lnet/minecraft/client/renderer/block/model/ItemCameraTransforms$TransformType;Z)V"
)
   )
   private void vonware$renderHeldItemWithOcclusionState(RenderItem renderer, ItemStack stack, EntityLivingBase entity, ItemCameraTransforms.TransformType transform, boolean leftHanded) {
      if (!FrameBufferShader.isPlayerOcclusionCapture()) {
         renderer.renderItem(stack, entity, transform, leftHanded);
      } else {
         boolean leftTransform = transform == TransformType.THIRD_PERSON_LEFT_HAND || transform == TransformType.FIRST_PERSON_LEFT_HAND;
         boolean logicalOffhand = entity.getPrimaryHand() == EnumHandSide.RIGHT ? leftTransform : !leftTransform;
         FrameBufferShader.setHeldItemOcclusionContext(logicalOffhand);

         try {
            renderer.renderItem(stack, entity, transform, leftHanded);
         } finally {
            FrameBufferShader.clearHeldItemOcclusionContext();
         }

      }
   }

   @Redirect(
      method = {"renderItemInFirstPerson(Lnet/minecraft/client/entity/AbstractClientPlayer;FFLnet/minecraft/util/EnumHand;FLnet/minecraft/item/ItemStack;F)V"},
      at = @At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/renderer/ItemRenderer;transformSideFirstPerson(Lnet/minecraft/util/EnumHandSide;F)V"
)
   )
   public void vonware$transformRedirect(ItemRenderer renderer, EnumHandSide hand, float y) {
      RenderItemEvent event = new RenderItemEvent(0.56, -0.52 + (double)y * -0.6, -0.72, -0.56, -0.52 + (double)y * -0.6, -0.72, (double)0.0F, (double)0.0F, (double)1.0F, (double)0.0F, (double)0.0F, (double)0.0F, (double)1.0F, (double)0.0F, (double)1.0F, (double)1.0F, (double)1.0F, (double)1.0F, (double)1.0F, (double)1.0F);
      MinecraftForge.EVENT_BUS.post(event);
      if (hand == EnumHandSide.RIGHT) {
         GlStateManager.translate((float)event.getMainX(), (float)event.getMainY(), (float)event.getMainZ());
         GlStateManager.scale((float)event.getMainHandScaleX(), (float)event.getMainHandScaleY(), (float)event.getMainHandScaleZ());
         GlStateManager.rotate((float)event.getMainRAngle(), (float)event.getMainRx(), (float)event.getMainRy(), (float)event.getMainRz());
      } else {
         GlStateManager.translate((float)event.getOffX(), (float)event.getOffY(), (float)event.getOffZ());
         GlStateManager.scale((float)event.getOffHandScaleX(), (float)event.getOffHandScaleY(), (float)event.getOffHandScaleZ());
         GlStateManager.rotate((float)event.getOffRAngle(), (float)event.getOffRx(), (float)event.getOffRy(), (float)event.getOffRz());
      }

   }

   private void vonware$applyPerspective(float fovy, float aspect, float zNear, float zFar) {
      float ymax = (float)((double)zNear * Math.tan((double)fovy * Math.PI / (double)360.0F));
      float ymin = -ymax;
      float xmin = ymin * aspect;
      float xmax = ymax * aspect;
      this.vonware$applyFrustum(xmin, xmax, ymin, ymax, zNear, zFar);
   }

   private void vonware$applyFrustum(float left, float right, float bottom, float top, float zNear, float zFar) {
      FloatBuffer matrix = BufferUtils.createFloatBuffer(16);
      float a = (right + left) / (right - left);
      float b = (top + bottom) / (top - bottom);
      float c = -(zFar + zNear) / (zFar - zNear);
      float d = -(2.0F * zFar * zNear) / (zFar - zNear);
      float[] m = new float[]{2.0F * zNear / (right - left), 0.0F, 0.0F, 0.0F, 0.0F, 2.0F * zNear / (top - bottom), 0.0F, 0.0F, a, b, c, -1.0F, 0.0F, 0.0F, d, 0.0F};
      matrix.put(m).flip();
      GL11.glMultMatrix(matrix);
   }

   @Inject(
      method = {"transformEatFirstPerson"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onTransformEat(float p_187454_1_, EnumHandSide hand, ItemStack stack, CallbackInfo ci) {
      HandVisuals handVisuals = HandVisuals.getInstance();
      if (handVisuals.isEnabled() && (Boolean)handVisuals.minimalEatBob.getValue()) {
         ci.cancel();
         float f = (float)this.mc.player.getItemInUseCount() - p_187454_1_ + 1.0F;
         float f1 = f / (float)stack.getMaxItemUseDuration();
         if (f1 < 0.8F) {
            float f2 = Math.abs(MathHelper.cos(f / 4.0F * (float)Math.PI) * 0.1F);
            GlStateManager.translate(0.0F, f2 * 0.2F, 0.0F);
         }

         float f3 = 1.0F - (float)Math.pow((double)f1, (double)27.0F);
         int i = hand == EnumHandSide.RIGHT ? 1 : -1;
         GlStateManager.translate(f3 * 0.6F * (float)i, f3 * -0.5F, f3 * 0.0F);
         GlStateManager.rotate((float)i * f3 * 90.0F, 0.0F, 1.0F, 0.0F);
         GlStateManager.rotate(f3 * 10.0F, 1.0F, 0.0F, 0.0F);
         GlStateManager.rotate((float)i * f3 * 30.0F, 0.0F, 0.0F, 1.0F);
      }

   }

   @Inject(
      method = {"updateEquippedItem"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onUpdateEquippedItem(CallbackInfo ci) {
      UpdateEquippedItemEvent event = new UpdateEquippedItemEvent();
      MinecraftForge.EVENT_BUS.post(event);
      if (event.isCanceled()) {
         ci.cancel();
      }

   }

   @Inject(
      method = {"renderFireInFirstPerson"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void renderFireInFirstPersonHook(CallbackInfo info) {
      NoRender noRender = (NoRender)Vonware.moduleManager.getModuleByClass(NoRender.class);
      if (noRender != null && noRender.noFire()) {
         info.cancel();
      }

   }
}
