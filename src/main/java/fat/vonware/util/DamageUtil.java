package fat.vonware.util;

import fat.vonware.mixin.mixins.accessor.IItemTool;
import java.util.Arrays;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemTool;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.CombatRules;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.RayTraceResult.Type;
import net.minecraft.world.Explosion;

public class DamageUtil implements Util {
   public static double getDamageInPercent(ItemStack stack) {
      double percent = (double)stack.getItemDamage() / (double)stack.getMaxDamage();
      if (percent == (double)0.0F) {
         return (double)100.0F;
      } else {
         return percent == (double)1.0F ? (double)0.0F : (double)100.0F - percent * (double)100.0F;
      }
   }

   public static float calculate(BlockPos pos, boolean destroyBlocks) {
      return calculate((double)((float)pos.getX() + 0.5F), (double)(pos.getY() + 1), (double)((float)pos.getZ() + 0.5F), mc.player, destroyBlocks);
   }

   public static float calculate(double x, double y, double z, EntityPlayer base, boolean destroyBlocks) {
      return calculate(x, y, z, base, base.getEntityBoundingBox(), destroyBlocks);
   }

   public static float calculate(Entity crystal, EntityPlayer base, boolean destroyBlocks) {
      return calculate(crystal.posX, crystal.posY, crystal.posZ, base, destroyBlocks);
   }

   public static RayTraceResult rayTraceBlocks(Vec3d vec31, Vec3d vec32, boolean stopOnLiquid, boolean ignoreBlockWithoutBoundingBox, boolean returnLastUncollidableBlock, boolean destroyBlocks) {
      int i = MathHelper.floor(vec32.x);
      int j = MathHelper.floor(vec32.y);
      int k = MathHelper.floor(vec32.z);
      int l = MathHelper.floor(vec31.x);
      int i2 = MathHelper.floor(vec31.y);
      int j2 = MathHelper.floor(vec31.z);
      BlockPos blockpos = new BlockPos(l, i2, j2);
      IBlockState iblockstate = mc.world.getBlockState(blockpos);
      Block block = iblockstate.getBlock();
      if ((!ignoreBlockWithoutBoundingBox || iblockstate.getCollisionBoundingBox(mc.world, blockpos) != Block.NULL_AABB) && block.canCollideCheck(iblockstate, stopOnLiquid)) {
         return iblockstate.collisionRayTrace(mc.world, blockpos, vec31, vec32);
      } else {
         RayTraceResult raytraceresult2 = null;
         int k2 = 200;

         while(k2-- >= 0) {
            if (Double.isNaN(vec31.x) || Double.isNaN(vec31.y) || Double.isNaN(vec31.z)) {
               return null;
            }

            if (l == i && i2 == j && j2 == k) {
               return returnLastUncollidableBlock ? raytraceresult2 : null;
            }

            boolean flag2 = true;
            boolean flag3 = true;
            boolean flag4 = true;
            double d0 = (double)999.0F;
            double d2 = (double)999.0F;
            double d3 = (double)999.0F;
            if (i > l) {
               d0 = (double)l + (double)1.0F;
            } else if (i < l) {
               d0 = (double)l + (double)0.0F;
            } else {
               flag2 = false;
            }

            if (j > i2) {
               d2 = (double)i2 + (double)1.0F;
            } else if (j < i2) {
               d2 = (double)i2 + (double)0.0F;
            } else {
               flag3 = false;
            }

            if (k > j2) {
               d3 = (double)j2 + (double)1.0F;
            } else if (k < j2) {
               d3 = (double)j2 + (double)0.0F;
            } else {
               flag4 = false;
            }

            double d4 = (double)999.0F;
            double d5 = (double)999.0F;
            double d6 = (double)999.0F;
            double d7 = vec32.x - vec31.x;
            double d8 = vec32.y - vec31.y;
            double d9 = vec32.z - vec31.z;
            if (flag2) {
               d4 = (d0 - vec31.x) / d7;
            }

            if (flag3) {
               d5 = (d2 - vec31.y) / d8;
            }

            if (flag4) {
               d6 = (d3 - vec31.z) / d9;
            }

            if (d4 == (double)-0.0F) {
               d4 = -1.0E-4;
            }

            if (d5 == (double)-0.0F) {
               d5 = -1.0E-4;
            }

            if (d6 == (double)-0.0F) {
               d6 = -1.0E-4;
            }

            EnumFacing enumfacing;
            if (d4 < d5 && d4 < d6) {
               enumfacing = i > l ? EnumFacing.WEST : EnumFacing.EAST;
               vec31 = new Vec3d(d0, vec31.y + d8 * d4, vec31.z + d9 * d4);
            } else if (d5 < d6) {
               enumfacing = j > i2 ? EnumFacing.DOWN : EnumFacing.UP;
               vec31 = new Vec3d(vec31.x + d7 * d5, d2, vec31.z + d9 * d5);
            } else {
               enumfacing = k > j2 ? EnumFacing.NORTH : EnumFacing.SOUTH;
               vec31 = new Vec3d(vec31.x + d7 * d6, vec31.y + d8 * d6, d3);
            }

            l = MathHelper.floor(vec31.x) - (enumfacing == EnumFacing.EAST ? 1 : 0);
            i2 = MathHelper.floor(vec31.y) - (enumfacing == EnumFacing.UP ? 1 : 0);
            j2 = MathHelper.floor(vec31.z) - (enumfacing == EnumFacing.SOUTH ? 1 : 0);
            blockpos = new BlockPos(l, i2, j2);
            IBlockState iblockstate2 = mc.world.getBlockState(blockpos);
            Block block2 = iblockstate2.getBlock();
            if (!ignoreBlockWithoutBoundingBox || iblockstate2.getMaterial() == Material.PORTAL || iblockstate2.getCollisionBoundingBox(mc.world, blockpos) != Block.NULL_AABB) {
               List<Block> resistantBlocks = Arrays.asList(Blocks.OBSIDIAN, Blocks.ANVIL, Blocks.ENCHANTING_TABLE, Blocks.ENDER_CHEST, Blocks.BEACON, Blocks.BEDROCK, Blocks.END_PORTAL_FRAME, Blocks.END_PORTAL, Blocks.BARRIER, Blocks.COMMAND_BLOCK, Blocks.CHAIN_COMMAND_BLOCK, Blocks.REPEATING_COMMAND_BLOCK, Blocks.STRUCTURE_BLOCK);
               boolean blockDestroy = !destroyBlocks || resistantBlocks.contains(block2);
               if (block2.canCollideCheck(iblockstate2, stopOnLiquid) && block2 != Blocks.WEB && blockDestroy) {
                  return iblockstate2.collisionRayTrace(mc.world, blockpos, vec31, vec32);
               }

               raytraceresult2 = new RayTraceResult(Type.MISS, vec31, enumfacing, blockpos);
            }
         }

         return returnLastUncollidableBlock ? raytraceresult2 : null;
      }
   }

   public static float getBlockDensity(Vec3d vec, AxisAlignedBB bb, boolean destroyBlocks) {
      double x = (double)1.0F / ((bb.maxX - bb.minX) * (double)2.0F + (double)1.0F);
      double y = (double)1.0F / ((bb.maxY - bb.minY) * (double)2.0F + (double)1.0F);
      double z = (double)1.0F / ((bb.maxZ - bb.minZ) * (double)2.0F + (double)1.0F);
      double xFloor = ((double)1.0F - Math.floor((double)1.0F / x) * x) / (double)2.0F;
      double zFloor = ((double)1.0F - Math.floor((double)1.0F / z) * z) / (double)2.0F;
      if (x >= (double)0.0F && y >= (double)0.0F && z >= (double)0.0F) {
         int air = 0;
         int traced = 0;

         for(float a = 0.0F; a <= 1.0F; a += (float)x) {
            for(float b = 0.0F; b <= 1.0F; b += (float)y) {
               for(float c = 0.0F; c <= 1.0F; c += (float)z) {
                  double xOff = bb.minX + (bb.maxX - bb.minX) * (double)a;
                  double yOff = bb.minY + (bb.maxY - bb.minY) * (double)b;
                  double zOff = bb.minZ + (bb.maxZ - bb.minZ) * (double)c;
                  RayTraceResult result = rayTraceBlocks(new Vec3d(xOff + xFloor, yOff, zOff + zFloor), vec, false, true, true, destroyBlocks);
                  if (result == null || result.typeOfHit == Type.MISS) {
                     ++air;
                  }

                  ++traced;
               }
            }
         }

         return (float)air / (float)traced;
      } else {
         return 0.0F;
      }
   }

   public static float calculate(double x, double y, double z, EntityPlayer base, AxisAlignedBB boundingBox, boolean destroyBlocks) {
      if (base != null && base.world != null && boundingBox != null) {
         if (base.capabilities.isCreativeMode) {
            return 0.0F;
         } else {
            double distance = base.getDistance(x, y, z) / (double)12.0F;
            if (distance > (double)1.0F) {
               return 0.0F;
            } else {
               float density = getBlockDensity(new Vec3d(x, y, z), boundingBox, destroyBlocks);
               double densityDistance;
               distance = densityDistance = ((double)1.0F - distance) * (double)density;
               float damage = CombatRules.getDamageAfterAbsorb(getDifficultyMultiplier((float)((densityDistance * densityDistance + distance) / (double)2.0F * (double)7.0F * (double)12.0F + (double)1.0F)), (float)base.getTotalArmorValue(), (float)base.getEntityAttribute(SharedMonsterAttributes.ARMOR_TOUGHNESS).getAttributeValue());
               int modifierDamage = EnchantmentHelper.getEnchantmentModifierDamage(base.inventory.armorInventory, createExplosionDamageSource(base, x, y, z));
               if (modifierDamage > 0) {
                  damage = CombatRules.getDamageAfterMagicAbsorb(damage, (float)modifierDamage);
               }

               PotionEffect resistance = base.getActivePotionEffect(MobEffects.RESISTANCE);
               if (resistance != null) {
                  damage = damage * (float)(25 - (resistance.getAmplifier() + 1) * 5) / 25.0F;
               }

               return Math.max(damage, 0.0F);
            }
         }
      } else {
         return 0.0F;
      }
   }

   public static float calculate(double x, double y, double z, EntityPlayer base) {
      return calculate(x, y, z, base, base.getEntityBoundingBox());
   }

   public static float calculate(double x, double y, double z, EntityPlayer base, AxisAlignedBB boundingBox) {
      if (base != null && base.world != null && boundingBox != null) {
         if (base.capabilities.isCreativeMode) {
            return 0.0F;
         } else {
            double distance = base.getDistance(x, y, z) / (double)12.0F;
            if (distance > (double)1.0F) {
               return 0.0F;
            } else {
               float density = getBlockDensity(new Vec3d(x, y, z), boundingBox);
               double densityDistance;
               distance = densityDistance = ((double)1.0F - distance) * (double)density;
               float damage = CombatRules.getDamageAfterAbsorb(getDifficultyMultiplier((float)((densityDistance * densityDistance + distance) / (double)2.0F * (double)7.0F * (double)12.0F + (double)1.0F)), (float)base.getTotalArmorValue(), (float)base.getEntityAttribute(SharedMonsterAttributes.ARMOR_TOUGHNESS).getAttributeValue());
               int modifierDamage = EnchantmentHelper.getEnchantmentModifierDamage(base.inventory.armorInventory, createExplosionDamageSource(base, x, y, z));
               if (modifierDamage > 0) {
                  damage = CombatRules.getDamageAfterMagicAbsorb(damage, (float)modifierDamage);
               }

               PotionEffect resistance = base.getActivePotionEffect(MobEffects.RESISTANCE);
               if (resistance != null) {
                  damage = damage * (float)(25 - (resistance.getAmplifier() + 1) * 5) / 25.0F;
               }

               return Math.max(damage, 0.0F);
            }
         }
      } else {
         return 0.0F;
      }
   }

   private static DamageSource createExplosionDamageSource(EntityPlayer base, double x, double y, double z) {
      if (base != null && base.world != null) {
         DamageSource source = DamageSource.causeExplosionDamage(new Explosion(base.world, (Entity)null, x, y, z, 6.0F, false, true));
         if (source != null) {
            return source;
         }
      }

      return DamageSource.causeExplosionDamage((EntityLivingBase)null);
   }

   public static float getDifficultyMultiplier(float distance) {
      switch (mc.world.getDifficulty()) {
         case PEACEFUL:
            return 0.0F;
         case EASY:
            return Math.min(distance / 2.0F + 1.0F, distance);
         case HARD:
            return distance * 3.0F / 2.0F;
         default:
            return distance;
      }
   }

   public static float getBlockDensity(Vec3d vec, AxisAlignedBB bb) {
      double x = (double)1.0F / ((bb.maxX - bb.minX) * (double)2.0F + (double)1.0F);
      double y = (double)1.0F / ((bb.maxY - bb.minY) * (double)2.0F + (double)1.0F);
      double z = (double)1.0F / ((bb.maxZ - bb.minZ) * (double)2.0F + (double)1.0F);
      double xFloor = ((double)1.0F - Math.floor((double)1.0F / x) * x) / (double)2.0F;
      double zFloor = ((double)1.0F - Math.floor((double)1.0F / z) * z) / (double)2.0F;
      if (x >= (double)0.0F && y >= (double)0.0F && z >= (double)0.0F) {
         int air = 0;
         int traced = 0;

         for(float a = 0.0F; a <= 1.0F; a += (float)x) {
            for(float b = 0.0F; b <= 1.0F; b += (float)y) {
               for(float c = 0.0F; c <= 1.0F; c += (float)z) {
                  double xOff = bb.minX + (bb.maxX - bb.minX) * (double)a;
                  double yOff = bb.minY + (bb.maxY - bb.minY) * (double)b;
                  double zOff = bb.minZ + (bb.maxZ - bb.minZ) * (double)c;
                  RayTraceResult result = rayTraceBlocks(new Vec3d(xOff + xFloor, yOff, zOff + zFloor), vec, false, true, true);
                  if (result == null || result.typeOfHit == Type.MISS) {
                     ++air;
                  }

                  ++traced;
               }
            }
         }

         return (float)air / (float)traced;
      } else {
         return 0.0F;
      }
   }

   public static boolean canBreakWeakness(boolean checkStack) {
      if (!mc.player.isPotionActive(MobEffects.WEAKNESS)) {
         return true;
      } else {
         int strengthAmp = 0;
         PotionEffect effect = mc.player.getActivePotionEffect(MobEffects.STRENGTH);
         if (effect != null) {
            strengthAmp = effect.getAmplifier();
         }

         return strengthAmp >= 1 || checkStack && canBreakWeakness(mc.player.getHeldItemMainhand());
      }
   }

   public static boolean canBreakWeakness(ItemStack stack) {
      if (stack.getItem() instanceof ItemSword) {
         return true;
      } else if (stack.getItem() instanceof ItemTool) {
         IItemTool tool = (IItemTool)stack.getItem();
         return tool.getAttackDamage() > 4.0F;
      } else {
         return false;
      }
   }

   public static int findAntiWeakness() {
      int slot = -1;

      for(int i = 8; i > -1; --i) {
         if (canBreakWeakness(mc.player.inventory.getStackInSlot(i))) {
            slot = i;
            if (mc.player.inventory.currentItem == i) {
               break;
            }
         }
      }

      return slot;
   }

   public static RayTraceResult rayTraceBlocks(Vec3d vec31, Vec3d vec32, boolean stopOnLiquid, boolean ignoreBlockWithoutBoundingBox, boolean returnLastUncollidableBlock) {
      int i = MathHelper.floor(vec32.x);
      int j = MathHelper.floor(vec32.y);
      int k = MathHelper.floor(vec32.z);
      int l = MathHelper.floor(vec31.x);
      int i2 = MathHelper.floor(vec31.y);
      int j2 = MathHelper.floor(vec31.z);
      BlockPos blockpos = new BlockPos(l, i2, j2);
      IBlockState iblockstate = mc.world.getBlockState(blockpos);
      Block block = iblockstate.getBlock();
      if ((!ignoreBlockWithoutBoundingBox || iblockstate.getCollisionBoundingBox(mc.world, blockpos) != Block.NULL_AABB) && block.canCollideCheck(iblockstate, stopOnLiquid)) {
         return iblockstate.collisionRayTrace(mc.world, blockpos, vec31, vec32);
      } else {
         RayTraceResult raytraceresult2 = null;
         int k2 = 200;

         while(k2-- >= 0) {
            if (Double.isNaN(vec31.x) || Double.isNaN(vec31.y) || Double.isNaN(vec31.z)) {
               return null;
            }

            if (l == i && i2 == j && j2 == k) {
               return returnLastUncollidableBlock ? raytraceresult2 : null;
            }

            boolean flag2 = true;
            boolean flag3 = true;
            boolean flag4 = true;
            double d0 = (double)999.0F;
            double d2 = (double)999.0F;
            double d3 = (double)999.0F;
            if (i > l) {
               d0 = (double)l + (double)1.0F;
            } else if (i < l) {
               d0 = (double)l + (double)0.0F;
            } else {
               flag2 = false;
            }

            if (j > i2) {
               d2 = (double)i2 + (double)1.0F;
            } else if (j < i2) {
               d2 = (double)i2 + (double)0.0F;
            } else {
               flag3 = false;
            }

            if (k > j2) {
               d3 = (double)j2 + (double)1.0F;
            } else if (k < j2) {
               d3 = (double)j2 + (double)0.0F;
            } else {
               flag4 = false;
            }

            double d4 = (double)999.0F;
            double d5 = (double)999.0F;
            double d6 = (double)999.0F;
            double d7 = vec32.x - vec31.x;
            double d8 = vec32.y - vec31.y;
            double d9 = vec32.z - vec31.z;
            if (flag2) {
               d4 = (d0 - vec31.x) / d7;
            }

            if (flag3) {
               d5 = (d2 - vec31.y) / d8;
            }

            if (flag4) {
               d6 = (d3 - vec31.z) / d9;
            }

            if (d4 == (double)-0.0F) {
               d4 = -1.0E-4;
            }

            if (d5 == (double)-0.0F) {
               d5 = -1.0E-4;
            }

            if (d6 == (double)-0.0F) {
               d6 = -1.0E-4;
            }

            EnumFacing enumfacing;
            if (d4 < d5 && d4 < d6) {
               enumfacing = i > l ? EnumFacing.WEST : EnumFacing.EAST;
               vec31 = new Vec3d(d0, vec31.y + d8 * d4, vec31.z + d9 * d4);
            } else if (d5 < d6) {
               enumfacing = j > i2 ? EnumFacing.DOWN : EnumFacing.UP;
               vec31 = new Vec3d(vec31.x + d7 * d5, d2, vec31.z + d9 * d5);
            } else {
               enumfacing = k > j2 ? EnumFacing.NORTH : EnumFacing.SOUTH;
               vec31 = new Vec3d(vec31.x + d7 * d6, vec31.y + d8 * d6, d3);
            }

            l = MathHelper.floor(vec31.x) - (enumfacing == EnumFacing.EAST ? 1 : 0);
            i2 = MathHelper.floor(vec31.y) - (enumfacing == EnumFacing.UP ? 1 : 0);
            j2 = MathHelper.floor(vec31.z) - (enumfacing == EnumFacing.SOUTH ? 1 : 0);
            blockpos = new BlockPos(l, i2, j2);
            IBlockState iblockstate2 = mc.world.getBlockState(blockpos);
            Block block2 = iblockstate2.getBlock();
            if (!ignoreBlockWithoutBoundingBox || iblockstate2.getMaterial() == Material.PORTAL || iblockstate2.getCollisionBoundingBox(mc.world, blockpos) != Block.NULL_AABB) {
               if (block2.canCollideCheck(iblockstate2, stopOnLiquid) && block2 != Blocks.WEB) {
                  return iblockstate2.collisionRayTrace(mc.world, blockpos, vec31, vec32);
               }

               raytraceresult2 = new RayTraceResult(Type.MISS, vec31, enumfacing, blockpos);
            }
         }

         return returnLastUncollidableBlock ? raytraceresult2 : null;
      }
   }

   public static float calculate(Entity crystal, EntityPlayer base) {
      return calculate(crystal.posX, crystal.posY, crystal.posZ, base);
   }

   public static float getBlastReduction(EntityLivingBase entity, float damageI, Explosion explosion) {
      if (entity instanceof EntityPlayer) {
         EntityPlayer ep = (EntityPlayer)entity;
         DamageSource ds = DamageSource.causeExplosionDamage(explosion);
         float var9 = CombatRules.getDamageAfterAbsorb(damageI, (float)ep.getTotalArmorValue(), (float)ep.getEntityAttribute(SharedMonsterAttributes.ARMOR_TOUGHNESS).getAttributeValue());
         int k = 0;

         try {
            k = EnchantmentHelper.getEnchantmentModifierDamage(ep.getArmorInventoryList(), ds);
         } catch (Exception var8) {
         }

         float f = MathHelper.clamp((float)k, 0.0F, 20.0F);
         var9 *= 1.0F - f / 25.0F;
         if (entity.isPotionActive(MobEffects.RESISTANCE)) {
            var9 -= var9 / 4.0F;
         }

         var9 = Math.max(var9, 0.0F);
         return var9;
      } else {
         float damage = CombatRules.getDamageAfterAbsorb(damageI, (float)entity.getTotalArmorValue(), (float)entity.getEntityAttribute(SharedMonsterAttributes.ARMOR_TOUGHNESS).getAttributeValue());
         return damage;
      }
   }

   public static float getDamageMultiplied(float damage) {
      int diff = mc.world.getDifficulty().getId();
      return damage * (diff == 0 ? 0.0F : (diff == 2 ? 1.0F : (diff == 1 ? 0.5F : 1.5F)));
   }

   public static float calculateDamage(BlockPos pos, Entity entity) {
      return calculateDamage((double)pos.getX() + (double)0.5F, (double)(pos.getY() + 1), (double)pos.getZ() + (double)0.5F, entity);
   }

   public static float calculateDamage(double posX, double posY, double posZ, Entity entity) {
      float doubleExplosionSize = 12.0F;
      double distancedsize = entity.getDistance(posX, posY, posZ) / (double)12.0F;
      Vec3d vec3d = new Vec3d(posX, posY, posZ);
      double blockDensity = (double)0.0F;

      try {
         blockDensity = (double)entity.world.getBlockDensity(vec3d, entity.getEntityBoundingBox());
      } catch (Exception var18) {
      }

      double v = ((double)1.0F - distancedsize) * blockDensity;
      float damage = (float)((int)((v * v + v) / (double)2.0F * (double)7.0F * (double)12.0F + (double)1.0F));
      double finald = (double)1.0F;
      if (entity instanceof EntityLivingBase) {
         finald = (double)getBlastReduction((EntityLivingBase)entity, getDamageMultiplied(damage), new Explosion(mc.world, (Entity)null, posX, posY, posZ, 6.0F, false, true));
      }

      return (float)finald;
   }
}
