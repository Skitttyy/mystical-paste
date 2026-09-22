package fat.vonware.util.Phase;

import fat.vonware.util.BlockUtil;
import fat.vonware.util.Util;
import fat.vonware.util.HoleFill.other.RayTracer;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.block.BlockAnvil;
import net.minecraft.block.BlockBed;
import net.minecraft.block.BlockWeb;
import net.minecraft.client.Minecraft;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.item.EntityEnderCrystal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.MobEffects;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.network.play.client.CPacketUseEntity;
import net.minecraft.util.CombatRules;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Explosion;
import net.minecraft.world.IBlockAccess;

public class CrystalUtil implements Util {
   public static List<Integer> hitCrystals = new ArrayList();
   public static List<BlockPos> placedPositions = new ArrayList();

   public static List<Entity> getLoadedCrystalsInRange(double range) {
      try {
         List<Entity> copy = new ArrayList(mc.world.loadedEntityList);
         return (List)copy.stream().filter((it) -> it instanceof EntityEnderCrystal).filter((it) -> (double)it.getDistance(mc.player) < range).collect(Collectors.toList());
      } catch (Exception var3) {
         return new ArrayList();
      }
   }

   public static float calculateDamage(double posX, double posY, double posZ, Entity entity, double moveFactor) {
      try {
         Vec3d applied = new Vec3d(entity.posX + entity.motionX * moveFactor, entity.posY + entity.motionY * moveFactor, entity.posZ + entity.motionZ * moveFactor);
         double factor = ((double)1.0F - applied.distanceTo(new Vec3d(posX, posY, posZ)) / (double)12.0F) * (double)getBlockDensity(new Vec3d(posX, posY, posZ), entity.getEntityBoundingBox(), mc.world, true, true, true, false, false);
         float calculatedDamage = (float)((int)((factor * factor + factor) / (double)2.0F * (double)7.0F * (double)12.0F + (double)1.0F));
         double damage = (double)1.0F;
         if (entity instanceof EntityLivingBase) {
            damage = (double)getBlastReduction((EntityLivingBase)entity, calculatedDamage * (Minecraft.getMinecraft().world.getDifficulty().getId() == 0 ? 0.0F : (Minecraft.getMinecraft().world.getDifficulty().getId() == 2 ? 1.0F : (Minecraft.getMinecraft().world.getDifficulty().getId() == 1 ? 0.5F : 1.5F))), new Explosion(Minecraft.getMinecraft().world, (Entity)null, posX, posY, posZ, 6.0F, false, true));
         }

         return (float)damage;
      } catch (Exception var15) {
         return 0.0F;
      }
   }

   public static boolean ncpCrystalCheck(double crystalY) {
      double eyes = mc.player.posY + (double)mc.player.getEyeHeight();
      double distance = eyes - crystalY;
      return distance <= (double)3.62F;
   }

   public static boolean canPlaceCrystal(BlockPos pos, boolean one13, boolean placeBlocks) {
      return (mc.world.getBlockState(pos).getBlock() == Blocks.OBSIDIAN || mc.world.getBlockState(pos).getBlock() == Blocks.BEDROCK || placeBlocks && BlockUtil.canPlaceBlock(pos)) && mc.world.getBlockState(pos.add(0, 1, 0)).getBlock() == Blocks.AIR && (mc.world.getBlockState(pos.add(0, 2, 0)).getBlock() == Blocks.AIR || one13);
   }

   public static float calcPlaceDmg(double posX, double posY, double posZ, Entity entity, double moveFactor, boolean mining) {
      try {
         Vec3d applied = new Vec3d(entity.posX + entity.motionX * moveFactor, entity.posY + entity.motionY * moveFactor, entity.posZ + entity.motionZ * moveFactor);
         double factor = ((double)1.0F - applied.distanceTo(new Vec3d(posX, posY, posZ)) / (double)12.0F) * (double)getBlockDensity(new Vec3d(posX, posY, posZ), entity.getEntityBoundingBox(), mc.world, true, true, true, false, mining);
         float calculatedDamage = (float)((int)((factor * factor + factor) / (double)2.0F * (double)7.0F * (double)12.0F + (double)1.0F));
         double damage = (double)1.0F;
         if (entity instanceof EntityLivingBase) {
            damage = (double)getBlastReduction((EntityLivingBase)entity, calculatedDamage * (Minecraft.getMinecraft().world.getDifficulty().getId() == 0 ? 0.0F : (Minecraft.getMinecraft().world.getDifficulty().getId() == 2 ? 1.0F : (Minecraft.getMinecraft().world.getDifficulty().getId() == 1 ? 0.5F : 1.5F))), new Explosion(Minecraft.getMinecraft().world, (Entity)null, posX, posY, posZ, 6.0F, false, true));
         }

         return (float)damage;
      } catch (Exception var16) {
         return 0.0F;
      }
   }

   public static float calcPlaceDmgAt(double posX, double posY, double posZ, Entity entity, double tx, double ty, double tz, boolean mining) {
      try {
         double dx = tx - entity.posX;
         double dy = ty - entity.posY;
         double dz = tz - entity.posZ;
         AxisAlignedBB shiftedBB = entity.getEntityBoundingBox().offset(dx, dy, dz);
         double factor = ((double)1.0F - (new Vec3d(tx, ty, tz)).distanceTo(new Vec3d(posX, posY, posZ)) / (double)12.0F) * (double)getBlockDensity(new Vec3d(posX, posY, posZ), shiftedBB, mc.world, true, true, true, false, mining);
         float calculatedDamage = (float)((int)((factor * factor + factor) / (double)2.0F * (double)7.0F * (double)12.0F + (double)1.0F));
         double damage = (double)1.0F;
         if (entity instanceof EntityLivingBase) {
            damage = (double)getBlastReduction((EntityLivingBase)entity, calculatedDamage * (mc.world.getDifficulty().getId() == 0 ? 0.0F : (mc.world.getDifficulty().getId() == 1 ? 0.5F : (mc.world.getDifficulty().getId() == 2 ? 1.0F : 1.5F))), new Explosion(mc.world, (Entity)null, posX, posY, posZ, 6.0F, false, true));
         }

         return (float)damage;
      } catch (Exception var26) {
         return 0.0F;
      }
   }

   public static float getBlastReduction(EntityLivingBase entity, float damage, Explosion explosion) {
      if (entity instanceof EntityPlayer) {
         damage = CombatRules.getDamageAfterAbsorb(damage, (float)entity.getTotalArmorValue(), (float)entity.getEntityAttribute(SharedMonsterAttributes.ARMOR_TOUGHNESS).getAttributeValue());
         damage *= 1.0F - MathHelper.clamp((float)EnchantmentHelper.getEnchantmentModifierDamage(entity.getArmorInventoryList(), DamageSource.causeExplosionDamage(explosion)), 0.0F, 20.0F) / 25.0F;
         if (entity.isPotionActive(MobEffects.RESISTANCE)) {
            damage -= damage / 4.0F;
         }

         return damage;
      } else {
         damage = CombatRules.getDamageAfterAbsorb(damage, (float)entity.getTotalArmorValue(), (float)entity.getEntityAttribute(SharedMonsterAttributes.ARMOR_TOUGHNESS).getAttributeValue());
         return damage;
      }
   }

   public static float getBlockDensity(Vec3d vec, AxisAlignedBB bb, IBlockAccess world, boolean ignoreWebs, boolean ignoreBeds, boolean terrainCalc, boolean anvils, boolean mining) {
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
                  RayTraceResult result = rayTraceBlocks(new Vec3d(xOff + xFloor, yOff, zOff + zFloor), vec, world, false, false, false, ignoreWebs, ignoreBeds, terrainCalc, anvils, mining);
                  if (result == null) {
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

   public static RayTraceResult rayTraceBlocks(Vec3d start, Vec3d end, IBlockAccess world, boolean stopOnLiquid, boolean ignoreNoBox, boolean lastUncollidableBlock, boolean ignoreWebs, boolean ignoreBeds, boolean terrainCalc, boolean anvils, boolean mining) {
      return RayTracer.trace(mc.world, world, start, end, stopOnLiquid, ignoreNoBox, lastUncollidableBlock, (b, p) -> (!terrainCalc || b.getExplosionResistance(mc.player) >= 100.0F || p.distanceSq(end.x, end.y, end.z) > (double)36.0F) && (!ignoreBeds || !(b instanceof BlockBed)) && (!ignoreWebs || !(b instanceof BlockWeb)) || anvils && b instanceof BlockAnvil);
   }

   public static void breakCrystal(EntityEnderCrystal entityEnderCrystal, boolean Packet) {
      breakCrystal(entityEnderCrystal, Packet, (float[])null, false);
   }

   public static void breakCrystal(EntityEnderCrystal entityEnderCrystal, boolean Packet, float[] rotations, boolean packetRotate) {
      if (packetRotate && rotations != null) {
         mc.getConnection().sendPacket(new CPacketPlayer.Rotation(rotations[0], rotations[1], mc.player.onGround));
      }

      if (Packet) {
         mc.getConnection().sendPacket(new CPacketUseEntity(entityEnderCrystal));
      } else {
         mc.playerController.attackEntity(mc.player, entityEnderCrystal);
      }

      hitCrystals.add(entityEnderCrystal.getEntityId());
      placedPositions.clear();
   }
}
