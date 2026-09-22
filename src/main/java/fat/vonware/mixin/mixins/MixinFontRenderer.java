package fat.vonware.mixin.mixins;

import com.mojang.authlib.GameProfile;
import fat.vonware.features.modules.client.Media;
import fat.vonware.features.modules.render.NoRender;
import fat.vonware.util.HexColorText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
   value = {FontRenderer.class},
   priority = 100
)
public abstract class MixinFontRenderer {
   private String cachedUsername = null;
   private GameProfile cachedProfile = null;
   private boolean vonware$renderingHexText;
   private boolean vonware$measuringHexText;

   @Shadow
   protected abstract void renderStringAtPos(String var1, boolean var2);

   @Redirect(
      method = {"renderString(Ljava/lang/String;FFIZ)I"},
      at = @At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/gui/FontRenderer;renderStringAtPos(Ljava/lang/String;Z)V"
)
   )
   public void renderStringAtPosHook(FontRenderer renderer, String text, boolean shadow) {
      if (Media.INSTANCE != null && Media.INSTANCE.isEnabled()) {
         GameProfile currentProfile = Minecraft.getMinecraft().getSession().getProfile();
         if (currentProfile != this.cachedProfile) {
            this.cachedProfile = currentProfile;
            this.cachedUsername = currentProfile.getName();
         }

         if (!text.contains("Logged in as") && text.contains(this.cachedUsername)) {
            this.renderStringAtPos(text.replace(this.cachedUsername, (CharSequence)Media.INSTANCE.newName.getValue()), shadow);
            return;
         }
      }

      this.renderStringAtPos(text, shadow);
   }

   @Inject(
      method = {"drawString(Ljava/lang/String;FFIZ)I"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onDrawString(String text, float x, float y, int color, boolean dropShadow, CallbackInfoReturnable<Integer> cir) {
      if (text != null && NoRender.getInstance() != null && (Boolean)NoRender.getInstance().noCombatTag.getValue() && this.shouldBlock(text.toLowerCase())) {
         cir.setReturnValue((int)x);
      } else {
         if (!this.vonware$renderingHexText && HexColorText.hasHexColor(text)) {
            FontRenderer renderer = (FontRenderer)(Object)this;
            float currentX = x;
            int currentColor = color;
            int alpha = color & -16777216;
            if (alpha == 0) {
               alpha = -16777216;
            }

            this.vonware$renderingHexText = true;

            try {
               StringBuilder segment = new StringBuilder();
               int i = 0;

               while(i < text.length()) {
                  if (HexColorText.isHexToken(text, i)) {
                     currentX = this.vonware$flushSegment(renderer, segment, currentX, y, currentColor, dropShadow);
                     currentColor = alpha | HexColorText.parseRgb(text, i);
                     i += 8;
                  } else {
                     if (text.charAt(i) == 167 && i + 1 < text.length()) {
                        char code = Character.toLowerCase(text.charAt(i + 1));
                        int legacyRgb = this.vonware$getLegacyColor(code);
                        if (legacyRgb >= 0) {
                           currentX = this.vonware$flushSegment(renderer, segment, currentX, y, currentColor, dropShadow);
                           currentColor = alpha | legacyRgb;
                           i += 2;
                           continue;
                        }

                        if (code == 'r') {
                           currentX = this.vonware$flushSegment(renderer, segment, currentX, y, currentColor, dropShadow);
                           currentColor = color;
                           i += 2;
                           continue;
                        }
                     }

                     segment.append(text.charAt(i++));
                  }
               }

               currentX = this.vonware$flushSegment(renderer, segment, currentX, y, currentColor, dropShadow);
            } finally {
               this.vonware$renderingHexText = false;
               GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            }

            cir.setReturnValue((int)currentX);
         }

      }
   }

   @Inject(
      method = {"getStringWidth"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onGetStringWidth(String text, CallbackInfoReturnable<Integer> cir) {
      if (!this.vonware$measuringHexText && HexColorText.hasHexColor(text)) {
         this.vonware$measuringHexText = true;

         try {
            cir.setReturnValue(((FontRenderer)(Object)this).getStringWidth(HexColorText.strip(text)));
         } finally {
            this.vonware$measuringHexText = false;
         }
      }

   }

   private float vonware$flushSegment(FontRenderer renderer, StringBuilder segment, float x, float y, int color, boolean dropShadow) {
      if (segment.length() == 0) {
         return x;
      } else {
         String part = segment.toString();
         renderer.drawString(part, x, y, color, dropShadow);
         float result = x + (float)renderer.getStringWidth(part);
         segment.setLength(0);
         return result;
      }
   }

   private int vonware$getLegacyColor(char code) {
      switch (code) {
         case '0':
            return 0;
         case '1':
            return 170;
         case '2':
            return 43520;
         case '3':
            return 43690;
         case '4':
            return 11141120;
         case '5':
            return 11141290;
         case '6':
            return 16755200;
         case '7':
            return 11184810;
         case '8':
            return 5592405;
         case '9':
            return 5592575;
         case ':':
         case ';':
         case '<':
         case '=':
         case '>':
         case '?':
         case '@':
         case 'A':
         case 'B':
         case 'C':
         case 'D':
         case 'E':
         case 'F':
         case 'G':
         case 'H':
         case 'I':
         case 'J':
         case 'K':
         case 'L':
         case 'M':
         case 'N':
         case 'O':
         case 'P':
         case 'Q':
         case 'R':
         case 'S':
         case 'T':
         case 'U':
         case 'V':
         case 'W':
         case 'X':
         case 'Y':
         case 'Z':
         case '[':
         case '\\':
         case ']':
         case '^':
         case '_':
         case '`':
         default:
            return -1;
         case 'a':
            return 5635925;
         case 'b':
            return 5636095;
         case 'c':
            return 16733525;
         case 'd':
            return 16733695;
         case 'e':
            return 16777045;
         case 'f':
            return 16777215;
      }
   }

   private boolean shouldBlock(String lower) {
      return lower.contains("combat for") || lower.contains("no longer in combat");
   }
}
