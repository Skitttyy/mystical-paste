package fat.vonware.manager;

import fat.vonware.event.events.Render2DEvent;
import fat.vonware.event.events.Render3DEvent;
import fat.vonware.features.Feature;
import fat.vonware.features.gui.newgui.newcomponents.NewVonwareGui;
import fat.vonware.features.modules.Module;
import fat.vonware.features.modules.client.BlockRender3D;
import fat.vonware.features.modules.client.ClickGui;
import fat.vonware.features.modules.client.Colors;
import fat.vonware.features.modules.client.FastLatency;
import fat.vonware.features.modules.client.Fonts;
import fat.vonware.features.modules.client.HudEditor;
import fat.vonware.features.modules.client.MainMenu;
import fat.vonware.features.modules.client.Manager;
import fat.vonware.features.modules.client.Media;
import fat.vonware.features.modules.client.NividaAutoClip;
import fat.vonware.features.modules.client.Optimizer;
import fat.vonware.features.modules.client.PacketDebug;
import fat.vonware.features.modules.combat.Aura;
import fat.vonware.features.modules.combat.AutoArmor;
import fat.vonware.features.modules.combat.AutoCity;
import fat.vonware.features.modules.combat.AutoCrystal;
import fat.vonware.features.modules.combat.AutoRegear;
import fat.vonware.features.modules.combat.AutoTrap;
import fat.vonware.features.modules.combat.BurrowBypass;
import fat.vonware.features.modules.combat.Criticals;
import fat.vonware.features.modules.combat.CrystalBase;
import fat.vonware.features.modules.combat.FutureFacePlace;
import fat.vonware.features.modules.combat.HoleFill;
import fat.vonware.features.modules.combat.Offhand;
import fat.vonware.features.modules.combat.PflySwitch;
import fat.vonware.features.modules.combat.PistonPush;
import fat.vonware.features.modules.combat.SelfFill;
import fat.vonware.features.modules.combat.SelfTrap;
import fat.vonware.features.modules.combat.Surround;
import fat.vonware.features.modules.elements.AdaptationWheel;
import fat.vonware.features.modules.elements.ArmorHUD;
import fat.vonware.features.modules.elements.ArmorWarner;
import fat.vonware.features.modules.elements.Coords;
import fat.vonware.features.modules.elements.CsgoNotifs;
import fat.vonware.features.modules.elements.DirectionHUD;
import fat.vonware.features.modules.elements.DotGod;
import fat.vonware.features.modules.elements.FeatureList;
import fat.vonware.features.modules.elements.Greeter;
import fat.vonware.features.modules.elements.HealthVisualizer;
import fat.vonware.features.modules.elements.Info;
import fat.vonware.features.modules.elements.InventoryPreview;
import fat.vonware.features.modules.elements.MotionDebug;
import fat.vonware.features.modules.elements.NoXPWaste;
import fat.vonware.features.modules.elements.PVPResources;
import fat.vonware.features.modules.elements.PacketsCounter;
import fat.vonware.features.modules.elements.PearlCooldown;
import fat.vonware.features.modules.elements.ResourceInfo;
import fat.vonware.features.modules.elements.ServerResponse;
import fat.vonware.features.modules.elements.StatusEffect;
import fat.vonware.features.modules.elements.TargetHUD;
import fat.vonware.features.modules.elements.TextRadar;
import fat.vonware.features.modules.elements.TotemHUD;
import fat.vonware.features.modules.elements.Watermark;
import fat.vonware.features.modules.misc.AirPlace;
import fat.vonware.features.modules.misc.Announcer;
import fat.vonware.features.modules.misc.AutoLog;
import fat.vonware.features.modules.misc.AutoReply;
import fat.vonware.features.modules.misc.AutoRespawn;
import fat.vonware.features.modules.misc.Chat;
import fat.vonware.features.modules.misc.ChatColors;
import fat.vonware.features.modules.misc.ChatSuffix;
import fat.vonware.features.modules.misc.CustomSounds;
import fat.vonware.features.modules.misc.Disorient;
import fat.vonware.features.modules.misc.ExtraTab;
import fat.vonware.features.modules.misc.KeyPearl;
import fat.vonware.features.modules.misc.MiddleClick;
import fat.vonware.features.modules.misc.NoUnicodeLag;
import fat.vonware.features.modules.misc.PingSpoof;
import fat.vonware.features.modules.misc.PopCounter;
import fat.vonware.features.modules.misc.PvPInfo;
import fat.vonware.features.modules.misc.RegearBot;
import fat.vonware.features.modules.misc.SmartDisconnect;
import fat.vonware.features.modules.misc.Spammer;
import fat.vonware.features.modules.misc.VisualRange;
import fat.vonware.features.modules.movement.AnchorRewrite;
import fat.vonware.features.modules.movement.AutoWalk;
import fat.vonware.features.modules.movement.FastSwim;
import fat.vonware.features.modules.movement.HoleSnap;
import fat.vonware.features.modules.movement.IceSpeed;
import fat.vonware.features.modules.movement.InstantSpeed;
import fat.vonware.features.modules.movement.LongJump;
import fat.vonware.features.modules.movement.NoSlow;
import fat.vonware.features.modules.movement.OldPhaseWalk;
import fat.vonware.features.modules.movement.PearlPhase;
import fat.vonware.features.modules.movement.Phase;
import fat.vonware.features.modules.movement.Speed;
import fat.vonware.features.modules.movement.Sprint;
import fat.vonware.features.modules.movement.Step;
import fat.vonware.features.modules.movement.TickBoost;
import fat.vonware.features.modules.movement.Velocity;
import fat.vonware.features.modules.player.AntiChainPop;
import fat.vonware.features.modules.player.AntiCrash;
import fat.vonware.features.modules.player.AntiInteract;
import fat.vonware.features.modules.player.AutoEat;
import fat.vonware.features.modules.player.AutoFrameDupe;
import fat.vonware.features.modules.player.AutoXP;
import fat.vonware.features.modules.player.Blink;
import fat.vonware.features.modules.player.ChestSwap;
import fat.vonware.features.modules.player.Cornerclip;
import fat.vonware.features.modules.player.FakePlayer;
import fat.vonware.features.modules.player.FastBreak;
import fat.vonware.features.modules.player.FastProjectile;
import fat.vonware.features.modules.player.FreeLook;
import fat.vonware.features.modules.player.Freecam;
import fat.vonware.features.modules.player.MultiTask;
import fat.vonware.features.modules.player.NoEntityTrace;
import fat.vonware.features.modules.player.NoRotate;
import fat.vonware.features.modules.player.Replenish;
import fat.vonware.features.modules.player.Scaffold;
import fat.vonware.features.modules.player.XCarry;
import fat.vonware.features.modules.player.XpHelper;
import fat.vonware.features.modules.render.Animations;
import fat.vonware.features.modules.render.BetterHotbar;
import fat.vonware.features.modules.render.BlockHighlight;
import fat.vonware.features.modules.render.CameraTweaks;
import fat.vonware.features.modules.render.Chams;
import fat.vonware.features.modules.render.ContainerPreview;
import fat.vonware.features.modules.render.Cosmetics;
import fat.vonware.features.modules.render.CrystalPosRender;
import fat.vonware.features.modules.render.CustomDeathUI;
import fat.vonware.features.modules.render.DamageText;
import fat.vonware.features.modules.render.ESP;
import fat.vonware.features.modules.render.EntityTweaks;
import fat.vonware.features.modules.render.Environment;
import fat.vonware.features.modules.render.GlintModify;
import fat.vonware.features.modules.render.HandVisuals;
import fat.vonware.features.modules.render.HoleESP;
import fat.vonware.features.modules.render.IggyModule;
import fat.vonware.features.modules.render.Indicators;
import fat.vonware.features.modules.render.JumpCircles;
import fat.vonware.features.modules.render.KillEffects;
import fat.vonware.features.modules.render.LogoutSpots;
import fat.vonware.features.modules.render.NameTags;
import fat.vonware.features.modules.render.NoRender;
import fat.vonware.features.modules.render.PearlRender;
import fat.vonware.features.modules.render.PopEffects;
import fat.vonware.features.modules.render.Shaders;
import fat.vonware.features.modules.render.Skeleton;
import fat.vonware.features.modules.render.Trails;
import fat.vonware.features.modules.render.Trajectories;
import fat.vonware.features.modules.render.WayPoints;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.EventBus;
import org.lwjgl.input.Keyboard;

public class ModuleManager extends Feature {
   public ArrayList<Module> modules = new ArrayList();

   public void init() {
      this.modules.add(new Step());
      this.modules.add(new KeyPearl());
      this.modules.add(new PacketDebug());
      this.modules.add(new MotionDebug());
      this.modules.add(new Offhand());
      this.modules.add(new NividaAutoClip());
      this.modules.add(new CrystalPosRender());
      this.modules.add(new AnchorRewrite());
      this.modules.add(new NoXPWaste());
      this.modules.add(new Indicators());
      this.modules.add(new NoSlow());
      this.modules.add(new MainMenu());
      this.modules.add(new AutoReply());
      this.modules.add(new BetterHotbar());
      this.modules.add(new TargetHUD());
      this.modules.add(new CustomSounds());
      this.modules.add(new XCarry());
      this.modules.add(new IggyModule());
      this.modules.add(new DamageText());
      this.modules.add(new AutoTrap());
      this.modules.add(new PacketsCounter());
      this.modules.add(new PearlRender());
      this.modules.add(new Sprint());
      this.modules.add(new Chams());
      this.modules.add(new Velocity());
      this.modules.add(new BlockRender3D());
      this.modules.add(new ChatColors());
      this.modules.add(new HudEditor());
      this.modules.add(new OldPhaseWalk());
      this.modules.add(new HealthVisualizer());
      this.modules.add(new RegearBot());
      this.modules.add(new XpHelper());
      this.modules.add(new CsgoNotifs());
      this.modules.add(new Fonts());
      this.modules.add(new Shaders());
      this.modules.add(new ChatSuffix());
      this.modules.add(new NoUnicodeLag());
      this.modules.add(new Blink());
      this.modules.add(new VisualRange());
      this.modules.add(new AntiChainPop());
      this.modules.add(new SelfFill());
      this.modules.add(new FastLatency());
      this.modules.add(new Manager());
      this.modules.add(new ServerResponse());
      this.modules.add(new Colors());
      this.modules.add(new Disorient());
      this.modules.add(new ResourceInfo());
      this.modules.add(new LongJump());
      this.modules.add(new Surround());
      this.modules.add(new ClickGui());
      this.modules.add(new AutoXP());
      this.modules.add(new JumpCircles());
      this.modules.add(new Watermark());
      this.modules.add(new StatusEffect());
      this.modules.add(new AutoCrystal());
      this.modules.add(new InventoryPreview());
      this.modules.add(new MultiTask());
      this.modules.add(new TextRadar());
      this.modules.add(new TotemHUD());
      this.modules.add(new PVPResources());
      this.modules.add(new PvPInfo());
      this.modules.add(new Coords());
      this.modules.add(new Greeter());
      this.modules.add(new DotGod());
      this.modules.add(new ArmorHUD());
      this.modules.add(new AutoWalk());
      this.modules.add(new Optimizer());
      this.modules.add(new FeatureList());
      this.modules.add(new NameTags());
      this.modules.add(new Info());
      this.modules.add(new ArmorWarner());
      this.modules.add(new PearlCooldown());
      this.modules.add(new FutureFacePlace());
      this.modules.add(new AutoRegear());
      this.modules.add(new PistonPush());
      this.modules.add(new CrystalBase());
      this.modules.add(new Aura());
      this.modules.add(new PflySwitch());
      this.modules.add(new AutoArmor());
      this.modules.add(new Criticals());
      this.modules.add(new Cornerclip());
      this.modules.add(new AutoFrameDupe());
      this.modules.add(new AirPlace());
      this.modules.add(new Announcer());
      this.modules.add(new PopCounter());
      this.modules.add(new AutoLog());
      this.modules.add(new Media());
      this.modules.add(new Spammer());
      this.modules.add(new MiddleClick());
      this.modules.add(new SmartDisconnect());
      this.modules.add(new Chat());
      this.modules.add(new Replenish());
      this.modules.add(new AutoRespawn());
      this.modules.add(new FastSwim());
      this.modules.add(new PearlPhase());
      this.modules.add(new HoleSnap());
      this.modules.add(new Phase());
      this.modules.add(new FastBreak());
      this.modules.add(new AutoCity());
      this.modules.add(new Speed());
      this.modules.add(new InstantSpeed());
      this.modules.add(new ExtraTab());
      this.modules.add(new TickBoost());
      this.modules.add(new WayPoints());
      this.modules.add(new Trails());
      this.modules.add(new FakePlayer());
      this.modules.add(new FreeLook());
      this.modules.add(new Scaffold());
      this.modules.add(new AntiCrash());
      this.modules.add(new FastProjectile());
      this.modules.add(new ESP());
      this.modules.add(new Environment());
      this.modules.add(new HandVisuals());
      this.modules.add(new Skeleton());
      this.modules.add(new NoRender());
      this.modules.add(new HoleESP());
      this.modules.add(new PopEffects());
      this.modules.add(new BlockHighlight());
      this.modules.add(new ContainerPreview());
      this.modules.add(new Animations());
      this.modules.add(new KillEffects());
      this.modules.add(new EntityTweaks());
      this.modules.add(new AdaptationWheel());
      this.modules.add(new GlintModify());
      this.modules.add(new Trajectories());
      this.modules.add(new CameraTweaks());
      this.modules.add(new LogoutSpots());
      this.modules.add(new AutoEat());
      this.modules.add(new PingSpoof());
      this.modules.add(new NoRotate());
      this.modules.add(new CustomDeathUI());
      this.modules.add(new Cosmetics());
      this.modules.add(new IceSpeed());
      this.modules.add(new ChestSwap());
      this.modules.add(new AntiInteract());
      this.modules.add(new NoEntityTrace());
      this.modules.add(new Freecam());
      this.modules.add(new HoleFill());
      this.modules.add(new SelfTrap());
      this.modules.add(new BurrowBypass());
      this.modules.add(new DirectionHUD());
   }

   public Module getModuleByName(String name) {
      for(Module module : this.modules) {
         if (module.getName().equalsIgnoreCase(name)) {
            return module;
         }
      }

      return null;
   }

   public <T extends Module> T getModuleByClass(Class<T> clazz) {
      for(Module module : this.modules) {
         if (clazz.isInstance(module)) {
            return (T)module;
         }
      }

      return null;
   }

   public ArrayList<Module> getModules() {
      return this.modules;
   }

   public boolean isModuleEnabled(String name) {
      Module module = this.getModuleByName(name);
      return module != null && module.isOn();
   }

   public Module getModuleByDisplayName(String displayName) {
      for(Module module : this.modules) {
         if (module.getDisplayName().equalsIgnoreCase(displayName)) {
            return module;
         }
      }

      return null;
   }

   public ArrayList<Module> getModulesByCategory(Module.Category category) {
      ArrayList<Module> modulesCategory = new ArrayList();
      this.modules.forEach((module) -> {
         if (module.getCategory() == category) {
            modulesCategory.add(module);
         }

      });
      return modulesCategory;
   }

   public List<Module.Category> getCategories() {
      return Arrays.asList(Module.Category.values());
   }

   public void onLoad() {
      Stream var10000 = this.modules.stream().filter(Module::listening);
      EventBus var10001 = MinecraftForge.EVENT_BUS;
      var10000.forEach(var10001::register);
      this.modules.forEach(Module::onLoad);
   }

   public void onUpdate() {
      this.modules.stream().filter(Feature::isEnabled).forEach(Module::onUpdate);
   }

   public void onTick() {
      (new ArrayList<Module>(this.modules)).stream().filter(Feature::isEnabled).forEach(Module::onTick);
   }

   public void onRender2D(Render2DEvent event) {
      this.modules.stream().filter(Feature::isEnabled).forEach((module) -> module.onRender2D(event));
   }

   public void onRender3D(Render3DEvent event) {
      this.modules.stream().filter(Feature::isEnabled).forEach((module) -> module.onRender3D(event));
   }

   public void onLogout() {
      this.modules.forEach(Module::onLogout);
   }

   public void onLogin() {
      this.modules.forEach(Module::onLogin);
   }

   public void onUnload() {
      EventBus var10001 = MinecraftForge.EVENT_BUS;
      this.modules.forEach(var10001::unregister);
      this.modules.forEach(Module::onUnload);
   }

   public void onUnloadPost() {
      for(Module module : this.modules) {
         module.enabled.setValue(false);
      }

   }

   public void onKeyPressed(int eventKey) {
      if (eventKey != 0 && Keyboard.getEventKeyState() && !(mc.currentScreen instanceof NewVonwareGui)) {
         this.modules.forEach((module) -> {
            if (module.getBind().getKey() == eventKey) {
               module.toggle();
            }

         });
      }
   }

   public void onMousePressed(int mouseButton) {
      if (mouseButton >= 0 && !(mc.currentScreen instanceof NewVonwareGui)) {
         int offsetButton = mouseButton + 256;
         this.modules.forEach((module) -> {
            if (module.getBind().getKey() == offsetButton) {
               module.toggle();
            }

         });
      }
   }
}
