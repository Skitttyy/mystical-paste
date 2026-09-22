package fat.vonware.features.modules;

import fat.vonware.Global;
import fat.vonware.event.events.Render2DEvent;
import fat.vonware.features.modules.client.Manager;
import fat.vonware.features.setting.Setting;
import fat.vonware.util.Animations.render.Render2DMethods;
import fat.vonware.util.HUD.Rectangle;
import java.awt.Color;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class HudModule extends Module {
   public Rectangle rect;
   public final Setting<Float> x = new Setting<Float>("X", 372.0F, 2.0F, 1500.0F, (v) -> false);
   public final Setting<Float> y = new Setting<Float>("Y", 205.0F, 2.0F, 1500.0F, (v) -> false);
   private boolean isDragging = false;
   private boolean autoPositionDisabled = false;

   public HudModule(String name, String description, Module.Category category, boolean hasListener, boolean hidden, boolean alwaysListening, float width, float height) {
      super(name, description, category, hasListener, hidden, alwaysListening);
      this.rect = new Rectangle((Float)this.x.getValue(), (Float)this.y.getValue(), width, height);
      this.register(this.x);
      this.register(this.y);
      this.unregister(this.bind);
   }

   protected int getScaledScreenWidth() {
      return mc.displayWidth / mc.gameSettings.guiScale;
   }

   protected int getScaledScreenHeight() {
      return mc.displayHeight / mc.gameSettings.guiScale;
   }

   private float clampX(float rawX) {
      float min = Manager.getInstance().getMarginPixels();
      float max = Math.max(min, (float)this.getScaledScreenWidth() - this.rect.getWidth() - min);
      return Math.max(min, Math.min(rawX, max));
   }

   private float clampY(float rawY) {
      float min = Manager.getInstance().getMarginPixels();
      float max = Math.max(min, (float)this.getScaledScreenHeight() - this.rect.getHeight() - min);
      return Math.max(min, Math.min(rawY, max));
   }

   private void applyClamp() {
      float cx = this.clampX(this.rect.getX());
      float cy = this.clampY(this.rect.getY());
      if (cx != this.rect.getX() || cy != this.rect.getY()) {
         this.rect.setX(cx);
         this.rect.setY(cy);
         this.x.setValue(cx);
         this.y.setValue(cy);
      }

   }

   @SubscribeEvent
   public void onRender2D(Render2DEvent event) {
      if (!this.isDragging) {
         this.rect.setX((Float)this.x.getValue());
         this.rect.setY((Float)this.y.getValue());
      }

      String display = this.getDisplayName();
      int textWidth = mc.fontRenderer.getStringWidth(display);
      int textHeight = mc.fontRenderer.FONT_HEIGHT;
      int padding = 4;
      this.rect.setWidth((float)(textWidth + padding));
      this.rect.setHeight((float)(textHeight + padding));
      float margin = Manager.getInstance().getMarginPixels();
      this.x.setMax((float)this.getScaledScreenWidth() - this.rect.getWidth() - margin);
      this.y.setMax((float)this.getScaledScreenHeight() - this.rect.getHeight() - margin);
      this.applyClamp();
   }

   public void drawEditorBackground(int mouseX, int mouseY, boolean isBeingDragged) {
      boolean isHovered = this.rect.inBound(mouseX, mouseY);
      Color accent = Global.getGlobalColorRainbowS();
      int ar = accent.getRed();
      int ag = accent.getGreen();
      int ab = accent.getBlue();
      int fillA = isBeingDragged ? 55 : (isHovered ? 27 : 0);
      if (fillA > 0) {
         Render2DMethods.drawRect(this.rect.getX() - 2.0F, this.rect.getY() - 2.0F, this.rect.getX() + this.rect.getWidth() + 2.0F, this.rect.getY() + this.rect.getHeight() + 2.0F, (new Color(ar, ag, ab, fillA)).getRGB());
      }

      int outlineAlpha = isBeingDragged ? 255 : 200;
      int bc = (new Color(ar, ag, ab, outlineAlpha)).getRGB();
      Render2DMethods.drawRect(this.rect.getX() - 2.0F, this.rect.getY() - 2.0F, this.rect.getX() - 1.0F, this.rect.getY() + this.rect.getHeight() + 2.0F, bc);
      Render2DMethods.drawRect(this.rect.getX() + this.rect.getWidth() + 1.0F, this.rect.getY() - 2.0F, this.rect.getX() + this.rect.getWidth() + 2.0F, this.rect.getY() + this.rect.getHeight() + 2.0F, bc);
      Render2DMethods.drawRect(this.rect.getX() - 1.0F, this.rect.getY() - 2.0F, this.rect.getX() + this.rect.getWidth() + 1.0F, this.rect.getY() - 1.0F, bc);
      Render2DMethods.drawRect(this.rect.getX() - 1.0F, this.rect.getY() + this.rect.getHeight() + 1.0F, this.rect.getX() + this.rect.getWidth() + 1.0F, this.rect.getY() + this.rect.getHeight() + 2.0F, bc);
   }

   public void setPosition(float newX, float newY) {
      if (!this.isDragging && !this.autoPositionDisabled) {
         this.disableAutoPosition();
         this.autoPositionDisabled = true;
      }

      this.isDragging = true;
      this.rect.setX(newX);
      this.rect.setY(newY);
      this.applyClamp();
      this.x.setValue(this.rect.getX());
      this.y.setValue(this.rect.getY());
   }

   public boolean isAutoPositioned() {
      return false;
   }

   protected void disableAutoPosition() {
   }

   public void stopDragging() {
      this.isDragging = false;
   }

   public void updateBounds() {
      this.rect.setX((Float)this.x.getValue());
      this.rect.setY((Float)this.y.getValue());
      String display = this.getDisplayName();
      int textWidth = mc.fontRenderer.getStringWidth(display);
      int textHeight = mc.fontRenderer.FONT_HEIGHT;
      int padding = 4;
      this.rect.setWidth((float)(textWidth + padding));
      this.rect.setHeight((float)(textHeight + padding));
      this.applyClamp();
   }

   public boolean isDragging() {
      return this.isDragging;
   }

   public Rectangle getSecondaryRect() {
      return null;
   }

   public void setSecondaryPosition(float newX, float newY) {
   }

   public void drawSecondaryEditorBackground(int mouseX, int mouseY, boolean isBeingDragged) {
   }
}
