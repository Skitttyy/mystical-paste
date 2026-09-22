package fat.vonware.util.Manager;

import fat.vonware.util.Util;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.init.SoundEvents;

public class NiggaUtil implements Util {
   public static boolean isWhisper(String unformatted) {
      String lower = unformatted.toLowerCase();
      if (!unformatted.matches(".*\\[.*->.*me.*\\].*") && !unformatted.matches(".*\\[.*whispers.*\\].*")) {
         if (!lower.contains("whispers to you") && !lower.contains("whispers:")) {
            if (lower.startsWith("from ") && lower.contains(":")) {
               return true;
            } else {
               return unformatted.matches("^\\*.*\\*.*");
            }
         } else {
            return true;
         }
      } else {
         return true;
      }
   }

   public static String extractPlayerName(String message) {
      if (message.contains("[") && message.contains("]")) {
         int start = message.indexOf("[") + 1;
         int end = message.indexOf("->", start);
         if (end == -1) {
            end = message.indexOf("whispers", start);
         }

         if (end == -1) {
            end = message.indexOf("]", start);
         }

         if (end > start) {
            return message.substring(start, end).trim();
         }
      }

      if (message.toLowerCase().contains("whispers to you")) {
         String[] parts = message.split("whispers to you");
         if (parts.length > 0) {
            return parts[0].trim();
         }
      }

      if (message.toLowerCase().contains("whispers:")) {
         String[] parts = message.split("whispers:");
         if (parts.length > 0) {
            return parts[0].trim();
         }
      }

      if (message.toLowerCase().startsWith("from ")) {
         String[] parts = message.substring(5).split(":");
         if (parts.length > 0) {
            return parts[0].trim();
         }
      }

      if (message.startsWith("*") && message.indexOf("*", 1) != -1) {
         int end = message.indexOf("*", 1);
         return message.substring(1, end).trim();
      } else {
         return "";
      }
   }

   public static void playNotificationSound() {
      if (mc.player != null) {
         mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 5.0F));
      }

   }
}
