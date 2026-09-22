package fat.vonware.features.modules.player;

import fat.vonware.event.events.PacketEvent;
import fat.vonware.features.Feature;
import fat.vonware.features.modules.Module;
import fat.vonware.features.setting.Setting;
import fat.vonware.util.InventoryUtil;
import net.minecraft.item.ItemEnderPearl;
import net.minecraft.network.play.client.CPacketEntityAction;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.network.play.client.CPacketPlayerTryUseItem;
import net.minecraft.network.play.client.CPacketEntityAction.Action;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class FastProjectile extends Module {
   private final Setting<Boolean> pearls = new Setting<Boolean>("Pearl", true, "Applies FastProjectile packet acceleration to ender pearls.");
   public final Setting<Integer> packets = new Setting<Integer>("Packets", 20, 1, 300, "Number of extra movement packets sent by FastProjectile.");
   public boolean suppressEvent = false;

   public FastProjectile() {
      super("FastProjectile", "Modify projectile behavior", Module.Category.PLAYER, true, false, false);
      this.register(this.pearls);
      this.register(this.packets);
   }

   @SubscribeEvent
   public void onPacket(PacketEvent event) {
      if (!Feature.fullNullCheck()) {
         if (!this.suppressEvent) {
            if (event.getPacket() instanceof CPacketPlayerTryUseItem) {
               if ((Boolean)this.pearls.getValue() && InventoryUtil.isHolding(ItemEnderPearl.class)) {
                  this.doTheWork();
               }

            }
         }
      }
   }

   public void doTheWork() {
      mc.player.connection.sendPacket(new CPacketEntityAction(mc.player, Action.START_SPRINTING));

      for(int i = 0; i < (Integer)this.packets.getValue(); ++i) {
         mc.player.connection.sendPacket(new CPacketPlayer.PositionRotation(mc.player.posX, mc.player.posY + 1.3E-13, mc.player.posZ, mc.player.rotationYaw, mc.player.rotationPitch, true));
         mc.player.connection.sendPacket(new CPacketPlayer.PositionRotation(mc.player.posX, mc.player.posY + 2.7E-13, mc.player.posZ, mc.player.rotationYaw, mc.player.rotationPitch, false));
      }

   }
}
