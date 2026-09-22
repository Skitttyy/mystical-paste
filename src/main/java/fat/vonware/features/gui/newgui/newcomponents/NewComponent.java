package fat.vonware.features.gui.newgui.newcomponents;

import fat.vonware.Global;
import fat.vonware.Vonware;
import fat.vonware.features.Feature;
import fat.vonware.features.gui.components.items.Item;
import fat.vonware.features.gui.newgui.newcomponents.newbuttons.ModuleComponent;
import fat.vonware.features.gui.newgui.newcomponents.newbuttons.NewButton;
import fat.vonware.features.gui.newgui.newcomponents.newbuttons.SearchComponent;
import fat.vonware.features.modules.client.ClickGui;
import fat.vonware.features.modules.client.Colors;
import fat.vonware.util.Animations.AnimationUtil;
import fat.vonware.util.Animations.ScrollBarUtil;
import fat.vonware.util.Animations.render.ColorUtil;
import fat.vonware.util.Animations.render.Render2DMethods;
import fat.vonware.util.Animations.render.RenderUtil;
import fat.vonware.util.OtherRenderShit.gui.SoundUtil;
import java.awt.Color;
import java.util.ArrayList;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.GlStateManager.DestFactor;
import net.minecraft.client.renderer.GlStateManager.SourceFactor;
import org.lwjgl.opengl.GL11;

public class NewComponent extends Feature {
   public static int[] counter1 = new int[]{1};
   public static int scissorX;
   public static int scissorY;
   public static int scissorW;
   public static int scissorH;
   private final ScrollBarUtil scrollBar = new ScrollBarUtil();
   private boolean needsScrollBar = false;
   private boolean scrollBarMouseDown = false;
   private final ArrayList<Item> items = new ArrayList();
   public boolean drag;
   private int x;
   private int y;
   private int x2;
   private int y2;
   private int width;
   private int height;
   private boolean open;
   private boolean hidden;
   private final AnimationUtil.DeltaAnimation panelFadeAnim = new AnimationUtil.DeltaAnimation(0.0F, 250.0F);
   private final AnimationUtil.DeltaAnimation expandAnim = new AnimationUtil.DeltaAnimation(0.0F, 650.0F);
   private float lastFadeProgress = 0.0F;
   private float lastExpandProgress = 0.0F;
   private float lastTotalBottom = 0.0F;
   private float cachedEffectiveH = 0.0F;
   private float cachedTotalItemH = 0.0F;
   private static long outlineGlowStart = System.currentTimeMillis();
   private static final float OUTLINE_GLOW_W = 140.0F;
   private static final float OUTLINE_SWEEP = 0.001F;

   private static int argb(int a, int r, int g, int b) {
      return (a & 255) << 24 | (r & 255) << 16 | (g & 255) << 8 | b & 255;
   }

   public NewComponent(String name, int x, int y, boolean open) {
      super(name);
      this.x = x;
      this.y = y;
      this.width = 120;
      this.height = 24;
      this.open = open;
      this.panelFadeAnim.setProgress(0.0F);
      this.expandAnim.setProgress(0.0F);
      this.setupItems();
   }

   public void setupItems() {
   }

   private void drag(int mouseX, int mouseY) {
      if (this.drag) {
         this.x = this.x2 + mouseX;
         this.y = this.y2 + mouseY;
      }
   }

   public boolean scroll(int delta, int mouseX, int mouseY) {
      if (this.open && !(this.cachedEffectiveH <= 0.0F)) {
         float contentTop = (float)(this.y + this.height) - 6.5F;
         float contentBot = contentTop + this.cachedEffectiveH;
         boolean over = mouseX >= this.x && mouseX <= this.x + this.width && (float)mouseY >= contentTop && (float)mouseY <= contentBot;
         if (!over) {
            return false;
         } else {
            float normalized = (float)delta / 120.0F;
            this.scrollBar.scroll((int)normalized, this.cachedTotalItemH, this.cachedEffectiveH, 30.0F);
            return true;
         }
      } else {
         return false;
      }
   }

   public void drawScreen(int mouseX, int mouseY, float partialTicks, long frameTimeMs) {
      this.drag(mouseX, mouseY);
      counter1[0] = 1;
      ClickGui clickGui = ClickGui.getInstance();
      Colors colors = Colors.getInstance();
      this.scrollBar.tickVisual();
      float fadeProgress = this.panelFadeAnim.tickEased(true);
      this.lastFadeProgress = fadeProgress;
      float expandProgress = this.expandAnim.tickEased(this.open);
      this.lastExpandProgress = expandProgress;
      if (!(fadeProgress < 0.04F)) {
         float totalItemH = this.getTotalItemHeight() - 2.0F;
         this.cachedTotalItemH = totalItemH;
         float appScale = NewVonwareGui.getAppliedScale();
         int guiScale = mc.gameSettings.guiScale > 0 ? mc.gameSettings.guiScale : 1;
         float screenH = (float)mc.displayHeight / (float)guiScale / appScale;
         float maxH = Math.max(20.0F, screenH - ((float)(this.y + this.height) - 6.5F));
         float effectiveH = Math.min(totalItemH, maxH);
         this.cachedEffectiveH = effectiveH;
         this.needsScrollBar = totalItemH > maxH;
         float visibleH = effectiveH * expandProgress;
         this.scrollBar.clampToContent(totalItemH, effectiveH);
         float actualItemH = 0.0F;

         for(Item item : this.items) {
            if (!item.isHidden()) {
               actualItemH += (float)item.getHeight() + 3.0F;
            }
         }

         float clampedItemH = Math.min(actualItemH, effectiveH);
         float closedBottom = (float)(this.y + this.height - 11);
         float openBottom = (float)(this.y + this.height) + clampedItemH - 4.5F;
         float totalBottom = closedBottom + (openBottom - closedBottom) * expandProgress;
         this.lastTotalBottom = totalBottom;
         int bR;
         int bG;
         int bB;
         if ((Boolean)colors.rainbow.getValue()) {
            Color rc = colors.rainbowMode.getValue() == Colors.RainbowMode.Static ? ColorUtil.rainbow(0) : ColorUtil.rainbow((int)((float)counter1[0] * (Float)colors.rainbowHue.getValue()));
            bR = rc.getRed();
            bG = rc.getGreen();
            bB = rc.getBlue();
         } else if ((Boolean)clickGui.pulseGui.getValue()) {
            Color pc = Global.getPulseColor(colors.getGuiColor(), 0);
            bR = pc.getRed();
            bG = pc.getGreen();
            bB = pc.getBlue();
         } else {
            Color cc = colors.getGuiColor();
            bR = cc.getRed();
            bG = cc.getGreen();
            bB = cc.getBlue();
         }

         if ((Boolean)clickGui.globalBackground.getValue()) {
            RenderUtil.drawRect((float)this.x, (float)this.y - 2.0F, (float)(this.x + this.width), totalBottom + 1.5F, argb((int)(30.0F * fadeProgress), bR, bG, bB));
         }

         RenderUtil.drawRect((float)this.x, (float)this.y - 2.0F, (float)(this.x + this.width), (float)(this.y + this.height - 10), argb((int)(90.0F * fadeProgress), bR, bG, bB));
         ClickGui.OutlineMode outlineMode = clickGui.outline.getValue();
         if (outlineMode == ClickGui.OutlineMode.Normal) {
            Render2DMethods.drawOutline((float)this.x, (float)this.y - 1.5F, (float)(this.x + this.width), totalBottom + 1.5F, 1.0F, argb((int)(120.0F * fadeProgress), bR, bG, bB));
         } else if (outlineMode == ClickGui.OutlineMode.OutlineFlow) {
            this.drawFlowingOutline((float)this.x, (float)this.y - 1.5F, (float)(this.x + this.width), totalBottom + 1.5F, 1.5F, bR, bG, bB, fadeProgress, frameTimeMs);
         }

         RenderUtil.drawRect((float)this.x, (float)(this.y + this.height - 10), (float)(this.x + this.width), (float)(this.y + this.height) - 9.5F, argb((int)(110.0F * fadeProgress), bR, bG, bB));
         int textAlpha = (int)(fadeProgress * 255.0F);
         if (textAlpha >= 8) {
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ZERO);
            Vonware.textManager.getCustomFont().drawStringWithShadow(this.getName(), (double)((float)this.x + 6.0F), (double)((float)this.y + 5.0F), textAlpha << 24 | 15658734);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
         }

         float expandThreshold = clickGui.panelAnimation.getValue() == ClickGui.PanelAnimation.Wipe ? 0.02F : 0.0F;
         if (expandProgress > expandThreshold) {
            RenderUtil.drawRect((float)this.x, (float)(this.y + this.height) - 9.0F, (float)(this.x + this.width), (float)(this.y + this.height) + visibleH - 4.5F, argb((int)(18.0F * fadeProgress), bR, bG, bB));
            float startY = (float)(this.getY() + this.getHeight()) - 6.5F;
            float scrollOff = this.needsScrollBar ? this.scrollBar.getVisualScrollOffset() : 0.0F;
            float currentY = startY - scrollOff;
            scissorX = (int)((float)(this.x * guiScale) * appScale);
            scissorY = (int)((float)mc.displayHeight - (startY + visibleH + 2.0F) * (float)guiScale * appScale);
            scissorW = (int)((float)(this.width * guiScale) * appScale);
            scissorH = (int)((visibleH + 2.0F) * (float)guiScale * appScale);
            GL11.glEnable(3089);
            GL11.glScissor(scissorX, scissorY, scissorW, scissorH);
            GlStateManager.pushMatrix();
            float slideOffset = clickGui.panelAnimation.getValue() == ClickGui.PanelAnimation.Pull ? (1.0F - expandProgress) * -effectiveH : 0.0F;
            boolean doHighlight = SearchComponent.highlightAlpha > 0.004F && clickGui.searchProperties.getValue() == ClickGui.SearchProperties.Highlight;
            int hlR = 255;
            int hlG = 255;
            int hlB = 255;
            if (doHighlight) {
               int col = (Integer)clickGui.getHighlightColorSetting().getValue();
               hlR = col >> 16 & 255;
               hlG = col >> 8 & 255;
               hlB = col & 255;
            }

            for(Item item : this.getItems()) {
               int var10002 = counter1[0]++;
               if (!item.isHidden()) {
                  float itemH = (float)item.getHeight() + 1.0F;
                  float itemY = currentY + slideOffset;
                  item.setLocation((float)this.x + 2.5F, itemY);
                  item.setWidth(this.getWidth() - 5 - (this.needsScrollBar ? 3 : 0));
                  float alpha = fadeProgress;
                  if (itemY < startY + 5.0F) {
                     alpha = fadeProgress * Math.max(0.0F, Math.min(1.0F, (itemY - startY) / 5.0F));
                  }

                  if (itemY + itemH > startY + visibleH - 5.0F) {
                     alpha *= Math.max(0.0F, Math.min(1.0F, (startY + visibleH - itemY) / 5.0F));
                  }

                  GlStateManager.color(1.0F, 1.0F, 1.0F, Math.max(0.0F, Math.min(1.0F, alpha)));
                  boolean fullyOpen = this.open && expandProgress >= 0.99F;
                  item.drawScreen(fullyOpen ? mouseX : -9999, fullyOpen ? mouseY : -9999, partialTicks);
                  GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                  if (doHighlight && item instanceof ModuleComponent) {
                     String modName = ((ModuleComponent)item).getModule().getName();
                     if (SearchComponent.isMatch(modName)) {
                        float combinedAlpha = SearchComponent.highlightAlpha * alpha;
                        drawHighlightOverlay((float)this.x + 2.5F, itemY, (float)this.x + 2.5F + (float)item.getWidth(), itemY + (float)item.getHeight(), combinedAlpha, hlR, hlG, hlB, 160);
                     }
                  }

                  currentY += itemH;
               }
            }

            GlStateManager.popMatrix();
            if (this.needsScrollBar) {
               float barX = (float)(this.x + this.width) - 2.5F - 1.5F;
               this.scrollBar.draw(barX, startY, visibleH, totalItemH, effectiveH, bR, bG, bB, mouseX, mouseY);
               if (this.scrollBarMouseDown) {
                  this.scrollBar.mouseDragged(mouseY, visibleH, totalItemH, effectiveH);
               }
            }

            GL11.glDisable(3089);
         }

      }
   }

   private static void drawHighlightOverlay(float x1, float y1, float x2, float y2, float combinedAlpha, int r, int g, int b, int maxOpacity) {
      int overlayAlpha = (int)((float)maxOpacity * combinedAlpha);
      if (overlayAlpha > 0) {
         RenderUtil.drawRect(x1, y1, x2, y2, argb(overlayAlpha, r, g, b));
         int accentAlpha = Math.min(255, (int)(100.0F * combinedAlpha));
         RenderUtil.drawRect(x1, y1, x1 + 1.5F, y2, argb(accentAlpha, Math.min(255, r + 55), Math.min(255, g + 55), Math.min(255, b + 55)));
      }
   }

   private void drawFlowingOutline(float x1, float y1, float x2, float y2, float lineW, int gR, int gG, int gB, float alpha, long frameTimeMs) {
      float w = x2 - x1;
      float h = y2 - y1;
      float per = 2.0F * (w + h);
      int segs = (int)Math.ceil((double)(per / 1.5F));
      float elapsed = (float)(frameTimeMs - outlineGlowStart) * 0.001F;
      float glowPos = (float)Math.sin((double)elapsed) * 0.5F + 0.5F;
      float glowCenter = glowPos * per;
      int lR = Math.min(255, gR + 80);
      int lG = Math.min(255, gG + 80);
      int lB = Math.min(255, gB + 80);
      GlStateManager.disableTexture2D();
      GlStateManager.enableBlend();
      GlStateManager.tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ZERO);
      GlStateManager.disableAlpha();
      GL11.glEnable(2848);
      GL11.glHint(3154, 4354);
      GL11.glLineWidth(lineW);
      GL11.glBegin(3);

      for(int i = 0; i <= segs; ++i) {
         float progress = (float)i / (float)segs;
         float dist = progress * per;
         float px;
         float py;
         if (dist < w) {
            px = x1 + dist;
            py = y1;
         } else if (dist < w + h) {
            px = x2;
            py = y1 + (dist - w);
         } else if (dist < 2.0F * w + h) {
            px = x2 - (dist - w - h);
            py = y2;
         } else {
            px = x1;
            py = y2 - (dist - 2.0F * w - h);
         }

         float dfc = Math.abs(dist - glowCenter);
         float wrapped = Math.min(dfc, per - dfc);
         float r;
         float g;
         float b;
         if (wrapped > 140.0F) {
            r = (float)gR / 255.0F;
            g = (float)gG / 255.0F;
            b = (float)gB / 255.0F;
         } else {
            float nd = wrapped / 140.0F;
            float gs = (float)Math.cos((double)nd * Math.PI / (double)2.0F);
            r = ((float)gR + (float)(lR - gR) * gs) / 255.0F;
            g = ((float)gG + (float)(lG - gG) * gs) / 255.0F;
            b = ((float)gB + (float)(lB - gB) * gs) / 255.0F;
         }

         GL11.glColor4f(r, g, b, alpha);
         GL11.glVertex2f(px, py);
      }

      GL11.glEnd();
      GL11.glDisable(2848);
      GL11.glLineWidth(1.0F);
      GlStateManager.enableTexture2D();
      GlStateManager.disableBlend();
      GlStateManager.enableAlpha();
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
   }

   public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
      if (mouseButton == 0 && this.isHovering(mouseX, mouseY)) {
         this.x2 = this.x - mouseX;
         this.y2 = this.y - mouseY;
         NewVonwareGui.getClickGui().getComponents().forEach((c) -> {
            if (c.drag) {
               c.drag = false;
            }

         });
         this.drag = true;
      } else if (mouseButton == 1 && this.isHovering(mouseX, mouseY)) {
         if (this.open) {
            this.getItems().forEach((itemx) -> {
               if (itemx instanceof ModuleComponent) {
                  ((ModuleComponent)itemx).closeSettings();
               }

            });
         }

         this.open = !this.open;
         if (!this.open) {
            this.scrollBar.resetScroll();
         }

         SoundUtil.playCategoryToggle(this.open);
      } else if (this.open) {
         if (this.needsScrollBar) {
            float startY = (float)(this.y + this.height) - 6.5F;
            float barX = (float)(this.x + this.width) - 2.5F - 1.5F;
            float animH = this.cachedEffectiveH * this.lastExpandProgress;
            if ((float)mouseX >= barX && mouseX <= this.x + this.width && (float)mouseY >= startY && (float)mouseY <= startY + animH) {
               this.scrollBar.mouseClicked(mouseX, mouseY, barX, startY, animH, this.cachedTotalItemH, this.cachedEffectiveH);
               this.scrollBarMouseDown = true;
               return;
            }
         }

         for(Item item : this.getItems()) {
            if (!item.isHidden()) {
               item.mouseClicked(mouseX, mouseY, mouseButton);
            }
         }

      }
   }

   public void mouseReleased(int mouseX, int mouseY, int button) {
      this.scrollBarMouseDown = false;
      this.scrollBar.mouseReleased();
      if (button == 0) {
         this.drag = false;
      }

      if (this.open) {
         for(Item item : this.getItems()) {
            if (!item.isHidden()) {
               item.mouseReleased(mouseX, mouseY, button);
            }
         }

      }
   }

   public void onKeyTyped(char typedChar, int keyCode) {
      if (this.open) {
         for(Item item : this.getItems()) {
            if (!item.isHidden()) {
               item.onKeyTyped(typedChar, keyCode);
            }
         }

      }
   }

   private boolean isHovering(int mouseX, int mouseY) {
      float headerH = (float)(this.height - 10);
      return mouseX >= this.x && mouseX <= this.x + this.width && (float)mouseY >= (float)this.y - 1.5F && (float)mouseY <= (float)this.y + headerH;
   }

   public float getTotalItemHeight() {
      float h = 0.0F;

      for(Item item : this.items) {
         if (!item.isHidden()) {
            h += (float)item.getHeight() + 1.0F;
         }
      }

      return h;
   }

   public void addButton(NewButton btn) {
      this.items.add(btn);
   }

   public float getAnimationProgress() {
      return this.panelFadeAnim.getProgress();
   }

   public float getEasedPanelProgress() {
      return this.lastFadeProgress;
   }

   public float getEasedExpandProgress() {
      return this.lastExpandProgress;
   }

   public int getX() {
      return this.x;
   }

   public void setX(int x) {
      this.x = x;
   }

   public int getY() {
      return this.y;
   }

   public void setY(int y) {
      this.y = y;
   }

   public int getWidth() {
      return this.width;
   }

   public void setWidth(int w) {
      this.width = w;
   }

   public int getHeight() {
      return this.height;
   }

   public boolean isOpen() {
      return this.open;
   }

   public boolean isHidden() {
      return this.hidden;
   }

   public void setHidden(boolean h) {
      this.hidden = h;
   }

   public ArrayList<Item> getItems() {
      return this.items;
   }

   public float getLastTotalBottom() {
      return this.lastTotalBottom;
   }
}
