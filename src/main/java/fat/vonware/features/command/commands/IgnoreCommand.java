package fat.vonware.features.command.commands;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import fat.vonware.features.command.Command;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class IgnoreCommand extends Command {
   private static final Set<String> ignoreList = new HashSet();
   private static final String IGNORE_FILE = "vonware/ignored.json";
   private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().create();

   public IgnoreCommand() {
      super("ignore", new String[]{"ignore"});
      MinecraftForge.EVENT_BUS.register(this);
      this.loadIgnoreList();
   }

   public void execute(String[] args) {
      if (args.length < 1) {
         sendMessage("Usage: .ignore <player> | .ignore <remove|list> <player>");
      } else {
         switch (args[0].toLowerCase()) {
            case "remove":
               if (args.length < 2) {
                  sendMessage("Please specify a player to remove from the ignore list.");
               } else {
                  String playerToRemove = args[1];
                  if (ignoreList.remove(playerToRemove.toLowerCase())) {
                     this.saveIgnoreList();
                     sendMessage("Removed " + playerToRemove + " from the ignore list.");
                  } else {
                     sendMessage(playerToRemove + " is not on the ignore list.");
                  }
               }
               break;
            case "list":
               if (ignoreList.isEmpty()) {
                  sendMessage("The ignore list is empty.");
               } else {
                  sendMessage("Ignore list: " + String.join(", ", ignoreList));
               }
               break;
            default:
               String playerToAdd = args[0];
               if (ignoreList.add(playerToAdd.toLowerCase())) {
                  this.saveIgnoreList();
                  sendMessage("Added " + playerToAdd + " to the ignore list.");
               } else {
                  sendMessage(playerToAdd + " is already on the ignore list.");
               }
         }

      }
   }

   @SubscribeEvent(
      priority = EventPriority.HIGHEST
   )
   public void onChatReceived(ClientChatReceivedEvent event) {
      if (event.getMessage() != null) {
         String messageText = event.getMessage().getUnformattedText();
         String extractedName = null;
         if (messageText.startsWith("<") && messageText.contains(">")) {
            extractedName = messageText.substring(1, messageText.indexOf(">"));
         } else if (messageText.contains(" whispers")) {
            extractedName = messageText.split(" ")[0];
         }

         if (extractedName != null) {
            for(String ignoredPlayer : ignoreList) {
               if (extractedName.equalsIgnoreCase(ignoredPlayer)) {
                  event.setCanceled(true);
                  return;
               }
            }
         }

      }
   }

   private void loadIgnoreList() {
      File file = new File("vonware/ignored.json");
      if (file.exists()) {
         try {
            Reader reader = new FileReader(file);
            Type type = (new TypeToken<HashSet<String>>() {
            }).getType();
            Set<String> loaded = (Set)GSON.fromJson(reader, type);
            if (loaded != null) {
               ignoreList.addAll(loaded);
            }

            reader.close();
         } catch (Exception e) {
            e.printStackTrace();
         }

      }
   }

   private void saveIgnoreList() {
      try {
         File file = new File("vonware/ignored.json");
         file.getParentFile().mkdirs();
         if (!file.exists()) {
            file.createNewFile();
         }

         Writer writer = new FileWriter(file);
         GSON.toJson(ignoreList, writer);
         writer.close();
      } catch (Exception e) {
         e.printStackTrace();
      }

   }

   public static Set<String> getIgnoreList() {
      return ignoreList;
   }
}
