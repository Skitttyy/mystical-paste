package fat.vonware.features.modules.render;

import fat.vonware.event.events.PacketEvent;
import fat.vonware.event.events.SuffocationEvent;
import fat.vonware.features.modules.Module;
import fat.vonware.features.setting.Setting;
import java.lang.reflect.Method;
import java.util.Comparator;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityEnderCrystal;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.entity.projectile.EntityPotion;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.SPacketAnimation;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityBed;
import net.minecraft.tileentity.TileEntityEnchantmentTable;
import net.minecraft.tileentity.TileEntityEnderChest;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class NoRender extends Module {
   public final Setting<Boolean> noCluster = new Setting<Boolean>("NoCluster", false);
   public final Setting<Float> clusterRange = new Setting<Float>("Cluster-Range", 1.0F, 1.0F, 5.0F, (v) -> (Boolean)this.noCluster.getValue());
   public final Setting<Integer> clusterOpacity = new Setting<Integer>("Cluster-Opacity", 28, 0, 255, (v) -> (Boolean)this.noCluster.getValue());
   public final Setting<Boolean> clusterPlayers = new Setting<Boolean>("Players", true, (v) -> (Boolean)this.noCluster.getValue());
   public final Setting<Boolean> clusterCrystals = new Setting<Boolean>("Crystals", true, (v) -> (Boolean)this.noCluster.getValue());
   public final Setting<Boolean> noCombatTag = new Setting<Boolean>("NoCombatTagRender", false);
   public final Setting<Boolean> fire = new Setting<Boolean>("Fire", true);
   public final Setting<Boolean> totemPops = new Setting<Boolean>("TotemPops", false);
   public final Setting<Boolean> nausea = new Setting<Boolean>("Nausea", true);
   public final Setting<Boolean> hurtCam = new Setting<Boolean>("HurtCam", true);
   public final Setting<Boolean> critParticles = new Setting<Boolean>("CritParticles", false);
   public final Setting<Boolean> dynamicFov = new Setting<Boolean>("DynamicFov", true);
   public final Setting<Boolean> explosions = new Setting<Boolean>("Explosions", true);
   public final Setting<Boolean> blocks = new Setting<Boolean>("Blocks", true);
   public final Setting<Boolean> portal = new Setting<Boolean>("Portal", true);
   public final Setting<Boolean> items = new Setting<Boolean>("Items", false);
   public final Setting<Boolean> potionHud = new Setting<Boolean>("PotionHUD", false);
   public final Setting<Fog> fog;
   public final Setting<Boolean> noWeather;
   public final Setting<Boolean> noArmor;
   public final Setting<Boolean> boss;
   public final Setting<Boolean> xpParticles;
   public final Setting<Boolean> optimizePlayerEntity;
   public final Setting<Boolean> onlyRenderFOV;
   public final Setting<Float> fovPadding;
   public final Setting<ItemFrameMode> itemFrames;
   public final Setting<Integer> maxFrames;
   public final Setting<Float> frameRange;
   public final Setting<Boolean> optimizeCrystal;
   public final Setting<Boolean> crystalFOV;
   public final Setting<Boolean> crystalByDistance;
   public final Setting<Integer> crystalDistance;
   public final Setting<Boolean> enchantBook;
   public final Setting<Boolean> armorStands;
   public final Setting<Boolean> tileEntities;
   public final Setting<Integer> tileRange;
   public final Setting<Boolean> healthPots;
   public final Setting<Boolean> beds;
   public final Setting<Integer> bedRange;
   private static NoRender INSTANCE = new NoRender();

   public NoRender() {
      super("NoRender", "Allows you to stop rendering stuff", Module.Category.RENDER, true, false, false);
      this.fog = new Setting<Fog>("Fog", NoRender.Fog.NONE);
      this.noWeather = new Setting<Boolean>("Weather", false);
      this.noArmor = new Setting<Boolean>("No Armor", true);
      this.boss = new Setting<Boolean>("Boss Title", true);
      this.xpParticles = new Setting<Boolean>("XP Particles", true);
      this.optimizePlayerEntity = new Setting<Boolean>("OptimizePlayer", false);
      this.onlyRenderFOV = new Setting<Boolean>("OnlyRenderFOV", true, (v) -> (Boolean)this.optimizePlayerEntity.getValue());
      this.fovPadding = new Setting<Float>("FOV-Padding", 10.0F, 0.0F, 45.0F, (v) -> (Boolean)this.optimizePlayerEntity.getValue() && (Boolean)this.onlyRenderFOV.getValue());
      this.itemFrames = new Setting<ItemFrameMode>("ItemFrames", NoRender.ItemFrameMode.Off);
      this.maxFrames = new Setting<Integer>("Max-Frames", 10, 1, 256, (v) -> this.itemFrames.getValue() == NoRender.ItemFrameMode.Max);
      this.frameRange = new Setting<Float>("Frame-Range", 16.0F, 1.0F, 64.0F, (v) -> this.itemFrames.getValue() == NoRender.ItemFrameMode.Range);
      this.optimizeCrystal = new Setting<Boolean>("OptimizeCrystal", false);
      this.crystalFOV = new Setting<Boolean>("Crystal-FOV", true, (v) -> (Boolean)this.optimizeCrystal.getValue());
      this.crystalByDistance = new Setting<Boolean>("Crystal-ByDistance", false, (v) -> (Boolean)this.optimizeCrystal.getValue());
      this.crystalDistance = new Setting<Integer>("Crystal-Distance", 20, 5, 64, (v) -> (Boolean)this.optimizeCrystal.getValue() && (Boolean)this.crystalByDistance.getValue());
      this.enchantBook = new Setting<Boolean>("EnchantBook", false);
      this.armorStands = new Setting<Boolean>("ArmorStands", false);
      this.tileEntities = new Setting<Boolean>("TileEntities", false);
      this.tileRange = new Setting<Integer>("TE-Range", 20, 5, 40, (v) -> (Boolean)this.tileEntities.getValue());
      this.healthPots = new Setting<Boolean>("HealthPots", false);
      this.beds = new Setting<Boolean>("Beds", false);
      this.bedRange = new Setting<Integer>("Bed-Range", 20, 5, 40, (v) -> (Boolean)this.beds.getValue());
      this.register(this.noCluster);
      this.register(this.clusterRange);
      this.register(this.clusterOpacity);
      this.register(this.clusterPlayers);
      this.register(this.clusterCrystals);
      this.register(this.optimizePlayerEntity);
      this.register(this.onlyRenderFOV);
      this.register(this.fovPadding);
      this.register(this.optimizeCrystal);
      this.register(this.crystalFOV);
      this.register(this.crystalByDistance);
      this.register(this.crystalDistance);
      this.register(this.noCombatTag);
      this.register(this.fire);
      this.register(this.totemPops);
      this.register(this.potionHud);
      this.register(this.nausea);
      this.register(this.hurtCam);
      this.register(this.critParticles);
      this.register(this.dynamicFov);
      this.register(this.explosions);
      this.register(this.blocks);
      this.register(this.portal);
      this.register(this.items);
      this.register(this.fog);
      this.register(this.noWeather);
      this.register(this.noArmor);
      this.register(this.boss);
      this.register(this.xpParticles);
      this.register(this.enchantBook);
      this.register(this.armorStands);
      this.register(this.tileEntities);
      this.register(this.tileRange);
      this.register(this.healthPots);
      this.register(this.beds);
      this.register(this.bedRange);
      this.register(this.itemFrames);
      this.register(this.maxFrames);
      this.register(this.frameRange);
      INSTANCE = this;
   }

   @SubscribeEvent
   public void onRenderOverlay(RenderGameOverlayEvent.Pre event) {
      if ((Boolean)this.potionHud.getValue()) {
         if (event.getType() == ElementType.POTION_ICONS) {
            event.setCanceled(true);
         }

      }
   }

   public boolean shouldRenderCrystal(Entity entity) {
      if (this.isEnabled() && (Boolean)this.optimizeCrystal.getValue()) {
         if ((Boolean)this.crystalByDistance.getValue()) {
            double dist = (double)(Integer)this.crystalDistance.getValue();
            if (mc.player.getDistanceSq(entity) > dist * dist) {
               return false;
            }
         }

         return !(Boolean)this.crystalFOV.getValue() || this.isInFOV(entity);
      } else {
         return true;
      }
   }

   public boolean shouldRenderItemFrame(Entity entity) {
      if (!this.isEnabled()) {
         return true;
      } else {
         switch ((ItemFrameMode)this.itemFrames.getValue()) {
            case NoFrames:
               return false;
            case Max:
               long visible = mc.world.loadedEntityList.stream().filter((e) -> e instanceof EntityItemFrame).filter((e) -> mc.player.getDistanceSq(e) <= (double)4096.0F).count();
               return mc.player.getDistanceSq(entity) <= (double)4096.0F && mc.world.loadedEntityList.stream().filter((e) -> e instanceof EntityItemFrame).sorted(Comparator.comparingDouble((e) -> mc.player.getDistanceSq(e))).limit((long)(Integer)this.maxFrames.getValue()).anyMatch((e) -> e == entity);
            case Range:
               double range = (double)(Float)this.frameRange.getValue();
               return mc.player.getDistanceSq(entity) <= range * range;
            default:
               return true;
         }
      }
   }

   public boolean shouldRenderPlayerEntity(Entity entity) {
      if (this.isEnabled() && (Boolean)this.optimizePlayerEntity.getValue()) {
         if (!(entity instanceof EntityOtherPlayerMP)) {
            return true;
         } else if (this.isLogoutSpotEntity(entity)) {
            return true;
         } else {
            return !(Boolean)this.onlyRenderFOV.getValue() ? true : this.isInFOV(entity);
         }
      } else {
         return true;
      }
   }

   private boolean isInFOV(Entity entity) {
      Vec3d lookVec = mc.player.getLookVec();
      Vec3d toEntity = (new Vec3d(entity.posX - mc.player.posX, entity.posY + (double)entity.height / (double)2.0F - (mc.player.posY + (double)mc.player.getEyeHeight()), entity.posZ - mc.player.posZ)).normalize();
      double dot = lookVec.dotProduct(toEntity);
      float halfFov = this.getActualFOV() / 2.0F + (Float)this.fovPadding.getValue();
      return dot >= Math.cos(Math.toRadians((double)halfFov));
   }

   private float getActualFOV() {
      try {
         Method m = mc.entityRenderer.getClass().getDeclaredMethod("getFOVModifier", Float.TYPE, Boolean.TYPE);
         m.setAccessible(true);
         return (Float)m.invoke(mc.entityRenderer, 1.0F, true);
      } catch (Exception var2) {
         return mc.gameSettings.fovSetting;
      }
   }

   private boolean isLogoutSpotEntity(Entity entity) {
      if (!(entity instanceof EntityOtherPlayerMP)) {
         return false;
      } else {
         return !mc.world.playerEntities.contains(entity);
      }
   }

   public float getClusterAlpha(Entity entity) {
      if (this.isEnabled() && (Boolean)this.noCluster.getValue()) {
         if (entity == mc.player) {
            return 1.0F;
         } else if (entity instanceof EntityOtherPlayerMP && !(Boolean)this.clusterPlayers.getValue()) {
            return 1.0F;
         } else if (entity instanceof EntityEnderCrystal && !(Boolean)this.clusterCrystals.getValue()) {
            return 1.0F;
         } else if (this.isLogoutSpotEntity(entity)) {
            return 1.0F;
         } else {
            double dist = (double)mc.player.getDistance(entity);
            double range = (double)(Float)this.clusterRange.getValue();
            if (dist >= range) {
               return 1.0F;
            } else {
               float minAlpha = (float)(Integer)this.clusterOpacity.getValue() / 255.0F;
               float t = (float)(dist / range);
               return minAlpha + (1.0F - minAlpha) * t;
            }
         }
      } else {
         return 1.0F;
      }
   }

   public boolean shouldDisableHurtForCluster(Entity entity) {
      if (this.isEnabled() && (Boolean)this.noCluster.getValue()) {
         if (entity == mc.player) {
            return false;
         } else if (this.isLogoutSpotEntity(entity)) {
            return false;
         } else {
            return this.getClusterAlpha(entity) < 1.0F;
         }
      } else {
         return false;
      }
   }

   @SubscribeEvent
   public void NoBlockRender(SuffocationEvent event) {
      if ((Boolean)this.blocks.getValue()) {
         event.setCanceled(true);
      }

   }

   @SubscribeEvent
   public void onPacketReceive(PacketEvent.Receive event) {
      if ((Boolean)this.critParticles.getValue()) {
         if (event.getPacket() instanceof SPacketAnimation) {
            SPacketAnimation packet = (SPacketAnimation)event.getPacket();
            int type = packet.getAnimationType();
            if (type == 4 || type == 5) {
               event.setCanceled(true);
            }
         }

      }
   }

   public boolean noPortal() {
      return this.isEnabled() && (Boolean)this.portal.getValue();
   }

   public boolean noNausea() {
      return this.isEnabled() && (Boolean)this.nausea.getValue();
   }

   public boolean dynamicFov() {
      return this.isEnabled() && (Boolean)this.dynamicFov.getValue();
   }

   public boolean noFire() {
      return this.isEnabled() && (Boolean)this.fire.getValue();
   }

   public boolean noTotems() {
      return this.isEnabled() && (Boolean)this.totemPops.getValue();
   }

   public boolean noHurtCam() {
      return this.isEnabled() && (Boolean)this.hurtCam.getValue();
   }

   public boolean noEnchantBook() {
      return this.isEnabled() && (Boolean)this.enchantBook.getValue();
   }

   public boolean noArmorStands() {
      return this.isEnabled() && (Boolean)this.armorStands.getValue();
   }

   public boolean shouldRenderTileEntity(TileEntity tileEntity) {
      if (this.isEnabled() && (Boolean)this.tileEntities.getValue()) {
         if (!(tileEntity instanceof TileEntityEnderChest) && !(tileEntity instanceof TileEntityEnchantmentTable)) {
            return true;
         } else {
            double range = (double)(Integer)this.tileRange.getValue();
            return mc.player.getDistanceSq(tileEntity.getPos()) < range * range;
         }
      } else {
         return true;
      }
   }

   public boolean shouldRenderBed(TileEntity bed) {
      if (this.isEnabled() && (Boolean)this.beds.getValue()) {
         if (!(bed instanceof TileEntityBed)) {
            return true;
         } else {
            double range = (double)(Integer)this.bedRange.getValue();
            return mc.player.getDistanceSq(bed.getPos()) < range * range;
         }
      } else {
         return true;
      }
   }

   public boolean shouldRenderHealthPotion(EntityPotion potion) {
      if (this.isEnabled() && (Boolean)this.healthPots.getValue()) {
         ItemStack stack = potion.getPotion();
         return stack.getItem() != Items.SPLASH_POTION && stack.getItem() != Items.LINGERING_POTION ? true : true;
      } else {
         return true;
      }
   }

   public void onUpdate() {
      if ((Boolean)this.items.getValue()) {
         mc.world.loadedEntityList.stream().filter(EntityItem.class::isInstance).map(EntityItem.class::cast).forEach(Entity::setDead);
      }

      if ((Boolean)this.healthPots.getValue()) {
         mc.world.loadedEntityList.stream().filter(EntityPotion.class::isInstance).map(EntityPotion.class::cast).filter((potion) -> {
            ItemStack stack = potion.getPotion();
            return stack.getItem() == Items.SPLASH_POTION || stack.getItem() == Items.LINGERING_POTION;
         }).forEach(Entity::setDead);
      }

      if ((Boolean)this.noWeather.getValue() && mc.world.isRaining()) {
         mc.world.setRainStrength(0.0F);
      }

   }

   public static NoRender getInstance() {
      if (INSTANCE == null) {
         INSTANCE = new NoRender();
      }

      return INSTANCE;
   }

   public static enum ItemFrameMode {
      Off,
      NoFrames,
      Max,
      Range;
   }

   public static enum Fog {
      NONE,
      AIR,
      NOFOG;
   }
}
