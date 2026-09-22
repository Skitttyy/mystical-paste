package fat.vonware.util;

import fat.vonware.event.events.MoveEvent;
import java.util.Objects;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.MovementInput;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.input.Keyboard;

public class MovementUtil implements Util {
   public static boolean wasdKeys() {
      return mc.player.movementInput.forwardKeyDown || mc.player.movementInput.backKeyDown || mc.player.movementInput.leftKeyDown || mc.player.movementInput.rightKeyDown;
   }

   public static boolean hasMotion(Entity entity) {
      return entity.posX != entity.prevPosX || entity.posY != entity.prevPosY || entity.posZ != entity.prevPosZ;
   }

   public static boolean isPhasing() {
      return isPhasing((EntityPlayer)EntityUtil.mc.player);
   }

   public static boolean isPhasing(EntityPlayer player) {
      return isPhasing(player.getEntityBoundingBox());
   }

   public static boolean isPhasing(AxisAlignedBB bb) {
      for(int x = MathHelper.floor(bb.minX); x < MathHelper.floor(bb.maxX) + 1; ++x) {
         for(int y = MathHelper.floor(bb.minY); y < MathHelper.floor(bb.maxY) + 1; ++y) {
            for(int z = MathHelper.floor(bb.minZ); z < MathHelper.floor(bb.maxZ) + 1; ++z) {
               BlockPos pos = new BlockPos(x, y, z);
               IBlockState state = EntityUtil.mc.world.getBlockState(pos);
               AxisAlignedBB blockBB = state.getSelectedBoundingBox(EntityUtil.mc.world, pos);
               if (state.getMaterial().blocksMovement() && bb.intersects(blockBB)) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   public static double getSpeed() {
      return getSpeed(false);
   }

   public static boolean isJumping() {
      return mc.gameSettings.keyBindJump.isKeyDown() || Keyboard.isKeyDown(mc.gameSettings.keyBindJump.getKeyCode()) && !(mc.currentScreen instanceof GuiChat);
   }

   public static double getStrictBaseSpeed(Double baseSpeed) {
      double maxModifier = baseSpeed;
      if (Minecraft.getMinecraft().player.isPotionActive((Potion)Objects.requireNonNull(Potion.getPotionById(1)))) {
         maxModifier *= (double)1.0F + 0.1 * (double)(((PotionEffect)Objects.requireNonNull(Minecraft.getMinecraft().player.getActivePotionEffect((Potion)Objects.requireNonNull(Potion.getPotionById(1))))).getAmplifier() + 1);
      }

      return maxModifier;
   }

   public static boolean isActuallyMoving(EntityLivingBase entity) {
      return entity.moveForward != 0.0F || entity.moveStrafing != 0.0F || mc.player.motionY != (double)0.0F;
   }

   public static boolean isMoving() {
      return (double)mc.player.moveForward != (double)0.0F || (double)mc.player.moveStrafing != (double)0.0F;
   }

   public static double getDistance2D() {
      double xDist = mc.player.posX - mc.player.prevPosX;
      double zDist = mc.player.posZ - mc.player.prevPosZ;
      return Math.sqrt(xDist * xDist + zDist * zDist);
   }

   public static void strafe(MoveEvent event, double speed) {
      if (isMoving()) {
         double[] strafe = strafe(speed);
         event.setX(strafe[0]);
         event.setZ(strafe[1]);
      } else {
         event.setX((double)0.0F);
         event.setZ((double)0.0F);
      }

   }

   public static double[] strafe(double speed) {
      return strafe((Entity)mc.player, speed);
   }

   public static double[] strafe(Entity entity, double speed) {
      return strafe(entity, mc.player.movementInput, speed);
   }

   public static double[] strafe(Entity entity, MovementInput movementInput, double speed) {
      float moveForward = movementInput.moveForward;
      float moveStrafe = movementInput.moveStrafe;
      float rotationYaw = entity.prevRotationYaw + (entity.rotationYaw - entity.prevRotationYaw) * mc.getRenderPartialTicks();
      if (moveForward != 0.0F) {
         if (moveStrafe > 0.0F) {
            rotationYaw += (float)(moveForward > 0.0F ? -45 : 45);
         } else if (moveStrafe < 0.0F) {
            rotationYaw += (float)(moveForward > 0.0F ? 45 : -45);
         }

         moveStrafe = 0.0F;
         if (moveForward > 0.0F) {
            moveForward = 1.0F;
         } else if (moveForward < 0.0F) {
            moveForward = -1.0F;
         }
      }

      double posX = (double)moveForward * speed * -Math.sin(Math.toRadians((double)rotationYaw)) + (double)moveStrafe * speed * Math.cos(Math.toRadians((double)rotationYaw));
      double posZ = (double)moveForward * speed * Math.cos(Math.toRadians((double)rotationYaw)) - (double)moveStrafe * speed * -Math.sin(Math.toRadians((double)rotationYaw));
      return new double[]{posX, posZ};
   }

   public static double[] directionSpeed(double speed) {
      float forward = mc.player.movementInput.moveForward;
      float side = mc.player.movementInput.moveStrafe;
      float yaw = mc.player.prevRotationYaw + (mc.player.rotationYaw - mc.player.prevRotationYaw) * mc.getRenderPartialTicks();
      if (forward != 0.0F) {
         if (side > 0.0F) {
            yaw += (float)(forward > 0.0F ? -45 : 45);
         } else if (side < 0.0F) {
            yaw += (float)(forward > 0.0F ? 45 : -45);
         }

         side = 0.0F;
         if (forward > 0.0F) {
            forward = 1.0F;
         } else if (forward < 0.0F) {
            forward = -1.0F;
         }
      }

      double sin = Math.sin(Math.toRadians((double)(yaw + 90.0F)));
      double cos = Math.cos(Math.toRadians((double)(yaw + 90.0F)));
      double posX = (double)forward * speed * cos + (double)side * speed * sin;
      double posZ = (double)forward * speed * sin - (double)side * speed * cos;
      return new double[]{posX, posZ};
   }

   public static double getSpeed(boolean slowness) {
      double defaultSpeed = 0.2873;
      if (mc.player.isPotionActive(MobEffects.SPEED)) {
         int amplifier = ((PotionEffect)Objects.requireNonNull(mc.player.getActivePotionEffect(MobEffects.SPEED))).getAmplifier();
         defaultSpeed *= (double)1.0F + 0.2 * (double)(amplifier + 1);
      }

      if (slowness && mc.player.isPotionActive(MobEffects.SLOWNESS)) {
         int amplifier = ((PotionEffect)Objects.requireNonNull(mc.player.getActivePotionEffect(MobEffects.SLOWNESS))).getAmplifier();
         defaultSpeed /= (double)1.0F + 0.2 * (double)(amplifier + 1);
      }

      return defaultSpeed;
   }

   public static double getSpeed(boolean slowness, double defaultSpeed) {
      if (mc.player.isPotionActive(MobEffects.SPEED)) {
         int amplifier = ((PotionEffect)Objects.requireNonNull(mc.player.getActivePotionEffect(MobEffects.SPEED))).getAmplifier();
         defaultSpeed *= (double)1.0F + 0.2 * (double)(amplifier + 1);
      }

      if (slowness && mc.player.isPotionActive(MobEffects.SLOWNESS)) {
         int amplifier = ((PotionEffect)Objects.requireNonNull(mc.player.getActivePotionEffect(MobEffects.SLOWNESS))).getAmplifier();
         defaultSpeed /= (double)1.0F + 0.2 * (double)(amplifier + 1);
      }

      return defaultSpeed;
   }

   public static double getJumpSpeed() {
      double defaultSpeed = (double)0.0F;
      if (mc.player.isPotionActive(MobEffects.JUMP_BOOST)) {
         int amplifier = ((PotionEffect)Objects.requireNonNull(mc.player.getActivePotionEffect(MobEffects.JUMP_BOOST))).getAmplifier();
         defaultSpeed += (double)(amplifier + 1) * 0.1;
      }

      return defaultSpeed;
   }
}
