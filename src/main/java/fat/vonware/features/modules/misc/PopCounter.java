package fat.vonware.features.modules.misc;

import fat.vonware.Global;
import fat.vonware.Vonware;
import fat.vonware.event.events.ConnectionEvent;
import fat.vonware.event.events.DeathEvent;
import fat.vonware.event.events.EntityWorldEvent;
import fat.vonware.event.events.TotemPopEvent;
import fat.vonware.features.command.Command;
import fat.vonware.features.modules.Module;
import fat.vonware.features.modules.client.Manager;
import fat.vonware.features.setting.Setting;
import fat.vonware.util.HexColorText;
import java.util.HashMap;
import java.util.function.Function;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class PopCounter extends Module {
   public final Setting<Boolean> self = new Setting<Boolean>("Self", true, "Includes your own totem pops in the pop counter.");
   protected final Setting<SelfType> selfType;
   public Setting<Boolean> clearOnLogout;
   public Setting<Boolean> clearOnVisRange;
   private final HashMap<String, Integer> lastPopTick;
   private final HashMap<String, Integer> lastPopMessageIds;
   private boolean deathMessageSent;

   public PopCounter() {
      super("PopCounter", "Counts totem pops", Module.Category.MISC, true, false, false);
      this.selfType = (new Setting("SelfType", PopCounter.SelfType.You, (v) -> (Boolean)this.self.getValue())).withDescription((Function<SelfType, String>)((mode) -> {
         switch (mode) {
            case You:
               return "You: Refers to your own pops using \"You\" in the message.";
            case Name:
               return "Name: Uses your Minecraft username in self-pop messages.";
            default:
               return "";
         }
      }));
      this.clearOnLogout = new Setting<Boolean>("ClearOnLogout", true, "Clears stored pop counts when a player logs out.");
      this.clearOnVisRange = new Setting<Boolean>("clearOnVisRange", true, "Clears a player's stored pop count when they leave visual range.");
      this.lastPopTick = new HashMap();
      this.lastPopMessageIds = new HashMap();
      this.deathMessageSent = false;
      this.register(this.self);
      this.register(this.selfType);
      this.register(this.clearOnLogout);
      this.register(this.clearOnVisRange);
   }

   public void onEnable() {
      this.lastPopTick.clear();
      this.lastPopMessageIds.clear();
      this.deathMessageSent = false;
   }

   @SubscribeEvent
   public void onTotemPop(TotemPopEvent event) {
      if (!fullNullCheck()) {
         EntityPlayer player = event.getEntity();
         if (player != null) {
            if ((Boolean)this.self.getValue() || !player.equals(mc.player)) {
               int currentTick = mc.player.ticksExisted;
               String name = player.getName();
               if ((Integer)this.lastPopTick.getOrDefault(name, -1) != currentTick) {
                  this.lastPopTick.put(name, currentTick);
                  int pops = Vonware.totemPopManager.getPopCount(name);
                  String message;
                  if (this.selfType.getValue() == PopCounter.SelfType.You && player.equals(mc.player)) {
                     message = this.getYouMessage(pops);
                  } else {
                     message = this.getPopMessage(name, pops);
                  }

                  if (message != null && !message.isEmpty()) {
                     if (this.lastPopMessageIds.containsKey(name)) {
                        Command.sendSilentMessage("", (Integer)this.lastPopMessageIds.get(name));
                     }

                     int messageId = name.hashCode();
                     Command.sendSilentMessage(message, messageId);
                     this.lastPopMessageIds.put(name, messageId);
                  }
               }
            }
         }
      }
   }

   @SubscribeEvent(
      priority = EventPriority.HIGH
   )
   public void onDeath(DeathEvent event) {
      if (!fullNullCheck()) {
         EntityPlayer player = event.getEntity();
         if (player != null) {
            if ((Boolean)this.self.getValue() || !player.equals(mc.player)) {
               String name = player.getName();
               int pops = Vonware.totemPopManager.getPopCount(name);
               if (pops != 0) {
                  if (this.selfType.getValue() == PopCounter.SelfType.You && player.equals(mc.player)) {
                     if (!this.deathMessageSent) {
                        Command.sendSilentMessage(this.getYouDeathMessage(pops));
                        this.deathMessageSent = true;
                     }
                  } else {
                     Command.sendSilentMessage(this.getDeathMessage(name, pops));
                  }

                  this.lastPopMessageIds.remove(name);
                  this.lastPopTick.remove(name);
                  this.deathMessageSent = false;
               }
            }
         }
      }
   }

   @SubscribeEvent
   public void onPlayerLeave(ConnectionEvent.Leave event) {
      if ((Boolean)this.clearOnLogout.getValue()) {
         this.lastPopMessageIds.clear();
         this.lastPopTick.clear();
         this.deathMessageSent = false;
      }

   }

   @SubscribeEvent
   public void onVisualRangeLeave(EntityWorldEvent.Remove event) {
      if ((Boolean)this.clearOnVisRange.getValue() && event.getEntity() instanceof EntityPlayer) {
         String name = event.getEntity().getName();
         this.lastPopMessageIds.remove(name);
         this.lastPopTick.remove(name);
      }

   }

   private String getPopMessage(String name, int pops) {
      String unit = pops == 1 ? "Totem!" : "Totems!";
      return Manager.getInstance().notifier.getValue() == Manager.Notifier.Iggyware ? TextFormatting.BLACK + "<Iggyware> " + TextFormatting.WHITE + name + " has popped " + TextFormatting.GREEN + pops + " " + TextFormatting.WHITE + (pops == 1 ? "time" : "times") + " in total!" : Global.getMysticName() + this.mystic() + name + TextFormatting.WHITE + " has popped " + this.mystic() + pops + " " + TextFormatting.WHITE + unit;
   }

   private String getYouMessage(int pops) {
      return Manager.getInstance().notifier.getValue() == Manager.Notifier.Iggyware ? TextFormatting.BLACK + "<Iggyware> " + TextFormatting.BLUE + "You " + TextFormatting.WHITE + "have popped " + TextFormatting.GREEN + pops + " " + TextFormatting.WHITE + (pops == 1 ? "time" : "times") + " in total!" : Global.getMysticName() + this.mystic() + "You " + TextFormatting.WHITE + "have popped " + this.mystic() + pops + " " + TextFormatting.WHITE + (pops == 1 ? "Totem!" : "Totems!");
   }

   private String getDeathMessage(String name, int pops) {
      return Manager.getInstance().notifier.getValue() == Manager.Notifier.Iggyware ? TextFormatting.BLACK + "<Iggyware> " + TextFormatting.WHITE + name + " died after popping " + TextFormatting.GREEN + pops + " " + TextFormatting.WHITE + (pops == 1 ? "Totem!" : "Totems!") : Global.getMysticName() + TextFormatting.RED + name + " died after popping " + TextFormatting.GREEN + pops + " " + TextFormatting.RED + (pops == 1 ? "Totem!" : "Totems!");
   }

   private String getYouDeathMessage(int pops) {
      return Manager.getInstance().notifier.getValue() == Manager.Notifier.Iggyware ? TextFormatting.BLACK + "<Iggyware> " + TextFormatting.BLUE + "You " + TextFormatting.WHITE + "died after popping " + TextFormatting.GREEN + pops + " " + TextFormatting.WHITE + (pops == 1 ? "Totem!" : "Totems!") : Global.getMysticName() + TextFormatting.RED + "You died after popping " + TextFormatting.GREEN + pops + " " + TextFormatting.RED + (pops == 1 ? "Totem!" : "Totems!");
   }

   private String mystic() {
      return HexColorText.color(Global.getGlobalColor().getRGB());
   }

   public static enum SelfType {
      You,
      Name;
   }
}
