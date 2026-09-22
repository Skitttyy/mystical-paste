package fat.vonware.features.command.commands.client;

import com.mojang.realmsclient.gui.ChatFormatting;
import fat.vonware.Global;
import fat.vonware.Vonware;
import fat.vonware.features.command.Command;
import fat.vonware.features.modules.Module;
import fat.vonware.features.setting.Bind;
import org.lwjgl.input.Keyboard;

public class BindCommand extends Command {
   public BindCommand() {
      super("bind", new String[]{"<module>", "<bind>"});
   }

   public void execute(String[] commands) {
      if (commands.length == 1) {
         sendMessage(ChatFormatting.GRAY + "Bind <module> <key>");
      } else {
         String rkey = commands[1];
         String moduleName = commands[0];
         Module module = Vonware.moduleManager.getModuleByName(moduleName);
         if (module == null) {
            sendMessage(ChatFormatting.GRAY + "'" + moduleName + "' does not exist.");
         } else if (rkey == null) {
            sendMessage(Global.getGlobalColorCode() + module.getName() + ChatFormatting.GRAY + " is bound to " + ChatFormatting.WHITE + module.getBind().toString());
         } else {
            int key = Keyboard.getKeyIndex(rkey.toUpperCase());
            if (rkey.equalsIgnoreCase("none")) {
               key = -1;
            }

            if (key == 0) {
               sendMessage("Unknown key '" + rkey + "'!");
            } else {
               module.bind.setValue(new Bind(key));
               sendMessage(Global.getGlobalColorCode() + module.getName() + ChatFormatting.GRAY + " has been binded to " + ChatFormatting.WHITE + rkey.toUpperCase());
            }
         }
      }
   }
}
