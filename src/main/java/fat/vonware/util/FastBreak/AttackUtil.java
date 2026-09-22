package fat.vonware.util.FastBreak;

import fat.vonware.util.DamageUtil;
import fat.vonware.util.InventoryUtil;
import fat.vonware.util.Util;
import fat.vonware.util.Phase.PacketUtil;
import net.minecraft.entity.Entity;
import net.minecraft.init.Items;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.CPacketClickWindow;
import net.minecraft.network.play.client.CPacketUseEntity;

public class AttackUtil implements Util {
   public static void attackEntity(Entity entity, boolean strict, boolean back) {
      int weaknessSlot = -1;
      if (DamageUtil.canBreakWeakness(true) || (weaknessSlot = DamageUtil.findAntiWeakness()) != -1) {
         if (entity != null) {
            CPacketUseEntity packet = PacketUtil.attackPacket(entity);
            attack(packet, weaknessSlot, strict, back);
         }

      }
   }

   public static void attack(Packet<?> attacking, int slot, boolean strict, boolean back) {
      int oldSlot = mc.player.inventory.currentItem;
      ItemStack oldItem = mc.player.getHeldItemMainhand();
      if (slot != -1) {
         if (strict) {
            InventoryUtil.switchToAlt(slot);
         } else {
            InventoryUtil.switchTo(slot);
         }
      }

      ItemStack newItem = mc.player.getHeldItemMainhand();
      PacketUtil.send(attacking);
      PacketUtil.swing();
      if (slot != -1 && back) {
         if (strict) {
            short id = mc.player.openContainer.getNextTransactionID(mc.player.inventory);
            ItemStack fakeStack = new ItemStack(Items.END_CRYSTAL, 64);
            int slotAlt = InventoryUtil.hotbarToInventory(slot);
            int altSlot = InventoryUtil.hotbarToInventory(oldSlot);
            Slot currentSlot = (Slot)mc.player.inventoryContainer.inventorySlots.get(altSlot);
            Slot swapSlot = (Slot)mc.player.inventoryContainer.inventorySlots.get(slotAlt);
            PacketUtil.send(new CPacketClickWindow(0, slotAlt, mc.player.inventory.currentItem, ClickType.SWAP, fakeStack, id));
            currentSlot.putStack(oldItem);
            swapSlot.putStack(newItem);
         } else {
            InventoryUtil.switchTo(oldSlot);
         }
      }

   }
}
