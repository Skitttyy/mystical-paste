package fat.vonware.features.command.commands.player;

import fat.vonware.Vonware;
import fat.vonware.features.command.Command;
import fat.vonware.features.modules.player.FakePlayer;

public class FakePlayerCommand extends Command {
   public FakePlayerCommand() {
      super("fakeplayer", new String[]{""});
   }

   public void execute(String[] commands) {
      if (fullNullCheck()) {
         Command.sendMessage("You must be in a world to use the fake player.");
      } else {
         FakePlayer fakePlayer = (FakePlayer)Vonware.moduleManager.getModuleByClass(FakePlayer.class);
         if (fakePlayer == null) {
            Command.sendMessage("Could not find the FakePlayer module.");
         } else {
            if (fakePlayer.isEnabled()) {
               fakePlayer.disable();
               Command.sendMessage("Removed the fake player.");
            } else {
               fakePlayer.enable();
               Command.sendMessage("Spawned the fake player.");
            }

         }
      }
   }
}
