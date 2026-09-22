package fat.vonware.ducks;

import net.minecraft.util.math.BlockPos;

public interface IPlayerControllerMP {
   void syncItem();

   void setBlockHitDelay(int var1);

   void setIsHittingBlock(boolean var1);

   void setCurBlockDamageMP(float var1);

   boolean getIsHittingPosition(BlockPos var1);
}
