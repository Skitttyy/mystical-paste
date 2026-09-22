package fat.vonware.features.modules.movement;

import fat.vonware.event.events.PacketEvent;
import fat.vonware.features.modules.Module;
import fat.vonware.features.setting.Setting;
import fat.vonware.mixin.mixins.accessor.IEntity;
import fat.vonware.util.PlayerUtil;
import java.util.function.Function;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.network.play.client.CPacketClickWindow;
import net.minecraft.network.play.client.CPacketEntityAction;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.network.play.client.CPacketEntityAction.Action;
import net.minecraft.util.MovementInput;
import net.minecraftforge.client.event.InputUpdateEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

public class NoSlow extends Module {
   public static NoSlow INSTANCE;
   public Setting<Mode> mode;
   public Setting<Boolean> guiMove;
   public Setting<Boolean> gui;
   public Setting<Boolean> airStrict;
   boolean sneakStopped;
   boolean sprintStopped;

   public NoSlow() {
      super("NoSlow", "Prevents slowdown", Module.Category.MOVEMENT, true, false, false);
      this.mode = (new Setting("Mode", NoSlow.Mode.Cancel)).withDescription((Function<Mode, String>)((mode) -> {
         switch (mode) {
            case Cancel:
               return "Cancel: Cancels the slowdown behavior while using items.";
            case Strict:
               return "Strict: Cancels slowdown while also handling inventory click packets with the module's stricter packet logic.";
            default:
               return "";
         }
      }));
      this.guiMove = new Setting<Boolean>("Inventory Move", true, "Allows movement keys to continue working while inventory/container GUIs are open.");
      this.gui = new Setting<Boolean>("Portal Move", true, "Allows movement while the player is inside a portal screen/state.");
      this.airStrict = new Setting<Boolean>("Air Strict", true, "Applies the stricter NoSlow handling while airborne.");
      this.register(this.mode);
      this.register(this.guiMove);
      this.register(this.gui);
      this.register(this.airStrict);
      INSTANCE = this;
   }

   public void onUpdate() {
      if (mc.player != null && mc.world != null) {
         if ((Boolean)this.gui.getValue()) {
            ((IEntity)mc.player).setInPortal(false);
         }

      }
   }

   @SubscribeEvent
   public void onPacketSend(PacketEvent.Send event) {
      if (!fullNullCheck()) {
         if (!event.isCanceled()) {
            if (event.getPacket() instanceof CPacketClickWindow && this.mode.getValue() == NoSlow.Mode.Strict) {
               this.doStrictPre();
            }

         }
      }
   }

   @SubscribeEvent
   public void onPacketPost(PacketEvent.Post event) {
      if (!fullNullCheck()) {
         if (!event.isCanceled()) {
            if (event.getPacket() instanceof CPacketClickWindow && this.mode.getValue() == NoSlow.Mode.Strict) {
               this.doStrictPost();
            }

         }
      }
   }

   public boolean doStrictPre() {
      if (mc.player.isHandActive()) {
         mc.playerController.onStoppedUsingItem(mc.player);
      }

      if (mc.player.isSneaking()) {
         mc.player.connection.sendPacket(new CPacketEntityAction(mc.player, Action.STOP_SNEAKING));
         this.sneakStopped = true;
      }

      if (mc.player.isSprinting()) {
         mc.player.connection.sendPacket(new CPacketEntityAction(mc.player, Action.STOP_SPRINTING));
         this.sprintStopped = true;
      }

      if ((Boolean)this.airStrict.getValue() && PlayerUtil.isMoving() && !mc.gameSettings.keyBindJump.isKeyDown() && mc.world.getCollisionBoxes(mc.player, mc.player.getEntityBoundingBox().offset((double)0.0F, 0.0656, (double)0.0F)).isEmpty() && mc.player.onGround) {
         mc.player.connection.sendPacket(new CPacketPlayer.Position(mc.player.posX, mc.player.posY + 0.0656, mc.player.posZ, false));
         return true;
      } else {
         return false;
      }
   }

   public void doStrictPost() {
      if (this.sprintStopped) {
         this.sprintStopped = false;
         mc.player.connection.sendPacket(new CPacketEntityAction(mc.player, Action.START_SPRINTING));
      }

      if (this.sneakStopped) {
         this.sneakStopped = false;
         mc.player.connection.sendPacket(new CPacketEntityAction(mc.player, Action.START_SNEAKING));
      }

   }

   @SubscribeEvent
   public void onKeyMove(TickEvent.ClientTickEvent event) {
      if (!fullNullCheck()) {
         if ((Boolean)this.guiMove.getValue() && this.isInMoveableScreen()) {
            this.handleInventoryRotate();
            handleInventoryMovement();
         }

      }
   }

   public static void handleInventoryMovement() {
      if (mc.player != null) {
         mc.player.movementInput.moveStrafe = 0.0F;
         mc.player.movementInput.moveForward = 0.0F;
         KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), Keyboard.isKeyDown(mc.gameSettings.keyBindForward.getKeyCode()));
         KeyBinding.setKeyBindState(mc.gameSettings.keyBindBack.getKeyCode(), Keyboard.isKeyDown(mc.gameSettings.keyBindBack.getKeyCode()));
         KeyBinding.setKeyBindState(mc.gameSettings.keyBindLeft.getKeyCode(), Keyboard.isKeyDown(mc.gameSettings.keyBindLeft.getKeyCode()));
         KeyBinding.setKeyBindState(mc.gameSettings.keyBindRight.getKeyCode(), Keyboard.isKeyDown(mc.gameSettings.keyBindRight.getKeyCode()));
         KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), Keyboard.isKeyDown(mc.gameSettings.keyBindJump.getKeyCode()));
         if (Keyboard.isKeyDown(mc.gameSettings.keyBindForward.getKeyCode())) {
            ++mc.player.movementInput.moveForward;
            mc.player.movementInput.forwardKeyDown = true;
         } else {
            mc.player.movementInput.forwardKeyDown = false;
         }

         if (Keyboard.isKeyDown(mc.gameSettings.keyBindBack.getKeyCode())) {
            --mc.player.movementInput.moveForward;
            mc.player.movementInput.backKeyDown = true;
         } else {
            mc.player.movementInput.backKeyDown = false;
         }

         if (Keyboard.isKeyDown(mc.gameSettings.keyBindLeft.getKeyCode())) {
            ++mc.player.movementInput.moveStrafe;
            mc.player.movementInput.leftKeyDown = true;
         } else {
            mc.player.movementInput.leftKeyDown = false;
         }

         if (Keyboard.isKeyDown(mc.gameSettings.keyBindRight.getKeyCode())) {
            --mc.player.movementInput.moveStrafe;
            mc.player.movementInput.rightKeyDown = true;
         } else {
            mc.player.movementInput.rightKeyDown = false;
         }

         mc.player.movementInput.jump = Keyboard.isKeyDown(mc.gameSettings.keyBindJump.getKeyCode());
      }
   }

   private void handleInventoryRotate() {
      if (mc.player != null) {
         EntityPlayerSP p = mc.player;
         if (Keyboard.isKeyDown(200)) {
            p.rotationPitch -= 4.0F;
         }

         if (Keyboard.isKeyDown(208)) {
            p.rotationPitch += 4.0F;
         }

         if (Keyboard.isKeyDown(203)) {
            p.rotationYaw -= 5.0F;
         }

         if (Keyboard.isKeyDown(205)) {
            p.rotationYaw += 5.0F;
         }

         if (p.rotationPitch > 90.0F) {
            p.rotationPitch = 90.0F;
         }

         if (p.rotationPitch < -90.0F) {
            p.rotationPitch = -90.0F;
         }

      }
   }

   private boolean isInMoveableScreen() {
      return mc.currentScreen != null && !(mc.currentScreen instanceof GuiChat);
   }

   @SubscribeEvent
   public void onInputUpdate(InputUpdateEvent event) {
      if ((this.mode.getValue() == NoSlow.Mode.Cancel || this.mode.getValue() == NoSlow.Mode.Strict) && this.isSlowed()) {
         MovementInput var10000 = event.getMovementInput();
         var10000.moveStrafe *= 5.0F;
         var10000 = event.getMovementInput();
         var10000.moveForward *= 5.0F;
      }

   }

   private boolean isSlowed() {
      return mc.player.isHandActive() && !mc.player.isRiding() && !mc.player.isElytraFlying();
   }

   public String getDisplayInfo() {
      return ((Mode)this.mode.getValue()).name();
   }

   public static enum Mode {
      Cancel,
      Strict;
   }
}
