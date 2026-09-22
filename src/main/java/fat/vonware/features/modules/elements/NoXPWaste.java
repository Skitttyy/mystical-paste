package fat.vonware.features.modules.elements;

import fat.vonware.Vonware;
import fat.vonware.event.events.Render2DEvent;
import fat.vonware.features.gui.newgui.newcomponents.NewVonwareGui;
import fat.vonware.features.modules.HudModule;
import fat.vonware.features.modules.Module;
import fat.vonware.features.setting.ColorSetting;
import fat.vonware.features.setting.Setting;
import fat.vonware.util.InventoryUtil;
import fat.vonware.util.Animations.render.ColorUtil;
import fat.vonware.util.OtherRenderShit.gui.SoundUtil;
import net.minecraft.item.ItemExpBottle;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;

public class NoXPWaste extends HudModule {
   private static final long NOTIFY_DURATION_MS = 1500L;
   private static final String LABEL = "Wasting XP!";
   public Setting<Boolean> render = new Setting<Boolean>("Render", true);
   public Setting<Boolean> sound = new Setting<Boolean>("Sound", true);
   public Setting<SoundChoice> soundType;
   public Setting<Sensitivity> sensitivity;
   public Setting<Float> barThreshold;
   public ColorSetting color;
   public Setting<Boolean> holding;
   private int lastLevel;
   private float lastExperience;
   private boolean thresholdCrossed;
   private long notifyUntil;

   public NoXPWaste() {
      super("NoXPWaste", "Warns you when you gain XP you should be spending", Module.Category.Elements, true, false, false, 100.0F, 20.0F);
      this.soundType = new Setting<SoundChoice>("Sounds", NoXPWaste.SoundChoice.SoundOne, (v) -> (Boolean)this.sound.getValue());
      this.sensitivity = new Setting<Sensitivity>("Sensitivity", NoXPWaste.Sensitivity.FullBar);
      this.barThreshold = new Setting<Float>("Threshold", 0.5F, 0.1F, 1.0F, (v) -> this.sensitivity.getValue() == NoXPWaste.Sensitivity.Divided);
      this.color = new ColorSetting("Color", 16711680, false);
      this.holding = new Setting<Boolean>("HoldingCheck", false);
      this.lastLevel = -1;
      this.lastExperience = -1.0F;
      this.thresholdCrossed = false;
      this.notifyUntil = 0L;
      this.register(this.render);
      this.register(this.sound);
      this.register(this.soundType);
      this.register(this.sensitivity);
      this.register(this.barThreshold);
      this.register(this.color);
      this.register(this.holding);
   }

   public void updateBounds() {
      if (!this.isDragging()) {
         this.rect.setX((Float)this.x.getValue());
         this.rect.setY((Float)this.y.getValue());
      }

      int textWidth = mc.fontRenderer.getStringWidth("Wasting XP!");
      int textHeight = mc.fontRenderer.FONT_HEIGHT;
      this.rect.setWidth((float)(textWidth + 4));
      this.rect.setHeight((float)(textHeight + 4));
   }

   @SubscribeEvent
   public void onTick(TickEvent.ClientTickEvent event) {
      if (event.phase == Phase.START) {
         if (mc.player != null) {
            int currentLevel = mc.player.experienceLevel;
            float currentExperience = mc.player.experience;
            if (this.lastLevel == -1) {
               this.lastLevel = currentLevel;
               this.lastExperience = currentExperience;
            } else {
               boolean wasted = false;
               if (currentLevel > this.lastLevel) {
                  wasted = true;
                  this.thresholdCrossed = false;
               } else if (currentLevel == this.lastLevel) {
                  if (this.sensitivity.getValue() == NoXPWaste.Sensitivity.Divided && !this.thresholdCrossed && currentExperience >= (Float)this.barThreshold.getValue() && this.lastExperience < (Float)this.barThreshold.getValue()) {
                     wasted = true;
                     this.thresholdCrossed = true;
                  }
               } else {
                  this.thresholdCrossed = false;
               }

               if (wasted && (!(Boolean)this.holding.getValue() || InventoryUtil.isHolding(ItemExpBottle.class))) {
                  this.onXpWasted();
               }

               this.lastLevel = currentLevel;
               this.lastExperience = currentExperience;
            }
         }
      }
   }

   private void onXpWasted() {
      this.notifyUntil = System.currentTimeMillis() + 1500L;
      if ((Boolean)this.sound.getValue()) {
         this.playWasteSound();
      }

   }

   private void playWasteSound() {
      switch ((SoundChoice)this.soundType.getValue()) {
         case SoundOne:
            SoundUtil.playSoundOne();
            break;
         case SoundTwo:
            SoundUtil.playSoundTwo();
            break;
         case SoundThree:
            SoundUtil.playSoundThree();
            break;
         case SoundFour:
            SoundUtil.playSoundFour();
      }

   }

   private boolean isHudEditorOpen() {
      return mc.currentScreen instanceof NewVonwareGui && ((NewVonwareGui)mc.currentScreen).isHudEditor();
   }

   public void onRender2D(Render2DEvent event) {
      this.updateBounds();
      if ((Boolean)this.render.getValue()) {
         boolean isPreview = this.isHudEditorOpen();
         if (isPreview || System.currentTimeMillis() <= this.notifyUntil) {
            Vonware.textManager.drawStringWithShadow("Wasting XP!", this.rect.getX(), this.rect.getY(), ColorUtil.getResolvedColor(this.color).getRGB());
         }
      }
   }

   public static enum Sensitivity {
      FullBar,
      Divided;
   }

   public static enum SoundChoice {
      SoundOne,
      SoundTwo,
      SoundThree,
      SoundFour;
   }
}
