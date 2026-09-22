package fat.vonware.manager;

import fat.vonware.features.Feature;
import java.util.HashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;

public class SpeedManager extends Feature {
   private static final Minecraft mc = Minecraft.getMinecraft();
   private static final double TICKS_PER_SECOND = (double)20.0F;
   private static final double SECONDS_PER_HOUR = (double)3600.0F;
   private static final double MIN_MOVEMENT_THRESHOLD = 0.001;
   public static boolean didJumpThisTick = false;
   public static boolean isJumping = false;
   private final int trackingDistance = 20;
   private final HashMap<EntityPlayer, Double> playerSpeeds = new HashMap();
   public double currentSpeed = (double)0.0F;
   public double displayedSpeed = (double)0.0F;
   public double lastJumpSpeed = (double)0.0F;
   public double percentJumpSpeedChanged = (double)0.0F;
   public double jumpSpeedChanged = (double)0.0F;
   public boolean didJumpLastTick = false;
   public long jumpInfoStartTime = 0L;
   private static final double SMOOTH_FACTOR = 0.15;

   public static void setDidJumpThisTick(boolean val) {
      didJumpThisTick = val;
   }

   public static void setIsJumping(boolean val) {
      isJumping = val;
   }

   private double getHorizontalDistance(double x1, double z1, double x2, double z2) {
      double dx = x1 - x2;
      double dz = z1 - z2;
      return Math.sqrt(dx * dx + dz * dz);
   }

   public void update() {
      if (mc.player != null && mc.world != null) {
         double distance = this.getHorizontalDistance(mc.player.posX, mc.player.posZ, mc.player.prevPosX, mc.player.prevPosZ);
         if (distance < 0.001) {
            distance = (double)0.0F;
         }

         double targetSpeed = this.blocksPerTickToKmh(distance);
         this.displayedSpeed += (targetSpeed - this.displayedSpeed) * 0.15;
         this.currentSpeed = targetSpeed;
         if (didJumpThisTick && (!mc.player.onGround || isJumping)) {
            if (!this.didJumpLastTick) {
               this.jumpSpeedChanged = this.currentSpeed - this.lastJumpSpeed;
               this.percentJumpSpeedChanged = this.lastJumpSpeed == (double)0.0F ? (double)0.0F : this.currentSpeed / this.lastJumpSpeed - (double)1.0F;
               this.jumpInfoStartTime = Minecraft.getSystemTime();
               this.lastJumpSpeed = this.currentSpeed;
            }

            this.didJumpLastTick = true;
         } else {
            this.didJumpLastTick = false;
         }

         this.updateNearbyPlayerSpeeds();
      }
   }

   private void updateNearbyPlayerSpeeds() {
      this.playerSpeeds.clear();

      for(EntityPlayer player : mc.world.playerEntities) {
         if (player != mc.player && !(mc.player.getDistanceSq(player) > (double)400.0F)) {
            double distance = this.getHorizontalDistance(player.posX, player.posZ, player.prevPosX, player.prevPosZ);
            if (distance < 0.001) {
               distance = (double)0.0F;
            }

            this.playerSpeeds.put(player, this.blocksPerTickToKmh(distance));
         }
      }

   }

   public double getPlayerSpeed(EntityPlayer player) {
      return (Double)this.playerSpeeds.getOrDefault(player, (double)0.0F);
   }

   public double getSpeedKmh() {
      return this.round(this.displayedSpeed, 1);
   }

   public String getFormattedSpeed() {
      return String.format("%.1f km/h", this.getSpeedKmh());
   }

   public double getSpeedMps() {
      return this.round(this.displayedSpeed / 3.6, 1);
   }

   private double blocksPerTickToKmh(double blocksPerTick) {
      return blocksPerTick * (double)20.0F * 3.6;
   }

   private double round(double value, int precision) {
      double scale = Math.pow((double)10.0F, (double)precision);
      return (double)Math.round(value * scale) / scale;
   }
}
