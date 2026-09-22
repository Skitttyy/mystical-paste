package fat.vonware.features.modules.render;

import fat.vonware.event.events.UpdateEquippedItemEvent;
import fat.vonware.features.modules.Module;
import fat.vonware.features.setting.Setting;
import fat.vonware.mixin.mixins.accessor.IItemRenderer;
import java.util.Objects;
import java.util.function.Function;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class Animations extends Module {
   public static Animations INSTANCE;
   public Setting<Boolean> oldSwapping = new Setting<Boolean>("SwitchBob", false, "Uses 1.8 style switch bob.");
   public Setting<Boolean> oldAttackBob = new Setting<Boolean>("AttackBob", true, "Uses 1.8 style attack bob.");
   public Setting<Boolean> guiAnimations = new Setting<Boolean>("GuiAnimations", false, "Parent setting for gui animations.");
   public Setting<GuiMode> guiMode;
   public Setting<Integer> guiSpeed;
   private AnimationState state;
   private long animationStart;
   private GuiScreen closingGui;

   public Animations() {
      super("Animations", "changes animations to 1.8 style & other animations", Module.Category.RENDER, true, false, false);
      this.guiMode = (new Setting("Mode", Animations.GuiMode.Smooth, (v) -> (Boolean)this.guiAnimations.getValue())).withDescription((Function<GuiMode, String>)((mode) -> {
         switch (mode) {
            case Smooth:
               return "Smooth: Uses the smooth GUI animation interpolation.";
            case Bounce:
               return "Bounce: Uses an overshooting/bounce interpolation for GUI animations.";
            default:
               return "";
         }
      }));
      this.guiSpeed = new Setting<Integer>("Speed", 200, 50, 500, (v) -> (Boolean)this.guiAnimations.getValue(), "Controls the speed value used by Animations.");
      this.state = Animations.AnimationState.STATIC;
      this.animationStart = 0L;
      this.closingGui = null;
      this.register(this.oldAttackBob);
      this.register(this.oldSwapping);
      this.register(this.guiAnimations);
      this.register(this.guiMode);
      this.register(this.guiSpeed);
      INSTANCE = this;
   }

   @SubscribeEvent
   public void onGuiOpen(GuiOpenEvent event) {
      if (this.isEnabled() && (Boolean)this.guiAnimations.getValue()) {
         if (event.getGui() == null && mc.currentScreen != null) {
            if (this.shouldAnimate(mc.currentScreen)) {
               this.state = Animations.AnimationState.CLOSING;
               this.animationStart = System.currentTimeMillis();
               this.closingGui = mc.currentScreen;
            }
         } else if (event.getGui() != null && this.shouldAnimate(event.getGui())) {
            this.state = Animations.AnimationState.OPENING;
            this.animationStart = System.currentTimeMillis();
            this.closingGui = null;
         }

      }
   }

   @SubscribeEvent
   public void onUpdateEquippedItem(UpdateEquippedItemEvent event) {
      event.setCanceled(true);
      IItemRenderer itemRenderer = (IItemRenderer)mc.entityRenderer.itemRenderer;
      itemRenderer.settPrevEquippedProgressMainHand(itemRenderer.gettEquippedProgressMainHand());
      itemRenderer.settPrevEquippedProgressOffHand(itemRenderer.gettEquippedProgressOffHand());
      EntityPlayerSP player = mc.player;
      ItemStack currentMainHandItem = player.getHeldItemMainhand();
      ItemStack currentOffHandItem = player.getHeldItemOffhand();
      if (player.isRowingBoat()) {
         itemRenderer.settEquippedProgressMainHand(MathHelper.clamp(itemRenderer.gettEquippedProgressMainHand() - 0.4F, 0.0F, 1.0F));
         itemRenderer.settEquippedProgressOffHand(MathHelper.clamp(itemRenderer.gettEquippedProgressOffHand() - 0.4F, 0.0F, 1.0F));
      } else {
         boolean mainHandChanged = ForgeHooksClient.shouldCauseReequipAnimation(itemRenderer.gettItemStackMainHand(), currentMainHandItem, player.inventory.currentItem);
         boolean offHandChanged = ForgeHooksClient.shouldCauseReequipAnimation(itemRenderer.gettItemStackOffHand(), currentOffHandItem, -1);
         boolean shouldAnimateMainHand = !(Boolean)this.oldSwapping.getValue() && mainHandChanged;
         boolean shouldAnimateOffHand = !(Boolean)this.oldSwapping.getValue() && offHandChanged;
         if (!shouldAnimateMainHand && !Objects.equals(itemRenderer.gettItemStackMainHand(), currentMainHandItem)) {
            itemRenderer.settItemStackMainHand(currentMainHandItem);
         }

         if (!shouldAnimateOffHand && !Objects.equals(itemRenderer.gettItemStackOffHand(), currentOffHandItem)) {
            itemRenderer.settItemStackOffHand(currentOffHandItem);
         }

         itemRenderer.settEquippedProgressMainHand(itemRenderer.gettEquippedProgressMainHand() + MathHelper.clamp((shouldAnimateMainHand ? 0.0F : 1.0F) - itemRenderer.gettEquippedProgressMainHand(), -0.4F, 0.4F));
         itemRenderer.settEquippedProgressOffHand(itemRenderer.gettEquippedProgressOffHand() + MathHelper.clamp((shouldAnimateOffHand ? 0.0F : 1.0F) - itemRenderer.gettEquippedProgressOffHand(), -0.4F, 0.4F));
      }

      if (itemRenderer.gettEquippedProgressMainHand() < 0.1F) {
         itemRenderer.settItemStackMainHand(currentMainHandItem);
      }

      if (itemRenderer.gettEquippedProgressOffHand() < 0.1F) {
         itemRenderer.settItemStackOffHand(currentOffHandItem);
      }

   }

   public float getScale() {
      if (this.isEnabled() && (Boolean)this.guiAnimations.getValue() && this.state != Animations.AnimationState.STATIC) {
         float progress = this.getProgress();
         if (progress >= 1.0F) {
            this.state = Animations.AnimationState.STATIC;
            this.closingGui = null;
            return 1.0F;
         } else {
            if (this.state == Animations.AnimationState.CLOSING) {
               progress = 1.0F - progress;
            }

            return this.applyEasing(progress);
         }
      } else {
         return 1.0F;
      }
   }

   private boolean shouldAnimate(Object gui) {
      return !(gui instanceof GuiChat);
   }

   private float getProgress() {
      long elapsed = System.currentTimeMillis() - this.animationStart;
      float progress = (float)elapsed / (float)(Integer)this.guiSpeed.getValue();
      return Math.min(progress, 1.0F);
   }

   private float applyEasing(float progress) {
      switch ((GuiMode)this.guiMode.getValue()) {
         case Smooth:
            return this.easeOutQuad(progress);
         case Bounce:
            return this.easeOutBounce(progress);
         default:
            return progress;
      }
   }

   private float easeOutQuad(float t) {
      return 1.0F - (1.0F - t) * (1.0F - t);
   }

   private float easeOutBounce(float t) {
      float n1 = 7.5625F;
      float d1 = 2.75F;
      if (t < 1.0F / d1) {
         return n1 * t * t;
      } else if (t < 2.0F / d1) {
         float var6;
         return n1 * (var6 = t - 1.5F / d1) * var6 + 0.75F;
      } else {
         float var4;
         float var5;
         return t < 2.5F / d1 ? n1 * (var4 = t - 2.25F / d1) * var4 + 0.9375F : n1 * (var5 = t - 2.625F / d1) * var5 + 0.984375F;
      }
   }

   private static enum AnimationState {
      OPENING,
      CLOSING,
      STATIC;
   }

   public static enum GuiMode {
      Smooth,
      Bounce;
   }
}
