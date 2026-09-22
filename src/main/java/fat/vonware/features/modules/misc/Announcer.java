package fat.vonware.features.modules.misc;

import fat.vonware.features.command.Command;
import fat.vonware.features.modules.Module;
import fat.vonware.features.setting.Setting;
import fat.vonware.util.EntityUtil;
import fat.vonware.util.MovementUtil;
import fat.vonware.util.Timer;
import fat.vonware.util.Announcer.Language;
import fat.vonware.util.Phase.PacketUtil;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.block.BlockLiquid;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.CPacketChatMessage;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;

public class Announcer extends Module {
   public Setting<Boolean> move = new Setting<Boolean>("Move", false, "Announces how far you have traveled.");
   public Setting<Boolean> mine = new Setting<Boolean>("Break", false, "Announces blocks that you break.");
   public Setting<Boolean> place = new Setting<Boolean>("Place", false, "Announces blocks that you place.");
   public Setting<Boolean> eat = new Setting<Boolean>("Eat", false, "Announces food that you eat.");
   public Setting<Boolean> cycle = new Setting<Boolean>("Cycle", false, "Cycles through the available announcement languages after each announcement.");
   public Setting<Integer> delay = new Setting<Integer>("Delay", 15, 1, 60, "Controls the delay between announcements.");
   protected final Setting<Language> language;
   protected final Setting<AnnounceMode> mode;
   protected String walkMessage;
   protected String placeMessage;
   protected String breakMessage;
   protected String eatMessage;
   protected Timer timer;
   protected Timer moveTimer;
   protected Random random;
   protected Block brokenBlock;
   protected ItemStack placeStack;
   protected ItemStack foodStack;
   protected double speed;
   private final Language[] languages;
   private int count;
   protected LinkedHashMap<Type, Float> events;

   public Announcer() {
      super("Announcer", "Announces shit", Module.Category.MISC, true, false, false);
      this.language = (new Setting("Language", Language.ENGLISH)).withDescription((Function<Language, String>)((mode) -> {
         switch (mode) {
            case ENGLISH:
               return "ENGLISH: Uses the English announcement message set.";
            case SPANISH:
               return "SPANISH: Uses the Spanish announcement message set.";
            case RUSSIAN:
               return "RUSSIAN: Uses the Russian announcement message set.";
            case GERMAN:
               return "GERMAN: Uses the German announcement message set.";
            case ARABIC:
               return "ARABIC: Uses the Arabic announcement message set.";
            case FRENCH:
               return "FRENCH: Uses the French announcement message set.";
            case SWEDISH:
               return "SWEDISH: Uses the Swedish announcement message set.";
            default:
               return "";
         }
      }));
      this.mode = (new Setting("AnnounceMode", Announcer.AnnounceMode.Broadcast)).withDescription((Function<AnnounceMode, String>)((mode) -> {
         switch (mode) {
            case Broadcast:
               return "Broadcast: Sends announcement messages to public server chat.";
            case Clientside:
               return "Clientside: Shows announcement messages only to you without sending them to the server.";
            default:
               return "";
         }
      }));
      this.register(this.move);
      this.register(this.mine);
      this.register(this.place);
      this.register(this.eat);
      this.register(this.cycle);
      this.register(this.delay);
      this.register(this.language);
      this.register(this.mode);
      this.timer = new Timer();
      this.moveTimer = new Timer();
      this.random = new Random();
      this.events = new LinkedHashMap();
      this.languages = new Language[]{Language.ENGLISH, Language.SPANISH, Language.RUSSIAN, Language.GERMAN, Language.ARABIC, Language.FRENCH, Language.SWEDISH};
   }

   public void onEnable() {
      this.reset();
   }

   @SubscribeEvent
   public void onPlayerTick(TickEvent.PlayerTickEvent event) {
      if (event.phase == Phase.START && (Boolean)this.move.getValue()) {
         this.speed += MovementUtil.getDistance2D();
      }

   }

   public void onUpdate() {
      if (mc.player != null && mc.world != null) {
         this.setStrings();
         if ((Boolean)this.move.getValue() && this.moveTimer.passed((long)((Integer)this.delay.getValue() * 1500))) {
            float dist = (float)this.speed;
            if ((double)dist > (double)1.0F) {
               this.events.put(Announcer.Type.WALK, dist);
               this.speed = (double)0.0F;
               this.moveTimer.reset();
            }
         }

         if (!this.events.isEmpty() && this.timer.passed((long)((Integer)this.delay.getValue() * 1000))) {
            try {
               int index = this.random.nextInt(this.events.entrySet().size());

               for(int i = 0; i < this.events.entrySet().size(); ++i) {
                  if (i == index) {
                     List<Map.Entry<Type, Float>> list = new ArrayList(this.events.entrySet());
                     Map.Entry<Type, Float> entry = (Map.Entry)list.get(i);
                     if (entry.getValue() != null && entry.getKey() != null && !EntityUtil.isDead(mc.player)) {
                        switch ((AnnounceMode)this.mode.getValue()) {
                           case Broadcast:
                              PacketUtil.send(new CPacketChatMessage(this.getMessage((Type)entry.getKey(), (Float)entry.getValue())));
                              break;
                           case Clientside:
                              Command.sendSilentMessage(this.getMessage((Type)entry.getKey(), (Float)entry.getValue()));
                        }
                     }
                  }
               }
            } catch (Exception e) {
               e.printStackTrace();
            }

            if ((Boolean)this.cycle.getValue()) {
               if (this.count == 8) {
                  this.count = 0;
               }

               this.language.setValue(this.languages[this.count]);
               ++this.count;
            }

            this.timer.reset();
            this.events.clear();
         }

      }
   }

   @SubscribeEvent
   public void onBreakBlock(PlayerInteractEvent.LeftClickBlock event) {
      if ((Boolean)this.mine.getValue()) {
         this.brokenBlock = mc.world.getBlockState(event.getPos()).getBlock();
         if (this.isValid(this.brokenBlock)) {
            this.addEvent(Announcer.Type.BREAK);
         }
      }

   }

   @SubscribeEvent
   public void onPlaceBlock(PlayerInteractEvent.RightClickBlock event) {
      if ((Boolean)this.place.getValue()) {
         ItemStack stack = mc.player.getHeldItemMainhand();
         if (stack.getItem() instanceof ItemBlock) {
            this.placeStack = stack;
            this.addEvent(Announcer.Type.PLACE);
         }
      }

   }

   private boolean isValid(Block block) {
      return !(block instanceof BlockAir) && !(block instanceof BlockLiquid);
   }

   private boolean isValid(ItemStack stack) {
      return !stack.isEmpty() && (stack.getItem() instanceof ItemBlock || stack.getItem() instanceof ItemFood);
   }

   @SubscribeEvent
   public void onEat(LivingEntityUseItemEvent.Finish event) {
      if (event.getEntity() == mc.player && (Boolean)this.eat.getValue() && this.isValid(event.getItem())) {
         this.foodStack = event.getItem();
         this.addEvent(Announcer.Type.EAT);
      }

   }

   public void onDisable() {
      this.reset();
   }

   public void onWorldLoad() {
      this.reset();
   }

   protected void setStrings() {
      this.walkMessage = ((Language)this.language.getValue()).getWalkMessage();
      this.eatMessage = ((Language)this.language.getValue()).getEatMessage();
      this.placeMessage = ((Language)this.language.getValue()).getPlaceMessage();
      this.breakMessage = ((Language)this.language.getValue()).getBreakMessage();
   }

   public void reset() {
      this.speed = (double)0.0F;
      this.foodStack = null;
      this.placeStack = null;
      this.brokenBlock = null;
      this.moveTimer.reset();
      this.timer.reset();
   }

   protected void addEvent(Type type) {
      this.events.put(type, (Float)this.events.getOrDefault(type, 0.0F) + 1.0F);
   }

   protected String getMessage(Type type, float count) {
      String blockName = "";
      String itemName = "";
      switch (type) {
         case BREAK:
            blockName = this.brokenBlock.getLocalizedName().toLowerCase();
            return this.breakMessage.replace("{amount}", count + "").replace("{name}", count > 1.0F ? "blocks of " + blockName : blockName).replace(".0", "");
         case PLACE:
            blockName = capitalize(this.placeStack.getDisplayName().toLowerCase());
            return this.placeMessage.replace("{amount}", count + "").replace("{name}", count > 1.0F ? "blocks of " + blockName : blockName).replace(".0", "");
         case EAT:
            itemName = this.foodStack.getDisplayName();
            if (count > 1.0F && !itemName.endsWith("s")) {
               itemName = itemName + "s";
            }

            return this.eatMessage.replace("{amount}", count + "").replace("{name}", itemName).replace(".0", "");
         case WALK:
            return this.walkMessage.replace("{blocks}", String.format("%.2f", count) + "");
         default:
            return "[default]";
      }
   }

   public static String capitalize(String str) {
      return str.substring(0, 1).toUpperCase() + str.substring(1);
   }

   public static enum Type {
      WALK,
      PLACE,
      BREAK,
      EAT;
   }

   public static enum AnnounceMode {
      Broadcast,
      Clientside;
   }
}
