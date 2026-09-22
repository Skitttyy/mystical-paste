package fat.vonware.features.modules.render;

import fat.vonware.Global;
import fat.vonware.event.events.Render3DEvent;
import fat.vonware.features.gui.components.items.buttons.render.ColorPickerConfig;
import fat.vonware.features.modules.Module;
import fat.vonware.features.setting.ColorSetting;
import fat.vonware.features.setting.Setting;
import fat.vonware.util.BlockUtil;
import fat.vonware.util.Interpolation;
import fat.vonware.util.MathUtil;
import fat.vonware.util.PositionUtil;
import fat.vonware.util.Animations.render.ColorUtil;
import fat.vonware.util.Animations.render.RenderMethods;
import fat.vonware.util.Animations.render.RenderUtil;
import fat.vonware.util.Chat.ThreadUtil;
import fat.vonware.util.holeesp.Hole;
import fat.vonware.util.holeesp.HoleUtil;
import fat.vonware.util.shader.Shader;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import net.minecraft.block.BlockAir;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.GlStateManager.DestFactor;
import net.minecraft.client.renderer.GlStateManager.SourceFactor;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.opengl.GL20;

public class HoleESP extends Module {
   protected final Setting<Page> page;
   protected final Setting<Integer> range;
   protected final Setting<Boolean> twoByOne;
   protected final Setting<Boolean> twoByTwo;
   protected final Setting<Boolean> incompleteDoubles;
   protected final Setting<Boolean> onlyVisible;
   protected final Setting<Boolean> dynamic;
   protected final Setting<Float> height;
   protected final Setting<Integer> alpha;
   protected final Setting<Float> heightVoid;
   protected final Setting<Boolean> fadeAlpha;
   public Setting<Boolean> glow;
   public Setting<Boolean> reverseGlow;
   public Setting<Boolean> glowEdges;
   protected final Setting<Outline> outlineMode;
   protected final Setting<Integer> wireAlpha;
   protected final Setting<Float> lineWidth;
   public Setting<Boolean> voids;
   protected final Setting<Integer> voidRange;
   public Setting<Boolean> traps;
   public Setting<Boolean> terrain;
   protected final Setting<Boolean> bloom;
   protected final Setting<Float> bloomIntensity;
   protected final Setting<Float> bloomRadius;
   protected final Setting<Boolean> animateIn;
   protected final Setting<Integer> animationSpeed;
   protected final Setting<Boolean> scaleAnimation;
   protected final Setting<Boolean> slideAnimation;
   protected final Setting<Float> slideDistance;
   public ColorSetting obbyColor;
   public ColorSetting mixedColor;
   public ColorSetting bedrockColor;
   public ColorSetting voidColor;
   public ColorSetting trapColor;
   public ColorSetting terrainColor;
   public Setting<Integer> voidAlpha;
   protected List<Hole> holes;
   protected List<BlockPos> voidHoles;
   public ExecutorService executorService;
   private final ConcurrentHashMap<BlockPos, HoleAnimation> holeAnimations;
   private final ConcurrentHashMap<BlockPos, HoleAnimation> voidAnimations;
   private final List<AxisAlignedBB> obbyBloomBoxes;
   private final List<AxisAlignedBB> mixedBloomBoxes;
   private final List<AxisAlignedBB> bedrockBloomBoxes;
   private final List<AxisAlignedBB> trapBloomBoxes;
   private final List<AxisAlignedBB> terrainBloomBoxes;
   private final List<AxisAlignedBB> voidBloomBoxes;
   private final AtomicBoolean scanning;
   public Frustum frustum;

   public HoleESP() {
      super("HoleESP", "Highlights safe holes", Module.Category.RENDER, true, false, false);
      this.page = new Setting<Page>("Page", HoleESP.Page.General);
      this.range = new Setting<Integer>("Range", 10, 1, 50, (v) -> this.page.getValue() == HoleESP.Page.General);
      this.twoByOne = new Setting<Boolean>("2x1", true, (v) -> this.page.getValue() == HoleESP.Page.General);
      this.twoByTwo = new Setting<Boolean>("2x2", true, (v) -> this.page.getValue() == HoleESP.Page.General);
      this.incompleteDoubles = new Setting<Boolean>("Incomplete 2x1", true, (v) -> this.page.getValue() == HoleESP.Page.General && (Boolean)this.twoByOne.getValue());
      this.onlyVisible = new Setting<Boolean>("Only Visible", false, (v) -> this.page.getValue() == HoleESP.Page.General && (Boolean)this.twoByOne.getValue() && (Boolean)this.incompleteDoubles.getValue());
      this.dynamic = new Setting<Boolean>("AutoVoidHeight", true, (v) -> this.page.getValue() == HoleESP.Page.General);
      this.height = (new Setting<Float>("Height", 0.05F, -1.0F, 1.0F, (v) -> this.page.getValue() == HoleESP.Page.General)).setStep(0.05F);
      this.alpha = new Setting<Integer>("Box-Alpha", 21, 0, 255, (v) -> this.page.getValue() == HoleESP.Page.General);
      this.heightVoid = new Setting<Float>("Void-Height", 0.05F, -1.0F, 1.0F, (v) -> this.page.getValue() == HoleESP.Page.General && (Boolean)this.voids.getValue());
      this.fadeAlpha = new Setting<Boolean>("Fade Alpha", false, (v) -> this.page.getValue() == HoleESP.Page.General);
      this.glow = new Setting<Boolean>("Glow", false, (v) -> this.page.getValue() == HoleESP.Page.General);
      this.reverseGlow = new Setting<Boolean>("Reverse Glow", false, (v) -> this.page.getValue() == HoleESP.Page.General && (Boolean)this.glow.getValue());
      this.glowEdges = new Setting<Boolean>("Glow Edges", false, (v) -> this.page.getValue() == HoleESP.Page.General && (Boolean)this.glow.getValue());
      this.outlineMode = new Setting<Outline>("Outline Mode", HoleESP.Outline.Normal, (v) -> this.page.getValue() == HoleESP.Page.General);
      this.wireAlpha = new Setting<Integer>("Line-Alpha", 125, 0, 255, (v) -> this.page.getValue() == HoleESP.Page.General);
      this.lineWidth = new Setting<Float>("LineWidth", 1.0F, 0.1F, 4.0F, (v) -> this.page.getValue() == HoleESP.Page.General);
      this.voids = new Setting<Boolean>("Void Holes", true, (v) -> this.page.getValue() == HoleESP.Page.General);
      this.voidRange = new Setting<Integer>("Void Range", 16, 1, 50, (v) -> this.page.getValue() == HoleESP.Page.General && (Boolean)this.voids.getValue());
      this.traps = new Setting<Boolean>("Trapped Holes", false, (v) -> this.page.getValue() == HoleESP.Page.General);
      this.terrain = new Setting<Boolean>("Terrain Holes", false, (v) -> this.page.getValue() == HoleESP.Page.General);
      this.bloom = new Setting<Boolean>("Bloom", false, (v) -> this.page.getValue() == HoleESP.Page.General);
      this.bloomIntensity = new Setting<Float>("Bloom-Intensity", 0.8F, 0.1F, 1.0F, (v) -> this.page.getValue() == HoleESP.Page.General && (Boolean)this.bloom.getValue());
      this.bloomRadius = new Setting<Float>("Bloom-Radius", 12.0F, 2.0F, 25.0F, (v) -> this.page.getValue() == HoleESP.Page.General && (Boolean)this.bloom.getValue());
      this.animateIn = new Setting<Boolean>("Animate In", true, (v) -> this.page.getValue() == HoleESP.Page.Animation);
      this.animationSpeed = new Setting<Integer>("Animation-Speed", 300, 50, 1000, (v) -> this.page.getValue() == HoleESP.Page.Animation && (Boolean)this.animateIn.getValue());
      this.scaleAnimation = new Setting<Boolean>("Scale Animation", true, (v) -> this.page.getValue() == HoleESP.Page.Animation && (Boolean)this.animateIn.getValue());
      this.slideAnimation = new Setting<Boolean>("Slide Animation", true, (v) -> this.page.getValue() == HoleESP.Page.Animation && (Boolean)this.animateIn.getValue());
      this.slideDistance = new Setting<Float>("Slide Distance", 2.0F, 0.5F, 10.0F, (v) -> this.page.getValue() == HoleESP.Page.Animation && (Boolean)this.animateIn.getValue() && (Boolean)this.slideAnimation.getValue());
      this.obbyColor = new ColorSetting("ObbyColor", 16711680, false, (v) -> this.page.getValue() == HoleESP.Page.Colors);
      this.mixedColor = new ColorSetting("MixedColor", 16711680, false, (v) -> this.page.getValue() == HoleESP.Page.Colors);
      this.bedrockColor = new ColorSetting("BedrockColor", 65280, false, (v) -> this.page.getValue() == HoleESP.Page.Colors);
      this.voidColor = new ColorSetting("VoidColor", 13012727, false, (v) -> this.page.getValue() == HoleESP.Page.Colors && (Boolean)this.voids.getValue());
      this.trapColor = new ColorSetting("TrapColor", 16776960, false, (v) -> this.page.getValue() == HoleESP.Page.Colors && (Boolean)this.traps.getValue());
      this.terrainColor = new ColorSetting("TerrainColor", 9127187, false, (v) -> this.page.getValue() == HoleESP.Page.Colors && (Boolean)this.terrain.getValue());
      this.voidAlpha = new Setting<Integer>("Void-Alpha", 30, 0, 255, (v) -> this.page.getValue() == HoleESP.Page.Colors && (Boolean)this.voids.getValue());
      this.holeAnimations = new ConcurrentHashMap();
      this.voidAnimations = new ConcurrentHashMap();
      this.obbyBloomBoxes = new ArrayList();
      this.mixedBloomBoxes = new ArrayList();
      this.bedrockBloomBoxes = new ArrayList();
      this.trapBloomBoxes = new ArrayList();
      this.terrainBloomBoxes = new ArrayList();
      this.voidBloomBoxes = new ArrayList();
      this.scanning = new AtomicBoolean(false);
      this.frustum = new Frustum();
      this.register(this.page);
      this.register(this.range);
      this.register(this.twoByOne);
      this.register(this.twoByTwo);
      this.register(this.incompleteDoubles);
      this.register(this.onlyVisible);
      this.register(this.height);
      this.register(this.alpha);
      this.register(this.outlineMode);
      this.register(this.wireAlpha);
      this.register(this.lineWidth);
      this.register(this.fadeAlpha);
      this.register(this.glow);
      this.register(this.reverseGlow);
      this.register(this.glowEdges);
      this.register(this.bloom);
      this.register(this.bloomIntensity);
      this.register(this.bloomRadius);
      this.register(this.voids);
      this.register(this.voidRange);
      this.register(this.dynamic);
      this.register(this.heightVoid);
      this.register(this.traps);
      this.register(this.terrain);
      this.register(this.animateIn);
      this.register(this.animationSpeed);
      this.register(this.scaleAnimation);
      this.register(this.slideAnimation);
      this.register(this.slideDistance);
      this.register(this.obbyColor);
      this.register(this.mixedColor);
      this.register(this.bedrockColor);
      this.register(this.voidColor);
      this.register(this.voidAlpha);
      this.register(this.trapColor);
      this.register(this.terrainColor);
      this.holes = new ArrayList();
      this.voidHoles = new ArrayList();
      this.executorService = ThreadUtil.newBoundedExecutor("Vonware-HoleESP", 1, 1, 1);
   }

   public String getDisplayInfo() {
      return ((Outline)this.outlineMode.getValue()).name();
   }

   public void onDisable() {
      this.holeAnimations.clear();
      this.voidAnimations.clear();
      this.clearBloomBuckets();
      if (this.executorService != null) {
         this.executorService.shutdownNow();
      }

      this.executorService = ThreadUtil.newBoundedExecutor("Vonware-HoleESP", 1, 1, 1);
      this.scanning.set(false);
   }

   private Color getColorFromInt(int colorInt) {
      return new Color(colorInt >> 16 & 255, colorInt >> 8 & 255, colorInt & 255);
   }

   protected Color getBedrockColor() {
      return ColorPickerConfig.getState(this.bedrockColor).useGlobalColor ? Global.getGlobalColorRainbowS() : this.getColorFromInt((Integer)this.bedrockColor.getValue());
   }

   protected Color getObbyColor() {
      return ColorPickerConfig.getState(this.obbyColor).useGlobalColor ? Global.getGlobalColorRainbowS() : this.getColorFromInt((Integer)this.obbyColor.getValue());
   }

   protected Color getMixedColor() {
      return ColorPickerConfig.getState(this.mixedColor).useGlobalColor ? Global.getGlobalColorRainbowS() : this.getColorFromInt((Integer)this.mixedColor.getValue());
   }

   protected Color getTrapColor() {
      return ColorPickerConfig.getState(this.trapColor).useGlobalColor ? Global.getGlobalColorRainbowS() : this.getColorFromInt((Integer)this.trapColor.getValue());
   }

   protected Color getTerrainColor() {
      return ColorPickerConfig.getState(this.terrainColor).useGlobalColor ? Global.getGlobalColorRainbowS() : this.getColorFromInt((Integer)this.terrainColor.getValue());
   }

   protected Color getVoidColor() {
      return ColorPickerConfig.getState(this.voidColor).useGlobalColor ? Global.getGlobalColorRainbowS() : this.getColorFromInt((Integer)this.voidColor.getValue());
   }

   public void onUpdate() {
      if (mc.player != null && mc.world != null) {
         if (this.scanning.compareAndSet(false, true)) {
            if (this.executorService == null || this.executorService.isShutdown()) {
               this.executorService = ThreadUtil.newBoundedExecutor("Vonware-HoleESP", 1, 1, 1);
            }

            this.executorService.submit(() -> {
               try {
                  this.holes = HoleUtil.getHoles(mc.player, (float)(Integer)this.range.getValue(), (Boolean)this.twoByOne.getValue(), false, (Boolean)this.traps.getValue(), (Boolean)this.terrain.getValue(), (Boolean)this.incompleteDoubles.getValue(), (Boolean)this.twoByTwo.getValue());
                  if ((Boolean)this.voids.getValue() && mc.player.dimension != 1) {
                     this.voidHoles = this.getVoidHoles();
                  } else if (mc.player.dimension == 1) {
                     this.voidHoles = Collections.emptyList();
                  }
               } finally {
                  this.scanning.set(false);
               }

            });
         }
      }
   }

   private boolean isBlockedAbove(BlockPos pos) {
      if (mc.world == null) {
         return false;
      } else {
         return !BlockUtil.isAir(pos.up(1)) || !BlockUtil.isAir(pos.up(2));
      }
   }

   private void clearBloomBuckets() {
      this.obbyBloomBoxes.clear();
      this.mixedBloomBoxes.clear();
      this.bedrockBloomBoxes.clear();
      this.trapBloomBoxes.clear();
      this.terrainBloomBoxes.clear();
      this.voidBloomBoxes.clear();
   }

   private void addToBloomBucket(Hole hole, AxisAlignedBB bb) {
      if ((Boolean)this.bloom.getValue()) {
         switch (hole.getSafety()) {
            case OBBY:
               this.obbyBloomBoxes.add(bb);
               break;
            case MIXED:
               this.mixedBloomBoxes.add(bb);
               break;
            case BEDROCK:
               this.bedrockBloomBoxes.add(bb);
               break;
            case TRAPPED:
               this.trapBloomBoxes.add(bb);
               break;
            case TERRAIN:
               this.terrainBloomBoxes.add(bb);
         }

      }
   }

   public void onRender3D(Render3DEvent event) {
      if (mc.player != null && mc.world != null) {
         if ((Boolean)this.animateIn.getValue()) {
            this.cleanupOldAnimations();
         }

         this.clearBloomBuckets();
         if (!this.holes.isEmpty()) {
            for(Hole hole : this.holes) {
               if (hole != null) {
                  if (hole.getSecond() != null && !hole.is2x2() && (Boolean)this.incompleteDoubles.getValue() && (Boolean)this.onlyVisible.getValue()) {
                     boolean firstBlocked = this.isBlockedAbove(hole.getFirst());
                     boolean secondBlocked = this.isBlockedAbove(hole.getSecond());
                     if (firstBlocked || secondBlocked) {
                        if (firstBlocked && secondBlocked) {
                           continue;
                        }

                        if (!firstBlocked) {
                           this.renderSingleHoleWithAnimation(hole, hole.getFirst(), event);
                        }

                        if (!secondBlocked) {
                           this.renderSingleHoleWithAnimation(hole, hole.getSecond(), event);
                        }
                        continue;
                     }
                  }

                  if (this.isInFrustum(hole.getFirst())) {
                     HoleAnimation anim = null;
                     if ((Boolean)this.animateIn.getValue()) {
                        anim = (HoleAnimation)this.holeAnimations.computeIfAbsent(hole.getFirst(), (posx) -> new HoleAnimation(System.currentTimeMillis(), (long)(Integer)this.animationSpeed.getValue()));
                     }

                     Color color = this.getHoleColor(hole);
                     AxisAlignedBB bb = Interpolation.interpolatePos(hole.getFirst(), HoleUtil.isTrapHole(hole.getFirst()) ? 2.0F : (Float)this.height.getValue());
                     if (hole.is2x2()) {
                        bb = new AxisAlignedBB((double)hole.getFirst().getX() - mc.getRenderManager().viewerPosX, (double)hole.getFirst().getY() - mc.getRenderManager().viewerPosY, (double)hole.getFirst().getZ() - mc.getRenderManager().viewerPosZ, (double)(hole.getFourth().getX() + 1) - mc.getRenderManager().viewerPosX, (double)((float)hole.getFourth().getY() + (Float)this.height.getValue()) - mc.getRenderManager().viewerPosY, (double)(hole.getFourth().getZ() + 1) - mc.getRenderManager().viewerPosZ);
                     } else if (hole.getSecond() != null) {
                        bb = new AxisAlignedBB((double)hole.getFirst().getX() - mc.getRenderManager().viewerPosX, (double)hole.getFirst().getY() - mc.getRenderManager().viewerPosY, (double)hole.getFirst().getZ() - mc.getRenderManager().viewerPosZ, (double)(hole.getSecond().getX() + 1) - mc.getRenderManager().viewerPosX, (double)((float)hole.getSecond().getY() + (Float)this.height.getValue()) - mc.getRenderManager().viewerPosY, (double)(hole.getSecond().getZ() + 1) - mc.getRenderManager().viewerPosZ);
                     }

                     AxisAlignedBB renderedBB = this.renderHoleBox(bb, color, anim, hole.getFirst());
                     this.addToBloomBucket(hole, renderedBB);
                  }
               }
            }
         }

         if (!this.voidHoles.isEmpty() && (Boolean)this.voids.getValue() && mc.player.dimension != 1) {
            for(BlockPos pos : this.voidHoles) {
               if (!(mc.world.getWorldBorder().getClosestDistance(mc.player) <= (double)(Integer)this.voidRange.getValue()) && (this.isInFrustum(pos) || (Boolean)this.dynamic.getValue()) && !(mc.player.getPositionVector().y > (double)(Integer)this.voidRange.getValue())) {
                  HoleAnimation anim = null;
                  if ((Boolean)this.animateIn.getValue()) {
                     anim = (HoleAnimation)this.voidAnimations.computeIfAbsent(pos, (p) -> new HoleAnimation(System.currentTimeMillis(), (long)(Integer)this.animationSpeed.getValue()));
                  }

                  AxisAlignedBB bb2 = Interpolation.interpolatePos(pos, (Boolean)this.dynamic.getValue() ? this.getDynamicHeight(pos) : (Float)this.heightVoid.getValue());
                  int voidAlphaValue = (Integer)this.voidAlpha.getValue();
                  int wireAlphaValue = (Integer)this.wireAlpha.getValue();
                  if ((Boolean)this.animateIn.getValue() && anim != null) {
                     float progress = anim.getProgress();
                     voidAlphaValue = (int)((float)voidAlphaValue * progress);
                     wireAlphaValue = (int)((float)wireAlphaValue * progress);
                     if ((Boolean)this.scaleAnimation.getValue()) {
                        float scale = 0.3F + progress * 0.7F;
                        bb2 = this.scaleBox(bb2, scale);
                     }

                     if ((Boolean)this.slideAnimation.getValue()) {
                        float slideOffset = (1.0F - progress) * (Float)this.slideDistance.getValue();
                        bb2 = bb2.offset((double)0.0F, (double)slideOffset, (double)0.0F);
                     }
                  }

                  RenderMethods.startRender();
                  RenderMethods.drawBox(bb2, ColorUtil.changeAlpha(this.getVoidColor(), voidAlphaValue));
                  RenderMethods.drawOutline(bb2, (Float)this.lineWidth.getValue(), ColorUtil.changeAlpha(this.getVoidColor(), wireAlphaValue));
                  RenderMethods.endRender();
                  if ((Boolean)this.bloom.getValue()) {
                     this.voidBloomBoxes.add(bb2);
                  }
               }
            }
         }

         if ((Boolean)this.bloom.getValue()) {
            this.renderBloomPass();
         }

      }
   }

   private void renderSingleHoleWithAnimation(Hole hole, BlockPos pos, Render3DEvent event) {
      if (this.isInFrustum(pos)) {
         HoleAnimation anim = null;
         if ((Boolean)this.animateIn.getValue()) {
            anim = (HoleAnimation)this.holeAnimations.computeIfAbsent(pos, (p) -> new HoleAnimation(System.currentTimeMillis(), (long)(Integer)this.animationSpeed.getValue()));
         }

         Color color = this.getHoleColor(hole);
         AxisAlignedBB bb = Interpolation.interpolatePos(pos, HoleUtil.isTrapHole(pos) ? 2.0F : (Float)this.height.getValue());
         AxisAlignedBB renderedBB = this.renderHoleBox(bb, color, anim, pos);
         this.addToBloomBucket(hole, renderedBB);
      }
   }

   private Color getHoleColor(Hole hole) {
      switch (hole.getSafety()) {
         case OBBY:
            return this.getObbyColor();
         case MIXED:
            return this.getMixedColor();
         case BEDROCK:
            return this.getBedrockColor();
         case TRAPPED:
            return this.getTrapColor();
         case TERRAIN:
            return this.getTerrainColor();
         default:
            return new Color(-1);
      }
   }

   private AxisAlignedBB renderHoleBox(AxisAlignedBB bb, Color color, HoleAnimation anim, BlockPos pos) {
      int finalAlpha = (Integer)this.alpha.getValue();
      int lineAlpha = (Integer)this.wireAlpha.getValue();
      if ((Boolean)this.fadeAlpha.getValue()) {
         double distance = mc.player.getDistanceSq((double)pos.getX() + (double)0.5F, (double)pos.getY(), (double)pos.getZ() + (double)0.5F);
         double maxDistSq = MathUtil.square((double)((float)(Integer)this.range.getValue()));
         double fadeRatio = Math.max((double)0.0F, Math.min((double)1.0F, (maxDistSq - distance) / maxDistSq));
         finalAlpha = MathUtil.clamp((int)((double)(Integer)this.alpha.getValue() * fadeRatio), 0, 255);
         lineAlpha = MathUtil.clamp((int)((double)(Integer)this.wireAlpha.getValue() * fadeRatio), 0, 255);
      }

      if (finalAlpha <= 0 && lineAlpha <= 0) {
         return bb;
      } else {
         if ((Boolean)this.animateIn.getValue() && anim != null) {
            float progress = anim.getProgress();
            finalAlpha = (int)((float)finalAlpha * progress);
            lineAlpha = (int)((float)lineAlpha * progress);
            if ((Boolean)this.scaleAnimation.getValue()) {
               float scale = 0.3F + progress * 0.7F;
               bb = this.scaleBox(bb, scale);
            }

            if ((Boolean)this.slideAnimation.getValue()) {
               float slideOffset = (1.0F - progress) * (Float)this.slideDistance.getValue();
               bb = bb.offset((double)0.0F, (double)slideOffset, (double)0.0F);
            }
         }

         if ((Boolean)this.glow.getValue()) {
            RenderUtil.prepare();
            Color colorBottom = (Boolean)this.reverseGlow.getValue() ? ColorUtil.changeAlpha(color, 0) : ColorUtil.changeAlpha(color, finalAlpha);
            Color colorTop = (Boolean)this.reverseGlow.getValue() ? ColorUtil.changeAlpha(color, finalAlpha) : ColorUtil.changeAlpha(color, 0);
            RenderUtil.renderBB(7, bb, colorBottom, colorTop);
            AxisAlignedBB outlineBB = (Boolean)this.glowEdges.getValue() ? bb : ((Boolean)this.reverseGlow.getValue() ? new AxisAlignedBB(bb.minX, bb.maxY - (double)0.01F, bb.minZ, bb.maxX, bb.maxY, bb.maxZ) : new AxisAlignedBB(bb.minX, bb.minY, bb.minZ, bb.maxX, bb.minY + (double)0.01F, bb.maxZ));
            GlStateManager.glLineWidth((Float)this.lineWidth.getValue());
            Color lineColorBottom = (Boolean)this.reverseGlow.getValue() ? ColorUtil.changeAlpha(color, 0) : ColorUtil.changeAlpha(color, lineAlpha);
            Color lineColorTop = (Boolean)this.reverseGlow.getValue() ? ColorUtil.changeAlpha(color, lineAlpha) : ColorUtil.changeAlpha(color, 0);
            RenderUtil.renderBB(3, outlineBB, lineColorBottom, lineColorTop);
            RenderUtil.release();
         } else {
            RenderMethods.startRender();
            RenderMethods.drawBox(bb, ColorUtil.changeAlpha(color, finalAlpha));
            this.drawOutline(bb, (Float)this.lineWidth.getValue(), ColorUtil.changeAlpha(color, lineAlpha));
            RenderMethods.endRender();
         }

         return bb;
      }
   }

   private void renderBloomPass() {
      Shader shader = Shader.INSTANCE;
      shader.setRainbowEnabled(false);
      shader.setGradientEnabled(false);
      shader.setBloomEnabled(true);
      shader.setBloomIntensity((Float)this.bloomIntensity.getValue());
      shader.setBloomRadius((Float)this.bloomRadius.getValue());
      shader.setBloomQuality(2);
      this.renderBloomGroup(shader, this.obbyBloomBoxes, this.getObbyColor());
      this.renderBloomGroup(shader, this.mixedBloomBoxes, this.getMixedColor());
      this.renderBloomGroup(shader, this.bedrockBloomBoxes, this.getBedrockColor());
      this.renderBloomGroup(shader, this.trapBloomBoxes, this.getTrapColor());
      this.renderBloomGroup(shader, this.terrainBloomBoxes, this.getTerrainColor());
      this.renderBloomGroup(shader, this.voidBloomBoxes, this.getVoidColor());
   }

   private void renderBloomGroup(Shader shader, List<AxisAlignedBB> boxes, Color color) {
      if (!boxes.isEmpty()) {
         GlStateManager.matrixMode(5889);
         GlStateManager.pushMatrix();
         GlStateManager.matrixMode(5888);
         GlStateManager.pushMatrix();
         shader.startDraw(mc.getRenderPartialTicks());
         GlStateManager.disableTexture2D();
         RenderMethods.startRender();

         for(AxisAlignedBB box : boxes) {
            RenderMethods.drawBox(box, new Color(255, 255, 255, 255));
         }

         RenderMethods.endRender();
         GlStateManager.enableTexture2D();
         Color insideColor = ColorUtil.changeAlpha(color, (Integer)this.alpha.getValue());
         Color outlineColor = ColorUtil.changeAlpha(color, (Integer)this.wireAlpha.getValue());
         shader.stopDraw(insideColor, outlineColor, (Float)this.lineWidth.getValue(), 1.0F, 0, 0, false);
         GL20.glUseProgram(0);
         mc.getFramebuffer().bindFramebuffer(true);
         GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit + 2);
         GlStateManager.bindTexture(0);
         GlStateManager.matrixMode(5890);
         GlStateManager.loadIdentity();
         GlStateManager.matrixMode(5888);
         GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
         GlStateManager.bindTexture(0);
         GlStateManager.matrixMode(5890);
         GlStateManager.loadIdentity();
         GlStateManager.matrixMode(5888);
         GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
         GlStateManager.bindTexture(0);
         GlStateManager.matrixMode(5890);
         GlStateManager.loadIdentity();
         GlStateManager.matrixMode(5888);
         GlStateManager.matrixMode(5889);
         GlStateManager.popMatrix();
         GlStateManager.matrixMode(5888);
         GlStateManager.popMatrix();
         GlStateManager.depthMask(true);
         GlStateManager.enableDepth();
         GlStateManager.depthFunc(515);
         GlStateManager.enableAlpha();
         GlStateManager.alphaFunc(516, 0.1F);
         GlStateManager.enableBlend();
         GlStateManager.tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ZERO);
         GlStateManager.enableTexture2D();
         GlStateManager.disableLighting();
         GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
         GlStateManager.resetColor();
      }
   }

   private void cleanupOldAnimations() {
      Set<BlockPos> currentHoles = new HashSet();

      for(Hole hole : this.holes) {
         if (hole != null) {
            currentHoles.add(hole.getFirst());
            if (hole.getSecond() != null) {
               currentHoles.add(hole.getSecond());
            }
         }
      }

      this.holeAnimations.keySet().removeIf((pos) -> !currentHoles.contains(pos));
      if ((Boolean)this.voids.getValue()) {
         Set<BlockPos> currentVoids = new HashSet(this.voidHoles);
         this.voidAnimations.keySet().removeIf((pos) -> !currentVoids.contains(pos));
      }

   }

   private AxisAlignedBB scaleBox(AxisAlignedBB bb, float scale) {
      double centerX = (bb.minX + bb.maxX) / (double)2.0F;
      double centerY = (bb.minY + bb.maxY) / (double)2.0F;
      double centerZ = (bb.minZ + bb.maxZ) / (double)2.0F;
      double halfWidth = (bb.maxX - bb.minX) / (double)2.0F * (double)scale;
      double halfHeight = (bb.maxY - bb.minY) / (double)2.0F * (double)scale;
      double halfDepth = (bb.maxZ - bb.minZ) / (double)2.0F * (double)scale;
      return new AxisAlignedBB(centerX - halfWidth, centerY - halfHeight, centerZ - halfDepth, centerX + halfWidth, centerY + halfHeight, centerZ + halfDepth);
   }

   private float getDynamicHeight(BlockPos pos) {
      float height;
      for(height = 1.0F; mc.world.getBlockState(pos.up((int)height)).getBlock() instanceof BlockAir && height < 5.0F; ++height) {
      }

      return height;
   }

   private boolean isInFrustum(BlockPos pos) {
      Entity entity = mc.getRenderViewEntity();
      if (entity != null) {
         this.frustum.setPosition(entity.posX, entity.posY, entity.posZ);
         return this.frustum.isBoundingBoxInFrustum(new AxisAlignedBB(pos));
      } else {
         return false;
      }
   }

   protected List<BlockPos> getVoidHoles() {
      BlockPos playerPos = PositionUtil.getPosition(RenderMethods.getEntity());
      return (List)BlockUtil.getCircle(playerPos.add(0, -playerPos.getY(), 0), (float)(Integer)this.voidRange.getValue()).stream().filter(this::isVoid).collect(Collectors.toList());
   }

   private boolean isVoid(BlockPos pos) {
      return (mc.world.getBlockState(pos).getBlock() == Blocks.AIR || mc.world.getBlockState(pos).getBlock() != Blocks.BEDROCK) && pos.getY() < 1 && pos.getY() >= 0;
   }

   protected void drawOutline(AxisAlignedBB bb, float with, Color color) {
      switch ((Outline)this.outlineMode.getValue()) {
         case Normal:
            RenderMethods.drawOutline(bb, with, color);
            break;
         case Cross:
            RenderMethods.drawCross(bb, with, color);
            if ((double)(Float)this.height.getValue() != (double)0.0F) {
               RenderMethods.drawOutline(bb, with, color);
            }
      }

   }

   public static enum Outline {
      Normal,
      Cross;
   }

   public static enum Page {
      General,
      Colors,
      Animation;
   }

   private static class HoleAnimation {
      private final long startTime;
      private final long duration;

      public HoleAnimation(long startTime, long duration) {
         this.startTime = startTime;
         this.duration = duration;
      }

      public float getProgress() {
         long elapsed = System.currentTimeMillis() - this.startTime;
         if (elapsed >= this.duration) {
            return 1.0F;
         } else {
            float t = (float)elapsed / (float)this.duration;
            return 1.0F - (float)Math.pow((double)(1.0F - t), (double)3.0F);
         }
      }
   }
}
