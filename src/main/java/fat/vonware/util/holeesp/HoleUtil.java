package fat.vonware.util.holeesp;

import fat.vonware.util.BlockUtil;
import fat.vonware.util.Util;
import fat.vonware.util.holeesp.Enums.SafetyEnum;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class HoleUtil implements Util {
   public static BlockPos[] HOLE_OFFSETS = new BlockPos[]{new BlockPos(1, 0, 0), new BlockPos(-1, 0, 0), new BlockPos(0, 0, 1), new BlockPos(0, 0, -1), new BlockPos(0, -1, 0)};
   public static BlockPos[] AROUND_OFFSETS = new BlockPos[]{new BlockPos(1, 0, 0), new BlockPos(-1, 0, 0), new BlockPos(0, 0, 1), new BlockPos(0, 0, -1)};

   public static BlockPos getPlayerPos() {
      double decimalPoint = mc.player.posY - Math.floor(mc.player.posY);
      return new BlockPos(mc.player.posX, decimalPoint > 0.8 ? Math.floor(mc.player.posY) + (double)1.0F : Math.floor(mc.player.posY), mc.player.posZ);
   }

   public static Hole getTargetHoleVec3D(double horizontalRange, double verticalRange, boolean includeTerrain) {
      return getHoles(mc.player, (float)horizontalRange, true, false, false, includeTerrain, false).stream().filter((hole) -> mc.player.getPositionVector().distanceTo(new Vec3d((double)hole.getFirst().getX() + (double)0.5F, mc.player.posY, (double)hole.getFirst().getZ() + (double)0.5F)) <= horizontalRange).min(Comparator.comparingDouble((hole) -> mc.player.getPositionVector().distanceTo(new Vec3d((double)hole.getFirst().getX() + (double)0.5F, mc.player.posY, (double)hole.getFirst().getZ() + (double)0.5F)))).orElse(null);
   }

   public static boolean isHole(BlockPos pos) {
      return isMixedHole(pos) || isBedrockHole(pos) || isObbyHole(pos);
   }

   public static boolean isInHole(EntityPlayer player) {
      return isHole(player.getPosition());
   }

   public static boolean isPositionInAnyHole(BlockPos pos) {
      if (isHole(pos)) {
         return true;
      } else if (!isDoubleHole(pos) && !isDoubleHole(pos.add(0, 0, -1)) && !isDoubleHole(pos.add(-1, 0, 0))) {
         if (!isIncompleteDouble(pos) && !isIncompleteDouble(pos.add(0, 0, -1)) && !isIncompleteDouble(pos.add(-1, 0, 0))) {
            return is2x2Hole(pos) || is2x2Hole(pos.add(-1, 0, 0)) || is2x2Hole(pos.add(0, 0, -1)) || is2x2Hole(pos.add(-1, 0, -1));
         } else {
            return true;
         }
      } else {
         return true;
      }
   }

   public static boolean isWebHole(BlockPos pos) {
      if (BlockUtil.isAir(pos.up()) && BlockUtil.isAir(pos.up().up())) {
         for(BlockPos off : HOLE_OFFSETS) {
            if (!isWeb(pos.add(off))) {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   public static boolean isWeb(BlockPos pos) {
      return mc.world.getBlockState(pos).getBlock() == Blocks.WEB || mc.world.getBlockState(pos).getBlock() == Blocks.OBSIDIAN || mc.world.getBlockState(pos).getBlock() == Blocks.BEDROCK;
   }

   public static boolean isObbyHole(BlockPos pos) {
      assert mc.world != null;

      if (BlockUtil.isAir(pos.up()) && BlockUtil.isAir(pos.up(2))) {
         for(BlockPos off : HOLE_OFFSETS) {
            if (!BlockUtil.isObby(pos.add(off))) {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   public static boolean isMixedHole(BlockPos pos) {
      assert mc.world != null;

      if (isBedrockHole(pos)) {
         return false;
      } else if (BlockUtil.isAir(pos.up()) && BlockUtil.isAir(pos.up(2))) {
         for(BlockPos off : HOLE_OFFSETS) {
            if (!BlockUtil.isSafe(pos.add(off))) {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   public static boolean isBedrockHole(BlockPos pos) {
      if (BlockUtil.isAir(pos.up()) && BlockUtil.isAir(pos.up(2))) {
         for(BlockPos off : HOLE_OFFSETS) {
            if (!BlockUtil.isBedrock(pos.add(off))) {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   public static boolean isTrapHole(BlockPos pos) {
      if (!isHole(pos) && BlockUtil.isAir(pos.up()) && !BlockUtil.isAir(pos.up(2)) && BlockUtil.isSafe(pos.down())) {
         if (!BlockUtil.isSafe(pos.down())) {
            return false;
         } else {
            for(BlockPos off : AROUND_OFFSETS) {
               if (!BlockUtil.isSafe(pos.up().add(off))) {
                  return false;
               }
            }

            return true;
         }
      } else {
         return false;
      }
   }

   public static boolean isTerrainHole(BlockPos pos) {
      if (isHole(pos)) {
         return false;
      } else if (BlockUtil.isAir(pos.up()) && BlockUtil.isAir(pos.up(2))) {
         for(BlockPos off : HOLE_OFFSETS) {
            Block block = mc.world.getBlockState(pos.add(off)).getBlock();
            if (block != Blocks.STONE && block != Blocks.DIRT && block != Blocks.NETHERRACK && block != Blocks.COBBLESTONE && block != Blocks.GRAVEL && block != Blocks.END_STONE && !BlockUtil.isSafe(pos.add(off))) {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   public static boolean isIncompleteDouble(BlockPos pos) {
      Hole hole = getIncompleteDouble(pos);
      return hole != null;
   }

   public static Hole getIncompleteDouble(BlockPos pos) {
      if (isIncompleteDoubleDirection(pos, 0, 1)) {
         SafetyEnum safety = getDoubleSafety(pos, 0, 1);
         return new Hole(pos, pos.add(0, 0, 1), safety);
      } else if (isIncompleteDoubleDirection(pos, 1, 0)) {
         SafetyEnum safety = getDoubleSafety(pos, 1, 0);
         return new Hole(pos, pos.add(1, 0, 0), safety);
      } else {
         return null;
      }
   }

   private static boolean isIncompleteDoubleDirection(BlockPos pos, int offX, int offZ) {
      BlockPos second = pos.add(offX, 0, offZ);
      if (BlockUtil.isAir(pos) && BlockUtil.isAir(second)) {
         if (!hasProperDoubleStructure(pos, offX, offZ)) {
            return false;
         } else {
            boolean firstHasBlockAbove = false;

            for(int i = 1; i <= 3; ++i) {
               if (!BlockUtil.isAir(pos.up(i))) {
                  firstHasBlockAbove = true;
                  break;
               }
            }

            boolean secondHasBlockAbove = false;

            for(int i = 1; i <= 3; ++i) {
               if (!BlockUtil.isAir(second.up(i))) {
                  secondHasBlockAbove = true;
                  break;
               }
            }

            return firstHasBlockAbove && !secondHasBlockAbove || !firstHasBlockAbove && secondHasBlockAbove;
         }
      } else {
         return false;
      }
   }

   private static boolean hasProperDoubleStructure(BlockPos pos, int offX, int offZ) {
      if (BlockUtil.isSafe(pos.add(0, -1, 0)) && BlockUtil.isSafe(pos.add(offX, -1, offZ))) {
         return BlockUtil.isSafe(pos.add(offX * 2, 0, offZ * 2)) && BlockUtil.isSafe(pos.add(-offX, 0, -offZ)) && BlockUtil.isSafe(pos.add(offZ, 0, offX)) && BlockUtil.isSafe(pos.add(-offZ, 0, -offX)) && BlockUtil.isSafe(pos.add(offX, 0, offZ).add(offZ, 0, offX)) && BlockUtil.isSafe(pos.add(offX, 0, offZ).add(-offZ, 0, -offX));
      } else {
         return false;
      }
   }

   private static SafetyEnum getDoubleSafety(BlockPos pos, int offX, int offZ) {
      int bedrockCount = 0;
      int obbyCount = 0;
      int totalCount = 0;
      BlockPos[] checkPositions = new BlockPos[]{pos.add(0, -1, 0), pos.add(offX, -1, offZ), pos.add(offX * 2, 0, offZ * 2), pos.add(-offX, 0, -offZ), pos.add(offZ, 0, offX), pos.add(-offZ, 0, -offX), pos.add(offX, 0, offZ).add(offZ, 0, offX), pos.add(offX, 0, offZ).add(-offZ, 0, -offX)};

      for(BlockPos checkPos : checkPositions) {
         if (BlockUtil.isBedrock(checkPos)) {
            ++bedrockCount;
            ++totalCount;
         } else if (BlockUtil.isObby(checkPos)) {
            ++obbyCount;
            ++totalCount;
         } else if (BlockUtil.isSafe(checkPos)) {
            ++totalCount;
         }
      }

      if (bedrockCount == totalCount) {
         return SafetyEnum.BEDROCK;
      } else if (obbyCount == totalCount) {
         return SafetyEnum.OBBY;
      } else {
         return SafetyEnum.MIXED;
      }
   }

   public static boolean isDoubleHole(BlockPos pos) {
      Hole hole = getDoubleHole(pos);
      return hole != null && BlockUtil.isAir(hole.getFirst().up()) && BlockUtil.isAir(hole.getSecond().up());
   }

   public static Hole getDoubleHole(BlockPos pos) {
      if (isBedrock(pos, 0, 1)) {
         return new Hole(pos, pos.add(0, 0, 1), SafetyEnum.BEDROCK);
      } else if (isBedrock(pos, 1, 0)) {
         return new Hole(pos, pos.add(1, 0, 0), SafetyEnum.BEDROCK);
      } else if (isObby(pos, 0, 1)) {
         return new Hole(pos, pos.add(0, 0, 1), SafetyEnum.OBBY);
      } else if (isObby(pos, 1, 0)) {
         return new Hole(pos, pos.add(1, 0, 0), SafetyEnum.OBBY);
      } else if (isMixed(pos, 0, 1)) {
         return new Hole(pos, pos.add(0, 0, 1), SafetyEnum.MIXED);
      } else {
         return isMixed(pos, 1, 0) ? new Hole(pos, pos.add(1, 0, 0), SafetyEnum.MIXED) : null;
      }
   }

   public static Vec3d getCenter(Hole hole) {
      if (hole.is2x2()) {
         return new Vec3d((double)hole.getFirst().getX() + (double)1.0F, (double)hole.getFirst().getY(), (double)hole.getFirst().getZ() + (double)1.0F);
      } else {
         double x = (double)hole.getFirst().getX() + (double)0.5F;
         double z = (double)hole.getFirst().getZ() + (double)0.5F;
         if (hole.getSecond() != null) {
            x = (x + (double)hole.getSecond().getX() + (double)0.5F) / (double)2.0F;
            z = (z + (double)hole.getSecond().getZ() + (double)0.5F) / (double)2.0F;
         }

         return new Vec3d(x, (double)hole.getFirst().getY(), z);
      }
   }

   public static boolean isObby(BlockPos pos, int offX, int offZ) {
      return BlockUtil.isAir(pos) && BlockUtil.isAir(pos.add(offX, 0, offZ)) && BlockUtil.isObby(pos.add(0, -1, 0)) && BlockUtil.isObby(pos.add(offX, -1, offZ)) && BlockUtil.isObby(pos.add(offX * 2, 0, offZ * 2)) && BlockUtil.isObby(pos.add(-offX, 0, -offZ)) && BlockUtil.isObby(pos.add(offZ, 0, offX)) && BlockUtil.isObby(pos.add(-offZ, 0, -offX)) && BlockUtil.isObby(pos.add(offX, 0, offZ).add(offZ, 0, offX)) && BlockUtil.isObby(pos.add(offX, 0, offZ).add(-offZ, 0, -offX));
   }

   public static boolean isMixed(BlockPos pos, int offX, int offZ) {
      return BlockUtil.isAir(pos) && BlockUtil.isAir(pos.add(offX, 0, offZ)) && BlockUtil.isSafe(pos.add(0, -1, 0)) && BlockUtil.isSafe(pos.add(offX, -1, offZ)) && BlockUtil.isSafe(pos.add(offX * 2, 0, offZ * 2)) && BlockUtil.isSafe(pos.add(-offX, 0, -offZ)) && BlockUtil.isSafe(pos.add(offZ, 0, offX)) && BlockUtil.isSafe(pos.add(-offZ, 0, -offX)) && BlockUtil.isSafe(pos.add(offX, 0, offZ).add(offZ, 0, offX)) && BlockUtil.isSafe(pos.add(offX, 0, offZ).add(-offZ, 0, -offX));
   }

   public static boolean isBedrock(BlockPos pos, int offX, int offZ) {
      return BlockUtil.isAir(pos) && BlockUtil.isAir(pos.add(offX, 0, offZ)) && BlockUtil.isBedrock(pos.add(0, -1, 0)) && BlockUtil.isBedrock(pos.add(offX, -1, offZ)) && BlockUtil.isBedrock(pos.add(offX * 2, 0, offZ * 2)) && BlockUtil.isBedrock(pos.add(-offX, 0, -offZ)) && BlockUtil.isBedrock(pos.add(offZ, 0, offX)) && BlockUtil.isBedrock(pos.add(-offZ, 0, -offX)) && BlockUtil.isBedrock(pos.add(offX, 0, offZ).add(offZ, 0, offX)) && BlockUtil.isBedrock(pos.add(offX, 0, offZ).add(-offZ, 0, -offX));
   }

   public static boolean is2x2Hole(BlockPos pos) {
      return get2x2Hole(pos) != null;
   }

   public static Hole get2x2Hole(BlockPos pos) {
      if (BlockUtil.isAir(pos) && BlockUtil.isAir(pos.add(1, 0, 0)) && BlockUtil.isAir(pos.add(0, 0, 1)) && BlockUtil.isAir(pos.add(1, 0, 1))) {
         if (BlockUtil.isAir(pos.up()) && BlockUtil.isAir(pos.up(2)) && BlockUtil.isAir(pos.add(1, 0, 0).up()) && BlockUtil.isAir(pos.add(1, 0, 0).up(2)) && BlockUtil.isAir(pos.add(0, 0, 1).up()) && BlockUtil.isAir(pos.add(0, 0, 1).up(2)) && BlockUtil.isAir(pos.add(1, 0, 1).up()) && BlockUtil.isAir(pos.add(1, 0, 1).up(2))) {
            if (BlockUtil.isSafe(pos.down()) && BlockUtil.isSafe(pos.add(1, 0, 0).down()) && BlockUtil.isSafe(pos.add(0, 0, 1).down()) && BlockUtil.isSafe(pos.add(1, 0, 1).down())) {
               BlockPos[] walls = new BlockPos[]{pos.add(-1, 0, 0), pos.add(-1, 0, 1), pos.add(2, 0, 0), pos.add(2, 0, 1), pos.add(0, 0, -1), pos.add(1, 0, -1), pos.add(0, 0, 2), pos.add(1, 0, 2)};
               int bedrockCount = 0;
               int obbyCount = 0;

               for(BlockPos wall : walls) {
                  if (BlockUtil.isBedrock(wall)) {
                     ++bedrockCount;
                  } else if (BlockUtil.isObby(wall)) {
                     ++obbyCount;
                  } else if (!BlockUtil.isSafe(wall)) {
                     return null;
                  }
               }

               BlockPos[] floor = new BlockPos[]{pos.down(), pos.add(1, 0, 0).down(), pos.add(0, 0, 1).down(), pos.add(1, 0, 1).down()};

               for(BlockPos f : floor) {
                  if (BlockUtil.isBedrock(f)) {
                     ++bedrockCount;
                  } else if (BlockUtil.isObby(f)) {
                     ++obbyCount;
                  }
               }

               SafetyEnum safety = bedrockCount == 12 ? SafetyEnum.BEDROCK : (obbyCount == 12 ? SafetyEnum.OBBY : SafetyEnum.MIXED);
               return new Hole(pos, pos.add(1, 0, 0), pos.add(0, 0, 1), pos.add(1, 0, 1), safety);
            } else {
               return null;
            }
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   public static Hole getHole(BlockPos pos, boolean doubles) {
      return getHole(pos, doubles, false);
   }

   public static Hole getHole(BlockPos pos, boolean doubles, boolean terrain) {
      if (!BlockUtil.isAir(pos)) {
         return null;
      } else {
         Hole hole = null;
         if (isBedrockHole(pos)) {
            hole = new Hole(pos, SafetyEnum.BEDROCK);
         } else if (isObbyHole(pos)) {
            hole = new Hole(pos, SafetyEnum.OBBY);
         } else if (isMixedHole(pos)) {
            hole = new Hole(pos, SafetyEnum.MIXED);
         } else if (terrain && isTerrainHole(pos)) {
            hole = new Hole(pos, SafetyEnum.TERRAIN);
         }

         if (doubles && isDoubleHole(pos)) {
            hole = getDoubleHole(pos);
         }

         return hole;
      }
   }

   public static List<Hole> getHoles(float range, boolean doubles, boolean webs) {
      return getHoles(mc.player, range, doubles, webs, false, false, false);
   }

   public static List<Hole> getHoles(float range, boolean doubles, boolean webs, boolean trap, boolean terrain) {
      return getHoles(mc.player, range, doubles, webs, trap, terrain, false);
   }

   public static List<Hole> getHoles(Entity player, float range, boolean doubles, boolean webs, boolean trap, boolean terrain) {
      return getHoles(player, range, doubles, webs, trap, terrain, false);
   }

   public static List<Hole> getHoles(Entity player, float range, boolean doubles, boolean webs, boolean trap, boolean terrain, boolean incompleteDoubles, boolean twoByTwo) {
      List<Hole> holes = new ArrayList();

      for(BlockPos pos : BlockUtil.getSphere(player, range, false)) {
         if (BlockUtil.isAir(pos)) {
            if (webs && isWebHole(pos)) {
               holes.add(new Hole(pos, SafetyEnum.MIXED));
            } else if (isBedrockHole(pos)) {
               holes.add(new Hole(pos, SafetyEnum.BEDROCK));
            } else if (isObbyHole(pos)) {
               holes.add(new Hole(pos, SafetyEnum.OBBY));
            } else if (isMixedHole(pos)) {
               holes.add(new Hole(pos, SafetyEnum.MIXED));
            } else if (trap && isTrapHole(pos)) {
               holes.add(new Hole(pos, SafetyEnum.TRAPPED));
            } else if (terrain && isTerrainHole(pos)) {
               holes.add(new Hole(pos, SafetyEnum.TERRAIN));
            } else if (doubles && isDoubleHole(pos)) {
               holes.add(getDoubleHole(pos));
            } else if (incompleteDoubles && isIncompleteDouble(pos)) {
               holes.add(getIncompleteDouble(pos));
            } else if (twoByTwo && is2x2Hole(pos)) {
               holes.add(get2x2Hole(pos));
            }
         }
      }

      return holes;
   }

   public static List<Hole> getHoles(Entity player, float range, boolean doubles, boolean webs, boolean trap, boolean terrain, boolean incompleteDoubles) {
      List<Hole> holes = new ArrayList();

      for(BlockPos pos : BlockUtil.getSphere(player, range, false)) {
         if (BlockUtil.isAir(pos)) {
            if (webs && isWebHole(pos)) {
               holes.add(new Hole(pos, SafetyEnum.MIXED));
            } else if (isBedrockHole(pos)) {
               holes.add(new Hole(pos, SafetyEnum.BEDROCK));
            } else if (isObbyHole(pos)) {
               holes.add(new Hole(pos, SafetyEnum.OBBY));
            } else if (isMixedHole(pos)) {
               holes.add(new Hole(pos, SafetyEnum.MIXED));
            } else if (trap && isTrapHole(pos)) {
               holes.add(new Hole(pos, SafetyEnum.TRAPPED));
            } else if (terrain && isTerrainHole(pos)) {
               holes.add(new Hole(pos, SafetyEnum.TERRAIN));
            } else if (doubles && isDoubleHole(pos)) {
               holes.add(getDoubleHole(pos));
            } else if (incompleteDoubles && isIncompleteDouble(pos)) {
               holes.add(getIncompleteDouble(pos));
            }
         }
      }

      return holes;
   }
}
