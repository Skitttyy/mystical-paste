package fat.vonware.features.modules.combat;

import fat.vonware.Vonware;
import fat.vonware.features.Feature;
import fat.vonware.features.modules.Module;
import fat.vonware.features.modules.client.Manager;
import fat.vonware.features.setting.Setting;
import fat.vonware.util.EntityUtil;
import fat.vonware.util.Locks;
import fat.vonware.util.Timer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.init.Enchantments;
import net.minecraft.init.Items;
import net.minecraft.inventory.ClickType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;

public class AutoArmor extends Module {
   private static final int[] NETHERITE_MAX_DAMAGE = new int[]{481, 555, 592, 407};
   public Setting<Boolean> protPantsPrio = new Setting<Boolean>("Prot Prio", false, "Prioritizes Protection leggings when choosing which leggings to equip.");
   public Setting<Float> delay = new Setting<Float>("Delay", 1.0F, 0.0F, 5.0F, "Delay between armor inventory actions.");
   public Setting<Boolean> armorSaver = new Setting<Boolean>("Armor Saver", true, "Swaps your armor piece when it reaches the threshold with a new armor piece saving it from being broken.");
   public Setting<Integer> saverThreshold = new Setting<Integer>("Saver Threshold", 20, 1, 100, (v) -> (Boolean)this.armorSaver.getValue(), "Durability percentage at which Armor Saver swaps the armor piece.");
   public Setting<Boolean> autoMend = new Setting<Boolean>("Auto Mend", false, "Automatically takes off your armor while you are throwing down XP bottles.");
   public Setting<Integer> mendSafePercent = new Setting<Integer>("Mend Safe %", 90, 0, 100, (v) -> (Boolean)this.autoMend.getValue(), "Armor durability percentage needed to allow AutoMend to start its process");
   public Setting<MendMode> mendMode;
   public Setting<Float> mendDelay;
   public Setting<Boolean> mendSafety;
   public Setting<Float> mendSafetyRange;
   Timer delayTimer;
   Timer armorSaverTimer;
   private boolean isSwapping;
   private int swapStep;
   private int swapReplacementSlot;
   private int swapEquipSlot;
   private final Timer mendTimer;
   private boolean mendActive;
   private int mendActiveIndex;
   private MendMode mendCycleMode;
   private final List<Integer> mendPendingIndices;
   private int xcStep;
   private int xcSourceSlot;
   private int xcTargetSlot;

   public AutoArmor() {
      super("AutoArmor", "Automatically equips armor pieces", Module.Category.COMBAT, true, false, false);
      this.mendMode = new Setting<MendMode>("Mend Mode", AutoArmor.MendMode.Strict, (v) -> (Boolean)this.autoMend.getValue(), "Controls how Auto Mend equips and removes armor during mending.");
      this.mendDelay = new Setting<Float>("Mend Delay", 0.25F, 0.0F, 2.0F, (v) -> (Boolean)this.autoMend.getValue(), "Delay between Auto Mend inventory actions.");
      this.mendSafety = new Setting<Boolean>("Safety", true, (v) -> (Boolean)this.autoMend.getValue(), "Pauses Auto Mend when a nearby player makes mending unsafe.");
      this.mendSafetyRange = new Setting<Float>("Target Range", 16.0F, 0.0F, 64.0F, (v) -> (Boolean)this.autoMend.getValue() && (Boolean)this.mendSafety.getValue(), "Maximum player distance used by the Auto Mend safety check.");
      this.isSwapping = false;
      this.swapStep = 0;
      this.swapReplacementSlot = -1;
      this.swapEquipSlot = -1;
      this.mendTimer = new Timer();
      this.mendActive = false;
      this.mendActiveIndex = -1;
      this.mendCycleMode = null;
      this.mendPendingIndices = new ArrayList();
      this.xcStep = 0;
      this.xcSourceSlot = -1;
      this.xcTargetSlot = -1;
      this.register(this.protPantsPrio);
      this.register(this.delay);
      this.register(this.armorSaver);
      this.register(this.saverThreshold);
      this.register(this.autoMend);
      this.register(this.mendSafePercent);
      this.register(this.mendMode);
      this.register(this.mendDelay);
      this.register(this.mendSafety);
      this.register(this.mendSafetyRange);
      this.delayTimer = new Timer();
      this.armorSaverTimer = new Timer();
   }

   public String getDisplayInfo() {
      if (this.mendActive) {
         return "Active";
      } else {
         return (Boolean)this.armorSaver.getValue() ? "Save" : null;
      }
   }

   public void onUpdate() {
      if (!Feature.fullNullCheck()) {
         if (this.isSwapping) {
            this.handleDragSwap();
         } else if (this.xcStep != 0) {
            this.handleXCarryDrag();
         } else if (mc.player.inventory.getItemStack().isEmpty()) {
            if ((Boolean)this.autoMend.getValue()) {
               boolean targetNearby = this.isTargetNearby();
               boolean xcarryUnsafe = this.mendActive && this.mendCycleMode == AutoArmor.MendMode.XcarryTest && !this.isXCarryModuleActive();
               boolean forcedRestore = this.mendActive && (xcarryUnsafe || targetNearby);
               if (forcedRestore) {
                  if (this.mendCycleMode == AutoArmor.MendMode.Strict) {
                     this.restoreMendedArmorStrict();
                  } else {
                     this.restoreMendedArmorXCarry();
                  }

                  return;
               }

               if (this.mendActive || !targetNearby && this.isThrowingXp()) {
                  this.handleMendCycle();
                  return;
               }
            }

            if ((Boolean)this.armorSaver.getValue() && !this.isSwapping && this.armorSaverTimer.isPassed(50L)) {
               for(int i = 3; i >= 0; --i) {
                  ItemStack currentArmor = (ItemStack)mc.player.inventory.armorInventory.get(i);
                  if (!currentArmor.isEmpty() && currentArmor.isItemStackDamageable()) {
                     int durabilityPercent = this.getDurabilityPercent(currentArmor, this.getArmorTypeFromSlot(i));
                     if (durabilityPercent <= (Integer)this.saverThreshold.getValue()) {
                        this.swapDamagedArmor(i);
                        return;
                     }
                  }
               }
            }

            if (this.delayTimer.isPassed((long)((Float)this.delay.getValue() * 1000.0F))) {
               for(int i = 3; i >= 0; --i) {
                  if (((ItemStack)mc.player.inventory.armorInventory.get(i)).isEmpty()) {
                     this.equipArmor(i);
                     return;
                  }
               }

            }
         }
      }
   }

   private void handleMendCycle() {
      if (!this.mendActive) {
         this.startMendCycle();
      } else if (this.mendTimer.isPassed((long)((Float)this.mendDelay.getValue() * 1000.0F))) {
         if (this.mendActiveIndex == -1) {
            if (!this.isThrowingXp()) {
               if (this.mendCycleMode == AutoArmor.MendMode.Strict) {
                  this.restoreMendedArmorStrict();
               } else {
                  this.restoreMendedArmorXCarry();
               }

            } else {
               if (this.mendCycleMode == AutoArmor.MendMode.Strict) {
                  this.advanceMendTargetStrict();
               } else {
                  this.advanceMendTargetXCarry();
               }

            }
         } else {
            ItemStack worn = (ItemStack)mc.player.inventory.armorInventory.get(this.mendActiveIndex);
            if (worn.isEmpty()) {
               this.mendActive = false;
               this.mendActiveIndex = -1;
               this.mendCycleMode = null;
               this.mendPendingIndices.clear();
            } else {
               boolean thresholdMet = this.getDurabilityPercent(worn) >= (Integer)this.mendSafePercent.getValue();
               boolean stoppedThrowing = !this.isThrowingXp();
               if (thresholdMet || stoppedThrowing) {
                  if (this.mendCycleMode == AutoArmor.MendMode.Strict) {
                     this.retireMendTargetStrict();
                  } else {
                     this.retireMendTargetXCarry();
                  }

               }
            }
         }
      }
   }

   private void startMendCycle() {
      if (this.mendMode.getValue() != AutoArmor.MendMode.XcarryTest || this.isXCarryModuleActive()) {
         this.mendPendingIndices.clear();

         for(int i = 0; i <= 3; ++i) {
            ItemStack piece = (ItemStack)mc.player.inventory.armorInventory.get(i);
            if (!piece.isEmpty() && this.getDurabilityPercent(piece) < (Integer)this.mendSafePercent.getValue()) {
               this.mendPendingIndices.add(i);
            }
         }

         this.mendPendingIndices.sort((a, b) -> Integer.compare(this.getDurabilityPercent((ItemStack)mc.player.inventory.armorInventory.get(a)), this.getDurabilityPercent((ItemStack)mc.player.inventory.armorInventory.get(b))));
         if (!this.mendPendingIndices.isEmpty()) {
            this.mendCycleMode = this.mendMode.getValue();
            this.mendActive = true;
            this.mendActiveIndex = -1;
            this.mendTimer.resetDelay();
         }
      }
   }

   private void advanceMendTargetStrict() {
      if (this.mendPendingIndices.isEmpty()) {
         this.restoreMendedArmorStrict();
      } else {
         int next = (Integer)this.mendPendingIndices.get(0);

         for(int i = 0; i <= 3; ++i) {
            if (i != next) {
               ItemStack piece = (ItemStack)mc.player.inventory.armorInventory.get(i);
               if (!piece.isEmpty()) {
                  if (!this.hasFreeInventorySlot()) {
                     this.mendActive = false;
                     this.mendCycleMode = null;
                     return;
                  }

                  int slot = this.getSlotByType(this.getArmorTypeFromSlot(i));
                  Locks.acquire(Locks.WINDOW_CLICK_LOCK, () -> mc.playerController.windowClick(0, slot, 0, ClickType.QUICK_MOVE, mc.player));
                  this.mendTimer.resetDelay();
                  return;
               }
            }
         }

         if (((ItemStack)mc.player.inventory.armorInventory.get(next)).isEmpty()) {
            int invSlot = this.findArmorInInventory(this.getArmorTypeFromSlot(next));
            if (invSlot != -1) {
               Locks.acquire(Locks.WINDOW_CLICK_LOCK, () -> mc.playerController.windowClick(0, invSlot, 0, ClickType.QUICK_MOVE, mc.player));
               this.mendTimer.resetDelay();
            }

         } else {
            this.mendPendingIndices.remove(next);
            this.mendActiveIndex = next;
            this.mendTimer.resetDelay();
         }
      }
   }

   private void retireMendTargetStrict() {
      int slot = this.getSlotByType(this.getArmorTypeFromSlot(this.mendActiveIndex));
      Locks.acquire(Locks.WINDOW_CLICK_LOCK, () -> mc.playerController.windowClick(0, slot, 0, ClickType.QUICK_MOVE, mc.player));
      this.mendActiveIndex = -1;
      this.mendTimer.resetDelay();
   }

   private void restoreMendedArmorStrict() {
      if (this.mendTimer.isPassed((long)((Float)this.mendDelay.getValue() * 1000.0F))) {
         for(int i = 0; i <= 3; ++i) {
            if (((ItemStack)mc.player.inventory.armorInventory.get(i)).isEmpty()) {
               int invSlot = this.findArmorInInventory(this.getArmorTypeFromSlot(i));
               if (invSlot != -1) {
                  Locks.acquire(Locks.WINDOW_CLICK_LOCK, () -> mc.playerController.windowClick(0, invSlot, 0, ClickType.QUICK_MOVE, mc.player));
                  this.mendTimer.resetDelay();
                  return;
               }
            }
         }

         this.mendActive = false;
         this.mendActiveIndex = -1;
         this.mendCycleMode = null;
         this.mendPendingIndices.clear();
      }
   }

   private void advanceMendTargetXCarry() {
      if (this.mendPendingIndices.isEmpty()) {
         this.restoreMendedArmorXCarry();
      } else {
         int next = (Integer)this.mendPendingIndices.get(0);

         for(int i = 0; i <= 3; ++i) {
            if (i != next) {
               ItemStack piece = (ItemStack)mc.player.inventory.armorInventory.get(i);
               if (!piece.isEmpty()) {
                  this.beginXCarryMove(this.getSlotByType(this.getArmorTypeFromSlot(i)), this.craftSlotForIndex(i));
                  return;
               }
            }
         }

         if (((ItemStack)mc.player.inventory.armorInventory.get(next)).isEmpty()) {
            this.beginXCarryMove(this.craftSlotForIndex(next), this.getSlotByType(this.getArmorTypeFromSlot(next)));
         } else {
            this.mendPendingIndices.remove(next);
            this.mendActiveIndex = next;
            this.mendTimer.resetDelay();
         }
      }
   }

   private void retireMendTargetXCarry() {
      int index = this.mendActiveIndex;
      this.mendActiveIndex = -1;
      this.beginXCarryMove(this.getSlotByType(this.getArmorTypeFromSlot(index)), this.craftSlotForIndex(index));
   }

   private void restoreMendedArmorXCarry() {
      if (this.mendTimer.isPassed((long)((Float)this.mendDelay.getValue() * 1000.0F))) {
         for(int i = 0; i <= 3; ++i) {
            if (((ItemStack)mc.player.inventory.armorInventory.get(i)).isEmpty()) {
               this.beginXCarryMove(this.craftSlotForIndex(i), this.getSlotByType(this.getArmorTypeFromSlot(i)));
               return;
            }
         }

         this.mendActive = false;
         this.mendActiveIndex = -1;
         this.mendCycleMode = null;
         this.mendPendingIndices.clear();
      }
   }

   private void beginXCarryMove(int sourceSlot, int targetSlot) {
      this.xcSourceSlot = sourceSlot;
      this.xcTargetSlot = targetSlot;
      this.xcStep = 1;
      this.mendTimer.resetDelay();
   }

   private void handleXCarryDrag() {
      if (this.xcStep == 1) {
         int slot = this.xcSourceSlot;
         Locks.acquire(Locks.WINDOW_CLICK_LOCK, () -> mc.playerController.windowClick(0, slot, 0, ClickType.PICKUP, mc.player));
         this.xcStep = 2;
      } else {
         if (this.xcStep == 2) {
            int slot = this.xcTargetSlot;
            Locks.acquire(Locks.WINDOW_CLICK_LOCK, () -> mc.playerController.windowClick(0, slot, 0, ClickType.PICKUP, mc.player));
            this.xcStep = 0;
            this.xcSourceSlot = -1;
            this.xcTargetSlot = -1;
         }

      }
   }

   private int craftSlotForIndex(int armorIndex) {
      return armorIndex + 1;
   }

   private boolean isXCarryModuleActive() {
      Module xcarry = Vonware.moduleManager.getModuleByName("XCarry");
      return xcarry != null && xcarry.isEnabled();
   }

   private boolean isThrowingXp() {
      ItemStack main = mc.player.getHeldItemMainhand();
      ItemStack off = mc.player.getHeldItemOffhand();
      boolean holdingBottle = main.getItem() == Items.EXPERIENCE_BOTTLE || off.getItem() == Items.EXPERIENCE_BOTTLE;
      return holdingBottle && mc.gameSettings.keyBindUseItem.isKeyDown();
   }

   private boolean isTargetNearby() {
      if (!(Boolean)this.mendSafety.getValue()) {
         return false;
      } else {
         return EntityUtil.getTarget((double)(Float)this.mendSafetyRange.getValue()) != null;
      }
   }

   private boolean hasFreeInventorySlot() {
      for(int i = 9; i <= 44; ++i) {
         if (mc.player.inventoryContainer.getSlot(i).getStack().isEmpty()) {
            return true;
         }
      }

      return false;
   }

   private int findArmorInInventory(ArmorType type) {
      if (type == null) {
         return -1;
      } else {
         for(int i = 9; i <= 44; ++i) {
            ItemStack stack = mc.player.inventoryContainer.getSlot(i).getStack();
            Item item = stack.getItem();
            if (item instanceof ItemArmor && this.getTypeFromItem(item) == type) {
               return i;
            }
         }

         return -1;
      }
   }

   private void handleDragSwap() {
      switch (this.swapStep) {
         case 1:
            int replacementSlot = this.swapReplacementSlot;
            Locks.acquire(Locks.WINDOW_CLICK_LOCK, () -> mc.playerController.windowClick(0, replacementSlot, 0, ClickType.PICKUP, mc.player));
            this.swapStep = 2;
            break;
         case 2:
            int equipSlot = this.swapEquipSlot;
            Locks.acquire(Locks.WINDOW_CLICK_LOCK, () -> mc.playerController.windowClick(0, equipSlot, 0, ClickType.PICKUP, mc.player));
            this.swapStep = 3;
            break;
         case 3:
            int returnedSlot = this.swapReplacementSlot;
            Locks.acquire(Locks.WINDOW_CLICK_LOCK, () -> mc.playerController.windowClick(0, returnedSlot, 0, ClickType.PICKUP, mc.player));
            this.swapStep = 4;
            break;
         case 4:
            this.isSwapping = false;
            this.swapStep = 0;
            int tempReplacementSlot = this.swapReplacementSlot;
            this.swapReplacementSlot = -1;
            this.swapEquipSlot = -1;
            this.armorSaverTimer.resetDelay();
            if (!mc.player.inventory.getItemStack().isEmpty()) {
               boolean placed = false;

               for(int i = 9; i <= 44; ++i) {
                  if (i != tempReplacementSlot && mc.player.inventoryContainer.getSlot(i).getStack().isEmpty()) {
                     int emptySlot = i;
                     Locks.acquire(Locks.WINDOW_CLICK_LOCK, () -> mc.playerController.windowClick(0, emptySlot, 0, ClickType.PICKUP, mc.player));
                     placed = true;
                     break;
                  }
               }

               if (!placed) {
                  Locks.acquire(Locks.WINDOW_CLICK_LOCK, () -> mc.playerController.windowClick(0, -999, 0, ClickType.PICKUP, mc.player));
               }
            }
      }

   }

   private int getDurabilityPercent(ItemStack stack) {
      if (!stack.isItemStackDamageable()) {
         return 100;
      } else {
         int maxDamage = stack.getMaxDamage();
         int currentDamage = stack.getItemDamage();
         return (int)((double)(maxDamage - currentDamage) / (double)maxDamage * (double)100.0F);
      }
   }

   private int getDurabilityPercent(ItemStack stack, ArmorType type) {
      if (!stack.isItemStackDamageable()) {
         return 100;
      } else {
         int maxDamage = (Boolean)Manager.getInstance().calcNetherite.getValue() && type != null ? NETHERITE_MAX_DAMAGE[this.getNetheriteIndex(type)] : stack.getMaxDamage();
         int currentDamage = stack.getItemDamage();
         return (int)((double)(maxDamage - currentDamage) / (double)maxDamage * (double)100.0F);
      }
   }

   private int getNetheriteIndex(ArmorType type) {
      switch (type) {
         case BOOTS:
            return 0;
         case PANTS:
            return 1;
         case CHESTPLATE:
            return 2;
         case HELMET:
            return 3;
         default:
            return -1;
      }
   }

   private void swapDamagedArmor(int armorSlot) {
      ArmorType armorType = this.getArmorTypeFromSlot(armorSlot);
      if (armorType != null) {
         int replacementSlot = this.findReplacementArmor(armorType);
         if (replacementSlot != -1) {
            this.isSwapping = true;
            this.swapStep = 1;
            this.swapReplacementSlot = replacementSlot;
            this.swapEquipSlot = this.getSlotByType(armorType);
         }
      }
   }

   private int findReplacementArmor(ArmorType armorType) {
      int bestSlot = -1;
      int bestRating = -1;
      int bestDurability = -1;

      for(int i = 9; i <= 44; ++i) {
         ItemStack stack = mc.player.inventoryContainer.getSlot(i).getStack();
         Item item = stack.getItem();
         if (item instanceof ItemArmor && this.getTypeFromItem(item) == armorType) {
            int rating = this.getArmorRating(stack, armorType);
            int durabilityPercent = this.getDurabilityPercent(stack, armorType);
            if (durabilityPercent > (Integer)this.saverThreshold.getValue() && (rating > bestRating || rating == bestRating && durabilityPercent > bestDurability)) {
               bestSlot = i;
               bestRating = rating;
               bestDurability = durabilityPercent;
            }
         }
      }

      return bestSlot;
   }

   private int getArmorRating(ItemStack stack, ArmorType armorType) {
      if (!(stack.getItem() instanceof ItemArmor)) {
         return 0;
      } else {
         int damageReduction = ((ItemArmor)stack.getItem()).damageReduceAmount;
         if (armorType == AutoArmor.ArmorType.PANTS) {
            if (EnchantmentHelper.getEnchantmentLevel(Enchantments.BLAST_PROTECTION, stack) > 0) {
               return damageReduction + 100;
            }

            if ((Boolean)this.protPantsPrio.getValue() && EnchantmentHelper.getEnchantmentLevel(Enchantments.PROTECTION, stack) > 0) {
               return damageReduction + 50;
            }
         }

         return damageReduction;
      }
   }

   private boolean needsArmor(ArmorType type) {
      switch (type) {
         case BOOTS:
            return ((ItemStack)mc.player.inventory.armorInventory.get(0)).isEmpty();
         case PANTS:
            return ((ItemStack)mc.player.inventory.armorInventory.get(1)).isEmpty();
         case CHESTPLATE:
            return ((ItemStack)mc.player.inventory.armorInventory.get(2)).isEmpty();
         case HELMET:
            return ((ItemStack)mc.player.inventory.armorInventory.get(3)).isEmpty();
         default:
            return false;
      }
   }

   private void equipArmor(int slot) {
      if (!this.isSwapping) {
         ArmorType armorType = this.getArmorTypeFromSlot(slot);
         if (armorType != null && this.needsArmor(armorType)) {
            int bestSlot = -1;
            int bestRating = -1;

            for(int i = 9; i <= 44; ++i) {
               ItemStack stack = mc.player.inventoryContainer.getSlot(i).getStack();
               Item item = stack.getItem();
               if (item instanceof ItemArmor && this.getTypeFromItem(item) == armorType && (!(Boolean)this.armorSaver.getValue() || this.getDurabilityPercent(stack, armorType) > (Integer)this.saverThreshold.getValue())) {
                  int damageReduction = ((ItemArmor)item).damageReduceAmount;
                  if (armorType == AutoArmor.ArmorType.PANTS) {
                     if (EnchantmentHelper.getEnchantmentLevel(Enchantments.BLAST_PROTECTION, stack) > 0) {
                        bestSlot = i;
                        bestRating = damageReduction;
                        continue;
                     }

                     if ((Boolean)this.protPantsPrio.getValue() && EnchantmentHelper.getEnchantmentLevel(Enchantments.PROTECTION, stack) > 0) {
                        bestSlot = i;
                        bestRating = damageReduction;
                        continue;
                     }
                  }

                  if (damageReduction >= bestRating) {
                     bestSlot = i;
                     bestRating = damageReduction;
                  }
               }
            }

            if (bestSlot != -1 && bestRating != -1) {
               int armorSlot = bestSlot;
               Locks.acquire(Locks.WINDOW_CLICK_LOCK, () -> mc.playerController.windowClick(0, armorSlot, 0, ClickType.QUICK_MOVE, mc.player));
               this.delayTimer.resetDelay();
            }

         }
      }
   }

   private ArmorType getTypeFromItem(Item item) {
      if (!Items.DIAMOND_HELMET.equals(item) && !Items.GOLDEN_HELMET.equals(item) && !Items.IRON_HELMET.equals(item) && !Items.CHAINMAIL_HELMET.equals(item) && !Items.LEATHER_HELMET.equals(item)) {
         if (!Items.DIAMOND_CHESTPLATE.equals(item) && !Items.GOLDEN_CHESTPLATE.equals(item) && !Items.IRON_CHESTPLATE.equals(item) && !Items.CHAINMAIL_CHESTPLATE.equals(item) && !Items.LEATHER_CHESTPLATE.equals(item)) {
            if (!Items.DIAMOND_LEGGINGS.equals(item) && !Items.GOLDEN_LEGGINGS.equals(item) && !Items.IRON_LEGGINGS.equals(item) && !Items.CHAINMAIL_LEGGINGS.equals(item) && !Items.LEATHER_LEGGINGS.equals(item)) {
               return !Items.DIAMOND_BOOTS.equals(item) && !Items.GOLDEN_BOOTS.equals(item) && !Items.IRON_BOOTS.equals(item) && !Items.CHAINMAIL_BOOTS.equals(item) && !Items.LEATHER_BOOTS.equals(item) ? null : AutoArmor.ArmorType.BOOTS;
            } else {
               return AutoArmor.ArmorType.PANTS;
            }
         } else {
            return AutoArmor.ArmorType.CHESTPLATE;
         }
      } else {
         return AutoArmor.ArmorType.HELMET;
      }
   }

   private ArmorType getArmorTypeFromSlot(int slot) {
      switch (slot) {
         case 0:
            return AutoArmor.ArmorType.BOOTS;
         case 1:
            return AutoArmor.ArmorType.PANTS;
         case 2:
            return AutoArmor.ArmorType.CHESTPLATE;
         case 3:
            return AutoArmor.ArmorType.HELMET;
         default:
            return null;
      }
   }

   private int getSlotByType(ArmorType type) {
      switch (type) {
         case BOOTS:
            return 8;
         case PANTS:
            return 7;
         case CHESTPLATE:
            return 6;
         case HELMET:
            return 5;
         default:
            return -1;
      }
   }

   public static enum MendMode {
      Strict,
      XcarryTest;
   }

   private static enum ArmorType {
      HELMET,
      CHESTPLATE,
      PANTS,
      BOOTS;
   }
}
