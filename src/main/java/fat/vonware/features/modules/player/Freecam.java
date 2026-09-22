package fat.vonware.features.modules.player;

import fat.vonware.event.events.PacketEvent;
import fat.vonware.event.events.player.FreecamEvent;
import fat.vonware.features.modules.Module;
import fat.vonware.features.setting.Setting;
import fat.vonware.util.OtherRenderShit.Render.FreecamEntity;
import net.minecraft.entity.Entity;
import net.minecraft.network.play.client.CPacketUseEntity;
import net.minecraft.util.MovementInput;
import net.minecraft.util.MovementInputFromOptions;
import net.minecraftforge.client.event.RenderSpecificHandEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class Freecam extends Module {
   public static Freecam INSTANCE;
   private final Setting<Float> horizontalSpeed = new Setting<Float>("HorizontalSpeed", 1.0F, 0.1F, 5.0F, "Controls horizontal Freecam movement speed.");
   private final Setting<Float> verticalSpeed = new Setting<Float>("VerticalSpeed", 0.5F, 0.1F, 5.0F, "Controls vertical Freecam movement speed.");
   public final Setting<Boolean> showHands = new Setting<Boolean>("ShowHands", false, "Keeps your first-person hands visible while Freecam is active.");
   private Entity cachedActiveEntity;
   private int lastActiveTick;
   private Entity oldRenderEntity;
   private FreecamEntity camera;
   private final MovementInput cameraMovement;
   private final MovementInput playerMovement;

   public Freecam() {
      super("Freecam", "Allows you to view from a different perspective", Module.Category.PLAYER, true, false, false);
      this.register(this.horizontalSpeed);
      this.register(this.verticalSpeed);
      this.register(this.showHands);
      this.cachedActiveEntity = null;
      this.lastActiveTick = -1;
      this.oldRenderEntity = null;
      this.camera = null;
      this.cameraMovement = new MovementInputFromOptions(mc.gameSettings) {
         public void updatePlayerMoveState() {
            if (!Freecam.INSTANCE.isEnabled()) {
               this.moveStrafe = 0.0F;
               this.moveForward = 0.0F;
               this.forwardKeyDown = false;
               this.backKeyDown = false;
               this.leftKeyDown = false;
               this.rightKeyDown = false;
               this.jump = false;
               this.sneak = false;
            } else {
               super.updatePlayerMoveState();
            }
         }
      };
      this.playerMovement = new MovementInputFromOptions(mc.gameSettings) {
         public void updatePlayerMoveState() {
            if (Freecam.INSTANCE.isEnabled()) {
               this.moveStrafe = 0.0F;
               this.moveForward = 0.0F;
               this.forwardKeyDown = false;
               this.backKeyDown = false;
               this.leftKeyDown = false;
               this.rightKeyDown = false;
               this.jump = false;
               this.sneak = false;
            } else {
               super.updatePlayerMoveState();
            }
         }
      };
      INSTANCE = this;
   }

   public Entity getActiveEntity() {
      if (this.cachedActiveEntity == null) {
         this.cachedActiveEntity = mc.player;
      }

      int currentTick = mc.player.ticksExisted;
      if (this.lastActiveTick != currentTick) {
         this.lastActiveTick = currentTick;
         if (this.isEnabled()) {
            this.cachedActiveEntity = (Entity)(mc.getRenderViewEntity() == null ? mc.player : mc.getRenderViewEntity());
         } else {
            this.cachedActiveEntity = mc.player;
         }
      }

      return this.cachedActiveEntity;
   }

   @SubscribeEvent
   public void onWorldLoad(WorldEvent.Unload event) {
      mc.setRenderViewEntity(mc.player);
      this.toggle();
   }

   @SubscribeEvent
   public void onFreecam(FreecamEvent event) {
      event.setCanceled(true);
   }

   @SubscribeEvent
   public void onRenderHand(RenderSpecificHandEvent event) {
      if (this.isEnabled() && !(Boolean)this.showHands.getValue()) {
         event.setCanceled(true);
      }

   }

   @SubscribeEvent
   public void onTick(TickEvent.ClientTickEvent event) {
      if (!fullNullCheck()) {
         if (this.camera == null) {
            this.setEnabled(false);
         } else {
            this.camera.sethSpeed(((Number)this.horizontalSpeed.getValue()).floatValue());
            this.camera.setvSpeed(((Number)this.verticalSpeed.getValue()).floatValue());
         }
      }
   }

   public void onEnable() {
      super.onEnable();
      if (!fullNullCheck()) {
         this.camera = new FreecamEntity(((Number)this.horizontalSpeed.getValue()).floatValue(), ((Number)this.verticalSpeed.getValue()).floatValue());
         this.camera.movementInput = this.cameraMovement;
         mc.player.movementInput = this.playerMovement;
         mc.world.addEntityToWorld(-921, this.camera);
         this.oldRenderEntity = mc.getRenderViewEntity();
         mc.setRenderViewEntity(this.camera);
         mc.renderChunksMany = false;
      }
   }

   public Entity getCameraEntity() {
      return this.camera;
   }

   @SubscribeEvent
   public void onPacket(PacketEvent event) {
      if (!fullNullCheck()) {
         if (event.getPacket() instanceof CPacketUseEntity) {
            CPacketUseEntity packet = (CPacketUseEntity)event.getPacket();
            Entity entity = packet.getEntityFromWorld(mc.world);
            if (entity != null && entity == mc.player) {
               event.setCanceled(true);
            }

         }
      }
   }

   public void onDisable() {
      super.onDisable();
      if (!fullNullCheck()) {
         if (this.camera != null) {
            mc.world.removeEntity(this.camera);
         }

         this.camera = null;
         mc.player.movementInput = new MovementInputFromOptions(mc.gameSettings);
         mc.setRenderViewEntity(this.oldRenderEntity);
         mc.renderChunksMany = true;
      }
   }
}
