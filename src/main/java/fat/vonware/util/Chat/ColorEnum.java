package fat.vonware.util.Chat;

import net.minecraft.util.text.TextFormatting;

public enum ColorEnum {
   NONE(""),
   BLACK(TextFormatting.BLACK.toString()),
   WHITE(TextFormatting.WHITE.toString()),
   DARK_BLUE(TextFormatting.DARK_BLUE.toString()),
   DARK_GREEN(TextFormatting.DARK_GREEN.toString()),
   DARK_AQUA(TextFormatting.DARK_AQUA.toString()),
   DARK_RED(TextFormatting.DARK_RED.toString()),
   DARK_PURPLE(TextFormatting.DARK_PURPLE.toString()),
   DARK_GRAY(TextFormatting.DARK_GRAY.toString()),
   GRAY(TextFormatting.GRAY.toString()),
   GOLD(TextFormatting.GOLD.toString()),
   BLUE(TextFormatting.BLUE.toString()),
   GREEN(TextFormatting.GREEN.toString()),
   AQUA(TextFormatting.AQUA.toString()),
   RED(TextFormatting.RED.toString()),
   LIGHT_PURPLE(TextFormatting.LIGHT_PURPLE.toString()),
   YELLOW(TextFormatting.YELLOW.toString());

   private final String color;

   private ColorEnum(String color) {
      this.color = color;
   }

   public String getColor() {
      return this.color;
   }
}
