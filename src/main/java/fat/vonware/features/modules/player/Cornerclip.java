package fat.vonware.features.modules.player;

import fat.vonware.features.modules.Module;
import fat.vonware.features.setting.Setting;
import fat.vonware.util.Timer;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.util.math.MathHelper;

public class Cornerclip extends Module {
   public Setting<Boolean> disable = new Setting<Boolean>("Auto Disable", true, "Automatically disables Cornerclip after the configured amount of time.");
   public Setting<Float> disableAfter = new Setting<Float>("Disable-After", 1.0F, 0.1F, 5.0F, (v) -> (Boolean)this.disable.getValue(), "Time before Cornerclip automatically disables.");
   private final Timer timer = new Timer();
   private static Cornerclip INSTANCE = new Cornerclip();

   public Cornerclip() {
      super("Cornerclip", "clips you into a corner", Module.Category.PLAYER, true, false, false);
      this.register(this.disable);
      this.register(this.disableAfter);
      this.setInstance();
   }

   public String getDisplayInfo() {
      if (!(Boolean)this.disable.getValue()) {
         return null;
      } else {
         long elapsed = this.timer.getPassedTimeMs();
         long total = (long)((Float)this.disableAfter.getValue() * 1000.0F);
         long remaining = Math.max(total - elapsed, 0L);
         double seconds = (double)remaining / (double)1000.0F;
         return String.format("%.1fs", seconds);
      }
   }

   public static Cornerclip getInstance() {
      if (INSTANCE == null) {
         INSTANCE = new Cornerclip();
      }

      return INSTANCE;
   }

   private void setInstance() {
      INSTANCE = this;
   }

   public double roundToClosest(double num, double low, double high) {
      double d1 = num - low;
      double d2 = high - num;
      return d2 > d1 ? low : high;
   }

   public boolean isMoving() {
      return mc.gameSettings.keyBindForward.isKeyDown() || mc.gameSettings.keyBindBack.isKeyDown() || mc.gameSettings.keyBindLeft.isKeyDown() || mc.gameSettings.keyBindRight.isKeyDown();
   }

   public void onEnable() {
      this.timer.reset();
   }

   public void onUpdate() {
      if (!fullNullCheck() && !this.isMoving()) {
         if ((Boolean)this.disable.getValue() && this.timer.passedMs((long)((Float)this.disableAfter.getValue() * 1000.0F))) {
            this.disable();
         }

         if (mc.world.getCollisionBoxes(mc.player, mc.player.getEntityBoundingBox().grow(0.01, (double)0.0F, 0.01)).size() < 2) {
            mc.player.setPosition(this.roundToClosest(mc.player.posX, Math.floor(mc.player.posX) + 0.301, Math.floor(mc.player.posX) + 0.699), mc.player.posY, this.roundToClosest(mc.player.posZ, Math.floor(mc.player.posZ) + 0.301, Math.floor(mc.player.posZ) + 0.699));
         } else if (mc.player.ticksExisted % 5 == 0) {
            mc.player.setPosition(mc.player.posX + MathHelper.clamp(this.roundToClosest(mc.player.posX, Math.floor(mc.player.posX) + 0.241, Math.floor(mc.player.posX) + 0.759) - mc.player.posX, -0.03, 0.03), mc.player.posY, mc.player.posZ + MathHelper.clamp(this.roundToClosest(mc.player.posZ, Math.floor(mc.player.posZ) + 0.241, Math.floor(mc.player.posZ) + 0.759) - mc.player.posZ, -0.03, 0.03));
            mc.player.connection.sendPacket(new CPacketPlayer.Position(mc.player.posX, mc.player.posY, mc.player.posZ, true));
            mc.player.connection.sendPacket(new CPacketPlayer.Position(this.roundToClosest(mc.player.posX, Math.floor(mc.player.posX) + 0.23, Math.floor(mc.player.posX) + 0.77), mc.player.posY, this.roundToClosest(mc.player.posZ, Math.floor(mc.player.posZ) + 0.23, Math.floor(mc.player.posZ) + 0.77), true));
         }

      }
   }
}
