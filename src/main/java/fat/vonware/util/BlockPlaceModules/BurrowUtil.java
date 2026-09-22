package fat.vonware.util.BlockPlaceModules;

import fat.vonware.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class BurrowUtil implements Util {
   public static float[] hitVecToPlaceVec(BlockPos pos, Vec3d hitVec) {
      float x = (float)(hitVec.x - (double)pos.getX());
      float y = (float)(hitVec.y - (double)pos.getY());
      float z = (float)(hitVec.z - (double)pos.getZ());
      return new float[]{x, y, z};
   }
}
