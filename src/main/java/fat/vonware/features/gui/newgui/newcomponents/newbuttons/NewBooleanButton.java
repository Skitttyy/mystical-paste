package fat.vonware.features.gui.newgui.newcomponents.newbuttons;

import fat.vonware.Vonware;
import fat.vonware.features.gui.newgui.newcomponents.NewComponent;
import fat.vonware.features.gui.newgui.newcomponents.NewVonwareGui;
import fat.vonware.features.setting.Setting;
import fat.vonware.util.Animations.AnimationUtil;
import fat.vonware.util.Animations.render.RenderUtil;
import fat.vonware.util.OtherRenderShit.gui.SoundUtil;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.GlStateManager.DestFactor;
import net.minecraft.client.renderer.GlStateManager.SourceFactor;
import org.lwjgl.opengl.GL11;

public class NewBooleanButton extends NewButton {
   private final Setting setting;
   private static final long ANIM_MS = 200L;
   private long toggleTime = 0L;
   private long disableTime = 0L;

   public NewBooleanButton(Setting setting) {
      super(setting.getName());
      this.setting = setting;
      this.width = 15;
      this.height = 13;
   }

   private float fillProgress() {
      long now = System.currentTimeMillis();
      if (this.getState()) {
         return this.toggleTime <= 0L ? 1.0F : Math.min(1.0F, (float)(now - this.toggleTime) / 200.0F);
      } else {
         return this.disableTime <= 0L ? 0.0F : Math.max(0.0F, 1.0F - (float)(now - this.disableTime) / 200.0F);
      }
   }

   public void drawScreen(int mouseX, int mouseY, float partialTicks) {
      boolean hovering = this.isHovering(mouseX, mouseY);
      this.updateHoverSound(hovering);
      float fp = this.fillProgress();
      float hoverRaw;
      if (hovering) {
         this.hoverAnimation.reset(true);
         hoverRaw = 1.0F;
      } else {
         hoverRaw = this.hoverAnimation.tick(false);
      }

      float hoverF = AnimationUtil.easeOutCubic(hoverRaw);
      int[] base = new int[3];
      resolveBaseColor(base);
      int bR = base[0];
      int bG = base[1];
      int bB = base[2];
      boolean enabled = this.getState();
      if (hoverF > 0.0F) {
         RenderUtil.drawRect(this.x, this.y, this.x + (float)this.width + 8.0F, this.y + (float)this.height - 0.5F, argb((int)(35.0F * hoverF), bR, bG, bB));
      }

      int gray = 170;
      int white = 255;
      int lerped = (int)((float)gray + (float)(white - gray) * fp);
      Vonware.textManager.getCustomFont().drawStringWithShadow(this.getName(), (double)(this.x + 3.5F), (double)(this.y + 3.0F), argb(255, lerped, lerped, lerped));
      float baseRadius = 3.5F;
      float cx = this.x + (float)this.width + 8.0F - baseRadius - 4.0F;
      float cy = this.y + (float)this.height / 2.0F;
      float radius;
      if (enabled) {
         radius = baseRadius + 0.55F * hoverF;
      } else {
         float pulse = hoverF > 0.0F ? (float)Math.sin((double)System.currentTimeMillis() / (double)300.0F) * 0.5F + 0.5F : 0.0F;
         radius = baseRadius + pulse * 1.1F * hoverF;
      }

      int bgAlpha = (int)(20.0F + 15.0F * fp);
      this.drawCircle(cx, cy, radius, argb(bgAlpha, bR, bG, bB));
      if (fp > 0.001F) {
         this.drawCircle(cx, cy, radius - 1.2F, argb((int)(255.0F * fp), bR, bG, bB));
      }

   }

   private void drawCircle(float x, float y, float r, int color) {
      float a = (float)(color >> 24 & 255) / 255.0F;
      float red = (float)(color >> 16 & 255) / 255.0F;
      float g = (float)(color >> 8 & 255) / 255.0F;
      float b = (float)(color & 255) / 255.0F;
      GlStateManager.disableTexture2D();
      GlStateManager.enableBlend();
      GlStateManager.tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ZERO);
      GL11.glEnable(2848);
      GL11.glEnable(2881);
      GlStateManager.color(red, g, b, a);
      GL11.glBegin(6);
      GL11.glVertex2f(x, y);
      int segments = 30;

      for(int i = 0; i <= segments; ++i) {
         double angle = (Math.PI * 2D) * (double)i / (double)segments;
         GL11.glVertex2f((float)((double)x + Math.sin(angle) * (double)r), (float)((double)y + Math.cos(angle) * (double)r));
      }

      GL11.glEnd();
      GL11.glDisable(2848);
      GL11.glDisable(2881);
      GlStateManager.enableTexture2D();
      GlStateManager.disableBlend();
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
   }

   public boolean isHovering(int mouseX, int mouseY) {
      for(NewComponent c : NewVonwareGui.getClickGui().getComponents()) {
         if (c.drag) {
            return false;
         }
      }

      return (float)mouseX >= this.x && (float)mouseX <= this.x + (float)this.width + 8.0F && (float)mouseY >= this.y && (float)mouseY <= this.y + (float)this.height;
   }

   public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
      if (mouseButton == 0 && this.isHovering(mouseX, mouseY)) {
         this.toggle();
      }

   }

   public void toggle() {
      this.setting.setValue(!(Boolean)this.setting.getValue());
      if ((Boolean)this.setting.getValue()) {
         this.toggleTime = System.currentTimeMillis();
         SoundUtil.playModuleEnable();
      } else {
         this.disableTime = System.currentTimeMillis();
         SoundUtil.playModuleDisable();
      }

   }

   public boolean getState() {
      return (Boolean)this.setting.getValue();
   }

   public void update() {
      this.setHidden(!this.setting.isVisible());
   }

   public int getHeight() {
      return 13;
   }
}
