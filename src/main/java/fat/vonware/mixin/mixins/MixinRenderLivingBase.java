package fat.vonware.mixin.mixins;

import fat.vonware.Vonware;
import fat.vonware.features.modules.client.Colors;
import fat.vonware.features.modules.render.Chams;
import fat.vonware.features.modules.render.Skeleton;
import fat.vonware.util.Util;
import fat.vonware.util.Animations.render.RenderMethods;
import fat.vonware.util.shader.FrameBufferShader;
import java.awt.Color;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.GlStateManager.DestFactor;
import net.minecraft.client.renderer.GlStateManager.SourceFactor;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
   value = {RenderLivingBase.class},
   priority = 10000
)
public abstract class MixinRenderLivingBase<T extends EntityLivingBase> extends Render<T> {
   @Shadow
   protected ModelBase mainModel;

   protected MixinRenderLivingBase(RenderManager renderManager) {
      super(renderManager);
   }

   @Inject(
      method = {"renderLayers"},
      at = {@At("HEAD")}
   )
   private void vonware$beginPlayerLayerOcclusionCleanup(T entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, float scaleIn, CallbackInfo ci) {
      if (entitylivingbaseIn instanceof EntityPlayer && FrameBufferShader.isPlayerOcclusionCleanup()) {
         FrameBufferShader.beginPlayerLayerCleanup();
      }

   }

   @Inject(
      method = {"renderLayers"},
      at = {@At("RETURN")}
   )
   private void vonware$endPlayerLayerOcclusionCleanup(T entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, float scaleIn, CallbackInfo ci) {
      if (entitylivingbaseIn instanceof EntityPlayer && FrameBufferShader.isPlayerOcclusionCleanup()) {
         FrameBufferShader.endPlayerLayerCleanup();
      }

   }

   @Inject(
      method = {"renderLayers"},
      at = {@At("RETURN")}
   )
   public void renderLayersHook(T entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, float scaleIn, CallbackInfo ci) {
      Skeleton SKELETON = (Skeleton)Vonware.moduleManager.getModuleByClass(Skeleton.class);
      if (SKELETON.isEnabled()) {
         SKELETON.onRenderModel(this.mainModel, entitylivingbaseIn);
      }

   }

   @Inject(
      method = {"renderModel"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/model/ModelBase;render(Lnet/minecraft/entity/Entity;FFFFFF)V",
   shift = At.Shift.BEFORE
)},
      cancellable = true
   )
   private void renderModelHook(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale, CallbackInfo info) {
      if (!(entity instanceof EntityPlayer) || !FrameBufferShader.isPlayerOcclusionCapture()) {
         Chams mod = Chams.INSTANCE;
         RenderLivingBase<?> renderLiving = (RenderLivingBase)RenderLivingBase.class.cast(this);
         ModelBase model = renderLiving.getMainModel();
         if (mod != null && mod.isOn() && entity instanceof EntityPlayer && mod.playerEnum.getValue() != Chams.PlayerEnum.Off && !mod.isNormalForPlayers()) {
            info.cancel();
            if (!(Boolean)mod.self.getValue() && entity instanceof EntityPlayerSP) {
               model.render(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
            } else {
               Color fillBase = mod.getPlayerFillColor();
               Color outlineBase = mod.getPlayerOutlineColor();
               boolean isFriend = entity instanceof EntityPlayer && Vonware.friendManager.isFriend(entity.getName());
               Color baseColor;
               Color lineColor;
               if (isFriend) {
                  Color friendCol = Colors.getInstance().getFriendColor();
                  baseColor = new Color(friendCol.getRed(), friendCol.getGreen(), friendCol.getBlue(), (Integer)mod.visibleAlpha.getValue());
                  lineColor = new Color(friendCol.getRed(), friendCol.getGreen(), friendCol.getBlue(), (Integer)mod.lineAlpha.getValue());
               } else if ((Boolean)mod.hurt.getValue() && entity.hurtTime > 0) {
                  int r = Math.min(255, (int)((double)fillBase.getRed() * 0.85 + (double)38.25F));
                  int g = (int)((double)fillBase.getGreen() * 0.92);
                  int b = (int)((double)fillBase.getBlue() * 0.92);
                  baseColor = new Color(r, g, b, (Integer)mod.visibleAlpha.getValue());
                  lineColor = new Color(outlineBase.getRed(), outlineBase.getGreen(), outlineBase.getBlue(), (Integer)mod.lineAlpha.getValue());
               } else {
                  baseColor = new Color(fillBase.getRed(), fillBase.getGreen(), fillBase.getBlue(), (Integer)mod.visibleAlpha.getValue());
                  lineColor = new Color(outlineBase.getRed(), outlineBase.getGreen(), outlineBase.getBlue(), (Integer)mod.lineAlpha.getValue());
               }

               boolean isLineMode = mod.playerEnum.getValue() == Chams.PlayerEnum.Line;
               boolean isChamMode = mod.playerEnum.getValue() == Chams.PlayerEnum.Cham;
               boolean isBothMode = mod.playerEnum.getValue() == Chams.PlayerEnum.Both;
               if (isLineMode) {
                  model.render(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
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
                     model.render(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
                     GL11.glEnable(2929);
                     GL11.glDepthMask(true);
                  }

                  GL11.glEnable(10754);
                  RenderMethods.glColor(new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), (Integer)mod.visibleAlpha.getValue()));
                  model.render(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
                  GL11.glPopAttrib();
               }

               if (isLineMode || isBothMode) {
                  GL11.glPushMatrix();
                  GL11.glPushAttrib(1048575);
                  GL11.glPolygonMode(1032, 6913);
                  GL11.glDisable(3553);
                  GL11.glDisable(2896);
                  GL11.glEnable(2848);
                  GL11.glEnable(3042);
                  GlStateManager.blendFunc(770, 771);
                  GL11.glDisable(2929);
                  GL11.glDepthMask(false);
                  RenderMethods.glColor(lineColor);
                  GlStateManager.glLineWidth((Float)mod.lineWidth.getValue());
                  model.render(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
                  GL11.glPopAttrib();
                  GL11.glPopMatrix();
               }

               if ((isChamMode || isBothMode) && (Boolean)mod.shine.getValue()) {
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
                     int baseAlpha = (int)((float)(Integer)mod.visibleAlpha.getValue() * 0.6F);
                     int layerAlpha = Math.min(180, baseAlpha + i * 25);
                     Color color = new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), layerAlpha);
                     RenderMethods.glColor(color);
                     GlStateManager.matrixMode(5890);
                     GlStateManager.loadIdentity();
                     GL11.glScalef(0.8F, 0.8F, 0.8F);
                     GlStateManager.rotate(45.0F - (float)i * 45.0F, 0.0F, 0.0F, 1.0F);
                     GlStateManager.matrixMode(5888);
                     model.render(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
                  }

                  GlStateManager.matrixMode(5890);
                  GL11.glPopMatrix();
                  GlStateManager.matrixMode(5888);
                  GlStateManager.bindTexture(0);
                  if ((Boolean)mod.xqz.getValue()) {
                     GL11.glDepthMask(true);
                     GL11.glEnable(2929);
                  }

                  GlStateManager.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA);
                  GL11.glEnable(2896);
                  GL11.glPopAttrib();
                  GL11.glPopMatrix();
               }

               this.restoreGLState();
            }
         }
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
