package fat.vonware.features.modules.elements;

import fat.vonware.Global;
import fat.vonware.Vonware;
import fat.vonware.event.events.EntityWorldEvent;
import fat.vonware.event.events.Render2DEvent;
import fat.vonware.features.gui.components.items.buttons.render.ColorPickerConfig;
import fat.vonware.features.gui.newgui.newcomponents.NewComponent;
import fat.vonware.features.modules.HudModule;
import fat.vonware.features.modules.Module;
import fat.vonware.features.modules.client.ClickGui;
import fat.vonware.features.modules.client.Colors;
import fat.vonware.features.setting.ColorSetting;
import fat.vonware.features.setting.Setting;
import fat.vonware.util.EntityUtil;
import fat.vonware.util.Animations.AnimationUtil;
import fat.vonware.util.Animations.render.ColorUtil;
import fat.vonware.util.HUD.HudElements.TRUtil;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

public class TextRadar extends HudModule {
   private final ColorSetting customColor = new ColorSetting("Color", 16777215, true);
   public Setting<SortMode> sortMode;
   public Setting<Boolean> health;
   public Setting<Boolean> distance;
   public Setting<Boolean> pops;
   public Setting<Boolean> heads;
   public Setting<Boolean> autoPosition;
   public Setting<Boolean> futurePosition;
   public Setting<Boolean> animate;
   private static final float HEAD_SIZE = 8.5F;
   private static final float HEAD_GAP = 2.0F;
   private static final long SLIDE_MS = 220L;
   private float currentWidth;
   private int currentPlayerCount;
   private boolean cachedLeftSide;
   private final Map<UUID, DeltaAnim> slideAnimMap;
   private final Map<UUID, Float> smoothYMap;
   private final Map<UUID, Long> yLastTimeMap;
   private final Map<UUID, LeavingEntry> leavingMap;
   private final Map<UUID, PlayerEntry> lastFrameEntry;

   public TextRadar() {
      super("TextRadar", "Displays players", Module.Category.Elements, true, false, false, 100.0F, 50.0F);
      this.sortMode = new Setting<SortMode>("Sort", TextRadar.SortMode.DISTANCE);
      this.health = new Setting<Boolean>("Health", false);
      this.distance = new Setting<Boolean>("Distance", true);
      this.pops = new Setting<Boolean>("TotemPops", false);
      this.heads = new Setting<Boolean>("Heads", true);
      this.autoPosition = new Setting<Boolean>("AutoPos", false);
      this.futurePosition = new Setting<Boolean>("FuturePos", false);
      this.animate = new Setting<Boolean>("Animate", true);
      this.currentWidth = 60.0F;
      this.currentPlayerCount = 0;
      this.cachedLeftSide = true;
      this.slideAnimMap = new HashMap();
      this.smoothYMap = new HashMap();
      this.yLastTimeMap = new HashMap();
      this.leavingMap = new HashMap();
      this.lastFrameEntry = new HashMap();
      this.register(this.customColor);
      this.register(this.sortMode);
      this.register(this.health);
      this.register(this.distance);
      this.register(this.pops);
      this.register(this.heads);
      this.register(this.autoPosition);
      this.register(this.futurePosition);
      this.register(this.animate);
   }

   public void updateBounds() {
      this.applyPosition();
      this.rect.setWidth(this.currentWidth);
      this.rect.setHeight((float)Math.max(10, this.currentPlayerCount * 10));
   }

   public boolean isAutoPositioned() {
      return (Boolean)this.autoPosition.getValue();
   }

   @SubscribeEvent
   public void onEntityRemoved(EntityWorldEvent.Remove event) {
      if (event.getEntity() instanceof EntityPlayer) {
         if (event.getEntity() != mc.player) {
            UUID id = event.getEntity().getUniqueID();
            if (this.slideAnimMap.containsKey(id)) {
               PlayerEntry pe = (PlayerEntry)this.lastFrameEntry.get(id);
               if ((Boolean)this.animate.getValue() && pe != null && !this.leavingMap.containsKey(id)) {
                  float lastY = (Float)this.smoothYMap.getOrDefault(id, 0.0F);
                  DeltaAnim slideAnim = (DeltaAnim)this.slideAnimMap.get(id);
                  this.leavingMap.put(id, new LeavingEntry(pe.label, pe.player, lastY, slideAnim));
               }

               this.slideAnimMap.remove(id);
               this.smoothYMap.remove(id);
               this.yLastTimeMap.remove(id);
            }
         }
      }
   }

   @SubscribeEvent
   public void onRender2D(Render2DEvent event) {
      this.applyPosition();
      this.refreshQuadrant();
      List<PlayerEntry> entries = new ArrayList();

      for(EntityPlayer player : mc.world.playerEntities) {
         if (player != null && player != mc.player && !player.isDead) {
            String playerName = player.getName();
            int hp = (int)EntityUtil.getHealth(player);
            String hpStr = (Boolean)this.health.getValue() ? TRUtil.getHealthColor(hp) + hp : "";
            float dist = mc.player.getDistance(player);
            String distStr = (Boolean)this.distance.getValue() ? " " + TextFormatting.GRAY + "(" + TRUtil.getDistanceColor(dist) + String.format("%.1f", dist) + "m" + TextFormatting.GRAY + ")" : "";
            String popsStr = "";
            if ((Boolean)this.pops.getValue()) {
               int popCount = Vonware.totemPopManager.getPopCount(playerName);
               if (popCount > 0) {
                  popsStr = " -" + popCount;
               }
            }

            String radarStr = TextFormatting.RESET + (Vonware.friendManager.isFriend(playerName) ? TextFormatting.AQUA.toString() : "") + playerName + distStr + " " + hpStr + TextFormatting.WHITE + popsStr;
            entries.add(new PlayerEntry(radarStr, (int)dist, player));
         }
      }

      switch ((SortMode)this.sortMode.getValue()) {
         case DISTANCE:
            entries.sort(Comparator.comparingInt((ex) -> ex.distance));
            break;
         case ALPHABETICAL:
            entries.sort(Comparator.comparing((ex) -> ex.player.getName()));
            break;
         case LENGTH:
            entries.sort(Comparator.comparingInt((ex) -> ex.label.length()));
      }

      this.currentPlayerCount = entries.size();
      float headOffset = (Boolean)this.heads.getValue() ? 10.5F : 0.0F;
      this.currentWidth = 60.0F;

      for(PlayerEntry e : entries) {
         float lw = headOffset + (float)Vonware.textManager.getStringWidth(e.label);
         if (lw > this.currentWidth) {
            this.currentWidth = lw;
         }
      }

      this.rect.setWidth(this.currentWidth);
      this.rect.setHeight((float)(this.currentPlayerCount * 10));
      Set<UUID> activeIds = new HashSet();

      for(PlayerEntry e : entries) {
         activeIds.add(e.player.getUniqueID());
      }

      this.slideAnimMap.keySet().removeIf((idx) -> !activeIds.contains(idx));
      this.smoothYMap.keySet().removeIf((idx) -> !activeIds.contains(idx));
      this.yLastTimeMap.keySet().removeIf((idx) -> !activeIds.contains(idx));
      long nowNs = System.nanoTime();
      boolean animOn = (Boolean)this.animate.getValue();

      for(int i = 0; i < entries.size(); ++i) {
         PlayerEntry e = (PlayerEntry)entries.get(i);
         UUID id = e.player.getUniqueID();
         float targetY = (float)i * 10.0F;
         this.slideAnimMap.computeIfAbsent(id, (k) -> new DeltaAnim(220L, 0.0F));
         if (animOn) {
            ((DeltaAnim)this.slideAnimMap.get(id)).tick(true);
         } else {
            ((DeltaAnim)this.slideAnimMap.get(id)).snap(true);
         }

         if (!this.smoothYMap.containsKey(id)) {
            this.smoothYMap.put(id, targetY);
            this.yLastTimeMap.put(id, nowNs);
         }

         if (animOn) {
            long lastNs = (Long)this.yLastTimeMap.getOrDefault(id, nowNs);
            float dtSec = (float)(nowNs - lastNs) / 1.0E9F;
            this.yLastTimeMap.put(id, nowNs);
            float curY = (Float)this.smoothYMap.getOrDefault(id, targetY);
            float step = 1.0F - (float)Math.pow((double)0.82F, (double)(dtSec * 60.0F));
            float newY = curY + (targetY - curY) * step;
            if (Math.abs(newY - targetY) < 0.3F) {
               newY = targetY;
            }

            this.smoothYMap.put(id, newY);
         } else {
            this.yLastTimeMap.put(id, nowNs);
            this.smoothYMap.put(id, targetY);
         }

         this.leavingMap.remove(id);
      }

      Iterator<Map.Entry<UUID, LeavingEntry>> leavingIter = this.leavingMap.entrySet().iterator();

      while(leavingIter.hasNext()) {
         LeavingEntry le = (LeavingEntry)((Map.Entry)leavingIter.next()).getValue();
         le.slideAnim.tick(false);
         if (le.slideAnim.getProgress() <= 0.0F) {
            leavingIter.remove();
         }
      }

      int color = this.getCustomColor().getRGB();
      float baseX = this.rect.getX();
      float baseY = this.rect.getY();
      boolean doHeads = (Boolean)this.heads.getValue();
      if (doHeads) {
         GL11.glPushAttrib(24577);
         GlStateManager.enableBlend();
         GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
      }

      for(PlayerEntry e : entries) {
         UUID id = e.player.getUniqueID();
         float slideP = AnimationUtil.easeOutCubic(((DeltaAnim)this.slideAnimMap.get(id)).getProgress());
         float drawX = this.computeDrawX(baseX, slideP);
         float rowY = baseY + (Float)this.smoothYMap.getOrDefault(id, 0.0F);
         if (doHeads) {
            this.drawHead(e.player, drawX, rowY, 8.5F);
         }

         Vonware.textManager.drawStringWithShadow(e.label, drawX + headOffset, rowY, color);
      }

      for(LeavingEntry le : this.leavingMap.values()) {
         float exitT = AnimationUtil.easeInCubic(1.0F - le.slideAnim.getProgress());
         float drawX = this.computeDrawX(baseX, 1.0F - exitT);
         float rowY = baseY + le.lastY;
         if (doHeads && le.player != null) {
            this.drawHead(le.player, drawX, rowY, 8.5F);
         }

         Vonware.textManager.drawStringWithShadow(le.label, drawX + headOffset, rowY, color);
      }

      if (doHeads) {
         GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
         GlStateManager.disableBlend();
         GL11.glPopAttrib();
      }

      this.lastFrameEntry.clear();

      for(PlayerEntry e : entries) {
         this.lastFrameEntry.put(e.player.getUniqueID(), e);
      }

   }

   private float computeDrawX(float baseX, float slideP) {
      return this.cachedLeftSide ? baseX + (slideP - 1.0F) * this.currentWidth : baseX + (1.0F - slideP) * this.currentWidth;
   }

   private void applyPosition() {
      if ((Boolean)this.autoPosition.getValue()) {
         this.rect.setX(1.0F);
         this.rect.setY(25.0F);
      } else if ((Boolean)this.futurePosition.getValue()) {
         this.rect.setX(1.0F);
         this.rect.setY(15.0F);
      } else {
         this.rect.setX((Float)this.x.getValue());
         this.rect.setY((Float)this.y.getValue());
      }

   }

   private void refreshQuadrant() {
      int sw = Vonware.textManager.scaledWidth;
      this.cachedLeftSide = this.rect.getX() + this.currentWidth / 2.0F < (float)sw / 2.0F;
   }

   private void drawHead(EntityPlayer player, float x, float y, float size) {
      if (player instanceof AbstractClientPlayer) {
         AbstractClientPlayer cp = (AbstractClientPlayer)player;
         GlStateManager.pushMatrix();
         GlStateManager.enableTexture2D();
         mc.getTextureManager().bindTexture(cp.getLocationSkin());
         GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
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
         GlStateManager.popMatrix();
      }
   }

   private Color getCustomColor() {
      if (ColorPickerConfig.getState(this.customColor).useGlobalColor) {
         Colors colors = Colors.getInstance();
         if ((Boolean)colors.rainbow.getValue()) {
            return colors.rainbowMode.getValue() == Colors.RainbowMode.Static ? ColorUtil.rainbow(0) : ColorUtil.rainbow((int)((float)NewComponent.counter1[0] * (Float)colors.rainbowHue.getValue()));
         } else {
            return (Boolean)ClickGui.getInstance().pulseGui.getValue() ? Global.getPulseColor(colors.getGuiColor(), 0) : colors.getCurrentColor();
         }
      } else {
         int c = (Integer)this.customColor.getValue();
         return new Color(c >> 16 & 255, c >> 8 & 255, c & 255);
      }
   }

   public static enum SortMode {
      DISTANCE,
      ALPHABETICAL,
      LENGTH;
   }

   private static final class PlayerEntry {
      final String label;
      final int distance;
      final EntityPlayer player;

      PlayerEntry(String label, int distance, EntityPlayer player) {
         this.label = label;
         this.distance = distance;
         this.player = player;
      }
   }

   private static final class LeavingEntry {
      final String label;
      final EntityPlayer player;
      final float lastY;
      final DeltaAnim slideAnim;

      LeavingEntry(String label, EntityPlayer player, float lastY, DeltaAnim slideAnim) {
         this.label = label;
         this.player = player;
         this.lastY = lastY;
         this.slideAnim = slideAnim;
      }
   }

   public static final class DeltaAnim {
      private float progress;
      private final long durationMs;
      private long lastTime;

      public DeltaAnim(long durationMs, float initial) {
         this.durationMs = durationMs;
         this.progress = initial;
         this.lastTime = System.nanoTime();
      }

      public float tick(boolean target) {
         long now = System.nanoTime();
         long elapsedMs = (now - this.lastTime) / 1000000L;
         this.lastTime = now;
         if (this.durationMs <= 0L) {
            this.progress = target ? 1.0F : 0.0F;
            return this.progress;
         } else {
            float delta = (float)elapsedMs / (float)this.durationMs;
            this.progress = target ? Math.min(1.0F, this.progress + delta) : Math.max(0.0F, this.progress - delta);
            return this.progress;
         }
      }

      public float getProgress() {
         return this.progress;
      }

      public void snap(boolean open) {
         this.progress = open ? 1.0F : 0.0F;
      }
   }
}
