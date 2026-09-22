package fat.vonware.features.modules.player;

import com.mojang.authlib.GameProfile;
import fat.vonware.event.events.DeathEvent;
import fat.vonware.features.command.Command;
import fat.vonware.features.modules.Module;
import fat.vonware.features.setting.Setting;
import fat.vonware.util.FakePlayer.FakePlayerUtil;
import fat.vonware.util.FakePlayer.Position;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.datasync.DataParameter;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class FakePlayer extends Module {
   public Setting<Boolean> record = new Setting<Boolean>("Record", false, "Records your movement for later FakePlayer playback.");
   public Setting<Boolean> playRecording = new Setting<Boolean>("Play", false, "Plays the recorded movement through the spawned fake player.");
   public Setting<Boolean> loop = new Setting<Boolean>("Loop", true, "Restarts the recorded movement when playback reaches the end.");
   public Setting<Boolean> circle = new Setting<Boolean>("Circle", false, "Makes the fake player move in a circle around its origin.");
   public Setting<Float> circleRange = new Setting<Float>("Circle Range", 3.0F, 1.0F, 10.0F, (v) -> (Boolean)this.circle.getValue(), "Radius of the FakePlayer circular movement.");
   public Setting<Float> circleSpeed = new Setting<Float>("Circle Speed", 1.0F, 0.1F, 5.0F, (v) -> (Boolean)this.circle.getValue(), "Speed of the FakePlayer circular movement.");
   protected final int playerId = -2147483647;
   protected final ArrayList<Position> playerPositions;
   private boolean wasRecording = false;
   private boolean wasPlaying = false;
   private boolean wasCircling = false;
   private int playbackIndex = 0;
   private int recordTick = 0;
   private int circleTick = 0;
   private double circleCenterX = (double)0.0F;
   private double circleCenterY = (double)0.0F;
   private double circleCenterZ = (double)0.0F;

   public FakePlayer() {
      super("FakePlayer", "Spawns a Fake Player", Module.Category.PLAYER, true, false, false);
      this.register(this.record);
      this.register(this.playRecording);
      this.register(this.loop);
      this.register(this.circle);
      this.register(this.circleRange);
      this.register(this.circleSpeed);
      this.playerPositions = new ArrayList();
   }

   public String getDisplayInfo() {
      if ((Boolean)this.record.getValue()) {
         return "Recording: " + this.playerPositions.size();
      } else if ((Boolean)this.playRecording.getValue()) {
         return "Playing: " + this.playbackIndex + "/" + this.playerPositions.size();
      } else {
         return (Boolean)this.circle.getValue() ? "Circling" : "Fakeplayer";
      }
   }

   public void onEnable() {
      if (!fullNullCheck()) {
         this.record.setValue(false);
         this.playRecording.setValue(false);
         this.circle.setValue(false);
         this.wasRecording = false;
         this.wasPlaying = false;
         this.wasCircling = false;
         this.playbackIndex = 0;
         this.recordTick = 0;
         this.circleTick = 0;
         GameProfile realProfile = mc.player.getGameProfile();
         GameProfile fakeProfile = new GameProfile(realProfile.getId(), "Fakeplayer");
         fakeProfile.getProperties().putAll(realProfile.getProperties());
         FakePlayerUtil.addFakePlayerToWorld("fakeplayer_module", fakeProfile, -2147483647);
         EntityOtherPlayerMP fakePlayer = this.getPlayer();
         if (fakePlayer == null) {
            Command.sendSilentMessage("Failed to spawn fake player");
            this.disable();
         } else {
            this.copySkinLayers(mc.player, fakePlayer);
            mc.getSkinManager().loadProfileTextures(fakeProfile, (type, location, profileTexture) -> fakePlayer.refreshDisplayName(), true);
            fakePlayer.getLocationSkin();
         }
      }
   }

   private void copySkinLayers(EntityPlayer source, EntityOtherPlayerMP target) {
      try {
         for(Field field : EntityPlayer.class.getDeclaredFields()) {
            if (field.getType().equals(DataParameter.class)) {
               field.setAccessible(true);
               Object obj = field.get((Object)null);
               if (obj instanceof DataParameter) {
                  DataParameter<Byte> param = (DataParameter<Byte>)obj;

                  try {
                     byte value = (Byte)source.getDataManager().get(param);
                     target.getDataManager().set(param, value);
                  } catch (ClassCastException var11) {
                  }
               }
            }
         }
      } catch (Exception e) {
         e.printStackTrace();
      }

   }

   @SubscribeEvent
   public void onDeath(DeathEvent event) {
      if (event.getEntity() == mc.player) {
         this.disable();
      }

   }

   public void onLogout() {
      this.disable();
   }

   public void onUpdate() {
      if (!fullNullCheck()) {
         EntityOtherPlayerMP fakePlayer = this.getPlayer();
         if (fakePlayer != null) {
            if ((Boolean)this.record.getValue() && !this.wasRecording) {
               this.startRecording();
            } else if (!(Boolean)this.record.getValue() && this.wasRecording) {
               this.stopRecording();
            }

            if ((Boolean)this.playRecording.getValue() && !this.wasPlaying) {
               this.startPlayback();
            } else if (!(Boolean)this.playRecording.getValue() && this.wasPlaying) {
               this.stopPlayback();
            }

            if ((Boolean)this.circle.getValue() && !this.wasCircling) {
               this.startCircle();
            } else if (!(Boolean)this.circle.getValue() && this.wasCircling) {
               this.stopCircle();
            }

            if ((Boolean)this.record.getValue() && this.wasRecording) {
               this.recordFrame();
            }

            if ((Boolean)this.playRecording.getValue() && this.wasPlaying) {
               this.playbackFrame();
            }

            if ((Boolean)this.circle.getValue() && this.wasCircling) {
               this.circleFrame();
            }

         }
      }
   }

   private void startRecording() {
      if ((Boolean)this.playRecording.getValue()) {
         this.playRecording.setValue(false);
         this.stopPlayback();
      }

      if ((Boolean)this.circle.getValue()) {
         this.circle.setValue(false);
         this.stopCircle();
      }

      this.playerPositions.clear();
      this.recordTick = 0;
      this.wasRecording = true;
      Command.sendSilentMessage("Started recording fake player");
   }

   private void stopRecording() {
      this.wasRecording = false;
      this.recordTick = 0;
      Command.sendSilentMessage("Stopped recording. Recorded " + this.playerPositions.size() + " frames");
      if (!this.playerPositions.isEmpty()) {
         Position startPos = (Position)this.playerPositions.get(0);
         EntityOtherPlayerMP fakePlayer = this.getPlayer();
         if (fakePlayer != null) {
            this.applyPositionSmooth(fakePlayer, startPos);
         }
      }

   }

   private void recordFrame() {
      if (mc.player != null) {
         this.playerPositions.add(new Position(mc.player));
         ++this.recordTick;
      }

   }

   private void startPlayback() {
      if ((Boolean)this.record.getValue()) {
         this.record.setValue(false);
         this.stopRecording();
      }

      if ((Boolean)this.circle.getValue()) {
         this.circle.setValue(false);
         this.stopCircle();
      }

      if (this.playerPositions.isEmpty()) {
         Command.sendSilentMessage("No recording to play");
         this.playRecording.setValue(false);
      } else {
         this.playbackIndex = 0;
         this.wasPlaying = true;
         Command.sendSilentMessage("Started playing recording (" + this.playerPositions.size() + " frames)");
      }
   }

   private void stopPlayback() {
      this.wasPlaying = false;
      this.playbackIndex = 0;
      EntityOtherPlayerMP fakePlayer = this.getPlayer();
      if (fakePlayer != null) {
         fakePlayer.motionX = (double)0.0F;
         fakePlayer.motionY = (double)0.0F;
         fakePlayer.motionZ = (double)0.0F;
      }

      Command.sendSilentMessage("Stopped playing recording");
   }

   private void playbackFrame() {
      EntityOtherPlayerMP fakePlayer = this.getPlayer();
      if (fakePlayer != null && !this.playerPositions.isEmpty()) {
         if (this.playbackIndex >= this.playerPositions.size()) {
            if (!(Boolean)this.loop.getValue()) {
               Command.sendSilentMessage("Finished playing recording");
               this.playRecording.setValue(false);
               this.stopPlayback();
               return;
            }

            this.playbackIndex = 0;
         }

         Position pos = (Position)this.playerPositions.get(this.playbackIndex);
         this.applyPositionSmooth(fakePlayer, pos);
         ++this.playbackIndex;
      }
   }

   private void stopCircle() {
      this.wasCircling = false;
      this.circleTick = 0;
      EntityOtherPlayerMP fakePlayer = this.getPlayer();
      if (fakePlayer != null) {
         fakePlayer.motionX = (double)0.0F;
         fakePlayer.motionY = (double)0.0F;
         fakePlayer.motionZ = (double)0.0F;
      }

      Command.sendSilentMessage("Stopped circle movement");
   }

   private void startCircle() {
      if ((Boolean)this.record.getValue()) {
         this.record.setValue(false);
         this.stopRecording();
      }

      if ((Boolean)this.playRecording.getValue()) {
         this.playRecording.setValue(false);
         this.stopPlayback();
      }

      EntityOtherPlayerMP fakePlayer = this.getPlayer();
      if (fakePlayer != null) {
         this.circleCenterX = fakePlayer.posX;
         this.circleCenterY = fakePlayer.posY;
         this.circleCenterZ = fakePlayer.posZ;
      }

      this.circleTick = 0;
      this.wasCircling = true;
      Command.sendSilentMessage("Started circle movement");
   }

   private void circleFrame() {
      EntityOtherPlayerMP fakePlayer = this.getPlayer();
      if (fakePlayer != null) {
         if (mc.player.ticksExisted % 2 == 0) {
            double angleIncrement = (double)(Float)this.circleSpeed.getValue() * 0.05;
            double angle = (double)this.circleTick * angleIncrement;
            double radius = (double)(Float)this.circleRange.getValue();
            double x = this.circleCenterX + Math.cos(angle) * radius;
            double z = this.circleCenterZ + Math.sin(angle) * radius;
            double y = this.circleCenterY;
            float yaw = (float)Math.toDegrees(angle) + 90.0F;
            double velocity = angleIncrement * radius;
            double motionX = -Math.sin(angle) * velocity;
            double motionZ = Math.cos(angle) * velocity;
            fakePlayer.setPositionAndRotationDirect(x, y, z, yaw, 0.0F, 3, false);
            fakePlayer.rotationYaw = yaw;
            fakePlayer.rotationPitch = 0.0F;
            fakePlayer.rotationYawHead = yaw;
            fakePlayer.motionX = motionX;
            fakePlayer.motionY = (double)0.0F;
            fakePlayer.motionZ = motionZ;
            fakePlayer.onGround = true;
            ++this.circleTick;
         }

      }
   }

   private void applyPositionSmooth(EntityOtherPlayerMP fakePlayer, Position pos) {
      if (mc.player.ticksExisted % 2 == 0) {
         fakePlayer.rotationYaw = pos.getYaw();
         fakePlayer.rotationPitch = pos.getPitch();
         fakePlayer.rotationYawHead = pos.getHead();
         fakePlayer.setPositionAndRotationDirect(pos.getX(), pos.getY(), pos.getZ(), pos.getYaw(), pos.getPitch(), 3, false);
         fakePlayer.motionX = pos.getMotionX();
         fakePlayer.motionY = pos.getMotionY();
         fakePlayer.motionZ = pos.getMotionZ();
      }

      fakePlayer.setSneaking(pos.isSneaking());
   }

   private EntityOtherPlayerMP getPlayer() {
      return FakePlayerUtil.getPlayer("fakeplayer_module");
   }

   public void onDisable() {
      this.record.setValue(false);
      this.playRecording.setValue(false);
      this.circle.setValue(false);
      this.wasRecording = false;
      this.wasPlaying = false;
      this.wasCircling = false;
      this.playbackIndex = 0;
      this.recordTick = 0;
      this.circleTick = 0;
      FakePlayerUtil.removeFakePlayerFromWorld("fakeplayer_module", -2147483647);
   }

   public void onWorldLoad() {
      if (this.isEnabled()) {
         this.disable();
      }

   }

   public void clearRecording() {
      if (!this.playerPositions.isEmpty()) {
         this.playerPositions.clear();
         Command.sendSilentMessage("Cleared recording");
      }

   }

   public List<Position> getPlayerPositions() {
      return this.playerPositions;
   }

   public boolean isRecording() {
      return (Boolean)this.record.getValue() && this.wasRecording;
   }

   public boolean isPlaying() {
      return (Boolean)this.playRecording.getValue() && this.wasPlaying;
   }

   public boolean isCircling() {
      return (Boolean)this.circle.getValue() && this.wasCircling;
   }
}
