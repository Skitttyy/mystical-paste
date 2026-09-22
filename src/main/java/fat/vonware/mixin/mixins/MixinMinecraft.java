package fat.vonware.mixin.mixins;

import fat.vonware.Vonware;
import fat.vonware.ducks.IMinecraft;
import fat.vonware.event.events.KeyEvent;
import fat.vonware.event.events.ca.FpsUpdateEvent;
import fat.vonware.features.modules.client.Manager;
import fat.vonware.features.modules.player.FastBreak;
import fat.vonware.features.modules.player.MultiTask;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.resources.data.MetadataSerializer;
import net.minecraft.crash.CrashReport;
import net.minecraft.util.Timer;
import net.minecraftforge.common.MinecraftForge;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Minecraft.class})
public abstract class MixinMinecraft implements IMinecraft {
   @Shadow
   public GuiScreen currentScreen;
   @Shadow
   public WorldClient world;

   @Inject(
      method = {"shutdownMinecraftApplet"},
      at = {@At("HEAD")}
   )
   private void stopClient(CallbackInfo callbackInfo) {
      this.unload();
   }

   @Redirect(
      method = {"run"},
      at = @At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/Minecraft;displayCrashReport(Lnet/minecraft/crash/CrashReport;)V"
)
   )
   public void displayCrashReport(Minecraft minecraft, CrashReport crashReport) {
      try {
         this.unload();
      } finally {
         minecraft.displayCrashReport(crashReport);
      }
   }

   @Inject(
      method = {"runTickKeyboard"},
      at = {@At(
   value = "INVOKE",
   remap = false,
   target = "Lorg/lwjgl/input/Keyboard;getEventKey()I",
   ordinal = 0,
   shift = At.Shift.BEFORE
)}
   )
   private void onKeyboard(CallbackInfo callbackInfo) {
      int i = Keyboard.getEventKey() == 0 ? Keyboard.getEventCharacter() + 256 : Keyboard.getEventKey();
      if (Keyboard.getEventKeyState()) {
         KeyEvent event = new KeyEvent(i);
         MinecraftForge.EVENT_BUS.post(event);
      }

   }

   private boolean shouldBypassHandActive() {
      if (MultiTask.INSTANCE != null && MultiTask.INSTANCE.isEnabled()) {
         boolean scaffoldBypass = (Boolean)MultiTask.INSTANCE.scaffold.getValue();
         FastBreak fastBreak = (FastBreak)Vonware.moduleManager.getModuleByClass(FastBreak.class);
         boolean fastBreakBypass = (Boolean)MultiTask.INSTANCE.fastBreak.getValue() && fastBreak != null && fastBreak.isEnabled();
         return scaffoldBypass || fastBreakBypass;
      } else {
         return false;
      }
   }

   @Redirect(
      method = {"sendClickBlockToController"},
      at = @At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/entity/EntityPlayerSP;isHandActive()Z"
)
   )
   public boolean isHandActiveHook(EntityPlayerSP playerSP) {
      return !this.shouldBypassHandActive() && playerSP.isHandActive();
   }

   @Redirect(
      method = {"rightClickMouse"},
      at = @At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/multiplayer/PlayerControllerMP;getIsHittingBlock()Z",
   ordinal = 0
),
      require = 1
   )
   public boolean isHittingBlockHook(PlayerControllerMP playerControllerMP) {
      return !this.shouldBypassHandActive() && playerControllerMP.getIsHittingBlock();
   }

   @Inject(
      method = {"getLimitFramerate"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void limitFramesHook(CallbackInfoReturnable<Integer> info) {
      Manager manager = Manager.getInstance();
      if (!Display.isActive() && (Boolean)manager.unfocusedFPS.getValue()) {
         info.setReturnValue(manager.fps.getValue());
      }

   }

   @Inject(
      method = {"runGameLoop"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/profiler/Profiler;endSection()V",
   ordinal = 0,
   shift = At.Shift.AFTER
)}
   )
   private void onGameLoopPost(CallbackInfo callbackInfo) {
      MinecraftForge.EVENT_BUS.post(new FpsUpdateEvent());
   }

   @Accessor("rightClickDelayTimer")
   public abstract int getRightClickDelay();

   @Accessor("rightClickDelayTimer")
   public abstract void setRightClickDelay(int var1);

   @Accessor("metadataSerializer")
   public abstract MetadataSerializer getMetadataSerializer();

   @Accessor("timer")
   public abstract Timer getTimer();

   private void unload() {
      Vonware.LOGGER.info("Initiated client shutdown.");
      Vonware.onUnload();
      Vonware.LOGGER.info("Finished client shutdown.");
   }
}
