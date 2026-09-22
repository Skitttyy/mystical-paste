package fat.vonware.features.modules.client;

import fat.vonware.event.events.PacketEvent;
import fat.vonware.features.modules.Module;
import fat.vonware.features.setting.Setting;
import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.CPacketClickWindow;
import net.minecraft.network.play.client.CPacketCloseWindow;
import net.minecraft.network.play.client.CPacketConfirmTransaction;
import net.minecraft.network.play.server.SPacketCloseWindow;
import net.minecraft.network.play.server.SPacketConfirmTransaction;
import net.minecraft.network.play.server.SPacketEntityStatus;
import net.minecraft.network.play.server.SPacketOpenWindow;
import net.minecraft.network.play.server.SPacketSetSlot;
import net.minecraft.network.play.server.SPacketWindowItems;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class PacketDebug extends Module {
   public final Setting<Mode> mode;
   public final Setting<Direction> direction;
   public final Setting<Boolean> details;
   public final Setting<Boolean> stacks;
   public final Setting<Boolean> nbt;
   public final Setting<Boolean> clickWindow;
   public final Setting<Boolean> setSlot;
   public final Setting<Boolean> confirm;
   public final Setting<Boolean> windowItems;
   public final Setting<Boolean> entityStatus;

   public PacketDebug() {
      super("PacketDebug", "Logs packets to console", Module.Category.CLIENT, true, false, false);
      this.mode = this.register(new Setting("Mode", PacketDebug.Mode.Offhand, "Offhand logs swap/pop packets, Inventory logs inventory packets, and All logs everything"));
      this.direction = this.register(new Setting("Direction", PacketDebug.Direction.Both, "Which packet direction should be logged"));
      this.details = this.register(new Setting("Details", true, "Shows packet fields instead of only the class name"));
      this.stacks = this.register(new Setting("Stacks", true, (v) -> (Boolean)this.details.getValue()));
      this.nbt = this.register(new Setting("NBT", false, (v) -> (Boolean)this.details.getValue() && (Boolean)this.stacks.getValue()));
      this.clickWindow = this.register(new Setting("ClickWindow", true, (v) -> this.mode.getValue() != PacketDebug.Mode.All));
      this.setSlot = this.register(new Setting("SetSlot", true, (v) -> this.mode.getValue() != PacketDebug.Mode.All));
      this.confirm = this.register(new Setting("Confirm", true, (v) -> this.mode.getValue() != PacketDebug.Mode.All));
      this.windowItems = this.register(new Setting("WindowItems", true, (v) -> this.mode.getValue() != PacketDebug.Mode.All));
      this.entityStatus = this.register(new Setting("EntityStatus", true, (v) -> this.mode.getValue() == PacketDebug.Mode.Offhand));
   }

   @SubscribeEvent
   public void onReceive(PacketEvent.Receive event) {
      if (this.direction.getValue() != PacketDebug.Direction.Outgoing) {
         Packet<?> packet = event.getPacket();
         if (this.shouldLog(packet)) {
            System.out.println("[IN]  " + this.format(packet));
         }
      }
   }

   @SubscribeEvent
   public void onSend(PacketEvent.Send event) {
      if (this.direction.getValue() != PacketDebug.Direction.Incoming) {
         Packet<?> packet = event.getPacket();
         if (this.shouldLog(packet)) {
            System.out.println("[OUT] " + this.format(packet));
         }
      }
   }

   private boolean shouldLog(Packet<?> packet) {
      if (this.mode.getValue() == PacketDebug.Mode.All) {
         return true;
      } else if (packet instanceof CPacketClickWindow) {
         if (!(Boolean)this.clickWindow.getValue()) {
            return false;
         } else {
            CPacketClickWindow click = (CPacketClickWindow)packet;
            return this.mode.getValue() != PacketDebug.Mode.Offhand || click.getWindowId() == 0;
         }
      } else if (packet instanceof SPacketSetSlot) {
         if (!(Boolean)this.setSlot.getValue()) {
            return false;
         } else if (this.mode.getValue() != PacketDebug.Mode.Offhand) {
            return true;
         } else {
            SPacketSetSlot set = (SPacketSetSlot)packet;
            return set.getWindowId() == 0 && set.getSlot() >= 9 && set.getSlot() <= 45 || set.getWindowId() == -1 && set.getSlot() == -1;
         }
      } else if (packet instanceof SPacketConfirmTransaction) {
         if (!(Boolean)this.confirm.getValue()) {
            return false;
         } else {
            SPacketConfirmTransaction confirmPacket = (SPacketConfirmTransaction)packet;
            return this.mode.getValue() != PacketDebug.Mode.Offhand || confirmPacket.getWindowId() == 0;
         }
      } else if (packet instanceof CPacketConfirmTransaction) {
         if (!(Boolean)this.confirm.getValue()) {
            return false;
         } else {
            CPacketConfirmTransaction confirmPacket = (CPacketConfirmTransaction)packet;
            return this.mode.getValue() != PacketDebug.Mode.Offhand || confirmPacket.getWindowId() == 0;
         }
      } else if (packet instanceof SPacketWindowItems) {
         if (!(Boolean)this.windowItems.getValue()) {
            return false;
         } else {
            SPacketWindowItems items = (SPacketWindowItems)packet;
            return this.mode.getValue() != PacketDebug.Mode.Offhand || items.getWindowId() == 0;
         }
      } else if (packet instanceof SPacketEntityStatus) {
         if (this.mode.getValue() == PacketDebug.Mode.Offhand && (Boolean)this.entityStatus.getValue()) {
            SPacketEntityStatus status = (SPacketEntityStatus)packet;
            if (status.getOpCode() == 35 && mc.world != null && mc.player != null) {
               return status.getEntity(mc.world) == mc.player;
            } else {
               return false;
            }
         } else {
            return false;
         }
      } else if (this.mode.getValue() != PacketDebug.Mode.Inventory) {
         return false;
      } else {
         return packet instanceof CPacketCloseWindow || packet instanceof SPacketCloseWindow || packet instanceof SPacketOpenWindow;
      }
   }

   private String format(Packet<?> packet) {
      String name = packet.getClass().getSimpleName();
      if (!(Boolean)this.details.getValue()) {
         return name;
      } else if (packet instanceof CPacketClickWindow) {
         CPacketClickWindow click = (CPacketClickWindow)packet;
         return name + " window=" + click.getWindowId() + " slot=" + click.getSlotId() + "(" + this.slotName(click.getSlotId()) + ") button=" + click.getUsedButton() + " type=" + click.getClickType() + " action=" + click.getActionNumber() + this.stackField(" clicked", click.getClickedItem());
      } else if (packet instanceof SPacketSetSlot) {
         SPacketSetSlot set = (SPacketSetSlot)packet;
         return name + " window=" + set.getWindowId() + " slot=" + set.getSlot() + "(" + this.slotName(set.getSlot()) + ")" + this.stackField(" stack", set.getStack());
      } else if (packet instanceof SPacketConfirmTransaction) {
         SPacketConfirmTransaction transaction = (SPacketConfirmTransaction)packet;
         return name + " window=" + transaction.getWindowId() + " action=" + transaction.getActionNumber() + " accepted=" + transaction.wasAccepted();
      } else if (packet instanceof CPacketConfirmTransaction) {
         CPacketConfirmTransaction transaction = (CPacketConfirmTransaction)packet;
         return name + " window=" + transaction.getWindowId() + " uid=" + transaction.getUid();
      } else if (packet instanceof SPacketWindowItems) {
         SPacketWindowItems window = (SPacketWindowItems)packet;
         List<ItemStack> itemStacks = window.getItemStacks();
         StringBuilder result = (new StringBuilder(name)).append(" window=").append(window.getWindowId()).append(" slots=").append(itemStacks.size());
         if ((Boolean)this.stacks.getValue() && window.getWindowId() == 0 && itemStacks.size() > 45) {
            result.append(" offhand=").append(this.stackToString((ItemStack)itemStacks.get(45)));
         }

         return result.toString();
      } else if (packet instanceof SPacketEntityStatus) {
         SPacketEntityStatus status = (SPacketEntityStatus)packet;
         Entity entity = mc.world == null ? null : status.getEntity(mc.world);
         return name + " opcode=" + status.getOpCode() + " entity=" + (entity == null ? "unknown" : entity.getName()) + " local=" + (entity != null && entity == mc.player);
      } else {
         return name;
      }
   }

   private String stackField(String fieldName, ItemStack stack) {
      return (Boolean)this.stacks.getValue() ? fieldName + "=" + this.stackToString(stack) : "";
   }

   private String stackToString(ItemStack stack) {
      if (stack != null && !stack.isEmpty()) {
         StringBuilder result = new StringBuilder();
         result.append(stack.getItem().getRegistryName()).append(" x").append(stack.getCount()).append(" meta=").append(stack.getMetadata());
         if (stack.isItemStackDamageable()) {
            result.append(" damage=").append(stack.getItemDamage()).append('/').append(stack.getMaxDamage());
         }

         if ((Boolean)this.nbt.getValue() && stack.hasTagCompound()) {
            result.append(" nbt=").append(stack.getTagCompound());
         }

         return result.toString();
      } else {
         return "EMPTY";
      }
   }

   private String slotName(int slot) {
      if (slot == -999) {
         return "OUTSIDE";
      } else if (slot == -1) {
         return "CURSOR";
      } else if (slot >= 5 && slot <= 8) {
         return "ARMOR";
      } else if (slot >= 9 && slot <= 35) {
         return "MAIN";
      } else if (slot >= 36 && slot <= 44) {
         return "HOTBAR_" + (slot - 36);
      } else {
         return slot == 45 ? "OFFHAND" : "OTHER";
      }
   }

   public static enum Mode {
      Offhand,
      Inventory,
      All;
   }

   public static enum Direction {
      Incoming,
      Outgoing,
      Both;
   }
}
