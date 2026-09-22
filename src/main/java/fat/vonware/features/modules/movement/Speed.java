package fat.vonware.features.modules.movement;

import fat.vonware.Vonware;
import fat.vonware.event.events.MotionUpdateEvent;
import fat.vonware.event.events.MoveEvent;
import fat.vonware.event.events.PacketEvent;
import fat.vonware.features.modules.Module;
import fat.vonware.features.setting.Setting;
import fat.vonware.manager.NCPManager;
import fat.vonware.util.MovementUtil;
import fat.vonware.util.PlayerUtil;
import fat.vonware.util.PositionUtil;
import fat.vonware.util.Timer;
import java.util.Objects;
import java.util.function.Function;
import net.minecraft.entity.Entity;
import net.minecraft.init.MobEffects;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.SPacketEntityVelocity;
import net.minecraft.network.play.server.SPacketExplosion;
import net.minecraft.network.play.server.SPacketPlayerPosLook;
import net.minecraft.potion.PotionEffect;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class Speed extends Module {
   public final Setting<SpeedMode> speedMode;
   public final Setting<StrafeMode> speedProperties;
   public final Setting<Boolean> inWater;
   public final Setting<Double> strafeSpeed;
   public final Setting<Boolean> useTimer;
   public final Setting<Integer> lagTime;
   public final Setting<Double> speedCap;
   public final Setting<Boolean> potionScaling;
   public final Setting<Boolean> strafeBoost;
   public final Setting<Double> boostFactor;
   public final Setting<Integer> boostTimeout;
   public boolean stop;
   public double speed;
   public double distance;
   public int stage;
   public double lastDist;
   public boolean boost;
   private double boostMomentum;
   private double lastKnockbackMagnitude;
   private final Timer boostTimer;
   private boolean wasOnGround;

   public Speed() {
      super("Speed", "Automatically strafes for you", Module.Category.MOVEMENT, true, false, false);
      this.speedMode = (new Setting("Mode", Speed.SpeedMode.Strafe)).withDescription((Function<SpeedMode, String>)((mode) -> {
         switch (mode) {
            case Strafe:
               return "Strafe: Uses the standard staged strafe speed values.";
            case StrictStrafe:
               return "StrictStrafe: Uses the stricter strafe base speed and movement values intended for tighter checks.";
            default:
               return "";
         }
      }));
      this.speedProperties = (new Setting("Properties", Speed.StrafeMode.Hardcode)).withDescription((Function<StrafeMode, String>)((mode) -> {
         switch (mode) {
            case Hardcode:
               return "Hardcode: Uses Vonware's built-in base strafe speed for the selected Speed mode.";
            case Custom:
               return "Custom: Uses the configurable StrafeSpeed value as the base movement speed.";
            default:
               return "";
         }
      }));
      this.inWater = new Setting<Boolean>("InWater", false, "Allows Speed to continue operating in water instead of pausing in liquids.");
      this.strafeSpeed = new Setting<Double>("StrafeSpeed", 0.2873, 0.1, (double)1.0F, (v) -> this.speedProperties.getValue() == Speed.StrafeMode.Custom, "Custom base strafe speed used when Properties is Custom.");
      this.useTimer = new Setting<Boolean>("UseTimer", true, "Applies the small timer increase used by Speed after the NCP lag window has passed.");
      this.lagTime = new Setting<Integer>("LagTime", 500, 0, 1000, "Time after a server correction that Speed waits before applying movement again.");
      this.speedCap = new Setting<Double>("SpeedCap", (double)10.0F, (double)0.0F, (double)10.0F, "Maximum horizontal speed the staged Speed calculation is allowed to reach.");
      this.potionScaling = new Setting<Boolean>("PotionScaling", true, "Includes movement-speed potion effects in the base speed calculation.");
      this.strafeBoost = new Setting<Boolean>("StrafeBoost", true, "Adds recent explosion/velocity knockback speed into the strafe calculation.");
      this.boostFactor = new Setting<Double>("BoostFactor", 0.4, 0.1, (double)1.0F, (v) -> (Boolean)this.strafeBoost.getValue(), "Multiplier applied to captured knockback before adding it to StrafeBoost.");
      this.boostTimeout = new Setting<Integer>("BoostTimeout", 400, 100, 1000, (v) -> (Boolean)this.strafeBoost.getValue(), "How long captured knockback remains available for StrafeBoost before expiring.");
      this.boostTimer = new Timer();
      this.register(this.speedMode);
      this.register(this.speedProperties);
      this.register(this.inWater);
      this.register(this.strafeSpeed);
      this.register(this.useTimer);
      this.register(this.lagTime);
      this.register(this.speedCap);
      this.register(this.potionScaling);
      this.register(this.strafeBoost);
      this.register(this.boostFactor);
      this.register(this.boostTimeout);
   }

   public double getEffectiveStrafeSpeed() {
      if (this.speedProperties.getValue() == Speed.StrafeMode.Hardcode) {
         return this.speedMode.getValue() == Speed.SpeedMode.StrictStrafe ? 0.2589 : 0.2873;
      } else {
         return (Double)this.strafeSpeed.getValue();
      }
   }

   private double getDistance2D() {
      double deltaX = mc.player.posX - mc.player.prevPosX;
      double deltaZ = mc.player.posZ - mc.player.prevPosZ;
      return Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
   }

   public void onKnockbackTaken(float factor, float knockbackSpeed) {
      if ((Boolean)this.strafeBoost.getValue() && !(knockbackSpeed <= 0.0F) && mc.player != null) {
         this.boostTimer.setDelay(((Integer)this.boostTimeout.getValue()).longValue());
         boolean fresh = this.boostTimer.isPassed() || this.lastKnockbackMagnitude <= (double)0.0F;
         double rawDelta = fresh ? (double)knockbackSpeed : Math.max((double)0.0F, (double)knockbackSpeed - this.lastKnockbackMagnitude);
         double contribution = rawDelta * (double)factor;
         double acceptedMagnitude = (double)(knockbackSpeed * factor);
         this.lastKnockbackMagnitude = (double)knockbackSpeed;
         this.boostTimer.resetDelay();
         if (contribution > (double)0.0F) {
            this.speed += contribution;
            this.distance += contribution;
         }

         this.boostMomentum = Math.max(this.boostMomentum, acceptedMagnitude);
      }
   }

   private double getBoostFloor() {
      if ((Boolean)this.strafeBoost.getValue() && MovementUtil.isMoving()) {
         this.boostTimer.setDelay(((Integer)this.boostTimeout.getValue()).longValue());
         if (this.boostTimer.isPassed()) {
            this.lastKnockbackMagnitude = (double)0.0F;
         }

         double floor = this.boostMomentum;
         this.boostMomentum *= 0.98;
         double base = MovementUtil.getSpeed(false, this.getEffectiveStrafeSpeed());
         if (this.boostMomentum <= base) {
            this.boostMomentum = (double)0.0F;
         }

         return floor;
      } else {
         this.boostMomentum = (double)0.0F;
         this.lastKnockbackMagnitude = (double)0.0F;
         return (double)0.0F;
      }
   }

   private void resetMovementState(boolean clearBoost) {
      if (mc.player == null) {
         this.stage = 0;
         this.speed = (double)0.0F;
         this.distance = (double)0.0F;
         this.lastDist = (double)0.0F;
         this.boost = false;
         this.stop = false;
         this.wasOnGround = false;
         if (clearBoost) {
            this.boostMomentum = (double)0.0F;
            this.lastKnockbackMagnitude = (double)0.0F;
         }

      } else {
         this.distance = (double)0.0F;
         this.lastDist = (double)0.0F;
         this.speed = MovementUtil.getSpeed();
         this.stage = 4;
         this.boost = false;
         this.stop = false;
         this.wasOnGround = mc.player.onGround;
         if (clearBoost) {
            this.boostMomentum = (double)0.0F;
            this.lastKnockbackMagnitude = (double)0.0F;
         }

      }
   }

   @SubscribeEvent
   public void onUpdateWalkingPlayer(MotionUpdateEvent event) {
      if (event.getStage() == 0) {
         this.lastDist = this.distance;
         this.distance = this.getDistance2D();
      }

   }

   @SubscribeEvent
   public void onPacketReceive(PacketEvent.Receive event) {
      if (!fullNullCheck()) {
         Packet<?> Packet = event.getPacket();
         if (Packet instanceof SPacketPlayerPosLook) {
            this.resetMovementState(true);
            PlayerUtil.setTimer(1.0F);
         }

         if (Packet instanceof SPacketExplosion && (Boolean)this.strafeBoost.getValue()) {
            SPacketExplosion packet = (SPacketExplosion)Packet;
            if (mc.player.getDistance(packet.getX(), packet.getY(), packet.getZ()) <= (double)6.0F && (packet.getMotionX() != 0.0F || packet.getMotionZ() != 0.0F)) {
               float knockbackSpeed = (float)Math.hypot((double)packet.getMotionX(), (double)packet.getMotionZ());
               this.onKnockbackTaken(((Double)this.boostFactor.getValue()).floatValue(), knockbackSpeed);
            }
         }

         if (Packet instanceof SPacketEntityVelocity && (Boolean)this.strafeBoost.getValue()) {
            SPacketEntityVelocity packet = (SPacketEntityVelocity)Packet;
            if (packet.getEntityID() == mc.player.getEntityId()) {
               float knockbackSpeed = (float)(Math.hypot((double)packet.getMotionX(), (double)packet.getMotionZ()) / (double)8000.0F);
               if (knockbackSpeed > 0.0F) {
                  this.onKnockbackTaken(((Double)this.boostFactor.getValue()).floatValue(), knockbackSpeed);
               }
            }
         }

      }
   }

   @SubscribeEvent
   public void onMove(MoveEvent event) {
      if (((Boolean)this.inWater.getValue() || !PositionUtil.inLiquid() && !PositionUtil.inLiquid(true)) && !mc.player.isOnLadder() && !mc.player.isEntityInsideOpaqueBlock()) {
         if (!MovementUtil.isMoving()) {
            this.wasOnGround = mc.player.onGround;
         } else if (mc.player.isElytraFlying()) {
            this.resetMovementState(true);
         } else if (((LongJump)Vonware.moduleManager.getModuleByClass(LongJump.class)).isEnabled()) {
            this.resetMovementState(true);
         } else if (!NCPManager.INSTANCE.passed((Integer)this.lagTime.getValue())) {
            PlayerUtil.setTimer(1.0F);
         } else {
            if ((Boolean)this.useTimer.getValue() && NCPManager.INSTANCE.passed(250)) {
               PlayerUtil.setTimer(1.0888F);
            }

            boolean onGround = mc.player.onGround;
            if (onGround && !this.wasOnGround && this.stage <= 0) {
               this.stage = 1;
               this.boost = false;
            }

            this.wasOnGround = onGround;
            if (this.speedMode.getValue() == Speed.SpeedMode.Strafe || this.speedMode.getValue() == Speed.SpeedMode.StrictStrafe) {
               double boostFloor = this.getBoostFloor();
               if (this.stage == 1 && MovementUtil.isMoving()) {
                  this.speed = 1.35 * MovementUtil.getSpeed(false, this.getEffectiveStrafeSpeed()) - 0.01;
               } else if (this.stage == 2 && MovementUtil.isMoving()) {
                  double yMotion = 0.3999 + MovementUtil.getJumpSpeed();
                  event.setY(mc.player.motionY = yMotion);
                  this.speed *= this.boost ? 1.6835 : 1.395;
               } else if (this.stage == 3) {
                  this.speed = this.distance - 0.66 * (this.distance - MovementUtil.getSpeed(false, this.getEffectiveStrafeSpeed()));
                  this.boost = !this.boost;
               } else {
                  if ((mc.world.getCollisionBoxes((Entity)null, mc.player.getEntityBoundingBox().offset((double)0.0F, mc.player.motionY, (double)0.0F)).size() > 0 || mc.player.collidedVertically) && this.stage > 0) {
                     this.stage = MovementUtil.isMoving() ? 1 : 0;
                  }

                  this.speed = this.distance - this.distance / (double)159.0F;
               }

               this.speed = Math.max(this.speed, boostFloor);
               this.speed = Math.min(this.speed, this.getCap());
               MovementUtil.strafe(event, this.speed = Math.max(this.speed, MovementUtil.getSpeed(false, this.getEffectiveStrafeSpeed())));
               if (MovementUtil.isMoving()) {
                  ++this.stage;
               }
            }

         }
      } else {
         this.resetMovementState(true);
      }
   }

   public String getDisplayInfo() {
      return ((SpeedMode)this.speedMode.getValue()).toString();
   }

   public void onEnable() {
      this.resetMovementState(true);
   }

   public void onDisable() {
      PlayerUtil.setTimer(1.0F);
      this.resetMovementState(true);
   }

   public double getCap() {
      double ret = (Double)this.speedCap.getValue();
      if (!(Boolean)this.potionScaling.getValue()) {
         return ret;
      } else {
         if (mc.player.isPotionActive(MobEffects.SPEED)) {
            int amplifier = ((PotionEffect)Objects.requireNonNull(mc.player.getActivePotionEffect(MobEffects.SPEED))).getAmplifier();
            ret *= (double)1.0F + 0.2 * (double)(amplifier + 1);
         }

         return ret;
      }
   }

   public SpeedMode getMode() {
      return this.speedMode.getValue();
   }

   public static enum SpeedMode {
      Strafe,
      StrictStrafe;
   }

   public static enum StrafeMode {
      Hardcode,
      Custom;
   }
}
