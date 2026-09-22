package fat.vonware.features.modules.movement;

import fat.vonware.event.events.StepEvent;
import fat.vonware.features.modules.Module;
import fat.vonware.features.setting.Setting;
import fat.vonware.util.PlayerUtil;
import fat.vonware.util.Timer;
import java.util.function.Function;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class Step extends Module {
   private final Setting<Float> stepHeight = new Setting<Float>("Height", 1.0F, 0.6F, 2.0F, "Maximum obstacle height Step is allowed to climb.");
   private final Setting<Mode> mode;
   private final Setting<Boolean> timer;
   private final Setting<Boolean> hold;
   public static Step INSTANCE;
   Timer stepTimer;
   public static boolean paused;
   float oldVal;
   private int ticks;

   public Step() {
      super("Step", "automatically steps you up", Module.Category.MOVEMENT, true, false, true);
      this.mode = (new Setting("Mode", Step.Mode.NCP)).withDescription((Function<Mode, String>)((mode) -> {
         switch (mode) {
            case Vanilla:
               return "Vanilla: Raises Minecraft's stepHeight directly to the configured Height.";
            case NCP:
               return "NCP: Uses position-packet step offsets for the detected step height and can slow the timer during the step.";
            default:
               return "";
         }
      }));
      this.timer = new Setting<Boolean>("Use Timer", false, "Slows the client timer during NCP packet steps to better match the step packet sequence.");
      this.hold = new Setting<Boolean>("Hold", false, "Makes Step enable only while its bind is held.");
      this.stepTimer = new Timer();
      this.register(this.mode);
      this.register(this.stepHeight);
      this.register(this.timer);
      this.register(this.hold);
      this.oldVal = 0.0F;
      this.ticks = 0;
      INSTANCE = this;
   }

   public String getDisplayInfo() {
      return ((Mode)this.mode.getValue()).toString();
   }

   public void onEnable() {
      super.onEnable();
      this.oldVal = 0.6F;
      this.stepTimer.setDelay(50L);
   }

   public void onDisable() {
      super.onDisable();
      if (!fullNullCheck()) {
         paused = false;
         mc.player.stepHeight = this.oldVal;
         if ((Boolean)this.timer.getValue()) {
            PlayerUtil.resetTimer();
         }

         this.stepTimer.setDelay(50L);
      }
   }

   public void toggle() {
      if (!(Boolean)this.hold.getValue()) {
         super.toggle();
      }
   }

   @SubscribeEvent
   public void onTick(TickEvent.ClientTickEvent event) {
      if (!fullNullCheck()) {
         if ((Boolean)this.hold.getValue()) {
            boolean keyDown = !this.getBind().isEmpty() && this.getBind().isDown();
            if (keyDown && !this.isEnabled()) {
               this.enable();
            } else if (!keyDown && this.isEnabled()) {
               this.disable();
               return;
            }
         }

         if (this.isEnabled()) {
            this.stepTimer.setDelay(50L);
            if (this.mode.getValue() == Step.Mode.NCP && !paused && (Boolean)this.timer.getValue()) {
               if (this.ticks == 0) {
                  PlayerUtil.resetTimer();
               } else {
                  --this.ticks;
               }
            }

            switch ((Mode)this.mode.getValue()) {
               case Vanilla:
                  if (!paused) {
                     mc.player.stepHeight = ((Number)this.stepHeight.getValue()).floatValue();
                  } else {
                     mc.player.stepHeight = this.oldVal;
                  }
                  break;
               case NCP:
                  if (paused) {
                     return;
                  }

                  if ((Boolean)this.timer.getValue()) {
                     if (this.ticks == 0) {
                        PlayerUtil.resetTimer();
                     } else {
                        --this.ticks;
                     }
                  }

                  if (mc.player.onGround && this.stepTimer.isPassed()) {
                     mc.player.stepHeight = ((Number)this.stepHeight.getValue()).floatValue();
                  } else {
                     mc.player.stepHeight = 0.6F;
                  }
            }

         }
      }
   }

   @SubscribeEvent
   public void onStep(StepEvent event) {
      if (this.isEnabled()) {
         if (this.mode.getValue() == Step.Mode.NCP) {
            if (!paused) {
               double height = event.getAxisAlignedBB().minY - mc.player.posY;
               if (!(height <= (double)0.0F) && !(height > ((Number)this.stepHeight.getValue()).doubleValue())) {
                  double[] offsets = this.getOffset(height);
                  if (offsets != null && offsets.length > 1) {
                     for(double offset : offsets) {
                        mc.player.connection.sendPacket(new CPacketPlayer.Position(mc.player.posX, mc.player.posY + offset, mc.player.posZ, false));
                     }

                     if ((Boolean)this.timer.getValue()) {
                        if (height == (double)1.0F) {
                           PlayerUtil.setTimer(0.6F);
                           this.ticks = 1;
                        } else if (height == (double)1.5F) {
                           PlayerUtil.setTimer(0.35F);
                           this.ticks = 1;
                        } else if (height == (double)2.0F) {
                           PlayerUtil.setTimer(0.25F);
                           this.ticks = 2;
                        } else if (height == (double)2.5F) {
                           PlayerUtil.setTimer(0.15F);
                           this.ticks = 2;
                        } else {
                           PlayerUtil.setTimer(1.0F / (float)offsets.length);
                           this.ticks = 2;
                        }
                     }
                  }

                  this.stepTimer.resetDelay();
               }
            }
         }
      }
   }

   public double[] getOffset(double height) {
      if (height == (double)0.75F) {
         return new double[]{0.42, 0.753, (double)0.75F};
      } else if (height == (double)0.8125F) {
         return new double[]{0.39, 0.7, (double)0.8125F};
      } else if (height == (double)0.875F) {
         return new double[]{0.39, 0.7, (double)0.875F};
      } else if (height == (double)1.0F) {
         return new double[]{0.41999998688698, 0.7531999805212, (double)1.0F};
      } else if (height == (double)1.5F) {
         return new double[]{0.42, (double)0.75F, (double)1.0F, 1.16, 1.23, 1.2};
      } else if (height == (double)2.0F) {
         return new double[]{0.42, 0.78, 0.63, 0.51, 0.9, 1.21, 1.45, 1.43};
      } else {
         return height == (double)2.5F ? new double[]{0.425, 0.821, 0.699, 0.599, 1.022, 1.372, 1.652, 1.869, 2.019, 1.907} : null;
      }
   }

   public static enum Mode {
      Vanilla,
      NCP;
   }
}
