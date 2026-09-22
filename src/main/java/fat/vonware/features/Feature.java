package fat.vonware.features;

import fat.vonware.Vonware;
import fat.vonware.features.gui.newgui.newcomponents.NewVonwareGui;
import fat.vonware.features.modules.Module;
import fat.vonware.features.setting.Setting;
import fat.vonware.manager.TextManager;
import fat.vonware.util.Util;
import java.util.ArrayList;
import java.util.List;

public class Feature implements Util {
   public List<Setting> settings = new ArrayList();
   public TextManager renderer;
   private String name;

   public Feature() {
      this.renderer = Vonware.textManager;
   }

   public Feature(String name) {
      this.renderer = Vonware.textManager;
      this.name = name;
   }

   public static boolean fullNullCheck() {
      return mc.player == null || mc.world == null;
   }

   public static boolean spawnCheck() {
      return mc.player.ticksExisted > 15;
   }

   public String getName() {
      return this.name;
   }

   public List<Setting> getSettings() {
      return this.settings;
   }

   public boolean isEnabled() {
      return this instanceof Module ? ((Module)this).isOn() : false;
   }

   public boolean isDisabled() {
      return !this.isEnabled();
   }

   public Setting register(Setting setting) {
      setting.setFeature(this);
      this.settings.add(setting);
      if (this instanceof Module && mc.currentScreen instanceof NewVonwareGui) {
         NewVonwareGui.getInstance().updateModule((Module)this);
      }

      return setting;
   }

   public void unregister(Setting settingIn) {
      ArrayList<Setting> removeList = new ArrayList();

      for(Setting setting : this.settings) {
         if (setting.equals(settingIn)) {
            removeList.add(setting);
         }
      }

      if (!removeList.isEmpty()) {
         this.settings.removeAll(removeList);
      }

      if (this instanceof Module && mc.currentScreen instanceof NewVonwareGui) {
         NewVonwareGui.getInstance().updateModule((Module)this);
      }

   }

   public Setting getSettingByName(String name) {
      for(Setting setting : this.settings) {
         if (setting.getName().equalsIgnoreCase(name)) {
            return setting;
         }
      }

      return null;
   }

   public void reset() {
      for(Setting setting : this.settings) {
         setting.setValue(setting.getDefaultValue());
      }

   }

   public void clearSettings() {
      this.settings = new ArrayList();
   }
}
