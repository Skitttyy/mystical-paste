package fat.vonware.util;

import fat.vonware.Vonware;
import fat.vonware.ducks.IPlayerControllerMP;
import java.util.function.Predicate;
import net.minecraft.block.Block;
import net.minecraft.block.BlockObsidian;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.ClickType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemAir;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.CPacketClickWindow;
import net.minecraft.network.play.client.CPacketHeldItemChange;
import net.minecraft.network.play.client.CPacketPlayer;

public class InventoryUtil implements Util {
   public static boolean hasFullArmor() {
      for(int i = 0; i < 4; ++i) {
         if (((ItemStack)mc.player.inventory.armorInventory.get(i)).isEmpty()) {
            return false;
         }
      }

      return true;
   }

   public static void switchTo(int slot) {
      if (mc.player.inventory.currentItem != slot && slot > -1 && slot < 9) {
         mc.player.inventory.currentItem = slot;
         syncItem();
      }

   }

   public static void syncItem() {
      ((IPlayerControllerMP)mc.playerController).syncItem();
   }

   public static void switchToAlt(int slot) {
      slot = hotbarToInventory(slot);
      if (mc.player.inventory.currentItem != slot && slot > 35 && slot < 45) {
         mc.playerController.windowClick(0, slot, mc.player.inventory.currentItem, ClickType.SWAP, mc.player);
      }

   }

   public static void PacketRotation(float[] angles) {
      mc.player.connection.sendPacket(new CPacketPlayer.Rotation(angles[0], angles[1], mc.player.onGround));
   }

   public static int getBlockFromHotbar(Block block) {
      int slot = -1;

      for(int i = 8; i >= 0; --i) {
         if (mc.player.inventory.getStackInSlot(i).getItem() == Item.getItemFromBlock(block)) {
            slot = i;
         }
      }

      return slot;
   }

   public static boolean isHolding(Class<?> clazz) {
      return clazz.isAssignableFrom(mc.player.getHeldItemMainhand().getItem().getClass()) || clazz.isAssignableFrom(mc.player.getHeldItemOffhand().getItem().getClass());
   }

   public static int getItemSlot(Class<?> clss) {
      int itemSlot = -1;

      for(int i = 45; i > 0; --i) {
         if (mc.player.inventory.getStackInSlot(i).getItem().getClass() == clss) {
            itemSlot = i;
            break;
         }
      }

      return itemSlot;
   }

   public static void SilentSwitch(int slot) {
      if (slot >= 0 && slot < 9 && mc.player.inventory.currentItem != slot) {
         mc.player.connection.sendPacket(new CPacketHeldItemChange(slot));
         mc.player.inventory.currentItem = slot;
      }

   }

   public static int findHotbarItem(Item item) {
      return findInHotbar((s) -> areSame(s, item));
   }

   public static boolean areSame(ItemStack stack, Item item) {
      return stack != null && areSame(stack.getItem(), item);
   }

   public static boolean areSame(Item item1, Item item2) {
      return Item.getIdFromItem(item1) == Item.getIdFromItem(item2);
   }

   public static int findInHotbar(Predicate<ItemStack> condition) {
      return findInHotbar(condition, true);
   }

   public static int findInHotbar(Predicate<ItemStack> condition, boolean offhand) {
      if (offhand && condition.test(mc.player.getHeldItemOffhand())) {
         return -2;
      } else {
         int result = -1;

         for(int i = 8; i > -1; --i) {
            if (condition.test(mc.player.inventory.getStackInSlot(i))) {
               result = i;
               if (mc.player.inventory.currentItem == i) {
                  break;
               }
            }
         }

         return result;
      }
   }

   public static void recoverSlot(int originalSlot) {
      if (originalSlot >= 0 && originalSlot < 9 && mc.player.inventory.currentItem != originalSlot) {
         mc.player.connection.sendPacket(new CPacketHeldItemChange(originalSlot));
         mc.player.inventory.currentItem = originalSlot;
      }

   }

   public static void switchToHotbarSlot(int slot, boolean silent) {
      if (mc.player.inventory.currentItem != slot && slot >= 0) {
         if (silent) {
            mc.player.connection.sendPacket(new CPacketHeldItemChange(slot));
            mc.playerController.updateController();
         } else {
            mc.player.connection.sendPacket(new CPacketHeldItemChange(slot));
            mc.player.inventory.currentItem = slot;
            mc.playerController.updateController();
         }
      }

   }

   public static int hotbarToInventory(int slot) {
      if (slot == -2) {
         return 45;
      } else {
         return slot > -1 && slot < 9 ? 36 + slot : slot;
      }
   }

   public static void switchToBypass(int slot, boolean resync) {
      if (slot != -1) {
         if (resync) {
            resyncClick(0, slot, mc.player.inventory.currentItem, ClickType.SWAP, mc.player);
         } else {
            mc.playerController.windowClick(0, slot, mc.player.inventory.currentItem, ClickType.SWAP, mc.player);
         }
      }

   }

   public static void click(int slot) {
      mc.playerController.windowClick(0, slot, 0, ClickType.PICKUP, mc.player);
   }

   public static void clickLocked(int slot, int to, Item inSlot, Item inTo) {
      Locks.acquire(Locks.WINDOW_CLICK_LOCK, () -> {
         if ((slot == -1 || get(slot).getItem() == inSlot) && get(to).getItem() == inTo) {
            boolean multi = slot >= 0;
            if (multi) {
               Vonware.ncpManager.startMultiClick();
            }

            try {
               if (multi) {
                  click(slot);
               }

               click(to);
            } finally {
               if (multi) {
                  Vonware.ncpManager.releaseMultiClick();
               }

            }
         }

      });
   }

   public static ItemStack get(int slot) {
      return slot == -2 ? mc.player.inventory.getItemStack() : (ItemStack)mc.player.inventoryContainer.getInventory().get(slot);
   }

   public static void resyncClick(int windowId, int slotId, int mouseButton, ClickType type, EntityPlayer player) {
      short short1 = player.openContainer.getNextTransactionID(player.inventory);
      player.openContainer.slotClick(slotId, mouseButton, type, player);
      mc.player.connection.sendPacket(new CPacketClickWindow(windowId, slotId, mouseButton, type, new ItemStack(Blocks.DIAMOND_BLOCK, 64), short1));
   }

   public static void switchtoSlotWithUpdate(int slot) {
      if (slot != -1) {
         mc.player.inventory.currentItem = slot;
         mc.player.connection.sendPacket(new CPacketHeldItemChange(slot));
      }

   }

   public static int getHotbarItemSlot(Item item) {
      int slot = -1;

      for(int i = 0; i < 9; ++i) {
         if (mc.player.inventory.getStackInSlot(i).getItem().equals(item)) {
            slot = i;
            break;
         }
      }

      return slot;
   }

   public static void switchToSlotUpdate(int slot) {
      mc.player.connection.sendPacket(new CPacketHeldItemChange(slot));
      mc.player.inventory.currentItem = slot;
   }

   public static void switchToSlotUpdateIf(int slot) {
      if (slot != mc.player.inventory.currentItem) {
         mc.player.connection.sendPacket(new CPacketHeldItemChange(slot));
      }

      mc.player.inventory.currentItem = slot;
   }

   public static void switchToSlotGhost(int slot) {
      if (slot != -1) {
         mc.player.connection.sendPacket(new CPacketHeldItemChange(slot));
      }

   }

   public static int findObsidianSlot() {
      int slot = -1;

      for(int i = 0; i < 9; ++i) {
         ItemStack stack = mc.player.inventory.getStackInSlot(i);
         if (!stack.isEmpty() && stack.getItem() instanceof ItemBlock) {
            ItemBlock itemBlock = (ItemBlock)stack.getItem();
            if (itemBlock.getBlock() instanceof BlockObsidian) {
               slot = i;
               break;
            }
         }
      }

      return slot;
   }

   public static String getArmorPieceName(ItemStack stack) {
      if (stack.getItem() != Items.DIAMOND_HELMET && stack.getItem() != Items.GOLDEN_HELMET && stack.getItem() != Items.IRON_HELMET && stack.getItem() != Items.CHAINMAIL_HELMET && stack.getItem() != Items.LEATHER_HELMET) {
         if (stack.getItem() != Items.DIAMOND_CHESTPLATE && stack.getItem() != Items.GOLDEN_CHESTPLATE && stack.getItem() != Items.IRON_CHESTPLATE && stack.getItem() != Items.CHAINMAIL_CHESTPLATE && stack.getItem() != Items.LEATHER_CHESTPLATE) {
            if (stack.getItem() != Items.DIAMOND_LEGGINGS && stack.getItem() != Items.GOLDEN_LEGGINGS && stack.getItem() != Items.IRON_LEGGINGS && stack.getItem() != Items.CHAINMAIL_LEGGINGS && stack.getItem() != Items.LEATHER_LEGGINGS) {
               return stack.getItem() != Items.DIAMOND_BOOTS && stack.getItem() != Items.GOLDEN_BOOTS && stack.getItem() != Items.IRON_BOOTS && stack.getItem() != Items.CHAINMAIL_BOOTS && stack.getItem() != Items.LEATHER_BOOTS ? null : "boots";
            } else {
               return "leggings";
            }
         } else {
            return "chest";
         }
      } else {
         return "helmet";
      }
   }

   public static boolean isNull(ItemStack stack) {
      return stack == null || stack.getItem() instanceof ItemAir;
   }

   public static int getFullBlockCount() {
      int count = 0;

      for(ItemStack stack : mc.player.inventory.mainInventory) {
         if (!isNull(stack) && stack.getItem() instanceof ItemBlock && Block.getBlockFromItem(stack.getItem()).getDefaultState().isFullBlock()) {
            count += stack.stackSize;
         }
      }

      return count;
   }

   public static int getItemFromHotbar(Item item) {
      int slot = -1;

      for(int i = 8; i >= 0; --i) {
         if (mc.player.inventory.getStackInSlot(i).getItem() == item) {
            slot = i;
         }
      }

      return slot;
   }

   public static void doSwap(int slot) {
      mc.player.inventory.currentItem = slot;
      mc.playerController.updateController();
   }

   public static int findHotbarClass(Class clazz) {
      for(int i = 0; i < 9; ++i) {
         ItemStack stack = mc.player.inventory.getStackInSlot(i);
         if (stack != ItemStack.EMPTY) {
            if (clazz.isInstance(stack.getItem())) {
               return i;
            }

            if (stack.getItem() instanceof ItemBlock && clazz.isInstance(((ItemBlock)stack.getItem()).getBlock())) {
               return i;
            }
         }
      }

      return -1;
   }

   public static int getItemCount(Item item) {
      return getItemCount(item, false);
   }

   public static int getItemCount(Item item, boolean countDrag) {
      int count = 0;
      int size = mc.player.inventory.mainInventory.size();

      for(int i = 0; i < size; ++i) {
         ItemStack itemStack = (ItemStack)mc.player.inventory.mainInventory.get(i);
         if (itemStack.getItem() == item) {
            count += itemStack.getCount();
         }
      }

      ItemStack offhandStack = mc.player.getHeldItemOffhand();
      if (offhandStack.getItem() == item) {
         count += offhandStack.getCount();
      }

      if (countDrag) {
         ItemStack drag = mc.player.inventory.getItemStack();
         Item dragItem = drag.getItem();
         if (dragItem == item) {
            count += drag.getCount();
         }
      }

      return count;
   }

   public static int findHotbarBlock(Class clazz) {
      for(int i = 0; i < 9; ++i) {
         ItemStack stack = mc.player.inventory.getStackInSlot(i);
         if (stack != ItemStack.EMPTY) {
            if (clazz.isInstance(stack.getItem())) {
               return i;
            }

            if (stack.getItem() instanceof ItemBlock && clazz.isInstance(((ItemBlock)stack.getItem()).getBlock())) {
               return i;
            }
         }
      }

      return -1;
   }

   public static int findHotbarBlock(Block blockIn) {
      for(int i = 0; i < 9; ++i) {
         ItemStack stack = mc.player.inventory.getStackInSlot(i);
         if (stack != ItemStack.EMPTY && stack.getItem() instanceof ItemBlock && ((ItemBlock)stack.getItem()).getBlock() == blockIn) {
            return i;
         }
      }

      return -1;
   }
}
