package fat.vonware.features.modules.client;

import fat.vonware.features.modules.Module;
import fat.vonware.features.setting.Setting;

public class Media extends Module {
   public static Media INSTANCE;
   public final Setting<String> newName = new Setting<String>("Hidden", "Hidden");

   public Media() {
      super("Media", "Hides your username", Module.Category.CLIENT, true, false, false);
      this.register(this.newName);
      INSTANCE = this;
   }
}
