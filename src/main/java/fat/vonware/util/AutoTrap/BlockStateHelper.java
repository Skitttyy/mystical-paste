package fat.vonware.util.AutoTrap;

import fat.vonware.util.Util;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;

public class BlockStateHelper implements Util, IBlockStateHelper {
   private final Map<BlockPos, IBlockState> states;
   private final Supplier<IBlockAccess> world;

   public BlockStateHelper() {
      this(new HashMap());
   }

   public BlockStateHelper(Supplier<IBlockAccess> world) {
      this(new HashMap(), world);
   }

   public BlockStateHelper(Map<BlockPos, IBlockState> stateMap) {
      this(stateMap, () -> mc.world);
   }

   public BlockStateHelper(Map<BlockPos, IBlockState> stateMap, Supplier<IBlockAccess> world) {
      this.states = stateMap;
      this.world = world;
   }

   public IBlockState getBlockState(BlockPos pos) {
      IBlockState state = (IBlockState)this.states.get(pos);
      return state == null ? ((IBlockAccess)this.world.get()).getBlockState(pos) : state;
   }

   public void addBlockState(BlockPos pos, IBlockState state) {
      this.states.putIfAbsent(pos.toImmutable(), state);
   }

   public void delete(BlockPos pos) {
      this.states.remove(pos);
   }

   public void clearAllStates() {
      this.states.clear();
   }

   public TileEntity getTileEntity(BlockPos pos) {
      return ((IBlockAccess)this.world.get()).getTileEntity(pos);
   }

   public int getCombinedLight(BlockPos pos, int lightValue) {
      return ((IBlockAccess)this.world.get()).getCombinedLight(pos, lightValue);
   }

   public boolean isAirBlock(BlockPos pos) {
      return this.getBlockState(pos).getBlock().isAir(this.getBlockState(pos), this, pos);
   }

   public Biome getBiome(BlockPos pos) {
      return ((IBlockAccess)this.world.get()).getBiome(pos);
   }

   public int getStrongPower(BlockPos pos, EnumFacing direction) {
      return this.getBlockState(pos).getStrongPower(this, pos, direction);
   }

   public WorldType getWorldType() {
      return ((IBlockAccess)this.world.get()).getWorldType();
   }

   public boolean isSideSolid(BlockPos pos, EnumFacing side, boolean _default) {
      if (!mc.world.isValid(pos)) {
         return _default;
      } else {
         Chunk chunk = mc.world.getChunk(pos);
         return chunk != null && !chunk.isEmpty() ? this.getBlockState(pos).isSideSolid(this, pos, side) : _default;
      }
   }
}
