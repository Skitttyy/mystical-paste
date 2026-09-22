package fat.vonware.features.setting;

import com.google.common.base.Predicate;

public class ColorSetting extends Setting<Integer> {
   private boolean defaultGlobal;

   public ColorSetting(String name, int defaultColor) {
      super(name, defaultColor);
      this.setColorPicker(true);
      this.defaultGlobal = true;
   }

   public ColorSetting(String name, int defaultColor, boolean defaultGlobal) {
      super(name, defaultColor);
      this.setColorPicker(true);
      this.colorPickerDefaultGlobal = defaultGlobal;
      this.defaultGlobal = defaultGlobal;
   }

   public ColorSetting(String name, int defaultColor, String description) {
      super(name, defaultColor, description);
      this.setColorPicker(true);
      this.defaultGlobal = true;
   }

   public ColorSetting(String name, int defaultColor, boolean defaultGlobal, String description) {
      super(name, defaultColor, description);
      this.setColorPicker(true);
      this.colorPickerDefaultGlobal = defaultGlobal;
      this.defaultGlobal = defaultGlobal;
   }

   public ColorSetting(String name, int defaultColor, boolean defaultGlobal, Predicate<Integer> visibility) {
      super(name, defaultColor, (Integer value) -> visibility.apply(value));
      this.setColorPicker(true);
      this.colorPickerDefaultGlobal = defaultGlobal;
      this.defaultGlobal = defaultGlobal;
   }

   public ColorSetting(String name, int defaultColor, boolean defaultGlobal, Predicate<Integer> visibility, String description) {
      super(name, defaultColor, (Integer value) -> visibility.apply(value), description);
      this.setColorPicker(true);
      this.colorPickerDefaultGlobal = defaultGlobal;
      this.defaultGlobal = defaultGlobal;
   }

   public boolean isDefaultGlobal() {
      return this.defaultGlobal;
   }
}
