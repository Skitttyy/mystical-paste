package fat.vonware.mixin.mixins;

import fat.vonware.features.modules.render.PopEffects;
import java.awt.Color;
import net.minecraft.client.particle.ParticleSimpleAnimated;
import net.minecraft.client.particle.ParticleTotem;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SideOnly(Side.CLIENT)
@Mixin({ParticleTotem.class})
public abstract class MixinParticleTotem extends ParticleSimpleAnimated {
   public MixinParticleTotem(World worldIn, double xCoordIn, double yCoordIn, double zCoordIn, double xSpeedIn, double ySpeedIn, double zSpeedIn) {
      super(worldIn, xCoordIn, yCoordIn, zCoordIn, 176, 8, -0.05F);
      this.motionX = xSpeedIn;
      this.motionY = ySpeedIn;
      this.motionZ = zSpeedIn;
   }

   @Inject(
      method = {"<init>"},
      at = {@At("RETURN")}
   )
   public void onParticleTotem(World worldIn, double xCoordIn, double yCoordIn, double zCoordIn, double xSpeedIn, double ySpeedIn, double zSpeedIn, CallbackInfo info) {
      if (PopEffects.INSTANCE.isEnabled() && (PopEffects.INSTANCE.popType.getValue() == PopEffects.PopType.Particles || PopEffects.INSTANCE.popType.getValue() == PopEffects.PopType.Both)) {
         boolean useThird = (Boolean)PopEffects.INSTANCE.thirdColorEnabled.getValue();
         Color color;
         if (useThird) {
            int roll = this.rand.nextInt(3);
            if (roll == 0) {
               color = PopEffects.INSTANCE.getFirstParticleColor();
            } else if (roll == 1) {
               color = PopEffects.INSTANCE.getSecondParticleColor();
            } else {
               color = PopEffects.INSTANCE.getThirdParticleColor();
            }
         } else if (this.rand.nextInt(4) == 0) {
            color = PopEffects.INSTANCE.getFirstParticleColor();
         } else {
            color = PopEffects.INSTANCE.getSecondParticleColor();
         }

         this.setRBGColorF((float)color.getRed() / 255.0F, (float)color.getGreen() / 255.0F, (float)color.getBlue() / 255.0F);
         this.motionX *= (double)(Float)PopEffects.INSTANCE.duration.getValue();
         this.motionY *= (double)(Float)PopEffects.INSTANCE.duration.getValue();
         this.motionZ *= (double)(Float)PopEffects.INSTANCE.duration.getValue();
      } else {
         this.particleScale *= 0.75F;
         this.particleMaxAge = 60 + this.rand.nextInt(12);
         if (this.rand.nextInt(4) == 0) {
            this.setRBGColorF(0.6F + this.rand.nextFloat() * 0.2F, 0.6F + this.rand.nextFloat() * 0.3F, this.rand.nextFloat() * 0.2F);
         } else {
            this.setRBGColorF(0.1F + this.rand.nextFloat() * 0.2F, 0.4F + this.rand.nextFloat() * 0.3F, this.rand.nextFloat() * 0.2F);
         }

         this.setBaseAirFriction(0.6F);
      }

   }
}
