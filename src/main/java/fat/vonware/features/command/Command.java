package fat.vonware.features.command;

import com.mojang.realmsclient.gui.ChatFormatting;
import fat.vonware.Global;
import fat.vonware.Vonware;
import fat.vonware.features.Feature;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentBase;

public abstract class Command extends Feature {
   protected String name;
   protected String[] commands;

   public Command(String name) {
      super(name);
      this.name = name;
      this.commands = new String[]{""};
   }

   public Command(String name, String[] commands) {
      super(name);
      this.name = name;
      this.commands = commands;
   }

   public static void sendMessage(String message) {
      sendSilentMessage(Global.getClientName() + ChatFormatting.GRAY + message);
   }

   public static void sendSilentMessage(String message, int id) {
      if (!fullNullCheck()) {
         mc.addScheduledTask(() -> mc.ingameGUI.getChatGUI().printChatMessageWithOptionalDeletion(new ChatMessage(message), id));
      }
   }

   public static void sendSilentMessage(String message) {
      if (!fullNullCheck()) {
         mc.addScheduledTask(() -> mc.ingameGUI.getChatGUI().printChatMessage(new ChatMessage(message)));
      }
   }

   public static void sendSilentMessage(ITextComponent component) {
      if (!fullNullCheck()) {
         mc.addScheduledTask(() -> mc.ingameGUI.getChatGUI().printChatMessage(component));
      }
   }

   public static String getCommandPrefix() {
      return Vonware.commandManager.getPrefix();
   }

   public abstract void execute(String[] var1);

   public String getName() {
      return this.name;
   }

   public String[] getCommands() {
      return this.commands;
   }

   public static class ChatMessage extends TextComponentBase {
      private final String text;

      public ChatMessage(String text) {
         Pattern pattern = Pattern.compile("&[0123456789abcdefrlosmk]");
         Matcher matcher = pattern.matcher(text);
         StringBuffer stringBuffer = new StringBuffer();

         while(matcher.find()) {
            String replacement = matcher.group().substring(1);
            matcher.appendReplacement(stringBuffer, replacement);
         }

         matcher.appendTail(stringBuffer);
         this.text = stringBuffer.toString();
      }

      public String getUnformattedComponentText() {
         return this.text;
      }

      public ITextComponent createCopy() {
         return null;
      }

      public ITextComponent shallowCopy() {
         return new ChatMessage(this.text);
      }
   }
}
