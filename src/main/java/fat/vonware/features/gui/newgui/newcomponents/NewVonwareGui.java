package fat.vonware.features.gui.newgui.newcomponents;

import fat.vonware.Global;
import fat.vonware.Vonware;
import fat.vonware.features.Feature;
import fat.vonware.features.gui.components.items.Item;
import fat.vonware.features.gui.font.CustomFont;
import fat.vonware.features.gui.newgui.newcomponents.newbuttons.KeyBindButton;
import fat.vonware.features.gui.newgui.newcomponents.newbuttons.ModuleComponent;
import fat.vonware.features.gui.newgui.newcomponents.newbuttons.SearchComponent;
import fat.vonware.features.modules.HudModule;
import fat.vonware.features.modules.Module;
import fat.vonware.features.modules.client.ClickGui;
import fat.vonware.features.modules.client.Manager;
import fat.vonware.manager.BlurManager;
import fat.vonware.util.Timer;
import fat.vonware.util.Animations.AnimationUtil;
import fat.vonware.util.Animations.render.Render2DMethods;
import fat.vonware.util.Animations.render.RenderUtil;
import fat.vonware.util.HUD.Rectangle;
import fat.vonware.util.OtherRenderShit.Render.GlowTextUtil;
import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Random;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.GlStateManager.DestFactor;
import net.minecraft.client.renderer.GlStateManager.SourceFactor;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

public class NewVonwareGui extends GuiScreen {
   private static NewVonwareGui INSTANCE;
   private boolean dragging = false;
   private boolean draggingSecondary = false;
   private HudModule draggedElement;
   private int dragOffsetX;
   private int dragOffsetY;
   private int dragButton = 0;
   private float mouseTooltipAlpha = 0.0F;
   private long mouseTooltipAnimMs = 0L;
   private long lastTooltipPresentMs = 0L;
   private String lastMouseTooltipStr = null;
   private String pendingMouseTooltipStr = null;
   private boolean tooltipSwitching = false;
   private int lastMouseTooltipX = 0;
   private int lastMouseTooltipY = 0;
   private int liveMouseX = 0;
   private int liveMouseY = 0;
   private final AnimationUtil.LerpAnimation descBarWidthAnim = new AnimationUtil.LerpAnimation(0.0F, 0.18F, 0.5F);
   private String prevTooltip = null;
   private static final String[] TIPS = new String[]{"Press Ctrl + F to search modules!", "Left click drag to use hud snap lines and right click drag to ignore them!", "MiddleClick the number in sliders to input your own value!", "the G in the color picker is for using global colors!"};
   private int tipIndex;
   private String tipCurrentText;
   private int tipVisibleChars;
   private TipState tipState;
   private final Timer tipStateTimer;
   private final Timer tipCharTimer;
   private static final long TIP_SHOW_DURATION_MS = 8000L;
   private static final long TIP_CHAR_INTERVAL_MS = 25L;
   private long guiOpenTimeMs;
   private static final long BAR_DELAY_MS = 250L;
   private static final float BAR_BOUNCE_MS = 580.0F;
   private static final float DESC_FADE_MS = 750.0F;
   static boolean guiZoomActive = false;
   private static boolean zoomHandlerRegistered = false;
   private static final float ZOOM_AMOUNT = 10.0F;
   private float appliedScale;
   private static float zoomProgress = 0.0F;
   private static final float ZOOM_IN_SPEED = 0.01F;
   private static final float ZOOM_OUT_SPEED = 0.03F;
   private float hLeftAlpha;
   private float hRightAlpha;
   private float vTopAlpha;
   private float vBottomAlpha;
   private static final float GUIDE_FULL = 0.5F;
   private static final float GUIDE_DIM = 0.08F;
   private static final float GUIDE_SPEED = 0.09F;
   private static final float SNAP_THRESHOLD = 10.0F;
   private boolean snapActiveH;
   private boolean snapActiveV;
   private float snapGlowH;
   private float snapGlowV;
   public static long frameTimeMs = 0L;
   private final SearchComponent searchComponent;
   protected final boolean isHudEditor;
   private final ArrayList<NewComponent> components;
   private static final String[] CHAR_CACHE = new String[128];

   private int mx(int raw) {
      return (int)((float)raw / this.appliedScale);
   }

   private int my(int raw) {
      return (int)((float)raw / this.appliedScale);
   }

   public NewVonwareGui(boolean isHudEditor) {
      this.tipIndex = (new Random()).nextInt(TIPS.length);
      this.tipCurrentText = TIPS[this.tipIndex];
      this.tipVisibleChars = TIPS[this.tipIndex].length();
      this.tipState = NewVonwareGui.TipState.SHOWING;
      this.tipStateTimer = new Timer();
      this.tipCharTimer = new Timer();
      this.guiOpenTimeMs = 0L;
      this.appliedScale = 1.0F;
      this.hLeftAlpha = 0.0F;
      this.hRightAlpha = 0.0F;
      this.vTopAlpha = 0.0F;
      this.vBottomAlpha = 0.0F;
      this.snapActiveH = false;
      this.snapActiveV = false;
      this.snapGlowH = 0.0F;
      this.snapGlowV = 0.0F;
      this.searchComponent = new SearchComponent();
      this.components = new ArrayList();
      this.setInstance();
      this.isHudEditor = isHudEditor;
      this.load();
   }

   public static float getAppliedScale() {
      return INSTANCE != null ? INSTANCE.appliedScale : 1.0F;
   }

   public void initGui() {
      super.initGui();
      this.appliedScale = (Float)ClickGui.getInstance().guiScale.getValue();
      this.guiOpenTimeMs = System.currentTimeMillis();
      this.descBarWidthAnim.reset(false);
      this.mouseTooltipAlpha = 0.0F;
      this.mouseTooltipAnimMs = 0L;
      this.lastTooltipPresentMs = 0L;
      this.lastMouseTooltipStr = null;
      this.pendingMouseTooltipStr = null;
      this.tooltipSwitching = false;
      guiZoomActive = true;
      if (!zoomHandlerRegistered) {
         MinecraftForge.EVENT_BUS.register(NewVonwareGui.ZoomFovHandler.INSTANCE);
         zoomHandlerRegistered = true;
      }

      if (this.isHudEditor) {
         this.hLeftAlpha = this.hRightAlpha = this.vTopAlpha = this.vBottomAlpha = 0.0F;
      }

      this.searchComponent.close();
   }

   public void onGuiClosed() {
      super.onGuiClosed();
      guiZoomActive = false;
      this.searchComponent.close();
   }

   public static NewVonwareGui getInstance() {
      if (INSTANCE == null) {
         INSTANCE = new NewVonwareGui(false);
      }

      return INSTANCE;
   }

   public static NewVonwareGui getClickGui() {
      return getInstance();
   }

   private void setInstance() {
      INSTANCE = this;
   }

   private void load() {
      int x = -107;
      if (this.isHudEditor) {
         ArrayList var10000 = this.components;
         String var10004 = Module.Category.Elements.getName();
         x += 307;
         var10000.add(new NewComponent(var10004, x, 70, true) {
            public void setupItems() {
               counter1 = new int[]{1};
               Vonware.moduleManager.getModulesByCategory(Module.Category.Elements).forEach((module) -> {
                  if (!module.hidden) {
                     this.addButton(new ModuleComponent(module));
                  }

               });
            }
         });
      } else {
         for(final Module.Category category : Vonware.moduleManager.getCategories()) {
            if (category != Module.Category.Elements) {
               this.components.add(new NewComponent(category.getName(), x += (Integer)ClickGui.getInstance().panelSpacing.getValue(), 4, true) {
                  public void setupItems() {
                     counter1 = new int[]{1};
                     Vonware.moduleManager.getModulesByCategory(category).forEach((module) -> {
                        if (!module.hidden) {
                           this.addButton(new ModuleComponent(module));
                        }

                     });
                  }
               });
            }
         }
      }

      this.components.forEach((c) -> c.getItems().sort(Comparator.comparing(Feature::getName)));
   }

   private static int argb(int a, int r, int g, int b) {
      return (a & 255) << 24 | (r & 255) << 16 | (g & 255) << 8 | b & 255;
   }

   private static String cachedChar(char c) {
      return c < 128 ? CHAR_CACHE[c] : String.valueOf(c);
   }

   private static float easeOutBack(float t) {
      float c1 = 0.35F;
      float c3 = 1.35F;
      float tm1 = t - 1.0F;
      return 1.0F + 1.35F * tm1 * tm1 * tm1 + 0.35F * tm1 * tm1;
   }

   private void drawBottomBar() {
      float scaledW = (float)this.width / this.appliedScale;
      float scaledH = (float)this.height / this.appliedScale;
      float barHeight = 40.0F;
      float radius = 20.0F;
      float bx1 = 0.0F;
      float by1 = scaledH - barHeight;
      ClickGui.GuiBlur blurMode = ClickGui.getInstance().guiBlur.getValue();
      if (blurMode != ClickGui.GuiBlur.Off) {
         Render2DMethods.drawPillBlur(bx1, by1, scaledW, scaledH, radius, (float)(Integer)ClickGui.getInstance().blurStrength.getValue());
      }

      Render2DMethods.drawDescriptionPillSurface(bx1, by1, scaledW, scaledH, radius, 1.0F);
   }

   public void drawScreen(int mouseX, int mouseY, float partialTicks) {
      frameTimeMs = System.currentTimeMillis();
      int smx = this.mx(mouseX);
      int smy = this.my(mouseY);
      this.liveMouseX = smx;
      this.liveMouseY = smy;
      Global.updatePulseProgress();
      this.checkMouseWheel();
      ClickGui clickGui = ClickGui.getInstance();
      ClickGui.GuiBlur blurMode = clickGui.guiBlur.getValue();
      if (!this.isHudEditor) {
         this.applySearchFilter();
      }

      if (blurMode == ClickGui.GuiBlur.Panel && this.mc.world != null && BlurManager.INSTANCE != null) {
         BlurManager.INSTANCE.captureAndBlur((float)(Integer)clickGui.blurStrength.getValue());
      }

      if (clickGui.background.getValue() == ClickGui.Background.All) {
         this.drawDefaultBackground();
      }

      if (this.isHudEditor) {
         for(NewComponent comp : this.components) {
            for(Item item : comp.getItems()) {
               if (item instanceof ModuleComponent) {
                  Module mod = ((ModuleComponent)item).getModule();
                  if (mod instanceof HudModule) {
                     HudModule hm = (HudModule)mod;
                     if (hm.isEnabled() && !hm.isAutoPositioned()) {
                        hm.updateBounds();
                        hm.drawEditorBackground(mouseX, mouseY, this.dragging && this.draggedElement == hm && !this.draggingSecondary);
                        hm.drawSecondaryEditorBackground(mouseX, mouseY, this.dragging && this.draggedElement == hm && this.draggingSecondary);
                     }
                  }
               }
            }
         }
      }

      if (this.isHudEditor && (Boolean)Manager.getInstance().guideLines.getValue()) {
         this.drawGuideLines(this.width, this.height);
      }

      if (this.isHudEditor && (Boolean)Manager.getInstance().pixelMarginLines.getValue()) {
         this.drawMarginLines(this.width, this.height);
      }

      GL11.glPushMatrix();
      GL11.glScalef(this.appliedScale, this.appliedScale, 1.0F);
      if (blurMode == ClickGui.GuiBlur.Panel && clickGui.background.getValue() != ClickGui.Background.All && BlurManager.INSTANCE != null && BlurManager.INSTANCE.isAvailable()) {
         for(NewComponent comp : this.components) {
            if (!(comp.getEasedPanelProgress() <= 0.01F)) {
               BlurManager.INSTANCE.drawBlurRect((float)comp.getX(), (float)comp.getY() - 1.5F, (float)(comp.getX() + comp.getWidth()), comp.getLastTotalBottom() + 1.5F, 0);
            }
         }
      }

      ClickGui.Background bg = clickGui.background.getValue();
      if (bg == ClickGui.Background.Panel) {
         for(NewComponent comp : this.components) {
            float fp = comp.getEasedPanelProgress();
            float ep = comp.getEasedExpandProgress();
            if (!(fp <= 0.0F)) {
               float tih = comp.getTotalItemHeight() - 2.0F;
               float closedBottom = (float)(comp.getY() + comp.getHeight() - 11);
               float openBottom = (float)(comp.getY() + comp.getHeight()) + tih - 4.5F;
               float bottom = closedBottom + (openBottom - closedBottom) * ep;
               this.drawGradientRect(comp.getX(), (int)((float)comp.getY() - 1.5F), comp.getX() + comp.getWidth(), (int)(bottom + 1.5F), -1072689136, -804253680);
            }
         }
      }

      if (!this.isHudEditor) {
         this.applySearchFilter();
      }

      this.components.forEach((c) -> c.drawScreen(smx, smy, partialTicks, frameTimeMs));
      if (!this.isHudEditor) {
         this.restoreSearchFilter();
      }

      if (!this.isHudEditor) {
         int matchCount = this.countSearchMatches();
         float scaledW = (float)this.width / this.appliedScale;
         float scaledH = (float)this.height / this.appliedScale;
         this.searchComponent.draw(scaledW, scaledH, matchCount, frameTimeMs);
         GlStateManager.enableTexture2D();
         GlStateManager.disableBlend();
         GlStateManager.enableAlpha();
         GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      }

      String savedTooltip = ModuleComponent.pendingMouseTooltip;
      ClickGui.Description descMode = clickGui.description.getValue();
      if (descMode != ClickGui.Description.Bottom) {
         long nowMs = frameTimeMs;
         long deltaMs = this.mouseTooltipAnimMs > 0L ? Math.min(nowMs - this.mouseTooltipAnimMs, 50L) : 0L;
         this.mouseTooltipAnimMs = nowMs;
         boolean hasTooltip = savedTooltip != null;
         boolean recentlyHad = nowMs - this.lastTooltipPresentMs < 200L;
         if (hasTooltip) {
            this.lastTooltipPresentMs = nowMs;
            boolean textChanged = !savedTooltip.equals(this.lastMouseTooltipStr);
            if (textChanged && recentlyHad && this.mouseTooltipAlpha > 0.05F) {
               this.tooltipSwitching = true;
               this.pendingMouseTooltipStr = savedTooltip;
               this.lastMouseTooltipX = ModuleComponent.pendingMouseTooltipX;
               this.lastMouseTooltipY = ModuleComponent.pendingMouseTooltipY;
            } else if (!this.tooltipSwitching) {
               this.lastMouseTooltipStr = savedTooltip;
               this.lastMouseTooltipX = ModuleComponent.pendingMouseTooltipX;
               this.lastMouseTooltipY = ModuleComponent.pendingMouseTooltipY;
            }

            ModuleComponent.pendingMouseTooltip = null;
         }

         if (this.tooltipSwitching) {
            this.mouseTooltipAlpha = Math.max(0.0F, this.mouseTooltipAlpha - 0.013333334F * (float)deltaMs);
            if (this.mouseTooltipAlpha <= 0.0F) {
               this.lastMouseTooltipStr = this.pendingMouseTooltipStr;
               this.pendingMouseTooltipStr = null;
               this.tooltipSwitching = false;
            }
         } else if (hasTooltip) {
            float speed = recentlyHad ? 0.013333334F : 0.0033333334F;
            this.mouseTooltipAlpha = Math.min(1.0F, this.mouseTooltipAlpha + speed * (float)deltaMs);
         } else {
            this.lastMouseTooltipX = this.liveMouseX;
            this.lastMouseTooltipY = this.liveMouseY;
            this.mouseTooltipAlpha = Math.max(0.0F, this.mouseTooltipAlpha - 0.0033333334F * (float)deltaMs);
         }

         if (this.mouseTooltipAlpha > 0.01F && this.lastMouseTooltipStr != null) {
            float SCALE = 0.95F;
            String text = this.lastMouseTooltipStr;
            CustomFont cf = Vonware.textManager.getCustomFont();
            float targetW = (float)(cf.getStringWidth(text) + 6);
            float unscaledH = (float)(cf.getHeight() + 4);
            float alpha = this.mouseTooltipAlpha;
            float rx = (float)this.lastMouseTooltipX + 4.0F;
            float ry = (float)this.lastMouseTooltipY - 3.0F;
            GlStateManager.pushMatrix();
            GlStateManager.translate(rx, ry, 0.0F);
            GlStateManager.scale(0.95F, 0.95F, 1.0F);
            RenderUtil.drawRect(0.0F, 0.0F, targetW, unscaledH, argb((int)(140.0F * alpha), 14, 14, 14));
            if (alpha > 0.05F) {
               Vonware.textManager.getCustomFont().drawStringWithShadow(text, (double)3.0F, (double)2.0F, argb((int)(255.0F * alpha), 255, 255, 255));
            }

            GlStateManager.popMatrix();
         }
      }

      if (!this.isHudEditor && (Boolean)clickGui.clientInfo.getValue()) {
         ClickGui.Description desc = clickGui.description.getValue();
         long now = frameTimeMs;
         long barElapsed = now - this.guiOpenTimeMs - 250L;
         if (barElapsed >= 0L) {
            float barT = Math.min(1.0F, (float)barElapsed / 580.0F);
            float eased = easeOutBack(barT);
            float slideOffset = (1.0F - eased) * ((float)this.width / this.appliedScale);
            GL11.glPushMatrix();
            GL11.glTranslatef(slideOffset, 0.0F, 0.0F);
            float scaledH2 = (float)this.height / this.appliedScale;
            float barTop = scaledH2 - 40.0F;
            if (blurMode == ClickGui.GuiBlur.Panel && BlurManager.INSTANCE != null && BlurManager.INSTANCE.isAvailable()) {
               for(NewComponent comp : this.components) {
                  if (!(comp.getEasedPanelProgress() <= 0.01F)) {
                     float compBottom = comp.getLastTotalBottom() + 1.5F;
                     if (compBottom > barTop) {
                        BlurManager.INSTANCE.drawBlurRect((float)comp.getX() - 1.0F, barTop, (float)(comp.getX() + comp.getWidth()) + 1.0F, scaledH2, 0);
                        GlStateManager.enableTexture2D();
                        GlStateManager.disableBlend();
                        GlStateManager.enableAlpha();
                        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                     }
                  }
               }
            }

            this.drawBottomBar();
            if (desc == ClickGui.Description.Bottom) {
               long descElapsed = barElapsed - 580L;
               float descAlpha = descElapsed <= 0L ? 0.0F : Math.min(1.0F, (float)descElapsed / 750.0F);
               if (savedTooltip != null && descAlpha > 0.0F) {
                  if (!savedTooltip.equals(this.prevTooltip)) {
                     this.descBarWidthAnim.reset(false);
                  }

                  this.prevTooltip = savedTooltip;
                  this.drawBottomBarDescription(savedTooltip, descAlpha);
               } else {
                  this.prevTooltip = null;
                  this.descBarWidthAnim.tick(0.0F);
               }
            }

            GL11.glPopMatrix();
         }
      }

      GL11.glPopMatrix();
      if (BlurManager.INSTANCE != null) {
         BlurManager.INSTANCE.endFrame();
      }

      if (this.dragging && this.draggedElement != null) {
         float m = Manager.getInstance().getMarginPixels();
         if (this.draggingSecondary) {
            Rectangle secRect = this.draggedElement.getSecondaryRect();
            if (secRect != null) {
               float newX = Math.max(m, Math.min((float)(mouseX - this.dragOffsetX), (float)this.width - secRect.getWidth() - m));
               float newY = Math.max(m, Math.min((float)(mouseY - this.dragOffsetY), (float)this.height - secRect.getHeight() - m));
               this.draggedElement.setSecondaryPosition(newX, newY);
            }
         } else {
            float w = this.draggedElement.rect.getWidth();
            float h = this.draggedElement.rect.getHeight();
            float newX = Math.max(m, Math.min((float)(mouseX - this.dragOffsetX), (float)this.width - w - m));
            float newY = Math.max(m, Math.min((float)(mouseY - this.dragOffsetY), (float)this.height - h - m));
            this.snapActiveV = false;
            this.snapActiveH = false;
            if (this.dragButton == 0) {
               float cx = (float)this.width * 0.5F;
               float cy = (float)this.height * 0.5F;
               float dVL = Math.abs(newX - cx);
               float dVC = Math.abs(newX + w * 0.5F - cx);
               float dVR = Math.abs(newX + w - cx);
               float minV = Math.min(dVL, Math.min(dVC, dVR));
               if (minV <= 10.0F) {
                  this.snapActiveV = true;
                  if (dVL <= dVC && dVL <= dVR) {
                     newX = cx;
                  } else if (dVC <= dVL && dVC <= dVR) {
                     newX = cx - w * 0.5F;
                  } else {
                     newX = cx - w;
                  }
               }

               float dHT = Math.abs(newY - cy);
               float dHM = Math.abs(newY + h * 0.5F - cy);
               float dHB = Math.abs(newY + h - cy);
               float minH = Math.min(dHT, Math.min(dHM, dHB));
               if (minH <= 10.0F) {
                  this.snapActiveH = true;
                  if (dHT <= dHM && dHT <= dHB) {
                     newY = cy;
                  } else if (dHM <= dHT && dHM <= dHB) {
                     newY = cy - h * 0.5F;
                  } else {
                     newY = cy - h;
                  }
               }
            }

            this.draggedElement.setPosition(newX, newY);
         }
      }

      if (!this.isHudEditor && !(Boolean)clickGui.clientInfo.getValue()) {
         ClickGui.Description desc = clickGui.description.getValue();
         if (desc == ClickGui.Description.Bottom && savedTooltip != null) {
            float barH = 20.0F;
            float pillRadius = barH / 2.0F;
            float padX = 12.0F;
            float marginL = 8.0F;
            float marginB = 8.0F;
            CustomFont df = Vonware.textManager.getCustomFont();
            float textW = (float)df.getStringWidth(savedTooltip);
            float textH = (float)df.getHeight();
            float targetW = textW + padX * 2.0F;
            float animW = this.descBarWidthAnim.tick(targetW);
            float bx2 = marginL + animW;
            float by2 = (float)this.height - marginB;
            float by1 = by2 - barH;
            if (blurMode == ClickGui.GuiBlur.Panel) {
               Render2DMethods.drawPillBlur(marginL, by1, bx2, by2, pillRadius, (float)(Integer)clickGui.blurStrength.getValue());
            }

            Render2DMethods.drawDescriptionPillSurface(marginL, by1, bx2, by2, pillRadius, 1.0F);
            if (animW > padX && textW > 0.0F) {
               float tx = marginL + padX;
               float ty = by1 + (barH - textH) / 2.0F - 0.5F;
               int guiScale = this.mc.gameSettings.guiScale > 0 ? this.mc.gameSettings.guiScale : 1;
               int scX = (int)(marginL * (float)guiScale);
               int scY = (int)((float)this.mc.displayHeight - by2 * (float)guiScale);
               int scW = Math.max(0, (int)(animW * (float)guiScale));
               int scH = Math.max(0, (int)((by2 - by1) * (float)guiScale));
               GL11.glEnable(3089);
               GL11.glScissor(scX, scY, scW, scH);
               df.drawStringWithShadow(savedTooltip, (double)tx, (double)ty, -1);
               GL11.glDisable(3089);
            }
         } else {
            this.descBarWidthAnim.tick(0.0F);
         }
      }

   }

   private void drawMarginLines(int screenW, int screenH) {
      float m = Manager.getInstance().getMarginPixels();
      if (!(m <= 0.0F)) {
         GlStateManager.disableTexture2D();
         GlStateManager.enableBlend();
         GlStateManager.tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ZERO);
         GlStateManager.disableAlpha();
         GL11.glEnable(2848);
         GL11.glHint(3154, 4354);
         GL11.glLineWidth(1.0F);
         Color accent = Global.getGlobalColorRainbowS();
         GlStateManager.color((float)accent.getRed() / 255.0F, (float)accent.getGreen() / 255.0F, (float)accent.getBlue() / 255.0F, 0.35F);
         GL11.glBegin(2);
         GL11.glVertex2f(m, m);
         GL11.glVertex2f((float)screenW - m, m);
         GL11.glVertex2f((float)screenW - m, (float)screenH - m);
         GL11.glVertex2f(m, (float)screenH - m);
         GL11.glEnd();
         GL11.glDisable(2848);
         GL11.glLineWidth(1.0F);
         GlStateManager.enableTexture2D();
         GlStateManager.disableBlend();
         GlStateManager.enableAlpha();
         GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      }
   }

   private void applySearchFilter() {
      if (SearchComponent.searchActive && !SearchComponent.activeQuery.isEmpty()) {
         ClickGui.SearchProperties mode = ClickGui.getInstance().searchProperties.getValue();
         if (mode == ClickGui.SearchProperties.Remove) {
            for(NewComponent comp : this.components) {
               for(Item item : comp.getItems()) {
                  if (item instanceof ModuleComponent) {
                     String name = ((ModuleComponent)item).getModule().getName();
                     if (!SearchComponent.isMatch(name)) {
                        item.setHidden(true);
                     }
                  }
               }
            }

         }
      }
   }

   private void restoreSearchFilter() {
      if (SearchComponent.searchActive && !SearchComponent.activeQuery.isEmpty()) {
         ClickGui.SearchProperties mode = ClickGui.getInstance().searchProperties.getValue();
         if (mode == ClickGui.SearchProperties.Remove) {
            for(NewComponent comp : this.components) {
               for(Item item : comp.getItems()) {
                  if (item instanceof ModuleComponent) {
                     String name = ((ModuleComponent)item).getModule().getName();
                     if (!SearchComponent.isMatch(name)) {
                        item.setHidden(false);
                     }
                  }
               }
            }

         }
      }
   }

   private int countSearchMatches() {
      if (SearchComponent.searchActive && !SearchComponent.activeQuery.isEmpty()) {
         int count = 0;

         for(NewComponent comp : this.components) {
            for(Item item : comp.getItems()) {
               if (item instanceof ModuleComponent && !item.isHidden() && SearchComponent.isMatch(((ModuleComponent)item).getModule().getName())) {
                  ++count;
               }
            }
         }

         return count;
      } else {
         return 0;
      }
   }

   private void drawGuideLines(int screenW, int screenH) {
      float cx = (float)screenW * 0.5F;
      float cy = (float)screenH * 0.5F;
      float hLeftT = 0.5F;
      float hRightT = 0.5F;
      float vTopT = 0.5F;
      float vBotT = 0.5F;
      if (this.dragging && this.draggedElement != null) {
         Rectangle r = this.draggedElement.rect;
         float rx1 = r.getX();
         float ry1 = r.getY();
         float rx2 = rx1 + r.getWidth();
         float ry2 = ry1 + r.getHeight();
         boolean crossesH = ry1 <= cy && cy <= ry2;
         boolean crossesV = rx1 <= cx && cx <= rx2;
         if (crossesH && rx1 < cx) {
            hLeftT = 0.08F;
         }

         if (crossesH && rx2 > cx) {
            hRightT = 0.08F;
         }

         if (crossesV && ry1 < cy) {
            vTopT = 0.08F;
         }

         if (crossesV && ry2 > cy) {
            vBotT = 0.08F;
         }
      }

      this.hLeftAlpha += (hLeftT - this.hLeftAlpha) * 0.09F;
      this.hRightAlpha += (hRightT - this.hRightAlpha) * 0.09F;
      this.vTopAlpha += (vTopT - this.vTopAlpha) * 0.09F;
      this.vBottomAlpha += (vBotT - this.vBottomAlpha) * 0.09F;
      float maxA = Math.max(Math.max(this.hLeftAlpha, this.hRightAlpha), Math.max(this.vTopAlpha, this.vBottomAlpha));
      if (!(maxA < 0.005F)) {
         GlStateManager.disableTexture2D();
         GlStateManager.enableBlend();
         GlStateManager.tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ZERO);
         GlStateManager.disableAlpha();
         GL11.glEnable(2848);
         GL11.glHint(3154, 4354);
         GL11.glLineWidth(1.0F);
         GL11.glBegin(1);
         if (this.hLeftAlpha > 0.005F) {
            GlStateManager.color(0.86F, 0.86F, 0.9F, this.hLeftAlpha);
            GL11.glVertex2f(0.0F, cy);
            GL11.glVertex2f(cx, cy);
         }

         if (this.hRightAlpha > 0.005F) {
            GlStateManager.color(0.86F, 0.86F, 0.9F, this.hRightAlpha);
            GL11.glVertex2f(cx, cy);
            GL11.glVertex2f((float)screenW, cy);
         }

         if (this.vTopAlpha > 0.005F) {
            GlStateManager.color(0.86F, 0.86F, 0.9F, this.vTopAlpha);
            GL11.glVertex2f(cx, 0.0F);
            GL11.glVertex2f(cx, cy);
         }

         if (this.vBottomAlpha > 0.005F) {
            GlStateManager.color(0.86F, 0.86F, 0.9F, this.vBottomAlpha);
            GL11.glVertex2f(cx, cy);
            GL11.glVertex2f(cx, (float)screenH);
         }

         GL11.glEnd();
         if (maxA > 0.005F) {
            float tickHalf = 6.0F;
            GlStateManager.color(0.86F, 0.86F, 0.9F, maxA);
            GL11.glLineWidth(1.5F);
            GL11.glBegin(1);
            GL11.glVertex2f(cx - tickHalf, cy);
            GL11.glVertex2f(cx + tickHalf, cy);
            GL11.glVertex2f(cx, cy - tickHalf);
            GL11.glVertex2f(cx, cy + tickHalf);
            GL11.glEnd();
            GL11.glLineWidth(1.0F);
         }

         this.snapGlowH += ((this.dragging && this.snapActiveH ? 1.0F : 0.0F) - this.snapGlowH) * 0.25F;
         this.snapGlowV += ((this.dragging && this.snapActiveV ? 1.0F : 0.0F) - this.snapGlowV) * 0.25F;
         if (this.snapGlowH > 0.005F || this.snapGlowV > 0.005F) {
            Color snapCol = Global.getGlobalColorRainbowS();
            float sr = (float)snapCol.getRed() / 255.0F;
            float sg = (float)snapCol.getGreen() / 255.0F;
            float sb = (float)snapCol.getBlue() / 255.0F;
            GL11.glLineWidth(2.0F);
            GL11.glBegin(1);
            if (this.snapGlowH > 0.005F) {
               GlStateManager.color(sr, sg, sb, this.snapGlowH * 0.9F);
               GL11.glVertex2f(0.0F, cy);
               GL11.glVertex2f((float)screenW, cy);
            }

            if (this.snapGlowV > 0.005F) {
               GlStateManager.color(sr, sg, sb, this.snapGlowV * 0.9F);
               GL11.glVertex2f(cx, 0.0F);
               GL11.glVertex2f(cx, (float)screenH);
            }

            GL11.glEnd();
            GL11.glLineWidth(1.0F);
         }

         GL11.glDisable(2848);
         GL11.glLineWidth(1.0F);
         GlStateManager.enableTexture2D();
         GlStateManager.disableBlend();
         GlStateManager.enableAlpha();
         GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      }
   }

   private void updateAndDrawTips(float scaledW, float baseY) {
      if (TIPS.length != 0) {
         switch (this.tipState) {
            case SHOWING:
               if (this.tipStateTimer.getPassedTimeMs() >= 8000L) {
                  this.tipState = NewVonwareGui.TipState.REMOVING;
                  this.tipStateTimer.reset();
                  this.tipCharTimer.reset();
               }
               break;
            case REMOVING:
               if (this.tipCharTimer.getPassedTimeMs() >= 25L) {
                  this.tipCharTimer.reset();
                  if (this.tipVisibleChars > 0) {
                     --this.tipVisibleChars;
                  } else {
                     this.tipIndex = (this.tipIndex + 1) % TIPS.length;
                     this.tipCurrentText = TIPS[this.tipIndex];
                     this.tipVisibleChars = 0;
                     this.tipState = NewVonwareGui.TipState.ADDING;
                     this.tipStateTimer.reset();
                  }
               }
               break;
            case ADDING:
               if (this.tipCharTimer.getPassedTimeMs() >= 25L) {
                  this.tipCharTimer.reset();
                  if (this.tipVisibleChars < this.tipCurrentText.length()) {
                     ++this.tipVisibleChars;
                  } else {
                     this.tipState = NewVonwareGui.TipState.SHOWING;
                     this.tipStateTimer.reset();
                  }
               }
         }

         if (this.tipVisibleChars > 0) {
            String visible = this.tipCurrentText.substring(0, Math.min(this.tipVisibleChars, this.tipCurrentText.length()));
            CustomFont tf = Vonware.textManager.getCustomFont();
            float fullW = (float)tf.getStringWidth(this.tipCurrentText);
            float tx = scaledW / 2.0F - fullW / 2.0F;
            GlowTextUtil.drawGlowString(visible, tx, baseY, Global.getGlobalColorRainbowS());
         }
      }
   }

   private void drawBottomBarDescription(String text, float descAlpha) {
      ClickGui.GuiBlur blurMode = ClickGui.getInstance().guiBlur.getValue();
      CustomFont df = Vonware.textManager.getCustomFont();
      float scaledH = (float)this.height / this.appliedScale;
      float fullBarH = 40.0F;
      float gapAbove = 5.0F;
      float barH = 20.0F;
      float pillRadius = barH / 2.0F;
      float padX = 12.0F;
      float marginL = 8.0F;
      float textW = (float)df.getStringWidth(text);
      float textH = (float)df.getHeight();
      float targetW = textW + padX * 2.0F;
      float animW = this.descBarWidthAnim.tick(targetW);
      float bx2 = marginL + animW;
      float by1 = scaledH - fullBarH - barH - gapAbove;
      float by2 = by1 + barH;
      if (blurMode == ClickGui.GuiBlur.Panel) {
         Render2DMethods.drawPillBlur(marginL, by1, bx2, by2, pillRadius, (float)(Integer)ClickGui.getInstance().blurStrength.getValue());
      }

      Render2DMethods.drawDescriptionPillSurface(marginL, by1, bx2, by2, pillRadius, descAlpha);
      if (descAlpha > 0.01F) {
         float tx = marginL + padX;
         float ty = by1 + (barH - textH) / 2.0F - 0.5F;
         int textCol = argb((int)(255.0F * descAlpha), 255, 255, 255);
         int guiScale = this.mc.gameSettings.guiScale > 0 ? this.mc.gameSettings.guiScale : 1;
         int scX = (int)(marginL * this.appliedScale * (float)guiScale);
         int scY = (int)((float)this.mc.displayHeight - by2 * this.appliedScale * (float)guiScale);
         int scW = Math.max(0, (int)(animW * this.appliedScale * (float)guiScale));
         int scH = Math.max(0, (int)((by2 - by1) * this.appliedScale * (float)guiScale));
         GL11.glEnable(3089);
         GL11.glScissor(scX, scY, scW, scH);
         df.drawStringWithShadow(text, (double)tx, (double)ty, textCol);
         GL11.glDisable(3089);
      }

   }

   public void mouseClicked(int mouseX, int mouseY, int clickedButton) {
      int smx = this.mx(mouseX);
      int smy = this.my(mouseY);
      if (!this.isHudEditor) {
         this.applySearchFilter();
      }

      this.components.forEach((c) -> c.mouseClicked(smx, smy, clickedButton));
      if (!this.isHudEditor) {
         this.restoreSearchFilter();
      }

      if (this.isHudEditor && (clickedButton == 0 || clickedButton == 1)) {
         for(int i = this.components.size() - 1; i >= 0; --i) {
            for(int j = ((NewComponent)this.components.get(i)).getItems().size() - 1; j >= 0; --j) {
               Item item = (Item)((NewComponent)this.components.get(i)).getItems().get(j);
               if (item instanceof ModuleComponent) {
                  Module mod = ((ModuleComponent)item).getModule();
                  if (mod instanceof HudModule) {
                     HudModule hm = (HudModule)mod;
                     if (hm.isEnabled() && !hm.isAutoPositioned()) {
                        hm.updateBounds();
                        Rectangle secRect = hm.getSecondaryRect();
                        if (secRect != null && secRect.inBound(mouseX, mouseY)) {
                           this.dragging = true;
                           this.draggingSecondary = true;
                           this.draggedElement = hm;
                           this.dragButton = clickedButton;
                           this.dragOffsetX = (int)((float)mouseX - secRect.getX());
                           this.dragOffsetY = (int)((float)mouseY - secRect.getY());
                           return;
                        }

                        if (hm.rect.inBound(mouseX, mouseY)) {
                           this.dragging = true;
                           this.draggingSecondary = false;
                           this.draggedElement = hm;
                           this.dragButton = clickedButton;
                           this.dragOffsetX = (int)((float)mouseX - hm.rect.getX());
                           this.dragOffsetY = (int)((float)mouseY - hm.rect.getY());
                           return;
                        }
                     }
                  }
               }
            }
         }
      }

   }

   public void mouseReleased(int mouseX, int mouseY, int releaseButton) {
      int smx = this.mx(mouseX);
      int smy = this.my(mouseY);
      this.components.forEach((c) -> c.mouseReleased(smx, smy, releaseButton));
      if ((releaseButton == 0 || releaseButton == 1) && this.dragging && releaseButton == this.dragButton) {
         this.dragging = false;
         this.draggingSecondary = false;
         this.snapActiveH = false;
         this.snapActiveV = false;
         if (this.draggedElement != null) {
            this.draggedElement.stopDragging();
            this.draggedElement = null;
         }

         this.dragOffsetX = this.dragOffsetY = 0;
      }

   }

   public void keyTyped(char typedChar, int keyCode) throws IOException {
      if (keyCode != 33 || !Keyboard.isKeyDown(29) && !Keyboard.isKeyDown(157)) {
         if (this.searchComponent.isOpen() && !this.isAnyBindListening()) {
            this.searchComponent.onKeyTyped(typedChar, keyCode);
         } else {
            super.keyTyped(typedChar, keyCode);
            this.components.forEach((c) -> c.onKeyTyped(typedChar, keyCode));
         }
      } else {
         this.searchComponent.toggle();
      }
   }

   private boolean isAnyBindListening() {
      for(NewComponent comp : this.components) {
         for(Item item : comp.getItems()) {
            if (item instanceof ModuleComponent) {
               ModuleComponent mc = (ModuleComponent)item;
               if (mc.isSettingsOpen()) {
                  for(Item sub : mc.getSubItems()) {
                     if (sub instanceof KeyBindButton && ((KeyBindButton)sub).isListening) {
                        return true;
                     }
                  }
               }
            }
         }
      }

      return false;
   }

   public void checkMouseWheel() {
      int dWheel = Mouse.getDWheel();
      if (dWheel != 0) {
         for(NewComponent comp : this.components) {
            if (comp.scroll(dWheel, this.liveMouseX, this.liveMouseY)) {
               return;
            }
         }

         int move = dWheel < 0 ? -10 : 10;
         this.components.forEach((c) -> c.setY(c.getY() + move));
      }
   }

   public boolean doesGuiPauseGame() {
      return false;
   }

   public void updateModule(Module module) {
      for(NewComponent comp : this.components) {
         for(Item item : comp.getItems()) {
            if (item instanceof ModuleComponent) {
               ModuleComponent mb = (ModuleComponent)item;
               if (module != null && module.equals(mb.getModule())) {
                  mb.initSettings();
               }
            }
         }
      }

   }

   public NewComponent getComponentByName(String name) {
      for(NewComponent c : this.components) {
         if (c.getName().equalsIgnoreCase(name)) {
            return c;
         }
      }

      return null;
   }

   public boolean isHudEditor() {
      return this.isHudEditor;
   }

   public ArrayList<NewComponent> getComponents() {
      return this.components;
   }

   static {
      for(int i = 0; i < 128; ++i) {
         CHAR_CACHE[i] = String.valueOf((char)i);
      }

   }

   private static enum TipState {
      SHOWING,
      REMOVING,
      ADDING;
   }

   public static class ZoomFovHandler {
      public static final ZoomFovHandler INSTANCE = new ZoomFovHandler();

      private ZoomFovHandler() {
      }

      @SubscribeEvent
      public void onFov(EntityViewRenderEvent.FOVModifier event) {
         ClickGui clickGui = ClickGui.getInstance();
         boolean shouldZoom = NewVonwareGui.guiZoomActive && clickGui != null && clickGui.guiBlur.getValue() == ClickGui.GuiBlur.All;
         float target = shouldZoom ? 10.0F : 0.0F;
         float speed = shouldZoom ? 0.01F : 0.03F;
         NewVonwareGui.zoomProgress = NewVonwareGui.zoomProgress + (target - NewVonwareGui.zoomProgress) * speed;
         if (!NewVonwareGui.guiZoomActive && NewVonwareGui.zoomProgress < 0.05F) {
            MinecraftForge.EVENT_BUS.unregister(this);
            NewVonwareGui.zoomHandlerRegistered = false;
            NewVonwareGui.zoomProgress = 0.0F;
         } else {
            event.setFOV(event.getFOV() - NewVonwareGui.zoomProgress);
         }
      }
   }
}
