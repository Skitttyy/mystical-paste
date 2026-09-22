package fat.vonware.features.modules.render;

import fat.vonware.event.events.Render2DEvent;
import fat.vonware.features.modules.Module;
import fat.vonware.features.setting.ColorSetting;
import fat.vonware.features.setting.Setting;
import fat.vonware.util.Animations.render.ColorUtil;
import fat.vonware.util.BlockHighlight.SlideUtil;
import java.awt.Color;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.GlStateManager.DestFactor;
import net.minecraft.client.renderer.GlStateManager.SourceFactor;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.ResourceLocation;

public class BetterHotbar extends Module {
   private static final ResourceLocation WIDGETS = new ResourceLocation("textures/gui/widgets.png");
   private final Setting<Boolean> hotbarTips = new Setting<Boolean>("Hotbar Numbers", true, "Toggles the Hotbar Numbers feature used by BetterHotbar.");
   private final ColorSetting tipColor = new ColorSetting("Color", 16777215, false, (v) -> (Boolean)this.hotbarTips.getValue(), "Selects the color used by BetterHotbar for Color rendering.");
   private final Setting<Float> tipScale = new Setting<Float>("Scale", 0.5F, 0.3F, 1.0F, (v) -> (Boolean)this.hotbarTips.getValue(), "Controls the rendered scale used by BetterHotbar.");
   private final Setting<Boolean> slideAnimation = new Setting<Boolean>("HotbarAnimation", true, "Toggles the HotbarAnimation feature used by BetterHotbar.");
   private final Setting<Float> animationTime = (new Setting<Float>("Animation-Time", 0.15F, 0.05F, 1.0F, (v) -> (Boolean)this.slideAnimation.getValue())).setStep(0.05F);
   private int currentSlot = -1;
   private int previousSlot = -1;
   private SlideUtil slideAnim;

   public BetterHotbar() {
      super("BetterHotbar", "Adds hotbar key tips and a slot switch animation", Module.Category.RENDER, true, false, false);
      this.slideAnim = new SlideUtil((long)((Float)this.animationTime.getValue() * 1000.0F));
      this.register(this.hotbarTips);
      this.register(this.tipColor);
      this.register(this.tipScale);
      this.register(this.slideAnimation);
      this.register(this.animationTime);
   }

   public void onRender2D(Render2DEvent event) {
      if (mc.player != null && !mc.gameSettings.hideGUI) {
         ScaledResolution sr = new ScaledResolution(mc);
         int screenWidth = sr.getScaledWidth();
         int screenHeight = sr.getScaledHeight();
         int hotbarLeft = screenWidth / 2 - 91;
         int hotbarTop = screenHeight - 22;
         int slot = mc.player.inventory.currentItem;
         if (this.currentSlot == -1) {
            this.currentSlot = slot;
         }

         if (this.currentSlot != slot) {
            this.previousSlot = this.currentSlot;
            this.currentSlot = slot;
            if ((Boolean)this.slideAnimation.getValue()) {
               this.slideAnim = new SlideUtil((long)((Float)this.animationTime.getValue() * 1000.0F));
            }
         }

         if ((Boolean)this.hotbarTips.getValue()) {
            this.drawHotbarTips(hotbarLeft, hotbarTop);
         }

         this.drawSelectionIndicator(hotbarLeft, hotbarTop);
      }
   }

   private void drawHotbarTips(int hotbarLeft, int hotbarTop) {
      Color color = ColorUtil.getResolvedColor(this.tipColor);
      KeyBinding[] keyBinds = mc.gameSettings.keyBindsHotbar;
      float scale = (Float)this.tipScale.getValue();

      for(int i = 0; i < 9 && i < keyBinds.length; ++i) {
         String label = keyBinds[i].getDisplayName();
         int slotX = hotbarLeft + i * 20;
         GlStateManager.pushMatrix();
         GlStateManager.scale(scale, scale, 1.0F);
         float drawX = (float)(slotX + 2) / scale;
         float drawY = (float)(hotbarTop + 2) / scale;
         mc.fontRenderer.drawStringWithShadow(label, drawX, drawY, color.getRGB());
         GlStateManager.popMatrix();
      }

   }

   private void drawSelectionIndicator(int hotbarLeft, int hotbarTop) {
      double x = (double)(hotbarLeft + this.currentSlot * 20 - 1);
      if ((Boolean)this.slideAnimation.getValue() && this.previousSlot != -1 && this.previousSlot != this.currentSlot && !this.slideAnim.isEnd()) {
         double progress = this.slideAnim.easeOutQuad();
         double fromX = (double)(hotbarLeft + this.previousSlot * 20 - 1);
         double toX = (double)(hotbarLeft + this.currentSlot * 20 - 1);
         x = fromX + (toX - fromX) * progress;
      }

      GlStateManager.pushMatrix();
      GlStateManager.enableBlend();
      GlStateManager.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA);
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      mc.getTextureManager().bindTexture(WIDGETS);
      Gui.drawModalRectWithCustomSizedTexture((int)x, hotbarTop - 1, 0.0F, 22.0F, 24, 22, 256.0F, 256.0F);
      GlStateManager.disableBlend();
      GlStateManager.popMatrix();
   }
}
