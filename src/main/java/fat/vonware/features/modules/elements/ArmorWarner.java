package fat.vonware.features.modules.elements;

import fat.vonware.Global;
import fat.vonware.Vonware;
import fat.vonware.event.events.Render2DEvent;
import fat.vonware.features.gui.newgui.newcomponents.NewVonwareGui;
import fat.vonware.features.modules.HudModule;
import fat.vonware.features.modules.Module;
import fat.vonware.features.modules.client.Manager;
import fat.vonware.features.setting.Setting;
import fat.vonware.util.InventoryUtil;
import fat.vonware.util.Animations.AnimationUtil;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

public class ArmorWarner extends HudModule {
   private static final ResourceLocation WARNING_ICON = new ResourceLocation("vonware", "textures/gui/warning.png");
   private static final int ICON_GAP = 3;
   private static final int ICON_SIZE_OFFSET = -3;
   private static final int[] NETHERITE_MAX_DAMAGE = new int[]{481, 555, 592, 407};
   private final Setting<Boolean> armorStack = new Setting<Boolean>("StackedArmor-Fix", true);
   private final Setting<Integer> armorThreshold = new Setting<Integer>("Threshold", 20, 1, 100);
   private final Setting<Boolean> pulseColor = new Setting<Boolean>("PulseColor", true);
   private final Setting<Boolean> renderIcon = new Setting<Boolean>("RenderIcon", true);
   private final Setting<Style> style;
   private final AnimationUtil.DeltaAnimation fadeAnim;

   public ArmorWarner() {
      super("ArmorWarner", "Warns you when your armor is breaking", Module.Category.Elements, true, false, false, 100.0F, 50.0F);
      this.style = new Setting<Style>("Style", ArmorWarner.Style.Old);
      this.fadeAnim = new AnimationUtil.DeltaAnimation(0.0F, 400.0F);
      this.register(this.armorThreshold);
      this.register(this.armorStack);
      this.register(this.pulseColor);
      this.register(this.renderIcon);
      this.register(this.style);
   }

   public void onRender2D(Render2DEvent event) {
      if (!fullNullCheck()) {
         this.rect.setX((Float)this.x.getValue());
         this.rect.setY((Float)this.y.getValue());
         boolean isEditing = mc.currentScreen instanceof NewVonwareGui && ((NewVonwareGui)mc.currentScreen).isHudEditor();
         List<String> lines = this.buildLines(isEditing);
         this.updateRectBounds(lines);
         float alpha = AnimationUtil.easeOutCubic(this.fadeAnim.tick(!lines.isEmpty()));
         if (!(alpha <= 0.0F)) {
            switch ((Style)this.style.getValue()) {
               case Modern:
                  this.renderModern(lines, alpha);
                  break;
               case Old:
               default:
                  this.renderOld(lines, alpha);
            }

         }
      }
   }

   private void renderOld(List<String> lines, float alpha) {
      float rx = this.rect.getX();
      float ry = this.rect.getY();
      int lineH = Vonware.textManager.getFontHeight2();
      float iconSz = (float)(lineH - -3);
      int offset = 0;
      int textColor = this.resolveColor(alpha);

      for(String s : lines) {
         float textW = (float)Vonware.textManager.getStringWidth(s);
         float iconAllowance = (Boolean)this.renderIcon.getValue() ? iconSz + 3.0F : 0.0F;
         float totalW = iconAllowance + textW;
         float rowLeft = rx + (this.rect.getWidth() - totalW) / 2.0F;
         if ((Boolean)this.renderIcon.getValue()) {
            float iconY = ry + (float)offset + ((float)lineH - iconSz) / 2.0F;
            mc.getTextureManager().bindTexture(WARNING_ICON);
            GlStateManager.enableBlend();
            GlStateManager.color(1.0F, 1.0F, 1.0F, alpha);
            Gui.drawModalRectWithCustomSizedTexture((int)rowLeft, (int)iconY, 0.0F, 0.0F, (int)iconSz, (int)iconSz, iconSz, iconSz);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.disableBlend();
         }

         Vonware.textManager.drawStringWithShadow(s, rowLeft + iconAllowance, ry + (float)offset, textColor);
         offset += lineH;
      }

   }

   private void renderModern(List<String> lines, float alpha) {
      this.renderOld(lines, alpha);
   }

   public void updateBounds() {
      this.rect.setX((Float)this.x.getValue());
      this.rect.setY((Float)this.y.getValue());
      boolean isEditing = mc.currentScreen instanceof NewVonwareGui && ((NewVonwareGui)mc.currentScreen).isHudEditor();
      this.updateRectBounds(this.buildLines(isEditing));
   }

   private List<String> buildLines(boolean isEditing) {
      List<String> lines = new ArrayList();
      if (isEditing) {
         if (mc.player != null) {
            String[] pieceNames = new String[]{"Boots", "Leggings", "Chestplate", "Helmet"};

            for(int i = 0; i < mc.player.inventory.armorInventory.size(); ++i) {
               ItemStack stack = (ItemStack)mc.player.inventory.armorInventory.get(i);
               String pieceName = pieceNames[i];
               if (stack.isEmpty()) {
                  lines.add("No " + pieceName + " equipped!");
               } else {
                  int maxDamage = (Boolean)Manager.getInstance().calcNetherite.getValue() ? NETHERITE_MAX_DAMAGE[i] : stack.getMaxDamage();
                  int dura = maxDamage - stack.getItemDamage();
                  int scaled = (int)((float)dura / (float)maxDamage * 100.0F);
                  lines.add("Your " + pieceName + " is at " + scaled + "%!");
               }
            }
         } else {
            lines.add("Your Helmet is at 12%!");
            lines.add("Your Chestplate is at 8%!");
         }

         return lines;
      } else if (mc.player == null) {
         return lines;
      } else {
         for(int i = 0; i < mc.player.inventory.armorInventory.size(); ++i) {
            ItemStack stack = (ItemStack)mc.player.inventory.armorInventory.get(i);
            if (!stack.isEmpty() && (!(Boolean)this.armorStack.getValue() || stack.getCount() <= 1)) {
               String armor = InventoryUtil.getArmorPieceName(stack);
               if (armor != null) {
                  int maxDamage = (Boolean)Manager.getInstance().calcNetherite.getValue() ? NETHERITE_MAX_DAMAGE[i] : stack.getMaxDamage();
                  int dura = maxDamage - stack.getItemDamage();
                  int scaledDurability = (int)((float)dura / (float)maxDamage * 100.0F);
                  if (scaledDurability <= (Integer)this.armorThreshold.getValue()) {
                     lines.add("Your " + armor + (armor.endsWith("s") ? " are" : " is") + " at " + scaledDurability + "%!");
                  }
               }
            }
         }

         return lines;
      }
   }

   private void updateRectBounds(List<String> lines) {
      int padding = 4;
      int lineH = Vonware.textManager.getFontHeight2();
      float iconSz = (float)(lineH - -3);
      if (lines.isEmpty()) {
         this.rect.setWidth(80.0F);
         this.rect.setHeight((float)(lineH + padding));
      } else {
         float maxWidth = 0.0F;

         for(String s : lines) {
            float iconAllowance = (Boolean)this.renderIcon.getValue() ? iconSz + 3.0F : 0.0F;
            float rowW = iconAllowance + (float)Vonware.textManager.getStringWidth(s);
            if (rowW > maxWidth) {
               maxWidth = rowW;
            }
         }

         this.rect.setWidth(maxWidth + (float)padding);
         this.rect.setHeight((float)(lines.size() * lineH + padding));
      }
   }

   private int resolveColor(float alpha) {
      return (Boolean)this.pulseColor.getValue() ? withAlpha(Global.getPulseColor(Color.RED, 0).getRGB(), alpha) : withAlpha(Color.RED.getRGB(), alpha);
   }

   private static int withAlpha(int argb, float factor) {
      int a = (int)((float)(argb >>> 24 & 255) * factor);
      return a << 24 | argb & 16777215;
   }

   public static enum Style {
      Old,
      Modern;
   }
}
