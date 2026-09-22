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
import fat.vonware.util.Animations.render.ColorUtil;
import fat.vonware.util.Animations.render.Render2DMethods;
import java.awt.Color;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class InventoryPreview extends HudModule {
   private static final int BASE_W = 148;
   private static final int BASE_H = 52;
   private final ColorSetting customColor = new ColorSetting("Color", 16777215, true);
   public Setting<Boolean> rectangle = new Setting<Boolean>("Rect", true);
   public Setting<Boolean> outline = new Setting<Boolean>("Outline", true);
   public Setting<Float> scale = new Setting<Float>("Scale", 1.0F, 0.1F, 2.0F);
   private final Setting<Boolean> autoPosition = new Setting<Boolean>("AutoPos", false);

   public InventoryPreview() {
      super("InventoryPreview", "Displays a preview of the inventory", Module.Category.Elements, true, false, true, 148.0F, 52.0F);
      this.register(this.customColor);
      this.register(this.rectangle);
      this.register(this.outline);
      this.register(this.scale);
      this.register(this.autoPosition);
   }

   private float scaledW() {
      return 148.0F * (Float)this.scale.getValue();
   }

   private float scaledH() {
      return 52.0F * (Float)this.scale.getValue();
   }

   public void updateBounds() {
      if ((Boolean)this.autoPosition.getValue()) {
         this.rect.setX(522.0F);
         this.rect.setY(440.0F);
      } else {
         this.rect.setX((Float)this.x.getValue());
         this.rect.setY((Float)this.y.getValue());
      }

      this.rect.setWidth(this.scaledW());
      this.rect.setHeight(this.scaledH());
   }

   public boolean isAutoPositioned() {
      return (Boolean)this.autoPosition.getValue();
   }

   @SubscribeEvent
   public void onRender2D(Render2DEvent event) {
      if ((Boolean)this.autoPosition.getValue()) {
         this.rect.setX(522.0F);
         this.rect.setY(440.0F);
      } else {
         this.rect.setX((Float)this.x.getValue());
         this.rect.setY((Float)this.y.getValue());
      }

      this.rect.setWidth(this.scaledW());
      this.rect.setHeight(this.scaledH());
      float rx = this.rect.getX();
      float ry = this.rect.getY();
      float s = (Float)this.scale.getValue();
      Color color = this.getCustomColor();
      if ((Boolean)this.rectangle.getValue()) {
         if ((Boolean)this.outline.getValue()) {
            Render2DMethods.drawBorderedRect(rx, ry, rx + this.scaledW(), ry + this.scaledH(), 1023410176, color.getRGB());
         } else {
            Render2DMethods.drawRect(rx, ry, rx + this.scaledW(), ry + this.scaledH(), 1023410176);
         }
      }

      GlStateManager.pushMatrix();
      GlStateManager.translate(rx, ry, 0.0F);
      GlStateManager.scale(s, s, 1.0F);
      RenderHelper.enableGUIStandardItemLighting();

      for(int i = 0; i < 27; ++i) {
         ItemStack stack = (ItemStack)mc.player.inventory.mainInventory.get(i + 9);
         int itemPosX = 2 + i % 9 * 16;
         int itemPosY = 2 + i / 9 * 16;
         mc.getRenderItem().renderItemAndEffectIntoGUI(stack, itemPosX, itemPosY);
         mc.getRenderItem().renderItemOverlayIntoGUI(mc.fontRenderer, stack, itemPosX, itemPosY, (String)null);
      }

      mc.getRenderItem().zLevel = -5.0F;
      RenderHelper.disableStandardItemLighting();
      GlStateManager.popMatrix();
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
}
