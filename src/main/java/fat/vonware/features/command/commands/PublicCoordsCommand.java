package fat.vonware.features.command.commands;

import fat.vonware.features.command.Command;
import net.minecraft.network.play.client.CPacketChatMessage;

public class PublicCoordsCommand extends Command {
   public PublicCoordsCommand() {
      super("pcoords", new String[0]);
   }

   public void execute(String[] args) {
      mc.player.connection.sendPacket(new CPacketChatMessage("<Coordinates> X: " + Math.round(mc.player.posX) + " Y: " + Math.round(mc.player.posY) + " Z: " + Math.round(mc.player.posZ) + " in the " + (mc.player.dimension == 0 ? "OverWorld" : (mc.player.dimension == 1 ? "End" : (mc.player.dimension == -1 ? "Nether" : "failed to detect dimension")))));
   }
}
