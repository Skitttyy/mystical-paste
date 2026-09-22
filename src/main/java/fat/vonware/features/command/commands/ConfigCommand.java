package fat.vonware.features.command.commands;

import com.mojang.realmsclient.gui.ChatFormatting;
import fat.vonware.Global;
import fat.vonware.Vonware;
import fat.vonware.features.command.Command;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;

public class ConfigCommand extends Command {
   public ConfigCommand() {
      super("config", new String[]{"<save/load/delete/list/folder>"});
   }

   public void execute(String[] commands) {
      if (commands.length == 1) {
         sendMessage(ChatFormatting.GRAY + "You'll find the config files in your gameProfile directory under vonware/configs");
      } else if (commands.length != 2) {
         if (commands.length >= 3) {
            switch (commands[0]) {
               case "save":
                  Vonware.configManager.saveConfig(commands[1]);
                  sendMessage(Global.getGlobalColorCode() + commands[1] + ChatFormatting.GRAY + " has been" + ChatFormatting.GREEN + " saved");
                  return;
               case "load":
                  if (Vonware.configManager.configExists(commands[1])) {
                     Vonware.configManager.loadConfig(commands[1]);
                     sendMessage(Global.getGlobalColorCode() + commands[1] + ChatFormatting.GRAY + " has been loaded");
                  } else {
                     sendMessage(ChatFormatting.RED + "Config '" + commands[1] + "' does not exist.");
                  }

                  return;
               case "delete":
                  if (commands[1].equalsIgnoreCase("config")) {
                     sendMessage(ChatFormatting.RED + "Cannot delete the default config!");
                     return;
                  }

                  if (Vonware.configManager.configExists(commands[1])) {
                     if (this.deleteConfig(commands[1])) {
                        sendMessage(Global.getGlobalColorCode() + commands[1] + ChatFormatting.GRAY + " has been" + ChatFormatting.RED + " deleted");
                     } else {
                        sendMessage(ChatFormatting.RED + "Failed to delete config '" + commands[1] + "'.");
                     }
                  } else {
                     sendMessage(ChatFormatting.RED + "Config '" + commands[1] + "' does not exist.");
                  }

                  return;
               default:
                  sendMessage("Not a valid command... Possible usage: <save/load/delete/list/folder>");
            }
         }

      } else {
         if ("list".equals(commands[0])) {
            File configsFolder = new File("vonware/configs/");
            if (!configsFolder.exists() || !configsFolder.isDirectory()) {
               sendMessage(ChatFormatting.RED + "No configs found.");
               return;
            }

            File[] files = configsFolder.listFiles((dir, name) -> name.endsWith(".json"));
            if (files == null || files.length == 0) {
               sendMessage(ChatFormatting.RED + "No configs found.");
               return;
            }

            StringBuilder builder = new StringBuilder(ChatFormatting.GRAY + "Configs: " + ChatFormatting.WHITE);

            for(int i = 0; i < files.length; ++i) {
               String configName = files[i].getName().replace(".json", "");
               builder.append(configName);
               if (i < files.length - 1) {
                  builder.append(", ");
               }
            }

            sendMessage(builder.toString());
         } else if ("folder".equals(commands[0])) {
            File configsFolder = new File("vonware/configs/");
            if (!configsFolder.exists()) {
               configsFolder.mkdirs();
            }

            try {
               if (Desktop.isDesktopSupported()) {
                  Desktop.getDesktop().open(configsFolder);
                  sendMessage("Opening configs folder...");
               } else {
                  sendMessage(ChatFormatting.RED + "Desktop is not supported on this system.");
               }
            } catch (IOException e) {
               sendMessage(ChatFormatting.RED + "Failed to open configs folder: " + e.getMessage());
               e.printStackTrace();
            }
         } else {
            sendMessage(ChatFormatting.GRAY + "Not a valid command... Possible usage: <list/save/load/delete/folder>");
         }

      }
   }

   private boolean deleteConfig(String name) {
      File configFile = new File("vonware/configs/" + name + ".json");
      return configFile.exists() && configFile.isFile() ? configFile.delete() : false;
   }
}
