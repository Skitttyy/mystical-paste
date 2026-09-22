package fat.vonware.util.OtherRenderShit.ca;

import fat.vonware.util.Util;
import net.minecraft.entity.player.EntityPlayer;

public class PredictPlayer implements Util {
   private final EntityPlayer oldPlayer;
   private final EntityPlayer target;

   public EntityPlayer getOldPlayer() {
      return this.oldPlayer;
   }

   public EntityPlayer getTarget() {
      return this.target;
   }

   public PredictPlayer(EntityPlayer oldPlayer, EntityPlayer target) {
      this.oldPlayer = oldPlayer;
      this.target = target;
   }
}
