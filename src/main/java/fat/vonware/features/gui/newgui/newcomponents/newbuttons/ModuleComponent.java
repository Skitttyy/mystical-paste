package fat.vonware.features.gui.newgui.newcomponents.newbuttons;

import fat.vonware.Vonware;
import fat.vonware.features.gui.components.items.Item;
import fat.vonware.features.gui.components.items.buttons.ColorPickerButton;
import fat.vonware.features.gui.newgui.newcomponents.NewComponent;
import fat.vonware.features.gui.newgui.newcomponents.NewVonwareGui;
import fat.vonware.features.modules.Module;
import fat.vonware.features.modules.client.ClickGui;
import fat.vonware.features.modules.client.Colors;
import fat.vonware.features.setting.Bind;
import fat.vonware.features.setting.Setting;
import fat.vonware.util.Animations.AnimationUtil;
import fat.vonware.util.Animations.render.RenderUtil;
import fat.vonware.util.OtherRenderShit.gui.SoundUtil;
import java.awt.Color;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.GlStateManager.DestFactor;
import net.minecraft.client.renderer.GlStateManager.SourceFactor;
import org.lwjgl.opengl.GL11;

public class ModuleComponent extends NewButton {
   private final Module module;
   private List<Item> items = new ArrayList();
   private final Map<Item, AnimationUtil.LerpAnimation> itemVisAnims = new LinkedHashMap();
   private final Map<Item, Boolean> itemPrevHidden = new LinkedHashMap();
   private final Map<Item, Long> itemRevealTime = new LinkedHashMap();
   private final Map<Item, Setting<?>> itemSettings = new LinkedHashMap();
   private static final long STAGGER_MS = 100L;
   private boolean subOpen;
   private AnimationUtil.LerpAnimation settingsAnimation = new AnimationUtil.LerpAnimation(0.0F, 0.09F, 0.005F);
   private AnimationUtil.LerpAnimation fillAnimation = new AnimationUtil.LerpAnimation(0.0F, 0.07F, 0.005F);
   private static final long outlineGlowStart = System.currentTimeMillis();
   private static final float LAP_MS = 1800.0F;
   private static final float GLOW_TAIL_W = 150.0F;
   private static final int DROP_TOP = 201326592;
   private static final int DROP_BOTTOM = 369098752;
   public static String pendingMouseTooltip = null;
   public static int pendingMouseTooltipX;
   public static int pendingMouseTooltipY;
   private float cachedAnimatedTotalH = 0.5F;

   public ModuleComponent(Module module) {
      super(module.getName());
      this.module = module;
      this.initSettings();
   }

   public void closeSettings() {
      this.subOpen = false;
      this.settingsAnimation.reset(false);
   }

   public void initSettings() {
      ArrayList<Item> newItems = new ArrayList();
      Map<Item, Setting<?>> newItemSettings = new LinkedHashMap();
      Setting bindSetting = this.module.getSettingByName("Keybind");
      if (bindSetting != null && !(this.module instanceof Colors)) {
         addSettingItem(newItems, newItemSettings, new KeyBindButton(bindSetting), bindSetting);
      }

      for(Setting setting : this.module.getSettings()) {
         if (setting.getValue() instanceof Boolean && !setting.getName().equals("Enabled")) {
            addSettingItem(newItems, newItemSettings, new NewBooleanButton(setting), setting);
         }

         if (setting.getValue() instanceof Bind && !setting.getName().equalsIgnoreCase("Keybind") && !this.module.getName().equalsIgnoreCase("Elements")) {
            addSettingItem(newItems, newItemSettings, new KeyBindButton(setting), setting);
         }

         if ((setting.getValue() instanceof String || setting.getValue() instanceof Character) && !setting.getName().equalsIgnoreCase("displayName")) {
            addSettingItem(newItems, newItemSettings, new NewStringButton(setting), setting);
         }

         if (setting.isColorPicker() && setting.isNumberSetting()) {
            addSettingItem(newItems, newItemSettings, new NewColorPickerButton(setting), setting);
         } else if (setting.isNumberSetting() && setting.hasRestriction()) {
            addSettingItem(newItems, newItemSettings, new ValueSlider(setting), setting);
         } else if (setting.isEnumSetting()) {
            addSettingItem(newItems, newItemSettings, new NewEnumButton(setting), setting);
         }
      }

      if (this.module instanceof Colors) {
         Colors col = (Colors)this.module;
         Item picker = new ColorPickerButton(col.hsvHue, col.hsvSaturation, col.hsvValue);
         newItems.add(picker);
         newItemSettings.put(picker, col.hsvHue);
      }

      this.itemSettings.clear();
      this.itemSettings.putAll(newItemSettings);
      Map<Item, AnimationUtil.LerpAnimation> newAnims = new LinkedHashMap();

      for(Item item : newItems) {
         AnimationUtil.LerpAnimation existing = (AnimationUtil.LerpAnimation)this.itemVisAnims.get(item);
         if (existing != null) {
            newAnims.put(item, existing);
         } else {
            float startVal = item.isHidden() ? 0.0F : 1.0F;
            newAnims.put(item, new AnimationUtil.LerpAnimation(startVal, 0.25F, 0.01F));
         }
      }

      this.itemVisAnims.clear();
      this.itemVisAnims.putAll(newAnims);
      this.items = newItems;
      float fixedSpeed = 0.13F;
      this.settingsAnimation = new AnimationUtil.LerpAnimation(this.settingsAnimation.getValue(), fixedSpeed, 0.005F);
      this.fillAnimation = new AnimationUtil.LerpAnimation(this.fillAnimation.getValue(), fixedSpeed * 0.55F, 0.005F);
   }

   private static void addSettingItem(List<Item> items, Map<Item, Setting<?>> settings, Item item, Setting<?> setting) {
      items.add(item);
      settings.put(item, setting);
   }

   private void queueDescription(String desc, int mouseX, int mouseY, ClickGui clickGui) {
      if (desc != null && !desc.trim().isEmpty()) {
         switch ((ClickGui.Description)clickGui.description.getValue()) {
            case Mouse:
               pendingMouseTooltip = desc;
               pendingMouseTooltipX = mouseX;
               pendingMouseTooltipY = mouseY;
               break;
            case Bottom:
               pendingMouseTooltip = desc;
               if (!(Boolean)clickGui.clientInfo.getValue()) {
                  pendingMouseTooltipX = mouseX;
                  pendingMouseTooltipY = mouseY;
               }
         }

      }
   }

   private boolean isSettingDescriptionHovered(Item item, int mouseX, int mouseY) {
      float left = item.getX() - 1.5F;
      float right = item.getX() + (float)item.getWidth() + 8.0F;
      float top = item.getY();
      float bottom = item.getY() + (float)item.getHeight();
      return (float)mouseX >= left && (float)mouseX <= right && (float)mouseY >= top && (float)mouseY <= bottom;
   }

   public void drawScreen(int mouseX, int mouseY, float partialTicks) {
      ClickGui clickGui = ClickGui.getInstance();
      float animValue = this.settingsAnimation.tickEased(this.subOpen);
      float fillValue = this.fillAnimation.tickEased(this.subOpen);
      boolean needsColor = fillValue > 0.0F || animValue > 0.0F;
      if (needsColor) {
         resolveBaseColor(this.baseColor);
         this.baseColorValid = true;
      }

      float animatedTotalH = 0.5F;
      if (animValue > 0.0F) {
         long nowForHeight = System.currentTimeMillis();

         for(Item item : this.items) {
            AnimationUtil.LerpAnimation va = (AnimationUtil.LerpAnimation)this.itemVisAnims.get(item);
            boolean shouldShow = !item.isHidden() && nowForHeight >= (Long)this.itemRevealTime.getOrDefault(item, 0L);
            float vis = va != null ? va.getValue() : (shouldShow ? 1.0F : 0.0F);
            if (vis > 0.001F) {
               animatedTotalH += ((float)item.getHeight() + 1.5F) * vis;
            }
         }
      }

      this.cachedAnimatedTotalH = animatedTotalH;
      if (fillValue > 0.0F) {
         RenderUtil.drawRect(this.x - 2.5F, this.y, this.x + (float)this.width + 2.5F, this.y + (float)this.height, argb((int)(100.0F * fillValue), this.baseColor[0], this.baseColor[1], this.baseColor[2]));
      }

      super.drawScreen(mouseX, mouseY, partialTicks);
      this.drawSettingsIndicator(clickGui);
      if (animValue > 0.0F) {
         int scl2 = mc.gameSettings.guiScale > 0 ? mc.gameSettings.guiScale : 1;
         float scrH2 = (float)mc.displayHeight / (float)scl2 / NewVonwareGui.getAppliedScale();
         float maxH2 = Math.max(20.0F, scrH2 - (this.y + (float)this.height));
         float effH2 = Math.min(animatedTotalH, maxH2);
         float expandedBottom = this.y + (float)this.height + effH2 * animValue;
         this.drawFlowingButtonOutline(this.x, this.y + (float)this.height, this.x + (float)this.width, expandedBottom, 1.0F, animValue, this.baseColor[0], this.baseColor[1], this.baseColor[2]);
      }

      if (!this.items.isEmpty() && animValue > 0.0F) {
         int scale = mc.gameSettings.guiScale > 0 ? mc.gameSettings.guiScale : 1;
         float appScale = NewVonwareGui.getAppliedScale();
         float screenH = (float)mc.displayHeight / (float)scale / appScale;
         float maxH = Math.max(20.0F, screenH - (this.y + (float)this.height));
         float effectiveH = Math.min(animatedTotalH, maxH);
         float animatedH = effectiveH * animValue;
         int dropX = (int)(this.x * (float)scale * appScale);
         int dropY = (int)((float)mc.displayHeight - (this.y + (float)this.height + animatedH) * (float)scale * appScale);
         int dropW = (int)((float)(this.width * scale) * appScale);
         int dropH = Math.max(0, (int)(animatedH * (float)scale * appScale));
         int ix = Math.max(NewComponent.scissorX, dropX);
         int iy = Math.max(NewComponent.scissorY, dropY);
         int iw = Math.max(0, Math.min(NewComponent.scissorX + NewComponent.scissorW, dropX + dropW) - ix);
         int ih = Math.max(0, Math.min(NewComponent.scissorY + NewComponent.scissorH, dropY + dropH) - iy);
         GL11.glScissor(ix, iy, iw, ih);
         RenderUtil.drawGradientRect(this.x + 1.0F, this.y + (float)this.height, this.x + (float)this.width - 1.0F, this.y + (float)this.height + animatedTotalH, 201326592, 369098752);
         RenderUtil.drawRect(this.x, this.y + (float)this.height, this.x + 1.0F, this.y + (float)this.height + animatedTotalH, argb((int)(60.0F * animValue), this.baseColor[0], this.baseColor[1], this.baseColor[2]));
         int iscl = scale;
         float iApp = appScale;
         long now = System.currentTimeMillis();
         int staggerSlot = 0;

         for(Item item : this.items) {
            item.update();
            boolean hidden = item.isHidden();
            boolean wasHidden = (Boolean)this.itemPrevHidden.getOrDefault(item, hidden);
            if (wasHidden && !hidden) {
               this.itemRevealTime.put(item, now + (long)staggerSlot * 100L);
               ++staggerSlot;
            }

            this.itemPrevHidden.put(item, hidden);
         }

         float yOffset = this.y + (float)this.height + 0.5F;

         for(Item item : this.items) {
            int var10002 = NewComponent.counter1[0]++;
            AnimationUtil.LerpAnimation va = (AnimationUtil.LerpAnimation)this.itemVisAnims.get(item);
            boolean hidden = item.isHidden();
            long revealAt = (Long)this.itemRevealTime.getOrDefault(item, 0L);
            boolean shouldShow = !hidden && now >= revealAt;
            if (va != null) {
               va.tick(shouldShow);
            }

            float vis = va != null ? va.getValue() : (shouldShow ? 1.0F : 0.0F);
            if (vis > 0.001F) {
               item.setLocation(this.x + 1.5F, yOffset);
               item.setWidth(this.width - 8);
               float visH = (float)item.getHeight() * vis;
               int ciX = (int)((this.x + 1.5F) * (float)iscl * iApp);
               int ciY = (int)((float)mc.displayHeight - (yOffset + visH) * (float)iscl * iApp);
               int ciW = (int)(((float)this.width - 1.5F) * (float)iscl * iApp);
               int ciH = Math.max(0, (int)(visH * (float)iscl * iApp));
               int fiX = Math.max(ix, ciX);
               int fiY = Math.max(iy, ciY);
               int fiW = Math.max(0, Math.min(ix + iw, ciX + ciW) - fiX);
               int fiH = Math.max(0, Math.min(iy + ih, ciY + ciH) - fiY);
               GL11.glScissor(fiX, fiY, fiW, fiH);
               item.drawScreen(mouseX, mouseY, partialTicks);
               if (clickGui.description != null && clickGui.description.getValue() != ClickGui.Description.None && (Boolean)clickGui.settingDescriptions.getValue() && this.isSettingDescriptionHovered(item, mouseX, mouseY)) {
                  Setting<?> hoveredSetting = (Setting)this.itemSettings.get(item);
                  if (hoveredSetting != null) {
                     this.queueDescription(hoveredSetting.getDescription(), mouseX, mouseY, clickGui);
                  }
               }

               GL11.glScissor(ix, iy, iw, ih);
               yOffset += ((float)item.getHeight() + 1.5F) * vis;
            }
         }

         GL11.glScissor(NewComponent.scissorX, NewComponent.scissorY, NewComponent.scissorW, NewComponent.scissorH);
      }

      if (clickGui.description != null && clickGui.description.getValue() != ClickGui.Description.None && this.isHovering(mouseX, mouseY)) {
         this.queueDescription(this.module.getDescription(), mouseX, mouseY, clickGui);
      }

   }

   private void drawSettingsIndicator(ClickGui clickGui) {
      if (!this.items.isEmpty() && clickGui.settingsIndicator.getValue() != ClickGui.SettingsIndicator.None) {
         int rawColor = (Integer)clickGui.indicatorColor.getValue();
         Color indicatorColor = new Color(rawColor >> 16 & 255, rawColor >> 8 & 255, rawColor & 255, 255);
         if (clickGui.settingsIndicator.getValue() == ClickGui.SettingsIndicator.Bubble) {
            float centerY = this.y + (float)this.height * 0.5F;
            float rightX = this.x + (float)this.width - 3.25F;
            float radius = 0.75F;
            this.beginBubbleRender();
            if (this.subOpen) {
               this.drawFilledCircle(rightX, centerY, radius, indicatorColor);
            } else {
               float spacing = 2.35F;
               this.drawFilledCircle(rightX - spacing * 2.0F, centerY, radius, indicatorColor);
               this.drawFilledCircle(rightX - spacing, centerY, radius, indicatorColor);
               this.drawFilledCircle(rightX, centerY, radius, indicatorColor);
            }

            this.endBubbleRender();
         } else {
            String indicator;
            if (clickGui.settingsIndicator.getValue() == ClickGui.SettingsIndicator.Dots) {
               indicator = this.subOpen ? "." : "...";
            } else {
               indicator = this.subOpen ? "-" : "+";
            }

            float indicatorWidth = (float)Vonware.textManager.getCustomFont().getStringWidth(indicator);
            float indicatorX = this.x + (float)this.width - indicatorWidth - 2.0F;
            float indicatorY = (float)Math.round(this.y + 3.0F);
            GlStateManager.enableTexture2D();
            GlStateManager.enableBlend();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            Vonware.textManager.getCustomFont().drawStringWithShadow(indicator, (double)indicatorX, (double)indicatorY, indicatorColor.getRGB());
            GlStateManager.disableBlend();
         }
      }
   }

   private void beginBubbleRender() {
      GlStateManager.disableTexture2D();
      GlStateManager.enableBlend();
      GlStateManager.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA);
      GlStateManager.disableDepth();
      GlStateManager.disableAlpha();
      GlStateManager.disableCull();
      GL11.glEnable(2848);
      GL11.glEnable(2881);
      GL11.glHint(3155, 4354);
   }

   private void endBubbleRender() {
      GL11.glDisable(2848);
      GL11.glDisable(2881);
      GlStateManager.enableTexture2D();
      GlStateManager.enableDepth();
      GlStateManager.enableAlpha();
      GlStateManager.enableCull();
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
   }

   private void drawFilledCircle(float cx, float cy, float radius, Color color) {
      int segments = 32;
      GL11.glColor4f((float)color.getRed() / 255.0F, (float)color.getGreen() / 255.0F, (float)color.getBlue() / 255.0F, (float)color.getAlpha() / 255.0F);
      GL11.glBegin(6);
      GL11.glVertex2f(cx, cy);
      double step = 0.19634954084936207;

      for(int i = 0; i <= 32; ++i) {
         double angle = (double)i * step;
         GL11.glVertex2f((float)((double)cx + Math.cos(angle) * (double)radius), (float)((double)cy + Math.sin(angle) * (double)radius));
      }

      GL11.glEnd();
   }

   private void drawFlowingButtonOutline(float x1, float y1, float x2, float y2, float lineW, float animAlpha, int gR, int gG, int gB) {
      float w = x2 - x1;
      float h = y2 - y1;
      float per = 2.0F * (w + h);
      int segs = (int)Math.ceil((double)(per / 1.0F));
      float elapsed = (float)((System.currentTimeMillis() - outlineGlowStart) % 1800L);
      float glowCenter = elapsed / 1800.0F * per;
      int lR = Math.min(255, gR + 110);
      int lG = Math.min(255, gG + 110);
      int lB = Math.min(255, gB + 110);
      GlStateManager.disableTexture2D();
      GlStateManager.enableBlend();
      GlStateManager.tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ZERO);
      GlStateManager.disableAlpha();
      GL11.glEnable(2848);
      GL11.glPushAttrib(32768);
      GL11.glHint(3154, 4354);
      GL11.glLineWidth(lineW);
      GL11.glBegin(3);

      for(int i = 0; i <= segs; ++i) {
         float t = (float)i / (float)segs;
         float dist = t * per;
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

         float behind = glowCenter - dist;
         if (behind < -per * 0.5F) {
            behind += per;
         }

         float r;
         float g;
         float b;
         float a;
         if (!(behind < 0.0F) && !(behind > 150.0F)) {
            float nd = behind / 150.0F;
            float gs = (float)Math.cos((double)nd * Math.PI / (double)2.0F);
            r = ((float)gR + (float)(lR - gR) * gs) / 255.0F;
            g = ((float)gG + (float)(lG - gG) * gs) / 255.0F;
            b = ((float)gB + (float)(lB - gB) * gs) / 255.0F;
            a = (80.0F + 175.0F * gs) / 255.0F * animAlpha;
         } else {
            r = (float)gR / 255.0F;
            g = (float)gG / 255.0F;
            b = (float)gB / 255.0F;
            a = 0.3137255F * animAlpha;
         }

         GL11.glColor4f(r, g, b, a);
         GL11.glVertex2f(px, py);
      }

      GL11.glEnd();
      GL11.glPopAttrib();
      GL11.glDisable(2848);
      GL11.glLineWidth(1.0F);
      GlStateManager.enableTexture2D();
      GlStateManager.disableBlend();
      GlStateManager.enableAlpha();
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
   }

   public int getHeight() {
      float animValue = this.settingsAnimation.getEased();
      return !this.subOpen && !(animValue > 0.0F) ? 13 : (int)((float)this.height + this.cachedAnimatedTotalH * animValue);
   }

   public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
      super.mouseClicked(mouseX, mouseY, mouseButton);
      if (!this.items.isEmpty()) {
         if (mouseButton == 1 && this.isHovering(mouseX, mouseY)) {
            this.subOpen = !this.subOpen;
            SoundUtil.playModuleSettingsToggle(this.subOpen);
         }

         if (this.subOpen) {
            for(Item item : this.items) {
               if (!item.isHidden()) {
                  item.mouseClicked(mouseX, mouseY, mouseButton);
               }
            }
         }
      }

   }

   public void onKeyTyped(char typedChar, int keyCode) {
      super.onKeyTyped(typedChar, keyCode);
      if (!this.items.isEmpty() && this.subOpen) {
         for(Item item : this.items) {
            if (!item.isHidden()) {
               item.onKeyTyped(typedChar, keyCode);
            }
         }
      }

   }

   public List<Item> getSubItems() {
      return this.items;
   }

   public Module getModule() {
      return this.module;
   }

   public boolean isSettingsOpen() {
      return this.subOpen;
   }

   public void toggle() {
      this.module.toggle();
   }

   public boolean getState() {
      return this.module.isEnabled();
   }
}
