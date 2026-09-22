package fat.vonware.features.modules.player;

import fat.vonware.event.events.MotionUpdateEvent;
import fat.vonware.event.events.MoveEvent;
import fat.vonware.event.events.Render2DEvent;
import fat.vonware.event.events.Render3DEvent;
import fat.vonware.features.modules.Module;
import fat.vonware.features.modules.client.BlockRender3D;
import fat.vonware.features.setting.Setting;
import fat.vonware.manager.RotationManager;
import fat.vonware.util.BlockUtil;
import fat.vonware.util.InventoryUtil;
import fat.vonware.util.MovementUtil;
import fat.vonware.util.RotationUtil;
import fat.vonware.util.Timer;
import fat.vonware.util.Animations.render.Render2DMethods;
import fat.vonware.util.BlockPlaceModules.BlockPlaceUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.minecraft.block.Block;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class Scaffold extends Module {
   private final Setting<Boolean> smart = new Setting<Boolean>("Smart", false, "Uses your health to decide whether Scaffold should continue placing.");
   private final Setting<Integer> health = new Setting<Integer>("Health", 5, 0, 20, (v) -> (Boolean)this.smart.getValue(), "Health threshold used by Smart Scaffold.");
   private final Setting<Boolean> safeWalk = new Setting<Boolean>("SafeWalk", true, "Prevents you from walking off unsupported block edges while scaffolding.");
   private final Setting<Boolean> extend = new Setting<Boolean>("Extend", false, "Places Scaffold blocks farther ahead of your current position.");
   private final Setting<Integer> extendBy = new Setting<Integer>("Blocks", 1, 0, 5, (v) -> (Boolean)this.extend.getValue(), "Number of blocks ahead used by Scaffold extension.");
   private final Setting<Boolean> breakCrystal = new Setting<Boolean>("Break Crystal", true, "Attacks end crystals that block a required Scaffold placement.");
   private final Setting<RotateMode> rotate;
   private final Setting<Integer> placeDelay;
   private final Setting<SwapMode> autoSwap;
   private final Setting<BlockListMode> blockList;
   private final Setting<Boolean> tower;
   private final Setting<Boolean> useTimer;
   private final Setting<Float> timerSpeed;
   private final Setting<Boolean> render;
   private final Setting<Boolean> render2D;
   private final Setting<Integer> hotbarGap;
   private static final ItemStack OBSIDIAN_ICON;
   private final Timer timer;
   private final Timer placeTimer;
   private BlockPos PlacePos;
   private final List<BlockRender3D.SimpleRenderBlock> renderBlocks;

   public Scaffold() {
      super("Scaffold", "Places blocks underneath you", Module.Category.PLAYER, true, false, false);
      this.rotate = (new Setting("Rotate", Scaffold.RotateMode.Packet)).withDescription((Function<RotateMode, String>)((mode) -> {
         switch (mode) {
            case None:
               return "None: Places Scaffold blocks without applying rotations.";
            case Packet:
               return "Packet: Sends direct rotation packets toward Scaffold placement positions.";
            case Event:
               return "Event: Uses the motion-event rotation path before Scaffold placement.";
            default:
               return "";
         }
      }));
      this.placeDelay = new Setting<Integer>("PlaceDelay", 1, 0, 5, "Delay in ticks between Scaffold placement attempts.");
      this.autoSwap = (new Setting("Swap", Scaffold.SwapMode.Silent)).withDescription((Function<SwapMode, String>)((mode) -> {
         switch (mode) {
            case None:
               return "None: Requires you to already be holding a valid Scaffold block.";
            case Normal:
               return "Normal: Visibly switches to a valid block slot for placement.";
            case Silent:
               return "Silent: Temporarily switches to a valid block slot and recovers your previous slot after placement.";
            default:
               return "";
         }
      }));
      this.blockList = (new Setting("BlockList", Scaffold.BlockListMode.AllBlocks)).withDescription((Function<BlockListMode, String>)((mode) -> {
         switch (mode) {
            case ObsidianOnly:
               return "ObsidianOnly: Only allows obsidian to be used for Scaffold placements.";
            case AllNonTileBlocks:
               return "AllNonTileBlocks: Allows valid full blocks except blocks that contain tile entities.";
            case AllBlocks:
               return "AllBlocks: Allows any valid full block to be used for Scaffold.";
            default:
               return "";
         }
      }));
      this.tower = new Setting<Boolean>("Tower", true, "Allows Scaffold to build vertically while using the tower behavior.");
      this.useTimer = new Setting<Boolean>("UseTimer", false, "Adjusts the client timer while Scaffold's timer behavior is active.");
      this.timerSpeed = new Setting<Float>("TimerSpeed", 1.0F, 0.1F, 10.0F, (v) -> (Boolean)this.useTimer.getValue(), "Timer speed used while UseTimer is enabled.");
      this.render = new Setting<Boolean>("Render", true, "Renders Scaffold placement positions.");
      this.render2D = new Setting<Boolean>("Render2D", true, "Displays the Scaffold block-count indicator on the HUD.");
      this.hotbarGap = new Setting<Integer>("HotbarGap", 63, 10, 100, (v) -> (Boolean)this.render2D.getValue(), "Controls the spacing used by the Scaffold HUD indicator.");
      this.timer = new Timer();
      this.placeTimer = new Timer();
      this.renderBlocks = new ArrayList();
      this.register(this.safeWalk);
      this.register(this.extend);
      this.register(this.extendBy);
      this.register(this.breakCrystal);
      this.register(this.rotate);
      this.register(this.placeDelay);
      this.register(this.autoSwap);
      this.register(this.blockList);
      this.register(this.tower);
      this.register(this.smart);
      this.register(this.health);
      this.register(this.useTimer);
      this.register(this.timerSpeed);
      this.register(this.render);
      this.register(this.render2D);
      this.register(this.hotbarGap);
   }

   public String getDisplayInfo() {
      return "Instant";
   }

   @SubscribeEvent
   public void onRender3D(Render3DEvent event) {
      if ((Boolean)this.render.getValue() && !this.renderBlocks.isEmpty()) {
         BlockRender3D.render(this.renderBlocks);
      }
   }

   private void addRenderBlock(BlockPos pos) {
      if ((Boolean)this.render.getValue()) {
         for(BlockRender3D.SimpleRenderBlock rb : this.renderBlocks) {
            if (rb.getPos().equals(pos)) {
               return;
            }
         }

         this.renderBlocks.add(new BlockRender3D.SimpleRenderBlock(pos));
      }
   }

   @SubscribeEvent
   public void onRender2D(Render2DEvent event) {
      if ((Boolean)this.render2D.getValue()) {
         Render2DMethods.drawHudPill("Scaffold " + InventoryUtil.getFullBlockCount(), OBSIDIAN_ICON, "x", (Integer)this.hotbarGap.getValue(), 20.0F, 1.3F, -1728053248, -1426063361, -1);
      }
   }

   private boolean isValidBlockStack(ItemStack stack) {
      if (!InventoryUtil.isNull(stack) && stack.getItem() instanceof ItemBlock) {
         Block block = Block.getBlockFromItem(stack.getItem());
         switch ((BlockListMode)this.blockList.getValue()) {
            case ObsidianOnly:
               return block == Blocks.OBSIDIAN;
            case AllNonTileBlocks:
               return block.getDefaultState().isFullBlock() && !block.hasTileEntity(block.getDefaultState());
            case AllBlocks:
            default:
               return block.getDefaultState().isFullBlock();
         }
      } else {
         return false;
      }
   }

   private int getPlaceableBlockCount() {
      int count = 0;

      for(ItemStack stack : mc.player.inventory.mainInventory) {
         if (this.isValidBlockStack(stack)) {
            count += stack.stackSize;
         }
      }

      return count;
   }

   @SubscribeEvent
   public void onMove(MoveEvent event) {
      if ((Boolean)this.safeWalk.getValue()) {
         double x = event.getX();
         double y = event.getY();
         double z = event.getZ();
         if (mc.player.onGround) {
            double increment = 0.05;

            while(x != (double)0.0F && this.isOffsetBBEmpty(x, (double)-1.0F, (double)0.0F)) {
               if (x < 0.05 && x >= -0.05) {
                  x = (double)0.0F;
               } else if (x > (double)0.0F) {
                  x -= 0.05;
               } else {
                  x += 0.05;
               }
            }

            while(z != (double)0.0F && this.isOffsetBBEmpty((double)0.0F, (double)-1.0F, z)) {
               if (z < 0.05 && z >= -0.05) {
                  z = (double)0.0F;
               } else if (z > (double)0.0F) {
                  z -= 0.05;
               } else {
                  z += 0.05;
               }
            }

            while(x != (double)0.0F && z != (double)0.0F && this.isOffsetBBEmpty(x, (double)-1.0F, z)) {
               if (z < 0.05 && z >= -0.05) {
                  z = (double)0.0F;
               } else if (z > (double)0.0F) {
                  z -= 0.05;
               } else {
                  z += 0.05;
               }
            }
         }

         event.setX(x);
         event.setY(y);
         event.setZ(z);
      }
   }

   public boolean isOffsetBBEmpty(double offsetX, double offsetY, double offsetZ) {
      EntityPlayerSP playerSP = mc.player;
      return mc.world.getCollisionBoxes(playerSP, playerSP.getEntityBoundingBox().offset(offsetX, offsetY, offsetZ)).isEmpty();
   }

   private boolean hasObsidianInHotbar() {
      if (mc.player == null) {
         return false;
      } else {
         for(int slot = 0; slot < 9; ++slot) {
            ItemStack stack = mc.player.inventory.getStackInSlot(slot);
            if (!InventoryUtil.isNull(stack) && stack.getItem() instanceof ItemBlock && Block.getBlockFromItem(stack.getItem()) == Blocks.OBSIDIAN) {
               return true;
            }
         }

         return false;
      }
   }

   public void onEnable() {
      if (!fullNullCheck() && this.hasObsidianInHotbar()) {
         this.PlacePos = new BlockPos(mc.player.posX, mc.player.posY - (double)1.0F, mc.player.posZ);
         this.timer.reset();
         this.renderBlocks.clear();
         if ((Boolean)this.useTimer.getValue()) {
            mc.timer.tickLength = 50.0F / (Float)this.timerSpeed.getValue();
         }

      } else {
         this.disable();
      }
   }

   public void onDisable() {
      this.renderBlocks.clear();
      mc.timer.tickLength = 50.0F;
   }

   @SubscribeEvent(
      priority = EventPriority.HIGHEST
   )
   public void onMotionUpdate(MotionUpdateEvent event) {
      if (event.getStage() == 0) {
         if (!fullNullCheck() && this.hasObsidianInHotbar()) {
            if (this.placeTimer.passedMs((long)(Integer)this.placeDelay.getValue() * 100L)) {
               if (this.PlacePos == null) {
                  this.PlacePos = new BlockPos(mc.player.posX, mc.player.posY - (double)1.0F, mc.player.posZ);
               }

               if ((Boolean)this.smart.getValue() && mc.player.getHealth() <= (float)(Integer)this.health.getValue()) {
                  this.disable();
               } else {
                  this.doScaffold(event);
               }
            }
         } else {
            this.disable();
         }
      }
   }

   public void doScaffold(MotionUpdateEvent event) {
      if (!MovementUtil.isJumping()) {
         this.timer.reset();
      }

      if ((Boolean)this.tower.getValue() && (Boolean)this.breakCrystal.getValue()) {
         BlockUtil.breakCrystalInWay(this.PlacePos);
      }

      this.PlacePos = new BlockPos(mc.player.posX, mc.player.posY - (double)1.0F, mc.player.posZ);
      EnumFacing facing = mc.player.getHorizontalFacing();
      if (MovementUtil.wasdKeys() && (Boolean)this.extend.getValue()) {
         int extendDistance = (Integer)this.extendBy.getValue();

         for(int i = 1; i <= extendDistance; ++i) {
            BlockPos extendedPos = this.PlacePos.offset(facing, i);
            this.place(extendedPos, event);
         }
      }

      this.place(this.PlacePos, event);
   }

   private void faceVector(Vec3d vec, MotionUpdateEvent event) {
      if (this.rotate.getValue() == Scaffold.RotateMode.Packet) {
         float[] rotations = RotationUtil.getLegitRotations(vec);
         mc.player.connection.sendPacket(RotationUtil.getRotationPacket(rotations));
      } else if (this.rotate.getValue() == Scaffold.RotateMode.Event) {
         if (event == null) {
            return;
         }

         float[] rotations = RotationUtil.getLegitRotations(vec);
         RotationManager.INSTANCE.requestRotation(RotationManager.RotationSource.SCAFFOLD, rotations[0], rotations[1]);
      }

   }

   public void place(BlockPos pos, MotionUpdateEvent event) {
      if (!(pos.getDistance((int)mc.player.posX, (int)mc.player.posY, (int)mc.player.posZ) > (double)6.0F)) {
         if (BlockUtil.canReplace(pos)) {
            if (BlockPlaceUtil.canPlace(pos)) {
               int oldSlot = mc.player.inventory.currentItem;
               int newSlot = -1;

               for(int i = 0; i < 9; ++i) {
                  ItemStack stack = mc.player.inventory.getStackInSlot(i);
                  if (this.isValidBlockStack(stack)) {
                     newSlot = i;
                     break;
                  }
               }

               if (newSlot != -1) {
                  EnumFacing side = BlockUtil.getFirstFacing(pos);
                  if (side != null) {
                     boolean switched = false;
                     if (!this.isValidBlockStack(mc.player.getHeldItemMainhand())) {
                        if (this.autoSwap.getValue() == Scaffold.SwapMode.None) {
                           return;
                        }

                        if (this.autoSwap.getValue() == Scaffold.SwapMode.Silent) {
                           switched = true;
                        }

                        InventoryUtil.doSwap(newSlot);
                     }

                     if (MovementUtil.isJumping() && !MovementUtil.isMoving() && (Boolean)this.tower.getValue()) {
                        mc.player.motionX = (double)0.0F;
                        mc.player.motionZ = (double)0.0F;
                        mc.player.jump();
                        if (this.timer.passedMs(1500L)) {
                           mc.player.motionY = -0.28;
                           this.timer.reset();
                        }
                     }

                     BlockPos neighbour = pos.offset(side);
                     EnumFacing opposite = side.getOpposite();
                     Vec3d hitVec = (new Vec3d(neighbour)).add((double)0.5F, (double)0.5F, (double)0.5F).add((new Vec3d(opposite.getDirectionVec())).scale((double)0.5F));
                     this.faceVector(hitVec, event);
                     this.placeTimer.reset();
                     BlockUtil.placeBlockFill(pos, side);
                     this.addRenderBlock(pos);
                     if (switched) {
                        InventoryUtil.doSwap(oldSlot);
                     }

                  }
               }
            }
         }
      }
   }

   static {
      OBSIDIAN_ICON = new ItemStack(Blocks.OBSIDIAN);
   }

   public static enum SwapMode {
      None,
      Normal,
      Silent;
   }

   public static enum RotateMode {
      None,
      Packet,
      Event;
   }

   public static enum BlockListMode {
      ObsidianOnly,
      AllBlocks,
      AllNonTileBlocks;
   }
}
