package fat.vonware.mixin.mixins;

import fat.vonware.Vonware;
import fat.vonware.features.modules.client.Fonts;
import fat.vonware.features.modules.client.MainMenu;
import fat.vonware.features.modules.client.Manager;
import fat.vonware.manager.ModuleManager;
import fat.vonware.util.Animations.render.Render2DMethods;
import fat.vonware.util.BlockPlaceModules.WelcomeScreen;
import fat.vonware.util.Chat.ThreadUtil;
import fat.vonware.util.OtherRenderShit.gui.SoundUtil;
import fat.vonware.util.shader.menu.MenuGLSLShader;
import java.awt.Color;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiLabel;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({GuiMainMenu.class})
public abstract class MixinGuiMainMenu extends GuiScreen {
   @Unique
   private boolean showUsername = false;
   @Unique
   private long initTime;
   @Unique
   private long lastFrameTime = 0L;
   @Unique
   private static boolean welcomeShownThisSession = false;
   @Unique
   private static boolean startupMusicStarted = false;
   @Unique
   private static boolean startupMusicPlayed = false;
   @Unique
   private int musicPathX;
   @Unique
   private int musicPathY;
   @Unique
   private int musicPathW;
   @Unique
   private int musicPathH;
   @Unique
   private MainMenu getMainMenuModule() {
      ModuleManager moduleManager = Vonware.moduleManager;
      return moduleManager == null ? null : moduleManager.getModuleByClass(MainMenu.class);
   }

   @Shadow
   protected abstract void renderSkybox(int var1, int var2, float var3);

   @Inject(
      method = {"initGui"},
      at = {@At("RETURN")}
   )
   public void initGuiHook(CallbackInfo info) {
      this.initTime = System.currentTimeMillis();
      if (!welcomeShownThisSession) {
         welcomeShownThisSession = true;
         this.mc.displayGuiScreen(new WelcomeScreen());
      }

      MainMenu mainMenuModule = this.getMainMenuModule();
      if (!startupMusicStarted && mainMenuModule != null && Boolean.TRUE.equals(mainMenuModule.startupMusic.getValue())) {
         startupMusicStarted = true;
         long delay = startupMusicPlayed ? 0L : 5000L;
         ThreadUtil.startDaemonDelayed("Startup-Music-Delay", delay, () -> this.mc.addScheduledTask(SoundUtil::playStartupMusic));
         startupMusicPlayed = true;
      }

   }

   @Inject(
      method = {"onGuiClosed"},
      at = {@At("HEAD")}
   )
   public void onGuiClosedHook(CallbackInfo ci) {
      MainMenu mainMenuModule = this.getMainMenuModule();
      if (mainMenuModule != null && Boolean.TRUE.equals(mainMenuModule.startupMusic.getValue())) {
         SoundUtil.stopStartupMusic();
      }

      startupMusicStarted = false;
   }

   @Inject(
      method = {"drawScreen"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void onDrawScreen(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
      MainMenu mainMenuModule = this.getMainMenuModule();
      if (mainMenuModule != null && mainMenuModule.isEnabled()) {
         if (Boolean.TRUE.equals(mainMenuModule.customButtons.getValue())) {
            ci.cancel();
            int limit = (Integer)mainMenuModule.maxFps.getValue();
            long frameTimeNs = 1000000000L / (long)limit;
            long now = System.nanoTime();
            if (now - this.lastFrameTime < frameTimeNs) {
               ci.cancel();
            } else {
               this.lastFrameTime = now;
               switch ((MainMenu.Background)mainMenuModule.background.getValue()) {
                  case Image:
                     String imagePath = this.getImagePath(mainMenuModule.backgroundImage.getValue());
                     this.mc.getTextureManager().bindTexture(new ResourceLocation(imagePath));
                     drawModalRectWithCustomSizedTexture(0, 0, 0.0F, 0.0F, this.width, this.height, (float)this.width, (float)this.height);
                     break;
                  case Color:
                     int c1 = (Integer)mainMenuModule.colorOne.getValue();
                     int c2 = (Integer)mainMenuModule.colorTwo.getValue();
                     int colorOne = (new Color(c1 >> 16 & 255, c1 >> 8 & 255, c1 & 255, 255)).getRGB();
                     int colorTwo = (new Color(c2 >> 16 & 255, c2 >> 8 & 255, c2 & 255, 255)).getRGB();
                     Render2DMethods.drawGradientRect(-1.0F, -1.0F, (float)this.width, (float)this.height, false, colorOne, colorTwo);
                     break;
                  case Shader:
                     drawRect(0, 0, this.width, this.height, -16777216);
                     MenuGLSLShader shader = mainMenuModule.getActiveShader();
                     if (shader != null) {
                        GlStateManager.disableCull();
                        GlStateManager.disableTexture2D();
                        shader.useShader(this.width * 2, this.height * 2, (float)(mouseX * 2), (float)(mouseY * 2), (float)(System.currentTimeMillis() - this.initTime) / 1000.0F);
                        GL11.glBegin(7);
                        GL11.glVertex2f(-1.0F, -1.0F);
                        GL11.glVertex2f(-1.0F, 1.0F);
                        GL11.glVertex2f(1.0F, 1.0F);
                        GL11.glVertex2f(1.0F, -1.0F);
                        GL11.glEnd();
                        GL20.glUseProgram(0);
                        GlStateManager.enableTexture2D();
                        GlStateManager.enableCull();
                     }
               }

               for(GuiButton button : this.buttonList) {
                  boolean isHovered = mouseX >= button.x && mouseX <= button.x + button.width && mouseY >= button.y && mouseY <= button.y + button.height;
                  this.drawStylizedButton(button.displayString, button.x + button.width / 2, button.y, button.width, button.height, isHovered, 255);
               }

               for(GuiLabel label : this.labelList) {
                  label.drawLabel(this.mc, mouseX, mouseY);
               }

               if ((Boolean)Manager.getInstance().accountPos.getValue()) {
                  String username = this.mc.getSession().getUsername();
                  String displayText = username;
                  if (!this.showUsername) {
                     StringBuilder asterisks = new StringBuilder();

                     for(int i = 0; i < username.length(); ++i) {
                        asterisks.append("*");
                     }

                     displayText = asterisks.toString();
                  }

                  String text = "Logged in as " + displayText;
                  int textWidth = this.mc.fontRenderer.getStringWidth(text);
                  int j = this.height / 4 + 48;
                  float textX = (float)this.width / 2.0F - (float)textWidth / 2.0F - 2.0F;
                  float textY = (float)(j + 72 + Manager.getInstance().getAccountMessagePosY());
                  this.mc.fontRenderer.drawString(String.format("%s%s", Manager.getInstance().getAccountTextColor(), text), textX, textY, -1, true);
                  if (!this.showUsername && (float)mouseX >= textX && (float)mouseX <= textX + (float)textWidth && (float)mouseY >= textY && (float)mouseY <= textY + (float)this.mc.fontRenderer.FONT_HEIGHT) {
                     this.drawHoveringText(Collections.singletonList("Click to reveal username"), mouseX, mouseY);
                  }
               }

            }
         }
      }
   }

   @Inject(
      method = {"drawScreen"},
      at = {@At("HEAD")}
   )
   public void drawScreenBackground(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
      MainMenu mainMenuModule = this.getMainMenuModule();
      if (mainMenuModule != null && mainMenuModule.isEnabled()) {
         if (!Boolean.TRUE.equals(mainMenuModule.customButtons.getValue())) {
            switch ((MainMenu.Background)mainMenuModule.background.getValue()) {
               case Image:
                  String imagePath = this.getImagePath(mainMenuModule.backgroundImage.getValue());
                  this.mc.getTextureManager().bindTexture(new ResourceLocation(imagePath));
                  drawModalRectWithCustomSizedTexture(0, 0, 0.0F, 0.0F, this.width, this.height, (float)this.width, (float)this.height);
                  break;
               case Color:
                  int c1 = (Integer)mainMenuModule.colorOne.getValue();
                  int c2 = (Integer)mainMenuModule.colorTwo.getValue();
                  int colorOne = (new Color(c1 >> 16 & 255, c1 >> 8 & 255, c1 & 255, 255)).getRGB();
                  int colorTwo = (new Color(c2 >> 16 & 255, c2 >> 8 & 255, c2 & 255, 255)).getRGB();
                  Render2DMethods.drawGradientRect(-1.0F, -1.0F, (float)this.width, (float)this.height, false, colorOne, colorTwo);
                  break;
               case Shader:
                  drawRect(0, 0, this.width, this.height, -16777216);
                  MenuGLSLShader shader = mainMenuModule.getActiveShader();
                  if (shader != null) {
                     GlStateManager.disableCull();
                     GlStateManager.disableTexture2D();
                     shader.useShader(this.width * 2, this.height * 2, (float)(mouseX * 2), (float)(mouseY * 2), (float)(System.currentTimeMillis() - this.initTime) / 1000.0F);
                     GL11.glBegin(7);
                     GL11.glVertex2f(-1.0F, -1.0F);
                     GL11.glVertex2f(-1.0F, 1.0F);
                     GL11.glVertex2f(1.0F, 1.0F);
                     GL11.glVertex2f(1.0F, -1.0F);
                     GL11.glEnd();
                     GL20.glUseProgram(0);
                     GlStateManager.enableTexture2D();
                     GlStateManager.enableCull();
                  }
            }

         }
      }
   }

   private String getImagePath(MainMenu.BackgroundImage image) {
      switch (image) {
         case Image2:
            return "textures/mommy1.jpg";
         default:
            return "textures/background1.png";
      }
   }

   @Redirect(
      method = {"drawScreen"},
      at = @At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/gui/GuiMainMenu;renderSkybox(IIF)V"
)
   )
   private void drawScreenHook1(GuiMainMenu guiMainMenu, int mouseX, int mouseY, float partialTicks) {
      MainMenu mainMenuModule = this.getMainMenuModule();
      if (mainMenuModule == null || !mainMenuModule.isEnabled()) {
         this.renderSkybox(mouseX, mouseY, partialTicks);
      }

   }

   @Redirect(
      method = {"drawScreen"},
      at = @At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/gui/GuiMainMenu;drawGradientRect(IIIIII)V",
   ordinal = 0
)
   )
   private void drawScreenHook2(GuiMainMenu guiMainMenu, int left, int top, int right, int bottom, int startColor, int endColor) {
      MainMenu mainMenuModule = this.getMainMenuModule();
      if (mainMenuModule == null || !mainMenuModule.isEnabled()) {
         this.drawGradientRect(left, top, right, bottom, startColor, endColor);
      }

   }

   @Redirect(
      method = {"drawScreen"},
      at = @At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/gui/GuiMainMenu;drawGradientRect(IIIIII)V",
   ordinal = 1
)
   )
   private void drawScreenHook3(GuiMainMenu guiMainMenu, int left, int top, int right, int bottom, int startColor, int endColor) {
      MainMenu mainMenuModule = this.getMainMenuModule();
      if (mainMenuModule == null || !mainMenuModule.isEnabled()) {
         this.drawGradientRect(left, top, right, bottom, startColor, endColor);
      }

   }

   @Inject(
      method = {"drawScreen"},
      at = {@At("RETURN")}
   )
   public void drawScreenHook0(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
      MainMenu mainMenuModule = this.getMainMenuModule();
      if (mainMenuModule == null || !mainMenuModule.isEnabled() || !Boolean.TRUE.equals(mainMenuModule.customButtons.getValue())) {
         if (mainMenuModule != null && mainMenuModule.isEnabled() && Boolean.TRUE.equals(mainMenuModule.startupMusic.getValue()) && !SoundUtil.hasCustomStartupMusic()) {
            String line1 = "To have custom music the file must be named startup_music.ogg (Game restart may be required.)";
            String line2 = SoundUtil.getMusicFolderPath();
            String line3 = "^ Click here to open the music file path! ^";
            int pad = 6;
            int lineH = this.mc.fontRenderer.FONT_HEIGHT + 2;
            int boxW = Math.max(Math.max(this.mc.fontRenderer.getStringWidth(line1), this.mc.fontRenderer.getStringWidth(line2)), this.mc.fontRenderer.getStringWidth(line3)) + pad * 2;
            int boxH = lineH * 3 + pad * 2;
            int boxX = this.width / 2 - boxW / 2;
            int boxY = this.height - boxH - 40;
            drawRect(boxX, boxY, boxX + boxW, boxY + boxH, -1442840576);
            drawRect(boxX, boxY, boxX + boxW, boxY + 1, -1437230593);
            int textX = boxX + pad;
            int textY = boxY + pad;
            this.mc.fontRenderer.drawStringWithShadow(line1, (float)textX, (float)textY, -1);
            this.musicPathX = textX;
            this.musicPathY = textY + lineH;
            this.musicPathW = this.mc.fontRenderer.getStringWidth(line2);
            this.musicPathH = this.mc.fontRenderer.FONT_HEIGHT;
            boolean pathHovered = mouseX >= this.musicPathX && mouseX <= this.musicPathX + this.musicPathW && mouseY >= this.musicPathY && mouseY <= this.musicPathY + this.musicPathH;
            this.mc.fontRenderer.drawStringWithShadow(line2, (float)textX, (float)(textY + lineH), pathHovered ? -3351041 : -7816193);
            this.mc.fontRenderer.drawStringWithShadow(line3, (float)textX, (float)(textY + lineH * 2), -1);
         }

         if ((Boolean)Manager.getInstance().accountPos.getValue()) {
            String username = this.mc.getSession().getUsername();
            String displayText = username;
            if (!this.showUsername) {
               StringBuilder asterisks = new StringBuilder();

               for(int i = 0; i < username.length(); ++i) {
                  asterisks.append("*");
               }

               displayText = asterisks.toString();
            }

            String text = "Logged in as " + displayText;
            int textWidth = this.mc.fontRenderer.getStringWidth(text);
            int j = this.height / 4 + 48;
            float textX = (float)this.width / 2.0F - (float)textWidth / 2.0F - 2.0F;
            float textY = (float)(j + 72 + Manager.getInstance().getAccountMessagePosY());
            this.mc.fontRenderer.drawString(String.format("%s%s", Manager.getInstance().getAccountTextColor(), text), textX, textY, -1, true);
            if (!this.showUsername && (float)mouseX >= textX && (float)mouseX <= textX + (float)textWidth && (float)mouseY >= textY && (float)mouseY <= textY + (float)this.mc.fontRenderer.FONT_HEIGHT) {
               this.drawHoveringText(Collections.singletonList("Click to reveal username"), mouseX, mouseY);
            }

         }
      }
   }

   private void drawStylizedButton(String text, int centerX, int y, int width, int height, boolean hovered, int alpha) {
      int x = centerX - width / 2;
      float scale = hovered ? 1.02F : 1.0F;
      GlStateManager.pushMatrix();
      GlStateManager.translate((float)centerX, (float)(y + height / 2), 0.0F);
      GlStateManager.scale(scale, scale, 1.0F);
      GlStateManager.translate((float)(-centerX), (float)(-(y + height / 2)), 0.0F);
      int halfWidth = width / 2;
      float shimmerPos = 0.0F;
      if (hovered) {
         long time = System.currentTimeMillis();
         shimmerPos = (float)Math.sin((double)time / (double)500.0F);
      }

      for(int i = 0; i < halfWidth; ++i) {
         float fade = (float)i / (float)halfWidth;
         if (hovered) {
            float normalizedPos = (float)i / (float)halfWidth * 2.0F - 1.0F;
            float distanceFromShimmer = Math.abs(normalizedPos - shimmerPos);
            float shimmer = Math.max(0.0F, 1.0F - distanceFromShimmer * 3.0F);
            fade = Math.min(1.0F, fade + shimmer * 0.4F);
         }

         int whiteAlpha = (int)(fade * (float)alpha);
         whiteAlpha = Math.min(255, whiteAlpha);
         int gradientColor = whiteAlpha << 24 | 16777215;
         drawRect(x + i, y, x + i + 1, y + 1, gradientColor);
         drawRect(x + width - i - 1, y, x + width - i, y + 1, gradientColor);
      }

      boolean customFontEnabled = Fonts.getInstance().isEnabled();
      int textColor = alpha << 24 | 16777215;
      int textWidth = customFontEnabled ? Vonware.textManager.getStringWidth(text) : this.mc.fontRenderer.getStringWidth(text);
      float textX = (float)centerX - (float)textWidth / 2.0F;
      float textY = (float)y + (float)(height - 8) / 2.0F;
      if (hovered && customFontEnabled) {
         for(int i = 5; i > 0; --i) {
            float offset = (float)i * 0.6F;
            int glowAlpha = alpha * 60 / i / 255;
            int textGlowColor = glowAlpha << 24 | 16777215;
            Vonware.textManager.drawStringWithShadow(text, textX - offset, textY, textGlowColor);
            Vonware.textManager.drawStringWithShadow(text, textX + offset, textY, textGlowColor);
            Vonware.textManager.drawStringWithShadow(text, textX, textY - offset, textGlowColor);
            Vonware.textManager.drawStringWithShadow(text, textX, textY + offset, textGlowColor);
         }
      }

      if (customFontEnabled) {
         Vonware.textManager.drawStringWithShadow(text, textX, textY, textColor);
      } else {
         this.mc.fontRenderer.drawStringWithShadow(text, textX, textY, textColor);
      }

      GlStateManager.popMatrix();
   }

   protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
      MainMenu mainMenuModule = this.getMainMenuModule();
      if (mainMenuModule != null && mainMenuModule.isEnabled() && Boolean.TRUE.equals(mainMenuModule.customButtons.getValue())) {
         if (mouseButton == 0) {
            for(GuiButton button : this.buttonList) {
               if (button.mousePressed(this.mc, mouseX, mouseY)) {
                  button.playPressSound(this.mc.getSoundHandler());
                  this.actionPerformed(button);
                  return;
               }
            }
         }
      } else {
         super.mouseClicked(mouseX, mouseY, mouseButton);
      }

      if (mainMenuModule != null && mainMenuModule.isEnabled() && Boolean.TRUE.equals(mainMenuModule.startupMusic.getValue()) && !SoundUtil.hasCustomStartupMusic() && mouseButton == 0 && mouseX >= this.musicPathX && mouseX <= this.musicPathX + this.musicPathW && mouseY >= this.musicPathY && mouseY <= this.musicPathY + this.musicPathH) {
         try {
            File folder = new File(SoundUtil.getMusicFolderPath());
            Desktop.getDesktop().open(folder);
         } catch (Exception var11) {
         }

      } else if ((Boolean)Manager.getInstance().accountPos.getValue()) {
         String username = this.mc.getSession().getUsername();
         String displayText = username;
         if (!this.showUsername) {
            StringBuilder asterisks = new StringBuilder();

            for(int i = 0; i < username.length(); ++i) {
               asterisks.append("*");
            }

            displayText = asterisks.toString();
         }

         String text = "Logged in as " + displayText;
         int textWidth = this.mc.fontRenderer.getStringWidth(text);
         int j = this.height / 4 + 48;
         float textX = (float)this.width / 2.0F - (float)textWidth / 2.0F - 2.0F;
         float textY = (float)(j + 72 + Manager.getInstance().getAccountMessagePosY());
         if ((float)mouseX >= textX && (float)mouseX <= textX + (float)textWidth && (float)mouseY >= textY && (float)mouseY <= textY + (float)this.mc.fontRenderer.FONT_HEIGHT) {
            this.showUsername = !this.showUsername;
         }

      }
   }
}
