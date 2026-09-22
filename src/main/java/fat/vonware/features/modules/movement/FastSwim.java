package fat.vonware.features.modules.movement;

import fat.vonware.event.events.MoveEvent;
import fat.vonware.event.events.PacketEvent;
import fat.vonware.features.modules.Module;
import fat.vonware.features.setting.Setting;
import fat.vonware.util.MovementUtil;
import fat.vonware.util.Timer;
import net.minecraft.block.material.Material;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class FastSwim extends Module {
   protected final Setting<Double> vWater = new Setting<Double>("V-Water", (double)1.0F, 0.1, (double)20.0F, "Upward movement multiplier applied while fully submerged in water.");
   protected final Setting<Double> downWater = new Setting<Double>("Down-Water", (double)1.0F, 0.1, (double)20.0F, "Downward movement multiplier applied while descending in water.");
   protected final Setting<Double> hWater = new Setting<Double>("H-Water", (double)1.0F, 0.1, (double)20.0F, "Horizontal movement multiplier applied while swimming in water.");
   protected final Setting<Double> vLava = new Setting<Double>("Up-Lava", (double)1.0F, 0.1, (double)20.0F, "Upward movement multiplier applied while fully submerged in lava.");
   protected final Setting<Double> downLava = new Setting<Double>("Down-Lava", (double)1.0F, 0.1, (double)20.0F, "Downward movement multiplier applied while descending in lava.");
   protected final Setting<Double> hLava = new Setting<Double>("H-Lava", (double)1.0F, 0.1, (double)20.0F, "Horizontal movement multiplier applied while moving through lava.");
   protected final Setting<Double> vLavaPartial = new Setting<Double>("Up-Lava-Partial", 1.2, 0.1, (double)20.0F, "Upward multiplier used when only partially inside lava.");
   protected final Setting<Double> vWaterPartial = new Setting<Double>("Up-Water-Partial", 1.2, 0.1, (double)20.0F, "Upward multiplier used when only partially inside water.");
   protected final Setting<Boolean> strafe = new Setting<Boolean>("Strafe", false, "Uses directional strafing calculations instead of only scaling the current liquid motion.");
   protected final Setting<Boolean> fall = new Setting<Boolean>("Fall", false, "Allows FastSwim to apply its downward/falling liquid movement behavior.");
   protected final Setting<Boolean> accelerate = new Setting<Boolean>("Accelerate", false, "Gradually increases the liquid movement speed instead of immediately applying the full multiplier.");
   protected final Setting<Double> accelerateFactor = new Setting<Double>("Factor", 1.1, 0.1, (double)20.0F, "Controls how strongly FastSwim increases speed while Accelerate is enabled.");
   protected double lavaSpeed;
   protected double waterSpeed;
   private final Timer timer;

   public FastSwim() {
      super("FastSwim", "makes you swim faster in liquids", Module.Category.MOVEMENT, true, false, false);
      this.register(this.vWater);
      this.register(this.downWater);
      this.register(this.hWater);
      this.register(this.vLava);
      this.register(this.downLava);
      this.register(this.hLava);
      this.register(this.vLavaPartial);
      this.register(this.vWaterPartial);
      this.register(this.strafe);
      this.register(this.fall);
      this.register(this.accelerate);
      this.register(this.accelerateFactor);
      this.timer = new Timer();
   }

   @SubscribeEvent
   public void onHMovePacket(PacketEvent.Receive event) {
      this.waterSpeed = (Double)this.hWater.getValue();
      this.lavaSpeed = (Double)this.hLava.getValue();
   }

   @SubscribeEvent
   public void onMove(MoveEvent event) {
      if ((Boolean)this.strafe.getValue()) {
         if (!(Boolean)this.accelerate.getValue() && this.timer.passed(250L)) {
            if (!mc.player.onGround) {
               if (mc.player.isInsideOfMaterial(Material.LAVA)) {
                  boolean fullySubmerged = this.isFullySubmerged(Material.LAVA);
                  double upSpeed = fullySubmerged ? (Double)this.vLava.getValue() : (Double)this.vLavaPartial.getValue();
                  MovementUtil.strafe(event, (Double)this.hLava.getValue());
                  this.handleVerticalMovement(event, (Double)this.downLava.getValue(), upSpeed);
               } else if (mc.player.isInsideOfMaterial(Material.WATER)) {
                  boolean fullySubmerged = this.isFullySubmerged(Material.WATER);
                  double upSpeed = fullySubmerged ? (Double)this.vWater.getValue() : (Double)this.vWaterPartial.getValue();
                  MovementUtil.strafe(event, (Double)this.hWater.getValue());
                  this.handleVerticalMovement(event, (Double)this.downWater.getValue(), upSpeed);
               }
            }
         } else if ((Boolean)this.accelerate.getValue()) {
            if (!mc.player.onGround) {
               if (this.timer.passed(250L)) {
                  if (mc.player.isInsideOfMaterial(Material.LAVA)) {
                     this.lavaSpeed *= (Double)this.accelerateFactor.getValue();
                  } else if (mc.player.isInsideOfMaterial(Material.WATER)) {
                     this.waterSpeed *= (Double)this.accelerateFactor.getValue();
                  }
               }

               if (mc.player.isInsideOfMaterial(Material.LAVA)) {
                  boolean fullySubmerged = this.isFullySubmerged(Material.LAVA);
                  double upSpeed = fullySubmerged ? (Double)this.vLava.getValue() : (Double)this.vLavaPartial.getValue();
                  MovementUtil.strafe(event, this.lavaSpeed);
                  this.handleVerticalMovement(event, (Double)this.downLava.getValue(), upSpeed);
               } else if (mc.player.isInsideOfMaterial(Material.WATER)) {
                  boolean fullySubmerged = this.isFullySubmerged(Material.WATER);
                  double upSpeed = fullySubmerged ? (Double)this.vWater.getValue() : (Double)this.vWaterPartial.getValue();
                  MovementUtil.strafe(event, this.waterSpeed);
                  this.handleVerticalMovement(event, (Double)this.downWater.getValue(), upSpeed);
               }
            } else {
               this.waterSpeed = (Double)this.hWater.getValue();
               this.lavaSpeed = (Double)this.hLava.getValue();
            }
         }
      } else if (this.timer.passed(250L) && !mc.player.onGround) {
         if (mc.player.isInsideOfMaterial(Material.LAVA)) {
            boolean fullySubmerged = this.isFullySubmerged(Material.LAVA);
            double upSpeed = fullySubmerged ? (Double)this.vLava.getValue() : (Double)this.vLavaPartial.getValue();
            event.setX(event.getX() * (Double)this.hLava.getValue());
            event.setY(event.getY() * upSpeed);
            event.setZ(event.getZ() * (Double)this.hLava.getValue());
         } else if (mc.player.isInsideOfMaterial(Material.WATER)) {
            boolean fullySubmerged = this.isFullySubmerged(Material.WATER);
            double upSpeed = fullySubmerged ? (Double)this.vWater.getValue() : (Double)this.vWaterPartial.getValue();
            event.setX(event.getX() * (Double)this.hWater.getValue());
            event.setY(event.getY() * upSpeed);
            event.setZ(event.getZ() * (Double)this.hWater.getValue());
         }
      }

   }

   private boolean isFullySubmerged(Material material) {
      return mc.player.isInsideOfMaterial(material) && mc.world.getBlockState(mc.player.getPosition().up()).getMaterial() == material;
   }

   private void handleVerticalMovement(MoveEvent event, double downValue, double upValue) {
      if (!(Boolean)this.fall.getValue()) {
         if (mc.gameSettings.keyBindSneak.isKeyDown()) {
            event.setY(-downValue);
         } else if (mc.gameSettings.keyBindJump.isKeyDown()) {
            event.setY(upValue);
         } else {
            event.setY((double)0.0F);
         }
      }

   }
}
