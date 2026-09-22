package fat.vonware.manager;

import fat.vonware.event.events.PacketEvent;
import fat.vonware.util.Timer;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.client.Minecraft;
import net.minecraft.network.play.client.CPacketClickWindow;
import net.minecraft.network.play.client.CPacketEntityAction;
import net.minecraft.network.play.client.CPacketEntityAction.Action;
import net.minecraft.network.play.server.SPacketPlayerPosLook;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class NCPManager {
   public static final NCPManager INSTANCE = new NCPManager();
   private static final Minecraft mc = Minecraft.getMinecraft();
   private final AtomicLong lagTimer = new AtomicLong();
   private final Timer clickTimer = new Timer();
   private boolean endedSprint = false;
   private boolean endedSneak = false;
   private boolean windowClicks = false;
   private boolean strict = false;

   public void init() {
      MinecraftForge.EVENT_BUS.register(this);
   }

   public Timer getClickTimer() {
      return this.clickTimer;
   }

   public boolean isStrict() {
      return this.strict;
   }

   public void setStrict(boolean strict) {
      if (this.strict && !strict) {
         this.releaseMultiClick();
      }

      this.strict = strict;
   }

   public void startMultiClick() {
      this.windowClicks = true;
   }

   public void releaseMultiClick() {
      this.windowClicks = false;
      this.release();
   }

   public boolean passed(int ms) {
      return System.currentTimeMillis() - this.lagTimer.get() >= (long)ms;
   }

   public long getTimeStamp() {
      return this.lagTimer.get();
   }

   public void reset() {
      this.lagTimer.set(System.currentTimeMillis());
   }

   @SubscribeEvent(
      priority = EventPriority.HIGHEST
   )
   public void onPacketReceive(PacketEvent.Receive event) {
      if (event.getPacket() instanceof SPacketPlayerPosLook) {
         this.lagTimer.set(System.currentTimeMillis());
      }

   }

   @SubscribeEvent
   public void onWorldLoad(WorldEvent.Load event) {
      this.endedSneak = false;
      this.endedSprint = false;
      this.windowClicks = false;
   }

   @SubscribeEvent(
      priority = EventPriority.HIGH
   )
   public void onPacketSend(PacketEvent.Send event) {
      if (event.getPacket() instanceof CPacketClickWindow) {
         if (this.isStrict() && !event.isCanceled()) {
            if (mc.player != null) {
               if (mc.player.isActiveItemStackBlocking()) {
                  mc.playerController.onStoppedUsingItem(mc.player);
               }

               if (mc.player.isSneaking()) {
                  this.endedSneak = true;
                  mc.player.connection.sendPacket(new CPacketEntityAction(mc.player, Action.STOP_SNEAKING));
               }

               if (mc.player.isSprinting()) {
                  this.endedSprint = true;
                  mc.player.connection.sendPacket(new CPacketEntityAction(mc.player, Action.STOP_SPRINTING));
               }

            }
         }
      }
   }

   @SubscribeEvent(
      priority = EventPriority.HIGH
   )
   public void onPacketPost(PacketEvent.Post event) {
      if (event.getPacket() instanceof CPacketClickWindow) {
         this.clickTimer.resetDelay();
         if (!this.windowClicks && this.isStrict()) {
            this.release();
         }

      }
   }

   private void release() {
      if (this.endedSneak) {
         this.endedSneak = false;
         if (mc.player != null) {
            mc.player.connection.sendPacket(new CPacketEntityAction(mc.player, Action.START_SNEAKING));
         }
      }

      if (this.endedSprint) {
         this.endedSprint = false;
         if (mc.player != null) {
            mc.player.connection.sendPacket(new CPacketEntityAction(mc.player, Action.START_SPRINTING));
         }
      }

   }
}
