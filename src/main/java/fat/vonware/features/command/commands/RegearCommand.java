package fat.vonware.features.command.commands;

import fat.vonware.features.command.Command;
import fat.vonware.util.Chat.ThreadUtil;
import net.minecraft.network.play.client.CPacketChatMessage;

public class RegearCommand extends Command {
   private static final String BOT_NAME = "RegearWarrior";

   public RegearCommand() {
      super("regear", new String[]{""});
   }

   public void execute(String[] args) {
      mc.player.connection.sendPacket(new CPacketChatMessage("/msg RegearWarrior I need a regear"));
      ThreadUtil.startDaemon("Regear-Command", () -> {
         try {
            Thread.sleep(500L);
            String coords = Math.round(mc.player.posX) + " " + Math.round(mc.player.posY) + " " + Math.round(mc.player.posZ);
            mc.player.connection.sendPacket(new CPacketChatMessage("/msg RegearWarrior " + coords));
            Command.sendMessage("\u00a7a[RegearCommand] \u00a7fSent regear request to \u00a7bRegearWarrior");
         } catch (InterruptedException var1) {
            Thread.currentThread().interrupt();
         }

      });
   }
}
