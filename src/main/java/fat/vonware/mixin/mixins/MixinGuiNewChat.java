package fat.vonware.mixin.mixins;

import fat.vonware.Vonware;
import fat.vonware.features.modules.client.Fonts;
import fat.vonware.features.modules.client.Media;
import fat.vonware.features.modules.misc.Chat;
import fat.vonware.util.TextUtil;
import fat.vonware.util.Animations.ChatAnimation;
import fat.vonware.util.Animations.render.ColorUtil;
import fat.vonware.util.Chat.ColorEnum;
import java.awt.Color;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.ChatLine;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer.EnumChatVisibility;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
   value = {GuiNewChat.class},
   priority = 10000
)
public abstract class MixinGuiNewChat extends Gui {
   @Final
   @Shadow
   private List<ChatLine> drawnChatLines;
   @Shadow
   private Minecraft mc;
   @Shadow
   private int scrollPos;
   @Shadow
   private boolean isScrolled;
   private float animationPercent = 0.0F;
   private long slideStartTime = -1L;
   private static final long SLIDE_DURATION_MS = 220L;
   private float chatOpenAnim = 0.0F;
   private boolean lastChatOpenState = false;
   private long chatOpenStartTime = -1L;
   private float chatOpenBase = 0.0F;
   private static final long OPEN_CLOSE_DURATION_MS = 110L;
   private static final int ARC_SEGMENTS = 36;

   @Shadow
   public abstract boolean getChatOpen();

   @Shadow
   public abstract int getLineCount();

   @Shadow
   public abstract float getChatScale();

   @Shadow
   public abstract int getChatWidth();

   @Redirect(
      method = {"setChatLine"},
      at = @At(
    value = "INVOKE",
    target = "Ljava/util/List;size()I",
    ordinal = 0,
    remap = false
)
   )
   public int drawnChatLinesSize(List<ChatLine> list) {
      return Chat.getInstance().isOn() && (Boolean)Chat.getInstance().infinite.getValue() ? -2147483647 : list.size();
   }

   @Inject(
      method = {"addToSentMessages"},
      at = {@At("HEAD")}
   )
   private void addMessageHook(String message, CallbackInfo ci) {
      ((Chat)Vonware.moduleManager.getModuleByClass(Chat.class)).getMessages().add(message);
      String name = this.mc.player.getName();
      if (message != null) {
         Chat BETTER_CHAT_MODULE = (Chat)Vonware.moduleManager.getModuleByClass(Chat.class);
         if (BETTER_CHAT_MODULE.isEnabled() && (Boolean)BETTER_CHAT_MODULE.getSoundOnMention().getValue()) {
            Pattern pattern = Pattern.compile("(?<!<)" + Pattern.quote(name) + "(?!>)");
            Matcher matcher = pattern.matcher(message);
            if (matcher.find()) {
               this.mc.getSoundHandler().playSound(PositionedSoundRecord.getRecord(SoundEvents.ENTITY_ARROW_HIT_PLAYER, 1.0F, 0.3F));
            }
         }
      }

   }

   @Redirect(
      method = {"setChatLine"},
      at = @At(
    value = "INVOKE",
    target = "Ljava/util/List;size()I",
    ordinal = 2,
    remap = false
)
   )
   public int chatLinesSize(List<ChatLine> list) {
      return Chat.getInstance().isOn() && (Boolean)Chat.getInstance().infinite.getValue() ? -2147483647 : list.size();
   }

   @Inject(
      method = {"printChatMessageWithOptionalDeletion"},
      at = {@At("HEAD")}
   )
   private void resetAnimation(CallbackInfo ci) {
      this.slideStartTime = System.currentTimeMillis();
   }

   @Inject(
      method = {"drawChat"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void drawChatHook(int updateCounter, CallbackInfo ci) {
      Chat BETTER_CHAT = (Chat)Vonware.moduleManager.getModuleByClass(Chat.class);
      if (BETTER_CHAT.isEnabled()) {
         ci.cancel();
         Chat.AnimationMode animMode = BETTER_CHAT.animations.getValue();
         if (animMode != Chat.AnimationMode.NONE && this.slideStartTime >= 0L) {
            float t = Math.min(1.0F, (float)(System.currentTimeMillis() - this.slideStartTime) / 220.0F);
            float inv = t - 1.0F;
            this.animationPercent = Math.max(0.0F, Math.min(1.0F - inv * inv * inv * inv, 1.0F));
         }

         if (this.mc.gameSettings.chatVisibility != EnumChatVisibility.HIDDEN) {
            int i = this.getLineCount();
            int j = this.drawnChatLines.size();
            float f = this.mc.gameSettings.chatOpacity * 0.9F + 0.1F;
            if (j > 0) {
               boolean flag = this.getChatOpen();
               float f2 = this.getChatScale();
               int k = MathHelper.ceil((float)this.getChatWidth() / f2);
               if ((Boolean)BETTER_CHAT.openCloseAnim.getValue()) {
                  if (flag != this.lastChatOpenState) {
                     this.chatOpenBase = this.chatOpenAnim;
                     this.chatOpenStartTime = System.currentTimeMillis();
                     this.lastChatOpenState = flag;
                  }

                  if (this.chatOpenStartTime >= 0L) {
                     float elapsed = (float)(System.currentTimeMillis() - this.chatOpenStartTime);
                     float remaining = flag ? 1.0F - this.chatOpenBase : this.chatOpenBase;
                     float segDuration = remaining * 110.0F;
                     float rawT = segDuration > 0.0F ? Math.min(1.0F, elapsed / segDuration) : 1.0F;
                     float easedT = rawT * rawT * (3.0F - 2.0F * rawT);
                     this.chatOpenAnim = flag ? this.chatOpenBase + (1.0F - this.chatOpenBase) * easedT : this.chatOpenBase - this.chatOpenBase * easedT;
                     this.chatOpenAnim = Math.max(0.0F, Math.min(1.0F, this.chatOpenAnim));
                  }
               } else {
                  this.chatOpenAnim = 1.0F;
                  this.lastChatOpenState = flag;
               }

               float t = this.chatOpenAnim;
               float easedOpen;
               if (flag) {
                  float c1 = 1.70158F;
                  float c3 = 2.70158F;
                  float tm1 = t - 1.0F;
                  easedOpen = Math.max(0.0F, 1.0F + 2.70158F * tm1 * tm1 * tm1 + 1.70158F * tm1 * tm1);
               } else {
                  easedOpen = t * t * (3.0F - 2.0F * t);
               }

               GlStateManager.pushMatrix();
               GlStateManager.translate(2.0F, 8.0F, 0.0F);
               GlStateManager.scale(f2, f2, 1.0F);
               if (animMode == Chat.AnimationMode.UP && !this.isScrolled) {
                  t = 1.0F + (9.0F - 9.0F * this.animationPercent) * f2;
                  GlStateManager.translate(0.0F, t, 0.0F);
               }

               t = BETTER_CHAT.getRectAlpha();
               if (t > 0.0F) {
                  int bgHeight = 0;
                  boolean countAllLines = flag || (Boolean)BETTER_CHAT.openCloseAnim.getValue() && this.chatOpenAnim > 0.0F;

                  for(int i2 = 0; i2 + this.scrollPos < this.drawnChatLines.size() && i2 < i; ++i2) {
                     ChatLine chatLine = (ChatLine)this.drawnChatLines.get(i2 + this.scrollPos);
                     if (chatLine != null) {
                        double dc = MathHelper.clamp(((double)1.0F - (double)(updateCounter - chatLine.getUpdatedCounter()) / (double)200.0F) * (double)10.0F, (double)0.0F, (double)1.0F);
                        if ((double)255.0F * dc * dc >= (double)255.0F || countAllLines) {
                           bgHeight -= this.mc.fontRenderer.FONT_HEIGHT;
                        }
                     }
                  }

                  if ((Boolean)BETTER_CHAT.customColor.getValue()) {
                     Color gc = ColorUtil.getResolvedColor(BETTER_CHAT.customBgColor);
                     int alpha = (int)(127.0F * t);
                     alpha = Math.max(0, Math.min(255, alpha));
                     int bgColor = alpha << 24 | gc.getRed() << 16 | gc.getGreen() << 8 | gc.getBlue();
                     GlStateManager.pushMatrix();
                     if ((Boolean)BETTER_CHAT.openCloseAnim.getValue() && this.chatOpenAnim > 0.0F) {
                        GlStateManager.scale(1.0F, easedOpen, 1.0F);
                     }

                     this.drawRoundedChatRect(-2.0F, (float)bgHeight, (float)(k + 4), 0.0F, 4.0F, bgColor);
                     GlStateManager.popMatrix();
                  } else {
                     int bgColor = (int)(127.0F * t) << 24;
                     Gui.drawRect(-2, 0, k + 4, bgHeight, bgColor);
                  }
               }

               int l = 0;

               for(int i3 = 0; i3 + this.scrollPos < this.drawnChatLines.size() && i3 < i; ++i3) {
                  ChatLine chatline = (ChatLine)this.drawnChatLines.get(i3 + this.scrollPos);
                  if (chatline != null) {
                     int j2 = updateCounter - chatline.getUpdatedCounter();
                     if (j2 < 200 || flag) {
                        double d0 = MathHelper.clamp(((double)1.0F - (double)j2 / (double)200.0F) * (double)10.0F, (double)0.0F, (double)1.0F);
                        d0 *= d0;
                        int l2 = flag ? 255 : (int)((double)255.0F * d0);
                        l2 = (int)((float)l2 * f);
                        ++l;
                        if (l2 > 3) {
                           int j3 = -i3 * 9;
                           if (!(Boolean)BETTER_CHAT.customColor.getValue() && t > 0.0F && l2 < 255) {
                              drawRect(-2, j3 - 9, k + 4, j3, (int)(t * (float)(l2 / 2)) << 24);
                           }

                           String s = chatline.getChatComponent().getFormattedText();
                           GlStateManager.enableBlend();
                           String realName = this.mc.player.getName();
                           if (realName != null && !realName.isEmpty()) {
                              StringBuilder sb = new StringBuilder();

                              for(int i4 = 0; i4 < s.length(); ++i4) {
                                 if (i4 + realName.length() <= s.length() && s.regionMatches(true, i4, realName, 0, realName.length())) {
                                    sb.append(((ColorEnum)BETTER_CHAT.getSelfColor().getValue()).getColor()).append(s, i4, i4 + realName.length()).append(TextUtil.findMatch(s.substring(0, i4 + realName.length())));
                                    i4 += realName.length() - 1;
                                 } else {
                                    sb.append(s.charAt(i4));
                                 }
                              }

                              s = sb.toString();
                              if (Media.INSTANCE != null && Media.INSTANCE.isEnabled() && !s.contains("Logged in as")) {
                                 String mediaName = Media.INSTANCE.newName.getValue();
                                 if (mediaName != null && !mediaName.isEmpty()) {
                                    s = s.replace(realName, mediaName);
                                 }
                              }
                           }

                           float xPos = 0.0F;
                           if (animMode == Chat.AnimationMode.SLIDE) {
                              ChatAnimation anim = (ChatAnimation)BETTER_CHAT.animationMap.get(chatline);
                              if (anim != null) {
                                 anim.update();
                                 xPos = anim.getCurrent();
                              }
                           }

                           if ((Boolean)BETTER_CHAT.customFont.getValue() && Vonware.moduleManager.isModuleEnabled(Fonts.getInstance().getName())) {
                              Vonware.textManager.drawString(s, xPos, (float)(j3 - 8), 16777215 + (l2 << 24), (Boolean)Fonts.getInstance().fontShadow.getValue());
                           } else {
                              this.mc.fontRenderer.drawStringWithShadow(s, xPos, (float)(j3 - 8), 16777215 + (l2 << 24));
                           }

                           GlStateManager.disableAlpha();
                           GlStateManager.disableBlend();
                        }
                     }
                  }
               }

               if (flag) {
                  int k2 = this.mc.fontRenderer.FONT_HEIGHT;
                  GlStateManager.translate(-3.0F, 0.0F, 0.0F);
                  int l3 = j * k2 + j;
                  int i5 = l * k2 + l;
                  int j4 = this.scrollPos * i5 / j;
                  int k3 = i5 * i5 / l3;
                  if (l3 != i5) {
                     int k4 = j4 > 0 ? 170 : 96;
                     int l4 = this.isScrolled ? 13382451 : 3355562;
                     drawRect(0, -j4, 2, -j4 - k3, l4 + (k4 << 24));
                     drawRect(2, -j4, 1, -j4 - k3, 13421772 + (k4 << 24));
                  }
               }

               GlStateManager.enableAlpha();
               GlStateManager.enableBlend();
               GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
               GlStateManager.popMatrix();
            }
         }
      }
   }

   @Inject(
      method = {"printChatMessageWithOptionalDeletion"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/gui/GuiNewChat;setChatLine(Lnet/minecraft/util/text/ITextComponent;IIZ)V",
   shift = At.Shift.AFTER
)}
   )
   private void onLineAdded(ITextComponent component, int chatLineId, CallbackInfo ci) {
      Chat chat = (Chat)Vonware.moduleManager.getModuleByClass(Chat.class);
      if (chat.isEnabled() && chat.animations.getValue() == Chat.AnimationMode.SLIDE) {
         if (!this.drawnChatLines.isEmpty()) {
            ChatLine newest = (ChatLine)this.drawnChatLines.get(0);
            float textWidth = (float)this.mc.fontRenderer.getStringWidth(newest.getChatComponent().getFormattedText());
            long durationMs = (long)((Float)chat.animationTime.getValue() * 1000.0F);
            chat.animationMap.put(newest, new ChatAnimation(durationMs, (double)(-textWidth), (double)0.0F));
         }
      }
   }

   private void drawRoundedChatRect(float x1, float y1, float x2, float y2, float radius, int color) {
      float top = Math.min(y1, y2);
      float bot = Math.max(y1, y2);
      float rad = Math.min(radius, Math.min((x2 - x1) / 2.0F, (bot - top) / 2.0F));
      int a = color >> 24 & 255;
      int r = color >> 16 & 255;
      int g = color >> 8 & 255;
      int b = color & 255;
      Gui.drawRect((int)(x1 + rad), (int)top, (int)(x2 - rad), (int)bot, color);
      Gui.drawRect((int)x1, (int)(top + rad), (int)(x1 + rad), (int)(bot - rad), color);
      Gui.drawRect((int)(x2 - rad), (int)(top + rad), (int)x2, (int)(bot - rad), color);
      GlStateManager.disableTexture2D();
      GlStateManager.enableBlend();
      GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
      GlStateManager.disableAlpha();
      GlStateManager.color((float)r / 255.0F, (float)g / 255.0F, (float)b / 255.0F, (float)a / 255.0F);
      drawArc((double)(x1 + rad), (double)(top + rad), (double)rad, (double)180.0F, (double)270.0F);
      drawArc((double)(x2 - rad), (double)(top + rad), (double)rad, (double)270.0F, (double)360.0F);
      drawArc((double)(x2 - rad), (double)(bot - rad), (double)rad, (double)0.0F, (double)90.0F);
      drawArc((double)(x1 + rad), (double)(bot - rad), (double)rad, (double)90.0F, (double)180.0F);
      GlStateManager.enableTexture2D();
      GlStateManager.disableBlend();
      GlStateManager.enableAlpha();
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
   }

   private static void drawArc(double cx, double cy, double r, double start_angle, double end_angle) {
      GlStateManager.glBegin(4);
      double totalSteps = Math.abs(end_angle - start_angle);
      int steps = Math.max(1, (int)(totalSteps / (double)10.0F));
      double stepSize = (end_angle - start_angle) / (double)steps;

      for(int i = 0; i < steps; ++i) {
         double a1 = start_angle + (double)i * stepSize;
         double a2 = start_angle + (double)(i + 1) * stepSize;
         double rad1 = Math.toRadians(a1);
         double rad2 = Math.toRadians(a2);
         GL11.glVertex2d(cx, cy);
         GL11.glVertex2d(cx + Math.cos(rad2) * r, cy + Math.sin(rad2) * r);
         GL11.glVertex2d(cx + Math.cos(rad1) * r, cy + Math.sin(rad1) * r);
      }

      GlStateManager.glEnd();
   }
}
