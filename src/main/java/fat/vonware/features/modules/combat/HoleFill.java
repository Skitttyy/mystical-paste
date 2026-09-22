package fat.vonware.features.modules.combat;

import fat.vonware.Vonware;
import fat.vonware.event.events.MotionUpdateEvent;
import fat.vonware.event.events.Render3DEvent;
import fat.vonware.features.modules.Module;
import fat.vonware.features.modules.client.BlockRender3D;
import fat.vonware.features.modules.player.Blink;
import fat.vonware.features.modules.player.FastBreak;
import fat.vonware.features.setting.Setting;
import fat.vonware.manager.PositionManager;
import fat.vonware.manager.RotationManager;
import fat.vonware.util.BlockUtil;
import fat.vonware.util.EntityUtil;
import fat.vonware.util.InventoryUtil;
import fat.vonware.util.MathUtil;
import fat.vonware.util.PlayerUtil;
import fat.vonware.util.RotationUtil;
import fat.vonware.util.Timer;
import fat.vonware.util.HoleFill.other.Ray;
import fat.vonware.util.HoleFill.other.RayTraceUtil;
import fat.vonware.util.Surround.SurroundUtil;
import fat.vonware.util.holeesp.Hole;
import fat.vonware.util.holeesp.HoleUtil;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.GlStateManager.DestFactor;
import net.minecraft.client.renderer.GlStateManager.SourceFactor;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityEnderCrystal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.network.play.client.CPacketEntityAction;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.network.play.client.CPacketPlayerDigging;
import net.minecraft.network.play.client.CPacketEntityAction.Action;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

public class HoleFill extends Module {
   private static HoleFill INSTANCE;
   private final Setting<SwapMode> switchMode;
   private final Setting<Double> range;
   private final Setting<Double> wallsRange;
   private final Setting<Integer> delay;
   private final Setting<Integer> blocksPerTick;
   private final Setting<Boolean> disableAfter;
   private final Setting<Boolean> noGlitchedBlocks;
   private final Setting<RotateMode> rotateMode;
   private final Setting<Boolean> strictDirection;
   private final Setting<Boolean> webs;
   private final Setting<Boolean> smart;
   private final Setting<Double> smartTargetRange;
   private final Setting<Double> smartBlockRange;
   private final Setting<Boolean> doubles;
   private final Setting<Boolean> incompleteDoubles;
   private final Setting<Boolean> twoByTwo;
   private final Setting<Boolean> terrain;
   private final Setting<Boolean> selfRange;
   private final Setting<Float> noSelfRange;
   private final Setting<Boolean> render;
   private final Setting<OnGroundMode> onGroundMode;
   private final Timer timer;
   private final List<Hole> holes;
   private final List<RenderHole> renderHoles;
   private Entity target;
   private int lastBlocksPlaced;
   private BlockPos pendingEventPos;
   private int pendingEventSlot;
   private int pendingOldSlot;

   public HoleFill() {
      super("HoleFill", "Automatically fills holes near you", Module.Category.COMBAT, true, false, false);
      this.switchMode = (new Setting("Switch", HoleFill.SwapMode.SilentSwap)).withDescription((Function<SwapMode, String>)((mode) -> {
         switch (mode) {
            case SilentSwap:
               return "SilentSwap: Temporarily switches to the required block using a normal slot update, then switches back after placing.";
            case Ghost:
               return "Ghost: Temporarily switches to the required block using a ghost slot switch, then switches back after placing.";
            case Require:
               return "Require: Only places when you are already holding the required HoleFill block.";
            default:
               return "";
         }
      }));
      this.range = new Setting<Double>("Range", (double)5.0F, (double)1.0F, (double)6.0F, "Maximum distance at which holes can be filled.");
      this.wallsRange = new Setting<Double>("Wall-Range", (double)3.0F, (double)1.0F, (double)6.0F, "Maximum fill distance for hole positions that cannot be directly raytraced.");
      this.delay = new Setting<Integer>("Delay", 100, 0, 1000, "Delay in milliseconds between HoleFill placement cycles.");
      this.blocksPerTick = new Setting<Integer>("BlocksPerTick", 2, 1, 10, "Maximum number of hole blocks placed during each placement cycle.");
      this.disableAfter = new Setting<Boolean>("AutoDisable", false, "Automatically disables HoleFill when no valid holes remain to fill.");
      this.noGlitchedBlocks = new Setting<Boolean>("NoGlitchedBlocks", false, "Sends extra abort-dig packets before placement to help prevent client/server block desync.");
      this.rotateMode = (new Setting("Rotate", HoleFill.RotateMode.Packet)).withDescription((Function<RotateMode, String>)((mode) -> {
         switch (mode) {
            case None:
               return "None: Places blocks without rotating toward the placement position.";
            case Event:
               return "Event: Requests the placement rotation through RotationManager during PRE, then places during POST once the rotation is applied.";
            case Packet:
               return "Packet: Sends a rotation packet directly before placing each block.";
            default:
               return "";
         }
      }));
      this.strictDirection = new Setting<Boolean>("Strict Direction", true, "Requires a stricter valid block face when choosing where to place.");
      this.webs = new Setting<Boolean>("Webs", false, "Uses cobwebs instead of obsidian for HoleFill placements.");
      this.smart = new Setting<Boolean>("Smart", true, "Only fills holes that are close enough to the current target.");
      this.smartTargetRange = new Setting<Double>("TargetRange", (double)7.0F, (double)1.0F, (double)10.0F, (v) -> (Boolean)this.smart.getValue(), "Maximum distance at which a player can be selected as the Smart HoleFill target.");
      this.smartBlockRange = new Setting<Double>("BlockRange", (double)2.0F, (double)0.0F, (double)6.0F, (v) -> (Boolean)this.smart.getValue(), "Maximum distance a hole can be from the Smart target before it is ignored.");
      this.doubles = new Setting<Boolean>("2x1", true, "Allows 2x1 double holes to be detected and filled.");
      this.incompleteDoubles = new Setting<Boolean>("Incomplete 2x1s", true, (v) -> (Boolean)this.doubles.getValue(), "Allows partially blocked 2x1 holes and fills whichever side is still available.");
      this.twoByTwo = new Setting<Boolean>("2x2", true, "Allows 2x2 holes to be included in HoleFill scanning.");
      this.terrain = new Setting<Boolean>("Terrain", false, "Includes terrain-based hole shapes that do not match the standard hole checks.");
      this.selfRange = new Setting<Boolean>("SelfRange", true, "Prevents nearby holes from being filled while you are unsafe and too close to them.");
      this.noSelfRange = new Setting<Float>("NoSelfRange", 1.0F, 0.3F, 5.0F, (v) -> (Boolean)this.selfRange.getValue(), "Minimum distance a hole must be from you while SelfRange protection is active.");
      this.render = new Setting<Boolean>("Render", true, "Renders positions that HoleFill successfully places.");
      this.onGroundMode = (new Setting("OnGround", HoleFill.OnGroundMode.None)).withDescription((Function<OnGroundMode, String>)((mode) -> {
         switch (mode) {
            case None:
               return "None: Allows HoleFill to run regardless of whether you are on the ground.";
            case Server:
               return "Server: Only runs when PositionManager reports that the server considers you on the ground.";
            case Vanilla:
               return "Vanilla: Only runs when Minecraft's local onGround state is true.";
            default:
               return "";
         }
      }));
      this.timer = new Timer();
      this.holes = new ArrayList();
      this.renderHoles = new ArrayList();
      this.lastBlocksPlaced = 0;
      this.pendingEventSlot = -1;
      this.pendingOldSlot = -1;
      this.register(this.render);
      this.register(this.switchMode);
      this.register(this.range);
      this.register(this.wallsRange);
      this.register(this.delay);
      this.register(this.blocksPerTick);
      this.register(this.disableAfter);
      this.register(this.noGlitchedBlocks);
      this.register(this.rotateMode);
      this.register(this.strictDirection);
      this.register(this.onGroundMode);
      this.register(this.doubles);
      this.register(this.incompleteDoubles);
      this.register(this.twoByTwo);
      this.register(this.terrain);
      this.register(this.smart);
      this.register(this.webs);
      this.register(this.smartTargetRange);
      this.register(this.smartBlockRange);
      this.register(this.selfRange);
      this.register(this.noSelfRange);
      INSTANCE = this;
   }

   public String getDisplayInfo() {
      return String.valueOf(this.lastBlocksPlaced);
   }

   @SubscribeEvent
   public void onRender3D(Render3DEvent event) {
      if ((Boolean)this.render.getValue() && !this.renderHoles.isEmpty()) {
         if (this.renderHoles.size() > 100) {
            this.renderHoles.clear();
         } else {
            BlockRender3D blockRender = (BlockRender3D)Vonware.moduleManager.getModuleByName("BlockRender");
            if (blockRender != null) {
               boolean texture2D = GL11.glIsEnabled(3553);
               boolean depth = GL11.glIsEnabled(2929);
               boolean blend = GL11.glIsEnabled(3042);
               GlStateManager.pushMatrix();
               GlStateManager.pushAttrib();

               try {
                  GlStateManager.enableBlend();
                  GlStateManager.disableTexture2D();
                  GlStateManager.disableLighting();
                  GlStateManager.tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ZERO);
                  GlStateManager.disableDepth();
                  GlStateManager.depthMask(false);
                  GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                  BlockRender3D.render(this.renderHoles);
               } finally {
                  GlStateManager.depthMask(true);
                  if (depth) {
                     GlStateManager.enableDepth();
                  } else {
                     GlStateManager.disableDepth();
                  }

                  if (texture2D) {
                     GlStateManager.enableTexture2D();
                  } else {
                     GlStateManager.disableTexture2D();
                  }

                  if (blend) {
                     GlStateManager.enableBlend();
                  } else {
                     GlStateManager.disableBlend();
                  }

                  GlStateManager.popAttrib();
                  GlStateManager.popMatrix();
               }

            }
         }
      }
   }

   @SubscribeEvent
   public void onMotionUpdate(MotionUpdateEvent event) {
      if (fullNullCheck()) {
         this.clearPendingEventPlacement();
      } else if (event.getStage() == 1) {
         this.executePendingEventPlacement();
      } else if (event.getStage() == 0) {
         if (((Blink)Vonware.moduleManager.getModuleByClass(Blink.class)).isEnabled()) {
            this.clearPendingEventPlacement();
         } else if (this.onGroundMode.getValue() == HoleFill.OnGroundMode.Server && !PositionManager.INSTANCE.isOnGround()) {
            this.clearPendingEventPlacement();
         } else if (this.onGroundMode.getValue() == HoleFill.OnGroundMode.Vanilla && !mc.player.onGround) {
            this.clearPendingEventPlacement();
         } else {
            FastBreak fastBreak = (FastBreak)Vonware.moduleManager.getModuleByClass(FastBreak.class);
            if (fastBreak != null && fastBreak.isEnabled() && fastBreak.breakLock) {
               this.clearPendingEventPlacement();
            } else {
               this.target = PlayerUtil.getTargetEntity((Double)this.smartTargetRange.getValue());
               if (this.target != null) {
                  BlockPos targetPos = new BlockPos(Math.floor(this.target.posX), Math.floor(this.target.posY), Math.floor(this.target.posZ));
                  if (HoleUtil.isPositionInAnyHole(targetPos)) {
                     this.clearPendingEventPlacement();
                     return;
                  }
               }

               this.timer.setDelay(((Integer)this.delay.getValue()).longValue());
               if (this.timer.isPassed()) {
                  this.getHoles();
                  if (this.holes != null && !this.holes.isEmpty()) {
                     int blockSlot = InventoryUtil.getHotbarItemSlot(Item.getItemFromBlock(this.getBlockType()));
                     if (this.switchMode.getValue() == HoleFill.SwapMode.Require && mc.player.getHeldItem(EnumHand.MAIN_HAND).getItem() != Item.getItemFromBlock(this.getBlockType())) {
                        this.clearPendingEventPlacement();
                     } else if (this.switchMode.getValue() != HoleFill.SwapMode.Require && blockSlot == -1) {
                        this.clearPendingEventPlacement();
                     } else {
                        int oldSlot = mc.player.inventory.currentItem;
                        if (this.rotateMode.getValue() == HoleFill.RotateMode.Event) {
                           this.queueEventPlacement(blockSlot, oldSlot);
                        } else {
                           this.clearPendingEventPlacement();
                           this.timer.reset();
                           int blocksPlaced = 0;

                           for(Hole hole : this.holes) {
                              if (blocksPlaced >= (Integer)this.blocksPerTick.getValue()) {
                                 break;
                              }

                              if (hole.getSecond() != null && (Boolean)this.incompleteDoubles.getValue()) {
                                 boolean firstBlocked = this.isBlockedAbove(hole.getFirst());
                                 boolean secondBlocked = this.isBlockedAbove(hole.getSecond());
                                 if (firstBlocked && secondBlocked) {
                                    continue;
                                 }

                                 if (firstBlocked || secondBlocked) {
                                    BlockPos openSide = secondBlocked ? hole.getFirst() : hole.getSecond();
                                    if (this.canPlaceBlock(openSide)) {
                                       if (this.switchMode.getValue() != HoleFill.SwapMode.Require) {
                                          this.doSwitch(blockSlot);
                                       }

                                       this.doSneakPlace(openSide, event);
                                       if ((Boolean)this.render.getValue()) {
                                          this.addRenderHole(openSide);
                                       }

                                       ++blocksPlaced;
                                    }
                                    continue;
                                 }
                              }

                              if (this.canPlaceBlock(hole.getFirst())) {
                                 if (this.switchMode.getValue() != HoleFill.SwapMode.Require) {
                                    this.doSwitch(blockSlot);
                                 }

                                 this.doSneakPlace(hole.getFirst(), event);
                                 if ((Boolean)this.render.getValue()) {
                                    this.addRenderHole(hole.getFirst());
                                 }

                                 ++blocksPlaced;
                                 if (hole.getSecond() != null && this.canPlaceBlock(hole.getSecond()) && blocksPlaced < (Integer)this.blocksPerTick.getValue()) {
                                    this.doSneakPlace(hole.getSecond(), event);
                                    if ((Boolean)this.render.getValue()) {
                                       this.addRenderHole(hole.getSecond());
                                    }

                                    ++blocksPlaced;
                                 }
                              }
                           }

                           if (this.switchMode.getValue() != HoleFill.SwapMode.Require) {
                              InventoryUtil.switchToSlotUpdate(oldSlot);
                           }

                           this.lastBlocksPlaced = blocksPlaced;
                        }
                     }
                  } else {
                     this.clearPendingEventPlacement();
                     if ((Boolean)this.disableAfter.getValue()) {
                        this.setEnabled(false);
                     }

                  }
               }
            }
         }
      }
   }

   private void queueEventPlacement(int blockSlot, int oldSlot) {
      this.clearPendingEventPlacement();

      for(Hole hole : this.holes) {
         BlockPos position = this.getFirstEventPlaceablePosition(hole);
         if (position != null) {
            EnumFacing side = BlockUtil.getPlaceableSide(position, (Boolean)this.strictDirection.getValue());
            if (side != null) {
               float[] rotations = RotationUtil.getBlockRotations(position, side);
               if (rotations != null) {
                  this.pendingEventPos = position;
                  this.pendingEventSlot = blockSlot;
                  this.pendingOldSlot = oldSlot;
                  RotationManager.INSTANCE.requestRotation(RotationManager.RotationSource.HOLE_FILL, rotations[0], rotations[1]);
                  return;
               }
            }
         }
      }

   }

   private BlockPos getFirstEventPlaceablePosition(Hole hole) {
      if (hole == null) {
         return null;
      } else {
         if (hole.getSecond() != null && (Boolean)this.incompleteDoubles.getValue()) {
            boolean firstBlocked = this.isBlockedAbove(hole.getFirst());
            boolean secondBlocked = this.isBlockedAbove(hole.getSecond());
            if (firstBlocked && secondBlocked) {
               return null;
            }

            if (firstBlocked || secondBlocked) {
               BlockPos openSide = secondBlocked ? hole.getFirst() : hole.getSecond();
               return this.canPlaceBlock(openSide) ? openSide : null;
            }
         }

         if (this.canPlaceBlock(hole.getFirst())) {
            return hole.getFirst();
         } else if (hole.getSecond() != null && this.canPlaceBlock(hole.getSecond())) {
            return hole.getSecond();
         } else {
            return null;
         }
      }
   }

   private void executePendingEventPlacement() {
      if (this.pendingEventPos != null) {
         if (RotationManager.INSTANCE.getActiveRotationSource() != RotationManager.RotationSource.HOLE_FILL) {
            this.clearPendingEventPlacement();
         } else {
            BlockPos position = this.pendingEventPos;
            int blockSlot = this.pendingEventSlot;
            int oldSlot = this.pendingOldSlot;
            this.clearPendingEventPlacement();
            if (this.canPlaceBlock(position)) {
               if (this.switchMode.getValue() != HoleFill.SwapMode.Require) {
                  if (blockSlot == -1) {
                     return;
                  }

                  this.doSwitch(blockSlot);
               }

               this.doSneakPlaceWithoutRotation(position);
               if ((Boolean)this.render.getValue()) {
                  this.addRenderHole(position);
               }

               if (this.switchMode.getValue() != HoleFill.SwapMode.Require && oldSlot != -1) {
                  InventoryUtil.switchToSlotUpdate(oldSlot);
               }

               this.timer.reset();
               this.lastBlocksPlaced = 1;
            }
         }
      }
   }

   private void doSneakPlaceWithoutRotation(BlockPos pos) {
      boolean sneak = this.placeNeedsSneak(pos);
      if (sneak) {
         mc.getConnection().sendPacket(new CPacketEntityAction(mc.player, Action.START_SNEAKING));
      }

      if ((Boolean)this.noGlitchedBlocks.getValue()) {
         this.sendNoGlitchPoke(pos);
      }

      BlockUtil.placeBlockFill(pos, BlockUtil.getPlaceableSide(pos, (Boolean)this.strictDirection.getValue()));
      if (sneak) {
         mc.getConnection().sendPacket(new CPacketEntityAction(mc.player, Action.STOP_SNEAKING));
      }

   }

   private void clearPendingEventPlacement() {
      this.pendingEventPos = null;
      this.pendingEventSlot = -1;
      this.pendingOldSlot = -1;
   }

   private void addRenderHole(BlockPos pos) {
      if ((Boolean)this.render.getValue() && pos != null) {
         int fadeTime = BlockRender3D.getInstance().getFadeTime();
         this.renderHoles.removeIf((rhx) -> rhx.getProgress(fadeTime) >= 1.0F);
         if (this.renderHoles.size() < 20) {
            for(RenderHole rh : this.renderHoles) {
               if (rh.pos.equals(pos)) {
                  return;
               }
            }

            this.renderHoles.add(new RenderHole(pos));
         }
      }
   }

   public void getHoles() {
      this.loadHoles();
   }

   public void loadHoles() {
      List<Hole> foundHoles = HoleUtil.getHoles(mc.player, ((Double)this.range.getValue()).floatValue(), (Boolean)this.doubles.getValue(), (Boolean)this.webs.getValue(), false, (Boolean)this.terrain.getValue(), (Boolean)this.incompleteDoubles.getValue(), (Boolean)this.twoByTwo.getValue());
      List<Hole> filteredHoles = new ArrayList();
      BlockPos playerPos = new BlockPos(Math.floor(mc.player.posX), Math.floor(mc.player.posY), Math.floor(mc.player.posZ));
      boolean playerIsSafe = HoleUtil.isHole(playerPos) || HoleUtil.isObbyHole(playerPos) || HoleUtil.isBedrockHole(playerPos) || EntityUtil.isInBurrow(mc.player);

      for(Hole hole : foundHoles) {
         if (this.isHoleEmpty(hole)) {
            if ((Boolean)this.selfRange.getValue() && !playerIsSafe) {
               double distToPlayer = mc.player.getDistance((double)hole.getFirst().getX() + (double)0.5F, (double)hole.getFirst().getY(), (double)hole.getFirst().getZ() + (double)0.5F);
               if (distToPlayer < (double)(Float)this.noSelfRange.getValue()) {
                  continue;
               }
            }

            if ((Boolean)this.smart.getValue()) {
               if (this.target == null) {
                  continue;
               }

               if (this.target instanceof EntityPlayer) {
                  EntityPlayer tp = (EntityPlayer)this.target;
                  if (EntityUtil.isFullPhasing(tp) || EntityUtil.isSuffocating(tp) || EntityUtil.isBurrowPhasing(tp) || EntityUtil.isTrapped(tp)) {
                     continue;
                  }
               }

               double distToTarget = this.target.getDistance((double)hole.getFirst().getX() + (double)0.5F, (double)(hole.getFirst().getY() + 1), (double)hole.getFirst().getZ() + (double)0.5F);
               if (distToTarget >= (Double)this.smartBlockRange.getValue()) {
                  continue;
               }
            }

            filteredHoles.add(hole);
         }
      }

      if ((Boolean)this.smart.getValue() && this.target != null) {
         Entity t = this.target;
         filteredHoles.sort(Comparator.comparingDouble((h) -> t.getDistanceSq((double)h.getFirst().getX() + (double)0.5F, (double)(h.getFirst().getY() + 1), (double)h.getFirst().getZ() + (double)0.5F)));
      }

      this.holes.clear();
      this.holes.addAll(filteredHoles);
   }

   private boolean isHoleEmpty(Hole hole) {
      AxisAlignedBB bb;
      if (hole.getSecond() != null) {
         bb = new AxisAlignedBB((double)hole.getFirst().getX(), (double)hole.getFirst().getY(), (double)hole.getFirst().getZ(), (double)(hole.getSecond().getX() + 1), (double)(hole.getSecond().getY() + 1), (double)(hole.getSecond().getZ() + 1));
      } else {
         bb = new AxisAlignedBB(hole.getFirst());
      }

      for(Entity entity : mc.world.getEntitiesWithinAABB(Entity.class, bb)) {
         if (entity instanceof EntityPlayer || entity instanceof EntityEnderCrystal) {
            return false;
         }
      }

      return true;
   }

   boolean canPlaceBlock(BlockPos pos) {
      if (!mc.world.getBlockState(pos).getMaterial().isReplaceable()) {
         return false;
      } else if (BlockUtil.getPlaceableSide(pos, (Boolean)this.strictDirection.getValue()) == null) {
         return false;
      } else {
         for(Entity entity : mc.world.getEntitiesWithinAABB(Entity.class, new AxisAlignedBB(pos))) {
            if (entity instanceof EntityPlayer || entity instanceof EntityEnderCrystal) {
               return false;
            }
         }

         if (this.noPlaceTrace(pos) && BlockUtil.getDistanceSq((Entity)mc.player, pos) >= MathUtil.square((Double)this.wallsRange.getValue())) {
            return false;
         } else {
            return PositionManager.INSTANCE.getDistanceSqEyeHeight((double)pos.getX() + (double)0.5F, (double)pos.getY() + (double)0.5F, (double)pos.getZ() + (double)0.5F) <= MathUtil.square((Double)this.range.getValue());
         }
      }
   }

   private boolean noPlaceTrace(BlockPos pos) {
      for(EnumFacing facing : EnumFacing.values()) {
         Ray ray = RayTraceUtil.rayTrace(mc.player, pos, facing, mc.world, Blocks.OBSIDIAN.getDefaultState(), (double)-1.0F);
         if (ray.isLegit()) {
            return false;
         }
      }

      return true;
   }

   private boolean isBlockedAbove(BlockPos pos) {
      if (mc.world == null) {
         return false;
      } else {
         for(int i = 1; i <= 3; ++i) {
            if (!BlockUtil.isAir(pos.up(i))) {
               return true;
            }
         }

         return false;
      }
   }

   public void doSwitch(int slot) {
      if (this.switchMode.getValue() == HoleFill.SwapMode.SilentSwap) {
         InventoryUtil.switchToSlotUpdateIf(slot);
      }

      if (this.switchMode.getValue() == HoleFill.SwapMode.Ghost) {
         InventoryUtil.switchToSlotGhost(slot);
      }

   }

   public void doRotate(BlockPos pos, MotionUpdateEvent event) {
      if (this.rotateMode.getValue() != HoleFill.RotateMode.None) {
         float[] rots = RotationUtil.getBlockRotations(pos, BlockUtil.getPlaceableSide(pos, (Boolean)this.strictDirection.getValue()));
         if (rots != null) {
            if (this.rotateMode.getValue() == HoleFill.RotateMode.Event) {
               RotationManager.INSTANCE.requestRotation(RotationManager.RotationSource.HOLE_FILL, rots[0], rots[1]);
            }

            if (this.rotateMode.getValue() == HoleFill.RotateMode.Packet) {
               mc.getConnection().sendPacket(new CPacketPlayer.Rotation(rots[0], rots[1], mc.player.onGround));
            }

         }
      }
   }

   private void doSneakPlace(BlockPos pos, MotionUpdateEvent event) {
      boolean sneak = this.placeNeedsSneak(pos);
      if (sneak) {
         mc.getConnection().sendPacket(new CPacketEntityAction(mc.player, Action.START_SNEAKING));
      }

      if ((Boolean)this.noGlitchedBlocks.getValue()) {
         this.sendNoGlitchPoke(pos);
      }

      this.doRotate(pos, event);
      BlockUtil.placeBlockFill(pos, BlockUtil.getPlaceableSide(pos, (Boolean)this.strictDirection.getValue()));
      if (sneak) {
         mc.getConnection().sendPacket(new CPacketEntityAction(mc.player, Action.STOP_SNEAKING));
      }

   }

   private void sendNoGlitchPoke(BlockPos pos) {
      EnumFacing side = BlockUtil.getPlaceableSide(pos, (Boolean)this.strictDirection.getValue());
      if (side != null) {
         BlockPos neighbor = pos.offset(side);
         mc.getConnection().sendPacket(new CPacketPlayerDigging(net.minecraft.network.play.client.CPacketPlayerDigging.Action.ABORT_DESTROY_BLOCK, neighbor, side.getOpposite()));
         mc.getConnection().sendPacket(new CPacketPlayerDigging(net.minecraft.network.play.client.CPacketPlayerDigging.Action.ABORT_DESTROY_BLOCK, pos, side));
      }
   }

   private boolean placeNeedsSneak(BlockPos pos) {
      EnumFacing side = BlockUtil.getPlaceableSide(pos, (Boolean)this.strictDirection.getValue());
      if (side == null) {
         return false;
      } else {
         BlockPos neighbor = pos.offset(side);
         return SurroundUtil.needsSneak(neighbor);
      }
   }

   public Block getBlockType() {
      return (Boolean)this.webs.getValue() ? Blocks.WEB : Blocks.OBSIDIAN;
   }

   public void onEnable() {
      this.clearPendingEventPlacement();
      this.renderHoles.clear();
      super.onEnable();
   }

   public void onDisable() {
      this.clearPendingEventPlacement();
      this.renderHoles.clear();
      super.onDisable();
   }

   public static HoleFill getInstance() {
      return INSTANCE;
   }

   public static enum OnGroundMode {
      None,
      Server,
      Vanilla;
   }

   public static enum RotateMode {
      None,
      Event,
      Packet;
   }

   public static enum SwapMode {
      SilentSwap,
      Ghost,
      Require;
   }

   private static class RenderHole implements BlockRender3D.RenderBlock {
      final BlockPos pos;
      final long startTime;
      final AxisAlignedBB bb;

      RenderHole(BlockPos pos) {
         this.pos = pos;
         this.startTime = System.currentTimeMillis();
         this.bb = new AxisAlignedBB(pos);
      }

      public BlockPos getPos() {
         return this.pos;
      }

      public long getStartTime() {
         return this.startTime;
      }

      public AxisAlignedBB getBB() {
         return this.bb;
      }

      public float getProgress(int duration) {
         return Math.min(1.0F, (float)(System.currentTimeMillis() - this.startTime) / (float)duration);
      }
   }
}
