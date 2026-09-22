package fat.vonware.features.command.commands.friend;

import com.mojang.realmsclient.gui.ChatFormatting;
import fat.vonware.Vonware;
import fat.vonware.features.command.Command;
import fat.vonware.manager.FutureManager;

public class RemoveCommand extends Command {
   public RemoveCommand() {
      super("remove", new String[]{"<name>"});
   }

   public void execute(String[] commands) {
      if (commands.length < 1) {
         sendMessage("Usage: remove <name>");
      } else {
         String name = commands[0];
         boolean wasFriend = Vonware.friendManager.isFriend(name);
         Vonware.friendManager.removeFriend(name);
         if (wasFriend && !Vonware.friendManager.isFriend(name)) {
            FutureManager.removeFriend(name);
         }

         sendMessage("" + ChatFormatting.RED + name + ChatFormatting.GRAY + " has been removed from your friends list.");
      }
   }
}
