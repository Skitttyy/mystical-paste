package fat.vonware.util.Animations.game;

import fat.vonware.util.Util;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.GlStateManager.DestFactor;
import net.minecraft.client.renderer.GlStateManager.SourceFactor;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;

public class FallingParticleRenderer {
   public static final FallingParticleRenderer INSTANCE = new FallingParticleRenderer();
   public static final ResourceLocation CHERRY_TEXTURE = new ResourceLocation("textures/cherry_petal.png");
   private static final float LEGACY_SPAWN_RADIUS = 40.0F;
   private static final float LEGACY_SPAWN_ABOVE = 20.0F;
   private static final float LEGACY_SPAWN_BELOW = 2.0F;
   private static final int MAX_PARTICLES = 1000;
   private static final float CULL_DOT_THRESHOLD = -0.28F;
   private final List<Particle> particles = new ArrayList();
   private final Random random = new Random();
   private ParticleType activeType;
   private float gustClock;

   public void update(ParticleType mode, int maxCount, float density) {
      this.update(mode, maxCount, density, 1.0F, 1.0F);
   }

   public void update(ParticleType mode, int maxCount, float density, float windStrength, float sizeScale) {
      if (Util.mc.player != null && Util.mc.world != null) {
         if (this.activeType != mode) {
            this.particles.clear();
            this.activeType = mode;
            this.gustClock = 0.0F;
         }

         this.gustClock += 0.045F;
         int cap = Math.min(Math.max(1, maxCount), 1000);
         float spawnMultiplier = mode == FallingParticleRenderer.ParticleType.Ambient ? 3.4F : 2.0F;
         int toSpawn = Math.max(1, Math.round(density * spawnMultiplier));

         for(int i = 0; i < toSpawn && this.particles.size() < cap; ++i) {
            this.spawn(mode, windStrength, sizeScale);
         }

         Entity viewer = Util.mc.getRenderViewEntity();
         double viewerX = viewer == null ? Util.mc.player.posX : viewer.posX;
         double viewerY = viewer == null ? Util.mc.player.posY : viewer.posY;
         double viewerZ = viewer == null ? Util.mc.player.posZ : viewer.posZ;
         double gust = 0.72 + Math.sin((double)this.gustClock) * 0.19 + Math.sin((double)this.gustClock * 0.37 + 1.7) * 0.09;
         Iterator<Particle> iterator = this.particles.iterator();

         while(iterator.hasNext()) {
            Particle particle = (Particle)iterator.next();
            particle.prevX = particle.x;
            particle.prevY = particle.y;
            particle.prevZ = particle.z;
            particle.prevRotation = particle.rotation;
            ++particle.age;
            if (particle.dead()) {
               iterator.remove();
            } else {
               if (particle.ambient) {
                  float wave = (float)Math.sin((double)(particle.flutterPhase + (float)particle.age * particle.flutterSpeed));
                  float wave2 = (float)Math.cos((double)(particle.flutterPhase * 0.73F + (float)particle.age * particle.flutterSpeed * 0.61F));
                  double nextX = particle.x + particle.velocityX * gust + (double)(wave * particle.flutterAmount);
                  double nextY = particle.y + particle.velocityY + (double)(wave2 * particle.flutterAmount) * 0.28;
                  double nextZ = particle.z + particle.velocityZ * gust + (double)(wave2 * particle.flutterAmount);
                  if (!this.isPassable(new BlockPos(nextX, nextY, nextZ))) {
                     iterator.remove();
                     continue;
                  }

                  particle.x = nextX;
                  particle.y = nextY;
                  particle.z = nextZ;
                  double dx = particle.x - viewerX;
                  double dy = particle.y - viewerY;
                  double dz = particle.z - viewerZ;
                  if (dx * dx + dz * dz > (double)1444.0F || dy < (double)-10.0F || dy > (double)22.0F) {
                     iterator.remove();
                     continue;
                  }
               } else {
                  double nextY = particle.y + particle.velocityY;
                  if (!this.isPassable(new BlockPos(particle.x, nextY, particle.z))) {
                     iterator.remove();
                     continue;
                  }

                  particle.y = nextY;
                  double swing = Math.sin((double)particle.age * 0.055 + (double)particle.driftAngle) * (double)particle.driftSpeed * 0.08;
                  particle.x += Math.cos((double)particle.driftAngle) * swing;
                  particle.z += Math.sin((double)particle.driftAngle) * swing;
               }

               particle.rotation += particle.rotationSpeed;
            }
         }

      } else {
         this.clear();
      }
   }

   public void render(float partialTicks, ParticleType mode, float r, float g, float b) {
      this.render(partialTicks, mode, r, g, b, 1.0F, 1.0F);
   }

   public void render(float partialTicks, ParticleType mode, float r, float g, float b, float opacity, float sizeScale) {
      if (Util.mc.player != null && !this.particles.isEmpty()) {
         Entity viewer = Util.mc.getRenderViewEntity();
         if (viewer != null) {
            double cameraX = viewer.lastTickPosX + (viewer.posX - viewer.lastTickPosX) * (double)partialTicks;
            double cameraY = viewer.lastTickPosY + (viewer.posY - viewer.lastTickPosY) * (double)partialTicks + (double)viewer.getEyeHeight();
            double cameraZ = viewer.lastTickPosZ + (viewer.posZ - viewer.lastTickPosZ) * (double)partialTicks;
            float yaw = viewer.prevRotationYaw + (viewer.rotationYaw - viewer.prevRotationYaw) * partialTicks;
            float pitch = viewer.prevRotationPitch + (viewer.rotationPitch - viewer.prevRotationPitch) * partialTicks;
            float yawRad = (float)Math.toRadians((double)yaw);
            float pitchRad = (float)Math.toRadians((double)pitch);
            float forwardX = -((float)(Math.sin((double)yawRad) * Math.cos((double)pitchRad)));
            float forwardY = -((float)Math.sin((double)pitchRad));
            float forwardZ = (float)(Math.cos((double)yawRad) * Math.cos((double)pitchRad));
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ONE_MINUS_SRC_ALPHA);
            GlStateManager.disableCull();
            GlStateManager.depthMask(false);
            GlStateManager.enableDepth();
            GlStateManager.enableAlpha();
            GlStateManager.alphaFunc(516, 0.003F);
            GlStateManager.disableLighting();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            if (mode != FallingParticleRenderer.ParticleType.Ambient) {
               Util.mc.getTextureManager().bindTexture(CHERRY_TEXTURE);
               GlStateManager.enableTexture2D();
               this.renderTexturedParticles(partialTicks, mode, r, g, b, opacity, sizeScale, cameraX, cameraY, cameraZ, yaw, pitch, forwardX, forwardY, forwardZ);
            } else {
               GlStateManager.disableTexture2D();
               this.renderAmbientParticles(partialTicks, r, g, b, opacity, sizeScale, cameraX, cameraY, cameraZ, forwardX, forwardY, forwardZ);
               GlStateManager.enableTexture2D();
            }

            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.depthMask(true);
            GlStateManager.enableCull();
            GlStateManager.disableBlend();
            GlStateManager.alphaFunc(516, 0.1F);
         }
      }
   }

   private void renderTexturedParticles(float partialTicks, ParticleType mode, float r, float g, float b, float opacity, float sizeScale, double cameraX, double cameraY, double cameraZ, float yaw, float pitch, float forwardX, float forwardY, float forwardZ) {
      Tessellator tessellator = Tessellator.getInstance();
      BufferBuilder buffer = tessellator.getBuffer();

      for(Particle particle : this.particles) {
         if (!particle.ambient) {
            double worldX = particle.prevX + (particle.x - particle.prevX) * (double)partialTicks;
            double worldY = particle.prevY + (particle.y - particle.prevY) * (double)partialTicks;
            double worldZ = particle.prevZ + (particle.z - particle.prevZ) * (double)partialTicks;
            float dx = (float)(worldX - cameraX);
            float dy = (float)(worldY - cameraY);
            float dz = (float)(worldZ - cameraZ);
            float distance = (float)Math.sqrt((double)(dx * dx + dy * dy + dz * dz));
            if (isVisible(dx, dy, dz, distance, forwardX, forwardY, forwardZ)) {
               float alpha = particle.getLifeAlpha() * particle.alphaScale * opacity;
               if (!(alpha <= 0.003F)) {
                  float rotation = particle.prevRotation + (particle.rotation - particle.prevRotation) * partialTicks;
                  float halfWidth = particle.size * sizeScale * 0.5F;
                  float halfHeight = halfWidth * particle.stretch;
                  GlStateManager.pushMatrix();
                  GlStateManager.translate(dx, dy, dz);
                  GlStateManager.rotate(-yaw, 0.0F, 1.0F, 0.0F);
                  GlStateManager.rotate(pitch, 1.0F, 0.0F, 0.0F);
                  GlStateManager.rotate(rotation, 0.0F, 0.0F, 1.0F);
                  buffer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
                  buffer.pos((double)(-halfWidth), (double)(-halfHeight), (double)0.0F).tex((double)0.0F, (double)1.0F).color(r, g, b, alpha).endVertex();
                  buffer.pos((double)halfWidth, (double)(-halfHeight), (double)0.0F).tex((double)1.0F, (double)1.0F).color(r, g, b, alpha).endVertex();
                  buffer.pos((double)halfWidth, (double)halfHeight, (double)0.0F).tex((double)1.0F, (double)0.0F).color(r, g, b, alpha).endVertex();
                  buffer.pos((double)(-halfWidth), (double)halfHeight, (double)0.0F).tex((double)0.0F, (double)0.0F).color(r, g, b, alpha).endVertex();
                  tessellator.draw();
                  GlStateManager.popMatrix();
               }
            }
         }
      }

   }

   private void renderAmbientParticles(float partialTicks, float r, float g, float b, float opacity, float sizeScale, double cameraX, double cameraY, double cameraZ, float forwardX, float forwardY, float forwardZ) {
      Tessellator tessellator = Tessellator.getInstance();
      BufferBuilder buffer = tessellator.getBuffer();
      buffer.begin(4, DefaultVertexFormats.POSITION_COLOR);

      for(Particle particle : this.particles) {
         if (particle.ambient) {
            double worldX = particle.prevX + (particle.x - particle.prevX) * (double)partialTicks;
            double worldY = particle.prevY + (particle.y - particle.prevY) * (double)partialTicks;
            double worldZ = particle.prevZ + (particle.z - particle.prevZ) * (double)partialTicks;
            float dx = (float)(worldX - cameraX);
            float dy = (float)(worldY - cameraY);
            float dz = (float)(worldZ - cameraZ);
            float distance = (float)Math.sqrt((double)(dx * dx + dy * dy + dz * dz));
            if (isVisible(dx, dy, dz, distance, forwardX, forwardY, forwardZ)) {
               float nearFade = Math.min(1.0F, Math.max(0.0F, (distance - 0.12F) / 0.48F));
               float farFade = distance > 24.0F ? Math.max(0.0F, 1.0F - (distance - 24.0F) / 10.0F) : 1.0F;
               float alpha = particle.getLifeAlpha() * particle.alphaScale * opacity * nearFade * farFade;
               if (!(alpha <= 0.003F)) {
                  float spin = (float)Math.toRadians((double)(particle.prevRotation + (particle.rotation - particle.prevRotation) * partialTicks));
                  float tilt = particle.flutterPhase * 0.61F + (float)Math.sin((double)(((float)particle.age + partialTicks) * particle.flutterSpeed)) * 0.34F;
                  float roll = particle.flutterPhase * 0.37F + spin * 0.72F;
                  float cy = (float)Math.cos((double)spin);
                  float sy = (float)Math.sin((double)spin);
                  float cp = (float)Math.cos((double)tilt);
                  float sp = (float)Math.sin((double)tilt);
                  float cr = (float)Math.cos((double)roll);
                  float sr = (float)Math.sin((double)roll);
                  float axisXX = cy * cr + sy * sp * sr;
                  float axisXY = cp * sr;
                  float axisXZ = -sy * cr + cy * sp * sr;
                  float axisYX = -cy * sr + sy * sp * cr;
                  float axisYY = cp * cr;
                  float axisYZ = sy * sr + cy * sp * cr;
                  float axisZX = sy * cp;
                  float axisZY = -sp;
                  float axisZZ = cy * cp;
                  float base = particle.size * sizeScale;
                  emitOctahedron(buffer, dx, dy, dz, axisXX, axisXY, axisXZ, axisYX, axisYY, axisYZ, axisZX, axisZY, axisZZ, base * 0.72F, base * 0.72F * Math.min(particle.stretch, 1.18F), base * 0.54F, r, g, b, Math.min(1.0F, alpha * 0.34F));
                  emitOctahedron(buffer, dx, dy, dz, axisXX, axisXY, axisXZ, axisYX, axisYY, axisYZ, axisZX, axisZY, axisZZ, base * 0.4F, base * 0.4F * Math.min(particle.stretch, 1.1F), base * 0.32F, r, g, b, Math.min(1.0F, alpha * 0.86F));
                  emitOctahedron(buffer, dx, dy, dz, axisXX, axisXY, axisXZ, axisYX, axisYY, axisYZ, axisZX, axisZY, axisZZ, base * 0.17F, base * 0.17F, base * 0.15F, 1.0F, 1.0F, 1.0F, Math.min(1.0F, alpha * 1.18F));
               }
            }
         }
      }

      tessellator.draw();
   }

   private static void emitOctahedron(BufferBuilder buffer, float x, float y, float z, float axisXX, float axisXY, float axisXZ, float axisYX, float axisYY, float axisYZ, float axisZX, float axisZY, float axisZZ, float radiusX, float radiusY, float radiusZ, float r, float g, float b, float alpha) {
      float pxX = x + axisXX * radiusX;
      float pxY = y + axisXY * radiusX;
      float pxZ = z + axisXZ * radiusX;
      float nxX = x - axisXX * radiusX;
      float nxY = y - axisXY * radiusX;
      float nxZ = z - axisXZ * radiusX;
      float pyX = x + axisYX * radiusY;
      float pyY = y + axisYY * radiusY;
      float pyZ = z + axisYZ * radiusY;
      float nyX = x - axisYX * radiusY;
      float nyY = y - axisYY * radiusY;
      float nyZ = z - axisYZ * radiusY;
      float pzX = x + axisZX * radiusZ;
      float pzY = y + axisZY * radiusZ;
      float pzZ = z + axisZZ * radiusZ;
      float nzX = x - axisZX * radiusZ;
      float nzY = y - axisZY * radiusZ;
      float nzZ = z - axisZZ * radiusZ;
      float tipAlpha = Math.min(1.0F, alpha * 0.82F);
      emitTriangle(buffer, pzX, pzY, pzZ, pxX, pxY, pxZ, pyX, pyY, pyZ, r, g, b, tipAlpha, alpha);
      emitTriangle(buffer, pzX, pzY, pzZ, pyX, pyY, pyZ, nxX, nxY, nxZ, r, g, b, tipAlpha, alpha);
      emitTriangle(buffer, pzX, pzY, pzZ, nxX, nxY, nxZ, nyX, nyY, nyZ, r, g, b, tipAlpha, alpha);
      emitTriangle(buffer, pzX, pzY, pzZ, nyX, nyY, nyZ, pxX, pxY, pxZ, r, g, b, tipAlpha, alpha);
      emitTriangle(buffer, nzX, nzY, nzZ, pyX, pyY, pyZ, pxX, pxY, pxZ, r, g, b, tipAlpha, alpha);
      emitTriangle(buffer, nzX, nzY, nzZ, nxX, nxY, nxZ, pyX, pyY, pyZ, r, g, b, tipAlpha, alpha);
      emitTriangle(buffer, nzX, nzY, nzZ, nyX, nyY, nyZ, nxX, nxY, nxZ, r, g, b, tipAlpha, alpha);
      emitTriangle(buffer, nzX, nzY, nzZ, pxX, pxY, pxZ, nyX, nyY, nyZ, r, g, b, tipAlpha, alpha);
   }

   private static void emitTriangle(BufferBuilder buffer, float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz, float r, float g, float b, float tipAlpha, float ringAlpha) {
      buffer.pos((double)ax, (double)ay, (double)az).color(r, g, b, tipAlpha).endVertex();
      buffer.pos((double)bx, (double)by, (double)bz).color(r, g, b, ringAlpha).endVertex();
      buffer.pos((double)cx, (double)cy, (double)cz).color(r, g, b, ringAlpha).endVertex();
   }

   private static boolean isVisible(float dx, float dy, float dz, float distance, float forwardX, float forwardY, float forwardZ) {
      if (distance <= 0.001F) {
         return true;
      } else {
         float dot = dx / distance * forwardX + dy / distance * forwardY + dz / distance * forwardZ;
         return dot >= -0.28F;
      }
   }

   public void clear() {
      this.particles.clear();
      this.gustClock = 0.0F;
   }

   private boolean isPassable(BlockPos position) {
      if (Util.mc.world != null && Util.mc.world.isBlockLoaded(position)) {
         Material material = Util.mc.world.getBlockState(position).getMaterial();
         return material == Material.AIR || material == Material.WATER || material == Material.LAVA || material == Material.VINE || material == Material.PLANTS || material == Material.LEAVES;
      } else {
         return true;
      }
   }

   private void spawn(ParticleType mode, float windStrength, float sizeScale) {
      Entity viewer = Util.mc.getRenderViewEntity();
      if (viewer == null) {
         viewer = Util.mc.player;
      }

      if (viewer != null) {
         Particle particle = new Particle();
         particle.driftAngle = this.random.nextFloat() * ((float)Math.PI * 2F);
         particle.rotation = this.random.nextFloat() * 360.0F;
         particle.prevRotation = particle.rotation;
         particle.flutterPhase = this.random.nextFloat() * ((float)Math.PI * 2F);
         particle.age = 0;
         if (mode == FallingParticleRenderer.ParticleType.Ambient) {
            this.spawnAmbient(particle, viewer, windStrength, sizeScale);
         } else {
            float angle = this.random.nextFloat() * ((float)Math.PI * 2F);
            float distance = (float)Math.sqrt((double)this.random.nextFloat()) * 40.0F;
            particle.x = viewer.posX + Math.cos((double)angle) * (double)distance;
            particle.z = viewer.posZ + Math.sin((double)angle) * (double)distance;
            particle.y = viewer.posY - (double)2.0F + (double)(this.random.nextFloat() * 22.0F);
            particle.prevX = particle.x;
            particle.prevY = particle.y;
            particle.prevZ = particle.z;
            particle.stretch = 1.0F;
            particle.alphaScale = 1.0F;
            if (mode == FallingParticleRenderer.ParticleType.Cherry) {
               particle.velocityY = (double)(-(0.04F + this.random.nextFloat() * 0.03F));
               particle.driftSpeed = 0.06F + this.random.nextFloat() * 0.04F;
               particle.rotationSpeed = (this.random.nextFloat() * 2.0F - 1.0F) * 3.0F;
               particle.size = (0.3F + this.random.nextFloat() * 0.15F) * sizeScale;
               particle.maxAge = 300 + this.random.nextInt(100);
            } else {
               particle.velocityY = (double)(-(0.06F + this.random.nextFloat() * 0.04F));
               particle.driftSpeed = 0.03F + this.random.nextFloat() * 0.02F;
               particle.rotationSpeed = (this.random.nextFloat() * 2.0F - 1.0F) * 1.2F;
               particle.size = (0.22F + this.random.nextFloat() * 0.1F) * sizeScale;
               particle.maxAge = 220 + this.random.nextInt(80);
            }
         }

         this.particles.add(particle);
      }
   }

   private void spawnAmbient(Particle particle, Entity viewer, float windStrength, float sizeScale) {
      float yawRad = (float)Math.toRadians((double)viewer.rotationYaw);
      double forwardX = -Math.sin((double)yawRad);
      double forwardZ = Math.cos((double)yawRad);
      double rightX = Math.cos((double)yawRad);
      double rightZ = Math.sin((double)yawRad);
      boolean foreground = this.random.nextFloat() < 0.42F;
      double forwardDistance = foreground ? (double)3.5F + this.random.nextDouble() * (double)7.5F : (double)8.0F + this.random.nextDouble() * (double)16.0F;
      double sideDistance = (this.random.nextDouble() * (double)2.0F - (double)1.0F) * (foreground ? (double)5.5F : (double)12.0F);
      double verticalDistance = (double)-2.0F + this.random.nextDouble() * (foreground ? (double)8.0F : (double)13.0F);
      particle.x = viewer.posX + forwardX * forwardDistance + rightX * sideDistance;
      particle.y = viewer.posY + (double)viewer.getEyeHeight() + verticalDistance;
      particle.z = viewer.posZ + forwardZ * forwardDistance + rightZ * sideDistance;
      particle.prevX = particle.x;
      particle.prevY = particle.y;
      particle.prevZ = particle.z;
      double crossWind = 0.045 + this.random.nextDouble() * 0.075;
      double towardCamera = 0.11 + this.random.nextDouble() * 0.16;
      double wind = Math.max(0.05, (double)windStrength);
      particle.velocityX = (-forwardX * towardCamera + rightX * crossWind) * wind;
      particle.velocityZ = (-forwardZ * towardCamera + rightZ * crossWind) * wind;
      particle.velocityY = -(0.018 + this.random.nextDouble() * 0.055) * ((double)0.75F + wind * (double)0.25F);
      particle.flutterSpeed = 0.08F + this.random.nextFloat() * 0.13F;
      particle.flutterAmount = 0.0035F + this.random.nextFloat() * 0.01F;
      particle.rotationSpeed = (this.random.nextFloat() * 2.0F - 1.0F) * (0.8F + this.random.nextFloat() * 2.2F);
      float layer = this.random.nextFloat();
      if (layer < 0.56F) {
         particle.size = 0.03F + this.random.nextFloat() * 0.038F;
         particle.alphaScale = 0.56F + this.random.nextFloat() * 0.22F;
         particle.stretch = 0.92F + this.random.nextFloat() * 0.16F;
      } else if (layer < 0.94F) {
         particle.size = 0.058F + this.random.nextFloat() * 0.06F;
         particle.alphaScale = 0.7F + this.random.nextFloat() * 0.22F;
         particle.stretch = 0.94F + this.random.nextFloat() * 0.22F;
      } else {
         particle.size = 0.105F + this.random.nextFloat() * 0.07F;
         particle.alphaScale = 0.78F + this.random.nextFloat() * 0.18F;
         particle.stretch = 0.98F + this.random.nextFloat() * 0.2F;
      }

      particle.maxAge = foreground ? 95 + this.random.nextInt(55) : 150 + this.random.nextInt(100);
      particle.ambient = true;
   }

   public static enum ParticleType {
      Ambient,
      Cherry;
   }

   private static final class Particle {
      double x;
      double y;
      double z;
      double prevX;
      double prevY;
      double prevZ;
      double velocityX;
      double velocityY;
      double velocityZ;
      float driftAngle;
      float driftSpeed;
      float flutterPhase;
      float flutterSpeed;
      float flutterAmount;
      float rotation;
      float prevRotation;
      float rotationSpeed;
      float size;
      float stretch;
      float alphaScale;
      int age;
      int maxAge;
      boolean ambient;

      private Particle() {
      }

      float getLifeAlpha() {
         float t = this.maxAge <= 0 ? 1.0F : (float)this.age / (float)this.maxAge;
         if (t < 0.08F) {
            return t / 0.08F;
         } else {
            return t > 0.78F ? Math.max(0.0F, 1.0F - (t - 0.78F) / 0.22F) : 1.0F;
         }
      }

      boolean dead() {
         return this.age >= this.maxAge;
      }
   }
}
