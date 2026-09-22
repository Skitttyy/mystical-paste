package fat.vonware.features.modules.player;

import com.mojang.authlib.GameProfile;
import fat.vonware.Global;
import fat.vonware.Vonware;
import fat.vonware.event.events.PacketEvent;
import fat.vonware.features.modules.Module;
import fat.vonware.features.modules.client.Colors;
import fat.vonware.features.setting.Setting;
import fat.vonware.util.Animations.render.ColorUtil;
import fat.vonware.util.Animations.render.RenderUtil;
import fat.vonware.util.FakePlayer.FakePlayerUtil;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.CPacketConfirmTeleport;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.network.play.server.SPacketPlayerPosLook;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.opengl.GL11;

public class Blink extends Module {
   private static final String GHOST_KEY = "blink_ghost";
   private static final int GHOST_PLAYER_ID = -2147483646;
   private final Setting<Boolean> cancel = new Setting<Boolean>("Cancel", false, "Controls whether Blink discards the buffered movement packets instead of sending them when released.");
   private final Setting<Boolean> showTrail = new Setting<Boolean>("Trail", false, "Renders a trail representing your buffered Blink movement.");
   private final Setting<Boolean> showText = new Setting<Boolean>("Text", true, "Displays the Blink status or packet information on the HUD.");
   private final Setting<Integer> xPos = new Setting<Integer>("X-Pos", 100, 0, 1000, (v) -> (Boolean)this.showText.getValue(), "Horizontal position of the Blink HUD text.");
   private final Setting<Integer> yPos = new Setting<Integer>("Y-Pos", 100, 0, 1000, (v) -> (Boolean)this.showText.getValue(), "Vertical position of the Blink HUD text.");
   ScaledResolution sr;
   Queue<CPacketPlayer> packets;
   Queue<CPacketConfirmTeleport> tpPackets;
   Map<UUID, ItemTrail> trails;
   private final Queue<Packet> storedPackets;
   private double ghostX;
   private double ghostY;
   private double ghostZ;
   private float ghostYaw;
   private float ghostPitch;
   private boolean ghostSpawned = false;

   public Blink() {
      super("Blink", "Sends teleport packets to blink you", Module.Category.PLAYER, true, false, false);
      this.register(this.cancel);
      this.register(this.showTrail);
      this.register(this.showText);
      this.register(this.xPos);
      this.register(this.yPos);
      this.packets = new LinkedList();
      this.tpPackets = new LinkedList();
      this.trails = new HashMap();
      this.storedPackets = new LinkedList();
   }

   public void onEnable() {
      if (!fullNullCheck()) {
         this.spawnGhost();
      }
   }

   private void spawnGhost() {
      if (mc.player != null && mc.world != null) {
         this.ghostX = mc.player.posX;
         this.ghostY = mc.player.posY;
         this.ghostZ = mc.player.posZ;
         this.ghostYaw = mc.player.rotationYaw;
         this.ghostPitch = mc.player.rotationPitch;
         GameProfile realProfile = mc.player.getGameProfile();
         GameProfile ghostProfile = new GameProfile(realProfile.getId(), mc.player.getName());
         ghostProfile.getProperties().putAll(realProfile.getProperties());
         FakePlayerUtil.addFakePlayerToWorld("blink_ghost", ghostProfile, -2147483646);
         EntityOtherPlayerMP ghost = FakePlayerUtil.getPlayer("blink_ghost");
         if (ghost != null) {
            ghost.setPositionAndRotationDirect(this.ghostX, this.ghostY, this.ghostZ, this.ghostYaw, this.ghostPitch, 0, true);
            ghost.posX = this.ghostX;
            ghost.posY = this.ghostY;
            ghost.posZ = this.ghostZ;
            ghost.rotationYaw = this.ghostYaw;
            ghost.rotationPitch = this.ghostPitch;
            ghost.rotationYawHead = this.ghostYaw;
            ghost.lastTickPosX = this.ghostX;
            ghost.lastTickPosY = this.ghostY;
            ghost.lastTickPosZ = this.ghostZ;
            ghost.motionX = (double)0.0F;
            ghost.motionY = (double)0.0F;
            ghost.motionZ = (double)0.0F;
            ghost.onGround = true;
            ghost.setSneaking(mc.player.isSneaking());
            mc.getSkinManager().loadProfileTextures(ghostProfile, (type, location, profileTexture) -> ghost.refreshDisplayName(), true);
            this.ghostSpawned = true;
         }
      }
   }

   private void removeGhost() {
      if (this.ghostSpawned) {
         FakePlayerUtil.removeFakePlayerFromWorld("blink_ghost", -2147483646);
         this.ghostSpawned = false;
      }

   }

   @SubscribeEvent
   public void onPacket(PacketEvent event) {
      if (!fullNullCheck()) {
         if (event.getPacket() instanceof SPacketPlayerPosLook && (Boolean)this.cancel.getValue()) {
            event.setCanceled(true);
         }

         if (event.getPacket() instanceof CPacketPlayer) {
            this.packets.add(event.getPacket());
            event.setCanceled(true);
         }

         if (event.getPacket() instanceof CPacketConfirmTeleport) {
            this.tpPackets.add(event.getPacket());
            event.setCanceled(true);
         }

      }
   }

   @SubscribeEvent
   public void onRender3D(RenderWorldLastEvent event) {
      if (!fullNullCheck()) {
         if ((Boolean)this.showTrail.getValue()) {
            for(Map.Entry<UUID, ItemTrail> entry : this.trails.entrySet()) {
               this.drawTrail((ItemTrail)entry.getValue());
            }
         }

      }
   }

   public void onDisable() {
      super.onDisable();

      while(!this.packets.isEmpty()) {
         mc.getConnection().sendPacket((Packet)this.packets.poll());
      }

      while(!this.tpPackets.isEmpty()) {
         mc.getConnection().sendPacket((Packet)this.tpPackets.poll());
      }

      this.trails.clear();
      this.removeGhost();
   }

   public void onLogout() {
      this.removeGhost();
      this.packets.clear();
      this.tpPackets.clear();
      this.trails.clear();
   }

   public void drawTrail(ItemTrail trail) {
      RenderUtil.prepare();
      GL11.glLineWidth(1.0F);
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      (RenderUtil.builder = RenderUtil.tessellator.getBuffer()).begin(3, DefaultVertexFormats.POSITION_COLOR);
      this.buildBuffer(RenderUtil.builder, trail);
      RenderUtil.tessellator.draw();
      GL11.glLineWidth(1.0F);
      RenderUtil.release();
   }

   public void buildBuffer(BufferBuilder builder, ItemTrail trail) {
      for(Position p : trail.positions) {
         Vec3d pos = RenderUtil.updateToCamera(p.pos);
         RenderUtil.addBuilderVertex(builder, pos.x, pos.y, pos.z, Color.WHITE);
      }

   }

   @SubscribeEvent
   public void onTick(TickEvent.ClientTickEvent event) {
      if (!fullNullCheck()) {
         if (this.ghostSpawned) {
            EntityOtherPlayerMP ghost = FakePlayerUtil.getPlayer("blink_ghost");
            if (ghost != null) {
               ghost.posX = this.ghostX;
               ghost.posY = this.ghostY;
               ghost.posZ = this.ghostZ;
               ghost.lastTickPosX = this.ghostX;
               ghost.lastTickPosY = this.ghostY;
               ghost.lastTickPosZ = this.ghostZ;
               ghost.motionX = (double)0.0F;
               ghost.motionY = (double)0.0F;
               ghost.motionZ = (double)0.0F;
               ghost.onGround = true;
            } else {
               this.ghostSpawned = false;
            }
         }

         if ((Boolean)this.showTrail.getValue()) {
            if (this.trails.containsKey(mc.player.getUniqueID())) {
               ((ItemTrail)this.trails.get(mc.player.getUniqueID())).positions.add(new Position(mc.player.getPositionVector()));
            } else {
               this.trails.put(mc.player.getUniqueID(), new ItemTrail(mc.player));
            }
         }

      }
   }

   @SubscribeEvent
   public void draw(RenderGameOverlayEvent.Text event) {
      if ((Boolean)this.showText.getValue()) {
         this.sr = new ScaledResolution(mc);
         this.xPos.setMax(this.sr.getScaledWidth());
         this.yPos.setMax(this.sr.getScaledHeight());
         String string = "ur blinked right meow :3";
         float xPos = ((Number)this.xPos.getValue()).floatValue();
         float yPos = ((Number)this.yPos.getValue()).floatValue();
         if ((Boolean)Colors.getInstance().rainbow.getValue()) {
            if (Colors.getInstance().rainbowMode.getValue() == Colors.RainbowMode.Flow) {
               char[] chars = string.toCharArray();
               float f = 0.0F;
               int index = 1;

               for(char c : chars) {
                  int delay = (int)((float)index * (Float)Colors.getInstance().rainbowHue.getValue());
                  Vonware.textManager.drawStringWithShadow(String.valueOf(c), xPos + f, yPos, ColorUtil.rainbow(delay).getRGB());
                  f += (float)Vonware.textManager.getStringWidth(String.valueOf(c));
                  ++index;
               }
            } else {
               Vonware.textManager.drawStringWithShadow(string, xPos, yPos, ColorUtil.rainbow(0).getRGB());
            }
         } else {
            Color color = Global.getGlobalColor();
            Vonware.textManager.drawStringWithShadow(string, xPos, yPos, color.getRGB());
         }
      }

   }

   public static class Position {
      public Vec3d pos;
      public long time;

      public Position(Vec3d pos) {
         this.pos = pos;
         this.time = System.currentTimeMillis();
      }
   }

   public static class ItemTrail {
      public Entity entity;
      public List<Position> positions;

      public ItemTrail(Entity entity) {
         this.entity = entity;
         this.positions = new ArrayList();
      }
   }
}
