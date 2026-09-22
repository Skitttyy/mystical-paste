package fat.vonware.util.OtherRenderShit.Render;

import java.util.Collection;
import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.FoodStats;
import net.minecraft.util.MovementInputFromOptions;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class FreecamEntity extends EntityPlayerSP {
   private boolean follow;
   private float hSpeed;
   private float vSpeed;

   public FreecamEntity(float hSpeed, float vSpeed) {
      super(Minecraft.getMinecraft(), Minecraft.getMinecraft().world, Minecraft.getMinecraft().player.connection, Minecraft.getMinecraft().player.getStatFileWriter(), Minecraft.getMinecraft().player.getRecipeBook());
      this.hSpeed = hSpeed;
      this.vSpeed = vSpeed;
      this.noClip = true;
      this.copyLocationAndAnglesFrom(this.mc.player);
      this.setHealth(Minecraft.getMinecraft().player.getHealth());
      this.rotationYawHead = Minecraft.getMinecraft().player.rotationYawHead;
      this.prevRotationYaw = Minecraft.getMinecraft().player.prevRotationYaw;
      this.prevRotationPitch = Minecraft.getMinecraft().player.prevRotationPitch;
      this.prevRotationYawHead = Minecraft.getMinecraft().player.prevRotationYawHead;
      this.inventory = Minecraft.getMinecraft().player.inventory;
      this.inventoryContainer = Minecraft.getMinecraft().player.inventoryContainer;
      NBTTagCompound compound = new NBTTagCompound();
      Minecraft.getMinecraft().player.capabilities.writeCapabilitiesToNBT(compound);
      this.capabilities.readCapabilitiesFromNBT(compound);
      this.capabilities.isFlying = true;
      this.attackedAtYaw = Minecraft.getMinecraft().player.attackedAtYaw;
      this.movementInput = new MovementInputFromOptions(Minecraft.getMinecraft().gameSettings);
   }

   public void writeEntityToNBT(NBTTagCompound compound) {
   }

   public void readEntityFromNBT(NBTTagCompound compound) {
   }

   public boolean isInsideOfMaterial(Material material) {
      return Minecraft.getMinecraft().player.isInsideOfMaterial(material);
   }

   public Map<Potion, PotionEffect> getActivePotionMap() {
      return Minecraft.getMinecraft().player.getActivePotionMap();
   }

   public Collection<PotionEffect> getActivePotionEffects() {
      return Minecraft.getMinecraft().player.getActivePotionEffects();
   }

   public int getTotalArmorValue() {
      return Minecraft.getMinecraft().player.getTotalArmorValue();
   }

   public float getAbsorptionAmount() {
      return Minecraft.getMinecraft().player.getAbsorptionAmount();
   }

   public boolean isPotionActive(Potion potion) {
      return Minecraft.getMinecraft().player.isPotionActive(potion);
   }

   public PotionEffect getActivePotionEffect(Potion potion) {
      return Minecraft.getMinecraft().player.getActivePotionEffect(potion);
   }

   public FoodStats getFoodStats() {
      return Minecraft.getMinecraft().player.getFoodStats();
   }

   public boolean canTriggerWalking() {
      return false;
   }

   public AxisAlignedBB getCollisionBox(Entity entity) {
      return null;
   }

   public AxisAlignedBB getCollisionBoundingBox() {
      return null;
   }

   public AxisAlignedBB getEntityBoundingBox() {
      return new AxisAlignedBB(this.posX, this.posY, this.posZ, this.posX, this.posY, this.posZ);
   }

   public boolean canBePushed() {
      return false;
   }

   public void applyEntityCollision(Entity entity) {
   }

   public boolean attackEntityFrom(DamageSource source, float amount) {
      return false;
   }

   public boolean canBeAttackedWithItem() {
      return false;
   }

   public boolean canBeCollidedWith() {
      return false;
   }

   public boolean canBeRidden(Entity entity) {
      return false;
   }

   public boolean canRenderOnFire() {
      return false;
   }

   public boolean canTrample(World world, Block block, BlockPos pos, float fallDistance) {
      return false;
   }

   public void doBlockCollisions() {
   }

   public void updateFallState(double y, boolean onGroundIn, IBlockState state, BlockPos pos) {
   }

   public boolean getIsInvulnerable() {
      return true;
   }

   public EnumPushReaction getPushReaction() {
      return EnumPushReaction.IGNORE;
   }

   public boolean hasNoGravity() {
      return true;
   }

   public void onLivingUpdate() {
      this.motionX = (double)0.0F;
      this.motionY = (double)0.0F;
      this.motionZ = (double)0.0F;
      this.movementInput.updatePlayerMoveState();
      float up = this.movementInput.jump ? 1.0F : (this.movementInput.sneak ? -1.0F : 0.0F);
      this.setMotion(this.movementInput.moveStrafe, up, this.movementInput.moveForward);
      if (Minecraft.getMinecraft().gameSettings.keyBindSprint.isKeyDown()) {
         this.motionX *= (double)2.0F;
         this.motionY *= (double)2.0F;
         this.motionZ *= (double)2.0F;
         this.setSprinting(true);
      } else {
         this.setSprinting(false);
      }

      this.setPosition(this.posX + this.motionX, this.posY + this.motionY, this.posZ + this.motionZ);
   }

   public void setMotion(float strafe, float up, float forward) {
      float side = strafe;
      float moveYaw = this.rotationYaw + (this.rotationYaw - this.prevRotationYaw) * this.mc.getRenderPartialTicks();
      if (forward != 0.0F) {
         if (strafe > 0.0F) {
            moveYaw += (float)(forward > 0.0F ? -45 : 45);
         } else if (strafe < 0.0F) {
            moveYaw += (float)(forward > 0.0F ? 45 : -45);
         }

         side = 0.0F;
         if (forward > 0.0F) {
            forward = 1.0F;
         } else if (forward < 0.0F) {
            forward = -1.0F;
         }
      }

      double sin = Math.sin(Math.toRadians((double)(moveYaw + 90.0F)));
      double cos = Math.cos(Math.toRadians((double)(moveYaw + 90.0F)));
      this.motionX = (double)(forward * this.hSpeed) * cos + (double)(side * this.hSpeed) * sin;
      this.motionY = (double)up * (double)this.vSpeed;
      this.motionZ = (double)(forward * this.hSpeed) * sin - (double)(side * this.hSpeed) * cos;
   }

   public void sethSpeed(float hSpeed) {
      this.hSpeed = hSpeed;
   }

   public void setvSpeed(float vSpeed) {
      this.vSpeed = vSpeed;
   }
}
