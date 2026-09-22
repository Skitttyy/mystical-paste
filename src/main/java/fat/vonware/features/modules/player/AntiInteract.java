package fat.vonware.features.modules.player;

import fat.vonware.event.events.PacketEvent;
import fat.vonware.event.events.Block.PickBlockEvent;
import fat.vonware.features.modules.Module;
import fat.vonware.features.setting.Setting;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemFood;
import net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class AntiInteract extends Module {
   private final Setting<Boolean> onlyFood = new Setting<Boolean>("OnlyFood", false, "Only applies AntiInteract while you are using food.");

   public AntiInteract() {
      super("AntiInteract", "Prevent you from openable tile blocks", Module.Category.PLAYER, true, false, false);
      this.register(this.onlyFood);
   }

   @SubscribeEvent
   public void onPickBlock(PickBlockEvent event) {
      event.setCanceled(true);
   }

   @SubscribeEvent
   public void onBlockInteract(PlayerInteractEvent.RightClickBlock event) {
      if (!fullNullCheck()) {
         if (this.doNot()) {
            Block block = mc.world.getBlockState(event.getPos()).getBlock();
            if (block == Blocks.ANVIL || block == Blocks.ENDER_CHEST) {
               event.setCanceled(true);
            }

         }
      }
   }

   @SubscribeEvent
   public void onPacket(PacketEvent event) {
      if (!fullNullCheck()) {
         if (event.getPacket() instanceof CPacketPlayerTryUseItemOnBlock) {
            if (!this.doNot()) {
               return;
            }

            CPacketPlayerTryUseItemOnBlock packet = (CPacketPlayerTryUseItemOnBlock)event.getPacket();
            Block block = mc.world.getBlockState(packet.getPos()).getBlock();
            if (block == Blocks.ANVIL || block == Blocks.ENDER_CHEST) {
               event.setCanceled(true);
            }
         }

      }
   }

   public boolean doNot() {
      return !(Boolean)this.onlyFood.getValue() || mc.player.getHeldItemMainhand().getItem() instanceof ItemFood;
   }
}
