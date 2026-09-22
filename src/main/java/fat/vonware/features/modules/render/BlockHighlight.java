package fat.vonware.features.modules.render;

import fat.vonware.event.events.Render3DEvent;
import fat.vonware.features.modules.Module;
import fat.vonware.features.setting.ColorSetting;
import fat.vonware.features.setting.Setting;
import fat.vonware.util.Animations.render.ColorUtil;
import fat.vonware.util.Animations.render.RenderMethods;
import fat.vonware.util.BlockHighlight.SlideUtil;
import java.awt.Color;
import java.util.function.Function;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.RayTraceResult.Type;

public class BlockHighlight extends Module {
   private final Setting<RenderMode> renderMode;
   private final Setting<Boolean> fade;
   private final Setting<Boolean> slide;
   private final Setting<Float> animationTime;
   private final Setting<Boolean> depthMask;
   private final ColorSetting customColor;
   private final Setting<Integer> alpha;
   private final Setting<Integer> boxAlpha;
   private final Setting<Float> lineWidth;
   BlockPos lastPos;
   BlockPos oldPos;
   SlideUtil slideAnim;
   SlideUtil fadeAnim;
   AxisAlignedBB renderBB;
   AxisAlignedBB oldBB;
   long fadeStartTime;

   public BlockHighlight() {
      super("BlockHighlight", "Highlights the block you look at", Module.Category.RENDER, true, false, false);
      this.renderMode = (new Setting("Mode", BlockHighlight.RenderMode.Outline)).withDescription((Function<RenderMode, String>)((mode) -> {
         switch (mode) {
            case Outline:
               return "Outline: Draws only an outline around the block under your crosshair.";
            case Box:
               return "Box: Draws only the filled block highlight.";
            case Both:
               return "Both: Draws both the filled box and its outline.";
            default:
               return "";
         }
      }));
      this.fade = new Setting<Boolean>("Fade", false, "Uses a fade animation between highlighted blocks.");
      this.slide = new Setting<Boolean>("Slide", false, "Uses a slide animation between highlighted blocks.");
      this.animationTime = new Setting<Float>("Animation-Time", 1.0F, 0.1F, 10.0F, (v) -> (Boolean)this.slide.getValue(), "Controls the animation-time timing for slide.");
      this.depthMask = new Setting<Boolean>("DepthMask", false, "Toggles DepthMask.");
      this.customColor = new ColorSetting("HighlightColor", 16777215, false, "");
      this.alpha = new Setting<Integer>("Alpha", 255, 0, 255, "Controls the opacity used by BlockHighlight for Alpha.");
      this.boxAlpha = new Setting<Integer>("Box-Alpha", 15, 0, 255, (v) -> this.renderMode.getValue() == BlockHighlight.RenderMode.Box || this.renderMode.getValue() == BlockHighlight.RenderMode.Both, "Controls the opacity used by BlockHighlight for Box-Alpha.");
      this.lineWidth = new Setting<Float>("LineWidth", 0.1F, 0.1F, 5.0F, (v) -> this.renderMode.getValue() == BlockHighlight.RenderMode.Outline || this.renderMode.getValue() == BlockHighlight.RenderMode.Both, "Controls the rendered line thickness.");
      this.slideAnim = new SlideUtil((long)((Float)this.animationTime.getValue() * 1000.0F));
      this.fadeAnim = new SlideUtil(300L);
      this.fadeStartTime = 0L;
      this.register(this.renderMode);
      this.register(this.fade);
      this.register(this.slide);
      this.register(this.animationTime);
      this.register(this.depthMask);
      this.register(this.customColor);
      this.register(this.alpha);
      this.register(this.boxAlpha);
      this.register(this.lineWidth);
   }

   private Color getCustomColor() {
      return ColorUtil.getResolvedColor(this.customColor);
   }

   private Color getOutlineColor() {
      Color custom = this.getCustomColor();
      return new Color(custom.getRed(), custom.getGreen(), custom.getBlue(), (Integer)this.alpha.getValue());
   }

   private Color getBoxColor() {
      Color custom = this.getCustomColor();
      return new Color(custom.getRed(), custom.getGreen(), custom.getBlue(), (Integer)this.boxAlpha.getValue());
   }

   private Color getFadedOutlineColor(float fadeProgress) {
      Color base = this.getOutlineColor();
      int fadedAlpha = (int)((float)base.getAlpha() * (1.0F - fadeProgress));
      return new Color(base.getRed(), base.getGreen(), base.getBlue(), Math.max(0, fadedAlpha));
   }

   private Color getFadedBoxColor(float fadeProgress) {
      Color base = this.getBoxColor();
      int fadedAlpha = (int)((float)base.getAlpha() * (1.0F - fadeProgress));
      return new Color(base.getRed(), base.getGreen(), base.getBlue(), Math.max(0, fadedAlpha));
   }

   private boolean isFadeFinished() {
      if (this.fadeStartTime == 0L) {
         return true;
      } else {
         long elapsed = System.currentTimeMillis() - this.fadeStartTime;
         return elapsed >= 300L;
      }
   }

   public void onRender3D(Render3DEvent event) {
      RayTraceResult ray = mc.objectMouseOver;
      if (ray != null && ray.typeOfHit == Type.BLOCK) {
         BlockPos pos = ray.getBlockPos();
         if (this.lastPos == null) {
            this.lastPos = pos;
            this.renderBB = mc.world.getBlockState(pos).getSelectedBoundingBox(mc.world, pos).grow(0.002);
         }

         if (!this.lastPos.equals(pos)) {
            if ((Boolean)this.fade.getValue()) {
               this.oldPos = this.lastPos;
               this.oldBB = this.renderBB;
               this.fadeAnim = new SlideUtil(300L);
               this.fadeStartTime = System.currentTimeMillis();
            }

            this.lastPos = pos;
            if ((Boolean)this.slide.getValue()) {
               this.slideAnim = new SlideUtil((long)((Float)this.animationTime.getValue() * 1000.0F));
            }
         }

         AxisAlignedBB bb2 = mc.world.getBlockState(pos).getSelectedBoundingBox(mc.world, pos).grow(0.002);
         if ((Boolean)this.slide.getValue()) {
            this.renderBB = new AxisAlignedBB(this.renderBB.minX - (this.renderBB.minX - bb2.minX) * this.slideAnim.easeOutQuad(), this.renderBB.minY - (this.renderBB.minY - bb2.minY) * this.slideAnim.easeOutQuad(), this.renderBB.minZ - (this.renderBB.minZ - bb2.minZ) * this.slideAnim.easeOutQuad(), this.renderBB.maxX - (this.renderBB.maxX - bb2.maxX) * this.slideAnim.easeOutQuad(), this.renderBB.maxY - (this.renderBB.maxY - bb2.maxY) * this.slideAnim.easeOutQuad(), this.renderBB.maxZ - (this.renderBB.maxZ - bb2.maxZ) * this.slideAnim.easeOutQuad());
         } else {
            this.renderBB = bb2;
         }

         if ((Boolean)this.fade.getValue() && this.oldBB != null && !this.isFadeFinished()) {
            float fadeProgress = (float)this.fadeAnim.easeOutQuad();
            Color fadedOutlineColor = this.getFadedOutlineColor(fadeProgress);
            Color fadedBoxColor = this.getFadedBoxColor(fadeProgress);
            boolean renderOutline = this.renderMode.getValue() == BlockHighlight.RenderMode.Outline || this.renderMode.getValue() == BlockHighlight.RenderMode.Both;
            boolean renderBox = this.renderMode.getValue() == BlockHighlight.RenderMode.Box || this.renderMode.getValue() == BlockHighlight.RenderMode.Both;
            RenderMethods.drawBlockHighlightAnimation(this.oldBB, fadedOutlineColor, false, fadedBoxColor, (Float)this.lineWidth.getValue(), renderOutline, renderBox, fadedBoxColor.getAlpha(), (Boolean)this.depthMask.getValue());
         }

         Color outlineColor = this.getOutlineColor();
         Color boxColor = this.getBoxColor();
         boolean renderOutline = this.renderMode.getValue() == BlockHighlight.RenderMode.Outline || this.renderMode.getValue() == BlockHighlight.RenderMode.Both;
         boolean renderBox = this.renderMode.getValue() == BlockHighlight.RenderMode.Box || this.renderMode.getValue() == BlockHighlight.RenderMode.Both;
         RenderMethods.drawBlockHighlightAnimation(this.renderBB, outlineColor, false, boxColor, (Float)this.lineWidth.getValue(), renderOutline, renderBox, (Integer)this.boxAlpha.getValue(), (Boolean)this.depthMask.getValue());
      }

   }

   public static enum RenderMode {
      Outline,
      Box,
      Both;
   }
}
