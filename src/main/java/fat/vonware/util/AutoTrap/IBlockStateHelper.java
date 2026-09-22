package fat.vonware.util.AutoTrap;

import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

public interface IBlockStateHelper extends IBlockAccess {
   default void addAir(BlockPos pos) {
      this.addBlockState(pos, Blocks.AIR.getDefaultState());
   }

   void addBlockState(BlockPos var1, IBlockState var2);

   void delete(BlockPos var1);

   void clearAllStates();
}
