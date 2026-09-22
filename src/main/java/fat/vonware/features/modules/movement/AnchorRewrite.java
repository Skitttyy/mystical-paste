package fat.vonware.features.modules.movement;

import fat.vonware.event.events.MoveEvent;
import fat.vonware.event.events.Render3DEvent;
import fat.vonware.features.modules.Module;
import fat.vonware.features.setting.ColorSetting;
import fat.vonware.features.setting.Setting;
import fat.vonware.util.BlockUtil;
import fat.vonware.util.Animations.render.ColorUtil;
import fat.vonware.util.Animations.render.RenderMethods;
import fat.vonware.util.holeesp.Hole;
import fat.vonware.util.holeesp.HoleUtil;
import java.awt.Color;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.init.Blocks;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

public class AnchorRewrite extends Module {
   private final Setting<Integer> yRange = new Setting<Integer>("Y-Range", 3, 1, 5, "Maximum vertical distance below you searched for anchor holes.");
   private final Setting<Float> activateRange = new Setting<Float>("ActivateRange", 0.9F, 0.0F, 2.0F, "Horizontal distance to the selected hole center at which Anchor begins pulling you into it.");
   private final Setting<Boolean> doubles = new Setting<Boolean>("2x1", true, "Allows 2x1 double holes to be selected as anchor targets.");
   private final Setting<Boolean> incomplete = new Setting<Boolean>("Incomplete", true, "Allows incomplete double-hole shapes in the Anchor hole scan.");
   private final Setting<Boolean> twoByTwo = new Setting<Boolean>("2x2", true, "Allows 2x2 holes to be selected by Anchor.");
   private final Setting<Boolean> terrain = new Setting<Boolean>("Terrain", true, "Includes terrain-style holes in Anchor detection.");
   private final Setting<Boolean> debugVisual = new Setting<Boolean>("Debug-Visual", false, "Toggles the Debug-Visual feature used by AnchorRewrite.");
   private final ColorSetting visColor = new ColorSetting("Color", 5227511, false, (v) -> (Boolean)this.debugVisual.getValue(), "Selects the color used by AnchorRewrite for Color rendering.");
   private final Setting<Integer> visAlpha = new Setting<Integer>("Vis-Alpha", 180, 0, 255, (v) -> (Boolean)this.debugVisual.getValue(), "Controls the opacity used by AnchorRewrite for Vis-Alpha.");
   private final Setting<Integer> visFillAlpha = new Setting<Integer>("Vis-Fill", 30, 0, 255, (v) -> (Boolean)this.debugVisual.getValue(), "Controls the filled-box portion of AnchorRewrite's render.");
   private final Setting<Float> visLineWidth = new Setting<Float>("Vis-LineWidth", 1.0F, 0.1F, 3.0F, (v) -> (Boolean)this.debugVisual.getValue(), "Controls the rendered line thickness used by AnchorRewrite.");
   private final Setting<Boolean> visRing = new Setting<Boolean>("Vis-Ring", true, (v) -> (Boolean)this.debugVisual.getValue(), "Toggles the Vis-Ring feature used by AnchorRewrite.");
   private final Setting<Boolean> visTarget = new Setting<Boolean>("Vis-Target", true, (v) -> (Boolean)this.debugVisual.getValue(), "Toggles the Vis-Target feature used by AnchorRewrite.");
   private final Setting<Boolean> visLine = new Setting<Boolean>("Vis-Line", true, (v) -> (Boolean)this.debugVisual.getValue(), "Toggles the Vis-Line feature used by AnchorRewrite.");
   private static final long COOLDOWN_MS = 1500L;
   private static final double Y_CHANGE_THRESHOLD = 0.015;
   private static final int RING_SEGMENTS = 64;
   private boolean anchoring = false;
   private Hole targetHole = null;
   private boolean isIncomplete = false;
   private long cooldownUntil = 0L;
   private double lastGroundY = Double.NaN;

   public AnchorRewrite() {
      super("AnchorRewrite", "Automatically pulls you into a hole", Module.Category.MOVEMENT, true, false, false);
      this.register(this.yRange);
      this.register(this.activateRange);
      this.register(this.doubles);
      this.register(this.incomplete);
      this.register(this.twoByTwo);
      this.register(this.terrain);
      this.register(this.debugVisual);
      this.register(this.visColor);
      this.register(this.visAlpha);
      this.register(this.visFillAlpha);
      this.register(this.visLineWidth);
      this.register(this.visRing);
      this.register(this.visTarget);
      this.register(this.visLine);
   }

   public void onEnable() {
      if (!fullNullCheck()) {
         this.anchoring = false;
         this.targetHole = null;
         this.isIncomplete = false;
         this.cooldownUntil = 0L;
         this.lastGroundY = Double.NaN;
      }
   }

   public void onDisable() {
      if (!fullNullCheck()) {
         this.anchoring = false;
         this.targetHole = null;
         this.isIncomplete = false;
         this.cooldownUntil = 0L;
         this.lastGroundY = Double.NaN;
      }
   }

   public void onUpdate() {
      if (!fullNullCheck()) {
         long now = System.currentTimeMillis();
         if (this.anchoring && this.targetHole != null) {
            if (mc.world.getBlockState(this.targetHole.getFirst()).getBlock() != Blocks.AIR) {
               this.stopAnchoring(false);
            } else {
               if (this.isIncomplete && this.targetHole.getSecond() != null) {
                  BlockPos openSide = this.getIncompleteOpenSide(this.targetHole);
                  if (openSide == null) {
                     this.stopAnchoring(false);
                     return;
                  }
               }

               if (mc.player.onGround && HoleUtil.isInHole(mc.player)) {
                  if (!Double.isNaN(this.lastGroundY) && Math.abs(mc.player.posY - this.lastGroundY) > 0.015) {
                     this.stopAnchoring(true);
                  } else {
                     this.stopAnchoring(true);
                  }
               } else {
                  if (mc.player.onGround) {
                     this.lastGroundY = mc.player.posY;
                  } else {
                     this.lastGroundY = Double.NaN;
                  }

               }
            }
         } else if (now >= this.cooldownUntil) {
            Hole found = this.findNearestHoleInRange();
            if (found != null) {
               this.targetHole = found;
               this.anchoring = true;
               this.isIncomplete = this.isIncompleteHole(found);
               this.lastGroundY = Double.NaN;
            }

         }
      }
   }

   @SubscribeEvent
   public void onMove(MoveEvent event) {
      if (!fullNullCheck()) {
         if (this.anchoring && this.targetHole != null) {
            if (mc.player.onGround && HoleUtil.isInHole(mc.player)) {
               this.stopAnchoring(true);
            } else {
               Vec3d center = this.getHoleCenter(this.targetHole);
               double dx = center.x - mc.player.posX;
               double dz = center.z - mc.player.posZ;
               double dist = Math.hypot(dx, dz);
               if (!this.isInRange(this.targetHole, dx, dz)) {
                  this.stopAnchoring(false);
               } else if (dist < 0.05) {
                  if (mc.player.onGround) {
                     this.stopAnchoring(true);
                  } else {
                     event.setX((double)0.0F);
                     event.setZ((double)0.0F);
                     mc.player.motionX = (double)0.0F;
                     mc.player.motionZ = (double)0.0F;
                  }
               } else {
                  double yawRad = Math.atan2(-dx, dz);
                  double speed = this.resolveSpeed(dist);
                  mc.player.motionX = (double)0.0F;
                  mc.player.motionZ = (double)0.0F;
                  event.setX(-Math.sin(yawRad) * speed);
                  event.setZ(Math.cos(yawRad) * speed);
               }
            }
         }
      }
   }

   @SubscribeEvent
   public void onRender3D(Render3DEvent event) {
      if (!fullNullCheck()) {
         if ((Boolean)this.debugVisual.getValue()) {
            Color base = ColorUtil.getResolvedColor(this.visColor);
            Color line = new Color(base.getRed(), base.getGreen(), base.getBlue(), (Integer)this.visAlpha.getValue());
            Color fill = new Color(base.getRed(), base.getGreen(), base.getBlue(), (Integer)this.visFillAlpha.getValue());
            Color lineDim = new Color(base.getRed(), base.getGreen(), base.getBlue(), Math.max(0, (Integer)this.visAlpha.getValue() / 3));
            Color fillDim = new Color(base.getRed(), base.getGreen(), base.getBlue(), Math.max(0, (Integer)this.visFillAlpha.getValue() / 3));
            float scanRadius = Math.max((Float)this.activateRange.getValue() + 3.0F, (float)(Integer)this.yRange.getValue() + 2.0F);

            for(Hole hole : HoleUtil.getHoles(mc.player, scanRadius, (Boolean)this.doubles.getValue(), false, false, (Boolean)this.terrain.getValue(), (Boolean)this.incomplete.getValue(), (Boolean)this.twoByTwo.getValue())) {
               double playerY = mc.player.posY;
               double holeY = (double)hole.getFirst().getY();
               if (!(playerY <= holeY) && !(playerY - holeY > (double)(Integer)this.yRange.getValue())) {
                  boolean isTarget = this.anchoring && this.targetHole != null && hole.getFirst().equals(this.targetHole.getFirst());
                  Vec3d center = this.getHoleCenter(hole);
                  double ringY = holeY + 1.01;
                  float r = (Float)this.activateRange.getValue();
                  boolean is2x1 = hole.getSecond() != null && !hole.is2x2();
                  boolean isNS = is2x1 && hole.getFirst().getX() == hole.getSecond().getX();
                  Color ringLine = isTarget ? line : lineDim;
                  Color ringFill = isTarget ? fill : fillDim;
                  if ((Boolean)this.visRing.getValue()) {
                     if (is2x1) {
                        double ra = isNS ? (double)r : (double)r + (double)0.5F;
                        double rb = isNS ? (double)r + (double)0.5F : (double)r;
                        this.drawHorizontalEllipse(center.x, ringY, center.z, ra, rb, ringLine, ringFill, (Float)this.visLineWidth.getValue());
                     } else if (hole.is2x2()) {
                        this.drawHorizontalRing(center.x, ringY, center.z, (double)r + (double)0.5F, ringLine, ringFill, (Float)this.visLineWidth.getValue());
                     } else {
                        this.drawHorizontalRing(center.x, ringY, center.z, (double)r, ringLine, ringFill, (Float)this.visLineWidth.getValue());
                     }
                  }

                  if ((Boolean)this.visTarget.getValue()) {
                     AxisAlignedBB holeBB = this.buildHoleBB(hole);
                     RenderMethods.drawBlockHighlightAnimation(holeBB, ringLine, false, ringFill, isTarget ? (Float)this.visLineWidth.getValue() : (Float)this.visLineWidth.getValue() * 0.5F, true, true, ringFill.getAlpha(), false);
                  }

                  if ((Boolean)this.visLine.getValue() && isTarget) {
                     this.drawLine(mc.player.posX, mc.player.posY, mc.player.posZ, center.x, ringY, center.z, line, (Float)this.visLineWidth.getValue());
                  }
               }
            }

         }
      }
   }

   private void drawHorizontalRing(double cx, double cy, double cz, double radius, Color lineColor, Color fillColor, float lineWidth) {
      double px = mc.getRenderManager().viewerPosX;
      double py = mc.getRenderManager().viewerPosY;
      double pz = mc.getRenderManager().viewerPosZ;
      GlStateManager.pushMatrix();
      GlStateManager.translate(cx - px, cy - py + 0.02, cz - pz);
      GlStateManager.disableTexture2D();
      GlStateManager.enableBlend();
      GlStateManager.blendFunc(770, 771);
      GlStateManager.disableDepth();
      GlStateManager.depthMask(false);
      GlStateManager.disableLighting();
      GlStateManager.disableCull();
      GL11.glBegin(6);
      GL11.glColor4f((float)fillColor.getRed() / 255.0F, (float)fillColor.getGreen() / 255.0F, (float)fillColor.getBlue() / 255.0F, (float)fillColor.getAlpha() / 255.0F);
      GL11.glVertex3d((double)0.0F, (double)0.0F, (double)0.0F);

      for(int i = 0; i <= 64; ++i) {
         double angle = (Math.PI * 2D) * (double)i / (double)64.0F;
         GL11.glVertex3d(Math.cos(angle) * radius, (double)0.0F, Math.sin(angle) * radius);
      }

      GL11.glEnd();
      GL11.glLineWidth(lineWidth);
      GL11.glBegin(2);
      GL11.glColor4f((float)lineColor.getRed() / 255.0F, (float)lineColor.getGreen() / 255.0F, (float)lineColor.getBlue() / 255.0F, (float)lineColor.getAlpha() / 255.0F);

      for(int i = 0; i < 64; ++i) {
         double angle = (Math.PI * 2D) * (double)i / (double)64.0F;
         GL11.glVertex3d(Math.cos(angle) * radius, (double)0.0F, Math.sin(angle) * radius);
      }

      GL11.glEnd();
      GlStateManager.enableCull();
      GlStateManager.depthMask(true);
      GlStateManager.enableDepth();
      GlStateManager.disableBlend();
      GlStateManager.enableTexture2D();
      GlStateManager.enableLighting();
      GL11.glLineWidth(1.0F);
      GlStateManager.popMatrix();
   }

   private void drawHorizontalEllipse(double cx, double cy, double cz, double ra, double rb, Color lineColor, Color fillColor, float lineWidth) {
      double px = mc.getRenderManager().viewerPosX;
      double py = mc.getRenderManager().viewerPosY;
      double pz = mc.getRenderManager().viewerPosZ;
      GlStateManager.pushMatrix();
      GlStateManager.translate(cx - px, cy - py, cz - pz);
      GlStateManager.disableTexture2D();
      GlStateManager.enableBlend();
      GlStateManager.blendFunc(770, 771);
      GlStateManager.disableDepth();
      GlStateManager.depthMask(false);
      GlStateManager.disableLighting();
      GlStateManager.disableCull();
      GL11.glBegin(6);
      GL11.glColor4f((float)fillColor.getRed() / 255.0F, (float)fillColor.getGreen() / 255.0F, (float)fillColor.getBlue() / 255.0F, (float)fillColor.getAlpha() / 255.0F);
      GL11.glVertex3d((double)0.0F, (double)0.0F, (double)0.0F);

      for(int i = 0; i <= 64; ++i) {
         double angle = (Math.PI * 2D) * (double)i / (double)64.0F;
         GL11.glVertex3d(Math.cos(angle) * ra, (double)0.0F, Math.sin(angle) * rb);
      }

      GL11.glEnd();
      GL11.glLineWidth(lineWidth);
      GL11.glBegin(2);
      GL11.glColor4f((float)lineColor.getRed() / 255.0F, (float)lineColor.getGreen() / 255.0F, (float)lineColor.getBlue() / 255.0F, (float)lineColor.getAlpha() / 255.0F);

      for(int i = 0; i < 64; ++i) {
         double angle = (Math.PI * 2D) * (double)i / (double)64.0F;
         GL11.glVertex3d(Math.cos(angle) * ra, (double)0.0F, Math.sin(angle) * rb);
      }

      GL11.glEnd();
      GlStateManager.enableCull();
      GlStateManager.depthMask(true);
      GlStateManager.enableDepth();
      GlStateManager.disableBlend();
      GlStateManager.enableTexture2D();
      GlStateManager.enableLighting();
      GL11.glLineWidth(1.0F);
      GlStateManager.popMatrix();
   }

   private void drawLine(double x1, double y1, double z1, double x2, double y2, double z2, Color color, float lineWidth) {
      double px = mc.getRenderManager().viewerPosX;
      double py = mc.getRenderManager().viewerPosY;
      double pz = mc.getRenderManager().viewerPosZ;
      GlStateManager.pushMatrix();
      GlStateManager.translate(-px, -py, -pz);
      GlStateManager.disableTexture2D();
      GlStateManager.enableBlend();
      GlStateManager.blendFunc(770, 771);
      GlStateManager.disableDepth();
      GlStateManager.depthMask(false);
      GlStateManager.disableLighting();
      GL11.glLineWidth(lineWidth);
      GL11.glBegin(1);
      GL11.glColor4f((float)color.getRed() / 255.0F, (float)color.getGreen() / 255.0F, (float)color.getBlue() / 255.0F, (float)color.getAlpha() / 255.0F);
      GL11.glVertex3d(x1, y1, z1);
      GL11.glVertex3d(x2, y2, z2);
      GL11.glEnd();
      GlStateManager.depthMask(true);
      GlStateManager.enableDepth();
      GlStateManager.disableBlend();
      GlStateManager.enableTexture2D();
      GlStateManager.enableLighting();
      GL11.glLineWidth(1.0F);
      GlStateManager.popMatrix();
   }

   private AxisAlignedBB buildHoleBB(Hole hole) {
      BlockPos first = hole.getFirst();
      if (hole.is2x2()) {
         return new AxisAlignedBB((double)first.getX(), (double)first.getY() + 0.9, (double)first.getZ(), (double)first.getX() + (double)2.0F, (double)first.getY() + (double)1.0F, (double)first.getZ() + (double)2.0F);
      } else if (hole.getSecond() != null) {
         BlockPos second = hole.getSecond();
         double minX = (double)Math.min(first.getX(), second.getX());
         double minZ = (double)Math.min(first.getZ(), second.getZ());
         double maxX = (double)Math.max(first.getX(), second.getX()) + (double)1.0F;
         double maxZ = (double)Math.max(first.getZ(), second.getZ()) + (double)1.0F;
         return new AxisAlignedBB(minX, (double)first.getY() + 0.9, minZ, maxX, (double)first.getY() + (double)1.0F, maxZ);
      } else {
         return new AxisAlignedBB((double)first.getX(), (double)first.getY() + 0.9, (double)first.getZ(), (double)first.getX() + (double)1.0F, (double)first.getY() + (double)1.0F, (double)first.getZ() + (double)1.0F);
      }
   }

   private boolean isInRange(Hole hole, double dx, double dz) {
      float r = (Float)this.activateRange.getValue();
      if (hole.is2x2()) {
         double r2x2 = (double)r + (double)0.5F;
         return dx * dx + dz * dz <= r2x2 * r2x2;
      } else if (hole.getSecond() == null) {
         return dx * dx + dz * dz <= (double)r * (double)r;
      } else {
         boolean isNS = hole.getFirst().getX() == hole.getSecond().getX();
         double a = isNS ? (double)r : (double)r + (double)0.5F;
         double b = isNS ? (double)r + (double)0.5F : (double)r;
         return dx / a * (dx / a) + dz / b * (dz / b) <= (double)1.0F;
      }
   }

   private void stopAnchoring(boolean withCooldown) {
      this.anchoring = false;
      this.targetHole = null;
      this.isIncomplete = false;
      if (withCooldown) {
         this.cooldownUntil = System.currentTimeMillis() + 1500L;
      }

   }

   private Hole findNearestHoleInRange() {
      float searchRadius = Math.max((Float)this.activateRange.getValue(), (float)(Integer)this.yRange.getValue());
      List<Hole> holes = HoleUtil.getHoles(mc.player, searchRadius, (Boolean)this.doubles.getValue(), false, false, (Boolean)this.terrain.getValue(), (Boolean)this.incomplete.getValue(), (Boolean)this.twoByTwo.getValue());
      return holes.isEmpty() ? null : (Hole)holes.stream().filter((h) -> {
         double playerY = mc.player.posY;
         double holeY = (double)h.getFirst().getY();
         if (playerY <= holeY) {
            return false;
         } else if (playerY - holeY > (double)(Integer)this.yRange.getValue()) {
            return false;
         } else {
            if (h.getSecond() != null && !h.is2x2() && this.isIncompleteHole(h)) {
               if (!(Boolean)this.incomplete.getValue()) {
                  return false;
               }

               if (!this.isIncompleteAnchorable(h)) {
                  return false;
               }
            }

            Vec3d center = this.getHoleCenter(h);
            return this.isInRange(h, mc.player.posX - center.x, mc.player.posZ - center.z);
         }
      }).min(Comparator.comparingDouble((h) -> {
         Vec3d center = this.getHoleCenter(h);
         return Math.hypot(mc.player.posX - center.x, mc.player.posZ - center.z);
      })).orElse(null);
   }

   private int getAirBlocksUp(BlockPos pos) {
      if (!BlockUtil.isAir(pos.up(1))) {
         return 0;
      } else {
         return !BlockUtil.isAir(pos.up(2)) ? 1 : 2;
      }
   }

   private boolean isIncompleteHole(Hole hole) {
      if (hole.getSecond() != null && !hole.is2x2()) {
         int firstUp = this.getAirBlocksUp(hole.getFirst());
         int secondUp = this.getAirBlocksUp(hole.getSecond());
         return firstUp != secondUp;
      } else {
         return false;
      }
   }

   private BlockPos getIncompleteOpenSide(Hole hole) {
      if (hole.getSecond() == null) {
         return null;
      } else {
         int firstUp = this.getAirBlocksUp(hole.getFirst());
         int secondUp = this.getAirBlocksUp(hole.getSecond());
         if (firstUp == secondUp) {
            return null;
         } else if (firstUp > secondUp) {
            return firstUp > 0 ? hole.getFirst() : null;
         } else {
            return secondUp > 0 ? hole.getSecond() : null;
         }
      }
   }

   private boolean isIncompleteAnchorable(Hole hole) {
      BlockPos openSide = this.getIncompleteOpenSide(hole);
      if (openSide == null) {
         return false;
      } else {
         return this.getAirBlocksUp(openSide) >= 1;
      }
   }

   private Vec3d getHoleCenter(Hole hole) {
      if (hole.is2x2()) {
         return new Vec3d((double)hole.getFirst().getX() + (double)1.0F, (double)hole.getFirst().getY(), (double)hole.getFirst().getZ() + (double)1.0F);
      } else {
         if (hole.getSecond() != null && this.isIncomplete && this.isIncompleteHole(hole)) {
            BlockPos openSide = this.getIncompleteOpenSide(hole);
            if (openSide != null) {
               return new Vec3d((double)openSide.getX() + (double)0.5F, (double)openSide.getY(), (double)openSide.getZ() + (double)0.5F);
            }
         }

         if (hole.getSecond() != null) {
            double cx = (double)(hole.getFirst().getX() + hole.getSecond().getX()) / (double)2.0F + (double)0.5F;
            double cz = (double)(hole.getFirst().getZ() + hole.getSecond().getZ()) / (double)2.0F + (double)0.5F;
            return new Vec3d(cx, (double)hole.getFirst().getY(), cz);
         } else {
            BlockPos pos = hole.getFirst();
            return new Vec3d((double)pos.getX() + (double)0.5F, (double)pos.getY(), (double)pos.getZ() + (double)0.5F);
         }
      }
   }

   private double resolveSpeed(double dist) {
      double base = this.getBaseSpeed();
      double current = Math.hypot(mc.player.motionX, mc.player.motionZ);
      double speed = Math.max(current, base);
      return Math.min(speed, dist);
   }

   private double getBaseSpeed() {
      double base = 0.2873;
      if (mc.player.isPotionActive(MobEffects.SPEED)) {
         PotionEffect e = mc.player.getActivePotionEffect(MobEffects.SPEED);
         if (e != null) {
            base *= (double)1.0F + 0.2 * (double)(e.getAmplifier() + 1);
         }
      }

      if (mc.player.isPotionActive(MobEffects.SLOWNESS)) {
         PotionEffect e = mc.player.getActivePotionEffect(MobEffects.SLOWNESS);
         if (e != null) {
            base *= Math.max((double)0.0F, (double)1.0F - 0.15 * (double)(e.getAmplifier() + 1));
         }
      }

      return base;
   }
}
