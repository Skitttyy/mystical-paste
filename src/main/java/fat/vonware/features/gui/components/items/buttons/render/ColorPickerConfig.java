package fat.vonware.features.gui.components.items.buttons.render;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fat.vonware.features.setting.Setting;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class ColorPickerConfig {
   private static final Path CONFIG_DIR = Paths.get("vonware", "other");
   private static final Path CONFIG_FILE;
   private static final Gson GSON;
   private static final Map<String, PickerState> states;
   private static boolean loaded;

   public static void setState(Setting<?> setting, boolean useGlobal, boolean sliderMode, int savedCustomColor) {
      if (!loaded) {
         load();
      }

      PickerState state = (PickerState)states.computeIfAbsent(keyFor(setting), (k) -> new PickerState());
      state.useGlobalColor = useGlobal;
      state.rgbSliderMode = sliderMode;
      state.savedCustomColor = savedCustomColor;
      save();
   }

   public static PickerState getState(Setting<?> setting) {
      return getState(setting, setting.colorPickerDefaultGlobal);
   }

   public static PickerState getState(Setting<?> setting, boolean defaultGlobal) {
      if (!loaded) {
         load();
      }

      return (PickerState)states.computeIfAbsent(keyFor(setting), (k) -> {
         PickerState state = new PickerState();
         state.useGlobalColor = defaultGlobal;
         return state;
      });
   }

   private static String keyFor(Setting<?> setting) {
      String featureName = setting.getFeature() != null ? setting.getFeature().getName() : "unknown";
      return featureName + "." + setting.getName();
   }

   public static void load() {
      loaded = true;
      states.clear();
      if (Files.exists(CONFIG_FILE, new LinkOption[0])) {
         try {
            Reader reader = Files.newBufferedReader(CONFIG_FILE);
            Throwable var1 = null;

            try {
               JsonObject root = (new JsonParser()).parse(reader).getAsJsonObject();

               for(Map.Entry<String, JsonElement> entry : root.entrySet()) {
                  JsonObject obj = ((JsonElement)entry.getValue()).getAsJsonObject();
                  PickerState state = new PickerState();
                  if (obj.has("useGlobalColor")) {
                     state.useGlobalColor = obj.get("useGlobalColor").getAsBoolean();
                  }

                  if (obj.has("rgbSliderMode")) {
                     state.rgbSliderMode = obj.get("rgbSliderMode").getAsBoolean();
                  }

                  if (obj.has("savedCustomColor")) {
                     state.savedCustomColor = obj.get("savedCustomColor").getAsInt();
                  }

                  states.put(entry.getKey(), state);
               }
            } catch (Throwable var15) {
               var1 = var15;
               throw var15;
            } finally {
               if (reader != null) {
                  if (var1 != null) {
                     try {
                        reader.close();
                     } catch (Throwable var14) {
                        var1.addSuppressed(var14);
                     }
                  } else {
                     reader.close();
                  }
               }

            }
         } catch (Exception var17) {
         }

      }
   }

   public static void save() {
      try {
         Files.createDirectories(CONFIG_DIR);
         JsonObject root = new JsonObject();

         for(Map.Entry<String, PickerState> entry : states.entrySet()) {
            JsonObject obj = new JsonObject();
            obj.addProperty("useGlobalColor", ((PickerState)entry.getValue()).useGlobalColor);
            obj.addProperty("rgbSliderMode", ((PickerState)entry.getValue()).rgbSliderMode);
            obj.addProperty("savedCustomColor", ((PickerState)entry.getValue()).savedCustomColor);
            root.add((String)entry.getKey(), obj);
         }

         Files.write(CONFIG_FILE, GSON.toJson(root).getBytes(), new OpenOption[0]);
      } catch (Exception var4) {
      }

   }

   static {
      CONFIG_FILE = CONFIG_DIR.resolve("colorpickers.json");
      GSON = (new GsonBuilder()).setPrettyPrinting().create();
      states = new HashMap();
      loaded = false;
   }

   public static class PickerState {
      public boolean useGlobalColor = true;
      public boolean rgbSliderMode = false;
      public int savedCustomColor = -1;
   }
}
