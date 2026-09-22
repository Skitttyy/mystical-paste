package fat.vonware.features.modules.render;

import fat.vonware.Global;
import fat.vonware.features.gui.components.items.buttons.render.ColorPickerConfig;
import fat.vonware.features.gui.newgui.newcomponents.NewComponent;
import fat.vonware.features.modules.Module;
import fat.vonware.features.modules.client.ClickGui;
import fat.vonware.features.modules.client.Colors;
import fat.vonware.features.setting.ColorSetting;
import fat.vonware.features.setting.Setting;
import fat.vonware.util.Animations.render.ColorUtil;
import java.awt.Color;

public class GlintModify extends Module {
   public Setting<Boolean> armor = new Setting<Boolean>("Armor", true);
   public final ColorSetting glintColor = new ColorSetting("GlintColor", 255, false);
   public Setting<Float> glintScale = new Setting<Float>("GlintScale", 8.0F, 0.0F, 20.0F);
   public Setting<Float> glintMult = new Setting<Float>("GlintMultiplier", 1.0F, 0.1F, 10.0F);
   public Setting<Float> glintRotate = new Setting<Float>("GlintRotate", 1.0F, 0.1F, 10.0F);

   public GlintModify() {
      super("GlintModify", "Modifies glint", Module.Category.RENDER, true, false, false);
      this.register(this.armor);
      this.register(this.glintScale);
      this.register(this.glintMult);
      this.register(this.glintRotate);
      this.register(this.glintColor);
   }

   public Color getEnchantColor() {
      if (ColorPickerConfig.getState(this.glintColor, false).useGlobalColor) {
         Colors colors = Colors.getInstance();
         if ((Boolean)colors.rainbow.getValue()) {
            return colors.rainbowMode.getValue() == Colors.RainbowMode.Static ? ColorUtil.rainbow(0) : ColorUtil.rainbow((int)((float)NewComponent.counter1[0] * (Float)colors.rainbowHue.getValue()));
         } else {
            return (Boolean)ClickGui.getInstance().pulseGui.getValue() ? Global.getPulseColor(colors.getGuiColor(), 0) : colors.getCurrentColor();
         }
      } else {
         int colorInt = (Integer)this.glintColor.getValue();
         return new Color(colorInt >> 16 & 255, colorInt >> 8 & 255, colorInt & 255);
      }
   }

   public boolean isArmorGlint() {
      return this.isEnabled() && (Boolean)this.armor.getValue();
   }

   public float getGlintScale() {
      return (Float)this.glintScale.getValue();
   }

   public float getFactor() {
      return (Float)this.glintMult.getValue();
   }

   public float getGlintRotate() {
      return (Float)this.glintRotate.getValue();
   }
}
