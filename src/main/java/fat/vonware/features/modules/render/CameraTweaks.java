package fat.vonware.features.modules.render;

import fat.vonware.event.events.player.PerspectiveEvent;
import fat.vonware.features.modules.Module;
import fat.vonware.features.setting.Setting;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class CameraTweaks extends Module {
   public static CameraTweaks INSTANCE;
   public final Setting<Boolean> viewClip = new Setting<Boolean>("ViewClip", false);
   public final Setting<Float> distance = new Setting<Float>("Distance", 3.0F, 1.0F, 10.0F, (v) -> (Boolean)this.viewClip.getValue());
   public final Setting<Boolean> aspectRatio = new Setting<Boolean>("Aspect Ratio", false);
   public final Setting<Float> aspectRatioAmount;

   public CameraTweaks() {
      super("CameraTweaks", "tweaks the camera", Module.Category.RENDER, true, false, false);
      this.aspectRatioAmount = new Setting<Float>("AspectRatioAmount", (float)Minecraft.getMinecraft().displayWidth / (float)Minecraft.getMinecraft().displayHeight, 0.1F, 3.0F, (v) -> (Boolean)this.aspectRatio.getValue());
      this.register(this.viewClip);
      this.register(this.distance);
      this.register(this.aspectRatio);
      this.register(this.aspectRatioAmount);
      INSTANCE = this;
   }

   @SubscribeEvent
   public void onPerspectiveEvent(PerspectiveEvent event) {
      if ((Boolean)this.aspectRatio.getValue()) {
         event.setAspect((Float)this.aspectRatioAmount.getValue());
      }

   }
}
