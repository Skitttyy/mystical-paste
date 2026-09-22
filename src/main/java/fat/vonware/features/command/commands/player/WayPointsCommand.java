package fat.vonware.features.command.commands.player;

import com.mojang.realmsclient.gui.ChatFormatting;
import fat.vonware.Global;
import fat.vonware.features.command.Command;
import fat.vonware.features.modules.render.WayPoints;
import fat.vonware.manager.WayPointsManager;
import fat.vonware.util.OtherRenderShit.WayPoint;
import java.util.List;
import net.minecraft.client.Minecraft;

public class WayPointsCommand extends Command {
   public WayPointsCommand() {
      super("waypoint", new String[]{"<add/remove/list/clear> [name] [x y z]"});
   }

   public void execute(String[] commands) {
      Minecraft mc = Minecraft.getMinecraft();
      WayPointsManager manager = WayPoints.getManager();
      if (commands.length == 1) {
         sendMessage(ChatFormatting.GRAY + "Usage:");
         sendMessage(ChatFormatting.GRAY + "  waypoint add <name>" + ChatFormatting.DARK_GRAY + " \u2014 current position");
         sendMessage(ChatFormatting.GRAY + "  waypoint add <name> <x> <y> <z>" + ChatFormatting.DARK_GRAY + " \u2014 specific position");
         sendMessage(ChatFormatting.GRAY + "  waypoint remove <name>");
         sendMessage(ChatFormatting.GRAY + "  waypoint list");
         sendMessage(ChatFormatting.GRAY + "  waypoint clear");
      } else if (commands.length == 2) {
         if ("list".equalsIgnoreCase(commands[0])) {
            this.listWaypoints(manager);
         } else if ("clear".equalsIgnoreCase(commands[0])) {
            int count = manager.getWaypoints().size();
            manager.clear();
            sendMessage(ChatFormatting.RED + "Cleared " + count + " waypoint" + (count == 1 ? "" : "s") + ".");
         } else {
            sendMessage(ChatFormatting.RED + "Unknown subcommand. Use: add, remove, list, clear");
         }

      } else if (commands.length != 3 && commands.length != 4) {
         if (commands.length >= 6 && "add".equalsIgnoreCase(commands[0])) {
            try {
               int x = Integer.parseInt(commands[2]);
               int y = Integer.parseInt(commands[3]);
               int z = Integer.parseInt(commands[4]);
               this.handleAdd(manager, mc, commands[1], x, y, z);
            } catch (NumberFormatException var7) {
               sendMessage(ChatFormatting.RED + "Invalid coordinates. Use whole numbers.");
            }

         } else {
            sendMessage(ChatFormatting.RED + "Unknown subcommand. Use: add, remove, list, clear");
         }
      } else {
         String sub = commands[0];
         String name = commands[1];
         if ("add".equalsIgnoreCase(sub)) {
            if (commands.length == 4 && !"pos".equalsIgnoreCase(commands[2])) {
               sendMessage(ChatFormatting.RED + "Usage: waypoint add <name> OR waypoint add <name> <x> <y> <z>");
            } else {
               this.handleAdd(manager, mc, name, (int)mc.player.posX, (int)mc.player.posY, (int)mc.player.posZ);
            }
         } else if ("remove".equalsIgnoreCase(sub)) {
            if (manager.remove(name)) {
               sendMessage(ChatFormatting.RED + "Removed waypoint " + Global.getGlobalColorCode() + name);
            } else {
               sendMessage(ChatFormatting.RED + "Waypoint '" + name + "' not found.");
            }

         } else {
            sendMessage(ChatFormatting.RED + "Unknown subcommand. Use: add, remove, list, clear");
         }
      }
   }

   private boolean handleAdd(WayPointsManager manager, Minecraft mc, String name, int x, int y, int z) {
      if (manager.exists(name)) {
         sendMessage(ChatFormatting.RED + "A waypoint named '" + name + "' already exists.");
         return false;
      } else {
         WayPoint wp = new WayPoint(name, (double)x, (double)y, (double)z);
         manager.add(wp);
         sendMessage("Added waypoint " + Global.getGlobalColorCode() + name + ChatFormatting.RESET + ChatFormatting.GRAY + " at " + ChatFormatting.WHITE + x + ", " + y + ", " + z + ChatFormatting.GRAY + " [" + wp.getColor() + "]");
         return true;
      }
   }

   private void listWaypoints(WayPointsManager manager) {
      List<WayPoint> waypoints = manager.getWaypoints();
      if (waypoints.isEmpty()) {
         sendMessage("No waypoints saved.");
      } else {
         sendMessage(ChatFormatting.GRAY + "Waypoints (" + waypoints.size() + "):");

         for(WayPoint wp : waypoints) {
            sendMessage(ChatFormatting.WHITE + "  " + wp.getName() + ChatFormatting.GRAY + " \u2014 " + ChatFormatting.WHITE + (int)wp.getX() + ", " + (int)wp.getY() + ", " + (int)wp.getZ() + ChatFormatting.GRAY + " [" + wp.getColor() + "]");
         }

      }
   }
}
