package fat.vonware.features.command.commands;

import fat.vonware.features.command.Command;

public class SwitchPacketFlyCommand extends Command {
   private boolean toggled;

   public SwitchPacketFlyCommand() {
      super("pfly", new String[]{"switchpacketfly", "switchpfly"});
   }

   public void execute(String[] args) {
      String mode;
      if (this.toggled) {
         mc.player.sendChatMessage(".packetfly mode factor");
         mc.player.sendChatMessage(".packetfly factor 0.6");
         mc.player.sendChatMessage(".packetfly type limitjitter");
         mc.player.sendChatMessage(".packetfly phase full");
         mode = "Phase";
      } else {
         mc.player.sendChatMessage(".packetfly mode factor");
         mc.player.sendChatMessage(".packetfly factor 1.8");
         mc.player.sendChatMessage(".packetfly type snap");
         mc.player.sendChatMessage(".packetfly phase off");
         mode = "Fly";
      }

      this.toggled = !this.toggled;
      sendMessage("Set PacketFly mode to " + mode);
   }
}
