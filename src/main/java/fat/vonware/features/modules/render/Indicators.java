package fat.vonware.features.modules.render;

import fat.vonware.Vonware;
import fat.vonware.features.modules.Module;
import fat.vonware.features.setting.Setting;
import fat.vonware.util.MathUtil;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class Indicators extends Module {
   private final Setting<Float> range = new Setting<Float>("Range", 100.0F, 10.0F, 500.0F);
   private final Setting<Boolean> showHead = new Setting<Boolean>("Show Head", true);
   private final Setting<Boolean> showName = new Setting<Boolean>("Show Name", true);
   private final Setting<Boolean> showDistance = new Setting<Boolean>("Show Distance", false);
   private final Setting<Float> indicatorSize = new Setting<Float>("Size", 22.0F, 12.0F, 48.0F);
   private final Setting<Integer> masterAlpha = new Setting<Integer>("Alpha", 220, 0, 255);
   private final Setting<Boolean> arrowTilt = new Setting<Boolean>("Arrow Tilt", true);
   private final Setting<Integer> maxIndicators = new Setting<Integer>("Max Indicators", 8, 1, 20);
   private final Setting<Float> edgePadding = new Setting<Float>("Edge Padding", 6.0F, 0.0F, 24.0F);
   private static final ResourceLocation ARROW_ICON = new ResourceLocation("vonware", "textures/gui/enumarrow.png");
   private final Map<String, Float> playerAlpha = new HashMap();
   private final Map<String, Boolean> inFovCommitted = new HashMap();
   private final Map<String, Integer> inFovFrames = new HashMap();
   private final Frustum frustum = new Frustum();
   private long lastUpdateTime = System.currentTimeMillis();
   private static final int DEBOUNCE_ENTER = 4;
   private static final int DEBOUNCE_LEAVE = 20;
   private final Map<String, Float> playerScreenY = new HashMap();

   public Indicators() {
      super("Indicators", "Shows off-screen player directions", Module.Category.RENDER, true, false, false);
      this.register(this.range);
      this.register(this.showHead);
      this.register(this.showName);
      this.register(this.showDistance);
      this.register(this.indicatorSize);
      this.register(this.masterAlpha);
      this.register(this.arrowTilt);
      this.register(this.maxIndicators);
      this.register(this.edgePadding);
   }

   public void onDisable() {
      this.playerAlpha.clear();
      this.playerScreenY.clear();
      this.inFovCommitted.clear();
      this.inFovFrames.clear();
   }

   @SubscribeEvent
   public void onRenderOverlay(RenderGameOverlayEvent.Text event) {
      if (mc.player != null && mc.world != null) {
         long now = System.currentTimeMillis();
         float deltaTime = Math.min((float)(now - this.lastUpdateTime) / 16.67F, 3.0F);
         this.lastUpdateTime = now;
         ScaledResolution sr = new ScaledResolution(mc);
         int screenW = sr.getScaledWidth();
         int screenH = sr.getScaledHeight();
         float size = (Float)this.indicatorSize.getValue();
         float arrowSz = size * 0.65F;
         Entity viewer = mc.getRenderViewEntity();
         if (viewer != null) {
            this.frustum.setPosition(viewer.posX, viewer.posY, viewer.posZ);
            List<EntityPlayer> nearby = (List)mc.world.playerEntities.stream().filter((p) -> !p.equals(mc.player) && !Vonware.friendManager.isFriend(p) && !p.isDead && p.getDistance(mc.player) <= (Float)this.range.getValue()).sorted(Comparator.comparingDouble((p) -> (double)p.getDistance(mc.player))).limit((long)(Integer)this.maxIndicators.getValue()).collect(Collectors.toList());
            Set<String> activeKeys = (Set)nearby.stream().map((p) -> p.getUniqueID().toString()).collect(Collectors.toSet());
            this.playerAlpha.keySet().removeIf((k) -> !activeKeys.contains(k));
            this.playerScreenY.keySet().removeIf((k) -> !activeKeys.contains(k));
            this.inFovCommitted.keySet().removeIf((k) -> !activeKeys.contains(k));
            this.inFovFrames.keySet().removeIf((k) -> !activeKeys.contains(k));
            List<IndicatorData> leftList = new ArrayList();
            List<IndicatorData> rightList = new ArrayList();

            for(EntityPlayer target : nearby) {
               float yawDiff = this.getHorizontalAngle(target);
               float pitchDiff = this.getVerticalAngle(target);
               String key = target.getUniqueID().toString();
               boolean rawInFov = this.frustum.isBoundingBoxInFrustum(target.getEntityBoundingBox());
               boolean committed = (Boolean)this.inFovCommitted.getOrDefault(key, false);
               if (rawInFov == committed) {
                  this.inFovFrames.put(key, 0);
               } else {
                  int streak = (Integer)this.inFovFrames.getOrDefault(key, 0) + 1;
                  int threshold = committed ? 20 : 4;
                  this.inFovFrames.put(key, streak);
                  if (streak >= threshold) {
                     committed = rawInFov;
                     this.inFovCommitted.put(key, rawInFov);
                     this.inFovFrames.put(key, 0);
                  }
               }

               float targetAlpha = committed ? 0.0F : 1.0F;
               float current = (Float)this.playerAlpha.getOrDefault(key, targetAlpha);
               float speed = 0.18F * deltaTime;
               current += (targetAlpha - current) * speed;
               if (committed && current < 0.01F) {
                  current = 0.0F;
               }

               current = Math.max(0.0F, Math.min(1.0F, current));
               this.playerAlpha.put(key, current);
               if (!(current < 0.01F)) {
                  float vertFrac = 0.5F + pitchDiff / 90.0F * 0.4F;
                  float targetY = (float)screenH * vertFrac;
                  float vPad = size + (Float)this.edgePadding.getValue() + 4.0F;
                  targetY = Math.max(vPad, Math.min((float)screenH - vPad, targetY));
                  float currentY = (Float)this.playerScreenY.getOrDefault(key, targetY);
                  currentY += (targetY - currentY) * 0.18F * deltaTime * 1.5F;
                  this.playerScreenY.put(key, currentY);
                  IndicatorData data = new IndicatorData(target, current, currentY, yawDiff, pitchDiff);
                  if (yawDiff >= 0.0F) {
                     rightList.add(data);
                  } else {
                     leftList.add(data);
                  }
               }
            }

            if (!leftList.isEmpty() || !rightList.isEmpty()) {
               leftList.sort(Comparator.comparingDouble((dx) -> (double)dx.screenY));
               rightList.sort(Comparator.comparingDouble((dx) -> (double)dx.screenY));
               this.resolveOverlaps(leftList, size + 3.0F);
               this.resolveOverlaps(rightList, size + 3.0F);
               GlStateManager.pushMatrix();
               GlStateManager.enableBlend();
               GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
               GlStateManager.disableAlpha();
               GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

               for(IndicatorData d : leftList) {
                  this.renderIndicator(d, true, screenW, size, arrowSz);
               }

               for(IndicatorData d : rightList) {
                  this.renderIndicator(d, false, screenW, size, arrowSz);
               }

               GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
               GlStateManager.enableAlpha();
               GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
               GlStateManager.disableBlend();
               GlStateManager.popMatrix();
            }
         }
      }
   }

   private void renderIndicator(IndicatorData d, boolean isLeft, int screenW, float headSz, float arrowSz) {
      int finalAlpha = MathUtil.clamp((int)(d.alpha * ((float)(Integer)this.masterAlpha.getValue() / 255.0F) * 255.0F), 0, 255);
      if (finalAlpha > 0) {
         float pad = (Float)this.edgePadding.getValue();
         float gap = 3.0F;
         float y = d.screenY;
         String label = this.buildLabel(d);
         boolean hasHead = (Boolean)this.showHead.getValue();
         boolean hasLabel = !label.isEmpty();
         int textW = hasLabel ? mc.fontRenderer.getStringWidth(label) : 0;
         float arrowX;
         float headX;
         float textX;
         if (isLeft) {
            arrowX = pad;
            headX = pad + arrowSz + (hasHead ? gap : 0.0F);
            textX = headX + (hasHead ? headSz + gap : 0.0F);
         } else {
            arrowX = (float)screenW - pad - arrowSz;
            headX = arrowX - (hasHead ? headSz + gap : 0.0F);
            float anchorLeft = hasHead ? headX : arrowX;
            textX = anchorLeft - (hasLabel ? (float)textW + gap : 0.0F);
         }

         float baseAngle = isLeft ? 180.0F : 0.0F;
         if ((Boolean)this.arrowTilt.getValue()) {
            float tilt = Math.max(-40.0F, Math.min(40.0F, d.pitchDiff * 0.5F));
            baseAngle += isLeft ? -tilt : tilt;
         }

         this.drawArrow(arrowX, y - arrowSz / 2.0F, arrowSz, arrowSz, baseAngle, finalAlpha);
         if (hasHead) {
            this.drawHead(d.player, headX, y - headSz / 2.0F, headSz, finalAlpha);
         }

         if (hasLabel && finalAlpha > 10) {
            float textY = y - (float)mc.fontRenderer.FONT_HEIGHT / 2.0F;
            int textColor = finalAlpha << 24 | 16777215;
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            GlStateManager.disableAlpha();
            Vonware.textManager.drawString(label, textX, textY, textColor, true);
            GlStateManager.enableAlpha();
         }

      }
   }

   private void drawArrow(float x, float y, float w, float h, float angleDeg, int alpha) {
      GlStateManager.pushMatrix();
      float cx = x + w / 2.0F;
      float cy = y + h / 2.0F;
      GlStateManager.translate(cx, cy, 0.0F);
      GlStateManager.rotate(angleDeg, 0.0F, 0.0F, 1.0F);
      GlStateManager.translate(-cx, -cy, 0.0F);
      GlStateManager.enableTexture2D();
      mc.getTextureManager().bindTexture(ARROW_ICON);
      GlStateManager.color(1.0F, 1.0F, 1.0F, (float)alpha / 255.0F);
      Tessellator tess = Tessellator.getInstance();
      BufferBuilder buf = tess.getBuffer();
      buf.begin(7, DefaultVertexFormats.POSITION_TEX);
      buf.pos((double)x, (double)(y + h), (double)0.0F).tex((double)0.0F, (double)1.0F).endVertex();
      buf.pos((double)(x + w), (double)(y + h), (double)0.0F).tex((double)1.0F, (double)1.0F).endVertex();
      buf.pos((double)(x + w), (double)y, (double)0.0F).tex((double)1.0F, (double)0.0F).endVertex();
      buf.pos((double)x, (double)y, (double)0.0F).tex((double)0.0F, (double)0.0F).endVertex();
      tess.draw();
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      GlStateManager.popMatrix();
   }

   private void drawHead(EntityPlayer player, float x, float y, float size, int alpha) {
      if (player instanceof AbstractClientPlayer) {
         AbstractClientPlayer cp = (AbstractClientPlayer)player;
         GlStateManager.pushMatrix();
         GlStateManager.enableBlend();
         GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
         GlStateManager.enableTexture2D();
         mc.getTextureManager().bindTexture(cp.getLocationSkin());
         GlStateManager.color(1.0F, 1.0F, 1.0F, (float)alpha / 255.0F);
         Tessellator tess = Tessellator.getInstance();
         BufferBuilder buf = tess.getBuffer();
         buf.begin(7, DefaultVertexFormats.POSITION_TEX);
         buf.pos((double)x, (double)(y + size), (double)0.0F).tex((double)0.125F, (double)0.25F).endVertex();
         buf.pos((double)(x + size), (double)(y + size), (double)0.0F).tex((double)0.25F, (double)0.25F).endVertex();
         buf.pos((double)(x + size), (double)y, (double)0.0F).tex((double)0.25F, (double)0.125F).endVertex();
         buf.pos((double)x, (double)y, (double)0.0F).tex((double)0.125F, (double)0.125F).endVertex();
         tess.draw();
         buf.begin(7, DefaultVertexFormats.POSITION_TEX);
         buf.pos((double)x, (double)(y + size), (double)0.0F).tex((double)0.625F, (double)0.25F).endVertex();
         buf.pos((double)(x + size), (double)(y + size), (double)0.0F).tex((double)0.75F, (double)0.25F).endVertex();
         buf.pos((double)(x + size), (double)y, (double)0.0F).tex((double)0.75F, (double)0.125F).endVertex();
         buf.pos((double)x, (double)y, (double)0.0F).tex((double)0.625F, (double)0.125F).endVertex();
         tess.draw();
         GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
         GlStateManager.popMatrix();
      }
   }

   private float getHorizontalAngle(EntityPlayer target) {
      double dx = target.posX - mc.player.posX;
      double dz = target.posZ - mc.player.posZ;
      float targetYaw = (float)Math.toDegrees(Math.atan2(-dx, dz));
      float playerYaw = mc.player.rotationYaw;

      float diff;
      for(diff = targetYaw - playerYaw; diff > 180.0F; diff -= 360.0F) {
      }

      while(diff < -180.0F) {
         diff += 360.0F;
      }

      return diff;
   }

   private float getVerticalAngle(EntityPlayer target) {
      double dx = target.posX - mc.player.posX;
      double dy = target.posY + (double)target.getEyeHeight() - (mc.player.posY + (double)mc.player.getEyeHeight());
      double dz = target.posZ - mc.player.posZ;
      double horizDst = Math.sqrt(dx * dx + dz * dz);
      float targetPitch = (float)(-Math.toDegrees(Math.atan2(dy, horizDst)));
      return targetPitch - mc.player.rotationPitch;
   }

   private void resolveOverlaps(List<IndicatorData> list, float minSpacing) {
      for(int i = 1; i < list.size(); ++i) {
         IndicatorData prev = (IndicatorData)list.get(i - 1);
         IndicatorData curr = (IndicatorData)list.get(i);
         if (curr.screenY - prev.screenY < minSpacing) {
            curr.screenY = prev.screenY + minSpacing;
         }
      }

   }

   private String buildLabel(IndicatorData d) {
      if (!(Boolean)this.showName.getValue() && !(Boolean)this.showDistance.getValue()) {
         return "";
      } else {
         StringBuilder sb = new StringBuilder();
         if ((Boolean)this.showName.getValue()) {
            sb.append(d.player.getName());
         }

         if ((Boolean)this.showDistance.getValue()) {
            if (sb.length() > 0) {
               sb.append(" ");
            }

            sb.append((int)d.player.getDistance(mc.player)).append("m");
         }

         return sb.toString();
      }
   }

   private static final class IndicatorData {
      final EntityPlayer player;
      final float alpha;
      float screenY;
      final float yawDiff;
      final float pitchDiff;

      IndicatorData(EntityPlayer player, float alpha, float screenY, float yawDiff, float pitchDiff) {
         this.player = player;
         this.alpha = alpha;
         this.screenY = screenY;
         this.yawDiff = yawDiff;
         this.pitchDiff = pitchDiff;
      }
   }
}
