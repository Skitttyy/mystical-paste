package fat.vonware.manager;

import fat.vonware.event.events.DeathEvent;
import fat.vonware.event.events.EntityWorldEvent;
import fat.vonware.event.events.TotemPopEvent;
import fat.vonware.features.Feature;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class TotemPopManager extends Feature {
   private static TotemPopManager INSTANCE;
   private final Map<String, Integer> popCounts = new HashMap();

   public TotemPopManager() {
      INSTANCE = this;
   }

   public static TotemPopManager getInstance() {
      return INSTANCE;
   }

   public void init() {
      MinecraftForge.EVENT_BUS.register(this);
   }

   public void onUnload() {
      MinecraftForge.EVENT_BUS.unregister(this);
   }

   public void clear() {
      this.popCounts.clear();
   }

   public int getPopCount(String name) {
      return (Integer)this.popCounts.getOrDefault(name, 0);
   }

   public int getPopCount(EntityPlayer player) {
      return this.getPopCount(player.getName());
   }

   public Map<String, Integer> getPopCounts() {
      return Collections.unmodifiableMap(this.popCounts);
   }

   public boolean hasPopped(String name) {
      return this.popCounts.containsKey(name);
   }

   @SubscribeEvent(
      priority = EventPriority.HIGHEST
   )
   public void onTotemPop(TotemPopEvent event) {
      if (!fullNullCheck()) {
         EntityPlayer player = event.getEntity();
         if (player != null) {
            String name = player.getName();
            this.popCounts.put(name, (Integer)this.popCounts.getOrDefault(name, 0) + 1);
         }
      }
   }

   @SubscribeEvent
   public void onVisualLeave(EntityWorldEvent.Remove event) {
      if (!fullNullCheck()) {
         Entity player = event.getEntity();
         if (player != null) {
            this.popCounts.remove(player.getName());
         }
      }
   }

   @SubscribeEvent
   public void onDeath(DeathEvent event) {
      if (!fullNullCheck()) {
         EntityPlayer player = event.getEntity();
         if (player != null) {
            this.popCounts.remove(player.getName());
         }
      }
   }
}
