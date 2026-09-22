package fat.vonware.mixin.mixins.player;

import fat.vonware.Vonware;
import fat.vonware.features.modules.render.NoRender;
import fat.vonware.mixin.mixins.MixinEntityPlayer;
import net.minecraft.client.entity.AbstractClientPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({AbstractClientPlayer.class})
public abstract class MixinAbstractClientPlayer extends MixinEntityPlayer {
   @Inject(
      method = {"getFovModifier"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void getFovModifierHook(CallbackInfoReturnable<Float> info) {
      NoRender noRender = (NoRender)Vonware.moduleManager.getModuleByClass(NoRender.class);
      if (noRender != null && noRender.dynamicFov()) {
         info.setReturnValue(1.0F);
      }

   }
}
