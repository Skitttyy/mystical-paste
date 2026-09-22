package fat.vonware.features.modules.client;

import fat.vonware.Global;
import fat.vonware.Vonware;
import fat.vonware.features.gui.components.items.buttons.render.ColorPickerConfig;
import fat.vonware.features.modules.Module;
import fat.vonware.features.setting.ColorSetting;
import fat.vonware.features.setting.Setting;
import fat.vonware.util.BibleQuoteUtil;
import fat.vonware.util.IconUtil;
import fat.vonware.util.Chat.ColorEnum;
import fat.vonware.util.Chat.ThreadUtil;
import fat.vonware.util.Manager.NiggaUtil;
import fat.vonware.util.OtherRenderShit.gui.SoundUtil;
import java.awt.Image;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.TrayIcon.MessageType;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.nio.ByteBuffer;
import javax.imageio.ImageIO;
import net.minecraft.network.play.client.CPacketChatMessage;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.ScreenshotEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import org.lwjgl.opengl.Display;

public class Manager extends Module {
   private static Manager INSTANCE;
   public Setting<Notifier> notifier;
   public ColorSetting mysticModuleColor;
   public ColorSetting mysticWasColor;
   public Setting<Boolean> friendNotifier;
   public Setting<Boolean> calcNetherite;
   public Setting<Boolean> gameTitleBoolean;
   public Setting<String> gameTitle;
   public Setting<Boolean> customIcon;
   public Setting<Icons> icon;
   public Setting<Boolean> gameTitleAnimate;
   public Setting<Boolean> unfocusedFPS;
   public Setting<Integer> fps;
   public Setting<Boolean> accountPos;
   public Setting<Integer> currentAccountPosY;
   public Setting<ColorEnum> accountTextColor;
   public Setting<Boolean> connectionInfo;
   public final Setting<Boolean> guideLines;
   public final Setting<Boolean> pixelMarginLines;
   public final Setting<Integer> marginPixels;
   public Setting<Boolean> bibleQuoteOnJoin;
   public Setting<WhisperMode> whisperNotifier;
   public Setting<Boolean> onlyFriends;
   public Setting<Boolean> replyHelper;
   public Setting<Boolean> screenshotHelper;
   public Setting<Boolean> screenshotSound;
   private int animationIndex;
   private int tickCounter;
   private boolean isAdding;
   private static final int ANIMATION_SPEED = 7;
   private Icons previousIcon;
   private boolean previousCustomIcon;
   private TrayIcon trayIcon;
   private String lastWhisperer;
   private long lastWhisperTime;
   private static final long WHISPER_TIMEOUT = 300000L;
   private boolean lastWhisperWasBlocked;

   public Manager() {
      super("Manager", "Changes settings (This Module cannot be disabled.)", Module.Category.CLIENT, true, false, true);
      this.notifier = new Setting<Notifier>("Theme", Manager.Notifier.Mystic);
      this.mysticModuleColor = new ColorSetting("Module Color", -1, true, (v) -> this.notifier.getValue() == Manager.Notifier.Mystic);
      this.mysticWasColor = new ColorSetting("Text Color", -1, false, (v) -> this.notifier.getValue() == Manager.Notifier.Mystic);
      this.friendNotifier = new Setting<Boolean>("Friend Notifier", false);
      this.calcNetherite = new Setting<Boolean>("CalcNetherite", false);
      this.gameTitleBoolean = new Setting<Boolean>("Game Title", false);
      this.gameTitle = new Setting<String>("Minecraft 1.12.2", "Minecraft 1.12.2", (v) -> (Boolean)this.gameTitleBoolean.getValue());
      this.customIcon = new Setting<Boolean>("Custom Icon", true);
      this.icon = new Setting<Icons>("Icon", Manager.Icons.Classic, (v) -> (Boolean)this.customIcon.getValue());
      this.gameTitleAnimate = new Setting<Boolean>("Animate Title", false, (v) -> (Boolean)this.gameTitleBoolean.getValue());
      this.unfocusedFPS = new Setting<Boolean>("UnfocusedFPS", false);
      this.fps = new Setting<Integer>("FPS-Amount", 30, 0, 240, (v) -> (Boolean)this.unfocusedFPS.getValue());
      this.accountPos = new Setting<Boolean>("Account info", true);
      this.currentAccountPosY = new Setting<Integer>("AccountPosY", 80, 0, 200, (v) -> (Boolean)this.accountPos.getValue());
      this.accountTextColor = new Setting<ColorEnum>("AccountTextColor", ColorEnum.WHITE, (v) -> (Boolean)this.accountPos.getValue());
      this.connectionInfo = new Setting<Boolean>("ConnectionInfo", true);
      this.guideLines = new Setting<Boolean>("HUDGuideLines", true);
      this.pixelMarginLines = new Setting<Boolean>("Pixel Margin Lines", false);
      this.marginPixels = new Setting<Integer>("Margin Pixels", 2, 0, 20, (v) -> true);
      this.bibleQuoteOnJoin = new Setting<Boolean>("Bible Quote", true);
      this.whisperNotifier = new Setting<WhisperMode>("Whisper Notifier", Manager.WhisperMode.None);
      this.onlyFriends = new Setting<Boolean>("Only Friends", false, (v) -> this.whisperNotifier.getValue() != Manager.WhisperMode.None);
      this.replyHelper = new Setting<Boolean>("Reply Helper", true);
      this.screenshotHelper = new Setting<Boolean>("Screenshot Helper", true);
      this.screenshotSound = new Setting<Boolean>("Screenshot Sound", true, (v) -> (Boolean)this.screenshotHelper.getValue());
      this.animationIndex = 0;
      this.tickCounter = 0;
      this.isAdding = true;
      this.lastWhisperer = "";
      this.lastWhisperTime = 0L;
      this.lastWhisperWasBlocked = false;
      this.register(this.notifier);
      this.register(this.mysticModuleColor);
      this.register(this.mysticWasColor);
      this.register(this.friendNotifier);
      this.register(this.calcNetherite);
      this.register(this.gameTitleBoolean);
      this.register(this.gameTitleAnimate);
      this.register(this.gameTitle);
      this.register(this.unfocusedFPS);
      this.register(this.fps);
      this.register(this.customIcon);
      this.register(this.icon);
      this.register(this.accountPos);
      this.register(this.currentAccountPosY);
      this.register(this.accountTextColor);
      this.register(this.connectionInfo);
      this.register(this.guideLines);
      this.register(this.pixelMarginLines);
      this.register(this.marginPixels);
      this.register(this.screenshotHelper);
      this.register(this.screenshotSound);
      this.register(this.bibleQuoteOnJoin);
      this.register(this.whisperNotifier);
      this.register(this.onlyFriends);
      this.register(this.replyHelper);
      this.previousIcon = this.icon.getValue();
      this.previousCustomIcon = (Boolean)this.customIcon.getValue();
      this.enabled.setValue(true);
      MinecraftForge.EVENT_BUS.register(this);
      INSTANCE = this;
      if (this.whisperNotifier.getValue() == Manager.WhisperMode.Windows) {
         this.initSystemTray();
      }

      if ((Boolean)this.customIcon.getValue()) {
         IconUtil.setWindowIcon(this.icon.getValue());
      }

   }

   @SubscribeEvent
   public void onScreenshot(ScreenshotEvent event) {
      if ((Boolean)this.screenshotHelper.getValue()) {
         File screenshotFile = event.getScreenshotFile();
         if (screenshotFile != null) {
            ThreadUtil.startDaemon("Screenshot-Helper", () -> {
               try {
                  long lastSize = -1L;
                  int attempts = 0;
                  int MAX_ATTEMPTS = 20;

                  while(attempts < 20) {
                     Thread.sleep(100L);
                     if (!screenshotFile.exists()) {
                        ++attempts;
                     } else {
                        long currentSize = screenshotFile.length();
                        if (currentSize > 0L && currentSize == lastSize) {
                           break;
                        }

                        lastSize = currentSize;
                        ++attempts;
                     }
                  }

                  if (!screenshotFile.exists() || screenshotFile.length() == 0L) {
                     return;
                  }

                  final BufferedImage image = ImageIO.read(screenshotFile);
                  if (image == null) {
                     return;
                  }

                  Transferable transferable = new Transferable() {
                     public DataFlavor[] getTransferDataFlavors() {
                        return new DataFlavor[]{DataFlavor.imageFlavor};
                     }

                     public boolean isDataFlavorSupported(DataFlavor flavor) {
                        return DataFlavor.imageFlavor.equals(flavor);
                     }

                     public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
                        if (!this.isDataFlavorSupported(flavor)) {
                           throw new UnsupportedFlavorException(flavor);
                        } else {
                           return image;
                        }
                     }
                  };
                  Toolkit.getDefaultToolkit().getSystemClipboard().setContents(transferable, (ClipboardOwner)null);
                  if ((Boolean)this.screenshotSound.getValue()) {
                     NiggaUtil.playNotificationSound();
                  }
               } catch (Exception e) {
                  e.printStackTrace();
               }

            });
         }
      }
   }

   public int getEffectiveColor(ColorSetting setting) {
      ColorPickerConfig.PickerState state = ColorPickerConfig.getState(setting, setting.colorPickerDefaultGlobal);
      return state.useGlobalColor ? Global.getGlobalColor().getRGB() : (Integer)setting.getValue();
   }

   public float getMarginPixels() {
      return ((Integer)this.marginPixels.getValue()).floatValue();
   }

   @SubscribeEvent
   public void onChatReceived(ClientChatReceivedEvent event) {
      if (this.whisperNotifier.getValue() != Manager.WhisperMode.None) {
         ITextComponent message = event.getMessage();
         String chatText = message.getUnformattedText();
         if (NiggaUtil.isWhisper(chatText)) {
            String playerName = NiggaUtil.extractPlayerName(chatText);
            if ((Boolean)this.replyHelper.getValue() && playerName != null && !playerName.isEmpty()) {
               this.lastWhisperer = playerName;
               this.lastWhisperTime = System.currentTimeMillis();
               this.lastWhisperWasBlocked = false;
            }

            if ((Boolean)this.onlyFriends.getValue() && !Vonware.friendManager.isFriend(playerName)) {
               return;
            }

            this.handleWhisperNotification(chatText);
         }

      }
   }

   public boolean wasLastWhisperBlocked() {
      return this.lastWhisperWasBlocked && this.hasRecentWhisper();
   }

   private void handleWhisperNotification(String message) {
      if (!Display.isActive()) {
         switch ((WhisperMode)this.whisperNotifier.getValue()) {
            case Sound:
               SoundUtil.playWhisperNotification();
               break;
            case Windows:
               this.showWindowsNotification(message);
            case None:
         }

      }
   }

   @SubscribeEvent
   public void onPlayerLoggedIn(FMLNetworkEvent.ClientConnectedToServerEvent event) {
      if ((Boolean)this.bibleQuoteOnJoin.getValue()) {
         ThreadUtil.startDaemonDelayed("Bible-Quote", 1000L, () -> {
            if (mc.player != null) {
               mc.addScheduledTask(() -> {
                  if (mc.player != null) {
                     mc.ingameGUI.getChatGUI().printChatMessage(BibleQuoteUtil.getRandomQuoteComponent());
                  }

               });
            }

         });
      }

   }

   public String getAccountTextColor() {
      return ((ColorEnum)this.accountTextColor.getValue()).getColor();
   }

   public int getAccountMessagePosY() {
      return (Integer)this.currentAccountPosY.getValue();
   }

   @SubscribeEvent
   public void onClientTick(TickEvent.ClientTickEvent event) {
      if (event.phase == Phase.END) {
         if ((Boolean)this.gameTitleBoolean.getValue()) {
            if ((Boolean)this.gameTitleAnimate.getValue()) {
               this.handleAnimatedTitle();
            } else {
               Display.setTitle(this.gameTitle.getValue());
            }
         }

         if (this.previousIcon != this.icon.getValue() || this.previousCustomIcon != (Boolean)this.customIcon.getValue()) {
            if (!(Boolean)this.customIcon.getValue()) {
               this.restoreMinecraftIcon();
            } else {
               IconUtil.setWindowIcon(this.icon.getValue());
            }

            this.previousIcon = this.icon.getValue();
            this.previousCustomIcon = (Boolean)this.customIcon.getValue();
         }

      }
   }

   public void setWhispererFromBlockedUnicode(String playerName) {
      if ((Boolean)this.replyHelper.getValue()) {
         if (playerName != null && !playerName.isEmpty()) {
            this.lastWhisperer = playerName;
            this.lastWhisperTime = System.currentTimeMillis();
            this.lastWhisperWasBlocked = true;
         }
      }
   }

   private void handleAnimatedTitle() {
      ++this.tickCounter;
      if (this.tickCounter >= 7) {
         this.tickCounter = 0;
         String fullTitle = this.gameTitle.getValue();
         this.animationIndex = Math.min(this.animationIndex, fullTitle.length());
         if (this.isAdding) {
            ++this.animationIndex;
            if (this.animationIndex >= fullTitle.length()) {
               this.isAdding = false;
               this.animationIndex = fullTitle.length();
            }
         } else {
            --this.animationIndex;
            if (this.animationIndex <= 0) {
               this.isAdding = true;
               this.animationIndex = 0;
            }
         }

         String currentTitle = fullTitle.substring(0, Math.max(0, Math.min(this.animationIndex, fullTitle.length())));
         Display.setTitle(currentTitle);
      }

   }

   private void restoreMinecraftIcon() {
      try {
         InputStream inputstream16 = mc.getResourceManager().getResource(new ResourceLocation("icons/icon_16x16.png")).getInputStream();
         InputStream inputstream32 = mc.getResourceManager().getResource(new ResourceLocation("icons/icon_32x32.png")).getInputStream();
         Display.setIcon(new ByteBuffer[]{IconUtil.readImageToBuffer(inputstream16), IconUtil.readImageToBuffer(inputstream32)});
         inputstream16.close();
         inputstream32.close();
      } catch (Exception var3) {
         Display.setIcon(new ByteBuffer[0]);
      }

   }

   public void alert(String name, boolean remove) {
      if (this.isEnabled() && (Boolean)this.friendNotifier.getValue() && mc.getConnection() != null) {
         String msg = remove ? "You've been Removed!" : "You've been Friended!";
         mc.getConnection().sendPacket(new CPacketChatMessage("/msg " + name + " " + msg));
      }

   }

   private void showWindowsNotification(String message) {
      if (SystemTray.isSupported()) {
         if (this.trayIcon == null) {
            this.initSystemTray();
         }

         if (this.trayIcon != null) {
            try {
               String displayMessage = message.length() > 100 ? message.substring(0, 97) + "..." : message;
               this.trayIcon.displayMessage("New Whisper", displayMessage, MessageType.INFO);
            } catch (Exception e) {
               e.printStackTrace();
            }
         }

      }
   }

   private void initSystemTray() {
      if (SystemTray.isSupported()) {
         try {
            SystemTray tray = SystemTray.getSystemTray();
            Image image = Toolkit.getDefaultToolkit().createImage(new byte[1]);
            this.trayIcon = new TrayIcon(image, "Vonware");
            this.trayIcon.setImageAutoSize(true);
            boolean alreadyAdded = false;

            for(TrayIcon icon : tray.getTrayIcons()) {
               if (icon == this.trayIcon) {
                  alreadyAdded = true;
                  break;
               }
            }

            if (!alreadyAdded) {
               tray.add(this.trayIcon);
            }
         } catch (Exception e) {
            e.printStackTrace();
            this.trayIcon = null;
         }

      }
   }

   public String getLastWhisperer() {
      return System.currentTimeMillis() - this.lastWhisperTime > 300000L ? "" : this.lastWhisperer;
   }

   public boolean hasRecentWhisper() {
      return !this.getLastWhisperer().isEmpty();
   }

   public boolean shouldShowReplyHelper(String input) {
      if (!(Boolean)this.replyHelper.getValue()) {
         return false;
      } else {
         return this.hasRecentWhisper() && (input.equals("/r") || input.startsWith("/r ") || input.equals("/w") || input.startsWith("/w "));
      }
   }

   public static Manager getInstance() {
      if (INSTANCE == null) {
         INSTANCE = new Manager();
      }

      return INSTANCE;
   }

   public static enum Notifier {
      Mystic,
      Iggyware;
   }

   public static enum Icons {
      Classic(new String[]{"textures/classic_16x16.png", "textures/classic_32x32.png"}),
      Kurumi(new String[]{"textures/kurumieye16x16.png", "textures/kurumieye32x32.png"});

      private final String[] iconFiles;

      private Icons(String[] iconFiles) {
         this.iconFiles = iconFiles;
      }

      public String[] getFiles() {
         return this.iconFiles;
      }
   }

   public static enum WhisperMode {
      Sound,
      Windows,
      None;
   }
}
