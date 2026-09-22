package fat.vonware.mixin.mixins;

import fat.vonware.Global;
import fat.vonware.Vonware;
import fat.vonware.features.modules.client.Colors;
import fat.vonware.features.modules.client.Manager;
import fat.vonware.features.modules.misc.Chat;
import fat.vonware.util.Animations.render.ColorUtil;
import java.awt.Color;
import java.util.List;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({GuiChat.class})
public abstract class MixinGuiChat extends GuiScreen {
   @Shadow
   protected GuiTextField inputField;
   private boolean justCompleted = false;
   private float chatSlideOffset = 20.0F;
   private int hoveredSuggestionIndex = -1;
   private int suggestionScrollOffset = 0;
   private String lastInputText = "";
   private static final int MAX_VISIBLE_SUGGESTIONS = 6;

   @Inject(
      method = {"keyTyped"},
      at = {@At("HEAD")},
      cancellable = true
   )
   protected void onKeyTyped(char typedChar, int keyCode, CallbackInfo ci) {
      if (Vonware.predictionManager != null) {
         String currentText = this.inputField.getText();
         List<String> suggestions = Vonware.predictionManager.getSuggestions();
         if (keyCode == 15) {
            String toComplete = this.resolveCompletion(suggestions, currentText);
            if (toComplete != null) {
               this.inputField.setText(toComplete);
               this.inputField.setCursorPositionEnd();
               this.justCompleted = true;
               ci.cancel();
            }

         } else {
            if (keyCode == 205 && this.inputField.getCursorPosition() == currentText.length()) {
               String toComplete = this.resolveCompletion(suggestions, currentText);
               if (toComplete != null && toComplete.startsWith(currentText)) {
                  this.inputField.setText(toComplete);
                  this.inputField.setCursorPositionEnd();
                  this.justCompleted = true;
                  ci.cancel();
               }
            }

         }
      }
   }

   private String resolveCompletion(List<String> suggestions, String currentText) {
      if (suggestions.isEmpty()) {
         return null;
      } else if (this.hoveredSuggestionIndex >= 0 && this.hoveredSuggestionIndex < suggestions.size()) {
         return (String)suggestions.get(this.hoveredSuggestionIndex);
      } else {
         String keyboardPick = Vonware.predictionManager.getSelectedSuggestion();
         if (keyboardPick != null) {
            return keyboardPick;
         } else {
            return this.suggestionScrollOffset < suggestions.size() ? (String)suggestions.get(this.suggestionScrollOffset) : null;
         }
      }
   }

   @Inject(
      method = {"handleMouseInput"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void onHandleMouseInput(CallbackInfo ci) {
      if (Vonware.predictionManager != null) {
         int wheel = Mouse.getEventDWheel();
         if (wheel != 0) {
            String currentText = this.inputField.getText();
            if (currentText.startsWith(Vonware.commandManager.getPrefix())) {
               List<String> suggestions = Vonware.predictionManager.getSuggestions();
               if (suggestions.size() > 1) {
                  int maxOffset = Math.max(0, suggestions.size() - 6);
                  if (wheel > 0) {
                     this.suggestionScrollOffset = Math.max(0, this.suggestionScrollOffset - 1);
                  } else {
                     this.suggestionScrollOffset = Math.min(maxOffset, this.suggestionScrollOffset + 1);
                  }

                  Vonware.predictionManager.setSelectedIndex(this.suggestionScrollOffset);
                  ci.cancel();
               }
            }
         }
      }
   }

   @Inject(
      method = {"updateScreen"},
      at = {@At("HEAD")}
   )
   public void onUpdateScreen(CallbackInfo ci) {
      if (Vonware.predictionManager != null) {
         if (!this.justCompleted) {
            Vonware.predictionManager.updateSuggestions(this.inputField.getText());
         }

         this.justCompleted = false;
         String current = this.inputField.getText();
         if (!current.equals(this.lastInputText)) {
            this.suggestionScrollOffset = 0;
         }

         this.lastInputText = current;
      }
   }

   @Inject(
      method = {"onGuiClosed"},
      at = {@At("HEAD")}
   )
   public void onGuiClosed(CallbackInfo ci) {
      if (Vonware.predictionManager != null) {
         Vonware.predictionManager.reset();
      }

      this.lastInputText = "";
      this.suggestionScrollOffset = 0;
      this.hoveredSuggestionIndex = -1;
   }

   @Redirect(
      method = {"drawScreen"},
      at = @At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/gui/GuiTextField;drawTextBox()V"
)
   )
   private void redirectInputFieldDraw(GuiTextField field) {
      Chat chat = Chat.getInstance();
      if (!chat.isEnabled() || !(Boolean)chat.customFontCharacters.getValue()) {
         field.drawTextBox();
      }

   }

   private boolean shouldDrawCustomInputFont() {
      Chat chat = Chat.getInstance();
      return chat.isEnabled() && (Boolean)chat.customFontCharacters.getValue();
   }

   private int customStringWidth(String text) {
      return text != null && !text.isEmpty() ? Vonware.textManager.getStringWidth(text) : 0;
   }

   private int findVisibleStart(String text, int cursor, int maxWidth) {
      int start;
      for(start = Math.max(0, Math.min(cursor, text.length())); start > 0 && this.customStringWidth(text.substring(start - 1, cursor)) <= maxWidth; --start) {
      }

      return start;
   }

   private int findVisibleEnd(String text, int start, int maxWidth) {
      int end;
      for(end = start; end < text.length() && this.customStringWidth(text.substring(start, end + 1)) <= maxWidth; ++end) {
      }

      return end;
   }

   private void drawCustomInputField() {
      if (this.shouldDrawCustomInputFont()) {
         String text = this.inputField.getText();
         int cursor = Math.max(0, Math.min(this.inputField.getCursorPosition(), text.length()));
         int selection = Math.max(0, Math.min(this.inputField.getSelectionEnd(), text.length()));
         int contentX = this.inputField.x + 4;
         int maxWidth = Math.max(1, this.inputField.width - 8);
         int start = this.findVisibleStart(text, cursor, maxWidth);
         int end = this.findVisibleEnd(text, start, maxWidth);
         String visible = text.substring(start, end);
         int fontHeight = Vonware.textManager.getFontHeight();
         float textY = (float)this.inputField.y + (float)(this.inputField.height - fontHeight) / 2.0F - 1.0F;
         if (selection != cursor) {
            int leftIndex = Math.max(start, Math.min(cursor, selection));
            int rightIndex = Math.min(end, Math.max(cursor, selection));
            if (rightIndex > leftIndex) {
               int selectX1 = contentX + this.customStringWidth(text.substring(start, leftIndex));
               int selectX2 = contentX + this.customStringWidth(text.substring(start, rightIndex));
               drawRect(selectX1, this.inputField.y + 1, selectX2, this.inputField.y + this.inputField.height - 1, -2141167361);
            }
         }

         Vonware.textManager.drawString(visible, (float)contentX, textY, -2039584, false);
         if (this.inputField.isFocused() && (System.currentTimeMillis() / 500L & 1L) == 0L) {
            int clampedCursor = Math.max(start, Math.min(cursor, end));
            int cursorX = contentX + this.customStringWidth(text.substring(start, clampedCursor));
            drawRect(cursorX, this.inputField.y + 2, cursorX + 1, this.inputField.y + this.inputField.height - 2, -3092272);
         }

      }
   }

   @Inject(
      method = {"drawScreen"},
      at = {@At("HEAD")}
   )
   public void onDrawScreenPre(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
      this.chatSlideOffset += (0.0F - this.chatSlideOffset) * 0.05F;
      GlStateManager.pushMatrix();
      GlStateManager.translate(0.0F, this.chatSlideOffset, 0.0F);
   }

   @Inject(
      method = {"drawScreen"},
      at = {@At("RETURN")}
   )
   public void onDrawScreen(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
      GlStateManager.popMatrix();
      this.drawCustomInputField();
      if (Vonware.predictionManager != null) {
         String currentText = this.inputField.getText();
         if (Manager.getInstance().shouldShowReplyHelper(currentText)) {
            String playerName = Manager.getInstance().getLastWhisperer();
            boolean isBlocked = Manager.getInstance().wasLastWhisperBlocked();
            boolean isFriend = Vonware.friendManager.isFriend(playerName);
            int typedTextWidth = this.fontRenderer.getStringWidth(currentText);
            int x = 4 + typedTextWidth;
            int y = this.height - 12;
            String prefix = "     Replying to -> ";
            this.fontRenderer.drawString(prefix, x, y, -2138535800);
            int prefixWidth = this.fontRenderer.getStringWidth(prefix);
            if (isBlocked) {
               String blockedTag = "[Unicode] ";
               this.fontRenderer.drawString(blockedTag, x + prefixWidth, y, -48060);
               int tagWidth = this.fontRenderer.getStringWidth(blockedTag);
               this.fontRenderer.drawString(playerName, x + prefixWidth + tagWidth, y, -2138535800);
            } else {
               int playerColor = isFriend ? -16711681 : -2138535800;
               this.fontRenderer.drawString(playerName, x + prefixWidth, y, playerColor);
            }
         }

         if (currentText.startsWith(Vonware.commandManager.getPrefix())) {
            int x = 2;
            int y = this.height - 14;
            int width = this.width - 4 - 2;
            int height = 12;
            Color outlineColor = (Boolean)Colors.getInstance().rainbow.getValue() ? ColorUtil.rainbow(0) : new Color(Global.getRed(), Global.getGreen(), Global.getBlue());
            this.drawHorizontalLine(x - 1, x + width, y - 1, outlineColor.getRGB());
            this.drawHorizontalLine(x - 1, x + width, y + height, outlineColor.getRGB());
            this.drawVerticalLine(x - 1, y - 1, y + height, outlineColor.getRGB());
            this.drawVerticalLine(x + width, y - 1, y + height, outlineColor.getRGB());
         }

         if (currentText.startsWith(Vonware.commandManager.getPrefix())) {
            String suggestion = Vonware.predictionManager.getSelectedSuggestion();
            if (suggestion != null && suggestion.startsWith(currentText) && !suggestion.equals(currentText)) {
               String hint = suggestion.substring(currentText.length());
               int textX = 4 + this.fontRenderer.getStringWidth(currentText);
               int textY = this.height - 12;
               GlStateManager.pushMatrix();
               this.fontRenderer.drawStringWithShadow(hint, (float)textX, (float)textY, -10066330);
               GlStateManager.popMatrix();
            }
         }

         if (currentText.startsWith(Vonware.commandManager.getPrefix())) {
            List<String> suggestionList = Vonware.predictionManager.getSuggestions();
            int totalSuggestions = suggestionList.size();
            if (totalSuggestions > 1) {
               int displayCount = Math.min(totalSuggestions - this.suggestionScrollOffset, 6);
               int yStart = this.height - 14 - displayCount * 10 - 2;
               int keyboardSel = Vonware.predictionManager.getSelectedIndex();
               this.hoveredSuggestionIndex = -1;

               for(int i = 0; i < displayCount; ++i) {
                  int absIdx = this.suggestionScrollOffset + i;
                  if (absIdx >= totalSuggestions) {
                     break;
                  }

                  String text = (String)suggestionList.get(absIdx);
                  int y = yStart + i * 10;
                  int x2 = 2 + this.fontRenderer.getStringWidth(text) + 4;
                  boolean hovered = mouseX >= 2 && mouseX <= x2 && mouseY >= y && mouseY < y + 10;
                  if (hovered) {
                     this.hoveredSuggestionIndex = absIdx;
                  }

                  int effectiveSel = this.hoveredSuggestionIndex >= 0 ? this.hoveredSuggestionIndex : keyboardSel;
                  drawRect(2, y, x2, y + 10, absIdx == effectiveSel ? -1879048192 : 1879048192);
                  if (absIdx == effectiveSel) {
                     Color accent = new Color(Global.getRed(), Global.getGreen(), Global.getBlue());
                     drawRect(2, y, 3, y + 10, accent.getRGB());
                  }

                  this.fontRenderer.drawStringWithShadow(text, 5.0F, (float)(y + 1), absIdx == effectiveSel ? -1 : -5592406);
               }

               String hint = "scroll \u2195  TAB/\u2192 complete";
               this.fontRenderer.drawStringWithShadow(hint, (float)(this.width - this.fontRenderer.getStringWidth(hint) - 4), (float)(this.height - 26), -7829368);
            }
         }

      }
   }

   @Inject(
      method = {"mouseClicked"},
      at = {@At("HEAD")},
      cancellable = true
   )
   protected void onMouseClicked(int mouseX, int mouseY, int mouseButton, CallbackInfo ci) {
      if (Vonware.predictionManager != null && mouseButton == 0) {
         String currentText = this.inputField.getText();
         if (currentText.startsWith(Vonware.commandManager.getPrefix())) {
            List<String> suggestionList = Vonware.predictionManager.getSuggestions();
            int totalSuggestions = suggestionList.size();
            if (totalSuggestions > 1) {
               int displayCount = Math.min(totalSuggestions - this.suggestionScrollOffset, 6);
               int yStart = this.height - 14 - displayCount * 10 - 2;

               for(int i = 0; i < displayCount; ++i) {
                  int absIdx = this.suggestionScrollOffset + i;
                  if (absIdx >= totalSuggestions) {
                     break;
                  }

                  String text = (String)suggestionList.get(absIdx);
                  int y = yStart + i * 10;
                  int x2 = 2 + this.fontRenderer.getStringWidth(text) + 4;
                  if (mouseX >= 2 && mouseX <= x2 && mouseY >= y && mouseY < y + 10) {
                     Vonware.predictionManager.setSelectedIndex(absIdx);
                     this.inputField.setText(text);
                     this.inputField.setCursorPositionEnd();
                     this.justCompleted = true;
                     Vonware.predictionManager.reset();
                     this.suggestionScrollOffset = 0;
                     ci.cancel();
                     return;
                  }
               }

            }
         }
      }
   }
}
