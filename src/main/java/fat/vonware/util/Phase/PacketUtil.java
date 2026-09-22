package fat.vonware.util.Phase;

import fat.vonware.Vonware;
import fat.vonware.ducks.INetHandlerPlayClient;
import fat.vonware.ducks.INetworkManager;
import fat.vonware.mixin.mixins.accessor.ICPacketUseEntity;
import fat.vonware.util.MovementUtil;
import fat.vonware.util.Util;
import io.netty.util.concurrent.GenericFutureListener;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.entity.Entity;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.CPacketAnimation;
import net.minecraft.network.play.client.CPacketConfirmTeleport;
import net.minecraft.network.play.client.CPacketEntityAction;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.network.play.client.CPacketUseEntity;
import net.minecraft.network.play.client.CPacketUseEntity.Action;
import net.minecraft.network.play.server.SPacketDisconnect;
import net.minecraft.network.play.server.SPacketPlayerPosLook;
import net.minecraft.network.play.server.SPacketPlayerPosLook.EnumFlags;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextComponentString;

public class PacketUtil implements Util {
   public static List<Packet<?>> noEventPackets = new ArrayList();

   public static void send(Packet<?> packet) {
      NetHandlerPlayClient connection = mc.getConnection();
      if (connection != null) {
         connection.sendPacket(packet);
      }

   }

   public static CPacketUseEntity attackPacket(int id) {
      CPacketUseEntity packet = new CPacketUseEntity();
      ((ICPacketUseEntity)packet).setEntityId(id);
      ((ICPacketUseEntity)packet).setAction(Action.ATTACK);
      return packet;
   }

   public static CPacketUseEntity attackPacket(Entity entity) {
      CPacketUseEntity packet = new CPacketUseEntity();
      ((ICPacketUseEntity)packet).setEntityId(entity.getEntityId());
      ((ICPacketUseEntity)packet).setAction(Action.ATTACK);
      return packet;
   }

   public static void sneak(boolean sneak) {
      send(new CPacketEntityAction(mc.player, sneak ? net.minecraft.network.play.client.CPacketEntityAction.Action.START_SNEAKING : net.minecraft.network.play.client.CPacketEntityAction.Action.STOP_SNEAKING));
   }

   public static void disconnect(String message) {
      if (mc.getConnection() != null) {
         mc.getConnection().handleDisconnect(new SPacketDisconnect(new TextComponentString(message)));
      }

   }

   public static void instantSend(Packet<?> packet) {
      if (mc.player.connection != null) {
         ((INetworkManager)mc.player.connection.getNetworkManager()).sendFast(packet, (GenericFutureListener[])null);
      }
   }

   public static void sendNoEventPacket(Packet<?> packet) {
      noEventPackets.add(packet);
      mc.player.connection.sendPacket(packet);
   }

   public static void sendNoEvent(Packet<?> packet) {
      NetHandlerPlayClient connection = mc.getConnection();
      if (connection != null) {
         INetworkManager manager = (INetworkManager)connection.getNetworkManager();
         manager.sendPacketNoEvent(packet);
      }

   }

   public static void swing() {
      swing(EnumHand.MAIN_HAND);
   }

   public static void swing(EnumHand hand) {
      send(new CPacketAnimation(hand));
   }

   public static void handlePosLook(SPacketPlayerPosLook packetIn, Entity entity, boolean noRotate) {
      handlePosLook(packetIn, entity, noRotate, false);
   }

   public static void handlePosLook(SPacketPlayerPosLook packet, Entity entity, boolean noRotate, boolean event) {
      double x = packet.getX();
      double y = packet.getY();
      double z = packet.getZ();
      float yaw = packet.getYaw();
      float pitch = packet.getPitch();
      if (packet.getFlags().contains(EnumFlags.X)) {
         x += entity.posX;
      } else {
         entity.motionX = (double)0.0F;
      }

      if (packet.getFlags().contains(EnumFlags.Y)) {
         y += entity.posY;
      } else {
         entity.motionY = (double)0.0F;
      }

      if (packet.getFlags().contains(EnumFlags.Z)) {
         z += entity.posZ;
      } else {
         entity.motionZ = (double)0.0F;
      }

      if (packet.getFlags().contains(EnumFlags.X_ROT)) {
         pitch += entity.rotationPitch;
      }

      if (packet.getFlags().contains(EnumFlags.Y_ROT)) {
         yaw += entity.rotationYaw;
      }

      entity.setPositionAndRotation(x, y, z, noRotate ? entity.rotationYaw : yaw, noRotate ? entity.rotationPitch : pitch);
      Packet<?> confirm = new CPacketConfirmTeleport(packet.getTeleportId());
      CPacketPlayer posRot = positionRotation(entity.posX, entity.getEntityBoundingBox().minY, entity.posZ, yaw, pitch, false);
      if (event) {
         send(confirm);
         Vonware.rotationManager.setBlocking(true);
         send(posRot);
         Vonware.rotationManager.setBlocking(false);
      } else {
         sendNoEventPacket(confirm);
         sendNoEventPacket(posRot);
      }

      mc.addScheduledTask(PacketUtil::loadTerrain);
   }

   public static CPacketPlayer positionRotation(double x, double y, double z, float yaw, float pitch, boolean onGround) {
      return new CPacketPlayer.PositionRotation(x, y, z, yaw, pitch, onGround);
   }

   public static void loadTerrain() {
      mc.addScheduledTask(() -> {
         if (!((INetHandlerPlayClient)mc.player.connection).isDoneLoadingTerrain()) {
            mc.player.prevPosX = mc.player.posX;
            mc.player.prevPosY = mc.player.posY;
            mc.player.prevPosZ = mc.player.posZ;
            ((INetHandlerPlayClient)mc.player.connection).setDoneLoadingTerrain(true);
            mc.displayGuiScreen((GuiScreen)null);
         }

      });
   }

   public static void rotate(float[] rotations) {
      rotate(rotations, mc.player.onGround);
   }

   public static void rotate(float[] rotations, boolean ground) {
      boolean isMoving = MovementUtil.isMoving();
      CPacketPlayer packet = (CPacketPlayer)(isMoving ? new CPacketPlayer.PositionRotation(mc.player.posX, mc.player.posY, mc.player.posZ, rotations[0], rotations[1], ground) : new CPacketPlayer.Rotation(rotations[0], rotations[1], ground));
      send(packet);
   }
}
