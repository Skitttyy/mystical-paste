package fat.vonware.features.modules.elements;

import fat.vonware.Global;
import fat.vonware.Vonware;
import fat.vonware.event.events.Render2DEvent;
import fat.vonware.features.modules.HudModule;
import fat.vonware.features.modules.Module;
import fat.vonware.features.modules.client.FastLatency;
import fat.vonware.features.setting.Setting;
import fat.vonware.util.EntityUtil;
import fat.vonware.util.InventoryUtil;
import fat.vonware.util.OtherRenderShit.Network.NetworkUtil;
import java.awt.Color;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class DotGod extends HudModule {
   protected boolean isTargetTrapped;
   protected boolean targetExists;
   protected boolean isTargetInRange;
   public Setting<String> name = new Setting<String>("Name", "Mystic");
   public Setting<Boolean> watermark = new Setting<Boolean>("Watermark", false);

   public DotGod() {
      super("DotGod", "DotGod.cc", Module.Category.Elements, true, false, false, 100.0F, 50.0F);
      this.register(this.name);
      this.register(this.watermark);
   }

   @SubscribeEvent
   public void onRender2D(Render2DEvent event) {
      int offsetY = 0;
      this.rect.setX((Float)this.x.getValue());
      this.rect.setY((Float)this.y.getValue());
      float x = this.rect.getX();
      float y = this.rect.getY();
      EntityPlayer closestPlayer = EntityUtil.getClosestEnemy();
      this.targetExists = closestPlayer != null;
      this.isTargetTrapped = this.targetExists && EntityUtil.isTrapped(closestPlayer);
      this.isTargetInRange = this.targetExists;
      if ((Boolean)this.watermark.getValue()) {
         Vonware.textManager.drawStringWithShadow(this.name.getValue(), x, y, this.getNameColor());
         offsetY += 10;
      }

      Vonware.textManager.drawString("PLR", x, y + (float)offsetY, this.isTargetInRange ? Color.GREEN.getRGB() : Color.RED.getRGB(), true);
      offsetY += 10;
      Vonware.textManager.drawString("HTR", x, y + (float)offsetY, this.isTargetInRange ? Color.GREEN.getRGB() : Color.RED.getRGB(), true);
      offsetY += 10;
      int totemCount = InventoryUtil.getItemCount(Items.TOTEM_OF_UNDYING);
      Vonware.textManager.drawString(String.valueOf(totemCount), x, y + (float)offsetY, totemCount == 0 ? Color.RED.getRGB() : Color.GREEN.getRGB(), true);
      offsetY += 10;
      int ping = ((FastLatency)Vonware.moduleManager.getModuleByClass(FastLatency.class)).isEnabled() ? NetworkUtil.getLatency() : Vonware.serverManager.getPing();
      Vonware.textManager.drawString("PING " + ping, x, y + (float)offsetY, this.getPingColor(ping), true);
      offsetY += 10;
      Vonware.textManager.drawString("LBY", x, y + (float)offsetY, this.isTargetTrapped && this.targetExists ? Color.GREEN.getRGB() : Color.RED.getRGB(), true);
   }

   public void updateBounds() {
      this.rect.setX((Float)this.x.getValue());
      this.rect.setY((Float)this.y.getValue());
      int maxWidth = 0;
      if ((Boolean)this.watermark.getValue()) {
         maxWidth = Math.max(maxWidth, Vonware.textManager.getStringWidth(this.name.getValue()));
      }

      maxWidth = Math.max(maxWidth, Vonware.textManager.getStringWidth("PLR"));
      maxWidth = Math.max(maxWidth, Vonware.textManager.getStringWidth("HTR"));
      maxWidth = Math.max(maxWidth, Vonware.textManager.getStringWidth("999"));
      maxWidth = Math.max(maxWidth, Vonware.textManager.getStringWidth("PING 999"));
      maxWidth = Math.max(maxWidth, Vonware.textManager.getStringWidth("LBY"));
      int lineCount = 5;
      if ((Boolean)this.watermark.getValue()) {
         ++lineCount;
      }

      int totalHeight = lineCount * 10;
      this.rect.setWidth((float)maxWidth);
      this.rect.setHeight((float)totalHeight);
   }

   private int getNameColor() {
      return Global.getGlobalColorRainbow(0).getRGB();
   }

   private int getPingColor(int ping) {
      if (ping >= 0 && ping <= 80) {
         return Color.GREEN.getRGB();
      } else if (ping >= 81 && ping <= 125) {
         return Color.YELLOW.getRGB();
      } else {
         return ping >= 126 ? Color.RED.getRGB() : Color.WHITE.getRGB();
      }
   }
}
