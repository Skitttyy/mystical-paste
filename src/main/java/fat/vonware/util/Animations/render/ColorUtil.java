package fat.vonware.util.Animations.render;

import fat.vonware.Global;
import fat.vonware.features.gui.components.items.buttons.render.ColorPickerConfig;
import fat.vonware.features.gui.newgui.newcomponents.NewComponent;
import fat.vonware.features.modules.client.ClickGui;
import fat.vonware.features.modules.client.Colors;
import fat.vonware.features.setting.ColorSetting;
import fat.vonware.util.MathUtil;
import java.awt.Color;
import net.minecraft.util.math.MathHelper;

public class ColorUtil {
   public static Color rainbow(int delay) {
      Colors colors = Colors.getInstance();
      float speed = (Float)colors.rainbowSpeed.getValue();
      float hueOffset = (Float)colors.rainbowHue.getValue();
      float saturation = (float)(Integer)colors.rainbowSaturation.getValue();
      float lightness = (float)(Integer)colors.rainbowLightness.getValue();
      float hue = (float)(((double)System.currentTimeMillis() / (double)10.0F * (double)speed + (double)((float)delay * hueOffset)) % (double)360.0F);
      return hslToRgb((int)hue, saturation, lightness);
   }

   public static Color newAlpha(Color color, int alpha) {
      return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
   }

   public static int toRGBA(int r, int g, int b) {
      return toRGBA(r, g, b, 255);
   }

   public static int toRGBA(int r, int g, int b, int a) {
      return (r << 16) + (g << 8) + b + (a << 24);
   }

   public static Color changeAlpha(Color origColor, int alpha) {
      return new Color(origColor.getRed(), origColor.getGreen(), origColor.getBlue(), alpha);
   }

   public static double fade(double value, double max) {
      double fadeAmount = normalize((double)System.currentTimeMillis() - value, max);
      int alpha = (int)(fadeAmount * (double)255.0F);
      alpha = MathHelper.clamp(alpha, 0, 255);
      alpha = 255 - alpha;
      return (double)alpha;
   }

   private static double normalize(double value, double max) {
      return (value - (double)0.0F) / (max - (double)0.0F);
   }

   public static float[] toArray(int color) {
      return new float[]{(float)(color >> 16 & 255) / 255.0F, (float)(color >> 8 & 255) / 255.0F, (float)(color & 255) / 255.0F, (float)(color >> 24 & 255) / 255.0F};
   }

   public static int toRGBA(Color color) {
      return toRGBA(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
   }

   public static Color getResolvedColor(ColorSetting setting) {
      if (ColorPickerConfig.getState(setting).useGlobalColor) {
         Colors colors = Colors.getInstance();
         if ((Boolean)colors.rainbow.getValue()) {
            return colors.rainbowMode.getValue() == Colors.RainbowMode.Static ? rainbow(0) : rainbow((int)((float)NewComponent.counter1[0] * (Float)colors.rainbowHue.getValue()));
         } else {
            return (Boolean)ClickGui.getInstance().pulseGui.getValue() ? Global.getPulseColor(colors.getGuiColor(), 0) : colors.getCurrentColor();
         }
      } else {
         int colorInt = (Integer)setting.getValue();
         return new Color(colorInt >> 16 & 255, colorInt >> 8 & 255, colorInt & 255);
      }
   }

   public static Color lighter(Color color, double amount) {
      int red = (int)Math.round((double)color.getRed() * ((double)1.0F + amount));
      int green = (int)Math.round((double)color.getGreen() * ((double)1.0F + amount));
      int blue = (int)Math.round((double)color.getBlue() * ((double)1.0F + amount));
      if (red < 0) {
         red = 0;
      } else if (red > 255) {
         red = 255;
      }

      if (green < 0) {
         green = 0;
      } else if (green > 255) {
         green = 255;
      }

      if (blue < 0) {
         blue = 0;
      } else if (blue > 255) {
         blue = 255;
      }

      return new Color(red, green, blue, color.getAlpha());
   }

   public static Color hslToRgb(int h, float s, float l) {
      float hue = (float)h / 360.0F;
      float saturation = s / 100.0F;
      float lightness = l / 100.0F;
      float r;
      float g;
      float b;
      if (saturation == 0.0F) {
         b = lightness;
         g = lightness;
         r = lightness;
      } else {
         float q = lightness < 0.5F ? lightness * (1.0F + saturation) : lightness + saturation - lightness * saturation;
         float p = 2.0F * lightness - q;
         r = hueToRgb(p, q, hue + 0.33333334F);
         g = hueToRgb(p, q, hue);
         b = hueToRgb(p, q, hue - 0.33333334F);
      }

      return new Color((int)(r * 255.0F), (int)(g * 255.0F), (int)(b * 255.0F));
   }

   private static float hueToRgb(float p, float q, float t) {
      if (t < 0.0F) {
         ++t;
      }

      if (t > 1.0F) {
         --t;
      }

      if (t < 0.16666667F) {
         return p + (q - p) * 6.0F * t;
      } else if (t < 0.5F) {
         return q;
      } else {
         return t < 0.6666667F ? p + (q - p) * (0.6666667F - t) * 6.0F : p;
      }
   }

   public static float[] rgbToHsl(Color color) {
      float r = (float)color.getRed() / 255.0F;
      float g = (float)color.getGreen() / 255.0F;
      float b = (float)color.getBlue() / 255.0F;
      float max = Math.max(r, Math.max(g, b));
      float min = Math.min(r, Math.min(g, b));
      float l = (max + min) / 2.0F;
      float h;
      float s;
      if (max == min) {
         s = 0.0F;
         h = 0.0F;
      } else {
         float d = max - min;
         s = l > 0.5F ? d / (2.0F - max - min) : d / (max + min);
         if (max == r) {
            h = (g - b) / d + (float)(g < b ? 6 : 0);
         } else if (max == g) {
            h = (b - r) / d + 2.0F;
         } else {
            h = (r - g) / d + 4.0F;
         }

         h /= 6.0F;
      }

      return new float[]{h * 360.0F, s * 100.0F, l * 100.0F};
   }

   public static Color hsvToRgb(float h, float s, float v) {
      int rgb = Color.HSBtoRGB(h / 360.0F, s, v);
      return new Color(rgb);
   }

   public static float[] rgbToHsv(Color color) {
      float[] hsv = new float[3];
      Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), hsv);
      return new float[]{hsv[0] * 360.0F, hsv[1], hsv[2]};
   }

   public static Color lerpRedGreen(float t) {
      t = MathUtil.clamp(t, 0.0F, 1.0F);
      int r = (int)(255.0F * (1.0F - t));
      int g = (int)(255.0F * t);
      return new Color(r, g, 0);
   }

   public static Color lerpRedYellowGreen(float t) {
      t = MathUtil.clamp(t, 0.0F, 1.0F);
      int r;
      int g;
      if (t < 0.5F) {
         float local = t / 0.5F;
         r = 255;
         g = (int)(255.0F * local);
      } else {
         float local = (t - 0.5F) / 0.5F;
         r = (int)(255.0F * (1.0F - local));
         g = 255;
      }

      return new Color(r, g, 0);
   }
}
