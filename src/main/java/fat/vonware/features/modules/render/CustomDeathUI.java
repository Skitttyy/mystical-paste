package fat.vonware.features.modules.render;

import fat.vonware.features.modules.Module;
import fat.vonware.features.setting.Setting;

public class CustomDeathUI extends Module {
   private static CustomDeathUI INSTANCE;
   public final Setting<Animation> animation;

   public CustomDeathUI() {
      super("CustomDeathUI", "Creates a custom death screen", Module.Category.RENDER, true, false, false);
      this.animation = new Setting<Animation>("Animation", CustomDeathUI.Animation.Heart);
      this.register(this.animation);
      INSTANCE = this;
   }

   public static CustomDeathUI getInstance() {
      return INSTANCE;
   }

   public static enum Animation {
      Heart,
      ReZero;
   }
}
