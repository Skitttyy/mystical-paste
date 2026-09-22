package fat.vonware.features.command.commands.client;

import com.mojang.realmsclient.gui.ChatFormatting;
import fat.vonware.Vonware;
import fat.vonware.features.command.Command;
import fat.vonware.features.modules.Module;

public class RenameCommand extends Command {
   public RenameCommand() {
      super("rename", new String[]{"<module>", "<newName>"});
   }

   public void execute(String[] commands) {
      if (commands.length != 3) {
         Command.sendMessage(ChatFormatting.GRAY + "Usage: rename <module> <newName>  |  rename resetname <module>");
      } else if (commands[0].equalsIgnoreCase("resetname")) {
         this.resetName(commands[1].replaceAll("_", " "));
      } else {
         String currentName = commands[0].replaceAll("_", " ");
         String newName = commands[1].replaceAll("_", " ");
         Module module = this.findModule(currentName);
         if (module == null) {
            Command.sendMessage("Unable to find a module with that name!");
         } else {
            module.setDisplayName(newName);
         }
      }
   }

   private void resetName(String currentName) {
      Module module = this.findModule(currentName);
      if (module == null) {
         Command.sendMessage("Unable to find a module with that name!");
      } else {
         String original = module.getName();
         if (module.getDisplayName().equalsIgnoreCase(original)) {
            Command.sendMessage(module.getDisplayName() + " is already using its original name.");
         } else {
            Command.sendMessage(module.getName() + ", has been reset to: " + original);
            module.displayName.setValue(original);
         }
      }
   }

   private Module findModule(String name) {
      Module module = Vonware.moduleManager.getModuleByDisplayName(name);
      if (module == null) {
         module = Vonware.moduleManager.getModuleByName(name);
      }

      return module;
   }
}
