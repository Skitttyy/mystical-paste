package fat.vonware.util.LogoutSpots;

import com.mojang.authlib.GameProfile;
import fat.vonware.util.MathUtil;
import fat.vonware.util.Util;
import java.lang.reflect.Field;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;

public class LogoutSpot implements Util {
   private final String name;
   private final AxisAlignedBB boundingBox;
   private final EntityOtherPlayerMP clientEntity;
   private final double x;
   private final double y;
   private final double z;
   private final float health;
   private final long logoutTime;

   public LogoutSpot(EntityPlayer player) {
      this.name = player.getName();
      this.boundingBox = player.getEntityBoundingBox();
      this.x = player.posX;
      this.y = player.posY;
      this.z = player.posZ;
      this.health = player.getHealth();
      this.logoutTime = System.currentTimeMillis();
      GameProfile profile = player.getGameProfile();
      this.clientEntity = new EntityOtherPlayerMP(player.world, profile);
      this.clientEntity.copyLocationAndAnglesFrom(player);
      this.clientEntity.rotationYaw = player.rotationYaw;
      this.clientEntity.prevRotationYaw = player.rotationYaw;
      this.clientEntity.rotationPitch = player.rotationPitch;
      this.clientEntity.prevRotationPitch = player.rotationPitch;
      this.clientEntity.rotationYawHead = player.rotationYawHead;
      this.clientEntity.prevRotationYawHead = player.rotationYawHead;
      this.clientEntity.renderYawOffset = player.renderYawOffset;
      this.clientEntity.prevRenderYawOffset = player.renderYawOffset;
      this.clientEntity.limbSwing = 0.0F;
      this.clientEntity.limbSwingAmount = 0.0F;
      this.clientEntity.prevLimbSwingAmount = 0.0F;
      this.clientEntity.motionX = (double)0.0F;
      this.clientEntity.motionY = (double)0.0F;
      this.clientEntity.motionZ = (double)0.0F;
      this.clientEntity.prevPosX = player.posX;
      this.clientEntity.prevPosY = player.posY;
      this.clientEntity.prevPosZ = player.posZ;
      this.clientEntity.lastTickPosX = player.posX;
      this.clientEntity.lastTickPosY = player.posY;
      this.clientEntity.lastTickPosZ = player.posZ;
      this.clientEntity.chasingPosX = player.posX;
      this.clientEntity.chasingPosY = player.posY;
      this.clientEntity.chasingPosZ = player.posZ;
      this.clientEntity.prevChasingPosX = player.posX;
      this.clientEntity.prevChasingPosY = player.posY;
      this.clientEntity.prevChasingPosZ = player.posZ;
      this.copySkinLayers(player);
      Minecraft.getMinecraft().getSkinManager().loadProfileTextures(profile, (type, location, profileTexture) -> this.clientEntity.refreshDisplayName(), true);
      this.clientEntity.getLocationSkin();
   }

   private void copySkinLayers(EntityPlayer player) {
      try {
         for(Field field : EntityPlayer.class.getDeclaredFields()) {
            if (field.getType().equals(DataParameter.class)) {
               field.setAccessible(true);
               Object obj = field.get((Object)null);
               if (obj instanceof DataParameter) {
                  DataParameter<Byte> param = (DataParameter<Byte>)obj;

                  try {
                     byte value = (Byte)player.getDataManager().get(param);
                     this.clientEntity.getDataManager().set(param, value);
                  } catch (ClassCastException var10) {
                  }
               }
            }
         }
      } catch (Exception e) {
         e.printStackTrace();
      }

   }

   public String getName() {
      return this.name;
   }

   public EntityPlayer getEntity() {
      return this.clientEntity;
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

   public float getHealth() {
      return this.health;
   }

   public long getLogoutTime() {
      return this.logoutTime;
   }

   public double getDistance() {
      return mc.player.getDistance(this.x, this.y, this.z);
   }

   public AxisAlignedBB getBoundingBox() {
      return this.boundingBox;
   }

   public Vec3d rounded() {
      return new Vec3d(MathUtil.round(this.x, 1), MathUtil.round(this.y, 1), MathUtil.round(this.z, 1));
   }

   public String getTimeSinceLogout() {
      long elapsed = System.currentTimeMillis() - this.logoutTime;
      long seconds = elapsed / 1000L;
      long minutes = seconds / 60L;
      long hours = minutes / 60L;
      if (hours > 0L) {
         return hours + "h " + minutes % 60L + "m";
      } else {
         return minutes > 0L ? minutes + "m " + seconds % 60L + "s" : seconds + "s";
      }
   }
}
