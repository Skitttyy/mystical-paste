package fat.vonware.util;

import com.mojang.realmsclient.gui.ChatFormatting;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.util.text.TextFormatting;

public class TextUtil {
   public static final String BLACK;
   public static final String DARK_BLUE;
   public static final String DARK_GREEN;
   public static final String DARK_AQUA;
   public static final String DARK_RED;
   public static final String DARK_PURPLE;
   public static final String GOLD;
   public static final String GRAY;
   public static final String DARK_GRAY;
   public static final String BLUE;
   public static final String GREEN;
   public static final String AQUA;
   public static final String RED;
   public static final String LIGHT_PURPLE;
   public static final String YELLOW;
   public static final String WHITE;
   public static final String OBFUSCATED;
   public static final String BOLD;
   public static final String STRIKE;
   public static final String UNDERLINE;
   public static final String ITALIC;
   public static final String RESET;
   public static final Pattern PATTERN;

   public static String capitalize(String str) {
      return str.substring(0, 1).toUpperCase() + str.substring(1);
   }

   public static String getFixedName(String i) {
      return i.charAt(0) + i.toLowerCase().replaceFirst(Character.toString(i.charAt(0)).toLowerCase(), "");
   }

   public static String findMatch(String string) {
      Matcher matcher = PATTERN.matcher(string);

      String s;
      for(s = TextFormatting.WHITE.toString(); matcher.find(); s = matcher.group()) {
      }

      return s;
   }

   static {
      BLACK = String.valueOf(ChatFormatting.BLACK);
      DARK_BLUE = String.valueOf(ChatFormatting.DARK_BLUE);
      DARK_GREEN = String.valueOf(ChatFormatting.DARK_GREEN);
      DARK_AQUA = String.valueOf(ChatFormatting.DARK_AQUA);
      DARK_RED = String.valueOf(ChatFormatting.DARK_RED);
      DARK_PURPLE = String.valueOf(ChatFormatting.DARK_PURPLE);
      GOLD = String.valueOf(ChatFormatting.GOLD);
      GRAY = String.valueOf(ChatFormatting.GRAY);
      DARK_GRAY = String.valueOf(ChatFormatting.DARK_GRAY);
      BLUE = String.valueOf(ChatFormatting.BLUE);
      GREEN = String.valueOf(ChatFormatting.GREEN);
      AQUA = String.valueOf(ChatFormatting.AQUA);
      RED = String.valueOf(ChatFormatting.RED);
      LIGHT_PURPLE = String.valueOf(ChatFormatting.LIGHT_PURPLE);
      YELLOW = String.valueOf(ChatFormatting.YELLOW);
      WHITE = String.valueOf(ChatFormatting.WHITE);
      OBFUSCATED = String.valueOf(ChatFormatting.OBFUSCATED);
      BOLD = String.valueOf(ChatFormatting.BOLD);
      STRIKE = String.valueOf(ChatFormatting.STRIKETHROUGH);
      UNDERLINE = String.valueOf(ChatFormatting.UNDERLINE);
      ITALIC = String.valueOf(ChatFormatting.ITALIC);
      RESET = String.valueOf(ChatFormatting.RESET);
      PATTERN = Pattern.compile("(?i)\u00a7[0-9A-FK-OR]");
   }

   public static enum Color {
      NONE,
      WHITE,
      BLACK,
      DARK_BLUE,
      DARK_GREEN,
      DARK_AQUA,
      DARK_RED,
      DARK_PURPLE,
      GOLD,
      GRAY,
      DARK_GRAY,
      BLUE,
      GREEN,
      AQUA,
      RED,
      LIGHT_PURPLE,
      YELLOW;
   }
}
