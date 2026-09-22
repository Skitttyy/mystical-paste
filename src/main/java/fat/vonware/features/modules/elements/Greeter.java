package fat.vonware.features.modules.elements;

import fat.vonware.Global;
import fat.vonware.Vonware;
import fat.vonware.event.events.Render2DEvent;
import fat.vonware.features.modules.HudModule;
import fat.vonware.features.modules.Module;
import fat.vonware.features.modules.client.ClickGui;
import fat.vonware.features.modules.client.Colors;
import fat.vonware.features.modules.client.Media;
import fat.vonware.features.setting.Setting;
import fat.vonware.util.MathUtil;
import fat.vonware.util.Animations.render.ColorUtil;
import java.awt.Color;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class Greeter extends HudModule {
   public final Setting<AnimationMode> animation;
   public final Setting<GreeterMode> greeter;
   private final Setting<String> spoofGreeter;
   public Setting<Boolean> white;
   public Setting<Boolean> twentyFour;
   public Setting<Boolean> seconds;
   private float animOffset;
   private boolean movingRight;

   public Greeter() {
      super("Greeter", "Displays a greeting message", Module.Category.Elements, true, false, false, 100.0F, 50.0F);
      this.animation = new Setting<AnimationMode>("Animation", Greeter.AnimationMode.None);
      this.greeter = new Setting<GreeterMode>("Greeter", Greeter.GreeterMode.Name);
      this.spoofGreeter = new Setting<String>("GreeterName", "Meow", (v) -> this.greeter.getValue() == Greeter.GreeterMode.Custom);
      this.white = new Setting<Boolean>("White Name", false);
      this.twentyFour = new Setting<Boolean>("24-Hour Notation", false, (v) -> this.greeter.getValue() == Greeter.GreeterMode.RealTime);
      this.seconds = new Setting<Boolean>("Show Seconds", false, (v) -> this.greeter.getValue() == Greeter.GreeterMode.RealTime);
      this.animOffset = 0.0F;
      this.movingRight = true;
      this.register(this.greeter);
      this.register(this.animation);
      this.register(this.spoofGreeter);
      this.register(this.white);
      this.register(this.twentyFour);
      this.register(this.seconds);
   }

   public boolean isAutoPositioned() {
      return true;
   }

   @SubscribeEvent
   public void onRender2D(Render2DEvent event) {
      super.onRender2D(event);
      Global.updatePulseProgress();
      float yPos = 2.0F;
      String text = this.getText();
      float baseX = (float)((event.getScreenWidth() - (double)this.renderer.getStringWidth(text)) / (double)2.0F);
      float xPos;
      switch ((AnimationMode)this.animation.getValue()) {
         case SideToSide:
            if (this.movingRight) {
               ++this.animOffset;
            } else {
               --this.animOffset;
            }

            float maxOffset = (float)((event.getScreenWidth() - (double)this.renderer.getStringWidth(text)) / (double)2.0F);
            if (this.animOffset >= maxOffset) {
               this.animOffset = maxOffset;
               this.movingRight = false;
               this.animOffset -= 2.0F;
            }

            if (this.animOffset <= -maxOffset) {
               this.animOffset = -maxOffset;
               this.movingRight = true;
               this.animOffset += 2.0F;
            }

            xPos = baseX + this.animOffset;
            break;
         case Scroll:
            ++this.animOffset;
            if ((double)this.animOffset > event.getScreenWidth()) {
               this.animOffset = (float)(-this.renderer.getStringWidth(text));
            }

            xPos = this.animOffset;
            break;
         case None:
         default:
            xPos = baseX;
      }

      if ((Boolean)Colors.getInstance().rainbow.getValue()) {
         if (Colors.getInstance().rainbowMode.getValue() == Colors.RainbowMode.Static) {
            this.renderer.drawString(text, xPos, yPos, ColorUtil.rainbow(0).getRGB(), true);
         } else {
            this.renderRainbowText(text, xPos, yPos);
         }
      } else if ((Boolean)ClickGui.getInstance().pulseColor.getValue()) {
         int color = Global.getPulseColor(Global.getGlobalColor(), 0).getRGB();
         this.renderer.drawString(text, xPos, yPos, color, true);
      } else {
         Color currentColor = Colors.getInstance().getCurrentColor();
         int color = ColorUtil.toRGBA(currentColor.getRed(), currentColor.getGreen(), currentColor.getBlue());
         this.renderer.drawString(text, xPos, yPos, color, true);
      }

   }

   private String getText() {
      String name = this.getSpoofedName();
      if ((Boolean)this.white.getValue()) {
         name = TextFormatting.WHITE + name + TextFormatting.RESET;
      }

      switch ((GreeterMode)this.greeter.getValue()) {
         case GameTime:
            return MathUtil.getTimeOfDay() + name;
         case RealTime:
            return this.getRealTime();
         case Hello:
            return "Hello, " + name;
         case Custom:
            return ((String)this.spoofGreeter.getValue()).replace("!player", name);
         default:
            return "Welcome, " + name + " :^)";
      }
   }

   private String getRealTime() {
      LocalTime currentTime = LocalTime.now();
      DateTimeFormatter formatter;
      if ((Boolean)this.twentyFour.getValue()) {
         formatter = (Boolean)this.seconds.getValue() ? DateTimeFormatter.ofPattern("HH:mm:ss") : DateTimeFormatter.ofPattern("HH:mm");
      } else {
         formatter = (Boolean)this.seconds.getValue() ? DateTimeFormatter.ofPattern("hh:mm:ss a") : DateTimeFormatter.ofPattern("hh:mm a");
      }

      return currentTime.format(formatter);
   }

   private void renderRainbowText(String text, float xPos, float yPos) {
      float offset = 0.0F;
      int index = 0;

      for(char c : text.toCharArray()) {
         int color = ColorUtil.rainbow((int)((float)index * (Float)Colors.getInstance().rainbowHue.getValue())).getRGB();
         this.renderer.drawString(String.valueOf(c), xPos + offset, yPos, color, true);
         offset += (float)this.renderer.getStringWidth(String.valueOf(c));
         ++index;
      }

   }

   public String getSpoofedName() {
      Media nameHiderModule = (Media)Vonware.moduleManager.getModuleByClass(Media.class);
      return nameHiderModule != null && nameHiderModule.isEnabled() ? (String)nameHiderModule.newName.getValue() : mc.player.getName();
   }

   public static enum GreeterMode {
      Name,
      GameTime,
      RealTime,
      Hello,
      Custom;
   }

   public static enum AnimationMode {
      None,
      SideToSide,
      Scroll;
   }
}
