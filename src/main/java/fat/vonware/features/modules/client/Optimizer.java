package fat.vonware.features.modules.client;

import fat.vonware.features.Feature;
import fat.vonware.features.modules.Module;
import fat.vonware.features.setting.Setting;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;

public class Optimizer extends Module {
   public Setting<Boolean> entityFrustum = new Setting<Boolean>("Entity Frustum", true);
   public Setting<Boolean> tileEntityFrustum = new Setting<Boolean>("TE Frustum", true);
   public Setting<Boolean> particleFrustum = new Setting<Boolean>("Particle Frustum", true);
   public Setting<Float> frustumBuffer = new Setting<Float>("Frustum Buffer", 1.5F, 0.0F, 8.0F);
   private static final Frustum frustum = new Frustum();
   private static boolean frustumReady = false;

   public Optimizer() {
      super("Optimizer", "Frustum culling and render optimizations", Module.Category.CLIENT, true, false, false);
      this.register(this.entityFrustum);
      this.register(this.tileEntityFrustum);
      this.register(this.particleFrustum);
      this.register(this.frustumBuffer);
      this.enabled.setValue(true);
   }

   public static void updateFrustum(double x, double y, double z) {
      frustum.setPosition(x, y, z);
      frustumReady = true;
   }

   public static boolean shouldCullEntity(Entity entity) {
      if (frustumReady && entity != null) {
         Optimizer mod = getModule();
         if (mod != null && mod.isEnabled() && (Boolean)mod.entityFrustum.getValue()) {
            float buf = (Float)mod.frustumBuffer.getValue();
            AxisAlignedBB bb = entity.getEntityBoundingBox().grow((double)buf);
            return !frustum.isBoundingBoxInFrustum(bb);
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public static boolean shouldCullAABB(AxisAlignedBB bb) {
      if (frustumReady && bb != null) {
         Optimizer mod = getModule();
         if (mod != null && mod.isEnabled() && (Boolean)mod.particleFrustum.getValue()) {
            float buf = (Float)mod.frustumBuffer.getValue();
            return !frustum.isBoundingBoxInFrustum(bb.grow((double)buf));
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private static Optimizer getModule() {
      return mc.player != null && mc.world != null ? null : null;
   }

   public void onUpdate() {
      if (!Feature.fullNullCheck()) {
         ;
      }
   }
}
