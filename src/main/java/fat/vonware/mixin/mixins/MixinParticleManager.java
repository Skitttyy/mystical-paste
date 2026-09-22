package fat.vonware.mixin.mixins;

import fat.vonware.features.modules.client.Optimizer;
import fat.vonware.features.modules.render.NoRender;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.AxisAlignedBB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({ParticleManager.class})
public class MixinParticleManager {
   @Inject(
      method = {"spawnEffectParticle(IDDDDDD[I)Lnet/minecraft/client/particle/Particle;"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void onSpawnParticle(int particleId, double xCoord, double yCoord, double zCoord, double xSpeed, double ySpeed, double zSpeed, int[] parameters, CallbackInfoReturnable<Object> cir) {
      NoRender noRender = NoRender.getInstance();
      if (noRender.isOn() && (Boolean)noRender.xpParticles.getValue() && particleId == EnumParticleTypes.SPELL.getParticleID()) {
         cir.setReturnValue((Object)null);
      }

      AxisAlignedBB bb = new AxisAlignedBB(xCoord - 0.1, yCoord - 0.1, zCoord - 0.1, xCoord + 0.1, yCoord + 0.1, zCoord + 0.1);
      if (Optimizer.shouldCullAABB(bb)) {
         cir.setReturnValue((Object)null);
      }

   }
}
