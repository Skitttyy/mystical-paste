package fat.vonware.features.command.commands;

import com.mojang.realmsclient.gui.ChatFormatting;
import fat.vonware.Vonware;
import fat.vonware.features.command.Command;

public class HelpCommand extends Command {
   public HelpCommand() {
      super("help");
   }

   public void execute(String[] commands) {
      sendMessage("Command List: " + ChatFormatting.RESET);

      for(Command command : Vonware.commandManager.getCommands()) {
         if (!(command instanceof ModuleCommand)) {
            sendMessage(ChatFormatting.GRAY + Vonware.commandManager.getPrefix() + command.getName());
         }
      }

      sendMessage(ChatFormatting.GRAY + "Tip: You can also use module names as commands (example " + Vonware.commandManager.getPrefix() + "holeesp height 1.0)");
   }
}
