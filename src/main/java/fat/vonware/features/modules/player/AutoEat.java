package fat.vonware.features.modules.player;

import fat.vonware.features.modules.Module;
import fat.vonware.features.setting.Setting;
import java.util.function.Function;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.init.Items;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.CPacketHeldItemChange;
import net.minecraft.util.EnumHand;

public class AutoEat extends Module {
   private final Setting<Mode> mode;
   private final Setting<Integer> health;
   private final Setting<Integer> hunger;
   private final Setting<Boolean> autoSwitch;
   private final Setting<Integer> switchDelay;
   private final Setting<Boolean> preferGaps;
   private final Setting<Boolean> pauseInGUI;
   private int originalSlot;
   private int eatDelay;
   private int switchDelayTimer;
   private int targetFoodSlot;
   private int eatHoldTicks;
   private boolean isEating;
   private boolean resetKeyBind;
   private boolean useStarted;
   private boolean wasHandActive;
   private int restartDelay;

   public AutoEat() {
      super("AutoEat", "Automatically eats for you", Module.Category.PLAYER, true, false, false);
      this.mode = (new Setting("Mode", AutoEat.Mode.Both)).withDescription((Function<Mode, String>)((mode) -> {
         switch (mode) {
            case Health:
               return "Health: Starts eating when your health reaches the configured Health threshold.";
            case Hunger:
               return "Hunger: Starts eating when your hunger reaches the configured Hunger threshold.";
            case Both:
               return "Both: Starts eating when either the Health or Hunger condition is met.";
            default:
               return "";
         }
      }));
      this.health = new Setting<Integer>("Health", 10, 0, 36, "Health threshold used by Health and Both modes.");
      this.hunger = new Setting<Integer>("Hunger", 15, 0, 20, "Hunger threshold used by Hunger and Both modes.");
      this.autoSwitch = new Setting<Boolean>("AutoSwitch", true, "Automatically switches to a valid food item when AutoEat needs to eat.");
      this.switchDelay = new Setting<Integer>("SwitchDelay", 0, 0, 20, "Delay in ticks after switching food slots before AutoEat begins using the item.");
      this.preferGaps = new Setting<Boolean>("PreferGaps", false, "Prefers golden apples over other valid food when selecting what to eat.");
      this.pauseInGUI = new Setting<Boolean>("PauseInGUI", false, "Pauses AutoEat while a GUI screen is open.");
      this.originalSlot = -1;
      this.eatDelay = 0;
      this.switchDelayTimer = 0;
      this.targetFoodSlot = -1;
      this.eatHoldTicks = 0;
      this.isEating = false;
      this.resetKeyBind = false;
      this.useStarted = false;
      this.wasHandActive = false;
      this.restartDelay = 0;
      this.register(this.mode);
      this.register(this.health);
      this.register(this.hunger);
      this.register(this.autoSwitch);
      this.register(this.switchDelay);
      this.register(this.preferGaps);
      this.register(this.pauseInGUI);
   }

   public void onUpdate() {
      if (mc.player != null && mc.world != null && !mc.player.isCreative()) {
         if ((Boolean)this.pauseInGUI.getValue() && mc.currentScreen != null && !(mc.currentScreen instanceof GuiInventory)) {
            this.stopEating();
         } else if (this.eatDelay > 0) {
            --this.eatDelay;
         } else {
            if (this.shouldEat()) {
               this.handleEating();
            } else {
               this.stopEating();
            }

         }
      }
   }

   private boolean shouldEat() {
      float currentHealth = mc.player.getHealth() + mc.player.getAbsorptionAmount();
      int currentHunger = mc.player.getFoodStats().getFoodLevel();
      switch ((Mode)this.mode.getValue()) {
         case Health:
            return currentHealth <= (float)(Integer)this.health.getValue();
         case Hunger:
            return currentHunger < 20 && currentHunger <= (Integer)this.hunger.getValue();
         case Both:
            return currentHealth <= (float)(Integer)this.health.getValue() || currentHunger < 20 && currentHunger <= (Integer)this.hunger.getValue();
         default:
            return false;
      }
   }

   private void handleEating() {
      if ((Boolean)this.autoSwitch.getValue()) {
         int foodSlot = this.findFoodSlot();
         if (foodSlot == -1) {
            this.stopEating();
            return;
         }

         if (mc.player.inventory.currentItem != foodSlot) {
            if (this.originalSlot == -1) {
               this.originalSlot = mc.player.inventory.currentItem;
            }

            if ((Integer)this.switchDelay.getValue() > 0) {
               if (this.targetFoodSlot != foodSlot) {
                  this.targetFoodSlot = foodSlot;
                  this.switchDelayTimer = (Integer)this.switchDelay.getValue();
               }

               if (this.switchDelayTimer > 0) {
                  --this.switchDelayTimer;
                  return;
               }
            }

            mc.player.inventory.currentItem = foodSlot;
            mc.player.connection.sendPacket(new CPacketHeldItemChange(foodSlot));
            this.targetFoodSlot = -1;
            this.useStarted = false;
         }
      }

      ItemStack stack = mc.player.getHeldItemMainhand();
      if (stack.getItem() instanceof ItemFood) {
         ItemFood food = (ItemFood)stack.getItem();
         if (!this.canEatFood(food, stack)) {
            this.stopEating();
         } else {
            ++this.eatHoldTicks;
            this.isEating = true;
            boolean handActive = mc.player.isHandActive();
            if (handActive) {
               this.wasHandActive = true;
               this.useStarted = true;
               this.restartDelay = 0;
            } else if (this.wasHandActive) {
               this.wasHandActive = false;
               this.useStarted = false;
               this.restartDelay = 1;
               if (this.resetKeyBind) {
                  KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
                  this.resetKeyBind = false;
               }

            } else if (this.restartDelay > 0) {
               --this.restartDelay;
            } else {
               if (mc.currentScreen != null && !(mc.currentScreen instanceof GuiInventory)) {
                  if (!this.useStarted) {
                     mc.playerController.processRightClick(mc.player, mc.world, EnumHand.MAIN_HAND);
                     this.useStarted = true;
                  }
               } else {
                  if (!this.resetKeyBind) {
                     KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), true);
                     this.resetKeyBind = true;
                  }

                  this.useStarted = true;
               }

            }
         }
      }
   }

   private void stopEating() {
      if (this.resetKeyBind) {
         KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), GameSettings.isKeyDown(mc.gameSettings.keyBindUseItem));
         this.resetKeyBind = false;
      }

      if (this.originalSlot != -1 && (Boolean)this.autoSwitch.getValue() && this.eatHoldTicks > 2) {
         if (mc.player.inventory.currentItem != this.originalSlot) {
            mc.player.inventory.currentItem = this.originalSlot;
            mc.player.connection.sendPacket(new CPacketHeldItemChange(this.originalSlot));
         }

         this.originalSlot = -1;
         if (this.isEating) {
            this.eatDelay = 10;
         }
      }

      this.switchDelayTimer = 0;
      this.targetFoodSlot = -1;
      this.eatHoldTicks = 0;
      this.isEating = false;
      this.useStarted = false;
      this.wasHandActive = false;
      this.restartDelay = 0;
   }

   private boolean canEatFood(ItemFood food, ItemStack stack) {
      if (stack.getItem() == Items.GOLDEN_APPLE) {
         return true;
      } else if (stack.getItem() == Items.CHORUS_FRUIT) {
         return true;
      } else {
         return mc.player.getFoodStats().getFoodLevel() < 20 || food.isWolfsFavoriteMeat();
      }
   }

   private int findFoodSlot() {
      int foodSlot = -1;
      int bestHealAmount = 0;

      for(int slot = 0; slot < 9; ++slot) {
         ItemStack stack = mc.player.inventory.getStackInSlot(slot);
         if (!stack.isEmpty() && stack.getItem() instanceof ItemFood) {
            ItemFood food = (ItemFood)stack.getItem();
            if (this.canEatFood(food, stack)) {
               if ((Boolean)this.preferGaps.getValue() && stack.getItem() == Items.GOLDEN_APPLE) {
                  return slot;
               }

               int healAmount = food.getHealAmount(stack);
               if (healAmount > bestHealAmount) {
                  bestHealAmount = healAmount;
                  foodSlot = slot;
               }
            }
         }
      }

      return foodSlot;
   }

   public void onDisable() {
      super.onDisable();
      this.stopEating();
      this.eatDelay = 0;
      this.switchDelayTimer = 0;
      this.targetFoodSlot = -1;
      this.eatHoldTicks = 0;
      this.useStarted = false;
      this.wasHandActive = false;
      this.restartDelay = 0;
   }

   public static enum Mode {
      Hunger,
      Health,
      Both;
   }
}
