package fat.vonware.features.command.commands.colors;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mojang.realmsclient.gui.ChatFormatting;
import fat.vonware.Global;
import fat.vonware.features.command.Command;
import fat.vonware.features.modules.client.Colors;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

public class ColorsPresetCommand extends Command {
   private static final String PRESETS_FOLDER = "vonware/color-presets/";
   private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().create();

   public ColorsPresetCommand() {
      super("colorpreset", new String[]{"<save/load/delete/list>", "<name>"});
   }

   public void execute(String[] commands) {
      if (commands.length < 2) {
         sendMessage(ChatFormatting.RED + "Usage: " + ChatFormatting.GRAY + "colorpreset <save/load/delete/list> <name>");
      } else {
         switch (commands[0].toLowerCase()) {
            case "save":
               if (commands.length < 3) {
                  sendMessage(ChatFormatting.RED + "Please specify a preset name.");
                  return;
               }

               this.savePreset(commands[1]);
               break;
            case "load":
               if (commands.length < 3) {
                  sendMessage(ChatFormatting.RED + "Please specify a preset name.");
                  return;
               }

               this.loadPreset(commands[1]);
               break;
            case "delete":
               if (commands.length < 3) {
                  sendMessage(ChatFormatting.RED + "Please specify a preset name.");
                  return;
               }

               this.deletePreset(commands[1]);
               break;
            case "list":
               this.listPresets();
               break;
            default:
               sendMessage(ChatFormatting.RED + "Invalid action. Use: " + ChatFormatting.GRAY + "save, load, delete, or list");
         }

      }
   }

   private void savePreset(String name) {
      try {
         File folder = new File("vonware/color-presets/");
         if (!folder.exists()) {
            folder.mkdirs();
         }

         File presetFile = new File("vonware/color-presets/" + name + ".json");
         Colors colors = Colors.getInstance();
         JsonObject preset = new JsonObject();
         preset.addProperty("red", (Number)colors.red.getValue());
         preset.addProperty("green", (Number)colors.green.getValue());
         preset.addProperty("blue", (Number)colors.blue.getValue());
         preset.addProperty("hue", (Number)colors.hue.getValue());
         preset.addProperty("saturation", (Number)colors.saturation.getValue());
         preset.addProperty("lightness", (Number)colors.lightness.getValue());
         preset.addProperty("hsvHue", (Number)colors.hsvHue.getValue());
         preset.addProperty("hsvSaturation", (Number)colors.hsvSaturation.getValue());
         preset.addProperty("hsvValue", (Number)colors.hsvValue.getValue());
         Writer writer = new FileWriter(presetFile);
         GSON.toJson(preset, writer);
         writer.close();
         sendMessage(ChatFormatting.GREEN + "Saved color preset: " + Global.getGlobalColorCode() + name);
      } catch (Exception e) {
         sendMessage(ChatFormatting.RED + "Failed to save preset: " + e.getMessage());
         e.printStackTrace();
      }

   }

   private void loadPreset(String name) {
      try {
         File presetFile = new File("vonware/color-presets/" + name + ".json");
         if (!presetFile.exists()) {
            sendMessage(ChatFormatting.RED + "Preset '" + name + "' does not exist.");
            return;
         }

         Reader reader = new FileReader(presetFile);
         JsonObject preset = (JsonObject)GSON.fromJson(reader, JsonObject.class);
         reader.close();
         Colors colors = Colors.getInstance();
         if (preset.has("red")) {
            colors.red.setValue(preset.get("red").getAsInt());
         }

         if (preset.has("green")) {
            colors.green.setValue(preset.get("green").getAsInt());
         }

         if (preset.has("blue")) {
            colors.blue.setValue(preset.get("blue").getAsInt());
         }

         if (preset.has("hue")) {
            colors.hue.setValue(preset.get("hue").getAsInt());
         }

         if (preset.has("saturation")) {
            colors.saturation.setValue(preset.get("saturation").getAsFloat());
         }

         if (preset.has("lightness")) {
            colors.lightness.setValue(preset.get("lightness").getAsFloat());
         }

         if (preset.has("hsvHue")) {
            colors.hsvHue.setValue(preset.get("hsvHue").getAsFloat());
         }

         if (preset.has("hsvSaturation")) {
            colors.hsvSaturation.setValue(preset.get("hsvSaturation").getAsFloat());
         }

         if (preset.has("hsvValue")) {
            colors.hsvValue.setValue(preset.get("hsvValue").getAsFloat());
         }

         if ((Boolean)colors.futureSync.getValue()) {
            colors.syncToFuture();
         }

         sendMessage(ChatFormatting.GRAY + "Loaded color preset: " + Global.getGlobalColorCode() + name);
      } catch (Exception e) {
         sendMessage(ChatFormatting.RED + "Failed to load preset: " + e.getMessage());
         e.printStackTrace();
      }

   }

   private void deletePreset(String name) {
      try {
         File presetFile = new File("vonware/color-presets/" + name + ".json");
         if (!presetFile.exists()) {
            sendMessage(ChatFormatting.RED + "Preset '" + name + "' does not exist.");
            return;
         }

         if (presetFile.delete()) {
            sendMessage(ChatFormatting.RED + "Deleted color preset: " + Global.getGlobalColorCode() + name);
         } else {
            sendMessage(ChatFormatting.RED + "Failed to delete preset.");
         }
      } catch (Exception e) {
         sendMessage(ChatFormatting.RED + "Failed to delete preset: " + e.getMessage());
         e.printStackTrace();
      }

   }

   private void listPresets() {
      try {
         File folder = new File("vonware/color-presets/");
         if (!folder.exists() || !folder.isDirectory()) {
            sendMessage(ChatFormatting.RED + "No color presets found.");
            return;
         }

         File[] files = folder.listFiles((dir, name) -> name.endsWith(".json"));
         if (files == null || files.length == 0) {
            sendMessage(ChatFormatting.RED + "No color presets found.");
            return;
         }

         List<String> presetNames = new ArrayList();

         for(File file : files) {
            presetNames.add(file.getName().replace(".json", ""));
         }

         StringBuilder message = new StringBuilder(ChatFormatting.GRAY + "Color Presets: " + ChatFormatting.WHITE);

         for(int i = 0; i < presetNames.size(); ++i) {
            message.append((String)presetNames.get(i));
            if (i < presetNames.size() - 1) {
               message.append(", ");
            }
         }

         sendMessage(message.toString());
      } catch (Exception e) {
         sendMessage(ChatFormatting.RED + "Failed to list presets: " + e.getMessage());
         e.printStackTrace();
      }

   }
}
