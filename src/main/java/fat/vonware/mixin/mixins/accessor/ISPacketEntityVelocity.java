package fat.vonware.mixin.mixins.accessor;

import net.minecraft.network.play.server.SPacketEntityVelocity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({SPacketEntityVelocity.class})
public interface ISPacketEntityVelocity {
   @Accessor("motionX")
   void setMotionX(int var1);

   @Accessor("motionY")
   void setMotionY(int var1);

   @Accessor("motionZ")
   void setMotionZ(int var1);
}
