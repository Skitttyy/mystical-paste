package fat.vonware.util.HUD.HudElements;

import net.minecraft.util.text.TextFormatting;

public class TRUtil {
   public static String getDistanceColor(float distance) {
      if (distance >= 25.0F) {
         return TextFormatting.GREEN.toString();
      } else {
         return distance > 15.0F ? TextFormatting.YELLOW.toString() : TextFormatting.RED.toString();
      }
   }

   public static String getHealthColor(int health) {
      if (health >= 20) {
         return TextFormatting.GREEN.toString();
      } else if (health >= 10) {
         return TextFormatting.YELLOW.toString();
      } else {
         return health >= 5 ? TextFormatting.GOLD.toString() : TextFormatting.RED.toString();
      }
   }
}
