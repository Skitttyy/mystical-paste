package fat.vonware.features.modules.client;

import fat.vonware.features.modules.Module;
import fat.vonware.features.setting.Setting;
import fat.vonware.util.shader.menu.MenuGLSLShader;

public class MainMenu extends Module {
   public Setting<Boolean> customButtons = new Setting<Boolean>("CustomButtons", false);
   public Setting<Boolean> startupMusic = new Setting<Boolean>("CustomMenuMusic", false);
   public Setting<Integer> maxFps = new Setting<Integer>("MaxFPS", 5, 5, 300, (v) -> true);
   public Setting<Background> background;
   public Setting<Integer> colorOne;
   public Setting<Integer> colorTwo;
   public Setting<BackgroundImage> backgroundImage;
   public Setting<MenuShaderType> menuShader;
   private MenuGLSLShader activeShaderInstance;
   private MenuShaderType loadedShaderType;

   public MainMenu() {
      super("MainMenu", "Changes Minecraft's Main Menu", Module.Category.CLIENT, true, false, true);
      this.background = new Setting<Background>("Background", MainMenu.Background.Shader);
      this.colorOne = (new Setting<Integer>("ColorOne", 5592405, 0, 16777215, (v) -> this.background.getValue() == MainMenu.Background.Color)).setColorPicker(true).setColorPickerDefaultGlobal(false);
      this.colorTwo = (new Setting<Integer>("ColorTwo", 986895, 0, 16777215, (v) -> this.background.getValue() == MainMenu.Background.Color)).setColorPicker(true).setColorPickerDefaultGlobal(false);
      this.backgroundImage = new Setting<BackgroundImage>("Image", MainMenu.BackgroundImage.Image2, (v) -> this.background.getValue() == MainMenu.Background.Image);
      this.menuShader = new Setting<MenuShaderType>("Shader", MainMenu.MenuShaderType.BlueWave, (v) -> this.background.getValue() == MainMenu.Background.Shader);
      this.register(this.customButtons);
      this.register(this.background);
      this.register(this.colorOne);
      this.register(this.colorTwo);
      this.register(this.backgroundImage);
      this.register(this.menuShader);
      this.register(this.startupMusic);
      this.register(this.maxFps);
      this.enabled.setValue(true);
   }

   public MenuGLSLShader getActiveShader() {
      if (this.background.getValue() != MainMenu.Background.Shader) {
         return null;
      } else {
         MenuShaderType desired = this.menuShader.getValue();
         if (this.activeShaderInstance == null || this.loadedShaderType != desired) {
            this.activeShaderInstance = null;
            this.loadedShaderType = desired;

            try {
               this.activeShaderInstance = new MenuGLSLShader(desired.path);
            } catch (Exception e) {
               e.printStackTrace();
            }
         }

         return this.activeShaderInstance;
      }
   }

   public void onDisable() {
      this.activeShaderInstance = null;
      this.loadedShaderType = null;
   }

   public static enum Background {
      Color,
      Image,
      Shader;
   }

   public static enum BackgroundImage {
      Image2;
   }

   public static enum MenuShaderType {
      LimestoneTunnel("/shaders/menu/limestone_tunnel.fsh"),
      Singularity("/shaders/menu/singularity.fsh"),
      BlackWarp("/shaders/menu/black_warp.fsh"),
      BlueWave("/shaders/menu/blue_wave.fsh");

      public final String path;

      private MenuShaderType(String path) {
         this.path = path;
      }
   }
}
