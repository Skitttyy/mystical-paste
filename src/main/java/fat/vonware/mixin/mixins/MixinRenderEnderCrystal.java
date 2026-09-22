package fat.vonware.mixin.mixins;

import fat.vonware.features.modules.render.Chams;
import fat.vonware.features.modules.render.NoRender;
import fat.vonware.util.Util;
import fat.vonware.util.Animations.render.RenderMethods;
import java.awt.Color;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.GlStateManager.DestFactor;
import net.minecraft.client.renderer.GlStateManager.SourceFactor;
import net.minecraft.client.renderer.entity.RenderEnderCrystal;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityEnderCrystal;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
   value = {RenderEnderCrystal.class},
   priority = 10000
)
public class MixinRenderEnderCrystal {
   @Inject(
      method = {"doRender(Lnet/minecraft/entity/item/EntityEnderCrystal;DDDFF)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onDoRenderFOVCheck(EntityEnderCrystal entity, double x, double y, double z, float entityYaw, float partialTicks, CallbackInfo ci) {
      if (!NoRender.getInstance().shouldRenderCrystal(entity)) {
         ci.cancel();
      }

   }

   @Inject(
      method = {"doRender(Lnet/minecraft/entity/item/EntityEnderCrystal;DDDFF)V"},
      at = {@At("HEAD")}
   )
   private void onRenderCrystalPre(EntityEnderCrystal entity, double x, double y, double z, float entityYaw, float partialTicks, CallbackInfo ci) {
      Chams mod = Chams.INSTANCE;
      if (mod != null && mod.isOn() && mod.isNormalForCrystals()) {
         GlStateManager.pushMatrix();
         GlStateManager.depthMask(true);
         GL11.glEnable(32823);
         GL11.glDepthRange((double)0.0F, 0.01);
      }

   }

   @Inject(
      method = {"doRender(Lnet/minecraft/entity/item/EntityEnderCrystal;DDDFF)V"},
      at = {@At("RETURN")}
   )
   private void onRenderCrystalPost(EntityEnderCrystal entity, double x, double y, double z, float entityYaw, float partialTicks, CallbackInfo ci) {
      Chams mod = Chams.INSTANCE;
      if (mod != null && mod.isOn() && mod.isNormalForCrystals()) {
         GlStateManager.depthMask(false);
         GL11.glDisable(32823);
         GL11.glDepthRange((double)0.0F, (double)1.0F);
         GlStateManager.popMatrix();
         this.restoreGLState();
      }

   }

   @Inject(
      method = {"doRender"},
      at = {@At("HEAD")}
   )
   private void onDoRenderPre(EntityEnderCrystal entity, double x, double y, double z, float entityYaw, float partialTicks, CallbackInfo ci) {
      NoRender noRender = NoRender.getInstance();
      if (noRender.isEnabled() && (Boolean)noRender.noCluster.getValue()) {
         float alpha = noRender.getClusterAlpha(entity);
         GlStateManager.color(1.0F, 1.0F, 1.0F, alpha);
         GlStateManager.enableBlend();
         GlStateManager.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA);
      }

   }

   @Inject(
      method = {"doRender"},
      at = {@At("RETURN")}
   )
   private void onDoRenderPost(EntityEnderCrystal entity, double x, double y, double z, float entityYaw, float partialTicks, CallbackInfo ci) {
      NoRender noRender = NoRender.getInstance();
      if (noRender.isEnabled() && (Boolean)noRender.noCluster.getValue()) {
         GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      }

   }

   @Redirect(
      method = {"doRender(Lnet/minecraft/entity/item/EntityEnderCrystal;DDDFF)V"},
      at = @At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/model/ModelBase;render(Lnet/minecraft/entity/Entity;FFFFFF)V"
)
   )
   public void renderModelBaseHook(ModelBase model, Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
      Chams mod = Chams.INSTANCE;
      if (mod != null && mod.isOn() && mod.isNormalForCrystals()) {
         GlStateManager.pushMatrix();
         GlStateManager.scale((Float)mod.scale.getValue(), (Float)mod.scale.getValue(), (Float)mod.scale.getValue());
         model.render(entity, limbSwing, limbSwingAmount * (Float)mod.spin.getValue(), ageInTicks * (Float)mod.bounce.getValue(), netHeadYaw, headPitch, scale);
         GlStateManager.popMatrix();
      } else if (mod != null && mod.isOn() && !mod.isNormalForCrystals()) {
         GlStateManager.pushMatrix();
         GlStateManager.scale((Float)mod.scale.getValue(), (Float)mod.scale.getValue(), (Float)mod.scale.getValue());
         float modifiedLimbSwingAmount = limbSwingAmount * (Float)mod.spin.getValue();
         float modifiedAgeInTicks = ageInTicks * (Float)mod.bounce.getValue();
         if (mod.crystalEnum.getValue() != Chams.CrystalEnum.Off) {
            Color baseColor = mod.getCrystalFillColor();
            boolean isLineMode = mod.crystalEnum.getValue() == Chams.CrystalEnum.Line;
            boolean isChamMode = mod.crystalEnum.getValue() == Chams.CrystalEnum.Cham;
            boolean isBothMode = mod.crystalEnum.getValue() == Chams.CrystalEnum.Both;
            if (isLineMode) {
               model.render(entity, limbSwing, modifiedLimbSwingAmount, modifiedAgeInTicks, netHeadYaw, headPitch, scale);
            }

            if (isChamMode || isBothMode) {
               GL11.glPushAttrib(1048575);
               GL11.glDisable(3008);
               if (!(Boolean)mod.texture.getValue()) {
                  GL11.glDisable(3553);
               }

               GL11.glDisable(2896);
               GL11.glEnable(3042);
               GL11.glBlendFunc(770, 771);
               if ((Boolean)mod.xqz.getValue()) {
                  GL11.glDisable(2929);
                  GL11.glDepthMask(false);
                  RenderMethods.glColor(new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), (Integer)mod.xqzAlpha.getValue()));
                  model.render(entity, limbSwing, modifiedLimbSwingAmount, modifiedAgeInTicks, netHeadYaw, headPitch, scale);
                  GL11.glEnable(2929);
                  GL11.glDepthMask(true);
               }

               GL11.glEnable(10754);
               RenderMethods.glColor(new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), (Integer)mod.visibleAlpha.getValue()));
               model.render(entity, limbSwing, modifiedLimbSwingAmount, modifiedAgeInTicks, netHeadYaw, headPitch, scale);
               GL11.glPopAttrib();
            }

            if (isLineMode || isBothMode) {
               Color crystalOutline = mod.getCrystalOutlineColor();
               Color lineColor = new Color(crystalOutline.getRed(), crystalOutline.getGreen(), crystalOutline.getBlue(), (Integer)mod.lineAlpha.getValue());
               GL11.glPushMatrix();
               GL11.glPushAttrib(1048575);
               GL11.glPolygonMode(1032, 6913);
               GL11.glDisable(3553);
               GL11.glDisable(2896);
               GL11.glDisable(2929);
               GL11.glDisable(3008);
               GL11.glEnable(2848);
               GL11.glEnable(3042);
               GL11.glBlendFunc(770, 771);
               GL11.glDepthMask(false);
               RenderMethods.glColor(lineColor);
               GlStateManager.glLineWidth((Float)mod.lineWidth.getValue());
               model.render(entity, limbSwing, modifiedLimbSwingAmount, modifiedAgeInTicks, netHeadYaw, headPitch, scale);
               GL11.glPopAttrib();
               GL11.glPopMatrix();
            }

            if ((isChamMode || isBothMode) && (Boolean)mod.shine.getValue() && entity instanceof EntityEnderCrystal) {
               GL11.glPushMatrix();
               GL11.glPushAttrib(1048575);
               GlStateManager.matrixMode(5890);
               GL11.glPushMatrix();
               GlStateManager.matrixMode(5888);
               GL11.glPolygonMode(1032, 6914);
               GL11.glDisable(2896);
               if ((Boolean)mod.xqz.getValue()) {
                  GL11.glDisable(2929);
                  GL11.glDepthMask(false);
               }

               GL11.glEnable(3042);
               GlStateManager.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE);
               Util.mc.getRenderManager().renderEngine.bindTexture(new ResourceLocation("textures/misc/enchanted_item_glint.png"));

               for(int i = 0; i < 2; ++i) {
                  int layerAlpha = Math.min(255, (Integer)mod.visibleAlpha.getValue() + i * 30);
                  Color color = new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), layerAlpha);
                  RenderMethods.glColor(color);
                  GlStateManager.matrixMode(5890);
                  GlStateManager.loadIdentity();
                  GL11.glScalef(0.8F, 0.8F, 0.8F);
                  GlStateManager.rotate(45.0F - (float)i * 45.0F, 0.0F, 0.0F, 1.0F);
                  GlStateManager.matrixMode(5888);
                  model.render(entity, limbSwing, modifiedLimbSwingAmount, modifiedAgeInTicks, netHeadYaw, headPitch, scale);
               }

               GlStateManager.matrixMode(5890);
               GL11.glPopMatrix();
               GlStateManager.matrixMode(5888);
               GlStateManager.bindTexture(0);
               GlStateManager.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA);
               if ((Boolean)mod.xqz.getValue()) {
                  GL11.glDepthMask(true);
                  GL11.glEnable(2929);
               }

               GL11.glDepthRange((double)0.0F, (double)1.0F);
               GL11.glEnable(2896);
               GL11.glPopAttrib();
               GL11.glPopMatrix();
            }

            GlStateManager.popMatrix();
            this.restoreGLState();
         } else {
            model.render(entity, limbSwing, modifiedLimbSwingAmount, modifiedAgeInTicks, netHeadYaw, headPitch, scale);
            GlStateManager.popMatrix();
         }

      } else {
         model.render(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
      }
   }

   private void restoreGLState() {
      GlStateManager.bindTexture(0);
      GlStateManager.matrixMode(5890);
      GlStateManager.loadIdentity();
      GlStateManager.matrixMode(5888);
      GL11.glPolygonMode(1032, 6914);
      GlStateManager.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA);
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      GlStateManager.enableDepth();
      GlStateManager.depthMask(true);
      GlStateManager.depthFunc(515);
      GL11.glDepthRange((double)0.0F, (double)1.0F);
      GlStateManager.glLineWidth(1.0F);
      GlStateManager.enableTexture2D();
      GlStateManager.enableLighting();
      GlStateManager.enableAlpha();
      GlStateManager.alphaFunc(516, 0.1F);
      GL11.glDisable(2848);
      GL11.glDisable(32823);
      GL11.glDisable(2960);
      GL11.glColorMask(true, true, true, true);
   }
}
