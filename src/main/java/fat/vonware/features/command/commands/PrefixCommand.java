package fat.vonware.features.command.commands;

import com.mojang.realmsclient.gui.ChatFormatting;
import fat.vonware.Vonware;
import fat.vonware.features.command.Command;

public class PrefixCommand extends Command {
   public PrefixCommand() {
      super("prefix", new String[]{"<char>"});
   }

   public void execute(String[] commands) {
      if (commands.length == 1) {
         Command.sendMessage(ChatFormatting.GRAY + "Current prefix is " + ChatFormatting.BLUE + Vonware.commandManager.getPrefix());
      } else {
         Vonware.commandManager.setPrefix(commands[0]);
         Command.sendMessage(ChatFormatting.GRAY + "Prefix changed to " + ChatFormatting.BLUE + commands[0]);
      }
   }
}
