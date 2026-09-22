package fat.vonware.mixin.mixins.chat;

import fat.vonware.Vonware;
import fat.vonware.features.modules.misc.ExtraTab;
import fat.vonware.util.Chat.GlobalExecutor;
import net.minecraft.client.renderer.ThreadDownloadImageData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({ThreadDownloadImageData.class})
public class MixinThreadDownloadImageData {
   @Redirect(
      method = {"loadTextureFromServer"},
      at = @At(
   value = "INVOKE",
   target = "Ljava/lang/Thread;start()V"
)
   )
   public void onStart(Thread thread) {
      ExtraTab EXTRA_TAB_MODULE = (ExtraTab)Vonware.moduleManager.getModuleByClass(ExtraTab.class);
      if (EXTRA_TAB_MODULE.pooled()) {
         GlobalExecutor.FIXED_EXECUTOR.submit(thread);
      } else {
         thread.start();
      }

   }
}
