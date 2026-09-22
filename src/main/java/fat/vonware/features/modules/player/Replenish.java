package fat.vonware.features.modules.player;

import fat.vonware.features.Feature;
import fat.vonware.features.modules.Module;
import fat.vonware.features.setting.Setting;
import fat.vonware.util.Locks;
import net.minecraft.inventory.ClickType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class Replenish extends Module {
   Item[] cache = new Item[9];
   Item offhandCache = null;
   private final Setting<Integer> threshold = new Setting<Integer>("Threshold", 10, 1, 30, "Refills a hotbar stack when its item count reaches or falls below this amount.");
   private final Setting<Float> delay = new Setting<Float>("Delay", 2.0F, 0.0F, 10.0F, "Delay between Replenish inventory actions.");
   private final Setting<Boolean> offhand = new Setting<Boolean>("Offhand", true, "Allows Replenish to refill supported stacks in the offhand.");
   private final Setting<Boolean> replenishIllegals = new Setting<Boolean>("IllegalStacks", false, "Allows Replenish to combine or refill stacks beyond normal vanilla stack sizes when available. (used for servers like 5b5t)");
   private final Setting<Boolean> idle = new Setting<Boolean>("Idle", false, "Only performs replenishment while the configured idle conditions are met.");
   private float tickDelay = 0.0F;

   public Replenish() {
      super("Replenish", "Replenishes your HotBar", Module.Category.PLAYER, true, false, false);
      this.register(this.threshold);
      this.register(this.delay);
      this.register(this.offhand);
      this.register(this.replenishIllegals);
      this.register(this.idle);
   }

   @SubscribeEvent
   public void onUpdate(TickEvent.ClientTickEvent event) {
      if (!Feature.fullNullCheck()) {
         if ((Boolean)this.idle.getValue() || mc.currentScreen == null) {
            if (mc.player.isDead) {
               this.cache = new Item[9];
               this.offhandCache = null;
            } else if (this.tickDelay > 0.0F) {
               this.tickDelay -= 0.05F;
            } else {
               for(int i = 0; i < 9; ++i) {
                  ItemStack stack = mc.player.inventory.getStackInSlot(i);
                  if (!stack.isEmpty()) {
                     this.cache[i] = stack.getItem();
                  } else {
                     this.cache[i] = null;
                  }
               }

               if ((Boolean)this.offhand.getValue()) {
                  ItemStack offhandStack = mc.player.getHeldItemOffhand();
                  if (!offhandStack.isEmpty()) {
                     this.offhandCache = offhandStack.getItem();
                  } else {
                     this.offhandCache = null;
                  }
               }

               for(int index = 0; index < 9; ++index) {
                  Item item = this.cache[index];
                  if (item != null && ((Boolean)this.replenishIllegals.getValue() || item.getItemStackLimit(new ItemStack(item)) != 1)) {
                     ItemStack stackInSlot = mc.player.inventory.getStackInSlot(index);
                     if (stackInSlot.isEmpty() || stackInSlot.getCount() < (Integer)this.threshold.getValue()) {
                        int slot = this.getSlot(item);
                        if (slot != -1) {
                           moveItem(slot, index);
                           this.tickDelay = (Float)this.delay.getValue();
                           return;
                        }
                     }
                  }
               }

               if ((Boolean)this.offhand.getValue() && this.offhandCache != null) {
                  if (!(Boolean)this.replenishIllegals.getValue() && this.offhandCache.getItemStackLimit(new ItemStack(this.offhandCache)) == 1) {
                     return;
                  }

                  ItemStack offhandStack = mc.player.getHeldItemOffhand();
                  if (offhandStack.isEmpty() || offhandStack.getCount() < (Integer)this.threshold.getValue()) {
                     int slot = this.getSlot(this.offhandCache);
                     if (slot != -1) {
                        moveItem(slot, 45);
                        this.tickDelay = (Float)this.delay.getValue();
                     }
                  }
               }

            }
         }
      }
   }

   public static void moveItem(int sourceSlot, int targetSlot) {
      Locks.acquire(Locks.WINDOW_CLICK_LOCK, () -> {
         int source = sourceSlot < 9 ? sourceSlot + 36 : sourceSlot;
         mc.playerController.windowClick(0, source, 0, ClickType.QUICK_MOVE, mc.player);
      });
   }

   int getSlot(Item item) {
      for(int i = 9; i < 36; ++i) {
         ItemStack stack = mc.player.inventory.getStackInSlot(i);
         if (!stack.isEmpty() && stack.getItem().equals(item)) {
            return i;
         }
      }

      return -1;
   }

   public void onDisable() {
      this.cache = new Item[9];
      this.offhandCache = null;
   }
}
