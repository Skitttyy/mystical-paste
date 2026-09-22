package fat.vonware.features.modules.player;

import fat.vonware.event.events.TurnEvent;
import fat.vonware.features.modules.Module;
import fat.vonware.features.setting.Setting;
import java.util.function.Function;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;

public class FreeLook extends Module {
   public final Setting<Mode> mode;
   public final Setting<Boolean> hold;
   private float dYaw;
   private float dPitch;
   private boolean holdActive;

   public FreeLook() {
      super("FreeLook", "3rd person view", Module.Category.PLAYER, true, false, true);
      this.mode = (new Setting("Mode", FreeLook.Mode.Normal)).withDescription((Function<Mode, String>)((mode) -> {
         switch (mode) {
            case Normal:
               return "Normal: Forces third-person view but keeps normal camera turning behavior.";
            case FreeLook:
               return "FreeLook: Lets you rotate the third-person camera independently without turning your player.";
            default:
               return "";
         }
      }));
      this.hold = new Setting<Boolean>("Hold", false, "Only keeps FreeLook enabled while its bind is being held.");
      this.holdActive = false;
      this.register(this.mode);
      this.register(this.hold);
   }

   public void onEnable() {
      this.dYaw = 0.0F;
      this.dPitch = 0.0F;
      this.holdActive = (Boolean)this.hold.getValue();
      if (!(Boolean)this.hold.getValue()) {
         mc.gameSettings.thirdPersonView = 1;
      }

   }

   public void onDisable() {
      mc.gameSettings.thirdPersonView = 0;
      this.holdActive = false;
   }

   private boolean isFreeLookActive() {
      if (!this.isEnabled()) {
         return false;
      } else {
         return !(Boolean)this.hold.getValue() ? true : this.holdActive;
      }
   }

   public void toggle() {
      if (!(Boolean)this.hold.getValue()) {
         super.toggle();
      }
   }

   @SubscribeEvent
   public void onClientTick(TickEvent.ClientTickEvent event) {
      if (event.phase == Phase.START) {
         if ((Boolean)this.hold.getValue()) {
            boolean keyDown = !this.getBind().isEmpty() && this.getBind().isDown();
            if (keyDown && !this.isEnabled()) {
               this.enable();
            } else if (!keyDown && this.isEnabled()) {
               this.disable();
               return;
            }

            this.holdActive = keyDown;
         }

         if (this.isEnabled()) {
            int expectedView = this.isFreeLookActive() ? 1 : 0;
            if (mc.gameSettings.thirdPersonView != expectedView) {
               mc.gameSettings.thirdPersonView = expectedView;
            }

         }
      }
   }

   @SubscribeEvent
   public void onCameraSetup(EntityViewRenderEvent.CameraSetup event) {
      if (this.isFreeLookActive()) {
         if (mc.gameSettings.thirdPersonView > 0) {
            if (this.mode.getValue() == FreeLook.Mode.FreeLook) {
               event.setYaw(event.getYaw() + this.dYaw);
               event.setPitch(event.getPitch() + this.dPitch);
            }
         }
      }
   }

   @SubscribeEvent
   public void onTurnEvent(TurnEvent event) {
      if (this.isFreeLookActive()) {
         if (mc.gameSettings.thirdPersonView > 0) {
            if (this.mode.getValue() == FreeLook.Mode.FreeLook) {
               this.dYaw += event.getYaw() * 0.15F;
               this.dPitch -= event.getPitch() * 0.15F;
               this.dPitch = MathHelper.clamp(this.dPitch, -90.0F, 90.0F);
               event.setCanceled(true);
            }
         }
      }
   }

   public static enum Mode {
      Normal,
      FreeLook;
   }
}
