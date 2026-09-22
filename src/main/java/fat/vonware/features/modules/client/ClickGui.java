package fat.vonware.features.modules.client;

import com.mojang.realmsclient.gui.ChatFormatting;
import fat.vonware.Vonware;
import fat.vonware.event.events.ClientEvent;
import fat.vonware.event.events.KeyPressedEvent;
import fat.vonware.features.command.Command;
import fat.vonware.features.gui.newgui.newcomponents.NewVonwareGui;
import fat.vonware.features.modules.Module;
import fat.vonware.features.setting.Bind;
import fat.vonware.features.setting.ColorSetting;
import fat.vonware.features.setting.Setting;
import fat.vonware.util.Util;
import fat.vonware.util.OtherRenderShit.gui.SoundUtil;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiScreenOptionsSounds;
import net.minecraft.client.gui.GuiVideoSettings;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Keyboard;

public class ClickGui extends Module {
   private static ClickGui INSTANCE = new ClickGui();
   public Setting<String> prefix = new Setting<String>("Prefix", ",");
   public Setting<Boolean> guiMove = new Setting<Boolean>("GuiMove", true);
   public Setting<Description> description;
   public Setting<Boolean> settingDescriptions;
   public Setting<Background> background;
   public Setting<Boolean> globalBackground;
   public Setting<OutlineMode> outline;
   public Setting<Float> guiScale;
   public Setting<Boolean> pulseColor;
   public Setting<Boolean> pulseGui;
   public Setting<Integer> pulseSpeed;
   public Setting<GuiBlur> guiBlur;
   public Setting<Integer> blurStrength;
   public Setting<Boolean> buttonFill;
   public Setting<EnabledStyle> enabledStyle;
   public Setting<SettingsIndicator> settingsIndicator;
   public ColorSetting indicatorColor;
   public Setting<Boolean> clientInfo;
   public Setting<Integer> panelSpacing;
   public Setting<PanelAnimation> panelAnimation;
   public Setting<SearchProperties> searchProperties;
   public Setting<NumberProperties> numberProperties;
   public Setting<Boolean> binds;
   private final ColorSetting customColor;
   private static KeyBinding[] keys;

   public ColorSetting getHighlightColorSetting() {
      return this.customColor;
   }

   public ClickGui() {
      super("ClickGui", "Opens the new ClickGui", Module.Category.CLIENT, true, false, false);
      this.description = new Setting<Description>("Description", ClickGui.Description.Mouse);
      this.settingDescriptions = new Setting<Boolean>("Setting-Descriptions", true, "Controls whether a description tooltip shows when hovering over an individual setting inside an expanded module.");
      this.background = new Setting<Background>("Background", ClickGui.Background.Panel);
      this.globalBackground = new Setting<Boolean>("Global Background", true);
      this.outline = new Setting<OutlineMode>("Outline", ClickGui.OutlineMode.OutlineFlow);
      this.guiScale = new Setting<Float>("Gui-Scale", 0.9F, 0.1F, 1.0F);
      this.pulseColor = new Setting<Boolean>("Pulse Color", false);
      this.pulseGui = new Setting<Boolean>("Pulse Gui", false);
      this.pulseSpeed = new Setting<Integer>("PulseSpeed", 5, 1, 10, (v) -> (Boolean)this.pulseColor.getValue());
      this.guiBlur = new Setting<GuiBlur>("GuiBlur", ClickGui.GuiBlur.All);
      this.blurStrength = new Setting<Integer>("Blur Strength", 15, 5, 40, (v) -> this.guiBlur.getValue() != ClickGui.GuiBlur.Off);
      this.buttonFill = new Setting<Boolean>("Button Fill", true);
      this.enabledStyle = new Setting<EnabledStyle>("EnabledStyle", ClickGui.EnabledStyle.Gradient);
      this.settingsIndicator = new Setting<SettingsIndicator>("Indicators", ClickGui.SettingsIndicator.None);
      this.indicatorColor = new ColorSetting("IndicatorColor", 16777215, false, (v) -> this.settingsIndicator.getValue() != ClickGui.SettingsIndicator.None);
      this.clientInfo = new Setting<Boolean>("ClientInfo", false);
      this.panelSpacing = new Setting<Integer>("Panel-Spacing", 124, 120, 145);
      this.panelAnimation = new Setting<PanelAnimation>("Panel-Animation", ClickGui.PanelAnimation.Wipe);
      this.searchProperties = new Setting<SearchProperties>("Search-Mode", ClickGui.SearchProperties.Remove);
      this.numberProperties = new Setting<NumberProperties>("NumberProperties", ClickGui.NumberProperties.End);
      this.binds = new Setting<Boolean>("Binds", true);
      this.customColor = new ColorSetting("HighlightColor", 16777215, false);
      this.register(this.prefix);
      this.register(this.guiMove);
      this.register(this.pulseColor);
      this.register(this.pulseGui);
      this.register(this.pulseSpeed);
      this.register(this.guiScale);
      this.register(this.outline);
      this.register(this.guiBlur);
      this.register(this.blurStrength);
      this.register(this.description);
      this.register(this.settingDescriptions);
      this.register(this.background);
      this.register(this.globalBackground);
      this.register(this.binds);
      this.register(this.buttonFill);
      this.register(this.enabledStyle);
      this.register(this.settingsIndicator);
      this.register(this.indicatorColor);
      this.register(this.clientInfo);
      this.register(this.panelSpacing);
      this.register(this.panelAnimation);
      this.register(this.searchProperties);
      this.register(this.numberProperties);
      this.register(this.customColor);
      this.setInstance();
      this.bind.setValue(new Bind(38));
   }

   public static ClickGui getInstance() {
      if (INSTANCE == null) {
         INSTANCE = new ClickGui();
      }

      return INSTANCE;
   }

   private void setInstance() {
      INSTANCE = this;
   }

   public void onEnable() {
      Util.mc.displayGuiScreen(new NewVonwareGui(false));
      SoundUtil.playSecretShiny();
   }

   public void onDisable() {
      if ((Boolean)Colors.getInstance().futureSync.getValue()) {
         Colors.getInstance().syncToFuture();
      }

   }

   public void onLoad() {
      Vonware.commandManager.setPrefix(this.prefix.getValue());
   }

   public void onTick() {
      if (!(mc.currentScreen instanceof NewVonwareGui)) {
         this.disable();
      }

   }

   public void onUpdate() {
      if (!(mc.currentScreen instanceof GuiOptions) && !(mc.currentScreen instanceof GuiVideoSettings) && !(mc.currentScreen instanceof GuiScreenOptionsSounds) && !(mc.currentScreen instanceof GuiContainer) && !(mc.currentScreen instanceof GuiIngameMenu) && !(mc.currentScreen instanceof NewVonwareGui)) {
         if (mc.currentScreen == null) {
            for(KeyBinding bind : keys) {
               if (!Keyboard.isKeyDown(bind.getKeyCode())) {
                  KeyBinding.setKeyBindState(bind.getKeyCode(), false);
               }
            }
         }
      } else {
         for(KeyBinding bind : keys) {
            KeyBinding.setKeyBindState(bind.getKeyCode(), Keyboard.isKeyDown(bind.getKeyCode()));
         }
      }

   }

   @SubscribeEvent
   public void onKeyEvent(KeyPressedEvent event) {
      if (event.getStage() == 0 && !(mc.currentScreen instanceof GuiChat)) {
         event.info = event.pressed;
      }

   }

   @SubscribeEvent
   public void onSettingChange(ClientEvent event) {
      if (event.getStage() == 2 && event.getSetting().getFeature().equals(this) && event.getSetting().equals(this.prefix)) {
         Vonware.commandManager.setPrefix(this.prefix.getPlannedValue());
         Command.sendMessage("Prefix set to " + ChatFormatting.DARK_GRAY + Vonware.commandManager.getPrefix());
      }

   }

   static {
      keys = new KeyBinding[]{mc.gameSettings.keyBindForward, mc.gameSettings.keyBindBack, mc.gameSettings.keyBindLeft, mc.gameSettings.keyBindRight, mc.gameSettings.keyBindJump, mc.gameSettings.keyBindSprint};
   }

   public static enum PanelAnimation {
      Pull,
      Wipe;
   }

   public static enum Background {
      Panel,
      None,
      All;
   }

   public static enum OutlineMode {
      None,
      Normal,
      OutlineFlow;
   }

   public static enum Description {
      Mouse,
      None,
      Bottom;
   }

   public static enum GuiBlur {
      Off,
      Panel,
      All;
   }

   public static enum SearchProperties {
      Highlight,
      Remove;
   }

   public static enum EnabledStyle {
      Gradient,
      Colored,
      None;
   }

   public static enum SettingsIndicator {
      PlusMinus,
      Dots,
      Bubble,
      None;
   }

   public static enum NumberProperties {
      NextTo,
      End;
   }
}
