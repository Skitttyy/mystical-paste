package fat.vonware.features.modules.render;

import fat.vonware.Vonware;
import fat.vonware.event.events.Render3DEvent;
import fat.vonware.features.modules.Module;
import fat.vonware.features.setting.Setting;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.GlStateManager.DestFactor;
import net.minecraft.client.renderer.GlStateManager.SourceFactor;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class DamageText extends Module {
   private final Setting<Boolean> self = new Setting<Boolean>("Self", false);
   private final Setting<Boolean> filter = new Setting<Boolean>("Filter", false);
   private final Setting<Float> filterValue = new Setting<Float>("FilterValue", 1.0F, 0.1F, 10.0F, (v) -> (Boolean)this.filter.getValue());
   private final Setting<Integer> fadeTime = new Setting<Integer>("FadeTime", 2000, 1000, 5000);
   private final Setting<Float> textScale = new Setting<Float>("TextScale", 0.5F, 0.1F, 1.0F);
   private final Map<EntityPlayer, PlayerHealthData> healthData = new HashMap();
   private final ArrayList<HealthParticle> particles = new ArrayList();

   public DamageText() {
      super("DamageText", "Renders damage text on players", Module.Category.RENDER, false, false, false);
      this.register(this.self);
      this.register(this.filter);
      this.register(this.filterValue);
      this.register(this.fadeTime);
      this.register(this.textScale);
   }

   public void onUpdate() {
      if (mc.world != null) {
         for(EntityPlayer player : mc.world.playerEntities) {
            if (player != null && (player != mc.player || (Boolean)this.self.getValue())) {
               float currentHealth = player.getHealth();
               float currentAbsorption = player.getAbsorptionAmount();
               if (this.healthData.containsKey(player)) {
                  PlayerHealthData data = (PlayerHealthData)this.healthData.get(player);
                  float healthDiff = currentHealth - data.health;
                  float absorptionDiff = currentAbsorption - data.absorption;
                  double x = player.posX;
                  double y = player.posY + (double)player.height / (double)2.0F + (double)0.5F;
                  double z = player.posZ;
                  if (healthDiff != 0.0F) {
                     float absHealthDiff = Math.abs(healthDiff);
                     if (!(Boolean)this.filter.getValue() || absHealthDiff >= (Float)this.filterValue.getValue()) {
                        boolean isDamage = healthDiff < 0.0F;
                        String text = (isDamage ? "" : "+") + String.format("%.1f", absHealthDiff);
                        this.particles.add(new HealthParticle(x, y, z, text, isDamage, System.currentTimeMillis()));
                     }
                  }

                  if (absorptionDiff > 0.0F) {
                     if (!(Boolean)this.filter.getValue() || absorptionDiff >= (Float)this.filterValue.getValue()) {
                        String text = "+" + String.format("%.1f", absorptionDiff) + " \u2665";
                        this.particles.add(new HealthParticle(x, y + 0.3, z, text, false, System.currentTimeMillis()));
                     }
                  } else if (absorptionDiff < 0.0F) {
                     float absAbsorptionDiff = Math.abs(absorptionDiff);
                     if (!(Boolean)this.filter.getValue() || absAbsorptionDiff >= (Float)this.filterValue.getValue()) {
                        String text = String.format("%.1f", absAbsorptionDiff);
                        this.particles.add(new HealthParticle(x, y, z, text, true, System.currentTimeMillis()));
                     }
                  }
               }

               this.healthData.put(player, new PlayerHealthData(currentHealth, currentAbsorption));
            }
         }

         this.particles.removeIf((particle) -> System.currentTimeMillis() - particle.spawnTime > (long)(Integer)this.fadeTime.getValue());
         this.healthData.keySet().removeIf((playerx) -> !mc.world.playerEntities.contains(playerx));
      }
   }

   @SubscribeEvent
   public void onRender3D(Render3DEvent event) {
      if (mc.world != null && !this.particles.isEmpty()) {
         Iterator<HealthParticle> iterator = this.particles.iterator();

         while(iterator.hasNext()) {
            HealthParticle particle = (HealthParticle)iterator.next();
            long elapsed = System.currentTimeMillis() - particle.spawnTime;
            float progress = (float)elapsed / (float)(Integer)this.fadeTime.getValue();
            if (progress >= 1.0F) {
               iterator.remove();
            } else {
               float alpha = Math.max(0.0F, 1.0F - progress);
               if (!(alpha < 0.05F)) {
                  double yOffset = (double)progress * (double)0.5F;
                  this.renderHealthParticle(particle, alpha, yOffset);
               }
            }
         }

      }
   }

   private void renderHealthParticle(HealthParticle particle, float alpha, double yOffset) {
      double x = particle.x - mc.getRenderManager().viewerPosX;
      double y = particle.y + yOffset - mc.getRenderManager().viewerPosY;
      double z = particle.z - mc.getRenderManager().viewerPosZ;
      GlStateManager.pushMatrix();
      GlStateManager.translate(x, y, z);
      GlStateManager.rotate(-mc.getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);
      float var10001 = mc.gameSettings.thirdPersonView == 2 ? -1.0F : 1.0F;
      GlStateManager.rotate(mc.getRenderManager().playerViewX, var10001, 0.0F, 0.0F);
      float scale = 0.03F * (Float)this.textScale.getValue();
      GlStateManager.scale(-scale, -scale, scale);
      GlStateManager.disableDepth();
      GlStateManager.disableLighting();
      GlStateManager.enableBlend();
      GlStateManager.tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ZERO);
      int color = particle.isDamage ? toRGBA(255, 255, 0, (int)(255.0F * alpha)) : toRGBA(0, 255, 0, (int)(255.0F * alpha));
      GlStateManager.enableTexture2D();
      int width = Vonware.textManager.getStringWidth(particle.text);
      Vonware.textManager.drawStringWithShadow(particle.text, (float)(-width) / 2.0F, 0.0F, color);
      GlStateManager.enableDepth();
      GlStateManager.disableBlend();
      GlStateManager.popMatrix();
   }

   private static int toRGBA(int r, int g, int b, int a) {
      return a << 24 | r << 16 | g << 8 | b;
   }

   public void onDisable() {
      this.particles.clear();
      this.healthData.clear();
   }

   private static class HealthParticle {
      final double x;
      final double y;
      final double z;
      final String text;
      final boolean isDamage;
      final long spawnTime;

      HealthParticle(double x, double y, double z, String text, boolean isDamage, long spawnTime) {
         this.x = x;
         this.y = y;
         this.z = z;
         this.text = text;
         this.isDamage = isDamage;
         this.spawnTime = spawnTime;
      }
   }

   private static class PlayerHealthData {
      final float health;
      final float absorption;

      PlayerHealthData(float health, float absorption) {
         this.health = health;
         this.absorption = absorption;
      }
   }
}
