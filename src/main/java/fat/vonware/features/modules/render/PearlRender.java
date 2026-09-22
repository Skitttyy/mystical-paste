package fat.vonware.features.modules.render;

import fat.vonware.Global;
import fat.vonware.event.events.Render3DEvent;
import fat.vonware.features.gui.components.items.buttons.render.ColorPickerConfig;
import fat.vonware.features.modules.Module;
import fat.vonware.features.setting.ColorSetting;
import fat.vonware.features.setting.Setting;
import java.awt.Color;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.item.EntityEnderPearl;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

public class PearlRender extends Module {
   private final ColorSetting effectColor = new ColorSetting("Color", 43775, false);
   private final Setting<Float> rippleSpeed = new Setting<Float>("RippleSpeed", 0.45F, 0.05F, 1.5F);
   private final Setting<Float> rippleRadius = new Setting<Float>("RippleRadius", 3.5F, 0.5F, 8.0F);
   private final Setting<Float> rippleWidth = new Setting<Float>("RippleWidth", 1.6F, 0.5F, 5.0F);
   private final Setting<Float> rippleGlow = new Setting<Float>("RippleGlow", 1.0F, 0.0F, 3.0F);
   private final Setting<Float> beamHeight = new Setting<Float>("BeamHeight", 20.0F, 1.0F, 20.0F);
   private final Setting<Float> beamRadius = new Setting<Float>("BeamRadius", 0.12F, 0.02F, 0.5F);
   private final Setting<Float> beamGlow = new Setting<Float>("BeamGlow", 1.0F, 0.0F, 3.0F);
   private final Setting<Float> duration = new Setting<Float>("Duration", 3.2F, 0.5F, 8.0F);
   private static final int BEAM_SHELLS = 15;
   private static final float BEAM_SIGMA = 0.32F;
   private static final float GLOW_NORM = 0.06F;
   private static final int SEGS = 72;
   private final Map<Integer, Vec3d> trackedPearls = new HashMap();
   private final CopyOnWriteArrayList<Effect> activeEffects = new CopyOnWriteArrayList();

   public PearlRender() {
      super("PearlRender", "Ripple & beam effect on pearl land", Module.Category.RENDER, true, false, false);
      this.register(this.effectColor);
      this.register(this.rippleSpeed);
      this.register(this.rippleRadius);
      this.register(this.rippleWidth);
      this.register(this.rippleGlow);
      this.register(this.beamHeight);
      this.register(this.beamRadius);
      this.register(this.beamGlow);
      this.register(this.duration);
   }

   public void onDisable() {
      this.trackedPearls.clear();
      this.activeEffects.clear();
   }

   public void onTick() {
      if (mc.world != null && mc.player != null) {
         Set<Integer> current = new HashSet();
         AxisAlignedBB box = mc.player.getEntityBoundingBox().grow((double)100.0F, (double)100.0F, (double)100.0F);

         for(EntityEnderPearl pearl : mc.world.getEntitiesWithinAABB(EntityEnderPearl.class, box)) {
            current.add(pearl.getEntityId());
            this.trackedPearls.put(pearl.getEntityId(), new Vec3d(pearl.posX, pearl.posY, pearl.posZ));
         }

         Iterator<Map.Entry<Integer, Vec3d>> it = this.trackedPearls.entrySet().iterator();

         while(it.hasNext()) {
            Map.Entry<Integer, Vec3d> entry = (Map.Entry)it.next();
            if (!current.contains(entry.getKey())) {
               this.activeEffects.add(new Effect((Vec3d)entry.getValue()));
               it.remove();
            }
         }

         this.activeEffects.removeIf(Effect::isDone);
      }
   }

   @SubscribeEvent
   public void onRender3D(Render3DEvent event) {
      if (!this.activeEffects.isEmpty()) {
         GL11.glPushAttrib(1048575);
         GlStateManager.pushMatrix();
         GL11.glDisable(3553);
         GL11.glDisable(2929);
         GL11.glDepthMask(false);
         GL11.glDisable(2884);
         GL11.glDisable(2896);
         GL11.glDisable(3008);
         GL11.glEnable(3042);
         GL11.glBlendFunc(770, 771);
         GL11.glShadeModel(7425);
         GL11.glEnable(2848);
         GL11.glHint(3154, 4354);
         GL11.glEnable(2881);
         GL11.glHint(3155, 4354);
         Color base = this.getEffectColor();
         float r = (float)base.getRed() / 255.0F;
         float g = (float)base.getGreen() / 255.0F;
         float b = (float)base.getBlue() / 255.0F;

         for(Effect fx : this.activeEffects) {
            double rx = fx.pos.x - mc.getRenderManager().viewerPosX;
            double ry = fx.pos.y - mc.getRenderManager().viewerPosY;
            double rz = fx.pos.z - mc.getRenderManager().viewerPosZ;
            this.renderRipple(rx, ry, rz, fx.getProgress(), r, g, b);
            this.renderBeam(rx, ry, rz, fx.getProgress(), r, g, b);
         }

         GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
         GlStateManager.popMatrix();
         GL11.glPopAttrib();
         GlStateManager.resetColor();
      }
   }

   private void renderRipple(double x, double y, double z, float progress, float r, float g, float b) {
      float maxR = (Float)this.rippleRadius.getValue();
      float glowMult = (Float)this.rippleGlow.getValue();
      float lineW = (Float)this.rippleWidth.getValue();
      float phase = Math.min(1.0F, progress * (Float)this.rippleSpeed.getValue() * 1.6F);
      float radius = phase * maxR;
      float alpha = (float)Math.pow((double)1.0F - (double)phase, 1.6);
      float bloomAlpha = alpha * alpha * 0.8F * glowMult;
      if (!(alpha < 0.005F) && !(radius < 0.02F)) {
         float outerGlowW = maxR * 0.1F * glowMult;
         float innerGlowW = outerGlowW * 0.35F;
         float bloomOutlineOffset = maxR * 0.04F * glowMult;
         float bloomOutlineAlpha = bloomAlpha * 0.5F;
         Tessellator tess = Tessellator.getInstance();
         BufferBuilder buf = tess.getBuffer();
         GL11.glBlendFunc(770, 1);
         if (outerGlowW > 0.005F && bloomAlpha > 0.003F) {
            buf.begin(5, DefaultVertexFormats.POSITION_COLOR);

            for(int i = 0; i <= 72; ++i) {
               double a = (Math.PI * 2D) * (double)i / (double)72.0F;
               double cos = Math.cos(a);
               double sin = Math.sin(a);
               buf.pos(x + cos * (double)radius, y, z + sin * (double)radius).color(r, g, b, bloomAlpha).endVertex();
               buf.pos(x + cos * (double)(radius + outerGlowW), y, z + sin * (double)(radius + outerGlowW)).color(r, g, b, 0.0F).endVertex();
            }

            tess.draw();
            if (innerGlowW > 0.005F && radius > innerGlowW) {
               buf.begin(5, DefaultVertexFormats.POSITION_COLOR);

               for(int i = 0; i <= 72; ++i) {
                  double a = (Math.PI * 2D) * (double)i / (double)72.0F;
                  double cos = Math.cos(a);
                  double sin = Math.sin(a);
                  buf.pos(x + cos * (double)(radius - innerGlowW), y, z + sin * (double)(radius - innerGlowW)).color(r, g, b, 0.0F).endVertex();
                  buf.pos(x + cos * (double)radius, y, z + sin * (double)radius).color(r, g, b, bloomAlpha).endVertex();
               }

               tess.draw();
            }
         }

         if (bloomOutlineAlpha > 0.003F && bloomOutlineOffset > 0.002F) {
            GL11.glLineWidth(Math.max(0.5F, lineW * 0.5F));
            buf.begin(2, DefaultVertexFormats.POSITION_COLOR);
            float outlineR = radius + bloomOutlineOffset;

            for(int i = 0; i < 72; ++i) {
               double a = (Math.PI * 2D) * (double)i / (double)72.0F;
               buf.pos(x + Math.cos(a) * (double)outlineR, y, z + Math.sin(a) * (double)outlineR).color(r, g, b, bloomOutlineAlpha).endVertex();
            }

            tess.draw();
         }

         GL11.glBlendFunc(770, 771);
         GL11.glLineWidth(lineW);
         buf.begin(2, DefaultVertexFormats.POSITION_COLOR);

         for(int i = 0; i < 72; ++i) {
            double a = (Math.PI * 2D) * (double)i / (double)72.0F;
            buf.pos(x + Math.cos(a) * (double)radius, y, z + Math.sin(a) * (double)radius).color(r, g, b, alpha).endVertex();
         }

         tess.draw();
         GL11.glLineWidth(1.0F);
      }
   }

   private void renderBeam(double x, double y, double z, float progress, float r, float g, float b) {
      float maxH = (Float)this.beamHeight.getValue();
      float coreR = (Float)this.beamRadius.getValue();
      float glowMult = (Float)this.beamGlow.getValue();
      float topH;
      float botH;
      float topA;
      float botA;
      if (progress < 0.3F) {
         float t = progress / 0.3F;
         float e = t * t * (3.0F - 2.0F * t);
         topH = e * maxH;
         botH = 0.0F;
         topA = e * 0.85F;
         botA = e;
      } else if (progress < 0.5F) {
         float t = (progress - 0.3F) / 0.2F;
         topH = maxH;
         botH = 0.0F;
         topA = 0.85F + t * 0.15F;
         botA = 1.0F;
      } else {
         float t = (progress - 0.5F) / 0.5F;
         float e = t * t;
         topH = maxH;
         botH = e * maxH;
         topA = Math.max(0.0F, 1.0F - t);
         botA = Math.max(0.0F, 1.0F - t * 2.0F);
      }

      if (!(topH - botH < 0.01F)) {
         if (!(topA < 0.005F) || !(botA < 0.005F)) {
            double topY = y + (double)topH;
            double botY = y + (double)botH;
            float glowWidth = coreR * 3.0F * Math.max(0.01F, glowMult);
            GL11.glBlendFunc(770, 1);

            for(int i = 0; i < 15; ++i) {
               float t = (float)i / 14.0F;
               float shellR = coreR + t * glowWidth;
               float weight = (float)Math.exp((double)(-(t * t) / 0.2048F));
               float sBotA = botA * weight * glowMult * 0.06F;
               float sTopA = topA * weight * glowMult * 0.06F;
               if (!(sBotA < 0.003F) || !(sTopA < 0.003F)) {
                  this.drawCylinder(x, botY, topY, z, shellR, r, g, b, sBotA, sTopA);
               }
            }

            float additiveCoreA = Math.min(0.85F, (botA + topA) * 0.5F * 0.8F);
            this.drawCylinder(x, botY, topY, z, coreR, Math.min(1.0F, r * 1.2F), Math.min(1.0F, g * 1.2F), Math.min(1.0F, b * 1.2F), additiveCoreA * botA, additiveCoreA * topA);
            GL11.glBlendFunc(770, 771);
            this.drawCylinder(x, botY, topY, z, coreR, Math.min(1.0F, r * 1.25F), Math.min(1.0F, g * 1.25F), Math.min(1.0F, b * 1.25F), botA, topA);
            GL11.glBlendFunc(770, 1);
            float capA = topA * Math.min(1.0F, glowMult * 0.65F);
            if (capA > 0.005F) {
               this.drawDiscCap(x, topY, z, coreR + glowWidth * 0.45F, r, g, b, capA);
            }

            GL11.glBlendFunc(770, 771);
         }
      }
   }

   private void drawCylinder(double cx, double botY, double topY, double cz, float radius, float r, float g, float b, float botAlpha, float topAlpha) {
      if (!(botAlpha < 0.003F) || !(topAlpha < 0.003F)) {
         Tessellator tess = Tessellator.getInstance();
         BufferBuilder buf = tess.getBuffer();
         buf.begin(8, DefaultVertexFormats.POSITION_COLOR);

         for(int i = 0; i <= 72; ++i) {
            double a = (Math.PI * 2D) * (double)i / (double)72.0F;
            double px = cx + Math.cos(a) * (double)radius;
            double pz = cz + Math.sin(a) * (double)radius;
            buf.pos(px, topY, pz).color(r, g, b, topAlpha).endVertex();
            buf.pos(px, botY, pz).color(r, g, b, botAlpha).endVertex();
         }

         tess.draw();
      }
   }

   private void drawDiscCap(double cx, double y, double cz, float radius, float r, float g, float b, float alpha) {
      if (!(alpha < 0.003F)) {
         Tessellator tess = Tessellator.getInstance();
         BufferBuilder buf = tess.getBuffer();
         buf.begin(6, DefaultVertexFormats.POSITION_COLOR);
         buf.pos(cx, y, cz).color(r, g, b, alpha).endVertex();

         for(int i = 0; i <= 72; ++i) {
            double a = (Math.PI * 2D) * (double)i / (double)72.0F;
            buf.pos(cx + Math.cos(a) * (double)radius, y, cz + Math.sin(a) * (double)radius).color(r, g, b, 0.0F).endVertex();
         }

         tess.draw();
      }
   }

   private Color getEffectColor() {
      if (ColorPickerConfig.getState(this.effectColor).useGlobalColor) {
         return Global.getGlobalColorRainbowS();
      } else {
         int c = (Integer)this.effectColor.getValue();
         return new Color(c >> 16 & 255, c >> 8 & 255, c & 255);
      }
   }

   private class Effect {
      final Vec3d pos;
      private final long birthMs;
      private final long durationMs;

      Effect(Vec3d pos) {
         this.pos = pos;
         this.birthMs = System.currentTimeMillis();
         this.durationMs = (long)((Float)PearlRender.this.duration.getValue() * 1000.0F);
      }

      float getProgress() {
         return Math.min(1.0F, (float)(System.currentTimeMillis() - this.birthMs) / (float)this.durationMs);
      }

      boolean isDone() {
         return System.currentTimeMillis() - this.birthMs > this.durationMs;
      }
   }
}
