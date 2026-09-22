package fat.vonware.util.OtherRenderShit.ca;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;

public class Result {
   public EntityPlayer target;
   public double bestPlaceDMG;
   public double bestBreakDMG;
   public BlockPos calcPos;
   public Entity calcCrystal;
   public float[] placeRots;
   public float[] breakRots;

   public Result(EntityPlayer target, double bestPlaceDMG, BlockPos calcPos, float[] placeRots, Entity calcCrystal, double bestBreakDMG, float[] breakRots) {
      this.target = target;
      this.bestPlaceDMG = bestPlaceDMG;
      this.calcPos = calcPos;
      this.calcCrystal = calcCrystal;
      this.bestBreakDMG = bestBreakDMG;
      this.placeRots = placeRots;
      this.breakRots = breakRots;
   }

   public Result(BreakResult breakResult, PlaceResult placeResult) {
      this.target = breakResult.target;
      this.bestPlaceDMG = placeResult.bestDMG;
      this.calcPos = placeResult.calcPos;
      this.calcCrystal = breakResult.calcCrystal;
      this.bestBreakDMG = breakResult.bestDMG;
      this.placeRots = placeResult.rots;
      this.breakRots = breakResult.rots;
   }

   public double getDamage() {
      return Math.max(this.bestBreakDMG, this.bestPlaceDMG);
   }
}
