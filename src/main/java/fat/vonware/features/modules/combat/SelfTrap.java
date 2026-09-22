package fat.vonware.features.modules.combat;

import fat.vonware.event.events.PacketEvent;
import fat.vonware.event.events.Render2DEvent;
import fat.vonware.event.events.Render3DEvent;
import fat.vonware.features.modules.Module;
import fat.vonware.features.modules.client.BlockRender3D;
import fat.vonware.features.setting.Setting;
import fat.vonware.util.BlockUtil;
import fat.vonware.util.DamageUtil;
import fat.vonware.util.EntityUtil;
import fat.vonware.util.InventoryUtil;
import fat.vonware.util.MathUtil;
import fat.vonware.util.MovementUtil;
import fat.vonware.util.PositionUtil;
import fat.vonware.util.RotationUtil;
import fat.vonware.util.Timer;
import fat.vonware.util.Animations.render.Render2DMethods;
import fat.vonware.util.AutoTrap.BlockStateHelper;
import fat.vonware.util.AutoTrap.Raytracer;
import fat.vonware.util.AutoTrap.Trap;
import fat.vonware.util.BlockPlaceModules.BurrowUtil;
import fat.vonware.util.HoleFill.other.RayTraceUtil;
import fat.vonware.util.Phase.PacketUtil;
import fat.vonware.util.holeesp.HoleUtil;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.block.BlockDeadBush;
import net.minecraft.block.BlockFire;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.BlockSnow;
import net.minecraft.block.BlockTallGrass;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.gui.GuiGameOver;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityEnderCrystal;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.CPacketAnimation;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock;
import net.minecraft.network.play.client.CPacketUseEntity;
import net.minecraft.network.play.server.SPacketPlayerPosLook;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;

public class SelfTrap extends Module {
   protected final BlockStateHelper HELPER;
   protected final Setting<Timing> timings;
   protected final Setting<Boolean> rotation;
   protected final Setting<Boolean> swing;
   protected final Setting<Integer> delay;
   protected final Setting<Integer> blocks;
   protected final Setting<Float> placeRange;
   protected final Setting<Boolean> strict;
   protected final Setting<Boolean> attack;
   protected final Setting<Boolean> jumpDisable;
   protected final Setting<Boolean> render;
   protected final Setting<Boolean> extension;
   protected final Setting<Boolean> extraBlock;
   protected final Setting<Boolean> trapHead;
   protected final Setting<Boolean> antiCev;
   protected final Setting<Boolean> render2D;
   protected final Setting<Integer> hotbarGap;
   protected List<BlockPos> placeList;
   private final List<Packet<?>> packets;
   private final List<BlockRender3D.SimpleRenderBlock> renderBlocks;
   private final Map<BlockPos, Long> placed;
   private final Timer timer;
   private final Timer attackTimer;
   private final Timer burstTimer;
   private final Timer jumpTimer;
   public double enablePosY;
   private int blocksPlaced;
   private int burstBlocks;
   private int slot;
   private static final ItemStack OBSIDIAN_ICON;
   private static volatile boolean placingBlocks;

   public SelfTrap() {
      super("SelfTrap", "Traps yourself in obsidian", Module.Category.COMBAT, true, false, false);
      this.timings = (new Setting("Timing", SelfTrap.Timing.Fast)).withDescription((Function<Timing, String>)((mode) -> {
         switch (mode) {
            case Fast:
               return "Fast: Uses faster placement timing with a shorter placed-position cooldown.";
            case Strict:
               return "Strict: Uses safer placement timing, tighter BlocksPerTick burst limits, and restricts placement while airborne.";
            default:
               return "";
         }
      }));
      this.rotation = new Setting<Boolean>("Rotation", true, "Sends rotation packets toward SelfTrap placement positions.");
      this.swing = new Setting<Boolean>("Swing", true, "Swings your hand when SelfTrap attacks crystals or places blocks.");
      this.delay = new Setting<Integer>("Delay", 1, 0, 5, "Delay in ticks between SelfTrap placement cycles.");
      this.blocks = new Setting<Integer>("BlocksPerTick", 1, 1, 10, "Maximum number of SelfTrap blocks placed during each placement cycle.");
      this.placeRange = new Setting<Float>("Place Range", 4.5F, 1.0F, 6.0F, "Maximum distance at which SelfTrap can place a block.");
      this.strict = new Setting<Boolean>("Strict Direction", false, "Requires a valid strict-direction face when choosing where to place SelfTrap blocks.");
      this.attack = new Setting<Boolean>("Attack", true, "Attacks end crystals blocking required SelfTrap positions.");
      this.jumpDisable = new Setting<Boolean>("Jump Disable", true, "Automatically disables SelfTrap when your Y position rises above the position where it was enabled.");
      this.render = new Setting<Boolean>("Render", true, "Renders positions where SelfTrap places blocks.");
      this.extension = new Setting<Boolean>("Extension", true, "Expands the trap around additional block positions occupied by your player.");
      this.extraBlock = new Setting<Boolean>("ExtraBlock", false, "Adds extra anti-step style blocks above the normal SelfTrap structure.");
      this.trapHead = new Setting<Boolean>("Trap Head", false, (v) -> (Boolean)this.extraBlock.getValue(), "Adds the top/head portion of the SelfTrap structure.");
      this.antiCev = new Setting<Boolean>("AntiCev", false, (v) -> (Boolean)this.extraBlock.getValue() && (Boolean)this.trapHead.getValue(), "Adds extra anti-scaffold positions intended to make head-level CEV attacks harder.");
      this.render2D = new Setting<Boolean>("Render2D", true, "Displays the SelfTrap obsidian count indicator on the HUD.");
      this.hotbarGap = new Setting<Integer>("HotbarGap", 63, 10, 100, (v) -> (Boolean)this.render2D.getValue(), "Controls the spacing used by the SelfTrap HUD indicator.");
      this.register(this.timings);
      this.register(this.rotation);
      this.register(this.swing);
      this.register(this.delay);
      this.register(this.blocks);
      this.register(this.placeRange);
      this.register(this.strict);
      this.register(this.attack);
      this.register(this.jumpDisable);
      this.register(this.render);
      this.register(this.render2D);
      this.register(this.hotbarGap);
      this.register(this.extension);
      this.register(this.extraBlock);
      this.register(this.antiCev);
      this.register(this.trapHead);
      this.HELPER = new BlockStateHelper();
      this.packets = new ArrayList();
      this.renderBlocks = new ArrayList();
      this.placed = new HashMap();
      this.timer = new Timer();
      this.attackTimer = new Timer();
      this.burstTimer = new Timer();
      this.jumpTimer = new Timer();
      this.blocksPlaced = 0;
      this.burstBlocks = 0;
      this.slot = -1;
   }

   public static boolean shouldPauseAura() {
      return placingBlocks;
   }

   @SubscribeEvent
   public void onClientTick(TickEvent.ClientTickEvent event) {
      if (mc.player != null && mc.world != null) {
         if (event.phase == Phase.START) {
            if (this.placeList != null) {
               this.placeList.clear();
            }

            this.getSelfPositions();
            if (this.handleJump()) {
               return;
            }

            this.blocksPlaced = 0;
            if (this.update()) {
               this.placeBlocks(this.placeList);
            }

            this.execute();
            placingBlocks = this.blocksPlaced > 0;
         } else {
            this.HELPER.clearAllStates();
         }

      }
   }

   public void onEnable() {
      this.packets.clear();
      this.renderBlocks.clear();
      this.blocksPlaced = 0;
      placingBlocks = false;
      if (fullNullCheck()) {
         this.disable();
      } else {
         this.enablePosY = mc.player.posY;
      }
   }

   public void onDisable() {
      placingBlocks = false;
   }

   private boolean update() {
      if (this.updatePlaced()) {
         return false;
      } else if (!this.timer.passed((long)((Integer)this.delay.getValue() * 50))) {
         return false;
      } else {
         this.setEnablePosY();
         this.slot = this.getSlot();
         return true;
      }
   }

   private boolean updatePlaced() {
      int delay = this.timings.getValue() == SelfTrap.Timing.Strict ? 150 : 50;
      this.placed.entrySet().removeIf((entry) -> System.currentTimeMillis() - (Long)entry.getValue() >= (long)delay);
      return false;
   }

   private void placeBlocks(List<BlockPos> blockList) {
      if (blockList != null && !blockList.isEmpty()) {
         for(BlockPos pos : blockList) {
            if (!this.placed.containsKey(pos) && this.HELPER.getBlockState(pos).getMaterial().isReplaceable()) {
               this.placeBlock(pos);
            }
         }

      }
   }

   public void placeBlock(BlockPos pos) {
      if (this.slot != -1) {
         Vec3d playerVec = PositionUtil.getEyesPos();
         if (!(pos.distanceSq(playerVec.x, playerVec.y, playerVec.z) > MathUtil.square((double)this.getPlacementRange()))) {
            EnumFacing facing = this.getFacing(pos, this.HELPER);
            if (facing != null) {
               if (!this.fastCrystal(pos)) {
                  if (this.burstTimer.passed(500L)) {
                     this.burstBlocks = 0;
                  }

                  int maxBlocks = (Integer)this.blocks.getValue();
                  if (this.burstBlocks >= (Integer)this.blocks.getValue() && this.timings.getValue() == SelfTrap.Timing.Strict) {
                     maxBlocks = 1;
                  }

                  if (!mc.player.onGround && this.timings.getValue() == SelfTrap.Timing.Strict) {
                     this.burstBlocks = (Integer)this.blocks.getValue();
                     this.burstTimer.reset();
                     maxBlocks = 1;
                  }

                  if (this.blocksPlaced < maxBlocks) {
                     if (this.canPlaceBlock(pos)) {
                        this.placeBlock(pos.offset(facing), facing.getOpposite());
                        this.setState(pos);
                        this.renderPos(pos);
                        this.addCallback(pos);
                     }

                  }
               }
            }
         }
      }
   }

   private void placeBlock(BlockPos pos, EnumFacing facing) {
      float[] rots = RotationUtil.getRotations(pos, facing);
      RayTraceResult result = RayTraceUtil.getRayTraceResult(rots[0], rots[1]);
      this.placeBlock(pos, facing, rots, result.hitVec);
   }

   private void placeBlock(BlockPos on, EnumFacing facing, float[] helpingRotations, Vec3d hitVec) {
      if ((Boolean)this.rotation.getValue()) {
         this.packets.add(this.getRotationPacket(helpingRotations));
      }

      float[] hitRots = BurrowUtil.hitVecToPlaceVec(on, hitVec);
      this.packets.add(new CPacketPlayerTryUseItemOnBlock(on, facing, EnumHand.MAIN_HAND, hitRots[0], hitRots[1], hitRots[2]));
      if ((Boolean)this.swing.getValue()) {
         this.packets.add(new CPacketAnimation(EnumHand.MAIN_HAND));
      }

      ++this.blocksPlaced;
      ++this.burstBlocks;
      if (this.burstBlocks != (Integer)this.blocks.getValue()) {
         this.burstTimer.reset();
      }

   }

   private void execute() {
      if (!this.packets.isEmpty()) {
         int lastSlot = mc.player.inventory.currentItem;
         InventoryUtil.switchTo(this.slot);
         float[] oldRots = new float[]{mc.player.rotationYaw, mc.player.rotationPitch};
         boolean didRotate = false;
         PacketUtil.sneak(true);
         if ((Boolean)this.swing.getValue()) {
            mc.player.swingArm(EnumHand.MAIN_HAND);
         }

         List<Packet<?>> packetsToRemove = new ArrayList();

         for(Packet<?> packet : this.packets) {
            if (packet instanceof CPacketPlayer) {
               didRotate = true;
            }

            PacketUtil.send(packet);
            packetsToRemove.add(packet);
         }

         this.packets.removeAll(packetsToRemove);
         this.timer.reset();
         PacketUtil.sneak(false);
         InventoryUtil.switchTo(lastSlot);
         if (didRotate) {
            PacketUtil.rotate(oldRots);
         }
      }

   }

   private EnumFacing getFacing(BlockPos pos, IBlockAccess provider) {
      for(EnumFacing direction : EnumFacing.VALUES) {
         BlockPos offset = pos.offset(direction);
         if ((!(Boolean)this.strict.getValue() || Raytracer.strictDirectionCheck(offset, direction.getOpposite(), this.HELPER)) && this.isValidPlacePos(offset, provider)) {
            return direction;
         }
      }

      return null;
   }

   private boolean isValidPlacePos(BlockPos pos, IBlockAccess provider) {
      IBlockState offsetState = provider.getBlockState(pos);
      return offsetState.getBlock().canCollideCheck(offsetState, false) || !offsetState.getMaterial().isReplaceable();
   }

   private boolean fastCrystal(BlockPos pos) {
      return this.crystalCheck(pos);
   }

   private boolean crystalCheck(BlockPos pos) {
      CPacketUseEntity attackPacket = null;
      EntityEnderCrystal crystal = null;
      float currentDmg = Float.MAX_VALUE;

      for(Entity entity : mc.world.getEntitiesWithinAABB(Entity.class, new AxisAlignedBB(pos))) {
         if (entity != null && EntityUtil.isDead(entity) && entity instanceof EntityEnderCrystal) {
            float damage = DamageUtil.calculate(entity, mc.player);
            if (!(damage >= currentDmg)) {
               currentDmg = damage;
               attackPacket = PacketUtil.attackPacket(entity);
               crystal = (EntityEnderCrystal)entity;
            }
         }
      }

      if (attackPacket == null) {
         return false;
      } else if (mc.player.getDistanceSq(crystal) > MathUtil.square((double)this.getPlacementRange())) {
         return true;
      } else {
         int weaknessSlot = -1;
         int oldSlot = mc.player.inventory.currentItem;
         if (!DamageUtil.canBreakWeakness(true) && (weaknessSlot = DamageUtil.findAntiWeakness()) == -1) {
            return true;
         } else if (!(Boolean)this.attack.getValue()) {
            return true;
         } else if (!this.attackTimer.passed(150L)) {
            return false;
         } else if (weaknessSlot != -1) {
            InventoryUtil.switchTo(weaknessSlot);
            PacketUtil.send(attackPacket);
            if ((Boolean)this.swing.getValue()) {
               mc.player.swingArm(EnumHand.MAIN_HAND);
            }

            InventoryUtil.switchTo(oldSlot);
            return false;
         } else {
            this.packets.add(attackPacket);
            if ((Boolean)this.swing.getValue()) {
               this.packets.add(new CPacketAnimation(EnumHand.MAIN_HAND));
            }

            this.attackTimer.reset();
            return false;
         }
      }
   }

   public void addCallback(BlockPos pos) {
      this.placed.put(pos, System.currentTimeMillis());
   }

   private boolean canPlaceBlock(BlockPos pos) {
      Block block = this.HELPER.getBlockState(pos).getBlock();
      if (!(block instanceof BlockAir) && !(block instanceof BlockLiquid) && !(block instanceof BlockTallGrass) && !(block instanceof BlockFire) && !(block instanceof BlockDeadBush) && !(block instanceof BlockSnow)) {
         return false;
      } else {
         for(Entity entity : mc.world.getEntitiesWithinAABBExcludingEntity((Entity)null, new AxisAlignedBB(pos))) {
            if (entity != null && !EntityUtil.isDead(entity) && !(entity instanceof EntityItem) && !(entity instanceof EntityXPOrb) && !(entity instanceof EntityArrow) && !(entity instanceof EntityEnderCrystal)) {
               return false;
            }
         }

         for(EnumFacing side : this.getFacings(pos)) {
            IBlockState state = this.HELPER.getBlockState(pos.offset(side));
            if (state.getBlock().canCollideCheck(state, false)) {
               return true;
            }
         }

         return false;
      }
   }

   private List<EnumFacing> getFacings(BlockPos pos) {
      ArrayList<EnumFacing> validFacings = new ArrayList();

      for(EnumFacing side : EnumFacing.values()) {
         BlockPos neighbour = pos.offset(side);
         IBlockState state = this.HELPER.getBlockState(neighbour);
         if (state.getBlock().canCollideCheck(state, false) && !state.getMaterial().isReplaceable()) {
            validFacings.add(side);
         }
      }

      return validFacings;
   }

   public int getSlot() {
      return InventoryUtil.getBlockFromHotbar(Blocks.OBSIDIAN);
   }

   public void setState(BlockPos pos) {
      Block block = this.slot > 0 && this.slot <= 8 ? null : Blocks.ENDER_CHEST;
      if (block == null) {
         Item item = this.slot == -2 ? mc.player.getHeldItemOffhand().getItem() : mc.player.inventory.getStackInSlot(this.slot).getItem();
         if (item instanceof ItemBlock) {
            block = ((ItemBlock)item).getBlock();
         }
      }

      if (block != null) {
         this.HELPER.addBlockState(pos, block.getDefaultState());
      }

   }

   private void renderPos(BlockPos pos) {
      if (mc.world.getBlockState(pos).getBlock() instanceof BlockAir) {
         for(BlockRender3D.SimpleRenderBlock rb : this.renderBlocks) {
            if (rb.getPos().equals(pos)) {
               return;
            }
         }

         this.renderBlocks.add(new BlockRender3D.SimpleRenderBlock(pos));
      }
   }

   private void setEnablePosY() {
      if (this.jumpTimer.passed(250L)) {
         this.enablePosY = mc.player.posY;
         this.jumpTimer.reset();
      }

   }

   private boolean handleJump() {
      if ((Boolean)this.jumpDisable.getValue() && mc.player.posY > this.enablePosY) {
         this.disable();
         return true;
      } else {
         return false;
      }
   }

   public float getPlacementRange() {
      return (Float)this.placeRange.getValue();
   }

   private CPacketPlayer getRotationPacket(float[] rots) {
      boolean onGround = mc.player.onGround;
      return (CPacketPlayer)(MovementUtil.hasMotion(mc.player) ? new CPacketPlayer.PositionRotation(mc.player.posX, mc.player.posY, mc.player.posZ, rots[0], rots[1], onGround) : new CPacketPlayer.Rotation(rots[0], rots[1], onGround));
   }

   @SubscribeEvent
   public void onRender3D(Render3DEvent event) {
      if ((Boolean)this.render.getValue()) {
         if (!this.renderBlocks.isEmpty()) {
            BlockRender3D.render(this.renderBlocks);
         }
      }
   }

   @SubscribeEvent
   public void onRender2D(Render2DEvent event) {
      if ((Boolean)this.render2D.getValue()) {
         Render2DMethods.drawHudPill("SelfTrap " + InventoryUtil.getFullBlockCount(), OBSIDIAN_ICON, "x", (Integer)this.hotbarGap.getValue(), 20.0F, 1.3F, -1728053248, -1426063361, -1);
      }
   }

   @SubscribeEvent
   public void onPacketReceive(PacketEvent.Receive event) {
      if (event.getPacket() instanceof SPacketPlayerPosLook) {
         SPacketPlayerPosLook packet = (SPacketPlayerPosLook)event.getPacket();
         this.enablePosY = packet.getY();
         this.jumpTimer.reset();
      }

   }

   @SubscribeEvent
   public void onGuiOpen(GuiOpenEvent event) {
      if (event.getGui() instanceof GuiGameOver) {
         this.disable();
      }

   }

   @SubscribeEvent
   public void onWorldUnload(WorldEvent.Unload event) {
      if (event.getWorld().isRemote) {
         this.disable();
      }

   }

   protected void getSelfPositions() {
      BlockPos playerPos = new BlockPos(mc.player);
      List<BlockPos> blocked = new ArrayList();
      if (!HoleUtil.isHole(playerPos) && (Boolean)this.extension.getValue()) {
         List<BlockPos> unfiltered = (new ArrayList<BlockPos>(PositionUtil.getBlockedPositions((Entity)mc.player))).stream().sorted(Comparator.comparingDouble(BlockUtil::getDistanceSq)).collect(Collectors.toList());
         List<BlockPos> filtered = (new ArrayList<BlockPos>(unfiltered)).stream().filter((posx) -> mc.world.getBlockState(posx).getMaterial().isReplaceable() && mc.world.getBlockState(posx.up()).getMaterial().isReplaceable()).collect(Collectors.toList());
         if ((Boolean)this.extension.getValue() && filtered.size() == 2 && unfiltered.size() == 4 && ((BlockPos)unfiltered.get(0)).equals(filtered.get(0)) && ((BlockPos)unfiltered.get(3)).equals(filtered.get(1))) {
            filtered.clear();
         }

         if ((Boolean)this.extension.getValue() && filtered.size() == 3) {
            while(filtered.size() > 2) {
               filtered.remove(filtered.size() - 1);
            }
         }

         for(BlockPos pos : filtered) {
            blocked.add(pos.up());
         }
      } else {
         blocked.add(playerPos.up());
      }

      if (blocked.isEmpty()) {
         blocked.add(playerPos.up());
      }

      List<BlockPos> positions = this.positionsFromBlocked(blocked);
      positions.sort(Comparator.comparingDouble((posx) -> -BlockUtil.getDistanceSq(posx)));
      positions.sort(Comparator.comparingInt(Vec3i::getY));
      this.placeList = (List)positions.stream().filter((posx) -> BlockUtil.getDistanceSq(posx) <= MathUtil.square((double)(Float)this.placeRange.getValue())).collect(Collectors.toList());
   }

   private List<BlockPos> positionsFromBlocked(List<BlockPos> blockedIn) {
      List<BlockPos> positions = new ArrayList();
      List<BlockPos> feetAdded = new ArrayList();
      blockedIn.forEach((pos) -> feetAdded.addAll(this.applyOffsets(pos, Trap.LEGS, feetAdded)));
      positions.addAll(feetAdded);
      List<BlockPos> alreadyAdded2 = new ArrayList();
      blockedIn.forEach((pos) -> alreadyAdded2.addAll(this.applyOffsets(pos, Trap.CHEST, alreadyAdded2)));
      positions.addAll(alreadyAdded2);
      if ((Boolean)this.trapHead.getValue()) {
         List<BlockPos> alreadyAdded = new ArrayList();
         blockedIn.forEach((pos) -> alreadyAdded.addAll(this.applyOffsets(pos, Trap.TOP, alreadyAdded)));
         positions.addAll(alreadyAdded);
      }

      if (blockedIn.size() == 1) {
         if ((Boolean)this.antiCev.getValue()) {
            List<BlockPos> alreadyAdded3 = new ArrayList();
            blockedIn.forEach((pos) -> alreadyAdded3.addAll(this.applyOffsets(pos, Trap.NO_SCAFFOLD, alreadyAdded3)));
            positions.addAll(alreadyAdded3);
         }

         if ((Boolean)this.extraBlock.getValue()) {
            List<BlockPos> alreadyAdded4 = new ArrayList();
            blockedIn.forEach((pos) -> alreadyAdded4.addAll(this.applyOffsets(pos, Trap.NO_STEP, alreadyAdded4)));
            positions.addAll(alreadyAdded4);
         }
      }

      return positions;
   }

   private List<BlockPos> applyOffsets(BlockPos pos, Vec3i[] offsets, List<BlockPos> alreadyAdded) {
      ArrayList<BlockPos> positions = new ArrayList();

      for(Vec3i vec3i : offsets) {
         BlockPos offset = pos.add(vec3i);
         if (!alreadyAdded.contains(offset)) {
            positions.add(offset);
         }
      }

      return positions;
   }

   static {
      OBSIDIAN_ICON = new ItemStack(Blocks.OBSIDIAN);
      placingBlocks = false;
   }

   public static enum Timing {
      Fast,
      Strict;
   }
}
