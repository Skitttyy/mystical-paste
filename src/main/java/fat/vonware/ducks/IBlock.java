package fat.vonware.ducks;

import net.minecraft.block.state.IBlockState;

public interface IBlock {
   void setHarvestLevelNonForge(String var1, int var2);

   String getHarvestToolNonForge(IBlockState var1);

   int getHarvestLevelNonForge(IBlockState var1);

   float getHardness();
}
