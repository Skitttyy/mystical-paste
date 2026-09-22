package fat.vonware.features.gui.components.items.buttons;

import fat.vonware.Vonware;
import fat.vonware.features.gui.newgui.newcomponents.NewComponent;
import fat.vonware.features.gui.newgui.newcomponents.NewVonwareGui;
import fat.vonware.features.gui.newgui.newcomponents.newbuttons.NewButton;
import fat.vonware.features.modules.client.Colors;
import fat.vonware.features.setting.Setting;
import fat.vonware.util.Animations.render.ColorUtil;
import fat.vonware.util.Animations.render.RenderUtil;
import java.awt.Color;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

public class ColorPickerButton extends NewButton {
   private final Setting<Float> hueSet;
   private final Setting<Float> satSet;
   private final Setting<Float> valSet;
   private boolean draggingHue = false;
   private boolean draggingSV = false;
   private boolean hexListening = false;
   private String hexCurrent = "";
   private int heldKey = 0;
   private char heldChar = 0;
   private long keyHeldSince = 0L;
   private long lastRepeatTime = 0L;
   private static final long INITIAL_DELAY = 400L;
   private static final long REPEAT_INTERVAL = 45L;
   private static final float PADDING = 2.0F;
   private static final float HUE_HEIGHT = 8.0F;
   private static final float HEX_HEIGHT = 13.0F;

   public ColorPickerButton(Setting<Float> hue, Setting<Float> sat, Setting<Float> val) {
      super("ColorPicker");
      this.hueSet = hue;
      this.satSet = sat;
      this.valSet = val;
      this.width = 15;
   }

   public void drawScreen(int mouseX, int mouseY, float partialTicks) {
      float startX = this.x + 2.0F;
      float SV_SIZE = (float)this.width + 8.0F - 4.0F;
      float contentStartY = this.y + 13.0F + 2.0F;
      this.drawSVBox(startX, contentStartY, SV_SIZE, SV_SIZE);
      float svX = startX + (Float)this.satSet.getValue() * SV_SIZE;
      float svY = contentStartY + (1.0F - (Float)this.valSet.getValue()) * SV_SIZE;
      float crossSize = 3.0F;
      float thickness = 0.4F;
      RenderUtil.drawRect(svX - crossSize, svY - thickness, svX + crossSize, svY + thickness, -16777216);
      RenderUtil.drawRect(svX - thickness, svY - crossSize, svX + thickness, svY + crossSize, -16777216);
      float hueBarY = contentStartY + SV_SIZE + 2.0F;
      this.drawHueBar(startX, hueBarY, SV_SIZE, 8.0F);
      float hueX = startX + (Float)this.hueSet.getValue() / 360.0F * SV_SIZE;
      RenderUtil.drawRect(hueX - 1.0F, hueBarY - 0.5F, hueX + 1.0F, hueBarY + 8.0F + 0.5F, -1);
      float hexY = hueBarY + 8.0F + 2.0F;
      float hexRight = startX + SV_SIZE;
      float hexBottom = hexY + 13.0F;
      boolean hexHovering = (float)mouseX >= startX && (float)mouseX <= hexRight && (float)mouseY >= hexY && (float)mouseY <= hexBottom;
      int bgAlpha = this.hexListening ? 25 : (hexHovering ? 16 : 8);
      RenderUtil.drawRect(startX, hexY, hexRight, hexBottom, bgAlpha << 24 | 16711680 | '\uff00' | 255);
      if (this.hexListening) {
         if (this.heldKey != 0) {
            if (Keyboard.isKeyDown(this.heldKey)) {
               long now = System.currentTimeMillis();
               if (now - this.keyHeldSince > 400L && now - this.lastRepeatTime > 45L) {
                  this.lastRepeatTime = now;
                  this.applyHexKey(this.heldChar, this.heldKey);
               }
            } else {
               this.heldKey = 0;
               this.heldChar = 0;
            }
         }

         String cursor = System.currentTimeMillis() / 500L % 2L == 0L ? "_" : "";
         Vonware.textManager.getCustomFont().drawStringWithShadow("#" + this.hexCurrent + cursor, (double)(startX + 3.5F), (double)(hexY + 3.0F), -1);
      } else {
         Color c = ColorUtil.hsvToRgb((Float)this.hueSet.getValue(), (Float)this.satSet.getValue(), (Float)this.valSet.getValue());
         String hex = String.format("%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());
         Vonware.textManager.getCustomFont().drawStringWithShadow("#", (double)(startX + 3.5F), (double)(hexY + 3.0F), -3355444);
         float hashWidth = (float)Vonware.textManager.getCustomFont().getStringWidth("#");
         Vonware.textManager.getCustomFont().drawStringWithShadow(hex, (double)(startX + 3.5F + hashWidth), (double)(hexY + 3.0F), -3355444);
         String pencil = "\u270e";
         int iconColor = hexHovering ? -5592406 : -10461088;
         float iconX = hexRight - (float)mc.fontRenderer.getStringWidth(pencil) - 3.0F;
         mc.fontRenderer.drawStringWithShadow(pencil, iconX, hexY + 3.5F, iconColor);
      }

      Vonware.textManager.getCustomFont().drawStringWithShadow("Global Color", (double)(this.x + 3.5F), (double)(this.y + 3.0F), -1);
      if (Mouse.isButtonDown(0)) {
         this.handleMouseDrag(mouseX, mouseY, startX, contentStartY, hueBarY, SV_SIZE);
      } else {
         this.draggingHue = false;
         this.draggingSV = false;
      }

   }

   private void handleMouseDrag(int mouseX, int mouseY, float startX, float contentStartY, float hueBarY, float svSize) {
      if ((float)mouseX >= startX && (float)mouseX <= startX + svSize && (float)mouseY >= contentStartY && (float)mouseY <= contentStartY + svSize) {
         this.draggingSV = true;
      }

      if ((float)mouseX >= startX && (float)mouseX <= startX + svSize && (float)mouseY >= hueBarY && (float)mouseY <= hueBarY + 8.0F) {
         this.draggingHue = true;
      }

      if (this.draggingSV) {
         float relX = Math.max(0.0F, Math.min(svSize, (float)mouseX - startX));
         float relY = Math.max(0.0F, Math.min(svSize, (float)mouseY - contentStartY));
         this.satSet.setValue(relX / svSize);
         this.valSet.setValue(1.0F - relY / svSize);
      }

      if (this.draggingHue) {
         float relX = Math.max(0.0F, Math.min(svSize, (float)mouseX - startX));
         float newHue = relX / svSize * 360.0F;
         this.hueSet.setValue(newHue);
      }

   }

   public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
      float startX = this.x + 2.0F;
      float SV_SIZE = (float)this.width + 8.0F - 4.0F;
      float hueBarY = this.y + 13.0F + 2.0F + SV_SIZE + 2.0F;
      float hexY = hueBarY + 8.0F + 2.0F;
      float hexRight = startX + SV_SIZE;
      float hexBottom = hexY + 13.0F;
      boolean clickedHex = (float)mouseX >= startX && (float)mouseX <= hexRight && (float)mouseY >= hexY && (float)mouseY <= hexBottom;
      if (mouseButton == 0 && clickedHex) {
         this.hexListening = !this.hexListening;
         this.hexCurrent = "";
         this.heldKey = 0;
         this.heldChar = 0;
      } else if (this.hexListening && !clickedHex) {
         this.hexListening = false;
         this.hexCurrent = "";
      }

   }

   public void onKeyTyped(char typedChar, int keyCode) {
      if (this.hexListening) {
         this.applyHexKey(typedChar, keyCode);
         if (keyCode != 1 && keyCode != 28) {
            this.heldKey = keyCode;
            this.heldChar = typedChar;
            this.keyHeldSince = System.currentTimeMillis();
            this.lastRepeatTime = this.keyHeldSince;
         } else {
            this.heldKey = 0;
         }

      }
   }

   private void applyHexKey(char typedChar, int keyCode) {
      boolean ctrl = Keyboard.isKeyDown(29) || Keyboard.isKeyDown(157);
      if (ctrl && keyCode == 47) {
         String clip = GuiScreen.getClipboardString();
         if (clip != null) {
            String stripped = clip.replace("#", "").toUpperCase();

            for(char c : stripped.toCharArray()) {
               if (this.hexCurrent.length() < 6 && this.isHexChar(c)) {
                  this.hexCurrent = this.hexCurrent + c;
               }
            }
         }

      } else {
         switch (keyCode) {
            case 1:
               this.hexListening = false;
               this.hexCurrent = "";
               return;
            case 14:
               if (!this.hexCurrent.isEmpty()) {
                  this.hexCurrent = this.hexCurrent.substring(0, this.hexCurrent.length() - 1);
               }

               return;
            case 28:
               this.commitHex();
               return;
            default:
               char upper = Character.toUpperCase(typedChar);
               if (this.hexCurrent.length() < 6 && this.isHexChar(upper)) {
                  this.hexCurrent = this.hexCurrent + upper;
                  if (this.hexCurrent.length() == 6) {
                     this.commitHex();
                  }
               }

         }
      }
   }

   private void commitHex() {
      if (this.hexCurrent.length() == 6) {
         try {
            int rgb = Integer.parseInt(this.hexCurrent, 16);
            Color c = new Color(rgb);
            float[] hsv = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), (float[])null);
            this.hueSet.setValue(hsv[0] * 360.0F);
            this.satSet.setValue(hsv[1]);
            this.valSet.setValue(hsv[2]);
         } catch (NumberFormatException var4) {
         }
      }

      this.hexListening = false;
      this.hexCurrent = "";
   }

   private boolean isHexChar(char c) {
      return c >= '0' && c <= '9' || c >= 'A' && c <= 'F';
   }

   public int getHeight() {
      float SV_SIZE = (float)this.width + 8.0F - 4.0F;
      return (int)(21.0F + SV_SIZE + 8.0F + 13.0F);
   }

   private void drawHueBar(float x, float y, float width, float height) {
      int seg = 6;
      float segW = width / (float)seg;
      GL11.glDisable(3553);
      GL11.glShadeModel(7425);
      GL11.glBegin(7);

      for(int i = 0; i < seg; ++i) {
         Color c1 = ColorUtil.hsvToRgb(360.0F * (float)i / (float)seg, 1.0F, 1.0F);
         Color c2 = ColorUtil.hsvToRgb(360.0F * (float)(i + 1) / (float)seg, 1.0F, 1.0F);
         float x1 = x + (float)i * segW;
         float x2 = x1 + segW;
         float yb = y + height;
         GL11.glColor4f((float)c1.getRed() / 255.0F, (float)c1.getGreen() / 255.0F, (float)c1.getBlue() / 255.0F, 1.0F);
         GL11.glVertex2f(x1, yb);
         GL11.glColor4f((float)c2.getRed() / 255.0F, (float)c2.getGreen() / 255.0F, (float)c2.getBlue() / 255.0F, 1.0F);
         GL11.glVertex2f(x2, yb);
         GL11.glColor4f((float)c2.getRed() / 255.0F, (float)c2.getGreen() / 255.0F, (float)c2.getBlue() / 255.0F, 1.0F);
         GL11.glVertex2f(x2, y);
         GL11.glColor4f((float)c1.getRed() / 255.0F, (float)c1.getGreen() / 255.0F, (float)c1.getBlue() / 255.0F, 1.0F);
         GL11.glVertex2f(x1, y);
      }

      GL11.glEnd();
      GL11.glShadeModel(7424);
      GL11.glEnable(3553);
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
   }

   private void drawSVBox(float x, float y, float width, float height) {
      int resolution = 8;
      float pixelWidth = width / (float)resolution;
      float pixelHeight = height / (float)resolution;
      GL11.glDisable(3553);
      GL11.glShadeModel(7425);
      GL11.glBegin(7);

      for(int i = 0; i < resolution; ++i) {
         for(int j = 0; j < resolution; ++j) {
            float sat1 = (float)i / (float)resolution;
            float sat2 = (float)(i + 1) / (float)resolution;
            float val1 = 1.0F - (float)j / (float)resolution;
            float val2 = 1.0F - (float)(j + 1) / (float)resolution;
            float x1 = x + (float)i * pixelWidth;
            float y1 = y + (float)j * pixelHeight;
            float x2 = x1 + pixelWidth;
            float y2 = y1 + pixelHeight;
            Color c1 = ColorUtil.hsvToRgb((Float)this.hueSet.getValue(), sat1, val2);
            GL11.glColor3f((float)c1.getRed() / 255.0F, (float)c1.getGreen() / 255.0F, (float)c1.getBlue() / 255.0F);
            GL11.glVertex2f(x1, y2);
            Color c2 = ColorUtil.hsvToRgb((Float)this.hueSet.getValue(), sat2, val2);
            GL11.glColor3f((float)c2.getRed() / 255.0F, (float)c2.getGreen() / 255.0F, (float)c2.getBlue() / 255.0F);
            GL11.glVertex2f(x2, y2);
            Color c3 = ColorUtil.hsvToRgb((Float)this.hueSet.getValue(), sat2, val1);
            GL11.glColor3f((float)c3.getRed() / 255.0F, (float)c3.getGreen() / 255.0F, (float)c3.getBlue() / 255.0F);
            GL11.glVertex2f(x2, y1);
            Color c4 = ColorUtil.hsvToRgb((Float)this.hueSet.getValue(), sat1, val1);
            GL11.glColor3f((float)c4.getRed() / 255.0F, (float)c4.getGreen() / 255.0F, (float)c4.getBlue() / 255.0F);
            GL11.glVertex2f(x1, y1);
         }
      }

      GL11.glEnd();
      GL11.glShadeModel(7424);
      GL11.glEnable(3553);
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
   }

   public void update() {
      this.setHidden(Colors.getInstance().colorSystem.getValue() != Colors.ColorSystem.Picker);
   }

   public boolean isHovering(int mouseX, int mouseY) {
      for(NewComponent component : NewVonwareGui.getClickGui().getComponents()) {
         if (component.drag) {
            return false;
         }
      }

      return (float)mouseX >= this.getX() && (float)mouseX <= this.getX() + (float)this.getWidth() + 8.0F && (float)mouseY >= this.getY() && (float)mouseY <= this.getY() + (float)this.getHeight();
   }
}
