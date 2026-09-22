package fat.vonware.features.modules.combat;

import fat.vonware.features.command.commands.KitCommand;
import fat.vonware.features.modules.Module;
import fat.vonware.features.setting.Setting;
import fat.vonware.util.Timer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.ContainerShulkerBox;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class AutoRegear extends Module {
   private final Setting<Boolean> closeAfter = new Setting<Boolean>("Close", false, "Closes the shulker container after AutoRegear finishes moving the required items.");
   private final Setting<Boolean> force = new Setting<Boolean>("Force", false, "Forces AutoRegear to continue its regear cycle instead of stopping after the first completed pass.");
   private final Setting<RegearMode> regearMode;
   private final Setting<Integer> clickDelay;
   private final Setting<Boolean> await;
   private final Setting<Integer> awaitDelay;
   private final Setting<Boolean> smartClick;
   private boolean openedBefore;
   private boolean doneBefore;
   private HashMap<Integer, String> planInventory;
   private final HashMap<Integer, String> containerInv;
   private boolean awaiting;
   private List<Integer> pendingClicks;
   private ClickType activeClickType;
   private final Timer clickTimer;
   private final Timer awaitTimer;

   public AutoRegear() {
      super("AutoRegear", "Automatically regears your inventory (Use kit command)", Module.Category.COMBAT, true, false, false);
      this.regearMode = (new Setting("Regear Mode", AutoRegear.RegearMode.Instant)).withDescription((Function<RegearMode, String>)((mode) -> {
         switch (mode) {
            case Instant:
               return "Instant: Executes the entire regear click plan in a single tick.";
            case Normal:
               return "Normal: Spreads the regear click plan across multiple ticks using Click Delay.";
            default:
               return "";
         }
      }));
      this.clickDelay = new Setting<Integer>("Click Delay", 50, 0, 1000, (v) -> this.regearMode.getValue() == AutoRegear.RegearMode.Normal, "Delay in milliseconds between each inventory click while using Normal mode.");
      this.await = new Setting<Boolean>("Await", false, (v) -> this.regearMode.getValue() == AutoRegear.RegearMode.Normal, "Waits after opening the shulker before Normal mode begins sending inventory clicks.");
      this.awaitDelay = new Setting<Integer>("Await Delay", 250, 0, 5000, (v) -> this.regearMode.getValue() == AutoRegear.RegearMode.Normal && (Boolean)this.await.getValue(), "Delay in milliseconds before Normal mode begins clicking after the shulker opens.");
      this.smartClick = new Setting<Boolean>("SmartClick", false, "Uses Quick Move to place kit items into their intended inventory and hotbar slots instead of pickup-and-place swaps.");
      this.activeClickType = ClickType.PICKUP;
      this.clickTimer = new Timer();
      this.awaitTimer = new Timer();
      this.register(this.closeAfter);
      this.register(this.force);
      this.register(this.regearMode);
      this.register(this.clickDelay);
      this.register(this.await);
      this.register(this.awaitDelay);
      this.register(this.smartClick);
      this.planInventory = new HashMap();
      this.containerInv = new HashMap();
   }

   public void onEnable() {
      super.onEnable();
      if (!fullNullCheck()) {
         String curConfigName = KitCommand.getCurrentKit();
         if (curConfigName.equals("")) {
            this.toggle();
         } else {
            String inventoryConfig = KitCommand.getKitInventory(curConfigName);
            if (inventoryConfig.equals("")) {
               this.toggle();
            } else {
               String[] inventoryDivided = inventoryConfig.split(" ");
               this.planInventory = new HashMap();
               HashMap<String, Integer> nItems = new HashMap();

               for(int i = 0; i < inventoryDivided.length; ++i) {
                  if (!inventoryDivided[i].contains("air")) {
                     this.planInventory.put(i, inventoryDivided[i]);
                     if (nItems.containsKey(inventoryDivided[i])) {
                        nItems.put(inventoryDivided[i], (Integer)nItems.get(inventoryDivided[i]) + 1);
                     } else {
                        nItems.put(inventoryDivided[i], 1);
                     }
                  }
               }

               boolean b = false;
               this.doneBefore = false;
               this.openedBefore = false;
               this.resetNormalState();
            }
         }
      }
   }

   @SubscribeEvent
   public void onUpdate(TickEvent.ClientTickEvent event) {
      if (!fullNullCheck()) {
         if (this.planInventory.size() == 0) {
            this.toggle();
         } else {
            if (mc.player.openContainer instanceof ContainerShulkerBox) {
               if (this.regearMode.getValue() == AutoRegear.RegearMode.Instant) {
                  this.sortInventoryAlgo();
               } else {
                  this.sortInventoryAlgoNormal();
               }
            } else {
               this.openedBefore = false;
               this.resetNormalState();
            }

         }
      }
   }

   private void resetNormalState() {
      this.awaiting = false;
      this.pendingClicks = null;
   }

   private int getMaxValue() {
      return mc.player.openContainer instanceof ContainerChest ? ((ContainerChest)mc.player.openContainer).getLowerChestInventory().getSizeInventory() : 27;
   }

   private void captureContainerInv(int maxValue) {
      this.containerInv.clear();

      for(int i = 0; i < maxValue; ++i) {
         ItemStack item = (ItemStack)mc.player.openContainer.getInventory().get(i);
         this.containerInv.put(i, ((ResourceLocation)Objects.requireNonNull(item.getItem().getRegistryName())).toString() + item.getMetadata());
      }

   }

   private void sortInventoryAlgo() {
      if (!this.openedBefore) {
         int maxValue = this.getMaxValue();
         this.captureContainerInv(maxValue);
         ArrayList<Integer> sortItems;
         ClickType clickTypeUsed;
         if ((Boolean)this.smartClick.getValue()) {
            sortItems = this.getSmartClickMoves(maxValue);
            clickTypeUsed = ClickType.QUICK_MOVE;
         } else {
            HashMap<Integer, String> inventoryCopy = this.getInventoryCopy(maxValue);
            HashMap<Integer, String> aimInventory = this.getInventoryCopy(maxValue, this.planInventory);
            sortItems = this.getInventorySort(inventoryCopy, aimInventory, maxValue);
            clickTypeUsed = ClickType.PICKUP;
         }

         if (sortItems.size() == 0 && !this.doneBefore) {
            if ((Boolean)this.closeAfter.getValue()) {
               mc.player.closeScreen();
            }
         } else {
            for(int slotChange : sortItems) {
               mc.playerController.windowClick(mc.player.openContainer.windowId, slotChange, 0, clickTypeUsed, mc.player);
            }

            if (!(Boolean)this.smartClick.getValue() && sortItems.size() != 0) {
               int slotChange = (Integer)sortItems.get(sortItems.size() - 1);
               if (((ItemStack)mc.player.openContainer.getInventory().get(slotChange)).isEmpty()) {
                  mc.playerController.windowClick(0, slotChange, 0, clickTypeUsed, mc.player);
               }
            }

            if ((Boolean)this.force.getValue() && !this.doneBefore) {
               this.doneBefore = true;
               return;
            }

            this.doneBefore = false;
            if ((Boolean)this.closeAfter.getValue()) {
               mc.player.closeScreen();
            }
         }

         this.openedBefore = true;
      }

   }

   private void sortInventoryAlgoNormal() {
      if (!this.openedBefore) {
         this.openedBefore = true;
         int maxValue = this.getMaxValue();
         this.captureContainerInv(maxValue);
         if ((Boolean)this.smartClick.getValue()) {
            this.pendingClicks = this.getSmartClickMoves(maxValue);
            this.activeClickType = ClickType.QUICK_MOVE;
         } else {
            HashMap<Integer, String> inventoryCopy = this.getInventoryCopy(maxValue);
            HashMap<Integer, String> aimInventory = this.getInventoryCopy(maxValue, this.planInventory);
            this.pendingClicks = this.getInventorySort(inventoryCopy, aimInventory, maxValue);
            this.activeClickType = ClickType.PICKUP;
         }

         if (this.pendingClicks.isEmpty()) {
            if ((Boolean)this.closeAfter.getValue()) {
               mc.player.closeScreen();
            }

         } else if ((Boolean)this.await.getValue()) {
            this.awaiting = true;
            this.awaitTimer.setDelay(((Integer)this.awaitDelay.getValue()).longValue());
            this.awaitTimer.resetDelay();
         } else {
            this.clickTimer.setDelay(((Integer)this.clickDelay.getValue()).longValue());
            this.clickTimer.resetDelay();
         }
      } else if (this.awaiting) {
         if (this.awaitTimer.isPassed()) {
            this.awaiting = false;
            this.clickTimer.setDelay(((Integer)this.clickDelay.getValue()).longValue());
            this.clickTimer.resetDelay();
         }
      } else if (this.pendingClicks != null && !this.pendingClicks.isEmpty()) {
         if (this.clickTimer.isPassed()) {
            int slotChange = (Integer)this.pendingClicks.remove(0);
            mc.playerController.windowClick(mc.player.openContainer.windowId, slotChange, 0, this.activeClickType, mc.player);
            this.clickTimer.resetDelay();
            if (this.pendingClicks.isEmpty()) {
               if (this.activeClickType == ClickType.PICKUP && ((ItemStack)mc.player.openContainer.getInventory().get(slotChange)).isEmpty()) {
                  mc.playerController.windowClick(0, slotChange, 0, this.activeClickType, mc.player);
               }

               this.doneBefore = false;
               if ((Boolean)this.closeAfter.getValue()) {
                  mc.player.closeScreen();
               }
            }

         }
      }
   }

   private ArrayList<Integer> getInventorySort(HashMap<Integer, String> copyInventory, HashMap<Integer, String> planInventoryCopy, int startValues) {
      ArrayList<Integer> planMove = new ArrayList();
      HashMap<String, Integer> nItemsCopy = new HashMap();

      for(String value : planInventoryCopy.values()) {
         if (nItemsCopy.containsKey(value)) {
            nItemsCopy.put(value, (Integer)nItemsCopy.get(value) + 1);
         } else {
            nItemsCopy.put(value, 1);
         }
      }

      ArrayList<Integer> ignoreValues = new ArrayList();
      int[] listValue = new int[planInventoryCopy.size()];
      int id = 0;

      for(int idx : planInventoryCopy.keySet()) {
         listValue[id++] = idx;
      }

      for(int item : listValue) {
         if (((String)copyInventory.get(item)).equals(planInventoryCopy.get(item))) {
            ignoreValues.add(item);
            nItemsCopy.put(planInventoryCopy.get(item), (Integer)nItemsCopy.get(planInventoryCopy.get(item)) - 1);
            if ((Integer)nItemsCopy.get(planInventoryCopy.get(item)) == 0) {
               nItemsCopy.remove(planInventoryCopy.get(item));
            }

            planInventoryCopy.remove(item);
         }
      }

      String pickedItem = null;

      for(int i = startValues; i < startValues + copyInventory.size(); ++i) {
         if (!ignoreValues.contains(i)) {
            String itemCheck = (String)copyInventory.get(i);
            Optional<Map.Entry<Integer, String>> momentAim = planInventoryCopy.entrySet().stream().filter((x) -> ((String)x.getValue()).equals(itemCheck)).findFirst();
            if (momentAim.isPresent()) {
               if (pickedItem == null) {
                  planMove.add(i);
               }

               int aimKey = (Integer)((Map.Entry)momentAim.get()).getKey();
               planMove.add(aimKey);
               if (pickedItem == null || !pickedItem.equals(itemCheck)) {
                  ignoreValues.add(aimKey);
               }

               nItemsCopy.put(itemCheck, (Integer)nItemsCopy.get(itemCheck) - 1);
               if ((Integer)nItemsCopy.get(itemCheck) == 0) {
                  nItemsCopy.remove(itemCheck);
               }

               copyInventory.put(i, copyInventory.get(aimKey));
               copyInventory.put(aimKey, itemCheck);
               if (!((String)copyInventory.get(aimKey)).equals("minecraft:air0")) {
                  if (i >= startValues + copyInventory.size()) {
                     continue;
                  }

                  pickedItem = (String)copyInventory.get(i);
                  --i;
               } else {
                  pickedItem = null;
               }

               planInventoryCopy.remove(aimKey);
            } else if (pickedItem != null) {
               planMove.add(i);
               copyInventory.put(i, pickedItem);
               pickedItem = null;
            }
         }
      }

      if (planMove.size() != 0 && ((Integer)planMove.get(planMove.size() - 1)).equals(planMove.get(planMove.size() - 2))) {
         planMove.remove(planMove.size() - 1);
      }

      Object[] keyList = this.containerInv.keySet().toArray();

      for(int values = 0; values < keyList.length; ++values) {
         int itemC = (Integer)keyList[values];
         if (nItemsCopy.containsKey(this.containerInv.get(itemC))) {
            int start = (Integer)((Map.Entry)planInventoryCopy.entrySet().stream().filter((x) -> ((String)x.getValue()).equals(this.containerInv.get(itemC))).findFirst().get()).getKey();
            planMove.add(start);
            planMove.add(itemC);
            planMove.add(start);
            nItemsCopy.put(planInventoryCopy.get(start), (Integer)nItemsCopy.get(planInventoryCopy.get(start)) - 1);
            if ((Integer)nItemsCopy.get(planInventoryCopy.get(start)) == 0) {
               nItemsCopy.remove(planInventoryCopy.get(start));
            }

            planInventoryCopy.remove(start);
         }
      }

      if ((Boolean)this.force.getValue()) {
         Iterator<Integer> iterator3 = planMove.iterator();

         while(iterator3.hasNext()) {
            iterator3.next();
         }
      }

      return planMove;
   }

   private ArrayList<Integer> getSmartClickMoves(int maxValue) {
      ArrayList<Integer> clicks = new ArrayList();
      int startPoint = maxValue;
      HashMap<Integer, String> aimInventory = this.getInventoryCopy(maxValue, this.planInventory);
      HashMap<String, ArrayDeque<Integer>> sourceSlots = new HashMap();

      for(Map.Entry<Integer, String> entry : this.containerInv.entrySet()) {
         if (!((String)entry.getValue()).equals("minecraft:air0")) {
            ((ArrayDeque)sourceSlots.computeIfAbsent(entry.getValue(), (k) -> new ArrayDeque())).add(entry.getKey());
         }
      }

      for(int idx = maxValue + 35; idx >= startPoint; --idx) {
         String want = (String)aimInventory.get(idx);
         if (want != null) {
            ItemStack current = (ItemStack)mc.player.openContainer.getInventory().get(idx);
            String currentId = current.isEmpty() ? "minecraft:air0" : ((ResourceLocation)Objects.requireNonNull(current.getItem().getRegistryName())).toString() + current.getMetadata();
            if (!currentId.equals(want)) {
               if (!currentId.equals("minecraft:air0")) {
                  clicks.add(idx);
               }

               ArrayDeque<Integer> queue = (ArrayDeque)sourceSlots.get(want);
               if (queue != null && !queue.isEmpty()) {
                  clicks.add(queue.poll());
               }
            }
         }
      }

      return clicks;
   }

   private HashMap<Integer, String> getInventoryCopy(int startPoint) {
      HashMap<Integer, String> output = new HashMap();
      int sizeInventory = mc.player.inventory.mainInventory.size();

      for(int i = 0; i < sizeInventory; ++i) {
         int value = i + startPoint + (i < 9 ? sizeInventory - 9 : -9);
         ItemStack item = (ItemStack)mc.player.openContainer.getInventory().get(value);
         output.put(value, ((ResourceLocation)Objects.requireNonNull(item.getItem().getRegistryName())).toString() + item.getMetadata());
      }

      return output;
   }

   private HashMap<Integer, String> getInventoryCopy(int startPoint, HashMap<Integer, String> inventory) {
      HashMap<Integer, String> output = new HashMap();
      int sizeInventory = mc.player.inventory.mainInventory.size();

      for(int val : inventory.keySet()) {
         output.put(val + startPoint + (val < 9 ? sizeInventory - 9 : -9), inventory.get(val));
      }

      return output;
   }

   public static enum RegearMode {
      Instant,
      Normal;
   }
}
