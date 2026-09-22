package fat.vonware.features.modules.elements;

import com.mojang.realmsclient.gui.ChatFormatting;
import fat.vonware.Global;
import fat.vonware.Vonware;
import fat.vonware.event.events.Render2DEvent;
import fat.vonware.features.command.Command;
import fat.vonware.features.modules.HudModule;
import fat.vonware.features.modules.Module;
import fat.vonware.features.modules.client.ClickGui;
import fat.vonware.features.modules.client.Colors;
import fat.vonware.features.setting.Setting;
import fat.vonware.util.Animations.render.ColorUtil;
import fat.vonware.util.Animations.render.RenderMethods;
import java.awt.Color;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.Entity;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Mouse;

public class Coords extends HudModule {
   private float chatOffset = 0.0F;
   private boolean wasMousePressed = false;
   private final Setting<Brackets> brackets;
   private final Setting<Boolean> netherCoords;
   private final Setting<Boolean> direction;
   private final Setting<Boolean> futureDirection;
   private final Setting<Boolean> futureColor;
   private final Setting<Boolean> simpleCoords;

   public Coords() {
      super("Coords", "Draws coordinates", Module.Category.Elements, true, false, false, 100.0F, 50.0F);
      this.brackets = new Setting<Brackets>("BracketsColor", Coords.Brackets.GRAY, "Color of the brackets for arraylist and coordinates");
      this.netherCoords = new Setting<Boolean>("NetherCoords", true, "Draws our nether coordinates");
      this.direction = new Setting<Boolean>("Direction", true, "Draws the direction you are looking");
      this.futureDirection = new Setting<Boolean>("Future Direction", true, (v) -> (Boolean)this.direction.getValue());
      this.futureColor = new Setting<Boolean>("Future Color", false);
      this.simpleCoords = new Setting<Boolean>("Simple Coords", false);
      this.register(this.brackets);
      this.register(this.netherCoords);
      this.register(this.direction);
      this.register(this.futureDirection);
      this.register(this.futureColor);
      this.register(this.simpleCoords);
   }

   public boolean isAutoPositioned() {
      return true;
   }

   @SubscribeEvent
   public void onRender2D(Render2DEvent event) {
      Entity player = RenderMethods.getEntity();
      Color globalColor;
      if ((Boolean)Colors.getInstance().rainbow.getValue()) {
         if (Colors.getInstance().rainbowMode.getValue() == Colors.RainbowMode.Static) {
            globalColor = ColorUtil.rainbow(0);
         } else {
            globalColor = ColorUtil.rainbow(0);
         }
      } else if ((Boolean)ClickGui.getInstance().pulseColor.getValue()) {
         globalColor = Global.getPulseColor(Global.getGlobalColor(), 0);
      } else {
         globalColor = Global.getGlobalColor();
      }

      int globalColorInt = globalColor.getRGB() & 16777215;
      String xyzColor = (Boolean)this.futureColor.getValue() ? TextFormatting.GRAY.toString() : this.getInfoColour(false);
      String coordsString = xyzColor + "XYZ " + this.getInfoColour(true) + this.getRoundedDouble(player.posX) + ((Brackets)this.brackets.getValue()).getColor() + ", " + this.getInfoColour(true) + this.getRoundedDouble(player.posY) + ((Brackets)this.brackets.getValue()).getColor() + ", " + this.getInfoColour(true) + this.getRoundedDouble(player.posZ);
      if ((Boolean)this.netherCoords.getValue()) {
         coordsString = coordsString + ((Brackets)this.brackets.getValue()).getColor() + " [" + this.getInfoColour(true) + this.getRoundedDouble(this.getDimensionCoord(player.posX)) + ((Brackets)this.brackets.getValue()).getColor() + ", " + this.getInfoColour(true) + this.getRoundedDouble(this.getDimensionCoord(player.posZ)) + ((Brackets)this.brackets.getValue()).getColor() + "]";
      }

      String directionString = this.getDirectionString();
      boolean chatOpen = mc.ingameGUI.getChatGUI().getChatOpen();
      ScaledResolution sr = new ScaledResolution(mc);
      float x = 2.0F;
      float y = (float)sr.getScaledHeight() - 10.0F;
      float targetOffset = chatOpen ? 13.0F : 0.0F;
      this.chatOffset += (targetOffset - this.chatOffset) * 0.05F;
      y -= this.chatOffset;
      if ((Boolean)this.futureDirection.getValue()) {
         float directionY = y - 10.0F;
         Vonware.textManager.drawString(this.getFixedDirection() + directionString, x, directionY, globalColorInt, true);
         Vonware.textManager.drawString(coordsString, x, y, globalColorInt, true);
      } else if ((Boolean)this.direction.getValue()) {
         Vonware.textManager.drawString(coordsString + directionString, x, y, globalColorInt, true);
      } else {
         Vonware.textManager.drawString(coordsString, x, y, globalColorInt, true);
      }

      float coordsWidth = (float)Vonware.textManager.getStringWidth(coordsString);
      float coordsHeight = (float)Vonware.textManager.getFontHeight();
      if ((Boolean)this.futureDirection.getValue()) {
         float directionY = y - 10.0F;
         Vonware.textManager.drawString(this.getFixedDirection() + directionString, x, directionY, globalColorInt, true);
         Vonware.textManager.drawString(coordsString, x, y, globalColorInt, true);
      } else if ((Boolean)this.direction.getValue()) {
         Vonware.textManager.drawString(coordsString + directionString, x, y, globalColorInt, true);
      } else {
         Vonware.textManager.drawString(coordsString, x, y, globalColorInt, true);
      }

      this.handleMouseClick(x, y, coordsWidth, coordsHeight, player, chatOpen);
   }

   private void handleMouseClick(float x, float y, float width, float height, Entity player, boolean chatOpen) {
      if (chatOpen) {
         boolean mousePressed = Mouse.isButtonDown(0);
         if (mousePressed && !this.wasMousePressed) {
            ScaledResolution sr = new ScaledResolution(mc);
            int mouseX = Mouse.getX() * sr.getScaledWidth() / mc.displayWidth;
            int mouseY = sr.getScaledHeight() - Mouse.getY() * sr.getScaledHeight() / mc.displayHeight - 1;
            if ((float)mouseX >= x && (float)mouseX <= x + width && (float)mouseY >= y && (float)mouseY <= y + height) {
               this.copyCoordinatesToClipboard(player);
            }
         }

         this.wasMousePressed = mousePressed;
      }
   }

   private void copyCoordinatesToClipboard(Entity player) {
      String coords = String.format("%d %d %d", Math.round(player.posX), Math.round(player.posY), Math.round(player.posZ));

      try {
         StringSelection selection = new StringSelection(coords);
         Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
         clipboard.setContents(selection, selection);
         Command.sendSilentMessage(ChatFormatting.GRAY + "Successfully copied coordinates");
      } catch (Exception e) {
         e.printStackTrace();
      }

   }

   private double getDimensionCoord(double coord) {
      if (mc.player.dimension == -1) {
         return coord * (double)8.0F;
      } else {
         return mc.player.dimension == 0 ? coord / (double)8.0F : coord;
      }
   }

   private String getRoundedDouble(double pos) {
      return (Boolean)this.simpleCoords.getValue() ? String.valueOf(Math.round(pos)) : String.format("%.1f", pos);
   }

   private String getDirectionString() {
      switch (mc.player.getHorizontalFacing()) {
         case SOUTH:
            return ((Brackets)this.brackets.getValue()).getColor() + " [" + this.getInfoColour(true) + "+Z" + ((Brackets)this.brackets.getValue()).getColor() + "]";
         case WEST:
            return ((Brackets)this.brackets.getValue()).getColor() + " [" + this.getInfoColour(true) + "-X" + ((Brackets)this.brackets.getValue()).getColor() + "]";
         case NORTH:
            return ((Brackets)this.brackets.getValue()).getColor() + " [" + this.getInfoColour(true) + "-Z" + ((Brackets)this.brackets.getValue()).getColor() + "]";
         default:
            return ((Brackets)this.brackets.getValue()).getColor() + " [" + this.getInfoColour(true) + "+X" + ((Brackets)this.brackets.getValue()).getColor() + "]";
      }
   }

   private String getFixedDirection() {
      String dir;
      switch (mc.player.getHorizontalFacing()) {
         case SOUTH:
            dir = "South";
            break;
         case WEST:
            dir = "West";
            break;
         case NORTH:
            dir = "North";
            break;
         case EAST:
            dir = "East";
            break;
         default:
            dir = "";
      }

      if ((Boolean)this.futureColor.getValue()) {
         dir = TextFormatting.WHITE + dir;
      }

      return dir;
   }

   public String getInfoColour(boolean white) {
      return white ? TextFormatting.WHITE.toString() : TextFormatting.RESET.toString();
   }

   public static enum Brackets {
      WHITE(TextFormatting.WHITE.toString()),
      GRAY(TextFormatting.GRAY.toString()),
      DARK_GRAY(TextFormatting.DARK_GRAY.toString());

      final String s;

      private Brackets(String s) {
         this.s = s;
      }

      public String getColor() {
         return this.s;
      }
   }
}
