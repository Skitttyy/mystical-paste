package fat.vonware.features.modules.combat;

import fat.vonware.Vonware;
import fat.vonware.event.events.PacketEvent;
import fat.vonware.event.events.Render2DEvent;
import fat.vonware.event.events.Render3DEvent;
import fat.vonware.features.modules.Module;
import fat.vonware.features.modules.client.BlockRender3D;
import fat.vonware.features.modules.player.FastBreak;
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
import fat.vonware.util.AutoTrap.FacingUtil;
import fat.vonware.util.AutoTrap.Trap;
import fat.vonware.util.BlockPlaceModules.BurrowUtil;
import fat.vonware.util.ESP.RenderPosition;
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
import net.minecraft.entity.player.EntityPlayer;
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

public class AutoTrap extends Module {
   protected final BlockStateHelper HELPER;
   protected final Setting<Timing> timings;
   protected final Setting<Boolean> rotation;
   protected final Setting<Boolean> swing;
   protected final Setting<Integer> delay;
   protected final Setting<Integer> blocks;
   protected final Setting<Float> placeRange;
   protected final Setting<Boolean> strict;
   protected final Setting<Boolean> attack;
   protected final Setting<Boolean> syncFastbreakCev;
   protected final Setting<Boolean> jumpDisable;
   protected final Setting<Boolean> render;
   protected final Setting<Float> targetRange;
   protected final Setting<Boolean> extension;
   protected final Setting<Boolean> noStep;
   protected final Setting<Boolean> noScaffold;
   protected final Setting<Boolean> render2D;
   protected final Setting<Integer> hotbarGap;
   protected final Map<EntityPlayer, List<BlockPos>> cached;
   protected final Map<EntityPlayer, Double> speeds;
   protected List<BlockPos> placeList;
   public EntityPlayer target;
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

   public AutoTrap() {
      super("AutoTrap", "Automatically traps enemies", Module.Category.COMBAT, true, false, false);
      this.timings = (new Setting("Timing", AutoTrap.Timing.Fast)).withDescription((Function<Timing, String>)((mode) -> {
         switch (mode) {
            case Fast:
               return "Fast: Uses faster placement timing with a shorter placed-block cooldown.";
            case Strict:
               return "Strict: Uses safer placement timing, enforces BlocksPerTick, and pauses placement while you are off-ground.";
            default:
               return "";
         }
      }));
      this.rotation = new Setting<Boolean>("Rotation", true, "Rotates toward blocks before placing them.");
      this.swing = new Setting<Boolean>("Swing", true, "Sends a hand swing when a trap block is placed.");
      this.delay = new Setting<Integer>("Delay", 5, 0, 10, "Delay between AutoTrap placement cycles.");
      this.blocks = new Setting<Integer>("BlocksPerTick", 1, 1, 10, "Maximum number of trap blocks placed per tick.");
      this.placeRange = new Setting<Float>("Place Range", 4.5F, 1.0F, 6.0F, "Maximum distance at which AutoTrap can place blocks.");
      this.strict = new Setting<Boolean>("Strict Direction", false, "Requires a strict valid placement side for trap blocks.");
      this.attack = new Setting<Boolean>("Attack", true, "Attacks crystals that block a required trap position.");
      this.syncFastbreakCev = new Setting<Boolean>("SyncFastbreakCev", true, "Synchronizes CEV-style crystal breaking with FastBreak before placing the trap block.");
      this.jumpDisable = new Setting<Boolean>("Jump Disable", false, "Automatically disables AutoTrap if you jump.");
      this.render = new Setting<Boolean>("Render", true, "Renders positions that AutoTrap is placing.");
      this.targetRange = new Setting<Float>("Target Range", 7.0F, 1.0F, 12.0F, "Maximum distance at which a player can be selected as the trap target.");
      this.extension = new Setting<Boolean>("Extension", true, "Extends the trap around additional target positions when needed.");
      this.noStep = new Setting<Boolean>("AntiStep", false, "Adds placements intended to prevent the target from stepping out of the trap.");
      this.noScaffold = new Setting<Boolean>("AntiScaffold", false, "Adds placements intended to prevent the target from scaffolding out of the trap.");
      this.render2D = new Setting<Boolean>("Render2D", true, "Displays the AutoTrap item and slot indicator on the HUD.");
      this.hotbarGap = new Setting<Integer>("HotbarGap", 63, 10, 100, (v) -> (Boolean)this.render2D.getValue(), "Controls the spacing and offset used by the AutoTrap hotbar HUD render.");
      this.register(this.timings);
      this.register(this.rotation);
      this.register(this.swing);
      this.register(this.delay);
      this.register(this.blocks);
      this.register(this.placeRange);
      this.register(this.strict);
      this.register(this.attack);
      this.register(this.syncFastbreakCev);
      this.register(this.jumpDisable);
      this.register(this.render);
      this.register(this.render2D);
      this.register(this.hotbarGap);
      this.register(this.targetRange);
      this.register(this.extension);
      this.register(this.noStep);
      this.register(this.noScaffold);
      this.HELPER = new BlockStateHelper();
      this.cached = new HashMap();
      this.speeds = new HashMap();
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

            this.target = null;
            this.getTargets();
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
      this.clear();
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
      int delay = this.timings.getValue() == AutoTrap.Timing.Strict ? 150 : 50;
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
      if ((Boolean)this.syncFastbreakCev.getValue()) {
         FastBreak fastBreak = (FastBreak)Vonware.moduleManager.getModuleByClass(FastBreak.class);
         if (fastBreak != null && fastBreak.shouldYieldAutoTrap(pos)) {
            return;
         }
      }

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
                  if (this.burstBlocks >= (Integer)this.blocks.getValue() && this.timings.getValue() == AutoTrap.Timing.Strict) {
                     maxBlocks = 1;
                  }

                  if (!mc.player.onGround && this.timings.getValue() == AutoTrap.Timing.Strict) {
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
      List<EnumFacing> valid = new ArrayList();

      for(EnumFacing direction : EnumFacing.VALUES) {
         BlockPos offset = pos.offset(direction);
         if (this.isValidPlacePos(offset, provider) && (!(Boolean)this.strict.getValue() || this.canSeeFace(offset, direction.getOpposite()))) {
            valid.add(direction);
         }
      }

      if (valid.isEmpty()) {
         return null;
      } else {
         valid.sort((f1, f2) -> {
            BlockPos n1 = pos.offset(f1);
            BlockPos n2 = pos.offset(f2);
            Vec3i d1 = f1.getOpposite().getDirectionVec();
            Vec3i d2 = f2.getOpposite().getDirectionVec();
            new Vec3d((double)n1.getX() + (double)0.5F + (double)d1.getX() * (double)0.5F, (double)n1.getY() + (double)0.5F + (double)d1.getY() * (double)0.5F, (double)n1.getZ() + (double)0.5F + (double)d1.getZ() * (double)0.5F);
            new Vec3d((double)n2.getX() + (double)0.5F + (double)d2.getX() * (double)0.5F, (double)n2.getY() + (double)0.5F + (double)d2.getY() * (double)0.5F, (double)n2.getZ() + (double)0.5F + (double)d2.getZ() * (double)0.5F);
            float[] r1 = RotationUtil.getRotations(n1, f1.getOpposite());
            float[] r2 = RotationUtil.getRotations(n2, f2.getOpposite());
            float delta1 = Math.abs(r1[0] - mc.player.rotationYaw) + Math.abs(r1[1] - mc.player.rotationPitch);
            float delta2 = Math.abs(r2[0] - mc.player.rotationYaw) + Math.abs(r2[1] - mc.player.rotationPitch);
            return Float.compare(delta1, delta2);
         });
         return (EnumFacing)valid.get(0);
      }
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

   private boolean canSeeFace(BlockPos target, EnumFacing face) {
      Vec3d eyes = new Vec3d(mc.player.posX, mc.player.posY + (double)mc.player.getEyeHeight(), mc.player.posZ);
      Vec3d centered = new Vec3d((double)target.getX() + (double)0.5F, (double)target.getY() + (double)0.5F, (double)target.getZ() + (double)0.5F);
      Vec3i dir = face.getDirectionVec();
      Vec3d faceCenter = centered.add((double)dir.getX() * (double)0.5F, (double)dir.getY() * (double)0.5F, (double)dir.getZ() * (double)0.5F);
      switch (face) {
         case NORTH:
            return eyes.z < faceCenter.z;
         case EAST:
            return eyes.x > faceCenter.x;
         case SOUTH:
            return eyes.z > faceCenter.z;
         case WEST:
            return eyes.x < faceCenter.x;
         case UP:
            return eyes.y > faceCenter.y;
         case DOWN:
            return eyes.y < faceCenter.y;
         default:
            return false;
      }
   }

   private void removeRenderPositions(List<RenderPosition> positions) {
      if (!positions.isEmpty()) {
         for(RenderPosition renderPos : positions) {
            this.renderBlocks.remove(renderPos);
         }

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
         Render2DMethods.drawHudPill("AutoTrap " + InventoryUtil.getFullBlockCount(), OBSIDIAN_ICON, "x", (Integer)this.hotbarGap.getValue(), 20.0F, 1.3F, -1728053248, -1426063361, -1);
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
      if (this.disablesOnDeath()) {
         if (event.getGui() instanceof GuiGameOver) {
            this.disable();
         }

      }
   }

   @SubscribeEvent
   public void onWorldUnload(WorldEvent.Unload event) {
      if (this.disabledOnDisconnect()) {
         if (event.getWorld().isRemote) {
            this.disable();
         }

      }
   }

   public void clear() {
      this.cached.clear();
      this.speeds.clear();
      this.target = null;
   }

   public boolean disabledOnDisconnect() {
      return true;
   }

   public boolean disablesOnDeath() {
      return true;
   }

   protected void getTargets() {
      this.cached.clear();
      this.updateSpeed();
      EntityPlayer newTarget = this.calcTarget();
      this.target = newTarget;
      if (newTarget != null) {
         List<BlockPos> newTrapping = (List)this.cached.get(newTarget);
         if (newTrapping == null) {
            newTrapping = this.getPositions(newTarget);
         }

         this.placeList = newTrapping;
      }
   }

   private List<BlockPos> getPositions(EntityPlayer player) {
      List<BlockPos> blocked = new ArrayList();
      BlockPos playerPos = new BlockPos(player);
      if (!HoleUtil.isHole(playerPos) && (Boolean)this.extension.getValue()) {
         List<BlockPos> unfiltered = (new ArrayList<BlockPos>(PositionUtil.getBlockedPositions((Entity)player))).stream().sorted(Comparator.comparingDouble(BlockUtil::getDistanceSq)).collect(Collectors.toList());
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
      return (List)positions.stream().filter((posx) -> BlockUtil.getDistanceSq(posx) <= MathUtil.square((double)(Float)this.placeRange.getValue())).collect(Collectors.toList());
   }

   private List<BlockPos> positionsFromBlocked(List<BlockPos> blockedIn) {
      List<BlockPos> positions = new ArrayList();
      if (!(Boolean)this.noStep.getValue() && !blockedIn.isEmpty()) {
         BlockPos[] helping = this.findTopHelping(blockedIn, true);

         for(BlockPos pos : helping) {
            if (pos != null) {
               positions.add(pos);
               break;
            }
         }
      }

      List<BlockPos> alreadyAdded = new ArrayList();
      blockedIn.forEach((posx) -> alreadyAdded.addAll(this.applyOffsets(posx, Trap.TOP, alreadyAdded)));
      positions.addAll(alreadyAdded);
      List<BlockPos> alreadyAdded2 = new ArrayList();
      blockedIn.forEach((posx) -> alreadyAdded2.addAll(this.applyOffsets(posx, Trap.CHEST, alreadyAdded2)));
      positions.addAll(alreadyAdded2);
      if (blockedIn.size() == 1) {
         if ((Boolean)this.noScaffold.getValue()) {
            List<BlockPos> alreadyAdded3 = new ArrayList();
            blockedIn.forEach((posx) -> alreadyAdded3.addAll(this.applyOffsets(posx, Trap.NO_SCAFFOLD, alreadyAdded3)));
            positions.addAll(alreadyAdded3);
         }

         if ((Boolean)this.noStep.getValue()) {
            List<BlockPos> alreadyAdded4 = new ArrayList();
            blockedIn.forEach((posx) -> alreadyAdded4.addAll(this.applyOffsets(posx, Trap.NO_STEP, alreadyAdded4)));
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

   private BlockPos[] findTopHelping(List<BlockPos> positions, boolean first) {
      BlockPos[] bestPos = new BlockPos[]{null, null, null, null, ((BlockPos)positions.get(0)).up().north(), null};

      for(BlockPos pos : positions) {
         BlockPos up = pos.up();

         for(EnumFacing facing : FacingUtil.TOP) {
            BlockPos helping = up.offset(facing);
            if (!(Boolean)this.strict.getValue() || this.canSeeFace(helping, facing.getOpposite())) {
               if (!mc.world.getBlockState(helping).getMaterial().isReplaceable()) {
                  bestPos[0] = helping;
                  return bestPos;
               }

               EnumFacing helpingFace = (Boolean)this.strict.getValue() ? this.getStrictDirection(helping) : BlockUtil.getFacing(helping, this.HELPER);
               byte blockingFactor = this.helpingEntityCheck(helping);
               if (helpingFace != null) {
                  switch (blockingFactor) {
                     case 0:
                        bestPos[0] = helping;
                        break;
                     case 1:
                        bestPos[2] = helping;
                  }
               } else {
                  switch (blockingFactor) {
                     case 0:
                        if (!first || bestPos[5] != null) {
                           break;
                        }

                        List<BlockPos> hPositions = new ArrayList();

                        for(BlockPos hPos : positions) {
                           hPositions.add(hPos.down());
                        }

                        bestPos[5] = this.findTopHelping(hPositions, false)[0];
                        bestPos[1] = helping;
                        break;
                     case 1:
                        bestPos[3] = helping;
                  }
               }
            }
         }
      }

      return bestPos;
   }

   private EnumFacing getStrictDirection(BlockPos pos) {
      for(EnumFacing facing : EnumFacing.VALUES) {
         BlockPos neighbor = pos.offset(facing);
         IBlockState state = this.HELPER.getBlockState(neighbor);
         if (!state.getMaterial().isReplaceable() && this.canSeeFace(neighbor, facing.getOpposite())) {
            return facing;
         }
      }

      return null;
   }

   private byte helpingEntityCheck(BlockPos pos) {
      byte blocking = 0;

      for(Entity entity : mc.world.getEntitiesWithinAABB(Entity.class, new AxisAlignedBB(pos))) {
         if (entity != null && !EntityUtil.isDead(entity) && entity.preventEntitySpawning && (!(entity instanceof EntityPlayer) || entity.getEntityBoundingBox().intersects(new AxisAlignedBB(pos)))) {
            return 2;
         }
      }

      return 0;
   }

   protected EntityPlayer calcTarget() {
      EntityPlayer closest = null;
      double distance = Double.MAX_VALUE;

      for(EntityPlayer player : mc.world.playerEntities) {
         double playerDist = mc.player.getDistanceSq(player);
         if (playerDist < distance && this.isValid(player)) {
            closest = player;
            distance = playerDist;
         }
      }

      return closest;
   }

   private boolean isValid(EntityPlayer player) {
      return player != null && !EntityUtil.isDead(player) && !player.equals(mc.player) && !player.getName().equals(mc.player.getName()) && !Vonware.friendManager.isFriend(player) && player.getDistanceSq(mc.player) <= MathUtil.square((double)(Float)this.targetRange.getValue()) && this.getSpeed(player) <= (double)22.0F;
   }

   protected void updateSpeed() {
      for(EntityPlayer player : mc.world.playerEntities) {
         double xDist = player.posX - player.prevPosX;
         double yDist = player.posY - player.prevPosY;
         double zDist = player.posZ - player.prevPosZ;
         double speed = xDist * xDist + yDist * yDist + zDist * zDist;
         this.speeds.put(player, speed);
      }

   }

   private double getSpeed(EntityPlayer player) {
      Double playerSpeed = (Double)this.speeds.get(player);
      return playerSpeed != null ? Math.sqrt(playerSpeed) * (double)20.0F * 3.6 : (double)0.0F;
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
