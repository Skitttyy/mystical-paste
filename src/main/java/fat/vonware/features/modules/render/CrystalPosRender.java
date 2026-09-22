package fat.vonware.features.modules.render;

import fat.vonware.Global;
import fat.vonware.Vonware;
import fat.vonware.event.events.PacketEvent;
import fat.vonware.features.modules.Module;
import fat.vonware.features.setting.Setting;
import fat.vonware.util.EntityUtil;
import fat.vonware.util.Animations.render.ColorUtil;
import fat.vonware.util.Animations.render.RenderMethods;
import fat.vonware.util.Phase.CrystalUtil;
import java.awt.Color;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityEnderCrystal;
import net.minecraft.init.Items;
import net.minecraft.network.play.server.SPacketDestroyEntities;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;

public class CrystalPosRender extends Module {
   public static CrystalPosRender INSTANCE;
   private final Setting<RenderMode> renderMode;
   private final Setting<Animation> animationType;
   private final Setting<Integer> fadeTime;
   private final Setting<Float> maxRange;
   private final Setting<Boolean> limitPos;
   private final Setting<Integer> positions;
   private final Setting<Integer> boxAlpha;
   private final Setting<Float> lineWidth;
   private final Setting<Boolean> renderText;
   private final Setting<Float> textScale;
   private final Setting<Boolean> renderIcons;
   private final Setting<Boolean> renderSelfDmg;
   private final Setting<Boolean> requireCrystal;
   private static final int MAX_HISTORY = 16;
   private final Map<Integer, EntityEnderCrystal> trackedCrystals;
   private final List<HitEntry> hitHistory;

   public CrystalPosRender() {
      super("CrystalPosRender", "Renders damage info at the position of the last crystal broken.", Module.Category.RENDER, true, false, false);
      this.renderMode = new Setting<RenderMode>("Render Mode", CrystalPosRender.RenderMode.Both);
      this.animationType = new Setting<Animation>("Animation", CrystalPosRender.Animation.None);
      this.fadeTime = new Setting<Integer>("Fade Time", 1000, 100, 5000);
      this.maxRange = new Setting<Float>("Max Range", 32.0F, 4.0F, 128.0F);
      this.limitPos = new Setting<Boolean>("Limit Pos", false);
      this.positions = new Setting<Integer>("Positions", 5, 1, 10, (v) -> (Boolean)this.limitPos.getValue());
      this.boxAlpha = new Setting<Integer>("Box Alpha", 50, 0, 255);
      this.lineWidth = new Setting<Float>("Line Width", 1.4F, 0.1F, 2.0F);
      this.renderText = new Setting<Boolean>("Render Text", true);
      this.textScale = new Setting<Float>("Text Scale", 1.0F, 0.1F, 2.0F);
      this.renderIcons = new Setting<Boolean>("Render Icons", true);
      this.renderSelfDmg = new Setting<Boolean>("Render Self", true);
      this.requireCrystal = new Setting<Boolean>("Require Crystal", false);
      this.trackedCrystals = new ConcurrentHashMap();
      this.hitHistory = new CopyOnWriteArrayList();
      INSTANCE = this;
      this.register(this.renderMode);
      this.register(this.animationType);
      this.register(this.fadeTime);
      this.register(this.limitPos);
      this.register(this.positions);
      this.register(this.maxRange);
      this.register(this.boxAlpha);
      this.register(this.lineWidth);
      this.register(this.renderText);
      this.register(this.textScale);
      this.register(this.renderIcons);
      this.register(this.renderSelfDmg);
      this.register(this.requireCrystal);
   }

   public void onEnable() {
      super.onEnable();
      this.trackedCrystals.clear();
      this.hitHistory.clear();
   }

   public void onDisable() {
      super.onDisable();
      this.trackedCrystals.clear();
      this.hitHistory.clear();
   }

   @SubscribeEvent
   public void onClientTick(TickEvent.ClientTickEvent event) {
      if (!fullNullCheck()) {
         if (event.phase == Phase.START) {
            for(Entity entity : mc.world.loadedEntityList) {
               if (entity instanceof EntityEnderCrystal) {
                  this.trackedCrystals.put(entity.getEntityId(), (EntityEnderCrystal)entity);
               }
            }

         }
      }
   }

   @SubscribeEvent
   public void onPacketReceive(PacketEvent.Receive event) {
      if (!fullNullCheck()) {
         if (event.getPacket() instanceof SPacketDestroyEntities) {
            SPacketDestroyEntities packet = (SPacketDestroyEntities)event.getPacket();

            for(int entityId : packet.getEntityIDs()) {
               EntityEnderCrystal crystal = (EntityEnderCrystal)this.trackedCrystals.remove(entityId);
               if (crystal != null) {
                  double cx = crystal.posX;
                  double cy = crystal.posY;
                  double cz = crystal.posZ;
                  float selfDmg = CrystalUtil.calculateDamage(cx, cy, cz, mc.player, (double)0.0F);
                  float targetDmg = (Float)EntityUtil.getTargets((double)16.0F).min(Comparator.comparingDouble((e) -> (double)mc.player.getDistance(e))).map((e) -> CrystalUtil.calculateDamage(cx, cy, cz, e, (double)0.0F)).orElse(0.0F);
                  if (this.animationType.getValue() == CrystalPosRender.Animation.None) {
                     this.hitHistory.clear();
                  } else {
                     int cap = (Boolean)this.limitPos.getValue() ? Math.min((Integer)this.positions.getValue(), 16) : 16;

                     while(this.hitHistory.size() >= cap) {
                        this.hitHistory.remove(0);
                     }
                  }

                  this.hitHistory.add(new HitEntry(cx, cy, cz, selfDmg, targetDmg));
               }
            }

         }
      }
   }

   @SubscribeEvent
   public void onRender(RenderWorldLastEvent event) {
      if (!fullNullCheck()) {
         if (this.renderMode.getValue() != CrystalPosRender.RenderMode.None || (Boolean)this.renderText.getValue()) {
            if (!this.hitHistory.isEmpty()) {
               if (!(Boolean)this.requireCrystal.getValue() || this.isHoldingCrystal()) {
                  long now = System.currentTimeMillis();
                  int fadeDur = (Integer)this.fadeTime.getValue();
                  this.hitHistory.removeIf((e) -> now - e.timestamp > (long)fadeDur);
                  if (!this.hitHistory.isEmpty()) {
                     double vpX = mc.getRenderManager().viewerPosX;
                     double vpY = mc.getRenderManager().viewerPosY;
                     double vpZ = mc.getRenderManager().viewerPosZ;
                     double rangeSq = (double)(Float)this.maxRange.getValue() * (double)(Float)this.maxRange.getValue();

                     for(HitEntry entry : this.hitHistory) {
                        double dx = entry.x - mc.player.posX;
                        double dy = entry.y - mc.player.posY;
                        double dz = entry.z - mc.player.posZ;
                        if (!(dx * dx + dy * dy + dz * dz > rangeSq)) {
                           this.renderEntry(entry, now, fadeDur, vpX, vpY, vpZ);
                        }
                     }

                  }
               }
            }
         }
      }
   }

   private boolean isHoldingCrystal() {
      boolean main = mc.player.getHeldItemMainhand().getItem() == Items.END_CRYSTAL;
      boolean off = mc.player.getHeldItemOffhand().getItem() == Items.END_CRYSTAL;
      return main || off;
   }

   private void renderEntry(HitEntry entry, long now, int fadeDur, double vpX, double vpY, double vpZ) {
      float normal = (float)Math.min((double)1.0F, (double)(now - entry.timestamp) / (double)fadeDur);
      float alpha = 1.0F - normal;
      Color base = Global.getGlobalColorRainbowS();
      Color fillColor = ColorUtil.newAlpha(base, (Integer)this.boxAlpha.getValue());
      Color lineColor = ColorUtil.newAlpha(base, 255);
      Color fadeFill = ColorUtil.newAlpha(fillColor, (int)(alpha * (float)fillColor.getAlpha()));
      Color fadeLine = ColorUtil.newAlpha(lineColor, (int)(alpha * (float)lineColor.getAlpha()));
      double bx = entry.x - (double)0.5F;
      double by = entry.y - (double)1.0F;
      double bz = entry.z - (double)0.5F;
      AxisAlignedBB bb = new AxisAlignedBB(bx - vpX, by - vpY, bz - vpZ, bx + (double)1.0F - vpX, by + (double)1.0F - vpY, bz + (double)1.0F - vpZ);
      RenderMethods.startRender();
      switch ((RenderMode)this.renderMode.getValue()) {
         case Box:
            RenderMethods.drawBox(bb, fadeFill);
            break;
         case Outline:
            RenderMethods.drawOutline(bb, (Float)this.lineWidth.getValue(), fadeLine);
            break;
         case Both:
            RenderMethods.drawBox(bb, fadeFill);
            RenderMethods.drawOutline(bb, (Float)this.lineWidth.getValue(), fadeLine);
      }

      RenderMethods.endRender();
      if ((Boolean)this.renderText.getValue()) {
         this.renderDamageText(entry, alpha, vpX, vpY, vpZ);
      }

   }

   private void renderDamageText(HitEntry entry, float alpha, double vpX, double vpY, double vpZ) {
      float wx = (float)entry.x;
      float wy = (float)(entry.y - 0.4);
      float wz = (float)entry.z;
      String targetText = entry.targetDmg > 0.0F ? String.format("%.1f", entry.targetDmg) : null;
      String selfText = (Boolean)this.renderSelfDmg.getValue() ? String.format("%.1f", entry.selfDmg) : null;
      GlStateManager.pushMatrix();
      RenderHelper.enableStandardItemLighting();
      GlStateManager.enablePolygonOffset();
      GlStateManager.doPolygonOffset(1.0F, -1500000.0F);
      GlStateManager.disableLighting();
      GlStateManager.disableDepth();
      GlStateManager.enableBlend();
      GlStateManager.color(1.0F, 1.0F, 1.0F, alpha);
      float baseScale = 0.021666666F;
      GlStateManager.translate((double)wx - vpX, (double)wy - vpY, (double)wz - vpZ);
      GlStateManager.glNormal3f(0.0F, 1.0F, 0.0F);
      GlStateManager.rotate(-mc.player.rotationYaw, 0.0F, 1.0F, 0.0F);
      GlStateManager.rotate(mc.player.rotationPitch, mc.gameSettings.thirdPersonView == 2 ? -1.0F : 1.0F, 0.0F, 0.0F);
      GlStateManager.scale(-0.021666666F, -0.021666666F, 0.021666666F);
      int distance = (int)mc.player.getDistance((double)wx, (double)wy, (double)wz);
      float scaleD = (float)distance / 2.0F / 3.0F;
      if (scaleD < 1.0F) {
         scaleD = 1.0F;
      }

      GlStateManager.scale(scaleD, scaleD, scaleD);
      float tScale = (Float)this.textScale.getValue();
      int iconSize = (int)(10.0F * tScale);
      int spacing = 2;
      int targetW = targetText != null ? (int)((float)Vonware.textManager.getStringWidth(targetText) * tScale) : 0;
      int selfW = selfText != null ? (int)((float)Vonware.textManager.getStringWidth(selfText) * tScale) : 0;
      int gapW = targetText != null && selfText != null ? (int)(4.0F * tScale) : 0;
      int totalWidth = ((Boolean)this.renderIcons.getValue() ? iconSize + 2 : 0) + targetW + gapW + selfW;
      GlStateManager.translate(-((double)totalWidth / (double)2.0F), (double)0.0F, (double)0.0F);
      int curX = 0;
      if ((Boolean)this.renderIcons.getValue()) {
         GlStateManager.enableTexture2D();
         GlStateManager.enableBlend();
         mc.getTextureManager().bindTexture(new ResourceLocation("textures/skull.png"));
         GlStateManager.color(1.0F, 1.0F, 1.0F, alpha);
         this.drawTexturedRect(curX, (int)(-2.0F * tScale), iconSize, iconSize);
         curX += iconSize + 2;
      }

      if (targetText != null) {
         GlStateManager.enableTexture2D();
         GlStateManager.pushMatrix();
         GlStateManager.translate((float)curX, 0.0F, 0.0F);
         GlStateManager.scale(tScale, tScale, 1.0F);
         Vonware.textManager.drawStringWithShadow(targetText, 0.0F, 0.0F, -1);
         GlStateManager.popMatrix();
         curX += targetW + gapW;
      }

      if (selfText != null) {
         GlStateManager.enableTexture2D();
         GlStateManager.pushMatrix();
         GlStateManager.translate((float)curX, 0.0F, 0.0F);
         GlStateManager.scale(tScale, tScale, 1.0F);
         Vonware.textManager.drawStringWithShadow(selfText, 0.0F, 0.0F, -1);
         GlStateManager.popMatrix();
      }

      GlStateManager.enableDepth();
      GlStateManager.disableBlend();
      GlStateManager.disablePolygonOffset();
      GlStateManager.doPolygonOffset(1.0F, 1500000.0F);
      GlStateManager.popMatrix();
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
   }

   private void drawTexturedRect(int x, int y, int width, int height) {
      Tessellator tess = Tessellator.getInstance();
      BufferBuilder buffer = tess.getBuffer();
      buffer.begin(7, DefaultVertexFormats.POSITION_TEX);
      buffer.pos((double)x, (double)(y + height), (double)0.0F).tex((double)0.0F, (double)1.0F).endVertex();
      buffer.pos((double)(x + width), (double)(y + height), (double)0.0F).tex((double)1.0F, (double)1.0F).endVertex();
      buffer.pos((double)(x + width), (double)y, (double)0.0F).tex((double)1.0F, (double)0.0F).endVertex();
      buffer.pos((double)x, (double)y, (double)0.0F).tex((double)0.0F, (double)0.0F).endVertex();
      tess.draw();
   }

   public static enum RenderMode {
      None,
      Outline,
      Box,
      Both;
   }

   public static enum Animation {
      None,
      Fade;
   }

   private final class HitEntry {
      final double x;
      final double y;
      final double z;
      final float selfDmg;
      final float targetDmg;
      final long timestamp;

      HitEntry(double x, double y, double z, float selfDmg, float targetDmg) {
         this.x = x;
         this.y = y;
         this.z = z;
         this.selfDmg = selfDmg;
         this.targetDmg = targetDmg;
         this.timestamp = System.currentTimeMillis();
      }
   }
}
