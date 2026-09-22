package fat.vonware.features.modules.render;

import fat.vonware.Global;
import fat.vonware.event.events.TotemPopEvent;
import fat.vonware.features.gui.components.items.buttons.render.ColorPickerConfig;
import fat.vonware.features.modules.Module;
import fat.vonware.features.setting.ColorSetting;
import fat.vonware.features.setting.Setting;
import fat.vonware.util.shader.BloomRenderUtil;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

public class PopEffects extends Module {
   public static PopEffects INSTANCE;
   public final Setting<Page> page;
   public final Setting<PopType> popType;
   public final Setting<Boolean> selfPop;
   public final Setting<Integer> alpha;
   public final Setting<Integer> olAlpha;
   public final Setting<Float> fadeTime;
   public final Setting<Float> duration;
   public final Setting<Boolean> bloom;
   public final Setting<Float> bloomIntensity;
   public final Setting<Float> bloomRadius;
   public final Setting<Float> lineWidth;
   public final ColorSetting chamsColor;
   public final ColorSetting firstColor;
   public final ColorSetting secondColor;
   public final Setting<Boolean> thirdColorEnabled;
   public final ColorSetting thirdColor;
   public final Setting<Boolean> shrink;
   public final Setting<Boolean> spin;
   public final Setting<Float> rotationSpeed;
   public final Setting<Double> yAnimations;
   public List<PopData> popDataList;

   public PopEffects() {
      super("PopEffects", "totem pop effects", Module.Category.RENDER, true, false, false);
      this.page = new Setting<Page>("Page", PopEffects.Page.General);
      this.popType = new Setting<PopType>("PopType", PopEffects.PopType.Chams, (v) -> this.page.getValue() == PopEffects.Page.General);
      this.selfPop = new Setting<Boolean>("SelfPop", false, (v) -> this.page.getValue() == PopEffects.Page.General);
      this.alpha = new Setting<Integer>("Alpha", 70, 0, 255, (v) -> this.page.getValue() == PopEffects.Page.General);
      this.olAlpha = new Setting<Integer>("OL Alpha", 190, 0, 255, (v) -> this.page.getValue() == PopEffects.Page.General);
      this.fadeTime = new Setting<Float>("FadeTime", 1.5F, 0.0F, 5.0F, (v) -> this.page.getValue() == PopEffects.Page.General);
      this.duration = new Setting<Float>("Duration", 1.0F, 0.1F, 2.5F, (v) -> this.page.getValue() == PopEffects.Page.General && (this.popType.getValue() == PopEffects.PopType.Particles || this.popType.getValue() == PopEffects.PopType.Both));
      this.bloom = new Setting<Boolean>("Bloom", false, (v) -> this.page.getValue() == PopEffects.Page.General && this.popType.getValue() != PopEffects.PopType.Particles);
      this.bloomIntensity = new Setting<Float>("Bloom-Intensity", 0.8F, 0.1F, 1.0F, (v) -> this.page.getValue() == PopEffects.Page.General && (Boolean)this.bloom.getValue());
      this.bloomRadius = new Setting<Float>("Bloom-Radius", 12.0F, 2.0F, 25.0F, (v) -> this.page.getValue() == PopEffects.Page.General && (Boolean)this.bloom.getValue());
      this.lineWidth = new Setting<Float>("LineWidth", 2.0F, 0.1F, 8.0F, (v) -> this.page.getValue() == PopEffects.Page.General && this.popType.getValue() != PopEffects.PopType.Particles);
      this.chamsColor = new ColorSetting("ChamColor", 16777215, true, (v) -> this.page.getValue() == PopEffects.Page.General);
      this.firstColor = new ColorSetting("FirstColor", 16762930, false, (v) -> this.page.getValue() == PopEffects.Page.General && (this.popType.getValue() == PopEffects.PopType.Particles || this.popType.getValue() == PopEffects.PopType.Both));
      this.secondColor = new ColorSetting("SecondColor", 2004530, false, (v) -> this.page.getValue() == PopEffects.Page.General && (this.popType.getValue() == PopEffects.PopType.Particles || this.popType.getValue() == PopEffects.PopType.Both));
      this.thirdColorEnabled = new Setting<Boolean>("UseThirdColor", false, (v) -> this.page.getValue() == PopEffects.Page.General && (this.popType.getValue() == PopEffects.PopType.Particles || this.popType.getValue() == PopEffects.PopType.Both));
      this.thirdColor = new ColorSetting("ThirdColor", 3302600, false, (v) -> this.page.getValue() == PopEffects.Page.General && (Boolean)this.thirdColorEnabled.getValue() && (this.popType.getValue() == PopEffects.PopType.Particles || this.popType.getValue() == PopEffects.PopType.Both));
      this.shrink = new Setting<Boolean>("Shrink", true, (v) -> this.page.getValue() == PopEffects.Page.Animations);
      this.spin = new Setting<Boolean>("Spin", false, (v) -> this.page.getValue() == PopEffects.Page.Animations);
      this.rotationSpeed = new Setting<Float>("Rotation-Speed", 360.0F, 0.0F, 1440.0F, (v) -> this.page.getValue() == PopEffects.Page.Animations && (Boolean)this.spin.getValue());
      this.yAnimations = new Setting<Double>("Y-Animation", (double)0.0F, (double)-7.0F, (double)7.0F, (v) -> this.page.getValue() == PopEffects.Page.Animations);
      this.popDataList = new ArrayList();
      INSTANCE = this;
      this.register(this.page);
      this.register(this.popType);
      this.register(this.chamsColor);
      this.register(this.firstColor);
      this.register(this.secondColor);
      this.register(this.thirdColorEnabled);
      this.register(this.thirdColor);
      this.register(this.alpha);
      this.register(this.olAlpha);
      this.register(this.fadeTime);
      this.register(this.selfPop);
      this.register(this.duration);
      this.register(this.bloom);
      this.register(this.bloomIntensity);
      this.register(this.bloomRadius);
      this.register(this.lineWidth);
      this.register(this.shrink);
      this.register(this.spin);
      this.register(this.rotationSpeed);
      this.register(this.yAnimations);
   }

   private Color getColorFromInt(int colorInt) {
      return new Color(colorInt >> 16 & 255, colorInt >> 8 & 255, colorInt & 255);
   }

   public Color getThirdParticleColor() {
      Color base = ColorPickerConfig.getState(this.thirdColor).useGlobalColor ? Global.getGlobalColor() : this.getColorFromInt((Integer)this.thirdColor.getValue());
      return new Color(base.getRed(), base.getGreen(), base.getBlue(), (Integer)this.alpha.getValue());
   }

   public Color getColor() {
      Color base = ColorPickerConfig.getState(this.chamsColor).useGlobalColor ? Global.getGlobalColorRainbowS() : this.getColorFromInt((Integer)this.chamsColor.getValue());
      return new Color(base.getRed(), base.getGreen(), base.getBlue(), (Integer)this.alpha.getValue());
   }

   public Color getOutlineColor() {
      Color base = ColorPickerConfig.getState(this.chamsColor).useGlobalColor ? Global.getGlobalColorRainbowS() : this.getColorFromInt((Integer)this.chamsColor.getValue());
      return new Color(base.getRed(), base.getGreen(), base.getBlue(), (Integer)this.olAlpha.getValue());
   }

   public Color getFirstParticleColor() {
      Color base = ColorPickerConfig.getState(this.firstColor).useGlobalColor ? Global.getGlobalColor() : this.getColorFromInt((Integer)this.firstColor.getValue());
      return new Color(base.getRed(), base.getGreen(), base.getBlue(), (Integer)this.alpha.getValue());
   }

   public Color getSecondParticleColor() {
      Color base = ColorPickerConfig.getState(this.secondColor).useGlobalColor ? Global.getGlobalColor() : this.getColorFromInt((Integer)this.secondColor.getValue());
      return new Color(base.getRed(), base.getGreen(), base.getBlue(), (Integer)this.alpha.getValue());
   }

   @SubscribeEvent
   public void onTotemPop(TotemPopEvent event) {
      this.popDataList.add(new PopData(event.getEntity(), System.currentTimeMillis(), event.getEntity().posX, event.getEntity().posY, event.getEntity().posZ, event.getEntity() instanceof AbstractClientPlayer && ((AbstractClientPlayer)event.getEntity()).getSkinType().equals("slim")));
   }

   @SubscribeEvent
   public void onRenderWorldLast(RenderWorldLastEvent event) {
      if (this.popType.getValue() != PopEffects.PopType.Particles) {
         try {
            for(PopData data : this.popDataList) {
               EntityPlayer player = data.getPlayer();
               if (player != mc.player || (Boolean)this.selfPop.getValue()) {
                  ModelPlayer model = data.getModel();
                  double x = data.getX() - mc.getRenderManager().viewerPosX;
                  double y = data.getY() - mc.getRenderManager().viewerPosY;
                  y += (Double)this.yAnimations.getValue() * (double)(System.currentTimeMillis() - data.getTime()) / ((double)(Float)this.fadeTime.getValue() * (double)1000.0F);
                  double z = data.getZ() - mc.getRenderManager().viewerPosZ;
                  float progress = (float)(System.currentTimeMillis() - data.getTime()) / ((Float)this.fadeTime.getValue() * 1000.0F);
                  progress = MathHelper.clamp(progress, 0.0F, 1.0F);
                  GlStateManager.pushMatrix();
                  GL11.glPushAttrib(1048575);
                  GL11.glPushMatrix();
                  GL11.glDisable(3008);
                  GL11.glEnable(3042);
                  GL11.glBlendFunc(770, 771);
                  GL11.glDisable(3553);
                  GL11.glDisable(2929);
                  GL11.glDepthMask(false);
                  GL11.glEnable(2884);
                  GL11.glEnable(2848);
                  GL11.glHint(3154, 4353);
                  GL11.glDisable(2896);
                  GlStateManager.translate(x, y, z);
                  GlStateManager.rotate(180.0F - data.yaw, 0.0F, 1.0F, 0.0F);
                  Color boxColor = this.getColor();
                  Color outlineColor = this.getOutlineColor();
                  float maxBoxAlpha = (float)boxColor.getAlpha();
                  float maxOutlineAlpha = (float)outlineColor.getAlpha();
                  float alphaBoxAmount = maxBoxAlpha / ((Float)this.fadeTime.getValue() * 1000.0F);
                  float alphaOutlineAmount = maxOutlineAlpha / ((Float)this.fadeTime.getValue() * 1000.0F);
                  int fadeBoxAlpha = MathHelper.clamp((int)((double)alphaBoxAmount * ((double)data.getTime() + (double)(Float)this.fadeTime.getValue() * (double)1000.0F - (double)System.currentTimeMillis())), 0, (int)maxBoxAlpha);
                  int fadeOutlineAlpha = MathHelper.clamp((int)((double)alphaOutlineAmount * ((double)data.getTime() + (double)(Float)this.fadeTime.getValue() * (double)1000.0F - (double)System.currentTimeMillis())), 0, (int)maxOutlineAlpha);
                  Color box = new Color(boxColor.getRed(), boxColor.getGreen(), boxColor.getBlue(), fadeBoxAlpha);
                  Color out = new Color(outlineColor.getRed(), outlineColor.getGreen(), outlineColor.getBlue(), fadeOutlineAlpha);
                  GlStateManager.enableRescaleNormal();
                  GlStateManager.scale(-1.0F, -1.0F, 1.0F);
                  double widthX = player.getEntityBoundingBox().maxX - player.getRenderBoundingBox().minX + (double)1.0F;
                  double widthZ = player.getEntityBoundingBox().maxZ - player.getEntityBoundingBox().minZ + (double)1.0F;
                  this.applyAnimation(progress, widthX, (double)player.height, widthZ);
                  GlStateManager.translate(0.0F, -1.501F, 0.0F);
                  color(box);
                  GL11.glPolygonMode(1032, 6914);
                  model.render(data.player, data.limbSwing, data.limbSwingAmount, (float)data.ticksExisted, data.yawHead, data.pitch, 0.0625F);
                  color(out);
                  GL11.glLineWidth((Float)this.lineWidth.getValue());
                  GL11.glPolygonMode(1032, 6913);
                  model.render(data.player, data.limbSwing, data.limbSwingAmount, (float)data.ticksExisted, data.yawHead, data.pitch, 0.0625F);
                  GL11.glEnable(2896);
                  GL11.glDisable(2848);
                  GL11.glEnable(3553);
                  GL11.glEnable(2929);
                  GL11.glDisable(3042);
                  GL11.glEnable(3008);
                  GL11.glDepthMask(true);
                  GL11.glCullFace(1029);
                  GL11.glPopMatrix();
                  GL11.glPopAttrib();
                  GlStateManager.popMatrix();
               }
            }

            if ((Boolean)this.bloom.getValue() && !this.popDataList.isEmpty()) {
               for(PopData data : new ArrayList<PopData>(this.popDataList)) {
                  if (data.getPlayer() != mc.player || (Boolean)this.selfPop.getValue()) {
                     this.renderBloomPop(event.getPartialTicks(), data);
                  }
               }
            }

            this.popDataList.removeIf((e) -> (double)e.getTime() + (double)(Float)this.fadeTime.getValue() * (double)1000.0F < (double)System.currentTimeMillis());
         } catch (Exception var27) {
         }

      }
   }

   private void renderBloomPop(float partialTicks, PopData data) {
      long now = System.currentTimeMillis();
      float durationMs = Math.max(1.0F, (Float)this.fadeTime.getValue() * 1000.0F);
      float progress = MathHelper.clamp((float)(now - data.getTime()) / durationMs, 0.0F, 1.0F);
      float remaining = 1.0F - progress;
      if (!(remaining <= 0.001F)) {
         Color inside = this.getColor();
         Color outside = this.getOutlineColor();
         BloomRenderUtil.render(partialTicks, (Float)this.bloomIntensity.getValue(), (Float)this.bloomRadius.getValue(), inside, outside, (Float)this.lineWidth.getValue(), remaining, () -> this.renderBloomMask(data, progress));
      }
   }

   private void renderBloomMask(PopData data, float progress) {
      EntityPlayer player = data.getPlayer();
      double x = data.getX() - mc.getRenderManager().viewerPosX;
      double y = data.getY() - mc.getRenderManager().viewerPosY;
      y += (Double)this.yAnimations.getValue() * (double)progress;
      double z = data.getZ() - mc.getRenderManager().viewerPosZ;
      GlStateManager.pushMatrix();

      try {
         GlStateManager.disableAlpha();
         GlStateManager.enableBlend();
         GlStateManager.blendFunc(770, 771);
         GlStateManager.disableTexture2D();
         GlStateManager.disableLighting();
         GlStateManager.disableDepth();
         GlStateManager.depthMask(false);
         GlStateManager.disableCull();
         GlStateManager.translate(x, y, z);
         GlStateManager.rotate(180.0F - data.yaw, 0.0F, 1.0F, 0.0F);
         GlStateManager.enableRescaleNormal();
         GlStateManager.scale(-1.0F, -1.0F, 1.0F);
         double widthX = player.getEntityBoundingBox().maxX - player.getRenderBoundingBox().minX + (double)1.0F;
         double widthZ = player.getEntityBoundingBox().maxZ - player.getEntityBoundingBox().minZ + (double)1.0F;
         this.applyAnimation(progress, widthX, (double)player.height, widthZ);
         GlStateManager.translate(0.0F, -1.501F, 0.0F);
         GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
         GL11.glPolygonMode(1032, 6914);
         data.getModel().render(data.player, data.limbSwing, data.limbSwingAmount, (float)data.ticksExisted, data.yawHead, data.pitch, 0.0625F);
      } finally {
         GL11.glPolygonMode(1032, 6914);
         GlStateManager.depthMask(true);
         GlStateManager.enableDepth();
         GlStateManager.enableTexture2D();
         GlStateManager.enableAlpha();
         GlStateManager.enableCull();
         GlStateManager.disableRescaleNormal();
         GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
         GlStateManager.popMatrix();
      }

   }

   private void applyAnimation(float progress, double widthX, double height, double widthZ) {
      if (!(Boolean)this.shrink.getValue() && !(Boolean)this.spin.getValue()) {
         GlStateManager.scale(widthX, height, widthZ);
      } else {
         if ((Boolean)this.spin.getValue()) {
            float rotation = progress * (Float)this.rotationSpeed.getValue();
            GlStateManager.rotate(rotation, 0.0F, 1.0F, 0.0F);
         }

         float scale = (Boolean)this.shrink.getValue() ? 1.0F - progress : 1.0F;
         GlStateManager.scale(widthX * (double)scale, height * (double)scale, widthZ * (double)scale);
      }
   }

   public static void color(Color color) {
      GL11.glColor4f((float)color.getRed() / 255.0F, (float)color.getGreen() / 255.0F, (float)color.getBlue() / 255.0F, (float)color.getAlpha() / 255.0F);
   }

   public static class PopData {
      public EntityPlayer player;
      public ModelPlayer model;
      public long time;
      public float limbSwing;
      public float yaw;
      public float limbSwingAmount;
      public float yawHead;
      public float pitch;
      public int ticksExisted;
      public double x;
      public double y;
      public double z;

      public PopData(EntityPlayer player, long time, double x, double y, double z, boolean slim) {
         this.player = player;
         this.time = time;
         this.x = x;
         this.y = y - (player.isSneaking() ? (double)0.125F : (double)0.0F);
         this.z = z;
         this.model = new ModelPlayer(0.0F, ((AbstractClientPlayer)player).getSkinType().equalsIgnoreCase("slim"));
         this.model.bipedBodyWear.showModel = false;
         this.model.bipedHeadwear.showModel = true;
         this.model.bipedHead.showModel = false;
         this.model.bipedRightArmwear.showModel = false;
         this.model.bipedRightLegwear.showModel = false;
         this.model.bipedLeftArmwear.showModel = false;
         this.model.bipedLeftLegwear.showModel = false;
         this.limbSwing = player.limbSwing;
         this.limbSwingAmount = player.limbSwingAmount;
         this.ticksExisted = player.ticksExisted;
         this.yawHead = player.rotationYawHead;
         this.pitch = player.rotationPitch;
         this.yaw = player.rotationYaw;
      }

      public EntityPlayer getPlayer() {
         return this.player;
      }

      public long getTime() {
         return this.time;
      }

      public double getX() {
         return this.x;
      }

      public double getY() {
         return this.y;
      }

      public double getZ() {
         return this.z;
      }

      public ModelPlayer getModel() {
         return this.model;
      }
   }

   public static enum PopType {
      Chams,
      Particles,
      Both;
   }

   public static enum Page {
      General,
      Animations;
   }
}
