package fat.vonware.manager;

import fat.vonware.features.Feature;
import fat.vonware.features.modules.Module;
import fat.vonware.mixin.mixins.accessor.ITimer;
import fat.vonware.util.Timer;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class TimerManager extends Feature {
   float timer = 1.0F;
   boolean flagged = false;
   Timer flagTimer = new Timer();
   private int currentTick = 0;
   private int timerTicks = 0;
   private float tempTimer = 1.0F;
   private final Map<Module, Modifier> modifiers = new ConcurrentHashMap();
   private int globalTicks = 0;
   private boolean modifiersActive = false;

   public void init() {
      MinecraftForge.EVENT_BUS.register(this);
   }

   @SubscribeEvent
   public void onUpdate(TickEvent.ClientTickEvent e) {
      if (!fullNullCheck()) {
         if (this.flagged && this.flagTimer.passedMs(1000L)) {
            this.flagged = false;
         }

         ++this.globalTicks;
         this.modifiers.values().removeIf((m) -> m.endTick < this.globalTicks);
         if (!this.modifiers.isEmpty()) {
            Modifier winner = this.modifiers.values().stream().min(Comparator.comparingDouble((m) -> (double)m.tickLength)).orElse(null);
            if (winner != null) {
               ((ITimer)mc.timer).setTickLength(winner.tickLength);
               this.modifiersActive = true;
            }

         } else {
            if (this.modifiersActive) {
               ((ITimer)mc.timer).setTickLength(50.0F);
               this.modifiersActive = false;
            }

            if (this.timerTicks > 0) {
               if (++this.currentTick >= this.timerTicks) {
                  this.currentTick = 0;
                  this.timerTicks = 0;
                  this.set(this.timer);
               } else {
                  ((ITimer)mc.timer).setTickLength(50.0F / this.tempTimer);
               }
            }

         }
      }
   }

   public void modifyTimer(Module owner, float tickLength, int timeoutTicks) {
      this.modifiers.put(owner, new Modifier(tickLength, this.globalTicks + timeoutTicks));
   }

   public void resetTimer(Module owner) {
      this.modifiers.remove(owner);
   }

   public void unload() {
      MinecraftForge.EVENT_BUS.unregister(this);
      this.timer = 1.0F;
      this.modifiers.clear();
      this.modifiersActive = false;
      ((ITimer)mc.timer).setTickLength(50.0F);
   }

   public void set(float timer) {
      if (timer > 0.0F) {
         ((ITimer)mc.timer).setTickLength(50.0F / timer);
      }

   }

   public void setPhase(float timer) {
      if (timer > 0.0F) {
         this.timer = timer;
         ((ITimer)mc.timer).setTickLength(50.0F / timer);
      }

   }

   public float getTimer() {
      return this.timer;
   }

   public boolean isFlagged() {
      return this.flagged;
   }

   public void reset() {
      this.timer = 1.0F;
      this.timerTicks = 0;
      this.currentTick = 0;
      this.modifiers.clear();
      this.modifiersActive = false;
      ((ITimer)mc.timer).setTickLength(50.0F);
   }

   private static class Modifier {
      final float tickLength;
      final int endTick;

      Modifier(float tickLength, int endTick) {
         this.tickLength = tickLength;
         this.endTick = endTick;
      }
   }
}
