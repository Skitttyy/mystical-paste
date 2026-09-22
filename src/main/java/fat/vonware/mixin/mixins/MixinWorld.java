package fat.vonware.mixin.mixins;

import com.google.common.base.Predicate;
import fat.vonware.Vonware;
import fat.vonware.event.events.PushEvent;
import fat.vonware.features.modules.render.Environment;
import fat.vonware.features.modules.render.NoRender;
import java.awt.Color;
import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({World.class})
public class MixinWorld {
   @Redirect(
      method = {"getEntitiesWithinAABB(Ljava/lang/Class;Lnet/minecraft/util/math/AxisAlignedBB;Lcom/google/common/base/Predicate;)Ljava/util/List;"},
      at = @At(
   value = "INVOKE",
   target = "Lnet/minecraft/world/chunk/Chunk;getEntitiesOfTypeWithinAABB(Ljava/lang/Class;Lnet/minecraft/util/math/AxisAlignedBB;Ljava/util/List;Lcom/google/common/base/Predicate;)V"
)
   )
   public <T extends Entity> void getEntitiesOfTypeWithinAABBHook(Chunk chunk, Class<? extends T> entityClass, AxisAlignedBB aabb, List<T> listToFill, Predicate<? super T> filter) {
      try {
         chunk.getEntitiesOfTypeWithinAABB(entityClass, aabb, listToFill, filter);
      } catch (Exception var7) {
      }

   }

   @Inject(
      method = {"spawnParticle(Lnet/minecraft/util/EnumParticleTypes;DDDDDD[I)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void spawnParticleHook(EnumParticleTypes particleType, double xCoord, double yCoord, double zCoord, double xSpeed, double ySpeed, double zSpeed, int[] parameters, CallbackInfo ci) {
      NoRender noRender = (NoRender)Vonware.moduleManager.getModuleByClass(NoRender.class);
      if (noRender != null && noRender.isEnabled() && (Boolean)noRender.explosions.getValue()) {
         switch (particleType) {
            case EXPLOSION_NORMAL:
            case EXPLOSION_LARGE:
            case EXPLOSION_HUGE:
            case FIREWORKS_SPARK:
               ci.cancel();
         }
      }

   }

   @Redirect(
      method = {"handleMaterialAcceleration"},
      at = @At(
   value = "INVOKE",
   target = "Lnet/minecraft/entity/Entity;isPushedByWater()Z"
)
   )
   public boolean isPushedbyWaterHook(Entity entity) {
      PushEvent event = new PushEvent(1, entity);
      MinecraftForge.EVENT_BUS.post(event);
      return entity.isPushedByWater() && !event.isCanceled();
   }

   @Inject(
      method = {"getWorldTime"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void getWorldTime(CallbackInfoReturnable<Long> cir) {
      Environment env = Environment.INSTANCE;
      if (env != null && env.isEnabled() && (Boolean)env.clientsideTime.getValue()) {
         cir.setReturnValue(((Integer)env.time.getValue()).longValue());
         cir.cancel();
      }

   }

   @Inject(
      method = {"getSunBrightness"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void getSunBrightness(float partialTicks, CallbackInfoReturnable<Float> cir) {
      Environment env = Environment.INSTANCE;
      if (env != null && env.isEnabled() && (Boolean)env.fullBright.getValue() && env.fullBrightMode.getValue() == Environment.FullBrightMode.Gamma) {
         cir.setReturnValue(1000.0F);
         cir.cancel();
      }

   }

   @Inject(
      method = {"getSkyColorBody"},
      at = {@At("RETURN")},
      cancellable = true,
      remap = false
   )
   public void getSkyColorBody(Entity entityIn, float partialTicks, CallbackInfoReturnable<Vec3d> cir) {
      Environment CUSTOM_SKY = (Environment)Vonware.moduleManager.getModuleByClass(Environment.class);
      if (CUSTOM_SKY.isEnabled() && (Boolean)CUSTOM_SKY.sky.getValue()) {
         Color color = CUSTOM_SKY.getSkyColor();
         if (color != null) {
            cir.setReturnValue(new Vec3d((double)((float)color.getRed() / 255.0F), (double)((float)color.getGreen() / 255.0F), (double)((float)color.getBlue() / 255.0F)));
         }
      }

   }
}
