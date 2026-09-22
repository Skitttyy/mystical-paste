package fat.vonware.features.modules.render;

import fat.vonware.Global;
import fat.vonware.Vonware;
import fat.vonware.event.events.ConnectionEvent;
import fat.vonware.event.events.Render3DEvent;
import fat.vonware.features.command.Command;
import fat.vonware.features.modules.Module;
import fat.vonware.features.setting.ColorSetting;
import fat.vonware.features.setting.Setting;
import fat.vonware.util.Interpolation;
import fat.vonware.util.MathUtil;
import fat.vonware.util.Animations.render.ColorUtil;
import fat.vonware.util.Animations.render.Render2DMethods;
import fat.vonware.util.Animations.render.RenderMethods;
import fat.vonware.util.LogoutSpots.LogoutSpot;
import fat.vonware.util.LogoutSpots.PlayerRecord;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.GlStateManager.DestFactor;
import net.minecraft.client.renderer.GlStateManager.SourceFactor;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class LogoutSpots extends Module {
   public Setting<Boolean> globalColor = new Setting<Boolean>("GlobalColor", false);
   protected final Setting<RenderMode> render;
   private final ColorSetting customColor;
   public final Setting<InfoMode> infoMode;
   public final Setting<Boolean> showHealth;
   public Setting<Boolean> syncBorder;
   public Setting<Boolean> message;
   public Setting<Boolean> removeLogoutSpotsOnLog;
   public Setting<Float> scaling;
   private final Setting<Integer> outlineAlpha;
   private final Setting<Integer> boxAlpha;
   public final Map<UUID, LogoutSpot> spots;
   protected final ConcurrentHashMap<UUID, PlayerRecord> cachePlayers;

   public LogoutSpots() {
      super("LogoutSpots", "logs players logout spots", Module.Category.RENDER, true, false, false);
      this.render = new Setting<RenderMode>("RenderMode", LogoutSpots.RenderMode.Box);
      this.customColor = new ColorSetting("BoxColor", 16777215, false);
      this.infoMode = new Setting<InfoMode>("InfoType", LogoutSpots.InfoMode.Coordinates);
      this.showHealth = new Setting<Boolean>("ShowHealth", true);
      this.syncBorder = new Setting<Boolean>("SyncBorder", false);
      this.message = new Setting<Boolean>("Message", false);
      this.removeLogoutSpotsOnLog = new Setting<Boolean>("RemoveSpotsOnLog", false);
      this.scaling = new Setting<Float>("Scale", 0.2F, 0.1F, 1.0F);
      this.outlineAlpha = new Setting<Integer>("Alpha", 140, 0, 255, (v) -> this.render.getValue() == LogoutSpots.RenderMode.Box);
      this.boxAlpha = new Setting<Integer>("Box-Alpha", 30, 0, 255, (v) -> this.render.getValue() == LogoutSpots.RenderMode.Box);
      this.register(this.render);
      this.register(this.customColor);
      this.register(this.infoMode);
      this.register(this.showHealth);
      this.register(this.syncBorder);
      this.register(this.message);
      this.register(this.removeLogoutSpotsOnLog);
      this.register(this.scaling);
      this.register(this.globalColor);
      this.register(this.outlineAlpha);
      this.register(this.boxAlpha);
      this.cachePlayers = new ConcurrentHashMap();
      this.spots = new ConcurrentHashMap();
   }

   protected Color getCustomColor() {
      return ColorUtil.getResolvedColor(this.customColor);
   }

   public String getDisplayInfo() {
      return ((RenderMode)this.render.getValue()).name();
   }

   protected int getBoxAlpha() {
      return (Integer)this.boxAlpha.getValue();
   }

   protected int getOutlineAlpha() {
      return (Integer)this.outlineAlpha.getValue();
   }

   @SubscribeEvent
   public void onTick(TickEvent.ClientTickEvent event) {
      this.cachePlayers.entrySet().removeIf((entry) -> System.currentTimeMillis() - ((PlayerRecord)entry.getValue()).getTime() > 2500L);
      if (mc.world != null) {
         for(EntityPlayer entity : mc.world.playerEntities) {
            if (entity != null) {
               UUID uuid = entity.getUniqueID();
               PlayerRecord playerRecord = new PlayerRecord(entity, System.currentTimeMillis());
               this.cachePlayers.put(uuid, playerRecord);
               this.spots.remove(uuid);
            }
         }

      }
   }

   @SubscribeEvent
   public void onRender3D(Render3DEvent event) {
      if (mc.player != null && mc.player.ticksExisted >= 20) {
         if (!this.spots.isEmpty()) {
            List<LogoutSpot> spotList = new ArrayList(this.spots.values());
            spotList.sort(Comparator.comparing(LogoutSpot::getDistance));
            Frustum frustum = Interpolation.createFrustum(RenderMethods.getEntity());

            for(LogoutSpot spot : spotList) {
               AxisAlignedBB bb = Interpolation.interpolateAxis(spot.getBoundingBox());
               if (Interpolation.isInCamera(frustum, bb)) {
                  switch ((RenderMode)this.render.getValue()) {
                     case Box:
                        this.renderBox(bb);
                        break;
                     case Ghost:
                        this.renderGhost(spot, event.getPartialTicks());
                  }

                  String text = this.buildInfoText(spot);
                  this.renderNameTag(text, bb);
               }
            }

         }
      }
   }

   private String buildInfoText(LogoutSpot spot) {
      StringBuilder text = new StringBuilder(spot.getName());
      if ((Boolean)this.showHealth.getValue()) {
         text.append(" ").append(MathUtil.round((double)spot.getHealth(), 1)).append(" HP");
      }

      switch ((InfoMode)this.infoMode.getValue()) {
         case Coordinates:
            text.append(" at ").append(MathUtil.round(spot.getX(), 1)).append(", ").append(MathUtil.round(spot.getY(), 1)).append(", ").append(MathUtil.round(spot.getZ(), 1));
            break;
         case Distance:
            text.append(" ").append(MathUtil.round(spot.getDistance(), 1)).append("m away");
            break;
         case Time:
            text.append(" ").append(spot.getTimeSinceLogout()).append(" ago");
      }

      return text.toString();
   }

   private void renderBox(AxisAlignedBB bb) {
      RenderMethods.startRender();
      Color color = this.getCustomColor();
      RenderMethods.drawBox(bb, new Color(color.getRed(), color.getGreen(), color.getBlue(), this.getBoxAlpha()));
      RenderMethods.drawOutline(bb, 1.5F, new Color(color.getRed(), color.getGreen(), color.getBlue(), this.getOutlineAlpha()));
      RenderMethods.endRender();
   }

   private void renderGhost(LogoutSpot spot, float partialTicks) {
      EntityPlayer player = spot.getEntity();
      if (player != null) {
         player.hurtTime = 0;
         double x = spot.getX() - mc.getRenderManager().viewerPosX;
         double y = spot.getY() - mc.getRenderManager().viewerPosY;
         double z = spot.getZ() - mc.getRenderManager().viewerPosZ;
         GlStateManager.pushMatrix();
         GlStateManager.enableTexture2D();
         GlStateManager.enableBlend();
         GlStateManager.tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ZERO);
         float alpha = 0.5F;
         GlStateManager.color(0.737F, 0.737F, 0.737F, alpha);
         GlStateManager.disableLighting();
         GlStateManager.enableDepth();
         OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.0F, 240.0F);
         GlStateManager.enableAlpha();
         GlStateManager.alphaFunc(516, 0.003921569F);
         mc.getRenderManager().renderEntity(player, x, y, z, player.rotationYaw, partialTicks, false);
         GlStateManager.matrixMode(5890);
         GlStateManager.loadIdentity();
         GlStateManager.matrixMode(5888);
         GlStateManager.enableLighting();
         GlStateManager.enableDepth();
         GlStateManager.enableTexture2D();
         GlStateManager.enableAlpha();
         GlStateManager.alphaFunc(516, 0.1F);
         GlStateManager.disableBlend();
         GlStateManager.tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ZERO);
         GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
         GlStateManager.resetColor();
         GlStateManager.popMatrix();
      }
   }

   public void onLogout() {
      if ((Boolean)this.removeLogoutSpotsOnLog.getValue()) {
         this.spots.clear();
      }

   }

   @SubscribeEvent
   public void onConnection(ConnectionEvent event) {
      if (event.getStage() == 0) {
         this.handlePlayerJoin(event);
      } else if (event.getStage() == 1) {
         this.handlePlayerLeave(event);
      }

   }

   private void handlePlayerJoin(ConnectionEvent event) {
      if (!event.getName().equals(mc.getSession().getProfile().getName())) {
         LogoutSpot spot = (LogoutSpot)this.spots.remove(event.getUuid());
         if ((Boolean)this.message.getValue()) {
            String text;
            if (spot != null) {
               Vec3d pos = spot.rounded();
               text = TextFormatting.YELLOW + event.getName() + " is back at: " + (int)pos.x + ", " + (int)pos.y + ", " + (int)pos.z + ".";
            } else {
               EntityPlayer player = event.getEntity();
               if (player == null) {
                  return;
               }

               text = TextFormatting.YELLOW + player.getName() + " joined at: " + (int)player.posX + ", " + (int)player.posY + ", " + (int)player.posZ + ".";
            }

            Command.sendSilentMessage(text);
         }

      }
   }

   private void handlePlayerLeave(ConnectionEvent event) {
      EntityPlayer player = event.getEntity();
      if (player == null || !player.getName().equals(mc.getSession().getProfile().getName())) {
         if (player == null || !this.spots.containsKey(player.getUniqueID())) {
            if ((Boolean)this.message.getValue() && player != null) {
               String text = TextFormatting.YELLOW + player.getName() + " logged out at: " + (int)player.posX + ", " + (int)player.posY + ", " + (int)player.posZ + ".";
               Command.sendSilentMessage(text);
            }

            if (player != null) {
               LogoutSpot spot = new LogoutSpot(player);
               this.spots.put(player.getUniqueID(), spot);
            }

         }
      }
   }

   public void onDisable() {
      this.spots.clear();
   }

   public void onLoad() {
      this.spots.clear();
   }

   protected void renderNameTag(String text, AxisAlignedBB interpolated) {
      double x = (interpolated.minX + interpolated.maxX) / (double)2.0F;
      double y = (interpolated.minY + interpolated.maxY) / (double)2.0F;
      double z = (interpolated.minZ + interpolated.maxZ) / (double)2.0F;
      this.renderNameTag(text, x, y - 0.2, z);
   }

   protected void renderNameTag(String name, double x, double y, double z) {
      double distance = RenderMethods.getEntity().getDistance(x + mc.getRenderManager().viewerPosX, y + mc.getRenderManager().viewerPosY, z + mc.getRenderManager().viewerPosZ);
      int width = Vonware.textManager.getStringWidth(name) >> 1;
      double scale = 0.0018 + (double)MathUtil.fixedNametagScaling((Float)this.scaling.getValue()) * distance;
      if (distance <= (double)8.0F) {
         scale = 0.0245;
      }

      GlStateManager.pushMatrix();
      GlStateManager.disableLighting();
      GlStateManager.translate((float)x, (float)y + 1.4F, (float)z);
      GlStateManager.rotate(-mc.getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);
      float var10001 = mc.gameSettings.thirdPersonView == 2 ? -1.0F : 1.0F;
      GlStateManager.rotate(mc.getRenderManager().playerViewX, var10001, 0.0F, 0.0F);
      GlStateManager.scale(-scale, -scale, scale);
      GlStateManager.disableDepth();
      GlStateManager.enableBlend();
      GlStateManager.tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ZERO);
      GlStateManager.disableTexture2D();
      int borderCol = (Boolean)this.syncBorder.getValue() ? Global.getGlobalColor().getRGB() : 1862270976;
      Render2DMethods.drawNameTagRect((float)(-width - 2), (float)(-mc.fontRenderer.FONT_HEIGHT), (float)width + 2.0F, 1.5F, 1392508928, borderCol, 1.4F);
      GlStateManager.enableTexture2D();
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      GlStateManager.enableBlend();
      GlStateManager.tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ZERO);
      Color textColor = (Boolean)this.globalColor.getValue() ? this.getCustomColor() : Color.WHITE;
      Vonware.textManager.drawString(name, (float)(-width), -8.0F, textColor.getRGB(), true);
      GlStateManager.enableDepth();
      GlStateManager.enableTexture2D();
      GlStateManager.disableBlend();
      GlStateManager.enableLighting();
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      GlStateManager.popMatrix();
   }

   public static enum RenderMode {
      Box,
      Ghost;
   }

   public static enum InfoMode {
      Coordinates,
      Distance,
      Time,
      None;
   }
}
