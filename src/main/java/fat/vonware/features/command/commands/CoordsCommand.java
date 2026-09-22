package fat.vonware.features.command.commands;

import fat.vonware.features.command.Command;
import net.minecraft.network.play.client.CPacketChatMessage;

public class CoordsCommand extends Command {
   public CoordsCommand() {
      super("coords", new String[]{"<playername>"});
   }

   public void execute(String[] args) {
      if (args.length == 1) {
         Command.sendMessage("Please enter a player name");
      }

      mc.player.connection.sendPacket(new CPacketChatMessage("/msg " + args[0] + " My coordinates are X: " + Math.round(mc.player.posX) + " Y: " + Math.round(mc.player.posY) + " Z: " + Math.round(mc.player.posZ) + " in the " + (mc.player.dimension == 0 ? "OverWorld" : (mc.player.dimension == 1 ? "End" : (mc.player.dimension == -1 ? "Nether" : "failed to detect dimension")))));
   }
}
