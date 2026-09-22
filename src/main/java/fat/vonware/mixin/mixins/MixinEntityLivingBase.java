package fat.vonware.mixin.mixins;

import fat.vonware.Vonware;
import fat.vonware.features.modules.render.HandVisuals;
import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({EntityLivingBase.class})
public class MixinEntityLivingBase {
   @Inject(
      method = {"getArmSwingAnimationEnd"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void getArmSwingAnimationEndHook(CallbackInfoReturnable<Integer> cir) {
      HandVisuals handVisuals = (HandVisuals)Vonware.moduleManager.getModuleByClass(HandVisuals.class);
      int stuff = handVisuals.isEnabled() && (Boolean)handVisuals.changeSwing.getValue() ? (Integer)handVisuals.swingSpeed.getValue() : 6;
      cir.setReturnValue(stuff);
   }
}
