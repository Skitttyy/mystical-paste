package fat.vonware.features.modules.combat;

import com.mojang.realmsclient.gui.ChatFormatting;
import fat.vonware.features.command.Command;
import fat.vonware.features.modules.Module;

public class PflySwitch extends Module {
   public PflySwitch() {
      super("PflySwitch", "Switches Future's PacketFly mode", Module.Category.COMBAT, true, false, false);
   }

   public void onEnable() {
      mc.player.sendChatMessage(".packetfly mode factor");
      mc.player.sendChatMessage(".packetfly factor 0.6");
      mc.player.sendChatMessage(".packetfly type limitjitter");
      mc.player.sendChatMessage(".packetfly phase full");
      Command.sendSilentMessage(ChatFormatting.GRAY + "Set PacketFly mode to Phase");
   }

   public void onDisable() {
      mc.player.sendChatMessage(".packetfly mode factor");
      mc.player.sendChatMessage(".packetfly factor 1.8");
      mc.player.sendChatMessage(".packetfly type snap");
      mc.player.sendChatMessage(".packetfly phase off");
      Command.sendSilentMessage(ChatFormatting.GRAY + "Set PacketFly mode to Fly");
   }
}
