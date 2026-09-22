package fat.vonware.features.gui.newgui.newcomponents.newbuttons;

import fat.vonware.Global;
import fat.vonware.Vonware;
import fat.vonware.features.gui.components.items.Item;
import fat.vonware.features.gui.newgui.newcomponents.NewComponent;
import fat.vonware.features.gui.newgui.newcomponents.NewVonwareGui;
import fat.vonware.features.modules.client.ClickGui;
import fat.vonware.features.modules.client.Colors;
import fat.vonware.features.modules.client.HudEditor;
import fat.vonware.features.setting.Bind;
import fat.vonware.util.Animations.AnimationUtil;
import fat.vonware.util.Animations.render.ColorUtil;
import fat.vonware.util.Animations.render.RenderUtil;
import fat.vonware.util.OtherRenderShit.gui.SoundUtil;
import java.awt.Color;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.GlStateManager.DestFactor;
import net.minecraft.client.renderer.GlStateManager.SourceFactor;
import org.lwjgl.opengl.GL11;

public class NewButton extends Item {
   static long glowStartTime = System.currentTimeMillis();
   private static final float GLOW_WIDTH = 2.5F;
   private static final float SWEEP_SPEED = 0.002F;
   static final float SIDE_INSET = 2.5F;
   private boolean wasHovering = false;
   final AnimationUtil.DeltaAnimation stateAnimation = new AnimationUtil.DeltaAnimation(0.0F, 100.0F);
   protected final AnimationUtil.DeltaAnimation hoverAnimation = new AnimationUtil.DeltaAnimation(0.0F, 50.0F);
   private final AnimationUtil.DeltaAnimation textNudgeAnimation = new AnimationUtil.DeltaAnimation(0.0F, 150.0F);
   private static final float TEXT_NUDGE_X = 1.6F;
   private static final float TEXT_NUDGE_Y = 0.35F;
   private static final String[] CHAR_CACHE = new String[128];
   protected final int[] baseColor = new int[3];
   protected boolean baseColorValid = false;
   private static final float BIND_LABEL_SCALE = 0.6F;

   protected final void updateHoverSound(boolean hovering) {
      if (hovering && !this.wasHovering) {
         SoundUtil.playButtonHover();
      }

      this.wasHovering = hovering;
   }

   static String cachedChar(char c) {
      return c < 128 ? CHAR_CACHE[c] : String.valueOf(c);
   }

   static int argb(int a, int r, int g, int b) {
      return (a & 255) << 24 | (r & 255) << 16 | (g & 255) << 8 | b & 255;
   }

   public NewButton(String name) {
      super(name);
      this.height = 13;
   }

   static void resolveBaseColor(int[] out) {
      Colors colors = Colors.getInstance();
      ClickGui clickGui = ClickGui.getInstance();
      Color c;
      if ((Boolean)colors.rainbow.getValue()) {
         c = colors.rainbowMode.getValue() == Colors.RainbowMode.Static ? ColorUtil.rainbow(0) : ColorUtil.rainbow((int)((float)NewComponent.counter1[0] * (Float)colors.rainbowHue.getValue()));
      } else if ((Boolean)clickGui.pulseGui.getValue()) {
         c = Global.getPulseColor(colors.getGuiColor(), 0);
      } else {
         c = colors.getCurrentColor();
      }

      out[0] = c.getRed();
      out[1] = c.getGreen();
      out[2] = c.getBlue();
   }

   public void drawScreen(int mouseX, int mouseY, float partialTicks) {
      ClickGui clickGui = ClickGui.getInstance();
      boolean hovering = this.isHovering(mouseX, mouseY);
      boolean enabled = this.getState();
      this.updateHoverSound(hovering);
      float hoverRaw;
      if (hovering) {
         this.hoverAnimation.reset(true);
         hoverRaw = 1.0F;
      } else {
         hoverRaw = this.hoverAnimation.tick(false);
      }

      float hoverAnim = AnimationUtil.easeOutCubic(hoverRaw);
      float textHoverRaw = this.textNudgeAnimation.tick(hovering);
      float easedNudge = textHoverRaw * textHoverRaw * (3.0F - 2.0F * textHoverRaw);
      float textOffsetX = easedNudge * 1.6F;
      float textOffsetY = easedNudge * 0.35F;
      if (!this.baseColorValid) {
         resolveBaseColor(this.baseColor);
      }

      this.baseColorValid = false;
      int bR = this.baseColor[0];
      int bG = this.baseColor[1];
      int bB = this.baseColor[2];
      float stateAnim = this.stateAnimation.tickEased(enabled);
      RenderUtil.drawRect(this.x - 2.5F, this.y, this.x + (float)this.width + 2.5F, this.y + (float)this.height, argb(18, bR, bG, bB));
      if (stateAnim > 0.0F) {
         int fillAlpha = (Boolean)clickGui.buttonFill.getValue() ? 85 : 20;
         float fillLeft = this.x - 2.5F;
         float fillRight = fillLeft + ((float)this.width + 5.0F) * stateAnim;
         RenderUtil.drawRect(fillLeft, this.y, fillRight, this.y + (float)this.height, argb(fillAlpha, bR, bG, bB));
      }

      if (hoverAnim > 0.0F) {
         RenderUtil.drawRect(this.x - 2.5F, this.y, this.x + (float)this.width + 2.5F, this.y + (float)this.height, argb((int)(45.0F * hoverAnim), bR, bG, bB));
      }

      GL11.glPushMatrix();
      GlStateManager.enableTexture2D();
      GlStateManager.enableBlend();
      GlStateManager.tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ZERO);
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      if (this instanceof ModuleComponent) {
         this.drawModuleText((ModuleComponent)this, stateAnim, bR, bG, bB, textOffsetX, textOffsetY);
      } else {
         int lc = (int)(170.0F + 85.0F * stateAnim);
         Vonware.textManager.getCustomFont().drawStringWithShadow(this.getName(), (double)((float)Math.round(this.x + 2.0F) + textOffsetX), (double)((float)Math.round(this.y + 3.0F) + textOffsetY), argb(255, lc, lc, lc));
      }

      GlStateManager.disableBlend();
      GL11.glPopMatrix();
   }

   private void drawModuleText(ModuleComponent mb, float stateAnim, int bR, int bG, int bB, float textOffsetX, float textOffsetY) {
      ClickGui clickGui = ClickGui.getInstance();
      Colors colors = Colors.getInstance();
      String displayName = mb.getModule().getDisplayName();
      float nameX = (float)Math.round(this.x + 2.0F) + textOffsetX;
      float nameY = (float)Math.round(this.y + 3.0F) + textOffsetY;
      if (mb.getModule().isEnabled()) {
         if (clickGui.enabledStyle.getValue() == ClickGui.EnabledStyle.Gradient) {
            long t = System.currentTimeMillis();
            float elapsed = (float)(t - glowStartTime) * 0.002F;
            float glowPos = (float)Math.sin((double)elapsed) * 0.5F + 0.5F;
            float glowCenter = glowPos * (float)displayName.length();
            int[] glow = new int[3];
            if ((Boolean)colors.rainbow.getValue()) {
               Color rc = colors.rainbowMode.getValue() == Colors.RainbowMode.Static ? ColorUtil.rainbow(0) : ColorUtil.rainbow((int)((float)NewComponent.counter1[0] * (Float)colors.rainbowHue.getValue()));
               glow[0] = rc.getRed();
               glow[1] = rc.getGreen();
               glow[2] = rc.getBlue();
            } else if ((Boolean)clickGui.pulseGui.getValue()) {
               Color pc = Global.getPulseColor(colors.getGuiColor(), 0);
               glow[0] = pc.getRed();
               glow[1] = pc.getGreen();
               glow[2] = pc.getBlue();
            } else {
               Color gc = colors.getGuiColor();
               glow[0] = gc.getRed();
               glow[1] = gc.getGreen();
               glow[2] = gc.getBlue();
            }

            float curX = nameX;

            for(int i = 0; i < displayName.length(); ++i) {
               char c = displayName.charAt(i);
               float dist = Math.abs((float)i - glowCenter);
               int cc;
               if (dist > 2.5F) {
                  cc = -1;
               } else {
                  float nd = dist / 2.5F;
                  float gs = (float)Math.cos((double)nd * Math.PI / (double)2.0F);
                  float ig = 1.0F - gs;
                  cc = argb(255, (int)((float)glow[0] + (float)(255 - glow[0]) * ig), (int)((float)glow[1] + (float)(255 - glow[1]) * ig), (int)((float)glow[2] + (float)(255 - glow[2]) * ig));
               }

               String cs = cachedChar(c);
               GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
               Vonware.textManager.getCustomFont().drawStringWithShadow(cs, (double)curX, (double)nameY, cc);
               curX += (float)Vonware.textManager.getCustomFont().getStringWidth(cs);
            }

            this.drawBindLabel(mb, curX, nameY);
         } else {
            int textColor = -1;
            if (clickGui.enabledStyle.getValue() == ClickGui.EnabledStyle.Colored) {
               textColor = ColorUtil.lighter(new Color(bR, bG, bB), 0.35).getRGB();
            }

            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            Vonware.textManager.getCustomFont().drawStringWithShadow(displayName, (double)nameX, (double)nameY, textColor);
            float nameW = (float)Vonware.textManager.getCustomFont().getStringWidth(displayName);
            this.drawBindLabel(mb, nameX + nameW, nameY);
         }
      } else {
         int lc = (int)(170.0F + 85.0F * stateAnim);
         GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
         Vonware.textManager.getCustomFont().drawStringWithShadow(displayName, (double)nameX, (double)nameY, argb(255, lc, lc, lc));
         float nameW = (float)Vonware.textManager.getCustomFont().getStringWidth(displayName);
         this.drawBindLabel(mb, nameX + nameW, nameY);
      }
   }

   private void drawBindLabel(ModuleComponent mb, float afterX, float nameY) {
      if ((Boolean)ClickGui.getInstance().binds.getValue()) {
         Bind moduleBind = mb.getModule().getBind();
         if (moduleBind != null && !moduleBind.isEmpty()) {
            String bindText = "[" + moduleBind.toString() + "]";
            float inv = 1.6666666F;
            float fullHeight = (float)Vonware.textManager.getCustomFont().getStringHeight(bindText);
            float vCenterOffset = (fullHeight - fullHeight * 0.6F) / 2.0F;
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GL11.glPushMatrix();
            GL11.glScalef(0.6F, 0.6F, 1.0F);
            Vonware.textManager.getCustomFont().drawStringWithShadow(bindText, (double)((afterX + 3.0F) * inv), (double)((nameY + vCenterOffset) * inv), argb(150, 200, 200, 200));
            GL11.glPopMatrix();
         }
      }
   }

   public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
      if (mouseButton == 0 && this.isHovering(mouseX, mouseY)) {
         this.onMouseClick();
      }

   }

   public void onMouseClick() {
      boolean hudEditorClick = this instanceof ModuleComponent && ((ModuleComponent)this).getModule() instanceof HudEditor;
      this.toggle();
      if (hudEditorClick) {
         SoundUtil.playHudEditorClick();
      } else {
         SoundUtil.playModuleToggle(this.getState());
      }

   }

   public void toggle() {
   }

   public boolean getState() {
      return false;
   }

   public int getHeight() {
      return 13;
   }

   public boolean isHovering(int mouseX, int mouseY) {
      for(NewComponent c : NewVonwareGui.getClickGui().getComponents()) {
         if (c.drag) {
            return false;
         }
      }

      return (float)mouseX >= this.x && (float)mouseX <= this.x + (float)this.width && (float)mouseY >= this.y && (float)mouseY <= this.y + (float)this.height;
   }

   static {
      for(int i = 0; i < 128; ++i) {
         CHAR_CACHE[i] = String.valueOf((char)i);
      }

   }
}
