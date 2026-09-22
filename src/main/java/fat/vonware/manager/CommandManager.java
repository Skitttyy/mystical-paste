package fat.vonware.manager;

import com.mojang.realmsclient.gui.ChatFormatting;
import fat.vonware.Global;
import fat.vonware.Vonware;
import fat.vonware.features.Feature;
import fat.vonware.features.command.Command;
import fat.vonware.features.command.commands.ConfigCommand;
import fat.vonware.features.command.commands.CoordsCommand;
import fat.vonware.features.command.commands.HelpCommand;
import fat.vonware.features.command.commands.IgnoreCommand;
import fat.vonware.features.command.commands.KitCommand;
import fat.vonware.features.command.commands.ModuleCommand;
import fat.vonware.features.command.commands.PrefixCommand;
import fat.vonware.features.command.commands.PublicCoordsCommand;
import fat.vonware.features.command.commands.RegearCommand;
import fat.vonware.features.command.commands.ReloadSoundCommand;
import fat.vonware.features.command.commands.ReplyCoordsCommand;
import fat.vonware.features.command.commands.ServerIPCommand;
import fat.vonware.features.command.commands.ShowWaypointCommand;
import fat.vonware.features.command.commands.SwitchPacketFlyCommand;
import fat.vonware.features.command.commands.client.BindCommand;
import fat.vonware.features.command.commands.client.OpenFolderCommand;
import fat.vonware.features.command.commands.client.RenameCommand;
import fat.vonware.features.command.commands.colors.ColorCommand;
import fat.vonware.features.command.commands.colors.ColorsPresetCommand;
import fat.vonware.features.command.commands.friend.AddCommand;
import fat.vonware.features.command.commands.friend.FriendCommand;
import fat.vonware.features.command.commands.friend.RemoveCommand;
import fat.vonware.features.command.commands.misc.ClearHistoryCommand;
import fat.vonware.features.command.commands.misc.FOVChangerCommand;
import fat.vonware.features.command.commands.player.FakePlayerCommand;
import fat.vonware.features.command.commands.player.TimerFixCommand;
import fat.vonware.features.command.commands.player.WayPointsCommand;
import fat.vonware.features.modules.Module;
import java.util.ArrayList;
import java.util.LinkedList;

public class CommandManager extends Feature {
   private final ArrayList<Command> commands = new ArrayList();
   private String clientMessage = "";
   private String prefix = "*";

   public CommandManager() {
      super("Command");
      this.commands.add(new BindCommand());
      this.commands.add(new PrefixCommand());
      this.commands.add(new ConfigCommand());
      this.commands.add(new FriendCommand());
      this.commands.add(new AddCommand());
      this.commands.add(new RemoveCommand());
      this.commands.add(new HelpCommand());
      this.commands.add(new KitCommand());
      this.commands.add(new ReloadSoundCommand());
      this.commands.add(new FOVChangerCommand());
      this.commands.add(new CoordsCommand());
      this.commands.add(new PublicCoordsCommand());
      this.commands.add(new ReplyCoordsCommand());
      this.commands.add(new IgnoreCommand());
      this.commands.add(new SwitchPacketFlyCommand());
      this.commands.add(new ServerIPCommand());
      this.commands.add(new ClearHistoryCommand());
      this.commands.add(new RegearCommand());
      this.commands.add(new ColorCommand());
      this.commands.add(new ColorsPresetCommand());
      this.commands.add(new TimerFixCommand());
      this.commands.add(new WayPointsCommand());
      this.commands.add(new OpenFolderCommand());
      this.commands.add(new ShowWaypointCommand());
      this.commands.add(new FakePlayerCommand());
      this.commands.add(new RenameCommand());
   }

   public void registerModuleCommands() {
      for(Module module : Vonware.moduleManager.getModules()) {
         this.commands.add(new ModuleCommand(module));
      }

   }

   public static String[] removeElement(String[] input, int indexToDelete) {
      LinkedList<String> result = new LinkedList();

      for(int i = 0; i < input.length; ++i) {
         if (i != indexToDelete) {
            result.add(input[i]);
         }
      }

      return (String[])result.toArray(input);
   }

   private static String strip(String str, String key) {
      return str.startsWith(key) && str.endsWith(key) ? str.substring(key.length(), str.length() - key.length()) : str;
   }

   public void executeCommand(String command) {
      String[] parts = command.split(" (?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
      String name = parts[0].substring(1);
      String[] args = removeElement(parts, 0);

      for(int i = 0; i < args.length; ++i) {
         if (args[i] != null) {
            args[i] = strip(args[i], "\"");
         }
      }

      for(Command c : this.commands) {
         if (c.getName().equalsIgnoreCase(name)) {
            c.execute(parts);
            return;
         }
      }

      Command.sendMessage(ChatFormatting.GRAY + "Command not found, type 'help' for the commands list.");
   }

   public ArrayList<Command> getCommands() {
      return this.commands;
   }

   public String getClientMessage() {
      return Global.getClientName();
   }

   public void setClientMessage(String clientMessage) {
      this.clientMessage = clientMessage;
   }

   public String getPrefix() {
      return this.prefix;
   }

   public void setPrefix(String prefix) {
      this.prefix = prefix;
   }
}
