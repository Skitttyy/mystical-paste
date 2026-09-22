package fat.vonware.manager;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.GlStateManager.DestFactor;
import net.minecraft.client.renderer.GlStateManager.SourceFactor;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class ScreenTransitionManager {
   private boolean active = false;
   private long startTime = 0L;
   private static final long FADE_MS = 700L;

   public ScreenTransitionManager() {
      MinecraftForge.EVENT_BUS.register(this);
   }

   public void beginFadeIn() {
      this.active = true;
      this.startTime = System.currentTimeMillis();
   }

   @SubscribeEvent
   public void onGuiDrawPost(GuiScreenEvent.DrawScreenEvent.Post event) {
      if (this.active) {
         float t = (float)(System.currentTimeMillis() - this.startTime) / 700.0F;
         float alpha = Math.max(0.0F, 1.0F - this.easeInCubic(Math.min(t, 1.0F)));
         if (alpha <= 0.005F) {
            this.active = false;
         } else {
            Minecraft mc = Minecraft.getMinecraft();
            ScaledResolution sr = new ScaledResolution(mc);
            int sw = sr.getScaledWidth();
            int sh = sr.getScaledHeight();
            GlStateManager.pushMatrix();
            GlStateManager.enableBlend();
            GlStateManager.disableTexture2D();
            GlStateManager.disableAlpha();
            GlStateManager.tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ZERO);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            Tessellator tess = Tessellator.getInstance();
            BufferBuilder buf = tess.getBuffer();
            int a = (int)(alpha * 255.0F);
            float r = 0.0F;
            float g = 0.0F;
            float b = 0.0F;
            buf.begin(7, DefaultVertexFormats.POSITION_COLOR);
            buf.pos((double)0.0F, (double)sh, (double)0.0F).color(r, g, b, (float)a / 255.0F).endVertex();
            buf.pos((double)sw, (double)sh, (double)0.0F).color(r, g, b, (float)a / 255.0F).endVertex();
            buf.pos((double)sw, (double)0.0F, (double)0.0F).color(r, g, b, (float)a / 255.0F).endVertex();
            buf.pos((double)0.0F, (double)0.0F, (double)0.0F).color(r, g, b, (float)a / 255.0F).endVertex();
            tess.draw();
            GlStateManager.enableAlpha();
            GlStateManager.enableTexture2D();
            GlStateManager.disableBlend();
            GlStateManager.popMatrix();
         }
      }
   }

   private float easeInCubic(float t) {
      return t * t * t;
   }
}
