package fat.vonware.mixin.mixins;

import fat.vonware.event.events.player.InputUpdateEvent;
import fat.vonware.features.modules.client.ClickGui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.MovementInput;
import net.minecraft.util.MovementInputFromOptions;
import net.minecraftforge.common.MinecraftForge;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({MovementInputFromOptions.class})
public class MixinMovementInputFromOptions extends MovementInput {
   @Inject(
      method = {"updatePlayerMoveState"},
      at = {@At("RETURN")}
   )
   public void updatePlayerMoveState(CallbackInfo ci) {
      if ((Boolean)ClickGui.getInstance().guiMove.getValue() && Minecraft.getMinecraft().currentScreen != null && !(Minecraft.getMinecraft().currentScreen instanceof GuiChat)) {
         KeyBinding forward = Minecraft.getMinecraft().gameSettings.keyBindForward;
         KeyBinding back = Minecraft.getMinecraft().gameSettings.keyBindBack;
         KeyBinding left = Minecraft.getMinecraft().gameSettings.keyBindLeft;
         KeyBinding right = Minecraft.getMinecraft().gameSettings.keyBindRight;
         KeyBinding jump = Minecraft.getMinecraft().gameSettings.keyBindJump;
         this.moveStrafe = 0.0F;
         this.moveForward = 0.0F;
         if (Keyboard.isKeyDown(forward.getKeyCode())) {
            ++this.moveForward;
         }

         if (Keyboard.isKeyDown(back.getKeyCode())) {
            --this.moveForward;
         }

         if (Keyboard.isKeyDown(left.getKeyCode())) {
            ++this.moveStrafe;
         }

         if (Keyboard.isKeyDown(right.getKeyCode())) {
            --this.moveStrafe;
         }

         this.jump = Keyboard.isKeyDown(jump.getKeyCode());
         if (this.sneak) {
            this.moveStrafe *= 0.3F;
            this.moveForward *= 0.3F;
         }
      }

      MinecraftForge.EVENT_BUS.post(new InputUpdateEvent((MovementInputFromOptions)(Object)this));
   }
}
