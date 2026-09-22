package fat.vonware.features.modules.elements;

import fat.vonware.Global;
import fat.vonware.Vonware;
import fat.vonware.event.events.ClientEvent;
import fat.vonware.event.events.Render2DEvent;
import fat.vonware.features.modules.HudModule;
import fat.vonware.features.modules.Module;
import fat.vonware.features.modules.client.Colors;
import fat.vonware.features.setting.Setting;
import fat.vonware.util.Timer;
import fat.vonware.util.Animations.render.ColorUtil;
import fat.vonware.util.BlockHighlight.SlideUtil;
import fat.vonware.util.HUD.CornerSortUtil;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class FeatureList extends HudModule {
   private final Setting<Boolean> autoPosition = new Setting<Boolean>("AutoPosition", true);
   private final Setting<Integer> animationTime = new Setting<Integer>("AnimationTime", 300, 0, 1000);
   public Setting<Boolean> space = new Setting<Boolean>("Space", false);
   private final Setting<Boolean> animationY = new Setting<Boolean>("AnimationY", true);
   private static FeatureList INSTANCE = new FeatureList();
   private List<Modules> moduleList = new ArrayList();
   private final Timer updateTimer = new Timer();
   private boolean needUpdate = false;
   private boolean cachedLeftSide = false;
   private boolean cachedBottomSide = false;
   private boolean lastBottomSide = false;

   public FeatureList() {
      super("FeatureList", "Displays active module list", Module.Category.Elements, true, false, false, 100.0F, 200.0F);
      this.register(this.autoPosition);
      this.register(this.animationTime);
      this.register(this.animationY);
      this.register(this.space);
      this.setInstance();
   }

   private void refreshQuadrant() {
      int sw = Vonware.textManager.scaledWidth;
      int sh = Vonware.textManager.scaledHeight;
      float cx = this.rect.getX() + this.rect.getWidth() / 2.0F;
      float cy = this.rect.getY() + this.rect.getHeight() / 2.0F;
      this.cachedLeftSide = CornerSortUtil.isLeftSide(cx, sw);
      this.cachedBottomSide = CornerSortUtil.isBottomSide(cy, sh);
   }

   public boolean isAutoPositioned() {
      return (Boolean)this.autoPosition.getValue();
   }

   protected void disableAutoPosition() {
      if ((Boolean)this.autoPosition.getValue()) {
         this.autoPosition.setValue(false);
      }

   }

   protected Color getColor() {
      return Global.getGlobalColor();
   }

   protected Color getColorForIndex(int index, int yPosition) {
      if ((Boolean)Colors.getInstance().rainbow.getValue()) {
         return Colors.getInstance().rainbowMode.getValue() == Colors.RainbowMode.Static ? ColorUtil.rainbow(0) : ColorUtil.rainbow(yPosition / 2);
      } else {
         return Global.getGlobalColor();
      }
   }

   public static FeatureList getInstance() {
      if (INSTANCE == null) {
         INSTANCE = new FeatureList();
      }

      return INSTANCE;
   }

   private void setInstance() {
      INSTANCE = this;
   }

   public void onLoad() {
      this.moduleList.clear();

      for(Module module : Vonware.moduleManager.getModules()) {
         this.moduleList.add(new Modules(module));
      }

      this.restoreRectFromSettings();
      this.refreshQuadrant();
      this.lastBottomSide = this.cachedBottomSide;
      this.needUpdate = true;
   }

   private void restoreRectFromSettings() {
      int sw = Vonware.textManager.scaledWidth;
      int sh = Vonware.textManager.scaledHeight;
      int[] size = this.measureList();
      int listW = size[0];
      int listH = size[1];
      float visualX = (Float)this.x.getValue();
      float visualY = (Float)this.y.getValue();
      this.rect.setX(Math.max(0.0F, Math.min(visualX, (float)(sw - listW))));
      this.rect.setY(Math.max(0.0F, Math.min(visualY, (float)(sh - listH))));
      this.rect.setWidth((float)listW);
      this.rect.setHeight((float)listH);
   }

   public void onLogin() {
      this.needUpdate = true;
   }

   @SubscribeEvent
   public void clientEvent(ClientEvent event) {
      if (this.updateTimer.passedMs(5000L)) {
         this.updateTimer.reset();
         this.needUpdate = true;
      }
   }

   public void onRender2D(Render2DEvent event) {
      this.doRender();
   }

   public void updateBounds() {
      this.resolveRect();
      this.refreshQuadrant();
   }

   private int[] measureList() {
      int maxWidth = 0;
      int totalHeight = 0;

      for(Modules m : this.moduleList) {
         if (m.module.isDrawn() && (m.isEnabled || m.fade.easeOutQuad() < (double)1.0F)) {
            int w = Vonware.textManager.getStringWidth(m.module.getFullArrayString());
            if (w > maxWidth) {
               maxWidth = w;
            }

            totalHeight += Vonware.textManager.getFontHeight2();
         }
      }

      if (maxWidth == 0) {
         maxWidth = 100;
      }

      if (totalHeight == 0) {
         totalHeight = 20;
      }

      return new int[]{maxWidth, totalHeight};
   }

   private void resolveRect() {
      int sw = Vonware.textManager.scaledWidth;
      int sh = Vonware.textManager.scaledHeight;
      int[] size = this.measureList();
      int listW = size[0];
      int listH = size[1];
      float visualX;
      float visualY;
      if ((Boolean)this.autoPosition.getValue()) {
         visualX = this.cachedLeftSide ? 2.0F : (float)(sw - listW) - 2.0F;
         visualY = this.cachedBottomSide ? (float)(sh - listH) - 2.0F : 2.0F;
      } else {
         visualX = Math.max(0.0F, Math.min((Float)this.x.getValue(), (float)(sw - listW)));
         visualY = Math.max(0.0F, Math.min((Float)this.y.getValue(), (float)(sh - listH)));
      }

      this.rect.setX(visualX);
      this.rect.setY(visualY);
      this.rect.setWidth((float)listW);
      this.rect.setHeight((float)listH);
   }

   public void setPosition(float newX, float newY) {
      this.disableAutoPosition();
      int sw = Vonware.textManager.scaledWidth;
      int sh = Vonware.textManager.scaledHeight;
      float clampedX = Math.max(0.0F, Math.min(newX, (float)sw - this.rect.getWidth()));
      float clampedY = Math.max(0.0F, Math.min(newY, (float)sh - this.rect.getHeight()));
      this.rect.setX(clampedX);
      this.rect.setY(clampedY);
      this.refreshQuadrant();
      this.x.setValue(clampedX);
      this.y.setValue(clampedY);
   }

   private void doRender() {
      if (!fullNullCheck()) {
         this.resolveRect();
         this.refreshQuadrant();
         boolean leftSide = this.cachedLeftSide;
         boolean bottomSide = this.cachedBottomSide;
         if (bottomSide != this.lastBottomSide) {
            this.lastBottomSide = bottomSide;
            this.needUpdate = true;
         }

         if (this.needUpdate) {
            this.needUpdate = false;
            CornerSortUtil.sortInPlace(this.moduleList, (mx) -> Vonware.textManager.getStringWidth(mx.module.getFullArrayString()), bottomSide);
         }

         for(Modules m : this.moduleList) {
            if (m.module.isDrawn() && m.isEnabled) {
               int curWidth = Vonware.textManager.getStringWidth(m.module.getFullArrayString());
               if (curWidth != m.lastRenderedWidth) {
                  m.lastRenderedWidth = curWidth;
                  CornerSortUtil.sortInPlace(this.moduleList, (mod) -> Vonware.textManager.getStringWidth(mod.module.getFullArrayString()), bottomSide);
                  break;
               }
            }
         }

         int lastY = (int)this.rect.getY();

         for(Modules modules : this.moduleList) {
            if (modules.module.isDrawn()) {
               modules.fade.setLength((long)(Integer)this.animationTime.getValue());
               if (modules.module.isOn()) {
                  modules.enable();
               } else {
                  modules.disable();
               }

               modules.updateName();
               String displayString = modules.module.getFullArrayString();
               int xOffset;
               int yOffset;
               int heightIncrement;
               if (!leftSide) {
                  if (modules.isEnabled) {
                     double size = modules.fade.easeOutQuad();
                     xOffset = (int)((double)Vonware.textManager.getStringWidth(displayString) * size);
                     yOffset = modules.lastY = (int)((double)Vonware.textManager.getFontHeight2() * size);
                     modules.lastX = xOffset;
                     heightIncrement = yOffset;
                  } else {
                     double size = Math.abs(modules.fade.easeOutQuad() - (double)1.0F);
                     xOffset = (int)((double)modules.lastX * size);
                     yOffset = (int)((double)modules.lastY * size);
                     if (size <= (double)0.0F) {
                        continue;
                     }

                     heightIncrement = modules.lastY;
                  }
               } else if (modules.isEnabled) {
                  double size = Math.abs(modules.fade.easeOutQuad() - (double)1.0F);
                  xOffset = (int)((double)Vonware.textManager.getStringWidth(displayString) * size);
                  size = modules.fade.easeOutQuad();
                  yOffset = modules.lastY = (int)((double)Vonware.textManager.getFontHeight2() * size);
                  modules.lastX = xOffset;
                  heightIncrement = yOffset;
               } else {
                  double size = modules.fade.easeOutQuad();
                  xOffset = (int)((double)Vonware.textManager.getStringWidth(displayString) * size) + modules.lastX;
                  size = Math.abs(modules.fade.easeOutQuad() - (double)1.0F);
                  yOffset = (int)((double)modules.lastY * size);
                  if (size <= (double)0.0F || xOffset >= Vonware.textManager.getStringWidth(displayString)) {
                     continue;
                  }

                  heightIncrement = modules.lastY;
               }

               xOffset += (int)((double)20.0F * Math.abs(modules.change.easeOutQuad() - (double)1.0F));
               int showX = leftSide ? (int)this.rect.getX() - xOffset : (int)this.rect.getX() + (int)this.rect.getWidth() - xOffset;
               int actualY = lastY - ((Boolean)this.animationY.getValue() ? Math.abs(yOffset - Vonware.textManager.getFontHeight2()) : 0);
               Vonware.textManager.drawString(displayString, (float)showX, (float)actualY, this.getColorForIndex(0, actualY).getRGB(), true);
               lastY += heightIncrement;
            }
         }

      }
   }

   public static class Modules {
      public final SlideUtil fade;
      public final SlideUtil change;
      public boolean isEnabled = false;
      public final Module module;
      public int lastX = 0;
      public int lastY = 0;
      public int lastRenderedWidth = 0;
      public String lastName;

      public Modules(Module module) {
         this.module = module;
         this.fade = new SlideUtil(500L);
         this.change = new SlideUtil(200L);
         this.lastName = module.getFullArrayString();
      }

      public void enable() {
         if (!this.isEnabled) {
            this.isEnabled = true;
            this.fade.reset();
         }
      }

      public void disable() {
         if (this.isEnabled) {
            this.isEnabled = false;
            this.fade.reset();
         }
      }

      public void updateName() {
         String cur = this.module.getFullArrayString();
         if (!this.lastName.equals(cur)) {
            this.lastName = cur;
            this.change.reset();
         }

      }
   }
}
