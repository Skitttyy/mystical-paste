package fat.vonware.features.modules.combat;

import fat.vonware.Vonware;
import fat.vonware.event.events.MoveEvent;
import fat.vonware.event.events.Render3DEvent;
import fat.vonware.features.modules.Module;
import fat.vonware.features.modules.client.BlockRender3D;
import fat.vonware.features.setting.Setting;
import fat.vonware.util.BlockUtil;
import fat.vonware.util.EntityUtil;
import fat.vonware.util.InventoryUtil;
import fat.vonware.util.RotationUtil;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEnderChest;
import net.minecraft.block.BlockObsidian;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityEnderCrystal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemBlock;
import net.minecraft.network.play.client.CPacketAnimation;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.network.play.client.CPacketUseEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class BurrowBypass extends Module {
   public Setting<Boolean> render = new Setting<Boolean>("Render", true, "Renders the block position used by BurrowBypass.");
   public Setting<Boolean> multiPlace = new Setting<Boolean>("MultiPlace", true, "Attempts to place burrow blocks around multiple positions intersecting your player.");
   public Setting<Boolean> tpCenter = new Setting<Boolean>("TPCenter", false, "Centers you inside the current block before starting the burrow placement.");
   public Setting<Boolean> rotate = new Setting<Boolean>("Rotate", true, "Rotates toward the placement side before placing the burrow block.");
   public Setting<Boolean> packet = new Setting<Boolean>("PacketPlace", true, "Uses packet-based right-click placement for the burrow block.");
   public Setting<Boolean> strict = new Setting<Boolean>("Strict", false, "Uses stricter placement-side checks when finding a valid face.");
   public Setting<Boolean> raytrace = new Setting<Boolean>("RayTrace", true, "Requires the placement side to pass the raytrace check.");
   public Setting<JumpMode> jumpMode;
   public Setting<BypassMode> bypassMode;
   public Setting<RubberBandMode> rubberBand;
   public Setting<Double> offsetX;
   public Setting<Double> offsetY;
   public Setting<Double> offsetZ;
   public Setting<Boolean> head;
   public Setting<Boolean> onlyOnGround;
   public Setting<Boolean> air;
   public Setting<BlockMode> mode;
   public Setting<Boolean> packetSwitch;
   public Setting<Boolean> breakCrystal;
   public Setting<Boolean> testMode;
   public Setting<Boolean> move;
   private boolean moved;
   private final Vec3d[] offsets;
   private final List<BlockRender3D.SimpleRenderBlock> renderBlocks;

   public BurrowBypass() {
      super("BurrowBypass", "Bypasses burrow by placing block", Module.Category.COMBAT, true, false, false);
      this.jumpMode = (new Setting("JumpMode", BurrowBypass.JumpMode.NORMAL)).withDescription((Function<JumpMode, String>)((mode) -> {
         switch (mode) {
            case NORMAL:
               return "NORMAL: Uses the standard four-packet vertical jump sequence before placing.";
            case FUTURE:
               return "FUTURE: Uses a five-packet Future-style vertical movement sequence with slightly different height offsets.";
            case STRICT:
               return "STRICT: Uses a stricter five-packet vertical sequence with tighter movement offsets.";
            default:
               return "";
         }
      }));
      this.bypassMode = (new Setting("Bypass", BurrowBypass.BypassMode.NORMAL)).withDescription((Function<BypassMode, String>)((mode) -> {
         switch (mode) {
            case NORMAL:
               return "NORMAL: Moves toward the selected bypass position along one horizontal axis while slightly raising your Y position.";
            case MIDDLE:
               return "MIDDLE: Moves directly to the center of the selected bypass block while slightly raising your Y position.";
            case TEST:
               return "TEST: Uses a multi-packet movement sequence that progressively moves through and past the selected bypass position.";
            default:
               return "";
         }
      }));
      this.rubberBand = (new Setting("RubberBand", BurrowBypass.RubberBandMode.CN)).withDescription((Function<RubberBandMode, String>)((mode) -> {
         switch (mode) {
            case CN:
               return "CN: Rubberbands to the nearest valid air position more than three blocks away, falling back to Y -7 if none is found.";
            case FUTURE:
               return "FUTURE: Sends two upward position packets using Future-style Y offsets after placement.";
            case FUTURE_STRICT:
               return "FUTURE_STRICT: Sends a stricter three-packet Future-style upward rubberband sequence.";
            case TROLL:
               return "TROLL: Sends one large upward offset followed by a downward offset.";
            case STRICT:
               return "STRICT: Searches upward for the nearest valid air position more than five blocks away, falling back to Y -7 if none is found.";
            case VOID:
               return "VOID: Sends your server position directly down to Y -7.";
            case AUTO:
               return "AUTO: Searches vertically around you for an open two-block-high position and rubberbands there.";
            case CUSTOM:
               return "CUSTOM: Rubberbands using your configured OffsetX, OffsetY, and OffsetZ values.";
            case TEST:
               return "TEST: Does not send an additional rubberband packet after placement.";
            default:
               return "";
         }
      }));
      this.offsetX = new Setting<Double>("OffsetX", (double)-7.0F, (double)-10.0F, (double)10.0F, (v) -> this.rubberBand.getValue() == BurrowBypass.RubberBandMode.CUSTOM, "Horizontal X offset used by Custom rubberband mode.");
      this.offsetY = new Setting<Double>("OffsetY", (double)-7.0F, (double)-10.0F, (double)10.0F, (v) -> this.rubberBand.getValue() == BurrowBypass.RubberBandMode.CUSTOM, "Vertical Y offset used by Custom rubberband mode.");
      this.offsetZ = new Setting<Double>("OffsetZ", (double)-7.0F, (double)-10.0F, (double)10.0F, (v) -> this.rubberBand.getValue() == BurrowBypass.RubberBandMode.CUSTOM, "Horizontal Z offset used by Custom rubberband mode.");
      this.head = new Setting<Boolean>("Head", false, "Also attempts to place blocks around the player's head position when the bypass sequence allows it.");
      this.onlyOnGround = new Setting<Boolean>("OnGroundOnly", true, "Only allows BurrowBypass to start while you are on the ground.");
      this.air = new Setting<Boolean>("NotAir", false, "Disables BurrowBypass if the block directly below you is air.");
      this.mode = (new Setting("BlockMode", BurrowBypass.BlockMode.OBBY_ECHEST)).withDescription((Function<BlockMode, String>)((mode) -> {
         switch (mode) {
            case OBSIDIAN:
               return "OBSIDIAN: Only uses obsidian for the burrow placement.";
            case ECHEST:
               return "ECHEST: Only uses ender chests for the burrow placement.";
            case ECHEST_OBBY:
               return "ECHEST_OBBY: Prefers an ender chest and falls back to obsidian if no ender chest is available.";
            case OBBY_ECHEST:
               return "OBBY_ECHEST: Prefers obsidian and falls back to an ender chest if no obsidian is available.";
            default:
               return "";
         }
      }));
      this.packetSwitch = new Setting<Boolean>("PacketSwitch", false, "Uses packet switching when temporarily swapping to the required burrow block.");
      this.breakCrystal = new Setting<Boolean>("BreakCrystal", false, "Attacks nearby end crystals before attempting the burrow placement.");
      this.testMode = new Setting<Boolean>("TestMode", true, "Enables the alternate burrow checks and movement behavior used by the test implementation.");
      this.move = new Setting<Boolean>("Move", false, (v) -> (Boolean)this.testMode.getValue(), "Moves you toward a nearby valid burrow position before continuing the TestMode sequence.");
      this.offsets = new Vec3d[]{new Vec3d(0.3, (double)0.0F, 0.3), new Vec3d(-0.3, (double)0.0F, 0.3), new Vec3d(0.3, (double)0.0F, -0.3), new Vec3d(-0.3, (double)0.0F, -0.3)};
      this.renderBlocks = new ArrayList();
      this.register(this.render);
      this.register(this.multiPlace);
      this.register(this.tpCenter);
      this.register(this.rotate);
      this.register(this.packet);
      this.register(this.strict);
      this.register(this.raytrace);
      this.register(this.jumpMode);
      this.register(this.bypassMode);
      this.register(this.rubberBand);
      this.register(this.offsetX);
      this.register(this.offsetY);
      this.register(this.offsetZ);
      this.register(this.head);
      this.register(this.onlyOnGround);
      this.register(this.air);
      this.register(this.mode);
      this.register(this.packetSwitch);
      this.register(this.breakCrystal);
      this.register(this.testMode);
      this.register(this.move);
   }

   @SubscribeEvent
   public void onPlayerMove(MoveEvent event) {
      if (mc.player != null && mc.player.isEntityAlive() && !mc.player.isRiding() && !mc.player.capabilities.isFlying && !this.moved) {
         BlockPos blockPos = EntityUtil.getPlayerPos(mc.player);

         for(Vec3d vec3d : new Vec3d[]{new Vec3d(0.4, (double)0.0F, 0.4), new Vec3d(0.4, (double)0.0F, -0.4), new Vec3d(-0.4, (double)0.0F, 0.4), new Vec3d(-0.4, (double)0.0F, -0.4)}) {
            BlockPos pos = new BlockPos(mc.player.posX + vec3d.x, mc.player.posY, mc.player.posZ + vec3d.z);
            if (isAir(pos.down()) && mc.world.isAirBlock(pos) && mc.world.isAirBlock(pos.up()) && mc.world.isAirBlock(pos.up(2))) {
               blockPos = pos;
               break;
            }
         }

         double x = this.roundToClosest(mc.player.posX, (double)blockPos.getX() + 0.02, (double)blockPos.getX() + 0.98);
         double y = mc.player.posY;
         double z = this.roundToClosest(mc.player.posZ, (double)blockPos.getZ() + 0.02, (double)blockPos.getZ() + 0.98);
         Vec3d playerPos = mc.player.getPositionVector();
         float[] rotation = this.getRotationTo(playerPos, new Vec3d(x, y, z));
         double yawRad = Math.toRadians((double)rotation[0]);
         double dist = Math.hypot(x - playerPos.x, z - playerPos.z);
         if (x - playerPos.x == (double)0.0F && z - playerPos.z == (double)0.0F) {
            this.moved = true;
         }

         double playerSpeed = this.getBaseMoveSpeed();
         double speed = Math.min(dist, playerSpeed);
         event.setX(-Math.sin(yawRad) * speed);
         event.setZ(Math.cos(yawRad) * speed);
         if (Vonware.speedManager.getPlayerSpeed(mc.player) == (double)0.0F) {
            this.moved = true;
         }
      }

   }

   public void onEnable() {
      this.moved = !(Boolean)this.move.getValue();
      if ((Boolean)this.onlyOnGround.getValue() && !mc.player.onGround) {
         this.disable();
      } else if ((Boolean)this.air.getValue() && mc.world.getBlockState(getFlooredPosition(mc.player).offset(EnumFacing.DOWN)).getBlock().equals(Blocks.AIR)) {
         this.disable();
      }

   }

   public void onUpdate() {
      if (fullNullCheck()) {
         this.disable();
      } else {
         BlockPos playerPos = new BlockPos(mc.player.posX, mc.player.posY + (double)0.5F, mc.player.posZ);
         Vec3d vecPos = new Vec3d(mc.player.posX, (double)((int)(mc.player.posY + (double)0.5F)), mc.player.posZ);
         int slot = -1;
         switch ((BlockMode)this.mode.getValue()) {
            case OBSIDIAN:
               slot = this.findHotbarBlock(BlockObsidian.class);
               break;
            case ECHEST:
               slot = this.findHotbarBlock(BlockEnderChest.class);
               break;
            case ECHEST_OBBY:
               slot = this.findHotbarBlock(BlockEnderChest.class);
               if (slot == -1) {
                  slot = this.findHotbarBlock(BlockObsidian.class);
               }
               break;
            case OBBY_ECHEST:
               slot = this.findHotbarBlock(BlockObsidian.class);
               if (slot == -1) {
                  slot = this.findHotbarBlock(BlockEnderChest.class);
               }
         }

         if (slot == -1) {
            this.disable();
         } else {
            if ((Boolean)this.testMode.getValue()) {
               if (!this.moved) {
                  return;
               }

               boolean burrow = false;

               for(Vec3d vec3d : this.offsets) {
                  if (!this.isPos2(new BlockPos(vecPos.add(vec3d)), playerPos)) {
                     burrow = true;
                     break;
                  }
               }

               if (!burrow) {
                  this.disable();
                  return;
               }
            }

            if ((Boolean)this.breakCrystal.getValue()) {
               this.breakCrystalNearby();
            }

            if (mc.world.isBlockLoaded(mc.player.getPosition()) && !mc.player.isInWater() && !mc.player.isInLava() && !mc.player.isInWeb) {
               if ((Boolean)this.tpCenter.getValue()) {
                  this.centerPlayer();
               }

               boolean bypassed;
               if (this.fakeBBoxCheck()) {
                  bypassed = false;
                  this.sendJumpPackets();
               } else {
                  if ((!(Boolean)this.testMode.getValue() || this.bypassBurrowed()) && (BlockUtil.canReplace(playerPos) && BlockUtil.canReplace(playerPos.up()) || !this.intersect(playerPos.up()))) {
                     List<BlockPos> posList = new ArrayList();
                     List<BlockPos> airList = new ArrayList();
                     if ((Boolean)this.testMode.getValue()) {
                        airList.add(playerPos);

                        for(Vec3d vec : this.offsets) {
                           BlockPos pos = new BlockPos(vecPos.add(vec));
                           if (isAir(pos)) {
                              posList.add(pos);
                           }
                        }
                     } else {
                        for(Vec3d vecx : this.offsets) {
                           boolean airCheck = true;
                           BlockPos pos = new BlockPos(vecPos.add(vecx));

                           for(int i = 0; i < 2; ++i) {
                              BlockPos blockPos = pos.down(i);
                              if (!isAir(blockPos)) {
                                 airCheck = false;
                              }
                           }

                           if (this.intersect(pos) && !airCheck) {
                              posList.add(pos);
                           } else {
                              airList.add(pos);
                           }
                        }
                     }

                     BlockPos movePos = posList.isEmpty() ? airList.stream().min(Comparator.comparing((p) -> mc.player.getDistance((double)p.getX() + (double)0.5F, mc.player.posY, (double)p.getZ() + (double)0.5F))).orElse(null) : posList.stream().min(Comparator.comparing((p) -> mc.player.getDistance((double)p.getX() + (double)0.5F, mc.player.posY, (double)p.getZ() + (double)0.5F))).orElse(null);
                     this.gotoPos(movePos);
                  } else {
                     this.gotoPos(playerPos);
                  }

                  bypassed = true;
               }

               this.runWithSlot(slot, () -> {
                  if (!(Boolean)this.multiPlace.getValue()) {
                     this.placeBlock(new BlockPos(this.getPlayerPosFixY(mc.player)));
                  } else {
                     for(Vec3d vec3dxx : this.offsets) {
                        this.placeBlock(vecPos.add(vec3dxx));
                     }

                     if ((Boolean)this.head.getValue() && bypassed) {
                        for(Vec3d vec3dx : this.offsets) {
                           this.placeBlock(vecPos.add(vec3dx).add((double)0.0F, (double)1.0F, (double)0.0F));
                        }
                     }
                  }

               });
               this.sendRubberBandPackets();
               this.disable();
            } else {
               this.disable();
            }
         }
      }

   }

   private void sendJumpPackets() {
      switch ((JumpMode)this.jumpMode.getValue()) {
         case NORMAL:
            mc.player.connection.sendPacket(new CPacketPlayer.Position(mc.player.posX, mc.player.posY + 0.419999986886978, mc.player.posZ, false));
            mc.player.connection.sendPacket(new CPacketPlayer.Position(mc.player.posX, mc.player.posY + 0.7531999805212015, mc.player.posZ, false));
            mc.player.connection.sendPacket(new CPacketPlayer.Position(mc.player.posX, mc.player.posY + 1.001335979112147, mc.player.posZ, false));
            mc.player.connection.sendPacket(new CPacketPlayer.Position(mc.player.posX, mc.player.posY + 1.166109260938214, mc.player.posZ, false));
            break;
         case FUTURE:
            mc.player.connection.sendPacket(new CPacketPlayer.Position(mc.player.posX, mc.player.posY + 0.419997486886978, mc.player.posZ, false));
            mc.player.connection.sendPacket(new CPacketPlayer.Position(mc.player.posX, mc.player.posY + 0.7500025, mc.player.posZ, false));
            mc.player.connection.sendPacket(new CPacketPlayer.Position(mc.player.posX, mc.player.posY + 0.999995, mc.player.posZ, false));
            mc.player.connection.sendPacket(new CPacketPlayer.Position(mc.player.posX, mc.player.posY + 1.170005001788139, mc.player.posZ, false));
            mc.player.connection.sendPacket(new CPacketPlayer.Position(mc.player.posX, mc.player.posY + 1.2426050013947485, mc.player.posZ, false));
            break;
         case STRICT:
            mc.player.connection.sendPacket(new CPacketPlayer.Position(mc.player.posX, mc.player.posY + 0.419998586886978, mc.player.posZ, false));
            mc.player.connection.sendPacket(new CPacketPlayer.Position(mc.player.posX, mc.player.posY + 0.7500014, mc.player.posZ, false));
            mc.player.connection.sendPacket(new CPacketPlayer.Position(mc.player.posX, mc.player.posY + 0.9999972, mc.player.posZ, false));
            mc.player.connection.sendPacket(new CPacketPlayer.Position(mc.player.posX, mc.player.posY + 1.170002801788139, mc.player.posZ, false));
            mc.player.connection.sendPacket(new CPacketPlayer.Position(mc.player.posX, mc.player.posY + 1.170009801788139, mc.player.posZ, false));
      }

   }

   private void sendRubberBandPackets() {
      switch ((RubberBandMode)this.rubberBand.getValue()) {
         case CN:
            double distance = (double)0.0F;
            BlockPos bestPos = null;

            for(BlockPos pos : BlockUtil.getSphere(EntityUtil.getPlayerPos(mc.player), 6.0F, 6, false, true, 0)) {
               if (this.canGoTo(pos) && !(mc.player.getDistance((double)pos.getX() + (double)0.5F, (double)pos.getY() + (double)0.5F, (double)pos.getZ() + (double)0.5F) <= (double)3.0F) && (bestPos == null || !(mc.player.getDistance((double)pos.getX() + (double)0.5F, (double)pos.getY() + (double)0.5F, (double)pos.getZ() + (double)0.5F) >= distance))) {
                  bestPos = pos;
                  distance = mc.player.getDistance((double)pos.getX() + (double)0.5F, (double)pos.getY() + (double)0.5F, (double)pos.getZ() + (double)0.5F);
               }
            }

            if (bestPos != null) {
               mc.player.connection.sendPacket(new CPacketPlayer.Position((double)bestPos.getX() + (double)0.5F, (double)bestPos.getY(), (double)bestPos.getZ() + (double)0.5F, false));
            } else {
               mc.player.connection.sendPacket(new CPacketPlayer.Position(mc.player.posX, (double)-7.0F, mc.player.posZ, false));
            }
            break;
         case FUTURE:
            mc.player.connection.sendPacket(new CPacketPlayer.Position(mc.player.posX, mc.player.posY + 1.242609801394749, mc.player.posZ, false));
            mc.player.connection.sendPacket(new CPacketPlayer.Position(mc.player.posX, mc.player.posY + 2.340028003576279, mc.player.posZ, false));
            break;
         case FUTURE_STRICT:
            mc.player.connection.sendPacket(new CPacketPlayer.Position(mc.player.posX, mc.player.posY + 1.315205001001358, mc.player.posZ, false));
            mc.player.connection.sendPacket(new CPacketPlayer.Position(mc.player.posX, mc.player.posY + 1.315205001001358, mc.player.posZ, false));
            mc.player.connection.sendPacket(new CPacketPlayer.Position(mc.player.posX, mc.player.posY + 2.485225002789497, mc.player.posZ, false));
            break;
         case TROLL:
            mc.player.connection.sendPacket(new CPacketPlayer.Position(mc.player.posX, mc.player.posY + 3.3400880035762786, mc.player.posZ, false));
            mc.player.connection.sendPacket(new CPacketPlayer.Position(mc.player.posX, mc.player.posY - (double)1.0F, mc.player.posZ, false));
            break;
         case STRICT:
            double strictDistance = (double)0.0F;
            BlockPos strictBestPos = null;

            for(int j = 0; j < 20; ++j) {
               BlockPos posx = new BlockPos(mc.player.posX, mc.player.posY + (double)0.5F + (double)j, mc.player.posZ);
               if (this.canGoTo(posx) && mc.player.getDistance((double)posx.getX() + (double)0.5F, (double)posx.getY() + (double)0.5F, (double)posx.getZ() + (double)0.5F) > (double)5.0F && (strictBestPos == null || mc.player.getDistance((double)posx.getX() + (double)0.5F, (double)posx.getY() + (double)0.5F, (double)posx.getZ() + (double)0.5F) < strictDistance)) {
                  strictBestPos = posx;
                  strictDistance = mc.player.getDistance((double)posx.getX() + (double)0.5F, (double)posx.getY() + (double)0.5F, (double)posx.getZ() + (double)0.5F);
               }
            }

            if (strictBestPos != null) {
               mc.player.connection.sendPacket(new CPacketPlayer.Position((double)strictBestPos.getX() + (double)0.5F, (double)strictBestPos.getY(), (double)strictBestPos.getZ() + (double)0.5F, false));
            } else {
               mc.player.connection.sendPacket(new CPacketPlayer.Position(mc.player.posX, (double)-7.0F, mc.player.posZ, false));
            }
            break;
         case VOID:
            mc.player.connection.sendPacket(new CPacketPlayer.Position(mc.player.posX, (double)-7.0F, mc.player.posZ, false));
            break;
         case AUTO:
            for(int i = -10; i < 10; ++i) {
               if (i == -1) {
                  i = 4;
               }

               if (mc.world.getBlockState(getFlooredPosition(mc.player).add(0, i, 0)).getBlock().equals(Blocks.AIR) && mc.world.getBlockState(getFlooredPosition(mc.player).add(0, i + 1, 0)).getBlock().equals(Blocks.AIR)) {
                  BlockPos posx = getFlooredPosition(mc.player).add(0, i, 0);
                  mc.player.connection.sendPacket(new CPacketPlayer.Position((double)posx.getX() + 0.3, (double)posx.getY(), (double)posx.getZ() + 0.3, false));
                  return;
               }
            }
            break;
         case CUSTOM:
            mc.player.connection.sendPacket(new CPacketPlayer.Position(mc.player.posX + (Double)this.offsetX.getValue(), mc.player.posY + (Double)this.offsetY.getValue(), mc.player.posZ + (Double)this.offsetZ.getValue(), false));
         case TEST:
      }

   }

   private void gotoPos(BlockPos pos) {
      switch ((BypassMode)this.bypassMode.getValue()) {
         case NORMAL:
            if (Math.abs((double)pos.getX() + (double)0.5F - mc.player.posX) < Math.abs((double)pos.getZ() + (double)0.5F - mc.player.posZ)) {
               mc.player.connection.sendPacket(new CPacketPlayer.Position(mc.player.posX, mc.player.posY + 0.2, (double)pos.getZ() + (double)0.5F, true));
            } else {
               mc.player.connection.sendPacket(new CPacketPlayer.Position((double)pos.getX() + (double)0.5F, mc.player.posY + 0.2, mc.player.posZ, true));
            }
            break;
         case MIDDLE:
            mc.player.connection.sendPacket(new CPacketPlayer.Position((double)pos.getX() + (double)0.5F, mc.player.posY + 0.2, (double)pos.getZ() + (double)0.5F, true));
            break;
         case TEST:
            mc.player.connection.sendPacket(new CPacketPlayer.Position(mc.player.posX + ((double)pos.getX() + (double)0.5F - mc.player.posX) * 0.42132, mc.player.posY + 0.12160004615784, mc.player.posZ + ((double)pos.getZ() + (double)0.5F - mc.player.posZ) * 0.42132, false));
            mc.player.connection.sendPacket(new CPacketPlayer.Position(mc.player.posX + ((double)pos.getX() + (double)0.5F - mc.player.posX) * 0.95, mc.player.posY + 0.200000047683716, mc.player.posZ + ((double)pos.getZ() + (double)0.5F - mc.player.posZ) * 0.95, false));
            mc.player.connection.sendPacket(new CPacketPlayer.Position(mc.player.posX + ((double)pos.getX() + (double)0.5F - mc.player.posX) * 1.03, mc.player.posY + 0.200000047683716, mc.player.posZ + ((double)pos.getZ() + (double)0.5F - mc.player.posZ) * 1.03, false));
            mc.player.connection.sendPacket(new CPacketPlayer.Position(mc.player.posX + ((double)pos.getX() + (double)0.5F - mc.player.posX) * 1.0933, mc.player.posY + 0.12160004615784, mc.player.posZ + ((double)pos.getZ() + (double)0.5F - mc.player.posZ) * 1.0933, false));
      }

   }

   private void breakCrystalNearby() {
      for(Entity crystal : mc.world.loadedEntityList.stream().filter((e) -> e instanceof EntityEnderCrystal && !e.isDead).sorted(Comparator.comparing((e) -> mc.player.getDistance(e))).collect(Collectors.toList())) {
         mc.player.connection.sendPacket(new CPacketUseEntity(crystal));
         mc.player.connection.sendPacket(new CPacketAnimation(EnumHand.OFF_HAND));
      }

   }

   private void runWithSlot(int slot, Runnable runnable) {
      int oldslot = mc.player.inventory.currentItem;
      if (slot >= 0 && slot != oldslot) {
         InventoryUtil.switchToHotbarSlot(slot, (Boolean)this.packetSwitch.getValue());
         runnable.run();
         InventoryUtil.switchToHotbarSlot(oldslot, (Boolean)this.packetSwitch.getValue());
         mc.player.inventoryContainer.detectAndSendChanges();
      } else {
         runnable.run();
      }

   }

   private void placeBlock(BlockPos pos) {
      EnumFacing side = BlockUtil.getFirstFacing(pos, (Boolean)this.strict.getValue(), (Boolean)this.raytrace.getValue());
      if (side != null) {
         BlockPos neighbour = pos.offset(side);
         EnumFacing opposite = side.getOpposite();
         Vec3d hitVec = (new Vec3d(neighbour)).add((double)0.5F, (double)0.5F, (double)0.5F).add((new Vec3d(opposite.getDirectionVec())).scale((double)0.5F));
         if ((Boolean)this.rotate.getValue()) {
            RotationUtil.faceVector(hitVec, true);
         }

         BlockUtil.rightClickBlock(neighbour, hitVec, EnumHand.MAIN_HAND, opposite, (Boolean)this.packet.getValue());
         this.addRenderBlock(neighbour);
      }

   }

   private void placeBlock(Vec3d vec3d) {
      BlockPos pos = new BlockPos(vec3d);
      if (!(Boolean)this.testMode.getValue() || this.bypassBurrowed() && (Boolean)this.head.getValue() || !this.isPos2(pos, EntityUtil.getPlayerPos(mc.player))) {
         this.placeBlock(pos);
      }

   }

   private boolean intersect(BlockPos pos) {
      AxisAlignedBB box = new AxisAlignedBB(pos);
      return box != null && mc.player.getEntityBoundingBox().intersects(box);
   }

   private boolean fakeBBoxCheck() {
      Vec3d playerPos = mc.player.getPositionVector();
      playerPos = new Vec3d(playerPos.x, (double)((int)(playerPos.y + (double)0.5F)), playerPos.z);

      for(Vec3d vec : this.offsets) {
         for(int i = 0; i < 3; ++i) {
            BlockPos pos = new BlockPos(playerPos.add(vec).add((double)0.0F, (double)i, (double)0.0F));
            if ((i >= 2 || this.intersect(pos)) && !isAir(pos)) {
               return false;
            }
         }
      }

      return true;
   }

   private boolean bypassBurrowed() {
      Vec3d pos = new Vec3d(mc.player.posX, (double)((int)(mc.player.posY + (double)0.5F)), mc.player.posZ);

      for(Vec3d vec3d : this.offsets) {
         if (!isAir((new BlockPos(pos.add(vec3d))).up())) {
            return true;
         }
      }

      return false;
   }

   private boolean canGoTo(BlockPos pos) {
      return isAir(pos) && isAir(pos.up());
   }

   private double roundToClosest(double num, double low, double high) {
      double d2 = high - num;
      double d1 = num - low;
      return d2 > d1 ? low : high;
   }

   private void centerPlayer() {
      double x = Math.floor(mc.player.posX) + (double)0.5F;
      double z = Math.floor(mc.player.posZ) + (double)0.5F;
      mc.player.connection.sendPacket(new CPacketPlayer.Position(x, mc.player.posY, z, mc.player.onGround));
      mc.player.setPosition(x, mc.player.posY, z);
   }

   private float[] getRotationTo(Vec3d posFrom, Vec3d posTo) {
      Vec3d diff = posTo.subtract(posFrom);
      double lengthXZ = Math.hypot(diff.x, diff.z);
      double yaw = this.normalizeAngle(Math.toDegrees(Math.atan2(diff.z, diff.x)) - (double)90.0F);
      double pitch = this.normalizeAngle(Math.toDegrees(-Math.atan2(diff.y, lengthXZ)));
      return new float[]{(float)yaw, (float)pitch};
   }

   private double normalizeAngle(double angle) {
      angle %= (double)360.0F;
      if (angle >= (double)180.0F) {
         angle -= (double)360.0F;
      }

      if (angle < (double)-180.0F) {
         angle += (double)360.0F;
      }

      return angle;
   }

   private double getBaseMoveSpeed() {
      double result = 0.2873;
      if (mc.player.isPotionActive(MobEffects.SPEED)) {
         result += 0.2873 * (double)(mc.player.getActivePotionEffect(MobEffects.SPEED).getAmplifier() + 1) * 0.2;
      }

      if (mc.player.isPotionActive(MobEffects.SLOWNESS)) {
         result -= 0.2873 * (double)(mc.player.getActivePotionEffect(MobEffects.SLOWNESS).getAmplifier() + 1) * 0.15;
      }

      return result;
   }

   private int findHotbarBlock(Class<?> clazz) {
      for(int i = 0; i < 9; ++i) {
         if (!mc.player.inventory.getStackInSlot(i).isEmpty() && mc.player.inventory.getStackInSlot(i).getItem() instanceof ItemBlock) {
            Block block = ((ItemBlock)mc.player.inventory.getStackInSlot(i).getItem()).getBlock();
            if (clazz.isInstance(block)) {
               return i;
            }
         }
      }

      return -1;
   }

   private static boolean isAir(BlockPos pos) {
      return BlockUtil.canReplace(pos);
   }

   private boolean isPos2(BlockPos pos1, BlockPos pos2) {
      return pos1 != null && pos2 != null && pos1.getX() == pos2.getX() && pos1.getY() == pos2.getY() && pos1.getZ() == pos2.getZ();
   }

   private BlockPos getPlayerPosFixY(EntityPlayer player) {
      return new BlockPos(Math.floor(player.posX), (double)Math.round(player.posY), Math.floor(player.posZ));
   }

   private static BlockPos getFlooredPosition(Entity entity) {
      return new BlockPos(Math.floor(entity.posX), (double)Math.round(entity.posY), Math.floor(entity.posZ));
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

   public static enum BlockMode {
      OBSIDIAN,
      ECHEST,
      OBBY_ECHEST,
      ECHEST_OBBY;
   }

   public static enum BypassMode {
      NORMAL,
      MIDDLE,
      TEST;
   }

   public static enum JumpMode {
      NORMAL,
      FUTURE,
      STRICT;
   }

   public static enum RubberBandMode {
      CN,
      STRICT,
      FUTURE,
      FUTURE_STRICT,
      TROLL,
      VOID,
      AUTO,
      TEST,
      CUSTOM;
   }
}
