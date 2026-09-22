package fat.vonware.util.OtherRenderShit.gui;

import fat.vonware.util.Chat.ThreadUtil;
import java.io.File;
import java.lang.reflect.Field;
import java.net.URL;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.audio.SoundManager;
import net.minecraft.client.audio.ISound.AttenuationType;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import paulscode.sound.SoundSystem;

public class SoundUtil {
   private static final Minecraft mc = Minecraft.getMinecraft();
   private static final String MUSIC_FOLDER = "vonware/music";
   private static final String MUSIC_FILENAME = "startup_music.ogg";
   private static final String MUSIC_SOURCE_NAME = "vonware_startup_music";
   private static SoundEvent CLICK_EVENT;
   private static SoundEvent MODULE_ENABLE_EVENT;
   private static SoundEvent MODULE_DISABLE_EVENT;
   private static SoundEvent MODULE_OPEN_EVENT;
   private static SoundEvent MODULE_CLOSE_EVENT;
   private static SoundEvent CATEGORY_OPEN_EVENT;
   private static SoundEvent CATEGORY_CLOSE_EVENT;
   private static SoundEvent STARTUP_EVENT;
   private static SoundEvent COLORPICKER_OPEN_EVENT;
   private static SoundEvent COLORPICKER_CLOSE_EVENT;
   private static SoundEvent GLOBALCOLORSYNC_EVENT;
   private static boolean registered = false;
   private static boolean customMusicPlaying = false;
   private static volatile boolean isDucked = false;
   private static volatile Thread fadingThread = null;
   private static final long FADE_DOWN_MS = 0L;
   private static final long FADE_UP_MS = 0L;
   private static final int FADE_STEPS = 40;
   private static float guiVolume = 1.0F;
   private static float guiPitch = 1.0F;
   private static final String CUSTOM_SOUNDS_FOLDER = "vonware/sounds/custom";
   private static final String CUSTOM_KILL_FILENAME = "custom_kill.ogg";
   private static final String CUSTOM_TOTEM_FILENAME = "custom_totem.ogg";

   public static void registerSounds() {
      createMusicFolder();

      try {
         CLICK_EVENT = registerSound("vonware", "gui_click");
         MODULE_ENABLE_EVENT = registerSound("vonware", "gui_module_enable");
         MODULE_DISABLE_EVENT = registerSound("vonware", "gui_module_disable");
         MODULE_OPEN_EVENT = registerSound("vonware", "gui_module_open");
         MODULE_CLOSE_EVENT = registerSound("vonware", "gui_module_close");
         CATEGORY_OPEN_EVENT = registerSound("vonware", "gui_category_open");
         CATEGORY_CLOSE_EVENT = registerSound("vonware", "gui_category_close");
         COLORPICKER_OPEN_EVENT = registerSound("vonware", "gui_colorpicker_open");
         COLORPICKER_CLOSE_EVENT = registerSound("vonware", "gui_colorpicker_close");
         GLOBALCOLORSYNC_EVENT = registerSound("vonware", "gui_globalcolorsync");
         STARTUP_EVENT = registerSound("vonware", "startup");
         registered = true;
      } catch (Exception var1) {
         System.err.println("[Vonware] Failed to register custom sounds, using vanilla fallbacks");
         registered = false;
      }

   }

   private static void createMusicFolder() {
      File folder = getMusicFolder();
      if (!folder.exists()) {
         boolean created = folder.mkdirs();
         if (created) {
            System.out.println("[Vonware] Created music folder: " + folder.getAbsolutePath());
            System.out.println("[Vonware] Drop 'startup_music.ogg' there to use custom startup music.");
         }
      }

   }

   private static File getMusicFolder() {
      return new File(mc.gameDir, "vonware/music");
   }

   private static File getMusicFile() {
      return new File(getMusicFolder(), "startup_music.ogg");
   }

   public static void playStartupMusic() {
      File musicFile = getMusicFile();
      if (!musicFile.exists()) {
         System.out.println("[Vonware] No startup music found. Drop an .ogg at: " + musicFile.getAbsolutePath());
      } else {
         ThreadUtil.startDaemon("StartupMusic", () -> {
            try {
               URL fileUrl = musicFile.toURI().toURL();
               mc.addScheduledTask(() -> {
                  try {
                     SoundSystem sndSystem = getSoundSystem();
                     if (sndSystem == null) {
                        System.err.println("[Vonware] Could not access SoundSystem for startup music.");
                        return;
                     }

                     sndSystem.removeSource("vonware_startup_music");
                     sndSystem.newSource(false, "vonware_startup_music", fileUrl, "startup_music.ogg", true, 0.0F, 0.0F, 0.0F, 0, 0.0F);
                     sndSystem.setVolume("vonware_startup_music", guiVolume);
                     sndSystem.play("vonware_startup_music");
                     customMusicPlaying = true;
                     System.out.println("[Vonware] Playing startup music: " + musicFile.getName());
                  } catch (Exception e) {
                     System.err.println("[Vonware] Failed to play startup music: " + e.getMessage());
                  }

               });
            } catch (Exception e) {
               System.err.println("[Vonware] Failed to resolve music file URL: " + e.getMessage());
            }

         });
      }
   }

   public static void stopStartupMusic() {
      if (customMusicPlaying) {
         try {
            SoundSystem sndSystem = getSoundSystem();
            if (sndSystem != null) {
               sndSystem.stop("vonware_startup_music");
               sndSystem.removeSource("vonware_startup_music");
            }
         } catch (Exception var1) {
         }

         customMusicPlaying = false;
      }
   }

   public static void playAdaptationWheelSpin() {
      mc.addScheduledTask(() -> {
         try {
            SoundSystem sndSystem = getSoundSystem();
            if (sndSystem == null) {
               return;
            }

            URL url = SoundUtil.class.getResource("/assets/vonware/sounds/adaptation_wheel_spin.ogg");
            if (url == null) {
               playVanilla(1.3F);
               return;
            }

            String src = "vonware_adaptation_wheel_spin";
            sndSystem.removeSource(src);
            sndSystem.newSource(false, src, url, "adaptation_wheel_spin.ogg", false, 0.0F, 0.0F, 0.0F, 0, 0.0F);
            sndSystem.setVolume(src, 1.0F);
            sndSystem.play(src);
         } catch (Exception var3) {
            playVanilla(1.3F);
         }

      });
   }

   public static boolean hasCustomStartupMusic() {
      return getMusicFile().exists();
   }

   public static String getMusicFolderPath() {
      return getMusicFolder().getAbsolutePath();
   }

   private static SoundSystem getSoundSystem() {
      try {
         SoundHandler handler = mc.getSoundHandler();
         Field managerField = getField(SoundHandler.class, "sndManager", "sndManager");
         if (managerField == null) {
            return null;
         } else {
            managerField.setAccessible(true);
            SoundManager manager = (SoundManager)managerField.get(handler);
            Field sysField = getField(SoundManager.class, "sndSystem", "sndSystem");
            if (sysField == null) {
               return null;
            } else {
               sysField.setAccessible(true);
               return (SoundSystem)sysField.get(manager);
            }
         }
      } catch (Exception e) {
         System.err.println("[Vonware] getSoundSystem reflection failed: " + e.getMessage());
         return null;
      }
   }

   private static Field getField(Class<?> clazz, String deobfName, String srgName) {
      for(String name : new String[]{deobfName, srgName}) {
         try {
            return clazz.getDeclaredField(name);
         } catch (NoSuchFieldException var8) {
         }
      }

      for(Field f : clazz.getDeclaredFields()) {
         if (f.getType() == SoundSystem.class || f.getType() == SoundManager.class) {
            return f;
         }
      }

      return null;
   }

   private static SoundEvent registerSound(String domain, String name) {
      ResourceLocation loc = new ResourceLocation(domain, name);
      SoundEvent event = new SoundEvent(loc);
      event.setRegistryName(loc);
      ForgeRegistries.SOUND_EVENTS.register(event);
      return event;
   }

   public static void playModuleEnable() {
      if (registered && MODULE_ENABLE_EVENT != null) {
         mc.getSoundHandler().playSound(new PositionedSoundRecord(MODULE_ENABLE_EVENT.getSoundName(), SoundCategory.MASTER, compensateVolume(SoundCategory.MASTER, 1.0F), 1.2F, false, 0, AttenuationType.NONE, 0.0F, 0.0F, 0.0F));
      } else {
         playVanilla(1.2F);
      }

   }

   public static void playModuleDisable() {
      if (registered && MODULE_DISABLE_EVENT != null) {
         mc.getSoundHandler().playSound(new PositionedSoundRecord(MODULE_DISABLE_EVENT.getSoundName(), SoundCategory.MASTER, compensateVolume(SoundCategory.MASTER, 1.0F), 0.8F, false, 0, AttenuationType.NONE, 0.0F, 0.0F, 0.0F));
      } else {
         playVanilla(0.8F);
      }

   }

   public static void playModuleOpen() {
      if (registered && MODULE_OPEN_EVENT != null) {
         mc.getSoundHandler().playSound(new PositionedSoundRecord(MODULE_OPEN_EVENT.getSoundName(), SoundCategory.MASTER, compensateVolume(SoundCategory.MASTER, 1.0F), 1.15F, false, 0, AttenuationType.NONE, 0.0F, 0.0F, 0.0F));
      } else {
         playVanilla(1.15F);
      }

   }

   public static void playModuleClose() {
      if (registered && MODULE_CLOSE_EVENT != null) {
         mc.getSoundHandler().playSound(new PositionedSoundRecord(MODULE_CLOSE_EVENT.getSoundName(), SoundCategory.MASTER, compensateVolume(SoundCategory.MASTER, 1.0F), 0.85F, false, 0, AttenuationType.NONE, 0.0F, 0.0F, 0.0F));
      } else {
         playVanilla(0.85F);
      }

   }

   public static void playCategoryOpen() {
      if (registered && CATEGORY_OPEN_EVENT != null) {
         mc.getSoundHandler().playSound(new PositionedSoundRecord(CATEGORY_OPEN_EVENT.getSoundName(), SoundCategory.MASTER, compensateVolume(SoundCategory.MASTER, 1.0F), 1.1F, false, 0, AttenuationType.NONE, 0.0F, 0.0F, 0.0F));
      } else {
         playVanilla(1.1F);
      }

   }

   public static void playCategoryClose() {
      if (registered && CATEGORY_CLOSE_EVENT != null) {
         mc.getSoundHandler().playSound(new PositionedSoundRecord(CATEGORY_CLOSE_EVENT.getSoundName(), SoundCategory.MASTER, compensateVolume(SoundCategory.MASTER, 1.0F), 0.9F, false, 0, AttenuationType.NONE, 0.0F, 0.0F, 0.0F));
      } else {
         playVanilla(0.9F);
      }

   }

   public static void playValorantKill() {
      mc.addScheduledTask(() -> {
         try {
            SoundSystem ss = getSoundSystem();
            if (ss == null) {
               return;
            }

            URL url = SoundUtil.class.getResource("/assets/vonware/sounds/valorant_kill.ogg");
            if (url == null) {
               playVanilla(1.3F);
               return;
            }

            String src = "vonware_kill_valorant";
            ss.removeSource(src);
            ss.newSource(false, src, url, "valorant_kill.ogg", false, 0.0F, 0.0F, 0.0F, 0, 0.0F);
            ss.setVolume(src, 1.0F);
            ss.play(src);
         } catch (Exception var3) {
            playVanilla(1.3F);
         }

      });
   }

   public static void playMurasakiKill() {
      mc.addScheduledTask(() -> {
         try {
            SoundSystem ss = getSoundSystem();
            if (ss == null) {
               return;
            }

            URL url = SoundUtil.class.getResource("/assets/vonware/sounds/murasaki_kill.ogg");
            if (url == null) {
               playVanilla(1.0F);
               return;
            }

            String src = "vonware_kill_murasaki";
            ss.removeSource(src);
            ss.newSource(false, src, url, "murasaki_kill.ogg", false, 0.0F, 0.0F, 0.0F, 0, 0.0F);
            ss.setVolume(src, 1.0F);
            ss.play(src);
         } catch (Exception var3) {
            playVanilla(1.0F);
         }

      });
   }

   public static void playEnumOpen() {
      mc.addScheduledTask(() -> {
         try {
            SoundSystem sndSystem = getSoundSystem();
            if (sndSystem == null) {
               return;
            }

            URL url = SoundUtil.class.getResource("/assets/vonware/sounds/enum_open.ogg");
            if (url == null) {
               playVanilla(1.1F);
               return;
            }

            String src = "vonware_enum_open";
            sndSystem.removeSource(src);
            sndSystem.newSource(false, src, url, "enum_open.ogg", false, 0.0F, 0.0F, 0.0F, 0, 0.0F);
            sndSystem.setVolume(src, 1.0F);
            sndSystem.play(src);
         } catch (Exception var3) {
            playVanilla(1.1F);
         }

      });
   }

   public static void playEnumClose() {
      mc.addScheduledTask(() -> {
         try {
            SoundSystem sndSystem = getSoundSystem();
            if (sndSystem == null) {
               return;
            }

            URL url = SoundUtil.class.getResource("/assets/vonware/sounds/enum_close.ogg");
            if (url == null) {
               playVanilla(0.9F);
               return;
            }

            String src = "vonware_enum_close";
            sndSystem.removeSource(src);
            sndSystem.newSource(false, src, url, "enum_close.ogg", false, 0.0F, 0.0F, 0.0F, 0, 0.0F);
            sndSystem.setVolume(src, 1.0F);
            sndSystem.play(src);
         } catch (Exception var3) {
            playVanilla(0.9F);
         }

      });
   }

   public static void playReZeroDeath() {
      mc.addScheduledTask(() -> {
         try {
            SoundSystem sndSystem = getSoundSystem();
            if (sndSystem == null) {
               return;
            }

            URL url = SoundUtil.class.getResource("/assets/vonware/sounds/rezero_death.ogg");
            if (url == null) {
               if (mc.player != null) {
                  mc.player.playSound(SoundEvents.ENTITY_WITHER_AMBIENT, 1.0F, 0.3F);
               }

               return;
            }

            String src = "vonware_rezero_death";
            sndSystem.removeSource(src);
            sndSystem.newSource(false, src, url, "rezero_death.ogg", false, 0.0F, 0.0F, 0.0F, 0, 0.0F);
            sndSystem.setVolume(src, 1.0F);
            sndSystem.play(src);
         } catch (Exception var3) {
            if (mc.player != null) {
               mc.player.playSound(SoundEvents.ENTITY_WITHER_AMBIENT, 1.0F, 0.3F);
            }
         }

      });
   }

   public static void playDismantle(float pitchMult) {
      mc.addScheduledTask(() -> {
         try {
            SoundSystem sndSystem = getSoundSystem();
            if (sndSystem == null) {
               return;
            }

            URL url = SoundUtil.class.getResource("/assets/vonware/sounds/dismantle.ogg");
            if (url == null) {
               System.err.println("[Vonware] dismantle.ogg not found in assets \u2014 drop it at assets/vonware/sounds/dismantle.ogg");
               return;
            }

            String src = "vonware_dismantle_" + System.nanoTime();
            sndSystem.removeSource(src);
            sndSystem.newSource(false, src, url, "dismantle.ogg", false, 0.0F, 0.0F, 0.0F, 0, 0.0F);
            sndSystem.setPitch(src, pitchMult);
            sndSystem.setVolume(src, 1.0F);
            sndSystem.play(src);
         } catch (Exception e) {
            System.err.println("[Vonware] Failed to play dismantle sound: " + e.getMessage());
         }

      });
   }

   public static void playDismantleSay() {
      mc.addScheduledTask(() -> {
         try {
            SoundSystem sndSystem = getSoundSystem();
            if (sndSystem == null) {
               return;
            }

            URL url = SoundUtil.class.getResource("/assets/vonware/sounds/dismantle_say.ogg");
            if (url == null) {
               System.err.println("[Vonware] dismantle_say.ogg not found \u2014 drop it at assets/vonware/sounds/dismantle_say.ogg");
               return;
            }

            String src = "vonware_dismantle_say";
            sndSystem.removeSource(src);
            sndSystem.newSource(false, src, url, "dismantle_say.ogg", false, 0.0F, 0.0F, 0.0F, 0, 0.0F);
            sndSystem.setVolume(src, 1.0F);
            sndSystem.play(src);
         } catch (Exception e) {
            System.err.println("[Vonware] Failed to play dismantle_say: " + e.getMessage());
         }

      });
   }

   private static File getCustomSoundsFolder() {
      return new File(mc.gameDir, "vonware/sounds/custom");
   }

   private static File getCustomKillFile() {
      return new File(getCustomSoundsFolder(), "custom_kill.ogg");
   }

   private static File getCustomTotemFile() {
      return new File(getCustomSoundsFolder(), "custom_totem.ogg");
   }

   private static void createCustomSoundsFolder() {
      File folder = getCustomSoundsFolder();
      if (!folder.exists()) {
         boolean created = folder.mkdirs();
         if (created) {
            System.out.println("[Vonware] Created custom sounds folder: " + folder.getAbsolutePath());
            System.out.println("[Vonware] Drop 'custom_kill.ogg' and 'custom_totem.ogg' there for Custom KillSounds mode.");
         }
      }

   }

   public static boolean hasCustomKillSound() {
      return getCustomKillFile().exists();
   }

   public static boolean hasCustomTotemSound() {
      return getCustomTotemFile().exists();
   }

   public static String getCustomSoundsFolderPath() {
      return getCustomSoundsFolder().getAbsolutePath();
   }

   public static void playCustomKill() {
      playCustomFile(getCustomKillFile(), "vonware_custom_kill", 1.0F);
   }

   public static void playCustomTotem() {
      playCustomFile(getCustomTotemFile(), "vonware_custom_totem", 1.0F);
   }

   private static void playCustomFile(File file, String sourceName, float volume) {
      if (!file.exists()) {
         System.err.println("[Vonware] Custom sound not found: " + file.getAbsolutePath());
      } else {
         ThreadUtil.startDaemon("CustomSound", () -> {
            try {
               URL fileUrl = file.toURI().toURL();
               mc.addScheduledTask(() -> {
                  try {
                     SoundSystem ss = getSoundSystem();
                     if (ss == null) {
                        return;
                     }

                     ss.removeSource(sourceName);
                     ss.newSource(false, sourceName, fileUrl, file.getName(), false, 0.0F, 0.0F, 0.0F, 0, 0.0F);
                     ss.setVolume(sourceName, volume);
                     ss.play(sourceName);
                  } catch (Exception e) {
                     System.err.println("[Vonware] Failed to play custom sound: " + e.getMessage());
                  }

               });
            } catch (Exception e) {
               System.err.println("[Vonware] Failed to resolve custom sound URL: " + e.getMessage());
            }

         });
      }
   }

   public static void playCutePopTotem() {
      mc.addScheduledTask(() -> {
         try {
            SoundSystem ss = getSoundSystem();
            if (ss == null) {
               return;
            }

            URL url = SoundUtil.class.getResource("/assets/vonware/sounds/cutepop_totem.ogg");
            if (url == null) {
               playVanilla(1.0F);
               return;
            }

            String src = "vonware_totem_cutepop";
            ss.removeSource(src);
            ss.newSource(false, src, url, "cutepop_totem.ogg", false, 0.0F, 0.0F, 0.0F, 0, 0.0F);
            ss.setVolume(src, 1.0F);
            ss.play(src);
         } catch (Exception var3) {
            playVanilla(1.0F);
         }

      });
   }

   public static void playButtonHover() {
      mc.addScheduledTask(() -> {
         try {
            SoundSystem sndSystem = getSoundSystem();
            if (sndSystem == null) {
               return;
            }

            URL url = SoundUtil.class.getResource("/assets/vonware/sounds/gui_hover.ogg");
            if (url == null) {
               playVanilla(0.6F);
               return;
            }

            String src = "vonware_hover_" + System.nanoTime();
            sndSystem.removeSource(src);
            sndSystem.newSource(false, src, url, "gui_hover.ogg", false, 0.0F, 0.0F, 0.0F, 0, 0.0F);
            sndSystem.setVolume(src, 0.35F);
            sndSystem.play(src);
         } catch (Exception var3) {
         }

      });
   }

   public static void playDismantleDestroy() {
      mc.addScheduledTask(() -> {
         try {
            SoundSystem sndSystem = getSoundSystem();
            if (sndSystem == null) {
               return;
            }

            URL url = SoundUtil.class.getResource("/assets/vonware/sounds/dismantle_destroy.ogg");
            if (url == null) {
               System.err.println("[Vonware] dismantle_destroy.ogg not found \u2014 drop it at assets/vonware/sounds/dismantle_destroy.ogg");
               return;
            }

            String src = "vonware_dismantle_destroy";
            sndSystem.removeSource(src);
            sndSystem.newSource(false, src, url, "dismantle_destroy.ogg", false, 0.0F, 0.0F, 0.0F, 0, 0.0F);
            sndSystem.setVolume(src, 1.0F);
            sndSystem.play(src);
         } catch (Exception e) {
            System.err.println("[Vonware] Failed to play dismantle_destroy: " + e.getMessage());
         }

      });
   }

   public static void playSoundOne() {
      mc.addScheduledTask(() -> {
         try {
            SoundSystem ss = getSoundSystem();
            if (ss == null) {
               return;
            }

            URL url = SoundUtil.class.getResource("/assets/vonware/sounds/sound_one.ogg");
            if (url == null) {
               playVanilla(1.0F);
               return;
            }

            String src = "vonware_sound_one";
            ss.removeSource(src);
            ss.newSource(false, src, url, "sound_one.ogg", false, 0.0F, 0.0F, 0.0F, 0, 0.0F);
            ss.setVolume(src, 1.0F);
            ss.play(src);
         } catch (Exception var3) {
            playVanilla(1.0F);
         }

      });
   }

   public static void playSoundTwo() {
      mc.addScheduledTask(() -> {
         try {
            SoundSystem ss = getSoundSystem();
            if (ss == null) {
               return;
            }

            URL url = SoundUtil.class.getResource("/assets/vonware/sounds/sound_two.ogg");
            if (url == null) {
               playVanilla(1.0F);
               return;
            }

            String src = "vonware_sound_two";
            ss.removeSource(src);
            ss.newSource(false, src, url, "sound_two.ogg", false, 0.0F, 0.0F, 0.0F, 0, 0.0F);
            ss.setVolume(src, 1.0F);
            ss.play(src);
         } catch (Exception var3) {
            playVanilla(1.0F);
         }

      });
   }

   public static void playSoundThree() {
      mc.addScheduledTask(() -> {
         try {
            SoundSystem ss = getSoundSystem();
            if (ss == null) {
               return;
            }

            URL url = SoundUtil.class.getResource("/assets/vonware/sounds/sound_three.ogg");
            if (url == null) {
               playVanilla(1.0F);
               return;
            }

            String src = "vonware_sound_three";
            ss.removeSource(src);
            ss.newSource(false, src, url, "sound_three.ogg", false, 0.0F, 0.0F, 0.0F, 0, 0.0F);
            ss.setVolume(src, 1.0F);
            ss.play(src);
         } catch (Exception var3) {
            playVanilla(1.0F);
         }

      });
   }

   public static void playBlackFlash() {
      mc.addScheduledTask(() -> {
         try {
            SoundSystem ss = getSoundSystem();
            if (ss == null) {
               return;
            }

            URL url = SoundUtil.class.getResource("/assets/vonware/sounds/blackflash.ogg");
            if (url == null) {
               playVanilla(1.0F);
               return;
            }

            String src = "vonware_blackflash";
            ss.removeSource(src);
            ss.newSource(false, src, url, "blackflash.ogg", false, 0.0F, 0.0F, 0.0F, 0, 0.0F);
            ss.setVolume(src, 1.0F);
            ss.play(src);
         } catch (Exception var3) {
            playVanilla(1.0F);
         }

      });
   }

   public static void playSoundFour() {
      mc.addScheduledTask(() -> {
         try {
            SoundSystem ss = getSoundSystem();
            if (ss == null) {
               return;
            }

            URL url = SoundUtil.class.getResource("/assets/vonware/sounds/sound_four.ogg");
            if (url == null) {
               playVanilla(1.0F);
               return;
            }

            String src = "vonware_sound_four";
            ss.removeSource(src);
            ss.newSource(false, src, url, "sound_four.ogg", false, 0.0F, 0.0F, 0.0F, 0, 0.0F);
            ss.setVolume(src, 1.0F);
            ss.play(src);
         } catch (Exception var3) {
            playVanilla(1.0F);
         }

      });
   }

   public static void playWhisperNotification() {
      playPackagedUiSound("/assets/vonware/sounds/whisper_sound.ogg", "vonware_whisper_notification", "whisper_sound.ogg", 0.9F, 1.0F);
   }

   public static void playHudEditorClick() {
      playPackagedUiSound("/assets/vonware/sounds/hudeditor_sound.ogg", "vonware_hud_editor_click", "hudeditor_sound.ogg", 0.85F, 1.0F);
   }

   private static void playPackagedUiSound(String resourcePath, String sourceName, String fileName, float volume, float fallbackPitch) {
      mc.addScheduledTask(() -> {
         try {
            SoundSystem soundSystem = getSoundSystem();
            URL url = SoundUtil.class.getResource(resourcePath);
            if (soundSystem == null || url == null) {
               playVanilla(fallbackPitch);
               return;
            }

            soundSystem.removeSource(sourceName);
            soundSystem.newSource(false, sourceName, url, fileName, false, 0.0F, 0.0F, 0.0F, 0, 0.0F);
            soundSystem.setVolume(sourceName, Math.max(0.0F, Math.min(1.0F, volume * guiVolume)));
            soundSystem.play(sourceName);
         } catch (Exception var7) {
            playVanilla(fallbackPitch);
         }

      });
   }

   public static void playModuleToggle(boolean enabled) {
      if (enabled) {
         playModuleEnable();
      } else {
         playModuleDisable();
      }

   }

   public static void playModuleSettingsToggle(boolean opened) {
      if (opened) {
         playModuleOpen();
      } else {
         playModuleClose();
      }

   }

   public static void playCategoryToggle(boolean opened) {
      if (opened) {
         playCategoryOpen();
      } else {
         playCategoryClose();
      }

   }

   public static void playColorPickerOpen() {
      if (registered && COLORPICKER_OPEN_EVENT != null) {
         play(COLORPICKER_OPEN_EVENT, guiVolume, guiPitch);
      } else {
         playVanilla(1.05F);
      }

   }

   public static void playSecretShiny() {
      if (!(Math.random() < 0.99)) {
         mc.addScheduledTask(() -> {
            try {
               SoundSystem sndSystem = getSoundSystem();
               if (sndSystem == null) {
                  return;
               }

               URL url = SoundUtil.class.getResource("/assets/vonware/sounds/secretshiny.ogg");
               if (url == null) {
                  System.err.println("[Vonware] secretshiny.ogg not found in assets.");
                  return;
               }

               sndSystem.removeSource("vonware_secretshiny");
               sndSystem.newSource(false, "vonware_secretshiny", url, "secretshiny.ogg", false, 0.0F, 0.0F, 0.0F, 0, 0.0F);
               sndSystem.setVolume("vonware_secretshiny", 2.0F);
               sndSystem.play("vonware_secretshiny");
            } catch (Exception e) {
               System.err.println("[Vonware] Failed to play secretshiny: " + e.getMessage());
            }

         });
      }
   }

   public static void playColorPickerClose() {
      if (registered && COLORPICKER_CLOSE_EVENT != null) {
         play(COLORPICKER_CLOSE_EVENT, guiVolume, guiPitch);
      } else {
         playVanilla(0.95F);
      }

   }

   public static void playGlobalColorSync(boolean enabled) {
      float pitch = enabled ? 1.1F : 0.85F;
      if (registered && GLOBALCOLORSYNC_EVENT != null) {
         play(GLOBALCOLORSYNC_EVENT, guiVolume, guiPitch * pitch);
      } else {
         playVanilla(pitch);
      }

   }

   public static void playStartup() {
      if (registered && STARTUP_EVENT != null) {
         ThreadUtil.startDaemon("Startup-Sound", () -> {
            try {
               Thread.sleep(1000L);
               mc.addScheduledTask(() -> {
                  float vol = compensateVolume(SoundCategory.MASTER, 0.8F);
                  mc.getSoundHandler().playSound(new PositionedSoundRecord(STARTUP_EVENT.getSoundName(), SoundCategory.MASTER, vol, 1.0F, false, 0, AttenuationType.NONE, 0.0F, 0.0F, 0.0F));
               });
            } catch (Exception var1) {
            }

         });
      }

   }

   private static float compensateVolume(SoundCategory category, float targetVolume) {
      float sliderLevel = mc.gameSettings.getSoundLevel(category);
      return sliderLevel <= 0.001F ? targetVolume : Math.min(targetVolume / sliderLevel, 256.0F);
   }

   private static void play(SoundEvent event, float volume, float pitch) {
      if (event != null && mc.getSoundHandler() != null) {
         try {
            mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(event, pitch));
         } catch (Exception var4) {
         }

      }
   }

   private static void playVanilla(float pitch) {
      if (mc.getSoundHandler() != null) {
         try {
            mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, pitch * guiPitch));
         } catch (Exception var2) {
         }

      }
   }
}
