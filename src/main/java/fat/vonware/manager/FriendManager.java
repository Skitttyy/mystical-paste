package fat.vonware.manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import fat.vonware.Vonware;
import fat.vonware.features.Feature;
import fat.vonware.features.command.Command;
import fat.vonware.features.modules.client.Manager;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.player.EntityPlayer;

public class FriendManager extends Feature {
   private List<Friend> friends = new ArrayList();
   private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().create();
   private static final String FRIENDS_FILE = "vonware/friends.json";

   public FriendManager() {
      super("Friends");
   }

   public boolean isAliasSameAsLabel(String name) {
      Friend friend = this.getFriend(name);
      return friend != null && friend.getUsername().equalsIgnoreCase(name);
   }

   public boolean isFriend(String name) {
      this.cleanFriends();
      return this.friends.stream().anyMatch((friend) -> friend.username.equalsIgnoreCase(name));
   }

   public boolean isFriend(NetworkPlayerInfo info) {
      return Vonware.friendManager.isFriend(info.getGameProfile().getName());
   }

   public boolean isFriend(EntityPlayer player) {
      return this.isFriend(player.getName());
   }

   public boolean isNotFriend(String name) {
      return !this.isFriend(name);
   }

   public Friend getFriend(String label) {
      for(Friend friend : this.friends) {
         if (friend.getUsername().equalsIgnoreCase(label)) {
            return friend;
         }
      }

      return null;
   }

   public void addFriend(String name) {
      if (this.isFriend(name)) {
         Command.sendMessage(name + " is already friended!");
      } else {
         this.commitFriend(new Friend(name));
      }
   }

   private void commitFriend(Friend friend) {
      this.friends.add(friend);
      Manager.getInstance().alert(friend.getUsername(), false);
      this.saveFriends();
   }

   public void removeFriend(String name) {
      this.cleanFriends();

      for(Friend friend : this.friends) {
         if (friend.getUsername().equalsIgnoreCase(name)) {
            this.friends.remove(friend);
            Manager.getInstance().alert(name, true);
            this.saveFriends();
            break;
         }
      }

   }

   public void onLoad() {
      this.friends = new ArrayList();
      this.loadFriends();
   }

   public void saveFriends() {
      this.cleanFriends();

      try {
         File file = new File("vonware/friends.json");
         if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
         }

         Writer writer = new FileWriter(file);
         Throwable var3 = null;

         try {
            GSON.toJson(this.friends, writer);
         } catch (Throwable var13) {
            var3 = var13;
            throw var13;
         } finally {
            if (writer != null) {
               if (var3 != null) {
                  try {
                     writer.close();
                  } catch (Throwable var12) {
                     var3.addSuppressed(var12);
                  }
               } else {
                  writer.close();
               }
            }

         }
      } catch (IOException e) {
         Vonware.LOGGER.error("Failed to save friends: " + e.getMessage());
      }

   }

   public void loadFriends() {
      try {
         File file = new File("vonware/friends.json");
         if (!file.exists()) {
            return;
         }

         String json = new String(Files.readAllBytes(Paths.get("vonware/friends.json")));
         Type listType = (new TypeToken<List<Friend>>() {
         }).getType();
         List<Friend> loadedFriends = (List)GSON.fromJson(json, listType);
         if (loadedFriends != null) {
            this.friends.clear();
            this.friends.addAll(loadedFriends);
         }
      } catch (IOException e) {
         Vonware.LOGGER.error("Failed to load friends: " + e.getMessage());
      }

   }

   public void cleanFriends() {
      this.friends = (List)this.friends.stream().filter(Objects::nonNull).filter((friend) -> friend.getUsername() != null).collect(Collectors.toList());
   }

   public List<Friend> getFriends() {
      this.cleanFriends();
      return this.friends;
   }

   public void addFriend(Friend friend) {
      this.friends.add(friend);
   }

   public static class Friend {
      private final String username;

      public Friend(String username) {
         this.username = username;
      }

      public String getUsername() {
         return this.username;
      }
   }
}
