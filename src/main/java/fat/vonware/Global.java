package fat.vonware;

import com.mojang.realmsclient.gui.ChatFormatting;
import fat.vonware.features.modules.client.ClickGui;
import fat.vonware.features.modules.client.Colors;
import fat.vonware.features.modules.client.Manager;
import fat.vonware.util.HexColorText;
import fat.vonware.util.Animations.render.ColorUtil;
import java.awt.Color;

public class Global {
   private static long pulseProgress = 0L;
   private static final long START_TIME = System.currentTimeMillis();
   public static final String CLIENT_VERSION = "v1.2.1";
   public static final String MYSTIC_NAME;

   public static String clientVersion() {
      return "v1.2.1";
   }

   public static String getMysticName() {
      try {
         return ChatFormatting.WHITE + "[" + HexColorText.color(getGlobalColor().getRGB()) + "Mystic" + ChatFormatting.WHITE + "] ";
      } catch (Exception var1) {
         return MYSTIC_NAME;
      }
   }

   public static String getGlobalColorCode() {
      return HexColorText.color(getGlobalColor().getRGB());
   }

   public static String getClientName() {
      try {
         Manager manager = Manager.getInstance();
         if (manager != null && manager.notifier.getValue() == Manager.Notifier.Iggyware) {
            return ChatFormatting.BLACK + "<Iggyware> ";
         }
      } catch (Exception var1) {
      }

      return getMysticName();
   }

   public static int getRed() {
      return Colors.getInstance().getCurrentColorRgb().getRed();
   }

   public static int getGreen() {
      return Colors.getInstance().getCurrentColorRgb().getGreen();
   }

   public static int getBlue() {
      return Colors.getInstance().getCurrentColorRgb().getBlue();
   }

   public static Color getGlobalColor() {
      return Colors.getInstance().getCurrentColorRgb();
   }

   public static Color getGlobalColorRainbowS() {
      Colors colors = Colors.getInstance();
      return (Boolean)colors.rainbow.getValue() ? ColorUtil.rainbow(0) : colors.getCurrentColorRgb();
   }

   public static Color getGlobalColorRainbowF(int delay) {
      Colors colors = Colors.getInstance();
      return (Boolean)colors.rainbow.getValue() ? ColorUtil.rainbow(delay) : colors.getCurrentColorRgb();
   }

   public static Color getGlobalColorRainbow(int delay) {
      Colors colors = Colors.getInstance();
      if ((Boolean)colors.rainbow.getValue()) {
         return colors.rainbowMode.getValue() == Colors.RainbowMode.Flow ? ColorUtil.rainbow(delay) : ColorUtil.rainbow(0);
      } else {
         return colors.getCurrentColorRgb();
      }
   }

   public static void updatePulseProgress() {
      pulseProgress += (long)(Integer)ClickGui.getInstance().pulseSpeed.getValue();
   }

   public static Color getPulseColor(Color base, int index) {
      ClickGui clickGui = ClickGui.getInstance();
      float speed = (float)(Integer)clickGui.pulseSpeed.getValue();
      long period = (long)(1000.0F * speed);
      long time = System.currentTimeMillis() - START_TIME;
      float offset = (float)index * 0.15F;
      float phase = (float)(time % period) / (float)period + offset;
      float pulse = (float)((Math.sin((double)phase * Math.PI * (double)2.0F) + (double)1.0F) * (double)0.5F);
      float[] hsb = new float[3];
      Color.RGBtoHSB(base.getRed(), base.getGreen(), base.getBlue(), hsb);
      float minBrightness = 0.55F;
      float maxBrightness = 1.0F;
      hsb[2] = minBrightness + (maxBrightness - minBrightness) * pulse;
      return new Color(Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]));
   }

   static {
      MYSTIC_NAME = ChatFormatting.WHITE + "[" + ChatFormatting.AQUA + "Mystic" + ChatFormatting.WHITE + "] ";
   }
}
