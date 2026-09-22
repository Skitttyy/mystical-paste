package fat.vonware.manager;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import fat.vonware.Vonware;
import fat.vonware.features.Feature;
import fat.vonware.features.command.Command;
import fat.vonware.features.command.commands.IgnoreCommand;
import fat.vonware.features.modules.Module;
import fat.vonware.features.modules.render.WayPoints;
import fat.vonware.features.setting.Setting;
import fat.vonware.util.OtherRenderShit.WayPoint;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.minecraft.client.network.NetworkPlayerInfo;

public class PredictionManager extends Feature {
   private static final String[] ALL_KEY_NAMES = new String[]{"none", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "F1", "F2", "F3", "F4", "F5", "F6", "F7", "F8", "F9", "F10", "F11", "F12", "LSHIFT", "RSHIFT", "LCONTROL", "RCONTROL", "LMENU", "RMENU", "TAB", "GRAVE", "BACK", "DELETE", "HOME", "END", "INSERT", "PRIOR", "NEXT", "RETURN", "ESCAPE", "SPACE", "UP", "DOWN", "LEFT", "RIGHT", "NUMPAD0", "NUMPAD1", "NUMPAD2", "NUMPAD3", "NUMPAD4", "NUMPAD5", "NUMPAD6", "NUMPAD7", "NUMPAD8", "NUMPAD9", "ADD", "SUBTRACT", "MULTIPLY", "DIVIDE", "DECIMAL", "NUMPADENTER", "MINUS", "EQUALS", "LBRACKET", "RBRACKET", "SEMICOLON", "APOSTROPHE", "BACKSLASH", "COMMA", "PERIOD", "SLASH"};
   private List<String> currentSuggestions = new ArrayList();
   private int selectedIndex = 0;
   private String currentInput = "";

   public PredictionManager() {
      super("Prediction");
   }

   public void updateSuggestions(String input) {
      this.currentInput = input;
      this.currentSuggestions.clear();
      this.selectedIndex = 0;
      String prefix = Vonware.commandManager.getPrefix();
      if (input.startsWith(prefix)) {
         String withoutPrefix = input.substring(prefix.length());
         String[] parts = withoutPrefix.split(" ", -1);
         if (parts.length != 0) {
            if (parts.length == 1) {
               this.suggestCommands(parts[0].toLowerCase());
            } else {
               this.suggestArguments(parts, prefix);
            }

            if (this.currentSuggestions.size() > 10) {
               this.currentSuggestions = this.currentSuggestions.subList(0, 10);
            }

         }
      }
   }

   public List<String> getSuggestions() {
      return this.currentSuggestions;
   }

   public int getSelectedIndex() {
      return this.selectedIndex;
   }

   public String getCurrentInput() {
      return this.currentInput;
   }

   public void incrementSelection() {
      if (!this.currentSuggestions.isEmpty()) {
         this.selectedIndex = (this.selectedIndex + 1) % this.currentSuggestions.size();
      }

   }

   public void decrementSelection() {
      if (!this.currentSuggestions.isEmpty()) {
         --this.selectedIndex;
         if (this.selectedIndex < 0) {
            this.selectedIndex = this.currentSuggestions.size() - 1;
         }
      }

   }

   public String getSelectedSuggestion() {
      return !this.currentSuggestions.isEmpty() && this.selectedIndex < this.currentSuggestions.size() ? (String)this.currentSuggestions.get(this.selectedIndex) : null;
   }

   public void reset() {
      this.currentSuggestions.clear();
      this.selectedIndex = 0;
      this.currentInput = "";
   }

   private void suggestCommands(String partial) {
      String prefix = Vonware.commandManager.getPrefix();

      for(Command cmd : Vonware.commandManager.getCommands()) {
         if (cmd.getName().toLowerCase().startsWith(partial)) {
            this.add(prefix + cmd.getName());
         }
      }

   }

   private void suggestArguments(String[] parts, String prefix) {
      String commandName = parts[0].toLowerCase();
      String lastArg = parts[parts.length - 1].toLowerCase();
      switch (commandName) {
         case "friend":
            if (parts.length == 2) {
               this.matchAndAdd(prefix + "friend add ", "add", lastArg);
               this.matchAndAdd(prefix + "friend del ", "del", lastArg);
               this.matchAndAdd(prefix + "friend list", "list", lastArg);
               this.matchAndAdd(prefix + "friend reset", "reset", lastArg);
            } else if (parts.length == 3) {
               if (parts[1].equalsIgnoreCase("add")) {
                  this.suggestPlayersNotFriended(prefix + "friend add ", lastArg);
               } else if (parts[1].equalsIgnoreCase("del")) {
                  this.suggestFriendNames(prefix + "friend del ", lastArg);
               }
            }
            break;
         case "add":
            if (parts.length == 2) {
               this.suggestPlayersNotFriended(prefix + "add ", lastArg);
            }
            break;
         case "remove":
            if (parts.length == 2) {
               this.suggestFriendNames(prefix + "remove ", lastArg);
            }
            break;
         case "bind":
            if (parts.length == 2) {
               this.suggestModuleNames(prefix + "bind ", lastArg);
            } else if (parts.length == 3) {
               this.suggestKeys(prefix + "bind " + parts[1] + " ", lastArg);
            }
            break;
         case "toggle":
         case "t":
            if (parts.length == 2) {
               this.suggestModuleNames(prefix + commandName + " ", lastArg);
            } else if (parts.length == 3) {
               this.matchAndAdd(prefix + commandName + " " + parts[1] + " on", "on", lastArg);
               this.matchAndAdd(prefix + commandName + " " + parts[1] + " off", "off", lastArg);
            }
            break;
         case "rename":
            if (parts.length == 2) {
               this.matchAndAdd(prefix + "rename resetname ", "resetname", lastArg);
               this.suggestModuleDisplayNames(prefix + "rename ", lastArg);
            } else if (parts.length == 3 && parts[1].equalsIgnoreCase("resetname")) {
               this.suggestModuleDisplayNames(prefix + "rename resetname ", lastArg);
            }
            break;
         case "config":
            if (parts.length == 2) {
               this.matchAndAdd(prefix + "config save ", "save", lastArg);
               this.matchAndAdd(prefix + "config load ", "load", lastArg);
               this.matchAndAdd(prefix + "config delete ", "delete", lastArg);
               this.matchAndAdd(prefix + "config list", "list", lastArg);
               this.matchAndAdd(prefix + "config folder", "folder", lastArg);
            } else if (parts.length == 3 && (parts[1].equalsIgnoreCase("save") || parts[1].equalsIgnoreCase("load") || parts[1].equalsIgnoreCase("delete"))) {
               this.suggestJsonFiles("vonware/configs/", prefix + "config " + parts[1] + " ", lastArg);
            }
            break;
         case "colorpreset":
            if (parts.length == 2) {
               this.matchAndAdd(prefix + "colorpreset save ", "save", lastArg);
               this.matchAndAdd(prefix + "colorpreset load ", "load", lastArg);
               this.matchAndAdd(prefix + "colorpreset delete ", "delete", lastArg);
               this.matchAndAdd(prefix + "colorpreset list", "list", lastArg);
            } else if (parts.length == 3 && (parts[1].equalsIgnoreCase("save") || parts[1].equalsIgnoreCase("load") || parts[1].equalsIgnoreCase("delete"))) {
               this.suggestJsonFiles("vonware/color-presets/", prefix + "colorpreset " + parts[1] + " ", lastArg);
            }
            break;
         case "color":
            if (parts.length == 2) {
               this.matchAndAdd(prefix + "color RGB", "rgb", lastArg);
               this.matchAndAdd(prefix + "color HSV", "hsv", lastArg);
               this.matchAndAdd(prefix + "color HSL", "hsl", lastArg);
               this.matchAndAdd(prefix + "color HEX", "hex", lastArg);
            }
            break;
         case "prefix":
            if (parts.length == 2) {
               for(String sym : new String[]{".", ",", ";", "-", "!", "?", "*", "+", "~", "#"}) {
                  if (lastArg.isEmpty() || sym.startsWith(lastArg)) {
                     this.add(prefix + "prefix " + sym);
                  }
               }
            }
            break;
         case "coords":
            if (parts.length == 2) {
               this.suggestPlayerNames(prefix + "coords ", lastArg);
            }
            break;
         case "publiccoords":
         case "pc":
            if (parts.length == 2) {
               this.suggestPlayerNames(prefix + commandName + " ", lastArg);
            }
            break;
         case "replycoords":
         case "rc":
            if (parts.length == 2) {
               this.suggestPlayerNames(prefix + commandName + " ", lastArg);
            }
            break;
         case "ignore":
            if (parts.length == 2) {
               this.matchAndAdd(prefix + "ignore remove ", "remove", lastArg);
               this.matchAndAdd(prefix + "ignore list", "list", lastArg);
               this.suggestPlayerNames(prefix + "ignore ", lastArg);
            } else if (parts.length == 3 && parts[1].equalsIgnoreCase("remove")) {
               this.suggestIgnoredPlayers(prefix + "ignore remove ", lastArg);
            }
            break;
         case "kit":
            if (parts.length == 2) {
               this.matchAndAdd(prefix + "kit list", "list", lastArg);
               this.matchAndAdd(prefix + "kit create ", "create", lastArg);
               this.matchAndAdd(prefix + "kit save ", "save", lastArg);
               this.matchAndAdd(prefix + "kit set ", "set", lastArg);
               this.matchAndAdd(prefix + "kit del ", "del", lastArg);
               this.matchAndAdd(prefix + "kit delete ", "delete", lastArg);
            } else if (parts.length == 3) {
               boolean needsExisting = parts[1].equalsIgnoreCase("set") || parts[1].equalsIgnoreCase("del") || parts[1].equalsIgnoreCase("delete") || parts[1].equalsIgnoreCase("save");
               if (needsExisting) {
                  this.suggestKitNames(prefix + "kit " + parts[1] + " ", lastArg);
               }
            }
            break;
         case "waypoint":
            if (parts.length == 2) {
               this.matchAndAdd(prefix + "waypoint add ", "add", lastArg);
               this.matchAndAdd(prefix + "waypoint remove ", "remove", lastArg);
               this.matchAndAdd(prefix + "waypoint list", "list", lastArg);
               this.matchAndAdd(prefix + "waypoint clear", "clear", lastArg);
            } else if (parts.length == 3 && parts[1].equalsIgnoreCase("remove")) {
               this.suggestWaypointNames(prefix + "waypoint remove ", lastArg);
            }
            break;
         case "lookup":
         case "names":
            if (parts.length == 2) {
               this.suggestPlayerNames(prefix + commandName + " ", lastArg);
            }
            break;
         case "help":
            if (parts.length == 2) {
               for(Command cmd : Vonware.commandManager.getCommands()) {
                  if (cmd.getName().toLowerCase().startsWith(lastArg)) {
                     this.add(prefix + "help " + cmd.getName());
                  }
               }
            }
            break;
         default:
            this.suggestModuleCommand(commandName, parts, lastArg, prefix);
      }

   }

   private void suggestModuleCommand(String commandName, String[] parts, String lastArg, String prefix) {
      Module module = Vonware.moduleManager.getModuleByName(commandName);
      if (module != null) {
         if (parts.length == 2) {
            this.matchAndAdd(prefix + commandName + " reset", "reset", lastArg);

            for(Setting setting : module.getSettings()) {
               if (!this.isInternalSetting(setting.getName()) && setting.getName().toLowerCase().startsWith(lastArg)) {
                  String hint = this.buildSettingHint(setting);
                  this.add(prefix + commandName + " " + setting.getName() + hint);
               }
            }
         } else if (parts.length == 3) {
            Setting setting = module.getSettingByName(parts[1]);
            if (setting == null) {
               return;
            }

            String type = setting.getType();
            if (type.equalsIgnoreCase("Boolean")) {
               this.matchAndAdd(prefix + commandName + " " + parts[1] + " true", "true", lastArg);
               this.matchAndAdd(prefix + commandName + " " + parts[1] + " false", "false", lastArg);
            } else if (type.equalsIgnoreCase("Enum")) {
               try {
                  Object value = setting.getValue();
                  if (value != null && value.getClass().isEnum()) {
                     for(Enum<?> e : (Enum[])value.getClass().getEnumConstants()) {
                        if (e.name().toLowerCase().startsWith(lastArg)) {
                           boolean isCurrent = e.equals(value);
                           String label = prefix + commandName + " " + parts[1] + " " + e.name() + (isCurrent ? " [current]" : "");
                           this.add(label);
                        }
                     }
                  }
               } catch (Exception var16) {
               }
            } else if (type.equalsIgnoreCase("Double") || type.equalsIgnoreCase("Float") || type.equalsIgnoreCase("Integer")) {
               try {
                  this.add(prefix + commandName + " " + parts[1] + " " + setting.getValue());
               } catch (Exception var15) {
               }
            }
         }

      }
   }

   private void matchAndAdd(String full, String candidate, String partial) {
      if (candidate.toLowerCase().startsWith(partial)) {
         this.add(full);
      }

   }

   private void add(String suggestion) {
      if (!this.currentSuggestions.contains(suggestion)) {
         this.currentSuggestions.add(suggestion);
      }

   }

   private List<String> getPlayerNames() {
      return (List<String>)(mc.getConnection() == null ? new ArrayList() : (List)mc.getConnection().getPlayerInfoMap().stream().map(NetworkPlayerInfo::getGameProfile).map(GameProfile::getName).sorted().collect(Collectors.toList()));
   }

   private void suggestPlayerNames(String base, String partial) {
      for(String name : this.getPlayerNames()) {
         if (name.toLowerCase().startsWith(partial)) {
            this.add(base + name);
         }
      }

   }

   private void suggestPlayersNotFriended(String base, String partial) {
      List<String> friends = this.getFriendUsernames();

      for(String name : this.getPlayerNames()) {
         if (name.toLowerCase().startsWith(partial) && !friends.contains(name.toLowerCase())) {
            this.add(base + name);
         }
      }

   }

   public void setSelectedIndex(int index) {
      if (index >= 0 && index < this.currentSuggestions.size()) {
         this.selectedIndex = index;
      }

   }

   private void suggestFriendNames(String base, String partial) {
      List<String> onlineNames = (List)this.getPlayerNames().stream().map(String::toLowerCase).collect(Collectors.toList());

      for(FriendManager.Friend friend : Vonware.friendManager.getFriends()) {
         try {
            String username = friend.getUsername();
            if (onlineNames.contains(username.toLowerCase()) && username.toLowerCase().startsWith(partial)) {
               this.add(base + username);
            }
         } catch (Exception var7) {
         }
      }

   }

   private List<String> getFriendUsernames() {
      List<String> names = new ArrayList();

      for(FriendManager.Friend f : Vonware.friendManager.getFriends()) {
         try {
            names.add(f.getUsername().toLowerCase());
         } catch (Exception var5) {
         }
      }

      return names;
   }

   private void suggestIgnoredPlayers(String base, String partial) {
      try {
         for(String ignored : IgnoreCommand.getIgnoreList()) {
            if (ignored.toLowerCase().startsWith(partial)) {
               this.add(base + ignored);
            }
         }
      } catch (Exception var5) {
         this.suggestPlayerNames(base, partial);
      }

   }

   private void suggestModuleNames(String base, String partial) {
      for(Module m : Vonware.moduleManager.getModules()) {
         if (m.getName().toLowerCase().startsWith(partial)) {
            this.add(base + m.getName());
         }
      }

   }

   private void suggestModuleDisplayNames(String base, String partial) {
      for(Module m : Vonware.moduleManager.getModules()) {
         if (m.getDisplayName().toLowerCase().startsWith(partial)) {
            this.add(base + m.getDisplayName());
         }
      }

   }

   private void suggestKeys(String base, String partial) {
      for(String key : ALL_KEY_NAMES) {
         if (key.toLowerCase().startsWith(partial)) {
            this.add(base + key);
         }
      }

   }

   private void suggestJsonFiles(String folderPath, String base, String partial) {
      File folder = new File(folderPath);
      if (folder.exists() && folder.isDirectory()) {
         File[] files = folder.listFiles((dir, namex) -> namex.endsWith(".json"));
         if (files != null) {
            Arrays.sort(files);

            for(File file : files) {
               String name = file.getName().replace(".json", "");
               if (name.toLowerCase().startsWith(partial)) {
                  this.add(base + name);
               }
            }

         }
      }
   }

   private void suggestKitNames(String base, String partial) {
      File kitsFile = new File("vonware/regear.json");
      if (kitsFile.exists()) {
         try {
            JsonParser parser = new JsonParser();
            JsonObject obj = parser.parse(new FileReader(kitsFile)).getAsJsonObject();
            List<String> names = new ArrayList();

            for(Map.Entry<String, JsonElement> entry : obj.entrySet()) {
               names.add(entry.getKey());
            }

            names.sort(String.CASE_INSENSITIVE_ORDER);

            for(String key : names) {
               if (!key.equals("pointer") && key.toLowerCase().startsWith(partial)) {
                  this.add(base + key);
               }
            }
         } catch (Exception var9) {
         }

      }
   }

   private void suggestWaypointNames(String base, String partial) {
      try {
         for(WayPoint wp : WayPoints.getManager().getWaypoints()) {
            if (wp.getName().toLowerCase().startsWith(partial)) {
               this.add(base + wp.getName());
            }
         }
      } catch (Exception var5) {
      }

   }

   private boolean isInternalSetting(String name) {
      String low = name.toLowerCase();
      return low.equals("enabled") || low.equals("visible") || low.equals("keybind") || low.equals("displayname");
   }

   private String buildSettingHint(Setting setting) {
      try {
         Object val = setting.getValue();
         if (val == null) {
            return "";
         }

         String type = setting.getType();
         if (type.equalsIgnoreCase("Boolean")) {
            return " [" + val + "]";
         }

         if (type.equalsIgnoreCase("Enum")) {
            return " [" + val.toString() + "]";
         }

         if (type.equalsIgnoreCase("Double") || type.equalsIgnoreCase("Float") || type.equalsIgnoreCase("Integer")) {
            return " [" + val + "]";
         }
      } catch (Exception var4) {
      }

      return "";
   }
}
