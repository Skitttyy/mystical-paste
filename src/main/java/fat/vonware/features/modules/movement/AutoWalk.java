package fat.vonware.features.modules.movement;

import fat.vonware.Vonware;
import fat.vonware.features.Feature;
import fat.vonware.features.modules.Module;
import fat.vonware.features.setting.Setting;
import net.minecraftforge.client.event.InputUpdateEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class AutoWalk extends Module {
   private final Setting<Boolean> lockYaw = new Setting<Boolean>("LockYaw", false, "Locks your yaw while AutoWalk is moving so directional input stays on the activation heading.");
   private float lockedYaw;

   public AutoWalk() {
      super("AutoWalk", "Automatically walks forward", Module.Category.MOVEMENT, true, false, false);
      this.register(this.lockYaw);
   }

   public void onEnable() {
      super.onEnable();
      if (mc.player != null && (Boolean)this.lockYaw.getValue()) {
         this.lockedYaw = mc.player.rotationYaw;
      }

   }

   @SubscribeEvent
   public void onInputUpdate(InputUpdateEvent event) {
      if (!Feature.fullNullCheck()) {
         Module noSlow = Vonware.moduleManager.getModuleByName("NoSlow");
         boolean isSlowed = mc.player.isHandActive() && noSlow != null && noSlow.isEnabled();
         event.getMovementInput().moveForward = isSlowed ? 5.0F : 1.0F;
         if ((Boolean)this.lockYaw.getValue()) {
            mc.player.rotationYaw = this.lockedYaw;
         }

      }
   }
}
