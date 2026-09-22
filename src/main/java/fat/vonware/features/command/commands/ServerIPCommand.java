package fat.vonware.features.command.commands;

import com.mojang.realmsclient.gui.ChatFormatting;
import fat.vonware.features.command.Command;

public class ServerIPCommand extends Command {
   public ServerIPCommand() {
      super("ip", new String[]{"ip", "server"});
   }

   public void execute(String[] args) {
      if (mc.getCurrentServerData() != null) {
         String serverIP = mc.getCurrentServerData().serverIP;
         sendMessage("Server IP: " + ChatFormatting.GREEN + serverIP);
      } else {
         sendMessage("You are not connected to a server (Singleplayer or LAN).");
      }

   }
}
