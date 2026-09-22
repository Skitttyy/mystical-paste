package fat.vonware.util;

import fat.vonware.Vonware;
import fat.vonware.util.holeesp.HoleUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class PlayerUtil implements Util {
   public static Entity getTargetEntity(double range) {
      EntityPlayer target = null;
      double closestDistance = range;

      for(Entity entity : mc.world.loadedEntityList) {
         if (entity instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer)entity;
            if (player != mc.player && EntityUtil.isAlive(player) && !Vonware.friendManager.isFriend(player.getName())) {
               double distance = (double)mc.player.getDistance(player);
               if (!(distance > range) && !isInBlock(player) && !HoleUtil.isHole(EntityUtil.getOtherPlayerPos(player)) && distance < closestDistance) {
                  closestDistance = distance;
                  target = player;
               }
            }
         }
      }

      return target;
   }

   public static boolean isInBlock(EntityPlayer entityPlayer) {
      BlockPos blockPos = new BlockPos(Math.floor(entityPlayer.posX), Math.floor(entityPlayer.posY + 0.2), Math.floor(entityPlayer.posZ));
      return mc.world.getBlockState(blockPos).getBlock() != Blocks.AIR;
   }

   public static Vec3d getEyePos(Entity entity) {
      return new Vec3d(entity.posX, getEyeHeight(entity), entity.posZ);
   }

   public static boolean isMoving() {
      return mc.player.moveForward != 0.0F || mc.player.moveStrafing != 0.0F;
   }

   public static double getEyeHeight() {
      return getEyeHeight(mc.player);
   }

   public static double getEyeHeight(Entity entity) {
      return entity.posY + (double)entity.getEyeHeight();
   }

   public static void setTimer(float speed) {
      Minecraft.getMinecraft().timer.tickLength = 50.0F / speed;
   }

   public static void resetTimer() {
      Minecraft.getMinecraft().timer.tickLength = 50.0F;
   }

   public static boolean isCreative() {
      return isCreative(mc.player);
   }

   public static boolean isCreative(EntityPlayer player) {
      return player != null && (player.isCreative() || player.capabilities.isCreativeMode);
   }

   public static boolean isSpectator() {
      return isSpectator(mc.player);
   }

   public static boolean isSpectator(EntityPlayer player) {
      return player != null && player.isSpectator();
   }

   public static BlockPos getPlayerPos() {
      double decimalPoint = mc.player.posY - Math.floor(mc.player.posY);
      return new BlockPos(mc.player.posX, decimalPoint > 0.8 ? Math.floor(mc.player.posY) + (double)1.0F : Math.floor(mc.player.posY), mc.player.posZ);
   }
}
