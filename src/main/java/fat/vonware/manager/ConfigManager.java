package fat.vonware.manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import fat.vonware.Vonware;
import fat.vonware.features.Feature;
import fat.vonware.features.modules.Module;
import fat.vonware.features.setting.Bind;
import fat.vonware.features.setting.EnumConverter;
import fat.vonware.features.setting.Setting;
import fat.vonware.util.Util;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraftforge.common.MinecraftForge;

public class ConfigManager implements Util {
   private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().create();
   private static final String ROOT_FOLDER = "vonware";
   private static final String CURRENT_CONFIG_FILE = "vonware/currentconfig.txt";
   private static final String KEYBINDS_FILE = "vonware/keybinds.json";
   public final ArrayList<Feature> features = new ArrayList();
   public String configsFolder = "vonware/configs/";
   public String currentConfigName = "config";
   public void init() {
      this.features.clear();
      this.features.addAll(Vonware.moduleManager.modules);
      this.features.add(Vonware.friendManager);
      this.ensureDirectories();
      this.loadConfig(this.loadCurrentConfig());
      this.loadKeybinds();
      Vonware.LOGGER.info("Config loaded.");
   }

   public void loadConfig(String name) {
      String safeName = this.normalizeConfigName(name);
      this.currentConfigName = safeName;
      this.ensureDirectories();
      Vonware.friendManager.onLoad();
      try {
         this.loadModulesConfig(safeName);
      } catch (Exception e) {
         Vonware.LOGGER.error("Failed to load config: " + safeName, e);
      }

      this.saveCurrentConfig();
   }

   public void saveConfig(String name) {
      String safeName = this.normalizeConfigName(name);
      this.currentConfigName = safeName;
      this.ensureDirectories();
      Vonware.friendManager.saveFriends();

      try {
         this.saveModulesConfig(safeName);
      } catch (Exception e) {
         Vonware.LOGGER.error("Failed to save config: " + safeName, e);
      }

      this.saveKeybinds();
      this.saveCurrentConfig();
   }

   public boolean configExists(String name) {
      return Files.isRegularFile(this.getConfigPath(this.normalizeConfigName(name)), new LinkOption[0]);
   }

   public void resetConfig(boolean saveConfig, String name) {
      for(Feature feature : this.features) {
         try {
            feature.reset();
         } catch (Exception e) {
            Vonware.LOGGER.error("Failed to reset feature: " + feature.getName(), e);
         }
      }

      if (saveConfig) {
         this.saveConfig(name);
      }

   }

   public void saveCurrentConfig() {
      try {
         Path path = Paths.get("vonware/currentconfig.txt");
         Files.createDirectories(path.getParent());
         this.writeTextAtomically(path, this.currentConfigName == null ? "config" : this.currentConfigName);
      } catch (Exception e) {
         Vonware.LOGGER.error("Failed to save current config name.", e);
      }

   }

   public String loadCurrentConfig() {
      Path path = Paths.get("vonware/currentconfig.txt");
      if (!Files.isRegularFile(path, new LinkOption[0])) {
         return "config";
      } else {
         try {
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            if (!lines.isEmpty() && !((String)lines.get(0)).trim().isEmpty()) {
               return this.normalizeConfigName((String)lines.get(0));
            }
         } catch (Exception e) {
            Vonware.LOGGER.error("Failed to read current config name.", e);
         }

         return "config";
      }
   }

   private void saveModulesConfig(String configName) throws IOException {
      JsonObject root = new JsonObject();
      root.addProperty("_format", 2);

      for(Feature feature : this.features) {
         if (feature instanceof Module) {
            root.add(feature.getName(), this.writeSettings(feature));
         }
      }

      this.writeJsonAtomically(this.getConfigPath(configName), root);
   }

   private void loadModulesConfig(String configName) throws IOException {
      Path configPath = this.getConfigPath(configName);
      if (!Files.isRegularFile(configPath, new LinkOption[0])) {
         Vonware.LOGGER.info("Config does not exist yet: " + configName);
      } else {
         JsonObject root;
         try {
            label180: {
               Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8);
               Throwable var5 = null;

               try {
                  JsonElement parsed = (new JsonParser()).parse(reader);
                  if (parsed != null && parsed.isJsonObject()) {
                     root = parsed.getAsJsonObject();
                     break label180;
                  }

                  Vonware.LOGGER.error("Config root is not a JSON object: " + configName);
               } catch (Throwable var19) {
                  var5 = var19;
                  throw var19;
               } finally {
                  if (reader != null) {
                     if (var5 != null) {
                        try {
                           reader.close();
                        } catch (Throwable var17) {
                           var5.addSuppressed(var17);
                        }
                     } else {
                        reader.close();
                     }
                  }

               }

               return;
            }
         } catch (IllegalStateException | JsonParseException e) {
            Vonware.LOGGER.error("Bad config file; no values were applied: " + configName, e);
            return;
         }

         int loadedCount = 0;
         int failedCount = 0;

         for(Feature feature : this.features) {
            if (feature instanceof Module) {
               try {
                  JsonObject moduleObject = getObjectIgnoreCase(root, feature.getName());
                  if (moduleObject != null) {
                     this.loadFeatureSettings(moduleObject, feature);
                     JsonElement enabled = getIgnoreCase(moduleObject, "Enabled");
                     if (enabled != null && enabled.isJsonPrimitive() && enabled.getAsJsonPrimitive().isBoolean()) {
                        this.applyEnabledState((Module)feature, enabled.getAsBoolean());
                     }

                     ++loadedCount;
                  }
               } catch (Exception e) {
                  ++failedCount;
                  Vonware.LOGGER.error("Skipping module while loading config (kept its current/default state): " + feature.getName(), e);
               }
            }
         }

         Vonware.LOGGER.info("Config '" + configName + "' loaded: " + loadedCount + " module(s) applied" + (failedCount > 0 ? ", " + failedCount + " skipped" : ""));
      }
   }

   private void loadFeatureSettings(JsonObject input, Feature feature) {
      for(Setting setting : feature.getSettings()) {
         String settingName = setting.getName();
         if (!"Enabled".equalsIgnoreCase(settingName) && !"Keybind".equalsIgnoreCase(settingName)) {
            JsonElement value = getIgnoreCase(input, settingName);
            if (value != null && !value.isJsonNull()) {
               try {
                  setValueFromJson(feature, setting, value);
               } catch (Exception e) {
                  Vonware.LOGGER.error("Skipping invalid setting " + feature.getName() + "." + settingName + " from config.", e);
               }
            }
         }
      }

   }

   public static void setValueFromJson(Feature feature, Setting setting, JsonElement element) {
      if (element != null && !element.isJsonNull()) {
         switch (setting.getType()) {
            case "Boolean":
               setting.setValue(element.getAsBoolean());
               return;
            case "Double":
               setting.setValue(element.getAsDouble());
               return;
            case "Float":
               setting.setValue(element.getAsFloat());
               return;
            case "Integer":
               setting.setValue(element.getAsInt());
               return;
            case "Short":
               setting.setValue(element.getAsShort());
               return;
            case "Long":
               setting.setValue(element.getAsLong());
               return;
            case "String":
               setting.setValue(element.getAsString());
               return;
            case "Bind":
               setting.setValue((new Bind.BindConverter()).doBackward(element));
               return;
            case "Enum":
               Enum current = (Enum)setting.getValue();
               if (current == null) {
                  current = (Enum)setting.getDefaultValue();
               }

               if (current == null) {
                  return;
               }

               EnumConverter converter = new EnumConverter(current.getClass());
               Enum enumValue = converter.doBackward(element);
               if (enumValue != null) {
                  setting.setValue(enumValue);
               } else {
                  Vonware.LOGGER.warn("Ignoring unknown enum value for " + feature.getName() + "." + setting.getName());
               }

               return;
            default:
               Vonware.LOGGER.warn("Unknown setting type for " + feature.getName() + "." + setting.getName() + ": " + setting.getType());
         }
      }
   }

   private void applyEnabledState(Module module, boolean shouldBeEnabled) {
      if (module.isEnabled() != shouldBeEnabled) {
         try {
            if (shouldBeEnabled) {
               module.enabled.setValue(true);
               module.onEnable();
               if (module.hasListener && !module.alwaysListening) {
                  try {
                     MinecraftForge.EVENT_BUS.unregister(module);
                  } catch (Exception var5) {
                  }

                  MinecraftForge.EVENT_BUS.register(module);
               }
            } else {
               if (module.hasListener && !module.alwaysListening) {
                  try {
                     MinecraftForge.EVENT_BUS.unregister(module);
                  } catch (Exception var4) {
                  }
               }

               module.enabled.setValue(false);
               module.onDisable();
            }
         } catch (Exception e) {
            Vonware.LOGGER.error("Error applying enabled state for module: " + module.getName(), e);
         }

      }
   }

   public JsonObject writeSettings(Feature feature) {
      JsonObject object = new JsonObject();
      if (feature instanceof Module) {
         object.addProperty("Enabled", ((Module)feature).isEnabled());
      }

      for(Setting setting : feature.getSettings()) {
         if (!"Keybind".equalsIgnoreCase(setting.getName()) && !"Enabled".equalsIgnoreCase(setting.getName())) {
            try {
               object.add(setting.getName(), this.serializeSetting(setting));
            } catch (Exception e) {
               Vonware.LOGGER.error("Skipping setting while saving: " + feature.getName() + "." + setting.getName(), e);
            }
         }
      }

      return object;
   }

   private JsonElement serializeSetting(Setting setting) {
      Object value = setting.getValue();
      if (value == null) {
         return JsonNull.INSTANCE;
      } else if (setting.isEnumSetting()) {
         EnumConverter converter = new EnumConverter(((Enum)value).getClass());
         return converter.doForward((Enum)value);
      } else if (value instanceof Boolean) {
         return new JsonPrimitive((Boolean)value);
      } else if (value instanceof Number) {
         return new JsonPrimitive((Number)value);
      } else if (value instanceof Character) {
         return new JsonPrimitive((Character)value);
      } else if (value instanceof String) {
         return new JsonPrimitive((String)value);
      } else {
         return (JsonElement)(value instanceof Bind ? (new Bind.BindConverter()).doForward((Bind)value) : new JsonPrimitive(String.valueOf(value)));
      }
   }

   private void saveKeybinds() {
      try {
         JsonObject root = new JsonObject();

         for(Feature feature : this.features) {
            if (feature instanceof Module) {
               Module module = (Module)feature;
               Setting setting = module.getSettingByName("Keybind");
               if (setting != null && setting.getValue() instanceof Bind) {
                  root.add(module.getName(), (new Bind.BindConverter()).doForward((Bind)setting.getValue()));
               }
            }
         }

         this.writeJsonAtomically(Paths.get("vonware/keybinds.json"), root);
      } catch (Exception e) {
         Vonware.LOGGER.error("Failed to save keybinds.", e);
      }

   }

   private void loadKeybinds() {
      Path path = Paths.get("vonware/keybinds.json");
      if (Files.isRegularFile(path, new LinkOption[0])) {
         try {
            Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8);
            Throwable var3 = null;

            try {
               JsonElement parsed = (new JsonParser()).parse(reader);
               if (parsed != null && parsed.isJsonObject()) {
                  JsonObject root = parsed.getAsJsonObject();

                  for(Feature feature : this.features) {
                     if (feature instanceof Module) {
                        JsonElement value = getIgnoreCase(root, feature.getName());
                        if (value != null && !value.isJsonNull()) {
                           Setting keybind = feature.getSettingByName("Keybind");
                           if (keybind != null) {
                              try {
                                 keybind.setValue((new Bind.BindConverter()).doBackward(value));
                              } catch (Exception e) {
                                 Vonware.LOGGER.error("Skipping invalid keybind for: " + feature.getName(), e);
                              }
                           }
                        }
                     }
                  }

                  return;
               }
            } catch (Throwable var22) {
               var3 = var22;
               throw var22;
            } finally {
               if (reader != null) {
                  if (var3 != null) {
                     try {
                        reader.close();
                     } catch (Throwable var20) {
                        var3.addSuppressed(var20);
                     }
                  } else {
                     reader.close();
                  }
               }

            }

         } catch (Exception e) {
            Vonware.LOGGER.error("Failed to load keybinds.", e);
         }
      }
   }

   public String getDirectory(Feature feature) {
      return feature instanceof Module ? ((Module)feature).getCategory().getName() + "/" : "";
   }

   private void ensureDirectories() {
      try {
         Files.createDirectories(Paths.get(this.configsFolder));
         Files.createDirectories(Paths.get("vonware"));
      } catch (IOException e) {
         Vonware.LOGGER.error("Failed to create config directories.", e);
      }

   }

   private Path getConfigPath(String configName) {
      return Paths.get(this.configsFolder, this.normalizeConfigName(configName) + ".json");
   }

   private String normalizeConfigName(String name) {
      if (name == null) {
         return "config";
      } else {
         String clean = name.trim();
         if (clean.toLowerCase(Locale.ROOT).endsWith(".json")) {
            clean = clean.substring(0, clean.length() - 5);
         }

         clean = clean.replaceAll("[\\\\/:*?\"<>|]", "_");
         clean = clean.replace("..", "_");
         clean = clean.trim();
         return clean.isEmpty() ? "config" : clean;
      }
   }

   private static JsonElement getIgnoreCase(JsonObject object, String key) {
      if (object.has(key)) {
         return object.get(key);
      } else {
         for(Map.Entry<String, JsonElement> entry : object.entrySet()) {
            if (((String)entry.getKey()).equalsIgnoreCase(key)) {
               return (JsonElement)entry.getValue();
            }
         }

         return null;
      }
   }

   private static JsonObject getObjectIgnoreCase(JsonObject object, String key) {
      JsonElement element = getIgnoreCase(object, key);
      return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
   }

   private void writeJsonAtomically(Path target, JsonElement json) throws IOException {
      this.writeTextAtomically(target, GSON.toJson(json));
   }

   private void writeTextAtomically(Path target, String text) throws IOException {
      Path parent = target.getParent();
      if (parent != null) {
         Files.createDirectories(parent);
      }

      Path temp = target.resolveSibling(target.getFileName().toString() + ".tmp");
      Files.write(temp, text.getBytes(StandardCharsets.UTF_8), new OpenOption[]{StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE});

      try {
         Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
      } catch (AtomicMoveNotSupportedException var6) {
         Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
      }

   }
}
