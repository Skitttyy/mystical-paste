package fat.vonware.features.modules.client;

import fat.vonware.Vonware;
import fat.vonware.event.events.ClientEvent;
import fat.vonware.features.command.Command;
import fat.vonware.features.modules.Module;
import fat.vonware.features.setting.ColorSetting;
import fat.vonware.features.setting.Setting;
import fat.vonware.manager.FutureManager;
import fat.vonware.util.Animations.render.ColorUtil;
import java.awt.Color;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class Colors extends Module {
   private static Colors INSTANCE = new Colors();
   public Setting<ColorSystem> colorSystem;
   public Setting<Integer> red;
   public Setting<Integer> green;
   public Setting<Integer> blue;
   public Setting<Integer> hue;
   public Setting<Float> saturation;
   public Setting<Float> lightness;
   public Setting<Float> hsvHue;
   public Setting<Float> hsvSaturation;
   public Setting<Float> hsvValue;
   public Setting<Boolean> rainbow;
   public Setting<RainbowMode> rainbowMode;
   public Setting<Integer> rainbowSaturation;
   public Setting<Integer> rainbowLightness;
   public Setting<Float> rainbowSpeed;
   public Setting<Float> rainbowHue;
   public Setting<Boolean> futureSync;
   public Setting<Boolean> futureFriendSync;
   public Setting<String> futurePrefix;
   public final ColorSetting friendColor;
   private Color lastSyncedColor;
   private boolean colorChangedSinceLastSync;

   public Colors() {
      super("Colors", "Handles the GUI and module colors (This Module cannot be disabled.)", Module.Category.CLIENT, true, false, true);
      this.colorSystem = new Setting<ColorSystem>("ColorSystem", Colors.ColorSystem.Picker);
      this.red = new Setting<Integer>("Red", 139, 0, 255, (v) -> this.colorSystem.getValue() == Colors.ColorSystem.RGB);
      this.green = new Setting<Integer>("Green", 185, 0, 255, (v) -> this.colorSystem.getValue() == Colors.ColorSystem.RGB);
      this.blue = new Setting<Integer>("Blue", 255, 0, 255, (v) -> this.colorSystem.getValue() == Colors.ColorSystem.RGB);
      this.hue = new Setting<Integer>("Hue", 290, 0, 360, (v) -> this.colorSystem.getValue() == Colors.ColorSystem.HSL);
      this.saturation = new Setting<Float>("Saturation", 100.0F, 0.0F, 100.0F, (v) -> this.colorSystem.getValue() == Colors.ColorSystem.HSL);
      this.lightness = new Setting<Float>("Lightness", 89.0F, 0.0F, 100.0F, (v) -> this.colorSystem.getValue() == Colors.ColorSystem.HSL);
      this.hsvHue = new Setting<Float>("HSV-Hue", 240.0F, 0.0F, 360.0F, (v) -> false);
      this.hsvSaturation = new Setting<Float>("HSV-Sat", 0.6F, 0.0F, 1.0F, (v) -> false);
      this.hsvValue = new Setting<Float>("HSV-Val", 0.9F, 0.0F, 1.0F, (v) -> false);
      this.rainbow = new Setting<Boolean>("Rainbow", true);
      this.rainbowMode = new Setting<RainbowMode>("RainbowMode", Colors.RainbowMode.Flow, (v) -> (Boolean)this.rainbow.getValue());
      this.rainbowSaturation = new Setting<Integer>("R-Saturation", 100, 0, 100, (v) -> (Boolean)this.rainbow.getValue());
      this.rainbowLightness = new Setting<Integer>("R-Lightness", 35, 0, 100, (v) -> (Boolean)this.rainbow.getValue());
      this.rainbowSpeed = new Setting<Float>("RainbowSpeed", 5.0F, 0.0F, 15.0F, (v) -> (Boolean)this.rainbow.getValue());
      this.rainbowHue = new Setting<Float>("RainbowHue", 4.0F, 0.0F, 10.0F, (v) -> (Boolean)this.rainbow.getValue());
      this.futureSync = new Setting<Boolean>("FutureColorSync", false);
      this.futureFriendSync = new Setting<Boolean>("FutureFriendSync", false);
      this.futurePrefix = new Setting<String>("FuturePrefix", ".", (v) -> (Boolean)this.futureSync.getValue() || (Boolean)this.futureFriendSync.getValue());
      this.friendColor = new ColorSetting("FriendColor", -11149569, false);
      this.lastSyncedColor = null;
      this.colorChangedSinceLastSync = false;
      this.register(this.colorSystem);
      this.register(this.red);
      this.register(this.green);
      this.register(this.blue);
      this.register(this.hue);
      this.register(this.saturation);
      this.register(this.lightness);
      this.register(this.hsvHue);
      this.register(this.hsvSaturation);
      this.register(this.hsvValue);
      this.register(this.rainbow);
      this.register(this.rainbowMode);
      this.register(this.rainbowSaturation);
      this.register(this.rainbowLightness);
      this.register(this.rainbowSpeed);
      this.register(this.rainbowHue);
      this.register(this.futureSync);
      this.register(this.futureFriendSync);
      this.register(this.futurePrefix);
      this.register(this.friendColor);
      this.enabled.setValue(true);
      MinecraftForge.EVENT_BUS.register(this);
      this.setInstance();
   }

   public void syncToFuture() {
      if ((Boolean)this.futureSync.getValue() && FutureManager.doesFutureExist() && mc.player != null) {
         Color currentColor = this.getCurrentColor();
         if (this.lastSyncedColor == null || this.lastSyncedColor.getRed() != currentColor.getRed() || this.lastSyncedColor.getGreen() != currentColor.getGreen() || this.lastSyncedColor.getBlue() != currentColor.getBlue() || this.colorChangedSinceLastSync) {
            float hueValue;
            float saturationValue;
            float lightnessValue;
            if (this.colorSystem.getValue() == Colors.ColorSystem.HSL) {
               hueValue = (float)(Integer)this.hue.getValue();
               saturationValue = (Float)this.saturation.getValue();
               lightnessValue = (Float)this.lightness.getValue();
            } else {
               float[] hsl = ColorUtil.rgbToHsl(currentColor);
               hueValue = (float)Math.round(hsl[0]);
               saturationValue = (float)Math.round(hsl[1]);
               lightnessValue = (float)Math.round(hsl[2]);
            }

            String prefix = this.futurePrefix.getValue();
            mc.player.sendChatMessage(prefix + "colors hue " + (int)hueValue);
            mc.player.sendChatMessage(prefix + "colors saturation " + saturationValue);
            mc.player.sendChatMessage(prefix + "colors lightness " + lightnessValue);
            this.lastSyncedColor = new Color(currentColor.getRed(), currentColor.getGreen(), currentColor.getBlue());
            this.colorChangedSinceLastSync = false;
            Command.sendSilentMessage("\u00a77Colors synced to Future client!");
         }
      }
   }

   @SubscribeEvent
   public void onSettingChange(ClientEvent event) {
      if (event.getStage() == 0 && event.getSetting() != null) {
         if (event.getSetting().equals(this.colorSystem)) {
            this.syncColorValues();
         }

         if (event.getSetting().equals(this.colorSystem) || event.getSetting().equals(this.red) || event.getSetting().equals(this.green) || event.getSetting().equals(this.blue) || event.getSetting().equals(this.hue) || event.getSetting().equals(this.saturation) || event.getSetting().equals(this.lightness) || event.getSetting().equals(this.hsvHue) || event.getSetting().equals(this.hsvSaturation) || event.getSetting().equals(this.hsvValue)) {
            Color finalColor = this.getCurrentColor();
            Vonware.colorManager.setColor(finalColor.getRed(), finalColor.getGreen(), finalColor.getBlue(), 160);
            this.colorChangedSinceLastSync = true;
            if ((Boolean)this.futureSync.getValue()) {
               this.syncToFuture();
            }
         }
      }

   }

   private void syncColorValues() {
      ColorSystem currentSystem = this.colorSystem.getValue();
      if (currentSystem == Colors.ColorSystem.HSL) {
         Color currentColor = this.getCurrentColorBeforeSwitch();
         float[] hslValues = ColorUtil.rgbToHsl(currentColor);
         this.hue.setValue((int)hslValues[0]);
         this.saturation.setValue(hslValues[1]);
         this.lightness.setValue(hslValues[2]);
      } else if (currentSystem == Colors.ColorSystem.RGB) {
         Color currentColor = this.getCurrentColorBeforeSwitch();
         this.red.setValue(currentColor.getRed());
         this.green.setValue(currentColor.getGreen());
         this.blue.setValue(currentColor.getBlue());
      } else if (currentSystem == Colors.ColorSystem.Picker) {
         Color currentColor = this.getCurrentColorBeforeSwitch();
         float[] hsvValues = ColorUtil.rgbToHsv(currentColor);
         this.hsvHue.setValue(hsvValues[0]);
         this.hsvSaturation.setValue(hsvValues[1]);
         this.hsvValue.setValue(hsvValues[2]);
      }

   }

   private Color getCurrentColorBeforeSwitch() {
      if ((Integer)this.red.getValue() == 0 && (Integer)this.green.getValue() == 0 && (Integer)this.blue.getValue() == 0) {
         return (Integer)this.hue.getValue() == 0 && (Float)this.saturation.getValue() == 0.0F && (Float)this.lightness.getValue() == 0.0F ? ColorUtil.hsvToRgb((Float)this.hsvHue.getValue(), (Float)this.hsvSaturation.getValue(), (Float)this.hsvValue.getValue()) : ColorUtil.hslToRgb((Integer)this.hue.getValue(), (Float)this.saturation.getValue(), (Float)this.lightness.getValue());
      } else {
         return new Color((Integer)this.red.getValue(), (Integer)this.green.getValue(), (Integer)this.blue.getValue());
      }
   }

   public void onLoad() {
      Color finalColor = this.getCurrentColor();
      Vonware.colorManager.setColor(finalColor.getRed(), finalColor.getGreen(), finalColor.getBlue(), 160);
      this.lastSyncedColor = new Color(finalColor.getRed(), finalColor.getGreen(), finalColor.getBlue());
   }

   public Color getCurrentColor() {
      switch ((ColorSystem)this.colorSystem.getValue()) {
         case RGB:
            return new Color((Integer)this.red.getValue(), (Integer)this.green.getValue(), (Integer)this.blue.getValue());
         case HSL:
            return ColorUtil.hslToRgb((Integer)this.hue.getValue(), (Float)this.saturation.getValue(), (Float)this.lightness.getValue());
         case Picker:
            return ColorUtil.hsvToRgb((Float)this.hsvHue.getValue(), (Float)this.hsvSaturation.getValue(), (Float)this.hsvValue.getValue());
         default:
            return new Color((Integer)this.red.getValue(), (Integer)this.green.getValue(), (Integer)this.blue.getValue());
      }
   }

   public Color getFriendColor() {
      return ColorUtil.getResolvedColor(this.friendColor);
   }

   public int getFriendColorARGB() {
      Color c = this.getFriendColor();
      return -16777216 | c.getRed() << 16 | c.getGreen() << 8 | c.getBlue();
   }

   public Color getCurrentColorRgb() {
      return this.getCurrentColor();
   }

   public static Colors getInstance() {
      if (INSTANCE == null) {
         INSTANCE = new Colors();
      }

      return INSTANCE;
   }

   private void setInstance() {
      INSTANCE = this;
   }

   public Color getGuiColor() {
      Color currentColor = this.getCurrentColor();
      return new Color(currentColor.getRed(), currentColor.getGreen(), currentColor.getBlue(), 160);
   }

   public static enum RainbowMode {
      Static,
      Flow;
   }

   public static enum ColorSystem {
      RGB,
      HSL,
      Picker;
   }
}
