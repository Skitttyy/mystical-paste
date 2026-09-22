package fat.vonware.features.modules.misc;

import fat.vonware.Vonware;
import fat.vonware.event.events.PacketEvent;
import fat.vonware.event.events.Render3DEvent;
import fat.vonware.features.modules.Module;
import fat.vonware.features.setting.Setting;
import fat.vonware.util.Animations.game.ReplyWaypointRenderer;
import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.network.play.server.SPacketChat;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.Display;

public class AutoReply extends Module {
   private final Setting<ReplyMode> replyMode;
   private final Setting<String> sendMessage;
   private final Setting<Boolean> accountForAFK;
   private final Setting<Boolean> antiBaseLeak;
   private final Setting<Integer> maxDistance;
   private final Setting<Boolean> simpleReply;
   private final Setting<Boolean> wayPointVisual;
   private final Setting<ReplyWaypointRenderer.WaypointMode> wayPointMode;
   private final Setting<Integer> cooldown;
   private static final long TAB_OUT_AFK_TIME_MS = 10000L;
   private static final long PAUSE_MENU_AFK_TIME_MS = 10000L;
   private static final Pattern XYZ_LABELED = Pattern.compile("x[:\\s]+(-?\\d+).*?y[:\\s]+(-?\\d+).*?z[:\\s]+(-?\\d+)", 34);
   private static final Pattern XYZ_PREFIXED = Pattern.compile("xyz[:\\s]+(-?\\d+)[,\\s]+(-?\\d+)[,\\s]+(-?\\d+)", 2);
   private static final Pattern XYZ_SIMPLE = Pattern.compile("(-?\\d+)[,\\s]+(-?\\d+)[,\\s]+(-?\\d+)");
   private final Map<String, Long> cooldowns;
   private long lastMovementTime;
   private long tabbedOutStartTime;
   private long pauseMenuStartTime;
   private double lastX;
   private double lastY;
   private double lastZ;

   public AutoReply() {
      super("AutoReply", "replies to people", Module.Category.MISC, true, false, false);
      this.replyMode = (new Setting("Mode", AutoReply.ReplyMode.Coords)).withDescription((Function<ReplyMode, String>)((mode) -> {
         switch (mode) {
            case Coords:
               return "Coords: Automatically replies with your coordinates.";
            case Custom:
               return "Custom: Automatically replies using the text configured in Message.";
            default:
               return "";
         }
      }));
      this.sendMessage = new Setting<String>("Message", "bitcoin miner", (v) -> this.replyMode.getValue() == AutoReply.ReplyMode.Custom, "The custom private message sent when Custom mode replies.");
      this.accountForAFK = new Setting<Boolean>("AccountForAFK", false, (v) -> this.replyMode.getValue() == AutoReply.ReplyMode.Coords || this.replyMode.getValue() == AutoReply.ReplyMode.Custom, "When you receive a message this Module will reply with an AFK message when it has assumed you have gone AFK.");
      this.antiBaseLeak = new Setting<Boolean>("AntiBaseLeak", true, (v) -> this.replyMode.getValue() == AutoReply.ReplyMode.Coords, "Prevents coordinate leaks when you are farther from spawn than the allowed distance.");
      this.maxDistance = new Setting<Integer>("MaxDistance", 1000, 0, 10000, (v) -> (Boolean)this.antiBaseLeak.getValue() && this.replyMode.getValue() == AutoReply.ReplyMode.Coords, "Maximum horizontal distance from spawn where coordinate replies are allowed.");
      this.simpleReply = new Setting<Boolean>("Simple Coords", false, (v) -> this.replyMode.getValue() == AutoReply.ReplyMode.Coords, "Uses the shorter coordinate-reply format.");
      this.wayPointVisual = new Setting<Boolean>("WayPointVisual", false, "Creates visual waypoints from coordinates received through supported messages.");
      this.wayPointMode = (new Setting("WayPointMode", ReplyWaypointRenderer.WaypointMode.Beam, (v) -> (Boolean)this.wayPointVisual.getValue())).withDescription((Function<ReplyWaypointRenderer.WaypointMode, String>)((mode) -> {
         switch (mode) {
            case Beam:
               return "Beam: Renders received-coordinate waypoints as vertical beacon-style beams.";
            case Box:
               return "Box: Renders received-coordinate waypoints as boxes at their world positions.";
            default:
               return "";
         }
      }));
      this.cooldown = new Setting<Integer>("Cooldown", 5, 0, 60, "Cooldown in seconds before the same player can trigger another automatic reply.");
      this.cooldowns = new HashMap();
      this.lastMovementTime = System.currentTimeMillis();
      this.tabbedOutStartTime = -1L;
      this.pauseMenuStartTime = -1L;
      this.register(this.replyMode);
      this.register(this.sendMessage);
      this.register(this.accountForAFK);
      this.register(this.antiBaseLeak);
      this.register(this.maxDistance);
      this.register(this.simpleReply);
      this.register(this.wayPointVisual);
      this.register(this.wayPointMode);
      this.register(this.cooldown);
   }

   public void onEnable() {
      if (!fullNullCheck()) {
         long now = System.currentTimeMillis();
         this.lastMovementTime = now;
         this.lastX = mc.player.posX;
         this.lastY = mc.player.posY;
         this.lastZ = mc.player.posZ;
         this.tabbedOutStartTime = Display.isActive() ? -1L : now;
         this.pauseMenuStartTime = mc.currentScreen instanceof GuiIngameMenu ? now : -1L;
      }
   }

   public void onDisable() {
      ReplyWaypointRenderer.clear();
      this.tabbedOutStartTime = -1L;
      this.pauseMenuStartTime = -1L;
   }

   public void onUpdate() {
      if (!fullNullCheck()) {
         if (!(Boolean)this.accountForAFK.getValue()) {
            this.tabbedOutStartTime = -1L;
            this.pauseMenuStartTime = -1L;
         } else {
            long now = System.currentTimeMillis();
            this.updateMovementTimer(now);
            this.updateTabbedOutTimer(now);
            this.updatePauseMenuTimer(now);
         }
      }
   }

   private void updateMovementTimer(long now) {
      double currentX = mc.player.posX;
      double currentY = mc.player.posY;
      double currentZ = mc.player.posZ;
      if (currentX != this.lastX || currentY != this.lastY || currentZ != this.lastZ) {
         this.lastMovementTime = now;
         this.lastX = currentX;
         this.lastY = currentY;
         this.lastZ = currentZ;
      }

   }

   private void updateTabbedOutTimer(long now) {
      if (!Display.isActive()) {
         if (this.tabbedOutStartTime == -1L) {
            this.tabbedOutStartTime = now;
         }
      } else {
         this.tabbedOutStartTime = -1L;
      }

   }

   private void updatePauseMenuTimer(long now) {
      if (mc.currentScreen instanceof GuiIngameMenu) {
         if (this.pauseMenuStartTime == -1L) {
            this.pauseMenuStartTime = now;
         }
      } else {
         this.pauseMenuStartTime = -1L;
      }

   }

   private boolean isAFK(long currentTime) {
      int movementThreshold = Display.isActive() ? 30 : 60;
      boolean movementAFK = currentTime - this.lastMovementTime >= (long)movementThreshold * 1000L;
      boolean tabbedOutAFK = this.tabbedOutStartTime != -1L && currentTime - this.tabbedOutStartTime >= 10000L;
      boolean pauseMenuAFK = this.pauseMenuStartTime != -1L && currentTime - this.pauseMenuStartTime >= 10000L;
      return movementAFK || tabbedOutAFK || pauseMenuAFK;
   }

   @SubscribeEvent
   public void onRender3D(Render3DEvent event) {
      if (!fullNullCheck() && (Boolean)this.wayPointVisual.getValue()) {
         ReplyWaypointRenderer.render(true, this.wayPointMode.getValue());
      }
   }

   @SubscribeEvent
   public void onPacketReceive(PacketEvent.Receive event) {
      if (!fullNullCheck() && !this.isDisabled()) {
         if (event.getPacket() instanceof SPacketChat) {
            SPacketChat packet = (SPacketChat)event.getPacket();
            String unformatted = packet.getChatComponent().getUnformattedText();
            if (unformatted.contains("says: ") || unformatted.contains("whispers: ")) {
               String ign = unformatted.split(" ")[0];
               if (!mc.player.getName().equals(ign) && Vonware.friendManager.isFriend(ign)) {
                  long currentTime = System.currentTimeMillis();
                  if (!this.cooldowns.containsKey(ign) || currentTime - (Long)this.cooldowns.get(ign) >= (long)(Integer)this.cooldown.getValue() * 1000L) {
                     if ((Boolean)this.accountForAFK.getValue() && this.isAFK(currentTime)) {
                        this.cooldowns.put(ign, currentTime);
                        mc.player.sendChatMessage("/msg " + ign + " I am currently AFK.");
                     } else {
                        String msg = unformatted.toLowerCase();
                        boolean isCoordMessage = msg.contains("my coordinates are") || msg.contains("<coords>") || msg.contains("i am at") && (msg.contains("x:") || msg.contains("x :")) || msg.contains("xyz:");
                        if (isCoordMessage) {
                           if ((Boolean)this.wayPointVisual.getValue()) {
                              this.parseAndAddWaypoint(ign, unformatted);
                           }

                        } else {
                           if (this.replyMode.getValue() == AutoReply.ReplyMode.Coords) {
                              this.handleCoordsReply(ign, msg, currentTime);
                           } else {
                              this.handleCustomReply(ign, currentTime);
                           }

                        }
                     }
                  }
               }
            }
         }
      }
   }

   private boolean isWithinReplyRange() {
      double x = mc.player.posX;
      double z = mc.player.posZ;
      if (mc.player.dimension == -1) {
         x *= (double)8.0F;
         z *= (double)8.0F;
      }

      return Math.sqrt(x * x + z * z) <= (double)(Integer)this.maxDistance.getValue();
   }

   private void handleCoordsReply(String ign, String msg, long currentTime) {
      if (msg.matches(".*(cord|coord|coords|cords|wya|where are u|where are you|where r u|where ru|wru|where u at).*") && !msg.matches(".*(discord|record).*")) {
         if (!(Boolean)this.antiBaseLeak.getValue() || this.isWithinReplyRange()) {
            this.cooldowns.put(ign, currentTime);
            int x = (int)mc.player.posX;
            int y = (int)mc.player.posY;
            int z = (int)mc.player.posZ;
            String dimension = this.getDimensionName(mc.player.dimension);
            if ((Boolean)this.simpleReply.getValue()) {
               mc.player.sendChatMessage("/msg " + ign + " <Coords> " + x + ", " + y + ", " + z + " in the " + dimension);
            } else {
               mc.player.sendChatMessage("/msg " + ign + " My Coordinates Are X: " + x + " Y: " + y + " Z: " + z + " in the " + dimension);
            }

         }
      }
   }

   private void handleCustomReply(String ign, long currentTime) {
      this.cooldowns.put(ign, currentTime);
      mc.player.sendChatMessage("/msg " + ign + " " + (String)this.sendMessage.getValue());
   }

   private void parseAndAddWaypoint(String ign, String rawMessage) {
      try {
         Matcher matcher = XYZ_LABELED.matcher(rawMessage);
         if (matcher.find()) {
            this.push(ign, Double.parseDouble(matcher.group(1)), Double.parseDouble(matcher.group(2)), Double.parseDouble(matcher.group(3)));
            return;
         }

         matcher = XYZ_PREFIXED.matcher(rawMessage);
         if (matcher.find()) {
            this.push(ign, Double.parseDouble(matcher.group(1)), Double.parseDouble(matcher.group(2)), Double.parseDouble(matcher.group(3)));
            return;
         }

         matcher = XYZ_SIMPLE.matcher(rawMessage);
         if (matcher.find()) {
            this.push(ign, Double.parseDouble(matcher.group(1)), Double.parseDouble(matcher.group(2)), Double.parseDouble(matcher.group(3)));
         }
      } catch (Exception var4) {
      }

   }

   private void push(String ign, double worldX, double worldY, double worldZ) {
      double deltaX = worldX + (double)0.5F - mc.player.posX;
      double deltaY = worldY - mc.player.posY;
      double deltaZ = worldZ + (double)0.5F - mc.player.posZ;
      double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
      ReplyWaypointRenderer.add(new ReplyWaypointRenderer.ReplyWaypoint(ign, worldX, worldY, worldZ, playerColor(ign), distance));
   }

   private static Color playerColor(String name) {
      float hue = (float)Math.abs(name.hashCode() % 360) / 360.0F;
      return Color.getHSBColor(hue, 0.85F, 1.0F);
   }

   private String getDimensionName(int id) {
      if (id == 0) {
         return "OverWorld";
      } else if (id == 1) {
         return "End";
      } else {
         return id == -1 ? "Nether" : "Unknown";
      }
   }

   public static enum ReplyMode {
      Coords,
      Custom;
   }
}
