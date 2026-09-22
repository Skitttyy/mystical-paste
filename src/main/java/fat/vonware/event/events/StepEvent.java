package fat.vonware.event.events;

import fat.vonware.event.EventStage;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.fml.common.eventhandler.Cancelable;

@Cancelable
public class StepEvent extends EventStage {
   private final AxisAlignedBB axisAlignedBB;
   private float height;

   public StepEvent(AxisAlignedBB axisAlignedBB, float height) {
      this.axisAlignedBB = axisAlignedBB;
      this.height = height;
   }

   public AxisAlignedBB getAxisAlignedBB() {
      return this.axisAlignedBB;
   }

   public void setHeight(float in) {
      this.height = in;
   }

   public float getHeight() {
      return this.height;
   }
}
