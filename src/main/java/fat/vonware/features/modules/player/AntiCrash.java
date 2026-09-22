package fat.vonware.features.modules.player;

import com.google.common.collect.Sets;
import com.mojang.realmsclient.gui.ChatFormatting;
import fat.vonware.event.events.PacketEvent;
import fat.vonware.features.command.Command;
import fat.vonware.features.modules.Module;
import fat.vonware.features.setting.Setting;
import java.util.Set;
import net.minecraft.init.SoundEvents;
import net.minecraft.network.play.server.SPacketParticles;
import net.minecraft.network.play.server.SPacketSoundEffect;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class AntiCrash extends Module {
   protected static final Set<SoundEvent> SOUNDS;
   public Setting<Boolean> particles = new Setting<Boolean>("Particles", true, "Blocks or limits malicious particle effects handled by AntiCrash.");
   public Setting<Boolean> sound = new Setting<Boolean>("Sound", true, "Blocks or limits malicious sound packets handled by AntiCrash.");

   public AntiCrash() {
      super("AntiCrash", "No crash", Module.Category.PLAYER, true, false, false);
      this.register(this.particles);
      this.register(this.sound);
   }

   @SubscribeEvent
   public void onPacket(PacketEvent event) {
      if (!fullNullCheck()) {
         if ((Boolean)this.particles.getValue() && event.getPacket() instanceof SPacketParticles) {
            SPacketParticles packet2 = (SPacketParticles)event.getPacket();
            if (packet2.getParticleCount() > 800) {
               Command.sendSilentMessage(ChatFormatting.DARK_AQUA + "[" + ChatFormatting.AQUA + "Mystic" + ChatFormatting.DARK_AQUA + "] A Player attempted to crash your game via Particles");
               event.setCanceled(true);
            }
         }

         if ((Boolean)this.sound.getValue() && event.getPacket() instanceof SPacketSoundEffect && SOUNDS.contains(((SPacketSoundEffect)event.getPacket()).getSound())) {
            event.setCanceled(true);
         }

      }
   }

   static {
      SOUNDS = Sets.newHashSet(new SoundEvent[]{SoundEvents.ITEM_ARMOR_EQUIP_GENERIC, SoundEvents.ITEM_ARMOR_EQIIP_ELYTRA, SoundEvents.ITEM_ARMOR_EQUIP_DIAMOND, SoundEvents.ITEM_ARMOR_EQUIP_IRON, SoundEvents.ITEM_ARMOR_EQUIP_GOLD, SoundEvents.ITEM_ARMOR_EQUIP_CHAIN, SoundEvents.ITEM_ARMOR_EQUIP_LEATHER});
   }
}
