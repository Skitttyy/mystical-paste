package fat.vonware.features.modules.client;

import com.mojang.realmsclient.gui.ChatFormatting;
import fat.vonware.Vonware;
import fat.vonware.event.events.ClientEvent;
import fat.vonware.features.command.Command;
import fat.vonware.features.gui.font.CFont;
import fat.vonware.features.modules.Module;
import fat.vonware.features.setting.Setting;
import java.awt.GraphicsEnvironment;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class Fonts extends Module {
   private static Fonts INSTANCE = new Fonts();
   public Setting<CFont.FontQuality> fontQuality;
   public Setting<String> fontName;
   public Setting<Boolean> antiAlias;
   public Setting<Boolean> fractionalMetrics;
   public Setting<Integer> fontSize;
   public Setting<FontStyle> fontStyle;
   public Setting<Boolean> fontShadow;
   public Setting<Float> shadowOffset;
   private boolean reloadFont;

   public Fonts() {
      super("Fonts", "Uses Custom Font for text", Module.Category.CLIENT, true, false, false);
      this.fontQuality = new Setting<CFont.FontQuality>("Quality", CFont.FontQuality.HIGH);
      this.fontName = new Setting<String>("FontName", "Impact");
      this.antiAlias = new Setting<Boolean>("AntiAlias", true);
      this.fractionalMetrics = new Setting<Boolean>("Metrics", true);
      this.fontSize = new Setting<Integer>("Size", 18, 12, 30);
      this.fontStyle = new Setting<FontStyle>("Style", Fonts.FontStyle.PLAIN);
      this.fontShadow = new Setting<Boolean>("Font Shadow", true);
      this.shadowOffset = (new Setting<Float>("Shadow Offset", 1.0F, 0.0F, 2.0F, (v) -> (Boolean)this.fontShadow.getValue(), "Controls how far the custom-font shadow is offset from the text")).setStep(0.1F);
      this.register(this.fontQuality);
      this.register(this.fontStyle);
      this.register(this.fontName);
      this.register(this.antiAlias);
      this.register(this.fractionalMetrics);
      this.register(this.fontSize);
      this.register(this.fontShadow);
      this.register(this.shadowOffset);
      this.reloadFont = true;
      this.enabled.setValue(true);
      this.setInstance();
   }

   public static Fonts getInstance() {
      if (INSTANCE == null) {
         INSTANCE = new Fonts();
      }

      return INSTANCE;
   }

   public static boolean checkFont(String font, boolean message) {
      for(String s : GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()) {
         if (!message && s.equals(font)) {
            return true;
         }

         if (message) {
            Command.sendMessage(s);
         }
      }

      return false;
   }

   private void setInstance() {
      INSTANCE = this;
   }

   @SubscribeEvent
   public void onSettingChange(ClientEvent event) {
      Setting setting;
      if (event.getStage() == 2 && (setting = event.getSetting()) != null && setting.getFeature().equals(this)) {
         if (setting.getName().equals("FontName") && !checkFont(setting.getPlannedValue().toString(), false)) {
            Command.sendMessage(ChatFormatting.RED + "That font doesnt exist.");
            event.setCanceled(true);
            return;
         }

         this.reloadFont = true;
      }

   }

   public void onTick() {
      if (this.reloadFont) {
         Vonware.textManager.init(false);
         this.reloadFont = false;
      }

   }

   public static enum FontStyle {
      PLAIN(0),
      BOLD(1),
      ITALIC(2),
      BOLD_ITALIC(3);

      private final int awtValue;

      private FontStyle(int awtValue) {
         this.awtValue = awtValue;
      }

      public int getAwtValue() {
         return this.awtValue;
      }
   }
}
