package fat.vonware.features.modules.elements;

import fat.vonware.Global;
import fat.vonware.event.events.Render2DEvent;
import fat.vonware.features.gui.components.items.buttons.render.ColorPickerConfig;
import fat.vonware.features.gui.newgui.newcomponents.NewComponent;
import fat.vonware.features.modules.HudModule;
import fat.vonware.features.modules.Module;
import fat.vonware.features.modules.client.ClickGui;
import fat.vonware.features.modules.client.Colors;
import fat.vonware.features.setting.ColorSetting;
import fat.vonware.features.setting.Setting;
import fat.vonware.util.PlayerUtil;
import fat.vonware.util.Animations.render.ColorUtil;
import fat.vonware.util.Animations.render.RenderUtil;
import java.awt.Color;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

public class TotemHUD extends HudModule {
   private static final ItemStack totem;
   private final ColorSetting customColor = new ColorSetting("Color", 16777215, false);
   private final Setting<Boolean> autoPosition = new Setting<Boolean>("AutoPos", true);
   private final Setting<Boolean> totemCrosshair = new Setting<Boolean>("CrossHair", false);
   private final Setting<Boolean> animate = new Setting<Boolean>("Animate", true);

   public TotemHUD() {
      super("Totem", "Displays totem", Module.Category.Elements, true, false, false, 100.0F, 50.0F);
      this.register(this.customColor);
      this.register(this.autoPosition);
      this.register(this.totemCrosshair);
      this.register(this.animate);
   }

   public boolean isAutoPositioned() {
      return (Boolean)this.autoPosition.getValue();
   }

   public void onRender2D(Render2DEvent event) {
      super.onRender2D(event);
      if (!PlayerUtil.isSpectator()) {
         this.renderTotemHUD();
      }
   }

   public String getDisplayInfo() {
      if (mc.player == null) {
         return null;
      } else {
         int totems = mc.player.inventory.mainInventory.stream().filter((itemStack) -> itemStack.getItem() == Items.TOTEM_OF_UNDYING).mapToInt(ItemStack::getCount).sum();
         if (mc.player.getHeldItemOffhand().getItem() == Items.TOTEM_OF_UNDYING) {
            totems += mc.player.getHeldItemOffhand().getCount();
         }

         return totems > 0 ? "" + totems : null;
      }
   }

   public void renderTotemHUD() {
      int totems = mc.player.inventory.mainInventory.stream().filter((itemStack) -> itemStack.getItem() == Items.TOTEM_OF_UNDYING).mapToInt(ItemStack::getCount).sum();
      if (mc.player.getHeldItemOffhand().getItem() == Items.TOTEM_OF_UNDYING) {
         totems += mc.player.getHeldItemOffhand().getCount();
      }

      if (totems > 0) {
         GlStateManager.enableTexture2D();
         int xPos;
         int yPos;
         if ((Boolean)this.totemCrosshair.getValue()) {
            xPos = 419;
            yPos = 240;
         } else if ((Boolean)this.autoPosition.getValue()) {
            ScaledResolution sr = new ScaledResolution(mc);
            xPos = sr.getScaledWidth() / 2 - 189 + 180 + 1;
            yPos = sr.getScaledHeight() - 55;
         } else {
            xPos = (int)this.rect.getX();
            yPos = (int)this.rect.getY();
         }

         GlStateManager.enableDepth();
         RenderUtil.itemRender.zLevel = 200.0F;
         RenderUtil.itemRender.renderItemAndEffectIntoGUI(totem, xPos, yPos);
         RenderUtil.itemRender.renderItemOverlayIntoGUI(mc.fontRenderer, totem, xPos, yPos, "");
         RenderUtil.itemRender.zLevel = 0.0F;
         GlStateManager.enableTexture2D();
         GlStateManager.disableLighting();
         GlStateManager.disableDepth();
         int color = this.getCustomColor().getRGB();
         float tx = (float)(xPos + 19 - 2 - this.renderer.getStringWidth(String.valueOf(totems)));
         float ty = (float)(yPos + 9);
         if ((Boolean)this.animate.getValue()) {
            RenderUtil.drawAnimatedInt("totem_count", totems, tx, ty, "", color, true, 1.0F);
         } else {
            this.renderer.drawStringWithShadow(totems + "", tx, ty, color);
         }

         GlStateManager.enableDepth();
         GlStateManager.disableLighting();
      }
   }

   private Color getCustomColor() {
      if (ColorPickerConfig.getState(this.customColor).useGlobalColor) {
         Colors colors = Colors.getInstance();
         if ((Boolean)colors.rainbow.getValue()) {
            return colors.rainbowMode.getValue() == Colors.RainbowMode.Static ? ColorUtil.rainbow(0) : ColorUtil.rainbow((int)((float)NewComponent.counter1[0] * (Float)colors.rainbowHue.getValue()));
         } else {
            return (Boolean)ClickGui.getInstance().pulseGui.getValue() ? Global.getPulseColor(colors.getGuiColor(), 0) : colors.getCurrentColor();
         }
      } else {
         int colorInt = (Integer)this.customColor.getValue();
         return new Color(colorInt >> 16 & 255, colorInt >> 8 & 255, colorInt & 255);
      }
   }

   static {
      totem = new ItemStack(Items.TOTEM_OF_UNDYING);
   }
}
