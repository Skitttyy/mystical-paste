package fat.vonware.features.command.commands.misc;

import fat.vonware.features.command.Command;

public class FOVChangerCommand extends Command {
   public FOVChangerCommand() {
      super("fov", new String[]{"<value>"});
   }

   public void execute(String[] args) {
      if (args.length == 0) {
         Command.sendMessage("Usage: ,fov <value>");
      } else {
         try {
            float fov = Float.parseFloat(args[0]);
            if (fov < 1.0F || fov > 180.0F) {
               Command.sendMessage("FOV must be between 1 and 180");
               return;
            }

            mc.gameSettings.fovSetting = fov;
            Command.sendMessage("FOV set to " + fov);
         } catch (NumberFormatException var3) {
            Command.sendMessage("Invalid FOV value. Please enter a number.");
         }

      }
   }
}
