package fat.vonware.util;

import fat.vonware.ducks.IPlayerControllerMP;
import fat.vonware.util.HoleFill.other.Ray;
import fat.vonware.util.HoleFill.other.RayTraceUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.block.BlockClay;
import net.minecraft.block.BlockConcretePowder;
import net.minecraft.block.BlockDirt;
import net.minecraft.block.BlockFarmland;
import net.minecraft.block.BlockGrass;
import net.minecraft.block.BlockGrassPath;
import net.minecraft.block.BlockGravel;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.BlockMycelium;
import net.minecraft.block.BlockNetherrack;
import net.minecraft.block.BlockOre;
import net.minecraft.block.BlockRedstoneOre;
import net.minecraft.block.BlockSand;
import net.minecraft.block.BlockSnow;
import net.minecraft.block.BlockSoulSand;
import net.minecraft.block.BlockStone;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityEnderCrystal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.network.play.client.CPacketEntityAction;
import net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock;
import net.minecraft.network.play.client.CPacketEntityAction.Action;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class BlockUtil implements Util {
   public static List<BlockPos> tickCache;
   public static final List<Block> resistantBlocks;
   public static final List<Block> unbreakableBlocks;
   public static final List<Block> blackList;
   public static final List<Block> shulkerList;

   public static EnumFacing getMineableSide(BlockPos pos, boolean strict, boolean strictSide) {
      for(EnumFacing side : EnumFacing.values()) {
         if (strictSide) {
            BlockPos neighbour = pos.offset(side);
            if (mc.world.getBlockState(neighbour).getBlock().canCollideCheck(mc.world.getBlockState(neighbour), false) && !(new AxisAlignedBB(pos)).contains(mc.player.getPositionEyes(mc.getRenderPartialTicks()))) {
               continue;
            }
         }

         if (!strict || canSeeFace(pos, side)) {
            return side;
         }
      }

      return null;
   }

   public static boolean isTerrainBlock(Block block, IBlockState state) {
      if (block != null && state != null) {
         if (!(block instanceof BlockDirt) && !(block instanceof BlockGrass) && !(block instanceof BlockSand) && !(block instanceof BlockGravel) && !(block instanceof BlockClay) && !(block instanceof BlockFarmland) && !(block instanceof BlockMycelium) && !(block instanceof BlockSnow) && !(block instanceof BlockSoulSand) && !(block instanceof BlockGrassPath) && !(block instanceof BlockConcretePowder) && !(block instanceof BlockOre) && !(block instanceof BlockRedstoneOre) && !(block instanceof BlockStone) && !(block instanceof BlockNetherrack)) {
            if (block != Blocks.STONE) {
               return false;
            } else {
               try {
                  int meta = block.getMetaFromState(state);
                  return meta >= 1 && meta <= 7;
               } catch (Exception var3) {
                  return false;
               }
            }
         } else {
            return true;
         }
      } else {
         return false;
      }
   }

   public static boolean placeTrace(BlockPos pos) {
      for(EnumFacing facing : EnumFacing.values()) {
         Ray ray = RayTraceUtil.rayTrace(mc.player, pos, facing, mc.world, Blocks.OBSIDIAN.getDefaultState(), (double)-1.0F);
         if (ray.isLegit()) {
            return false;
         }
      }

      return true;
   }

   public static boolean canPlaceBlock(BlockPos pos) {
      boolean allow = mc.world.getBlockState(pos).getMaterial().isReplaceable();
      Iterator<Entity> iterator = mc.world.getEntitiesWithinAABB(Entity.class, new AxisAlignedBB(pos)).iterator();
      if (iterator.hasNext()) {
         Entity entity = (Entity)iterator.next();
         allow = false;
      }

      return allow;
   }

   public static EnumFacing getPlaceableSideCrystal(BlockPos pos, boolean strict) {
      for(EnumFacing side : EnumFacing.values()) {
         BlockPos neighbour = pos.offset(side);
         IBlockState blockState = mc.world.getBlockState(neighbour);
         if ((!strict || canSeeFace(pos, side) && blockState.getMaterial().isReplaceable()) && (!strict || !mc.world.getBlockState(neighbour).getBlock().canCollideCheck(mc.world.getBlockState(neighbour), false))) {
            return side;
         }
      }

      return null;
   }

   public static boolean isReplacable(BlockPos pos) {
      return mc.world.getBlockState(pos).getMaterial().isReplaceable();
   }

   public static boolean packetPlaceBlock(BlockPos pos, boolean sneak, EnumFacing side) {
      Block block = mc.world.getBlockState(pos).getBlock();
      if (!(block instanceof BlockAir) && !(block instanceof BlockLiquid)) {
         return false;
      } else if (side == null) {
         return false;
      } else {
         BlockPos neighbour = pos.offset(side);
         EnumFacing opposite = side.getOpposite();
         Vec3d hitVec = (new Vec3d(neighbour)).add((double)0.5F, (double)0.5F, (double)0.5F).add((new Vec3d(opposite.getDirectionVec())).scale((double)0.5F));
         if (!mc.player.isSneaking()) {
            mc.getConnection().sendPacket(new CPacketEntityAction(mc.player, Action.START_SNEAKING));
         }

         rightClickPacket(neighbour, opposite, hitVec, EnumHand.MAIN_HAND);
         if (!mc.player.isSneaking()) {
            mc.getConnection().sendPacket(new CPacketEntityAction(mc.player, Action.STOP_SNEAKING));
         }

         mc.player.swingArm(EnumHand.MAIN_HAND);
         tickCache.add(pos);
         return true;
      }
   }

   public static void rightClickPacket(BlockPos pos, EnumFacing direction, Vec3d vec, EnumHand hand) {
      ((IPlayerControllerMP)mc.playerController).syncItem();
      float f = (float)(vec.x - (double)pos.getX());
      float f2 = (float)(vec.y - (double)pos.getY());
      float f3 = (float)(vec.z - (double)pos.getZ());
      mc.player.connection.sendPacket(new CPacketPlayerTryUseItemOnBlock(pos, direction, hand, f, f2, f3));
   }

   public static boolean isInterceptedByOtherPlayer(BlockPos pos) {
      for(Entity entity : mc.world.loadedEntityList) {
         if (entity instanceof EntityPlayer && entity != mc.player && (new AxisAlignedBB(pos)).intersects(entity.getEntityBoundingBox())) {
            return true;
         }
      }

      return false;
   }

   public static boolean isInterceptedByCrystal(BlockPos pos) {
      for(Entity entity : mc.world.loadedEntityList) {
         if (entity instanceof EntityEnderCrystal && (new AxisAlignedBB(pos)).intersects(entity.getEntityBoundingBox())) {
            return true;
         }
      }

      return false;
   }

   public static EnumFacing getPlaceableSide(BlockPos pos, boolean strict) {
      for(EnumFacing side : EnumFacing.values()) {
         BlockPos neighbour = pos.offset(side);
         if (mc.world.getBlockState(neighbour).getBlock().canCollideCheck(mc.world.getBlockState(neighbour), false)) {
            IBlockState blockState = mc.world.getBlockState(neighbour);
            if ((!strict || canSeeFace(neighbour, side.getOpposite())) && !blockState.getMaterial().isReplaceable()) {
               return side;
            }
         }
      }

      return null;
   }

   public static boolean placeBlockFill(BlockPos pos, EnumFacing side) {
      Block block = mc.world.getBlockState(pos).getBlock();
      if (!(block instanceof BlockAir) && !(block instanceof BlockLiquid)) {
         return false;
      } else if (side == null) {
         return false;
      } else {
         BlockPos neighbour = pos.offset(side);
         EnumFacing opposite = side.getOpposite();
         Vec3d hitVec = (new Vec3d(neighbour)).add((double)0.5F, (double)0.5F, (double)0.5F).add((new Vec3d(opposite.getDirectionVec())).scale((double)0.5F));
         EnumActionResult action = mc.playerController.processRightClickBlock(mc.player, mc.world, neighbour, opposite, hitVec, EnumHand.MAIN_HAND);
         mc.player.swingArm(EnumHand.MAIN_HAND);
         tickCache.add(pos);
         return action == EnumActionResult.SUCCESS;
      }
   }

   public static boolean canSeeFace(BlockPos target, EnumFacing face) {
      Vec3d eyes = mc.player.getPositionEyes(mc.timer.renderPartialTicks);
      Vec3d centered = new Vec3d((double)target.getX() + (double)0.5F, (double)target.getY() + (double)0.5F, (double)target.getZ() + (double)0.5F);
      Vec3i dir = face.getDirectionVec();
      Vec3d offset = new Vec3d((double)dir.getX(), (double)dir.getY(), (double)dir.getZ());
      Vec3d scaled = new Vec3d(offset.x * (double)0.5F, offset.y * (double)0.5F, offset.z * (double)0.5F);
      Vec3d pos = centered.add(scaled);
      switch (face) {
         case NORTH:
            return eyes.z < pos.z;
         case EAST:
            return eyes.x > pos.x;
         case SOUTH:
            return eyes.z > pos.z;
         case WEST:
            return eyes.x < pos.x;
         case UP:
            return eyes.y + (double)0.5F > pos.y;
         case DOWN:
            return eyes.y < pos.y;
         default:
            return false;
      }
   }

   public static double getDistanceSq(BlockPos pos) {
      return getDistanceSq((Entity)mc.player, pos);
   }

   public static double getDistanceSq(BlockPos from, BlockPos to) {
      return from.distanceSqToCenter((double)to.getX(), (double)to.getY(), (double)to.getZ());
   }

   public static double getDistanceSq(Entity from, BlockPos to) {
      return from.getDistanceSqToCenter(to);
   }

   public static boolean canPlaceCrystal(BlockPos pos, boolean check, boolean newVer) {
      BlockPos boost = pos.add(0, 1, 0);
      if (mc.world.getBlockState(pos).getBlock() != Blocks.BEDROCK && mc.world.getBlockState(pos).getBlock() != Blocks.OBSIDIAN) {
         return false;
      } else {
         BlockPos boost2 = pos.add(0, 2, 0);
         if ((mc.world.getBlockState(boost).getBlock() != Blocks.AIR || mc.world.getBlockState(boost2).getBlock() != Blocks.AIR) && !newVer) {
            return false;
         } else {
            for(Entity entity : mc.world.getEntitiesWithinAABB(Entity.class, new AxisAlignedBB(boost))) {
               if (!EntityUtil.isDead(entity) && !(entity instanceof EntityEnderCrystal)) {
                  return false;
               }
            }

            if (check) {
               for(Entity entity : mc.world.getEntitiesWithinAABB(Entity.class, new AxisAlignedBB(boost2))) {
                  if (!EntityUtil.isDead(entity) && !(entity instanceof EntityEnderCrystal)) {
                     return false;
                  }
               }
            }

            return true;
         }
      }
   }

   @SubscribeEvent
   public void onUpdate(TickEvent.ClientTickEvent event) {
      tickCache = new ArrayList();
   }

   public static List<BlockPos> getCircle(BlockPos pos, float r) {
      ArrayList<BlockPos> circleblocks = new ArrayList();
      int cx = pos.getX();
      int cy = pos.getY();
      int cz = pos.getZ();

      for(int x = cx - (int)r; (float)x <= (float)cx + r; ++x) {
         for(int z = cz - (int)r; (float)z <= (float)cz + r; ++z) {
            double dist = (double)((cx - x) * (cx - x) + (cz - z) * (cz - z));
            if (dist < (double)(r * r)) {
               BlockPos position = new BlockPos(x, cy, z);
               circleblocks.add(position);
            }
         }
      }

      return circleblocks;
   }

   public static void breakCrystalInWay(BlockPos pos) {
      mc.world.loadedEntityList.stream().filter((entity) -> entity instanceof EntityEnderCrystal).filter((entity) -> entity.getDistanceSq(pos) <= (double)1.5F).forEach((entity) -> mc.playerController.attackEntity(mc.player, entity));
   }

   public static NonNullList<BlockPos> getBox(float range) {
      NonNullList positions = NonNullList.create();
      positions.addAll(getSphere(new BlockPos(Math.floor(mc.player.posX), Math.floor(mc.player.posY), Math.floor(mc.player.posZ)), range, 0, false, true, 0));
      return positions;
   }

   public static NonNullList<BlockPos> getBox(float range, BlockPos pos) {
      NonNullList positions = NonNullList.create();
      positions.addAll(getSphere(pos, range, 0, false, true, 0));
      return positions;
   }

   public static EnumFacing getFacing(BlockPos pos, IBlockAccess provider) {
      for(EnumFacing facing : EnumFacing.values()) {
         if (!provider.getBlockState(pos.offset(facing)).getMaterial().isReplaceable()) {
            return facing;
         }
      }

      return null;
   }

   public static boolean canReplace(BlockPos pos) {
      return getState(pos).getMaterial().isReplaceable();
   }

   public static boolean isBedrock(BlockPos pos) {
      return mc.world.getBlockState(pos).getBlock() == Blocks.BEDROCK;
   }

   public static boolean isSafe(BlockPos pos) {
      return isObby(pos) || isBedrock(pos) || isEnderChest(pos);
   }

   public static boolean isEnderChest(BlockPos pos) {
      return mc.world.getBlockState(pos).getBlock() == Blocks.ENDER_CHEST;
   }

   public static boolean isAir(BlockPos pos) {
      return mc.world.getBlockState(pos).getBlock() == Blocks.AIR;
   }

   public static boolean isObby(BlockPos pos) {
      return mc.world.getBlockState(pos).getBlock() == Blocks.OBSIDIAN;
   }

   public static boolean PistonCheck(BlockPos blockPos) {
      if (mc.world.getBlockState(blockPos.add(0, 0, 1)).getBlock() == Blocks.AIR && mc.world.getBlockState(blockPos.add(0, 0, -1)).getBlock() == Blocks.AIR && mc.world.getBlockState(blockPos.add(1, 0, 0)).getBlock() == Blocks.AIR && mc.world.getBlockState(blockPos.add(-1, 0, 0)).getBlock() == Blocks.AIR && mc.world.getBlockState(blockPos.add(0, 1, 0)).getBlock() == Blocks.AIR && mc.world.getBlockState(blockPos.add(0, -1, 0)).getBlock() == Blocks.AIR) {
         return true;
      } else if (mc.world.getBlockState(blockPos).getBlock() != Blocks.AIR) {
         return true;
      } else {
         return checkEntityCrystal(EntityUtil.getVarOffsets(0, 0, 0), blockPos) != null;
      }
   }

   public static double distanceToXZ(double d, double d2) {
      double d3 = mc.player.posX - d;
      double d4 = mc.player.posZ - d2;
      return Math.sqrt(d3 * d3 + d4 * d4);
   }

   public static boolean cantPistonPlace(BlockPos blockPos) {
      if (mc.world.getBlockState(blockPos.add(0, 0, 1)).getBlock() == Blocks.AIR && mc.world.getBlockState(blockPos.add(0, 0, -1)).getBlock() == Blocks.AIR && mc.world.getBlockState(blockPos.add(1, 0, 0)).getBlock() == Blocks.AIR && mc.world.getBlockState(blockPos.add(-1, 0, 0)).getBlock() == Blocks.AIR && mc.world.getBlockState(blockPos.add(0, 1, 0)).getBlock() == Blocks.AIR && mc.world.getBlockState(blockPos).getBlock() != Blocks.AIR) {
         return true;
      } else {
         return checkEntity(EntityUtil.getVarOffsets(0, 0, 0), blockPos) != null;
      }
   }

   static Entity checkEntity(Vec3d[] vec3dArray, BlockPos blockPos) {
      Entity entity = null;

      for(Vec3d vec3d : vec3dArray) {
         BlockPos blockPos2 = (new BlockPos(blockPos)).add(vec3d.x, vec3d.y, vec3d.z);

         for(Entity entity2 : mc.world.getEntitiesWithinAABB(Entity.class, new AxisAlignedBB(blockPos2))) {
            if (entity2 instanceof EntityPlayer && entity == null) {
               entity = entity2;
            }
         }
      }

      return entity;
   }

   static Entity checkEntityCrystal(Vec3d[] vec3dArray, BlockPos blockPos) {
      Entity entity = null;

      for(Vec3d vec3d : vec3dArray) {
         BlockPos blockPos2 = (new BlockPos(blockPos)).add(vec3d.x, vec3d.y, vec3d.z);

         for(Entity entity2 : mc.world.getEntitiesWithinAABB(Entity.class, new AxisAlignedBB(blockPos2))) {
            if ((entity2 instanceof EntityPlayer || entity2 instanceof EntityEnderCrystal) && entity == null) {
               entity = entity2;
            }
         }
      }

      return entity;
   }

   public static List<EnumFacing> getPossibleSides(BlockPos pos) {
      ArrayList<EnumFacing> facings = new ArrayList();

      for(EnumFacing side : EnumFacing.values()) {
         BlockPos neighbour = pos.offset(side);
         if (mc.world.getBlockState(neighbour).getBlock().canCollideCheck(mc.world.getBlockState(neighbour), false) && !mc.world.getBlockState(neighbour).getMaterial().isReplaceable()) {
            facings.add(side);
         }
      }

      return facings;
   }

   public static EnumFacing getFirstFacing(BlockPos pos, boolean strict, boolean raytrace) {
      if (!strict) {
         Iterator<EnumFacing> iterator = getPossibleSides(pos).iterator();
         if (iterator.hasNext()) {
            return (EnumFacing)iterator.next();
         }
      } else {
         for(EnumFacing side : getPossibleSides(pos)) {
            if (canBeClicked(pos.offset(side))) {
               return side;
            }
         }
      }

      return null;
   }

   public static boolean canBeClicked(BlockPos pos) {
      return getBlock(pos).canCollideCheck(getState(pos), false);
   }

   public static EnumFacing getFirstFacing(BlockPos pos) {
      Iterator<EnumFacing> iterator = getPossibleSides(pos).iterator();
      if (iterator.hasNext()) {
         EnumFacing facing = (EnumFacing)iterator.next();
         return facing;
      } else {
         return null;
      }
   }

   public static EnumFacing getRayTraceFacing(BlockPos pos) {
      RayTraceResult result = mc.world.rayTraceBlocks(new Vec3d(mc.player.posX, mc.player.posY + (double)mc.player.getEyeHeight(), mc.player.posZ), new Vec3d((double)pos.getX() + (double)0.5F, (double)pos.getX() - (double)0.5F, (double)pos.getX() + (double)0.5F));
      return result != null && result.sideHit != null ? result.sideHit : EnumFacing.UP;
   }

   public static void rightClickBlock(BlockPos pos, Vec3d vec, EnumHand hand, EnumFacing direction, boolean packet) {
      if (packet) {
         float f = (float)(vec.x - (double)pos.getX());
         float f1 = (float)(vec.y - (double)pos.getY());
         float f2 = (float)(vec.z - (double)pos.getZ());
         mc.player.connection.sendPacket(new CPacketPlayerTryUseItemOnBlock(pos, direction, hand, f, f1, f2));
      } else {
         mc.playerController.processRightClickBlock(mc.player, mc.world, pos, direction, vec, hand);
      }

      mc.player.swingArm(EnumHand.MAIN_HAND);
      mc.rightClickDelayTimer = 4;
   }

   public static boolean placeBlock(BlockPos pos, EnumHand hand, boolean rotate, boolean packet, boolean isSneaking) {
      boolean sneaking = false;
      EnumFacing side = getFirstFacing(pos);
      if (side == null) {
         return isSneaking;
      } else {
         BlockPos neighbour = pos.offset(side);
         EnumFacing opposite = side.getOpposite();
         Vec3d hitVec = (new Vec3d(neighbour)).add((double)0.5F, (double)0.5F, (double)0.5F).add((new Vec3d(opposite.getDirectionVec())).scale((double)0.5F));
         Block neighbourBlock = mc.world.getBlockState(neighbour).getBlock();
         if (!mc.player.isSneaking() && (blackList.contains(neighbourBlock) || shulkerList.contains(neighbourBlock))) {
            mc.player.connection.sendPacket(new CPacketEntityAction(mc.player, Action.START_SNEAKING));
            mc.player.setSneaking(true);
            sneaking = true;
         }

         if (rotate) {
            RotationUtil.faceVector(hitVec, true);
         }

         rightClickBlock(neighbour, hitVec, hand, opposite, packet);
         mc.player.swingArm(EnumHand.MAIN_HAND);
         mc.rightClickDelayTimer = 4;
         return sneaking || isSneaking;
      }
   }

   public static List<BlockPos> getSphere(float radius, boolean ignoreAir) {
      return getSphere(mc.player, radius, ignoreAir);
   }

   public static List<BlockPos> getSphere(double range, BlockPos pos, boolean sphere, boolean hollow) {
      List<BlockPos> circleblocks = new ArrayList();
      int cx = pos.getX();
      int cy = pos.getY();
      int cz = pos.getZ();

      for(int x = cx - (int)range; (double)x <= (double)cx + range; ++x) {
         for(int z = cz - (int)range; (double)z <= (double)cz + range; ++z) {
            for(int y = sphere ? cy - (int)range : cy; (double)y < (double)cy + range; ++y) {
               double dist = (double)((cx - x) * (cx - x) + (cz - z) * (cz - z) + (sphere ? (cy - y) * (cy - y) : 0));
               if (dist < range * range && (!hollow || dist >= (range - (double)1.0F) * (range - (double)1.0F))) {
                  BlockPos l = new BlockPos(x, y, z);
                  circleblocks.add(l);
               }
            }
         }
      }

      return circleblocks;
   }

   public static List<BlockPos> getSphere(Entity entity, float radius, boolean ignoreAir) {
      List<BlockPos> sphere = new ArrayList();
      BlockPos pos = PositionUtil.getPosition(entity);
      int posX = pos.getX();
      int posY = pos.getY();
      int posZ = pos.getZ();
      int radiuss = (int)radius;

      for(int x = posX - radiuss; (float)x <= (float)posX + radius; ++x) {
         for(int z = posZ - radiuss; (float)z <= (float)posZ + radius; ++z) {
            for(int y = posY - radiuss; (float)y < (float)posY + radius; ++y) {
               if ((float)((posX - x) * (posX - x) + (posZ - z) * (posZ - z) + (posY - y) * (posY - y)) < radius * radius) {
                  BlockPos position = new BlockPos(x, y, z);
                  if (!ignoreAir || mc.world.getBlockState(position).getBlock() != Blocks.AIR) {
                     sphere.add(position);
                  }
               }
            }
         }
      }

      return sphere;
   }

   public static List<BlockPos> getSphere(BlockPos pos, float r, int h, boolean hollow, boolean sphere, int plus_y) {
      ArrayList<BlockPos> circleblocks = new ArrayList();
      int cx = pos.getX();
      int cy = pos.getY();
      int cz = pos.getZ();

      for(int x = cx - (int)r; (float)x <= (float)cx + r; ++x) {
         for(int z = cz - (int)r; (float)z <= (float)cz + r; ++z) {
            int y = sphere ? cy - (int)r : cy;

            while(true) {
               float f = (float)y;
               float f2 = sphere ? (float)cy + r : (float)(cy + h);
               if (!(f < f2)) {
                  break;
               }

               double dist = (double)((cx - x) * (cx - x) + (cz - z) * (cz - z) + (sphere ? (cy - y) * (cy - y) : 0));
               if (dist < (double)(r * r) && (!hollow || dist >= (double)((r - 1.0F) * (r - 1.0F)))) {
                  BlockPos l = new BlockPos(x, y + plus_y, z);
                  circleblocks.add(l);
               }

               ++y;
            }
         }
      }

      return circleblocks;
   }

   public static Block getBlock(BlockPos pos) {
      return getState(pos).getBlock();
   }

   public static Block getBlock(double x, double y, double z) {
      return mc.world.getBlockState(new BlockPos(x, y, z)).getBlock();
   }

   private static IBlockState getState(BlockPos pos) {
      return mc.world.getBlockState(pos);
   }

   public static boolean rayTracePlaceCheck(BlockPos pos, boolean shouldCheck, float height) {
      return !shouldCheck || mc.world.rayTraceBlocks(new Vec3d(mc.player.posX, mc.player.posY + (double)mc.player.getEyeHeight(), mc.player.posZ), new Vec3d((double)pos.getX(), (double)((float)pos.getY() + height), (double)pos.getZ()), false, true, false) == null;
   }

   static {
      blackList = Arrays.asList(Blocks.ENDER_CHEST, Blocks.CHEST, Blocks.TRAPPED_CHEST, Blocks.CRAFTING_TABLE, Blocks.ANVIL, Blocks.BREWING_STAND, Blocks.HOPPER, Blocks.DROPPER, Blocks.DISPENSER, Blocks.TRAPDOOR, Blocks.ENCHANTING_TABLE);
      shulkerList = Arrays.asList(Blocks.WHITE_SHULKER_BOX, Blocks.ORANGE_SHULKER_BOX, Blocks.MAGENTA_SHULKER_BOX, Blocks.LIGHT_BLUE_SHULKER_BOX, Blocks.YELLOW_SHULKER_BOX, Blocks.LIME_SHULKER_BOX, Blocks.PINK_SHULKER_BOX, Blocks.GRAY_SHULKER_BOX, Blocks.SILVER_SHULKER_BOX, Blocks.CYAN_SHULKER_BOX, Blocks.PURPLE_SHULKER_BOX, Blocks.BLUE_SHULKER_BOX, Blocks.BROWN_SHULKER_BOX, Blocks.GREEN_SHULKER_BOX, Blocks.RED_SHULKER_BOX, Blocks.BLACK_SHULKER_BOX);
      tickCache = new ArrayList();
      resistantBlocks = Arrays.asList(Blocks.OBSIDIAN, Blocks.ANVIL, Blocks.ENCHANTING_TABLE, Blocks.ENDER_CHEST, Blocks.BEACON);
      unbreakableBlocks = Arrays.asList(Blocks.BEDROCK, Blocks.COMMAND_BLOCK, Blocks.CHAIN_COMMAND_BLOCK, Blocks.END_PORTAL_FRAME, Blocks.BARRIER, Blocks.PORTAL);
   }
}
