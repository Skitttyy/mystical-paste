package fat.vonware.util;

import fat.vonware.Vonware;
import fat.vonware.util.holeesp.HoleUtil;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class EntityUtil implements Util {
   private static final List<Block> burrowList;

   public static Stream<Entity> getTargets(double targetRange) {
      List<Entity> entities;
      try {
         entities = new ArrayList(mc.world.getLoadedEntityList());
      } catch (Exception var4) {
         return Stream.empty();
      }

      return entities.stream().filter(Objects::nonNull).filter((entity) -> entity instanceof EntityPlayer).filter(EntityUtil::isAlive).filter((entity) -> entity.getEntityId() != mc.player.getEntityId()).filter((entity) -> !Vonware.friendManager.isFriend((EntityPlayer)entity)).filter((entity) -> (double)mc.player.getDistance(entity) <= targetRange);
   }

   public static BlockPos getPlayerPos(EntityPlayer player) {
      return new BlockPos(Math.floor(player.posX), Math.floor(player.posY), Math.floor(player.posZ));
   }

   public static boolean isPartialPhasing(Entity entity) {
      if (entity != null && entity.world != null) {
         int collisions = entity.world.getCollisionBoxes(entity, entity.getEntityBoundingBox().grow(0.01, (double)0.0F, 0.01)).size();
         if (collisions < 2) {
            return false;
         } else {
            double xOffset = entity.posX - Math.floor(entity.posX);
            double zOffset = entity.posZ - Math.floor(entity.posZ);
            boolean xPartial = xOffset >= 0.235 && xOffset <= 0.245 || xOffset >= 0.755 && xOffset <= 0.765;
            boolean zPartial = zOffset >= 0.235 && zOffset <= 0.245 || zOffset >= 0.755 && zOffset <= 0.765;
            return xPartial && zPartial;
         }
      } else {
         return false;
      }
   }

   public static List<EnumFacing> getCornerClipDirections() {
      return getCornerClipDirections(mc.player);
   }

   public static List<EnumFacing> getCornerClipDirections(EntityPlayer player) {
      double xOffset = player.posX - Math.floor(player.posX);
      double zOffset = player.posZ - Math.floor(player.posZ);
      List<EnumFacing> dirs = new ArrayList();
      if (xOffset >= 0.235 && xOffset <= 0.245) {
         dirs.add(EnumFacing.WEST);
      } else if (xOffset >= 0.755 && xOffset <= 0.765) {
         dirs.add(EnumFacing.EAST);
      }

      if (zOffset >= 0.235 && zOffset <= 0.245) {
         dirs.add(EnumFacing.NORTH);
      } else if (zOffset >= 0.755 && zOffset <= 0.765) {
         dirs.add(EnumFacing.SOUTH);
      }

      return dirs;
   }

   public static boolean isCornerClipping(EntityPlayer player) {
      return getCornerClipDirections(player).size() >= 2;
   }

   public static void filterForCornerClip(List<BlockPos> placePositions) {
      BlockPos playerPos = PositionUtil.getPosition(mc.player);
      List<EnumFacing> clipDirs = getCornerClipDirections();
      if (clipDirs.size() >= 2) {
         Set<BlockPos> allowed = new HashSet();

         for(EnumFacing dir : EnumFacing.HORIZONTALS) {
            if (!clipDirs.contains(dir)) {
               allowed.add(playerPos.offset(dir));
            }
         }

         placePositions.removeIf((pos) -> pos.getY() == playerPos.getY() && !allowed.contains(pos));
      }
   }

   public static boolean isBurrowPhasing(Entity entity) {
      if (entity != null && entity.world != null) {
         AxisAlignedBB bodyBB = entity.getEntityBoundingBox();
         boolean bodyInBlock = isIntersectingBlock(entity, bodyBB);
         if (!bodyInBlock) {
            return false;
         } else {
            BlockPos headPos = new BlockPos(entity.posX, entity.posY + (double)entity.getEyeHeight(), entity.posZ);
            IBlockState headState = entity.world.getBlockState(headPos);
            boolean headInBlock = headState.isFullBlock() && headState.getBlock() != Blocks.AIR;
            return !headInBlock;
         }
      } else {
         return false;
      }
   }

   public static boolean isFullPhasing(Entity entity) {
      if (entity != null && entity.world != null) {
         AxisAlignedBB bodyBB = entity.getEntityBoundingBox();
         boolean bodyInBlock = isIntersectingBlock(entity, bodyBB);
         if (!bodyInBlock) {
            return false;
         } else {
            BlockPos headPos = new BlockPos(entity.posX, entity.posY + (double)entity.getEyeHeight(), entity.posZ);
            IBlockState headState = entity.world.getBlockState(headPos);
            boolean headInBlock = headState.isFullBlock() && headState.getBlock() != Blocks.AIR;
            return headInBlock;
         }
      } else {
         return false;
      }
   }

   private static boolean isIntersectingBlock(Entity entity, AxisAlignedBB bb) {
      int minX = MathHelper.floor(bb.minX);
      int minY = MathHelper.floor(bb.minY);
      int minZ = MathHelper.floor(bb.minZ);
      int maxX = MathHelper.floor(bb.maxX);
      int maxY = MathHelper.floor(bb.maxY);
      int maxZ = MathHelper.floor(bb.maxZ);

      for(int x = minX; x <= maxX; ++x) {
         for(int y = minY; y <= maxY; ++y) {
            for(int z = minZ; z <= maxZ; ++z) {
               BlockPos pos = new BlockPos(x, y, z);
               IBlockState state = entity.world.getBlockState(pos);
               if (state.isFullBlock() && state.getBlock() != Blocks.AIR) {
                  AxisAlignedBB blockBB = state.getBoundingBox(entity.world, pos).offset(pos);
                  if (blockBB != Block.NULL_AABB && bb.intersects(blockBB)) {
                     double intersectVolume = getIntersectionVolume(bb, blockBB);
                     if (intersectVolume > 0.1) {
                        return true;
                     }
                  }
               }
            }
         }
      }

      return false;
   }

   private static double getIntersectionVolume(AxisAlignedBB bb1, AxisAlignedBB bb2) {
      double x = Math.max((double)0.0F, Math.min(bb1.maxX, bb2.maxX) - Math.max(bb1.minX, bb2.minX));
      double y = Math.max((double)0.0F, Math.min(bb1.maxY, bb2.maxY) - Math.max(bb1.minY, bb2.minY));
      double z = Math.max((double)0.0F, Math.min(bb1.maxZ, bb2.maxZ) - Math.max(bb1.minZ, bb2.minZ));
      return x * y * z;
   }

   public static Vec3d getInterpolatedRenderPos(Entity entity, float ticks) {
      return getInterpolatedPos(entity, (double)ticks).subtract(Minecraft.getMinecraft().getRenderManager().viewerPosX, Minecraft.getMinecraft().getRenderManager().viewerPosY, Minecraft.getMinecraft().getRenderManager().viewerPosZ);
   }

   public static Vec3d getInterpolatedPos(Entity entity, double ticks) {
      double dx = entity.posX - entity.lastTickPosX;
      double dy = entity.posY - entity.lastTickPosY;
      double dz = entity.posZ - entity.lastTickPosZ;
      double distSq = dx * dx + dy * dy + dz * dz;
      return entity.ticksExisted > 1 && !(distSq > (double)16.0F) ? (new Vec3d(entity.lastTickPosX, entity.lastTickPosY, entity.lastTickPosZ)).add(getLastTickPos(entity, ticks, ticks, ticks)) : new Vec3d(entity.posX, entity.posY, entity.posZ);
   }

   public static Vec3d getLastTickPos(Entity entity, double x, double y, double z) {
      return new Vec3d((entity.posX - entity.lastTickPosX) * x, (entity.posY - entity.lastTickPosY) * y, (entity.posZ - entity.lastTickPosZ) * z);
   }

   public static boolean isMonster(Entity entity) {
      return entity.isCreatureType(EnumCreatureType.MONSTER, false);
   }

   public static boolean isMoving() {
      return isMoving(mc.player);
   }

   public static boolean isMoving(EntityLivingBase entity) {
      return entity.moveForward != 0.0F || entity.moveStrafing != 0.0F;
   }

   public static boolean isPhasing() {
      return isPhasing(mc.player);
   }

   public static boolean isPhasing(EntityPlayer player) {
      AxisAlignedBB bb = player.getEntityBoundingBox();

      for(int x = MathHelper.floor(bb.minX); x < MathHelper.floor(bb.maxX) + 1; ++x) {
         for(int y = MathHelper.floor(bb.minY); y < MathHelper.floor(bb.maxY) + 1; ++y) {
            for(int z = MathHelper.floor(bb.minZ); z < MathHelper.floor(bb.maxZ) + 1; ++z) {
               BlockPos pos = new BlockPos(x, y, z);
               IBlockState state = mc.world.getBlockState(pos);
               AxisAlignedBB blockBB = state.getSelectedBoundingBox(mc.world, pos);
               if (state.getMaterial().blocksMovement() && bb.intersects(blockBB)) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   public static boolean isSuffocating(EntityPlayer player) {
      BlockPos playerPos = getOtherPlayerPos(player);
      return mc.world.getBlockState(playerPos.up()).getBlock() != Blocks.AIR;
   }

   public static void setTimer(float speed) {
      mc.timer.tickLength = 50.0F / speed;
   }

   public static boolean isTrapped(EntityPlayer player) {
      List<Vec3d> poses = getOffsetList(1, false);
      poses.addAll(getOffsetList(0, false));
      poses.add(new Vec3d((double)0.0F, (double)2.0F, (double)0.0F));

      for(Vec3d vec : poses) {
         Block block = mc.world.getBlockState((new BlockPos(player)).add(vec.x, vec.y, vec.z)).getBlock();
         if (block != Blocks.OBSIDIAN && block != Blocks.BEDROCK) {
            return false;
         }
      }

      return true;
   }

   public static EntityPlayer getClosestEnemy() {
      EntityPlayer closest = null;
      double distance = (double)Float.MAX_VALUE;

      for(EntityPlayer player : mc.world.playerEntities) {
         if (isValid(player)) {
            Vec3d pos = mc.player.getPositionVector();
            double dist = player.getDistanceSq(pos.x, pos.y, pos.z);
            if (!(dist >= distance)) {
               closest = player;
               distance = dist;
            }
         }
      }

      return closest;
   }

   public static boolean isValid(EntityPlayer player) {
      return player != null && !isDead(player) && !player.equals(mc.player) && !player.getName().equals(mc.player.getName()) && !Vonware.friendManager.isFriend(player);
   }

   public static boolean isPlayerSafe(EntityPlayer player) {
      Vec3d playerVec = player.getPositionVector();
      BlockPos position = new BlockPos(playerVec);
      return HoleUtil.isHole(position) || isInBurrow(player);
   }

   public static double getDistance(double p_X, double p_Y, double p_Z, double x, double y, double z) {
      double d0 = p_X - x;
      double d1 = p_Y - y;
      double d2 = p_Z - z;
      return (double)MathHelper.sqrt(d0 * d0 + d1 * d1 + d2 * d2);
   }

   public static float getHealth(Entity entity, boolean absorption) {
      if (isLiving(entity)) {
         EntityLivingBase livingBase = (EntityLivingBase)entity;
         return livingBase.getHealth() + (absorption ? livingBase.getAbsorptionAmount() : 0.0F);
      } else {
         return 0.0F;
      }
   }

   public static boolean isInBurrow(EntityPlayer player) {
      BlockPos pos = PositionUtil.getPosition(player);
      return isBurrow(pos, player) || isBurrow(pos.up(), player);
   }

   public static boolean isBurrow(BlockPos pos, EntityPlayer player) {
      IBlockState state = mc.world.getBlockState(pos);
      return burrowList.contains(state.getBlock()) && state.getBoundingBox(mc.world, pos).offset(pos).maxY > player.posY;
   }

   public static boolean isBurrow(EntityPlayer target) {
      if (mc.world != null && mc.player != null && target != null) {
         BlockPos blockPos = new BlockPos(target.posX, target.posY, target.posZ);
         return mc.world.getBlockState(blockPos).getBlock().equals(Blocks.OBSIDIAN) || mc.world.getBlockState(blockPos).getBlock().equals(Blocks.ENDER_CHEST);
      } else {
         return false;
      }
   }

   public static double getEyeHeight(Entity entity) {
      return entity.posY + (double)entity.getEyeHeight();
   }

   public static BlockPos getOtherPlayerPos(EntityPlayer player) {
      double decimalPoint = player.posY - Math.floor(player.posY);
      return new BlockPos(player.posX, decimalPoint > 0.8 ? Math.floor(player.posY) + (double)1.0F : Math.floor(player.posY), player.posZ);
   }

   public static EntityPlayer getTarget(double targetRange) {
      return (EntityPlayer)mc.world.getLoadedEntityList().stream().filter(Objects::nonNull).filter((entity) -> entity instanceof EntityPlayer).filter(EntityUtil::isAlive).filter((entity) -> !entity.getName().equals(mc.player.getName())).filter((entity) -> entity.getEntityId() != mc.player.getEntityId()).filter((entity) -> !Vonware.friendManager.isFriend(((EntityPlayer)entity).getDisplayNameString())).filter((entity) -> (double)mc.player.getDistance(entity) <= targetRange).min(Comparator.comparingDouble((entity) -> (double)mc.player.getDistance(entity))).orElse(null);
   }

   public static Vec3d[] getVarOffsets(int n, int n2, int n3) {
      List<Vec3d> list = getVarOffsetList(n, n2, n3);
      Vec3d[] vec3dArray = new Vec3d[list.size()];
      return (Vec3d[])list.toArray(vec3dArray);
   }

   public static List<Vec3d> getVarOffsetList(int n, int n2, int n3) {
      ArrayList<Vec3d> arrayList = new ArrayList();
      arrayList.add(new Vec3d((double)n, (double)n2, (double)n3));
      return arrayList;
   }

   public static BlockPos getEntityPos(Entity target) {
      return new BlockPos(target.posX, target.posY + (double)0.5F, target.posZ);
   }

   public static List<Vec3d> getOffsetList(int y, boolean floor) {
      ArrayList<Vec3d> offsets = new ArrayList();
      offsets.add(new Vec3d((double)-1.0F, (double)y, (double)0.0F));
      offsets.add(new Vec3d((double)1.0F, (double)y, (double)0.0F));
      offsets.add(new Vec3d((double)0.0F, (double)y, (double)-1.0F));
      offsets.add(new Vec3d((double)0.0F, (double)y, (double)1.0F));
      if (floor) {
         offsets.add(new Vec3d((double)0.0F, (double)(y - 1), (double)0.0F));
      }

      return offsets;
   }

   public static boolean isLiving(Entity entity) {
      return entity instanceof EntityLivingBase;
   }

   public static boolean isAlive(Entity entity) {
      return isLiving(entity) && !entity.isDead && ((EntityLivingBase)entity).getHealth() > 0.0F;
   }

   public static boolean isDead(Entity entity) {
      return !isAlive(entity);
   }

   public static float getHealth(Entity entity) {
      if (isLiving(entity)) {
         EntityLivingBase livingBase = (EntityLivingBase)entity;
         return livingBase.getHealth() + livingBase.getAbsorptionAmount();
      } else {
         return 0.0F;
      }
   }

   public static boolean isntValid(Entity entity, double range) {
      return entity == null || isDead(entity) || entity.equals(mc.player) || entity instanceof EntityPlayer && Vonware.friendManager.isFriend(entity.getName()) || mc.player.getDistanceSq(entity) > MathUtil.square(range);
   }

   public static Color getColor(Entity entity, int red, int green, int blue, int alpha, boolean colorFriends) {
      Color color = new Color((float)red / 255.0F, (float)green / 255.0F, (float)blue / 255.0F, (float)alpha / 255.0F);
      if (entity instanceof EntityPlayer && colorFriends && Vonware.friendManager.isFriend((EntityPlayer)entity)) {
         color = new Color(0.33333334F, 1.0F, 1.0F, (float)alpha / 255.0F);
      }

      return color;
   }

   static {
      burrowList = Arrays.asList(Blocks.BEDROCK, Blocks.OBSIDIAN, Blocks.ENDER_CHEST, Blocks.CHEST, Blocks.TRAPPED_CHEST, Blocks.BEACON, Blocks.PISTON, Blocks.REDSTONE_BLOCK, Blocks.ENCHANTING_TABLE, Blocks.ANVIL);
   }
}
