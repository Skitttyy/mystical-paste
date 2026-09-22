package fat.vonware.manager;

import fat.vonware.event.events.PacketEvent;
import fat.vonware.features.Feature;
import net.minecraft.entity.Entity;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.network.play.server.SPacketPlayerPosLook;
import net.minecraft.network.play.server.SPacketPlayerPosLook.EnumFlags;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class PositionManager extends Feature {
   public static PositionManager INSTANCE;
   private volatile double serverX;
   private volatile double serverY;
   private volatile double serverZ;
   private volatile boolean onGround;
   private volatile int teleportID;
   private boolean blocking;

   public void init() {
      MinecraftForge.EVENT_BUS.register(this);
   }

   public void onUnload() {
      MinecraftForge.EVENT_BUS.unregister(this);
   }

   public PositionManager() {
      INSTANCE = this;
   }

   @SubscribeEvent
   public void onPacketPost(PacketEvent.Post event) {
      if (event.getPacket() instanceof CPacketPlayer.Position || event.getPacket() instanceof CPacketPlayer.PositionRotation) {
         this.readCPacket((CPacketPlayer)event.getPacket());
      }

   }

   @SubscribeEvent
   public void onPacketReceive(PacketEvent.Receive event) {
      if (event.getPacket() instanceof SPacketPlayerPosLook && mc.player != null) {
         SPacketPlayerPosLook packet = (SPacketPlayerPosLook)event.getPacket();
         double x = packet.getX();
         double y = packet.getY();
         double z = packet.getZ();
         if (packet.getFlags().contains(EnumFlags.X)) {
            x += mc.player.posX;
         }

         if (packet.getFlags().contains(EnumFlags.Y)) {
            y += mc.player.posY;
         }

         if (packet.getFlags().contains(EnumFlags.Z)) {
            z += mc.player.posZ;
         }

         this.serverX = MathHelper.clamp(x, (double)-3.0E7F, (double)3.0E7F);
         this.serverY = y;
         this.serverZ = MathHelper.clamp(z, (double)-3.0E7F, (double)3.0E7F);
         this.onGround = false;
         this.teleportID = packet.getTeleportId();
      }
   }

   public void readCPacket(CPacketPlayer packet) {
      if (mc.player != null) {
         this.serverX = packet.getX(mc.player.posX);
         this.serverY = packet.getY(mc.player.posY);
         this.serverZ = packet.getZ(mc.player.posZ);
         this.setOnGround(packet.isOnGround());
      }
   }

   public double getDistanceSq(Entity entity) {
      return this.getDistanceSq(entity.posX, entity.posY, entity.posZ);
   }

   public double getDistanceSq(double x, double y, double z) {
      double xDiff = this.serverX - x;
      double yDiff = this.serverY - y;
      double zDiff = this.serverZ - z;
      return xDiff * xDiff + yDiff * yDiff + zDiff * zDiff;
   }

   public double getDistanceSqEyeHeight(double x, double y, double z) {
      double xDiff = this.serverX - x;
      double yDiff = this.serverY + (double)mc.player.getEyeHeight() - y;
      double zDiff = this.serverZ - z;
      return xDiff * xDiff + yDiff * yDiff + zDiff * zDiff;
   }

   public double getDistance(Entity entity) {
      return Math.sqrt(this.getDistanceSq(entity));
   }

   public double getDistance(double x, double y, double z) {
      return Math.sqrt(this.getDistanceSq(x, y, z));
   }

   public boolean canEntityBeSeen(Entity entity) {
      Vec3d eyePos = new Vec3d(this.serverX, this.serverY + (double)mc.player.getEyeHeight(), this.serverZ);
      Vec3d entityPos = new Vec3d(entity.posX, entity.posY + (double)entity.getEyeHeight(), entity.posZ);
      return mc.world.rayTraceBlocks(eyePos, entityPos, false, true, false) == null;
   }

   public AxisAlignedBB getBB() {
      float width = mc.player.width / 2.0F;
      float height = mc.player.height;
      return new AxisAlignedBB(this.serverX - (double)width, this.serverY, this.serverZ - (double)width, this.serverX + (double)width, this.serverY + (double)height, this.serverZ + (double)width);
   }

   public Vec3d getVec() {
      return new Vec3d(this.serverX, this.serverY, this.serverZ);
   }

   public Vec3d getEyeVec() {
      return new Vec3d(this.serverX, this.serverY + (double)mc.player.getEyeHeight(), this.serverZ);
   }

   public double getX() {
      return this.serverX;
   }

   public double getY() {
      return this.serverY;
   }

   public double getZ() {
      return this.serverZ;
   }

   public boolean isOnGround() {
      return this.onGround;
   }

   public int getTeleportID() {
      return this.teleportID;
   }

   public void set(double x, double y, double z) {
      this.serverX = x;
      this.serverY = y;
      this.serverZ = z;
   }

   public void setPosition(double x, double y, double z) {
      this.serverX = x;
      this.serverY = y;
      this.serverZ = z;
   }

   public void setOnGround(boolean onGround) {
      this.onGround = onGround;
   }

   public void setBlocking(boolean blocking) {
      this.blocking = blocking;
   }

   public boolean isBlocking() {
      return this.blocking;
   }

   public void reset() {
      if (mc.player != null) {
         this.serverX = mc.player.posX;
         this.serverY = mc.player.posY;
         this.serverZ = mc.player.posZ;
         this.onGround = mc.player.onGround;
      }

   }
}
