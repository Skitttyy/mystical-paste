package fat.vonware.features.modules.render;

import fat.vonware.Global;
import fat.vonware.features.gui.components.items.buttons.render.ColorPickerConfig;
import fat.vonware.features.gui.newgui.newcomponents.NewComponent;
import fat.vonware.features.gui.newgui.newcomponents.preview.IPreviewable;
import fat.vonware.features.modules.Module;
import fat.vonware.features.modules.client.ClickGui;
import fat.vonware.features.modules.client.Colors;
import fat.vonware.features.setting.ColorSetting;
import fat.vonware.features.setting.Setting;
import fat.vonware.util.Animations.render.ColorUtil;
import java.awt.Color;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

public class Chams extends Module implements IPreviewable {
   public static Chams INSTANCE;
   public final Setting<CrystalEnum> crystalEnum;
   public final Setting<PlayerEnum> playerEnum;
   public final Setting<Boolean> self;
   public final Setting<Boolean> texture;
   public final Setting<Boolean> xqz;
   public final Setting<Boolean> shine;
   public final Setting<Boolean> normal;
   public final Setting<Boolean> normalCrystals;
   public final Setting<Boolean> normalPlayers;
   public final ColorSetting playerFillColor;
   public final ColorSetting playerOutlineColor;
   public final ColorSetting crystalFillColor;
   public final ColorSetting crystalOutlineColor;
   public final Setting<Float> lineWidth;
   public final Setting<Integer> visibleAlpha;
   public final Setting<Integer> xqzAlpha;
   public final Setting<Integer> lineAlpha;
   public final Setting<Boolean> hurt;
   public final Setting<Float> scale;
   public final Setting<Float> spin;
   public final Setting<Float> bounce;

   public Chams() {
      super("Chams", "Draws chams on entities", Module.Category.RENDER, true, false, false);
      this.crystalEnum = new Setting<CrystalEnum>("Crystal", Chams.CrystalEnum.Line);
      this.playerEnum = new Setting<PlayerEnum>("Player", Chams.PlayerEnum.Line);
      this.self = new Setting<Boolean>("Self", false);
      this.texture = new Setting<Boolean>("Texture", false);
      this.xqz = new Setting<Boolean>("XQZ", false);
      this.shine = new Setting<Boolean>("Shine", false);
      this.normal = new Setting<Boolean>("Normal", false);
      this.normalCrystals = new Setting<Boolean>("Crystals", false, (v) -> (Boolean)this.normal.getValue());
      this.normalPlayers = new Setting<Boolean>("Players", false, (v) -> (Boolean)this.normal.getValue());
      this.playerFillColor = new ColorSetting("PlayerFillColor", 16777215, true, (v) -> this.playerEnum.getValue() == Chams.PlayerEnum.Cham || this.playerEnum.getValue() == Chams.PlayerEnum.Both);
      this.playerOutlineColor = new ColorSetting("PlayerOutline", 16777215, true, (v) -> this.playerEnum.getValue() == Chams.PlayerEnum.Line || this.playerEnum.getValue() == Chams.PlayerEnum.Both);
      this.crystalFillColor = new ColorSetting("CrystalFillColor", 16777215, true, (v) -> this.crystalEnum.getValue() == Chams.CrystalEnum.Cham || this.crystalEnum.getValue() == Chams.CrystalEnum.Both);
      this.crystalOutlineColor = new ColorSetting("CrystalOutline", 16777215, true, (v) -> this.crystalEnum.getValue() == Chams.CrystalEnum.Line || this.crystalEnum.getValue() == Chams.CrystalEnum.Both);
      this.lineWidth = new Setting<Float>("LineWidth", 1.0F, 0.0F, 3.0F);
      this.visibleAlpha = new Setting<Integer>("Alpha", 150, 0, 255);
      this.xqzAlpha = new Setting<Integer>("XQZ Alpha", 150, 0, 255);
      this.lineAlpha = new Setting<Integer>("LineAlpha", 150, 0, 255);
      this.hurt = new Setting<Boolean>("Hurt", true);
      this.scale = new Setting<Float>("ScaleFactor", 1.0F, 0.1F, 1.0F);
      this.spin = new Setting<Float>("SpinFactor", 1.0F, 0.0F, 10.0F);
      this.bounce = new Setting<Float>("FloatFactor", 1.0F, 0.0F, 1.0F);
      this.register(this.crystalEnum);
      this.register(this.playerEnum);
      this.register(this.self);
      this.register(this.texture);
      this.register(this.xqz);
      this.register(this.shine);
      this.register(this.normal);
      this.register(this.normalCrystals);
      this.register(this.normalPlayers);
      this.register(this.playerFillColor);
      this.register(this.playerOutlineColor);
      this.register(this.crystalFillColor);
      this.register(this.crystalOutlineColor);
      this.register(this.lineWidth);
      this.register(this.visibleAlpha);
      this.register(this.xqzAlpha);
      this.register(this.lineAlpha);
      this.register(this.hurt);
      this.register(this.scale);
      this.register(this.spin);
      this.register(this.bounce);
      INSTANCE = this;
   }

   public IPreviewable.PreviewType getPreviewType() {
      return IPreviewable.PreviewType.CHAMS;
   }

   public boolean isNormalForPlayers() {
      return (Boolean)this.normal.getValue() && (Boolean)this.normalPlayers.getValue();
   }

   public boolean isNormalForCrystals() {
      return (Boolean)this.normal.getValue() && (Boolean)this.normalCrystals.getValue();
   }

   @SubscribeEvent
   public void onRenderLivingPre(RenderLivingEvent.Pre<EntityPlayer> event) {
      if (this.isNormalForPlayers()) {
         if (event.getEntity() instanceof EntityPlayer && ((Boolean)this.self.getValue() || !(event.getEntity() instanceof EntityPlayerSP))) {
            mc.getRenderManager().setRenderShadow(false);
            mc.getRenderManager().setRenderOutlines(false);
            GlStateManager.pushMatrix();
            GlStateManager.depthMask(true);
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.0F, 240.0F);
            GL11.glEnable(32823);
            GL11.glDepthRange((double)0.0F, 0.01);
            GlStateManager.popMatrix();
         }

      }
   }

   @SubscribeEvent
   public void onRenderLivingPost(RenderLivingEvent.Post<EntityPlayer> event) {
      if (this.isNormalForPlayers()) {
         if (event.getEntity() instanceof EntityPlayer && ((Boolean)this.self.getValue() || !(event.getEntity() instanceof EntityPlayerSP))) {
            boolean shadow = mc.getRenderManager().isRenderShadow();
            mc.getRenderManager().setRenderShadow(shadow);
            GlStateManager.pushMatrix();
            GL11.glDisable(32823);
            GL11.glDepthRange((double)0.0F, (double)1.0F);
            GlStateManager.popMatrix();
            GlStateManager.depthMask(true);
         }

      }
   }

   private Color resolveColor(ColorSetting setting) {
      if (ColorPickerConfig.getState(setting).useGlobalColor) {
         Colors colors = Colors.getInstance();
         if ((Boolean)colors.rainbow.getValue()) {
            return colors.rainbowMode.getValue() == Colors.RainbowMode.Static ? ColorUtil.rainbow(0) : ColorUtil.rainbow((int)((float)NewComponent.counter1[0] * (Float)colors.rainbowHue.getValue()));
         } else {
            return (Boolean)ClickGui.getInstance().pulseGui.getValue() ? Global.getPulseColor(colors.getGuiColor(), 0) : colors.getCurrentColor();
         }
      } else {
         int colorInt = (Integer)setting.getValue();
         return new Color(colorInt >> 16 & 255, colorInt >> 8 & 255, colorInt & 255);
      }
   }

   public Color getPlayerFillColor() {
      return this.resolveColor(this.playerFillColor);
   }

   public Color getPlayerOutlineColor() {
      return this.resolveColor(this.playerOutlineColor);
   }

   public Color getCrystalFillColor() {
      return this.resolveColor(this.crystalFillColor);
   }

   public Color getCrystalOutlineColor() {
      return this.resolveColor(this.crystalOutlineColor);
   }

   public static enum CrystalEnum {
      Off,
      Cham,
      Line,
      Both;
   }

   public static enum PlayerEnum {
      Off,
      Cham,
      Line,
      Both;
   }
}
