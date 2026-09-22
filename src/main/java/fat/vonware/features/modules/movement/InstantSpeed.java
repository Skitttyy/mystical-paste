package fat.vonware.features.modules.movement;

import fat.vonware.Vonware;
import fat.vonware.event.events.MoveEvent;
import fat.vonware.features.Feature;
import fat.vonware.features.modules.Module;
import fat.vonware.features.setting.Setting;
import fat.vonware.util.MovementUtil;
import java.util.function.Function;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class InstantSpeed extends Module {
   public Setting<Boolean> liquidStop = new Setting<Boolean>("Liquid Stop", false, "Stops InstantSpeed from modifying movement while you are in liquid.");
   protected final Setting<Mode> mode;
   public static boolean paused;
   boolean jumpBoosting;

   public InstantSpeed() {
      super("InstantSpeed", "Instantly accelerates your movement", Module.Category.MOVEMENT, true, false, false);
      this.mode = (new Setting("Mode", InstantSpeed.Mode.Max)).withDescription((Function<Mode, String>)((mode) -> {
         switch (mode) {
            case Strict:
               return "Strict: Uses the stricter movement-speed calculation intended to reduce server corrections.";
            case Max:
               return "Max: Applies the module's maximum instant horizontal speed calculation.";
            default:
               return "";
         }
      }));
      this.register(this.liquidStop);
      this.register(this.mode);
      this.jumpBoosting = false;
   }

   @SubscribeEvent
   public void onMove(MoveEvent event) {
      if (!Feature.fullNullCheck()) {
         if (!mc.player.isCreative()) {
            if (!mc.player.isSpectator()) {
               if (!mc.player.isElytraFlying()) {
                  if (!((Speed)Vonware.moduleManager.getModuleByClass(Speed.class)).isEnabled()) {
                     if (!((HoleSnap)Vonware.moduleManager.getModuleByClass(HoleSnap.class)).isEnabled()) {
                        if (!this.liquidDisable()) {
                           if (this.mode.getValue() == InstantSpeed.Mode.Strict) {
                              if (!paused) {
                                 if (!mc.player.isSneaking() && !mc.player.isInWater() && !mc.player.isInLava() && (mc.player.movementInput.moveForward != 0.0F || mc.player.movementInput.moveStrafe != 0.0F) && mc.player.onGround && !mc.gameSettings.keyBindJump.isKeyDown()) {
                                    MovementUtil.strafe(event, MovementUtil.getStrictBaseSpeed(0.2873));
                                 } else if (mc.player.movementInput.moveForward == 0.0F && mc.player.movementInput.moveStrafe == 0.0F) {
                                    mc.player.motionX = (double)0.0F;
                                    event.setX(mc.player.motionZ = (double)0.0F);
                                    event.setZ((double)0.0F);
                                 }
                              }
                           } else if (!paused) {
                              if (mc.player.isSneaking() || mc.player.isInWater() || mc.player.isInLava() || mc.player.movementInput.moveForward == 0.0F && mc.player.movementInput.moveStrafe == 0.0F) {
                                 if (mc.player.movementInput.moveForward == 0.0F && mc.player.movementInput.moveStrafe == 0.0F) {
                                    mc.player.motionZ = (double)0.0F;
                                    event.setX(mc.player.motionX = (double)0.0F);
                                    event.setZ((double)0.0F);
                                 }
                              } else {
                                 MovementUtil.strafe(event, MovementUtil.getStrictBaseSpeed(0.2873));
                              }
                           }

                        }
                     }
                  }
               }
            }
         }
      }
   }

   private boolean liquidDisable() {
      if (!(Boolean)this.liquidStop.getValue()) {
         return false;
      } else if (!mc.player.isInLava() && !mc.player.isInWater()) {
         return false;
      } else {
         this.disable();
         return true;
      }
   }

   public static enum Mode {
      Strict,
      Max;
   }
}
