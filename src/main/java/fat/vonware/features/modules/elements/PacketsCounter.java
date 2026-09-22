package fat.vonware.features.modules.elements;

import fat.vonware.Global;
import fat.vonware.Vonware;
import fat.vonware.event.events.PacketEvent;
import fat.vonware.event.events.Render2DEvent;
import fat.vonware.features.gui.newgui.newcomponents.NewComponent;
import fat.vonware.features.modules.HudModule;
import fat.vonware.features.modules.Module;
import fat.vonware.features.modules.client.ClickGui;
import fat.vonware.features.modules.client.Colors;
import fat.vonware.features.setting.Setting;
import fat.vonware.util.Timer;
import fat.vonware.util.Animations.render.ColorUtil;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class PacketsCounter extends HudModule {
   public Timer timer = new Timer();
   public int incoming = 0;
   public int outgoing = 0;
   public Setting<Boolean> autoPosition = new Setting<Boolean>("AutoPosition", false);

   public PacketsCounter() {
      super("PacketsCounter", "Counts the amount of packets sent and received per second", Module.Category.Elements, true, false, true, 100.0F, 50.0F);
      this.register(this.autoPosition);
   }

   public void onUpdate() {
      if (this.timer.passedS((double)1.0F)) {
         this.incoming = 0;
         this.outgoing = 0;
         this.timer.reset();
      }

   }

   public boolean isAutoPositioned() {
      return (Boolean)this.autoPosition.getValue();
   }

   @SubscribeEvent
   public void onPacketReceive(PacketEvent.Receive event) {
      ++this.incoming;
   }

   @SubscribeEvent
   public void onPacketSend(PacketEvent.Send event) {
      ++this.outgoing;
   }

   public void updateBounds() {
      if ((Boolean)this.autoPosition.getValue()) {
         this.rect.setX(69.0F);
         this.rect.setY(3.0F);
      } else {
         this.rect.setX((Float)this.x.getValue());
         this.rect.setY((Float)this.y.getValue());
      }

      String displayText = "Packets (Sent: " + this.outgoing + ") (Received: " + this.incoming + ")";
      int width = Vonware.textManager.getStringWidth(displayText);
      int height = Vonware.textManager.getFontHeight();
      this.rect.setWidth((float)width);
      this.rect.setHeight((float)(height + 4));
   }

   @SubscribeEvent
   public void onRender2D(Render2DEvent event) {
      Global.updatePulseProgress();
      if ((Boolean)this.autoPosition.getValue()) {
         this.rect.setX(69.0F);
         this.rect.setY(3.0F);
      } else {
         this.rect.setX((Float)this.x.getValue());
         this.rect.setY((Float)this.y.getValue());
      }

      float x = this.rect.getX();
      float y = this.rect.getY();
      int color;
      if ((Boolean)Colors.getInstance().rainbow.getValue()) {
         if (Colors.getInstance().rainbowMode.getValue() == Colors.RainbowMode.Static) {
            color = ColorUtil.rainbow(0).getRGB();
         } else {
            color = ColorUtil.rainbow((int)((float)NewComponent.counter1[0] * (Float)Colors.getInstance().rainbowHue.getValue())).getRGB();
         }
      } else if ((Boolean)ClickGui.getInstance().pulseColor.getValue()) {
         color = Global.getPulseColor(Global.getGlobalColor(), 0).getRGB();
      } else {
         color = Global.getGlobalColor().getRGB();
      }

      Vonware.textManager.drawStringWithShadow("Packets (Sent: " + this.outgoing + ") (Received: " + this.incoming + ")", (float)((int)x), (float)((int)y), color);
   }
}
