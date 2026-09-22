package fat.vonware.mixin.mixins;

import fat.vonware.event.events.SuffocationEvent;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({EntityPlayer.class})
public abstract class MixinEntityPlayer {
   @Inject(
      method = {"isEntityInsideOpaqueBlock"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void isEntityInsideOpaqueBlockHook(CallbackInfoReturnable<Boolean> info) {
      SuffocationEvent event = new SuffocationEvent((EntityPlayer)(Object)this);
      MinecraftForge.EVENT_BUS.post(event);
      if (event.isCanceled()) {
         info.setReturnValue(false);
         info.cancel();
      }

   }
}
