package fat.vonware.features.modules.player;

import fat.vonware.Global;
import fat.vonware.Vonware;
import fat.vonware.ducks.ICPacketPlayerDigging;
import fat.vonware.ducks.IMinecraft;
import fat.vonware.ducks.IPlayerControllerMP;
import fat.vonware.event.events.BreakBlockEvent;
import fat.vonware.event.events.DamageBlockEvent;
import fat.vonware.event.events.MotionUpdateEvent;
import fat.vonware.event.events.PacketEvent;
import fat.vonware.event.events.Render3DEvent;
import fat.vonware.event.events.Block.ClickBlockEvent;
import fat.vonware.features.command.Command;
import fat.vonware.features.modules.Module;
import fat.vonware.features.modules.client.ClickGui;
import fat.vonware.features.modules.client.Debug;
import fat.vonware.features.setting.Setting;
import fat.vonware.manager.NCPManager;
import fat.vonware.manager.RotationManager;
import fat.vonware.mixin.mixins.accessor.ITimer;
import fat.vonware.util.BlockUtil;
import fat.vonware.util.EntityUtil;
import fat.vonware.util.Interpolation;
import fat.vonware.util.InventoryUtil;
import fat.vonware.util.MathUtil;
import fat.vonware.util.PlayerUtil;
import fat.vonware.util.PositionUtil;
import fat.vonware.util.RotationUtil;
import fat.vonware.util.Timer;
import fat.vonware.util.Animations.AnimationUtil;
import fat.vonware.util.Animations.render.ColorUtil;
import fat.vonware.util.Animations.render.RenderMethods;
import fat.vonware.util.AutoCity.MineUtil;
import fat.vonware.util.FastBreak.AttackUtil;
import fat.vonware.util.HoleFill.other.RayTraceUtil;
import fat.vonware.util.Phase.PacketUtil;
import java.awt.Color;
import java.util.function.Function;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.block.BlockShulkerBox;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityEnderCrystal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.ClickType;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.CPacketAnimation;
import net.minecraft.network.play.client.CPacketClickWindow;
import net.minecraft.network.play.client.CPacketHeldItemChange;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.network.play.client.CPacketPlayerDigging;
import net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock;
import net.minecraft.network.play.client.CPacketPlayerDigging.Action;
import net.minecraft.network.play.server.SPacketBlockChange;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;

public class FastBreak extends Module {
   private final Setting<RotationsEnum> rotation;
   private final Setting<MineMode> mode;
   private final Setting<GroundMode> groundMode;
   private final Setting<RenderMode> renderMode;
   private final Setting<ColorType> colorMode;
   private final Setting<SwapEnum> swap;
   private final Setting<Boolean> smartTerrain;
   private final Setting<BlockList> blockMode;
   private final Setting<Boolean> fast;
   private final Setting<Boolean> strict;
   private final Setting<Float> range;
   private final Setting<Float> lineWidth;
   private final Setting<Integer> alpha;
   private final Setting<Integer> lineAlpha;
   private final Setting<Boolean> fade;
   private final Setting<Boolean> crystals;
   private final Setting<Boolean> newVerPlace;
   private final Setting<Boolean> raytrace;
   private final Setting<RotationsEnum> crystalRotation;
   private final Setting<Float> crystalDelay;
   protected final Timer timer;
   protected final Timer rotationTimer;
   protected final Timer crystalTimer;
   private final Timer unbreakTimer;
   private ITimer breakTimerSync;
   private final AnimationUtil.LerpAnimation growAnim;
   protected final float[] damages;
   protected float maxDamage;
   protected boolean sent;
   protected boolean shouldAbort;
   protected boolean canBreak;
   protected BlockPos pos;
   protected IBlockState state;
   protected EnumFacing direction;
   protected boolean render;
   protected boolean crystalAttack;
   protected int crystalRetries;
   private boolean isBreaking;
   private long cevCrystalStartedAt;
   private long cevCrystalAttackedAt;
   private boolean internalSwapActive;
   private long lastInternalSwapTime;
   private boolean smartTerrainActive;
   private BlockPos terrainPos;
   private int oldSlot;
   private int currentTerrainTool;
   private boolean brokenAwaitingReset;
   private boolean confirmedAirObserved;
   private boolean beginCounting;
   private BlockPos lastAttemptedBreak;
   private BlockPos lastConfirmedBroke;
   private EnumFacing lastAttemptedFacing;
   private int groundedTicks;
   private int airTicks;
   private final Timer terrainSwapBackTimer;
   private boolean pendingTerrainSwapBack;
   private int pendingOldSlot;
   public boolean breakLock;
   private final AnimationUtil.DeltaAnimation growDelta;
   private boolean breakDelay;
   private final Timer remineDelay;
   private BlockPos scheduledPos;
   private EnumFacing scheduledFacing;
   private AxisAlignedBB lastRawBox;
   private Color lastFillColor;
   private Color lastWireColor;
   private AxisAlignedBB fadeRawBox;
   private Color fadeFillColor;
   private Color fadeWireColor;
   private long fadeStartMs;

   public FastBreak() {
      super("FastBreak", "Fast block breaking", Module.Category.PLAYER, true, false, false);
      this.rotation = (new Setting("Rotations", FastBreak.RotationsEnum.Packet)).withDescription((Function<RotationsEnum, String>)((mode) -> {
         switch (mode) {
            case None:
               return "None: Mines blocks without applying FastBreak rotations.";
            case Event:
               return "Event: Requests mining rotations through the motion-event rotation path.";
            case Packet:
               return "Packet: Sends rotation packets directly for FastBreak mining actions.";
            default:
               return "";
         }
      }));
      this.mode = (new Setting("MineMode", FastBreak.MineMode.Packet)).withDescription((Function<MineMode, String>)((mode) -> {
         switch (mode) {
            case Packet:
               return "Packet: Uses FastBreak's packet mining and remine logic instead of normal vanilla progress.";
            case Instant:
               return "Instant: Uses the instant-break mining path when the block is ready to be destroyed.";
            default:
               return "";
         }
      }));
      this.groundMode = (new Setting("GroundMode", FastBreak.GroundMode.Vanilla)).withDescription((Function<GroundMode, String>)((mode) -> {
         switch (mode) {
            case Server:
               return "Server: Uses Vonware's tracked server-side on-ground state for FastBreak checks.";
            case Vanilla:
               return "Vanilla: Uses the local Minecraft player onGround value for FastBreak checks.";
            default:
               return "";
         }
      }));
      this.renderMode = (new Setting("RenderType", FastBreak.RenderMode.Static)).withDescription((Function<RenderMode, String>)((mode) -> {
         switch (mode) {
            case Grow:
               return "Grow: Animates the mining box growing with the standard interpolation.";
            case GrowEased:
               return "GrowEased: Animates the mining box growing with an eased interpolation.";
            case Static:
               return "Static: Renders the mining box at its normal full size.";
            default:
               return "";
         }
      }));
      this.colorMode = (new Setting("ColorType", FastBreak.ColorType.Damage)).withDescription((Function<ColorType, String>)((mode) -> {
         switch (mode) {
            case Damage:
               return "Damage: Colors the mining render using the first damage-progress color calculation.";
            case DamageTwo:
               return "DamageTwo: Uses the second damage-progress color calculation.";
            case DamageThree:
               return "DamageThree: Uses the third damage-progress color calculation.";
            case Global:
               return "Global: Uses the client's global color for the mining render.";
            default:
               return "";
         }
      }));
      this.swap = (new Setting("Swap", FastBreak.SwapEnum.Alternative)).withDescription((Function<SwapEnum, String>)((mode) -> {
         switch (mode) {
            case Silent:
               return "Silent: Temporarily switches to the best mining tool and recovers the previous slot.";
            case Alternative:
               return "Alternative: Uses FastBreak's alternate silent-tool switching path.";
            case Bypass:
               return "Bypass: Uses the inventory bypass swap path for the mining tool.";
            case None:
               return "None: Does not automatically switch to a better mining tool.";
            default:
               return "";
         }
      }));
      this.smartTerrain = new Setting<Boolean>("SmartTerrainMine", false, "Automatically manages tool switching for terrain blocks handled by SmartTerrainMine.");
      this.blockMode = (new Setting("BlockList", FastBreak.BlockList.Minimal)).withDescription((Function<BlockList, String>)((mode) -> {
         switch (mode) {
            case Minimal:
               return "Minimal: Restricts FastBreak to the module's smaller intended block set.";
            case AllBlocks:
               return "AllBlocks: Allows FastBreak to operate on all otherwise valid breakable blocks.";
            default:
               return "";
         }
      }));
      this.fast = new Setting<Boolean>("Fast", false, (v) -> this.mode.getValue() == FastBreak.MineMode.Packet, "Enables the faster Packet-mode mining behavior.");
      this.strict = new Setting<Boolean>("Strict", true, "Uses stricter FastBreak checks and packet handling.");
      this.range = new Setting<Float>("Range", 4.5F, 1.0F, 6.0F, "Maximum distance at which FastBreak continues mining the selected block.");
      this.lineWidth = new Setting<Float>("LineWidth", 1.4F, 1.0F, 4.0F, "Controls the thickness of the FastBreak render outline.");
      this.alpha = new Setting<Integer>("BoxAlpha", 50, 0, 255, "Controls the transparency of the FastBreak filled box.");
      this.lineAlpha = new Setting<Integer>("LineAlpha", 125, 0, 255, "Controls the transparency of the FastBreak outline.");
      this.fade = new Setting<Boolean>("Fade", false, "Enables the FastBreak render fade behavior.");
      this.crystals = new Setting<Boolean>("Crystal", false, "Enables crystal placement or attack handling tied to the mined block.");
      this.newVerPlace = new Setting<Boolean>("1.13+", false, (v) -> (Boolean)this.crystals.getValue(), "Uses post-1.12 crystal placement rules for FastBreak crystal handling.");
      this.raytrace = new Setting<Boolean>("Raytrace", false, (v) -> (Boolean)this.crystals.getValue(), "Requires the crystal interaction to pass the configured raytrace check.");
      this.crystalRotation = (new Setting("CrystalRotation", FastBreak.RotationsEnum.Packet, (v) -> (Boolean)this.crystals.getValue())).withDescription((Function<RotationsEnum, String>)((mode) -> {
         switch (mode) {
            case None:
               return "None: Handles FastBreak crystals without applying a crystal rotation.";
            case Event:
               return "Event: Uses the event rotation path for FastBreak crystal interactions.";
            case Packet:
               return "Packet: Sends a direct rotation packet for FastBreak crystal interactions.";
            default:
               return "";
         }
      }));
      this.crystalDelay = new Setting<Float>("Crystal Delay", 1.4F, 1.0F, 4.0F, (v) -> (Boolean)this.crystals.getValue(), "Delay used by FastBreak's crystal interaction sequence.");
      this.growAnim = new AnimationUtil.LerpAnimation(0.0F, 0.18F, 0.005F);
      this.cevCrystalStartedAt = -1L;
      this.cevCrystalAttackedAt = -1L;
      this.internalSwapActive = false;
      this.lastInternalSwapTime = 0L;
      this.smartTerrainActive = false;
      this.terrainPos = null;
      this.oldSlot = -1;
      this.currentTerrainTool = -1;
      this.brokenAwaitingReset = false;
      this.confirmedAirObserved = false;
      this.beginCounting = false;
      this.lastAttemptedBreak = null;
      this.lastConfirmedBroke = null;
      this.lastAttemptedFacing = null;
      this.groundedTicks = 0;
      this.airTicks = 0;
      this.terrainSwapBackTimer = new Timer();
      this.pendingTerrainSwapBack = false;
      this.pendingOldSlot = -1;
      this.breakLock = false;
      this.growDelta = new AnimationUtil.DeltaAnimation(0.0F, 400.0F);
      this.breakDelay = false;
      this.remineDelay = new Timer();
      this.scheduledPos = null;
      this.scheduledFacing = null;
      this.lastRawBox = null;
      this.lastFillColor = null;
      this.lastWireColor = null;
      this.fadeRawBox = null;
      this.fadeFillColor = null;
      this.fadeWireColor = null;
      this.fadeStartMs = 0L;
      this.register(this.rotation);
      this.register(this.mode);
      this.register(this.groundMode);
      this.register(this.renderMode);
      this.register(this.colorMode);
      this.register(this.swap);
      this.register(this.blockMode);
      this.register(this.smartTerrain);
      this.register(this.fast);
      this.register(this.strict);
      this.register(this.crystals);
      this.register(this.newVerPlace);
      this.register(this.raytrace);
      this.register(this.crystalRotation);
      this.register(this.crystalDelay);
      this.register(this.range);
      this.register(this.lineWidth);
      this.register(this.alpha);
      this.register(this.lineAlpha);
      this.register(this.fade);
      this.timer = new Timer();
      this.rotationTimer = new Timer();
      this.crystalTimer = new Timer();
      this.unbreakTimer = new Timer();
      this.damages = new float[]{0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F};
      this.sent = false;
      this.render = false;
      this.isBreaking = false;
   }

   public void onLogout() {
      if (this.smartTerrainActive) {
         this.forceResetTerrainSwap();
      }

      this.reset();
      this.pendingTerrainSwapBack = false;
      this.pendingOldSlot = -1;
   }

   @SubscribeEvent
   public void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
      if ((Boolean)this.smartTerrain.getValue()) {
         if (mc.player != null && mc.world != null) {
            BlockPos pos = event.getPos();
            Block block = mc.world.getBlockState(pos).getBlock();
            IBlockState state = mc.world.getBlockState(pos);
            if (BlockUtil.isTerrainBlock(block, state)) {
               if (this.smartTerrainActive && !pos.equals(this.terrainPos)) {
                  this.forceResetTerrainSwap();
               }

               if (!this.smartTerrainActive) {
                  this.oldSlot = mc.player.inventory.currentItem;
                  int pickSlot = MineUtil.findBestTool(pos);
                  if (pickSlot != -1 && pickSlot != this.oldSlot) {
                     mc.player.inventory.currentItem = pickSlot;
                     PacketUtil.send(new CPacketHeldItemChange(pickSlot));
                     this.currentTerrainTool = pickSlot;
                     this.smartTerrainActive = true;
                     this.terrainPos = pos;
                     if (Debug.getInstance().isEnabled() && (Boolean)Debug.getInstance().fastBreakDebug.getValue()) {
                        Command.sendSilentMessage(TextFormatting.AQUA + "SmartTerrain: Swapped to slot " + pickSlot + " for mining");
                     }
                  }
               }
            }

         }
      }
   }

   @SubscribeEvent
   public void onClientTick(TickEvent.ClientTickEvent event) {
      if (event.phase == Phase.END) {
         if (!(Boolean)this.smartTerrain.getValue()) {
            if (this.smartTerrainActive) {
               this.forceResetTerrainSwap();
            }

         } else {
            if (this.pendingTerrainSwapBack && this.terrainSwapBackTimer.passed(75L)) {
               if (mc.player != null && mc.player.inventory.currentItem != this.pendingOldSlot) {
                  mc.player.inventory.currentItem = this.pendingOldSlot;
                  PacketUtil.send(new CPacketHeldItemChange(this.pendingOldSlot));
                  if (Debug.getInstance().isEnabled() && (Boolean)Debug.getInstance().fastBreakDebug.getValue()) {
                     Command.sendSilentMessage(TextFormatting.GREEN + "SmartTerrain: Swapped back to slot " + this.pendingOldSlot);
                  }
               }

               this.pendingTerrainSwapBack = false;
               this.pendingOldSlot = -1;
            }

            if (mc.player != null && mc.world != null) {
               if (this.smartTerrainActive) {
                  boolean isClicking = mc.gameSettings.keyBindAttack.isKeyDown();
                  boolean blockBroken = this.terrainPos != null && mc.world.getBlockState(this.terrainPos).getBlock() == Blocks.AIR;
                  if (!isClicking || blockBroken) {
                     if (blockBroken && Debug.getInstance().isEnabled() && (Boolean)Debug.getInstance().fastBreakDebug.getValue()) {
                        Command.sendSilentMessage(TextFormatting.GREEN + "SmartTerrain: Block broken, swapping back");
                     } else if (!isClicking && Debug.getInstance().isEnabled() && (Boolean)Debug.getInstance().fastBreakDebug.getValue()) {
                        Command.sendSilentMessage(TextFormatting.AQUA + "SmartTerrain: Stopped clicking, swapping back");
                     }

                     this.forceResetTerrainSwap();
                  }
               }

            }
         }
      }
   }

   @SubscribeEvent
   public void onPacketSend(PacketEvent.Send event) {
      if ((Boolean)this.smartTerrain.getValue() && this.smartTerrainActive) {
         if (event.getPacket() instanceof CPacketHeldItemChange) {
            CPacketHeldItemChange packet = (CPacketHeldItemChange)event.getPacket();
            if (packet.getSlotId() != this.currentTerrainTool && packet.getSlotId() != this.oldSlot) {
               if (Debug.getInstance().isEnabled() && (Boolean)Debug.getInstance().fastBreakDebug.getValue()) {
                  Command.sendSilentMessage(TextFormatting.YELLOW + "SmartTerrain: Manual slot change detected, resetting");
               }

               this.smartTerrainActive = false;
               this.terrainPos = null;
               this.oldSlot = -1;
               this.currentTerrainTool = -1;
            }
         }

         if (event.getPacket() instanceof CPacketPlayerDigging) {
            CPacketPlayerDigging packet = (CPacketPlayerDigging)event.getPacket();
            ICPacketPlayerDigging access = (ICPacketPlayerDigging)packet;
            if (access.isNormalDigging() && (packet.getAction() == Action.START_DESTROY_BLOCK || packet.getAction() == Action.STOP_DESTROY_BLOCK || packet.getAction() == Action.ABORT_DESTROY_BLOCK)) {
               event.setCanceled(true);
            }
         }

      }
   }

   private void forceResetTerrainSwap() {
      if (this.smartTerrainActive) {
         if (this.oldSlot != -1 && mc.player != null) {
            this.pendingOldSlot = this.oldSlot;
            this.pendingTerrainSwapBack = true;
            this.terrainSwapBackTimer.reset();
            if (Debug.getInstance().isEnabled() && (Boolean)Debug.getInstance().fastBreakDebug.getValue()) {
               Command.sendSilentMessage(TextFormatting.AQUA + "SmartTerrain: Waiting 75ms before swapping back to slot " + this.oldSlot);
            }
         }

         this.smartTerrainActive = false;
         this.terrainPos = null;
         this.oldSlot = -1;
         this.currentTerrainTool = -1;
      }
   }

   private boolean resolveOnGround() {
      return this.groundMode.getValue() == FastBreak.GroundMode.Server ? Vonware.positionManager.isOnGround() : mc.player.onGround;
   }

   private float getSpeedMultiplierScale() {
      if (mc == null) {
         return 1.0F;
      } else {
         if (this.breakTimerSync == null) {
            try {
               this.breakTimerSync = (ITimer)((IMinecraft)mc).getTimer();
            } catch (Exception var3) {
               return 1.0F;
            }
         }

         if (this.breakTimerSync == null) {
            return 1.0F;
         } else {
            float tickLength = this.breakTimerSync.getTickLength();
            if (tickLength <= 0.1F) {
               return 1.0F;
            } else {
               float scale = tickLength / 50.0F;
               return MathUtil.clamp(scale, 0.05F, 5.0F);
            }
         }
      }
   }

   @SubscribeEvent(
      priority = EventPriority.HIGHEST
   )
   public void onMotionUpdate(MotionUpdateEvent event) {
      if (event.getStage() == 0) {
         if (!PlayerUtil.isCreative() && !PlayerUtil.isSpectator()) {
            this.breakLock = false;
            if (this.scheduledPos != null && this.pos == null) {
               EnumFacing facing = this.scheduledFacing != null ? this.scheduledFacing : BlockUtil.getMineableSide(this.scheduledPos, (Boolean)this.strict.getValue(), (Boolean)this.strict.getValue());
               if (facing != null && MineUtil.canBreak(this.scheduledPos)) {
                  PacketUtil.swing();
                  PacketUtil.send(new CPacketPlayerDigging(Action.START_DESTROY_BLOCK, this.scheduledPos, facing));
                  this.pos = this.scheduledPos;
                  this.direction = facing;
                  this.state = mc.world.getBlockState(this.scheduledPos);
                  this.isBreaking = true;
                  this.scheduledPos = null;
                  this.scheduledFacing = null;
                  this.brokenAwaitingReset = false;
               }
            }

            if (this.mode.getValue() == FastBreak.MineMode.Packet) {
               this.handleInstantRemine();
            }

            if (this.pos != null && this.isBlockValid(mc.world.getBlockState(this.getPos()).getBlock(), this.getPos())) {
               Vec3d playerVec = PositionUtil.getEyesPos();
               if (this.pos.distanceSq(playerVec.x, playerVec.y, playerVec.z) > MathUtil.square((double)(Float)this.range.getValue())) {
                  this.abortCurrentPos(true);
               } else {
                  if (this.getBlock() != Blocks.AIR) {
                     this.state = mc.world.getBlockState(this.pos);
                     this.isBreaking = true;
                  } else {
                     this.isBreaking = false;
                  }

                  this.updateDamages();
                  int pickSlot = MineUtil.findBestTool(this.pos);
                  if (this.maxDamage >= 0.9F) {
                     this.render = true;
                  } else if (this.maxDamage <= 0.8F && this.maxDamage >= 0.2F) {
                     this.render = false;
                  }

                  if (this.damages[mc.player.inventory.currentItem] >= 1.0F || pickSlot >= 0 && this.damages[pickSlot] >= 1.0F) {
                     this.canBreak = true;
                     boolean canAttempt = this.swap.getValue() != FastBreak.SwapEnum.None || mc.player.inventory.currentItem == pickSlot;
                     if (canAttempt) {
                        if (!this.breakDelay) {
                           this.breakDelay = true;
                        } else {
                           this.tryBreak(pickSlot);
                           this.breakDelay = false;
                        }
                     }
                  } else {
                     this.canBreak = false;
                     this.breakDelay = false;
                  }
               }
            } else {
               this.isBreaking = false;
            }
         }

         if (this.rotation.getValue() == FastBreak.RotationsEnum.Event && this.pos != null && this.maxDamage >= 1.0F && MineUtil.canBreak(this.pos)) {
            float[] rotations = RotationUtil.getRotations(this.pos);
            RotationManager.INSTANCE.requestRotation(RotationManager.RotationSource.FAST_BREAK, rotations[0], rotations[1]);
         }

         if (!(Boolean)this.crystals.getValue()) {
            return;
         }

         if (!this.crystalAttack) {
            return;
         }

         Entity entity = null;
         if (this.pos != null) {
            for(Entity crystal : mc.world.loadedEntityList) {
               if (crystal instanceof EntityEnderCrystal && !crystal.isDead && !(crystal.getDistance(mc.player) > 3.3F) && (!(Boolean)this.raytrace.getValue() || mc.player.canEntityBeSeen(crystal))) {
                  entity = crystal;
               }
            }

            if (entity != null && entity.getPosition().equals(this.pos.up()) && this.getPlacePlayer(this.pos) != null && this.crystalTimer.passed((long)(100.0F * (Float)this.crystalDelay.getValue())) && this.getBlock() == Blocks.AIR) {
               if (this.crystalRetries >= 3) {
                  this.crystalAttack = false;
                  this.crystalRetries = 0;
                  return;
               }

               float[] angle = RotationUtil.getRotations(entity.posX, entity.posY, entity.posZ);
               if (this.crystalRotation.getValue() == FastBreak.RotationsEnum.Event) {
                  RotationManager.INSTANCE.requestRotation(RotationManager.RotationSource.FAST_BREAK, angle[0], angle[1]);
               }

               if (this.crystalRotation.getValue() == FastBreak.RotationsEnum.Packet) {
                  PacketUtil.rotate(angle);
               }

               AttackUtil.attackEntity(entity, this.swap.getValue() == FastBreak.SwapEnum.Alternative, true);
               this.cevCrystalAttackedAt = System.currentTimeMillis();
               ++this.crystalRetries;
               this.crystalTimer.reset();
            }
         }
      }

   }

   @SubscribeEvent
   public void onPacketPost(PacketEvent.Post event) {
      if (event.getPacket() instanceof CPacketHeldItemChange) {
         if (this.pos != null && this.swap.getValue() == FastBreak.SwapEnum.Alternative) {
            if (this.internalSwapActive && System.currentTimeMillis() - this.lastInternalSwapTime < 100L) {
               return;
            }

            BlockPos lostTarget = this.pos;
            EnumFacing lostFacing = this.direction;
            this.abortCurrentPos(true);
            this.render = false;
            if (lostTarget != null) {
               this.scheduledPos = lostTarget;
               this.scheduledFacing = lostFacing;
            }
         }

         if (this.lastAttemptedBreak != null || this.lastConfirmedBroke != null || this.beginCounting) {
            this.beginCounting = false;
            this.lastConfirmedBroke = null;
            this.lastAttemptedBreak = null;
            this.airTicks = 0;
            this.groundedTicks = 0;
         }
      }

   }

   @SubscribeEvent
   public void onRender3D(Render3DEvent event) {
      if (!PlayerUtil.isCreative() && this.getPos() != null && !this.brokenAwaitingReset) {
         IBlockState state = mc.world.getBlockState(this.getPos());
         AxisAlignedBB fullRawBb = state.getSelectedBoundingBox(mc.world, this.getPos()).grow((double)0.002F);
         AxisAlignedBB rawBb = fullRawBb;
         float damage = Math.min(this.maxDamage, 1.0F);
         if (damage < 1.0F) {
            switch ((RenderMode)this.renderMode.getValue()) {
               case Grow:
                  rawBb = fullRawBb.grow((double)-0.5F + (double)damage / (double)2.0F);
                  break;
               case GrowEased:
                  float t = Math.min(AnimationUtil.easeOutCubic(this.growDelta.tick(damage)), damage);
                  rawBb = fullRawBb.grow((double)-0.5F + (double)t / (double)2.0F);
            }
         }

         if (this.isBlockValid(mc.world.getBlockState(this.getPos()).getBlock(), this.getPos())) {
            AxisAlignedBB bb = Interpolation.interpolateAxis(rawBb);
            Color fillColor = this.getColorByDamage();
            Color wireColor = this.getWireColorByDamage();
            RenderMethods.startRender();
            RenderMethods.drawBox(bb, fillColor);
            RenderMethods.drawOutline(bb, (Float)this.lineWidth.getValue(), wireColor);
            RenderMethods.endRender();
            if ((Boolean)this.fade.getValue()) {
               this.lastRawBox = fullRawBb;
               this.lastFillColor = fillColor;
               this.lastWireColor = wireColor;
            }
         }
      }

      if ((Boolean)this.fade.getValue() && this.fadeRawBox != null) {
         long elapsed = System.currentTimeMillis() - this.fadeStartMs;
         int duration = Math.max(1, 2000);
         if (elapsed >= (long)duration) {
            this.fadeRawBox = null;
         } else {
            float progress = (float)elapsed / (float)duration;
            float fadeFactor = 1.0F - AnimationUtil.easeOutCubic(progress);
            Color fillC = ColorUtil.changeAlpha(this.fadeFillColor, (int)MathUtil.clamp((float)this.fadeFillColor.getAlpha() * fadeFactor, 0.0F, 255.0F));
            Color wireC = ColorUtil.changeAlpha(this.fadeWireColor, (int)MathUtil.clamp((float)this.fadeWireColor.getAlpha() * fadeFactor, 0.0F, 255.0F));
            AxisAlignedBB bb = Interpolation.interpolateAxis(this.fadeRawBox);
            RenderMethods.startRender();
            RenderMethods.drawBox(bb, fillC);
            RenderMethods.drawOutline(bb, (Float)this.lineWidth.getValue(), wireC);
            RenderMethods.endRender();
         }
      }

   }

   private boolean render() {
      return this.render;
   }

   private Color getColorByDamage() {
      if (this.colorMode.getValue() == FastBreak.ColorType.Global) {
         if ((Boolean)ClickGui.getInstance().pulseColor.getValue()) {
            Global.updatePulseProgress();
            Color baseColor = Global.getGlobalColor();
            Color pulsedColor = Global.getPulseColor(baseColor, 0);
            return ColorUtil.changeAlpha(pulsedColor, (Integer)this.alpha.getValue());
         } else {
            return ColorUtil.changeAlpha(Global.getGlobalColor(), (Integer)this.alpha.getValue());
         }
      } else if (this.colorMode.getValue() == FastBreak.ColorType.DamageTwo) {
         return ColorUtil.changeAlpha(ColorUtil.lerpRedGreen(this.maxDamage), (Integer)this.alpha.getValue());
      } else if (this.colorMode.getValue() == FastBreak.ColorType.DamageThree) {
         return ColorUtil.changeAlpha(ColorUtil.lerpRedYellowGreen(this.maxDamage), (Integer)this.alpha.getValue());
      } else {
         return this.render() ? ColorUtil.changeAlpha(Color.GREEN, (Integer)this.alpha.getValue()) : ColorUtil.changeAlpha(Color.RED, (Integer)this.alpha.getValue());
      }
   }

   private Color getWireColorByDamage() {
      if (this.colorMode.getValue() == FastBreak.ColorType.Global) {
         if ((Boolean)ClickGui.getInstance().pulseColor.getValue()) {
            Global.updatePulseProgress();
            Color baseColor = Global.getGlobalColor();
            Color pulsedColor = Global.getPulseColor(baseColor, 0);
            return ColorUtil.changeAlpha(pulsedColor, (Integer)this.lineAlpha.getValue());
         } else {
            return ColorUtil.changeAlpha(Global.getGlobalColor(), (Integer)this.lineAlpha.getValue());
         }
      } else if (this.colorMode.getValue() == FastBreak.ColorType.DamageTwo) {
         return ColorUtil.changeAlpha(ColorUtil.lerpRedGreen(this.maxDamage), (Integer)this.lineAlpha.getValue());
      } else if (this.colorMode.getValue() == FastBreak.ColorType.DamageThree) {
         return ColorUtil.changeAlpha(ColorUtil.lerpRedYellowGreen(this.maxDamage), (Integer)this.lineAlpha.getValue());
      } else {
         return this.render() ? ColorUtil.changeAlpha(Color.GREEN, (Integer)this.lineAlpha.getValue()) : ColorUtil.changeAlpha(Color.RED, (Integer)this.lineAlpha.getValue());
      }
   }

   public void scheduleMine(BlockPos blockPos) {
      if (this.pos == null || !this.pos.equals(blockPos)) {
         if (this.scheduledPos == null || !this.scheduledPos.equals(blockPos)) {
            if (this.pos != null) {
               this.abortCurrentPos(true);
            }

            this.scheduledPos = blockPos;
            this.scheduledFacing = BlockUtil.getMineableSide(blockPos, (Boolean)this.strict.getValue(), (Boolean)this.strict.getValue());
         }
      }
   }

   @SubscribeEvent
   public void onBlockDamage(DamageBlockEvent event) {
      if (!PlayerUtil.isCreative() && !PlayerUtil.isSpectator()) {
         BlockPos pos = event.getPos();
         EnumFacing facing = event.getFacing();
         if (MineUtil.canBreak(pos) && this.isBlockValid(mc.world.getBlockState(pos).getBlock(), pos)) {
            event.setCanceled(true);
            if (this.pos != null && this.pos.equals(pos) && this.scheduledPos == null) {
               if (this.unbreakTimer.passed(150L)) {
                  if (Debug.getInstance().isEnabled() && (Boolean)Debug.getInstance().fastBreakDebug.getValue()) {
                     Command.sendSilentMessage(TextFormatting.YELLOW + "Unbreak: canceled " + pos);
                  }

                  this.abortCurrentPos(true);
                  this.timer.reset();
                  this.remineDelay.reset();
                  this.unbreakTimer.reset();
               }

               return;
            }

            if (this.timer.passed(250L) && this.remineDelay.passed(150L)) {
               boolean aborted = false;
               if (this.pos != null && !this.pos.equals(pos)) {
                  if (Debug.getInstance().isEnabled() && (Boolean)Debug.getInstance().fastBreakDebug.getValue()) {
                     Command.sendSilentMessage(TextFormatting.YELLOW + "Switching target: " + this.pos + " -> " + pos);
                  }

                  this.abortCurrentPos(false);
                  aborted = true;
               }

               if (!aborted && this.pos != null && this.pos.equals(pos)) {
                  return;
               }

               if (this.pos == null) {
                  this.setPos(event);
                  this.unbreakTimer.reset();
                  if (Debug.getInstance().isEnabled() && (Boolean)Debug.getInstance().fastBreakDebug.getValue()) {
                     Command.sendSilentMessage(TextFormatting.GREEN + "Started mining: " + pos);
                  }
               }

               PacketUtil.send(this.getAbortPacket());
               PacketUtil.swing();
               PacketUtil.send(new CPacketPlayerDigging(Action.START_DESTROY_BLOCK, pos, facing));
               this.sent = true;
               this.isBreaking = true;
               if (this.mode.getValue() == FastBreak.MineMode.Instant) {
                  this.shouldAbort = true;
               }

               this.timer.reset();
            }
         }

      }
   }

   @SubscribeEvent
   public void onBlockClick(ClickBlockEvent event) {
      if (!fullNullCheck()) {
         if (!(event instanceof ClickBlockEvent.Right)) {
            if ((Boolean)this.smartTerrain.getValue()) {
               BlockPos lookPos = event.getPos();
               Block block = mc.world.getBlockState(lookPos).getBlock();
               IBlockState state = mc.world.getBlockState(lookPos);
               if (BlockUtil.isTerrainBlock(block, state)) {
                  return;
               }
            }

            event.setCanceled(true);
         }
      }
   }

   private void setPos(DamageBlockEvent event) {
      this.reset();
      this.pos = event.getPos();
      this.direction = event.getFacing();
   }

   @SubscribeEvent
   public void onBlockBreak(PacketEvent.Receive event) {
      if (event.getPacket() instanceof SPacketBlockChange) {
         SPacketBlockChange packet = (SPacketBlockChange)event.getPacket();
         if (mc.world != null && mc.player != null) {
            BlockPos packetPos = packet.getBlockPosition();
            if (packetPos != null) {
               if (this.smartTerrainActive && packetPos.equals(this.terrainPos) && packet.getBlockState().getBlock() == Blocks.AIR) {
                  if (Debug.getInstance().isEnabled() && (Boolean)Debug.getInstance().fastBreakDebug.getValue()) {
                     Command.sendSilentMessage(TextFormatting.GREEN + "SmartTerrain: Server confirmed block broken");
                  }

                  this.forceResetTerrainSwap();
               }

               if (packetPos.equals(this.pos)) {
                  if (packet.getBlockState().getBlock() == Blocks.AIR) {
                     if (packetPos.equals(this.lastAttemptedBreak)) {
                        this.lastConfirmedBroke = this.lastAttemptedBreak;
                        this.lastAttemptedBreak = null;
                     }

                     this.remineDelay.reset();
                     this.brokenAwaitingReset = true;
                     this.confirmedAirObserved = false;
                     if ((Boolean)this.fade.getValue() && this.lastRawBox != null) {
                        this.fadeRawBox = this.lastRawBox;
                        this.fadeFillColor = this.lastFillColor;
                        this.fadeWireColor = this.lastWireColor;
                        this.fadeStartMs = System.currentTimeMillis();
                     }

                     this.softReset(false);
                     MinecraftForge.EVENT_BUS.post(new BreakBlockEvent(packetPos));
                  } else {
                     PacketUtil.send(this.getAbortPacket());
                  }
               }

               if (packetPos.equals(this.pos) && packet.getBlockState().equals(mc.world.getBlockState(this.pos)) && this.shouldAbort && this.mode.getValue() == FastBreak.MineMode.Instant) {
                  if (Debug.getInstance().isEnabled() && (Boolean)Debug.getInstance().fastBreakDebug.getValue()) {
                     Command.sendSilentMessage(TextFormatting.RED + "Sending instant mine packet");
                  }

                  PacketUtil.send(new CPacketPlayerDigging(Action.START_DESTROY_BLOCK, this.pos, EnumFacing.DOWN));
                  this.shouldAbort = false;
               }

            }
         }
      }
   }

   public void onEnable() {
      this.reset();
      NCPManager.INSTANCE.setStrict(true);
   }

   public void onDisable() {
      if (this.smartTerrainActive) {
         this.forceResetTerrainSwap();
      }

      this.reset();
      this.beginCounting = false;
      this.lastAttemptedBreak = null;
      this.lastConfirmedBroke = null;
      this.confirmedAirObserved = false;
      this.lastAttemptedFacing = null;
      this.groundedTicks = 0;
      this.airTicks = 0;
      this.pendingTerrainSwapBack = false;
      this.pendingOldSlot = -1;
      this.fadeRawBox = null;
      this.lastRawBox = null;
      NCPManager.INSTANCE.setStrict(false);
   }

   private void handleInstantRemine() {
      if (mc.player != null && mc.world != null) {
         if (this.beginCounting) {
            if (this.lastConfirmedBroke == null) {
               if (mc.player.onGround) {
                  ++this.groundedTicks;
               } else {
                  ++this.airTicks;
               }

               return;
            }

            this.beginCounting = false;
         }

         if (this.lastConfirmedBroke != null && this.pos != null && !this.pos.equals(this.lastConfirmedBroke)) {
            this.lastConfirmedBroke = null;
            this.confirmedAirObserved = false;
            this.lastAttemptedFacing = null;
            this.groundedTicks = 0;
            this.airTicks = 0;
         } else {
            if (this.lastConfirmedBroke != null) {
               IBlockState remineState = mc.world.getBlockState(this.lastConfirmedBroke);
               if (remineState.getBlock() == Blocks.AIR) {
                  this.confirmedAirObserved = true;
                  return;
               }

               if (this.confirmedAirObserved) {
                  for(int i = 0; i < 9; ++i) {
                     this.damages[i] = 0.0F;
                  }

                  this.maxDamage = 0.0F;

                  for(int i = 0; i < this.airTicks; ++i) {
                     this.updateDamagesForPos(this.lastConfirmedBroke, false);
                  }

                  for(int i = 0; i < this.groundedTicks; ++i) {
                     this.updateDamagesForPos(this.lastConfirmedBroke, true);
                  }

                  this.pos = this.lastConfirmedBroke;
                  this.direction = this.lastAttemptedFacing;
                  this.state = remineState;
                  this.isBreaking = true;
                  this.brokenAwaitingReset = false;
                  if (Debug.getInstance().isEnabled() && (Boolean)Debug.getInstance().fastBreakDebug.getValue()) {
                     Command.sendSilentMessage(TextFormatting.AQUA + "Instant remine: pre-filled " + MathUtil.round((double)this.maxDamage, 2) + " damage from " + (this.groundedTicks + this.airTicks) + " ticks");
                  }

                  this.lastConfirmedBroke = null;
                  this.confirmedAirObserved = false;
                  this.groundedTicks = 0;
                  this.airTicks = 0;
               }
            }

         }
      }
   }

   protected void updateDamagesForPos(BlockPos reminePos, boolean onGround) {
      float rateScale = this.getSpeedMultiplierScale();

      for(int i = 0; i < 9; ++i) {
         ItemStack stack = mc.player.inventory.getStackInSlot(i);
         float damage = MineUtil.getDamage(stack, reminePos, onGround);
         damage *= Vonware.serverManager.getFactor();
         damage *= rateScale;
         this.damages[i] = MathUtil.clamp(this.damages[i] + damage, 0.0F, 1.0F);
         if (this.damages[i] > this.maxDamage) {
            this.maxDamage = this.damages[i];
         }
      }

   }

   public String getDisplayInfo() {
      return "" + MathUtil.round((double)this.maxDamage, 1);
   }

   private void beginCevCrystalSync() {
      this.crystalAttack = true;
      this.crystalRetries = 0;
      this.cevCrystalStartedAt = System.currentTimeMillis();
      this.cevCrystalAttackedAt = -1L;
   }

   public boolean shouldYieldAutoTrap(BlockPos placementPos) {
      if (this.isEnabled() && (Boolean)this.crystals.getValue() && this.crystalAttack && placementPos != null && this.pos != null && this.pos.equals(placementPos) && mc.world != null) {
         boolean crystalPresent = false;

         for(Entity entity : mc.world.loadedEntityList) {
            if (entity instanceof EntityEnderCrystal && !entity.isDead && entity.getPosition().equals(this.pos.up())) {
               crystalPresent = true;
               break;
            }
         }

         if (crystalPresent) {
            return true;
         } else {
            long now = System.currentTimeMillis();
            if (this.cevCrystalAttackedAt < 0L) {
               if (this.cevCrystalStartedAt >= 0L && now - this.cevCrystalStartedAt <= 750L) {
                  return true;
               }
            } else if (now - this.cevCrystalAttackedAt <= 125L) {
               return true;
            }

            this.crystalAttack = false;
            this.crystalRetries = 0;
            this.cevCrystalStartedAt = -1L;
            this.cevCrystalAttackedAt = -1L;
            return false;
         }
      } else {
         return false;
      }
   }

   public BlockPos getPos() {
      return this.pos;
   }

   public Block getBlock() {
      return this.pos == null ? Blocks.AIR : mc.world.getBlockState(this.pos).getBlock();
   }

   public boolean isBreaking() {
      return this.isBreaking && this.pos != null && this.getBlock() != Blocks.AIR;
   }

   public void abortCurrentPos(boolean packet) {
      if (Debug.getInstance().isEnabled() && (Boolean)Debug.getInstance().fastBreakDebug.getValue()) {
         Command.sendSilentMessage(TextFormatting.RED + "Aborting current position: " + this.pos);
      }

      if (packet) {
         PacketUtil.swing();
         PacketUtil.send(this.getAbortPacket());
         PacketUtil.swing();
      }

      ((IPlayerControllerMP)mc.playerController).setIsHittingBlock(false);
      ((IPlayerControllerMP)mc.playerController).setCurBlockDamageMP(0.0F);
      if (this.pos != null) {
         mc.world.sendBlockBreakProgress(mc.player.getEntityId(), this.pos, -1);
      }

      this.reset();
   }

   public void reset() {
      this.pos = null;
      this.direction = null;
      this.maxDamage = 0.0F;
      this.canBreak = false;
      this.crystalAttack = false;
      this.crystalRetries = 0;
      this.cevCrystalStartedAt = -1L;
      this.cevCrystalAttackedAt = -1L;
      this.render = false;
      this.sent = false;
      this.isBreaking = false;
      this.internalSwapActive = false;
      this.breakDelay = false;
      this.growAnim.reset(false);
      this.growDelta.reset(false);
      this.beginCounting = false;
      this.groundedTicks = 0;
      this.scheduledPos = null;
      this.scheduledFacing = null;
      this.airTicks = 0;
      this.brokenAwaitingReset = false;
      this.confirmedAirObserved = false;

      for(int i = 0; i < 9; ++i) {
         this.damages[i] = 0.0F;
      }

   }

   protected void softReset(boolean full) {
      if (full && this.pos != null && this.direction != null) {
         PacketUtil.send(new CPacketPlayerDigging(Action.START_DESTROY_BLOCK, this.pos, this.direction));
      }

      this.maxDamage = 0.0F;
      this.breakDelay = false;

      for(int i = 0; i < 9; ++i) {
         this.damages[i] = 0.0F;
      }

   }

   protected void updateDamages() {
      this.maxDamage = 0.0F;
      float rateScale = this.getSpeedMultiplierScale();

      for(int i = 0; i < 9; ++i) {
         ItemStack stack = mc.player.inventory.getStackInSlot(i);
         boolean onGround = this.resolveOnGround();
         float damage;
         if (this.getBlock() == Blocks.AIR && this.state != null) {
            damage = MineUtil.getDamage(this.state, stack, onGround);
         } else {
            damage = MineUtil.getDamage(stack, this.pos, onGround);
         }

         damage *= Vonware.serverManager.getFactor();
         damage *= rateScale;
         this.damages[i] = MathUtil.clamp(this.damages[i] + damage, 0.0F, 1.0F);
         if (this.damages[i] > this.maxDamage) {
            this.maxDamage = this.damages[i];
         }
      }

   }

   protected void tryBreak(int pickSlot) {
      this.breakLock = true;
      int lastSlot = mc.player.inventory.currentItem;
      float[] rotations = RotationUtil.getRotations(this.pos);
      EntityPlayer target = this.getPlacePlayer(this.pos);
      this.internalSwapActive = true;
      this.lastInternalSwapTime = System.currentTimeMillis();
      NCPManager.INSTANCE.startMultiClick();
      if (target != null && (Boolean)this.crystals.getValue() && mc.world.getBlockState(this.pos).getBlock() == Blocks.OBSIDIAN && BlockUtil.canPlaceCrystal(this.pos, false, (Boolean)this.newVerPlace.getValue()) && this.checkCrystalPos(this.pos)) {
         RayTraceResult result = RayTraceUtil.getRayTraceResult(rotations[0], rotations[1]);
         if (mc.player.getHeldItemOffhand() != ItemStack.EMPTY && mc.player.getHeldItemOffhand().getItem() == Items.END_CRYSTAL) {
            this.beginCevCrystalSync();
            InventoryUtil.syncItem();
            CPacketPlayerTryUseItemOnBlock placePacket = this.getPlacePacket(EnumHand.OFF_HAND, result);
            PacketUtil.send(placePacket);
            PacketUtil.send(placePacket);
            PacketUtil.send(new CPacketAnimation(EnumHand.OFF_HAND));
         } else {
            int crystalSlot = InventoryUtil.findHotbarItem(Items.END_CRYSTAL);
            if (crystalSlot != -1) {
               this.beginCevCrystalSync();
               this.swap(this.swap.getValue(), crystalSlot);
               this.lastInternalSwapTime = System.currentTimeMillis();
               CPacketPlayerTryUseItemOnBlock placePacket2 = this.getPlacePacket(EnumHand.MAIN_HAND, result);
               PacketUtil.send(placePacket2);
               PacketUtil.send(placePacket2);
               PacketUtil.swing();
               if (this.swap.getValue() == FastBreak.SwapEnum.Alternative) {
                  if (lastSlot != pickSlot) {
                     this.swap(this.swap.getValue(), crystalSlot);
                     this.lastInternalSwapTime = System.currentTimeMillis();
                  } else {
                     short id = mc.player.openContainer.getNextTransactionID(mc.player.inventory);
                     int slotC = InventoryUtil.hotbarToInventory(crystalSlot);
                     mc.player.openContainer.slotClick(slotC, lastSlot, ClickType.SWAP, mc.player);
                     PacketUtil.send(new CPacketClickWindow(0, slotC, lastSlot, ClickType.SWAP, new ItemStack(Blocks.DIAMOND_BLOCK, 64), id));
                  }
               } else if (this.swap.getValue() == FastBreak.SwapEnum.Bypass) {
                  if (lastSlot != pickSlot) {
                     this.swap(this.swap.getValue(), crystalSlot);
                     this.lastInternalSwapTime = System.currentTimeMillis();
                  } else {
                     InventoryUtil.switchToBypass(InventoryUtil.hotbarToInventory(crystalSlot), true);
                  }
               } else if (lastSlot == pickSlot) {
                  this.swap(this.swap.getValue(), lastSlot);
                  this.lastInternalSwapTime = System.currentTimeMillis();
               }
            }
         }
      }

      if (this.swap.getValue() != FastBreak.SwapEnum.None || lastSlot == pickSlot) {
         if (this.getBlock() != Blocks.AIR) {
            EnumFacing validFacing = BlockUtil.getMineableSide(this.pos, (Boolean)this.strict.getValue(), (Boolean)this.strict.getValue());
            if (validFacing != null) {
               this.direction = validFacing;
            } else if ((Boolean)this.strict.getValue() || this.direction == null) {
               this.internalSwapActive = false;
               NCPManager.INSTANCE.releaseMultiClick();
               return;
            }

            if (this.mode.getValue() == FastBreak.MineMode.Packet) {
               this.rotationTimer.reset();
            }

            ItemStack currentStack = mc.player.inventory.getStackInSlot(lastSlot);
            ItemStack pickStack = mc.player.inventory.getStackInSlot(pickSlot);
            boolean doSwitch = pickSlot != lastSlot && currentStack != pickStack;
            if (doSwitch) {
               this.swap(this.swap.getValue(), pickSlot);
               this.lastInternalSwapTime = System.currentTimeMillis();
            }

            PacketUtil.send(new CPacketPlayer(true));
            if (this.rotation.getValue() == FastBreak.RotationsEnum.Packet) {
               PacketUtil.rotate(rotations);
            }

            PacketUtil.swing();
            PacketUtil.send(new CPacketPlayerDigging(Action.STOP_DESTROY_BLOCK, this.pos, this.direction));
            PacketUtil.swing();
            PacketUtil.send(this.getAbortPacket());
            if (this.mode.getValue() == FastBreak.MineMode.Packet) {
               PacketUtil.swing();
               PacketUtil.send(new CPacketPlayerDigging(Action.START_DESTROY_BLOCK, this.pos, this.direction));
               if ((Boolean)this.fast.getValue()) {
                  PacketUtil.send(new CPacketPlayerDigging(Action.STOP_DESTROY_BLOCK, this.pos, this.direction));
               }
            }

            if (doSwitch) {
               if (this.swap.getValue() == FastBreak.SwapEnum.Alternative) {
                  short id2 = mc.player.openContainer.getNextTransactionID(mc.player.inventory);
                  int slot = InventoryUtil.hotbarToInventory(pickSlot);
                  mc.player.openContainer.slotClick(slot, lastSlot, ClickType.SWAP, mc.player);
                  PacketUtil.send(new CPacketClickWindow(0, slot, lastSlot, ClickType.SWAP, new ItemStack(Blocks.DIAMOND_BLOCK, 64), id2));
               } else if (this.swap.getValue() == FastBreak.SwapEnum.Bypass) {
                  InventoryUtil.switchToBypass(InventoryUtil.hotbarToInventory(pickSlot), true);
               } else {
                  this.swap(this.swap.getValue(), lastSlot);
                  this.lastInternalSwapTime = System.currentTimeMillis();
               }
            }

            this.beginCounting = true;
            this.lastAttemptedBreak = this.pos;
            this.lastAttemptedFacing = this.direction;
            this.lastConfirmedBroke = null;
            this.groundedTicks = 0;
            this.airTicks = 0;
         }

         if (this.mode.getValue() == FastBreak.MineMode.Packet) {
            this.softReset(false);
         }
      }

      this.internalSwapActive = false;
      NCPManager.INSTANCE.releaseMultiClick();
   }

   private CPacketPlayerTryUseItemOnBlock getPlacePacket(EnumHand hand, RayTraceResult result) {
      return new CPacketPlayerTryUseItemOnBlock(this.pos, result.sideHit, hand, 0.5F, 1.0F, 0.5F);
   }

   protected EntityPlayer getPlacePlayer(BlockPos pos) {
      for(EntityPlayer player : mc.world.playerEntities) {
         if (!Vonware.friendManager.isFriend(player) && player != mc.player && !EntityUtil.isInBurrow(player) && !EntityUtil.isDead(player)) {
            BlockPos playerPos = PositionUtil.getPosition(player);

            for(EnumFacing facing : EnumFacing.HORIZONTALS) {
               if (playerPos.offset(facing).equals(pos) || playerPos.up().offset(facing).equals(pos)) {
                  return player;
               }
            }

            if (playerPos.offset(EnumFacing.UP).offset(EnumFacing.UP).equals(pos)) {
               return player;
            }
         }
      }

      return null;
   }

   private boolean checkCrystalPos(BlockPos pos) {
      boolean canPlace = true;

      for(Entity entity : mc.world.getEntitiesWithinAABB(Entity.class, new AxisAlignedBB(pos.up()))) {
         if (entity != null && EntityUtil.isDead(entity)) {
            canPlace = false;
         }
      }

      return canPlace;
   }

   protected CPacketPlayerDigging getAbortPacket() {
      return new CPacketPlayerDigging(Action.ABORT_DESTROY_BLOCK, new BlockPos((int)mc.player.posX + 2, (int)mc.player.posY + 2, (int)mc.player.posZ + 2), EnumFacing.UP);
   }

   private void swap(SwapEnum swap, int slot) {
      switch (swap) {
         case Silent:
            InventoryUtil.switchTo(slot);
            break;
         case Alternative:
            InventoryUtil.switchToAlt(slot);
            break;
         case Bypass:
            InventoryUtil.switchToBypass(InventoryUtil.hotbarToInventory(slot), false);
      }

   }

   public boolean isBlockValid(Block block, BlockPos pos) {
      if (block != null && !(block instanceof BlockAir)) {
         float hardness = block.getBlockHardness(mc.world.getBlockState(pos), mc.world, pos);
         if (hardness < 0.0F) {
            return false;
         } else {
            switch ((BlockList)this.blockMode.getValue()) {
               case Minimal:
                  if (block != Blocks.OBSIDIAN && block != Blocks.ENDER_CHEST && block != Blocks.ANVIL && !(block instanceof BlockShulkerBox)) {
                     return false;
                  }

                  if ((Boolean)this.smartTerrain.getValue()) {
                     IBlockState state = mc.world.getBlockState(pos);
                     if (BlockUtil.isTerrainBlock(block, state)) {
                        return false;
                     }
                  }
                  break;
               case AllBlocks:
                  if ((Boolean)this.smartTerrain.getValue()) {
                     IBlockState state = mc.world.getBlockState(pos);
                     if (BlockUtil.isTerrainBlock(block, state)) {
                        return false;
                     }
                  }
            }

            return true;
         }
      } else {
         return false;
      }
   }

   public static enum GroundMode {
      Server,
      Vanilla;
   }

   public static enum MineMode {
      Packet,
      Instant;
   }

   public static enum RenderMode {
      Static,
      Grow,
      GrowEased;
   }

   public static enum ColorType {
      Damage,
      DamageTwo,
      DamageThree,
      Global;
   }

   public static enum SwapEnum {
      Silent,
      Alternative,
      Bypass,
      None;
   }

   public static enum RotationsEnum {
      None,
      Event,
      Packet;
   }

   public static enum BlockList {
      Minimal,
      AllBlocks;
   }
}
