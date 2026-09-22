package fat.vonware.features.modules.misc;

import fat.vonware.Global;
import fat.vonware.Vonware;
import fat.vonware.event.events.EntityWorldEvent;
import fat.vonware.features.command.Command;
import fat.vonware.features.modules.Module;
import fat.vonware.features.modules.client.Manager;
import fat.vonware.features.setting.Setting;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class VisualRange extends Module {
   public Setting<Boolean> sound = new Setting<Boolean>("Sound", true, "Plays a sound when a player enters or leaves visual range.");
   public Setting<Boolean> coordinates = new Setting<Boolean>("Coordinates", false, "Includes the player's coordinates in visual-range notifications.");
   protected final Setting<Type> visualType;
   private final Minecraft mc;
   private final Map<UUID, Long> recentlyRemoved;

   public VisualRange() {
      super("VisualRange", "Detects when someone enters your visual range", Module.Category.MISC, true, false, false);
      this.visualType = (new Setting("VisualType", VisualRange.Type.Both)).withDescription((Function<Type, String>)((mode) -> {
         switch (mode) {
            case Entering:
               return "Entering: Only notifies when players enter visual range.";
            case Leaving:
               return "Leaving: Only notifies when players leave visual range.";
            case Both:
               return "Both: Notifies when players both enter and leave visual range.";
            default:
               return "";
         }
      }));
      this.mc = Minecraft.getMinecraft();
      this.recentlyRemoved = new HashMap();
      this.register(this.sound);
      this.register(this.coordinates);
      this.register(this.visualType);
   }

   @SubscribeEvent
   public void onEntityAdded(EntityWorldEvent.Add event) {
      if (this.mc.player != null && this.mc.world != null) {
         if (event.getEntity() instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer)event.getEntity();
            if (!this.isSelf(player)) {
               if (this.visualType.getValue() == VisualRange.Type.Entering || this.visualType.getValue() == VisualRange.Type.Both) {
                  this.handlePlayerEnter(player);
               }

            }
         }
      }
   }

   @SubscribeEvent
   public void onEntityRemoved(EntityWorldEvent.Remove event) {
      if (this.mc.player != null && this.mc.world != null) {
         if (event.getEntity() instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer)event.getEntity();
            if (!this.isSelf(player)) {
               UUID uuid = player.getUniqueID();
               long currentTime = System.currentTimeMillis();
               if (this.recentlyRemoved.containsKey(uuid)) {
                  long lastRemoval = (Long)this.recentlyRemoved.get(uuid);
                  if (currentTime - lastRemoval < 1000L) {
                     return;
                  }
               }

               this.recentlyRemoved.put(uuid, currentTime);
               this.recentlyRemoved.entrySet().removeIf((entry) -> currentTime - (Long)entry.getValue() > 2000L);
               if (this.visualType.getValue() == VisualRange.Type.Leaving || this.visualType.getValue() == VisualRange.Type.Both) {
                  this.handlePlayerLeave(player);
               }

            }
         }
      }
   }

   private boolean isSelf(EntityPlayer player) {
      return this.mc.player != null && player.getUniqueID().equals(this.mc.player.getUniqueID());
   }

   private void handlePlayerEnter(EntityPlayer player) {
      Command.sendSilentMessage(this.getEnterMessage(player));
      if ((Boolean)this.sound.getValue()) {
         this.mc.player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.0F);
      }

   }

   private void handlePlayerLeave(EntityPlayer player) {
      Command.sendSilentMessage(this.getLeaveMessage(player));
      if ((Boolean)this.sound.getValue()) {
         this.mc.player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 0.5F);
      }

   }

   private String getNotifierPrefix() {
      return Global.getClientName();
   }

   private String getEnterMessage(EntityPlayer player) {
      Manager.Notifier notifier = Manager.getInstance().notifier.getValue();
      String name = player.getName();
      String nameColor = Vonware.friendManager.isFriend(player) ? TextFormatting.AQUA.toString() : "";
      String message = this.getNotifierPrefix();
      if (notifier == Manager.Notifier.Iggyware) {
         message = message + (nameColor.isEmpty() ? TextFormatting.WHITE.toString() : nameColor) + name + " has" + TextFormatting.GREEN + " entered" + TextFormatting.WHITE + " your visual range!";
      } else {
         message = message + (nameColor.isEmpty() ? TextFormatting.RED.toString() : nameColor) + name + TextFormatting.GRAY + " has entered your visual range";
      }

      if ((Boolean)this.coordinates.getValue()) {
         int x = (int)player.posX;
         int y = (int)player.posY;
         int z = (int)player.posZ;
         message = message + " " + TextFormatting.GRAY + "(" + TextFormatting.WHITE + x + ", " + y + ", " + z + TextFormatting.GRAY + ")";
      }

      return message;
   }

   private String getLeaveMessage(EntityPlayer player) {
      Manager.Notifier notifier = Manager.getInstance().notifier.getValue();
      String name = player.getName();
      String nameColor = Vonware.friendManager.isFriend(player) ? TextFormatting.AQUA.toString() : "";
      String message = this.getNotifierPrefix();
      if (notifier == Manager.Notifier.Iggyware) {
         message = message + (nameColor.isEmpty() ? TextFormatting.WHITE.toString() : nameColor) + name + " has" + TextFormatting.GREEN + " left" + TextFormatting.WHITE + " your visual range!";
      } else {
         message = message + (nameColor.isEmpty() ? TextFormatting.GREEN.toString() : nameColor) + name + TextFormatting.GRAY + " has left your visual range";
      }

      if ((Boolean)this.coordinates.getValue()) {
         int x = (int)player.posX;
         int y = (int)player.posY;
         int z = (int)player.posZ;
         message = message + " " + TextFormatting.GRAY + "(" + TextFormatting.WHITE + x + ", " + y + ", " + z + TextFormatting.GRAY + ")";
      }

      return message;
   }

   public static enum Type {
      Entering,
      Leaving,
      Both;
   }
}
