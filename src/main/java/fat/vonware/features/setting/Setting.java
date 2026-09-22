package fat.vonware.features.setting;

import fat.vonware.event.events.ClientEvent;
import fat.vonware.features.Feature;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraftforge.common.MinecraftForge;

public class Setting<T> {
   private final String name;
   private final T defaultValue;
   private T value;
   private T plannedValue;
   private T min;
   private T max;
   private boolean hasRestriction;
   private Predicate<T> visibility;
   private String description;
   private Function<T, String> descriptionProvider;
   private Feature feature;
   private boolean isColorPicker;
   public boolean colorPickerDefaultGlobal;
   private T step;

   public Setting(String name, T defaultValue) {
      this.isColorPicker = false;
      this.colorPickerDefaultGlobal = true;
      this.step = null;
      this.name = name;
      this.defaultValue = defaultValue;
      this.value = defaultValue;
      this.plannedValue = defaultValue;
      this.description = "";
   }

   public Setting(String name, T defaultValue, String description) {
      this.isColorPicker = false;
      this.colorPickerDefaultGlobal = true;
      this.step = null;
      this.name = name;
      this.defaultValue = defaultValue;
      this.value = defaultValue;
      this.plannedValue = defaultValue;
      this.description = description;
   }

   public Setting(String name, T defaultValue, T min, T max, String description) {
      this.isColorPicker = false;
      this.colorPickerDefaultGlobal = true;
      this.step = null;
      this.name = name;
      this.defaultValue = defaultValue;
      this.value = defaultValue;
      this.min = min;
      this.max = max;
      this.plannedValue = defaultValue;
      this.description = description;
      this.hasRestriction = true;
   }

   public Setting(String name, T defaultValue, T min, T max) {
      this.isColorPicker = false;
      this.colorPickerDefaultGlobal = true;
      this.step = null;
      this.name = name;
      this.defaultValue = defaultValue;
      this.value = defaultValue;
      this.min = min;
      this.max = max;
      this.plannedValue = defaultValue;
      this.description = "";
      this.hasRestriction = true;
   }

   public Setting(String name, T defaultValue, T min, T max, Predicate<T> visibility, String description) {
      this.isColorPicker = false;
      this.colorPickerDefaultGlobal = true;
      this.step = null;
      this.name = name;
      this.defaultValue = defaultValue;
      this.value = defaultValue;
      this.min = min;
      this.max = max;
      this.plannedValue = defaultValue;
      this.visibility = visibility;
      this.description = description;
      this.hasRestriction = true;
   }

   public Setting(String name, T defaultValue, T min, T max, Predicate<T> visibility) {
      this.isColorPicker = false;
      this.colorPickerDefaultGlobal = true;
      this.step = null;
      this.name = name;
      this.defaultValue = defaultValue;
      this.value = defaultValue;
      this.min = min;
      this.max = max;
      this.plannedValue = defaultValue;
      this.visibility = visibility;
      this.description = "";
      this.hasRestriction = true;
   }

   public Setting(String name, T defaultValue, Predicate<T> visibility) {
      this(name, defaultValue, visibility, "");
   }

   public Setting(String name, T defaultValue, Predicate<T> visibility, String description) {
      this.isColorPicker = false;
      this.colorPickerDefaultGlobal = true;
      this.step = null;
      this.name = name;
      this.defaultValue = defaultValue;
      this.value = defaultValue;
      this.visibility = visibility;
      this.plannedValue = defaultValue;
      this.description = description == null ? "" : description;
   }

   public Setting<T> setColorPickerDefaultGlobal(boolean defaultGlobal) {
      this.colorPickerDefaultGlobal = defaultGlobal;
      return this;
   }

   public String getName() {
      return this.name;
   }

   public T getValue() {
      return this.value;
   }

   public void setValue(T value) {
      this.setPlannedValue(value);
      if (this.hasRestriction) {
         if (((Number)this.min).floatValue() > ((Number)value).floatValue()) {
            this.setPlannedValue(this.min);
         }

         if (((Number)this.max).floatValue() < ((Number)value).floatValue()) {
            this.setPlannedValue(this.max);
         }
      }

      ClientEvent event = new ClientEvent(this);
      MinecraftForge.EVENT_BUS.post(event);
      if (!event.isCanceled()) {
         this.value = this.plannedValue;
      } else {
         this.plannedValue = this.value;
      }
   }

   public T getPlannedValue() {
      return this.plannedValue;
   }

   public void setPlannedValue(T value) {
      this.plannedValue = value;
   }

   public T getMin() {
      return this.min;
   }

   public void setMin(T min) {
      this.min = min;
   }

   public T getMax() {
      return this.max;
   }

   public void setMax(T max) {
      this.max = max;
   }

   public Feature getFeature() {
      return this.feature;
   }

   public void setFeature(Feature feature) {
      this.feature = feature;
   }

   public String getType() {
      return this.isEnumSetting() ? "Enum" : this.getClassName(this.defaultValue);
   }

   public boolean isColorPicker() {
      return this.isColorPicker;
   }

   public Setting<T> setColorPicker(boolean colorPicker) {
      this.isColorPicker = colorPicker;
      return this;
   }

   public <T> String getClassName(T value) {
      return value.getClass().getSimpleName();
   }

   public String getDescription() {
      if (this.descriptionProvider != null) {
         try {
            String dynamicDescription = (String)this.descriptionProvider.apply(this.getValue());
            if (dynamicDescription != null && !dynamicDescription.trim().isEmpty()) {
               return dynamicDescription;
            }
         } catch (Exception var2) {
         }
      }

      return this.description != null && !this.description.trim().isEmpty() ? this.description : this.createDefaultDescription();
   }

   public Setting<T> withDescription(String description) {
      this.descriptionProvider = null;
      this.description = description == null ? "" : description;
      return this;
   }

   public Setting<T> withDescription(Function<T, String> descriptionProvider) {
      this.description = "";
      this.descriptionProvider = descriptionProvider;
      return this;
   }

   public void setDescription(String description) {
      this.descriptionProvider = null;
      this.description = description == null ? "" : description;
   }

   public void setDescriptionProvider(Function<T, String> descriptionProvider) {
      this.description = "";
      this.descriptionProvider = descriptionProvider;
   }

   private String createDefaultDescription() {
      String settingName = this.name != null && !this.name.trim().isEmpty() ? this.name : "this setting";
      if (this.isColorPicker) {
         return "Changes the " + settingName + " color.";
      } else if (this.value instanceof Boolean) {
         return "Toggles " + settingName + ".";
      } else if (this.value instanceof Bind) {
         return "Sets the keybind for " + settingName + ".";
      } else if (this.isEnumSetting()) {
         return "Selects the mode used for " + settingName + ".";
      } else if (this.isNumberSetting()) {
         return "Adjusts the value of " + settingName + ".";
      } else {
         return !(this.value instanceof String) && !(this.value instanceof Character) ? "Changes " + settingName + "." : "Changes the value of " + settingName + ".";
      }
   }

   public boolean isNumberSetting() {
      return this.value instanceof Double || this.value instanceof Integer || this.value instanceof Short || this.value instanceof Long || this.value instanceof Float;
   }

   public boolean isEnumSetting() {
      return !this.isNumberSetting() && !(this.value instanceof String) && !(this.value instanceof Bind) && !(this.value instanceof Character) && !(this.value instanceof Boolean);
   }

   public boolean isStringSetting() {
      return this.value instanceof String;
   }

   public T getDefaultValue() {
      return this.defaultValue;
   }

   public String getValueAsString() {
      return this.value.toString();
   }

   public boolean hasRestriction() {
      return this.hasRestriction;
   }

   public Setting<T> setStep(T step) {
      this.step = step;
      return this;
   }

   public T getStep() {
      return this.step;
   }

   public boolean hasStep() {
      return this.step != null;
   }

   public boolean isVisible() {
      if (this.visibility == null) {
         return true;
      } else {
         try {
            return this.visibility.test(this.getValue());
         } catch (Exception var2) {
            return false;
         }
      }
   }

   public void changeEnumValue(int amount) {
      if (this.getValue() != null) {
         Enum<?>[] array = (Enum[])this.getValue().getClass().getEnumConstants();
         int currentIndex = -1;

         for(int i = 0; i < array.length; ++i) {
            if (this.getValue().toString().equalsIgnoreCase(array[i].toString())) {
               currentIndex = i;
               break;
            }
         }

         if (currentIndex != -1) {
            int newIndex = (currentIndex + amount + array.length) % array.length;
            this.setValue((T)array[newIndex]);
         }

      }
   }
}
