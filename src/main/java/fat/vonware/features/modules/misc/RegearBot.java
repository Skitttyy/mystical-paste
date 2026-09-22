package fat.vonware.features.modules.misc;

import fat.vonware.Vonware;
import fat.vonware.features.modules.Module;
import fat.vonware.features.modules.movement.AutoWalk;
import fat.vonware.features.modules.movement.Step;
import fat.vonware.features.setting.Setting;
import fat.vonware.util.Chat.ThreadUtil;
import fat.vonware.util.Manager.NiggaUtil;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class RegearBot extends Module {
   private String requestingPlayer = null;
   private volatile boolean gotoSent = false;
   private volatile Thread pitThread;
   private volatile Thread arrivalThread;
   public Setting<Boolean> pitFactor = this.register(new Setting("PitFactor", false, "Factors in 2bpvp spawn area to drop into the pit below, temporarily using Step and AutoWalk."));

   public RegearBot() {
      super("RegearBot", "You are the regear bot if you enable this module", Module.Category.MISC, true, false, false);
   }

   public void onEnable() {
      this.requestingPlayer = null;
      this.gotoSent = false;
      if ((Boolean)this.pitFactor.getValue()) {
         this.handlePitFactor();
      }

   }

   public void onDisable() {
      this.requestingPlayer = null;
      this.gotoSent = false;
      interruptWorker(this.pitThread);
      interruptWorker(this.arrivalThread);
      this.pitThread = null;
      this.arrivalThread = null;
   }

   public void onUnload() {
      this.onDisable();
   }

   private static void interruptWorker(Thread worker) {
      if (worker != null) {
         worker.interrupt();
      }

   }

   private void handlePitFactor() {
      interruptWorker(this.pitThread);
      this.pitThread = ThreadUtil.startDaemon("RegearBot-Pit", () -> {
         try {
            mc.addScheduledTask(() -> {
               if (mc.player != null) {
                  mc.player.rotationYaw = 45.0F;
               }

            });
            Thread.sleep(100L);
            Step stepModule = (Step)Vonware.moduleManager.getModuleByClass(Step.class);
            if (stepModule != null && !stepModule.isEnabled()) {
               mc.addScheduledTask(() -> stepModule.enable());
            }

            AutoWalk autoWalkModule = (AutoWalk)Vonware.moduleManager.getModuleByClass(AutoWalk.class);
            if (autoWalkModule != null && !autoWalkModule.isEnabled()) {
               mc.addScheduledTask(() -> autoWalkModule.enable());
            }

            this.sendMessage("\u00a7a[RegearBot] \u00a7fPitFactor activated - Walking for 10 seconds");
            Thread.sleep(4500L);
            if (autoWalkModule != null && autoWalkModule.isEnabled()) {
               mc.addScheduledTask(() -> autoWalkModule.disable());
            }

            if (stepModule != null && stepModule.isEnabled()) {
               mc.addScheduledTask(() -> stepModule.disable());
            }

            this.sendMessage("\u00a7a[RegearBot] \u00a7fPitFactor completed");
         } catch (InterruptedException var3) {
            Thread.currentThread().interrupt();
         }

      });
   }

   @SubscribeEvent
   public void onChatReceived(ClientChatReceivedEvent event) {
      if (mc.player != null) {
         String message = event.getMessage().getUnformattedText();
         if (NiggaUtil.isWhisper(message)) {
            String playerName = this.extractPlayerNameFromWhisper(message);
            String content = this.extractMessageContent(message);
            if (content == null || playerName == null || playerName.isEmpty()) {
               return;
            }

            if (content.toLowerCase().contains("i need a regear")) {
               this.requestingPlayer = playerName;
               this.gotoSent = false;
               NiggaUtil.playNotificationSound();
               this.sendMessage("\u00a7a[RegearBot] \u00a7fReceived regear request from \u00a7b" + playerName);
            } else if (this.requestingPlayer != null && playerName.equals(this.requestingPlayer) && !this.gotoSent) {
               String coords = this.extractCoordinates(content);
               if (coords != null) {
                  mc.player.sendChatMessage("#goto " + coords);
                  this.gotoSent = true;
                  mc.player.sendChatMessage("/msg " + playerName + " On my way! Will /kill when I arrive.");
                  this.sendMessage("\u00a7a[RegearBot] \u00a7fGoing to coordinates: \u00a7e" + coords);
                  NiggaUtil.playNotificationSound();
                  interruptWorker(this.arrivalThread);
                  this.arrivalThread = ThreadUtil.startDaemon("RegearBot-Arrival", () -> {
                     try {
                        Thread.sleep(2000L);
                        this.checkArrival(coords, playerName);
                     } catch (InterruptedException var4) {
                        Thread.currentThread().interrupt();
                     }

                  });
               }
            }
         }

      }
   }

   private String extractPlayerNameFromWhisper(String message) {
      if (message.toLowerCase().contains("whispers:")) {
         String beforeWhispers = message.substring(0, message.toLowerCase().indexOf("whispers:"));
         String cleaned = beforeWhispers.replaceAll("\u00a7.", "").replaceAll("[0-9<>rf ]", "").trim();
         if (!cleaned.isEmpty()) {
            return cleaned;
         }
      }

      if (message.contains("[") && message.contains("->") && message.contains("me")) {
         int start = message.indexOf("[") + 1;
         int end = message.indexOf("->", start);
         if (end > start) {
            String name = message.substring(start, end).trim();
            name = name.replaceAll("\u00a7.", "").replaceAll("[0-9<>rf ]", "").trim();
            if (!name.isEmpty()) {
               return name;
            }
         }
      }

      String fallback = NiggaUtil.extractPlayerName(message);
      return fallback != null && !fallback.isEmpty() ? fallback.replaceAll("\u00a7.", "").replaceAll("[0-9<>rf ]", "").trim() : null;
   }

   private String extractMessageContent(String message) {
      if (message.toLowerCase().contains("whispers:")) {
         String[] parts = message.split("whispers:", 2);
         if (parts.length > 1) {
            return parts[1].trim();
         }
      }

      if (message.contains("]")) {
         int index = message.lastIndexOf("]");
         if (index != -1 && index < message.length() - 1) {
            return message.substring(index + 1).trim();
         }
      }

      if (message.toLowerCase().contains("whispers to you:")) {
         String[] parts = message.split("whispers to you:", 2);
         if (parts.length > 1) {
            return parts[1].trim();
         }
      }

      if (message.contains(":")) {
         String[] parts = message.split(":", 2);
         if (parts.length > 1) {
            return parts[1].trim();
         }
      }

      return message;
   }

   private String extractCoordinates(String message) {
      String cleaned = message.replaceAll("[(),]", " ").trim();
      String[] parts = cleaned.split("\\s+");
      int numbersFound = 0;
      StringBuilder coords = new StringBuilder();

      for(String part : parts) {
         try {
            Double.parseDouble(part);
            if (numbersFound > 0) {
               coords.append(" ");
            }

            coords.append(part);
            ++numbersFound;
            if (numbersFound == 3) {
               return coords.toString();
            }
         } catch (NumberFormatException var11) {
         }
      }

      return null;
   }

   private void checkArrival(String targetCoords, String playerName) {
      String[] coords = targetCoords.split(" ");
      if (coords.length == 3) {
         try {
            double targetX = Double.parseDouble(coords[0]);
            double targetY = Double.parseDouble(coords[1]);
            double targetZ = Double.parseDouble(coords[2]);

            while(this.gotoSent && mc.player != null) {
               double distX = Math.abs(mc.player.posX - targetX);
               double distY = Math.abs(mc.player.posY - targetY);
               double distZ = Math.abs(mc.player.posZ - targetZ);
               if (distX <= (double)3.0F && distY <= (double)3.0F && distZ <= (double)3.0F) {
                  mc.addScheduledTask(() -> {
                     mc.player.sendChatMessage("#stop");
                     mc.player.sendChatMessage("/kill");
                     mc.player.sendChatMessage("/msg " + playerName + " Arrived and killed myself. Pick up the gear!");
                     this.sendMessage("\u00a7a[RegearBot] \u00a7fArrived at destination and killed!");
                     NiggaUtil.playNotificationSound();
                     this.requestingPlayer = null;
                     this.gotoSent = false;
                  });
                  if ((Boolean)this.pitFactor.getValue()) {
                     try {
                        Thread.sleep(3000L);
                        mc.addScheduledTask(() -> this.handlePitFactor());
                     } catch (InterruptedException var17) {
                        Thread.currentThread().interrupt();
                     }
                  }
                  break;
               }

               Thread.sleep(1000L);
            }
         } catch (Exception e) {
            e.printStackTrace();
         }

      }
   }

   private void sendMessage(String message) {
      if (mc.player != null) {
         mc.player.sendMessage(new TextComponentString(message));
      }

   }
}
