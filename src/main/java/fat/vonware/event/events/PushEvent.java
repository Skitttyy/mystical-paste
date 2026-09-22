package fat.vonware.event.events;

import fat.vonware.event.EventStage;
import net.minecraft.entity.Entity;
import net.minecraftforge.fml.common.eventhandler.Cancelable;

@Cancelable
public class PushEvent extends EventStage {
   public Entity entity;
   public double x;
   public double y;
   public double z;
   public boolean airbone;
   public int type;

   public PushEvent(Entity entity, double x, double y, double z, boolean airbone, int type) {
      this.entity = entity;
      this.x = x;
      this.y = y;
      this.z = z;
      this.airbone = airbone;
      this.type = type;
   }

   public PushEvent(int type, Entity entity) {
      this.entity = entity;
      this.type = type;
   }
}
