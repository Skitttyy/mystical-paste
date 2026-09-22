package fat.vonware.features.modules.elements;

import fat.vonware.Vonware;
import fat.vonware.event.events.Render2DEvent;
import fat.vonware.features.gui.newgui.newcomponents.NewVonwareGui;
import fat.vonware.features.modules.HudModule;
import fat.vonware.features.modules.Module;
import fat.vonware.features.setting.ColorSetting;
import fat.vonware.features.setting.Setting;
import fat.vonware.util.Timer;
import fat.vonware.util.Animations.render.ColorUtil;
import net.minecraft.entity.item.EntityEnderPearl;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class PearlCooldown extends HudModule {
   protected final Timer timer = new Timer();
   private final Setting<Float> coolDown = new Setting<Float>("Cooldown", 15.0F, 1.0F, 30.0F);
   private final Setting<Boolean> autoPosition = new Setting<Boolean>("AutoPos", false);
   private final ColorSetting color = new ColorSetting("Color", 16777215, true);

   public PearlCooldown() {
      super("PearlCooldown", "Displays pearl cooldown", Module.Category.Elements, true, false, false, 100.0F, 50.0F);
      this.register(this.coolDown);
      this.register(this.autoPosition);
   }

   public boolean isAutoPositioned() {
      return (Boolean)this.autoPosition.getValue();
   }

   @SubscribeEvent
   public void onRender2D(Render2DEvent event) {
      if (mc.player != null && mc.world != null) {
         if ((Boolean)this.autoPosition.getValue()) {
            this.rect.setX(400.0F);
            this.rect.setY(103.0F);
         } else {
            this.rect.setX((Float)this.x.getValue());
            this.rect.setY((Float)this.y.getValue());
         }

         float xPos = this.rect.getX();
         float yPos = this.rect.getY();
         String displayText;
         if (mc.currentScreen instanceof NewVonwareGui && ((NewVonwareGui)mc.currentScreen).isHudEditor()) {
            displayText = "Cooldown preview";
         } else if (this.isCooldownOver()) {
            displayText = "";
         } else {
            float remaining = ((Float)this.coolDown.getValue() * 1000.0F - (float)this.timer.getPassedTimeMs()) / 1000.0F;
            displayText = String.format("Cooldown (%.1fs)", remaining);
         }

         int color = this.getColor();
         Vonware.textManager.drawStringWithShadow(displayText, xPos, yPos, color);
      }
   }

   private int getColor() {
      return ColorUtil.getResolvedColor(this.color).getRGB();
   }

   public void updateBounds() {
      if ((Boolean)this.autoPosition.getValue()) {
         this.rect.setX(400.0F);
         this.rect.setY(103.0F);
      } else {
         this.rect.setX((Float)this.x.getValue());
         this.rect.setY((Float)this.y.getValue());
      }

      String displayText;
      if (mc.currentScreen instanceof NewVonwareGui && ((NewVonwareGui)mc.currentScreen).isHudEditor()) {
         displayText = "Cooldown preview";
      } else if (this.isCooldownOver()) {
         displayText = "Cooldown (0.0s)";
      } else {
         float remaining = ((Float)this.coolDown.getValue() * 1000.0F - (float)this.timer.getPassedTimeMs()) / 1000.0F;
         displayText = String.format("Cooldown (%.1fs)", remaining);
      }

      int width = Vonware.textManager.getStringWidth(displayText);
      int height = Vonware.textManager.getFontHeight();
      this.rect.setWidth((float)width);
      this.rect.setHeight((float)(height + 4));
   }

   public void resetCooldown() {
      this.timer.reset();
   }

   public boolean isCooldownOver() {
      return this.timer.passed((long)((Float)this.coolDown.getValue() * 1000.0F));
   }

   @SubscribeEvent
   public void onEntityJoinWorld(EntityJoinWorldEvent event) {
      if (event.getEntity() instanceof EntityEnderPearl) {
         if (mc.world == null || mc.player == null) {
            return;
         }

         EntityEnderPearl pearl = (EntityEnderPearl)event.getEntity();
         double distance = pearl.getDistance(mc.player.posX, mc.player.posY + (double)mc.player.getEyeHeight(), mc.player.posZ);
         if (distance < (double)2.0F) {
            this.resetCooldown();
         }
      }

   }
}
