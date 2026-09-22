package fat.vonware.features.modules.combat;

import fat.vonware.Global;
import fat.vonware.event.events.Render3DEvent;
import fat.vonware.features.modules.Module;
import fat.vonware.features.modules.client.Colors;
import fat.vonware.features.setting.Setting;
import fat.vonware.util.BlockUtil;
import fat.vonware.util.DamageUtil;
import fat.vonware.util.EntityUtil;
import fat.vonware.util.InventoryUtil;
import fat.vonware.util.MathUtil;
import fat.vonware.util.Timer;
import fat.vonware.util.Animations.render.ColorUtil;
import fat.vonware.util.Animations.render.RenderUtil;
import fat.vonware.util.BlockHighlight.SlideUtil;
import fat.vonware.util.BlockPlaceModules.BlockPlaceUtil;
import java.awt.Color;
import net.minecraft.block.Block;
import net.minecraft.block.BlockObsidian;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityEnderCrystal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;

public class CrystalBase extends Module {
   private final Timer delayTimer = new Timer();
   private final Timer renderTimer = new Timer();
   public Setting<Boolean> render = new Setting<Boolean>("Render", true, "Renders crystal base placement positions.");
   public Setting<Boolean> outline = new Setting<Boolean>("Outline", true, (v) -> (Boolean)this.render.getValue(), "Draws an outline around rendered crystal base positions.");
   public Setting<Integer> outlineAlpha = new Setting<Integer>("Outline Alpha", 150, 0, 255, (v) -> (Boolean)this.render.getValue() && (Boolean)this.outline.getValue(), "Controls the transparency of the crystal base outline.");
   public Setting<Boolean> box = new Setting<Boolean>("Box", true, (v) -> (Boolean)this.render.getValue(), "Draws a filled box at rendered crystal base positions.");
   public Setting<Integer> boxAlpha = new Setting<Integer>("Box Alpha", 70, 0, 255, (v) -> (Boolean)this.render.getValue() && (Boolean)this.box.getValue(), "Controls the transparency of the filled crystal base box.");
   private final Setting<Integer> renderTime = new Setting<Integer>("Render Time", 3000, 0, 5000, (v) -> (Boolean)this.render.getValue(), "How long a placed crystal base remains rendered.");
   private final Setting<Integer> shrinkTime = new Setting<Integer>("Shrink Time", 600, 0, 5000, (v) -> (Boolean)this.render.getValue(), "How long the crystal base render takes to shrink away.");
   public Setting<Boolean> rotate = new Setting<Boolean>("Rotate", true, "Rotates toward the base position before placing.");
   public Setting<Boolean> packet = new Setting<Boolean>("Packet", true, "Uses packet placement for the crystal base block.");
   public Setting<Boolean> strictPlace = new Setting<Boolean>("Strict", false, "Uses stricter placement direction checks for crystal bases.");
   public Setting<Boolean> checkRaytrace = new Setting<Boolean>("Check Raytrace", false, "Requires a valid raytrace to the selected crystal base position.");
   public Setting<Float> range = new Setting<Float>("Range", 6.0F, 0.0F, 10.0F, "Maximum distance used when searching for valid targets or base positions.");
   public Setting<Float> wallsRange = new Setting<Float>("Walls Range", 3.5F, 0.0F, 10.0F, "Maximum usable distance when the position is behind a wall.");
   public Setting<Integer> placeDelay = new Setting<Integer>("Place Delay", 1, 0, 20, "Delay between crystal base placement attempts.");
   public Setting<Float> minDmg = new Setting<Float>("Min Dmg", 6.0F, 0.0F, 10.0F, "Minimum crystal damage required before placing a base for it.");
   public Setting<Float> placeRange = new Setting<Float>("Place Range", 4.0F, 1.0F, 6.0F, "Maximum distance at which the base block can be placed.");
   private static CrystalBase INSTANCE = new CrystalBase();
   private BlockPos placePos;
   private SlideUtil shrinkTimer;

   public CrystalBase() {
      super("CrystalBase", "Auto places obsidian for crystal placement", Module.Category.COMBAT, true, false, false);
      this.register(this.render);
      this.register(this.outline);
      this.register(this.outlineAlpha);
      this.register(this.box);
      this.register(this.boxAlpha);
      this.register(this.renderTime);
      this.register(this.shrinkTime);
      this.register(this.rotate);
      this.register(this.packet);
      this.register(this.range);
      this.register(this.wallsRange);
      this.register(this.placeDelay);
      this.register(this.minDmg);
      this.register(this.placeRange);
      this.register(this.strictPlace);
      this.register(this.checkRaytrace);
      this.shrinkTimer = new SlideUtil((long)(Integer)this.shrinkTime.getValue());
      this.setInstance();
   }

   public static CrystalBase getInstance() {
      if (INSTANCE == null) {
         INSTANCE = new CrystalBase();
      }

      return INSTANCE;
   }

   private void setInstance() {
      INSTANCE = this;
   }

   public void onUpdate() {
      EntityPlayer target = EntityUtil.getTarget((double)(Float)this.range.getValue());
      if (this.delayTimer.passedMs((long)(Integer)this.placeDelay.getValue() * 100L) && target != null) {
         this.placePos = this.getPlaceTarget(target);
         if (this.placePos != null && BlockPlaceUtil.canPlace(this.placePos)) {
            this.shrinkTimer = new SlideUtil((long)(Integer)this.shrinkTime.getValue());
            this.delayTimer.reset();
            this.renderTimer.reset();
            this.placeBlock(this.placePos);
         }

      }
   }

   public void onRender3D(Render3DEvent event) {
      if (this.placePos != null && !this.renderTimer.passedMs((long)(Integer)this.renderTime.getValue()) && !this.renderTimer.passedMs((long)(Integer)this.renderTime.getValue()) && (Boolean)this.render.getValue()) {
         if (mc.world.getBlockState(this.placePos).getBlock() != Blocks.AIR) {
            if (mc.world.getBlockState(this.placePos).getBlock() != Blocks.FIRE) {
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

               AxisAlignedBB axisAlignedBB = mc.world.getBlockState(this.placePos).getSelectedBoundingBox(mc.world, this.placePos).grow(this.shrinkTimer.easeInQuad() / (double)2.0F - (double)1.0F);
               if ((Boolean)this.outline.getValue()) {
                  RenderUtil.drawBBBox(axisAlignedBB, color, (Integer)this.outlineAlpha.getValue());
               }

               if ((Boolean)this.box.getValue()) {
                  RenderUtil.drawBBFill(axisAlignedBB, color, (Integer)this.boxAlpha.getValue());
               }

            }
         }
      }
   }

   private void placeBlock(BlockPos pos) {
      if (BlockPlaceUtil.canPlace(pos)) {
         int old = mc.player.inventory.currentItem;
         if (InventoryUtil.findHotbarClass(BlockObsidian.class) != -1) {
            InventoryUtil.doSwap(InventoryUtil.findHotbarClass(BlockObsidian.class));
            BlockUtil.placeBlock(pos, EnumHand.MAIN_HAND, (Boolean)this.rotate.getValue(), (Boolean)this.packet.getValue(), false);
            InventoryUtil.doSwap(old);
         }
      }
   }

   public String getInfo() {
      if (fullNullCheck()) {
         return null;
      } else {
         EntityPlayer target = EntityUtil.getTarget((double)(Float)this.range.getValue());
         return target != null ? target.getName() : null;
      }
   }

   private boolean canPlaceCrystal(BlockPos pos) {
      BlockPos obsPos = pos.down();
      BlockPos boost = obsPos.up();
      BlockPos boost2 = obsPos.up(2);
      return (this.getBlock(obsPos) == Blocks.BEDROCK || this.getBlock(obsPos) == Blocks.OBSIDIAN) && this.getBlock(boost) == Blocks.AIR && this.getBlock(boost2) == Blocks.AIR && !this.checkEntity(boost2) && !this.checkEntity(boost);
   }

   private Block getBlock(BlockPos pos) {
      return mc.world.getBlockState(pos).getBlock();
   }

   private boolean checkEntity(BlockPos pos) {
      for(Entity entity : mc.world.getEntitiesWithinAABB(Entity.class, new AxisAlignedBB(pos))) {
         if (!entity.isDead && !(entity instanceof EntityEnderCrystal)) {
            return true;
         }
      }

      return false;
   }

   public static boolean behindWall(BlockPos pos) {
      RayTraceResult result = mc.world.rayTraceBlocks(new Vec3d(mc.player.posX, mc.player.posY + (double)mc.player.getEyeHeight(), mc.player.posZ), new Vec3d((double)pos.getX() + (double)0.5F, (double)pos.getY() - (double)0.5F, (double)pos.getZ() + (double)0.5F));
      return (result == null || result.sideHit == null || mc.world.rayTraceBlocks(new Vec3d(mc.player.posX, mc.player.posY + (double)mc.player.getEyeHeight(), mc.player.posZ), new Vec3d((double)pos.getX() + (double)0.5F, (double)pos.getY(), (double)pos.getZ() + (double)0.5F), false, true, false) != null) && mc.player.getDistance((double)pos.getX() + (double)0.5F, (double)pos.getY(), (double)pos.getZ() + (double)0.5F) > (double)(Float)INSTANCE.wallsRange.getValue();
   }

   private BlockPos getPlaceTarget(Entity target) {
      for(BlockPos pos : BlockUtil.getBox(5.0F, EntityUtil.getEntityPos(target).down())) {
         if (this.canPlaceCrystal(pos) && !behindWall(pos) && mc.player.getDistance((double)pos.getX() + (double)0.5F, (double)pos.getY() + (double)0.5F, (double)pos.getZ() + (double)0.5F) <= (double)(Float)this.placeRange.getValue() && !(DamageUtil.calculateDamage(pos.down(), target) <= (Float)this.minDmg.getValue())) {
            return null;
         }
      }

      BlockPos bestPos = null;
      float bestDamage = 0.0F;

      for(BlockPos pos2 : BlockUtil.getBox((Float)this.range.getValue())) {
         float damage2;
         if (mc.world.isAirBlock(pos2) && mc.world.isAirBlock(pos2.up()) && mc.world.isAirBlock(pos2.up(2)) && mc.player.getDistance((double)pos2.getX() + (double)0.5F, (double)pos2.getY() + (double)0.5F, (double)pos2.getZ() + (double)0.5F) <= (double)(Float)this.placeRange.getValue() && !this.checkEntity(pos2.up()) && !this.checkEntity(pos2.up(2)) && BlockPlaceUtil.canPlace(pos2) && (double)pos2.getY() <= (double)(new BlockPos(target.posX, target.posY + (double)0.5F, target.posZ)).getY() - (double)0.5F && (damage2 = DamageUtil.calculateDamage(pos2, target)) >= (Float)this.minDmg.getValue() && (!(mc.player.getDistanceSq(pos2) >= MathUtil.square((double)(Float)this.wallsRange.getValue())) || !BlockUtil.rayTracePlaceCheck(pos2, true, 1.0F))) {
            if (bestPos == null) {
               bestDamage = damage2;
               bestPos = pos2;
            } else if (!(damage2 < bestDamage)) {
               bestDamage = damage2;
               bestPos = pos2;
            }
         }
      }

      return bestPos;
   }
}
