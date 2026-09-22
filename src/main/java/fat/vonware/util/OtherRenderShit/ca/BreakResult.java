package fat.vonware.util.OtherRenderShit.ca;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

public class BreakResult {
   EntityPlayer target;
   double bestDMG;
   public Entity calcCrystal;
   public float[] rots;

   public BreakResult(EntityPlayer target, double bestDMG, Entity calcCrystal, float[] rots) {
      this.target = target;
      this.bestDMG = bestDMG;
      this.calcCrystal = calcCrystal;
      this.rots = rots;
   }
}
