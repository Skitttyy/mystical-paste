package fat.vonware.features.modules.combat;

import fat.vonware.ducks.ICPacketUseEntity;
import fat.vonware.event.events.PacketEvent;
import fat.vonware.features.modules.Module;
import fat.vonware.features.setting.Setting;
import java.util.function.Function;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityEnderCrystal;
import net.minecraft.item.ItemSword;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.network.play.client.CPacketUseEntity;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class Criticals extends Module {
   protected final Setting<Mode> mode;
   protected final Setting<Boolean> onlySword;
   protected final Setting<Boolean> pauseWhileMoving;

   public Criticals() {
      super("Criticals", "Sword critical hit enhancer", Module.Category.COMBAT, true, false, false);
      this.mode = (new Setting("Mode", Criticals.Mode.NCP)).withDescription((Function<Mode, String>)((mode) -> {
         switch (mode) {
            case NCP:
               return "NCP: Sends a standard NCP-compatible packet sequence before attacking to force a critical hit.";
            case NCPStrict:
               return "NCPStrict: Uses a stricter NCP-compatible packet sequence.";
            case Jump:
               return "Jump: Performs a normal jump and attacks while airborne to trigger a critical hit.";
            case MiniJump:
               return "MiniJump: Performs a smaller jump than normal to trigger critical hits with less visible movement.";
            default:
               return "";
         }
      }));
      this.onlySword = new Setting<Boolean>("OnlySword", true, "Only applies critical-hit logic when you are holding a sword.");
      this.pauseWhileMoving = new Setting<Boolean>("PauseWhileMoving", false, "Pauses the critical-hit logic when you are moving.");
      this.register(this.mode);
      this.register(this.onlySword);
      this.register(this.pauseWhileMoving);
   }

   @SubscribeEvent
   public void onPacketSend(PacketEvent.Send event) {
      if (event.getPacket() instanceof CPacketUseEntity) {
         Entity entity = ((ICPacketUseEntity)event.getPacket()).getAttackedEntity();
         if (!(entity instanceof EntityEnderCrystal)) {
            if (!(Boolean)this.onlySword.getValue() || mc.player.getHeldItemMainhand().getItem() instanceof ItemSword) {
               if (!(Boolean)this.pauseWhileMoving.getValue() || mc.player.moveForward == 0.0F && mc.player.moveStrafing == 0.0F) {
                  if (this.mode.getValue() == Criticals.Mode.NCP) {
                     mc.getConnection().sendPacket(new CPacketPlayer.Position(mc.player.posX, mc.player.posY + (double)0.065F, mc.player.posZ, false));
                     mc.getConnection().sendPacket(new CPacketPlayer.Position(mc.player.posX, mc.player.posY, mc.player.posZ, false));
                  }

                  if (this.mode.getValue() == Criticals.Mode.NCPStrict) {
                     mc.player.connection.sendPacket(new CPacketPlayer.Position(mc.player.posX, mc.player.posY + (double)6.5E-7F, mc.player.posZ, false));
                     mc.player.connection.sendPacket(new CPacketPlayer.Position(mc.player.posX, mc.player.posY, mc.player.posZ, false));
                  }

                  if (this.mode.getValue() == Criticals.Mode.Jump) {
                     mc.player.jump();
                  }

                  if (this.mode.getValue() == Criticals.Mode.MiniJump) {
                     mc.player.jump();
                     EntityPlayerSP player = mc.player;
                     player.motionY /= (double)2.0F;
                  }

               }
            }
         }
      }
   }

   public String getDisplayInfo() {
      return ((Mode)this.mode.getValue()).name();
   }

   public static enum Mode {
      NCP,
      NCPStrict,
      Jump,
      MiniJump;
   }
}
