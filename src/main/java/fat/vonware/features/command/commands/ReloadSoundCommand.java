package fat.vonware.features.command.commands;

import com.mojang.realmsclient.gui.ChatFormatting;
import fat.vonware.features.command.Command;

public class ReloadSoundCommand extends Command {
   public ReloadSoundCommand() {
      super("reloadsoundsystem", new String[0]);
   }

   public void execute(String[] commands) {
      try {
         mc.getSoundHandler().onResourceManagerReload(mc.getResourceManager());
         Command.sendMessage(ChatFormatting.GREEN + "Reloaded Sound System.");
      } catch (Exception e) {
         e.printStackTrace();
         Command.sendMessage(ChatFormatting.RED + "Couldn't Reload Sound System: " + e.getMessage());
      }

   }
}
