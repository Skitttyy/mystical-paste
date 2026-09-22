package fat.vonware.util;

import java.util.Locale;

public final class HexColorText {
   public static final char FORMAT = '\u00a7';
   public static final char HEX_MARKER = '#';
   public static final int TOKEN_LENGTH = 8;

   private HexColorText() {
   }

   public static String color(int argb) {
      return String.format(Locale.ROOT, "\u00a7#%06X", argb & 16777215);
   }

   public static boolean hasHexColor(String text) {
      if (text != null && text.length() >= 8) {
         for(int i = 0; i <= text.length() - 8; ++i) {
            if (isHexToken(text, i)) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   public static boolean isHexToken(String text, int index) {
      if (text != null && index >= 0 && index + 8 <= text.length()) {
         if (text.charAt(index) == 167 && text.charAt(index + 1) == '#') {
            for(int i = index + 2; i < index + 8; ++i) {
               if (Character.digit(text.charAt(i), 16) < 0) {
                  return false;
               }
            }

            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public static int parseRgb(String text, int index) {
      return Integer.parseInt(text.substring(index + 2, index + 8), 16);
   }

   public static String strip(String text) {
      if (!hasHexColor(text)) {
         return text;
      } else {
         StringBuilder out = new StringBuilder(text.length());
         int i = 0;

         while(i < text.length()) {
            if (isHexToken(text, i)) {
               i += 8;
            } else {
               out.append(text.charAt(i++));
            }
         }

         return out.toString();
      }
   }
}
