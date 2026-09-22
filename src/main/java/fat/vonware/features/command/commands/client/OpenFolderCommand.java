package fat.vonware.features.command.commands.client;

import com.mojang.realmsclient.gui.ChatFormatting;
import fat.vonware.features.command.Command;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;

public class OpenFolderCommand extends Command {
   public OpenFolderCommand() {
      super("openfolder", new String[0]);
   }

   public void execute(String[] commands) {
      File configsFolder = new File("vonware/configs/");
      if (!configsFolder.exists()) {
         configsFolder.mkdirs();
      }

      try {
         if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().open(configsFolder);
            sendMessage(ChatFormatting.GRAY + "Opening configs folder...");
         } else {
            sendMessage(ChatFormatting.RED + "Desktop is not supported on this system.");
         }
      } catch (IOException e) {
         sendMessage(ChatFormatting.RED + "Failed to open configs folder: " + e.getMessage());
         e.printStackTrace();
      }

   }
}
