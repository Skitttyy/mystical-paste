package fat.vonware.features.modules.movement;

import fat.vonware.Vonware;
import fat.vonware.event.events.MoveEvent;
import fat.vonware.event.events.PacketEvent;
import fat.vonware.features.modules.Module;
import fat.vonware.features.setting.Setting;
import fat.vonware.util.EntityUtil;
import fat.vonware.util.MovementUtil;
import fat.vonware.util.Timer;
import fat.vonware.util.Phase.PacketUtil;
import fat.vonware.util.Phase.TimeVec3d;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.network.play.client.CPacketConfirmTeleport;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.network.play.server.SPacketPlayerPosLook;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class OldPhaseWalk extends Module {
   public Setting<Boolean> shiftDown = new Setting<Boolean>("ShiftDown", true, "Toggles the ShiftDown feature used by OldPhaseWalk.");
   public Setting<Boolean> antiVoid = new Setting<Boolean>("AntiVoid", true, (v) -> (Boolean)this.shiftDown.getValue(), "Toggles the AntiVoid feature used by OldPhaseWalk.");
   public Setting<Boolean> startPosCheck = new Setting<Boolean>("StartPosCheck", true, (v) -> this.accelMode.getValue() != OldPhaseWalk.AccelMode.Off, "Toggles the StartPosCheck feature used by OldPhaseWalk.");
   public Setting<Boolean> cancel = new Setting<Boolean>("Cancel", true, "Toggles the Cancel feature used by OldPhaseWalk.");
   public Setting<AccelMode> accelMode;
   protected final Setting<PhaseMode> mode;
   protected final Setting<CancelRotsMode> cancelMoving;
   public Setting<Float> delay;
   public Setting<Float> shiftDownDelay;
   protected int timerLeft;
   protected int teleportID;
   protected AxisAlignedBB startingBB;
   protected final Timer timer;
   protected final Timer shiftTimer;
   protected final Timer limitTimer;
   protected final Map<Integer, TimeVec3d> posLooks;
   private long lastPacketTime;
   private int adjustmentDepth;
   private static final int MAX_ADJUSTMENT_DEPTH = 3;

   public OldPhaseWalk() {
      super("OldPhaseWalk", "New (Old) strict.2b2tpvp.net phase (Now Patched)", Module.Category.MOVEMENT, true, false, false);
      this.accelMode = (new Setting("AccelMode", OldPhaseWalk.AccelMode.HyperAccel)).withDescription((Function<AccelMode, String>)((mode) -> {
         switch (mode) {
            case Off:
               return "Off: Disables acceleration and keeps phase movement at its base behavior.";
            case NormalAccel:
               return "NormalAccel: Gradually increases phase movement using the normal acceleration path.";
            case HyperAccel:
               return "HyperAccel: Uses the stronger hyper-acceleration path while phase walking.";
            default:
               return "";
         }
      }));
      this.mode = (new Setting("Mode", OldPhaseWalk.PhaseMode.Fast)).withDescription((Function<PhaseMode, String>)((mode) -> {
         switch (mode) {
            case Fast:
               return "Fast: Uses the fast packet/teleport-ID phase movement path.";
            case Wall:
               return "Wall: Uses the wall-phase movement path and its configurable Delay.";
            default:
               return "";
         }
      }));
      this.cancelMoving = (new Setting("CancelRots", OldPhaseWalk.CancelRotsMode.Always)).withDescription((Function<CancelRotsMode, String>)((mode) -> {
         switch (mode) {
            case Only_Phase:
               return "Only_Phase: Cancels rotation/movement handling only while the player is phasing.";
            case Only_Moving:
               return "Only_Moving: Cancels it only while the player is moving.";
            case Moving_Phase:
               return "Moving_Phase: Cancels it only when the player is both moving and phasing.";
            case Off:
               return "Off: Disables this cancellation behavior.";
            case Always:
               return "Always: Always applies the cancellation while OldPhaseWalk is active.";
            default:
               return "";
         }
      }));
      this.delay = new Setting<Float>("Delay", 2.4F, 0.0F, 5.0F, (v) -> this.mode.getValue() == OldPhaseWalk.PhaseMode.Wall, "Adjusts the Delay offset used by OldPhaseWalk.");
      this.shiftDownDelay = new Setting<Float>("ShiftDownDelay", 3.5F, 0.0F, 5.0F, (v) -> (Boolean)this.shiftDown.getValue(), "Adjusts the ShiftDownDelay offset used by OldPhaseWalk.");
      this.lastPacketTime = 0L;
      this.adjustmentDepth = 0;
      this.register(this.shiftDown);
      this.register(this.antiVoid);
      this.register(this.accelMode);
      this.register(this.startPosCheck);
      this.register(this.cancel);
      this.register(this.mode);
      this.register(this.cancelMoving);
      this.register(this.delay);
      this.register(this.shiftDownDelay);
      this.timerLeft = 0;
      this.teleportID = -1;
      this.timer = new Timer();
      this.shiftTimer = new Timer();
      this.limitTimer = new Timer();
      this.posLooks = new ConcurrentHashMap();
   }

   public void onEnable() {
      if (mc.player != null && this.mode.getValue() == OldPhaseWalk.PhaseMode.Fast && !mc.isSingleplayer()) {
         this.setBB();
         PacketUtil.sendNoEvent(new CPacketPlayer.Position((double)564647.0F, (double)-72.0F, (double)867595.0F, false));
      }

   }

   public String getDisplayInfo() {
      return ((PhaseMode)this.mode.getValue()).name();
   }

   public void onDisable() {
      Vonware.timerManager.reset();
   }

   public void onToggle() {
      this.timerLeft = -1;
   }

   protected void sendBounds(double x, double y, double z) {
      boolean onGround = mc.player.onGround;
      PacketUtil.sendNoEvent(new CPacketPlayer.Position(x, y, z, onGround));
      PacketUtil.sendNoEvent(new CPacketPlayer.Position(x, y - (double)87.0F, z, onGround));
      if (this.teleportID > 0) {
         PacketUtil.send(new CPacketConfirmTeleport(++this.teleportID));
         this.posLooks.put(this.teleportID, new TimeVec3d(x, y, z, System.currentTimeMillis()));
      }

   }

   @SubscribeEvent
   public void onMove(MoveEvent event) {
      if (!fullNullCheck()) {
         if (!mc.player.isSpectator()) {
            if (MovementUtil.isPhasing()) {
               if (this.vertical()) {
                  return;
               }

               if (this.mode.getValue() == OldPhaseWalk.PhaseMode.Fast && this.teleportID == -1) {
                  return;
               }

               if (!this.preMovement(event) && MovementUtil.wasdKeys() && mc.player.onGround) {
                  this.horizontal(event);
               }

               boolean emptyBB = mc.world.getEntitiesWithinAABBExcludingEntity(mc.player, this.startingBB).isEmpty();
               boolean isStartPosBlocked = (Boolean)this.startPosCheck.getValue() && !emptyBB;
               if (isStartPosBlocked) {
                  return;
               }

               if (this.accelMode.getValue() == OldPhaseWalk.AccelMode.NormalAccel) {
                  MovementUtil.strafe(event, (double)this.getAccelSpeed());
               } else if (this.accelMode.getValue() == OldPhaseWalk.AccelMode.HyperAccel) {
                  this.phaseSpeed(event, (double)this.getAccelSpeed());
               }
            }

         }
      }
   }

   private void horizontal(MoveEvent event) {
      double ratio = this.horizontalLength(event.getX(), event.getZ()) / 0.06;
      double x = event.getX() / ratio;
      double z = event.getZ() / ratio;
      x = MathHelper.clamp(x, -0.3, 0.3);
      z = MathHelper.clamp(z, -0.3, 0.3);
      Vec3d playerVec = new Vec3d(mc.player.posX, mc.player.posY, mc.player.posZ);
      Vec3d vec = playerVec.add(x, (double)0.0F, z);
      if (this.mode.getValue() == OldPhaseWalk.PhaseMode.Fast) {
         this.timerLeft = 3;
         if (System.currentTimeMillis() - this.lastPacketTime > 50L) {
            this.sendBounds(vec.x, vec.y, vec.z);
            mc.player.setPosition(vec.x, vec.y, vec.z);
            this.setMotion(event);
            this.lastPacketTime = System.currentTimeMillis();
         }
      } else if (this.timer.passed((long)((Float)this.delay.getValue() * 100.0F)) && mc.player.collidedHorizontally) {
         this.sendBounds(vec.x, vec.y, vec.z);
         mc.player.setPosition(vec.x, vec.y, vec.z);
         this.timer.reset();
         this.setMotion(event);
      }

   }

   private boolean vertical() {
      boolean down = (Boolean)this.shiftDown.getValue() && mc.gameSettings.keyBindSneak.isKeyDown() && mc.player.collidedVertically;
      if (!down) {
         return false;
      } else if (!this.shiftTimer.passed((long)((Float)this.shiftDownDelay.getValue() * 100.0F))) {
         return false;
      } else if (mc.player.posY == (double)1.0F && (Boolean)this.antiVoid.getValue()) {
         return false;
      } else {
         double offset = mc.player.getEntityBoundingBox().minY - 0.0042123;
         this.sendBounds(mc.player.posX, offset, mc.player.posZ);
         mc.player.setPosition(mc.player.posX, offset, mc.player.posZ);
         this.shiftTimer.reset();
         return true;
      }
   }

   protected float getAccelSpeed() {
      float baseSpeed = 0.2873F;
      PotionEffect speed = mc.player.getActivePotionEffect(MobEffects.SPEED);
      if (speed != null) {
         int amplifier = speed.getAmplifier();
         baseSpeed *= (float)((double)1.0F + (double)0.1F * (double)(amplifier + 1));
      }

      return baseSpeed;
   }

   private void phaseSpeed(MoveEvent event, double speed) {
      if (EntityUtil.isMoving()) {
         float boost = 0.0F;
         if (!mc.player.collidedHorizontally && MovementUtil.isPhasing() && mc.player.onGround) {
            boost = (float)(Math.random() * (double)0.2F + (double)0.1F);
         }

         double[] strafe = MovementUtil.strafe(speed + (double)boost);
         event.setX(strafe[0]);
         event.setZ(strafe[1]);
      } else {
         event.setX((double)0.0F);
         event.setZ((double)0.0F);
      }

   }

   private void setMotion(MoveEvent event) {
      mc.player.setVelocity((double)0.0F, (double)0.0F, (double)0.0F);
      event.setX((double)0.0F);
      event.setY((double)0.0F);
      event.setZ((double)0.0F);
   }

   private boolean preMovement(MoveEvent event) {
      try {
         this.adjustmentDepth = 0;
         Vec3d predictVec = this.predict(mc.player);
         Vec3d playerVec = new Vec3d(mc.player.posX, mc.player.posY, mc.player.posZ);
         Vec3d eventVec = playerVec.add(new Vec3d(event.getX(), event.getY(), event.getZ()));
         double length = this.horizontalLength(predictVec.subtract(eventVec));
         double length2 = this.horizontalLength(predictVec.subtract(playerVec));
         boolean result = length <= 0.05 || length2 >= 0.1;
         return result;
      } catch (Exception var10) {
         return true;
      }
   }

   public Vec3d adjustMovementForCollisions(Entity entity, Vec3d movement) {
      if (this.adjustmentDepth > 3) {
         return movement;
      } else {
         ++this.adjustmentDepth;

         Vec3d vec3d;
         try {
            AxisAlignedBB box = entity.getEntityBoundingBox();
            List<AxisAlignedBB> list = mc.world.getCollisionBoxes(entity, this.stretch(box, movement));
            if (!(movement.lengthSquared() < 1.0E-6)) {
               vec3d = movement.lengthSquared() == (double)0.0F ? movement : this.adjustMovementForCollisions(movement, box, list);
               boolean bl = movement.x != vec3d.x;
               boolean bl2 = movement.y != vec3d.y;
               boolean bl3 = movement.z != vec3d.z;
               boolean bl4 = entity.onGround || bl2 && movement.y < (double)0.0F;
               if (entity.stepHeight > 0.0F && bl4 && (bl || bl3)) {
                  Vec3d vec3d2 = this.adjustMovementForCollisions(new Vec3d(movement.x, (double)entity.stepHeight, movement.z), box, list);
                  Vec3d vec3d3 = this.adjustMovementForCollisions(new Vec3d((double)0.0F, (double)entity.stepHeight, (double)0.0F), this.stretch(box, movement.x, (double)0.0F, movement.z), list);
                  if (vec3d3.y < (double)entity.stepHeight) {
                     Vec3d vec3d4 = this.adjustMovementForCollisions(new Vec3d(movement.x, (double)0.0F, movement.z), box.offset(vec3d3), list).add(vec3d3);
                     if (this.horizontalLengthSquared(vec3d4) > this.horizontalLengthSquared(vec3d2)) {
                        vec3d2 = vec3d4;
                     }
                  }

                  if (this.horizontalLengthSquared(vec3d2) > this.horizontalLengthSquared(vec3d)) {
                     Vec3d var18 = vec3d2.add(this.adjustMovementForCollisions(new Vec3d((double)0.0F, -vec3d2.y + movement.y, (double)0.0F), box.offset(vec3d2), list));
                     return var18;
                  }
               }

               Vec3d var17 = vec3d;
               return var17;
            }

            vec3d = movement;
         } finally {
            --this.adjustmentDepth;
         }

         return vec3d;
      }
   }

   private double horizontalLength(Vec3d vec3d) {
      return this.horizontalLength(vec3d.x, vec3d.z);
   }

   private double horizontalLength(double x, double z) {
      return Math.sqrt(x * x + z * z);
   }

   public double horizontalLengthSquared(Vec3d vec3d) {
      return vec3d.x * vec3d.x + vec3d.z * vec3d.z;
   }

   public AxisAlignedBB stretch(AxisAlignedBB bb, Vec3d vec3d) {
      return this.stretch(bb, vec3d.x, vec3d.y, vec3d.z);
   }

   public AxisAlignedBB stretch(AxisAlignedBB bb, double x, double y, double z) {
      double d = bb.minX;
      double e = bb.minY;
      double f = bb.minZ;
      double g = bb.maxX;
      double h = bb.maxY;
      double i = bb.maxZ;
      if (x < (double)0.0F) {
         d += x;
      } else if (x > (double)0.0F) {
         g += x;
      }

      if (y < (double)0.0F) {
         e += y;
      } else if (y > (double)0.0F) {
         h += y;
      }

      if (z < (double)0.0F) {
         f += z;
      } else if (z > (double)0.0F) {
         i += z;
      }

      return new AxisAlignedBB(d, e, f, g, h, i);
   }

   public Vec3d predict(EntityPlayer player) {
      Vec3d movement = new Vec3d(player.motionX, player.motionY, player.motionZ);
      movement = this.adjustMovementForSneaking(player.getEntityBoundingBox(), movement);
      movement = this.adjustMovementForCollisions(player, movement);
      return (new Vec3d(mc.player.posX, mc.player.posY, mc.player.posZ)).add(movement);
   }

   public Vec3d adjustMovementForSneaking(AxisAlignedBB bb, Vec3d vec3d) {
      double x = vec3d.x;
      double y = vec3d.y;
      double z = vec3d.z;

      while(x != (double)0.0F && mc.world.getCollisionBoxes(mc.player, bb.offset(x, (double)(-mc.player.stepHeight), (double)0.0F)).isEmpty()) {
         if (x < 0.05 && x >= -0.05) {
            x = (double)0.0F;
         } else if (x > (double)0.0F) {
            x -= 0.05;
         } else {
            x += 0.05;
         }
      }

      while(z != (double)0.0F && mc.world.getCollisionBoxes(mc.player, bb.offset((double)0.0F, (double)(-mc.player.stepHeight), z)).isEmpty()) {
         if (z < 0.05 && z >= -0.05) {
            z = (double)0.0F;
         } else if (z > (double)0.0F) {
            z -= 0.05;
         } else {
            z += 0.05;
         }
      }

      while(x != (double)0.0F && z != (double)0.0F && mc.world.getCollisionBoxes(mc.player, bb.offset(x, (double)(-mc.player.stepHeight), z)).isEmpty()) {
         if (x < 0.05 && x >= -0.05) {
            x = (double)0.0F;
         } else if (x > (double)0.0F) {
            x -= 0.05;
         } else {
            x += 0.05;
         }

         if (z < 0.05 && z >= -0.05) {
            z = (double)0.0F;
         } else if (z > (double)0.0F) {
            z -= 0.05;
         } else {
            z += 0.05;
         }
      }

      return new Vec3d(x, y, z);
   }

   private Vec3d adjustMovementForCollisions(Vec3d movement, AxisAlignedBB bb, List<AxisAlignedBB> collisions) {
      if (collisions.isEmpty()) {
         return movement;
      } else {
         double x = movement.x;
         double y = movement.y;
         double z = movement.z;
         if (y != (double)0.0F) {
            for(AxisAlignedBB collision : collisions) {
               y = collision.calculateYOffset(bb, y);
            }

            bb = bb.offset((double)0.0F, y, (double)0.0F);
         }

         if (x != (double)0.0F) {
            for(AxisAlignedBB collision : collisions) {
               x = collision.calculateXOffset(bb, x);
            }

            if (x != (double)0.0F) {
               bb = bb.offset(x, (double)0.0F, (double)0.0F);
            }
         }

         if (z != (double)0.0F) {
            for(AxisAlignedBB collision : collisions) {
               z = collision.calculateZOffset(bb, z);
            }
         }

         return new Vec3d(x, y, z);
      }
   }

   @SubscribeEvent
   public void onPacketSend(PacketEvent.Send event) {
      switch ((CancelRotsMode)this.cancelMoving.getValue()) {
         case Only_Phase:
            if (!MovementUtil.isPhasing()) {
               return;
            }
            break;
         case Only_Moving:
            if (!MovementUtil.wasdKeys()) {
               return;
            }
            break;
         case Moving_Phase:
            if (!MovementUtil.wasdKeys() && !MovementUtil.isPhasing()) {
               return;
            }
         case Off:
      }

      if (event.getPacket() instanceof CPacketPlayer && this.mode.getValue() == OldPhaseWalk.PhaseMode.Fast && MovementUtil.isPhasing()) {
         CPacketPlayer packet = (CPacketPlayer)event.getPacket();
         boolean holdingCrystal = mc.player.getHeldItemMainhand().getItem() == Items.END_CRYSTAL || mc.player.getHeldItemOffhand().getItem() == Items.END_CRYSTAL;
         if (holdingCrystal) {
            if (packet instanceof CPacketPlayer.PositionRotation) {
               PacketUtil.sendNoEvent(new CPacketPlayer.Position(packet.getX(mc.player.posX), packet.getY(mc.player.posY), packet.getZ(mc.player.posZ), mc.player.onGround));
               event.setCanceled(true);
            }
         } else if (packet instanceof CPacketPlayer.PositionRotation) {
            PacketUtil.sendNoEvent(new CPacketPlayer.Position(packet.getX(mc.player.posX), packet.getY(mc.player.posY), packet.getZ(mc.player.posZ), mc.player.onGround));
            event.setCanceled(true);
         } else if (packet instanceof CPacketPlayer.Rotation) {
            event.setCanceled(true);
         }
      }

   }

   @SubscribeEvent
   public void onPacketReceive(PacketEvent.Receive event) {
      if (event.getPacket() instanceof SPacketPlayerPosLook) {
         SPacketPlayerPosLook packet = (SPacketPlayerPosLook)event.getPacket();
         if ((Boolean)this.cancel.getValue() && mc.player.isEntityAlive()) {
            if (this.teleportID <= 0) {
               this.teleportID = packet.getTeleportId();
            } else if (mc.world.isBlockLoaded(new BlockPos(mc.player.posX, mc.player.posY, mc.player.posZ), false) && this.posLooks.containsKey(packet.getTeleportId())) {
               TimeVec3d vec = (TimeVec3d)this.posLooks.get(packet.getTeleportId());
               double tolerance = 0.001;
               boolean xMatch = Math.abs(vec.x - packet.getX()) < tolerance;
               boolean yMatch = Math.abs(vec.y - packet.getY()) < tolerance;
               boolean zMatch = Math.abs(vec.z - packet.getZ()) < tolerance;
               if (xMatch && yMatch && zMatch) {
                  this.posLooks.remove(packet.getTeleportId());
                  event.setCanceled(true);
                  return;
               }
            }
         }

         this.setBB();
         this.teleportID = packet.getTeleportId();
         this.limitTimer.reset();
      }
   }

   public void onTick() {
      this.posLooks.entrySet().removeIf((entry) -> System.currentTimeMillis() - ((TimeVec3d)entry.getValue()).getTime() > TimeUnit.SECONDS.toMillis(30L));
   }

   protected void setBB() {
      this.startingBB = mc.player.getEntityBoundingBox().expand((double)0.125F, (double)0.0F, (double)0.125F).contract((double)0.0F, (double)1.0F, (double)0.0F);
   }

   public static enum PhaseMode {
      Fast,
      Wall;
   }

   public static enum CancelRotsMode {
      Only_Phase,
      Only_Moving,
      Moving_Phase,
      Always,
      Off;
   }

   public static enum AccelMode {
      Off,
      NormalAccel,
      HyperAccel;
   }
}
