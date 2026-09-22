package fat.vonware.mixin.mixins.player;

import fat.vonware.event.events.Block.PickBlockEvent;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({ForgeHooks.class})
public class MixinForgeHooks {
   @Inject(
      method = {"onPickBlock"},
      at = {@At("HEAD")},
      cancellable = true,
      remap = false
   )
   private static void onPickBlock(RayTraceResult target, EntityPlayer player, World world, CallbackInfoReturnable<Boolean> cir) {
      PickBlockEvent event = new PickBlockEvent();
      MinecraftForge.EVENT_BUS.post(event);
      if (event.isCanceled()) {
         cir.cancel();
      }

   }
}
