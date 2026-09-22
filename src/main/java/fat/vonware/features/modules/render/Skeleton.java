package fat.vonware.features.modules.render;

import fat.vonware.Global;
import fat.vonware.Vonware;
import fat.vonware.event.events.DeathEvent;
import fat.vonware.event.events.Render3DEvent;
import fat.vonware.features.gui.components.items.buttons.render.ColorPickerConfig;
import fat.vonware.features.modules.Module;
import fat.vonware.features.modules.client.Colors;
import fat.vonware.features.setting.ColorSetting;
import fat.vonware.features.setting.Setting;
import fat.vonware.util.EntityUtil;
import fat.vonware.util.Interpolation;
import fat.vonware.util.Animations.render.RenderMethods;
import java.awt.Color;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.GlStateManager.DestFactor;
import net.minecraft.client.renderer.GlStateManager.SourceFactor;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

public class Skeleton extends Module {
   private final ColorSetting color = new ColorSetting("Color", 16777215, true);
   private final Setting<Float> lineWidth = new Setting<Float>("LineWidth", 1.2F, 0.1F, 4.0F);
   protected final Map<EntityPlayer, float[][]> rotationList;

   public Skeleton() {
      super("Skeleton", "draws a skeleton on players", Module.Category.RENDER, true, false, false);
      this.register(this.color);
      this.register(this.lineWidth);
      this.rotationList = new HashMap();
   }

   protected Color getColor() {
      if (ColorPickerConfig.getState(this.color).useGlobalColor) {
         return Global.getGlobalColorRainbowS();
      } else {
         int colorInt = (Integer)this.color.getValue();
         return new Color(colorInt >> 16 & 255, colorInt >> 8 & 255, colorInt & 255);
      }
   }

   protected Color getFriendColour() {
      return Colors.getInstance().getFriendColor();
   }

   public void onRenderModel(ModelBase modelBase, Entity entity) {
      if (entity instanceof EntityPlayer && modelBase instanceof ModelBiped) {
         this.rotationList.put((EntityPlayer)entity, this.getBipedRotations((ModelBiped)modelBase));
      }

   }

   public void onRender3D(Render3DEvent event) {
      boolean lightning = GL11.glIsEnabled(2896);
      boolean blend = GL11.glIsEnabled(3042);
      boolean texture = GL11.glIsEnabled(3553);
      boolean depth = GL11.glIsEnabled(2929);
      boolean lineSmooth = GL11.glIsEnabled(2848);
      if (lightning) {
         GL11.glDisable(2896);
      }

      if (!blend) {
         GL11.glEnable(3042);
      }

      GL11.glLineWidth(this.getLineWidth());
      if (texture) {
         GL11.glDisable(3553);
      }

      if (depth) {
         GL11.glDisable(2929);
      }

      if (!lineSmooth) {
         GL11.glEnable(2848);
      }

      GlStateManager.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA);
      GL11.glHint(3154, 4354);
      GlStateManager.depthMask(false);
      Entity renderEntity = RenderMethods.getEntity();
      Frustum frustum = Interpolation.createFrustum(renderEntity);
      List<EntityPlayer> playerList = mc.world.playerEntities;
      Object o = mc.player;
      this.rotationList.keySet().removeIf((playerx) -> playerx == null || playerx.equals(o) || playerx.isPlayerSleeping() || playerx.isSpectator() || EntityUtil.isDead(playerx));

      for(EntityPlayer player : playerList) {
         if (player != null) {
            AxisAlignedBB bb = player.getEntityBoundingBox();
            if (frustum.isBoundingBoxInFrustum(bb) && this.rotationList.get(player) != null) {
               this.renderSkeleton(player, (float[][])this.rotationList.get(player));
            }
         }
      }

      GlStateManager.depthMask(true);
      if (!lineSmooth) {
         GL11.glDisable(2848);
      }

      if (depth) {
         GL11.glEnable(2929);
      }

      if (texture) {
         GL11.glEnable(3553);
      }

      if (!blend) {
         GL11.glDisable(3042);
      }

      if (lightning) {
         GL11.glEnable(2896);
      }

   }

   @SubscribeEvent
   public void onDeath(DeathEvent event) {
      if (event.getEntity() instanceof EntityPlayer) {
         EntityPlayer player = event.getEntity();
         this.rotationList.keySet().remove(player);
      }

   }

   protected void renderSkeleton(EntityPlayer player, float[][] rotations) {
      GlStateManager.pushMatrix();
      if (Vonware.friendManager.isFriend(player)) {
         Color frdColor = this.getFriendColour();
         GlStateManager.color((float)frdColor.getRed() / 255.0F, (float)frdColor.getGreen() / 255.0F, (float)frdColor.getBlue() / 255.0F, (float)this.getColor().getAlpha() / 255.0F);
      } else {
         Color color = this.getColor();
         GlStateManager.color((float)color.getRed() / 255.0F, (float)color.getGreen() / 255.0F, (float)color.getBlue() / 255.0F, (float)color.getAlpha() / 255.0F);
      }

      Vec3d interpolateEntity = Interpolation.interpolateEntity(player);
      GlStateManager.translate(interpolateEntity.x, interpolateEntity.y, interpolateEntity.z);
      GlStateManager.rotate(-player.renderYawOffset, 0.0F, 1.0F, 0.0F);
      GlStateManager.translate((double)0.0F, (double)0.0F, player.isSneaking() ? -0.235 : (double)0.0F);
      float sneak = player.isSneaking() ? 0.6F : 0.75F;
      if (player.isElytraFlying()) {
         float f = (float)player.getTicksElytraFlying() + mc.getRenderPartialTicks();
         float f2 = MathHelper.clamp(f * f / 100.0F, 0.0F, 1.0F);
         GlStateManager.rotate(f2 * (90.0F - player.rotationPitch), 1.0F, 0.0F, 0.0F);
         Vec3d vec3d = player.getLook(mc.getRenderPartialTicks());
         double d0 = player.motionX * player.motionX + player.motionZ * player.motionZ;
         double d2 = vec3d.x * vec3d.x + vec3d.z * vec3d.z;
         if (d0 > (double)0.0F && d2 > (double)0.0F) {
            double d3 = (player.motionX * vec3d.x + player.motionZ * vec3d.z) / (Math.sqrt(d0) * Math.sqrt(d2));
            double d4 = player.motionX * vec3d.z - player.motionZ * vec3d.x;
            GlStateManager.rotate((float)(Math.signum(d4) * Math.acos(d3)) * 180.0F / (float)Math.PI, 0.0F, 1.0F, 0.0F);
         }
      }

      GlStateManager.pushMatrix();
      GlStateManager.translate((double)-0.125F, (double)sneak, (double)0.0F);
      if (rotations[3][0] != 0.0F) {
         GlStateManager.rotate(rotations[3][0] * (180F / (float)Math.PI), 1.0F, 0.0F, 0.0F);
      }

      if (rotations[3][1] != 0.0F) {
         GlStateManager.rotate(rotations[3][1] * (180F / (float)Math.PI), 0.0F, 1.0F, 0.0F);
      }

      if (rotations[3][2] != 0.0F) {
         GlStateManager.rotate(rotations[3][2] * (180F / (float)Math.PI), 0.0F, 0.0F, 1.0F);
      }

      GlStateManager.glBegin(3);
      GL11.glVertex2d((double)0.0F, (double)0.0F);
      GL11.glVertex2d((double)0.0F, (double)(-sneak));
      GlStateManager.glEnd();
      GlStateManager.popMatrix();
      GlStateManager.pushMatrix();
      GlStateManager.translate((double)0.125F, (double)sneak, (double)0.0F);
      if (rotations[4][0] != 0.0F) {
         GlStateManager.rotate(rotations[4][0] * (180F / (float)Math.PI), 1.0F, 0.0F, 0.0F);
      }

      if (rotations[4][1] != 0.0F) {
         GlStateManager.rotate(rotations[4][1] * (180F / (float)Math.PI), 0.0F, 1.0F, 0.0F);
      }

      if (rotations[4][2] != 0.0F) {
         GlStateManager.rotate(rotations[4][2] * (180F / (float)Math.PI), 0.0F, 0.0F, 1.0F);
      }

      GlStateManager.glBegin(3);
      GL11.glVertex2d((double)0.0F, (double)0.0F);
      GL11.glVertex2d((double)0.0F, (double)(-sneak));
      GlStateManager.glEnd();
      GlStateManager.popMatrix();
      GlStateManager.translate((double)0.0F, (double)0.0F, player.isSneaking() ? (double)0.25F : (double)0.0F);
      GlStateManager.pushMatrix();
      double sneakOffset = (double)0.0F;
      if (player.isSneaking()) {
         sneakOffset = -0.05;
      }

      GlStateManager.translate((double)0.0F, sneakOffset, player.isSneaking() ? -0.01725 : (double)0.0F);
      GlStateManager.pushMatrix();
      GlStateManager.translate((double)-0.375F, (double)sneak + 0.55, (double)0.0F);
      if (rotations[1][0] != 0.0F) {
         GlStateManager.rotate(rotations[1][0] * (180F / (float)Math.PI), 1.0F, 0.0F, 0.0F);
      }

      if (rotations[1][1] != 0.0F) {
         GlStateManager.rotate(rotations[1][1] * (180F / (float)Math.PI), 0.0F, 1.0F, 0.0F);
      }

      if (rotations[1][2] != 0.0F) {
         GlStateManager.rotate(-rotations[1][2] * (180F / (float)Math.PI), 0.0F, 0.0F, 1.0F);
      }

      GlStateManager.glBegin(3);
      GL11.glVertex2d((double)0.0F, (double)0.0F);
      GL11.glVertex2d((double)0.0F, (double)-0.5F);
      GlStateManager.glEnd();
      GlStateManager.popMatrix();
      GlStateManager.pushMatrix();
      GlStateManager.translate((double)0.375F, (double)sneak + 0.55, (double)0.0F);
      if (rotations[2][0] != 0.0F) {
         GlStateManager.rotate(rotations[2][0] * (180F / (float)Math.PI), 1.0F, 0.0F, 0.0F);
      }

      if (rotations[2][1] != 0.0F) {
         GlStateManager.rotate(rotations[2][1] * (180F / (float)Math.PI), 0.0F, 1.0F, 0.0F);
      }

      if (rotations[2][2] != 0.0F) {
         GlStateManager.rotate(-rotations[2][2] * (180F / (float)Math.PI), 0.0F, 0.0F, 1.0F);
      }

      GlStateManager.glBegin(3);
      GL11.glVertex2d((double)0.0F, (double)0.0F);
      GL11.glVertex2d((double)0.0F, (double)-0.5F);
      GlStateManager.glEnd();
      GlStateManager.popMatrix();
      GlStateManager.pushMatrix();
      GlStateManager.translate((double)0.0F, (double)sneak + 0.55, (double)0.0F);
      if (rotations[0][0] != 0.0F) {
         GlStateManager.rotate(rotations[0][0] * (180F / (float)Math.PI), 1.0F, 0.0F, 0.0F);
      }

      GlStateManager.glBegin(3);
      GL11.glVertex2d((double)0.0F, (double)0.0F);
      GL11.glVertex2d((double)0.0F, 0.3);
      GlStateManager.glEnd();
      GlStateManager.popMatrix();
      GlStateManager.popMatrix();
      GlStateManager.rotate(player.isSneaking() ? 25.0F : 0.0F, 1.0F, 0.0F, 0.0F);
      if (player.isSneaking()) {
         sneakOffset = -0.16175;
      }

      GlStateManager.translate((double)0.0F, sneakOffset, player.isSneaking() ? -0.48025 : (double)0.0F);
      GlStateManager.pushMatrix();
      GlStateManager.translate((double)0.0F, (double)sneak, (double)0.0F);
      GlStateManager.glBegin(3);
      GL11.glVertex2d((double)-0.125F, (double)0.0F);
      GL11.glVertex2d((double)0.125F, (double)0.0F);
      GlStateManager.glEnd();
      GlStateManager.popMatrix();
      GlStateManager.pushMatrix();
      GlStateManager.translate((double)0.0F, (double)sneak, (double)0.0F);
      GlStateManager.glBegin(3);
      GL11.glVertex2d((double)0.0F, (double)0.0F);
      GL11.glVertex2d((double)0.0F, 0.55);
      GlStateManager.glEnd();
      GlStateManager.popMatrix();
      GlStateManager.pushMatrix();
      GlStateManager.translate((double)0.0F, (double)sneak + 0.55, (double)0.0F);
      GlStateManager.glBegin(3);
      GL11.glVertex2d((double)-0.375F, (double)0.0F);
      GL11.glVertex2d((double)0.375F, (double)0.0F);
      GlStateManager.glEnd();
      GlStateManager.popMatrix();
      GlStateManager.popMatrix();
   }

   private float[][] getBipedRotations(ModelBiped biped) {
      float[][] rotations = new float[5][];
      float[] headRotation = new float[]{biped.bipedHead.rotateAngleX, biped.bipedHead.rotateAngleY, biped.bipedHead.rotateAngleZ};
      rotations[0] = headRotation;
      float[] rightArmRotation = new float[]{biped.bipedRightArm.rotateAngleX, biped.bipedRightArm.rotateAngleY, biped.bipedRightArm.rotateAngleZ};
      rotations[1] = rightArmRotation;
      float[] leftArmRotation = new float[]{biped.bipedLeftArm.rotateAngleX, biped.bipedLeftArm.rotateAngleY, biped.bipedLeftArm.rotateAngleZ};
      rotations[2] = leftArmRotation;
      float[] rightLegRotation = new float[]{biped.bipedRightLeg.rotateAngleX, biped.bipedRightLeg.rotateAngleY, biped.bipedRightLeg.rotateAngleZ};
      rotations[3] = rightLegRotation;
      float[] leftLegRotation = new float[]{biped.bipedLeftLeg.rotateAngleX, biped.bipedLeftLeg.rotateAngleY, biped.bipedLeftLeg.rotateAngleZ};
      rotations[4] = leftLegRotation;
      return rotations;
   }

   protected float getLineWidth() {
      return (Float)this.lineWidth.getValue();
   }

   public static enum ColorType {
      Global,
      Custom;
   }
}
