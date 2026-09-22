package fat.vonware.features.modules.render;

import fat.vonware.Global;
import fat.vonware.event.events.Render3DEvent;
import fat.vonware.features.modules.Module;
import fat.vonware.features.setting.Setting;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class JumpCircles extends Module {
   private final Setting<Boolean> self = new Setting<Boolean>("Self", true);
   private final Setting<Boolean> players = new Setting<Boolean>("Players", false);
   private final Setting<Float> speed = new Setting<Float>("Speed", 1.0F, 0.1F, 1.0F);
   private final Setting<Float> maxRadius = new Setting<Float>("MaxRadius", 2.0F, 0.5F, 5.0F);
   private final Setting<Float> lineWidth = new Setting<Float>("LineWidth", 2.0F, 0.5F, 5.0F);
   private final Setting<Integer> alpha = new Setting<Integer>("Alpha", 255, 0, 255);
   private final List<Circle> circles = new ArrayList();
   private final Map<Entity, Boolean> wasOnGround = new HashMap();

   public JumpCircles() {
      super("JumpCircles", "Renders ripple circles when landing", Module.Category.RENDER, true, false, false);
      this.register(this.self);
      this.register(this.players);
      this.register(this.speed);
      this.register(this.maxRadius);
      this.register(this.lineWidth);
      this.register(this.alpha);
   }

   public void onUpdate() {
      if (!fullNullCheck()) {
         if ((Boolean)this.self.getValue()) {
            this.checkEntityLanding(mc.player);
         }

         if ((Boolean)this.players.getValue()) {
            for(Entity entity : mc.world.loadedEntityList) {
               if (entity instanceof EntityPlayer && entity != mc.player) {
                  this.checkEntityLanding(entity);
               }
            }
         }

      }
   }

   private void checkEntityLanding(Entity entity) {
      boolean wasGrounded = (Boolean)this.wasOnGround.getOrDefault(entity, true);
      boolean isGrounded = entity.onGround;
      if (!wasGrounded && isGrounded && entity.motionY < (double)0.0F) {
         this.circles.add(new Circle(new Vec3d(entity.posX, entity.posY, entity.posZ), 0.0F));
      }

      this.wasOnGround.put(entity, isGrounded);
   }

   @SubscribeEvent
   public void onRender3D(Render3DEvent event) {
      if (!fullNullCheck() && !this.circles.isEmpty()) {
         GlStateManager.pushMatrix();
         GlStateManager.disableTexture2D();
         GlStateManager.enableBlend();
         GlStateManager.disableAlpha();
         GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
         GlStateManager.shadeModel(7425);
         GlStateManager.disableDepth();
         GlStateManager.glLineWidth((Float)this.lineWidth.getValue());
         double viewerPosX = mc.getRenderManager().viewerPosX;
         double viewerPosY = mc.getRenderManager().viewerPosY;
         double viewerPosZ = mc.getRenderManager().viewerPosZ;
         Iterator<Circle> iterator = this.circles.iterator();

         while(iterator.hasNext()) {
            Circle circle = (Circle)iterator.next();
            circle.radius += (Float)this.speed.getValue() * 0.05F;
            if (circle.radius >= (Float)this.maxRadius.getValue()) {
               iterator.remove();
            } else {
               float alphaMultiplier = 1.0F - circle.radius / (Float)this.maxRadius.getValue();
               Color globalColor = Global.getGlobalColorRainbowS();
               int finalAlpha = (int)((float)(Integer)this.alpha.getValue() * alphaMultiplier);
               Color color = new Color(globalColor.getRed(), globalColor.getGreen(), globalColor.getBlue(), finalAlpha);
               this.renderCircle(circle.pos.x - viewerPosX, circle.pos.y - viewerPosY + 0.01, circle.pos.z - viewerPosZ, circle.radius, 100, color);
            }
         }

         GlStateManager.glLineWidth(1.0F);
         GlStateManager.shadeModel(7424);
         GlStateManager.disableBlend();
         GlStateManager.enableAlpha();
         GlStateManager.enableTexture2D();
         GlStateManager.enableDepth();
         GlStateManager.popMatrix();
      }
   }

   private void renderCircle(double x, double y, double z, float radius, int segments, Color color) {
      Tessellator tessellator = Tessellator.getInstance();
      BufferBuilder buffer = tessellator.getBuffer();
      buffer.begin(2, DefaultVertexFormats.POSITION_COLOR);

      for(int i = 0; i < segments; ++i) {
         double angle = (Math.PI * 2D) * (double)i / (double)segments;
         double xPos = x + (double)radius * Math.cos(angle);
         double zPos = z + (double)radius * Math.sin(angle);
         buffer.pos(xPos, y, zPos).color((float)color.getRed() / 255.0F, (float)color.getGreen() / 255.0F, (float)color.getBlue() / 255.0F, (float)color.getAlpha() / 255.0F).endVertex();
      }

      tessellator.draw();
   }

   public void onDisable() {
      this.circles.clear();
      this.wasOnGround.clear();
   }

   private static class Circle {
      Vec3d pos;
      float radius;

      Circle(Vec3d pos, float radius) {
         this.pos = pos;
         this.radius = radius;
      }
   }
}
