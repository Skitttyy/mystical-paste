package fat.vonware.mixin.mixins.accessor;

import net.minecraft.network.play.server.SPacketExplosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({SPacketExplosion.class})
public interface ISPacketExplosion {
   @Accessor("motionX")
   void setMotionX(float var1);

   @Accessor("motionY")
   void setMotionY(float var1);

   @Accessor("motionZ")
   void setMotionZ(float var1);
}
