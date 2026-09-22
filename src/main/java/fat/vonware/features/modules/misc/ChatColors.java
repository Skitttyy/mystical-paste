package fat.vonware.features.modules.misc;

import fat.vonware.event.events.PacketEvent;
import fat.vonware.features.modules.Module;
import fat.vonware.features.setting.Setting;
import java.util.function.Function;
import net.minecraft.network.play.client.CPacketChatMessage;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class ChatColors extends Module {
   private final Setting<Colour> color;
   private final Setting<Modifier> modifier;
   private final Setting<Boolean> space;
   private final Setting<Colour> doubleColorPrimary;
   private final Setting<Colour> doubleColorSecondary;
   private int lastRainbowColorIndex;
   private int lastTransColorIndex;
   private boolean lastDoubleColorPrimary;
   private final String[] disallowed;

   public ChatColors() {
      super("ChatColors", "Changes the color and style of chat messages. (by felix)", Module.Category.MISC, true, false, false);
      this.color = (new Setting("Color", ChatColors.Colour.None)).withDescription((Function<Colour, String>)((mode) -> {
         switch (mode) {
            case RGB:
               return "RGB: Applies the module's RGB color effect across the outgoing message.";
            case Rainbow:
               return "Rainbow: Applies the first rainbow chat effect.";
            case Rainbow2:
               return "Rainbow2: Applies the second rainbow chat effect.";
            case Rainbow3:
               return "Rainbow3: Applies the third rainbow chat effect.";
            case Trans:
               return "Trans: Applies the module's alternating trans-flag color sequence.";
            case DoubleColor:
               return "DoubleColor: Alternates between DoubleColorPrimary and DoubleColorSecondary.";
            case None:
               return "None: Sends chat without an added color effect.";
            case Black:
               return "Black: Applies Minecraft black formatting.";
            case DarkGray:
               return "DarkGray: Applies Minecraft dark gray formatting.";
            case Gray:
               return "Gray: Applies Minecraft gray formatting.";
            case DarkBlue:
               return "DarkBlue: Applies Minecraft dark blue formatting.";
            case Blue:
               return "Blue: Applies Minecraft blue formatting.";
            case DarkGreen:
               return "DarkGreen: Applies Minecraft dark green formatting.";
            case Green:
               return "Green: Applies Minecraft green formatting.";
            case DarkAqua:
               return "DarkAqua: Applies Minecraft dark aqua formatting.";
            case Aqua:
               return "Aqua: Applies Minecraft aqua formatting.";
            case DarkRed:
               return "DarkRed: Applies Minecraft dark red formatting.";
            case Red:
               return "Red: Applies Minecraft red formatting.";
            case DarkPurple:
               return "DarkPurple: Applies Minecraft dark purple formatting.";
            case Purple:
               return "Purple: Applies Minecraft light-purple formatting.";
            case Gold:
               return "Gold: Applies Minecraft gold formatting.";
            case Yellow:
               return "Yellow: Applies Minecraft yellow formatting.";
            default:
               return "";
         }
      }));
      this.modifier = (new Setting("Modifier", ChatColors.Modifier.None)).withDescription((Function<Modifier, String>)((mode) -> {
         switch (mode) {
            case None:
               return "None: Applies no additional text style.";
            case Bold:
               return "Bold: Makes outgoing chat text bold.";
            case Italic:
               return "Italic: Makes outgoing chat text italic.";
            case BoldItalic:
               return "BoldItalic: Applies both bold and italic formatting.";
            case LineThrough:
               return "LineThrough: Applies strikethrough formatting.";
            case Underline:
               return "Underline: Underlines outgoing chat text.";
            default:
               return "";
         }
      }));
      this.space = new Setting<Boolean>("Space", true, "Adds a space after the chat formatting prefix.");
      this.doubleColorPrimary = new Setting<Colour>("DoubleColorPrimary", ChatColors.Colour.Red, "Selects the first color used by DoubleColor mode.");
      this.doubleColorSecondary = new Setting<Colour>("DoubleColorSecondary", ChatColors.Colour.DarkRed, "Selects the second color used by DoubleColor mode.");
      this.lastRainbowColorIndex = 0;
      this.lastTransColorIndex = 0;
      this.lastDoubleColorPrimary = true;
      this.disallowed = new String[]{".", "/", ",", "-", "!"};
      this.register(this.color);
      this.register(this.modifier);
      this.register(this.space);
      this.register(this.doubleColorPrimary);
      this.register(this.doubleColorSecondary);
   }

   @SubscribeEvent
   public void onPacketSend(PacketEvent.Send event) {
      if (!fullNullCheck() && event.getPacket() instanceof CPacketChatMessage) {
         CPacketChatMessage message = (CPacketChatMessage)event.getPacket();
         if (this.isAllowed(message.getMessage())) {
            String theMessage = message.getMessage();
            String modifierCode = this.getModifierFromSetting(this.modifier.getValue());
            switch ((Colour)this.color.getValue()) {
               case RGB:
                  theMessage = this.applyRgbEffect(theMessage, modifierCode);
                  break;
               case Rainbow:
                  theMessage = this.getRainbowMessage(theMessage, modifierCode);
                  break;
               case Rainbow2:
                  theMessage = this.getRainbow2Message(theMessage, modifierCode);
                  break;
               case Rainbow3:
                  theMessage = this.getRainbow3Message(theMessage, modifierCode);
                  break;
               case Trans:
                  theMessage = this.getTransMessage(theMessage, modifierCode);
                  break;
               case DoubleColor:
                  theMessage = this.getDoubleColorMessage(theMessage, this.getCodeFromSetting(this.doubleColorPrimary.getValue()), this.getCodeFromSetting(this.doubleColorSecondary.getValue()), modifierCode);
                  break;
               default:
                  theMessage = this.getCodeFromSetting(this.color.getValue()) + modifierCode + ((Boolean)this.space.getValue() ? " " : "") + theMessage;
            }

            message.message = theMessage;
         }
      }

   }

   public String getDisplayInfo() {
      return this.color.getName();
   }

   public boolean isAllowed(String message) {
      for(String s : this.disallowed) {
         if (message.startsWith(s)) {
            return false;
         }
      }

      return true;
   }

   public String getCodeFromSetting(Colour value) {
      switch (value) {
         case None:
            return "";
         case Black:
            return "&0";
         case DarkGray:
            return "&8";
         case Gray:
            return "&7";
         case DarkBlue:
            return "&1";
         case Blue:
            return "&9";
         case DarkGreen:
            return "&2";
         case Green:
            return "&a";
         case DarkAqua:
            return "&3";
         case Aqua:
            return "&b";
         case DarkRed:
            return "&4";
         case Red:
            return "&c";
         case DarkPurple:
            return "&5";
         case Purple:
            return "&d";
         case Gold:
            return "&6";
         case Yellow:
            return "&e";
         default:
            return "";
      }
   }

   public String getModifierFromSetting(Modifier value) {
      switch (value) {
         case None:
            return "";
         case Bold:
            return "&l";
         case Italic:
            return "&o";
         case BoldItalic:
            return "&l&o";
         case LineThrough:
            return "&m";
         case Underline:
            return "&n";
         default:
            return "";
      }
   }

   private String applyRgbEffect(String message, String modifier) {
      String[] colours = new String[]{"&4", "&c", "&6", "&e", "&a", "&2", "&b", "&3", "&1", "&9", "&5", "&d"};
      StringBuilder newMessage = new StringBuilder();
      int colorIndex = 0;

      for(char c : message.toCharArray()) {
         newMessage.append(colours[colorIndex]).append(modifier).append(c);
         colorIndex = (colorIndex + 1) % colours.length;
      }

      return newMessage.toString();
   }

   private String getRainbowMessage(String message, String modifierCode) {
      StringBuilder rainbowMessage = new StringBuilder();
      String[] colors = new String[]{"&c", "&6", "&e", "&a", "&2", "&3", "&9", "&1"};
      int colorIndex = this.lastRainbowColorIndex;

      for(char c : message.toCharArray()) {
         if (c != ' ') {
            rainbowMessage.append(colors[colorIndex % colors.length]).append(modifierCode).append(c);
            ++colorIndex;
         } else {
            rainbowMessage.append(c);
         }
      }

      this.lastRainbowColorIndex = colorIndex % colors.length;
      return rainbowMessage.toString();
   }

   private String getRainbow2Message(String message, String modifierCode) {
      StringBuilder rainbowMessage = new StringBuilder();
      String[] colors = new String[]{"&4", "&c", "&6", "&e", "&2", "&a", "&b", "&3", "&9", "&1", "&5", "&d"};
      int colorIndex = this.lastRainbowColorIndex;

      for(char c : message.toCharArray()) {
         if (c != ' ') {
            rainbowMessage.append(colors[colorIndex % colors.length]).append(modifierCode).append(c);
            ++colorIndex;
         } else {
            rainbowMessage.append(c);
         }
      }

      this.lastRainbowColorIndex = colorIndex % colors.length;
      return rainbowMessage.toString();
   }

   private String getRainbow3Message(String message, String modifierCode) {
      StringBuilder rainbowMessage = new StringBuilder();
      String[] colors = new String[]{"&b", "&3", "&9", "&d", "&c", "&6", "&e", "&2", "&a"};
      int colorIndex = this.lastRainbowColorIndex;

      for(char c : message.toCharArray()) {
         if (c != ' ') {
            rainbowMessage.append(colors[colorIndex % colors.length]).append(modifierCode).append(c);
            ++colorIndex;
         } else {
            rainbowMessage.append(c);
         }
      }

      this.lastRainbowColorIndex = colorIndex % colors.length;
      return rainbowMessage.toString();
   }

   private String getTransMessage(String message, String modifierCode) {
      StringBuilder transMessage = new StringBuilder();
      String[] colors = new String[]{"&b", "&d", "&f", "&b", "&d", "&b"};
      int colorIndex = this.lastTransColorIndex;

      for(char c : message.toCharArray()) {
         if (c != ' ') {
            transMessage.append(colors[colorIndex % colors.length]).append(modifierCode).append(c);
            ++colorIndex;
         } else {
            transMessage.append(c);
         }
      }

      this.lastTransColorIndex = colorIndex % colors.length;
      return transMessage.toString();
   }

   private String getDoubleColorMessage(String message, String primaryColor, String secondaryColor, String modifierCode) {
      StringBuilder doubleColorMessage = new StringBuilder();
      boolean usePrimary = this.lastDoubleColorPrimary;

      for(char c : message.toCharArray()) {
         if (c != ' ') {
            doubleColorMessage.append(usePrimary ? primaryColor : secondaryColor).append(modifierCode).append(c);
            usePrimary = !usePrimary;
         } else {
            doubleColorMessage.append(c);
         }
      }

      this.lastDoubleColorPrimary = usePrimary;
      return doubleColorMessage.toString();
   }

   public static enum Colour {
      None,
      Black,
      DarkGray,
      Gray,
      DarkBlue,
      Blue,
      DarkGreen,
      Green,
      DarkAqua,
      Aqua,
      DarkRed,
      Red,
      DarkPurple,
      Purple,
      Gold,
      Yellow,
      RGB,
      Rainbow,
      Rainbow2,
      Rainbow3,
      Trans,
      DoubleColor;
   }

   public static enum Modifier {
      None,
      Bold,
      Italic,
      BoldItalic,
      LineThrough,
      Underline;
   }
}
