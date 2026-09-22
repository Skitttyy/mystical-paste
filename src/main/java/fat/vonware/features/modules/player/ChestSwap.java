package fat.vonware.features.modules.player;

import fat.vonware.features.modules.Module;
import fat.vonware.features.setting.Setting;
import net.minecraft.init.Items;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;

public class ChestSwap extends Module {
   private final Setting<Boolean> onePointNineTeen = new Setting<Boolean>("1.19+", true, "Swaps the armor piece with an elytra when you right click on it, mimicking that of newer versions of Minecraft.");
   private boolean wasRightClickPressed = false;

   public ChestSwap() {
      super("ChestSwap", "swap a chestplate with an elytra (and vice versa)", Module.Category.PLAYER, true, false, false);
      this.register(this.onePointNineTeen);
   }

   public void onEnable() {
      if (!(Boolean)this.onePointNineTeen.getValue()) {
         this.performSwap();
         this.disable();
      }

   }

   public void onUpdate() {
      if ((Boolean)this.onePointNineTeen.getValue()) {
         boolean isRightClickPressed = mc.gameSettings.keyBindUseItem.isKeyDown();
         if (isRightClickPressed && !this.wasRightClickPressed) {
            ItemStack heldItem = mc.player.getHeldItemMainhand();
            if (this.isSwappableItem(heldItem)) {
               this.performSwap();
            }
         }

         this.wasRightClickPressed = isRightClickPressed;
      }
   }

   private boolean isSwappableItem(ItemStack stack) {
      if (stack.isEmpty()) {
         return false;
      } else {
         return stack.getItem() == Items.ELYTRA || stack.getItem() == Items.DIAMOND_CHESTPLATE || stack.getItem() == Items.IRON_CHESTPLATE || stack.getItem() == Items.GOLDEN_CHESTPLATE || stack.getItem() == Items.CHAINMAIL_CHESTPLATE || stack.getItem() == Items.LEATHER_CHESTPLATE;
      }
   }

   private void performSwap() {
      if (mc.player != null) {
         ItemStack currentChest = mc.player.getItemStackFromSlot(EntityEquipmentSlot.CHEST);
         int swapSlot = -1;

         for(int i = 0; i < 36; ++i) {
            ItemStack stack = mc.player.inventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
               if (currentChest.getItem() == Items.ELYTRA) {
                  if (stack.getItem() == Items.DIAMOND_CHESTPLATE || stack.getItem() == Items.IRON_CHESTPLATE || stack.getItem() == Items.GOLDEN_CHESTPLATE || stack.getItem() == Items.CHAINMAIL_CHESTPLATE || stack.getItem() == Items.LEATHER_CHESTPLATE) {
                     swapSlot = i;
                     break;
                  }
               } else if (stack.getItem() == Items.ELYTRA) {
                  swapSlot = i;
                  break;
               }
            }
         }

         if (swapSlot != -1) {
            int windowSlot = swapSlot < 9 ? swapSlot + 36 : swapSlot;
            mc.playerController.windowClick(0, windowSlot, 0, ClickType.PICKUP, mc.player);
            mc.playerController.windowClick(0, 6, 0, ClickType.PICKUP, mc.player);
            mc.playerController.windowClick(0, windowSlot, 0, ClickType.PICKUP, mc.player);
         }
      }
   }
}
