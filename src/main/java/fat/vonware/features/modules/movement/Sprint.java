package fat.vonware.features.modules.movement;

import fat.vonware.features.Feature;
import fat.vonware.features.modules.Module;
import fat.vonware.features.setting.Setting;
import fat.vonware.util.MovementUtil;
import java.util.function.Function;
import net.minecraft.init.MobEffects;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class Sprint extends Module {
   public static Sprint INSTANCE;
   public Setting<SprintMode> mode;

   public Sprint() {
      super("Sprint", "Automatically sprints for you", Module.Category.MOVEMENT, true, false, false);
      this.mode = (new Setting("Mode", Sprint.SprintMode.Rage)).withDescription((Function<SprintMode, String>)((mode) -> {
         switch (mode) {
            case Legit:
               return "Legit: Only enables sprint through the normal forward-sprint condition.";
            case Rage:
               return "Rage: Forces sprint while moving forward or strafing, including movement that vanilla would not normally sprint through.";
            default:
               return "";
         }
      }));
      this.register(this.mode);
      INSTANCE = this;
   }

   public String getDisplayInfo() {
      return ((SprintMode)this.mode.getValue()).name();
   }

   @SubscribeEvent
   public void onUpdate(TickEvent.ClientTickEvent tickEvent) {
      if (!Feature.fullNullCheck()) {
         if (canSprint()) {
            if (this.mode.getValue() == Sprint.SprintMode.Legit) {
               if (mc.player.moveForward > 0.0F) {
                  mc.player.setSprinting(!mc.player.collidedHorizontally && mc.gameSettings.keyBindForward.isKeyDown());
               }
            } else if (this.mode.getValue() == Sprint.SprintMode.Rage && (mc.player.moveForward != 0.0F || mc.player.moveStrafing != 0.0F)) {
               mc.player.setSprinting(!mc.player.collidedHorizontally && !mc.player.isSneaking());
            }
         }

      }
   }

   public static boolean canSprint() {
      return mc.player != null && !mc.player.isSneaking() && !mc.player.collidedHorizontally && MovementUtil.isMoving() && ((float)mc.player.getFoodStats().getFoodLevel() > 6.0F || mc.player.capabilities.allowFlying) && !mc.player.isPotionActive(MobEffects.BLINDNESS);
   }

   public static enum SprintMode {
      Legit,
      Rage;
   }
}
