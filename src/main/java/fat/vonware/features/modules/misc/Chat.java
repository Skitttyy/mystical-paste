package fat.vonware.features.modules.misc;

import com.mojang.realmsclient.gui.ChatFormatting;
import fat.vonware.Global;
import fat.vonware.features.modules.Module;
import fat.vonware.features.setting.ColorSetting;
import fat.vonware.features.setting.Setting;
import fat.vonware.util.HexColorText;
import fat.vonware.util.Animations.ChatAnimation;
import fat.vonware.util.Chat.ColorEnum;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Function;
import net.minecraft.client.gui.ChatLine;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class Chat extends Module {
   protected final Setting<ColorEnum> selfColor;
   public Setting<Boolean> customFont;
   public Setting<Boolean> customFontCharacters;
   public Setting<Float> alphaFactor;
   public Setting<Boolean> greenText;
   public Setting<Boolean> infinite;
   public Setting<Boolean> chatTimeStamps;
   public Setting<Boolean> soundOnMention;
   public Setting<Boolean> customColor;
   public final ColorSetting customBgColor;
   public Setting<Boolean> openCloseAnim;
   public Setting<Boolean> hideChat;
   public Setting<AnimationMode> animations;
   public Setting<Float> animationTime;
   public final Map<ChatLine, ChatAnimation> animationMap;
   private static Chat INSTANCE;
   protected final List<String> messages;

   public Chat() {
      super("Chat", "Chat modifier", Module.Category.MISC, true, false, false);
      this.selfColor = new Setting<ColorEnum>("SelfColor", ColorEnum.BLUE);
      this.customFont = new Setting<Boolean>("CustomFont", false, "Uses Mystic's custom font renderer for chat text.");
      this.customFontCharacters = new Setting<Boolean>("CustomFontCharacters", false, (v) -> (Boolean)this.customFont.getValue(), "Uses the custom font for characters typed in the chat input box.");
      this.alphaFactor = new Setting<Float>("Alpha Factor", 0.5F, 0.0F, 1.0F, "Controls the opacity multiplier used by the chat background.");
      this.greenText = new Setting<Boolean>("Green Text", false, "Automatically prefixes normal outgoing chat messages with >");
      this.infinite = new Setting<Boolean>("Infinite", false, "Removes the normal retained-chat-line limit.");
      this.chatTimeStamps = new Setting<Boolean>("ChatTimeStamps", true, "Adds a timestamp to incoming chat messages.");
      this.soundOnMention = new Setting<Boolean>("SoundOnMention", false, "Plays a notification sound when your name is mentioned in chat.");
      this.customColor = new Setting<Boolean>("CustomColor", false, "Uses a custom color for the chat background.");
      this.customBgColor = new ColorSetting("BgColor", 0, false, "Selects the custom chat background color.");
      this.openCloseAnim = new Setting<Boolean>("BGAnimation", false, "Animates the chat background when the chat screen opens or closes.");
      this.hideChat = new Setting<Boolean>("HideChat", false, "Hides normal player chat messages while leaving other supported messages visible.");
      this.animations = (new Setting("Animations", Chat.AnimationMode.UP)).withDescription((Function<AnimationMode, String>)((mode) -> {
         switch (mode) {
            case NONE:
               return "None: Displays new chat lines without an entry animation.";
            case UP:
               return "Up: Animates new chat lines upward into their normal position.";
            case SLIDE:
               return "Slide: Slides new chat lines into view using AnimationTime.";
            default:
               return "";
         }
      }));
      this.animationTime = (new Setting<Float>("AnimationTime", 0.3F, 0.05F, 1.0F, (v) -> this.animations.getValue() == Chat.AnimationMode.SLIDE, "Controls how long the Slide animation takes to complete.")).setStep(0.05F);
      this.animationMap = new WeakHashMap();
      this.register(this.customFont);
      this.register(this.customFontCharacters);
      this.register(this.alphaFactor);
      this.register(this.greenText);
      this.register(this.infinite);
      this.register(this.chatTimeStamps);
      this.register(this.soundOnMention);
      this.register(this.customColor);
      this.register(this.customBgColor);
      this.register(this.openCloseAnim);
      this.register(this.selfColor);
      this.register(this.hideChat);
      this.register(this.animations);
      this.register(this.animationTime);
      this.messages = new ArrayList();
      this.setInstance();
   }

   @SubscribeEvent
   public void onChatReceived(ClientChatReceivedEvent event) {
      if ((Boolean)this.hideChat.getValue()) {
         String fullMessage = event.getMessage().getUnformattedText();
         if (fullMessage.startsWith("<") && fullMessage.contains(">")) {
            event.setCanceled(true);
            return;
         }
      }

      if ((Boolean)this.chatTimeStamps.getValue()) {
         String c1 = ChatFormatting.WHITE.toString();
         String c2 = HexColorText.color(Global.getGlobalColor().getRGB());
         TextComponentString newTextComponentString = new TextComponentString(c1 + "<" + c2 + (new SimpleDateFormat("k:mm")).format(new Date()) + c1 + ">" + ChatFormatting.RESET + " ");
         newTextComponentString.appendSibling(event.getMessage());
         event.setMessage(newTextComponentString);
      }

   }

   public float getRectAlpha() {
      return (Float)this.alphaFactor.getValue();
   }

   public Setting<Boolean> getSoundOnMention() {
      return this.soundOnMention;
   }

   public Setting<ColorEnum> getSelfColor() {
      return this.selfColor;
   }

   public List<String> getMessages() {
      return this.messages;
   }

   private void setInstance() {
      INSTANCE = this;
   }

   public static Chat getInstance() {
      if (INSTANCE == null) {
         INSTANCE = new Chat();
      }

      return INSTANCE;
   }

   @SubscribeEvent
   public void onChatMessage(ClientChatEvent event) {
      String message = event.getMessage();
      String newMessage = message;
      if ((Boolean)this.greenText.getValue()) {
         if (message.startsWith("/") || message.startsWith("!") || message.startsWith("+") || message.startsWith(".") || message.startsWith(",") || message.startsWith("-")) {
            return;
         }

         newMessage = "> " + message;
      }

      event.setMessage(newMessage);
   }

   public static enum AnimationMode {
      NONE("None"),
      UP("Up"),
      SLIDE("Slide");

      private final String name;

      private AnimationMode(String name) {
         this.name = name;
      }

      public String getName() {
         return this.name;
      }

      public String toString() {
         return this.name;
      }
   }
}
