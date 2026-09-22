package fat.vonware.features.modules.misc;

import fat.vonware.features.modules.Module;
import fat.vonware.util.InventoryUtil;
import fat.vonware.util.Phase.PacketUtil;
import net.minecraft.init.Items;
import net.minecraft.network.play.client.CPacketPlayerTryUseItem;
import net.minecraft.util.EnumHand;

public class KeyPearl extends Module {
   public KeyPearl() {
      super("KeyPearl", "Throw a pearl on keybind", Module.Category.MISC, true, false, false);
   }

   public void onEnable() {
      if (fullNullCheck()) {
         this.disable();
      } else {
         int pearlSlot = InventoryUtil.getItemFromHotbar(Items.ENDER_PEARL);
         int lastSlot = mc.player.inventory.currentItem;
         if (pearlSlot != -1) {
            InventoryUtil.switchTo(pearlSlot);
            PacketUtil.send(new CPacketPlayerTryUseItem(EnumHand.MAIN_HAND));
            InventoryUtil.switchTo(lastSlot);
         }

         this.disable();
      }
   }
}
