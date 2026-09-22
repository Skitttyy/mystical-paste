package fat.vonware.features.command.commands;

import com.google.gson.JsonParser;
import com.mojang.realmsclient.gui.ChatFormatting;
import fat.vonware.features.command.Command;
import fat.vonware.features.modules.Module;
import fat.vonware.features.setting.Setting;
import fat.vonware.manager.ConfigManager;

public class ModuleCommand extends Command {
   private final Module module;

   public ModuleCommand(Module module) {
      super(module.getName().toLowerCase(), new String[]{"<setting>", "<value>"});
      this.module = module;
   }

   public void execute(String[] commands) {
      if (commands.length == 1) {
         sendMessage(this.module.getDisplayName() + " : " + this.module.getDescription());

         for(Setting setting : this.module.getSettings()) {
            sendMessage(setting.getName() + " : " + setting.getValue());
         }

      } else if (commands.length == 2 && commands[0].equalsIgnoreCase("reset")) {
         for(Setting setting : this.module.getSettings()) {
            setting.setValue(setting.getDefaultValue());
         }

         sendMessage(ChatFormatting.GRAY + this.module.getDisplayName() + " settings reset.");
      } else if (commands.length == 2) {
         Setting setting = this.module.getSettingByName(commands[0]);
         if (setting == null) {
            sendMessage("Setting '" + commands[0] + "' not found.");
         } else {
            sendMessage(setting.getName() + " = " + setting.getValue());
         }
      } else {
         if (commands.length >= 3) {
            Setting setting = this.module.getSettingByName(commands[0]);
            if (setting == null) {
               sendMessage("Setting '" + commands[0] + "' not found.");
               return;
            }

            String value = commands[1];
            if (setting.getType().equalsIgnoreCase("String")) {
               setting.setValue(value);
               sendMessage(ChatFormatting.GRAY + setting.getName() + " set to " + value);
               return;
            }

            try {
               JsonParser jp = new JsonParser();
               if (setting.getName().equalsIgnoreCase("Enabled")) {
                  if (value.equalsIgnoreCase("true")) {
                     this.module.enable();
                  } else if (value.equalsIgnoreCase("false")) {
                     this.module.disable();
                  }
               }

               ConfigManager.setValueFromJson(this.module, setting, jp.parse(value));
               sendMessage(ChatFormatting.GRAY + setting.getName() + " set to " + value);
            } catch (Exception var5) {
               sendMessage("Bad value! Requires " + setting.getType());
            }
         }

      }
   }
}
