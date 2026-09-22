package fat.vonware.features.modules.player;

import fat.vonware.event.events.PacketEvent;
import fat.vonware.features.modules.Module;
import fat.vonware.features.setting.Setting;
import fat.vonware.mixin.mixins.accessor.ICPacketCloseWindow;
import net.minecraft.item.ItemAir;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.CPacketCloseWindow;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class XCarry extends Module {
   private final Setting<Boolean> always = new Setting<Boolean>("Always", false, "Keeps XCarry active outside inventory screens instead of only while the inventory is open.");

   public XCarry() {
      super("XCarry", "allows you to store items in the crafting slots", Module.Category.PLAYER, true, false, false);
      this.register(this.always);
   }

   @SubscribeEvent
   public void onPacket(PacketEvent event) {
      if (!fullNullCheck()) {
         if (event.getPacket() instanceof CPacketCloseWindow) {
            CPacketCloseWindow packetCloseWindow = (CPacketCloseWindow)event.getPacket();
            if ((Boolean)this.always.getValue()) {
               event.setCanceled(true);
            } else {
               if (this.isAir(1) && this.isAir(2) && this.isAir(3) && this.isAir(4)) {
                  return;
               }

               event.setCanceled(((ICPacketCloseWindow)packetCloseWindow).getWindowId() == 0);
            }
         }

      }
   }

   private boolean isAir(int slot) {
      return ((ItemStack)mc.player.inventoryContainer.getInventory().get(slot)).getItem() instanceof ItemAir;
   }
}
