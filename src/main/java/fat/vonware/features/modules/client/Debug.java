package fat.vonware.features.modules.client;

import fat.vonware.features.modules.Module;
import fat.vonware.features.setting.Setting;

public class Debug extends Module {
   private static Debug INSTANCE;
   public Setting<Boolean> surroundDebug = new Setting<Boolean>("Surround", false);
   public Setting<Boolean> holeFillDebug = new Setting<Boolean>("HoleFill", false);
   public Setting<Boolean> fastBreakDebug = new Setting<Boolean>("FastBreak", false);

   public Debug() {
      super("Debug", "Only to be used by the DEV", Module.Category.CLIENT, true, false, false);
      this.register(this.surroundDebug);
      this.register(this.holeFillDebug);
      this.register(this.fastBreakDebug);
      INSTANCE = this;
   }

   public static Debug getInstance() {
      if (INSTANCE == null) {
         INSTANCE = new Debug();
      }

      return INSTANCE;
   }
}
