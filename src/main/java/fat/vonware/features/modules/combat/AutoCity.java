package fat.vonware.features.modules.combat;

import fat.vonware.Vonware;
import fat.vonware.event.events.MotionUpdateEvent;
import fat.vonware.features.command.Command;
import fat.vonware.features.modules.Module;
import fat.vonware.features.modules.player.FastBreak;
import fat.vonware.features.setting.Setting;
import fat.vonware.manager.RotationManager;
import fat.vonware.util.BlockUtil;
import fat.vonware.util.EntityUtil;
import fat.vonware.util.MathUtil;
import fat.vonware.util.PlayerUtil;
import fat.vonware.util.PositionUtil;
import fat.vonware.util.RotationUtil;
import fat.vonware.util.Timer;
import fat.vonware.util.AutoCity.MineUtil;
import fat.vonware.util.AutoCity.Offsets;
import fat.vonware.util.holeesp.HoleUtil;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.block.BlockObsidian;
import net.minecraft.block.BlockShulkerBox;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class AutoCity extends Module {
   public Setting<Boolean> newVer = new Setting<Boolean>("1.13+", false, "Uses block and mining behavior intended for Minecraft 1.13 and newer servers.");
   public Setting<Boolean> strictDirection = new Setting<Boolean>("StrictDirection", false, "Requires a valid strict-facing side when selecting a block to mine.");
   public Setting<Boolean> breakEnderChests = new Setting<Boolean>("BreakEChests", true, "Allows ender chests to be selected as city targets.");
   public Setting<Boolean> breakShulkerBox = new Setting<Boolean>("BreakShulkerBox", true, "Allows shulker boxes to be selected as city targets.");
   public Setting<Boolean> breakBottom = new Setting<Boolean>("BreakBottom", false, "Allows blocks below the target to be selected for mining.");
   public Setting<Boolean> breakFeet = new Setting<Boolean>("BreakSurround", true, "Allows blocks directly surrounding the target feet to be mined.");
   public Setting<Boolean> breakSemi = new Setting<Boolean>("BreakSemi", true, "Allows semi-surround positions to be selected for mining.");
   public Setting<Boolean> breakFilled = new Setting<Boolean>("BreakBurrow", true, "Allows the block occupying a burrowed target position to be mined.");
   public Setting<Boolean> breakSelfTrapped = new Setting<Boolean>("BreakSelfTrapped", false, "Allows blocks from your own self-trap to be selected.");
   public Setting<Boolean> breakEnemyTrap = new Setting<Boolean>("BreakEnemyTrapped", false, "Allows blocks from the enemy trap above them to be selected.");
   public Setting<Boolean> breakAntiStep = new Setting<Boolean>("BreakAntiStep", false, "Allows anti-step blocks above a target to be selected.");
   public Setting<Boolean> breakSelfBlocker = new Setting<Boolean>("BreakSelfBlocker", false, "Allows blocks that obstruct your own city path to be mined.");
   public Setting<Boolean> breakSelf = new Setting<Boolean>("BreakSelf", true, "Allows AutoCity to mine positions that can also affect your own surround.");
   public Setting<Boolean> breakCornerClip = new Setting<Boolean>("BreakCornerClip", true, "Allows corner-clip city positions to be selected.");
   public Setting<Boolean> prioritizeFeet = new Setting<Boolean>("PrioritizeFeet", true, (v) -> (Boolean)this.breakCornerClip.getValue(), "Prioritizes feet-level positions when using corner-clip breaking.");
   private final Setting<Float> breakRange = new Setting<Float>("BreakRange", 4.5F, 0.1F, 6.0F, "Maximum distance from you to the block AutoCity can mine.");
   private final Setting<Float> wallRange = new Setting<Float>("WallRange", 3.0F, 0.1F, 6.0F, "Maximum mining distance for blocks that are not directly visible.");
   private final Setting<Float> enemyRange = new Setting<Float>("EnemyRange", 7.5F, 0.1F, 12.0F, "Maximum distance at which a player can be selected as the AutoCity target.");
   public Setting<Boolean> onGroundOnly = new Setting<Boolean>("OnGroundOnly", true, "Only runs AutoCity while you are on the ground.");
   public Setting<Boolean> holeCheck = new Setting<Boolean>("HoleCheck", false, "Restricts target selection using hole checks.");
   public Setting<Boolean> doubleHoles = new Setting<Boolean>("2x1", false, (v) -> (Boolean)this.holeCheck.getValue(), "Includes 2x1 holes in the hole check.");
   public Setting<Boolean> twoByTwoHoles = new Setting<Boolean>("2x2", false, (v) -> (Boolean)this.holeCheck.getValue(), "Includes 2x2 holes in the hole check.");
   public Setting<Boolean> useCooldown = new Setting<Boolean>("ReMineCooldown", false, "Adds a cooldown before attempting to mine the same position again.");
   public Setting<Integer> reMineCooldown = new Setting<Integer>("Cooldown", 4, 0, 10, (v) -> (Boolean)this.useCooldown.getValue(), "Number of cooldown units to wait before re-mining a recently selected position.");
   public Setting<Boolean> smartDeselect = new Setting<Boolean>("Deselect", false, "Deselects the mining target when it is no longer useful or valid.");
   protected final Timer timer;
   protected EntityPlayer target;
   protected BlockPos attackPos;
   private boolean isBreaking = false;
   private BlockPos lastFailedPos = null;
   private final Timer cooldownTimer = new Timer();
   private boolean onCooldown = false;
   private EntityPlayer miningTarget = null;
   private BlockPos miningTargetHole = null;

   public AutoCity() {
      super("AutoCity", "Automatically mines surround blocks (works only with FastBreak)", Module.Category.COMBAT, true, false, false);
      this.register(this.onGroundOnly);
      this.register(this.holeCheck);
      this.register(this.doubleHoles);
      this.register(this.twoByTwoHoles);
      this.register(this.breakRange);
      this.register(this.enemyRange);
      this.register(this.wallRange);
      this.register(this.strictDirection);
      this.register(this.breakEnderChests);
      this.register(this.breakShulkerBox);
      this.register(this.breakBottom);
      this.register(this.breakFeet);
      this.register(this.breakSemi);
      this.register(this.breakFilled);
      this.register(this.breakSelfTrapped);
      this.register(this.breakEnemyTrap);
      this.register(this.breakAntiStep);
      this.register(this.breakSelfBlocker);
      this.register(this.breakSelf);
      this.register(this.breakCornerClip);
      this.register(this.prioritizeFeet);
      this.register(this.newVer);
      this.register(this.useCooldown);
      this.register(this.reMineCooldown);
      this.register(this.smartDeselect);
      this.timer = new Timer();
      this.target = null;
      this.attackPos = null;
   }

   public String getDisplayInfo() {
      return this.target != null ? this.target.getName() : null;
   }

   @SubscribeEvent(
      priority = EventPriority.NORMAL
   )
   public void onMotionUpdate(MotionUpdateEvent event) {
      if (event.getStage() == 0) {
         this.target = null;
         if (!PlayerUtil.isCreative() && !PlayerUtil.isSpectator()) {
            if (!(Boolean)this.onGroundOnly.getValue() || Vonware.positionManager.isOnGround()) {
               FastBreak FAST_BREAK = (FastBreak)Vonware.moduleManager.getModuleByClass(FastBreak.class);
               if (!FAST_BREAK.isEnabled()) {
                  Command.sendSilentMessage(TextFormatting.RED + "Enable FastBreak to use this module");
                  this.disable();
               } else {
                  if ((Boolean)this.smartDeselect.getValue() && this.isBreaking && this.miningTarget != null && this.miningTargetHole != null && this.attackPos != null) {
                     BlockPos currentTargetPos = PositionUtil.getPosition(this.miningTarget);
                     if (!currentTargetPos.equals(this.miningTargetHole)) {
                        FAST_BREAK.abortCurrentPos(true);
                        this.attackPos = null;
                        this.isBreaking = false;
                        this.miningTarget = null;
                        this.miningTargetHole = null;
                     }
                  }

                  if (FAST_BREAK.getPos() != null && !(FAST_BREAK.getBlock() instanceof BlockAir)) {
                     if (this.attackPos != null && this.attackPos.equals(FAST_BREAK.getPos())) {
                        this.isBreaking = true;
                     }
                  } else {
                     if (this.isBreaking && (Boolean)this.useCooldown.getValue()) {
                        this.lastFailedPos = this.attackPos;
                        this.onCooldown = true;
                        this.cooldownTimer.reset();
                     }

                     this.isBreaking = false;
                     if (this.timer.passed(50L)) {
                        this.target = null;
                        this.attackPos = null;
                        if ((Boolean)this.breakSelfTrapped.getValue() && EntityUtil.isTrapped(mc.player)) {
                           if (mc.world.getBlockState(PositionUtil.getPosition().up(3)).getBlock() != Blocks.AIR && (Boolean)this.breakAntiStep.getValue()) {
                              this.attackPos = PositionUtil.getPosition().up(3);
                           } else if (mc.world.getBlockState(PositionUtil.getPosition().up(3)).getBlock() == Blocks.AIR) {
                              this.attackPos = PositionUtil.getPosition().up(2);
                           }

                           this.attack(event);
                        } else {
                           this.target = EntityUtil.getTarget((double)(Float)this.enemyRange.getValue());
                           if (this.target == null) {
                              this.breakEnderChests(event);
                              this.breakShulkerBoxes(event);
                           } else {
                              BlockPos targetPos = PositionUtil.getPosition(this.target);
                              if ((Boolean)this.breakCornerClip.getValue() && EntityUtil.isCornerClipping(this.target)) {
                                 BlockPos cornerBlockPos = this.getCornerClipBlock(this.target, targetPos);
                                 if (cornerBlockPos != null) {
                                    this.attackPos = cornerBlockPos;
                                    this.attack(event);
                                    return;
                                 }
                              }

                              if ((Boolean)this.breakFilled.getValue() && EntityUtil.isInBurrow(this.target)) {
                                 this.attackPos = targetPos;
                                 this.attack(event);
                              } else if ((Boolean)this.holeCheck.getValue() && !this.isHole(this.target)) {
                                 this.breakEnderChests(event);
                                 this.breakShulkerBoxes(event);
                              } else {
                                 if (this.isHole(this.target) && (Boolean)this.breakSelfBlocker.getValue()) {
                                    for(BlockPos pos : Offsets.FACE_PLACE) {
                                       if (mc.world.getBlockState(targetPos.add(pos)).getBlock() == Blocks.OBSIDIAN && mc.world.getBlockState(targetPos.add(pos.up())).getBlock() == Blocks.AIR) {
                                          this.attackPos = targetPos.add(pos);
                                          this.attack(event);
                                          return;
                                       }
                                    }
                                 }

                                 if ((Boolean)this.breakEnemyTrap.getValue() && EntityUtil.isTrapped(this.target) && this.isHole(this.target)) {
                                    if (mc.world.getBlockState(targetPos.up(3)).getBlock() != Blocks.AIR && (Boolean)this.breakAntiStep.getValue()) {
                                       this.attackPos = targetPos.up(3);
                                    } else if (mc.world.getBlockState(targetPos.up(3)).getBlock() == Blocks.AIR) {
                                       this.attackPos = targetPos.up(2);
                                    }

                                    this.attack(event);
                                 } else if ((Boolean)this.breakBottom.getValue() && this.isHole(this.target)) {
                                    this.attackPos = targetPos.down();
                                    this.attack(event);
                                 } else {
                                    this.checkSide(event);
                                    if (!(Boolean)this.newVer.getValue() && this.getCity(this.target) == null) {
                                       this.checkSide(event);
                                       this.breakEnderChests(event);
                                       this.breakShulkerBoxes(event);
                                    } else {
                                       if (!(Boolean)this.breakSelf.getValue()) {
                                          for(EntityPlayer player : mc.world.getEntitiesWithinAABB(EntityPlayer.class, (new AxisAlignedBB(PositionUtil.getPosition(mc.player))).contract(0.2, (double)0.0F, 0.2))) {
                                             if (player == this.target) {
                                                return;
                                             }
                                          }
                                       }

                                       if ((Boolean)this.breakFeet.getValue() && !this.isInBurrow(this.target) && !(Boolean)this.newVer.getValue()) {
                                          this.attackPos = this.getCity(this.target);
                                          this.attack(event);
                                       } else if ((Boolean)this.breakFeet.getValue() && !this.isInBurrow(this.target)) {
                                          for(EnumFacing facing : EnumFacing.HORIZONTALS) {
                                             BlockPos yea = PositionUtil.getPosition(this.target).offset(facing);
                                             boolean canPlace = mc.world.getBlockState(yea.down()).getBlock() == Blocks.BEDROCK || mc.world.getBlockState(yea.down()).getBlock() == Blocks.OBSIDIAN;
                                             if (mc.world.getBlockState(yea).getBlock() instanceof BlockObsidian && canPlace) {
                                                this.attackPos = yea;
                                                this.attack(event);
                                                return;
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
               }
            }
         }
      }
   }

   private boolean isValidClipBlock(BlockPos pos) {
      Block block = mc.world.getBlockState(pos).getBlock();
      return !(block instanceof BlockAir) && block != Blocks.BEDROCK;
   }

   private BlockPos getCornerClipBlock(EntityPlayer player, BlockPos playerPos) {
      List<EnumFacing> clipDirs = EntityUtil.getCornerClipDirections(player);
      if (clipDirs.isEmpty()) {
         return null;
      } else {
         BlockPos headPos = playerPos.up();
         if ((Boolean)this.prioritizeFeet.getValue()) {
            for(EnumFacing facing : clipDirs) {
               BlockPos feetAdjacent = playerPos.offset(facing);
               if (this.isValidClipBlock(feetAdjacent)) {
                  if ((Boolean)this.breakAntiStep.getValue()) {
                     BlockPos aboveFeet = feetAdjacent.up();
                     if (this.isValidClipBlock(aboveFeet)) {
                        return aboveFeet;
                     }
                  }

                  return feetAdjacent;
               }
            }

            for(EnumFacing facing : clipDirs) {
               BlockPos headAdjacent = headPos.offset(facing);
               if (this.isValidClipBlock(headAdjacent)) {
                  return headAdjacent;
               }
            }

            return null;
         } else {
            for(EnumFacing facing : clipDirs) {
               BlockPos feetAdjacent = playerPos.offset(facing);
               BlockPos headAdjacent = headPos.offset(facing);
               boolean feetSolid = this.isValidClipBlock(feetAdjacent);
               boolean headSolid = this.isValidClipBlock(headAdjacent);
               if (feetSolid && (Boolean)this.breakAntiStep.getValue()) {
                  BlockPos aboveFeet = feetAdjacent.up();
                  if (this.isValidClipBlock(aboveFeet)) {
                     return aboveFeet;
                  }

                  return feetAdjacent;
               }

               if (feetSolid) {
                  return feetAdjacent;
               }

               if (headSolid) {
                  return headAdjacent;
               }
            }

            return null;
         }
      }
   }

   private void rotateTo(MotionUpdateEvent event, BlockPos pos) {
      if (pos != null) {
         EnumFacing mineSide = BlockUtil.getMineableSide(pos, (Boolean)this.strictDirection.getValue(), (Boolean)this.strictDirection.getValue());
         if (mineSide != null) {
            float[] rotations = RotationUtil.getRotations(pos);
            RotationManager.INSTANCE.requestRotation(RotationManager.RotationSource.AUTO_CITY, rotations[0], rotations[1]);
         }
      }
   }

   private BlockPos getCity(EntityPlayer player) {
      BlockPos targetPos = PositionUtil.getPosition(player);

      for(EnumFacing facing : EnumFacing.HORIZONTALS) {
         BlockPos offsetPos = targetPos.offset(facing);
         if (mc.world.getBlockState(offsetPos).getBlock() instanceof BlockObsidian) {
            BlockPos fullOffset = offsetPos.offset(facing);
            if (mc.world.getBlockState(fullOffset).getBlock() instanceof BlockAir && this.isPlaceable(fullOffset.down()) && mc.world.getBlockState(fullOffset.up()).getBlock() instanceof BlockAir) {
               return offsetPos;
            }
         }
      }

      return null;
   }

   private void checkSide(MotionUpdateEvent event) {
      if ((Boolean)this.breakSemi.getValue() && this.target != null) {
         for(BlockPos pos : Offsets.FEET_PLACE) {
            BlockPos targetPos = PositionUtil.getPosition(this.target);
            BlockPos offsetPos = targetPos.add(pos);
            if (mc.world.getBlockState(offsetPos).getBlock() instanceof BlockObsidian && mc.world.getBlockState(offsetPos.up()).getBlock() instanceof BlockAir) {
               this.attackPos = offsetPos;
               this.attack(event);
               return;
            }
         }
      }

   }

   private void breakEnderChests(MotionUpdateEvent event) {
      if ((Boolean)this.breakEnderChests.getValue()) {
         for(BlockPos pos : BlockUtil.getSphere((Float)this.breakRange.getValue(), true)) {
            if (mc.world.getBlockState(pos).getBlock() == Blocks.ENDER_CHEST) {
               this.attackPos = pos;
               this.attack(event);
               return;
            }
         }
      }

   }

   private void breakShulkerBoxes(MotionUpdateEvent event) {
      if ((Boolean)this.breakShulkerBox.getValue()) {
         EntityPlayer nearbyEnemy = EntityUtil.getTarget((double)(Float)this.enemyRange.getValue());
         if (nearbyEnemy == null) {
            return;
         }

         for(BlockPos pos : BlockUtil.getSphere((Float)this.breakRange.getValue(), true)) {
            if (mc.world.getBlockState(pos).getBlock() instanceof BlockShulkerBox) {
               this.attackPos = pos;
               this.attack(event);
               return;
            }
         }
      }

   }

   private boolean isPlaceable(BlockPos pos) {
      return mc.world.getBlockState(pos).getBlock() instanceof BlockObsidian || mc.world.getBlockState(pos).getBlock() == Blocks.BEDROCK;
   }

   public boolean isInBurrow(EntityPlayer target) {
      return mc.world.getBlockState(PositionUtil.getPosition(target)).getBlock() != Blocks.AIR;
   }

   public void onEnable() {
      this.attackPos = null;
      this.isBreaking = false;
      this.lastFailedPos = null;
      this.onCooldown = false;
      this.miningTarget = null;
      this.miningTargetHole = null;
   }

   public void onDisable() {
      this.attackPos = null;
      this.isBreaking = false;
      this.lastFailedPos = null;
      this.onCooldown = false;
      this.miningTarget = null;
      this.miningTargetHole = null;
   }

   protected void attack(MotionUpdateEvent event) {
      if (this.attackPos != null) {
         if ((Boolean)this.useCooldown.getValue() && this.onCooldown && this.lastFailedPos != null && this.lastFailedPos.equals(this.attackPos)) {
            if (!this.cooldownTimer.passed((long)((Integer)this.reMineCooldown.getValue() * 500))) {
               return;
            }

            this.onCooldown = false;
            this.lastFailedPos = null;
         }

         Vec3d eyePos = PositionUtil.getEyesPos();
         float effectiveRange = BlockUtil.placeTrace(this.attackPos) ? (Float)this.wallRange.getValue() : (Float)this.breakRange.getValue();
         if (!(this.attackPos.distanceSq(eyePos.x, eyePos.y, eyePos.z) > MathUtil.square((double)effectiveRange))) {
            if (((FastBreak)Vonware.moduleManager.getModuleByClass(FastBreak.class)).isBlockValid(mc.world.getBlockState(this.attackPos).getBlock(), this.attackPos)) {
               if (MineUtil.canBreak(this.attackPos)) {
                  EnumFacing mineSide = BlockUtil.getMineableSide(this.attackPos, (Boolean)this.strictDirection.getValue(), (Boolean)this.strictDirection.getValue());
                  if (mineSide != null) {
                     float[] rotations = RotationUtil.getRotations(this.attackPos);
                     RotationManager.INSTANCE.requestRotation(RotationManager.RotationSource.AUTO_CITY, rotations[0], rotations[1]);
                     ((FastBreak)Vonware.moduleManager.getModuleByClass(FastBreak.class)).scheduleMine(this.attackPos);
                     this.timer.reset();
                     this.isBreaking = true;
                     if ((Boolean)this.smartDeselect.getValue() && this.target != null) {
                        this.miningTarget = this.target;
                        this.miningTargetHole = PositionUtil.getPosition(this.target);
                     }

                  }
               }
            }
         }
      }
   }

   protected boolean isHole(EntityPlayer target) {
      BlockPos targetPos = PositionUtil.getPosition(target);
      return this.isPositionInAnyHole(targetPos);
   }

   private boolean isPositionInAnyHole(BlockPos pos) {
      if (HoleUtil.isHole(pos)) {
         return true;
      } else if (!(Boolean)this.doubleHoles.getValue() || !HoleUtil.isDoubleHole(pos) && !HoleUtil.isDoubleHole(pos.add(0, 0, -1)) && !HoleUtil.isDoubleHole(pos.add(-1, 0, 0))) {
         return (Boolean)this.twoByTwoHoles.getValue() && (HoleUtil.is2x2Hole(pos) || HoleUtil.is2x2Hole(pos.add(-1, 0, 0)) || HoleUtil.is2x2Hole(pos.add(0, 0, -1)) || HoleUtil.is2x2Hole(pos.add(-1, 0, -1)));
      } else {
         return true;
      }
   }

   private boolean isObbyHole(BlockPos pos) {
      for(BlockPos off : HoleUtil.HOLE_OFFSETS) {
         if (!BlockUtil.isObby(pos.add(off))) {
            return false;
         }
      }

      return true;
   }

   private boolean isMixedHole(BlockPos pos) {
      if (this.isBedrockHole(pos)) {
         return false;
      } else {
         for(BlockPos off : HoleUtil.HOLE_OFFSETS) {
            if (!BlockUtil.isSafe(pos.add(off))) {
               return false;
            }
         }

         return true;
      }
   }

   private boolean isBedrockHole(BlockPos pos) {
      for(BlockPos off : HoleUtil.HOLE_OFFSETS) {
         if (mc.world.getBlockState(pos.add(off)).getBlock() != Blocks.BEDROCK) {
            return false;
         }
      }

      return true;
   }
}
