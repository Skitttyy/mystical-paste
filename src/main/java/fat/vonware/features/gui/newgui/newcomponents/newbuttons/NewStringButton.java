package fat.vonware.features.gui.newgui.newcomponents.newbuttons;

import com.mojang.realmsclient.gui.ChatFormatting;
import fat.vonware.Vonware;
import fat.vonware.features.gui.newgui.newcomponents.NewComponent;
import fat.vonware.features.gui.newgui.newcomponents.NewVonwareGui;
import fat.vonware.features.setting.Setting;
import fat.vonware.util.Animations.render.RenderUtil;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.ChatAllowedCharacters;
import org.lwjgl.input.Keyboard;

public class NewStringButton extends NewButton {
   private final Setting setting;
   public boolean isListening;
   private String current = "";
   private int heldKey = 0;
   private char heldChar = 0;
   private long keyHeldSince = 0L;
   private long lastRepeatTime = 0L;
   private static final long INITIAL_DELAY = 400L;
   private static final long REPEAT_INTERVAL = 45L;

   public NewStringButton(Setting setting) {
      super(setting.getName());
      this.setting = setting;
      this.width = 15;
      this.height = 13;
   }

   public void drawScreen(int mouseX, int mouseY, float partialTicks) {
      boolean hovering = this.isHovering(mouseX, mouseY);
      this.updateHoverSound(hovering);
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

      int[] base = new int[3];
      resolveBaseColor(base);
      int bR = base[0];
      int bG = base[1];
      int bB = base[2];
      float right = this.x + (float)this.width + 8.0F;
      float bottom = this.y + (float)this.height - 0.5F;
      int bgAlpha = this.isListening ? 25 : (hovering ? 16 : 8);
      RenderUtil.drawRect(this.x, this.y, right, bottom, argb(bgAlpha, bR, bG, bB));
      if (!this.isListening && hovering) {
         RenderUtil.drawRect(this.x + 1.0F, bottom - 0.5F, right - 1.0F, bottom, argb(60, bR, bG, bB));
      }

      if (this.isListening) {
         String cursor = System.currentTimeMillis() / 500L % 2L == 0L ? "_" : "";
         Vonware.textManager.getCustomFont().drawStringWithShadow(this.current + cursor, (double)(this.x + 3.5F), (double)(this.y + 3.0F), -1);
      } else {
         String prefix = "";
         String name = this.setting.getName();
         if (name.equals("Buttons") || name.equals("Prefix")) {
            prefix = name + " ";
         }

         Vonware.textManager.getCustomFont().drawStringWithShadow(prefix + ChatFormatting.GRAY + this.setting.getValue(), (double)(this.x + 3.5F), (double)(this.y + 3.0F), -3355444);
         String pencil = "\u270e";
         int iconColor = hovering ? -5592406 : -10461088;
         float iconX = right - (float)mc.fontRenderer.getStringWidth(pencil) - 3.0F;
         mc.fontRenderer.drawStringWithShadow(pencil, iconX, this.y + 3.5F, iconColor);
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
               for(char c : clip.toCharArray()) {
                  if (ChatAllowedCharacters.isAllowedCharacter(c)) {
                     this.current = this.current + c;
                  }
               }
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
            return;
         case 14:
            if (!this.current.isEmpty()) {
               this.current = this.current.substring(0, this.current.length() - 1);
            }

            return;
         case 28:
            this.enterString();
            return;
         default:
            if (ChatAllowedCharacters.isAllowedCharacter(typedChar)) {
               this.current = this.current + typedChar;
            }

      }
   }

   private void enterString() {
      this.setting.setValue(this.current.isEmpty() ? this.setting.getDefaultValue() : this.current);
      this.current = "";
      this.onMouseClick();
   }

   public void toggle() {
      this.isListening = !this.isListening;
      this.heldKey = 0;
      this.heldChar = 0;
   }

   public boolean getState() {
      return !this.isListening;
   }

   public void update() {
      this.setHidden(!this.setting.isVisible());
   }

   public int getHeight() {
      return 13;
   }

   public boolean isHovering(int mouseX, int mouseY) {
      for(NewComponent c : NewVonwareGui.getClickGui().getComponents()) {
         if (c.drag) {
            return false;
         }
      }

      return (float)mouseX >= this.x && (float)mouseX <= this.x + (float)this.width + 8.0F && (float)mouseY >= this.y && (float)mouseY <= this.y + (float)this.height;
   }
}
