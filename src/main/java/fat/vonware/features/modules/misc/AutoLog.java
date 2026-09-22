package fat.vonware.features.modules.misc;

import fat.vonware.Vonware;
import fat.vonware.event.events.ConnectionEvent;
import fat.vonware.event.events.EntityWorldEvent;
import fat.vonware.features.modules.Module;
import fat.vonware.features.modules.combat.Offhand;
import fat.vonware.features.setting.Setting;
import fat.vonware.util.InventoryUtil;
import fat.vonware.util.MathUtil;
import fat.vonware.util.Timer;
import fat.vonware.util.Phase.PacketUtil;
import java.util.function.Function;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.CPacketHeldItemChange;
import net.minecraft.util.EnumHand;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

public class AutoLog extends Module {
   public Setting<LeaveMethod> leaveMethod;
   public Setting<Boolean> onRender;
   public Setting<Boolean> force;
   public Setting<Float> healthAmount;
   public Setting<Boolean> noTotems;
   public Setting<Integer> totemAmount;
   public Setting<Boolean> fakeMessage;
   private boolean duelSent;
   private boolean selfKilling;
   private final Timer portalTimer;

   public AutoLog() {
      super("AutoLog", "Auto logs you out", Module.Category.MISC, true, false, false);
      this.leaveMethod = (new Setting("LeaveMethod", AutoLog.LeaveMethod.Log)).withDescription((Function<LeaveMethod, String>)((mode) -> {
         switch (mode) {
            case Log:
               return "Log: Disconnects from the server when an AutoLog condition is triggered.";
            case Duel:
               return "Duel: Sends a duel request to the bot instead of immediately disconnecting.";
            default:
               return "";
         }
      }));
      this.onRender = new Setting<Boolean>("OnRender", false, "Triggers AutoLog when a non-friend player enters your render distance.");
      this.force = new Setting<Boolean>("ForceDisconnect", false, "Forces a disconnect using the module's alternate disconnect method.");
      this.healthAmount = new Setting<Float>("Health", 12.0F, 1.0F, 19.0F, "Health threshold used to trigger AutoLog.");
      this.noTotems = new Setting<Boolean>("NoTotems", false, "Uses your remaining totem count as part of the AutoLog trigger.");
      this.totemAmount = new Setting<Integer>("Totem Count", 1, 1, 6, (v) -> (Boolean)this.noTotems.getValue(), "Triggers AutoLog when your remaining totems reach this amount.");
      this.fakeMessage = new Setting<Boolean>("FakeKickMessage", false, "Uses a vanilla-looking connection error message when disconnecting.");
      this.duelSent = false;
      this.selfKilling = false;
      this.portalTimer = new Timer();
      this.register(this.leaveMethod);
      this.register(this.onRender);
      this.register(this.noTotems);
      this.register(this.totemAmount);
      this.register(this.healthAmount);
      this.register(this.force);
      this.register(this.fakeMessage);
   }

   @SubscribeEvent
   public void onConnection(ConnectionEvent event) {
      this.duelSent = false;
      this.selfKilling = false;
      this.portalTimer.reset();
   }

   @SubscribeEvent
   public void onAdd(EntityWorldEvent.Add event) {
      if (event.getEntity() instanceof EntityPlayer && !Vonware.friendManager.isFriend((EntityPlayer)event.getEntity()) && (Boolean)this.onRender.getValue() && event.getEntity() != mc.player) {
         String disconnectMessage = "Logged because -> [" + event.getEntity().getName() + "] came into render distance";
         this.trigger(disconnectMessage);
      }

   }

   @SubscribeEvent
   public void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
      if (mc.player != null && event.player == mc.player) {
         this.portalTimer.reset();
         this.duelSent = false;
      }

   }

   @SubscribeEvent
   public void onChat(ClientChatEvent event) {
      if (event.getMessage().equalsIgnoreCase("/kill")) {
         this.selfKilling = true;
      }

   }

   @SubscribeEvent
   public void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
      if (mc.player != null && event.player == mc.player) {
         this.selfKilling = false;
      }

   }

   public void onUpdate() {
      if (!mc.isSingleplayer() && mc.player != null && !mc.player.isDead && !(mc.player.getHealth() <= 0.0F)) {
         if (this.portalTimer.passedMs(1000L)) {
            int totems = this.getTotalTotems();
            String disconnectMessage = "Logged out with " + MathUtil.round((double)mc.player.getHealth(), 2) + " hearts and " + totems + " totems remaining";
            boolean belowThreshold;
            if (Offhand.INSTANCE != null && Offhand.INSTANCE.isEnabled()) {
               if (!(Boolean)this.noTotems.getValue()) {
                  belowThreshold = mc.player.getHealth() <= (Float)this.healthAmount.getValue() && totems == 0;
               } else {
                  belowThreshold = totems <= (Integer)this.totemAmount.getValue();
               }
            } else if (!(Boolean)this.noTotems.getValue()) {
               belowThreshold = mc.player.getHealth() <= (Float)this.healthAmount.getValue();
            } else {
               belowThreshold = mc.player.getHealth() <= (Float)this.healthAmount.getValue() && totems <= (Integer)this.totemAmount.getValue();
            }

            if (belowThreshold) {
               this.trigger(disconnectMessage);
            } else {
               this.duelSent = false;
            }

         }
      }
   }

   private int getTotalTotems() {
      int count = InventoryUtil.getItemCount(Items.TOTEM_OF_UNDYING);
      if (Offhand.INSTANCE != null && Offhand.INSTANCE.isEnabled()) {
         if (mc.player.getHeldItem(EnumHand.OFF_HAND).getItem() == Items.TOTEM_OF_UNDYING) {
            ++count;
         }

         ItemStack cursor = mc.player.inventory.getItemStack();
         if (!cursor.isEmpty() && cursor.getItem() == Items.TOTEM_OF_UNDYING) {
            ++count;
         }
      }

      return count;
   }

   private boolean isInventoryEmpty() {
      for(int i = 0; i <= 8; ++i) {
         if (!mc.player.inventory.getStackInSlot(i).isEmpty()) {
            return false;
         }
      }

      for(int i = 36; i <= 40; ++i) {
         if (!mc.player.inventory.getStackInSlot(i).isEmpty()) {
            return false;
         }
      }

      return true;
   }

   public void onDisable() {
      this.duelSent = false;
      this.selfKilling = false;
   }

   private void trigger(String disconnectMessage) {
      if (!this.selfKilling) {
         switch ((LeaveMethod)this.leaveMethod.getValue()) {
            case Log:
               this.disconnect(disconnectMessage);
               break;
            case Duel:
               if (!this.duelSent && !this.isInventoryEmpty() && !this.isSwapInFlight()) {
                  mc.player.sendChatMessage("/duel poopgaming781 send request");
                  this.duelSent = true;
               }
         }

      }
   }

   private boolean isSwapInFlight() {
      if (Offhand.INSTANCE != null && Offhand.INSTANCE.isEnabled()) {
         ItemStack cursor = mc.player.inventory.getItemStack();
         if (!cursor.isEmpty() && cursor.getItem() == Items.TOTEM_OF_UNDYING) {
            return true;
         } else {
            int totems = this.getTotalTotems();
            return (Boolean)this.noTotems.getValue() ? totems > (Integer)this.totemAmount.getValue() : totems > 0;
         }
      } else {
         return false;
      }
   }

   protected void disconnect(String disconnectMessage) {
      if ((Boolean)this.fakeMessage.getValue() && this.leaveMethod.getValue() == AutoLog.LeaveMethod.Log) {
         PacketUtil.disconnect("Internal Exception: java.io.IOException: An existing connection was forcibly closed by the remote host");
      } else if ((Boolean)this.force.getValue() && this.leaveMethod.getValue() == AutoLog.LeaveMethod.Log) {
         PacketUtil.send(new CPacketHeldItemChange(-1));
      } else {
         PacketUtil.disconnect(disconnectMessage);
      }

      this.disable();
   }

   public static enum LeaveMethod {
      Log,
      Duel;
   }
}
