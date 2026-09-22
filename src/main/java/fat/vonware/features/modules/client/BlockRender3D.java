package fat.vonware.features.modules.client;

import fat.vonware.features.modules.Module;
import fat.vonware.features.setting.ColorSetting;
import fat.vonware.features.setting.Setting;
import fat.vonware.util.Animations.render.ColorUtil;
import fat.vonware.util.Animations.render.RenderUtil;
import java.awt.Color;
import java.util.List;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.MinecraftForge;

public class BlockRender3D extends Module {
   private static BlockRender3D INSTANCE;
   private final Setting<RenderType> renderType;
   private final ColorSetting color;
   private final Setting<Integer> opacity;
   private final Setting<Integer> outlineAlpha;
   private final Setting<Integer> shrinkTime;
   private final Setting<Integer> fadeTime;
   public final Setting<Float> outlineWidth;

   public BlockRender3D() {
      super("BlockRender", "How block placement modules render", Module.Category.CLIENT, true, false, false);
      this.renderType = new Setting<RenderType>("RenderType", BlockRender3D.RenderType.Solid, "How block placements should render");
      this.color = new ColorSetting("Color", 9157119, true);
      this.opacity = new Setting<Integer>("Opacity", 85, 0, 255);
      this.outlineAlpha = new Setting<Integer>("OutlineAlpha", 255, 0, 255);
      this.shrinkTime = new Setting<Integer>("Shrink-Time", 150, 0, 2000);
      this.fadeTime = new Setting<Integer>("Fade-Time", 500, 0, 2000);
      this.outlineWidth = new Setting<Float>("LineWidth", 1.0F, 0.1F, 3.0F);
      this.register(this.renderType);
      this.register(this.color);
      this.register(this.opacity);
      this.register(this.outlineAlpha);
      this.register(this.shrinkTime);
      this.register(this.fadeTime);
      this.register(this.outlineWidth);
      this.enabled.setValue(true);
      MinecraftForge.EVENT_BUS.register(this);
      INSTANCE = this;
   }

   public static BlockRender3D getInstance() {
      if (INSTANCE == null) {
         INSTANCE = new BlockRender3D();
      }

      return INSTANCE;
   }

   public RenderType getRenderType() {
      return this.renderType.getValue();
   }

   public int getShrinkTime() {
      return (Integer)this.shrinkTime.getValue();
   }

   public int getFadeTime() {
      return (Integer)this.fadeTime.getValue();
   }

   public int getSolidAlpha() {
      return (Integer)this.opacity.getValue();
   }

   public int getOutlineAlpha() {
      return (Integer)this.outlineAlpha.getValue();
   }

   public static Color getColor() {
      return ColorUtil.getResolvedColor(getInstance().color);
   }

   public static float getShrinkProgress(float linearProgress) {
      float progress = Math.max(0.0F, Math.min(1.0F, linearProgress));
      return progress * progress * (3.0F - 2.0F * progress);
   }

   public static AxisAlignedBB getShrinkBB(BlockPos pos, float linearProgress) {
      double inset = (double)0.5F * (double)getShrinkProgress(linearProgress);
      return new AxisAlignedBB((double)pos.getX() + inset, (double)pos.getY() + inset, (double)pos.getZ() + inset, (double)pos.getX() + (double)1.0F - inset, (double)pos.getY() + (double)1.0F - inset, (double)pos.getZ() + (double)1.0F - inset);
   }

   public static <T extends RenderBlock> void renderWithShrink(List<T> renderBlocks, int shrinkTimeMs) {
      Color color = getColor();
      int outlineAlpha = getInstance().getOutlineAlpha();
      renderBlocks.removeIf((rb) -> {
         try {
            float progress = rb.getProgress(shrinkTimeMs);
            if (progress >= 1.0F) {
               return true;
            } else {
               AxisAlignedBB bb = getShrinkBB(rb.getPos(), progress);
               float alphaProgress = 1.0F - getShrinkProgress(progress);
               int fillAlpha = Math.max(0, Math.round((float)getInstance().getSolidAlpha() * alphaProgress));
               int currentOutlineAlpha = Math.max(0, Math.round((float)outlineAlpha * alphaProgress));
               RenderUtil.drawBBFill(bb, color, fillAlpha);
               RenderUtil.drawBBBox(bb, color, currentOutlineAlpha);
               return false;
            }
         } catch (Throwable t) {
            t.printStackTrace();
            return true;
         }
      });
   }

   public static <T extends RenderBlock> void renderWithFade(List<T> renderBlocks, int fadeTimeMs, int startAlpha) {
      Color color = getColor();
      int maxOutlineAlpha = getInstance().getOutlineAlpha();
      renderBlocks.removeIf((rb) -> {
         try {
            float progress = rb.getProgress(fadeTimeMs);
            if (progress >= 1.0F) {
               return true;
            } else {
               int currentAlpha = (int)((float)startAlpha * (1.0F - progress));
               int outlineAlpha = Math.min(maxOutlineAlpha, currentAlpha + 55);
               AxisAlignedBB bb = rb.getBB();
               RenderUtil.drawBBFill(bb, color, currentAlpha);
               RenderUtil.drawBBBox(bb, color, outlineAlpha);
               return false;
            }
         } catch (Throwable t) {
            t.printStackTrace();
            return true;
         }
      });
   }

   public static <T extends RenderBlock> void render(List<T> renderBlocks) {
      BlockRender3D instance = getInstance();
      if (instance.getRenderType() == BlockRender3D.RenderType.Shrink) {
         renderWithShrink(renderBlocks, instance.getShrinkTime());
      } else {
         renderWithFade(renderBlocks, instance.getFadeTime(), instance.getSolidAlpha());
      }

   }

   public interface RenderBlock {
      BlockPos getPos();

      long getStartTime();

      AxisAlignedBB getBB();

      default float getProgress(int timeMs) {
         long elapsed = System.currentTimeMillis() - this.getStartTime();
         return timeMs <= 0 ? 1.0F : Math.min(1.0F, (float)elapsed / (float)timeMs);
      }
   }

   public static class SimpleRenderBlock implements RenderBlock {
      private final BlockPos pos;
      private final long startTime;
      private final AxisAlignedBB bb;

      public SimpleRenderBlock(BlockPos pos) {
         this.pos = pos;
         this.startTime = System.currentTimeMillis();
         this.bb = new AxisAlignedBB(pos);
      }

      public BlockPos getPos() {
         return this.pos;
      }

      public long getStartTime() {
         return this.startTime;
      }

      public AxisAlignedBB getBB() {
         return this.bb;
      }
   }

   public static enum RenderType {
      Shrink,
      Solid;
   }
}
