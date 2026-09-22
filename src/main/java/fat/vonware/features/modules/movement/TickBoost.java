package fat.vonware.features.modules.movement;

import fat.vonware.ducks.IMinecraft;
import fat.vonware.features.Feature;
import fat.vonware.features.modules.Module;
import fat.vonware.features.setting.Setting;
import fat.vonware.mixin.mixins.accessor.ITimer;
import fat.vonware.util.MovementUtil;
import java.util.function.Function;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class TickBoost extends Module {
   public static TickBoost INSTANCE;
   public Setting<Float> timerAmount = new Setting<Float>("Timer Amount", 5.0F, 1.0F, 5.0F, "Timer multiplier used during the active boost portion of a TickBoost cycle.");
   public Setting<Integer> boostSpacing;
   public Setting<Integer> BoostTime = new Setting<Integer>("Boost Time", 25, 10, 100, "Number of ticks the accelerated portion of a TickBoost cycle lasts.");
   public Setting<Integer> ChargeSpeed;
   public Setting<Boolean> rampUpEnabled;
   public Setting<Integer> rampUpTicks;
   protected final Setting<Mode> mode;
   float oldTickLength;
   ITimer timer;
   int boostTime;
   int boostSpaceTime;
   boolean doLoop;
   int rampTickCounter;
   float currentTimerDivisor;

   public TickBoost() {
      super("TickBoost", "Boosts player speed (with ticks)", Module.Category.MOVEMENT, true, false, false);
      this.mode = (new Setting("Mode", TickBoost.Mode.Spacing)).withDescription((Function<Mode, String>)((mode) -> {
         switch (mode) {
            case Disable:
               return "Disable: Runs one configured timer-boost burst and disables when that burst completes.";
            case Charge:
               return "Charge: Uses the module's charge-style timer behavior controlled by Charge Speed.";
            case Spacing:
               return "Spacing: Repeats timer-boost bursts separated by Boost Spacing and optionally ramps the timer up.";
            default:
               return "";
         }
      }));
      this.boostSpacing = new Setting<Integer>("Boost Spacing", 10, 1, 30, (v) -> this.mode.getValue() == TickBoost.Mode.Spacing, "Number of normal-speed ticks between repeated boosts in Spacing mode.");
      this.ChargeSpeed = new Setting<Integer>("Charge Speed", 25, 10, 100, (v) -> this.mode.getValue() == TickBoost.Mode.Charge, "Controls how quickly Charge mode builds/uses its timer boost.");
      this.rampUpEnabled = new Setting<Boolean>("Ramp Up", false, (v) -> this.mode.getValue() == TickBoost.Mode.Spacing, "Gradually increases the timer multiplier during Spacing boosts instead of applying it instantly.");
      this.rampUpTicks = new Setting<Integer>("Ramp Up Ticks", 5, 1, 20, (v) -> this.mode.getValue() == TickBoost.Mode.Spacing && (Boolean)this.rampUpEnabled.getValue(), "Ticks between each timer-multiplier increase while Ramp Up is active.");
      this.register(this.timerAmount);
      this.register(this.boostSpacing);
      this.register(this.BoostTime);
      this.register(this.ChargeSpeed);
      this.register(this.rampUpEnabled);
      this.register(this.rampUpTicks);
      this.register(this.mode);
      this.oldTickLength = 50.0F;
      INSTANCE = this;
   }

   public boolean isBoosting() {
      return this.isEnabled() && !this.doLoop && this.boostTime < (Integer)this.BoostTime.getValue();
   }

   public void onEnable() {
      super.onEnable();
      this.boostTime = 0;
      this.doLoop = false;
      this.boostSpaceTime = 0;
      this.rampTickCounter = 0;
      this.currentTimerDivisor = 1.0F;
      if (!fullNullCheck()) {
         this.timer = (ITimer)((IMinecraft)mc).getTimer();
         this.oldTickLength = this.timer.getTickLength();
      }
   }

   public void onDisable() {
      super.onDisable();
      if (!fullNullCheck()) {
         if (this.timer != null) {
            this.timer.setTickLength(this.oldTickLength);
         }
      }
   }

   @SubscribeEvent
   public void onUpdate(TickEvent.ClientTickEvent event) {
      if (!Feature.fullNullCheck()) {
         if (this.timer == null) {
            this.timer = (ITimer)((IMinecraft)mc).getTimer();
         } else {
            if (this.mode.getValue() != TickBoost.Mode.Charge) {
               if (!this.doLoop) {
                  ++this.boostTime;
               }

               if (this.doLoop) {
                  ++this.boostSpaceTime;
               }

               if (this.boostSpaceTime > (Integer)this.boostSpacing.getValue() && this.doLoop) {
                  this.doLoop = false;
                  this.boostTime = 0;
               }

               if (this.boostTime < (Integer)this.BoostTime.getValue() && !this.doLoop) {
                  if ((Boolean)this.rampUpEnabled.getValue()) {
                     ++this.rampTickCounter;
                     if (this.rampTickCounter >= (Integer)this.rampUpTicks.getValue()) {
                        this.currentTimerDivisor = Math.min(this.currentTimerDivisor + 1.0F, (Float)this.timerAmount.getValue());
                        this.rampTickCounter = 0;
                     }

                     this.timer.setTickLength(this.oldTickLength / this.currentTimerDivisor);
                  } else {
                     this.timer.setTickLength(this.oldTickLength / (Float)this.timerAmount.getValue());
                  }
               } else if (!this.doLoop) {
                  this.timer.setTickLength(this.oldTickLength);
                  this.doLoop = true;
                  this.boostSpaceTime = 0;
                  this.currentTimerDivisor = 1.0F;
                  this.rampTickCounter = 0;
                  if (this.mode.getValue() != TickBoost.Mode.Disable) {
                     this.toggle();
                  }
               } else {
                  if (HoleSnap.getInstance().isEnabled()) {
                     this.boostTime = 0;
                     return;
                  }

                  if (mc.player.motionX == (double)0.0F && mc.player.motionZ == (double)0.0F) {
                     this.timer.setTickLength(this.oldTickLength);
                     if (this.boostTime < (Integer)this.BoostTime.getValue()) {
                        ++this.boostTime;
                     }
                  } else if (MovementUtil.isActuallyMoving(mc.player)) {
                     if (this.boostTime > 0) {
                        this.timer.setTickLength(this.oldTickLength / (Float)this.timerAmount.getValue());
                        --this.boostTime;
                     } else {
                        this.timer.setTickLength(this.oldTickLength);
                     }
                  } else {
                     this.timer.setTickLength(this.oldTickLength);
                  }
               }
            }

         }
      }
   }

   public static enum Mode {
      Disable,
      Charge,
      Spacing;
   }
}
