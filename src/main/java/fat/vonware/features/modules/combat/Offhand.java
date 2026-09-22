package fat.vonware.features.modules.combat;

import fat.vonware.Vonware;
import fat.vonware.event.events.PacketEvent;
import fat.vonware.features.modules.Module;
import fat.vonware.features.setting.Setting;
import fat.vonware.util.DamageUtil;
import fat.vonware.util.InventoryUtil;
import fat.vonware.util.Locks;
import fat.vonware.util.Timer;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityEnderCrystal;
import net.minecraft.init.Items;
import net.minecraft.inventory.ClickType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.CPacketClickWindow;
import net.minecraft.network.play.server.SPacketConfirmTransaction;
import net.minecraft.network.play.server.SPacketEntityStatus;
import net.minecraft.network.play.server.SPacketSetSlot;
import net.minecraft.network.play.server.SPacketWindowItems;
import net.minecraft.util.EnumHand;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class Offhand extends Module {
   public static Offhand INSTANCE;
   protected final Setting<OffhandItem> item;
   public final Setting<Boolean> swordGap;
   public final Setting<Boolean> strictGap;
   public final Setting<Integer> gappleHealth;
   public final Setting<Integer> totemHealth;
   public final Setting<Boolean> lethal;
   public final Setting<Boolean> armorCheck;
   public final Setting<Integer> delay;
   public final Setting<Boolean> packetPopDetect;
   public final Setting<Boolean> doubleClicks;
   public final Setting<Integer> gappleGrace;
   private final Timer swapTimer;
   private final Map<Item, Integer> lastSlots;
   private final Set<Short> ownTransactions;
   private volatile SwapState swapState;
   private Item requestedItem;
   private int sourceSlot;
   private ItemStack displacedStack;
   private boolean moduleOwnsDrag;
   private boolean sendingOwnClick;
   private long stateStarted;
   private long recoveryUntil;
   private long lastSwapTime;
   private long lastOwnClickTime;
   private volatile boolean transactionRejected;
   private volatile boolean serverResyncReceived;
   private volatile boolean localPopPending;
   private volatile boolean offhandEmptyConfirmed;
   private volatile long localPopTime;
   private boolean gappleRequested;
   private boolean lastUseKeyDown;
   private long gappleReleaseTime;

   public Offhand() {
      super("Offhand", "Automatically swaps your offhand with a totem", Module.Category.COMBAT, true, false, false);
      this.item = (new Setting("Item", Offhand.OffhandItem.Totem)).withDescription((Function<OffhandItem, String>)((mode) -> {
         switch (mode) {
            case Totem:
               return "Totem: Keeps a Totem of Undying in your offhand when no higher-priority override is active.";
            case Crystal:
               return "Crystal: Keeps an end crystal in your offhand when safe conditions allow it.";
            case Gapple:
               return "Gapple: Keeps a golden apple in your offhand when safe conditions allow it.";
            default:
               return "";
         }
      }));
      this.swordGap = new Setting<Boolean>("Sword Gap", true, "Temporarily switches your offhand to a golden apple while holding a diamond sword and using the item key.");
      this.strictGap = new Setting<Boolean>("StrictGap", false, (v) -> (Boolean)this.swordGap.getValue(), "Pauses Aura while Sword Gap is requested or actively eating so combat actions do not interrupt it.");
      this.gappleHealth = new Setting<Integer>("Gapple Health", 15, 0, 20, (v) -> (Boolean)this.swordGap.getValue(), "Minimum health required before Sword Gap is allowed to switch your offhand to a golden apple.");
      this.totemHealth = new Setting<Integer>("Health", 15, 0, 20, "Forces a totem into your offhand when your health reaches or falls below this value.");
      this.lethal = new Setting<Boolean>("LethalTest", false, "Test setting do NOT use high likelihood to crash.");
      this.armorCheck = new Setting<Boolean>("Armor Check", false, "Forces a totem whenever you are not wearing a complete armor set.");
      this.delay = new Setting<Integer>("Delay", 0, 0, 300, "Delay in milliseconds between normal offhand inventory click actions.");
      this.packetPopDetect = new Setting<Boolean>("DetectPopPacket", true, "Detects your totem-pop packet and immediately refills the empty offhand after the server confirms it.");
      this.doubleClicks = new Setting<Boolean>("DoubleClicks", false, "Performs the source-slot and offhand pickup clicks together in one NCP multi-click batch.");
      this.gappleGrace = new Setting<Integer>("GappleHoldTime", 0, 0, 1000, (v) -> (Boolean)this.swordGap.getValue(), "Keeps Sword Gap active for this many milliseconds after releasing the use-item key.");
      this.swapTimer = new Timer();
      this.lastSlots = new HashMap();
      this.ownTransactions = Collections.synchronizedSet(new HashSet());
      this.swapState = Offhand.SwapState.IDLE;
      this.requestedItem = Items.AIR;
      this.sourceSlot = -1;
      this.displacedStack = ItemStack.EMPTY;
      this.register(this.item);
      this.register(this.totemHealth);
      this.register(this.lethal);
      this.register(this.swordGap);
      this.register(this.strictGap);
      this.register(this.gappleHealth);
      this.register(this.armorCheck);
      this.register(this.delay);
      this.register(this.packetPopDetect);
      this.register(this.doubleClicks);
      this.register(this.gappleGrace);
      INSTANCE = this;
   }

   public void onEnable() {
      this.resetAllState();
      Vonware.ncpManager.setStrict(true);
   }

   public void onDisable() {
      if (!fullNullCheck() && this.moduleOwnsDrag && !mc.player.inventory.getItemStack().isEmpty()) {
         this.returnOwnedCursor(true);
      }

      Vonware.ncpManager.setStrict(false);
      this.resetAllState();
   }

   public void onLogout() {
      this.resetAllState();
   }

   public String getDisplayInfo() {
      return ((OffhandItem)this.item.getValue()).name();
   }

   public void onUpdate() {
      if (!fullNullCheck()) {
         this.swapTimer.setDelay(((Integer)this.delay.getValue()).longValue());
         this.updateGappleRequest();
         this.expirePopRequest();
         if (!(mc.currentScreen instanceof GuiContainer) || mc.currentScreen instanceof GuiInventory) {
            if (this.transactionRejected || this.serverResyncReceived) {
               this.enterRecovery();
            }

            if (this.swapState == Offhand.SwapState.RECOVER) {
               this.processRecovery();
            } else if (!this.handleFastPopRefill()) {
               Item wanted = this.getItem();
               if (this.swapState != Offhand.SwapState.IDLE && wanted == Items.TOTEM_OF_UNDYING && this.requestedItem != Items.TOTEM_OF_UNDYING) {
                  this.abortForEmergencyTotem();
               } else if (this.swapState != Offhand.SwapState.IDLE) {
                  this.processActiveSwap();
               } else if (mc.player.inventory.getItemStack().isEmpty()) {
                  if (mc.player.getHeldItem(EnumHand.OFF_HAND).getItem() == wanted) {
                     this.clearCompletedContextIfSafe();
                  } else if (this.swapTimer.isPassed()) {
                     this.beginSwap(wanted);
                  }
               }
            }
         }
      }
   }

   @SubscribeEvent(
      priority = EventPriority.HIGHEST
   )
   public void onPacketReceive(PacketEvent.Receive event) {
      if (event.getPacket() instanceof SPacketEntityStatus) {
         SPacketEntityStatus packet = (SPacketEntityStatus)event.getPacket();
         if ((Boolean)this.packetPopDetect.getValue() && packet.getOpCode() == 35 && mc.world != null && mc.player != null) {
            Entity entity = packet.getEntity(mc.world);
            if (entity == mc.player) {
               this.localPopPending = true;
               this.offhandEmptyConfirmed = false;
               this.localPopTime = System.currentTimeMillis();
            }
         }

      } else if (event.getPacket() instanceof SPacketSetSlot) {
         SPacketSetSlot packet = (SPacketSetSlot)event.getPacket();
         if ((Boolean)this.packetPopDetect.getValue() && this.localPopPending && packet.getWindowId() == 0 && packet.getSlot() == 45 && packet.getStack().isEmpty()) {
            this.offhandEmptyConfirmed = true;
         }

      } else if (event.getPacket() instanceof SPacketConfirmTransaction) {
         SPacketConfirmTransaction packet = (SPacketConfirmTransaction)event.getPacket();
         if (packet.getWindowId() == 0 && this.removeOwnTransaction(packet.getActionNumber()) && !packet.wasAccepted()) {
            this.transactionRejected = true;
         }

      } else {
         if (event.getPacket() instanceof SPacketWindowItems) {
            SPacketWindowItems packet = (SPacketWindowItems)event.getPacket();
            if (packet.getWindowId() == 0 && (this.swapState != Offhand.SwapState.IDLE || this.hasPendingTransactions())) {
               this.serverResyncReceived = true;
            }
         }

      }
   }

   @SubscribeEvent(
      priority = EventPriority.HIGHEST
   )
   public void onPacketSend(PacketEvent.Send event) {
      if (this.sendingOwnClick && event.getPacket() instanceof CPacketClickWindow) {
         CPacketClickWindow packet = (CPacketClickWindow)event.getPacket();
         if (packet.getWindowId() == 0) {
            this.ownTransactions.add(packet.getActionNumber());
            this.lastOwnClickTime = System.currentTimeMillis();
         }

      }
   }

   private void beginSwap(Item wanted) {
      if (wanted != null && wanted != Items.AIR) {
         int slot = findItemContainerSlot(wanted);
         if (slot != -1) {
            ItemStack source = getContainerStack(slot);
            ItemStack cursor = mc.player.inventory.getItemStack();
            ItemStack offhand = mc.player.getHeldItem(EnumHand.OFF_HAND);
            if (!source.isEmpty() && source.getItem() == wanted && cursor.isEmpty()) {
               this.requestedItem = wanted;
               this.sourceSlot = slot;
               this.displacedStack = offhand.copy();
               this.stateStarted = System.currentTimeMillis();
               this.moduleOwnsDrag = false;
               if ((Boolean)this.doubleClicks.getValue()) {
                  this.performPickupAndPlace(slot, wanted, false);
               } else if (this.clickSinglePickup(slot)) {
                  this.moduleOwnsDrag = !mc.player.inventory.getItemStack().isEmpty();
                  this.swapState = mc.player.inventory.getItemStack().getItem() == wanted ? Offhand.SwapState.PLACE : Offhand.SwapState.RECOVER;
                  this.markClickActivity();
               }

            }
         }
      }
   }

   private void processActiveSwap() {
      switch (this.swapState) {
         case PLACE:
            this.processPlace();
            break;
         case RETURN:
            this.processReturn();
            break;
         case VERIFY:
            this.processVerify();
      }

   }

   private void processPlace() {
      ItemStack offhand = mc.player.getHeldItem(EnumHand.OFF_HAND);
      ItemStack cursor = mc.player.inventory.getItemStack();
      if (offhand.getItem() == this.requestedItem) {
         this.swapState = cursor.isEmpty() ? Offhand.SwapState.VERIFY : Offhand.SwapState.RETURN;
         this.stateStarted = System.currentTimeMillis();
      } else if (!cursor.isEmpty() && cursor.getItem() == this.requestedItem) {
         if (this.swapTimer.isPassed()) {
            this.displacedStack = offhand.copy();
            if (this.clickSinglePickup(45)) {
               this.markClickActivity();
               this.moduleOwnsDrag = !mc.player.inventory.getItemStack().isEmpty();
               if (mc.player.getHeldItem(EnumHand.OFF_HAND).getItem() == this.requestedItem) {
                  this.swapState = mc.player.inventory.getItemStack().isEmpty() ? Offhand.SwapState.VERIFY : Offhand.SwapState.RETURN;
                  this.stateStarted = System.currentTimeMillis();
               } else {
                  this.enterRecovery();
               }
            }

         }
      } else {
         this.enterRecovery();
      }
   }

   private void processReturn() {
      ItemStack cursor = mc.player.inventory.getItemStack();
      if (cursor.isEmpty()) {
         this.swapState = Offhand.SwapState.VERIFY;
         this.stateStarted = System.currentTimeMillis();
      } else if (!this.moduleOwnsDrag) {
         this.enterRecovery();
      } else if (this.swapTimer.isPassed()) {
         int destination = this.getReturnDestination();
         if (destination == -1) {
            this.enterRecovery();
         } else {
            Item returnedItem = cursor.getItem();
            if (this.clickSinglePickup(destination)) {
               this.markClickActivity();
               if (mc.player.inventory.getItemStack().isEmpty()) {
                  this.lastSlots.put(returnedItem, fromContainerSlot(destination));
                  this.moduleOwnsDrag = false;
                  this.swapState = Offhand.SwapState.VERIFY;
                  this.stateStarted = System.currentTimeMillis();
               } else {
                  this.enterRecovery();
               }
            }

         }
      }
   }

   private void processVerify() {
      ItemStack offhand = mc.player.getHeldItem(EnumHand.OFF_HAND);
      ItemStack cursor = mc.player.inventory.getItemStack();
      if (offhand.getItem() == this.requestedItem && cursor.isEmpty()) {
         this.moduleOwnsDrag = false;
         this.swapState = Offhand.SwapState.IDLE;
         this.stateStarted = System.currentTimeMillis();
         this.lastSwapTime = this.stateStarted;
      } else {
         if (System.currentTimeMillis() - this.stateStarted >= (long)Math.max(150, 100)) {
            this.enterRecovery();
         }

      }
   }

   private boolean handleFastPopRefill() {
      if ((Boolean)this.packetPopDetect.getValue() && this.localPopPending) {
         if (this.offhandEmptyConfirmed && mc.player.getHeldItem(EnumHand.OFF_HAND).isEmpty()) {
            if (this.swapState != Offhand.SwapState.IDLE && !this.abortForEmergencyTotem()) {
               return true;
            } else {
               ItemStack cursor = mc.player.inventory.getItemStack();
               if (!cursor.isEmpty()) {
                  if (cursor.getItem() == Items.TOTEM_OF_UNDYING && this.moduleOwnsDrag) {
                     this.requestedItem = Items.TOTEM_OF_UNDYING;
                     this.displacedStack = ItemStack.EMPTY;
                     if (this.clickSinglePickup(45)) {
                        this.markClickActivity();
                        this.moduleOwnsDrag = !mc.player.inventory.getItemStack().isEmpty();
                        this.swapState = this.moduleOwnsDrag ? Offhand.SwapState.RETURN : Offhand.SwapState.VERIFY;
                        this.clearPopFlags();
                     }

                     return true;
                  } else {
                     return false;
                  }
               } else {
                  int slot = findItemContainerSlot(Items.TOTEM_OF_UNDYING);
                  if (slot == -1) {
                     return false;
                  } else {
                     this.requestedItem = Items.TOTEM_OF_UNDYING;
                     this.sourceSlot = slot;
                     this.displacedStack = ItemStack.EMPTY;
                     this.stateStarted = System.currentTimeMillis();
                     this.performPickupAndPlace(slot, Items.TOTEM_OF_UNDYING, true);
                     this.clearPopFlags();
                     return true;
                  }
               }
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private void performPickupAndPlace(int slot, Item wanted, boolean requireEmptyOffhand) {
      Locks.WINDOW_CLICK_LOCK.lock();

      try {
         ItemStack source = getContainerStack(slot);
         ItemStack cursor = mc.player.inventory.getItemStack();
         ItemStack offhand = mc.player.getHeldItem(EnumHand.OFF_HAND);
         if (source.isEmpty() || source.getItem() != wanted || !cursor.isEmpty() || requireEmptyOffhand && !offhand.isEmpty()) {
            this.swapState = Offhand.SwapState.RECOVER;
            this.recoveryUntil = System.currentTimeMillis() + 100L;
            return;
         }

         Vonware.ncpManager.startMultiClick();

         try {
            this.sendPickupClick(slot);
            this.moduleOwnsDrag = !mc.player.inventory.getItemStack().isEmpty();
            if (mc.player.inventory.getItemStack().getItem() == wanted && (!requireEmptyOffhand || mc.player.getHeldItem(EnumHand.OFF_HAND).isEmpty())) {
               this.sendPickupClick(45);
            }
         } finally {
            Vonware.ncpManager.releaseMultiClick();
         }
      } finally {
         Locks.WINDOW_CLICK_LOCK.unlock();
      }

      this.markClickActivity();
      this.moduleOwnsDrag = !mc.player.inventory.getItemStack().isEmpty();
      if (mc.player.getHeldItem(EnumHand.OFF_HAND).getItem() == wanted) {
         this.swapState = this.moduleOwnsDrag ? Offhand.SwapState.RETURN : Offhand.SwapState.VERIFY;
      } else if (mc.player.inventory.getItemStack().getItem() == wanted) {
         this.swapState = Offhand.SwapState.PLACE;
      } else {
         this.enterRecovery();
      }

      this.stateStarted = System.currentTimeMillis();
   }

   private boolean abortForEmergencyTotem() {
      if (this.swapState == Offhand.SwapState.IDLE) {
         return true;
      } else if (mc.player.inventory.getItemStack().isEmpty() || this.moduleOwnsDrag && this.returnOwnedCursor(true)) {
         this.swapState = Offhand.SwapState.IDLE;
         this.requestedItem = Items.AIR;
         this.displacedStack = ItemStack.EMPTY;
         this.stateStarted = System.currentTimeMillis();
         return true;
      } else {
         return false;
      }
   }

   private void enterRecovery() {
      this.transactionRejected = false;
      this.serverResyncReceived = false;
      this.swapState = Offhand.SwapState.RECOVER;
      this.recoveryUntil = System.currentTimeMillis() + 100L;
      this.stateStarted = System.currentTimeMillis();
      if (!mc.player.inventory.getItemStack().isEmpty() && System.currentTimeMillis() - this.lastOwnClickTime <= 2000L) {
         this.moduleOwnsDrag = true;
      }

   }

   private void processRecovery() {
      if (System.currentTimeMillis() >= this.recoveryUntil) {
         if (mc.player.inventory.getItemStack().isEmpty() || this.moduleOwnsDrag && this.returnOwnedCursor(false)) {
            this.moduleOwnsDrag = false;
            this.swapState = Offhand.SwapState.IDLE;
            this.requestedItem = Items.AIR;
            this.displacedStack = ItemStack.EMPTY;
            this.stateStarted = System.currentTimeMillis();
            this.swapTimer.resetDelay();
         } else {
            this.recoveryUntil = System.currentTimeMillis() + 100L;
         }
      }
   }

   private boolean returnOwnedCursor(boolean immediate) {
      if (mc.player.inventory.getItemStack().isEmpty()) {
         this.moduleOwnsDrag = false;
         return true;
      } else if (!this.moduleOwnsDrag) {
         return false;
      } else if (!immediate && !this.swapTimer.isPassed()) {
         return false;
      } else {
         int destination = this.getReturnDestination();
         if (destination == -1) {
            return false;
         } else {
            Item cursorItem = mc.player.inventory.getItemStack().getItem();
            if (!this.clickSinglePickup(destination)) {
               return false;
            } else {
               this.markClickActivity();
               boolean returned = mc.player.inventory.getItemStack().isEmpty();
               if (returned) {
                  this.lastSlots.put(cursorItem, fromContainerSlot(destination));
                  this.moduleOwnsDrag = false;
               }

               return returned;
            }
         }
      }
   }

   private int getReturnDestination() {
      return isPlayerInventorySlot(this.sourceSlot) && getContainerStack(this.sourceSlot).isEmpty() ? this.sourceSlot : findEmptyContainerSlot();
   }

   private boolean clickSinglePickup(int slot) {
      if (!isClickableSlot(slot)) {
         return false;
      } else {
         Locks.WINDOW_CLICK_LOCK.lock();

         boolean var2;
         try {
            this.sendPickupClick(slot);
            var2 = true;
         } finally {
            Locks.WINDOW_CLICK_LOCK.unlock();
         }

         return var2;
      }
   }

   private void sendPickupClick(int slot) {
      this.sendingOwnClick = true;

      try {
         mc.playerController.windowClick(mc.player.inventoryContainer.windowId, slot, 0, ClickType.PICKUP, mc.player);
      } finally {
         this.sendingOwnClick = false;
      }

   }

   private void markClickActivity() {
      this.swapTimer.resetDelay();
      this.lastSwapTime = System.currentTimeMillis();
   }

   private void updateGappleRequest() {
      boolean keyDown = mc.gameSettings.keyBindUseItem.isKeyDown();
      boolean holdingSword = mc.player.getHeldItem(EnumHand.MAIN_HAND).getItem() == Items.DIAMOND_SWORD;
      double health = (double)(mc.player.getHealth() + mc.player.getAbsorptionAmount());
      boolean allowed = (Boolean)this.swordGap.getValue() && holdingSword && mc.currentScreen == null && health > (double)(Integer)this.gappleHealth.getValue() && (!(Boolean)this.armorCheck.getValue() || InventoryUtil.hasFullArmor());
      if (!allowed) {
         this.gappleRequested = false;
         this.gappleReleaseTime = 0L;
         this.lastUseKeyDown = keyDown;
      } else {
         if (keyDown) {
            this.gappleRequested = true;
            this.gappleReleaseTime = 0L;
         } else if (this.lastUseKeyDown) {
            this.gappleReleaseTime = System.currentTimeMillis();
         }

         if (this.gappleRequested && !keyDown && !mc.player.isHandActive() && this.gappleReleaseTime != 0L && System.currentTimeMillis() - this.gappleReleaseTime >= (long)(Integer)this.gappleGrace.getValue()) {
            this.gappleRequested = false;
            this.gappleReleaseTime = 0L;
         }

         this.lastUseKeyDown = keyDown;
      }
   }

   private void expirePopRequest() {
      if (!(Boolean)this.packetPopDetect.getValue()) {
         this.clearPopFlags();
      } else {
         if (this.localPopPending && System.currentTimeMillis() - this.localPopTime > 750L) {
            this.clearPopFlags();
         }

      }
   }

   private void clearPopFlags() {
      this.localPopPending = false;
      this.offhandEmptyConfirmed = false;
      this.localPopTime = 0L;
   }

   private void clearCompletedContextIfSafe() {
      if (!this.hasPendingTransactions() && System.currentTimeMillis() - this.lastOwnClickTime > 1500L) {
         this.requestedItem = Items.AIR;
         this.sourceSlot = -1;
         this.displacedStack = ItemStack.EMPTY;
      }

   }

   private void resetAllState() {
      this.swapState = Offhand.SwapState.IDLE;
      this.requestedItem = Items.AIR;
      this.sourceSlot = -1;
      this.displacedStack = ItemStack.EMPTY;
      this.moduleOwnsDrag = false;
      this.sendingOwnClick = false;
      this.transactionRejected = false;
      this.serverResyncReceived = false;
      this.gappleRequested = false;
      this.lastUseKeyDown = false;
      this.gappleReleaseTime = 0L;
      this.stateStarted = 0L;
      this.recoveryUntil = 0L;
      this.lastSwapTime = 0L;
      this.lastOwnClickTime = 0L;
      this.clearPopFlags();
      this.ownTransactions.clear();
      this.swapTimer.resetDelay();
   }

   private boolean removeOwnTransaction(short action) {
      synchronized(this.ownTransactions) {
         return this.ownTransactions.remove(action);
      }
   }

   private boolean hasPendingTransactions() {
      synchronized(this.ownTransactions) {
         return !this.ownTransactions.isEmpty();
      }
   }

   private static boolean isClickableSlot(int slot) {
      return slot == 45 || isPlayerInventorySlot(slot);
   }

   private static boolean isPlayerInventorySlot(int slot) {
      return slot >= 9 && slot <= 44;
   }

   private static int toContainerSlot(int inventorySlot) {
      return inventorySlot >= 0 && inventorySlot < 9 ? inventorySlot + 36 : inventorySlot;
   }

   private static int fromContainerSlot(int containerSlot) {
      return containerSlot >= 36 && containerSlot <= 44 ? containerSlot - 36 : containerSlot;
   }

   private static ItemStack getContainerStack(int slot) {
      return mc.player != null && slot >= 0 && slot < mc.player.inventoryContainer.inventorySlots.size() ? mc.player.inventoryContainer.getSlot(slot).getStack() : ItemStack.EMPTY;
   }

   private static int findEmptyContainerSlot() {
      for(int i = 9; i <= 35; ++i) {
         if (getContainerStack(i).isEmpty()) {
            return i;
         }
      }

      for(int i = 36; i <= 44; ++i) {
         if (getContainerStack(i).isEmpty()) {
            return i;
         }
      }

      return -1;
   }

   private static int findItemContainerSlot(Item wanted) {
      int inventorySlot = findItemSlot(wanted);
      return inventorySlot == -1 ? -1 : toContainerSlot(inventorySlot);
   }

   public static boolean findItemInOffhand(Item wanted) {
      return mc.player != null && mc.player.getHeldItem(EnumHand.OFF_HAND).getItem() == wanted;
   }

   public static int findItemSlot(Item wanted) {
      return findItemSlot(wanted, -1);
   }

   public static int findItemSlot(Item wanted, int ignoreSlot) {
      if (mc.player != null && wanted != null) {
         Integer cached = INSTANCE != null ? (Integer)INSTANCE.lastSlots.get(wanted) : null;
         if (cached != null && cached != ignoreSlot) {
            int containerSlot = toContainerSlot(cached);
            ItemStack stack = getContainerStack(containerSlot);
            if (!stack.isEmpty() && stack.getItem() == wanted) {
               return cached;
            }
         }

         for(int i = 9; i <= 35; ++i) {
            if (i != ignoreSlot) {
               ItemStack stack = getContainerStack(i);
               if (!stack.isEmpty() && stack.getItem() == wanted) {
                  if (INSTANCE != null) {
                     INSTANCE.lastSlots.put(wanted, i);
                  }

                  return i;
               }
            }
         }

         for(int i = 0; i < 9; ++i) {
            if (i != ignoreSlot) {
               ItemStack stack = getContainerStack(i + 36);
               if (!stack.isEmpty() && stack.getItem() == wanted) {
                  if (INSTANCE != null) {
                     INSTANCE.lastSlots.put(wanted, i);
                  }

                  return i;
               }
            }
         }

         if (INSTANCE != null) {
            INSTANCE.lastSlots.remove(wanted);
         }

         return -1;
      } else {
         return -1;
      }
   }

   private boolean isGapping() {
      return (Boolean)this.swordGap.getValue() && mc.player != null && mc.player.getHeldItem(EnumHand.MAIN_HAND).getItem() == Items.DIAMOND_SWORD && mc.player.getHeldItem(EnumHand.OFF_HAND).getItem() == Items.GOLDEN_APPLE && mc.player.isHandActive();
   }

   public static boolean shouldPauseAura() {
      if (INSTANCE != null && INSTANCE.isEnabled()) {
         return (Boolean)INSTANCE.strictGap.getValue() && (INSTANCE.gappleRequested || INSTANCE.isGapping());
      } else {
         return false;
      }
   }

   public static boolean isBusy() {
      return INSTANCE != null && INSTANCE.isEnabled() && (INSTANCE.swapState != Offhand.SwapState.IDLE || INSTANCE.moduleOwnsDrag || INSTANCE.localPopPending);
   }

   public Item getItem() {
      if ((Boolean)this.armorCheck.getValue() && !InventoryUtil.hasFullArmor()) {
         return Items.TOTEM_OF_UNDYING;
      } else {
         double health = (double)(mc.player.getHealth() + mc.player.getAbsorptionAmount());
         if ((Boolean)this.lethal.getValue() && this.isCrystalLethal(health)) {
            return Items.TOTEM_OF_UNDYING;
         } else {
            boolean gapEligible = this.gappleRequested && (Boolean)this.swordGap.getValue() && mc.player.getHeldItem(EnumHand.MAIN_HAND).getItem() == Items.DIAMOND_SWORD && health > (double)(Integer)this.gappleHealth.getValue();
            if (gapEligible) {
               return Items.GOLDEN_APPLE;
            } else if (health <= (double)(Integer)this.totemHealth.getValue()) {
               return Items.TOTEM_OF_UNDYING;
            } else {
               Item selected = this.getSelectedItem();
               return selected != Items.TOTEM_OF_UNDYING && mc.player.getHeldItem(EnumHand.OFF_HAND).getItem() != selected && findItemSlot(selected) == -1 ? Items.TOTEM_OF_UNDYING : selected;
            }
         }
      }
   }

   private boolean isCrystalLethal(double health) {
      if (mc.world != null && mc.player != null && !(health <= (double)0.0F)) {
         for(Entity entity : mc.world.loadedEntityList) {
            if (entity != null && entity instanceof EntityEnderCrystal && !entity.isDead && !(mc.player.getDistanceSq(entity) > (double)144.0F) && (double)DamageUtil.calculate(entity, mc.player) >= health) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   public Item getSelectedItem() {
      switch ((OffhandItem)this.item.getValue()) {
         case Totem:
            return Items.TOTEM_OF_UNDYING;
         case Crystal:
            return Items.END_CRYSTAL;
         case Gapple:
            return Items.GOLDEN_APPLE;
         default:
            return Items.TOTEM_OF_UNDYING;
      }
   }

   public static enum OffhandItem {
      Totem,
      Gapple,
      Crystal;
   }

   private static enum SwapState {
      IDLE,
      PLACE,
      RETURN,
      VERIFY,
      RECOVER;
   }
}
