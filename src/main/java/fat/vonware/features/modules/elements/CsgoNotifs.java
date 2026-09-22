package fat.vonware.features.modules.elements;

import fat.vonware.Vonware;
import fat.vonware.event.events.DeathEvent;
import fat.vonware.event.events.Render2DEvent;
import fat.vonware.event.events.TotemPopEvent;
import fat.vonware.features.gui.newgui.newcomponents.NewVonwareGui;
import fat.vonware.features.modules.HudModule;
import fat.vonware.features.modules.Module;
import fat.vonware.features.setting.Setting;
import fat.vonware.util.Timer;
import fat.vonware.util.Animations.AnimationUtil;
import fat.vonware.util.Animations.render.RenderUtil;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class CsgoNotifs extends HudModule {
   private static CsgoNotifs INSTANCE = new CsgoNotifs();
   public Setting<Boolean> totems = new Setting<Boolean>("Totems", true);
   public Setting<Boolean> self = new Setting<Boolean>("Self", true, (v) -> (Boolean)this.totems.getValue());
   public Setting<Boolean> fadeAnimation = new Setting<Boolean>("Fade Animation", true);
   public final Setting<Integer> dura = new Setting<Integer>("Duration", 1500, 200, 5000);
   public Timer timer = new Timer();
   public String messageHud = "";
   public boolean newnoiajhsdfbngaof = true;
   private final AnimationUtil.DeltaAnimation fadeAnim = new AnimationUtil.DeltaAnimation(0.0F, 200.0F);
   private static final float PANEL_W = 250.0F;
   private static final float PANEL_H = 30.0F;

   public CsgoNotifs() {
      super("CsgoNotifs", "idfk man", Module.Category.Elements, true, false, false, 250.0F, 30.0F);
      this.register(this.totems);
      this.register(this.self);
      this.register(this.fadeAnimation);
      this.register(this.dura);
      this.setInstance();
   }

   public static CsgoNotifs getInstance() {
      if (INSTANCE == null) {
         INSTANCE = new CsgoNotifs();
      }

      return INSTANCE;
   }

   private void setInstance() {
      INSTANCE = this;
   }

   public void onRender2D(Render2DEvent event) {
      this.rect.setX((Float)this.x.getValue());
      this.rect.setY((Float)this.y.getValue());
      this.rect.setWidth(250.0F);
      this.rect.setHeight(30.0F);
      boolean isEditing = mc.currentScreen instanceof NewVonwareGui && ((NewVonwareGui)mc.currentScreen).isHudEditor();
      String previewMsg = isEditing ? "Notch popped 2 totems" : this.messageHud;
      boolean timerActive = isEditing || !this.timer.passedMs(((Integer)this.dura.getValue()).longValue());
      float alpha;
      if ((Boolean)this.fadeAnimation.getValue()) {
         float raw = this.fadeAnim.tick(timerActive);
         alpha = AnimationUtil.easeOutCubic(raw);
      } else {
         alpha = timerActive ? 1.0F : 0.0F;
         this.fadeAnim.reset(timerActive);
      }

      if (!(alpha <= 0.0F)) {
         float rx = this.rect.getX();
         float ry = this.rect.getY();
         float rw = this.rect.getWidth();
         float rh = this.rect.getHeight();
         RenderUtil.drawGradientRect(rx, ry, rx + rw / 2.0F, ry + rh, (long)withAlpha(0, alpha), (long)withAlpha(1610612736, alpha), false);
         RenderUtil.drawGradientRect(rx + rw / 2.0F, ry, rx + rw, ry + rh, (long)withAlpha(1610612736, alpha), (long)withAlpha(0, alpha), false);
         RenderUtil.drawGradientRect(rx, ry - 1.0F, rx + rw / 2.0F, ry, (long)withAlpha(0, alpha), (long)withAlpha(Integer.MIN_VALUE, alpha), false);
         RenderUtil.drawGradientRect(rx + rw / 2.0F, ry - 1.0F, rx + rw, ry, (long)withAlpha(Integer.MIN_VALUE, alpha), (long)withAlpha(0, alpha), false);
         RenderUtil.drawGradientRect(rx, ry + rh, rx + rw / 2.0F, ry + rh + 1.0F, (long)withAlpha(0, alpha), (long)withAlpha(Integer.MIN_VALUE, alpha), false);
         RenderUtil.drawGradientRect(rx + rw / 2.0F, ry + rh, rx + rw, ry + rh + 1.0F, (long)withAlpha(Integer.MIN_VALUE, alpha), (long)withAlpha(0, alpha), false);
         this.renderer.drawString(previewMsg, rx + (rw - (float)this.renderer.getStringWidth(previewMsg)) / 2.0F, ry + (rh - (float)this.renderer.getFontHeight()) / 2.0F, withAlpha(-1, alpha), true);
         mc.getTextureManager().bindTexture(new ResourceLocation("textures/outline_info.png"));
         GlStateManager.color(1.0F, 1.0F, 1.0F, alpha);
         Gui.drawModalRectWithCustomSizedTexture((int)(rx + (rw - 11.0F) / 2.0F), (int)(ry - 2.0F), 0.0F, 0.0F, 11, 11, 11.0F, 11.0F);
         GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      }
   }

   private static int withAlpha(int argb, float factor) {
      int a = (int)((float)(argb >>> 24 & 255) * factor);
      return a << 24 | argb & 16777215;
   }

   public void updateBounds() {
      this.rect.setX((Float)this.x.getValue());
      this.rect.setY((Float)this.y.getValue());
      this.rect.setWidth(250.0F);
      this.rect.setHeight(30.0F);
   }

   @SubscribeEvent
   public void onTotemPop(TotemPopEvent event) {
      if (!fullNullCheck()) {
         EntityPlayer player = event.getEntity();
         if (player != null) {
            if (!player.equals(mc.player) || (Boolean)this.self.getValue()) {
               if ((Boolean)this.totems.getValue()) {
                  String name = player.getName();
                  int count = Vonware.totemPopManager.getPopCount(name);
                  this.messageHud = count == 1 ? name + " popped 1 totem" : name + " popped " + count + " totems";
                  this.onidkfatwanker();
               }
            }
         }
      }
   }

   @SubscribeEvent(
      priority = EventPriority.HIGH
   )
   public void onDeath(DeathEvent event) {
      if (!fullNullCheck()) {
         EntityPlayer player = event.getEntity();
         if (player != null) {
            if ((Boolean)this.totems.getValue()) {
               String name = player.getName();
               int count = Vonware.totemPopManager.getPopCount(name);
               if (count != 0) {
                  this.messageHud = count == 1 ? name + " popped 1 totem" : name + " died after popping " + count + " totems";
                  this.onidkfatwanker();
               }
            }
         }
      }
   }

   public void onidkfatwanker() {
      this.timer.reset();
      this.newnoiajhsdfbngaof = true;
   }
}
