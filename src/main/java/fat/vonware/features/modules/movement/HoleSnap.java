package fat.vonware.features.modules.movement;

import com.mojang.realmsclient.gui.ChatFormatting;
import fat.vonware.Vonware;
import fat.vonware.ducks.IMinecraft;
import fat.vonware.event.events.MoveEvent;
import fat.vonware.event.events.PacketEvent;
import fat.vonware.event.events.Render3DEvent;
import fat.vonware.event.events.player.InputUpdateEvent;
import fat.vonware.features.command.Command;
import fat.vonware.features.modules.Module;
import fat.vonware.features.setting.Setting;
import fat.vonware.mixin.mixins.accessor.ITimer;
import fat.vonware.util.BlockUtil;
import fat.vonware.util.Animations.render.RenderUtil;
import fat.vonware.util.holeesp.Hole;
import fat.vonware.util.holeesp.HolePathFinder;
import fat.vonware.util.holeesp.HoleUtil;
import java.awt.Color;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.minecraft.init.Blocks;
import net.minecraft.init.MobEffects;
import net.minecraft.network.play.server.SPacketPlayerPosLook;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.MovementInputFromOptions;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class HoleSnap extends Module {
   public Setting<Boolean> useTimer = new Setting<Boolean>("Use Timer", true, "Speeds up the client timer while HoleSnap is actively moving you toward a hole.");
   public Setting<Boolean> strict = new Setting<Boolean>("Strict", true, (v) -> (Boolean)this.useTimer.getValue(), "Uses the stricter timer behavior while HoleSnap is active.");
   public Setting<Boolean> safety = new Setting<Boolean>("Safety", true, "Applies HoleSnap safety checks before committing to a target hole.");
   public Setting<Boolean> terrain = new Setting<Boolean>("Terrain", true, "Allows terrain holes in HoleSnap target selection.");
   public Setting<Boolean> twoByTwo = new Setting<Boolean>("2x2", true, "Allows 2x2 holes as HoleSnap targets.");
   private final Setting<ObstacleLogic> obstacleLogic;
   private final Setting<Boolean> strictCollision;
   private final Setting<Integer> pathStepHeight;
   private final Setting<Integer> horizontalRange;
   private final Setting<Integer> verticalRange;
   public Setting<Float> timerAmount;
   public Setting<Float> postTimer;
   private final Setting<Integer> maxPostTicks;
   private final Setting<Integer> timeoutTicks;
   private final Setting<StepMode> stepMode;
   public Setting<Boolean> disableSpeed;
   private final Setting<Boolean> pauseInstantSpeed;
   public Setting<Boolean> render;
   private Hole hole;
   private boolean isIncompleteHole;
   private SnapState state;
   private int stuckTime;
   private int ranTicks;
   private int enabledTicks;
   private int postTimerCountdown;
   private final ITimer timer;
   private float oldTimer;
   private double lastPlayerY;
   private boolean instantSpeedPauseOwned;
   private boolean instantSpeedWasPaused;
   private final HolePathFinder pathFinder;
   private int pathStuckTicks;
   private int collisionSlowTicks;
   private static final int PATH_STUCK_THRESHOLD = 8;
   private static final int COLLISION_SLOW_PHASE = 2;
   private static final int COLLISION_RAMP_PHASE = 20;
   private static HoleSnap INSTANCE = new HoleSnap();

   public HoleSnap() {
      super("Holesnap", "Automatically Snaps You Into A Hole.", Module.Category.MOVEMENT, true, false, false);
      this.obstacleLogic = (new Setting("Obstacle Logic", HoleSnap.ObstacleLogic.AltPath)).withDescription((Function<ObstacleLogic, String>)((mode) -> {
         switch (mode) {
            case None:
               return "None: Moves directly toward the selected hole without calculating a detour around blocking terrain.";
            case AltPath:
               return "AltPath: Builds and follows an alternate path around blocking terrain before continuing toward the hole.";
            default:
               return "";
         }
      }));
      this.strictCollision = new Setting<Boolean>("Strict Collision", true, (v) -> this.obstacleLogic.getValue() == HoleSnap.ObstacleLogic.AltPath, "Rejects alternate-path nodes that do not pass the stricter collision checks.");
      this.pathStepHeight = new Setting<Integer>("Path Step Height", 1, 0, 2, (v) -> this.obstacleLogic.getValue() == HoleSnap.ObstacleLogic.AltPath, "Maximum block height the alternate path finder may step while routing to the hole.");
      this.horizontalRange = new Setting<Integer>("Horizontal Range", 6, 1, 10, "Maximum horizontal search range used to find a HoleSnap target.");
      this.verticalRange = new Setting<Integer>("Vertical Range", 5, 1, 6, "Maximum vertical search range used to find a HoleSnap target.");
      this.timerAmount = new Setting<Float>("Timer Amount", 6.8F, 1.0F, 15.0F, (v) -> (Boolean)this.useTimer.getValue(), "Timer multiplier used while HoleSnap is moving toward its target.");
      this.postTimer = new Setting<Float>("Post Timer", 1.0F, 0.01F, 1.0F, (v) -> (Boolean)this.useTimer.getValue() && (Float)this.timerAmount.getValue() > 1.0F, "Timer multiplier applied after the main HoleSnap boost to smooth the transition back to normal speed.");
      this.maxPostTicks = new Setting<Integer>("Max Post Ticks", 20, 0, 50, (v) -> (Boolean)this.useTimer.getValue() && (Float)this.timerAmount.getValue() > 1.0F && (Float)this.postTimer.getValue() < 1.0F, "Maximum number of ticks the reduced Post Timer may remain active.");
      this.timeoutTicks = new Setting<Integer>("Timeout Ticks", 10, 0, 100, (v) -> true, "Maximum time HoleSnap can stay in its movement attempt before timing out.");
      this.stepMode = (new Setting("Step Mode", HoleSnap.StepMode.Future)).withDescription((Function<StepMode, String>)((mode) -> {
         switch (mode) {
            case None:
               return "None: Does not apply an automatic step while snapping toward a hole.";
            case Client:
               return "Client: Uses the Client's built in step Module when following the snap path.";
            case Future:
               return "Future: Uses the Future-style step packet sequence while following the snap path.";
            default:
               return "";
         }
      }));
      this.disableSpeed = new Setting<Boolean>("Disable Speed", true, "Disables the Speed module while HoleSnap controls movement.");
      this.pauseInstantSpeed = new Setting<Boolean>("Pause InstantSpeed", true, "Temporarily pauses InstantSpeed while HoleSnap controls movement.");
      this.render = new Setting<Boolean>("Render", true, "Renders the current HoleSnap target and path information.");
      this.pathFinder = new HolePathFinder();
      this.pathStuckTicks = 0;
      this.collisionSlowTicks = 0;
      this.register(this.useTimer);
      this.register(this.strict);
      this.register(this.safety);
      this.register(this.terrain);
      this.register(this.twoByTwo);
      this.register(this.obstacleLogic);
      this.register(this.strictCollision);
      this.register(this.pathStepHeight);
      this.register(this.stepMode);
      this.register(this.disableSpeed);
      this.register(this.pauseInstantSpeed);
      this.register(this.horizontalRange);
      this.register(this.verticalRange);
      this.register(this.timerAmount);
      this.register(this.postTimer);
      this.register(this.maxPostTicks);
      this.register(this.timeoutTicks);
      this.register(this.render);
      this.state = HoleSnap.SnapState.MOVING;
      this.stuckTime = 0;
      this.ranTicks = 0;
      this.enabledTicks = 0;
      this.postTimerCountdown = 0;
      this.pathStuckTicks = 0;
      this.collisionSlowTicks = 0;
      this.timer = (ITimer)((IMinecraft)mc).getTimer();
      this.isIncompleteHole = false;
      this.lastPlayerY = (double)0.0F;
      this.instantSpeedPauseOwned = false;
      this.instantSpeedWasPaused = false;
   }

   public void onRender3D(Render3DEvent event) {
      if ((Boolean)this.render.getValue() && this.hole != null && this.state == HoleSnap.SnapState.MOVING) {
         Vec3d playerPos = new Vec3d(mc.player.posX, mc.player.posY, mc.player.posZ);
         if (this.obstacleLogic.getValue() == HoleSnap.ObstacleLogic.AltPath && this.pathFinder.isValid()) {
            Vec3d prev = playerPos;

            for(Vec3d wp : this.pathFinder.getWaypoints()) {
               RenderUtil.drawLine(prev, wp, new Color(0, 200, 255, 180), 2.0F);
               prev = wp;
            }
         } else {
            Vec3d holeEdge = this.getHoleEdgePoint(this.hole, playerPos);
            RenderUtil.drawLine(playerPos, holeEdge, new Color(0, 255, 0, 255), 3.0F);
         }

      }
   }

   private Vec3d getHoleEdgePoint(Hole hole, Vec3d playerPos) {
      Vec3d targetPos = this.getHoleTargetPos(hole);
      Vec3d direction = targetPos.subtract(playerPos).normalize();
      double radius = hole.is2x2() ? (double)1.0F : (double)0.5F;
      return new Vec3d(targetPos.x - direction.x * radius, targetPos.y, targetPos.z - direction.z * radius);
   }

   private Vec3d getHoleTargetPos(Hole h) {
      if (h.is2x2()) {
         return this.getCenterOf2x2Hole(h);
      } else if (h.getSecond() != null) {
         return this.isIncompleteHole ? this.getIncompleteHoleTarget(h) : this.getCenterOfDoubleHole(h);
      } else {
         BlockPos pos = h.getFirst();
         return new Vec3d((double)pos.getX() + (double)0.5F, (double)pos.getY(), (double)pos.getZ() + (double)0.5F);
      }
   }

   public static HoleSnap getInstance() {
      if (INSTANCE == null) {
         INSTANCE = new HoleSnap();
      }

      return INSTANCE;
   }

   public void onEnable() {
      if (!fullNullCheck()) {
         if (mc.player.isSpectator()) {
            this.disable();
         } else {
            this.state = HoleSnap.SnapState.MOVING;
            this.stuckTime = 0;
            this.ranTicks = 0;
            this.enabledTicks = 0;
            this.postTimerCountdown = 0;
            this.pathStuckTicks = 0;
            this.collisionSlowTicks = 0;
            this.isIncompleteHole = false;
            this.lastPlayerY = mc.player.posY;
            this.oldTimer = this.timer.getTickLength();
            this.hole = this.findBestHole();
            if (this.hole == null) {
               Command.sendSilentMessage(ChatFormatting.GRAY + "No holes in range.");
               this.disable();
            } else {
               Vec3d enableCenter = this.getHoleTargetPos(this.hole);
               if (this.isCentered(enableCenter) && Math.abs(mc.player.posY - enableCenter.y) < (double)0.5F) {
                  this.disable();
               } else {
                  this.pauseInstantSpeed();
                  if (this.stepMode.getValue() == HoleSnap.StepMode.Future) {
                     mc.player.sendChatMessage(".toggle step on");
                     if ((Boolean)this.disableSpeed.getValue()) {
                        mc.player.sendChatMessage(".toggle speed off");
                     }
                  } else if (this.stepMode.getValue() == HoleSnap.StepMode.Client) {
                     Vonware.moduleManager.getModuleByName("Step").enable();
                     if ((Boolean)this.disableSpeed.getValue()) {
                        Vonware.moduleManager.getModuleByName("Speed").disable();
                     }
                  }

               }
            }
         }
      }
   }

   private void pauseInstantSpeed() {
      if ((Boolean)this.pauseInstantSpeed.getValue() && !this.instantSpeedPauseOwned) {
         this.instantSpeedWasPaused = InstantSpeed.paused;
         InstantSpeed.paused = true;
         this.instantSpeedPauseOwned = true;
      }
   }

   private void restoreInstantSpeed() {
      if (this.instantSpeedPauseOwned) {
         InstantSpeed.paused = this.instantSpeedWasPaused;
         this.instantSpeedPauseOwned = false;
      }
   }

   public void onDisable() {
      this.restoreInstantSpeed();
      if (!fullNullCheck()) {
         mc.player.stepHeight = 0.6F;
         this.timer.setTickLength(this.oldTimer);
         Vonware.timerManager.resetTimer(this);
         if (this.state == HoleSnap.SnapState.MOVING) {
            if (this.stepMode.getValue() == HoleSnap.StepMode.Future) {
               mc.player.sendChatMessage(".toggle step off");
            } else if (this.stepMode.getValue() == HoleSnap.StepMode.Client) {
               Vonware.moduleManager.getModuleByName("Step").disable();
            }
         }

         this.state = HoleSnap.SnapState.MOVING;
         this.hole = null;
         this.ranTicks = 0;
         this.enabledTicks = 0;
         this.postTimerCountdown = 0;
         this.pathStuckTicks = 0;
         this.collisionSlowTicks = 0;
         this.pathFinder.invalidate();
      }
   }

   public void onUpdate() {
      if (!fullNullCheck()) {
         if (!mc.player.isInLava() && !mc.player.isInWater()) {
            if (this.state == HoleSnap.SnapState.POST_TIMER) {
               if (--this.postTimerCountdown <= 0) {
                  this.disable();
               }

            } else {
               ++this.enabledTicks;
               if (this.enabledTicks > (Integer)this.timeoutTicks.getValue()) {
                  Command.sendSilentMessage(ChatFormatting.GRAY + "Timed out.");
                  this.finishSnap();
               } else {
                  boolean tickBoostActive = TickBoost.INSTANCE != null && TickBoost.INSTANCE.isBoosting();
                  boolean boosting = (Boolean)this.useTimer.getValue() && !tickBoostActive;
                  float baseTickLength = boosting ? 50.0F / (Float)this.timerAmount.getValue() : this.oldTimer;
                  if (boosting && (Boolean)this.strict.getValue()) {
                     double yDelta = Math.abs(mc.player.posY - this.lastPlayerY);
                     baseTickLength = yDelta > 0.01 ? 50.0F / ((Float)this.timerAmount.getValue() * 0.4F) : 50.0F / (Float)this.timerAmount.getValue();
                  }

                  this.lastPlayerY = mc.player.posY;
                  float finalTickLength = baseTickLength;
                  if (this.obstacleLogic.getValue() == HoleSnap.ObstacleLogic.AltPath && (Boolean)this.strictCollision.getValue() && this.collisionSlowTicks > 0) {
                     int total = 22;
                     if (this.collisionSlowTicks <= 2) {
                        finalTickLength = baseTickLength * 2.0F;
                     } else {
                        float t = (float)(this.collisionSlowTicks - 2) / 20.0F;
                        finalTickLength = baseTickLength * 2.0F + (baseTickLength - baseTickLength * 2.0F) * t;
                     }

                     if (++this.collisionSlowTicks > total) {
                        this.collisionSlowTicks = 0;
                     }
                  }

                  Vonware.timerManager.modifyTimer(this, finalTickLength, 5);
               }
            }
         } else {
            this.disable();
         }
      }
   }

   @SubscribeEvent
   public void onRubberBand(PacketEvent.Receive event) {
      if (event.getPacket() instanceof SPacketPlayerPosLook) {
         this.disable();
      }
   }

   @SubscribeEvent
   public void onMove(MoveEvent event) {
      if (!fullNullCheck()) {
         if (this.state != HoleSnap.SnapState.POST_TIMER) {
            BlockPos playerBlockPos = HoleUtil.getPlayerPos();
            if (!HoleUtil.isHole(playerBlockPos) && (!(Boolean)this.terrain.getValue() || !HoleUtil.isTerrainHole(playerBlockPos))) {
               Hole freshHole = this.findBestHole();
               if (freshHole == null) {
                  Command.sendSilentMessage(ChatFormatting.GRAY + "No holes in range.");
                  this.finishSnap();
               } else {
                  boolean holeChanged = this.hole == null || !freshHole.getFirst().equals(this.hole.getFirst());
                  this.hole = freshHole;
                  if (holeChanged && this.obstacleLogic.getValue() == HoleSnap.ObstacleLogic.AltPath) {
                     if (!this.computePath()) {
                        Command.sendSilentMessage(ChatFormatting.GRAY + "No path to hole.");
                        this.finishSnap();
                        return;
                     }

                     this.pathStuckTicks = 0;
                  }

                  this.enabledTicks = 0;
                  Vec3d targetPos = this.resolveTargetPos(playerBlockPos);
                  if (targetPos != null) {
                     switch ((ObstacleLogic)this.obstacleLogic.getValue()) {
                        case None:
                           this.handleObstacleNone(event, targetPos);
                           break;
                        case AltPath:
                           this.handleObstacleAltPath(event, targetPos, playerBlockPos);
                     }

                  }
               }
            } else {
               this.finishSnap();
            }
         }
      }
   }

   private Vec3d resolveTargetPos(BlockPos playerBlockPos) {
      Vec3d targetPos;
      if (this.hole.is2x2()) {
         targetPos = this.getCenterOf2x2Hole(this.hole);
         if (this.isCentered(targetPos) && Math.abs(mc.player.posY - targetPos.y) < (double)0.5F) {
            this.finishSnap();
            return null;
         }
      } else if (this.hole.getSecond() != null) {
         if (this.isIncompleteHole) {
            boolean fo = BlockUtil.isAir(this.hole.getFirst().up(1));
            boolean so = BlockUtil.isAir(this.hole.getSecond().up(1));
            if (fo && so) {
               boolean insideHole = playerBlockPos.equals(this.hole.getFirst()) || playerBlockPos.equals(this.hole.getSecond());
               if (insideHole) {
                  targetPos = this.getCenterOfDoubleHole(this.hole);
                  if (this.isCentered(targetPos) && Math.abs(mc.player.posY - targetPos.y) < (double)0.5F) {
                     this.finishSnap();
                     return null;
                  }
               } else {
                  boolean firstFullyOpen = this.isPositionOpen(this.hole.getFirst());
                  boolean secondFullyOpen = this.isPositionOpen(this.hole.getSecond());
                  BlockPos entry;
                  if (firstFullyOpen && !secondFullyOpen) {
                     entry = this.hole.getFirst();
                  } else if (secondFullyOpen && !firstFullyOpen) {
                     entry = this.hole.getSecond();
                  } else {
                     double d1 = mc.player.getDistanceSqToCenter(this.hole.getFirst());
                     double d2 = mc.player.getDistanceSqToCenter(this.hole.getSecond());
                     entry = d1 <= d2 ? this.hole.getFirst() : this.hole.getSecond();
                  }

                  targetPos = new Vec3d((double)entry.getX() + (double)0.5F, (double)entry.getY(), (double)entry.getZ() + (double)0.5F);
               }
            } else {
               targetPos = this.getIncompleteHoleTarget(this.hole);
               if ((playerBlockPos.equals(this.hole.getFirst()) || playerBlockPos.equals(this.hole.getSecond())) && Math.hypot(mc.player.posX - targetPos.x, mc.player.posZ - targetPos.z) < 0.1 && Math.abs(mc.player.posY - targetPos.y) < (double)0.5F) {
                  this.finishSnap();
                  return null;
               }
            }
         } else {
            targetPos = this.getCenterOfDoubleHole(this.hole);
            if (this.isCentered(targetPos) && Math.abs(mc.player.posY - targetPos.y) < (double)0.5F) {
               this.finishSnap();
               return null;
            }
         }
      } else {
         targetPos = new Vec3d((double)this.hole.getFirst().getX() + (double)0.5F, (double)this.hole.getFirst().getY(), (double)this.hole.getFirst().getZ() + (double)0.5F);
      }

      return targetPos;
   }

   private void handleObstacleNone(MoveEvent event, Vec3d targetPos) {
      if (mc.player.collidedHorizontally && mc.player.onGround) {
         ++this.stuckTime;
         if (this.stuckTime >= 10) {
            this.finishSnap();
            return;
         }
      } else {
         this.stuckTime = 0;
         mc.player.stepHeight = 0.6F;
      }

      this.moveDirectly(event, targetPos);
   }

   private void handleObstacleAltPath(MoveEvent event, Vec3d targetPos, BlockPos playerBlockPos) {
      if (this.isPlayerInsideHole(playerBlockPos)) {
         this.pathFinder.invalidate();
         this.moveDirectly(event, targetPos);
      } else {
         if (mc.player.collidedHorizontally && mc.player.onGround) {
            ++this.stuckTime;
            ++this.pathStuckTicks;
            if ((Boolean)this.strictCollision.getValue() && this.collisionSlowTicks == 0) {
               this.collisionSlowTicks = 1;
            }

            if (this.pathStuckTicks >= 8) {
               if (!this.computePath()) {
                  Command.sendSilentMessage(ChatFormatting.GRAY + "No path to hole.");
                  this.finishSnap();
                  return;
               }

               this.pathStuckTicks = 0;
            }

            if (this.stuckTime >= 30) {
               this.finishSnap();
               return;
            }
         } else {
            this.stuckTime = 0;
            this.pathStuckTicks = 0;
            mc.player.stepHeight = 0.6F;
         }

         if (this.pathFinder.isValid()) {
            if (!this.pathFinder.isPathStillValid()) {
               this.pathFinder.invalidate();
               this.moveDirectly(event, targetPos);
            } else {
               Vec3d playerVec = mc.player.getPositionVector();
               if (this.pathFinder.tick(playerVec)) {
                  this.pathFinder.invalidate();
                  this.moveDirectly(event, targetPos);
               } else {
                  Vec3d wp = this.pathFinder.getCurrentWaypoint();
                  if (wp == null) {
                     this.pathFinder.invalidate();
                     this.moveDirectly(event, targetPos);
                  } else {
                     this.moveToward(event, wp);
                  }
               }
            }
         } else {
            this.moveDirectly(event, targetPos);
         }
      }
   }

   private boolean computePath() {
      this.pathFinder.invalidate();
      this.pathFinder.setMaxStepUp((Integer)this.pathStepHeight.getValue());
      return this.pathFinder.computePath(HoleUtil.getPlayerPos(), this.getPathGoal());
   }

   private BlockPos getPathGoal() {
      if (this.isIncompleteHole && this.hole.getSecond() != null) {
         boolean fo = this.isPositionOpen(this.hole.getFirst());
         boolean so = this.isPositionOpen(this.hole.getSecond());
         if (!fo && so) {
            return this.hole.getSecond();
         }

         if (fo && !so) {
            return this.hole.getFirst();
         }
      }

      return this.hole.getFirst();
   }

   private boolean isPlayerInsideHole(BlockPos pos) {
      if (this.hole == null) {
         return false;
      } else if (pos.equals(this.hole.getFirst())) {
         return true;
      } else if (this.hole.getSecond() != null && pos.equals(this.hole.getSecond())) {
         return true;
      } else if (this.hole.getThird() != null && pos.equals(this.hole.getThird())) {
         return true;
      } else {
         return this.hole.getFourth() != null && pos.equals(this.hole.getFourth());
      }
   }

   private void moveToward(MoveEvent event, Vec3d wp) {
      double dx = wp.x - mc.player.posX;
      double dz = wp.z - mc.player.posZ;
      double dist = Math.hypot(dx, dz);
      if (dist < 0.01) {
         event.setX((double)0.0F);
         event.setZ((double)0.0F);
         mc.player.motionX = (double)0.0F;
         mc.player.motionZ = (double)0.0F;
         ++this.ranTicks;
      } else {
         double yawRad = Math.atan2(-dx, dz);
         double speed = this.resolveSpeed(dist);
         mc.player.motionX = (double)0.0F;
         mc.player.motionZ = (double)0.0F;
         event.setX(-Math.sin(yawRad) * speed);
         event.setZ(Math.cos(yawRad) * speed);
         ++this.ranTicks;
      }
   }

   @SubscribeEvent
   public void onInputUpdate(InputUpdateEvent event) {
      if (this.state == HoleSnap.SnapState.MOVING) {
         if (this.hole != null) {
            if (!fullNullCheck()) {
               MovementInputFromOptions input = event.getMovementInput();
               input.moveForward = 0.0F;
               input.moveStrafe = 0.0F;
               input.forwardKeyDown = false;
               input.backKeyDown = false;
               input.leftKeyDown = false;
               input.rightKeyDown = false;
            }
         }
      }
   }

   private void moveDirectly(MoveEvent event, Vec3d targetPos) {
      double dx = targetPos.x - mc.player.posX;
      double dz = targetPos.z - mc.player.posZ;
      double dist = Math.hypot(dx, dz);
      if (dist < 0.01) {
         if (mc.player.posY - targetPos.y > 0.1) {
            event.setX((double)0.0F);
            event.setZ((double)0.0F);
            mc.player.motionX = (double)0.0F;
            mc.player.motionZ = (double)0.0F;
            ++this.ranTicks;
         } else {
            this.finishSnap();
         }
      } else {
         double yawRad = Math.atan2(-dx, dz);
         double speed = this.resolveSpeed(dist);
         mc.player.motionX = (double)0.0F;
         mc.player.motionZ = (double)0.0F;
         event.setX(-Math.sin(yawRad) * speed);
         event.setZ(Math.cos(yawRad) * speed);
         ++this.ranTicks;
         if (!mc.player.collidedHorizontally) {
            mc.player.stepHeight = 0.6F;
         }

      }
   }

   private void finishSnap() {
      if (this.stepMode.getValue() == HoleSnap.StepMode.Future) {
         mc.player.sendChatMessage(".toggle step off");
      } else if (this.stepMode.getValue() == HoleSnap.StepMode.Client) {
         Vonware.moduleManager.getModuleByName("Step").disable();
      }

      mc.player.stepHeight = 0.6F;
      if ((Boolean)this.useTimer.getValue() && (Float)this.timerAmount.getValue() > 1.0F && (Float)this.postTimer.getValue() < 1.0F && this.ranTicks > 0) {
         float ta = (Float)this.timerAmount.getValue();
         float pt = (Float)this.postTimer.getValue();
         float x = (pt * (float)this.ranTicks - ta * pt * (float)this.ranTicks) / (ta * (pt - 1.0F));
         int postTicks = Math.min((Integer)this.maxPostTicks.getValue(), (int)Math.ceil((double)x));
         if (postTicks > 0) {
            this.state = HoleSnap.SnapState.POST_TIMER;
            this.postTimerCountdown = postTicks;
            Vonware.timerManager.modifyTimer(this, 50.0F / (Float)this.postTimer.getValue(), postTicks + 1);
            this.hole = null;
            return;
         }
      }

      this.disable();
   }

   private Hole findBestHole() {
      List<Hole> allHoles = HoleUtil.getHoles(mc.player, ((Integer)this.horizontalRange.getValue()).floatValue(), true, false, false, (Boolean)this.terrain.getValue(), true, (Boolean)this.twoByTwo.getValue());
      if (allHoles.isEmpty()) {
         return null;
      } else {
         List<Hole> sorted = (List)allHoles.stream().filter((hx) -> {
            if (Math.abs(mc.player.posY - (double)hx.getFirst().getY()) > (double)(Integer)this.verticalRange.getValue()) {
               return false;
            } else {
               return hx.getSecond() != null && !hx.is2x2() && this.isIncompleteDoubleHole(hx) ? this.canAccessIncompleteHole(hx) : true;
            }
         }).sorted(Comparator.comparingDouble((hx) -> mc.player.getDistanceSqToCenter(hx.getFirst()))).collect(Collectors.toList());
         if (sorted.isEmpty()) {
            return null;
         } else if (!(Boolean)this.safety.getValue()) {
            this.isIncompleteHole = this.isIncompleteDoubleHole((Hole)sorted.get(0));
            return (Hole)sorted.get(0);
         } else {
            for(Hole h : sorted) {
               boolean incomplete = !h.is2x2() && this.isIncompleteDoubleHole(h);
               if (incomplete || h.is2x2() || this.isSafeHole(h)) {
                  this.isIncompleteHole = incomplete;
                  return h;
               }
            }

            Command.sendSilentMessage(ChatFormatting.GRAY + "No safe holes in range.");
            return null;
         }
      }
   }

   private double applySpeedPotionEffects(double speed) {
      PotionEffect speedEffect = mc.player.getActivePotionEffect(MobEffects.SPEED);
      if (speedEffect != null) {
         speed += 0.2873 * (double)(speedEffect.getAmplifier() + 1) * 0.2;
      }

      PotionEffect slownessEffect = mc.player.getActivePotionEffect(MobEffects.SLOWNESS);
      if (slownessEffect != null) {
         speed -= 0.2873 * (double)(slownessEffect.getAmplifier() + 1) * 0.15;
      }

      return Math.max((double)0.0F, speed);
   }

   private double resolveSpeed(double dist) {
      double base = this.applySpeedPotionEffects(0.2873);
      double current = Math.hypot(mc.player.motionX, mc.player.motionZ);
      double speed = mc.player.onGround ? base : Math.max(current + 0.02, base);
      return Math.min(speed, dist);
   }

   private boolean isCentered(Vec3d pos) {
      return Math.abs(mc.player.posX - pos.x) < 0.2 && Math.abs(mc.player.posZ - pos.z) < 0.2;
   }

   private boolean isIncompleteDoubleHole(Hole hole) {
      if (hole.getSecond() == null) {
         return false;
      } else {
         return !this.isPositionOpen(hole.getFirst()) || !this.isPositionOpen(hole.getSecond());
      }
   }

   private boolean canAccessIncompleteHole(Hole hole) {
      if (hole.getSecond() == null) {
         return false;
      } else {
         boolean fo = this.isPositionOpen(hole.getFirst());
         boolean so = this.isPositionOpen(hole.getSecond());
         int open = (fo ? 1 : 0) + (so ? 1 : 0);
         if (open == 1) {
            return true;
         } else if (open != 2) {
            return true;
         } else {
            int playerY = (int)Math.floor(mc.player.posY);
            int holeY = hole.getFirst().getY();
            if (playerY > holeY) {
               Vec3d center = this.getCenterOfDoubleHole(hole);
               BlockPos cPos = new BlockPos(center.x, (double)holeY, center.z);

               for(int y = holeY + 1; y <= playerY + 2; ++y) {
                  if (!BlockUtil.isAir(new BlockPos(cPos.getX(), y, cPos.getZ()))) {
                     return false;
                  }
               }
            }

            return true;
         }
      }
   }

   private boolean isPositionOpen(BlockPos pos) {
      return BlockUtil.isAir(pos.up(1)) && BlockUtil.isAir(pos.up(2));
   }

   private Vec3d getIncompleteHoleTarget(Hole hole) {
      if (hole.getSecond() == null) {
         return new Vec3d((double)hole.getFirst().getX() + (double)0.5F, (double)hole.getFirst().getY(), (double)hole.getFirst().getZ() + (double)0.5F);
      } else {
         boolean fo = this.isPositionOpen(hole.getFirst());
         boolean so = this.isPositionOpen(hole.getSecond());
         if (fo && !so) {
            return new Vec3d((double)hole.getFirst().getX() + (double)0.5F, (double)hole.getFirst().getY(), (double)hole.getFirst().getZ() + (double)0.5F);
         } else {
            return !fo && so ? new Vec3d((double)hole.getSecond().getX() + (double)0.5F, (double)hole.getSecond().getY(), (double)hole.getSecond().getZ() + (double)0.5F) : this.getCenterOfDoubleHole(hole);
         }
      }
   }

   private boolean isSafeHole(Hole hole) {
      if (mc.world.getBlockState(hole.getFirst().up()).getBlock() != Blocks.AIR) {
         return false;
      } else {
         int playerY = (int)Math.floor(mc.player.posY);
         int holeY = hole.getFirst().getY();
         if (playerY > holeY) {
            for(int y = holeY + 1; y <= playerY + 1; ++y) {
               if (!mc.world.getBlockState(new BlockPos(hole.getFirst().getX(), y, hole.getFirst().getZ())).getMaterial().isReplaceable()) {
                  return false;
               }

               if (hole.getSecond() != null && !mc.world.getBlockState(new BlockPos(hole.getSecond().getX(), y, hole.getSecond().getZ())).getMaterial().isReplaceable()) {
                  return false;
               }
            }
         }

         return true;
      }
   }

   private Vec3d getCenterOf2x2Hole(Hole hole) {
      return new Vec3d((double)hole.getFirst().getX() + (double)1.0F, (double)hole.getFirst().getY(), (double)hole.getFirst().getZ() + (double)1.0F);
   }

   private Vec3d getCenterOfDoubleHole(Hole hole) {
      double cx = (double)(hole.getFirst().getX() + hole.getSecond().getX()) / (double)2.0F;
      double cz = (double)(hole.getFirst().getZ() + hole.getSecond().getZ()) / (double)2.0F;
      return new Vec3d(cx + (double)0.5F, (double)hole.getFirst().getY(), cz + (double)0.5F);
   }

   private static enum StepMode {
      None,
      Client,
      Future;
   }

   private static enum SnapState {
      MOVING,
      POST_TIMER;
   }

   private static enum ObstacleLogic {
      None,
      AltPath;
   }
}
