package fat.vonware.features.command.commands;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.realmsclient.gui.ChatFormatting;
import fat.vonware.Global;
import fat.vonware.features.command.Command;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import net.minecraft.item.ItemStack;

public class KitCommand extends Command {
   public static final File pathSave = new File("vonware/regear.json");

   public KitCommand() {
      super("kit", new String[]{"<list/set/create/del>", "<name>"});
   }

   public void execute(String[] commands) {
      if (commands.length == 0) {
         sendMessage("Usage: kit <list/set/save/del> <name>");
      } else {
         switch (commands[0].toLowerCase()) {
            case "list":
               this.sendList();
               break;
            case "set":
               if (commands.length < 2) {
                  sendMessage(ChatFormatting.RED + "Usage: kit set <name>");
                  return;
               }

               this.setKit(commands[1]);
               break;
            case "save":
            case "add":
            case "create":
               if (commands.length < 2) {
                  sendMessage(ChatFormatting.RED + "Usage: kit save <name>");
                  return;
               }

               this.saveKit(commands[1]);
               break;
            case "del":
            case "delete":
            case "remove":
               if (commands.length < 2) {
                  sendMessage(ChatFormatting.RED + "Usage: kit del <name>");
                  return;
               }

               this.deleteKit(commands[1]);
               break;
            default:
               sendMessage("Usage: kit <list/set/create/del> <name>");
         }

      }
   }

   private void sendList() {
      JsonObject completeJson = this.loadJson();
      if (completeJson != null && !completeJson.entrySet().isEmpty()) {
         sendMessage("Saved Kits:");

         for(Map.Entry<String, JsonElement> entry : completeJson.entrySet()) {
            String key = (String)entry.getKey();
            if (!key.equals("pointer")) {
               String prefix = "";

               try {
                  if (completeJson.has("pointer") && completeJson.get("pointer").getAsString().equals(key)) {
                     prefix = ChatFormatting.GREEN + "[In Use] " + Global.getGlobalColorCode();
                  }
               } catch (Exception var7) {
               }

               sendMessage(prefix + key);
            }
         }

      } else {
         sendMessage(ChatFormatting.RED + "No kits found!");
      }
   }

   private void setKit(String name) {
      JsonObject completeJson = this.loadJson();
      if (completeJson == null) {
         sendMessage(ChatFormatting.RED + "No kits found! Create one first with: kit save <name>");
      } else {
         if (completeJson.has(name) && !name.equals("pointer")) {
            completeJson.addProperty("pointer", name);
            this.saveJson(completeJson);
            sendMessage("Selected kit: " + Global.getGlobalColorCode() + name);
         } else {
            sendMessage(ChatFormatting.RED + "Kit '" + name + "' doesn't exist!");
         }

      }
   }

   private void deleteKit(String name) {
      JsonObject completeJson = this.loadJson();
      if (completeJson == null) {
         sendMessage(ChatFormatting.RED + "No kits found!");
      } else {
         if (completeJson.has(name) && !name.equals("pointer")) {
            completeJson.remove(name);

            try {
               if (completeJson.has("pointer") && completeJson.get("pointer").getAsString().equals(name)) {
                  completeJson.addProperty("pointer", "none");
               }
            } catch (Exception var4) {
            }

            this.saveJson(completeJson);
            sendMessage(ChatFormatting.RED + "Deleted kit: " + Global.getGlobalColorCode() + name);
         } else {
            sendMessage(ChatFormatting.RED + "Kit '" + name + "' doesn't exist!");
         }

      }
   }

   private void saveKit(String name) {
      if (name.equalsIgnoreCase("pointer")) {
         sendMessage(ChatFormatting.RED + "'pointer' is a reserved name!");
      } else {
         JsonObject completeJson = this.loadJson();
         if (completeJson == null) {
            completeJson = new JsonObject();
            completeJson.addProperty("pointer", "none");
         }

         if (completeJson.has(name)) {
            sendMessage(ChatFormatting.RED + "Kit '" + name + "' already exists! Delete it first or use a different name.");
         } else {
            StringBuilder jsonInventory = new StringBuilder();

            for(ItemStack item : mc.player.inventory.mainInventory) {
               if (item.isEmpty()) {
                  jsonInventory.append("minecraft:air0 ");
               } else {
                  jsonInventory.append(item.getItem().getRegistryName().toString()).append(item.getMetadata()).append(" ");
               }
            }

            completeJson.addProperty(name, jsonInventory.toString().trim());
            this.saveJson(completeJson);
            sendMessage("Saved kit: " + Global.getGlobalColorCode() + name);
         }
      }
   }

   private JsonObject loadJson() {
      try {
         return !pathSave.exists() ? null : (new JsonParser()).parse(new FileReader(pathSave)).getAsJsonObject();
      } catch (Exception var2) {
         return null;
      }
   }

   private void saveJson(JsonObject json) {
      try {
         pathSave.getParentFile().mkdirs();
         BufferedWriter writer = new BufferedWriter(new FileWriter(pathSave));
         writer.write(json.toString());
         writer.close();
      } catch (IOException var3) {
         sendMessage(ChatFormatting.RED + "Error saving kits!");
      }

   }

   public static String getCurrentKit() {
      try {
         if (!pathSave.exists()) {
            return "";
         } else {
            JsonObject completeJson = (new JsonParser()).parse(new FileReader(pathSave)).getAsJsonObject();
            return completeJson.has("pointer") && !completeJson.get("pointer").getAsString().equals("none") ? completeJson.get("pointer").getAsString() : "";
         }
      } catch (Exception var1) {
         return "";
      }
   }

   public static String getKitInventory(String kit) {
      try {
         if (!pathSave.exists()) {
            return "";
         } else {
            JsonObject completeJson = (new JsonParser()).parse(new FileReader(pathSave)).getAsJsonObject();
            return completeJson.has(kit) ? completeJson.get(kit).getAsString() : "";
         }
      } catch (Exception var2) {
         return "";
      }
   }
}
