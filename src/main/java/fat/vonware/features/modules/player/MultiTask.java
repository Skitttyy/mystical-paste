package fat.vonware.features.modules.player;

import fat.vonware.features.modules.Module;
import fat.vonware.features.setting.Setting;

public class MultiTask extends Module {
   public static MultiTask INSTANCE;
   public final Setting<Boolean> crystalAura = new Setting<Boolean>("AutoCrystal", true, "Allows AutoCrystal actions while MultiTask is active.");
   public final Setting<Boolean> scaffold = new Setting<Boolean>("Scaffold", true, "Allows Scaffold actions while MultiTask is active.");
   public final Setting<Boolean> fastBreak = new Setting<Boolean>("FastBreak", true, "Allows FastBreak actions while MultiTask is active.");

   public MultiTask() {
      super("MultiTask", "Allows you to eat & mine and do an action at the same time", Module.Category.PLAYER, true, false, false);
      INSTANCE = this;
      this.register(this.crystalAura);
      this.register(this.scaffold);
      this.register(this.fastBreak);
   }
}
