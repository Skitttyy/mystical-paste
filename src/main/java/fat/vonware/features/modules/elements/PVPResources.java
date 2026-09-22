package fat.vonware.features.modules.elements;

import com.mojang.realmsclient.gui.ChatFormatting;
import fat.vonware.Global;
import fat.vonware.Vonware;
import fat.vonware.event.events.Render2DEvent;
import fat.vonware.features.modules.HudModule;
import fat.vonware.features.modules.Module;
import fat.vonware.features.modules.client.Colors;
import fat.vonware.features.setting.Setting;
import fat.vonware.util.Animations.render.ColorUtil;
import java.awt.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.potion.PotionUtils;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class PVPResources extends HudModule {
   public Setting<Boolean> watermark = new Setting<Boolean>("Watermark", false);
   public Setting<Boolean> totems = new Setting<Boolean>("Totems", true);
   public Setting<Boolean> xpBottles = new Setting<Boolean>("XPBottles", true);
   public Setting<Boolean> goldenApples = new Setting<Boolean>("Golden Apples", true);
   public Setting<Boolean> crystals = new Setting<Boolean>("Crystals", true);
   public Setting<Boolean> beds = new Setting<Boolean>("Beds", false);
   public Setting<Boolean> autoPosition = new Setting<Boolean>("AutoPos", false);
   public Setting<Boolean> healthPots = new Setting<Boolean>("Health Pots", false);

   public PVPResources() {
      super("PVPResources", "Shows you PVP information about your inventory", Module.Category.Elements, true, false, false, 100.0F, 50.0F);
      this.register(this.watermark);
      this.register(this.totems);
      this.register(this.xpBottles);
      this.register(this.goldenApples);
      this.register(this.crystals);
      this.register(this.beds);
      this.register(this.autoPosition);
      this.register(this.healthPots);
   }

   public boolean isAutoPositioned() {
      return (Boolean)this.autoPosition.getValue();
   }

   @SubscribeEvent
   public void onRender2D(Render2DEvent event) {
      super.onRender2D(event);
      float xPos;
      float yPos;
      if ((Boolean)this.autoPosition.getValue()) {
         xPos = 1.0F;
         yPos = 313.0F;
      } else {
         xPos = this.rect.getX();
         yPos = this.rect.getY();
      }

      int color;
      if ((Boolean)Colors.getInstance().rainbow.getValue()) {
         if (Colors.getInstance().rainbowMode.getValue() == Colors.RainbowMode.Static) {
            color = ColorUtil.rainbow(0).getRGB();
         } else {
            color = ColorUtil.rainbow(0).getRGB();
         }
      } else {
         Color currentColor = Colors.getInstance().getCurrentColor();
         color = ColorUtil.toRGBA(currentColor.getRed(), currentColor.getGreen(), currentColor.getBlue());
      }

      if ((Boolean)this.watermark.getValue()) {
         String displayWatermark = "Mystic " + Global.clientVersion();
         Vonware.textManager.drawStringWithShadow(displayWatermark, xPos, yPos, color);
         yPos += 10.0F;
      }

      if ((Boolean)this.totems.getValue()) {
         int totemCount = this.getItemCount(Items.TOTEM_OF_UNDYING);
         Vonware.textManager.drawStringWithShadow("Totems: " + ChatFormatting.WHITE + totemCount, xPos, yPos, color);
         yPos += 10.0F;
      }

      if ((Boolean)this.xpBottles.getValue()) {
         int xpBottleCount = this.getItemCount(Items.EXPERIENCE_BOTTLE);
         Vonware.textManager.drawStringWithShadow("XP: " + ChatFormatting.WHITE + xpBottleCount, xPos, yPos, color);
         yPos += 10.0F;
      }

      if ((Boolean)this.goldenApples.getValue()) {
         int goldenAppleCount = this.getItemCount(Items.GOLDEN_APPLE);
         Vonware.textManager.drawStringWithShadow("Gaps: " + ChatFormatting.WHITE + goldenAppleCount, xPos, yPos, color);
         yPos += 10.0F;
      }

      if ((Boolean)this.crystals.getValue()) {
         int crystalCount = this.getItemCount(Items.END_CRYSTAL);
         Vonware.textManager.drawStringWithShadow("Crystals: " + ChatFormatting.WHITE + crystalCount, xPos, yPos, color);
         yPos += 10.0F;
      }

      if ((Boolean)this.beds.getValue()) {
         int bedCount = this.getItemCount(Items.BED);
         Vonware.textManager.drawStringWithShadow("Beds: " + ChatFormatting.WHITE + bedCount, xPos, yPos, color);
         yPos += 10.0F;
      }

      if ((Boolean)this.healthPots.getValue()) {
         int healthPotCount = this.getHealthSplashPotionCount();
         Vonware.textManager.drawStringWithShadow("Pots: " + ChatFormatting.WHITE + healthPotCount, xPos, yPos, color);
         yPos += 10.0F;
      }

   }

   public void updateBounds() {
      if ((Boolean)this.autoPosition.getValue()) {
         this.rect.setX(1.0F);
         this.rect.setY(313.0F);
      } else {
         this.rect.setX((Float)this.x.getValue());
         this.rect.setY((Float)this.y.getValue());
      }

      int maxWidth = 0;
      if ((Boolean)this.watermark.getValue()) {
         String displayWatermark = "Zenith " + Global.clientVersion();
         maxWidth = Math.max(maxWidth, Vonware.textManager.getStringWidth(displayWatermark));
      }

      if ((Boolean)this.totems.getValue()) {
         maxWidth = Math.max(maxWidth, Vonware.textManager.getStringWidth("Totems: 999"));
      }

      if ((Boolean)this.xpBottles.getValue()) {
         maxWidth = Math.max(maxWidth, Vonware.textManager.getStringWidth("XP: 999"));
      }

      if ((Boolean)this.goldenApples.getValue()) {
         maxWidth = Math.max(maxWidth, Vonware.textManager.getStringWidth("Gaps: 999"));
      }

      if ((Boolean)this.crystals.getValue()) {
         maxWidth = Math.max(maxWidth, Vonware.textManager.getStringWidth("Crystals: 999"));
      }

      if ((Boolean)this.beds.getValue()) {
         maxWidth = Math.max(maxWidth, Vonware.textManager.getStringWidth("Beds: 999"));
      }

      if ((Boolean)this.healthPots.getValue()) {
         maxWidth = Math.max(maxWidth, Vonware.textManager.getStringWidth("Pots: 999"));
      }

      int lineCount = 0;
      if ((Boolean)this.watermark.getValue()) {
         ++lineCount;
      }

      if ((Boolean)this.totems.getValue()) {
         ++lineCount;
      }

      if ((Boolean)this.xpBottles.getValue()) {
         ++lineCount;
      }

      if ((Boolean)this.goldenApples.getValue()) {
         ++lineCount;
      }

      if ((Boolean)this.crystals.getValue()) {
         ++lineCount;
      }

      if ((Boolean)this.beds.getValue()) {
         ++lineCount;
      }

      if ((Boolean)this.healthPots.getValue()) {
         ++lineCount;
      }

      int totalHeight = lineCount * 10;
      this.rect.setWidth((float)maxWidth);
      this.rect.setHeight((float)totalHeight);
   }

   private int getItemCount(Item item) {
      int count = 0;

      for(ItemStack stack : Minecraft.getMinecraft().player.inventory.mainInventory) {
         if (stack.getItem() == item) {
            count += stack.getCount();
         }
      }

      return count;
   }

   private int getHealthSplashPotionCount() {
      int count = 0;

      for(ItemStack stack : Minecraft.getMinecraft().player.inventory.mainInventory) {
         if (stack.getItem() == Items.SPLASH_POTION) {
            for(PotionEffect effect : PotionUtils.getEffectsFromStack(stack)) {
               if (effect.getPotion() == MobEffects.INSTANT_HEALTH) {
                  count += stack.getCount();
                  break;
               }
            }
         }
      }

      return count;
   }
}
