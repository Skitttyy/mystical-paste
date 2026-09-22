package fat.vonware.mixin.mixins.accessor;

import net.minecraft.network.play.client.CPacketPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({CPacketPlayer.class})
public interface ICPacketPlayer {
   @Accessor("rotating")
   boolean isRotating();

   @Accessor("moving")
   boolean isMoving();

   @Accessor("x")
   void setX(double var1);

   @Accessor("x")
   double getX();

   @Accessor("y")
   void setY(double var1);

   @Accessor("y")
   double getY();

   @Accessor("z")
   void setZ(double var1);

   @Accessor("z")
   double getZ();

   @Accessor("yaw")
   void setYaw(float var1);

   @Accessor("yaw")
   float getYaw();

   @Accessor("pitch")
   void setPitch(float var1);

   @Accessor("pitch")
   float getPitch();

   @Accessor("onGround")
   void setOnGround(boolean var1);
}
