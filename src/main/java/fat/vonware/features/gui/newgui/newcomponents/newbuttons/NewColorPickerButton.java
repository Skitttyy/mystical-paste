package fat.vonware.features.gui.newgui.newcomponents.newbuttons;

import fat.vonware.Global;
import fat.vonware.Vonware;
import fat.vonware.features.gui.components.items.Item;
import fat.vonware.features.gui.components.items.buttons.render.ColorPickerConfig;
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
import java.nio.IntBuffer;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.BufferUtils;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

public class NewColorPickerButton extends Item {
   private final Setting<Integer> colorSetting;
   private boolean draggingHue = false;
   private boolean draggingSV = false;
   private boolean isOpen = false;
   private final AnimationUtil.DeltaAnimation openAnimation = new AnimationUtil.DeltaAnimation(0.0F, 180.0F);
   private float hue = 0.0F;
   private float saturation = 1.0F;
   private float value = 1.0F;
   private boolean useGlobalColor = true;
   private int savedCustomColor = -1;
   private boolean hexEditing = false;
   private String hexInput = "";
   private boolean rgbSliderMode = false;
   private float modeToggleAnim = 0.0F;
   private int activeRgbField = -1;
   private String rgbFieldText = "";
   private int draggingRgbSlider = -1;
   private long cursorBlinkTime = System.currentTimeMillis();
   private long copyFeedbackTime = 0L;
   private long pasteFeedbackTime = 0L;
   private final AnimationUtil.LerpAnimation copyHoverAnim = new AnimationUtil.LerpAnimation(0.0F, 0.15F, 0.01F);
   private final AnimationUtil.LerpAnimation pasteHoverAnim = new AnimationUtil.LerpAnimation(0.0F, 0.15F, 0.01F);
   private static int clipboardColor = -1;
   private static final float PADDING = 2.0F;
   private static final float PREVIEW_SIZE = 9.0F;
   private static final float HUE_HEIGHT = 7.0F;
   private static final long BLINK_INTERVAL = 530L;
   private static final IntBuffer SCISSOR_BUF = BufferUtils.createIntBuffer(16);

   private static int argb(int a, int r, int g, int b) {
      return (a & 255) << 24 | (r & 255) << 16 | (g & 255) << 8 | b & 255;
   }

   private static void drawOutline(float x1, float y1, float x2, float y2, int color) {
      float t = 0.5F;
      RenderUtil.drawRect(x1, y1, x2, y1 + t, color);
      RenderUtil.drawRect(x1, y2 - t, x2, y2, color);
      RenderUtil.drawRect(x1, y1 + t, x1 + t, y2 - t, color);
      RenderUtil.drawRect(x2 - t, y1 + t, x2, y2 - t, color);
   }

   public NewColorPickerButton(Setting<Integer> colorSetting) {
      super(colorSetting.getName());
      this.colorSetting = colorSetting;
      this.width = 15;
      ColorPickerConfig.PickerState state = ColorPickerConfig.getState(colorSetting, colorSetting.colorPickerDefaultGlobal);
      this.useGlobalColor = state.useGlobalColor;
      this.rgbSliderMode = state.rgbSliderMode;
      this.savedCustomColor = state.savedCustomColor != -1 ? state.savedCustomColor : (Integer)colorSetting.getValue();
      this.syncHSVFromSetting();
   }

   private Color computeBaseColor() {
      Colors colors = Colors.getInstance();
      ClickGui clickGui = ClickGui.getInstance();
      if ((Boolean)colors.rainbow.getValue()) {
         return colors.rainbowMode.getValue() == Colors.RainbowMode.Static ? ColorUtil.rainbow(0) : ColorUtil.rainbow((int)((float)NewComponent.counter1[0] * (Float)colors.rainbowHue.getValue()));
      } else {
         return (Boolean)clickGui.pulseGui.getValue() ? Global.getPulseColor(colors.getGuiColor(), 0) : colors.getCurrentColor();
      }
   }

   private boolean showCursor() {
      return (System.currentTimeMillis() - this.cursorBlinkTime) / 530L % 2L == 0L;
   }

   private void resetCursorBlink() {
      this.cursorBlinkTime = System.currentTimeMillis();
   }

   private void syncHSVFromSetting() {
      Color current = this.getEffectiveColor();
      float[] hsv = ColorUtil.rgbToHsv(current);
      this.hue = hsv[0];
      this.saturation = hsv[1];
      this.value = hsv[2];
   }

   private Color getColorFromSetting() {
      int c = (Integer)this.colorSetting.getValue();
      return new Color(c >> 16 & 255, c >> 8 & 255, c & 255);
   }

   private void setColorToSetting(Color color) {
      int c = (color.getRed() & 255) << 16 | (color.getGreen() & 255) << 8 | color.getBlue() & 255;
      this.colorSetting.setValue(c);
   }

   private Color getEffectiveColor() {
      if (this.useGlobalColor) {
         Colors colors = Colors.getInstance();
         if ((Boolean)colors.rainbow.getValue()) {
            return ColorUtil.rainbow(0);
         } else {
            ClickGui clickGui = ClickGui.getInstance();
            return (Boolean)clickGui.pulseGui.getValue() ? Global.getPulseColor(colors.getGuiColor(), 0) : colors.getCurrentColor();
         }
      } else {
         return this.getColorFromSetting();
      }
   }

   private void updateColorFromHSV() {
      Color newColor = ColorUtil.hsvToRgb(this.hue, this.saturation, this.value);
      if (!this.useGlobalColor) {
         this.setColorToSetting(newColor);
         this.savedCustomColor = (Integer)this.colorSetting.getValue();
      }

   }

   private float getFullExpandedHeight() {
      float availableWidth = (float)this.width + 8.0F - 4.0F;
      float rgbHeight = this.rgbSliderMode ? 27.0F : 8.0F;
      return 15.0F + availableWidth + 2.0F + 7.0F + 2.0F + 10.0F + 14.0F + 10.0F + rgbHeight + 4.0F;
   }

   private float getAnimatedHeight() {
      float easedProgress = AnimationUtil.easeInOutCubic(this.openAnimation.getProgress());
      float fullHeight = this.getFullExpandedHeight();
      return 13.0F + (fullHeight - 13.0F) * easedProgress;
   }

   private float headerPreviewX() {
      float circleCenterX = this.x + (float)this.width + 8.0F - 3.5F - 4.0F;
      return circleCenterX - 4.5F;
   }

   private float headerPreviewY() {
      return this.y + 2.0F;
   }

   private float headerBadgeX() {
      float badgeSize = 9.0F;
      return this.headerPreviewX() - badgeSize - 2.0F;
   }

   private float headerBadgeY() {
      return this.headerPreviewY();
   }

   public void drawScreen(int mouseX, int mouseY, float partialTicks) {
      Color baseColor = this.computeBaseColor();
      int baseR = baseColor.getRed();
      int baseG = baseColor.getGreen();
      int baseB = baseColor.getBlue();
      if (this.useGlobalColor) {
         Color globalColor = this.getEffectiveColor();
         this.setColorToSetting(globalColor);
         this.syncHSVFromSetting();
      }

      Color currentColor = this.getEffectiveColor();
      this.openAnimation.tick(this.isOpen);
      float animProgress = this.openAnimation.getProgress();
      float easedProgress = AnimationUtil.easeInOutCubic(animProgress);
      boolean hovering = this.isHovering(mouseX, mouseY) && !this.isOpen;
      float totalWidth = (float)this.width + 8.0F;
      float previewX = this.headerPreviewX();
      float previewY = this.headerPreviewY();
      if (!this.isOpen && animProgress <= 0.0F) {
         if (hovering) {
            RenderUtil.drawRect(this.x, this.y, this.x + totalWidth, this.y + (float)this.height - 0.5F, (new Color(55, 55, 55, 180)).getRGB());
            RenderUtil.drawGradientRect(this.x, this.y, this.x + totalWidth, this.y + 1.5F, argb(25, 255, 255, 255), argb(0, 255, 255, 255));
         }

         Vonware.textManager.getCustomFont().drawStringWithShadow(this.colorSetting.getName(), (double)(this.x + 3.5F), (double)(this.y + 3.0F), -1);
         RenderUtil.drawRect(previewX, previewY, previewX + 9.0F, previewY + 9.0F, currentColor.getRGB());
      } else {
         float animatedHeight = this.getAnimatedHeight();
         int scale = mc.gameSettings.guiScale;
         if (scale <= 0) {
            scale = 1;
         }

         int displayHeight = mc.displayHeight;
         float appScale = NewVonwareGui.getAppliedScale();
         GL11.glPushAttrib(524288);
         int myX = (int)(this.x * (float)scale * appScale);
         int myY = (int)((float)displayHeight - (this.y + animatedHeight) * (float)scale * appScale);
         int myW = Math.max(0, (int)(totalWidth * (float)scale * appScale));
         int myH = Math.max(0, (int)(animatedHeight * (float)scale * appScale));
         if (GL11.glIsEnabled(3089)) {
            SCISSOR_BUF.clear();
            GL11.glGetInteger(3088, SCISSOR_BUF);
            int pX = SCISSOR_BUF.get(0);
            int pY = SCISSOR_BUF.get(1);
            int pW = SCISSOR_BUF.get(2);
            int pH = SCISSOR_BUF.get(3);
            int ix = Math.max(pX, myX);
            int iy = Math.max(pY, myY);
            int iw = Math.max(0, Math.min(pX + pW, myX + myW) - ix);
            int ih = Math.max(0, Math.min(pY + pH, myY + myH) - iy);
            GL11.glScissor(ix, iy, iw, ih);
         } else {
            GL11.glScissor(myX, myY, myW, myH);
         }

         GL11.glEnable(3089);
         Vonware.textManager.getCustomFont().drawStringWithShadow(this.colorSetting.getName(), (double)(this.x + 3.5F), (double)(this.y + 3.0F), -1);
         RenderUtil.drawRect(previewX, previewY, previewX + 9.0F, previewY + 9.0F, currentColor.getRGB());
         float badgeSize = 9.0F;
         float badgeX = this.headerBadgeX();
         float badgeY = this.headerBadgeY();
         boolean badgeHover = this.isOpen && (float)mouseX >= badgeX && (float)mouseX <= badgeX + badgeSize && (float)mouseY >= badgeY && (float)mouseY <= badgeY + badgeSize;
         if (easedProgress > 0.05F) {
            RenderUtil.drawGradientRect(badgeX, badgeY, badgeX + badgeSize, badgeY + badgeSize, argb((int)((float)(this.useGlobalColor ? 60 : (badgeHover ? 35 : 18)) * easedProgress), baseR, baseG, baseB), argb((int)((float)(this.useGlobalColor ? 35 : (badgeHover ? 18 : 6)) * easedProgress), baseR / 2, baseG / 2, baseB / 2));
            drawOutline(badgeX, badgeY, badgeX + badgeSize, badgeY + badgeSize, argb((int)((float)(this.useGlobalColor ? 150 : (badgeHover ? 80 : 30)) * easedProgress), baseR, baseG, baseB));
            GL11.glPushMatrix();
            GL11.glScalef(0.65F, 0.65F, 1.0F);
            float badgeS = 1.5384616F;
            float gW = (float)Vonware.textManager.getCustomFont().getStringWidth("G") * 0.65F;
            Vonware.textManager.getCustomFont().drawStringWithShadow("G", (double)((badgeX + (badgeSize - gW) / 2.0F) * badgeS), (double)((badgeY + 1.5F) * badgeS), this.useGlobalColor ? argb((int)(255.0F * easedProgress), 255, 255, 255) : (badgeHover ? argb((int)(255.0F * easedProgress), 204, 204, 204) : argb((int)(255.0F * easedProgress), 102, 102, 102)));
            GL11.glPopMatrix();
         }

         float startX = this.x + 2.0F;
         float startY = this.y + 2.0F;
         float availableWidth = totalWidth - 4.0F;
         float contentStartY = startY + 13.0F;
         this.drawSVBox(startX, contentStartY, availableWidth, availableWidth);
         float svX = startX + this.saturation * availableWidth;
         float svY = contentStartY + (1.0F - this.value) * availableWidth;
         RenderUtil.drawRect(svX - 3.0F, svY - 0.4F, svX + 3.0F, svY + 0.4F, -16777216);
         RenderUtil.drawRect(svX - 0.4F, svY - 3.0F, svX + 0.4F, svY + 3.0F, -16777216);
         float hueBarY = contentStartY + availableWidth + 2.0F;
         this.drawHueBar(startX, hueBarY, availableWidth, 7.0F);
         float hueX = startX + this.hue / 360.0F * availableWidth;
         RenderUtil.drawRect(hueX - 1.0F, hueBarY - 0.5F, hueX + 1.0F, hueBarY + 7.0F + 0.5F, -1);
         if (this.isOpen && !this.useGlobalColor && Mouse.isButtonDown(0)) {
            this.handlePickerDrag(mouseX, mouseY, startX, contentStartY, hueBarY, availableWidth);
         } else {
            this.draggingHue = false;
            this.draggingSV = false;
         }

         long now = System.currentTimeMillis();
         float btnY = hueBarY + 7.0F + 2.0F;
         float btnHeight = 10.0F;
         float btnGap = 2.0F;
         float btnW = (availableWidth - btnGap) / 2.0F;
         float s75 = 1.3333334F;
         boolean showCopyFB = now - this.copyFeedbackTime < 400L;
         boolean showPasteFB = now - this.pasteFeedbackTime < 400L;
         boolean canPaste = clipboardColor != -1;
         boolean copyHover = this.isOpen && (float)mouseX >= startX && (float)mouseX <= startX + btnW && (float)mouseY >= btnY && (float)mouseY <= btnY + btnHeight;
         float copyHoverT = this.copyHoverAnim.tick(copyHover);
         int copyBrightness = showCopyFB ? 255 : (int)(150.0F + 105.0F * copyHoverT);
         RenderUtil.drawRect(startX, btnY, startX + btnW, btnY + btnHeight, argb((int)(50.0F * easedProgress), baseR, baseG, baseB));
         drawOutline(startX, btnY, startX + btnW, btnY + btnHeight, argb((int)(85.0F * easedProgress), baseR, baseG, baseB));
         String copyText = showCopyFB ? "Copied!" : "Copy";
         float copyTextW = (float)Vonware.textManager.getCustomFont().getStringWidth(copyText) * 0.75F;
         float copyTextX = startX + (btnW - copyTextW) / 2.0F;
         GL11.glPushMatrix();
         GL11.glScalef(0.75F, 0.75F, 1.0F);
         Vonware.textManager.getCustomFont().drawStringWithShadow(copyText, (double)(copyTextX * s75), (double)((btnY + 2.0F) * s75), argb((int)(255.0F * easedProgress), copyBrightness, copyBrightness, copyBrightness));
         GL11.glPopMatrix();
         float pasteBtnX = startX + btnW + btnGap;
         boolean pasteHover = this.isOpen && (float)mouseX >= pasteBtnX && (float)mouseX <= pasteBtnX + btnW && (float)mouseY >= btnY && (float)mouseY <= btnY + btnHeight;
         float pasteHoverT = this.pasteHoverAnim.tick(canPaste && pasteHover);
         int pasteBrightness = !canPaste ? 150 : (showPasteFB ? 255 : (int)(150.0F + 105.0F * pasteHoverT));
         RenderUtil.drawRect(pasteBtnX, btnY, pasteBtnX + btnW, btnY + btnHeight, argb((int)(50.0F * easedProgress), baseR, baseG, baseB));
         drawOutline(pasteBtnX, btnY, pasteBtnX + btnW, btnY + btnHeight, argb((int)(85.0F * easedProgress), baseR, baseG, baseB));
         String pasteText = showPasteFB ? "Pasted!" : "Paste";
         float pasteTextW = (float)Vonware.textManager.getCustomFont().getStringWidth(pasteText) * 0.75F;
         float pasteTextX = pasteBtnX + (btnW - pasteTextW) / 2.0F;
         GL11.glPushMatrix();
         GL11.glScalef(0.75F, 0.75F, 1.0F);
         Vonware.textManager.getCustomFont().drawStringWithShadow(pasteText, (double)(pasteTextX * s75), (double)((btnY + 2.0F) * s75), argb((int)(255.0F * easedProgress), pasteBrightness, pasteBrightness, pasteBrightness));
         GL11.glPopMatrix();
         float hexY = btnY + btnHeight + 2.0F;
         String hexDisplay;
         if (this.hexEditing) {
            hexDisplay = "#" + this.hexInput + (this.showCursor() ? "_" : "");
         } else {
            Color hexDerived = this.useGlobalColor ? currentColor : ColorUtil.hsvToRgb(this.hue, this.saturation, this.value);
            hexDisplay = String.format("#%06X", hexDerived.getRGB() & 16777215);
         }

         GL11.glPushMatrix();
         GL11.glScalef(0.75F, 0.75F, 1.0F);
         float hexTextWidth = (float)Vonware.textManager.getCustomFont().getStringWidth(hexDisplay) * 0.75F;
         boolean hexHover = this.isOpen && !this.hexEditing && (float)mouseX >= startX && (float)mouseX <= startX + hexTextWidth && (float)mouseY >= hexY && (float)mouseY <= hexY + 10.0F;
         Vonware.textManager.getCustomFont().drawStringWithShadow(hexDisplay, (double)(startX * s75), (double)(hexY * s75), this.hexEditing ? argb((int)(255.0F * easedProgress), 255, 255, 255) : (hexHover ? argb((int)(255.0F * easedProgress), 255, 255, 255) : argb((int)(255.0F * easedProgress), 150, 150, 150)));
         if (this.hexEditing) {
            RenderUtil.drawRect(startX, hexY + 8.0F, startX + hexTextWidth, hexY + 8.5F, argb((int)(180.0F * easedProgress), baseR, baseG, baseB));
         } else if (hexHover) {
            RenderUtil.drawRect(startX, hexY + 8.0F, startX + hexTextWidth, hexY + 8.5F, argb((int)(60.0F * easedProgress), baseR, baseG, baseB));
         }

         GL11.glPopMatrix();
         float rgbStartY = hexY + 12.0F;
         RenderUtil.drawGradientRect(startX, rgbStartY - 2.0F, startX + availableWidth, rgbStartY - 1.5F, argb(0, baseR, baseG, baseB), argb((int)(40.0F * easedProgress), baseR, baseG, baseB));
         float switchWidth = 50.0F;
         float switchHeight = 8.0F;
         float switchX = startX + availableWidth - switchWidth;
         float switchY = rgbStartY - 1.0F;
         float halfSwitch = switchWidth / 2.0F;
         float modeTarget = this.rgbSliderMode ? 1.0F : 0.0F;
         this.modeToggleAnim += (modeTarget - this.modeToggleAnim) * 0.15F;
         if (Math.abs(this.modeToggleAnim - modeTarget) < 0.01F) {
            this.modeToggleAnim = modeTarget;
         }

         boolean inputHover = this.isOpen && (float)mouseX >= switchX && (float)mouseX <= switchX + halfSwitch && (float)mouseY >= switchY && (float)mouseY <= switchY + switchHeight;
         boolean sliderHover = this.isOpen && (float)mouseX >= switchX + halfSwitch && (float)mouseX <= switchX + switchWidth && (float)mouseY >= switchY && (float)mouseY <= switchY + switchHeight;
         boolean anyHover = inputHover || sliderHover;
         RenderUtil.drawGradientRect(switchX, switchY, switchX + switchWidth, switchY + switchHeight, argb((int)((float)(anyHover ? 35 : 22) * easedProgress), baseR, baseG, baseB), argb((int)((float)(anyHover ? 18 : 8) * easedProgress), baseR / 2, baseG / 2, baseB / 2));
         RenderUtil.drawRect(switchX, switchY, switchX + switchWidth, switchY + 0.5F, argb((int)(20.0F * easedProgress), 0, 0, 0));
         float indicatorX = switchX + this.modeToggleAnim * halfSwitch;
         RenderUtil.drawGradientRect(indicatorX + 0.5F, switchY + 0.5F, indicatorX + halfSwitch - 0.5F, switchY + switchHeight - 0.5F, argb((int)(70.0F * easedProgress), baseR, baseG, baseB), argb((int)(35.0F * easedProgress), baseR / 2, baseG / 2, baseB / 2));
         RenderUtil.drawGradientRect(indicatorX + 0.5F, switchY + 0.5F, indicatorX + halfSwitch - 0.5F, switchY + switchHeight - 0.5F, argb((int)(30.0F * easedProgress), 255, 255, 255), argb(0, 255, 255, 255));
         RenderUtil.drawRect(switchX + halfSwitch - 0.25F, switchY + 1.5F, switchX + halfSwitch + 0.25F, switchY + switchHeight - 1.5F, argb((int)(40.0F * easedProgress), baseR, baseG, baseB));
         drawOutline(switchX, switchY, switchX + switchWidth, switchY + switchHeight, argb((int)((float)(anyHover ? 80 : 35) * easedProgress), baseR, baseG, baseB));
         RenderUtil.drawGradientRect(switchX, switchY + switchHeight, switchX + switchWidth, switchY + switchHeight + 1.0F, argb((int)(15.0F * easedProgress), 0, 0, 0), argb(0, 0, 0, 0));
         String inputLabel = "Input";
         String sliderLabel = "Slider";
         float inputLabelW = (float)Vonware.textManager.getCustomFont().getStringWidth(inputLabel) * 0.6F;
         float sliderLabelW = (float)Vonware.textManager.getCustomFont().getStringWidth(sliderLabel) * 0.6F;
         float s60 = 1.6666666F;
         GL11.glPushMatrix();
         GL11.glScalef(0.6F, 0.6F, 1.0F);
         Vonware.textManager.getCustomFont().drawStringWithShadow(inputLabel, (double)((switchX + (halfSwitch - inputLabelW) / 2.0F) * s60), (double)((switchY + 1.5F) * s60), !this.rgbSliderMode ? argb((int)(255.0F * easedProgress), 255, 255, 255) : (inputHover ? argb((int)(255.0F * easedProgress), 204, 204, 204) : argb((int)(255.0F * easedProgress), 119, 119, 119)));
         Vonware.textManager.getCustomFont().drawStringWithShadow(sliderLabel, (double)((switchX + halfSwitch + (halfSwitch - sliderLabelW) / 2.0F) * s60), (double)((switchY + 1.5F) * s60), this.rgbSliderMode ? argb((int)(255.0F * easedProgress), 255, 255, 255) : (sliderHover ? argb((int)(255.0F * easedProgress), 204, 204, 204) : argb((int)(255.0F * easedProgress), 119, 119, 119)));
         GL11.glPopMatrix();
         GL11.glPushMatrix();
         GL11.glScalef(0.7F, 0.7F, 1.0F);
         float s70 = 1.4285715F;
         Vonware.textManager.getCustomFont().drawStringWithShadow("RGB", (double)(startX * s70), (double)(rgbStartY * s70), argb((int)(200.0F * easedProgress), baseR, baseG, baseB));
         GL11.glPopMatrix();
         float rgbControlsY = rgbStartY + 10.0F;
         Color displayColor = this.useGlobalColor ? currentColor : ColorUtil.hsvToRgb(this.hue, this.saturation, this.value);
         int r = displayColor.getRed();
         int g = displayColor.getGreen();
         int b = displayColor.getBlue();
         if (this.rgbSliderMode) {
            this.drawRgbSliders(startX, rgbControlsY, availableWidth, r, g, b, mouseX, mouseY, baseR, baseG, baseB, easedProgress);
         } else {
            this.drawRgbInputInline(startX, rgbControlsY, availableWidth, r, g, b, mouseX, mouseY, baseR, baseG, baseB, easedProgress);
         }

         GL11.glPopAttrib();
      }
   }

   private void drawRgbSliders(float x, float y, float width, int r, int g, int b, int mouseX, int mouseY, int baseR, int baseG, int baseB, float alpha) {
      float sliderHeight = 6.0F;
      float sliderSpacing = 9.0F;
      float labelWidth = 9.0F;
      float sliderX = x + labelWidth;
      float sliderWidth = width - labelWidth;
      String[] labels = new String[]{"R", "G", "B"};
      int[] values = new int[]{r, g, b};
      int[][] sc = new int[][]{{180, 50, 50}, {50, 180, 50}, {50, 80, 180}};

      for(int i = 0; i < 3; ++i) {
         float sliderY = y + (float)i * sliderSpacing;
         boolean sliderHover = this.isOpen && (float)mouseX >= sliderX && (float)mouseX <= sliderX + sliderWidth && (float)mouseY >= sliderY - 1.0F && (float)mouseY <= sliderY + sliderHeight + 1.0F;
         GL11.glPushMatrix();
         GL11.glScalef(0.65F, 0.65F, 1.0F);
         float s = 1.5384616F;
         Vonware.textManager.getCustomFont().drawStringWithShadow(labels[i], (double)((x + 1.0F) * s), (double)((sliderY + 0.5F) * s), argb((int)(255.0F * alpha), sc[i][0], sc[i][1], sc[i][2]));
         GL11.glPopMatrix();
         RenderUtil.drawGradientRect(sliderX, sliderY, sliderX + sliderWidth, sliderY + sliderHeight, argb((int)(40.0F * alpha), 0, 0, 0), argb((int)(20.0F * alpha), 0, 0, 0));
         RenderUtil.drawRect(sliderX, sliderY, sliderX + sliderWidth, sliderY + 0.5F, argb((int)(30.0F * alpha), 0, 0, 0));
         float fillWidth = (float)values[i] / 255.0F * sliderWidth;
         RenderUtil.drawGradientRect(sliderX, sliderY, sliderX + fillWidth, sliderY + sliderHeight, argb((int)(160.0F * alpha), sc[i][0], sc[i][1], sc[i][2]), argb((int)(220.0F * alpha), sc[i][0] / 2, sc[i][1] / 2, sc[i][2] / 2));
         RenderUtil.drawRect(sliderX, sliderY, sliderX + fillWidth, sliderY + 0.5F, argb((int)(60.0F * alpha), 255, 255, 255));
         float handleX = sliderX + fillWidth;
         RenderUtil.drawRect(handleX - 0.5F, sliderY - 0.5F, handleX + 1.0F, sliderY + sliderHeight + 0.5F, argb((int)(221.0F * alpha), 221, 221, 221));
         if (sliderHover || this.draggingRgbSlider == i) {
            RenderUtil.drawRect(handleX - 1.0F, sliderY - 1.0F, handleX + 1.5F, sliderY + sliderHeight + 1.0F, argb((int)(40.0F * alpha), 255, 255, 255));
         }

         RenderUtil.drawGradientRect(sliderX, sliderY + sliderHeight, sliderX + sliderWidth, sliderY + sliderHeight + 1.0F, argb((int)(15.0F * alpha), 0, 0, 0), argb(0, 0, 0, 0));
         drawOutline(sliderX, sliderY, sliderX + sliderWidth, sliderY + sliderHeight, argb((int)((float)(!sliderHover && this.draggingRgbSlider != i ? 30 : 80) * alpha), sc[i][0], sc[i][1], sc[i][2]));
         GL11.glPushMatrix();
         GL11.glScalef(0.55F, 0.55F, 1.0F);
         float s55 = 1.8181818F;
         String valStr = String.valueOf(values[i]);
         float valW = (float)Vonware.textManager.getCustomFont().getStringWidth(valStr) * 0.55F;
         Vonware.textManager.getCustomFont().drawStringWithShadow(valStr, (double)((sliderX + sliderWidth - valW - 1.0F) * s55), (double)((sliderY + 0.8F) * s55), argb((int)(170.0F * alpha), 255, 255, 255));
         GL11.glPopMatrix();
         if (Mouse.isButtonDown(0) && !this.useGlobalColor) {
            if (this.draggingRgbSlider == i || sliderHover && this.draggingRgbSlider == -1) {
               this.draggingRgbSlider = i;
               float rel = Math.max(0.0F, Math.min(sliderWidth, (float)mouseX - sliderX));
               this.applyRgbChange(i, Math.max(0, Math.min(255, Math.round(rel / sliderWidth * 255.0F))));
            }
         } else if (this.draggingRgbSlider == i) {
            this.draggingRgbSlider = -1;
         }
      }

      if (!Mouse.isButtonDown(0)) {
         this.draggingRgbSlider = -1;
      }

   }

   private void drawRgbInputInline(float x, float y, float width, int r, int g, int b, int mouseX, int mouseY, int baseR, int baseG, int baseB, float alpha) {
      String[] labels = new String[]{"R", "G", "B"};
      int[] values = new int[]{r, g, b};
      int[][] fc = new int[][]{{180, 50, 50}, {50, 180, 50}, {50, 80, 180}};
      float fieldHeight = 8.0F;
      float labelSpace = 6.0F;
      float spacing = 2.0F;
      float sectionWidth = (width - spacing * 2.0F) / 3.0F;

      for(int i = 0; i < 3; ++i) {
         float sectionX = x + (float)i * (sectionWidth + spacing);
         float fieldX = sectionX + labelSpace;
         float fieldW = sectionWidth - labelSpace;
         boolean isActive = this.activeRgbField == i;
         boolean fieldHover = this.isOpen && (float)mouseX >= fieldX && (float)mouseX <= fieldX + fieldW && (float)mouseY >= y && (float)mouseY <= y + fieldHeight;
         GL11.glPushMatrix();
         GL11.glScalef(0.65F, 0.65F, 1.0F);
         float s65 = 1.5384616F;
         Vonware.textManager.getCustomFont().drawStringWithShadow(labels[i], (double)(sectionX * s65), (double)((y + 1.0F) * s65), argb((int)(255.0F * alpha), fc[i][0], fc[i][1], fc[i][2]));
         GL11.glPopMatrix();
         RenderUtil.drawGradientRect(fieldX, y, fieldX + fieldW, y + fieldHeight, isActive ? argb((int)(45.0F * alpha), baseR, baseG, baseB) : argb((int)(30.0F * alpha), 0, 0, 0), isActive ? argb((int)(25.0F * alpha), baseR, baseG, baseB) : argb((int)(15.0F * alpha), 0, 0, 0));
         RenderUtil.drawRect(fieldX, y, fieldX + fieldW, y + 0.5F, argb((int)((float)(isActive ? 15 : 20) * alpha), 0, 0, 0));
         RenderUtil.drawRect(fieldX, y + fieldHeight - 0.5F, fieldX + fieldW, y + fieldHeight, isActive ? argb((int)(220.0F * alpha), fc[i][0], fc[i][1], fc[i][2]) : argb((int)(30.0F * alpha), baseR, baseG, baseB));
         RenderUtil.drawGradientRect(fieldX, y + fieldHeight, fieldX + fieldW, y + fieldHeight + 1.0F, argb((int)(15.0F * alpha), 0, 0, 0), argb(0, 0, 0, 0));
         if (fieldHover && !isActive) {
            RenderUtil.drawGradientRect(fieldX, y, fieldX + fieldW, y + fieldHeight, argb((int)(15.0F * alpha), 255, 255, 255), argb(0, 255, 255, 255));
         }

         drawOutline(fieldX, y, fieldX + fieldW, y + fieldHeight, isActive ? argb((int)(100.0F * alpha), fc[i][0], fc[i][1], fc[i][2]) : argb((int)((float)(fieldHover ? 50 : 20) * alpha), baseR, baseG, baseB));
         String displayText = isActive ? this.rgbFieldText + (this.showCursor() ? "_" : "") : String.valueOf(values[i]);
         GL11.glPushMatrix();
         GL11.glScalef(0.65F, 0.65F, 1.0F);
         float s65b = 1.5384616F;
         Vonware.textManager.getCustomFont().drawStringWithShadow(displayText, (double)((fieldX + 1.5F) * s65b), (double)((y + 1.5F) * s65b), isActive ? argb((int)(255.0F * alpha), 255, 255, 255) : argb((int)(255.0F * alpha), 170, 170, 170));
         GL11.glPopMatrix();
      }

   }

   private void applyRgbChange(int channel, int val) {
      Color current = this.useGlobalColor ? this.getEffectiveColor() : ColorUtil.hsvToRgb(this.hue, this.saturation, this.value);
      int r = current.getRed();
      int g = current.getGreen();
      int b = current.getBlue();
      switch (channel) {
         case 0:
            r = val;
            break;
         case 1:
            g = val;
            break;
         case 2:
            b = val;
      }

      Color nc = new Color(r, g, b);
      float[] hsv = ColorUtil.rgbToHsv(nc);
      this.hue = hsv[0];
      this.saturation = hsv[1];
      this.value = hsv[2];
      if (!this.useGlobalColor) {
         this.setColorToSetting(nc);
         this.savedCustomColor = (Integer)this.colorSetting.getValue();
      }

   }

   private void handlePickerDrag(int mouseX, int mouseY, float svBoxX, float svBoxY, float hueBarY, float size) {
      if ((float)mouseX >= svBoxX && (float)mouseX <= svBoxX + size && (float)mouseY >= svBoxY && (float)mouseY <= svBoxY + size) {
         this.draggingSV = true;
      }

      if ((float)mouseX >= svBoxX && (float)mouseX <= svBoxX + size && (float)mouseY >= hueBarY && (float)mouseY <= hueBarY + 7.0F) {
         this.draggingHue = true;
      }

      if (this.draggingSV) {
         this.saturation = Math.max(0.0F, Math.min(size, (float)mouseX - svBoxX)) / size;
         this.value = 1.0F - Math.max(0.0F, Math.min(size, (float)mouseY - svBoxY)) / size;
         this.updateColorFromHSV();
      }

      if (this.draggingHue) {
         this.hue = Math.max(0.0F, Math.min(size, (float)mouseX - svBoxX)) / size * 360.0F;
         this.updateColorFromHSV();
      }

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
      int res = 16;
      float pw = width / (float)res;
      float ph = height / (float)res;
      GL11.glDisable(3553);
      GL11.glShadeModel(7425);
      GL11.glBegin(7);

      for(int i = 0; i < res; ++i) {
         for(int j = 0; j < res; ++j) {
            float s1 = (float)i / (float)res;
            float s2 = (float)(i + 1) / (float)res;
            float v1 = 1.0F - (float)j / (float)res;
            float v2 = 1.0F - (float)(j + 1) / (float)res;
            float x1 = x + (float)i * pw;
            float y1 = y + (float)j * ph;
            float x2 = x1 + pw;
            float y2 = y1 + ph;
            Color c = ColorUtil.hsvToRgb(this.hue, s1, v2);
            GL11.glColor3f((float)c.getRed() / 255.0F, (float)c.getGreen() / 255.0F, (float)c.getBlue() / 255.0F);
            GL11.glVertex2f(x1, y2);
            c = ColorUtil.hsvToRgb(this.hue, s2, v2);
            GL11.glColor3f((float)c.getRed() / 255.0F, (float)c.getGreen() / 255.0F, (float)c.getBlue() / 255.0F);
            GL11.glVertex2f(x2, y2);
            c = ColorUtil.hsvToRgb(this.hue, s2, v1);
            GL11.glColor3f((float)c.getRed() / 255.0F, (float)c.getGreen() / 255.0F, (float)c.getBlue() / 255.0F);
            GL11.glVertex2f(x2, y1);
            c = ColorUtil.hsvToRgb(this.hue, s1, v1);
            GL11.glColor3f((float)c.getRed() / 255.0F, (float)c.getGreen() / 255.0F, (float)c.getBlue() / 255.0F);
            GL11.glVertex2f(x1, y1);
         }
      }

      GL11.glEnd();
      GL11.glShadeModel(7424);
      GL11.glEnable(3553);
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
   }

   public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
      if (mouseButton == 1) {
         boolean onHeader = (float)mouseX >= this.getX() && (float)mouseX <= this.getX() + (float)this.getWidth() + 8.0F && (float)mouseY >= this.getY() && (float)mouseY <= this.getY() + 13.0F;
         if (this.isOpen && onHeader) {
            if (this.activeRgbField >= 0) {
               this.applyRgbInput();
               this.activeRgbField = -1;
            }

            if (this.hexEditing) {
               this.applyHexInput();
               this.hexEditing = false;
            }

            this.isOpen = false;
            SoundUtil.playColorPickerClose();
         } else if (!this.isOpen && onHeader) {
            this.isOpen = true;
            this.syncHSVFromSetting();
            this.hexEditing = false;
            this.activeRgbField = -1;
            SoundUtil.playColorPickerOpen();
         }

      } else if (this.isOpen) {
         float totalWidth = (float)this.width + 8.0F;
         float startX = this.x + 2.0F;
         float startY = this.y + 2.0F;
         float availableWidth = totalWidth - 4.0F;
         if (mouseButton == 0) {
            if (this.activeRgbField >= 0) {
               this.applyRgbInput();
            }

            if (this.hexEditing) {
               this.applyHexInput();
            }

            long now = System.currentTimeMillis();
            float contentStartY = startY + 13.0F;
            float hueBarY = contentStartY + availableWidth + 2.0F;
            float btnY = hueBarY + 7.0F + 2.0F;
            float btnHeight = 10.0F;
            float btnGap = 2.0F;
            float btnW = (availableWidth - btnGap) / 2.0F;
            if ((float)mouseX >= startX && (float)mouseX <= startX + btnW && (float)mouseY >= btnY && (float)mouseY <= btnY + btnHeight) {
               Color toCopy = this.useGlobalColor ? this.getEffectiveColor() : ColorUtil.hsvToRgb(this.hue, this.saturation, this.value);
               clipboardColor = toCopy.getRGB() & 16777215;
               this.copyFeedbackTime = now;
            } else {
               float pasteBtnX = startX + btnW + btnGap;
               if ((float)mouseX >= pasteBtnX && (float)mouseX <= pasteBtnX + btnW && (float)mouseY >= btnY && (float)mouseY <= btnY + btnHeight) {
                  if (clipboardColor != -1 && !this.useGlobalColor) {
                     this.colorSetting.setValue(clipboardColor);
                     this.savedCustomColor = clipboardColor;
                     this.syncHSVFromSetting();
                     this.pasteFeedbackTime = now;
                  }

               } else {
                  float hexY = btnY + btnHeight + 2.0F;
                  Color hexDerived = this.useGlobalColor ? this.getEffectiveColor() : ColorUtil.hsvToRgb(this.hue, this.saturation, this.value);
                  String hexDisp = this.hexEditing ? "#" + this.hexInput : String.format("#%06X", hexDerived.getRGB() & 16777215);
                  float hexTW = (float)Vonware.textManager.getCustomFont().getStringWidth(hexDisp) * 0.75F;
                  if ((float)mouseX >= startX && (float)mouseX <= startX + Math.max(hexTW, 40.0F) && (float)mouseY >= hexY && (float)mouseY <= hexY + 10.0F) {
                     if (!this.hexEditing) {
                        this.hexEditing = true;
                        this.activeRgbField = -1;
                        this.hexInput = String.format("%06X", hexDerived.getRGB() & 16777215);
                        this.resetCursorBlink();
                     }

                  } else {
                     float rgbStartY = hexY + 12.0F;
                     float switchWidth = 50.0F;
                     float switchHeight = 8.0F;
                     float switchX = startX + availableWidth - switchWidth;
                     float switchY = rgbStartY - 1.0F;
                     float halfSwitch = switchWidth / 2.0F;
                     if ((float)mouseX >= switchX && (float)mouseX <= switchX + switchWidth && (float)mouseY >= switchY && (float)mouseY <= switchY + switchHeight) {
                        boolean clickedSlider = (float)mouseX >= switchX + halfSwitch;
                        if (clickedSlider != this.rgbSliderMode) {
                           this.rgbSliderMode = clickedSlider;
                           this.activeRgbField = -1;
                           this.hexEditing = false;
                           ColorPickerConfig.setState(this.colorSetting, this.useGlobalColor, this.rgbSliderMode, this.savedCustomColor);
                           SoundUtil.playModuleToggle(this.rgbSliderMode);
                        }

                     } else {
                        if (!this.rgbSliderMode && !this.useGlobalColor) {
                           float rgbControlsY = rgbStartY + 10.0F;
                           float labelSpace = 6.0F;
                           float spacing = 2.0F;
                           float sectionWidth = (availableWidth - spacing * 2.0F) / 3.0F;

                           for(int i = 0; i < 3; ++i) {
                              float sectionX = startX + (float)i * (sectionWidth + spacing);
                              float fieldX = sectionX + labelSpace;
                              float fieldW = sectionWidth - labelSpace;
                              if ((float)mouseX >= fieldX && (float)mouseX <= fieldX + fieldW && (float)mouseY >= rgbControlsY && (float)mouseY <= rgbControlsY + 8.0F) {
                                 this.hexEditing = false;
                                 this.activeRgbField = i;
                                 Color cur = this.useGlobalColor ? this.getEffectiveColor() : ColorUtil.hsvToRgb(this.hue, this.saturation, this.value);
                                 this.rgbFieldText = String.valueOf((new int[]{cur.getRed(), cur.getGreen(), cur.getBlue()})[i]);
                                 this.resetCursorBlink();
                                 return;
                              }
                           }
                        }

                        float badgeSize = 9.0F;
                        float gbBadgeX = this.headerBadgeX();
                        float gbBadgeY = this.headerBadgeY();
                        if ((float)mouseX >= gbBadgeX && (float)mouseX <= gbBadgeX + badgeSize && (float)mouseY >= gbBadgeY && (float)mouseY <= gbBadgeY + badgeSize) {
                           if (!this.useGlobalColor) {
                              this.savedCustomColor = (Integer)this.colorSetting.getValue();
                           }

                           this.useGlobalColor = !this.useGlobalColor;
                           if (!this.useGlobalColor) {
                              this.colorSetting.setValue(this.savedCustomColor);
                              this.syncHSVFromSetting();
                           }

                           ColorPickerConfig.setState(this.colorSetting, this.useGlobalColor, this.rgbSliderMode, this.savedCustomColor);
                           SoundUtil.playGlobalColorSync(this.useGlobalColor);
                        } else {
                           this.hexEditing = false;
                           this.activeRgbField = -1;
                        }
                     }
                  }
               }
            }
         }
      }
   }

   public void onKeyTyped(char typedChar, int keyCode) {
      if (this.hexEditing) {
         this.resetCursorBlink();
         if (keyCode != 28 && keyCode != 156) {
            if (keyCode == 1) {
               this.hexEditing = false;
            } else if (keyCode == 14) {
               if (!this.hexInput.isEmpty()) {
                  this.hexInput = this.hexInput.substring(0, this.hexInput.length() - 1);
               }

            } else if (GuiScreen.isCtrlKeyDown() && keyCode == 47) {
               String cb = GuiScreen.getClipboardString();
               if (cb != null) {
                  cb = cb.trim().replace("#", "");
                  StringBuilder sb = new StringBuilder();

                  for(char c : cb.toCharArray()) {
                     if ("0123456789ABCDEFabcdef".indexOf(c) >= 0) {
                        sb.append(Character.toUpperCase(c));
                     }
                  }

                  this.hexInput = sb.toString();
                  if (this.hexInput.length() > 6) {
                     this.hexInput = this.hexInput.substring(0, 6);
                  }

                  if (this.hexInput.length() == 6) {
                     this.applyHexInput();
                     this.hexEditing = false;
                  }
               }

            } else {
               if ("0123456789ABCDEFabcdef".indexOf(typedChar) >= 0 && this.hexInput.length() < 6) {
                  this.hexInput = this.hexInput + Character.toUpperCase(typedChar);
                  if (this.hexInput.length() == 6) {
                     this.applyHexInput();
                     this.hexEditing = false;
                  }
               }

            }
         } else {
            this.applyHexInput();
            this.hexEditing = false;
         }
      } else {
         if (this.activeRgbField >= 0) {
            this.resetCursorBlink();
            if (keyCode == 28 || keyCode == 156) {
               this.applyRgbInput();
               this.activeRgbField = -1;
               return;
            }

            if (keyCode == 1) {
               this.activeRgbField = -1;
               return;
            }

            if (keyCode == 14) {
               if (!this.rgbFieldText.isEmpty()) {
                  this.rgbFieldText = this.rgbFieldText.substring(0, this.rgbFieldText.length() - 1);
               }

               return;
            }

            if (keyCode == 15) {
               this.applyRgbInput();
               this.activeRgbField = (this.activeRgbField + 1) % 3;
               Color cur = this.useGlobalColor ? this.getEffectiveColor() : ColorUtil.hsvToRgb(this.hue, this.saturation, this.value);
               this.rgbFieldText = String.valueOf((new int[]{cur.getRed(), cur.getGreen(), cur.getBlue()})[this.activeRgbField]);
               return;
            }

            if (Character.isDigit(typedChar) && this.rgbFieldText.length() < 3) {
               this.rgbFieldText = this.rgbFieldText + typedChar;
            }
         }

      }
   }

   private void applyHexInput() {
      if (!this.hexInput.isEmpty()) {
         while(this.hexInput.length() < 6) {
            this.hexInput = this.hexInput + "0";
         }

         try {
            int ci = Integer.parseInt(this.hexInput, 16);
            Color parsed = new Color(ci >> 16 & 255, ci >> 8 & 255, ci & 255);
            float[] hsv = ColorUtil.rgbToHsv(parsed);
            this.hue = hsv[0];
            this.saturation = hsv[1];
            this.value = hsv[2];
            if (!this.useGlobalColor) {
               this.setColorToSetting(parsed);
               this.savedCustomColor = (Integer)this.colorSetting.getValue();
            }
         } catch (NumberFormatException var4) {
         }

      }
   }

   private void applyRgbInput() {
      if (!this.rgbFieldText.isEmpty()) {
         try {
            int val = Math.max(0, Math.min(255, Integer.parseInt(this.rgbFieldText)));
            this.rgbFieldText = String.valueOf(val);
            this.applyRgbChange(this.activeRgbField, val);
         } catch (NumberFormatException var2) {
         }

      }
   }

   public int getHeight() {
      float animProgress = this.openAnimation.getProgress();
      if (animProgress <= 0.0F) {
         return 13;
      } else {
         float easedProgress = AnimationUtil.easeInOutCubic(animProgress);
         float fullHeight = this.getFullExpandedHeight();
         return (int)(13.0F + (fullHeight - 13.0F) * easedProgress);
      }
   }

   public void update() {
      this.setHidden(!this.colorSetting.isVisible());
   }

   public boolean isHovering(int mouseX, int mouseY) {
      for(NewComponent c : NewVonwareGui.getClickGui().getComponents()) {
         if (c.drag) {
            return false;
         }
      }

      return (float)mouseX >= this.getX() && (float)mouseX <= this.getX() + (float)this.getWidth() + 8.0F && (float)mouseY >= this.getY() && (float)mouseY <= this.getY() + (float)this.getHeight();
   }

   public boolean isUsingGlobalColor() {
      return this.useGlobalColor;
   }

   public void setUseGlobalColor(boolean v) {
      this.useGlobalColor = v;
      if (!v) {
         this.syncHSVFromSetting();
      }

   }
}
