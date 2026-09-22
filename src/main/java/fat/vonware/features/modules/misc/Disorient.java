package fat.vonware.features.modules.misc;

import fat.vonware.features.modules.Module;
import fat.vonware.features.setting.Setting;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class Disorient extends Module {
   public Setting<Boolean> flippedVision = new Setting<Boolean>("Flipped Vision", true, "Flips the rendered view to disorient your screen.");
   public Setting<Boolean> blackScreen = new Setting<Boolean>("Black Screen", false, "Periodically covers the screen with a black overlay.");
   public Setting<Integer> blackScreenTime = new Setting<Integer>("Screen Time", 2, 1, 10, (v) -> (Boolean)this.blackScreen.getValue(), "How long the black-screen effect remains active.");
   public Setting<Integer> timeBetween = new Setting<Integer>("Time Between", 5, 1, 30, (v) -> (Boolean)this.blackScreen.getValue(), "Delay between each black-screen effect.");
   private long lastBlackScreenTime = 0L;
   private boolean isBlackScreenActive = false;
   private long blackScreenStartTime = 0L;

   public Disorient() {
      super("Disorient", "this module handicaps yourself", Module.Category.MISC, true, false, true);
      this.register(this.flippedVision);
      this.register(this.blackScreen);
      this.register(this.blackScreenTime);
      this.register(this.timeBetween);
   }

   @SubscribeEvent
   @SideOnly(Side.CLIENT)
   public void onCameraSetup(EntityViewRenderEvent.CameraSetup event) {
      if (this.isEnabled() && event.getEntity() instanceof EntityPlayer && (Boolean)this.flippedVision.getValue()) {
         event.setRoll(event.getRoll() + 180.0F);
      }

   }

   @SubscribeEvent
   public void onRenderGameOverlay(RenderGameOverlayEvent.Post event) {
      if (this.isEnabled() && (Boolean)this.blackScreen.getValue()) {
         if (event.getType() == ElementType.ALL) {
            long currentTime = System.currentTimeMillis();
            if (!this.isBlackScreenActive) {
               if (currentTime - this.lastBlackScreenTime >= (long)((Integer)this.timeBetween.getValue() * 1000)) {
                  this.isBlackScreenActive = true;
                  this.blackScreenStartTime = currentTime;
               }
            } else if (currentTime - this.blackScreenStartTime >= (long)((Integer)this.blackScreenTime.getValue() * 1000)) {
               this.isBlackScreenActive = false;
               this.lastBlackScreenTime = currentTime;
            }

            if (this.isBlackScreenActive) {
               ScaledResolution sr = new ScaledResolution(mc);
               Gui.drawRect(0, 0, sr.getScaledWidth(), sr.getScaledHeight(), -16777216);
            }

         }
      }
   }

   public void onDisable() {
      super.onDisable();
      this.isBlackScreenActive = false;
      this.lastBlackScreenTime = 0L;
   }
}
