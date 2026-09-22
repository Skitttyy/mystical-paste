package fat.vonware.features.modules.combat;

import fat.vonware.Global;
import fat.vonware.Vonware;
import fat.vonware.ducks.IMinecraft;
import fat.vonware.event.events.PacketEvent;
import fat.vonware.event.events.Render3DEvent;
import fat.vonware.features.modules.Module;
import fat.vonware.features.modules.client.BlockRender3D;
import fat.vonware.features.modules.client.Colors;
import fat.vonware.features.setting.Setting;
import fat.vonware.mixin.mixins.accessor.ITimer;
import fat.vonware.util.BlockUtil;
import fat.vonware.util.InventoryUtil;
import fat.vonware.util.MovementUtil;
import fat.vonware.util.RotationUtil;
import fat.vonware.util.Timer;
import fat.vonware.util.Animations.render.ColorUtil;
import fat.vonware.util.Animations.render.RenderUtil;
import fat.vonware.util.Phase.CrystalUtil;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.block.BlockLiquid;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityEnderCrystal;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.network.play.server.SPacketSoundEffect;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class SelfFill extends Module {
   Timer hitcrystalCooldown;
   Timer burrowCooldown;
   public Setting<Boolean> render = new Setting<Boolean>("Render", true, "Renders SelfFill block placements using the current BlockRender style.");
   public Setting<RotateMode> rotate;
   public Setting<Boolean> strictDirection;
   public Setting<Boolean> breakCrystal;
   public Setting<Integer> breakDelay;
   public Setting<Float> ticksExisted;
   public Setting<Boolean> await;
   public Setting<Integer> burrowDelay;
   public Setting<Boolean> breakSync;
   public Setting<BlockMode> block;
   public Setting<Boolean> dynamic;
   public Setting<Boolean> movementDisable;
   List<BlockPos> originalPos;
   List<BlockPos> placedPositions;
   private final List<RenderBlock> renderBlocks;
   double startY;
   ITimer timer;
   int tick;
   double[] heights;
   boolean didBreak;

   public SelfFill() {
      super("SelfFill", "Burrows you into blocks", Module.Category.COMBAT, true, false, false);
      this.rotate = (new Setting("Rotate", SelfFill.RotateMode.Packet)).withDescription((Function<RotateMode, String>)((mode) -> {
         switch (mode) {
            case None:
               return "None: Breaks crystals and places the burrow block without applying SelfFill rotations.";
            case Normal:
               return "Normal: Directly changes your client yaw and pitch when SelfFill needs to rotate.";
            case Packet:
               return "Packet: Sends rotation packets for crystal breaking and burrow placement without directly turning your client view.";
            default:
               return "";
         }
      }));
      this.strictDirection = new Setting<Boolean>("Strict Direction", true, "Uses stricter placement-side checks when choosing the face used to place the SelfFill block.");
      this.breakCrystal = new Setting<Boolean>("Break", true, "Attacks end crystals that intersect the SelfFill placement position.");
      this.breakDelay = new Setting<Integer>("Break Delay", 0, 0, 1000, (v) -> (Boolean)this.breakCrystal.getValue(), "Delay in milliseconds between attempts to break crystals blocking the burrow position.");
      this.ticksExisted = new Setting<Float>("Ticks Existed", 0.0F, 0.0F, 10.0F, (v) -> (Boolean)this.breakCrystal.getValue(), "Minimum age in ticks a blocking crystal must reach before SelfFill attacks it.");
      this.await = new Setting<Boolean>("Await", true, "Keeps SelfFill enabled and waits for invalid placement conditions or crystal interference to clear instead of immediately disabling.");
      this.burrowDelay = new Setting<Integer>("Burrow Delay", 100, 0, 1000, (v) -> (Boolean)this.await.getValue(), "Delay between completed SelfFill burrow placement attempts while Await is enabled.");
      this.breakSync = new Setting<Boolean>("Sync", true, (v) -> (Boolean)this.await.getValue(), "Requires a blocking crystal to be broken before SelfFill continues with the synchronized burrow placement.");
      this.block = (new Setting("Block", SelfFill.BlockMode.Dynamic)).withDescription((Function<BlockMode, String>)((mode) -> {
         switch (mode) {
            case Piston:
               return "Piston: Uses a piston as the SelfFill burrow block.";
            case EnderChest:
               return "EnderChest: Uses an ender chest as the SelfFill burrow block.";
            case Obsidian:
               return "Obsidian: Uses obsidian as the SelfFill burrow block.";
            case Dynamic:
               return "Dynamic: Uses obsidian when available and falls back to an ender chest when no obsidian is in your hotbar.";
            default:
               return "";
         }
      }));
      this.dynamic = new Setting<Boolean>("Dynamic", false, "Uses every block position your player bounding box overlaps instead of only your primary block position.");
      this.movementDisable = new Setting<Boolean>("Movement Disable", false, "Disables SelfFill when you begin moving while a crystal is still blocking the burrow position.");
      this.renderBlocks = new ArrayList();
      this.hitcrystalCooldown = new Timer();
      this.burrowCooldown = new Timer();
      this.register(this.render);
      this.register(this.rotate);
      this.register(this.strictDirection);
      this.register(this.breakCrystal);
      this.register(this.breakDelay);
      this.register(this.ticksExisted);
      this.register(this.await);
      this.register(this.burrowDelay);
      this.register(this.breakSync);
      this.register(this.block);
      this.register(this.dynamic);
      this.register(this.movementDisable);
      this.placedPositions = new ArrayList();
      this.startY = Double.NaN;
      this.tick = 0;
      this.heights = new double[]{0.41999998688698, 0.7531999805212, 1.00133597911214, 1.16610926093821};
      this.didBreak = false;
   }

   private void addRenderBlock(BlockPos pos) {
      if ((Boolean)this.render.getValue()) {
         for(RenderBlock rb : this.renderBlocks) {
            if (rb.pos.equals(pos)) {
               return;
            }
         }

         if (this.renderBlocks.size() >= 30) {
            this.renderBlocks.remove(0);
         }

         this.renderBlocks.add(new RenderBlock(pos));
      }
   }

   @SubscribeEvent
   public void onRender3D(Render3DEvent event) {
      if (!this.renderBlocks.isEmpty()) {
         if (!(Boolean)this.render.getValue()) {
            this.renderBlocks.clear();
         } else {
            BlockRender3D blockRender = (BlockRender3D)Vonware.moduleManager.getModuleByName("BlockRender");
            if (blockRender != null && blockRender.isEnabled()) {
               BlockRender3D.RenderType type = blockRender.getRenderType();
               if (type == BlockRender3D.RenderType.Shrink) {
                  this.renderWithShrink(blockRender.getShrinkTime());
                  this.renderBlocks.removeIf((rb) -> rb.getProgress(blockRender.getShrinkTime()) >= 1.0F);
                  this.cleanupOldRenderBlocks(blockRender.getShrinkTime());
               } else if (type == BlockRender3D.RenderType.Solid) {
                  this.renderWithFade(blockRender.getFadeTime(), blockRender.getSolidAlpha());
                  this.renderBlocks.removeIf((rb) -> rb.getProgress(blockRender.getFadeTime()) >= 1.0F);
                  this.cleanupOldRenderBlocks(blockRender.getFadeTime());
               }
            } else {
               this.renderWithShrink(125);
               this.renderBlocks.removeIf((rb) -> rb.getProgress(125) >= 1.0F);
               this.cleanupOldRenderBlocks(125);
            }

         }
      }
   }

   private void renderWithShrink(int shrinkTimeMs) {
      for(RenderBlock rb : this.renderBlocks) {
         float progress = rb.getProgress(shrinkTimeMs);
         if (!(progress >= 1.0F)) {
            Color color;
            if ((Boolean)Colors.getInstance().rainbow.getValue()) {
               if (Colors.getInstance().rainbowMode.getValue() == Colors.RainbowMode.Static) {
                  color = ColorUtil.rainbow(0);
               } else {
                  color = ColorUtil.rainbow(0);
               }
            } else {
               color = Global.getGlobalColor();
            }

            AxisAlignedBB bb = (new AxisAlignedBB(rb.pos)).shrink((double)0.5F * (double)progress);
            RenderUtil.drawBBFill(bb, color, 80);
            RenderUtil.drawBBBox(bb, color, 255);
         }
      }

   }

   private void renderWithFade(int fadeTimeMs, int startAlpha) {
      for(RenderBlock rb : this.renderBlocks) {
         float progress = rb.getProgress(fadeTimeMs);
         if (!(progress >= 1.0F)) {
            Color color;
            if ((Boolean)Colors.getInstance().rainbow.getValue()) {
               if (Colors.getInstance().rainbowMode.getValue() == Colors.RainbowMode.Static) {
                  color = ColorUtil.rainbow(0);
               } else {
                  color = ColorUtil.rainbow(0);
               }
            } else {
               color = Global.getGlobalColor();
            }

            int currentAlpha = (int)((float)startAlpha * (1.0F - progress));
            int outlineAlpha = Math.min(255, currentAlpha + 55);
            AxisAlignedBB bb = new AxisAlignedBB(rb.pos);
            RenderUtil.drawBBFill(bb, color, currentAlpha);
            RenderUtil.drawBBBox(bb, color, outlineAlpha);
         }
      }

   }

   private void cleanupOldRenderBlocks(int maxAgeMs) {
      long currentTime = System.currentTimeMillis();
      this.renderBlocks.removeIf((rb) -> currentTime - rb.startTime > (long)maxAgeMs);

      while(this.renderBlocks.size() > 30) {
         this.renderBlocks.remove(0);
      }

   }

   public void onEnable() {
      if (!fullNullCheck()) {
         this.placedPositions.clear();
         this.timer = (ITimer)((IMinecraft)mc).getTimer();
         this.startY = Double.NaN;
         this.originalPos = new ArrayList();
         if ((Boolean)this.dynamic.getValue()) {
            this.originalPos = this.getOverlapPos();
         } else {
            this.originalPos.add(this.getPlayerPos());
         }

         this.tick = 0;
      }
   }

   public void onUpdate() {
      if (!fullNullCheck()) {
         if ((Boolean)this.movementDisable.getValue() && MovementUtil.wasdKeys()) {
            BlockPos burrowPos = this.getPlayerPos();
            if (BlockUtil.isInterceptedByCrystal(burrowPos) && !this.didBreak) {
               this.disable();
               return;
            }
         }

         List<BlockPos> positions = new ArrayList();
         if (mc.player.onGround) {
            for(BlockPos pos : this.getOverlapPos()) {
               if (mc.world.getBlockState(pos.up()).getMaterial().blocksMovement() || !mc.world.getBlockState(pos.up(2)).getMaterial().isReplaceable()) {
                  if (!(Boolean)this.await.getValue()) {
                     this.disable();
                  }

                  return;
               }
            }

            if (Double.isNaN(this.startY)) {
               this.startY = mc.player.posY;
            }

            if (!Double.isNaN(this.startY) && mc.player.posY > this.startY) {
               this.disable();
               this.startY = Double.NaN;
            } else if (InventoryUtil.getHotbarItemSlot(Item.getItemFromBlock(this.getSelectedBlock())) == -1) {
               this.disable();
            } else {
               for(BlockPos position : this.placedPositions) {
                  if (!BlockUtil.isReplacable(position)) {
                     this.disable();
                     return;
                  }
               }

               if (!(Boolean)this.dynamic.getValue()) {
                  BlockPos pos = this.getPlayerPos();
                  if (this.canPlaceBurrow(pos, false, false)) {
                     boolean intersectsWithCrystal = BlockUtil.isInterceptedByCrystal(pos);
                     if ((Boolean)this.breakCrystal.getValue() && this.hitcrystalCooldown.isPassed()) {
                        EntityEnderCrystal crystal = null;
                        double bestDistance = (double)9999.0F;

                        for(Entity entity : mc.world.loadedEntityList) {
                           if (entity != null && mc.player.getDistance(entity) <= 3.0F && entity instanceof EntityEnderCrystal && !entity.isDead && (((Float)this.ticksExisted.getValue()).intValue() == 0 || entity.ticksExisted > ((Float)this.ticksExisted.getValue()).intValue())) {
                              double distance = (double)mc.player.getDistance(entity);
                              if (!(bestDistance <= distance)) {
                                 crystal = (EntityEnderCrystal)entity;
                                 bestDistance = distance;
                              }
                           }
                        }

                        this.didBreak = crystal != null;
                        if (this.didBreak) {
                           this.hitcrystalCooldown.resetDelay();
                           float[] rots = RotationUtil.calculateLookAt(crystal.posX, crystal.posY - 1.1, crystal.posZ);
                           if (this.rotate.getValue() == SelfFill.RotateMode.Packet) {
                              CrystalUtil.breakCrystal(crystal, true, rots, true);
                           } else if (this.rotate.getValue() == SelfFill.RotateMode.Normal) {
                              this.doRotate(rots, false);
                              CrystalUtil.breakCrystal(crystal, true, (float[])null, false);
                           } else {
                              CrystalUtil.breakCrystal(crystal, true, (float[])null, false);
                           }

                           mc.player.swingArm(EnumHand.MAIN_HAND);
                        }
                     }

                     if ((Boolean)this.breakSync.getValue() && (Boolean)this.await.getValue()) {
                        if ((this.didBreak || !intersectsWithCrystal) && this.rotate.getValue() == SelfFill.RotateMode.Normal && !this.didBreak) {
                           this.doRotate(pos, false);
                        }
                     } else if (this.rotate.getValue() == SelfFill.RotateMode.Normal && !this.didBreak) {
                        this.doRotate(pos, false);
                     }
                  }
               }

               BlockPos burrowPos;
               if ((Boolean)this.dynamic.getValue()) {
                  positions = this.getOverlapPos();
                  burrowPos = (BlockPos)positions.get(0);
               } else {
                  burrowPos = this.getPlayerPos();
                  positions.add(burrowPos);
               }

               if (this.canPlaceBurrow(burrowPos, true, (Boolean)this.await.getValue() && (Boolean)this.breakSync.getValue())) {
                  if (!BlockUtil.isInterceptedByCrystal(burrowPos)) {
                     this.didBreak = false;
                  }

                  int old = mc.player.inventory.currentItem;
                  if (Double.isNaN(this.startY) && mc.player.onGround) {
                     this.startY = mc.player.posY;
                  }

                  InventoryUtil.switchtoSlotWithUpdate(InventoryUtil.getHotbarItemSlot(Item.getItemFromBlock(this.getSelectedBlock())));
                  this.resetPos();
                  mc.player.setPosition(mc.player.posX, mc.player.posY + 1.16610926093821, mc.player.posZ);

                  for(BlockPos pos2 : positions) {
                     if (this.rotate.getValue() == SelfFill.RotateMode.Packet || this.rotate.getValue() == SelfFill.RotateMode.Normal && this.didBreak) {
                        this.doRotate(pos2, true);
                     }

                     BlockUtil.packetPlaceBlock(pos2, true, BlockUtil.getPlaceableSide(pos2, (Boolean)this.strictDirection.getValue()));
                     if (!this.placedPositions.contains(pos2)) {
                        this.placedPositions.add(pos2);
                     }

                     this.addRenderBlock(pos2);
                  }

                  mc.player.setPosition(mc.player.posX, mc.player.posY - 1.16610926093821, mc.player.posZ);
                  mc.getConnection().sendPacket(new CPacketPlayer.Position(mc.player.posX, mc.player.posY + (double)0.9375F, mc.player.posZ, false));
                  this.didBreak = false;
                  InventoryUtil.switchtoSlotWithUpdate(old);
                  this.burrowCooldown.resetDelay();
                  if (!(Boolean)this.await.getValue()) {
                     this.disable();
                  }

               }
            }
         }
      }
   }

   public boolean canPlaceBurrow(BlockPos burrowPos, Boolean disable, boolean doSync) {
      if (mc.world.getBlockState(burrowPos.down()).getBlock() instanceof BlockAir) {
         if (!(Boolean)this.await.getValue() && disable) {
            this.disable();
         }

         return false;
      } else if (BlockUtil.isInterceptedByOtherPlayer(burrowPos)) {
         return false;
      } else if ((!mc.world.getBlockState(burrowPos).getMaterial().isReplaceable() || mc.world.getBlockState(burrowPos).getBlock() instanceof BlockLiquid) && (Boolean)this.await.getValue()) {
         return false;
      } else if (!mc.world.getBlockState(burrowPos.up(2)).getMaterial().isReplaceable()) {
         if (!(Boolean)this.await.getValue() && disable) {
            this.disable();
         }

         return false;
      } else if (mc.world.getBlockState(burrowPos.up()).getMaterial().blocksMovement()) {
         if (!(Boolean)this.await.getValue() && disable) {
            this.disable();
         }

         return false;
      } else if ((Boolean)this.await.getValue() && !this.burrowCooldown.isPassed()) {
         return false;
      } else if (!doSync) {
         return true;
      } else {
         boolean intercepted = BlockUtil.isInterceptedByCrystal(burrowPos);
         return this.didBreak || !intercepted;
      }
   }

   public void onDisable() {
      if (!fullNullCheck()) {
         this.placedPositions.clear();
         this.startY = Double.NaN;
         this.renderBlocks.clear();
      }
   }

   public void doRotate(BlockPos pos, boolean packet) {
      float[] rots = RotationUtil.getBlockRotations(pos, BlockUtil.getPlaceableSide(pos, (Boolean)this.strictDirection.getValue()));
      if (rots != null) {
         this.doRotate(rots, packet);
      }

   }

   public void doRotate(float[] rots, boolean packet) {
      if (packet) {
         mc.getConnection().sendPacket(new CPacketPlayer.Rotation(rots[0], rots[1], false));
      } else {
         mc.player.rotationYaw = rots[0];
         mc.player.rotationPitch = rots[1];
      }

   }

   public void resetPos() {
      mc.player.connection.sendPacket(new CPacketPlayer.Position(mc.player.posX, mc.player.posY + 0.41999998688698, mc.player.posZ, false));
      mc.player.connection.sendPacket(new CPacketPlayer.Position(mc.player.posX, mc.player.posY + 0.7531999805211997, mc.player.posZ, false));
      mc.player.connection.sendPacket(new CPacketPlayer.Position(mc.player.posX, mc.player.posY + 1.00133597911214, mc.player.posZ, false));
      mc.player.connection.sendPacket(new CPacketPlayer.Position(mc.player.posX, mc.player.posY + 1.16610926093821, mc.player.posZ, false));
   }

   @SubscribeEvent
   public void onPacket(PacketEvent event) {
      if (!fullNullCheck()) {
         if (event.getPacket() instanceof SPacketSoundEffect) {
            SPacketSoundEffect packet = (SPacketSoundEffect)event.getPacket();
            if (packet.getCategory() == SoundCategory.BLOCKS && packet.getSound() == SoundEvents.ENTITY_GENERIC_EXPLODE) {
               for(Entity c : CrystalUtil.getLoadedCrystalsInRange((double)3.0F)) {
                  if (c.getDistance(packet.getX(), packet.getY(), packet.getZ()) <= (double)6.0F) {
                     c.setDead();
                  }
               }
            }
         }

      }
   }

   Block getSelectedBlock() {
      Block selectedBlock = Blocks.OBSIDIAN;
      switch ((BlockMode)this.block.getValue()) {
         case Piston:
            selectedBlock = Blocks.PISTON;
            break;
         case EnderChest:
            selectedBlock = Blocks.ENDER_CHEST;
            break;
         case Obsidian:
            selectedBlock = Blocks.OBSIDIAN;
            break;
         case Dynamic:
            if (InventoryUtil.getHotbarItemSlot(Item.getItemFromBlock(Blocks.OBSIDIAN)) == -1) {
               return Blocks.ENDER_CHEST;
            }
      }

      return selectedBlock;
   }

   boolean intersectsWithEntity(BlockPos pos) {
      for(Entity entity : mc.world.loadedEntityList) {
         if (!entity.equals(mc.player) && !(entity instanceof EntityItem) && (!(entity instanceof EntityEnderCrystal) || !(Boolean)this.breakCrystal.getValue() && !(Boolean)this.await.getValue()) && (new AxisAlignedBB(pos)).intersects(entity.getEntityBoundingBox())) {
            return true;
         }
      }

      return false;
   }

   BlockPos getPlayerPos() {
      double decimalPoint = mc.player.posY - Math.floor(mc.player.posY);
      return new BlockPos(mc.player.posX, decimalPoint > 0.8 ? Math.floor(mc.player.posY) + (double)1.0F : Math.floor(mc.player.posY), mc.player.posZ);
   }

   List<BlockPos> getOverlapPos() {
      List<BlockPos> positions = new ArrayList();
      double decimalX = mc.player.posX - Math.floor(mc.player.posX);
      double decimalZ = mc.player.posZ - Math.floor(mc.player.posZ);
      int offX = this.calcOffset(decimalX);
      int offZ = this.calcOffset(decimalZ);
      positions.add(this.getPlayerPos());

      for(int x = 0; x <= Math.abs(offX); ++x) {
         for(int z = 0; z <= Math.abs(offZ); ++z) {
            int properX = x * offX;
            int properZ = z * offZ;
            positions.add(this.getPlayerPos().add(properX, 0, properZ));
         }
      }

      return positions;
   }

   int calcOffset(double dec) {
      return dec >= 0.7 ? 1 : (dec <= 0.3 ? -1 : 0);
   }

   public static enum RotateMode {
      None,
      Normal,
      Packet;
   }

   public static enum BlockMode {
      EnderChest,
      Obsidian,
      Piston,
      Dynamic;
   }

   private static class RenderBlock {
      BlockPos pos;
      long startTime;

      public RenderBlock(BlockPos pos) {
         this.pos = pos;
         this.startTime = System.currentTimeMillis();
      }

      float getProgress(int timeMs) {
         long elapsed = System.currentTimeMillis() - this.startTime;
         return Math.min(1.0F, (float)elapsed / (float)timeMs);
      }
   }
}
