package fat.vonware.features.modules.combat;

import fat.vonware.Vonware;
import fat.vonware.features.modules.Module;
import fat.vonware.features.setting.Setting;
import fat.vonware.util.BlockUtil;
import fat.vonware.util.EntityUtil;
import fat.vonware.util.InventoryUtil;
import fat.vonware.util.RotationUtil;
import fat.vonware.util.Timer;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.block.Block;
import net.minecraft.block.BlockPistonBase;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityEnderCrystal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.network.play.client.CPacketAnimation;
import net.minecraft.network.play.client.CPacketUseEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class PistonPush extends Module {
   private EntityPlayer target;
   private final Timer breakTimer = new Timer();
   private final Timer delayTime = new Timer();
   private int rotationPitch = 0;
   private int rotationYaw = 0;
   public final Setting<Boolean> pullBack = new Setting<Boolean>("pullBack", true, "Retracts or restores the piston setup after pushing the target.");
   public final Setting<Integer> delay = new Setting<Integer>("Delay", 5, 0, 10, "Delay between PistonPush action steps.");
   public final Setting<Boolean> autoToggle = new Setting<Boolean>("autoToggle", true, "Automatically disables PistonPush after completing or failing its sequence.");
   public final Setting<Boolean> onlyBurrow = new Setting<Boolean>("onlyBurrow", true, this::new0, "Only targets players that satisfy the module burrow check.");
   public final Setting<Boolean> onlyGround = new Setting<Boolean>("onlyGround", false, "Only targets players that are on the ground.");
   public final Setting<Double> pistonCheck = new Setting<Double>("PistonCheck", (double)2.0F, (double)0.0F, (double)4.0F, "Distance used when checking for existing/usable piston positions around the target.");
   public final Setting<Integer> surroundCheck = new Setting<Integer>("SurroundCheck", 2, 0, 4, "Controls how many surrounding target positions are checked when finding a push setup.");
   public final Setting<Double> range = new Setting<Double>("Range", (double)4.0F, (double)0.0F, (double)6.0F, "Maximum distance at which players can be targeted for piston pushing.");

   public PistonPush() {
      super("PistonPush", "Pushes your enemies out of safe holes", Module.Category.COMBAT, true, false, false);
      this.register(this.autoToggle);
      this.register(this.pullBack);
      this.register(this.onlyBurrow);
      this.register(this.range);
      this.register(this.delay);
      this.register(this.surroundCheck);
      this.register(this.pistonCheck);
      this.register(this.onlyGround);
   }

   Boolean canPush(EntityPlayer entityPlayer) {
      int n = 0;
      if ((Boolean)this.pullBack.getValue() && !(Boolean)this.onlyBurrow.getValue()) {
         return true;
      } else if (!mc.world.isAirBlock(new BlockPos(entityPlayer.posX, entityPlayer.posY + (double)2.5F, entityPlayer.posZ))) {
         return false;
      } else if (!mc.world.isAirBlock(new BlockPos(entityPlayer.posX, entityPlayer.posY + (double)0.5F, entityPlayer.posZ))) {
         return true;
      } else {
         if (!mc.world.isAirBlock(new BlockPos(entityPlayer.posX + (double)1.0F, entityPlayer.posY + (double)0.5F, entityPlayer.posZ))) {
            ++n;
         }

         if (!mc.world.isAirBlock(new BlockPos(entityPlayer.posX - (double)1.0F, entityPlayer.posY + (double)0.5F, entityPlayer.posZ))) {
            ++n;
         }

         if (!mc.world.isAirBlock(new BlockPos(entityPlayer.posX, entityPlayer.posY + (double)0.5F, entityPlayer.posZ + (double)1.0F))) {
            ++n;
         }

         if (!mc.world.isAirBlock(new BlockPos(entityPlayer.posX, entityPlayer.posY + (double)0.5F, entityPlayer.posZ - (double)1.0F))) {
            ++n;
         }

         return n > (Integer)this.surroundCheck.getValue() - 1;
      }
   }

   private static Float attackCrystal2(Entity entity) {
      return mc.player.getDistance(entity);
   }

   public boolean attackCrystal(BlockPos blockPos) {
      for(Entity entity : mc.world.loadedEntityList.stream().filter(PistonPush::attackCrystal1).sorted(Comparator.comparing(PistonPush::attackCrystal2)).collect(Collectors.toList())) {
         if (entity instanceof EntityEnderCrystal && entity.getDistance((double)blockPos.getX(), (double)blockPos.getY(), (double)blockPos.getZ()) < (double)4.0F) {
            if (!this.breakTimer.passedMs(300L)) {
               return true;
            }

            this.breakTimer.reset();
            mc.player.connection.sendPacket(new CPacketUseEntity(entity));
            mc.player.connection.sendPacket(new CPacketAnimation(EnumHand.MAIN_HAND));
            BlockPos blockPos2 = new BlockPos(entity.posX, entity.posY, entity.posZ);
            EnumFacing enumFacing = BlockUtil.getFirstFacing(blockPos2);
            if (enumFacing == null) {
               return true;
            }

            BlockPos blockPos3 = blockPos2.offset(enumFacing);
            EnumFacing enumFacing2 = enumFacing.getOpposite();
            Vec3d vec3d = (new Vec3d(blockPos3)).add((double)0.5F, (double)0.5F, (double)0.5F).add((new Vec3d(enumFacing2.getDirectionVec())).scale((double)0.5F));
            RotationUtil.faceVector(vec3d, true);
            return true;
         }
      }

      return false;
   }

   public static int getPitch(BlockPos blockPos) {
      EnumFacing enumFacing = BlockUtil.getFirstFacing(blockPos);
      if (enumFacing == null) {
         return 0;
      } else {
         BlockPos blockPos2 = blockPos.offset(enumFacing);
         EnumFacing enumFacing2 = enumFacing.getOpposite();
         Vec3d vec3d = (new Vec3d(blockPos2)).add((double)0.5F, (double)0.5F, (double)0.5F).add((new Vec3d(enumFacing2.getDirectionVec())).scale((double)0.5F));
         float[] fArray = RotationUtil.getLegitRotations(vec3d);
         return (int)fArray[0];
      }
   }

   public static int getYaw(BlockPos blockPos) {
      EnumFacing enumFacing = BlockUtil.getFirstFacing(blockPos);
      if (enumFacing == null) {
         return 0;
      } else {
         BlockPos blockPos2 = blockPos.offset(enumFacing);
         EnumFacing enumFacing2 = enumFacing.getOpposite();
         Vec3d vec3d = (new Vec3d(blockPos2)).add((double)0.5F, (double)0.5F, (double)0.5F).add((new Vec3d(enumFacing2.getDirectionVec())).scale((double)0.5F));
         float[] fArray = RotationUtil.getLegitRotations(vec3d);
         return (int)fArray[1];
      }
   }

   public void onUpdate() {
      if (this.delayTime.passedMs((long)((Integer)this.delay.getValue() * 50))) {
         if (InventoryUtil.findHotbarBlock(Blocks.REDSTONE_BLOCK) != -1) {
            if (InventoryUtil.findHotbarBlock(BlockPistonBase.class) != -1) {
               this.target = this.getTarget((Double)this.range.getValue());
               if (this.target != null) {
                  BlockPos blockPos = new BlockPos(this.target.posX, this.target.posY + (double)0.5F, this.target.posZ);
                  if (this.getBlock(blockPos.add(0, 2, 0)) == Blocks.AIR) {
                     for(EnumFacing enumFacing : EnumFacing.VALUES) {
                        if (enumFacing != EnumFacing.DOWN && enumFacing != EnumFacing.UP) {
                           BlockPos blockPos2 = blockPos.offset(enumFacing).offset(EnumFacing.UP);
                           if (this.getBlock(blockPos2) instanceof BlockPistonBase && this.getBlock(blockPos2.offset(enumFacing, -2)) == Blocks.AIR && this.getBlock(blockPos2.offset(enumFacing, -2).offset(EnumFacing.UP)) == Blocks.AIR) {
                              this.delayTime.reset();

                              for(EnumFacing enumFacing2 : EnumFacing.VALUES) {
                                 if (this.getBlock(blockPos2.offset(enumFacing2)) == Blocks.REDSTONE_BLOCK) {
                                    if ((Boolean)this.autoToggle.getValue()) {
                                       this.disable();
                                    }

                                    return;
                                 }
                              }

                              for(EnumFacing enumFacing2 : EnumFacing.VALUES) {
                                 if (!BlockUtil.PistonCheck(blockPos2.offset(enumFacing2))) {
                                    int n3 = mc.player.inventory.currentItem;
                                    mc.player.inventory.currentItem = InventoryUtil.findHotbarBlock(Blocks.REDSTONE_BLOCK);
                                    mc.playerController.updateController();
                                    BlockUtil.placeBlock(blockPos2.offset(enumFacing2), EnumHand.MAIN_HAND, true, true, false);
                                    mc.player.inventory.currentItem = n3;
                                    mc.playerController.updateController();
                                    return;
                                 }
                              }
                           }
                        }
                     }

                     EnumFacing[] var17 = EnumFacing.VALUES;
                     int var20 = var17.length;
                     int var23 = 0;

                     BlockPos blockPos2;
                     EnumFacing enumFacing;
                     while(true) {
                        if (var23 >= var20) {
                           if (this.getBlock(blockPos) == Blocks.AIR && (Boolean)this.onlyBurrow.getValue() || !(Boolean)this.pullBack.getValue()) {
                              return;
                           }

                           for(EnumFacing facing : EnumFacing.VALUES) {
                              if (facing != EnumFacing.DOWN && facing != EnumFacing.UP) {
                                 blockPos2 = blockPos.offset(facing).offset(EnumFacing.UP);
                                 if (this.getBlock(blockPos2) instanceof BlockPistonBase && (this.getBlock(blockPos2.offset(EnumFacing.UP)) == Blocks.AIR || this.getBlock(blockPos2.offset(EnumFacing.UP)) == Blocks.REDSTONE_BLOCK)) {
                                    this.delayTime.reset();

                                    for(EnumFacing enumFacing4 : EnumFacing.VALUES) {
                                       if (this.getBlock(blockPos2.offset(enumFacing4)) == Blocks.REDSTONE_BLOCK) {
                                          this.mine(blockPos2.offset(enumFacing4));
                                          if ((Boolean)this.autoToggle.getValue()) {
                                             this.disable();
                                          }

                                          return;
                                       }
                                    }

                                    for(EnumFacing enumFacing4 : EnumFacing.VALUES) {
                                       if (!BlockUtil.PistonCheck(blockPos2.offset(enumFacing4))) {
                                          int n6 = mc.player.inventory.currentItem;
                                          mc.player.inventory.currentItem = InventoryUtil.findHotbarBlock(Blocks.REDSTONE_BLOCK);
                                          mc.playerController.updateController();
                                          BlockUtil.placeBlock(blockPos2.offset(enumFacing4), EnumHand.MAIN_HAND, true, true, false);
                                          mc.player.inventory.currentItem = n6;
                                          mc.playerController.updateController();
                                          return;
                                       }
                                    }
                                 }
                              }
                           }

                           var17 = EnumFacing.VALUES;
                           var20 = var17.length;
                           var23 = 0;

                           while(true) {
                              if (var23 >= var20) {
                                 return;
                              }

                              enumFacing = var17[var23];
                              if (enumFacing != EnumFacing.DOWN && enumFacing != EnumFacing.UP) {
                                 blockPos2 = blockPos.offset(enumFacing).offset(EnumFacing.UP);
                                 if (!(mc.player.posY - this.target.posY <= (double)-1.0F) && !(mc.player.posY - this.target.posY >= (double)2.0F) || !(BlockUtil.distanceToXZ((double)blockPos2.getX() + (double)0.5F, (double)blockPos2.getZ() + (double)0.5F) < (Double)this.pistonCheck.getValue())) {
                                    boolean bl = false;

                                    for(EnumFacing enumFacing3 : EnumFacing.VALUES) {
                                       if (!BlockUtil.PistonCheck(blockPos2.offset(enumFacing3))) {
                                          bl = true;
                                          break;
                                       }
                                    }

                                    if (bl) {
                                       if (!BlockUtil.PistonCheck(blockPos2) && this.attackCrystal(blockPos2)) {
                                          return;
                                       }

                                       if (!BlockUtil.cantPistonPlace(blockPos2) && (this.getBlock(blockPos2.offset(EnumFacing.UP)) == Blocks.AIR || this.getBlock(blockPos2.offset(EnumFacing.UP)) == Blocks.REDSTONE_BLOCK)) {
                                          if (!BlockUtil.PistonCheck(blockPos2)) {
                                             break;
                                          }

                                          if (!BlockUtil.PistonCheck(blockPos2.add(0, -1, 0))) {
                                             int n7 = mc.player.inventory.currentItem;
                                             mc.player.inventory.currentItem = InventoryUtil.findHotbarBlock(Blocks.REDSTONE_BLOCK);
                                             mc.playerController.updateController();
                                             BlockUtil.placeBlock(blockPos2.add(0, -1, 0), EnumHand.MAIN_HAND, true, true, false);
                                             mc.player.inventory.currentItem = n7;
                                             mc.playerController.updateController();
                                             break;
                                          }
                                       }
                                    }
                                 }
                              }

                              ++var23;
                           }

                           this.delayTime.reset();
                           if (this.getBlock(blockPos2) == Blocks.AIR) {
                              this.rotationPitch = getPitch(blockPos2);
                              this.rotationYaw = getYaw(blockPos2);
                              if (enumFacing == EnumFacing.EAST) {
                                 RotationUtil.faceYawAndPitch(-90.0F, 0.0F);
                              } else if (enumFacing == EnumFacing.WEST) {
                                 RotationUtil.faceYawAndPitch(90.0F, 0.0F);
                              } else if (enumFacing == EnumFacing.NORTH) {
                                 RotationUtil.faceYawAndPitch(180.0F, 0.0F);
                              } else if (enumFacing == EnumFacing.SOUTH) {
                                 RotationUtil.faceYawAndPitch(0.0F, 0.0F);
                              }

                              int n8 = mc.player.inventory.currentItem;
                              mc.player.inventory.currentItem = InventoryUtil.findHotbarBlock(BlockPistonBase.class);
                              mc.playerController.updateController();
                              BlockUtil.placeBlock(blockPos2, EnumHand.MAIN_HAND, false, true, false);
                              mc.player.inventory.currentItem = n8;
                              mc.playerController.updateController();
                           }

                           return;
                        }

                        enumFacing = var17[var23];
                        if (enumFacing != EnumFacing.DOWN && enumFacing != EnumFacing.UP) {
                           blockPos2 = blockPos.offset(enumFacing).offset(EnumFacing.UP);
                           if (!(mc.player.posY - this.target.posY <= (double)-1.0F) && !(mc.player.posY - this.target.posY >= (double)2.0F) || !(BlockUtil.distanceToXZ((double)blockPos2.getX() + (double)0.5F, (double)blockPos2.getZ() + (double)0.5F) < (Double)this.pistonCheck.getValue())) {
                              boolean bl = false;

                              for(EnumFacing enumFacing3 : EnumFacing.VALUES) {
                                 if (!BlockUtil.PistonCheck(blockPos2.offset(enumFacing3))) {
                                    bl = true;
                                    break;
                                 }
                              }

                              if (bl) {
                                 if (!BlockUtil.PistonCheck(blockPos2) && this.attackCrystal(blockPos2)) {
                                    return;
                                 }

                                 if (!BlockUtil.cantPistonPlace(blockPos2) && this.getBlock(blockPos2.offset(enumFacing, -2)) == Blocks.AIR && this.getBlock(blockPos2.offset(enumFacing, -2).offset(EnumFacing.UP)) == Blocks.AIR) {
                                    if (!BlockUtil.PistonCheck(blockPos2)) {
                                       break;
                                    }

                                    if (!BlockUtil.PistonCheck(blockPos2.add(0, -1, 0))) {
                                       int n4 = mc.player.inventory.currentItem;
                                       mc.player.inventory.currentItem = InventoryUtil.findHotbarBlock(Blocks.REDSTONE_BLOCK);
                                       mc.playerController.updateController();
                                       BlockUtil.placeBlock(blockPos2.add(0, -1, 0), EnumHand.MAIN_HAND, true, true, false);
                                       mc.player.inventory.currentItem = n4;
                                       mc.playerController.updateController();
                                       break;
                                    }
                                 }
                              }
                           }
                        }

                        ++var23;
                     }

                     this.delayTime.reset();
                     if (this.getBlock(blockPos2) == Blocks.AIR) {
                        this.rotationPitch = getPitch(blockPos2);
                        this.rotationYaw = getYaw(blockPos2);
                        if (enumFacing == EnumFacing.EAST) {
                           RotationUtil.faceYawAndPitch(-90.0F, 0.0F);
                        } else if (enumFacing == EnumFacing.WEST) {
                           RotationUtil.faceYawAndPitch(90.0F, 0.0F);
                        } else if (enumFacing == EnumFacing.NORTH) {
                           RotationUtil.faceYawAndPitch(180.0F, 0.0F);
                        } else if (enumFacing == EnumFacing.SOUTH) {
                           RotationUtil.faceYawAndPitch(0.0F, 0.0F);
                        }

                        int n5 = mc.player.inventory.currentItem;
                        mc.player.inventory.currentItem = InventoryUtil.findHotbarBlock(BlockPistonBase.class);
                        mc.playerController.updateController();
                        BlockUtil.placeBlock(blockPos2, EnumHand.MAIN_HAND, false, true, false);
                        mc.player.inventory.currentItem = n5;
                        mc.playerController.updateController();
                        RotationUtil.facePos(blockPos2);
                     }

                  }
               }
            }
         }
      }
   }

   private static boolean attackCrystal1(Entity entity) {
      return entity instanceof EntityEnderCrystal && !entity.isDead;
   }

   EntityPlayer getTarget(double d) {
      EntityPlayer entityPlayer = null;
      double d2 = d;

      for(EntityPlayer entityPlayer2 : mc.world.playerEntities) {
         if (this.canPush(entityPlayer2) && !EntityUtil.isntValid(entityPlayer2, d) && (entityPlayer2.onGround || !(Boolean)this.onlyGround.getValue()) && !Vonware.friendManager.isFriend(entityPlayer2.getName()) && !(mc.player.posY - entityPlayer2.posY >= (double)5.0F)) {
            if (entityPlayer == null) {
               entityPlayer = entityPlayer2;
               d2 = mc.player.getDistanceSq(entityPlayer2);
            } else if (!(mc.player.getDistanceSq(entityPlayer2) >= d2)) {
               entityPlayer = entityPlayer2;
               d2 = mc.player.getDistanceSq(entityPlayer2);
            }
         }
      }

      return entityPlayer;
   }

   Block getBlock(BlockPos blockPos) {
      return mc.world.getBlockState(blockPos).getBlock();
   }

   public void mine(BlockPos blockPos) {
      if (mc.world.getBlockState(blockPos).getBlock() != Blocks.AIR && mc.world.getBlockState(blockPos).getBlock() != Blocks.WEB && mc.world.getBlockState(blockPos).getBlock() != Blocks.REDSTONE_WIRE && mc.world.getBlockState(blockPos).getBlock() != Blocks.WATER && mc.world.getBlockState(blockPos).getBlock() != Blocks.LAVA) {
         mc.playerController.onPlayerDamageBlock(blockPos, BlockUtil.getRayTraceFacing(blockPos));
      }

   }

   private boolean new0(Boolean bl) {
      return (Boolean)this.pullBack.getValue();
   }

   public String getDisplayInfo() {
      return this.target != null ? this.target.getName() : null;
   }
}
