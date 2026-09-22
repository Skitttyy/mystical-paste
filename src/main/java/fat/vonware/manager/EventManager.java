package fat.vonware.manager;

import com.google.common.base.Strings;
import com.mojang.authlib.GameProfile;
import com.mojang.realmsclient.gui.ChatFormatting;
import fat.vonware.Vonware;
import fat.vonware.event.events.ConnectionEvent;
import fat.vonware.event.events.DeathEvent;
import fat.vonware.event.events.MotionUpdateEvent;
import fat.vonware.event.events.PacketEvent;
import fat.vonware.event.events.Render2DEvent;
import fat.vonware.event.events.Render3DEvent;
import fat.vonware.event.events.TotemPopEvent;
import fat.vonware.features.Feature;
import fat.vonware.features.command.Command;
import fat.vonware.util.Timer;
import fat.vonware.util.Util;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.server.SPacketEntityStatus;
import net.minecraft.network.play.server.SPacketPlayerListItem;
import net.minecraft.network.play.server.SPacketTimeUpdate;
import net.minecraft.network.play.server.SPacketPlayerListItem.Action;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public class EventManager extends Feature {
   private static final Method PLAYER_LIST_ENTRIES_METHOD = resolveNoArgMethod(SPacketPlayerListItem.class, List.class, "getEntries", "func_179767_a");
   private static volatile Class<?> playerDataClass;
   private static volatile Method playerDataProfileMethod;
   private final Timer logoutTimer = new Timer();

   private static Method resolveNoArgMethod(Class<?> owner, Class<?> returnType, String... preferredNames) {
      for(String name : preferredNames) {
         try {
            Method method = owner.getDeclaredMethod(name);
            if (returnType.isAssignableFrom(method.getReturnType())) {
               try {
                  method.setAccessible(true);
               } catch (SecurityException var9) {
               }

               return method;
            }
         } catch (NoSuchMethodException var10) {
         }
      }

      for(Method method : owner.getDeclaredMethods()) {
         if (method.getParameterTypes().length == 0 && returnType.isAssignableFrom(method.getReturnType())) {
            try {
               method.setAccessible(true);
            } catch (SecurityException var8) {
            }

            return method;
         }
      }

      return null;
   }

   private static List<?> getPlayerListEntries(SPacketPlayerListItem packet) {
      if (PLAYER_LIST_ENTRIES_METHOD == null) {
         return Collections.emptyList();
      } else {
         try {
            Object entries = PLAYER_LIST_ENTRIES_METHOD.invoke(packet);
            return entries instanceof List ? (List)entries : Collections.emptyList();
         } catch (ReflectiveOperationException | RuntimeException var2) {
            return Collections.emptyList();
         }
      }
   }

   private static GameProfile getPlayerProfile(Object playerData) {
      Class<?> dataClass = playerData.getClass();
      if (playerDataClass != dataClass) {
         playerDataProfileMethod = resolveNoArgMethod(dataClass, GameProfile.class, "getProfile", "func_179962_a");
         playerDataClass = dataClass;
      }

      Method profileMethod = playerDataProfileMethod;
      if (profileMethod == null) {
         return null;
      } else {
         try {
            Object profile = profileMethod.invoke(playerData);
            return profile instanceof GameProfile ? (GameProfile)profile : null;
         } catch (ReflectiveOperationException | RuntimeException var4) {
            return null;
         }
      }
   }

   public void init() {
      MinecraftForge.EVENT_BUS.register(this);
   }

   public void onUnload() {
      MinecraftForge.EVENT_BUS.unregister(this);
   }

   @SubscribeEvent
   public void onUpdate(LivingEvent.LivingUpdateEvent event) {
      if (!fullNullCheck() && event.getEntity().getEntityWorld().isRemote && event.getEntityLiving().equals(mc.player)) {
         Vonware.moduleManager.onUpdate();
      }

   }

   @SubscribeEvent
   public void onClientConnect(FMLNetworkEvent.ClientConnectedToServerEvent event) {
      this.logoutTimer.reset();
      Vonware.moduleManager.onLogin();
      Vonware.totemPopManager.clear();
   }

   @SubscribeEvent
   public void onClientDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
      Vonware.moduleManager.onLogout();
      Vonware.totemPopManager.clear();
   }

   @SubscribeEvent
   public void onTick(TickEvent.ClientTickEvent event) {
      if (!fullNullCheck()) {
         Vonware.moduleManager.onTick();

         try {
            for(EntityPlayer player : Util.mc.world.playerEntities) {
               if (player != null && !(player.getHealth() > 0.0F)) {
                  MinecraftForge.EVENT_BUS.post(new DeathEvent(player));
               }
            }
         } catch (Exception var4) {
         }

      }
   }

   @SubscribeEvent
   public void onUpdateWalkingPlayer(MotionUpdateEvent event) {
      if (!fullNullCheck()) {
         if (event.getStage() == 0) {
            Vonware.speedManager.update();
         }

      }
   }

   @SubscribeEvent
   public void onPacketReceive(PacketEvent.Receive event) {
      if (event.getStage() == 0) {
         Vonware.serverManager.onPacketReceived();
         if (event.getPacket() instanceof SPacketEntityStatus) {
            SPacketEntityStatus packet = (SPacketEntityStatus)event.getPacket();
            if (packet.getOpCode() == 35 && packet.getEntity(Util.mc.world) instanceof EntityPlayer) {
               EntityPlayer player = (EntityPlayer)packet.getEntity(Util.mc.world);
               MinecraftForge.EVENT_BUS.post(new TotemPopEvent(player));
            }
         }

         if (event.getPacket() instanceof SPacketPlayerListItem && !fullNullCheck() && this.logoutTimer.passedS((double)1.0F)) {
            SPacketPlayerListItem packet = (SPacketPlayerListItem)event.getPacket();
            if (!Action.ADD_PLAYER.equals(packet.getAction()) && !Action.REMOVE_PLAYER.equals(packet.getAction())) {
               return;
            }

            for(Object data : getPlayerListEntries(packet)) {
               if (data == null) {
                  continue;
               }

               GameProfile profile = getPlayerProfile(data);
               if (profile == null || Strings.isNullOrEmpty(profile.getName()) && profile.getId() == null) {
                  continue;
               }

               UUID id = profile.getId();
               switch (packet.getAction()) {
                  case ADD_PLAYER:
                     String name = profile.getName();
                     MinecraftForge.EVENT_BUS.post(new ConnectionEvent(0, id, name));
                     break;
                  case REMOVE_PLAYER:
                     EntityPlayer entity = Util.mc.world.getPlayerEntityByUUID(id);
                     if (entity != null) {
                        String logoutName = entity.getName();
                        MinecraftForge.EVENT_BUS.post(new ConnectionEvent(1, entity, id, logoutName));
                     } else {
                        MinecraftForge.EVENT_BUS.post(new ConnectionEvent(2, id, (String)null));
                     }
               }
            }
         }

         if (event.getPacket() instanceof SPacketTimeUpdate) {
            Vonware.serverManager.update();
         }

      }
   }

   @SubscribeEvent
   public void onWorldRender(RenderWorldLastEvent event) {
      if (!event.isCanceled()) {
         Util.mc.profiler.startSection("vonware");
         GlStateManager.disableTexture2D();
         GlStateManager.enableBlend();
         GlStateManager.disableAlpha();
         GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
         GlStateManager.shadeModel(7425);
         GlStateManager.disableDepth();
         GlStateManager.glLineWidth(1.0F);
         Render3DEvent render3dEvent = new Render3DEvent(event.getPartialTicks());
         Vonware.moduleManager.onRender3D(render3dEvent);
         GlStateManager.glLineWidth(1.0F);
         GlStateManager.shadeModel(7424);
         GlStateManager.disableBlend();
         GlStateManager.enableAlpha();
         GlStateManager.enableTexture2D();
         GlStateManager.enableDepth();
         GlStateManager.enableCull();
         GlStateManager.enableCull();
         GlStateManager.depthMask(true);
         GlStateManager.enableTexture2D();
         GlStateManager.enableBlend();
         GlStateManager.enableDepth();
         Util.mc.profiler.endSection();
      }
   }

   @SubscribeEvent
   public void renderHUD(RenderGameOverlayEvent.Post event) {
      if (event.getType() == ElementType.HOTBAR) {
         Vonware.textManager.updateResolution();
      }

   }

   @SubscribeEvent(
      priority = EventPriority.LOW
   )
   public void onRenderGameOverlayEvent(RenderGameOverlayEvent.Text event) {
      if (event.getType().equals(ElementType.TEXT)) {
         ScaledResolution resolution = new ScaledResolution(Util.mc);
         Render2DEvent render2DEvent = new Render2DEvent(event.getPartialTicks(), resolution);

         try {
            Vonware.moduleManager.onRender2D(render2DEvent);
         } finally {
            if (BlurManager.INSTANCE != null) {
               BlurManager.INSTANCE.endFrame();
            }

         }

         GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      }

   }

   @SubscribeEvent(
      priority = EventPriority.NORMAL,
      receiveCanceled = true
   )
   public void onKeyInput(InputEvent.KeyInputEvent event) {
      if (Keyboard.getEventKeyState()) {
         Vonware.moduleManager.onKeyPressed(Keyboard.getEventKey());
      }

   }

   @SubscribeEvent(
      priority = EventPriority.NORMAL,
      receiveCanceled = true
   )
   public void onMouseInput(InputEvent.MouseInputEvent event) {
      if (Mouse.getEventButtonState()) {
         int button = Mouse.getEventButton();
         if (button >= 2) {
            Vonware.moduleManager.onMousePressed(button);
         }
      }

   }

   @SubscribeEvent(
      priority = EventPriority.HIGHEST
   )
   public void onChatSent(ClientChatEvent event) {
      if (event.getMessage().startsWith(Command.getCommandPrefix())) {
         event.setCanceled(true);

         try {
            Util.mc.ingameGUI.getChatGUI().addToSentMessages(event.getMessage());
            if (event.getMessage().length() > 1) {
               Vonware.commandManager.executeCommand(event.getMessage().substring(Command.getCommandPrefix().length() - 1));
            } else {
               Command.sendMessage("Please enter a command.");
            }
         } catch (Exception e) {
            e.printStackTrace();
            Command.sendMessage(ChatFormatting.RED + "An error occurred while running this command. Check the log!");
         }
      }

   }
}
