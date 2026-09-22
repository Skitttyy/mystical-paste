package fat.vonware.features.command.commands.friend;

import com.mojang.realmsclient.gui.ChatFormatting;
import fat.vonware.Vonware;
import fat.vonware.features.command.Command;
import fat.vonware.manager.FriendManager;
import fat.vonware.manager.FutureManager;

public class FriendCommand extends Command {
   public FriendCommand() {
      super("friend", new String[]{"<add/del/list/reset>", "<name>"});
   }

   public void execute(String[] commands) {
      if (commands.length == 1) {
         sendMessage("Usage: friend <add/del/list/reset>");
      } else if (commands.length == 2) {
         switch (commands[0]) {
            case "list":
               if (Vonware.friendManager.getFriends().isEmpty()) {
                  sendMessage("You have no friends.");
               } else {
                  String f = "" + ChatFormatting.GRAY + "Friends: ";

                  for(FriendManager.Friend friend : Vonware.friendManager.getFriends()) {
                     try {
                        f = f + friend.getUsername() + ", ";
                     } catch (Exception var8) {
                     }
                  }

                  sendMessage(f);
               }

               return;
            case "reset":
               Vonware.friendManager.onLoad();
               sendMessage("Friends have been cleared.");
               return;
            default:
               sendMessage("Unknown command. Usage: friend <add/del/list/reset>");
         }
      } else {
         if (commands.length >= 3) {
            switch (commands[0]) {
               case "add":
                  boolean wasFriend = Vonware.friendManager.isFriend(commands[1]);
                  Vonware.friendManager.addFriend(commands[1]);
                  if (!wasFriend && Vonware.friendManager.isFriend(commands[1])) {
                     FutureManager.addFriend(commands[1]);
                  }

                  sendMessage("" + ChatFormatting.GRAY + commands[1] + " has been added to your friends list.");
                  return;
               case "del":
                  boolean wasFriendBeforeRemoval = Vonware.friendManager.isFriend(commands[1]);
                  Vonware.friendManager.removeFriend(commands[1]);
                  if (wasFriendBeforeRemoval && !Vonware.friendManager.isFriend(commands[1])) {
                     FutureManager.removeFriend(commands[1]);
                  }

                  sendMessage("" + ChatFormatting.RED + commands[1] + ChatFormatting.GRAY + " has been removed from your friends list.");
                  return;
            }
         }

         sendMessage("Unknown command. Usage: friend <add/del/list/reset>");
      }
   }
}
