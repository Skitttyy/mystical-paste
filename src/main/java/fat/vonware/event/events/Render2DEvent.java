package fat.vonware.event.events;

import fat.vonware.event.EventStage;
import net.minecraft.client.gui.ScaledResolution;

public class Render2DEvent extends EventStage {
   public float partialTicks;
   public ScaledResolution scaledResolution;

   public Render2DEvent(float partialTicks, ScaledResolution scaledResolution) {
      this.partialTicks = partialTicks;
      this.scaledResolution = scaledResolution;
   }

   public float getPartialTicks() {
      return this.partialTicks;
   }

   public void setPartialTicks(float partialTicks) {
      this.partialTicks = partialTicks;
   }

   public void setScaledResolution(ScaledResolution scaledResolution) {
      this.scaledResolution = scaledResolution;
   }

   public double getScreenWidth() {
      return this.scaledResolution.getScaledWidth_double();
   }

   public double getScreenHeight() {
      return this.scaledResolution.getScaledHeight_double();
   }
}
