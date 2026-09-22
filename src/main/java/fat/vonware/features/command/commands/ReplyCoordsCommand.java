package fat.vonware.features.command.commands;

import fat.vonware.features.command.Command;
import net.minecraft.network.play.client.CPacketChatMessage;

public class ReplyCoordsCommand extends Command {
   public ReplyCoordsCommand() {
      super("rcoords", new String[0]);
   }

   public void execute(String[] args) {
      mc.player.connection.sendPacket(new CPacketChatMessage("/r <Coordinates> X: " + Math.round(mc.player.posX) + " Y: " + Math.round(mc.player.posY) + " Z: " + Math.round(mc.player.posZ) + " in the " + (mc.player.dimension == 0 ? "OverWorld" : (mc.player.dimension == 1 ? "End" : (mc.player.dimension == -1 ? "Nether" : "failed to detect dimension")))));
   }
}
