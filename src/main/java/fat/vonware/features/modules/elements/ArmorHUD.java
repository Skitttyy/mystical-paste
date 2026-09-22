package fat.vonware.features.modules.elements;

import fat.vonware.Vonware;
import fat.vonware.event.events.Render2DEvent;
import fat.vonware.features.modules.HudModule;
import fat.vonware.features.modules.Module;
import fat.vonware.features.modules.client.Manager;
import fat.vonware.features.setting.Setting;
import fat.vonware.util.DamageUtil;
import fat.vonware.util.PlayerUtil;
import fat.vonware.util.Animations.render.RenderUtil;
import java.awt.Color;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemAir;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;

public class ArmorHUD extends HudModule {
   private final Setting<Boolean> armorPercent = new Setting<Boolean>("Percent Symbol", true);
   protected final Setting<Text> text;
   private final Setting<Boolean> animate;
   private static final int[] NETHERITE_MAX_DAMAGE = new int[]{481, 555, 592, 407};

   public ArmorHUD() {
      super("ArmorHUD", "Displays your armor", Module.Category.Elements, true, false, false, 100.0F, 50.0F);
      this.text = new Setting<Text>("Text", ArmorHUD.Text.New);
      this.animate = new Setting<Boolean>("Animate", true);
      this.register(this.armorPercent);
      this.register(this.text);
      this.register(this.animate);
      this.enabled.setValue(true);
   }

   public boolean isAutoPositioned() {
      return true;
   }

   public void onRender2D(Render2DEvent event) {
      if (!PlayerUtil.isSpectator()) {
         GlStateManager.pushMatrix();
         GlStateManager.enableTexture2D();
         RenderHelper.enableGUIStandardItemLighting();
         int width = (int)(event.getScreenWidth() / (double)2.0F);
         int height = (int)event.getScreenHeight();
         int i2 = 15;
         int i4 = 3;

         for(int i3 = 3; i4 >= 0; i4 = i3) {
            ItemStack stack = (ItemStack)mc.player.inventory.armorInventory.get(i3);
            if (!(stack.getItem() instanceof ItemAir)) {
               int y = height - getArmorY();
               int x = width + i2;
               int dmg = (int)((Boolean)Manager.getInstance().calcNetherite.getValue() ? this.getNetheriteDamageInPercent(stack, i3) : DamageUtil.getDamageInPercent(stack));
               int color = (new Color((Boolean)Manager.getInstance().calcNetherite.getValue() ? this.getNetheriteColor(stack, i3) : stack.getItem().getRGBDurabilityForDisplay(stack))).getRGB();
               GlStateManager.enableDepth();
               mc.getRenderItem().zLevel = 200.0F;
               mc.getRenderItem().renderItemAndEffectIntoGUI(stack, x, y);
               mc.getRenderItem().renderItemOverlayIntoGUI(mc.fontRenderer, stack, x, y, "");
               mc.getRenderItem().zLevel = 0.0F;
               GlStateManager.enableTexture2D();
               GlStateManager.disableLighting();
               GlStateManager.disableDepth();
               String count = stack.getCount() > 1 ? stack.getCount() + "" : "";
               Vonware.textManager.drawStringWithShadow(count, (float)(x + 19 - 2 - this.getWidth(count)), (float)(y + 9), -1);
               i2 += 18;
               if (this.text.getValue() == ArmorHUD.Text.Old) {
                  String suffix = (Boolean)this.armorPercent.getValue() && dmg != 100 ? "%" : "";
                  float tx = (float)(x + 8 - this.getWidth(dmg + suffix) / 2);
                  float ty = (float)(y - 8);
                  if ((Boolean)this.animate.getValue()) {
                     RenderUtil.drawAnimatedInt("armor_old_" + i3, dmg, tx, ty, suffix, color, true, 1.0F);
                  } else {
                     Vonware.textManager.drawStringWithShadow(dmg + suffix, tx, ty, color);
                  }
               } else {
                  GlStateManager.pushMatrix();
                  GlStateManager.scale(0.625F, 0.625F, 0.625F);
                  GlStateManager.disableDepth();
                  String suffix = (Boolean)this.armorPercent.getValue() ? "%" : "";
                  float xOff = (float)((Boolean)this.armorPercent.getValue() ? 0 : 1);
                  float tx = xOff + (float)(x + this.getFixedArmorOffset(dmg)) * 1.6F;
                  float ty = (float)y * 1.6F - 8.0F;
                  if ((Boolean)this.animate.getValue()) {
                     RenderUtil.drawAnimatedInt("armor_new_" + i3, dmg, tx, ty, suffix, color, true, 0.625F);
                  } else {
                     Vonware.textManager.drawStringWithShadow(dmg + suffix, tx, ty, color);
                  }

                  GlStateManager.enableDepth();
                  GlStateManager.scale(1.0F, 1.0F, 1.0F);
                  GlStateManager.popMatrix();
               }
            }

            --i3;
         }

         GlStateManager.enableDepth();
         GlStateManager.disableLighting();
         RenderHelper.disableStandardItemLighting();
         GlStateManager.popMatrix();
      }
   }

   public static int getArmorY() {
      int y;
      if (mc.player.isInsideOfMaterial(Material.WATER) && mc.player.getAir() > 0 && !mc.player.capabilities.isCreativeMode) {
         y = 65;
      } else if (mc.player.getRidingEntity() != null && !mc.player.capabilities.isCreativeMode) {
         if (mc.player.getRidingEntity() instanceof EntityLivingBase) {
            EntityLivingBase entity = (EntityLivingBase)mc.player.getRidingEntity();
            y = (int)((double)45.0F + Math.ceil((double)((entity.getMaxHealth() - 1.0F) / 20.0F)) * (double)10.0F);
         } else {
            y = 45;
         }
      } else if (mc.player.capabilities.isCreativeMode) {
         y = mc.player.isRidingHorse() ? 45 : 38;
      } else {
         y = 55;
      }

      return y;
   }

   public int getFixedArmorOffset(int percent) {
      if (percent == 100) {
         return 1;
      } else {
         return percent < 10 ? 5 : 3;
      }
   }

   public int getWidth(String text) {
      return mc.fontRenderer.getStringWidth(text);
   }

   private double getNetheriteDamageInPercent(ItemStack stack, int slot) {
      int maxDamage = NETHERITE_MAX_DAMAGE[MathHelper.clamp(slot, 0, 3)];
      double percent = (double)stack.getItemDamage() / (double)maxDamage;
      if (percent <= (double)0.0F) {
         return (double)100.0F;
      } else {
         return percent >= (double)1.0F ? (double)0.0F : (double)100.0F - percent * (double)100.0F;
      }
   }

   private int getNetheriteColor(ItemStack stack, int slot) {
      int maxDamage = NETHERITE_MAX_DAMAGE[MathHelper.clamp(slot, 0, 3)];
      float fraction = MathHelper.clamp((float)stack.getItemDamage() / (float)maxDamage, 0.0F, 1.0F);
      return MathHelper.hsvToRGB(Math.max(0.0F, 1.0F - fraction) / 3.0F, 1.0F, 1.0F);
   }

   public static enum Text {
      New,
      Old;
   }
}
