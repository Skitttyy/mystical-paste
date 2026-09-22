package fat.vonware.features.modules.movement;

import fat.vonware.event.events.MoveEvent;
import fat.vonware.event.events.PacketEvent;
import fat.vonware.features.Feature;
import fat.vonware.features.modules.Module;
import fat.vonware.features.setting.Setting;
import java.util.Objects;
import java.util.function.Function;
import net.minecraft.init.MobEffects;
import net.minecraft.network.play.server.SPacketPlayerPosLook;
import net.minecraft.potion.PotionEffect;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class LongJump extends Module {
   public static LongJump INSTANCE;
   private final Setting<Mode> mode;
   private final Setting<Float> boost;
   private final Setting<Integer> boostCooldown;
   private final Setting<Boolean> autoDisable;
   private final Setting<Boolean> onGround;
   private final Setting<Boolean> hold;
   private boolean hasBeenGrounded;
   private int stage;
   private double moveSpeed;
   private double lastDist;
   private boolean damaged;
   private int ticksSinceDamage;
   private int boostCount;
   private long lastBoostTime;

   public LongJump() {
      super("LongJump", "makes you jump further", Module.Category.MOVEMENT, true, false, true);
      this.mode = (new Setting("Mode", LongJump.Mode.Normal)).withDescription((Function<Mode, String>)((mode) -> {
         switch (mode) {
            case Normal:
               return "Normal: Uses the normal staged long-jump motion sequence.";
            case Strict:
               return "Strict: Uses the stricter staged long-jump sequence with different motion and speed values.";
            default:
               return "";
         }
      }));
      this.boost = new Setting<Float>("Boost-Factor", 3.2F, 1.0F, 10.0F, "Multiplier used to increase horizontal speed during the long-jump launch stages.");
      this.boostCooldown = new Setting<Integer>("Boost-Cooldown", 1000, 0, 3000, "Cooldown before another LongJump boost sequence can begin.");
      this.autoDisable = new Setting<Boolean>("Auto Disable", false, "Automatically disables LongJump after its jump sequence finishes.");
      this.onGround = new Setting<Boolean>("OnGround Check", false, "Requires the configured LongJump activation path to satisfy its on-ground check.");
      this.hold = new Setting<Boolean>("Hold", false, "Makes LongJump operate only while its bind is held instead of toggling normally.");
      this.register(this.mode);
      this.register(this.boost);
      this.register(this.boostCooldown);
      this.register(this.autoDisable);
      this.register(this.onGround);
      this.register(this.hold);
      this.hasBeenGrounded = false;
      this.stage = 0;
      this.damaged = false;
      this.ticksSinceDamage = 0;
      this.boostCount = 0;
      this.lastBoostTime = 0L;
      INSTANCE = this;
   }

   public void onEnable() {
      super.onEnable();
      this.hasBeenGrounded = false;
      this.damaged = false;
      this.ticksSinceDamage = 0;
      this.boostCount = 0;
      this.lastBoostTime = 0L;
      if (!Feature.fullNullCheck()) {
         if (!InstantSpeed.paused) {
            InstantSpeed.paused = true;
         }

      }
   }

   public void onDisable() {
      super.onDisable();
      if (!Feature.fullNullCheck()) {
         if (InstantSpeed.paused) {
            InstantSpeed.paused = false;
         }

      }
   }

   public void toggle() {
      if (!(Boolean)this.hold.getValue()) {
         super.toggle();
      }
   }

   @SubscribeEvent
   public void onUpdate(TickEvent.ClientTickEvent event) {
      if (!Feature.fullNullCheck()) {
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
            if (!this.hasBeenGrounded) {
               this.hasBeenGrounded = mc.player.onGround;
            }

            if (this.hasBeenGrounded || !(Boolean)this.onGround.getValue()) {
               if (this.mode.getValue() == LongJump.Mode.Strict) {
                  if (mc.player.hurtTime > 0) {
                     if (!this.damaged) {
                        this.damaged = true;
                        this.ticksSinceDamage = 0;
                     }

                     ++this.ticksSinceDamage;
                  } else if (this.damaged) {
                     ++this.ticksSinceDamage;
                     if (this.ticksSinceDamage > 40) {
                        this.damaged = false;
                        this.ticksSinceDamage = 0;
                     }
                  }
               }

               double deltaX = mc.player.posX - mc.player.prevPosX;
               double deltaZ = mc.player.posZ - mc.player.prevPosZ;
               this.lastDist = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
               if (this.canSprint()) {
                  mc.player.setSprinting(true);
               }

            }
         }
      }
   }

   @SubscribeEvent
   public void onMove(MoveEvent event) {
      if (!Feature.fullNullCheck()) {
         if (this.isEnabled()) {
            if (this.hasBeenGrounded || !(Boolean)this.onGround.getValue()) {
               if (!mc.player.isElytraFlying() && !mc.player.isInLava() && !mc.player.isInWater()) {
                  if (this.mode.getValue() == LongJump.Mode.Normal) {
                     this.handleNormalMode(event);
                  } else if (this.mode.getValue() == LongJump.Mode.Strict) {
                     this.handleStrictMode(event);
                  }

                  ++this.stage;
               }
            }
         }
      }
   }

   private void handleNormalMode(MoveEvent event) {
      switch (this.stage) {
         case 0:
            ++this.stage;
            this.lastDist = (double)0.0F;
            break;
         case 1:
         default:
            if ((!mc.world.getCollisionBoxes(mc.player, mc.player.getEntityBoundingBox().offset((double)0.0F, mc.player.motionY, (double)0.0F)).isEmpty() || mc.player.collidedVertically) && this.stage > 0) {
               this.stage = mc.player.moveForward == 0.0F && mc.player.moveStrafing == 0.0F ? 0 : 1;
            }

            this.moveSpeed = this.lastDist - this.lastDist / (double)159.0F;
            break;
         case 2:
            double motionY = 0.40123128;
            if ((mc.player.moveForward != 0.0F || mc.player.moveStrafing != 0.0F) && mc.player.onGround) {
               if (mc.player.isPotionActive(MobEffects.JUMP_BOOST)) {
                  PotionEffect jumpBoost = mc.player.getActivePotionEffect(MobEffects.JUMP_BOOST);
                  if (jumpBoost != null) {
                     motionY += (double)((float)(jumpBoost.getAmplifier() + 1) * 0.1F);
                  }
               }

               event.setY(mc.player.motionY = motionY);
               this.moveSpeed *= 2.149;
            }
            break;
         case 3:
            this.moveSpeed = this.lastDist - 0.76 * (this.lastDist - this.getBaseMoveSpeed());
      }

      this.moveSpeed = Math.max(this.moveSpeed, this.getBaseMoveSpeed());
      this.setMotion(event);
   }

   private void handleStrictMode(MoveEvent event) {
      if (this.damaged) {
         switch (this.stage) {
            case 0:
               ++this.stage;
               this.lastDist = (double)0.0F;
               break;
            case 1:
               if (mc.player.hurtTime == 9 && mc.player.onGround) {
                  this.moveSpeed = this.getBaseMoveSpeed() * 1.8;
               }
               break;
            case 2:
               if ((mc.player.moveForward != 0.0F || mc.player.moveStrafing != 0.0F) && mc.player.onGround) {
                  long currentTime = System.currentTimeMillis();
                  long timeSinceLastBoost = currentTime - this.lastBoostTime;
                  if (this.boostCount <= 0 || timeSinceLastBoost >= (long)(Integer)this.boostCooldown.getValue()) {
                     double motionY = 0.38;
                     if (mc.player.isPotionActive(MobEffects.JUMP_BOOST)) {
                        PotionEffect jumpBoost = mc.player.getActivePotionEffect(MobEffects.JUMP_BOOST);
                        if (jumpBoost != null) {
                           motionY += (double)((float)(jumpBoost.getAmplifier() + 1) * 0.08F);
                        }
                     }

                     event.setY(mc.player.motionY = motionY);
                     this.moveSpeed = this.lastDist * 1.4;
                     ++this.boostCount;
                     this.lastBoostTime = currentTime;
                  }
               }
               break;
            case 3:
               this.moveSpeed = this.lastDist - 0.66 * (this.lastDist - this.getBaseMoveSpeed());
               break;
            default:
               if ((!mc.world.getCollisionBoxes(mc.player, mc.player.getEntityBoundingBox().offset((double)0.0F, mc.player.motionY, (double)0.0F)).isEmpty() || mc.player.collidedVertically) && this.stage > 0) {
                  this.stage = mc.player.moveForward == 0.0F && mc.player.moveStrafing == 0.0F ? 0 : 1;
               }

               this.moveSpeed = this.lastDist - this.lastDist / (double)160.0F;
         }

         this.moveSpeed = Math.max(this.moveSpeed, this.getBaseMoveSpeed());
         if (this.moveSpeed > this.getBaseMoveSpeed() * 2.8) {
            this.moveSpeed = this.getBaseMoveSpeed() * 2.8;
         }

         this.setMotion(event);
      }
   }

   private void setMotion(MoveEvent event) {
      double forward = (double)mc.player.movementInput.moveForward;
      double strafe = (double)mc.player.movementInput.moveStrafe;
      double yaw = (double)mc.player.rotationYaw;
      if (forward == (double)0.0F && strafe == (double)0.0F) {
         event.setX((double)0.0F);
         event.setZ((double)0.0F);
      } else {
         if (forward != (double)0.0F && strafe != (double)0.0F) {
            forward *= Math.sin((Math.PI / 4D));
            strafe *= Math.cos((Math.PI / 4D));
         }

         double yawRadians = Math.toRadians(yaw);
         double sinYaw = Math.sin(yawRadians);
         double cosYaw = Math.cos(yawRadians);
         event.setX((forward * this.moveSpeed * -sinYaw + strafe * this.moveSpeed * cosYaw) * 0.99);
         event.setZ((forward * this.moveSpeed * cosYaw - strafe * this.moveSpeed * -sinYaw) * 0.99);
      }

   }

   private double getBaseMoveSpeed() {
      double baseSpeed = 0.272;
      if (mc.player.isPotionActive(MobEffects.SPEED)) {
         int amplifier = ((PotionEffect)Objects.requireNonNull(mc.player.getActivePotionEffect(MobEffects.SPEED))).getAmplifier();
         baseSpeed *= (double)1.0F + 0.2 * (double)(amplifier + 1);
      }

      return baseSpeed * ((Number)this.boost.getValue()).doubleValue();
   }

   @SubscribeEvent
   public void onPacket(PacketEvent event) {
      if (this.isEnabled()) {
         if (event.getPacket() instanceof SPacketPlayerPosLook && (Boolean)this.autoDisable.getValue()) {
            this.toggle();
         }

      }
   }

   private boolean canSprint() {
      return (mc.player.movementInput.moveStrafe != 0.0F || mc.player.moveForward != 0.0F) && !mc.player.isActiveItemStackBlocking() && !mc.player.isOnLadder() && !mc.player.collidedHorizontally && mc.player.getFoodStats().getFoodLevel() > 6;
   }

   public static enum Mode {
      Normal,
      Strict;
   }
}
