package fat.vonware.manager;

import fat.vonware.features.modules.client.Colors;
import java.io.File;
import net.minecraft.client.Minecraft;

public class FutureManager {
   private static final Minecraft mc = Minecraft.getMinecraft();

   public static boolean doesFutureExist() {
      File futureDirectory = new File(System.getProperty("user.home"), "Future");
      return futureDirectory.exists() && futureDirectory.isDirectory();
   }

   public static void addFriend(String playerName) {
      Colors colors = Colors.getInstance();
      if ((Boolean)colors.futureFriendSync.getValue() && doesFutureExist() && mc.player != null) {
         String prefix = colors.futurePrefix.getValue();
         mc.player.sendChatMessage(prefix + "add " + playerName);
      }
   }

   public static void removeFriend(String playerName) {
      Colors colors = Colors.getInstance();
      if ((Boolean)colors.futureFriendSync.getValue() && doesFutureExist() && mc.player != null) {
         String prefix = colors.futurePrefix.getValue();
         mc.player.sendChatMessage(prefix + "remove " + playerName);
      }
   }
}
