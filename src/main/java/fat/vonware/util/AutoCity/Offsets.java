package fat.vonware.util.AutoCity;

import net.minecraft.util.math.BlockPos;

public class Offsets {
   public static final BlockPos[] NORTH = new BlockPos[]{new BlockPos(0, 0, -1), new BlockPos(0, 0, -2), new BlockPos(0, -1, -2), new BlockPos(0, 0, -2)};
   public static final BlockPos[] EAST = new BlockPos[]{new BlockPos(1, 0, 0), new BlockPos(2, 0, 0), new BlockPos(2, -1, 0), new BlockPos(2, 0, 0)};
   public static final BlockPos[] SOUTH = new BlockPos[]{new BlockPos(0, 0, 1), new BlockPos(0, 0, 2), new BlockPos(0, -1, 2), new BlockPos(0, 0, 2)};
   public static final BlockPos[] WEST = new BlockPos[]{new BlockPos(-1, 0, 0), new BlockPos(-2, 0, 0), new BlockPos(-2, -1, 0), new BlockPos(-2, 0, 0)};
   public static final BlockPos[] FACE_PLACE = new BlockPos[]{new BlockPos(1, 1, 0), new BlockPos(-1, 1, 0), new BlockPos(0, 1, 1), new BlockPos(0, 1, -1)};
   public static final BlockPos[] FEET_PLACE = new BlockPos[]{new BlockPos(1, 0, 0), new BlockPos(-1, 0, 0), new BlockPos(0, 0, 1), new BlockPos(0, 0, -1)};
}
