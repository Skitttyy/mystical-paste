package fat.vonware.features.modules.render;

import fat.vonware.event.events.PacketEvent;
import fat.vonware.event.events.Render3DEvent;
import fat.vonware.features.modules.Module;
import fat.vonware.features.modules.client.Colors;
import fat.vonware.features.setting.ColorSetting;
import fat.vonware.features.setting.Setting;
import fat.vonware.util.Animations.render.ColorUtil;
import java.awt.Color;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.GlStateManager.DestFactor;
import net.minecraft.client.renderer.GlStateManager.SourceFactor;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.server.SPacketSpawnObject;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

public class Trails extends Module {
   private final Setting<Boolean> playerTrails = new Setting<Boolean>("PlayerTrails", true);
   private final ColorSetting playerColor = new ColorSetting("Player Color", 16777215, true, (v) -> (Boolean)this.playerTrails.getValue());
   private final Setting<Boolean> self = new Setting<Boolean>("Self", false, (v) -> (Boolean)this.playerTrails.getValue());
   private final Setting<Boolean> others = new Setting<Boolean>("Others", true, (v) -> (Boolean)this.playerTrails.getValue());
   private final Setting<Float> playerFadeTime = new Setting<Float>("Player Fade", 1.5F, 0.1F, 30.0F, (v) -> (Boolean)this.playerTrails.getValue());
   private final Setting<Float> playerWidth = new Setting<Float>("Player Width", 1.5F, 0.1F, 5.0F, (v) -> (Boolean)this.playerTrails.getValue());
   private final Setting<Boolean> arrows = new Setting<Boolean>("Arrows", false);
   private final Setting<Boolean> pearls = new Setting<Boolean>("Pearls", true);
   private final Setting<Boolean> snowballs = new Setting<Boolean>("Snowballs", false);
   private final ColorSetting projectileColor = new ColorSetting("Projectile Color", 16777215, true);
   private final Setting<Boolean> projectileRainbow = new Setting<Boolean>("Projectile Rainbow", false);
   private final Setting<Float> projectileFadeTime = new Setting<Float>("Projectile Fade", 2.0F, 0.1F, 10.0F);
   private final Setting<Integer> projectileAlpha = new Setting<Integer>("Projectile Alpha", 230, 0, 255);
   private final Setting<Float> projectileWidth = new Setting<Float>("Projectile Width", 1.6F, 0.1F, 5.0F);
   private final Map<UUID, List<TrailPoint>> playerPoints = new ConcurrentHashMap();
   private final Map<Integer, List<TrailPoint>> projectilePoints = new ConcurrentHashMap();
   private final Map<Integer, Integer> projectileDimensions = new ConcurrentHashMap();

   public Trails() {
      super("Trails", "Draws trails behind players and projectiles.", Module.Category.RENDER, true, false, false);
      this.register(this.playerTrails);
      this.register(this.self);
      this.register(this.others);
      this.register(this.playerColor);
      this.register(this.playerFadeTime);
      this.register(this.playerWidth);
      this.register(this.arrows);
      this.register(this.pearls);
      this.register(this.snowballs);
      this.register(this.projectileColor);
      this.register(this.projectileRainbow);
      this.register(this.projectileFadeTime);
      this.register(this.projectileAlpha);
      this.register(this.projectileWidth);
   }

   public void onDisable() {
      this.playerPoints.clear();
      this.projectilePoints.clear();
      this.projectileDimensions.clear();
   }

   public void onUpdate() {
      if (!fullNullCheck()) {
         long now = System.currentTimeMillis();
         if ((Boolean)this.playerTrails.getValue()) {
            this.updatePlayerTrails(now);
         } else {
            this.playerPoints.clear();
         }

         if (this.hasProjectileTrailsEnabled()) {
            this.updateProjectileTrails(now);
         } else {
            this.projectilePoints.clear();
            this.projectileDimensions.clear();
         }

      }
   }

   private void updatePlayerTrails(long now) {
      if (!mc.player.isSpectator()) {
         for(Entity entity : mc.world.loadedEntityList) {
            if (entity instanceof EntityPlayer && (entity != mc.player || (Boolean)this.self.getValue()) && (entity == mc.player || (Boolean)this.others.getValue())) {
               List<TrailPoint> points = this.playerPoints.computeIfAbsent(entity.getUniqueID(), (id) -> new ArrayList<TrailPoint>());
               Vec3d position = entity.getPositionVector();
               if (points.isEmpty() || !position.equals(((TrailPoint)points.get(points.size() - 1)).pos)) {
                  points.add(new TrailPoint(position, now));
               }
            }
         }

         long fade = (long)((Float)this.playerFadeTime.getValue() * 1000.0F);
         Iterator<Map.Entry<UUID, List<TrailPoint>>> iterator = this.playerPoints.entrySet().iterator();

         while(iterator.hasNext()) {
            Map.Entry<UUID, List<TrailPoint>> entry = iterator.next();
            entry.getValue().removeIf((point) -> now - point.timestamp > fade);
            if (entry.getValue().isEmpty()) {
               iterator.remove();
            }
         }

      }
   }

   private void updateProjectileTrails(long now) {
      Iterator<Map.Entry<Integer, List<TrailPoint>>> iterator = this.projectilePoints.entrySet().iterator();

      while(iterator.hasNext()) {
         Map.Entry<Integer, List<TrailPoint>> entry = (Map.Entry)iterator.next();
         int entityId = (Integer)entry.getKey();
         List<TrailPoint> points = (List)entry.getValue();
         Entity entity = mc.world.getEntityByID(entityId);
         if (entity != null && !entity.isDead) {
            Vec3d position = entity.getPositionVector();
            int dimension = mc.world.provider.getDimensionType().getId();
            Integer previousDimension = (Integer)this.projectileDimensions.get(entityId);
            if (previousDimension != null && previousDimension != dimension) {
               points.clear();
            } else if (!points.isEmpty() && position.distanceTo(((TrailPoint)points.get(points.size() - 1)).pos) > (double)100.0F) {
               points.clear();
            }

            if (points.isEmpty() || !position.equals(((TrailPoint)points.get(points.size() - 1)).pos)) {
               points.add(new TrailPoint(position, now));
               this.projectileDimensions.put(entityId, dimension);
            }

            while(points.size() > 100) {
               points.remove(0);
            }
         }

         long fade = (long)((Float)this.projectileFadeTime.getValue() * 1000.0F);
         points.removeIf((point) -> now - point.timestamp > fade);
         if (points.isEmpty() && (entity == null || entity.isDead)) {
            iterator.remove();
            this.projectileDimensions.remove(entityId);
         }
      }

   }

   @SubscribeEvent
   public void onPacketReceive(PacketEvent.Receive event) {
      if (this.hasProjectileTrailsEnabled() && event.getPacket() instanceof SPacketSpawnObject && mc.world != null) {
         SPacketSpawnObject packet = (SPacketSpawnObject)event.getPacket();
         int type = packet.getType();
         if ((Boolean)this.pearls.getValue() && type == 65 || (Boolean)this.arrows.getValue() && type == 60 || (Boolean)this.snowballs.getValue() && type == 61) {
            List<TrailPoint> points = new ArrayList();
            points.add(new TrailPoint(new Vec3d(packet.getX(), packet.getY(), packet.getZ()), System.currentTimeMillis()));
            this.projectilePoints.put(packet.getEntityID(), points);
            this.projectileDimensions.put(packet.getEntityID(), mc.world.provider.getDimensionType().getId());
         }
      }
   }

   @SubscribeEvent
   public void onRender3D(Render3DEvent event) {
      if (!fullNullCheck()) {
         GLStateSnapshot state = new GLStateSnapshot();
         prepareGL();

         try {
            if ((Boolean)this.playerTrails.getValue()) {
               this.renderPlayerTrails();
            }

            if (this.hasProjectileTrailsEnabled()) {
               this.renderProjectileTrails();
            }
         } finally {
            releaseGL(state);
         }

      }
   }

   private boolean hasProjectileTrailsEnabled() {
      return (Boolean)this.arrows.getValue() || (Boolean)this.pearls.getValue() || (Boolean)this.snowballs.getValue();
   }

   private void renderPlayerTrails() {
      long now = System.currentTimeMillis();
      float fade = (Float)this.playerFadeTime.getValue() * 1000.0F;
      Color color = ColorUtil.getResolvedColor(this.playerColor);
      GL11.glLineWidth((Float)this.playerWidth.getValue());

      for(List<TrailPoint> points : this.playerPoints.values()) {
         List<TrailPoint> snapshot;
         try {
            snapshot = new ArrayList(points);
         } catch (ConcurrentModificationException var15) {
            continue;
         }

         if (snapshot.size() >= 2) {
            GL11.glBegin(3);

            try {
               for(TrailPoint point : snapshot) {
                  float alpha = MathHelper.clamp(1.0F - (float)(now - point.timestamp) / fade, 0.0F, 1.0F);
                  GlStateManager.color((float)color.getRed() / 255.0F, (float)color.getGreen() / 255.0F, (float)color.getBlue() / 255.0F, alpha);
                  GL11.glVertex3d(point.pos.x - mc.getRenderManager().renderPosX, point.pos.y - mc.getRenderManager().renderPosY, point.pos.z - mc.getRenderManager().renderPosZ);
               }
            } finally {
               GL11.glEnd();
            }
         }
      }

   }

   private void renderProjectileTrails() {
      long now = System.currentTimeMillis();
      float maxAlpha = (float)(Integer)this.projectileAlpha.getValue() / 255.0F;
      Colors colors = Colors.getInstance();
      boolean rainbow = (Boolean)this.projectileRainbow.getValue() && colors != null && (Boolean)colors.rainbow.getValue();
      Color color = rainbow ? ColorUtil.rainbow(0) : ColorUtil.getResolvedColor(this.projectileColor);
      GL11.glLineWidth((Float)this.projectileWidth.getValue());

      for(List<TrailPoint> points : this.projectilePoints.values()) {
         List<TrailPoint> snapshot;
         try {
            snapshot = new ArrayList(points);
         } catch (ConcurrentModificationException var19) {
            continue;
         }

         if (snapshot.size() >= 2) {
            GL11.glBegin(3);

            try {
               for(TrailPoint point : snapshot) {
                  float fade = (Float)this.projectileFadeTime.getValue() * 1000.0F;
                  float progress = Math.min(1.0F, (float)(now - point.timestamp) / fade);
                  float alpha = MathHelper.clamp(maxAlpha * (1.0F - progress), 0.0F, 1.0F);
                  GlStateManager.color((float)color.getRed() / 255.0F, (float)color.getGreen() / 255.0F, (float)color.getBlue() / 255.0F, alpha);
                  GL11.glVertex3d(point.pos.x - mc.getRenderManager().renderPosX, point.pos.y - mc.getRenderManager().renderPosY, point.pos.z - mc.getRenderManager().renderPosZ);
               }
            } finally {
               GL11.glEnd();
            }
         }
      }

   }

   private static void prepareGL() {
      GlStateManager.pushMatrix();
      GlStateManager.enableBlend();
      GL11.glEnable(3042);
      GlStateManager.tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ZERO);
      GL14.glBlendFuncSeparate(770, 771, 1, 0);
      GlStateManager.disableTexture2D();
      GL11.glDisable(3553);
      GlStateManager.disableLighting();
      GL11.glDisable(2896);
      GlStateManager.disableCull();
      GL11.glDisable(2884);
      GlStateManager.disableDepth();
      GL11.glDisable(2929);
      GlStateManager.depthMask(false);
      GL11.glDepthMask(false);
      GlStateManager.shadeModel(7425);
      GL11.glShadeModel(7425);
      GL11.glEnable(2848);
      GL11.glHint(3154, 4354);
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
   }

   private static void releaseGL(GLStateSnapshot state) {
      try {
         setBlend(state.blend);
         GlStateManager.tryBlendFuncSeparate(state.blendSrcRgb, state.blendDstRgb, state.blendSrcAlpha, state.blendDstAlpha);
         GL14.glBlendFuncSeparate(state.blendSrcRgb, state.blendDstRgb, state.blendSrcAlpha, state.blendDstAlpha);
         setTexture2D(state.texture2D);
         setLighting(state.lighting);
         setCull(state.cull);
         setDepth(state.depth);
         GlStateManager.depthMask(state.depthMask);
         GL11.glDepthMask(state.depthMask);
         GlStateManager.shadeModel(state.shadeModel);
         GL11.glShadeModel(state.shadeModel);
         if (state.lineSmooth) {
            GL11.glEnable(2848);
         } else {
            GL11.glDisable(2848);
         }

         GL11.glHint(3154, state.lineSmoothHint);
         GL11.glLineWidth(state.lineWidth);
         GlStateManager.color(state.color[0], state.color[1], state.color[2], state.color[3]);
         GL11.glColor4f(state.color[0], state.color[1], state.color[2], state.color[3]);
      } finally {
         GlStateManager.popMatrix();
      }

   }

   private static void setBlend(boolean enabled) {
      if (enabled) {
         GlStateManager.enableBlend();
         GL11.glEnable(3042);
      } else {
         GlStateManager.disableBlend();
         GL11.glDisable(3042);
      }

   }

   private static void setTexture2D(boolean enabled) {
      if (enabled) {
         GlStateManager.enableTexture2D();
         GL11.glEnable(3553);
      } else {
         GlStateManager.disableTexture2D();
         GL11.glDisable(3553);
      }

   }

   private static void setLighting(boolean enabled) {
      if (enabled) {
         GlStateManager.enableLighting();
         GL11.glEnable(2896);
      } else {
         GlStateManager.disableLighting();
         GL11.glDisable(2896);
      }

   }

   private static void setCull(boolean enabled) {
      if (enabled) {
         GlStateManager.enableCull();
         GL11.glEnable(2884);
      } else {
         GlStateManager.disableCull();
         GL11.glDisable(2884);
      }

   }

   private static void setDepth(boolean enabled) {
      if (enabled) {
         GlStateManager.enableDepth();
         GL11.glEnable(2929);
      } else {
         GlStateManager.disableDepth();
         GL11.glDisable(2929);
      }

   }

   private static final class GLStateSnapshot {
      private final boolean blend;
      private final boolean texture2D;
      private final boolean lighting;
      private final boolean cull;
      private final boolean depth;
      private final boolean depthMask;
      private final boolean lineSmooth;
      private final int blendSrcRgb;
      private final int blendDstRgb;
      private final int blendSrcAlpha;
      private final int blendDstAlpha;
      private final int shadeModel;
      private final int lineSmoothHint;
      private final float lineWidth;
      private final float[] color;

      private GLStateSnapshot() {
         this.blend = GL11.glIsEnabled(3042);
         this.texture2D = GL11.glIsEnabled(3553);
         this.lighting = GL11.glIsEnabled(2896);
         this.cull = GL11.glIsEnabled(2884);
         this.depth = GL11.glIsEnabled(2929);
         this.depthMask = GL11.glGetBoolean(2930);
         this.lineSmooth = GL11.glIsEnabled(2848);
         this.blendSrcRgb = GL11.glGetInteger(32969);
         this.blendDstRgb = GL11.glGetInteger(32968);
         this.blendSrcAlpha = GL11.glGetInteger(32971);
         this.blendDstAlpha = GL11.glGetInteger(32970);
         this.shadeModel = GL11.glGetInteger(2900);
         this.lineSmoothHint = GL11.glGetInteger(3154);
         this.lineWidth = GL11.glGetFloat(2849);
         this.color = readCurrentColor();
      }

      private static float[] readCurrentColor() {
         FloatBuffer buffer = BufferUtils.createFloatBuffer(16);
         GL11.glGetFloat(2816, buffer);
         return new float[]{buffer.get(0), buffer.get(1), buffer.get(2), buffer.get(3)};
      }
   }

   private static class TrailPoint {
      private final Vec3d pos;
      private final long timestamp;

      private TrailPoint(Vec3d pos, long timestamp) {
         this.pos = pos;
         this.timestamp = timestamp;
      }
   }
}
