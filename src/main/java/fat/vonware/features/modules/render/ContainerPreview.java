package fat.vonware.features.modules.render;

import fat.vonware.Global;
import fat.vonware.features.gui.components.items.buttons.render.ColorPickerConfig;
import fat.vonware.features.modules.Module;
import fat.vonware.features.setting.ColorSetting;
import fat.vonware.features.setting.Setting;
import java.awt.Color;

public class ContainerPreview extends Module {
   public static ContainerPreview INSTANCE;
   public Setting<Boolean> shulkerPreview = new Setting<Boolean>("ShulkerPreview", true);
   public Setting<Boolean> mapPreview = new Setting<Boolean>("MapPreview", true);
   public Setting<Integer> alpha = new Setting<Integer>("Alpha", 70, 0, 255);
   private final ColorSetting color = new ColorSetting("Color", 2009087, false);

   public ContainerPreview() {
      super("ContainerPreview", "Shows a container preview on shulkers & maps", Module.Category.RENDER, true, false, false);
      this.register(this.shulkerPreview);
      this.register(this.mapPreview);
      this.register(this.alpha);
      this.register(this.color);
      INSTANCE = this;
   }

   public Color getColor() {
      if (ColorPickerConfig.getState(this.color).useGlobalColor) {
         return new Color(Global.getRed(), Global.getGreen(), Global.getBlue(), (Integer)this.alpha.getValue());
      } else {
         int colorInt = (Integer)this.color.getValue();
         return new Color(colorInt >> 16 & 255, colorInt >> 8 & 255, colorInt & 255, (Integer)this.alpha.getValue());
      }
   }
}
