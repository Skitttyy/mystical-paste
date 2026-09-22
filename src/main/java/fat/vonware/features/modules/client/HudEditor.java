package fat.vonware.features.modules.client;

import fat.vonware.features.gui.newgui.newcomponents.NewVonwareGui;
import fat.vonware.features.modules.Module;
import fat.vonware.util.Util;

public class HudEditor extends Module {
   public HudEditor() {
      super("HudEditor", "Opens the HudEditor", Module.Category.CLIENT, true, false, false);
   }

   public void onEnable() {
      Util.mc.displayGuiScreen(new NewVonwareGui(true));
      this.disable();
   }
}
