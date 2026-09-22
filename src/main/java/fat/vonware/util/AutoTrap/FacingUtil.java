package fat.vonware.util.AutoTrap;

import fat.vonware.util.Util;
import net.minecraft.util.EnumFacing;

public class FacingUtil implements Util {
   public static EnumFacing[] TOP;

   static {
      TOP = new EnumFacing[]{EnumFacing.UP, EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.WEST, EnumFacing.EAST};
   }
}
