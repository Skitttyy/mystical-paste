package fat.vonware.features.command.commands.misc;

import fat.vonware.features.command.Command;
import net.minecraft.client.gui.GuiNewChat;

public class ClearHistoryCommand extends Command {
   public ClearHistoryCommand() {
      super("clearchat", new String[0]);
   }

   public void execute(String[] args) {
      GuiNewChat chat = mc.ingameGUI.getChatGUI();
      chat.clearChatMessages(true);
      Command.sendMessage("Chat history cleared.");
   }
}
