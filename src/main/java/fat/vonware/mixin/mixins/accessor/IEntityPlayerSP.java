package fat.vonware.mixin.mixins.accessor;

import net.minecraft.client.entity.EntityPlayerSP;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin({EntityPlayerSP.class})
public interface IEntityPlayerSP {
   @Accessor("handActive")
   void pkSetActiveHand(boolean var1);

   @Accessor("serverSneakState")
   void setServerSneakState(boolean var1);

   @Accessor("serverSneakState")
   boolean getServerSneakState();

   @Accessor("serverSprintState")
   void setServerSprintState(boolean var1);

   @Accessor("serverSprintState")
   boolean getServerSprintState();

   @Accessor("prevOnGround")
   void setPrevOnGround(boolean var1);

   @Accessor("prevOnGround")
   boolean getPrevOnGround();

   @Accessor("autoJumpEnabled")
   void setAutoJumpEnabled(boolean var1);

   @Accessor("autoJumpEnabled")
   boolean getAutoJumpEnabled();

   @Accessor("lastReportedPosX")
   void setLastReportedPosX(double var1);

   @Accessor("lastReportedPosX")
   double getLastReportedPosX();

   @Accessor("lastReportedPosY")
   void setLastReportedPosY(double var1);

   @Accessor("lastReportedPosY")
   double getLastReportedPosY();

   @Accessor("lastReportedPosZ")
   void setLastReportedPosZ(double var1);

   @Accessor("lastReportedPosZ")
   double getLastReportedPosZ();

   @Accessor("lastReportedYaw")
   void setLastReportedYaw(float var1);

   @Accessor("lastReportedYaw")
   float getLastReportedYaw();

   @Accessor("lastReportedPitch")
   void setLastReportedPitch(float var1);

   @Accessor("lastReportedPitch")
   float getLastReportedPitch();

   @Accessor("positionUpdateTicks")
   void setPositionUpdateTicks(int var1);

   @Accessor("positionUpdateTicks")
   int getPositionUpdateTicks();

   @Invoker("onUpdateWalkingPlayer")
   void invokeOnUpdateWalkingPlayer();

   @Invoker("onUpdate")
   void superUpdate();
}
