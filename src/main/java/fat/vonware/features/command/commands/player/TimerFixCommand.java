package fat.vonware.features.command.commands.player;

import fat.vonware.features.command.Command;

public class TimerFixCommand extends Command {
   public TimerFixCommand() {
      super("timerfix", new String[0]);
   }

   public void execute(String[] commands) {
      mc.timer.tickLength = 50.0F;
      sendMessage("Timer state has reset.");
   }
}
