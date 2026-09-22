package fat.vonware.features.modules.render;

import fat.vonware.event.events.RenderItemEvent;
import fat.vonware.features.modules.Module;
import fat.vonware.features.setting.Setting;
import net.minecraft.client.settings.GameSettings.Options;
import net.minecraft.util.EnumHand;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class HandVisuals extends Module {
   private static HandVisuals INSTANCE;
   private final Setting<Boolean> handSwitch = new Setting<Boolean>("HandSwitch", false);
   public Setting<Boolean> minimalEatBob = new Setting<Boolean>("MinimalEatBob", false);
   public Setting<Boolean> smallShield = new Setting<Boolean>("SmallShield", false);
   public Setting<Float> offX = new Setting<Float>("OffX", 0.0F, -1.0F, 1.0F, (v) -> (Boolean)this.smallShield.getValue());
   public Setting<Float> offY = new Setting<Float>("OffY", 0.0F, -1.0F, 1.0F, (v) -> (Boolean)this.smallShield.getValue());
   public Setting<Float> mainX = new Setting<Float>("MainX", 0.0F, -1.0F, 1.0F, (v) -> (Boolean)this.smallShield.getValue());
   public Setting<Float> mainY = new Setting<Float>("MainY", 0.0F, -1.0F, 1.0F, (v) -> (Boolean)this.smallShield.getValue());
   public Setting<Boolean> changeSwing = new Setting<Boolean>("ChangeSwing", false);
   public Setting<Integer> swingSpeed = new Setting<Integer>("Swing Speed", 6, 0, 20, (v) -> (Boolean)this.changeSwing.getValue());
   public Setting<Integer> swingDelay = new Setting<Integer>("Swing Delay", 6, 1, 20, (v) -> (Boolean)this.changeSwing.getValue());
   public Setting<Hand> hand;
   public Setting<Boolean> viewModel;
   public Setting<Boolean> viewModelLeft;
   public Setting<Boolean> viewModelRight;
   public Setting<Float> viewModelScaleLeft;
   public Setting<Float> viewModelScaleRight;
   public Setting<Float> viewModelXLeft;
   public Setting<Float> viewModelYLeft;
   public Setting<Float> viewModelZLeft;
   public Setting<Float> viewModelRotateLeft;
   public Setting<Float> viewModelXRight;
   public Setting<Float> viewModelYRight;
   public Setting<Float> viewModelZRight;
   public Setting<Float> viewModelRotateRight;
   public Setting<Float> viewModelFov;
   private boolean previousHandSwitch;

   public HandVisuals() {
      super("HandVisuals", "Allows you to modify your hand", Module.Category.RENDER, true, false, false);
      this.hand = new Setting<Hand>("Hand", HandVisuals.Hand.OFFHAND, (v) -> (Boolean)this.changeSwing.getValue());
      this.viewModel = new Setting<Boolean>("ViewModel", false);
      this.viewModelLeft = new Setting<Boolean>("Left Hand", false, (v) -> (Boolean)this.viewModel.getValue());
      this.viewModelRight = new Setting<Boolean>("Right Hand", false, (v) -> (Boolean)this.viewModel.getValue());
      this.viewModelScaleLeft = new Setting<Float>("Left Scale", 1.0F, 0.1F, 2.0F, (v) -> (Boolean)this.viewModel.getValue() && (Boolean)this.viewModelLeft.getValue());
      this.viewModelScaleRight = new Setting<Float>("Right Scale", 1.0F, 0.1F, 2.0F, (v) -> (Boolean)this.viewModel.getValue() && (Boolean)this.viewModelRight.getValue());
      this.viewModelXLeft = new Setting<Float>("Left X", 0.0F, -2.0F, 2.0F, (v) -> (Boolean)this.viewModel.getValue() && (Boolean)this.viewModelLeft.getValue());
      this.viewModelYLeft = new Setting<Float>("Left Y", 0.0F, -2.0F, 2.0F, (v) -> (Boolean)this.viewModel.getValue() && (Boolean)this.viewModelLeft.getValue());
      this.viewModelZLeft = new Setting<Float>("Left Z", 0.0F, -2.0F, 2.0F, (v) -> (Boolean)this.viewModel.getValue() && (Boolean)this.viewModelLeft.getValue());
      this.viewModelRotateLeft = new Setting<Float>("Left Rotate", 0.0F, -180.0F, 180.0F, (v) -> (Boolean)this.viewModel.getValue() && (Boolean)this.viewModelLeft.getValue());
      this.viewModelXRight = new Setting<Float>("Right X", 0.0F, -2.0F, 2.0F, (v) -> (Boolean)this.viewModel.getValue() && (Boolean)this.viewModelRight.getValue());
      this.viewModelYRight = new Setting<Float>("Right Y", 0.0F, -2.0F, 2.0F, (v) -> (Boolean)this.viewModel.getValue() && (Boolean)this.viewModelRight.getValue());
      this.viewModelZRight = new Setting<Float>("Right Z", 0.0F, -2.0F, 2.0F, (v) -> (Boolean)this.viewModel.getValue() && (Boolean)this.viewModelRight.getValue());
      this.viewModelRotateRight = new Setting<Float>("Right Rotate", 0.0F, -180.0F, 180.0F, (v) -> (Boolean)this.viewModel.getValue() && (Boolean)this.viewModelRight.getValue());
      this.viewModelFov = new Setting<Float>("ViewModel FOV", 70.0F, 30.0F, 110.0F, (v) -> (Boolean)this.viewModel.getValue() && ((Boolean)this.viewModelLeft.getValue() || (Boolean)this.viewModelRight.getValue()));
      this.previousHandSwitch = false;
      this.register(this.minimalEatBob);
      this.register(this.handSwitch);
      this.register(this.smallShield);
      this.register(this.offX);
      this.register(this.offY);
      this.register(this.mainX);
      this.register(this.mainY);
      this.register(this.changeSwing);
      this.register(this.swingSpeed);
      this.register(this.swingDelay);
      this.register(this.hand);
      this.register(this.viewModel);
      this.register(this.viewModelLeft);
      this.register(this.viewModelRight);
      this.register(this.viewModelScaleLeft);
      this.register(this.viewModelScaleRight);
      this.register(this.viewModelXLeft);
      this.register(this.viewModelYLeft);
      this.register(this.viewModelZLeft);
      this.register(this.viewModelRotateLeft);
      this.register(this.viewModelXRight);
      this.register(this.viewModelYRight);
      this.register(this.viewModelZRight);
      this.register(this.viewModelRotateRight);
      this.register(this.viewModelFov);
      INSTANCE = this;
   }

   public static HandVisuals getInstance() {
      if (INSTANCE == null) {
         INSTANCE = new HandVisuals();
      }

      return INSTANCE;
   }

   public void onUpdate() {
      if (!fullNullCheck()) {
         if ((Boolean)this.handSwitch.getValue() != this.previousHandSwitch) {
            if ((Boolean)this.handSwitch.getValue()) {
               mc.gameSettings.setOptionValue(Options.MAIN_HAND, 1);
            } else {
               mc.gameSettings.setOptionValue(Options.MAIN_HAND, 0);
            }

            this.previousHandSwitch = (Boolean)this.handSwitch.getValue();
         }

         if ((Boolean)this.changeSwing.getValue()) {
            if (((Hand)this.hand.getValue()).equals(HandVisuals.Hand.OFFHAND)) {
               mc.player.swingingHand = EnumHand.OFF_HAND;
            }

            if (((Hand)this.hand.getValue()).equals(HandVisuals.Hand.MAINHAND)) {
               mc.player.swingingHand = EnumHand.MAIN_HAND;
            }
         }

      }
   }

   @SubscribeEvent
   public void onRenderItem(RenderItemEvent event) {
      if (this.isEnabled() && (Boolean)this.viewModel.getValue()) {
         if ((Boolean)this.viewModelRight.getValue()) {
            event.setMainX(event.getMainX() + (double)(Float)this.viewModelXRight.getValue());
            event.setMainY(event.getMainY() + (double)(Float)this.viewModelYRight.getValue());
            event.setMainZ(event.getMainZ() + (double)(Float)this.viewModelZRight.getValue());
            event.setMainRAngle((double)(Float)this.viewModelRotateRight.getValue());
            event.setMainRx((double)0.0F);
            event.setMainRy((double)1.0F);
            event.setMainRz((double)0.0F);
            event.setMainHandScaleX((double)(Float)this.viewModelScaleRight.getValue());
            event.setMainHandScaleY((double)(Float)this.viewModelScaleRight.getValue());
            event.setMainHandScaleZ((double)(Float)this.viewModelScaleRight.getValue());
         }

         if ((Boolean)this.viewModelLeft.getValue()) {
            event.setOffX(event.getOffX() + (double)(Float)this.viewModelXLeft.getValue());
            event.setOffY(event.getOffY() + (double)(Float)this.viewModelYLeft.getValue());
            event.setOffZ(event.getOffZ() + (double)(Float)this.viewModelZLeft.getValue());
            event.setOffRAngle((double)(Float)this.viewModelRotateLeft.getValue());
            event.setOffRx((double)0.0F);
            event.setOffRy((double)1.0F);
            event.setOffRz((double)0.0F);
            event.setOffHandScaleX((double)(Float)this.viewModelScaleLeft.getValue());
            event.setOffHandScaleY((double)(Float)this.viewModelScaleLeft.getValue());
            event.setOffHandScaleZ((double)(Float)this.viewModelScaleLeft.getValue());
         }

      }
   }

   public static enum Hand {
      OFFHAND,
      MAINHAND;
   }
}
