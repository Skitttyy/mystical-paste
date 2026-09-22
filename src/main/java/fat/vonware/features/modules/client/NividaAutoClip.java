package fat.vonware.features.modules.client;

import fat.vonware.Vonware;
import fat.vonware.event.events.DeathEvent;
import fat.vonware.features.modules.Module;
import fat.vonware.features.setting.Bind;
import fat.vonware.features.setting.Setting;
import fat.vonware.manager.TotemPopManager;
import java.awt.AWTException;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Keyboard;

public class NividaAutoClip extends Module {
   private final Setting<Integer> clipDelay = new Setting<Integer>("ClipDelay", 1500, 0, 10000);
   private final Setting<Float> maxRange = new Setting<Float>("MaxRange", 10.0F, 1.0F, 100.0F);
   private final Setting<Boolean> ignoreNonThreat = new Setting<Boolean>("IgnoreNonThreat", true);
   private final Setting<Boolean> ignoreSuicide = new Setting<Boolean>("IgnoreSuicide", true);
   private final Setting<Boolean> selfKillsOnly = new Setting<Boolean>("SelfKillsOnly", false);
   private final Setting<Boolean> notify = new Setting<Boolean>("Notify", false);
   private final Setting<Bind> clipBind = new Setting<Bind>("ClipBind", Bind.none());
   private final Setting<Boolean> twoBinds = new Setting<Boolean>("TwoBinds", false);
   private final Setting<Bind> clipBind2 = new Setting<Bind>("ClipBind2", Bind.none(), (v) -> (Boolean)this.twoBinds.getValue());
   private final Setting<Boolean> threeBinds = new Setting<Boolean>("ThreeBinds", false, (v) -> (Boolean)this.twoBinds.getValue());
   private final Setting<Bind> clipBind3 = new Setting<Bind>("ThirdBind", Bind.none(), (v) -> (Boolean)this.twoBinds.getValue() && (Boolean)this.threeBinds.getValue());
   private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor((r) -> {
      Thread t = new Thread(r, "NividaAutoClip-Scheduler");
      t.setDaemon(true);
      return t;
   });
   private static final long CLIP_COOLDOWN_MS = 3000L;
   private volatile long lastClipTime = 0L;

   public NividaAutoClip() {
      super("NividaAutoClip", "Auto-clips when a player dies using keybinds", Module.Category.CLIENT, true, false, false);
      this.register(this.clipDelay);
      this.register(this.maxRange);
      this.register(this.ignoreNonThreat);
      this.register(this.ignoreSuicide);
      this.register(this.selfKillsOnly);
      this.register(this.notify);
      this.register(this.clipBind);
      this.register(this.twoBinds);
      this.register(this.clipBind2);
      this.register(this.threeBinds);
      this.register(this.clipBind3);
   }

   public String getDisplayInfo() {
      if ((Boolean)this.twoBinds.getValue()) {
         String info = ((Bind)this.clipBind.getValue()).toString() + " + " + ((Bind)this.clipBind2.getValue()).toString();
         if ((Boolean)this.threeBinds.getValue() && !((Bind)this.clipBind3.getValue()).isEmpty()) {
            info = info + " + " + ((Bind)this.clipBind3.getValue()).toString();
         }

         return info;
      } else {
         return ((Bind)this.clipBind.getValue()).toString();
      }
   }

   @SubscribeEvent
   public void onDeath(DeathEvent event) {
      if (mc.player != null && mc.world != null) {
         if (event.getEntity() != mc.player) {
            if (event.getEntity() instanceof EntityPlayer) {
               if (!((Bind)this.clipBind.getValue()).isEmpty()) {
                  if (!(Boolean)this.selfKillsOnly.getValue()) {
                     if (!(mc.player.getDistance(event.getEntity()) > (Float)this.maxRange.getValue())) {
                        EntityPlayer victim = event.getEntity();
                        if (!(Boolean)this.ignoreNonThreat.getValue() || !this.isUnarmored(victim)) {
                           if (!(Boolean)this.ignoreSuicide.getValue() || TotemPopManager.getInstance().getPopCount(victim) <= 15) {
                              if (!Vonware.friendManager.isFriend(victim)) {
                                 long now = System.currentTimeMillis();
                                 if (now - this.lastClipTime >= 3000L) {
                                    this.lastClipTime = now;
                                    String victimName = victim.getName();
                                    this.scheduler.schedule(() -> this.triggerClip(victimName), (long)(Integer)this.clipDelay.getValue(), TimeUnit.MILLISECONDS);
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   @SubscribeEvent(
      priority = EventPriority.LOW
   )
   public void onChat(ClientChatReceivedEvent event) {
      if ((Boolean)this.selfKillsOnly.getValue()) {
         if (mc.player != null && mc.world != null) {
            if (!((Bind)this.clipBind.getValue()).isEmpty()) {
               String unformatted = event.getMessage().getUnformattedText().replaceAll("(?i)[\u00a7&][0-9a-fk-or]", "").trim();
               String stripped = unformatted.replaceAll("^<\\d{1,2}:\\d{2}>\\s*", "");
               if (stripped.contains(mc.player.getName())) {
                  if (stripped.toLowerCase().contains("end crystal")) {
                     String[] words = stripped.split("\\s+");
                     if (words.length <= 0 || !words[0].equals(mc.player.getName())) {
                        String victimName = words.length > 0 ? words[0] : null;
                        if (victimName != null) {
                           if (!Vonware.friendManager.isFriend(victimName)) {
                              EntityPlayer victim = null;

                              for(EntityPlayer p : mc.world.playerEntities) {
                                 if (p.getName().equals(victimName)) {
                                    victim = p;
                                    break;
                                 }
                              }

                              if (victim != null) {
                                 if ((Boolean)this.ignoreNonThreat.getValue() && this.isUnarmored(victim)) {
                                    return;
                                 }

                                 if ((Boolean)this.ignoreSuicide.getValue() && TotemPopManager.getInstance().getPopCount(victim) > 15) {
                                    return;
                                 }
                              }

                              long now = System.currentTimeMillis();
                              if (now - this.lastClipTime >= 3000L) {
                                 this.lastClipTime = now;
                                 this.scheduler.schedule(() -> this.triggerClip(victimName), (long)(Integer)this.clipDelay.getValue(), TimeUnit.MILLISECONDS);
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private void triggerClip(String victimName) {
      try {
         Robot robot = new Robot();
         if ((Boolean)this.twoBinds.getValue() && !((Bind)this.clipBind2.getValue()).isEmpty()) {
            this.pressKeyDown(robot, this.clipBind.getValue());
            this.pressKeyDown(robot, this.clipBind2.getValue());
            if ((Boolean)this.threeBinds.getValue() && !((Bind)this.clipBind3.getValue()).isEmpty()) {
               this.pressKeyDown(robot, this.clipBind3.getValue());
            }

            robot.delay(50);
            this.pressKeyUp(robot, this.clipBind.getValue());
            this.pressKeyUp(robot, this.clipBind2.getValue());
            if ((Boolean)this.threeBinds.getValue() && !((Bind)this.clipBind3.getValue()).isEmpty()) {
               this.pressKeyUp(robot, this.clipBind3.getValue());
            }
         } else {
            this.pressKey(robot, this.clipBind.getValue());
         }

         if ((Boolean)this.notify.getValue()) {
            mc.addScheduledTask(() -> {
               if (mc.player != null) {
                  mc.player.sendMessage(new TextComponentString("\u00a7b[AutoClip] \u00a77Clipped \u00a7c" + victimName + "\u00a77's death!"));
               }

            });
         }
      } catch (AWTException e) {
         mc.addScheduledTask(() -> {
            if (mc.player != null) {
               mc.player.sendMessage(new TextComponentString("\u00a7b[AutoClip] \u00a7cFailed: " + e.getMessage()));
            }

         });
      }

   }

   private void pressKeyDown(Robot robot, Bind bind) {
      if (!bind.isEmpty()) {
         int key = bind.getKey();
         if (key >= 256) {
            robot.mousePress(InputEvent.getMaskForButton(key - 256 + 1));
         } else {
            int awtKey = this.lwjglToAwt(key);
            if (awtKey == -1) {
               return;
            }

            robot.keyPress(awtKey);
         }

      }
   }

   private void pressKeyUp(Robot robot, Bind bind) {
      if (!bind.isEmpty()) {
         int key = bind.getKey();
         if (key >= 256) {
            robot.mouseRelease(InputEvent.getMaskForButton(key - 256 + 1));
         } else {
            int awtKey = this.lwjglToAwt(key);
            if (awtKey == -1) {
               return;
            }

            robot.keyRelease(awtKey);
         }

      }
   }

   private void pressKey(Robot robot, Bind bind) {
      if (!bind.isEmpty()) {
         int key = bind.getKey();
         if (key >= 256) {
            int mask = InputEvent.getMaskForButton(key - 256 + 1);
            robot.mousePress(mask);
            robot.delay(50);
            robot.mouseRelease(mask);
         } else {
            int awtKey = this.lwjglToAwt(key);
            if (awtKey == -1) {
               return;
            }

            robot.keyPress(awtKey);
            robot.delay(50);
            robot.keyRelease(awtKey);
         }

      }
   }

   private boolean isUnarmored(EntityPlayer player) {
      return player.inventory.armorInventory.stream().allMatch((stack) -> stack.isEmpty());
   }

   private int lwjglToAwt(int lwjgl) {
      switch (lwjgl) {
         case 29:
            return 17;
         case 42:
            return 16;
         case 54:
            return 16;
         case 56:
            return 18;
         case 59:
            return 112;
         case 60:
            return 113;
         case 61:
            return 114;
         case 62:
            return 115;
         case 63:
            return 116;
         case 64:
            return 117;
         case 65:
            return 118;
         case 66:
            return 119;
         case 67:
            return 120;
         case 68:
            return 121;
         case 87:
            return 122;
         case 88:
            return 123;
         case 157:
            return 17;
         case 184:
            return 18;
         case 199:
            return 36;
         case 207:
            return 35;
         case 210:
            return 155;
         case 211:
            return 127;
         default:
            String name = Keyboard.getKeyName(lwjgl);
            return name != null && name.length() == 1 ? KeyEvent.getExtendedKeyCodeForChar(name.charAt(0)) : -1;
      }
   }

   public void onDisable() {
      this.lastClipTime = 0L;
   }
}
