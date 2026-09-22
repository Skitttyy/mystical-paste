package fat.vonware.features.gui.newgui.newcomponents.newbuttons;

import com.mojang.realmsclient.gui.ChatFormatting;
import fat.vonware.Vonware;
import fat.vonware.features.gui.newgui.newcomponents.NewComponent;
import fat.vonware.features.gui.newgui.newcomponents.NewVonwareGui;
import fat.vonware.features.setting.Setting;
import fat.vonware.util.Animations.AnimationUtil;
import fat.vonware.util.Animations.render.RenderUtil;
import fat.vonware.util.OtherRenderShit.gui.SoundUtil;
import java.nio.IntBuffer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.GlStateManager.DestFactor;
import net.minecraft.client.renderer.GlStateManager.SourceFactor;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.BufferUtils;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

public class NewEnumButton extends NewButton {
   public Setting setting;
   private boolean isOpen = false;
   private float openAnim = 0.0F;
   private long animStart = 0L;
   private static final long ANIM_MS = 180L;
   private static final float ROW_HEIGHT = 12.0F;
   private static final float PAD = 2.0F;
   private static final ResourceLocation ARROW_ICON = new ResourceLocation("vonware", "textures/gui/enumarrow.png");
   private float bobOffset = 0.0F;
   private float bobTarget = 0.0F;
   private long lastBobUpdate = 0L;
   private String currentValueText = "";
   private boolean mouseHeld = false;
   private static final IntBuffer SCISSOR_BUF = BufferUtils.createIntBuffer(16);

   public NewEnumButton(Setting setting) {
      super(setting.getName());
      this.setting = setting;
      this.width = 15;
      this.height = 13;
      this.currentValueText = setting.getValue().toString();
   }

   private float easeOut(float t) {
      float t1 = t - 1.0F;
      return t1 * t1 * t1 + 1.0F;
   }

   private float tickAnim() {
      if (this.animStart == 0L) {
         return this.isOpen ? 1.0F : 0.0F;
      } else {
         long elapsed = System.currentTimeMillis() - this.animStart;
         float raw = Math.max(0.0F, Math.min(1.0F, (float)elapsed / 180.0F));
         float eased = this.easeOut(raw);
         this.openAnim = this.isOpen ? eased : 1.0F - eased;
         if (raw >= 1.0F) {
            this.animStart = 0L;
         }

         return this.openAnim;
      }
   }

   private Enum<?>[] getEnumValues() {
      Object v = this.setting.getValue();
      return v instanceof Enum ? (Enum[])((Enum[])v.getClass().getEnumConstants()) : new Enum[0];
   }

   private int currentIndex() {
      Enum<?>[] vals = this.getEnumValues();
      Object cur = this.setting.getValue();

      for(int i = 0; i < vals.length; ++i) {
         if (vals[i] == cur) {
            return i;
         }
      }

      return 0;
   }

   private float rightEdge() {
      return this.x + (float)this.width + 8.0F;
   }

   private float dropY() {
      return this.y + (float)this.height;
   }

   private float fullDropHeight() {
      return (float)this.getEnumValues().length * 12.0F + 4.0F;
   }

   public void drawScreen(int mouseX, int mouseY, float partialTicks) {
      float anim = this.tickAnim();
      boolean hovering = this.isHovering(mouseX, mouseY);
      this.updateHoverSound(hovering);
      float hoverRaw;
      if (hovering) {
         this.hoverAnimation.reset(true);
         hoverRaw = 1.0F;
      } else {
         hoverRaw = this.hoverAnimation.tick(false);
      }

      float hoverF = AnimationUtil.easeOutCubic(hoverRaw);
      int[] base = new int[3];
      resolveBaseColor(base);
      int bR = base[0];
      int bG = base[1];
      int bB = base[2];
      int bgAlpha = this.isOpen ? 18 : 8;
      RenderUtil.drawRect(this.x, this.y, this.rightEdge(), this.y + (float)this.height - 0.5F, argb(bgAlpha, bR, bG, bB));
      if (hoverF > 0.0F) {
         RenderUtil.drawRect(this.x, this.y, this.rightEdge(), this.y + (float)this.height - 0.5F, argb((int)(35.0F * hoverF), bR, bG, bB));
      }

      if (hovering || this.isOpen) {
         RenderUtil.drawGradientRect(this.x, this.y, this.rightEdge(), this.y + 1.5F, argb(18, 255, 255, 255), argb(0, 255, 255, 255));
      }

      float textX = this.x + 3.5F;
      float textY = this.y + 3.0F;
      String prefix = ChatFormatting.WHITE + this.setting.getName() + " " + ChatFormatting.GRAY;
      Vonware.textManager.getCustomFont().drawStringWithShadow(prefix + this.currentValueText, (double)textX, (double)textY, -3355444);
      long now = System.currentTimeMillis();
      float dtSec = Math.min((float)(now - this.lastBobUpdate) / 1000.0F, 0.1F);
      this.lastBobUpdate = now;
      this.bobTarget = hovering ? (float)(Math.sin((double)now / (double)280.0F) * (double)1.5F) : 0.0F;
      this.bobOffset += (this.bobTarget - this.bobOffset) * Math.min(1.0F, dtSec * 14.0F);
      float iconSize = 7.0F;
      String fullText = ChatFormatting.WHITE + this.setting.getName() + " " + ChatFormatting.GRAY + this.currentValueText;
      float icX = textX + (float)Vonware.textManager.getCustomFont().getStringWidth(fullText) + 5.0F;
      float icY = this.y + (float)this.height / 2.0F - iconSize / 2.0F;
      float rotation = this.openAnim * 90.0F;
      mc.getTextureManager().bindTexture(ARROW_ICON);
      Tessellator tess = Tessellator.getInstance();
      BufferBuilder buffer = tess.getBuffer();
      GL11.glPushMatrix();
      GL11.glTranslatef(icX + iconSize / 2.0F, icY + iconSize / 2.0F + this.bobOffset, 0.0F);
      GL11.glRotatef(rotation, 0.0F, 0.0F, 1.0F);
      GlStateManager.enableBlend();
      GlStateManager.tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ZERO);
      GlStateManager.enableTexture2D();
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      float h = iconSize / 2.0F;
      buffer.begin(7, DefaultVertexFormats.POSITION_TEX);
      buffer.pos((double)(-h), (double)(-h), (double)0.0F).tex((double)0.0F, (double)0.0F).endVertex();
      buffer.pos((double)(-h), (double)h, (double)0.0F).tex((double)0.0F, (double)1.0F).endVertex();
      buffer.pos((double)h, (double)h, (double)0.0F).tex((double)1.0F, (double)1.0F).endVertex();
      buffer.pos((double)h, (double)(-h), (double)0.0F).tex((double)1.0F, (double)0.0F).endVertex();
      tess.draw();
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      GL11.glPopMatrix();
      GlStateManager.disableBlend();
      if (!(anim <= 0.001F)) {
         Enum<?>[] values = this.getEnumValues();
         int curIdx = this.currentIndex();
         float fullH = this.fullDropHeight();
         float visH = fullH * anim;
         float pLeft = this.x;
         float pTop = this.dropY();
         float pRight = this.rightEdge();
         this.enableScissor(pLeft, pTop, pRight, pTop + visH);
         RenderUtil.drawRect(pLeft, pTop + visH - 0.75F, pRight, pTop + visH, argb(100, bR, bG, bB));

         for(int i = 0; i < values.length; ++i) {
            float rowY = pTop + 2.0F + (float)i * 12.0F;
            boolean rowHov = this.isRowHovered(mouseX, mouseY, i);
            boolean isCur = i == curIdx;
            if (isCur) {
               RenderUtil.drawRect(pLeft, rowY, pRight, rowY + 12.0F, argb(14, bR, bG, bB));
               RenderUtil.drawRect(pLeft, rowY + 1.5F, pLeft + 1.5F, rowY + 12.0F - 1.5F, argb(200, bR, bG, bB));
            } else if (rowHov) {
               RenderUtil.drawRect(pLeft, rowY, pRight, rowY + 12.0F, argb(10, 255, 255, 255));
            }

            if (i < values.length - 1) {
               RenderUtil.drawRect(pLeft + 4.0F, rowY + 12.0F - 0.25F, pRight - 4.0F, rowY + 12.0F, argb(15, 255, 255, 255));
            }

            int textColor;
            if (isCur) {
               textColor = -16777216 | bR << 16 | bG << 8 | bB;
            } else if (rowHov) {
               textColor = -3355444;
            } else {
               textColor = -11184794;
            }

            Vonware.textManager.getCustomFont().drawStringWithShadow(values[i].toString(), (double)(pLeft + 5.5F), (double)(rowY + 2.5F), textColor);
         }

         this.disableScissor();
      }
   }

   public boolean isHovering(int mouseX, int mouseY) {
      for(NewComponent c : NewVonwareGui.getClickGui().getComponents()) {
         if (c.drag) {
            return false;
         }
      }

      return (float)mouseX >= this.x && (float)mouseX <= this.rightEdge() && (float)mouseY >= this.y && (float)mouseY <= this.y + (float)this.height;
   }

   private boolean isRowHovered(int mouseX, int mouseY, int rowIndex) {
      float rowY = this.dropY() + 2.0F + (float)rowIndex * 12.0F;
      return (float)mouseX >= this.x && (float)mouseX <= this.rightEdge() && (float)mouseY >= rowY && (float)mouseY < rowY + 12.0F;
   }

   public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
      if (mouseButton == 1 && this.isHovering(mouseX, mouseY)) {
         this.toggle();
      } else {
         if (mouseButton == 0 && this.isOpen && !this.mouseHeld) {
            Enum<?>[] values = this.getEnumValues();

            for(int i = 0; i < values.length; ++i) {
               if (this.isRowHovered(mouseX, mouseY, i)) {
                  this.selectIndex(i);
                  this.closeDropdown();
                  this.mouseHeld = true;
                  return;
               }
            }
         }

      }
   }

   private void selectIndex(int index) {
      int delta = index - this.currentIndex();
      if (delta != 0) {
         for(int i = 0; i < Math.abs(delta); ++i) {
            this.setting.changeEnumValue(delta > 0 ? 1 : -1);
         }

         this.currentValueText = this.setting.getValue().toString();
         SoundUtil.playModuleEnable();
      }
   }

   private void openDropdown() {
      if (!this.isOpen) {
         this.isOpen = true;
         this.animStart = System.currentTimeMillis();
         SoundUtil.playEnumOpen();
      }

   }

   private void closeDropdown() {
      if (this.isOpen) {
         this.isOpen = false;
         this.animStart = System.currentTimeMillis();
         SoundUtil.playEnumClose();
      }

   }

   public void toggle() {
      if (this.isOpen) {
         this.closeDropdown();
      } else {
         this.openDropdown();
      }

   }

   public int getHeight() {
      return (int)(13.0F + this.fullDropHeight() * this.openAnim);
   }

   public void update() {
      this.setHidden(!this.setting.isVisible());
      if (!Mouse.isButtonDown(0)) {
         this.mouseHeld = false;
      }

   }

   public boolean getState() {
      return this.isOpen;
   }

   private void enableScissor(float l, float t, float r, float b) {
      ScaledResolution sr = new ScaledResolution(mc);
      int f = sr.getScaleFactor();
      int sh = mc.displayHeight;
      float appScale = NewVonwareGui.getAppliedScale();
      GL11.glPushAttrib(524288);
      int sx = (int)(l * (float)f * appScale);
      int sy = (int)((float)sh - b * (float)f * appScale);
      int sw = (int)((r - l) * (float)f * appScale);
      int sH = (int)((b - t) * (float)f * appScale);
      if (GL11.glIsEnabled(3089)) {
         SCISSOR_BUF.clear();
         GL11.glGetInteger(3088, SCISSOR_BUF);
         int px = SCISSOR_BUF.get(0);
         int py = SCISSOR_BUF.get(1);
         int pw = SCISSOR_BUF.get(2);
         int ph = SCISSOR_BUF.get(3);
         int ix = Math.max(px, sx);
         int iy = Math.max(py, sy);
         int iw = Math.max(0, Math.min(px + pw, sx + sw) - ix);
         int ih = Math.max(0, Math.min(py + ph, sy + sH) - iy);
         GL11.glScissor(ix, iy, iw, ih);
      } else {
         GL11.glScissor(sx, sy, sw, sH);
      }

      GL11.glEnable(3089);
   }

   private void disableScissor() {
      GL11.glPopAttrib();
   }
}
