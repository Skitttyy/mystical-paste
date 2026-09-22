package fat.vonware.features.command.commands.colors;

import com.mojang.realmsclient.gui.ChatFormatting;
import fat.vonware.features.command.Command;
import fat.vonware.features.modules.client.Colors;
import fat.vonware.util.Animations.render.ColorUtil;
import java.awt.Color;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;

public class ColorCommand extends Command {
   public ColorCommand() {
      super("color", new String[]{"<RGB/HSL/HEX/HSV>"});
   }

   public void execute(String[] commands) {
      if (commands.length < 2) {
         sendMessage(ChatFormatting.RED + "Usage: " + ChatFormatting.GRAY + "color <RGB/HSL/HEX/HSV>");
      } else {
         String mode = commands[0].toLowerCase();
         Color globalColor = Colors.getInstance().getGuiColor();

         try {
            switch (mode) {
               case "rgb":
                  String rgb = globalColor.getRed() + ", " + globalColor.getGreen() + ", " + globalColor.getBlue();
                  this.copyToClipboard(rgb);
                  sendMessage(ChatFormatting.GREEN + "RGB: " + ChatFormatting.WHITE + rgb + ChatFormatting.GRAY + " (Copied to clipboard)");
                  break;
               case "hsl":
                  float[] hsl = ColorUtil.rgbToHsl(globalColor);
                  int h = Math.round(hsl[0]);
                  int s = Math.round(hsl[1]);
                  int l = Math.round(hsl[2]);
                  String hslString = h + "\u00b0, " + s + "%, " + l + "%";
                  this.copyToClipboard(hslString);
                  sendMessage(ChatFormatting.GREEN + "HSL: " + ChatFormatting.WHITE + hslString + ChatFormatting.GRAY + " (Copied to clipboard)");
                  break;
               case "hex":
                  String hex = String.format("#%02X%02X%02X", globalColor.getRed(), globalColor.getGreen(), globalColor.getBlue());
                  this.copyToClipboard(hex);
                  sendMessage(ChatFormatting.GREEN + "HEX: " + ChatFormatting.WHITE + hex + ChatFormatting.GRAY + " (Copied to clipboard)");
                  break;
               case "hsv":
                  float[] hsv = ColorUtil.rgbToHsv(globalColor);
                  int hue = Math.round(hsv[0]);
                  int sat = Math.round(hsv[1] * 100.0F);
                  int val = Math.round(hsv[2] * 100.0F);
                  String hsvString = hue + "\u00b0, " + sat + "%, " + val + "%";
                  this.copyToClipboard(hsvString);
                  sendMessage(ChatFormatting.GREEN + "HSV: " + ChatFormatting.WHITE + hsvString + ChatFormatting.GRAY + " (Copied to clipboard)");
                  break;
               default:
                  sendMessage(ChatFormatting.RED + "Invalid format. Use: " + ChatFormatting.GRAY + "RGB, HSL, HEX, or HSV");
            }
         } catch (Exception e) {
            sendMessage(ChatFormatting.RED + "Error getting color: " + e.getMessage());
            e.printStackTrace();
         }

      }
   }

   private void copyToClipboard(String text) {
      try {
         StringSelection selection = new StringSelection(text);
         Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
         clipboard.setContents(selection, selection);
      } catch (Exception e) {
         e.printStackTrace();
      }

   }
}
