package fat.vonware.features.modules.movement;

import com.mojang.realmsclient.gui.ChatFormatting;
import fat.vonware.event.events.PacketEvent;
import fat.vonware.event.events.PushEvent;
import fat.vonware.event.events.PushOutOfBlocksEvent;
import fat.vonware.features.Feature;
import fat.vonware.features.modules.Module;
import fat.vonware.features.setting.Setting;
import fat.vonware.mixin.mixins.accessor.ISPacketEntityVelocity;
import fat.vonware.mixin.mixins.accessor.ISPacketExplosion;
import fat.vonware.util.MathUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.EntityFishHook;
import net.minecraft.network.play.server.SPacketEntityStatus;
import net.minecraft.network.play.server.SPacketEntityVelocity;
import net.minecraft.network.play.server.SPacketExplosion;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class Velocity extends Module {
   private final Setting<Integer> horizontal = new Setting<Integer>("Horizontal", 0, 0, 100);
   private final Setting<Integer> vertical = new Setting<Integer>("Vertical", 0, 0, 100);
   private final Setting<Boolean> noWaterPush = new Setting<Boolean>("NoLiquidPush", true);
   private final Setting<Boolean> noEntityPush = new Setting<Boolean>("NoEntityPush", true);
   private final Setting<Boolean> noBlockPush = new Setting<Boolean>("NoBlockPush", true);
   private final Setting<Boolean> noFishingRod = new Setting<Boolean>("NoBobbers", true);

   public Velocity() {
      super("Velocity", "Anti KnockBack", Module.Category.MOVEMENT, true, false, false);
      this.register(this.horizontal);
      this.register(this.vertical);
      this.register(this.noWaterPush);
      this.register(this.noEntityPush);
      this.register(this.noBlockPush);
      this.register(this.noFishingRod);
   }

   @SubscribeEvent
   public void onPacket(PacketEvent event) {
      if (!Feature.fullNullCheck()) {
         if (event.getPacket() instanceof SPacketEntityVelocity) {
            SPacketEntityVelocity packet = (SPacketEntityVelocity)event.getPacket();
            if (packet.getEntityID() != mc.player.getEntityId()) {
               return;
            }

            if ((Integer)this.horizontal.getValue() == 0 && (Integer)this.vertical.getValue() == 0) {
               event.setCanceled(true);
               return;
            }

            ISPacketEntityVelocity inter = (ISPacketEntityVelocity)packet;
            inter.setMotionX(packet.getMotionX() * (Integer)this.horizontal.getValue() / 100);
            inter.setMotionY(packet.getMotionY() * (Integer)this.vertical.getValue() / 100);
            inter.setMotionZ(packet.getMotionZ() * (Integer)this.horizontal.getValue() / 100);
         }

         if (event.getPacket() instanceof SPacketExplosion) {
            SPacketExplosion packet = (SPacketExplosion)event.getPacket();
            if ((Integer)this.horizontal.getValue() == 0 && (Integer)this.vertical.getValue() == 0) {
               event.setCanceled(true);
               return;
            }

            ISPacketExplosion inter = (ISPacketExplosion)packet;
            inter.setMotionX(packet.getMotionX() * (float)(Integer)this.horizontal.getValue() / 100.0F);
            inter.setMotionY(packet.getMotionY() * (float)(Integer)this.vertical.getValue() / 100.0F);
            inter.setMotionZ(packet.getMotionZ() * (float)(Integer)this.horizontal.getValue() / 100.0F);
         }

         if (event.getPacket() instanceof SPacketEntityStatus && (Boolean)this.noFishingRod.getValue()) {
            SPacketEntityStatus packet = (SPacketEntityStatus)event.getPacket();
            if (packet.getOpCode() == 31) {
               Entity entity = packet.getEntity(Minecraft.getMinecraft().world);
               if (entity instanceof EntityFishHook) {
                  EntityFishHook fishHook = (EntityFishHook)entity;
                  if (fishHook.caughtEntity == mc.player) {
                     event.setCanceled(true);
                  }
               }
            }
         }

      }
   }

   @SubscribeEvent
   public void onPushOutOfBlocks(PushOutOfBlocksEvent event) {
      if ((Boolean)this.noBlockPush.getValue()) {
         event.setCanceled(true);
      }

   }

   @SubscribeEvent
   public void onPushWater(PushEvent event) {
      if (event.type == 1) {
         if ((Boolean)this.noWaterPush.getValue() && event.entity == mc.player) {
            event.setCanceled(true);
         }
      } else if (event.type == 2 && (Boolean)this.noEntityPush.getValue() && event.entity == mc.player) {
         event.setCanceled(true);
      }

   }

   public String getDisplayInfo() {
      return "H" + MathUtil.round((double)((Number)this.horizontal.getValue()).floatValue(), 1) + "%" + ChatFormatting.WHITE + "V" + MathUtil.round((double)((Number)this.vertical.getValue()).floatValue(), 1) + "%";
   }
}
