package fat.vonware.event.events.player;

import fat.vonware.event.EventStage;

public class PerspectiveEvent extends EventStage {
   private float aspect;

   public PerspectiveEvent(float aspect) {
      this.aspect = aspect;
   }

   public float getAspect() {
      return this.aspect;
   }

   public void setAspect(float aspect) {
      this.aspect = aspect;
   }
}
