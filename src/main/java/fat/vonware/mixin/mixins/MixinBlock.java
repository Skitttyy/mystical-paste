package fat.vonware.mixin.mixins;

import com.google.common.collect.UnmodifiableIterator;
import fat.vonware.ducks.IBlock;
import fat.vonware.util.Util;
import java.util.Arrays;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Block.class})
public abstract class MixinBlock implements IBlock, Util {
   @Unique
   private String[] harvestToolNonForge;
   @Unique
   private int[] harvestLevelNonForge;
   @Shadow
   protected float blockHardness;

   @Shadow
   protected static void addCollisionBoxToList(BlockPos pos, AxisAlignedBB entityBox, List<AxisAlignedBB> cBoxes, AxisAlignedBB blockBox) {
      throw new IllegalStateException("MixinBlock.addCollisionBoxToList has not been shadowed");
   }

   @Shadow
   public abstract BlockStateContainer getBlockState();

   @Shadow
   public abstract int getMetaFromState(IBlockState var1);

   public float getHardness() {
      return this.blockHardness;
   }

   @Unique
   public void setHarvestLevelNonForge(String toolClass, int level) {
      int idx;
      for(UnmodifiableIterator var3 = this.getBlockState().getValidStates().iterator(); var3.hasNext(); this.harvestLevelNonForge[idx] = level) {
         IBlockState state = (IBlockState)var3.next();
         idx = this.getMetaFromState(state);
         this.harvestToolNonForge[idx] = toolClass;
      }

   }

   @Unique
   public String getHarvestToolNonForge(IBlockState state) {
      return this.harvestToolNonForge[this.getMetaFromState(state)];
   }

   @Unique
   public int getHarvestLevelNonForge(IBlockState state) {
      return this.harvestLevelNonForge[this.getMetaFromState(state)];
   }

   @Inject(
      method = {"<init>(Lnet/minecraft/block/material/Material;Lnet/minecraft/block/material/MapColor;)V"},
      at = {@At("RETURN")}
   )
   public void ctrHook(Material blockMaterialIn, MapColor blockMapColorIn, CallbackInfo ci) {
      this.harvestToolNonForge = new String[16];
      this.harvestLevelNonForge = new int[16];
      Arrays.fill(this.harvestLevelNonForge, -1);
   }
}
