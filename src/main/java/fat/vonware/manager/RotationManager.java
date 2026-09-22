package fat.vonware.manager;

import fat.vonware.event.events.MotionUpdateEvent;
import fat.vonware.event.events.PacketEvent;
import fat.vonware.features.Feature;
import fat.vonware.mixin.mixins.accessor.IEntityPlayerSP;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.network.play.client.CPacketEntityAction;
import net.minecraft.network.play.client.CPacketHeldItemChange;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.network.play.server.SPacketPlayerPosLook;
import net.minecraft.network.play.server.SPacketPlayerPosLook.EnumFlags;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class RotationManager extends Feature {
   public static RotationManager INSTANCE;
   private final List<RotationRequest> rotationRequests = new ArrayList();
   private long requestOrder;
   private RotationSource activeRotationSource;
   private static final int ROTATION_GRACE_TICKS = 1;
   private RotationRequest lastResolvedRequest;
   private int lastResolvedTick;
   private volatile float serverYaw;
   private volatile float serverPitch;
   private float yaw;
   private float pitch;
   private float renderYaw;
   private float renderPitch;
   private float renderYawOffset;
   private float prevYaw;
   private float prevPitch;
   private float prevRenderYawOffset;
   private float prevRotationYawHead;
   private float rotationYawHead;
   private int ticksExisted;
   private int serverSlot;
   private boolean blocking;
   public boolean sneaking;
   public boolean sprinting;
   private float smoothedRenderYaw;
   private float smoothedRenderPitch;
   private float smoothedRenderYawOffset;
   private float smoothedRotationYawHead;
   private float prevSmoothedRenderYaw;
   private float prevSmoothedRenderPitch;
   private float prevSmoothedRenderYawOffset;
   private float prevSmoothedRotationYawHead;
   private boolean moduleRotating;
   private static final float VISUAL_SMOOTH_FACTOR = 0.35F;
   private float savedRenderYawOffset;
   private float savedRotationYawHead;
   private float savedPrevRenderYawOffset;
   private float savedPrevRotationYawHead;

   public RotationManager() {
      this.activeRotationSource = RotationManager.RotationSource.OTHER;
      this.lastResolvedTick = Integer.MIN_VALUE;
      this.serverSlot = -1;
      INSTANCE = this;
   }

   public boolean isModuleRotating() {
      return this.moduleRotating;
   }

   public void init() {
      MinecraftForge.EVENT_BUS.register(this);
   }

   public void onUnload() {
      MinecraftForge.EVENT_BUS.unregister(this);
   }

   private float lerpAngle(float from, float to, float factor) {
      float delta = MathHelper.wrapDegrees(to - from);
      return from + delta * factor;
   }

   @SubscribeEvent
   public void onPacketSend(PacketEvent.Send event) {
      if (event.getPacket() instanceof CPacketHeldItemChange) {
         this.serverSlot = ((CPacketHeldItemChange)event.getPacket()).getSlotId();
      }

      if (event.getPacket() instanceof CPacketEntityAction) {
         CPacketEntityAction packet = (CPacketEntityAction)event.getPacket();
         switch (packet.getAction()) {
            case START_SPRINTING:
               this.sprinting = true;
               break;
            case STOP_SPRINTING:
               this.sprinting = false;
               break;
            case START_SNEAKING:
               this.sneaking = true;
               break;
            case STOP_SNEAKING:
               this.sneaking = false;
         }
      }

   }

   @SubscribeEvent
   public void onPacketPost(PacketEvent.Post event) {
      if (event.getPacket() instanceof CPacketPlayer) {
         this.readCPacket((CPacketPlayer)event.getPacket());
      }

   }

   @SubscribeEvent
   public void onPacketReceive(PacketEvent.Receive event) {
      if (event.getPacket() instanceof SPacketPlayerPosLook && mc.player != null) {
         SPacketPlayerPosLook packet = (SPacketPlayerPosLook)event.getPacket();
         float yaw = packet.getYaw();
         float pitch = packet.getPitch();
         if (packet.getFlags().contains(EnumFlags.X_ROT)) {
            yaw += mc.player.rotationYaw;
         }

         if (packet.getFlags().contains(EnumFlags.Y_ROT)) {
            pitch += mc.player.rotationPitch;
         }

         this.setServerRotations(yaw, pitch);
      }
   }

   public void requestRotation(RotationSource source, float yaw, float pitch) {
      this.requestRotation(source, yaw, pitch, false);
   }

   public void requestRotation(RotationSource source, float yaw, float pitch, boolean forced) {
      if (source != null && !Float.isNaN(yaw) && !Float.isNaN(pitch) && !Float.isInfinite(yaw) && !Float.isInfinite(pitch)) {
         this.rotationRequests.add(new RotationRequest(source, yaw, MathHelper.clamp(pitch, -90.0F, 90.0F), forced, (long)(this.requestOrder++)));
      }
   }

   private RotationRequest resolveRotationRequest() {
      return this.rotationRequests.stream().max(Comparator.comparing((RotationRequest request) -> request.forced).thenComparingInt((RotationRequest request) -> request.source.getWeight()).thenComparingLong((RotationRequest request) -> request.order)).orElse(null);
   }

   private void clearRotationRequests() {
      this.rotationRequests.clear();
      this.requestOrder = 0L;
   }

   public RotationSource getActiveRotationSource() {
      return this.activeRotationSource;
   }

   @SubscribeEvent(
      priority = EventPriority.LOWEST
   )
   public void onUpdateWalkingPlayer(MotionUpdateEvent event) {
      if (event.getStage() == 0) {
         RotationRequest winner = this.resolveRotationRequest();
         int currentTick = mc.player == null ? Integer.MIN_VALUE : mc.player.ticksExisted;
         if (winner != null) {
            this.lastResolvedRequest = winner;
            this.lastResolvedTick = currentTick;
         } else if (this.lastResolvedRequest != null && currentTick != Integer.MIN_VALUE && currentTick - this.lastResolvedTick <= 1) {
            winner = this.lastResolvedRequest;
         } else {
            this.lastResolvedRequest = null;
         }

         if (winner != null) {
            event.setYaw(winner.yaw);
            event.setPitch(winner.pitch);
            this.activeRotationSource = winner.source;
            this.moduleRotating = true;
         } else {
            float clampedPitch = MathHelper.clamp(event.getPitch(), -90.0F, 90.0F);
            if (Float.compare(clampedPitch, event.getPitch()) != 0) {
               event.setPitch(clampedPitch);
            }

            this.activeRotationSource = RotationManager.RotationSource.OTHER;
            if (this.moduleRotating) {
               this.resetVisualRotations();
            }

            this.moduleRotating = false;
         }

         this.set(event.getYaw(), event.getPitch());
         this.clearRotationRequests();
      }
   }

   public void readCPacket(CPacketPlayer packetIn) {
      if (mc.player != null) {
         ((IEntityPlayerSP)mc.player).setLastReportedYaw(packetIn.getYaw(((IEntityPlayerSP)mc.player).getLastReportedYaw()));
         ((IEntityPlayerSP)mc.player).setLastReportedPitch(packetIn.getPitch(((IEntityPlayerSP)mc.player).getLastReportedPitch()));
      }

      this.setServerRotations(packetIn.getYaw(this.serverYaw), MathHelper.clamp(packetIn.getPitch(this.serverPitch), -90.0F, 90.0F));
   }

   public void setServerRotations(float yaw, float pitch) {
      this.serverYaw = yaw;
      this.serverPitch = MathHelper.clamp(pitch, -90.0F, 90.0F);
   }

   private void set(float yaw, float pitch) {
      if (mc.player != null && mc.player.ticksExisted != this.ticksExisted) {
         this.ticksExisted = mc.player.ticksExisted;
         this.prevYaw = this.renderYaw;
         this.prevPitch = this.renderPitch;
         this.prevRenderYawOffset = this.renderYawOffset;
         this.prevRotationYawHead = this.rotationYawHead;
         this.renderYawOffset = this.getRenderYawOffset(yaw, this.prevRenderYawOffset);
         this.rotationYawHead = yaw;
         this.renderYaw = yaw;
         this.renderPitch = pitch;
         this.prevSmoothedRenderYaw = this.smoothedRenderYaw;
         this.prevSmoothedRenderPitch = this.smoothedRenderPitch;
         this.prevSmoothedRenderYawOffset = this.smoothedRenderYawOffset;
         this.prevSmoothedRotationYawHead = this.smoothedRotationYawHead;
         this.smoothedRenderYaw = this.lerpAngle(this.smoothedRenderYaw, yaw, 0.35F);
         this.smoothedRenderPitch = this.lerpAngle(this.smoothedRenderPitch, pitch, 0.35F);
         this.smoothedRenderYawOffset = this.lerpAngle(this.smoothedRenderYawOffset, this.renderYawOffset, 0.35F);
         this.smoothedRotationYawHead = this.lerpAngle(this.smoothedRotationYawHead, yaw, 0.35F);
      }
   }

   private float getRenderYawOffset(float yaw, float offsetIn) {
      float result = offsetIn;
      double xDif = mc.player.posX - mc.player.prevPosX;
      double zDif = mc.player.posZ - mc.player.prevPosZ;
      if (xDif * xDif + zDif * zDif > (double)0.0025000002F) {
         float offset = (float)MathHelper.atan2(zDif, xDif) * (180F / (float)Math.PI) - 90.0F;
         float wrap = MathHelper.abs(MathHelper.wrapDegrees(yaw) - offset);
         if (95.0F < wrap && wrap < 265.0F) {
            result = offset - 180.0F;
         } else {
            result = offset;
         }
      }

      if (mc.player.swingProgress > 0.0F) {
         result = yaw;
      }

      result = offsetIn + MathHelper.wrapDegrees(result - offsetIn) * 0.3F;
      float offset = MathHelper.wrapDegrees(yaw - result);
      if (offset < -75.0F) {
         offset = -75.0F;
      } else if (offset >= 75.0F) {
         offset = 75.0F;
      }

      result = yaw - offset;
      if (offset * offset > 2500.0F) {
         result += offset * 0.2F;
      }

      return result;
   }

   private void resetVisualRotations() {
      if (mc.player != null) {
         this.renderYaw = mc.player.rotationYaw;
         this.renderPitch = mc.player.rotationPitch;
         this.renderYawOffset = mc.player.renderYawOffset;
         this.rotationYawHead = mc.player.rotationYawHead;
         this.prevYaw = this.renderYaw;
         this.prevPitch = this.renderPitch;
         this.prevRenderYawOffset = this.renderYawOffset;
         this.prevRotationYawHead = this.rotationYawHead;
         this.smoothedRenderYaw = this.renderYaw;
         this.smoothedRenderPitch = this.renderPitch;
         this.smoothedRenderYawOffset = this.renderYawOffset;
         this.smoothedRotationYawHead = this.rotationYawHead;
         this.prevSmoothedRenderYaw = this.smoothedRenderYaw;
         this.prevSmoothedRenderPitch = this.smoothedRenderPitch;
         this.prevSmoothedRenderYawOffset = this.smoothedRenderYawOffset;
         this.prevSmoothedRotationYawHead = this.smoothedRotationYawHead;
      }
   }

   public void savePlayerRotations() {
      if (mc.player != null) {
         this.savedRenderYawOffset = mc.player.renderYawOffset;
         this.savedRotationYawHead = mc.player.rotationYawHead;
         this.savedPrevRenderYawOffset = mc.player.prevRenderYawOffset;
         this.savedPrevRotationYawHead = mc.player.prevRotationYawHead;
      }
   }

   public void applyPlayerRotations(float partialTicks) {
      if (mc.player != null && this.moduleRotating) {
         float interpYawOffset = this.prevSmoothedRenderYawOffset + MathHelper.wrapDegrees(this.smoothedRenderYawOffset - this.prevSmoothedRenderYawOffset) * partialTicks;
         float interpYawHead = this.prevSmoothedRotationYawHead + MathHelper.wrapDegrees(this.smoothedRotationYawHead - this.prevSmoothedRotationYawHead) * partialTicks;
         mc.player.renderYawOffset = interpYawOffset;
         mc.player.rotationYawHead = interpYawHead;
         mc.player.prevRenderYawOffset = this.prevSmoothedRenderYawOffset;
         mc.player.prevRotationYawHead = this.prevSmoothedRotationYawHead;
      }
   }

   public void applyPlayerRotations() {
      this.applyPlayerRotations(1.0F);
   }

   public void restorePlayerRotations() {
      if (mc.player != null) {
         mc.player.renderYawOffset = this.savedRenderYawOffset;
         mc.player.rotationYawHead = this.savedRotationYawHead;
         mc.player.prevRenderYawOffset = this.savedPrevRenderYawOffset;
         mc.player.prevRotationYawHead = this.savedPrevRotationYawHead;
      }
   }

   public float getServerYaw() {
      return this.serverYaw;
   }

   public float getServerPitch() {
      return this.serverPitch;
   }

   public float getRenderYaw() {
      return this.renderYaw;
   }

   public float getRenderPitch() {
      return this.renderPitch;
   }

   public float getRotationYawHead() {
      return this.rotationYawHead;
   }

   public float getRenderYawOffset() {
      return this.renderYawOffset;
   }

   public float getPrevYaw() {
      return this.prevYaw;
   }

   public float getPrevPitch() {
      return this.prevPitch;
   }

   public float getPrevRotationYawHead() {
      return this.prevRotationYawHead;
   }

   public float getPrevRenderYawOffset() {
      return this.prevRenderYawOffset;
   }

   public float getSmoothedRenderYaw() {
      return this.smoothedRenderYaw;
   }

   public float getSmoothedRenderPitch() {
      return this.smoothedRenderPitch;
   }

   public float getSmoothedRenderYawOffset() {
      return this.smoothedRenderYawOffset;
   }

   public float getSmoothedRotationYawHead() {
      return this.smoothedRotationYawHead;
   }

   public float getPrevSmoothedRenderYaw() {
      return this.prevSmoothedRenderYaw;
   }

   public float getPrevSmoothedRenderPitch() {
      return this.prevSmoothedRenderPitch;
   }

   public float getPrevSmoothedRenderYawOffset() {
      return this.prevSmoothedRenderYawOffset;
   }

   public float getPrevSmoothedRotationYawHead() {
      return this.prevSmoothedRotationYawHead;
   }

   public float getYaw() {
      return this.yaw;
   }

   public float getPitch() {
      return this.pitch;
   }

   public void setYaw(float yaw) {
      this.yaw = yaw;
   }

   public void setPitch(float pitch) {
      this.pitch = pitch;
   }

   public int getServerSlot() {
      return this.serverSlot;
   }

   public void setBlocking(boolean blocking) {
      this.blocking = blocking;
   }

   public boolean isBlocking() {
      return this.blocking;
   }

   public static enum RotationSource {
      AURA(10),
      AUTO_CRYSTAL(20),
      HOLE_FILL(79),
      AUTO_TRAP(40),
      FAST_BREAK(50),
      AUTO_CITY(60),
      SCAFFOLD(70),
      SURROUND(80),
      SELF_TRAP(90),
      EMERGENCY(100),
      OTHER(0);

      private final int weight;

      private RotationSource(int weight) {
         this.weight = weight;
      }

      public int getWeight() {
         return this.weight;
      }
   }

   private static final class RotationRequest {
      private final RotationSource source;
      private final float yaw;
      private final float pitch;
      private final boolean forced;
      private final long order;

      private RotationRequest(RotationSource source, float yaw, float pitch, boolean forced, long order) {
         this.source = source;
         this.yaw = yaw;
         this.pitch = pitch;
         this.forced = forced;
         this.order = order;
      }
   }
}
