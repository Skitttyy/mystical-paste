package fat.vonware.features.modules.combat;

import fat.vonware.Vonware;
import fat.vonware.event.events.PacketEvent;
import fat.vonware.event.events.Render2DEvent;
import fat.vonware.event.events.Render3DEvent;
import fat.vonware.features.command.Command;
import fat.vonware.features.modules.Module;
import fat.vonware.features.modules.client.BlockRender3D;
import fat.vonware.features.modules.client.Debug;
import fat.vonware.features.setting.Setting;
import fat.vonware.manager.PriorityManager;
import fat.vonware.util.BlockUtil;
import fat.vonware.util.DamageUtil;
import fat.vonware.util.EntityUtil;
import fat.vonware.util.InventoryUtil;
import fat.vonware.util.PositionUtil;
import fat.vonware.util.RotationUtil;
import fat.vonware.util.Timer;
import fat.vonware.util.Animations.render.Render2DMethods;
import fat.vonware.util.Surround.AntiCityMethods;
import fat.vonware.util.Surround.SurroundUtil;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAnvil;
import net.minecraft.block.BlockBrewingStand;
import net.minecraft.block.BlockChest;
import net.minecraft.block.BlockDispenser;
import net.minecraft.block.BlockDropper;
import net.minecraft.block.BlockEnchantmentTable;
import net.minecraft.block.BlockEnderChest;
import net.minecraft.block.BlockFurnace;
import net.minecraft.block.BlockHopper;
import net.minecraft.block.BlockWorkbench;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityEnderCrystal;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.CPacketAnimation;
import net.minecraft.network.play.client.CPacketClickWindow;
import net.minecraft.network.play.client.CPacketEntityAction;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.network.play.client.CPacketPlayerDigging;
import net.minecraft.network.play.client.CPacketPlayerTryUseItem;
import net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock;
import net.minecraft.network.play.client.CPacketUseEntity;
import net.minecraft.network.play.client.CPacketPlayerDigging.Action;
import net.minecraft.network.play.server.SPacketBlockBreakAnim;
import net.minecraft.network.play.server.SPacketBlockChange;
import net.minecraft.network.play.server.SPacketMultiBlockChange;
import net.minecraft.network.play.server.SPacketPlayerPosLook;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class Surround extends Module {
   private static Surround INSTANCE;
   private final Setting<Integer> blocksPerTick = new Setting<Integer>("BlocksPerTick", 8, 1, 8, "Maximum number of surround and support blocks placed during each tick.");
   private final Setting<Boolean> rotate = new Setting<Boolean>("Rotate", true, "Rotates toward surround placements and blocking crystals before interacting.");
   private final Setting<Boolean> swing = new Setting<Boolean>("Swing", true, "Swings your hand when Surround places blocks or attacks blocking crystals.");
   private final Setting<Boolean> center = new Setting<Boolean>("Center", false, "Moves you to the center of your current block when Surround enables.");
   private final Setting<Boolean> autoDisable = new Setting<Boolean>("AutoDisable", true, "Allows Surround to automatically disable after configured movement or teleport events.");
   private final Setting<Boolean> adJump = new Setting<Boolean>("Jump", true, (v) -> (Boolean)this.autoDisable.getValue(), "Disables Surround when your Y position rises above the position where it was enabled.");
   private final Setting<Boolean> adRubberband = new Setting<Boolean>("Rubberband", true, (v) -> (Boolean)this.autoDisable.getValue(), "Disables Surround when the server sends an unexpected position correction.");
   private final Setting<Boolean> strictDirection = new Setting<Boolean>("Strict Direction", false, "Only uses placement faces that are directly visible when selecting a side.");
   private final Setting<SwapMode> swapMode;
   private final Setting<Boolean> onlyGround;
   private final Setting<Boolean> supportPlace;
   private final Setting<Boolean> breakCrystals;
   private final Setting<CrystalBreakMode> crystalBreakMode;
   private final Setting<Boolean> floorPlace;
   private final Setting<Boolean> render;
   private final Setting<Boolean> antiCity;
   private final Setting<Boolean> cornerClip;
   private final Setting<Boolean> extend;
   private final Setting<Boolean> render2D;
   private final Setting<Integer> hotbarGap;
   private static final ItemStack OBSIDIAN_ICON;
   private final List<BlockRender3D.SimpleRenderBlock> renderBlocks;
   private final AntiCityMethods.AntiCityData antiCityData;
   private final Timer timer;
   private final List<BlockPos> placePositions;
   private final List<Packet<?>> packets;
   private final Set<BlockPos> placedThisTick;
   private final Set<BlockPos> brokeCrystalsAt;
   private int blocksPlaced;
   private boolean didPlace;
   private int lastHotbarSlot;
   private double enablePosY;
   private long lastAntiCityPlace;
   private ItemStack stackAlt;
   private int lastSlot;
   private ItemStack stackAltNew;
   private boolean slotSwapFellBackToSilent;
   private boolean pendingPearlTeleport;
   private boolean pendingChorusTeleport;

   public Surround() {
      super("Surround", "Automatically surrounds you with obsidian", Module.Category.COMBAT, true, false, false);
      this.swapMode = (new Setting("Swap", Surround.SwapMode.Silent)).withDescription((Function<SwapMode, String>)((mode) -> {
         switch (mode) {
            case Normal:
               return "Normal: Directly changes your selected hotbar slot for placement, then restores the previous slot afterward.";
            case Silent:
               return "Silent: Temporarily switches to the required block through InventoryUtil and recovers your previous slot after placing.";
            case SlotSwap:
               return "SlotSwap: Uses inventory SWAP clicks to place with the required block without normally changing your visible selected slot.";
            default:
               return "";
         }
      }));
      this.onlyGround = new Setting<Boolean>("On Ground", true, "Only allows Surround to place blocks while you are on the ground.");
      this.supportPlace = new Setting<Boolean>("SupportBlocks", true, "Places temporary support blocks when a surround position has no usable neighboring face.");
      this.breakCrystals = new Setting<Boolean>("CrystalBreaker", true, "Attacks end crystals occupying positions that Surround needs to place in.");
      this.crystalBreakMode = (new Setting("CrystalRotate", Surround.CrystalBreakMode.Old, (v) -> (Boolean)this.breakCrystals.getValue())).withDescription((Function<CrystalBreakMode, String>)((mode) -> {
         switch (mode) {
            case Old:
               return "Old: Immediately attacks the blocking crystal before continuing the surround placement.";
            case EventRotate:
               return "EventRotate: Adds crystal rotation and attack packets into the same ordered packet sequence used for the placement.";
            default:
               return "";
         }
      }));
      this.floorPlace = new Setting<Boolean>("PlaceFloor", true, "Adds a protective surround block directly beneath your occupied positions while grounded.");
      this.render = new Setting<Boolean>("Render", true, "Renders recently placed Surround blocks.");
      this.antiCity = new Setting<Boolean>("AntiCity", false, "Detects surround blocks being mined and adds replacement or protective positions around them.");
      this.cornerClip = new Setting<Boolean>("CornerClipCheck", false, "Filters Surround positions when your player is partially clipped into a block corner.");
      this.extend = new Setting<Boolean>("Extend", true, "Adds additional surround positions when your player bounding box overlaps neighboring blocks.");
      this.render2D = new Setting<Boolean>("Render2D", true, "Displays the Surround block count indicator on the HUD.");
      this.hotbarGap = new Setting<Integer>("HotbarGap", 63, 10, 100, (v) -> (Boolean)this.render2D.getValue(), "Controls the spacing used by the Surround HUD indicator.");
      this.renderBlocks = new ArrayList();
      this.antiCityData = new AntiCityMethods.AntiCityData();
      this.timer = new Timer();
      this.placePositions = new ArrayList();
      this.packets = new ArrayList();
      this.placedThisTick = new HashSet();
      this.brokeCrystalsAt = new HashSet();
      this.lastHotbarSlot = -1;
      this.lastAntiCityPlace = 0L;
      this.stackAlt = null;
      this.lastSlot = -1;
      this.stackAltNew = null;
      this.slotSwapFellBackToSilent = false;
      this.pendingPearlTeleport = false;
      this.pendingChorusTeleport = false;
      this.register(this.render);
      this.register(this.render2D);
      this.register(this.hotbarGap);
      this.register(this.swapMode);
      this.register(this.rotate);
      this.register(this.blocksPerTick);
      this.register(this.breakCrystals);
      this.register(this.crystalBreakMode);
      this.register(this.swing);
      this.register(this.center);
      this.register(this.autoDisable);
      this.register(this.adJump);
      this.register(this.adRubberband);
      this.register(this.strictDirection);
      this.register(this.onlyGround);
      this.register(this.supportPlace);
      this.register(this.floorPlace);
      this.register(this.antiCity);
      this.register(this.extend);
      this.register(this.cornerClip);
      INSTANCE = this;
   }

   public String getDisplayInfo() {
      return String.valueOf(this.blocksPlaced);
   }

   public static Surround getInstance() {
      if (INSTANCE == null) {
         INSTANCE = new Surround();
      }

      return INSTANCE;
   }

   private void debugLog(TextFormatting color, String message) {
      if (Debug.getInstance().isEnabled() && (Boolean)Debug.getInstance().surroundDebug.getValue()) {
         Command.sendSilentMessage(color + message);
      }

   }

   @SubscribeEvent
   public void onPacketReceive(PacketEvent.Receive event) {
      if (!fullNullCheck() && this.isEnabled()) {
         if (event.getPacket() instanceof SPacketBlockBreakAnim) {
            SPacketBlockBreakAnim packet = (SPacketBlockBreakAnim)event.getPacket();
            AntiCityMethods.processBlockBreakAnim(packet, this.antiCityData);
         } else if (event.getPacket() instanceof SPacketBlockChange) {
            SPacketBlockChange packet = (SPacketBlockChange)event.getPacket();
            BlockPos pos = packet.getBlockPosition();
            IBlockState state = packet.getBlockState();
            BlockPos playerPos = PositionUtil.getPosition(mc.player);
            if (this.placedThisTick.contains(pos) && BlockUtil.canReplace(pos)) {
               this.placedThisTick.remove(pos);
            }

            if (this.isSurroundPosition(pos, playerPos) && BlockUtil.canReplace(pos)) {
            }
         } else if (event.getPacket() instanceof SPacketPlayerPosLook) {
            if (!(Boolean)this.autoDisable.getValue()) {
               return;
            }

            if (this.pendingPearlTeleport) {
               this.pendingPearlTeleport = false;
               this.disable();
               return;
            }

            if (this.pendingChorusTeleport) {
               this.pendingChorusTeleport = false;
               this.disable();
               return;
            }

            if ((Boolean)this.adRubberband.getValue()) {
               this.disable();
            }
         } else if (event.getPacket() instanceof SPacketMultiBlockChange) {
            SPacketMultiBlockChange packet = (SPacketMultiBlockChange)event.getPacket();
            BlockPos playerPos = PositionUtil.getPosition(mc.player);

            for(SPacketMultiBlockChange.BlockUpdateData data : packet.getChangedBlocks()) {
               BlockPos pos = data.getPos();
               IBlockState state = data.getBlockState();
               if (this.placedThisTick.contains(pos) && BlockUtil.canReplace(pos)) {
                  this.placedThisTick.remove(pos);
               }
            }
         }

      }
   }

   private boolean isSurroundPosition(BlockPos pos, BlockPos playerPos) {
      if (pos.getY() != playerPos.getY() && pos.getY() != playerPos.getY() - 1) {
         return false;
      } else {
         int dx = Math.abs(pos.getX() - playerPos.getX());
         int dz = Math.abs(pos.getZ() - playerPos.getZ());
         return dx <= 2 && dz <= 2 && (dx > 0 || dz > 0);
      }
   }

   @SubscribeEvent
   public void onPacketSend(PacketEvent.Send event) {
      if (!fullNullCheck() && this.isEnabled()) {
         if (event.getPacket() instanceof CPacketPlayerDigging) {
            CPacketPlayerDigging packet = (CPacketPlayerDigging)event.getPacket();
            if (packet.getAction() == Action.START_DESTROY_BLOCK) {
               AntiCityMethods.processPlayerDigging(packet, this.antiCityData);
            }
         }

         if (event.getPacket() instanceof CPacketPlayerTryUseItem && (Boolean)this.autoDisable.getValue()) {
            ItemStack held = mc.player.getHeldItemMainhand();
            if (!held.isEmpty() && held.getItem() == Items.ENDER_PEARL) {
               this.pendingPearlTeleport = true;
            }
         }

      }
   }

   @SubscribeEvent
   public void onRender3D(Render3DEvent event) {
      if (!this.renderBlocks.isEmpty()) {
         if (!(Boolean)this.render.getValue()) {
            this.renderBlocks.clear();
         } else {
            BlockRender3D.render(this.renderBlocks);
         }
      }
   }

   @SubscribeEvent
   public void onRender2D(Render2DEvent event) {
      if ((Boolean)this.render2D.getValue()) {
         Render2DMethods.drawHudPill("Surround " + InventoryUtil.getFullBlockCount(), OBSIDIAN_ICON, "x", (Integer)this.hotbarGap.getValue(), 20.0F, 1.3F, -1728053248, -1426063361, -1);
      }
   }

   private void addRenderBlock(BlockPos pos) {
      if ((Boolean)this.render.getValue()) {
         for(BlockRender3D.SimpleRenderBlock rb : this.renderBlocks) {
            if (rb.getPos().equals(pos)) {
               return;
            }
         }

         if (this.renderBlocks.size() >= 30) {
            this.renderBlocks.remove(0);
         }

         this.renderBlocks.add(new BlockRender3D.SimpleRenderBlock(pos));
      }
   }

   public void onEnable() {
      if (fullNullCheck()) {
         this.disable();
      } else {
         this.blocksPlaced = 0;
         this.lastHotbarSlot = mc.player.inventory.currentItem;
         this.enablePosY = mc.player.posY;
         this.clearState();
         if ((Boolean)this.center.getValue()) {
            this.centerPlayer();
         }

      }
   }

   public void onDisable() {
      if (PriorityManager.INSTANCE.isLockedBy("Surround")) {
         PriorityManager.INSTANCE.unlockUsageLock();
      }

      this.clearState();
      this.antiCityData.reset();
      this.renderBlocks.clear();
   }

   public void onUpdate() {
      if (fullNullCheck()) {
         this.disable();
      } else {
         this.placedThisTick.clear();
         this.brokeCrystalsAt.clear();
         if ((Boolean)this.autoDisable.getValue() && mc.player.isHandActive()) {
            ItemStack active = mc.player.getActiveItemStack();
            if (!active.isEmpty() && active.getItem() == Items.CHORUS_FRUIT) {
               this.pendingChorusTeleport = true;
            }
         }

         if (this.shouldDisable()) {
            this.disable();
         } else if (this.canOperate()) {
            if (!PriorityManager.INSTANCE.usageLock || PriorityManager.INSTANCE.usageLockCause.equals("Surround")) {
               int obbySlot = SurroundUtil.findSurroundBlock();
               if (obbySlot == -1) {
                  this.disable();
               } else {
                  this.calculatePlacePositions();
                  if (this.placePositions.isEmpty()) {
                     if (PriorityManager.INSTANCE.isLockedBy("Surround")) {
                        PriorityManager.INSTANCE.unlockUsageLock();
                     }

                  } else {
                     this.didPlace = false;
                     this.placeNormalMode(obbySlot);
                     if (this.didPlace) {
                        this.timer.reset();
                     }

                  }
               }
            }
         }
      }
   }

   private boolean shouldDisable() {
      return (Boolean)this.autoDisable.getValue() && (Boolean)this.adJump.getValue() && mc.player.posY > this.enablePosY;
   }

   private boolean canOperate() {
      return (!(Boolean)this.onlyGround.getValue() || mc.player.onGround) && this.timer.passed(0L);
   }

   private void clearState() {
      this.packets.clear();
      this.placedThisTick.clear();
      this.brokeCrystalsAt.clear();
   }

   private void centerPlayer() {
      BlockPos startPos = PositionUtil.getPosition(mc.player);
      double centerX = (double)startPos.getX() + (double)0.5F;
      double centerZ = (double)startPos.getZ() + (double)0.5F;
      mc.player.connection.sendPacket(new CPacketPlayer.Position(centerX, mc.player.posY, centerZ, mc.player.onGround));
      mc.player.setPosition(centerX, mc.player.posY, centerZ);
   }

   private void calculatePlacePositions() {
      this.placePositions.clear();
      BlockPos playerPos = PositionUtil.getPosition(mc.player);
      Set<BlockPos> blocked = this.createBlocked(playerPos);
      Set<BlockPos> surrounding = this.createSurrounding(blocked);

      for(BlockPos pos : surrounding) {
         if (this.isValidPlacePosition(pos, playerPos, true)) {
            this.placePositions.add(pos);
         }
      }

      if ((Boolean)this.extend.getValue()) {
         this.addExtendPositions(playerPos, surrounding);
      }

      if ((Boolean)this.cornerClip.getValue() && EntityUtil.isPartialPhasing(mc.player)) {
         EntityUtil.filterForCornerClip(this.placePositions);
      }

      this.addAntiCityPositions(playerPos);
   }

   private void addExtendPositions(BlockPos playerPos, Set<BlockPos> surrounding) {
      List<BlockPos> extendPositions = this.getExtendPositions(playerPos, surrounding);
      if (!extendPositions.isEmpty()) {
         for(BlockPos pos : extendPositions) {
            if (this.isValidPlacePosition(pos, playerPos, false) && !this.placePositions.contains(pos)) {
               this.placePositions.add(pos);
            }
         }
      }

   }

   private void addAntiCityPositions(BlockPos playerPos) {
      if (System.currentTimeMillis() - this.lastAntiCityPlace > 100L) {
         List<BlockPos> antiCityPos = AntiCityMethods.getAntiCityPositions(this.antiCityData, playerPos, (Boolean)this.antiCity.getValue());
         if (!antiCityPos.isEmpty()) {
            for(BlockPos pos : antiCityPos) {
               if (this.isValidPlacePosition(pos, playerPos, true) && !this.placePositions.contains(pos)) {
                  this.placePositions.add(pos);
               }
            }

            this.lastAntiCityPlace = System.currentTimeMillis();
         }

      }
   }

   private List<BlockPos> getExtendPositions(BlockPos playerPos, Set<BlockPos> surrounding) {
      List<BlockPos> extendPositions = new ArrayList();
      Set<BlockPos> blocked = this.createBlocked(playerPos);

      for(BlockPos surroundPos : surrounding) {
         if (BlockUtil.canReplace(surroundPos) && SurroundUtil.hasEntityAt(surroundPos)) {
            for(EnumFacing facing : EnumFacing.HORIZONTALS) {
               BlockPos extendPos = surroundPos.offset(facing);
               if (!blocked.contains(extendPos) && !extendPositions.contains(extendPos) && this.isValidExtendPosition(extendPos, playerPos)) {
                  extendPositions.add(extendPos);
               }
            }
         }
      }

      return extendPositions;
   }

   private boolean isValidExtendPosition(BlockPos pos, BlockPos playerPos) {
      if (BlockUtil.canReplace(pos) && SurroundUtil.isInsideWorldBorder(pos)) {
         if (pos.getY() != playerPos.getY()) {
            return false;
         } else {
            double distance = mc.player.getDistanceSq(pos);
            if (distance > (double)36.0F) {
               return false;
            } else {
               EnumFacing facing = this.getFacing(pos, false);
               return facing != null || (Boolean)this.supportPlace.getValue();
            }
         }
      } else {
         return false;
      }
   }

   private boolean isValidPlacePosition(BlockPos pos, BlockPos playerPos, boolean allowSupport) {
      Set<BlockPos> blockedPositions = this.createBlocked(playerPos);
      if (blockedPositions.contains(pos)) {
         return false;
      } else if (!BlockUtil.canReplace(pos)) {
         return false;
      } else {
         if (pos.getY() < playerPos.getY()) {
            BlockPos above = pos.up();
            if (!BlockUtil.canReplace(above)) {
               return false;
            }
         }

         if (!SurroundUtil.hasEntityBlockingPlacement(pos) && SurroundUtil.isInsideWorldBorder(pos)) {
            boolean isSurroundLevel = pos.getY() == playerPos.getY();
            List<EnumFacing> validFacings = this.getAllValidFacings(pos, false);
            if ((Boolean)this.strictDirection.getValue() && validFacings.isEmpty()) {
               if (allowSupport && (Boolean)this.supportPlace.getValue() && isSurroundLevel) {
                  BlockPos supportPos = this.findSupportPositionWithVisibleFace(pos);
                  if (supportPos != null) {
                     return true;
                  }
               }

               return false;
            } else if (!validFacings.isEmpty()) {
               return true;
            } else if (allowSupport && (Boolean)this.supportPlace.getValue() && isSurroundLevel) {
               return this.findSupportPosition(pos) != null;
            } else {
               return false;
            }
         } else {
            return false;
         }
      }
   }

   private void placeNormalMode(int obbySlot) {
      this.packets.clear();
      this.didPlace = false;
      List<BlockPos> mainBlocks = new ArrayList();
      List<BlockPos> supportBlocks = new ArrayList();
      BlockPos playerPos = PositionUtil.getPosition(mc.player);

      for(BlockPos pos : this.placePositions) {
         (pos.getY() == playerPos.getY() ? mainBlocks : supportBlocks).add(pos);
      }

      this.sortBlocksByPriority(mainBlocks, playerPos);
      this.sortBlocksByPriority(supportBlocks, playerPos);
      int blocksThisTick = 0;
      blocksThisTick += this.placeBlockList(supportBlocks, blocksThisTick, true);
      blocksThisTick += this.placeBlockList(mainBlocks, blocksThisTick, false);
      if (!this.packets.isEmpty()) {
         this.executePackets(obbySlot);
      }

   }

   private int placeBlockList(List<BlockPos> blocks, int currentCount, boolean isSupport) {
      int placed = 0;
      BlockPos playerPos = PositionUtil.getPosition(mc.player);

      for(BlockPos pos : blocks) {
         if (currentCount + placed >= (Integer)this.blocksPerTick.getValue()) {
            break;
         }

         if (!this.placedThisTick.contains(pos) && BlockUtil.canReplace(pos)) {
            List<EnumFacing> validFacings = this.getAllValidFacings(pos, true);
            if (validFacings.isEmpty()) {
               if ((Boolean)this.strictDirection.getValue() && !isSupport && pos.getY() == playerPos.getY() && (Boolean)this.supportPlace.getValue()) {
                  BlockPos supportPos = this.findSupportPositionWithVisibleFace(pos);
                  if (supportPos != null && !this.placedThisTick.contains(supportPos) && BlockUtil.canReplace(supportPos)) {
                     List<EnumFacing> supportFacings = this.getAllValidFacings(supportPos, true);
                     if (!supportFacings.isEmpty()) {
                        EnumFacing supportFacing = (EnumFacing)supportFacings.get(0);
                        if (this.placeBlockAtPos(supportPos, supportFacing)) {
                           ++placed;
                           this.didPlace = true;
                           this.placedThisTick.add(supportPos);
                           this.addRenderBlock(supportPos);
                        }
                     }
                  }
               }
            } else {
               EnumFacing facing = (EnumFacing)validFacings.get(0);
               if (this.placeBlockAtPos(pos, facing)) {
                  ++placed;
                  this.didPlace = true;
                  this.placedThisTick.add(pos);
                  this.addRenderBlock(pos);
               }
            }
         }
      }

      return placed;
   }

   private List<EnumFacing> getAllValidFacings(BlockPos pos, boolean includePlacedThisTick) {
      List<EnumFacing> validFacings = new ArrayList();

      for(EnumFacing facing : EnumFacing.VALUES) {
         BlockPos neighbor = pos.offset(facing);
         IBlockState neighborState = mc.world.getBlockState(neighbor);
         boolean solidNeighbor = neighborState.getBlock().canCollideCheck(neighborState, false) && !neighborState.getMaterial().isReplaceable() && SurroundUtil.isInsideWorldBorder(neighbor);
         boolean placedNeighbor = includePlacedThisTick && this.placedThisTick.contains(neighbor);
         if (solidNeighbor || placedNeighbor) {
            validFacings.add(facing);
         }
      }

      if (validFacings.isEmpty()) {
         return validFacings;
      } else {
         List<EnumFacing> visibleFacings = new ArrayList();
         List<EnumFacing> nonVisibleFacings = new ArrayList();

         for(EnumFacing facing : validFacings) {
            if (BlockUtil.canSeeFace(pos.offset(facing), facing.getOpposite())) {
               visibleFacings.add(facing);
            } else {
               nonVisibleFacings.add(facing);
            }
         }

         float serverYaw = Vonware.rotationManager.getServerYaw();
         float serverPitch = Vonware.rotationManager.getServerPitch();
         visibleFacings.sort((f1, f2) -> {
            BlockPos n1 = pos.offset(f1);
            BlockPos n2 = pos.offset(f2);
            Vec3i d1 = f1.getOpposite().getDirectionVec();
            Vec3i d2 = f2.getOpposite().getDirectionVec();
            Vec3d h1 = new Vec3d((double)n1.getX() + (double)0.5F + (double)d1.getX() * (double)0.5F, (double)n1.getY() + (double)0.5F + (double)d1.getY() * (double)0.5F, (double)n1.getZ() + (double)0.5F + (double)d1.getZ() * (double)0.5F);
            Vec3d h2 = new Vec3d((double)n2.getX() + (double)0.5F + (double)d2.getX() * (double)0.5F, (double)n2.getY() + (double)0.5F + (double)d2.getY() * (double)0.5F, (double)n2.getZ() + (double)0.5F + (double)d2.getZ() * (double)0.5F);
            float[] r1 = RotationUtil.getLegitRotations(h1);
            float[] r2 = RotationUtil.getLegitRotations(h2);
            float delta1 = Math.abs(MathHelper.wrapDegrees(r1[0] - serverYaw)) + Math.abs(r1[1] - serverPitch);
            float delta2 = Math.abs(MathHelper.wrapDegrees(r2[0] - serverYaw)) + Math.abs(r2[1] - serverPitch);
            return Float.compare(delta1, delta2);
         });
         if ((Boolean)this.strictDirection.getValue()) {
            return visibleFacings;
         } else if (!visibleFacings.isEmpty()) {
            return visibleFacings;
         } else {
            nonVisibleFacings.sort((f1, f2) -> {
               double d1 = mc.player.getDistanceSq((double)pos.offset(f1).getX() + (double)0.5F, (double)pos.offset(f1).getY() + (double)0.5F, (double)pos.offset(f1).getZ() + (double)0.5F);
               double d2 = mc.player.getDistanceSq((double)pos.offset(f2).getX() + (double)0.5F, (double)pos.offset(f2).getY() + (double)0.5F, (double)pos.offset(f2).getZ() + (double)0.5F);
               return Double.compare(d1, d2);
            });
            return nonVisibleFacings;
         }
      }
   }

   private EnumFacing getFacing(BlockPos pos, boolean includePlacedThisTick) {
      return BlockUtil.getPlaceableSide(pos, (Boolean)this.strictDirection.getValue());
   }

   private BlockPos findSupportPositionWithVisibleFace(BlockPos targetPos) {
      BlockPos playerPos = PositionUtil.getPosition(mc.player);
      Set<BlockPos> blockedPositions = this.createBlocked(playerPos);

      for(EnumFacing facing : EnumFacing.VALUES) {
         BlockPos supportPos = targetPos.offset(facing);
         if (supportPos.getY() <= targetPos.getY() && !blockedPositions.contains(supportPos) && BlockUtil.canReplace(supportPos) && !SurroundUtil.hasEntityAt(supportPos) && SurroundUtil.isInsideWorldBorder(supportPos)) {
            if ((Boolean)this.strictDirection.getValue()) {
               BlockPos belowSupport = supportPos.down();
               if (BlockUtil.canReplace(belowSupport) && !this.placedThisTick.contains(belowSupport)) {
                  continue;
               }
            }

            List<EnumFacing> supportFacings = this.getAllValidFacings(supportPos, false);
            if (!supportFacings.isEmpty()) {
               EnumFacing first = (EnumFacing)supportFacings.get(0);
               if (BlockUtil.canSeeFace(supportPos.offset(first), first.getOpposite())) {
                  return supportPos;
               }
            }
         }
      }

      return null;
   }

   private void sortBlocksByPriority(List<BlockPos> blocks, BlockPos playerPos) {
      blocks.sort(Comparator.comparingInt((BlockPos pos) -> Math.abs(pos.getY() - playerPos.getY())).thenComparingDouble((BlockPos pos) -> mc.player.getDistanceSq(pos)));
   }

   private Set<BlockPos> createBlocked(BlockPos playerPos) {
      Set<BlockPos> blocked = new HashSet();
      double xOffset = mc.player.posX - (double)playerPos.getX();
      double zOffset = mc.player.posZ - (double)playerPos.getZ();
      blocked.add(playerPos);
      boolean extendsEast = xOffset > 0.7;
      boolean extendsWest = xOffset < 0.3;
      boolean extendsSouth = zOffset > 0.7;
      boolean extendsNorth = zOffset < 0.3;
      if (extendsEast) {
         blocked.add(playerPos.offset(EnumFacing.EAST));
      }

      if (extendsWest) {
         blocked.add(playerPos.offset(EnumFacing.WEST));
      }

      if (extendsSouth) {
         blocked.add(playerPos.offset(EnumFacing.SOUTH));
      }

      if (extendsNorth) {
         blocked.add(playerPos.offset(EnumFacing.NORTH));
      }

      if (extendsEast && extendsSouth) {
         blocked.add(playerPos.offset(EnumFacing.EAST).offset(EnumFacing.SOUTH));
      }

      if (extendsEast && extendsNorth) {
         blocked.add(playerPos.offset(EnumFacing.EAST).offset(EnumFacing.NORTH));
      }

      if (extendsWest && extendsSouth) {
         blocked.add(playerPos.offset(EnumFacing.WEST).offset(EnumFacing.SOUTH));
      }

      if (extendsWest && extendsNorth) {
         blocked.add(playerPos.offset(EnumFacing.WEST).offset(EnumFacing.NORTH));
      }

      return blocked;
   }

   private Set<BlockPos> createSurrounding(Set<BlockPos> blocked) {
      Set<BlockPos> surrounding = new HashSet();

      for(BlockPos pos : blocked) {
         for(EnumFacing facing : EnumFacing.HORIZONTALS) {
            BlockPos offset = pos.offset(facing);
            if (!blocked.contains(offset) && !SurroundUtil.isUnbreakableBlock(offset)) {
               surrounding.add(offset);
               if (this.getFacing(offset, false) == null && (Boolean)this.supportPlace.getValue() && !this.hasUnbreakableNeighbor(offset)) {
                  BlockPos supportPos = this.findSupportPosition(offset);
                  if (supportPos != null) {
                     surrounding.add(supportPos);
                  }
               }
            }
         }

         if ((Boolean)this.floorPlace.getValue() && mc.player.onGround) {
            surrounding.add(pos.down());
         }
      }

      return surrounding;
   }

   private boolean hasUnbreakableNeighbor(BlockPos pos) {
      for(EnumFacing facing : EnumFacing.VALUES) {
         BlockPos neighbor = pos.offset(facing);
         if (!BlockUtil.canReplace(neighbor) && SurroundUtil.isUnbreakableBlock(neighbor)) {
            return true;
         }
      }

      return false;
   }

   private BlockPos findSupportPosition(BlockPos targetPos) {
      BlockPos playerPos = PositionUtil.getPosition(mc.player);
      boolean isBurrowed = !BlockUtil.canReplace(playerPos);
      EnumFacing[] var4 = EnumFacing.VALUES;
      int var5 = var4.length;
      int var6 = 0;

      BlockPos supportPos;
      while(true) {
         if (var6 >= var5) {
            return null;
         }

         EnumFacing facing = var4[var6];
         supportPos = targetPos.offset(facing);
         if (supportPos.getY() <= targetPos.getY() && BlockUtil.canReplace(supportPos) && !SurroundUtil.hasEntityAt(supportPos) && this.getFacing(supportPos, false) != null && SurroundUtil.isInsideWorldBorder(supportPos)) {
            if (!(Boolean)this.strictDirection.getValue() || !isBurrowed || targetPos.getY() != playerPos.getY()) {
               break;
            }

            BlockPos belowSupport = supportPos.down();
            if (!BlockUtil.canReplace(belowSupport)) {
               break;
            }
         }

         ++var6;
      }

      return supportPos;
   }

   private boolean placeBlockAtPos(BlockPos pos, EnumFacing facing) {
      BlockPos neighbor = pos.offset(facing);
      EnumFacing opposite = facing.getOpposite();
      Vec3d hitVec = (new Vec3d(neighbor)).add((double)0.5F, (double)0.5F, (double)0.5F).add((new Vec3d(opposite.getDirectionVec())).scale((double)0.5F));
      Vec3d eyePos = new Vec3d(mc.player.posX, mc.player.posY + (double)mc.player.getEyeHeight(), mc.player.posZ);
      if (eyePos.distanceTo(hitVec) > (double)6.0F) {
         return false;
      } else {
         if ((Boolean)this.breakCrystals.getValue() && !this.brokeCrystalsAt.contains(pos)) {
            if (this.crystalBreakMode.getValue() == Surround.CrystalBreakMode.Old) {
               if (this.breakCrystalsAt(pos)) {
                  this.brokeCrystalsAt.add(pos);
               }
            } else {
               for(Entity entity : mc.world.getEntitiesWithinAABB(EntityEnderCrystal.class, new AxisAlignedBB(pos))) {
                  if (!entity.isDead) {
                     Vec3d crystalVec = new Vec3d(entity.posX, entity.posY + (double)entity.height / (double)2.0F, entity.posZ);
                     if (!DamageUtil.canBreakWeakness(false)) {
                        int antiWeaknessSlot = DamageUtil.findAntiWeakness();
                        if (antiWeaknessSlot == -1) {
                           break;
                        }

                        int oldSlot = mc.player.inventory.currentItem;
                        InventoryUtil.switchTo(antiWeaknessSlot);
                        if ((Boolean)this.rotate.getValue()) {
                           mc.player.connection.sendPacket(RotationUtil.getRotationPacket(RotationUtil.getLegitRotationsServer(crystalVec)));
                        }

                        mc.player.connection.sendPacket(new CPacketUseEntity(entity));
                        if ((Boolean)this.swing.getValue()) {
                           mc.player.swingArm(EnumHand.MAIN_HAND);
                        }

                        InventoryUtil.recoverSlot(oldSlot);
                     } else {
                        if ((Boolean)this.rotate.getValue()) {
                           this.packets.add(RotationUtil.getRotationPacket(RotationUtil.getLegitRotationsServer(crystalVec)));
                        }

                        this.packets.add(new CPacketUseEntity(entity));
                        this.packets.add(new CPacketAnimation(EnumHand.MAIN_HAND));
                     }

                     this.brokeCrystalsAt.add(pos);
                     break;
                  }
               }
            }
         }

         float[] rots = RotationUtil.getLegitRotationsServer(hitVec);
         float[] hitRots = new float[]{(float)(hitVec.x - (double)neighbor.getX()), (float)(hitVec.y - (double)neighbor.getY()), (float)(hitVec.z - (double)neighbor.getZ())};
         if ((Boolean)this.rotate.getValue()) {
            this.packets.add(RotationUtil.getRotationPacket(rots));
         }

         boolean needsSneak = SurroundUtil.needsSneak(neighbor);
         if (!needsSneak && this.placedThisTick.contains(neighbor)) {
            int obbySlot = SurroundUtil.findSurroundBlock();
            if (obbySlot != -1) {
               ItemStack stack = mc.player.inventory.getStackInSlot(obbySlot);
               if (!stack.isEmpty() && stack.getItem() instanceof ItemBlock) {
                  ItemBlock itemBlock = (ItemBlock)stack.getItem();
                  Block block = itemBlock.getBlock();
                  if (block instanceof BlockEnderChest || block instanceof BlockChest || block instanceof BlockWorkbench || block instanceof BlockFurnace || block instanceof BlockEnchantmentTable || block instanceof BlockBrewingStand || block instanceof BlockAnvil || block instanceof BlockHopper || block instanceof BlockDispenser || block instanceof BlockDropper) {
                     needsSneak = true;
                  }
               }
            }
         }

         if (needsSneak) {
            this.packets.add(new CPacketEntityAction(mc.player, net.minecraft.network.play.client.CPacketEntityAction.Action.START_SNEAKING));
         }

         this.packets.add(new CPacketPlayerTryUseItemOnBlock(neighbor, opposite, EnumHand.MAIN_HAND, hitRots[0], hitRots[1], hitRots[2]));
         if (needsSneak) {
            this.packets.add(new CPacketEntityAction(mc.player, net.minecraft.network.play.client.CPacketEntityAction.Action.STOP_SNEAKING));
         }

         this.packets.add(new CPacketAnimation(EnumHand.MAIN_HAND));
         ++this.blocksPlaced;
         return true;
      }
   }

   private void executePackets(int slot) {
      PriorityManager.INSTANCE.lockUsageLock("Surround");
      int oldSlot = mc.player.inventory.currentItem;
      if (this.swapMode.getValue() == Surround.SwapMode.Silent) {
         InventoryUtil.switchTo(slot);
      } else if (this.swapMode.getValue() == Surround.SwapMode.SlotSwap) {
         this.lastSlot = mc.player.inventory.currentItem;
         this.stackAlt = mc.player.getHeldItemMainhand();
         this.slotSwapFellBackToSilent = false;
         ItemStack held = mc.player.getHeldItemMainhand();
         boolean holdingCrystal = !held.isEmpty() && held.getItem() == Items.END_CRYSTAL;
         int convertedSlot = this.convertSlot(slot);
         if (!holdingCrystal && mc.player.inventory.currentItem != convertedSlot && convertedSlot > 35 && convertedSlot < 45) {
            mc.playerController.windowClick(0, convertedSlot, mc.player.inventory.currentItem, ClickType.SWAP, mc.player);
            this.stackAltNew = mc.player.getHeldItemMainhand();
         } else {
            InventoryUtil.switchTo(slot);
            this.slotSwapFellBackToSilent = true;
         }
      } else {
         mc.player.inventory.currentItem = slot;
      }

      for(Packet<?> p : this.packets) {
         mc.player.connection.sendPacket(p);
      }

      if ((Boolean)this.swing.getValue()) {
         mc.player.swingArm(EnumHand.MAIN_HAND);
      }

      if (this.swapMode.getValue() == Surround.SwapMode.Silent) {
         InventoryUtil.recoverSlot(oldSlot);
      } else if (this.swapMode.getValue() == Surround.SwapMode.SlotSwap) {
         if (this.slotSwapFellBackToSilent) {
            InventoryUtil.recoverSlot(this.lastSlot);
         } else {
            this.switchBackSlotSwap(slot);
         }
      } else {
         mc.player.inventory.currentItem = oldSlot;
      }

      this.packets.clear();
   }

   private void switchBackSlotSwap(int slot) {
      short id = mc.player.openContainer.getNextTransactionID(mc.player.inventory);
      ItemStack fakeStack = new ItemStack(Items.END_CRYSTAL, 64);
      int newSlot = this.convertSlot(slot);
      int altSlot = this.convertSlot(this.lastSlot);
      Slot currentSlot = (Slot)mc.player.inventoryContainer.inventorySlots.get(altSlot);
      Slot swapSlot = (Slot)mc.player.inventoryContainer.inventorySlots.get(newSlot);
      mc.player.connection.sendPacket(new CPacketClickWindow(0, newSlot, mc.player.inventory.currentItem, ClickType.SWAP, fakeStack, id));
      currentSlot.putStack(this.stackAlt);
      swapSlot.putStack(this.stackAltNew);
   }

   private int convertSlot(int slot) {
      if (slot == -2) {
         return 45;
      } else {
         return slot > -1 && slot < 9 ? 36 + slot : slot;
      }
   }

   private boolean breakCrystalsAt(BlockPos pos) {
      for(Entity entity : mc.world.getEntitiesWithinAABB(EntityEnderCrystal.class, new AxisAlignedBB(pos))) {
         if (!entity.isDead) {
            if (!DamageUtil.canBreakWeakness(false)) {
               int antiWeaknessSlot = DamageUtil.findAntiWeakness();
               if (antiWeaknessSlot == -1) {
                  return false;
               }

               int oldSlot = mc.player.inventory.currentItem;
               InventoryUtil.switchTo(antiWeaknessSlot);
               mc.player.connection.sendPacket(new CPacketUseEntity(entity));
               if ((Boolean)this.swing.getValue()) {
                  mc.player.swingArm(EnumHand.MAIN_HAND);
               }

               InventoryUtil.recoverSlot(oldSlot);
            } else {
               mc.player.connection.sendPacket(new CPacketUseEntity(entity));
               if ((Boolean)this.swing.getValue()) {
                  mc.player.swingArm(EnumHand.MAIN_HAND);
               }
            }

            return true;
         }
      }

      return false;
   }

   static {
      OBSIDIAN_ICON = new ItemStack(Blocks.OBSIDIAN);
   }

   public static enum CrystalBreakMode {
      Old,
      EventRotate;
   }

   public static enum SwapMode {
      Normal,
      Silent,
      SlotSwap;
   }
}
