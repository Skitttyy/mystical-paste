package fat.vonware.features.modules.render;

import fat.vonware.event.events.Render3DEvent;
import fat.vonware.features.modules.Module;
import fat.vonware.features.setting.Setting;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class EntityTweaks extends Module {
   private static EntityTweaks instance;
   public Setting<Boolean> noInterp = new Setting<Boolean>("NoInterp", false);
   public Setting<Boolean> alwaysSneak = new Setting<Boolean>("Sneak", false);
   public Setting<Boolean> selfSneak = new Setting<Boolean>("VioletSelfSneak", false);
   public Setting<Boolean> changePlayerSize = new Setting<Boolean>("PlayerSize", false);
   public Setting<Float> playerSize = new Setting<Float>("Size", 1.0F, 0.1F, 2.0F, (v) -> (Boolean)this.changePlayerSize.getValue());

   public EntityTweaks() {
      super("EntityTweaks", "tweaks player entities", Module.Category.RENDER, true, false, false);
      this.register(this.alwaysSneak);
      this.register(this.selfSneak);
      this.register(this.noInterp);
      this.register(this.changePlayerSize);
      this.register(this.playerSize);
   }

   public static EntityTweaks getInstance() {
      if (instance == null) {
         instance = new EntityTweaks();
      }

      return instance;
   }

   @SubscribeEvent
   public void onRender3D(Render3DEvent e) {
      if ((Boolean)this.noInterp.getValue()) {
         if (mc.world != null && mc.world.playerEntities != null) {
            for(EntityPlayer player : mc.world.playerEntities) {
               player.limbSwing = 0.0F;
               player.limbSwingAmount = 0.0F;
            }

         }
      }
   }
}
