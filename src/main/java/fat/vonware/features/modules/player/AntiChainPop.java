package fat.vonware.features.modules.player;

import fat.vonware.Vonware;
import fat.vonware.features.modules.Module;
import fat.vonware.features.setting.Setting;

public class AntiChainPop extends Module {
   private final Setting<Float> health = new Setting<Float>("Health", 10.0F, 0.0F, 20.0F, "Health threshold needed to enable HoleSnap to trigger its protection.");
   private boolean wasBelowThreshold = false;

   public AntiChainPop() {
      super("AntiChainPop", "SUPER HIGH TECH", Module.Category.PLAYER, true, false, false);
      this.register(this.health);
   }

   public String getDisplayInfo() {
      if (mc.player != null) {
         float currentHealth = mc.player.getHealth();
         return String.format("%.1f", currentHealth);
      } else {
         return "Health: N/A";
      }
   }

   public void onUpdate() {
      if (mc.player != null) {
         float currentHealth = mc.player.getHealth();
         if (currentHealth <= (Float)this.health.getValue()) {
            if (!this.wasBelowThreshold) {
               Module holeSnap = Vonware.moduleManager.getModuleByName("HoleSnap");
               if (holeSnap != null && !holeSnap.isEnabled()) {
                  holeSnap.enable();
               }

               this.wasBelowThreshold = true;
            }
         } else {
            this.wasBelowThreshold = false;
         }
      }

   }
}
