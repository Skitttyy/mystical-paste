package fat.vonware.features.command.commands;

import com.mojang.realmsclient.gui.ChatFormatting;
import fat.vonware.features.command.Command;
import fat.vonware.util.Animations.game.ReplyWaypointRenderer;
import java.awt.Color;
import net.minecraft.client.Minecraft;

public class ShowWaypointCommand extends Command {
   public ShowWaypointCommand() {
      super("showwaypoint", new String[]{"<label> <x> <y> <z>"});
   }

   public void execute(String[] args) {
      if (args.length < 4) {
         sendMessage(ChatFormatting.RED + "Usage: showwaypoint <label> <x> <y> <z>");
      } else {
         Minecraft mc = Minecraft.getMinecraft();
         if (mc.player != null && mc.world != null) {
            try {
               String label = args[0];
               double wx = Double.parseDouble(args[1]);
               double wy = Double.parseDouble(args[2]);
               double wz = Double.parseDouble(args[3]);
               double dx = wx + (double)0.5F - mc.player.posX;
               double dy = wy - mc.player.posY;
               double dz = wz + (double)0.5F - mc.player.posZ;
               double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
               ReplyWaypointRenderer.add(new ReplyWaypointRenderer.ReplyWaypoint(label, wx, wy, wz, new Color(220, 60, 60), dist));
            } catch (NumberFormatException var18) {
            }

         }
      }
   }
}
