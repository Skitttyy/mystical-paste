package fat.vonware.features.modules.movement;

import fat.vonware.features.command.Command;
import fat.vonware.features.modules.Module;
import fat.vonware.features.setting.Setting;
import fat.vonware.util.InventoryUtil;
import java.util.function.Function;
import net.minecraft.init.Items;
import net.minecraft.util.EnumHand;

public class PearlPhase extends Module {
   protected final Setting<SwitchMode> switchMode;
   protected final Setting<RotationMode> rotationMode;
   private final Setting<Integer> throwPitch;
   private final Setting<Boolean> smart;
   private boolean pearlThrown;

   public PearlPhase() {
      super("PearlPhase", "Automatically throws ender pearls for phase", Module.Category.MOVEMENT, true, false, false);
      this.switchMode = (new Setting("SwitchMode", PearlPhase.SwitchMode.Silent)).withDescription((Function<SwitchMode, String>)((mode) -> {
         switch (mode) {
            case Silent:
               return "Silent: Temporarily switches to the pearl slot without leaving the visible hotbar selection changed.";
            case Vanilla:
               return "Vanilla: Uses a normal visible hotbar switch to the pearl slot.";
            default:
               return "";
         }
      }));
      this.rotationMode = (new Setting("Rotations", PearlPhase.RotationMode.Packet)).withDescription((Function<RotationMode, String>)((mode) -> {
         switch (mode) {
            case Packet:
               return "Packet: Sends the pearl throw rotation through packets without directly turning the client view.";
            case Vanilla:
               return "Vanilla: Applies the throw yaw/pitch directly to the player view.";
            default:
               return "";
         }
      }));
      this.throwPitch = new Setting<Integer>("Pitch", 80, 0, 90, "Pitch used when aiming the pearl for the phase throw.");
      this.smart = new Setting<Boolean>("Smart", false, "Adjusts PearlPhase behavior based on the player position/collision state instead of always using the same throw setup.");
      this.pearlThrown = false;
      this.register(this.switchMode);
      this.register(this.rotationMode);
      this.register(this.throwPitch);
      this.register(this.smart);
   }

   public void onEnable() {
      if (mc.player != null && mc.world != null) {
         float[] rotationAngles = new float[]{mc.player.rotationYaw, (float)(Integer)this.throwPitch.getValue()};
         InventoryUtil.PacketRotation(rotationAngles);
         if (this.switchMode.getValue() == PearlPhase.SwitchMode.Vanilla) {
            this.swapToPearlAndThrow();
         } else if (this.switchMode.getValue() == PearlPhase.SwitchMode.Silent) {
            this.silentSwitchAndThrow();
         } else {
            this.throwPearl();
         }

         if (!(Boolean)this.smart.getValue()) {
            this.disable();
         }

      }
   }

   public void onUpdate() {
      if ((Boolean)this.smart.getValue()) {
         if (mc.player.rotationPitch >= (float)(Integer)this.throwPitch.getValue() && !this.pearlThrown) {
            if (this.switchMode.getValue() == PearlPhase.SwitchMode.Vanilla) {
               this.swapToPearlAndThrow();
            } else if (this.switchMode.getValue() == PearlPhase.SwitchMode.Silent) {
               this.silentSwitchAndThrow();
            } else {
               this.throwPearl();
            }

            this.pearlThrown = true;
         } else if (mc.player.rotationPitch < (float)(Integer)this.throwPitch.getValue()) {
            this.pearlThrown = false;
         }
      }

   }

   private void throwPearl() {
      if (mc.player.getHeldItem(EnumHand.MAIN_HAND).getItem() == Items.ENDER_PEARL) {
         mc.playerController.processRightClick(mc.player, mc.world, EnumHand.MAIN_HAND);
      } else if (mc.player.getHeldItem(EnumHand.OFF_HAND).getItem() == Items.ENDER_PEARL) {
         mc.playerController.processRightClick(mc.player, mc.world, EnumHand.OFF_HAND);
      } else {
         Command.sendSilentMessage("No ender pearl found in hand.");
      }

   }

   private void swapToPearlAndThrow() {
      int currentSlot = mc.player.inventory.currentItem;
      int pearlSlot = this.findPearlInHotbar();
      if (pearlSlot != -1) {
         InventoryUtil.SilentSwitch(pearlSlot);
         this.throwPearl();
         InventoryUtil.SilentSwitch(currentSlot);
      } else {
         Command.sendSilentMessage("No ender pearl found in inventory.");
      }

   }

   private void silentSwitchAndThrow() {
      int currentSlot = mc.player.inventory.currentItem;
      int pearlSlot = this.findPearlInHotbar();
      if (pearlSlot != -1) {
         InventoryUtil.SilentSwitch(pearlSlot);
         this.throwPearl();
         InventoryUtil.SilentSwitch(currentSlot);
      } else {
         Command.sendSilentMessage("No ender pearl found in inventory.");
      }

   }

   private int findPearlInHotbar() {
      for(int i = 0; i < 9; ++i) {
         if (mc.player.inventory.getStackInSlot(i).getItem() == Items.ENDER_PEARL) {
            return i;
         }
      }

      return -1;
   }

   public static enum SwitchMode {
      Silent,
      Vanilla;
   }

   public static enum RotationMode {
      Packet,
      Vanilla;
   }
}
