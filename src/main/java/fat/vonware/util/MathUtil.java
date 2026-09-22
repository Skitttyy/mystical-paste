package fat.vonware.util;

import com.mojang.authlib.GameProfile;
import fat.vonware.util.OtherRenderShit.ca.PredictPlayer;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Calendar;
import java.util.UUID;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.RayTraceResult.Type;

public class MathUtil implements Util {
   public static float rad(float angle) {
      return (float)((double)angle * Math.PI / (double)180.0F);
   }

   public static float fixedNametagScaling(float scale) {
      return scale * 0.01F;
   }

   public static double round(double value, int places) {
      if (places < 0) {
         throw new IllegalArgumentException();
      } else {
         BigDecimal bd = BigDecimal.valueOf(value);
         bd = bd.setScale(places, RoundingMode.FLOOR);
         return bd.doubleValue();
      }
   }

   public static double getDistance(double posX, double posY, double posZ) {
      double x = mc.player.posX - posX;
      double y = mc.player.posY - posY;
      double z = mc.player.posZ - posZ;
      return (double)MathHelper.sqrt(x * x + y * y + z * z);
   }

   public static double getDistance(Entity entity) {
      double x = mc.player.posX - entity.posX;
      double y = mc.player.posY - entity.posY;
      double z = mc.player.posZ - entity.posZ;
      return (double)MathHelper.sqrt(x * x + y * y + z * z);
   }

   public static int clamp(int num, int min, int max) {
      return num < min ? min : Math.min(num, max);
   }

   public static float clamp(float num, float min, float max) {
      return num < min ? min : Math.min(num, max);
   }

   public static double clamp(double num, double min, double max) {
      return num < min ? min : Math.min(num, max);
   }

   public static double square(double input) {
      return input * input;
   }

   public static PredictPlayer predictPlayer(EntityPlayer entity, int ticks) {
      if (entity == null) {
         return null;
      } else {
         double[] posVec = new double[]{entity.posX, entity.posY, entity.posZ};
         double motionX = entity.posX - entity.prevPosX;
         double motionY = entity.posY - entity.prevPosY;
         double motionZ = entity.posZ - entity.prevPosZ;
         boolean goingUp = false;
         boolean start = true;
         boolean isHole = false;

         for(int i = 0; i < ticks; ++i) {
            double[] newPosVec = posVec.clone();
            newPosVec[0] += motionX;
            RayTraceResult result = mc.world.rayTraceBlocks(new Vec3d(posVec[0], posVec[1], posVec[2]), new Vec3d(newPosVec[0], posVec[1], posVec[2]));
            if (result == null || result.typeOfHit == Type.ENTITY) {
               posVec = newPosVec.clone();
            }

            newPosVec = posVec.clone();
            newPosVec[2] += motionZ;
            result = mc.world.rayTraceBlocks(new Vec3d(posVec[0], posVec[1], posVec[2]), new Vec3d(newPosVec[0], posVec[1], newPosVec[2]));
            if (result == null || result.typeOfHit == Type.ENTITY) {
               posVec = newPosVec.clone();
            }

            if (!isHole) {
               newPosVec = posVec.clone();
               if (!entity.onGround && motionY != -0.0784000015258789 && motionY != (double)0.0F) {
                  double decreasePow = (double)4.0F / Math.pow((double)10.0F, (double)1.0F);
                  if (start) {
                     if (motionY == (double)0.0F) {
                        motionY = decreasePow;
                     }

                     start = false;
                  }

                  float increasePowY = 0.1F;
                  float decreasePowY = 0.5F;
                  motionY += goingUp ? (double)increasePowY : (double)decreasePowY;
                  if (Math.abs(motionY) > decreasePow) {
                     goingUp = false;
                     motionY = (double)decreasePowY;
                  }

                  newPosVec[1] += (double)(goingUp ? 1 : -1) * motionY;
                  result = mc.world.rayTraceBlocks(new Vec3d(posVec[0], posVec[1], posVec[2]), new Vec3d(newPosVec[0], newPosVec[1], newPosVec[2]));
                  if (result != null && result.typeOfHit != Type.ENTITY) {
                     if (!goingUp) {
                        goingUp = true;
                        newPosVec[1] += (double)increasePowY;
                        motionY = (double)increasePowY;
                        newPosVec[1] += motionY;
                     }
                  } else {
                     posVec = newPosVec.clone();
                  }
               }
            }
         }

         EntityOtherPlayerMP clonedPlayer = new EntityOtherPlayerMP(mc.world, new GameProfile(UUID.fromString("fdee323e-7f0c-4c15-8d1c-0f277442342a"), entity.getName()));
         clonedPlayer.setPosition(posVec[0], posVec[1], posVec[2]);
         clonedPlayer.inventory.copyInventory(entity.inventory);
         clonedPlayer.setHealth(entity.getHealth());
         clonedPlayer.prevPosX = entity.prevPosX;
         clonedPlayer.prevPosY = entity.prevPosY;
         clonedPlayer.prevPosZ = entity.prevPosZ;

         for(PotionEffect effect : entity.getActivePotionEffects()) {
            clonedPlayer.addPotionEffect(effect);
         }

         return new PredictPlayer(entity, clonedPlayer);
      }
   }

   public static String getTimeOfDay() {
      Calendar c = Calendar.getInstance();
      int timeOfDay = c.get(11);
      if (timeOfDay < 12) {
         return "Good Morning, ";
      } else if (timeOfDay < 16) {
         return "Good Afternoon, ";
      } else {
         return timeOfDay < 21 ? "Good Evening, " : "Good Night, ";
      }
   }
}
