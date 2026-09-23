package fat.vonware.features.modules;

import com.mojang.realmsclient.gui.ChatFormatting;
import fat.vonware.Global;
import fat.vonware.Vonware;
import fat.vonware.event.events.ClientEvent;
import fat.vonware.event.events.Render2DEvent;
import fat.vonware.event.events.Render3DEvent;
import fat.vonware.features.Feature;
import fat.vonware.features.command.Command;
import fat.vonware.features.modules.client.BlockRender3D;
import fat.vonware.features.modules.client.Colors;
import fat.vonware.features.modules.client.Manager;
import fat.vonware.features.modules.elements.FeatureList;
import fat.vonware.features.setting.Bind;
import fat.vonware.features.setting.Setting;
import fat.vonware.util.HexColorText;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.common.MinecraftForge;

public class Module extends Feature {
   private final String description;
   private final Category category;
   public Setting<Boolean> enabled = this.register(new Setting("Enabled", false));
   public Setting<Bind> bind = this.register(new Setting("Keybind", new Bind(-1), "Sets the key that toggles this module."));
   public Setting<Boolean> drawn = this.register(new Setting("Visible", false, "Displays this module's name on FeatureList when enabled."));
   public Setting<Boolean> toggleNotifs = this.register(new Setting("Notify", true, "Sends a toggle message whenever this module is enabled or disabled."));
   public Setting<String> displayName;
   public boolean hasListener;
   public boolean alwaysListening;
   public boolean hidden;
   public float offset;
   public boolean sliding;

   public Module(String name, String description, Category category, boolean hasListener, boolean hidden, boolean alwaysListening) {
      super(name);
      this.displayName = this.register(new Setting("DisplayName", name));
      this.description = description;
      this.category = category;
      this.hasListener = hasListener;
      this.hidden = hidden;
      this.alwaysListening = alwaysListening;
   }

   public boolean isSliding() {
      return this.sliding;
   }

   public void onEnable() {
   }

   public void onDisable() {
   }

   public void onToggle() {
   }

   public void onLoad() {
   }

   public void onTick() {
   }

   public void onLogin() {
   }

   public void onLogout() {
   }

   public void onUpdate() {
   }

   public void onRender2D(Render2DEvent event) {
   }

   public void onRender3D(Render3DEvent event) {
   }

   public void onUnload() {
   }

   public String getDisplayInfo() {
      return null;
   }

   public String getArrayListInfo() {
      return ((Boolean)FeatureList.getInstance().space.getValue() ? Vonware.textManager.capitalSpace(this.getDisplayName()) : this.getDisplayName()) + ChatFormatting.GRAY + (this.getInfo() != null ? " [" + ChatFormatting.WHITE + this.getInfo() + ChatFormatting.GRAY + "]" : "");
   }

   public String getFullArrayString() {
      return this.getDisplayName() + ChatFormatting.GRAY + (this.getDisplayInfo() != null ? " [" + ChatFormatting.WHITE + this.getDisplayInfo() + ChatFormatting.GRAY + "]" : "");
   }

   public boolean isOn() {
      return (Boolean)this.enabled.getValue();
   }

   public boolean isOff() {
      return !(Boolean)this.enabled.getValue();
   }

   public void setEnabled(boolean enabled) {
      if (enabled) {
         this.enable();
      } else {
         this.disable();
      }

   }

   public void enable() {
      if (!(this instanceof Colors) && !(this instanceof Manager) && !(this instanceof BlockRender3D)) {
         this.enabled.setValue(Boolean.TRUE);
         this.onToggle();
         this.onEnable();
         if ((Boolean)this.toggleNotifs.getValue()) {
            mc.ingameGUI.getChatGUI().printChatMessageWithOptionalDeletion(this.getNotifierOn(), 1);
         }

         if (this.isOn() && this.hasListener && !this.alwaysListening) {
            MinecraftForge.EVENT_BUS.register(this);
         }

      }
   }

   public void disable() {
      if (!(this instanceof Colors) && !(this instanceof Manager) && !(this instanceof BlockRender3D)) {
         if (this.hasListener && !this.alwaysListening) {
            MinecraftForge.EVENT_BUS.unregister(this);
         }

         this.enabled.setValue(false);
         if ((Boolean)this.toggleNotifs.getValue()) {
            mc.ingameGUI.getChatGUI().printChatMessageWithOptionalDeletion(this.getNotifierOff(), 1);
         }

         this.onToggle();
         this.onDisable();
      }
   }

   public void toggle() {
      ClientEvent event = new ClientEvent(!this.isEnabled() ? 1 : 0, this);
      MinecraftForge.EVENT_BUS.post(event);
      if (!event.isCanceled()) {
         this.setEnabled(!this.isEnabled());
      }
   }

   public TextComponentString getNotifierOn() {
      Manager manager = Manager.getInstance();
      if (manager != null && manager.notifier.getValue() == Manager.Notifier.Iggyware) {
         return new TextComponentString(ChatFormatting.BLACK + "<niger ware> " + ChatFormatting.WHITE + this.getDisplayName() + " was" + ChatFormatting.GREEN + " enabled" + ChatFormatting.RESET);
      } else {
         String global = HexColorText.color(Global.getGlobalColor().getRGB());
         String moduleColor = manager != null ? HexColorText.color(manager.getEffectiveColor(manager.mysticModuleColor)) : global;
         String wasColor = manager != null ? HexColorText.color(manager.getEffectiveColor(manager.mysticWasColor)) : ChatFormatting.WHITE.toString();
         return new TextComponentString(ChatFormatting.WHITE + "[" + global + "alberto" + ChatFormatting.WHITE + "] " + moduleColor + this.getDisplayName() + wasColor + " was" + ChatFormatting.GREEN + ChatFormatting.BOLD + " enabled!" + ChatFormatting.RESET);
      }
   }

   public TextComponentString getNotifierOff() {
      Manager manager = Manager.getInstance();
      if (manager != null && manager.notifier.getValue() == Manager.Notifier.Iggyware) {
         return new TextComponentString(ChatFormatting.BLACK + "<niger ware> " + ChatFormatting.WHITE + this.getDisplayName() + " was" + ChatFormatting.RED + " disabled" + ChatFormatting.RESET);
      } else {
         String global = HexColorText.color(Global.getGlobalColor().getRGB());
         String moduleColor = manager != null ? HexColorText.color(manager.getEffectiveColor(manager.mysticModuleColor)) : global;
         String wasColor = manager != null ? HexColorText.color(manager.getEffectiveColor(manager.mysticWasColor)) : ChatFormatting.WHITE.toString();
         return new TextComponentString(ChatFormatting.WHITE + "[" + global + "alberto" + ChatFormatting.WHITE + "] " + moduleColor + this.getDisplayName() + wasColor + " was" + ChatFormatting.RED + ChatFormatting.BOLD + " disabled!" + ChatFormatting.RESET);
      }
   }

   public String getDisplayName() {
      return this.displayName.getValue();
   }

   public void setDisplayName(String name) {
      Module module = Vonware.moduleManager.getModuleByDisplayName(name);
      Module originalModule = Vonware.moduleManager.getModuleByName(name);
      if (module == null && originalModule == null) {
         Command.sendMessage(this.getName() + ", has been renamed to: " + name);
         this.displayName.setValue(name);
      } else {
         Command.sendMessage(ChatFormatting.RED + "A module of this name already exists.");
      }
   }

   public String getDescription() {
      return this.description;
   }

   public boolean isDrawn() {
      return (Boolean)this.drawn.getValue();
   }

   public void setDrawn(boolean drawn) {
      this.drawn.setValue(drawn);
   }

   public Category getCategory() {
      return this.category;
   }

   public String getInfo() {
      return null;
   }

   public Bind getBind() {
      return this.bind.getValue();
   }

   public void setBind(int key) {
      this.bind.setValue(new Bind(key));
   }

   public boolean listening() {
      return this.hasListener && this.isOn() || this.alwaysListening;
   }

   public static enum Category {
      COMBAT("Combat"),
      MISC("Misc"),
      PLAYER("Player"),
      MOVEMENT("Movement"),
      RENDER("Render"),
      Elements("Elements"),
      CLIENT("Other");

      private final String name;

      private Category(String name) {
         this.name = name;
      }

      public String getName() {
         return this.name;
      }
   }
}
