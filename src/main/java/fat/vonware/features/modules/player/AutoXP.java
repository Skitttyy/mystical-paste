package fat.vonware.features.modules.player;

import fat.vonware.features.modules.Module;
import fat.vonware.features.setting.Setting;
import fat.vonware.util.InventoryUtil;
import fat.vonware.util.Timer;
import java.util.function.Function;
import net.minecraft.init.Items;
import net.minecraft.inventory.ClickType;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;

public class AutoXP extends Module {
   private final Setting<SwapLogic> swapLogic;
   private final Setting<Boolean> swapBack;
   private final Setting<Boolean> silentInventory;
   private final Setting<Integer> swapBackSlot;
   private int previousSlot;
   private int silentInvXpContainerSlot;
   private final Timer swapTimer;
   private boolean justSwapped;

   public AutoXP() {
      super("AutoXP", "Automatically throws XP bottles", Module.Category.PLAYER, true, false, false);
      this.swapLogic = (new Setting("Swap", AutoXP.SwapLogic.Normal)).withDescription((Function<SwapLogic, String>)((mode) -> {
         switch (mode) {
            case Normal:
               return "Normal: Visibly switches to an XP bottle hotbar slot and optionally switches back when AutoXP disables.";
            case SilentSwap:
               return "SilentSwap: Temporarily switches to the XP slot for each throw and immediately recovers the previous slot.";
            default:
               return "";
         }
      }));
      this.swapBack = new Setting<Boolean>("Swap Back", true, (v) -> this.swapLogic.getValue() == AutoXP.SwapLogic.Normal, "Restores the slot you were holding before AutoXP enabled.");
      this.silentInventory = new Setting<Boolean>("Silent Inventory", false, (v) -> this.swapLogic.getValue() == AutoXP.SwapLogic.SilentSwap, "Allows SilentSwap to pull XP bottles from the full inventory into the configured hotbar slot.");
      this.swapBackSlot = new Setting<Integer>("Swap Back Slot", 1, 1, 9, (v) -> this.swapLogic.getValue() == AutoXP.SwapLogic.SilentSwap && (Boolean)this.silentInventory.getValue(), "Hotbar slot temporarily used for inventory XP bottles while Silent Inventory is enabled.");
      this.previousSlot = -1;
      this.silentInvXpContainerSlot = -1;
      this.swapTimer = new Timer();
      this.justSwapped = false;
      this.register(this.swapLogic);
      this.register(this.swapBack);
      this.register(this.silentInventory);
      this.register(this.swapBackSlot);
   }

   public void onEnable() {
      if (!fullNullCheck()) {
         this.previousSlot = mc.player.inventory.currentItem;
         if (this.swapLogic.getValue() == AutoXP.SwapLogic.Normal) {
            int xpSlot = InventoryUtil.getHotbarItemSlot(Items.EXPERIENCE_BOTTLE);
            if (xpSlot == -1) {
               return;
            }

            InventoryUtil.switchToSlotUpdate(xpSlot);
            this.swapTimer.reset();
            this.justSwapped = true;
         }

         if (this.swapLogic.getValue() == AutoXP.SwapLogic.SilentSwap && (Boolean)this.silentInventory.getValue()) {
            int xpContainerSlot = this.findXpInFullInventory();
            if (xpContainerSlot == -1) {
               return;
            }

            this.silentInvXpContainerSlot = xpContainerSlot;
            int targetHotbarSlot = (Integer)this.swapBackSlot.getValue() - 1;
            InventoryUtil.resyncClick(0, xpContainerSlot, targetHotbarSlot, ClickType.SWAP, mc.player);
         }

      }
   }

   public void onDisable() {
      if (mc.player != null) {
         if (this.swapLogic.getValue() == AutoXP.SwapLogic.Normal && (Boolean)this.swapBack.getValue() && this.previousSlot != -1) {
            InventoryUtil.switchToSlotUpdate(this.previousSlot);
         }

         if (this.swapLogic.getValue() == AutoXP.SwapLogic.SilentSwap && (Boolean)this.silentInventory.getValue() && this.silentInvXpContainerSlot != -1) {
            int targetHotbarSlot = (Integer)this.swapBackSlot.getValue() - 1;
            InventoryUtil.resyncClick(0, this.silentInvXpContainerSlot, targetHotbarSlot, ClickType.SWAP, mc.player);
            InventoryUtil.recoverSlot(this.previousSlot);
            InventoryUtil.syncItem();
            this.silentInvXpContainerSlot = -1;
         }

         this.previousSlot = -1;
         this.justSwapped = false;
      }
   }

   public void onUpdate() {
      if (!fullNullCheck()) {
         if (this.swapLogic.getValue() == AutoXP.SwapLogic.SilentSwap) {
            int xpSlot = (Boolean)this.silentInventory.getValue() ? (Integer)this.swapBackSlot.getValue() - 1 : InventoryUtil.getHotbarItemSlot(Items.EXPERIENCE_BOTTLE);
            if (xpSlot != -1) {
               InventoryUtil.switchTo(xpSlot);
               mc.playerController.processRightClick(mc.player, mc.world, EnumHand.MAIN_HAND);
               InventoryUtil.recoverSlot(this.previousSlot);
            }
         } else {
            if (mc.player.getHeldItemMainhand().getItem() != Items.EXPERIENCE_BOTTLE) {
               int xpSlot = InventoryUtil.getHotbarItemSlot(Items.EXPERIENCE_BOTTLE);
               if (xpSlot == -1) {
                  return;
               }

               InventoryUtil.switchToSlotUpdate(xpSlot);
               this.swapTimer.reset();
               this.justSwapped = true;
            }

            if (this.justSwapped && this.swapTimer.passedMs(50L)) {
               mc.playerController.processRightClick(mc.player, mc.world, EnumHand.MAIN_HAND);
               this.justSwapped = false;
            } else if (!this.justSwapped) {
               mc.playerController.processRightClick(mc.player, mc.world, EnumHand.MAIN_HAND);
            }

         }
      }
   }

   private int findXpInFullInventory() {
      for(int i = 9; i <= 44; ++i) {
         ItemStack stack = mc.player.inventoryContainer.getSlot(i).getStack();
         if (!stack.isEmpty() && stack.getItem() == Items.EXPERIENCE_BOTTLE) {
            return i;
         }
      }

      return -1;
   }

   public static enum SwapLogic {
      Normal,
      SilentSwap;
   }
}
