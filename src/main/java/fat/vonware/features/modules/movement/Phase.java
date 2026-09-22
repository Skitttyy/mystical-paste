package fat.vonware.features.modules.movement;

import fat.vonware.event.events.MoveEvent;
import fat.vonware.features.modules.Module;
import fat.vonware.features.setting.Setting;
import fat.vonware.util.EntityUtil;
import fat.vonware.util.MovementUtil;
import fat.vonware.util.Timer;
import fat.vonware.util.Phase.PacketUtil;
import java.util.function.Function;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class Phase extends Module {
   public Setting<Boolean> shiftDown = new Setting<Boolean>("ShiftDown", true, "Toggles the ShiftDown feature used by Phase.");
   public Setting<Boolean> longerOnVoid = new Setting<Boolean>("LongerIfVoid", false, "Toggles the LongerIfVoid feature used by Phase.");
   public Setting<Boolean> antiVoid = new Setting<Boolean>("AntiVoid", true, "Toggles the AntiVoid feature used by Phase.");
   public Setting<Float> speed = new Setting<Float>("Speed", 3.0F, 1.0F, 10.0F, "Controls the speed value used by Phase.");
   public Setting<Float> delay = new Setting<Float>("Delay", 3.0F, 0.0F, 5.0F, "Adjusts the Delay offset used by Phase.");
   public Setting<Float> shiftDelay = new Setting<Float>("ShiftDelay", 4.0F, 0.0F, 5.0F, "Adjusts the ShiftDelay offset used by Phase.");
   public Setting<Integer> minCollisions = new Setting<Integer>("MinCollisions", 1, 0, 10, "Controls the MinCollisions value used by Phase.");
   protected final Setting<PacketType> type;
   protected final Timer timer;
   protected int collisions;
   protected final Timer shiftTimer;

   public Phase() {
      super("Phase", "Old strict.2b2tpvp.net phase (Now Patched)", Module.Category.MOVEMENT, true, false, false);
      this.type = (new Setting("Phase Mode", Phase.PacketType.UP)).withDescription((Function<PacketType, String>)((mode) -> {
         switch (mode) {
            case UP:
               return "UP: Sends the phase packet using the module's upward Y offset.";
            case DOWN:
               return "DOWN: Sends the phase packet using the downward Y offset.";
            case ZERO:
               return "ZERO: Uses the zero/neutral Y packet offset for phasing.";
            default:
               return "";
         }
      }));
      this.register(this.shiftDown);
      this.register(this.longerOnVoid);
      this.register(this.antiVoid);
      this.register(this.speed);
      this.register(this.delay);
      this.register(this.shiftDelay);
      this.register(this.minCollisions);
      this.register(this.type);
      this.timer = new Timer();
      this.shiftTimer = new Timer();
   }

   public String getDisplayInfo() {
      return ((PacketType)this.type.getValue()).toString();
   }

   @SubscribeEvent
   public void onPhase(MoveEvent event) {
      boolean isVoid = mc.player.posY == (double)1.0F && (Boolean)this.longerOnVoid.getValue() && !((Float)this.shiftDelay.getValue()).equals(this.shiftDelay.getMin());
      float shiftDelay = isVoid ? (Float)this.shiftDelay.getValue() * 2.0F : (Float)this.shiftDelay.getValue();
      if (mc.gameSettings.keyBindSneak.isKeyDown() && EntityUtil.isPhasing() && (Boolean)this.shiftDown.getValue() && mc.player.collidedVertically && this.shiftTimer.passed((long)(shiftDelay * 100.0F))) {
         if ((Boolean)this.antiVoid.getValue() && mc.player.posY == (double)1.0F) {
            return;
         }

         double offset = mc.player.posY - 0.0042123;
         mc.player.setPosition(mc.player.posX, offset, mc.player.posZ);
         this.sendPackets(mc.player.posX, offset, mc.player.posZ);
         mc.player.motionX = (double)0.0F;
         mc.player.motionY = (double)0.0F;
         event.setX(mc.player.motionZ = (double)0.0F);
         event.setY((double)0.0F);
         event.setZ((double)0.0F);
         this.shiftTimer.reset();
      }

   }

   @SubscribeEvent
   public void onMove(MoveEvent event) {
      boolean valid = EntityUtil.isPhasing() && !mc.player.isOnLadder() && mc.player.collidedHorizontally;
      if (valid) {
         ++this.collisions;
      } else {
         this.collisions = 0;
      }

      if (this.collisions >= (Integer)this.minCollisions.getValue() && valid && this.timer.passed((long)((Float)this.delay.getValue() * 100.0F))) {
         double[] movement = MovementUtil.directionSpeed((double)((Float)this.speed.getValue() / 100.0F));
         double xSpeed = mc.player.posX + movement[0];
         double zSpeed = mc.player.posZ + movement[1];
         mc.player.setPosition(xSpeed, mc.player.posY, zSpeed);
         this.sendPackets(xSpeed, mc.player.posY, zSpeed);
         mc.player.motionX = (double)0.0F;
         mc.player.motionY = (double)0.0F;
         event.setX(mc.player.motionZ = (double)0.0F);
         event.setY((double)0.0F);
         event.setZ((double)0.0F);
         this.timer.reset();
         this.collisions = 0;
      }

   }

   protected void sendPackets(double x, double y, double z) {
      PacketUtil.send(new CPacketPlayer.Position(x, y, z, true));
      if (!mc.isSingleplayer()) {
         PacketUtil.send(new CPacketPlayer.Position(x, y + ((PacketType)this.type.getValue()).getYPos(), z, true));
      }

   }

   public static enum PacketType {
      UP((double)1337.0F),
      DOWN((double)-1337.0F),
      ZERO((double)0.0F);

      private final double yPos;

      private PacketType(double yPos) {
         this.yPos = yPos;
      }

      public double getYPos() {
         return this.yPos;
      }
   }
}
