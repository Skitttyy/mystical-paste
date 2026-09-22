package fat.vonware.util.FakePlayer;

import net.minecraft.entity.player.EntityPlayer;

public class Position {
   private final double x;
   private final double y;
   private final double z;
   private final float yaw;
   private final float pitch;
   private final float head;
   private final double motionX;
   private final double motionY;
   private final double motionZ;
   private final boolean isSneaking;

   public Position(EntityPlayer player) {
      this.x = player.posX;
      this.y = player.posY;
      this.z = player.posZ;
      this.yaw = player.rotationYaw;
      this.pitch = player.rotationPitch;
      this.head = player.rotationYawHead;
      this.motionX = player.motionX;
      this.motionY = player.motionY;
      this.motionZ = player.motionZ;
      this.isSneaking = player.isSneaking();
   }

   public double getX() {
      return this.x;
   }

   public double getY() {
      return this.y;
   }

   public double getZ() {
      return this.z;
   }

   public float getYaw() {
      return this.yaw;
   }

   public float getPitch() {
      return this.pitch;
   }

   public float getHead() {
      return this.head;
   }

   public double getMotionX() {
      return this.motionX;
   }

   public double getMotionY() {
      return this.motionY;
   }

   public double getMotionZ() {
      return this.motionZ;
   }

   public boolean isSneaking() {
      return this.isSneaking;
   }
}
