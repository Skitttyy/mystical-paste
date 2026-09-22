package fat.vonware.features.modules.elements;

import fat.vonware.Vonware;
import fat.vonware.event.events.Render2DEvent;
import fat.vonware.features.modules.HudModule;
import fat.vonware.features.modules.Module;
import fat.vonware.features.setting.Setting;
import fat.vonware.util.EntityUtil;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class MotionDebug extends HudModule {
   public Setting<Boolean> autoPosition = new Setting<Boolean>("AutoPosition", false);

   public MotionDebug() {
      super("MotionDebug", "Dev debug :nerd:", Module.Category.Elements, true, false, false, 100.0F, 50.0F);
      this.register(this.autoPosition);
   }

   public boolean isAutoPositioned() {
      return (Boolean)this.autoPosition.getValue();
   }

   @SubscribeEvent
   public void onRender2D(Render2DEvent event) {
      if (!fullNullCheck()) {
         this.rect.setX((Float)this.x.getValue());
         this.rect.setY((Float)this.y.getValue());
         float x = this.rect.getX();
         float y = this.rect.getY();
         int yOffset = 0;
         this.drawText("OnGround: " + this.fixedText(mc.player.onGround), x, y + (float)yOffset);
         yOffset += 10;
         this.drawText("ServerOnGround: " + this.fixedText(Vonware.positionManager.isOnGround()), x, y + (float)yOffset);
         yOffset += 10;
         this.drawText("Sprint: " + this.fixedText(mc.player.isSprinting()), x, y + (float)yOffset);
         yOffset += 10;
         this.drawText("Sneaking: " + this.fixedText(mc.player.isSneaking()), x, y + (float)yOffset);
         yOffset += 10;
         this.drawText("IsMoving: " + this.fixedText(EntityUtil.isMoving()), x, y + (float)yOffset);
         yOffset += 10;
         this.drawText("Partial Phase: " + this.fixedText(EntityUtil.isPartialPhasing(mc.player)), x, y + (float)yOffset);
         yOffset += 10;
         this.drawText("Burrow Phase: " + this.fixedText(EntityUtil.isBurrowPhasing(mc.player)), x, y + (float)yOffset);
         yOffset += 10;
         this.drawText("Full Phase: " + this.fixedText(EntityUtil.isFullPhasing(mc.player)), x, y + (float)yOffset);
         yOffset += 10;
         this.drawText("TickLength: " + this.formatTickLength(mc.timer.tickLength), x, y + (float)yOffset);
      }
   }

   private String formatTickLength(float tickLength) {
      float normal = 50.0F;
      TextFormatting color = tickLength == normal ? TextFormatting.GREEN : (tickLength < normal ? TextFormatting.AQUA : TextFormatting.RED);
      return color + String.format("%.2f", tickLength) + TextFormatting.GRAY + " / " + TextFormatting.WHITE + normal;
   }

   private void drawText(String text, float x, float y) {
      Vonware.textManager.drawStringWithShadow(text, x, y, -1);
   }

   private String fixedText(boolean value) {
      return (value ? TextFormatting.GREEN : TextFormatting.RED) + String.valueOf(value);
   }
}
