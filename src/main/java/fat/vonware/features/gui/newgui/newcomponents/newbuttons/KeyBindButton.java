package fat.vonware.features.gui.newgui.newcomponents.newbuttons;

import fat.vonware.Vonware;
import fat.vonware.features.gui.newgui.newcomponents.NewComponent;
import fat.vonware.features.gui.newgui.newcomponents.NewVonwareGui;
import fat.vonware.features.setting.Bind;
import fat.vonware.features.setting.Setting;
import fat.vonware.util.Animations.AnimationUtil;
import fat.vonware.util.Animations.render.RenderUtil;

public class KeyBindButton extends NewButton {
   private final Setting setting;
   public boolean isListening;
   private static long listenPulseStart = System.currentTimeMillis();
   private long hoverStart = 0L;
   private long hoverEndTime = 0L;
   private boolean wasHovering = false;

   public KeyBindButton(Setting setting) {
      super(setting.getName());
      this.setting = setting;
      this.width = 15;
      this.height = 13;
   }

   public void drawScreen(int mouseX, int mouseY, float partialTicks) {
      boolean hovering = this.isHovering(mouseX, mouseY);
      this.updateHoverSound(hovering);
      int[] base = new int[3];
      resolveBaseColor(base);
      int bR = base[0];
      int bG = base[1];
      int bB = base[2];
      float right = this.x + (float)this.width + 8.0F;
      float bottom = this.y + (float)this.height - 0.5F;
      float hoverRaw;
      if (hovering) {
         this.hoverAnimation.reset(true);
         hoverRaw = 1.0F;
      } else {
         hoverRaw = this.hoverAnimation.tick(false);
      }

      float hoverF = AnimationUtil.easeOutCubic(hoverRaw);
      if (hoverF > 0.0F) {
         RenderUtil.drawRect(this.x, this.y, right, bottom, argb((int)(35.0F * hoverF), bR, bG, bB));
      }

      if (this.isListening) {
         RenderUtil.drawRect(this.x, this.y, right, bottom, argb(22, bR, bG, bB));
         RenderUtil.drawRect(this.x + 1.0F, bottom - 0.5F, right - 1.0F, bottom, argb(200, bR, bG, bB));
      }

      if (this.isListening) {
         long now = System.currentTimeMillis();
         float elapsed = (float)(now - listenPulseStart) * 0.003F;
         int dotCount = (int)((now - listenPulseStart) / 400L % 4L);
         StringBuilder dotsBuilder = new StringBuilder();

         for(int d = 0; d < dotCount; ++d) {
            dotsBuilder.append('.');
         }

         String fullText = "Listening" + dotsBuilder.toString();
         float currentX = this.x + 3.5F;
         float baseY = this.y + 1.6F;

         for(int i = 0; i < fullText.length(); ++i) {
            String ch = String.valueOf(fullText.charAt(i));
            float wave = (float)Math.sin((double)(elapsed + (float)i * 0.3F));
            float yOffset = wave * 0.5F;
            float intensity = (wave + 1.0F) / 2.0F;
            int color = this.interpolateColor(-7829368, -3355444, intensity * 0.7F);
            Vonware.textManager.getCustomFont().drawStringWithShadow(ch, (double)currentX, (double)(baseY + yOffset), color);
            currentX += (float)Vonware.textManager.getCustomFont().getStringWidth(ch);
         }
      } else {
         if (hovering && !this.wasHovering) {
            this.hoverStart = System.currentTimeMillis();
         }

         if (!hovering && this.wasHovering) {
            this.hoverEndTime = System.currentTimeMillis();
         }

         this.wasHovering = hovering;
         float t;
         if (hovering) {
            t = Math.min((float)(System.currentTimeMillis() - this.hoverStart) / 200.0F, 1.0F);
         } else {
            t = 1.0F - Math.min((float)(System.currentTimeMillis() - this.hoverEndTime) / 200.0F, 1.0F);
         }

         String bindText = this.setting.getValue().toString().toUpperCase();
         int bindColor = this.interpolateColor(-5592406, -1, t);
         Vonware.textManager.getCustomFont().drawStringWithShadow(this.setting.getName() + " ", (double)(this.x + 3.5F), (double)(this.y + 3.0F), -3355444);
         float nameWidth = (float)Vonware.textManager.getCustomFont().getStringWidth(this.setting.getName() + " ");
         Vonware.textManager.getCustomFont().drawStringWithShadow(bindText, (double)(this.x + 3.5F + nameWidth), (double)(this.y + 3.0F), bindColor);
      }

   }

   private int interpolateColor(int c1, int c2, float t) {
      int r = (int)((float)(c1 >> 16 & 255) + (float)((c2 >> 16 & 255) - (c1 >> 16 & 255)) * t);
      int g = (int)((float)(c1 >> 8 & 255) + (float)((c2 >> 8 & 255) - (c1 >> 8 & 255)) * t);
      int b = (int)((float)(c1 & 255) + (float)((c2 & 255) - (c1 & 255)) * t);
      return -16777216 | r << 16 | g << 8 | b;
   }

   public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
      super.mouseClicked(mouseX, mouseY, mouseButton);
      if (this.isListening && mouseButton != 0 && mouseButton != 1) {
         this.setting.setValue(new Bind(mouseButton + 256));
         this.onMouseClick();
      }

   }

   public void onKeyTyped(char typedChar, int keyCode) {
      if (this.isListening) {
         Bind bind = new Bind(keyCode);
         if (!bind.toString().equalsIgnoreCase("Escape")) {
            if (bind.toString().equalsIgnoreCase("Delete") || bind.toString().equalsIgnoreCase("Back")) {
               bind = new Bind(-1);
            }

            this.setting.setValue(bind);
            this.onMouseClick();
         }
      }
   }

   public void toggle() {
      this.isListening = !this.isListening;
      if (this.isListening) {
         listenPulseStart = System.currentTimeMillis();
      }

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
