package fat.vonware.features.modules.player;

import fat.vonware.ducks.IMinecraft;
import fat.vonware.event.events.PacketEvent;
import fat.vonware.features.modules.Module;
import fat.vonware.features.setting.Setting;
import net.minecraft.init.Items;
import net.minecraft.item.ItemExpBottle;
import net.minecraft.item.ItemStack;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.CPacketPlayerTryUseItem;
import net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock;
import net.minecraft.util.EnumHand;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class XpHelper extends Module {
   public final Setting<Integer> delay = new Setting<Integer>("Delay", 0, 0, 5, "Delay in ticks between XpHelper actions.");

   public XpHelper() {
      super("XpHelper", "Helps you with xp", Module.Category.PLAYER, true, false, false);
      this.register(this.delay);
   }

   public String getDisplayInfo() {
      return this.delay.getValue() + "";
   }

   @SubscribeEvent
   public void onPacketSend(PacketEvent.Send event) {
      Packet<?> packet = event.getPacket();
      EnumHand hand;
      if (packet instanceof CPacketPlayerTryUseItem) {
         hand = ((CPacketPlayerTryUseItem)packet).getHand();
      } else {
         if (!(packet instanceof CPacketPlayerTryUseItemOnBlock)) {
            return;
         }

         CPacketPlayerTryUseItemOnBlock blockPacket = (CPacketPlayerTryUseItemOnBlock)packet;
         hand = blockPacket.getHand();
      }

      ItemStack heldItem = mc.player.getHeldItem(hand);
      if (heldItem.getItem() == Items.EXPERIENCE_BOTTLE && packet instanceof CPacketPlayerTryUseItemOnBlock) {
         event.setCanceled(true);
      }

   }

   private boolean isStackValid(ItemStack stack) {
      return stack != null && stack.getItem() instanceof ItemExpBottle;
   }

   @SubscribeEvent
   public void onTick(TickEvent event) {
      if (mc.player != null && mc.world != null) {
         if (mc.gameSettings.keyBindUseItem.isKeyDown() && (Integer)this.delay.getValue() < ((IMinecraft)mc).getRightClickDelay() && (this.isStackValid(mc.player.getHeldItemMainhand()) || this.isStackValid(mc.player.getHeldItemOffhand()))) {
            ((IMinecraft)mc).setRightClickDelay((Integer)this.delay.getValue());
         }

      }
   }
}
