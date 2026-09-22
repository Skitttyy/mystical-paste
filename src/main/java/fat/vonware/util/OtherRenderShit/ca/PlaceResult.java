package fat.vonware.util.OtherRenderShit.ca;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

public class PlaceResult {
   public final EntityPlayer target;
   public final double bestDMG;
   public final BlockPos calcPos;
   public final float[] rots;
   public final EnumFacing side;
   public final long createdAt;

   public PlaceResult(EntityPlayer target, double bestDMG, BlockPos calcPos, float[] rots) {
      this(target, bestDMG, calcPos, rots, (EnumFacing)null, System.currentTimeMillis());
   }

   public PlaceResult(EntityPlayer target, double bestDMG, BlockPos calcPos, float[] rots, EnumFacing side, long createdAt) {
      this.target = target;
      this.bestDMG = bestDMG;
      this.calcPos = calcPos;
      this.rots = rots;
      this.side = side;
      this.createdAt = createdAt;
   }
}
