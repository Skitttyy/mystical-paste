package fat.vonware.features.command.commands.friend;

import com.mojang.realmsclient.gui.ChatFormatting;
import fat.vonware.Vonware;
import fat.vonware.features.command.Command;
import fat.vonware.manager.FutureManager;

public class AddCommand extends Command {
   public AddCommand() {
      super("add", new String[]{"<name>"});
   }

   public void execute(String[] commands) {
      if (commands.length < 1) {
         sendMessage("Usage: add <name>");
      } else {
         String name = commands[0];
         boolean wasFriend = Vonware.friendManager.isFriend(name);
         Vonware.friendManager.addFriend(name);
         if (!wasFriend && Vonware.friendManager.isFriend(name)) {
            FutureManager.addFriend(name);
         }

         sendMessage("" + ChatFormatting.GRAY + name + " has been added to your friends list.");
      }
   }
}
