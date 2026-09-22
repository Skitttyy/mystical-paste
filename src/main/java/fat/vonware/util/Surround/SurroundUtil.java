package fat.vonware.util.Surround;

import fat.vonware.features.command.Command;
import fat.vonware.features.modules.client.Debug;
import fat.vonware.util.BlockUtil;
import fat.vonware.util.InventoryUtil;
import fat.vonware.util.Util;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAnvil;
import net.minecraft.block.BlockBeacon;
import net.minecraft.block.BlockBrewingStand;
import net.minecraft.block.BlockButton;
import net.minecraft.block.BlockChest;
import net.minecraft.block.BlockDispenser;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockDropper;
import net.minecraft.block.BlockEnchantmentTable;
import net.minecraft.block.BlockEnderChest;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.BlockFurnace;
import net.minecraft.block.BlockHopper;
import net.minecraft.block.BlockNote;
import net.minecraft.block.BlockShulkerBox;
import net.minecraft.block.BlockTrapDoor;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityEnderCrystal;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;

public class SurroundUtil implements Util {
   public static boolean hasEntityAt(BlockPos pos) {
      AxisAlignedBB box = (new AxisAlignedBB(pos)).shrink(0.045);

      for(Entity entity : mc.world.loadedEntityList) {
         if (entity != mc.player && !entity.isDead && !(entity instanceof EntityEnderCrystal) && entity instanceof EntityPlayer && entity.getEntityBoundingBox().intersects(box)) {
            return true;
         }
      }

      return false;
   }

   public static boolean hasEntityBlockingPlacement(BlockPos pos) {
      AxisAlignedBB box = new AxisAlignedBB(pos);

      for(Entity entity : mc.world.loadedEntityList) {
         if (entity != mc.player && !entity.isDead && !(entity instanceof EntityEnderCrystal) && !(entity instanceof EntityItem) && !(entity instanceof EntityXPOrb) && !(entity instanceof EntityArrow) && entity.getEntityBoundingBox().intersects(box)) {
            if ((Boolean)Debug.getInstance().surroundDebug.getValue()) {
               Command.sendSilentMessage(TextFormatting.RED + "Entity " + entity.getName() + " blocking placement at " + pos);
            }

            return true;
         }
      }

      return false;
   }

   public static boolean isUnbreakableBlock(BlockPos pos) {
      if (BlockUtil.canReplace(pos)) {
         return false;
      } else {
         try {
            return mc.world.getBlockState(pos).getBlock() == Blocks.BEDROCK || mc.world.getBlockState(pos).getBlock() == Blocks.BARRIER || mc.world.getBlockState(pos).getBlock() == Blocks.COMMAND_BLOCK || mc.world.getBlockState(pos).getBlockHardness(mc.world, pos) < 0.0F;
         } catch (Exception var2) {
            return false;
         }
      }
   }

   public static int findSurroundBlock() {
      int slot = InventoryUtil.findObsidianSlot();
      if (slot != -1) {
         return slot;
      } else {
         for(int i = 0; i < 9; ++i) {
            if (mc.player.inventory.getStackInSlot(i).getItem() == Item.getItemFromBlock(Blocks.ENDER_CHEST)) {
               if ((Boolean)Debug.getInstance().surroundDebug.getValue()) {
                  Command.sendSilentMessage(TextFormatting.LIGHT_PURPLE + "Falling back to Ender Chest from slot " + i);
               }

               return i;
            }
         }

         return -1;
      }
   }

   public static boolean needsSneak(BlockPos pos) {
      Block block = mc.world.getBlockState(pos).getBlock();
      return block instanceof BlockChest || block instanceof BlockEnderChest || block instanceof BlockFurnace || block instanceof BlockDispenser || block instanceof BlockDropper || block instanceof BlockHopper || block instanceof BlockEnchantmentTable || block instanceof BlockAnvil || block instanceof BlockBrewingStand || block instanceof BlockBeacon || block instanceof BlockShulkerBox || block instanceof BlockNote || block instanceof BlockButton || block instanceof BlockDoor || block instanceof BlockTrapDoor || block instanceof BlockFenceGate || block == Blocks.DRAGON_EGG;
   }

   public static boolean isInsideWorldBorder(BlockPos pos) {
      return mc.world.getWorldBorder() == null ? true : mc.world.getWorldBorder().contains(pos);
   }
}
