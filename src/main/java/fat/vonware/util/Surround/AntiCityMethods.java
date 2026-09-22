package fat.vonware.util.Surround;

import fat.vonware.util.PositionUtil;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.init.Blocks;
import net.minecraft.network.play.client.CPacketPlayerDigging;
import net.minecraft.network.play.client.CPacketPlayerDigging.Action;
import net.minecraft.network.play.server.SPacketBlockBreakAnim;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;

public class AntiCityMethods {
   private static final Minecraft mc = Minecraft.getMinecraft();

   public static void processBlockBreakAnim(SPacketBlockBreakAnim packet, AntiCityData data) {
      if (mc.player != null) {
         data.breakPos = packet.getPosition();
         BlockPos playerPos = PositionUtil.getPosition(mc.player);
         data.placeID = calculatePlaceID(data.breakPos, playerPos);
      }
   }

   public static void processPlayerDigging(CPacketPlayerDigging packet, AntiCityData data) {
      if (mc.player != null) {
         if (packet.getAction() == Action.START_DESTROY_BLOCK) {
            data.breakPos = packet.getPosition();
            BlockPos playerPos = PositionUtil.getPosition(mc.player);
            data.placeID = calculatePlaceID(data.breakPos, playerPos);
         }

      }
   }

   private static int calculatePlaceID(BlockPos breakPos, BlockPos playerPos) {
      if (breakPos.equals(playerPos.add(1, 0, 0))) {
         return 1;
      } else if (breakPos.equals(playerPos.add(-1, 0, 0))) {
         return 2;
      } else if (breakPos.equals(playerPos.add(0, 0, 1))) {
         return 3;
      } else if (breakPos.equals(playerPos.add(0, 0, -1))) {
         return 4;
      } else if (breakPos.equals(playerPos.add(2, 0, 0))) {
         return 5;
      } else if (breakPos.equals(playerPos.add(-2, 0, 0))) {
         return 6;
      } else if (breakPos.equals(playerPos.add(0, 0, 2))) {
         return 7;
      } else if (breakPos.equals(playerPos.add(0, 0, -2))) {
         return 8;
      } else if (breakPos.equals(playerPos.add(1, 1, 0))) {
         return 9;
      } else if (breakPos.equals(playerPos.add(-1, 1, 0))) {
         return 10;
      } else if (breakPos.equals(playerPos.add(0, 1, 1))) {
         return 11;
      } else if (breakPos.equals(playerPos.add(0, 1, -1))) {
         return 12;
      } else if (breakPos.equals(playerPos.add(1, 0, 1))) {
         return 13;
      } else if (breakPos.equals(playerPos.add(1, 0, -1))) {
         return 14;
      } else if (breakPos.equals(playerPos.add(-1, 0, 1))) {
         return 15;
      } else {
         return breakPos.equals(playerPos.add(-1, 0, -1)) ? 16 : 0;
      }
   }

   public static List<BlockPos> getAntiCityPositions(AntiCityData data, BlockPos playerPos, boolean enabled) {
      List<BlockPos> positions = new ArrayList();
      if (enabled && data.breakPos != null) {
         BlockPos pos = new BlockPos((double)playerPos.getX(), (double)playerPos.getY() + 0.2, (double)playerPos.getZ());
         handleVerticalBlocking(data.breakPos, pos, positions);
         switch (data.placeID) {
            case 1:
               if (isNotBedrock(pos.add(1, 0, 0))) {
                  positions.add(pos.add(2, 0, 0));
                  positions.add(pos.add(1, 0, 1));
                  positions.add(pos.add(1, 0, -1));
                  positions.add(pos.add(1, 1, 0));
                  if (hasEntity(pos.add(2, 0, 0))) {
                     positions.add(pos.add(3, 0, 0));
                     positions.add(pos.add(3, 1, 0));
                  }
               }
               break;
            case 2:
               if (isNotBedrock(pos.add(-1, 0, 0))) {
                  positions.add(pos.add(-2, 0, 0));
                  positions.add(pos.add(-1, 0, 1));
                  positions.add(pos.add(-1, 0, -1));
                  positions.add(pos.add(-1, 1, 0));
                  if (hasEntity(pos.add(-2, 0, 0))) {
                     positions.add(pos.add(-3, 0, 0));
                     positions.add(pos.add(-3, 1, 0));
                  }
               }
               break;
            case 3:
               if (isNotBedrock(pos.add(0, 0, 1))) {
                  positions.add(pos.add(0, 0, 2));
                  positions.add(pos.add(1, 0, 1));
                  positions.add(pos.add(-1, 0, 1));
                  positions.add(pos.add(0, 1, 1));
                  if (hasEntity(pos.add(0, 0, 2))) {
                     positions.add(pos.add(0, 0, 3));
                     positions.add(pos.add(0, 1, 3));
                  }
               }
               break;
            case 4:
               if (isNotBedrock(pos.add(0, 0, -1))) {
                  positions.add(pos.add(0, 0, -2));
                  positions.add(pos.add(1, 0, -1));
                  positions.add(pos.add(-1, 0, -1));
                  positions.add(pos.add(0, 1, -1));
                  if (hasEntity(pos.add(0, 0, -2))) {
                     positions.add(pos.add(0, 0, -3));
                     positions.add(pos.add(0, 1, -3));
                  }
               }
               break;
            case 5:
               if (isNotBedrock(pos.add(1, 0, 0))) {
                  positions.add(pos.add(1, 0, 0));
                  positions.add(pos.add(2, 1, 0));
                  positions.add(pos.add(3, 0, 0));
               }
               break;
            case 6:
               if (isNotBedrock(pos.add(-1, 0, 0))) {
                  positions.add(pos.add(-1, 0, 0));
                  positions.add(pos.add(-2, 1, 0));
                  positions.add(pos.add(-3, 0, 0));
               }
               break;
            case 7:
               if (isNotBedrock(pos.add(0, 0, 1))) {
                  positions.add(pos.add(0, 0, 1));
                  positions.add(pos.add(0, 1, 2));
                  positions.add(pos.add(0, 0, 3));
               }
               break;
            case 8:
               if (isNotBedrock(pos.add(0, 0, -1))) {
                  positions.add(pos.add(0, 0, -1));
                  positions.add(pos.add(0, 1, -2));
                  positions.add(pos.add(0, 0, -3));
               }
               break;
            case 9:
               if (isNotBedrock(pos.add(1, 0, 0))) {
                  positions.add(pos.add(1, 0, 0));
                  positions.add(pos.add(2, 1, 0));
               }
               break;
            case 10:
               if (isNotBedrock(pos.add(-1, 0, 0))) {
                  positions.add(pos.add(-1, 0, 0));
                  positions.add(pos.add(-2, 1, 0));
               }
               break;
            case 11:
               if (isNotBedrock(pos.add(0, 0, 1))) {
                  positions.add(pos.add(0, 0, 1));
                  positions.add(pos.add(0, 1, 2));
               }
               break;
            case 12:
               if (isNotBedrock(pos.add(0, 0, -1))) {
                  positions.add(pos.add(0, 0, -1));
                  positions.add(pos.add(0, 1, -2));
               }
               break;
            case 13:
            case 14:
               if (isNotBedrock(pos.add(1, 0, 0))) {
                  positions.add(pos.add(1, 0, 0));
               }
               break;
            case 15:
            case 16:
               if (isNotBedrock(pos.add(-1, 0, 0))) {
                  positions.add(pos.add(-1, 0, 0));
               }
         }

         if ((data.placeID == 13 || data.placeID == 15) && isNotBedrock(pos.add(0, 0, 1))) {
            positions.add(pos.add(0, 0, 1));
         }

         if ((data.placeID == 14 || data.placeID == 16) && isNotBedrock(pos.add(0, 0, -1))) {
            positions.add(pos.add(0, 0, -1));
         }

         return positions;
      } else {
         return positions;
      }
   }

   private static void handleVerticalBlocking(BlockPos breakPos, BlockPos pos, List<BlockPos> positions) {
      if ((breakPos.equals(pos.add(1, 0, 0)) || breakPos.equals(pos.add(1, 1, 0))) && isAir(pos.add(1, 0, 0)) && isAir(pos.add(1, 1, 0))) {
         if (breakPos.equals(pos.add(1, 0, 0))) {
            positions.add(pos.add(1, 1, 0));
         } else {
            positions.add(pos.add(1, 0, 0));
         }
      }

      if ((breakPos.equals(pos.add(-1, 0, 0)) || breakPos.equals(pos.add(-1, 1, 0))) && isAir(pos.add(-1, 0, 0)) && isAir(pos.add(-1, 1, 0))) {
         if (breakPos.equals(pos.add(-1, 0, 0))) {
            positions.add(pos.add(-1, 1, 0));
         } else {
            positions.add(pos.add(-1, 0, 0));
         }
      }

      if ((breakPos.equals(pos.add(0, 0, 1)) || breakPos.equals(pos.add(0, 1, 1))) && isAir(pos.add(0, 0, 1)) && isAir(pos.add(0, 1, 1))) {
         if (breakPos.equals(pos.add(0, 0, 1))) {
            positions.add(pos.add(0, 1, 1));
         } else {
            positions.add(pos.add(0, 0, 1));
         }
      }

      if ((breakPos.equals(pos.add(0, 0, -1)) || breakPos.equals(pos.add(0, 1, -1))) && isAir(pos.add(0, 0, -1)) && isAir(pos.add(0, 1, -1))) {
         if (breakPos.equals(pos.add(0, 0, -1))) {
            positions.add(pos.add(0, 1, -1));
         } else {
            positions.add(pos.add(0, 0, -1));
         }
      }

   }

   private static boolean hasEntity(BlockPos pos) {
      for(Entity entity : mc.world.getEntitiesWithinAABB(Entity.class, new AxisAlignedBB(pos))) {
         if (!(entity instanceof EntityItem) && !(entity instanceof EntityXPOrb) && entity != null) {
            return true;
         }
      }

      return false;
   }

   private static boolean isNotBedrock(BlockPos pos) {
      if (mc.world == null) {
         return false;
      } else {
         return mc.world.getBlockState(pos).getBlock() != Blocks.BEDROCK;
      }
   }

   private static boolean isAir(BlockPos pos) {
      if (mc.world == null) {
         return false;
      } else {
         return mc.world.getBlockState(pos).getBlock() == Blocks.AIR;
      }
   }

   public static class AntiCityData {
      public BlockPos breakPos = null;
      public int placeID = 0;
      public int blocksPlaced = 0;

      public void reset() {
         this.breakPos = null;
         this.placeID = 0;
         this.blocksPlaced = 0;
      }
   }
}
