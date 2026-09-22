package fat.vonware.mixin;

import fat.vonware.Vonware;
import java.util.Map;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;

public class MixinLoader implements IFMLLoadingPlugin {
   private static boolean isObfuscatedEnvironment = false;

   public MixinLoader() {
      Vonware.LOGGER.info("\n\nLoading mixins by Vonware");
      MixinBootstrap.init();
      Mixins.addConfiguration("mixins.vonware.json");
      MixinEnvironment.getDefaultEnvironment().setObfuscationContext("searge");
      Vonware.LOGGER.info(MixinEnvironment.getDefaultEnvironment().getObfuscationContext());
   }

   public String[] getASMTransformerClass() {
      return new String[0];
   }

   public String getModContainerClass() {
      return null;
   }

   public String getSetupClass() {
      return null;
   }

   public void injectData(Map<String, Object> data) {
      isObfuscatedEnvironment = (Boolean)data.get("runtimeDeobfuscationEnabled");
   }

   public String getAccessTransformerClass() {
      return null;
   }
}
