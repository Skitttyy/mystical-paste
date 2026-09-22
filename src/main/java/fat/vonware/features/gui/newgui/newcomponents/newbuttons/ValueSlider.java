package fat.vonware.features.gui.newgui.newcomponents.newbuttons;

import com.mojang.realmsclient.gui.ChatFormatting;
import fat.vonware.Global;
import fat.vonware.Vonware;
import fat.vonware.features.gui.newgui.newcomponents.NewComponent;
import fat.vonware.features.gui.newgui.newcomponents.NewVonwareGui;
import fat.vonware.features.modules.client.ClickGui;
import fat.vonware.features.modules.client.Colors;
import fat.vonware.features.setting.Setting;
import fat.vonware.util.Animations.AnimationUtil;
import fat.vonware.util.Animations.render.ColorUtil;
import fat.vonware.util.Animations.render.RenderUtil;
import fat.vonware.util.OtherRenderShit.gui.SoundUtil;
import java.awt.Color;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

public class ValueSlider extends NewButton {
   private final Number min;
   private final Number max;
   private final int difference;
   public Setting setting;
   private boolean mouseWasDownLastFrame = false;
   private static final float TEXT_HEIGHT = 11.0F;
   private static final float TRACK_AREA_HEIGHT = 8.0F;
   private static final float TRACK_HEIGHT = 1.5F;
   private static final float KNOB_RADIUS = 2.2F;
   private static final int CIRCLE_SEGMENTS = 28;
   private static final float TRACK_LEFT_PAD = 4.0F;
   private static final float TRACK_RIGHT_PAD = 4.0F;
   private final AnimationUtil.LerpAnimation visualFill = new AnimationUtil.LerpAnimation(0.0F, 0.75F, 0.001F);
   private boolean wasDragging = false;
   private boolean isListening = false;
   private String current = "";
   private int heldKey = 0;
   private char heldChar = 0;
   private long keyHeldSince = 0L;
   private long lastRepeatTime = 0L;
   private static final long INITIAL_DELAY = 400L;
   private static final long REPEAT_INTERVAL = 45L;

   public ValueSlider(Setting setting) {
      super(setting.getName());
      this.setting = setting;
      this.min = (Number)setting.getMin();
      this.max = (Number)setting.getMax();
      this.difference = this.max.intValue() - this.min.intValue();
      this.width = 15;
      this.height = 20;
      this.visualFill.setValue(this.partialMultiplier());
   }

   private boolean isHoveringTextArea(int mouseX, int mouseY) {
      float hitLeft = this.x + 4.0F;
      float hitRight = this.x + (float)this.width + 8.0F - 4.0F;
      return (float)mouseX >= hitLeft && (float)mouseX <= hitRight && (float)mouseY >= this.y && (float)mouseY <= this.y + 11.0F;
   }

   private boolean isHoveringFullArea(int mouseX, int mouseY) {
      return (float)mouseX >= this.x && (float)mouseX <= this.x + (float)this.width + 8.0F && (float)mouseY >= this.y && (float)mouseY <= this.y + (float)this.height;
   }

   public boolean isHovering(int mouseX, int mouseY) {
      for(NewComponent c : NewVonwareGui.getClickGui().getComponents()) {
         if (c.drag) {
            return false;
         }
      }

      float trackCenterY = this.y + 11.0F + 4.0F;
      float hitTop = trackCenterY - 2.2F - 2.0F;
      float hitBottom = trackCenterY + 2.2F + 2.0F;
      float hitLeft = this.x + 4.0F - 2.2F;
      float hitRight = this.x + (float)this.getWidth() + 8.0F - 4.0F + 2.2F;
      return (float)mouseX >= hitLeft && (float)mouseX <= hitRight && (float)mouseY >= hitTop && (float)mouseY <= hitBottom;
   }

   public void drawScreen(int mouseX, int mouseY, float partialTicks) {
      if (this.isListening && this.heldKey != 0) {
         if (Keyboard.isKeyDown(this.heldKey)) {
            long now = System.currentTimeMillis();
            if (now - this.keyHeldSince > 400L && now - this.lastRepeatTime > 45L) {
               this.lastRepeatTime = now;
               this.applyKey(this.heldChar, this.heldKey);
            }
         } else {
            this.heldKey = 0;
            this.heldChar = 0;
         }
      }

      if (!this.isListening) {
         this.dragSetting(mouseX, mouseY);
      }

      boolean hovering = this.isHovering(mouseX, mouseY);
      this.updateHoverSound(hovering);
      float totalWidth = (float)this.width + 8.0F;
      Color baseColor = this.resolveAccentColor();
      if (this.isListening) {
         String cursor = System.currentTimeMillis() / 500L % 2L == 0L ? "|" : " ";
         String liveValue = ChatFormatting.WHITE + this.current + ChatFormatting.GRAY + cursor;
         this.drawLabelAndValue(liveValue, -1);
      } else {
         String valueText;
         if (!(this.setting.getValue() instanceof Float) && !(this.setting.getValue() instanceof Double)) {
            valueText = String.valueOf(this.setting.getValue());
         } else if (this.setting.hasStep()) {
            float step = ((Number)this.setting.getStep()).floatValue();
            valueText = String.format("%." + this.getDecimalsForStep(step) + "f", this.setting.getValue());
         } else {
            valueText = String.format("%.1f", this.setting.getValue());
         }

         this.drawLabelAndValue(ChatFormatting.GRAY + valueText, -1);
      }

      float trackLeft = this.x + 4.0F;
      float trackRight = this.x + totalWidth - 4.0F;
      float trackWidth = trackRight - trackLeft;
      float trackCenterY = this.y + 11.0F + 4.0F;
      float trackTop = trackCenterY - 0.75F;
      float trackBottom = trackCenterY + 0.75F;
      float fill = this.visualFill.tick(this.partialMultiplier());
      float fillX = trackLeft + trackWidth * fill;
      int activeAlpha = this.isListening ? 100 : (hovering ? 210 : 170);
      float kr = hovering && !this.isListening ? 2.5F : 2.2F;
      float unfilledStart = Math.min(fillX + kr, trackRight);
      if (unfilledStart < trackRight - 0.5F) {
         int emptyAlpha = this.isListening ? 15 : (hovering ? 50 : 30);
         RenderUtil.drawRect(unfilledStart, trackTop, trackRight, trackBottom, (new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), emptyAlpha)).getRGB());
      }

      float filledEnd = Math.max(trackLeft, fillX - kr);
      if (fill > 0.005F && filledEnd > trackLeft) {
         RenderUtil.drawRect(trackLeft, trackTop, filledEnd, trackBottom, (new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), activeAlpha)).getRGB());
         RenderUtil.drawGradientRect(trackLeft, trackTop, filledEnd, trackTop + 0.5F, (new Color(255, 255, 255, this.isListening ? 8 : 25)).getRGB(), (new Color(255, 255, 255, 0)).getRGB());
      }

      this.drawFilledCircle(fillX, trackCenterY, kr + 0.7F, new Color(0, 0, 0, this.isListening ? 20 : 50));
      this.drawFilledCircle(fillX, trackCenterY, kr, new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), activeAlpha));
   }

   private void drawLabelAndValue(String valueText, int color) {
      float textX = this.x + 3.5F;
      float textY = this.y + 2.5F;
      if (ClickGui.getInstance().numberProperties.getValue() == ClickGui.NumberProperties.End) {
         Vonware.textManager.getCustomFont().drawStringWithShadow(this.getName(), (double)textX, (double)textY, color);
         float rightPadding = 4.0F;
         float rightEdge = this.x + (float)this.width + 8.0F - rightPadding;
         float valueWidth = (float)Vonware.textManager.getCustomFont().getStringWidth(valueText);
         Vonware.textManager.getCustomFont().drawStringWithShadow(valueText, (double)(rightEdge - valueWidth), (double)textY, color);
      } else {
         Vonware.textManager.getCustomFont().drawStringWithShadow(this.getName() + " " + valueText, (double)textX, (double)textY, color);
      }

   }

   public void onKeyTyped(char typedChar, int keyCode) {
      if (this.isListening) {
         this.applyKey(typedChar, keyCode);
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

   private void applyKey(char typedChar, int keyCode) {
      boolean ctrl = Keyboard.isKeyDown(29) || Keyboard.isKeyDown(157);
      if (ctrl) {
         if (keyCode == 47) {
            String clip = GuiScreen.getClipboardString();
            if (clip != null) {
               this.appendFiltered(clip);
            }

            return;
         }

         if (keyCode == 211) {
            this.current = "";
            return;
         }
      }

      switch (keyCode) {
         case 1:
            this.cancelInput();
            return;
         case 14:
            if (!this.current.isEmpty()) {
               this.current = this.current.substring(0, this.current.length() - 1);
            }

            return;
         case 28:
            this.commitInput();
            return;
         default:
            this.appendFiltered(String.valueOf(typedChar));
      }
   }

   private void appendFiltered(String chars) {
      for(char c : chars.toCharArray()) {
         if (c == '-') {
            if (this.current.isEmpty()) {
               this.current = this.current + c;
            }
         } else if ((c != '.' || !this.isIntegerSetting()) && (c != '.' || !this.current.contains(".")) && (Character.isDigit(c) || c == '.')) {
            this.current = this.current + c;
         }
      }

   }

   private void commitInput() {
      if (!this.current.isEmpty() && !this.current.equals("-")) {
         try {
            if (this.isIntegerSetting()) {
               long parsed = Long.parseLong(this.current);
               int clamped = (int)Math.max(this.min.longValue(), Math.min(this.max.longValue(), parsed));
               this.setting.setValue(clamped);
            } else if (this.setting.getValue() instanceof Float) {
               float parsed = Float.parseFloat(this.current);
               float clamped = Math.max(this.min.floatValue(), Math.min(this.max.floatValue(), parsed));
               if (this.setting.hasStep()) {
                  float s = ((Number)this.setting.getStep()).floatValue();
                  clamped = (float)Math.round(clamped / s) * s;
               } else {
                  clamped = (float)Math.round(10.0F * clamped) / 10.0F;
               }

               this.setting.setValue(clamped);
            } else if (this.setting.getValue() instanceof Double) {
               double parsed = Double.parseDouble(this.current);
               double clamped = Math.max(this.min.doubleValue(), Math.min(this.max.doubleValue(), parsed));
               if (this.setting.hasStep()) {
                  double s = ((Number)this.setting.getStep()).doubleValue();
                  clamped = (double)Math.round(clamped / s) * s;
               } else {
                  clamped = (double)Math.round((double)10.0F * clamped) / (double)10.0F;
               }

               this.setting.setValue(clamped);
            }
         } catch (NumberFormatException var7) {
         }
      }

      this.stopListening();
   }

   private void cancelInput() {
      this.stopListening();
   }

   private void stopListening() {
      this.isListening = false;
      this.current = "";
      this.heldKey = 0;
      this.heldChar = 0;
   }

   public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
      if (this.isListening) {
         if (!this.isHoveringFullArea(mouseX, mouseY)) {
            this.cancelInput();
         }

      } else if (mouseButton == 2 && this.isHoveringTextArea(mouseX, mouseY)) {
         this.isListening = true;
         this.current = "";
      } else {
         if (mouseButton == 0 && this.isHovering(mouseX, mouseY)) {
            this.setSettingFromX(mouseX);
         }

      }
   }

   private void dragSetting(int mouseX, int mouseY) {
      if (!this.isListening) {
         boolean mouseDown = Mouse.isButtonDown(0);
         boolean freshPress = mouseDown && !this.mouseWasDownLastFrame;
         if (this.isHovering(mouseX, mouseY) && mouseDown) {
            if (this.wasDragging || freshPress) {
               this.setSettingFromX(mouseX);
               if (!this.wasDragging) {
                  SoundUtil.playModuleEnable();
                  this.wasDragging = true;
               }
            }
         } else if (this.wasDragging && !mouseDown) {
            SoundUtil.playModuleDisable();
            this.wasDragging = false;
         }

         this.mouseWasDownLastFrame = mouseDown;
      }
   }

   private void setSettingFromX(int mouseX) {
      float trackLeft = this.x + 4.0F;
      float trackRight = this.x + (float)this.width + 8.0F - 4.0F;
      float trackWidth = trackRight - trackLeft;
      float percent = Math.max(0.0F, Math.min(1.0F, ((float)mouseX - trackLeft) / trackWidth));
      if (this.setting.getValue() instanceof Double) {
         double result = (Double)this.setting.getMin() + (double)((float)this.difference * percent);
         if (this.setting.hasStep()) {
            double s = ((Number)this.setting.getStep()).doubleValue();
            result = (double)Math.round(result / s) * s;
         } else {
            result = (double)Math.round((double)10.0F * result) / (double)10.0F;
         }

         this.setting.setValue(result);
      } else if (this.setting.getValue() instanceof Float) {
         float result = (Float)this.setting.getMin() + (float)this.difference * percent;
         if (this.setting.hasStep()) {
            float s = ((Number)this.setting.getStep()).floatValue();
            result = (float)Math.round(result / s) * s;
         } else {
            result = (float)Math.round(10.0F * result) / 10.0F;
         }

         this.setting.setValue(result);
      } else if (this.setting.getValue() instanceof Integer) {
         this.setting.setValue((Integer)this.setting.getMin() + (int)((float)this.difference * percent));
      }

   }

   private boolean isIntegerSetting() {
      return this.setting.getValue() instanceof Integer;
   }

   private Color resolveAccentColor() {
      if ((Boolean)Colors.getInstance().rainbow.getValue()) {
         return Colors.getInstance().rainbowMode.getValue() == Colors.RainbowMode.Static ? ColorUtil.rainbow(0) : ColorUtil.rainbow((int)((float)NewComponent.counter1[0] * (Float)Colors.getInstance().rainbowHue.getValue()));
      } else {
         return (Boolean)ClickGui.getInstance().pulseGui.getValue() ? Global.getPulseColor(Colors.getInstance().getGuiColor(), 0) : Colors.getInstance().getCurrentColor();
      }
   }

   private int getDecimalsForStep(float step) {
      if (step <= 0.0F) {
         return 1;
      } else {
         int decimals = 0;

         for(float s = step; s < 0.99999F; ++decimals) {
            s *= 10.0F;
         }

         return decimals;
      }
   }

   private float partialMultiplier() {
      float mid = this.max.floatValue() - this.min.floatValue();
      if (mid == 0.0F) {
         return 0.0F;
      } else {
         float part = ((Number)this.setting.getValue()).floatValue() - this.min.floatValue();
         return Math.max(0.0F, Math.min(1.0F, part / mid));
      }
   }

   private void drawFilledCircle(float cx, float cy, float radius, Color color) {
      GL11.glPushAttrib(1048575);
      GL11.glDisable(3553);
      GL11.glEnable(3042);
      GL11.glDisable(2929);
      GL11.glDisable(3008);
      GL11.glDisable(2884);
      GL11.glBlendFunc(770, 771);
      GL11.glEnable(2848);
      GL11.glEnable(2881);
      GL11.glHint(3155, 4354);
      GL11.glColor4f((float)color.getRed() / 255.0F, (float)color.getGreen() / 255.0F, (float)color.getBlue() / 255.0F, (float)color.getAlpha() / 255.0F);
      GL11.glBegin(6);
      GL11.glVertex2f(cx, cy);
      double step = 0.2243994752564138;

      for(int i = 0; i <= 28; ++i) {
         double angle = (double)i * step;
         GL11.glVertex2f((float)((double)cx + Math.cos(angle) * (double)radius), (float)((double)cy + Math.sin(angle) * (double)radius));
      }

      GL11.glEnd();
      GL11.glPopAttrib();
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
   }

   public void update() {
      this.setHidden(!this.setting.isVisible());
   }

   public int getHeight() {
      return 20;
   }
}
