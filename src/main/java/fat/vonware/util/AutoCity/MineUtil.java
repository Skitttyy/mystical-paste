package fat.vonware.util.AutoCity;

import fat.vonware.ducks.IBlock;
import fat.vonware.mixin.mixins.accessor.IItemTool;
import fat.vonware.util.Util;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.init.Blocks;
import net.minecraft.init.Enchantments;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemSpade;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;

public class MineUtil implements Util {
   public static boolean canBreak(BlockPos pos) {
      return canBreak(mc.world.getBlockState(pos), pos);
   }

   public static int findBestTool(BlockPos pos) {
      return findBestTool(pos, mc.world.getBlockState(pos));
   }

   public static int findBestTool(BlockPos pos, IBlockState state) {
      int result = mc.player.inventory.currentItem;
      if (state.getBlockHardness(mc.world, pos) > 0.0F) {
         double speed = getSpeed(state, mc.player.getHeldItemMainhand());

         for(int i = 0; i < 9; ++i) {
            ItemStack stack = mc.player.inventory.getStackInSlot(i);
            double stackSpeed = getSpeed(state, stack);
            if (stackSpeed > speed) {
               speed = stackSpeed;
               result = i;
            }
         }
      }

      return result;
   }

   public static double getSpeed(IBlockState state, ItemStack stack) {
      double str = (double)stack.getDestroySpeed(state);
      int effect = EnchantmentHelper.getEnchantmentLevel(Enchantments.EFFICIENCY, stack);
      return Math.max(str + (str > (double)1.0F ? (double)(effect * effect) + (double)1.0F : (double)0.0F), (double)0.0F);
   }

   public static boolean canBreak(IBlockState state, BlockPos pos) {
      return state.getBlockHardness(mc.world, pos) != -1.0F && state.getBlock() != Blocks.AIR && !state.getMaterial().isLiquid();
   }

   public static float getDamage(ItemStack stack, BlockPos pos, boolean onGround) {
      IBlockState state = mc.world.getBlockState(pos);
      return getDamage(state, stack, pos, onGround);
   }

   public static float getDamage(IBlockState state, ItemStack stack, boolean onGround) {
      return getDigSpeed(stack, state, onGround) / (((IBlock)state.getBlock()).getHardness() * (float)(canHarvestBlock(state, stack) ? 30 : 100));
   }

   public static float getDamage(IBlockState state, ItemStack stack, BlockPos pos, boolean onGround) {
      return getDigSpeed(stack, state, onGround) / (state.getBlockHardness(mc.world, pos) * (float)(canHarvestBlock(pos, stack) ? 30 : 100));
   }

   public static boolean canHarvestBlock(BlockPos pos, ItemStack stack) {
      IBlockState state = mc.world.getBlockState(pos);
      state = state.getActualState(mc.world, pos);
      return canHarvestBlock(state, stack);
   }

   public static boolean canHarvestBlock(IBlockState state, ItemStack stack) {
      Block block = state.getBlock();
      if (state.getMaterial().isToolNotRequired()) {
         return true;
      } else if (stack.isEmpty()) {
         return stack.canHarvestBlock(state);
      } else {
         String tool = ((IBlock)block).getHarvestToolNonForge(state);
         if (tool == null) {
            return stack.canHarvestBlock(state);
         } else {
            int toolLevel = -1;
            if (stack.getItem() instanceof IItemTool) {
               String toolClass = null;
               if (stack.getItem() instanceof ItemPickaxe) {
                  toolClass = "pickaxe";
               } else if (stack.getItem() instanceof ItemAxe) {
                  toolClass = "axe";
               } else if (stack.getItem() instanceof ItemSpade) {
                  toolClass = "shovel";
               }

               if (tool.equals(toolClass)) {
                  toolLevel = ((IItemTool)stack.getItem()).getToolMaterial().getHarvestLevel();
               }
            }

            if (toolLevel < 0) {
               return stack.canHarvestBlock(state);
            } else {
               return toolLevel >= ((IBlock)block).getHarvestLevelNonForge(state);
            }
         }
      }
   }

   private static float getDigSpeed(ItemStack stack, IBlockState state, boolean onGround) {
      float digSpeed = 1.0F;
      if (!stack.isEmpty()) {
         digSpeed *= stack.getDestroySpeed(state);
      }

      if (digSpeed > 1.0F) {
         int i = EnchantmentHelper.getEnchantmentLevel(Enchantments.EFFICIENCY, stack);
         if (i > 0 && !stack.isEmpty()) {
            digSpeed += (float)(i * i + 1);
         }
      }

      if (mc.player.isPotionActive(MobEffects.HASTE)) {
         digSpeed *= 1.0F + (float)(mc.player.getActivePotionEffect(MobEffects.HASTE).getAmplifier() + 1) * 0.2F;
      }

      if (mc.player.isPotionActive(MobEffects.MINING_FATIGUE)) {
         float miningFatigue = 0.0F;
         switch (mc.player.getActivePotionEffect(MobEffects.MINING_FATIGUE).getAmplifier()) {
            case 0:
               miningFatigue = 0.3F;
               break;
            case 1:
               miningFatigue = 0.09F;
               break;
            case 2:
               miningFatigue = 0.0027F;
               break;
            default:
               miningFatigue = 8.1E-4F;
         }

         digSpeed *= miningFatigue;
      }

      if (mc.player.isInsideOfMaterial(Material.WATER) && !EnchantmentHelper.getAquaAffinityModifier(mc.player)) {
         digSpeed /= 5.0F;
      }

      if (!onGround) {
         digSpeed /= 5.0F;
      }

      return digSpeed < 0.0F ? 0.0F : digSpeed;
   }
}
