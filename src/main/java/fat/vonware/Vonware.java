package fat.vonware;

import fat.vonware.manager.BlurManager;
import fat.vonware.manager.ColorManager;
import fat.vonware.manager.CommandManager;
import fat.vonware.manager.ConfigManager;
import fat.vonware.manager.EventManager;
import fat.vonware.manager.FriendManager;
import fat.vonware.manager.ModuleManager;
import fat.vonware.manager.NCPManager;
import fat.vonware.manager.PositionManager;
import fat.vonware.manager.PredictionManager;
import fat.vonware.manager.ReloadManager;
import fat.vonware.manager.RotationManager;
import fat.vonware.manager.ScreenTransitionManager;
import fat.vonware.manager.ServerManager;
import fat.vonware.manager.SpeedManager;
import fat.vonware.manager.TextManager;
import fat.vonware.manager.TimerManager;
import fat.vonware.manager.TotemPopManager;
import fat.vonware.manager.WayPointsManager;
import fat.vonware.util.Animations.font.FontLoader;
import fat.vonware.util.Chat.GlobalExecutor;
import fat.vonware.util.OtherRenderShit.gui.SoundUtil;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.Display;

@Mod(
   modid = "vonware",
   name = "Vonware",
   version = "0.1"
)
public class Vonware {
   public static final String MODID = "vonware";
   public static final String MODNAME = "Minecraft";
   public static final String MODVER = "1.12.2";
   public static final Logger LOGGER = LogManager.getLogger("Vonware ");
   public static CommandManager commandManager;
   public static FriendManager friendManager;
   public static ModuleManager moduleManager;
   public static PositionManager positionManager;
   public static ColorManager colorManager;
   public static RotationManager rotationManager;
   public static SpeedManager speedManager;
   public static ReloadManager reloadManager;
   public static ConfigManager configManager;
   public static ServerManager serverManager;
   public static EventManager eventManager;
   public static TextManager textManager;
   public static TimerManager timerManager;
   public static PredictionManager predictionManager;
   public static BlurManager blurManager;
   public static WayPointsManager wayPointsManager;
   public static ScreenTransitionManager screenTransitionManager;
   public static NCPManager ncpManager;
   public static TotemPopManager totemPopManager;
   @Instance
   public static Vonware INSTANCE;
   private static boolean unloaded = false;

   public static void load() {
      LOGGER.info("\n\nLoading Vonware");
      unloaded = false;
      if (reloadManager != null) {
         reloadManager.unload();
         reloadManager = null;
      }

      FontLoader.registerFont("Lora-Regular.ttf");
      FontLoader.registerFont("Manrope-Regular.ttf");
      textManager = new TextManager();
      commandManager = new CommandManager();
      positionManager = new PositionManager();
      friendManager = new FriendManager();
      moduleManager = new ModuleManager();
      rotationManager = new RotationManager();
      eventManager = new EventManager();
      speedManager = new SpeedManager();
      serverManager = new ServerManager();
      colorManager = new ColorManager();
      configManager = new ConfigManager();
      timerManager = new TimerManager();
      predictionManager = new PredictionManager();
      blurManager = new BlurManager();
      wayPointsManager = new WayPointsManager();
      screenTransitionManager = new ScreenTransitionManager();
      totemPopManager = new TotemPopManager();
      ncpManager = new NCPManager();
      LOGGER.info("Managers loaded.");
      moduleManager.init();
      commandManager.registerModuleCommands();
      LOGGER.info("Modules loaded.");
      ncpManager.init();
      configManager.init();
      eventManager.init();
      timerManager.init();
      positionManager.init();
      rotationManager.init();
      totemPopManager.init();
      blurManager.init();
      LOGGER.info("EventManager loaded.");
      moduleManager.onLoad();
      SoundUtil.registerSounds();
      LOGGER.info("Vonware successfully loaded!\n");
      Display.setTitle("Minecraft 1.12.2");
   }

   public static void unload(boolean unload) {
      LOGGER.info("\n\nUnloading Vonware");
      if (unload) {
         reloadManager = new ReloadManager();
         reloadManager.init(commandManager != null ? commandManager.getPrefix() : ".");
      }

      onUnload();
      eventManager = null;
      friendManager = null;
      speedManager = null;
      rotationManager = null;
      configManager = null;
      commandManager = null;
      colorManager = null;
      serverManager = null;
      moduleManager = null;
      textManager = null;
      LOGGER.info("Vonware unloaded!\n");
   }

   public static void reload() {
      unload(false);
      load();
   }

   public static void onUnload() {
      if (!unloaded) {
         try {
            GlobalExecutor.shutdownNow();
         } catch (Throwable var1) {
         }

         eventManager.onUnload();
         moduleManager.onUnload();
         configManager.saveConfig(configManager.currentConfigName);
         moduleManager.onUnloadPost();
         unloaded = true;
      }

   }

   @EventHandler
   public void init(FMLInitializationEvent event) {
      load();
   }
}
