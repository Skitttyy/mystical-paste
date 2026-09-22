package fat.vonware.features.modules.combat;

import fat.vonware.Global;
import fat.vonware.Vonware;
import fat.vonware.event.events.EntityWorldEvent;
import fat.vonware.event.events.MotionUpdateEvent;
import fat.vonware.event.events.PacketEvent;
import fat.vonware.event.events.ca.FpsUpdateEvent;
import fat.vonware.features.modules.Module;
import fat.vonware.features.modules.player.FastBreak;
import fat.vonware.features.modules.player.Freecam;
import fat.vonware.features.modules.player.MultiTask;
import fat.vonware.features.setting.Setting;
import fat.vonware.manager.PositionManager;
import fat.vonware.manager.RotationManager;
import fat.vonware.mixin.mixins.accessor.ICPacketUseEntity;
import fat.vonware.mixin.mixins.accessor.ISPacketEntity;
import fat.vonware.util.BlockUtil;
import fat.vonware.util.EntityUtil;
import fat.vonware.util.InventoryUtil;
import fat.vonware.util.MathUtil;
import fat.vonware.util.RotationUtil;
import fat.vonware.util.Timer;
import fat.vonware.util.Animations.render.ColorUtil;
import fat.vonware.util.Animations.render.RenderMethods;
import fat.vonware.util.HoleFill.other.RayTraceUtil;
import fat.vonware.util.OtherRenderShit.ca.BreakResult;
import fat.vonware.util.OtherRenderShit.ca.Pair;
import fat.vonware.util.OtherRenderShit.ca.PlaceResult;
import fat.vonware.util.OtherRenderShit.ca.Result;
import fat.vonware.util.OtherRenderShit.ca.Smoother;
import fat.vonware.util.Phase.CrystalUtil;
import java.awt.Color;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EnumCreatureAttribute;
import net.minecraft.entity.item.EntityEnderCrystal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemAppleGold;
import net.minecraft.item.ItemEndCrystal;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.network.play.client.CPacketHeldItemChange;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock;
import net.minecraft.network.play.client.CPacketUseEntity;
import net.minecraft.network.play.client.CPacketUseEntity.Action;
import net.minecraft.network.play.server.SPacketDestroyEntities;
import net.minecraft.network.play.server.SPacketEntity;
import net.minecraft.network.play.server.SPacketEntityTeleport;
import net.minecraft.network.play.server.SPacketPlayerPosLook;
import net.minecraft.network.play.server.SPacketSoundEffect;
import net.minecraft.network.play.server.SPacketSpawnObject;
import net.minecraft.network.play.server.SPacketSpawnPlayer;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class AutoCrystal extends Module {
   public static AutoCrystal INSTANCE;
   private final List<BlockPos> cachedPlaceCandidates = new ArrayList();
   private BlockPos cachedCandidateOrigin = null;
   private float cachedCandidateRange = -1.0F;
   private final Setting<TargetSorting> targetSorting;
   private final Setting<Float> targetRange;
   private final Setting<Float> minDmg;
   private final Setting<Float> maxSelfDmg;
   private final Setting<Boolean> noSuicide;
   private final Setting<Integer> lethalCrystals;
   private final Setting<Handling> handlePlacements;
   private final Setting<Boolean> armorBreaker;
   private final Setting<Integer> armorThreshold;
   private final Setting<Float> minDmgArmor;
   private final Setting<Boolean> extrapolation;
   private final Setting<Integer> extrapolateTicks;
   private final Setting<Integer> extrapolationSamples;
   private final Setting<Float> placeRange;
   private final Setting<Float> placeWallsRange;
   private final Setting<SwitchMode> autoSwitch;
   private final Setting<Boolean> noGapSwitch;
   private final Setting<Boolean> strictDirection;
   private final Setting<Boolean> onePointThirteen;
   private final Setting<Float> breakRange;
   private final Setting<Float> breakWallsRange;
   private final Setting<SetDeadMode> setDeadMode;
   private final Setting<Boolean> predict;
   private final Setting<Boolean> mainhand;
   private final Setting<AntiWeakness> antiWeakness;
   private final Setting<Float> ticksExisted;
   private final Setting<TicksMode> ticksExistedMode;
   private final Setting<Boolean> ticksExistedOptimize;
   private final Setting<Boolean> inhibit;
   private final Setting<Integer> inhibitDelay;
   private final Setting<Integer> hitCrystalsRemove;
   private final Setting<Boolean> limit;
   private final Setting<Boolean> stuckBreak;
   private final Setting<Boolean> unbreakableCheck;
   private final Setting<Integer> maxHits;
   private final Setting<Integer> stuckBreakTimeout;
   private final Setting<Integer> stuckBreakRetryDelay;
   private final Setting<Boolean> strictRange;
   private final Setting<Boolean> rotate;
   private final Setting<Smoothing> smoothing;
   private final Setting<Float> smoothSpeed;
   private final Setting<Float> smoothDistance;
   private final Setting<Mining> miningIgnore;
   private final Setting<SwapWait> swapWait;
   private final Setting<Integer> swapWaitDelay;
   private final Setting<Integer> breakDelay;
   private final Setting<Integer> placeDelay;
   private final Setting<Sequence> sequence;
   private final Setting<EventSetting> eventSetting;
   private final Setting<Animation> animationType;
   private final Setting<Integer> fadeTime;
   private final Setting<Boolean> renderText;
   private final Setting<RenderMode> renderMode;
   private final Setting<Boolean> renderIcons;
   private final Setting<Boolean> renderSelfDmg;
   private final Setting<Integer> boxAlpha;
   private final Setting<Float> lineWidth;
   private final Setting<Float> textScale;
   private final Map<Integer, Long> hitCrystals;
   private final Map<Integer, Long> lastAttackTimes;
   private final Map<Integer, Integer> crystalHitCounts;
   private final Map<Integer, Integer> lastAttackTicks;
   private final Map<Integer, Long> maxHitsCooldowns;
   private static final long MAX_HITS_COOLDOWN_MS = 350L;
   private static final long ATTACK_TO_PLACE_WINDOW_MS = 30L;
   private static final long FIRST_HIT_CONFIRM_RETRY_MS = 45L;
   private int stuckBreakCrystalId;
   private long stuckBreakStartTime;
   private long lastForcedBreakTime;
   public static List<Pair.BlockPairTime> oldPlacements = new ArrayList();
   public BlockPos renderPos;
   public BlockPos calcPos;
   public BlockPos lastPos;
   boolean breaking;
   boolean swapStop;
   Entity swapStopCrystal;
   boolean doPacketRotPlace;
   boolean doPacketRotBreak;
   private int lastInhibitedCrystalId;
   protected final LinkedList<Long> brokeCrystals;
   int CRYSTALS_PER_SECOND;
   public EntityPlayer target;
   public boolean offhand;
   boolean placeFlag;
   boolean breakFlag;
   Timer placeTimer;
   Timer breakTimer;
   Timer swapTimer;
   private static final long STUCK_TIMEOUT_MS = 750L;
   private final Timer stuckTimer;
   private boolean stuckTimerRunning;
   private BlockPos stuckBlacklistedPos;
   private static final long PREDICT_MATCH_WINDOW_MS = 400L;
   private static final double OWN_CRYSTAL_MATCH_DISTANCE_SQ = 0.16;
   private static final double PLACE_SWITCH_ADVANTAGE = (double)0.5F;
   private long lastPlaceTimestamp;
   private BlockPos expectedOwnSpawnPos;
   private long expectedOwnSpawnTime;
   private float expectedOwnSpawnDamage;
   private final Map<Integer, double[]> trackedPositions;
   private final Map<Integer, MotionHistory> extrapolationHistory;
   private final Map<Integer, MotionHistory> breakExtrapolationHistory;
   private final Map<Integer, CrystalActionState> crystalStates;
   private final Map<Integer, Long> simulatedDeadUntil;
   private final Map<Integer, Long> pendingSpawnAttacks;
   private final Deque<RotationSample> rotationHistory;
   private static final long SIMULATED_DEAD_MS = 125L;
   private static final long ROTATION_HISTORY_MS = 350L;
   private static final float ROTATION_DAMAGE_WINDOW = 1.5F;
   private static final float ROTATION_SCORE_WEIGHT = 0.75F;
   private static final float BLOCKS_PLACE_BONUS = 1.25F;
   private static final float BREAK_SWITCH_ADVANTAGE = 0.35F;
   private static final float ROTATION_VALID_BONUS = 0.85F;
   private static final float BREAK_PREDICTED_WEIGHT = 0.65F;
   private static final float BREAK_CURRENT_WEIGHT = 0.35F;
   private static final float MOVING_COMMIT_BONUS = 0.45F;
   private static final double MOVING_TARGET_SPEED_SQ = 0.0025;
   private int committedCrystalId;
   private BlockPos committedPlacePos;
   private double committedBreakDamage;
   public Entity calcCrystal;
   double renderDMG;
   float[] lastRots;
   boolean attacked;
   float[] placeTargetRot;
   float[] breakTargetRot;
   private EnumFacing currentPlaceSide;
   private final Smoother yawMouseFilter;
   private String lastDisplayInfo;
   private long displayInfoLastChanged;
   private static final long DISPLAY_DEBOUNCE_MS = 150L;

   public AutoCrystal() {
      super("AutoCrystal", "Automatically breaks and places crystals.", Module.Category.COMBAT, true, false, false);
      this.targetSorting = (new Setting("Sort", AutoCrystal.TargetSorting.Damage)).withDescription((Function<TargetSorting, String>)((mode) -> {
         switch (mode) {
            case Damage:
               return "Damage: Chooses the target that produces the highest crystal damage.";
            case Range:
               return "Range: Chooses the closest valid target within Target Range.";
            default:
               return "";
         }
      }));
      this.targetRange = new Setting<Float>("Target Range", 12.0F, 3.0F, 20.0F, "Maximum distance at which players can be considered as targets.");
      this.minDmg = new Setting<Float>("Min Damage", 5.0F, 0.0F, 36.0F, "Minimum damage a crystal placement must deal to the target.");
      this.maxSelfDmg = new Setting<Float>("Max Self Damage", 20.0F, 0.0F, 36.0F, "Maximum self-damage allowed from a crystal placement.");
      this.noSuicide = new Setting<Boolean>("Anti Suicide", false, "Rejects crystal placements that could kill you or deal at least as much damage to you as the target.");
      this.lethalCrystals = new Setting<Integer>("Lethal Crystals", 3, 0, 5, "Allows lower-damage placements when the calculated crystal damage is lethal within this many crystals.");
      this.handlePlacements = (new Setting("Handling", AutoCrystal.Handling.None)).withDescription((Function<Handling, String>)((mode) -> {
         switch (mode) {
            case HitIgnore:
               return "HitIgnore: Allows placement through crystals that were just attacked while still respecting other blocking entities.";
            case Ignore:
               return "Ignore: Ignores end crystals when checking whether a placement position is occupied.";
            case None:
               return "None: Uses normal entity collision checks but makes exceptions for the current crystal and recent placement position.";
            case RealNone:
               return "RealNone: Uses strict entity collision checks and does not ignore living crystals occupying the placement space.";
            default:
               return "";
         }
      }));
      this.armorBreaker = new Setting<Boolean>("Armor Breaker", true, "Allows lower minimum damage when the target has a sufficiently damaged armor piece.");
      this.armorThreshold = new Setting<Integer>("Armor Threshold", 20, 1, 100, (v) -> (Boolean)this.armorBreaker.getValue(), "Armor durability percentage at or below which Armor Breaker uses Armor Min Dmg.");
      this.minDmgArmor = new Setting<Float>("Armor Min Dmg", 1.0F, 0.0F, 10.0F, (v) -> (Boolean)this.armorBreaker.getValue(), "Minimum target damage required when Armor Breaker detects low-durability armor.");
      this.extrapolation = new Setting<Boolean>("Extrapolation", false, "Predicts target movement and calculates crystal damage using the predicted future position.");
      this.extrapolateTicks = new Setting<Integer>("Extrap Ticks", 2, 1, 10, (v) -> (Boolean)this.extrapolation.getValue(), "Number of ticks ahead used when predicting the target's position.");
      this.extrapolationSamples = new Setting<Integer>("Extrap Samples", 3, 2, 8, (v) -> (Boolean)this.extrapolation.getValue(), "Number of recent movement samples used to smooth the target movement prediction.");
      this.placeRange = new Setting<Float>("Place Range", 4.8F, 1.0F, 6.0F, "Maximum distance at which AutoCrystal can place crystals.");
      this.placeWallsRange = new Setting<Float>("Place Walls Range", 3.0F, 1.0F, 6.0F, "Maximum crystal placement distance when the placement position is not visible.");
      this.autoSwitch = (new Setting("Switch Mode", AutoCrystal.SwitchMode.Normal)).withDescription((Function<SwitchMode, String>)((mode) -> {
         switch (mode) {
            case None:
               return "None: Only places when you are already holding crystals in your main hand or offhand.";
            case Normal:
               return "Normal: Visibly switches your selected hotbar slot to crystals before placing.";
            case Silent:
               return "Silent: Temporarily switches to crystals with a ghost slot switch, places, then switches back.";
            case SilentBypass:
               return "SilentBypass: Temporarily switches through the inventory bypass method, places, then switches back.";
            default:
               return "";
         }
      }));
      this.noGapSwitch = new Setting<Boolean>("No Gap Switch", true, (v) -> this.autoSwitch.getValue() == AutoCrystal.SwitchMode.Normal, "Prevents Normal switching from taking you off a golden apple while you are eating it.");
      this.strictDirection = new Setting<Boolean>("Strict Direction", true, "Uses stricter valid-facing checks when choosing the side used to place a crystal.");
      this.onePointThirteen = new Setting<Boolean>("1.13", false, "Uses post-1.12 crystal placement rules that only require the block directly above the base to be clear.");
      this.breakRange = new Setting<Float>("Break Range", 4.8F, 1.0F, 6.0F, "Maximum distance at which visible crystals can be attacked.");
      this.breakWallsRange = new Setting<Float>("Break Walls Range", 3.0F, 1.0F, 6.0F, "Maximum distance at which crystals can be attacked through walls.");
      this.setDeadMode = (new Setting("Set Dead", AutoCrystal.SetDeadMode.Sound)).withDescription((Function<SetDeadMode, String>)((mode) -> {
         switch (mode) {
            case None:
               return "None: Waits for the server to remove attacked crystals normally.";
            case Sound:
               return "Sound: Marks nearby crystals dead when the server sends an explosion sound.";
            case Ghost:
               return "Ghost: Immediately marks an attacked crystal dead client-side and also clears crystals from explosion sounds.";
            default:
               return "";
         }
      }));
      this.predict = new Setting<Boolean>("Boost", false, "Immediately attacks matching crystals as soon as their spawn packet is received after your own placement.");
      this.mainhand = new Setting<Boolean>("Main Swing", false, "Always swings the main hand for crystal place and break animations instead of following the hand holding crystals.");
      this.antiWeakness = (new Setting("AntiWeakness", AutoCrystal.AntiWeakness.Silent)).withDescription((Function<AntiWeakness, String>)((mode) -> {
         switch (mode) {
            case Normal:
               return "Normal: Visibly switches to the best valid weapon before attacking while affected by Weakness.";
            case Silent:
               return "Silent: Temporarily ghost-switches to the best valid weapon for the attack, then switches back.";
            case None:
               return "None: Does not switch weapons when Weakness prevents normal crystal attacks.";
            default:
               return "";
         }
      }));
      this.ticksExisted = new Setting<Float>("Ticks Existed", 1.0F, 0.0F, 10.0F, "Minimum crystal age in ticks required by the selected TicksMode.");
      this.ticksExistedMode = (new Setting("TicksMode", AutoCrystal.TicksMode.OB)).withDescription((Function<TicksMode, String>)((mode) -> {
         switch (mode) {
            case PB:
               return "PB: Applies Ticks Existed while deciding whether a crystal is valid to break.";
            case OB:
               return "OB: Lets the first attack bypass Ticks Existed, then applies the age check to surviving crystal retries.";
            default:
               return "";
         }
      }));
      this.ticksExistedOptimize = new Setting<Boolean>("Optimize", false, "Uses the optimized ticks-existed break handling path where supported by the crystal selection logic.");
      this.inhibit = new Setting<Boolean>("Inhibit", true, "Temporarily suppresses repeated attacks on the same crystal to reduce unnecessary break packet spam.");
      this.inhibitDelay = new Setting<Integer>("InhibitDelay", 100, 0, 1000, "Delay in milliseconds used by the inhibit logic before allowing another attack.");
      this.hitCrystalsRemove = new Setting<Integer>("hitCrystalsRemove", 500, 0, 1000, "How long attacked crystals remain in the recent-hit tracking map before being removed.");
      this.limit = new Setting<Boolean>("Limit", false, "Limits placing while the current crystal is still considered active in the attack/place sequence.");
      this.stuckBreak = new Setting<Boolean>("Stuck Break", true, "Retries crystals that survive normal break attempts instead of allowing the breaker to remain stuck.");
      this.unbreakableCheck = new Setting<Boolean>("Unbreakable Check", true, "Temporarily stops attacking a crystal after too many failed hits so rejected packets are not spammed.");
      this.maxHits = new Setting<Integer>("MaxHits", 8, 1, 10, (v) -> (Boolean)this.unbreakableCheck.getValue(), "Maximum failed attack attempts allowed on one crystal before its temporary cooldown.");
      this.stuckBreakTimeout = new Setting<Integer>("Stuck Timeout", 1500, 250, 5000, (v) -> (Boolean)this.stuckBreak.getValue(), "Time in milliseconds a crystal may survive before Stuck Break performs its forced retry.");
      this.stuckBreakRetryDelay = new Setting<Integer>("Stuck Retry Delay", 1250, 500, 10000, (v) -> (Boolean)this.stuckBreak.getValue(), "Cooldown in milliseconds between forced Stuck Break retry attempts.");
      this.strictRange = new Setting<Boolean>("Strict Range", true, "Applies the stricter NCP crystal range/height validity check.");
      this.rotate = new Setting<Boolean>("Rotate", true, "Rotates toward crystal placement and break positions before interacting.");
      this.smoothing = (new Setting("Smoothing", AutoCrystal.Smoothing.None, (v) -> (Boolean)this.rotate.getValue())).withDescription((Function<Smoothing, String>)((mode) -> {
         switch (mode) {
            case None:
               return "None: Uses the requested crystal rotations without yaw smoothing.";
            case Semi:
               return "Semi: Smooths large yaw changes toward the requested crystal rotation.";
            default:
               return "";
         }
      }));
      this.smoothSpeed = new Setting<Float>("Smooth Speed", 0.5F, 0.1F, 2.0F, (v) -> this.smoothing.getValue() != AutoCrystal.Smoothing.None, "Controls how quickly smoothed yaw moves toward the requested rotation.");
      this.smoothDistance = new Setting<Float>("Distance", 10.0F, 1.0F, 50.0F, (v) -> this.smoothing.getValue() != AutoCrystal.Smoothing.None, "Yaw difference threshold used to decide when rotation smoothing should be applied.");
      this.miningIgnore = (new Setting("Mining", AutoCrystal.Mining.StrictIgnore)).withDescription((Function<Mining, String>)((mode) -> {
         switch (mode) {
            case Ignore:
               return "Ignore: Always ignores mining state when calculating crystal damage.";
            case StrictIgnore:
               return "StrictIgnore: Ignores mining unless FastBreak is actively breaking while you are not already holding crystals.";
            case None:
               return "None: Includes normal mining state when calculating crystal damage.";
            default:
               return "";
         }
      }));
      this.swapWait = (new Setting("Swap Delay", AutoCrystal.SwapWait.Semi)).withDescription((Function<SwapWait, String>)((mode) -> {
         switch (mode) {
            case None:
               return "None: Does not pause crystal actions after a held-item slot change.";
            case Semi:
               return "Semi: Waits after a slot change before breaking crystals, but does not gate placement.";
            case Full:
               return "Full: Waits after a slot change before both breaking and placing crystals.";
            default:
               return "";
         }
      }));
      this.swapWaitDelay = new Setting<Integer>("Swap Wait Delay", 50, 50, 500, (v) -> this.swapWait.getValue() != AutoCrystal.SwapWait.None, "Delay in milliseconds applied after a held-item slot change.");
      this.breakDelay = new Setting<Integer>("Break Delay", 0, 0, 1000, "Delay in milliseconds between normal crystal break attempts.");
      this.placeDelay = new Setting<Integer>("Place Delay", 0, 0, 1000, "Delay in milliseconds between crystal placements.");
      this.sequence = (new Setting("Sequence", AutoCrystal.Sequence.Soft)).withDescription((Function<Sequence, String>)((mode) -> {
         switch (mode) {
            case None:
               return "None: Uses the basic placement/break rotation sequencing logic.";
            case Soft:
               return "Soft: Briefly favors placement rotation after an attack, then returns to break rotation for fast retries.";
            case Full:
               return "Full: Currently uses the same fast sequencing behavior as Soft in this AutoCrystal implementation.";
            default:
               return "";
         }
      }));
      this.eventSetting = (new Setting("Event", AutoCrystal.EventSetting.Pre)).withDescription((Function<EventSetting, String>)((mode) -> {
         switch (mode) {
            case Pre:
               return "Pre: Performs crystal break and place interactions during the PRE motion-update stage.";
            case Post:
               return "Post: Performs crystal break and place interactions during the POST motion-update stage.";
            default:
               return "";
         }
      }));
      this.animationType = (new Setting("Animation", AutoCrystal.Animation.None)).withDescription((Function<Animation, String>)((mode) -> {
         switch (mode) {
            case None:
               return "None: Renders only the current crystal placement position.";
            case Fade:
               return "Fade: Leaves previous placement renders behind and fades them out over time.";
            default:
               return "";
         }
      }));
      this.fadeTime = new Setting<Integer>("Fade Time", 1000, 100, 2000, (v) -> this.animationType.getValue() == AutoCrystal.Animation.Fade, "Time in milliseconds for old crystal placement renders to fully fade out.");
      this.renderText = new Setting<Boolean>("Render Text", true, "Displays calculated crystal damage text at the current placement position.");
      this.renderMode = (new Setting("Render Mode", AutoCrystal.RenderMode.Both)).withDescription((Function<RenderMode, String>)((mode) -> {
         switch (mode) {
            case Box:
               return "Box: Draws only a filled box at the current crystal placement position.";
            case Outline:
               return "Outline: Draws only an outline around the current crystal placement position.";
            case Both:
               return "Both: Draws both a filled box and an outline at the current placement position.";
            case Debug:
               return "Debug: Renders the current placement normally and highlights the calculated position in green.";
            case None:
               return "None: Disables the AutoCrystal placement box render.";
            default:
               return "";
         }
      }));
      this.renderIcons = new Setting<Boolean>("Render Icons", true, "Displays armor and related target icons with the AutoCrystal damage render.");
      this.renderSelfDmg = new Setting<Boolean>("Render Self Dmg", true, "Displays the calculated self-damage alongside the target damage render.");
      this.boxAlpha = new Setting<Integer>("BoxAlpha", 50, 0, 255, "Controls the transparency of the filled crystal placement box.");
      this.lineWidth = new Setting<Float>("LineWidth", 1.4F, 0.1F, 2.0F, "Controls the thickness of the crystal placement outline.");
      this.textScale = new Setting<Float>("TextScale", 1.0F, 0.1F, 2.0F, "Controls the scale of the AutoCrystal damage text.");
      this.hitCrystals = new ConcurrentHashMap();
      this.lastAttackTimes = new ConcurrentHashMap();
      this.crystalHitCounts = new ConcurrentHashMap();
      this.lastAttackTicks = new ConcurrentHashMap();
      this.maxHitsCooldowns = new ConcurrentHashMap();
      this.stuckBreakCrystalId = -1;
      this.stuckBreakStartTime = 0L;
      this.lastForcedBreakTime = 0L;
      this.breaking = false;
      this.swapStop = false;
      this.swapStopCrystal = null;
      this.doPacketRotPlace = false;
      this.doPacketRotBreak = false;
      this.lastInhibitedCrystalId = -1;
      this.brokeCrystals = new LinkedList();
      this.CRYSTALS_PER_SECOND = 0;
      this.placeFlag = false;
      this.breakFlag = false;
      this.placeTimer = new Timer();
      this.breakTimer = new Timer();
      this.swapTimer = new Timer();
      this.stuckTimer = new Timer();
      this.stuckTimerRunning = false;
      this.stuckBlacklistedPos = null;
      this.lastPlaceTimestamp = 0L;
      this.expectedOwnSpawnPos = null;
      this.expectedOwnSpawnTime = 0L;
      this.expectedOwnSpawnDamage = 0.0F;
      this.trackedPositions = new ConcurrentHashMap();
      this.extrapolationHistory = new ConcurrentHashMap();
      this.breakExtrapolationHistory = new ConcurrentHashMap();
      this.crystalStates = new ConcurrentHashMap();
      this.simulatedDeadUntil = new ConcurrentHashMap();
      this.pendingSpawnAttacks = new ConcurrentHashMap();
      this.rotationHistory = new ArrayDeque();
      this.committedCrystalId = -1;
      this.committedPlacePos = null;
      this.committedBreakDamage = (double)0.0F;
      this.lastRots = new float[]{RotationManager.INSTANCE.getServerYaw(), RotationManager.INSTANCE.getServerPitch()};
      this.attacked = false;
      this.placeTargetRot = null;
      this.breakTargetRot = null;
      this.currentPlaceSide = null;
      this.yawMouseFilter = new Smoother();
      this.lastDisplayInfo = "";
      this.displayInfoLastChanged = 0L;
      INSTANCE = this;
      this.register(this.targetSorting);
      this.register(this.targetRange);
      this.register(this.minDmg);
      this.register(this.maxSelfDmg);
      this.register(this.noSuicide);
      this.register(this.lethalCrystals);
      this.register(this.handlePlacements);
      this.register(this.armorBreaker);
      this.register(this.armorThreshold);
      this.register(this.minDmgArmor);
      this.register(this.extrapolation);
      this.register(this.extrapolateTicks);
      this.register(this.extrapolationSamples);
      this.register(this.placeRange);
      this.register(this.placeWallsRange);
      this.register(this.autoSwitch);
      this.register(this.noGapSwitch);
      this.register(this.strictDirection);
      this.register(this.onePointThirteen);
      this.register(this.breakRange);
      this.register(this.breakWallsRange);
      this.register(this.setDeadMode);
      this.register(this.predict);
      this.register(this.mainhand);
      this.register(this.antiWeakness);
      this.register(this.ticksExisted);
      this.register(this.ticksExistedMode);
      this.register(this.ticksExistedOptimize);
      this.register(this.inhibit);
      this.register(this.inhibitDelay);
      this.register(this.hitCrystalsRemove);
      this.register(this.limit);
      this.register(this.stuckBreak);
      this.register(this.unbreakableCheck);
      this.register(this.maxHits);
      this.register(this.stuckBreakTimeout);
      this.register(this.stuckBreakRetryDelay);
      this.register(this.strictRange);
      this.register(this.rotate);
      this.register(this.smoothing);
      this.register(this.smoothSpeed);
      this.register(this.smoothDistance);
      this.register(this.miningIgnore);
      this.register(this.swapWait);
      this.register(this.swapWaitDelay);
      this.register(this.breakDelay);
      this.register(this.placeDelay);
      this.register(this.sequence);
      this.register(this.eventSetting);
      this.register(this.renderText);
      this.register(this.renderMode);
      this.register(this.animationType);
      this.register(this.fadeTime);
      this.register(this.renderIcons);
      this.register(this.renderSelfDmg);
      this.register(this.textScale);
      this.register(this.boxAlpha);
      this.register(this.lineWidth);
   }

   public void onEnable() {
      super.onEnable();
      if (!fullNullCheck()) {
         this.invalidatePlacementCache();
         this.brokeCrystals.clear();
         oldPlacements.clear();
         this.renderPos = null;
         this.target = null;
         this.swapTimer.setDelay(((Integer)this.swapWaitDelay.getValue()).longValue());
         this.calcPos = null;
         this.calcCrystal = null;
         this.breaking = false;
         this.swapStopCrystal = null;
         this.swapStop = false;
         this.breakTargetRot = null;
         this.placeTargetRot = null;
         this.stuckTimerRunning = false;
         this.stuckBlacklistedPos = null;
         this.lastAttackTimes.clear();
         this.lastAttackTicks.clear();
         this.crystalHitCounts.clear();
         this.maxHitsCooldowns.clear();
         this.lastPlaceTimestamp = 0L;
         this.expectedOwnSpawnPos = null;
         this.expectedOwnSpawnTime = 0L;
         this.expectedOwnSpawnDamage = 0.0F;
         this.stuckBreakCrystalId = -1;
         this.stuckBreakStartTime = 0L;
         this.lastForcedBreakTime = 0L;
         this.extrapolationHistory.clear();
         this.breakExtrapolationHistory.clear();
         this.crystalStates.clear();
         this.simulatedDeadUntil.clear();
         this.pendingSpawnAttacks.clear();
         this.rotationHistory.clear();
         this.committedCrystalId = -1;
         this.committedPlacePos = null;
         this.committedBreakDamage = (double)0.0F;
      }
   }

   public void onDisable() {
      super.onDisable();
      this.invalidatePlacementCache();
      this.lastDisplayInfo = "";
      this.displayInfoLastChanged = 0L;
      this.extrapolationHistory.clear();
      this.breakExtrapolationHistory.clear();
      this.crystalStates.clear();
      this.simulatedDeadUntil.clear();
      this.pendingSpawnAttacks.clear();
      this.rotationHistory.clear();
      this.expectedOwnSpawnPos = null;
      this.expectedOwnSpawnTime = 0L;
      this.expectedOwnSpawnDamage = 0.0F;
   }

   @SubscribeEvent
   public void onFpsUpdate(FpsUpdateEvent event) {
      long currentTimeMs = System.currentTimeMillis();
      if (!this.brokeCrystals.isEmpty()) {
         try {
            while(!this.brokeCrystals.isEmpty()) {
               long firstCrystal = (Long)this.brokeCrystals.getFirst();
               long second = 1000L;
               if (currentTimeMs - firstCrystal <= 1000L) {
                  break;
               }

               this.brokeCrystals.remove();
            }
         } catch (Exception var8) {
            this.CRYSTALS_PER_SECOND = 0;
         }
      }

      this.CRYSTALS_PER_SECOND = this.brokeCrystals.size();
      this.hitCrystals.entrySet().removeIf((entry) -> currentTimeMs - (Long)entry.getValue() > (long)(Integer)this.hitCrystalsRemove.getValue());
      this.lastAttackTimes.entrySet().removeIf((entry) -> currentTimeMs - (Long)entry.getValue() > 1500L);
      this.lastAttackTicks.keySet().removeIf((id) -> mc.world == null || mc.world.getEntityByID(id) == null);
      this.crystalHitCounts.keySet().removeIf((id) -> mc.world == null || mc.world.getEntityByID(id) == null);
      this.simulatedDeadUntil.entrySet().removeIf((e) -> currentTimeMs >= (Long)e.getValue());
      this.pendingSpawnAttacks.entrySet().removeIf((e) -> currentTimeMs >= (Long)e.getValue());
      this.rotationHistory.removeIf((sample) -> currentTimeMs - sample.time > 350L);
   }

   @SubscribeEvent
   public void onUpdatePre(MotionUpdateEvent event) {
      if (!fullNullCheck()) {
         if (event.getStage() == 0) {
            this.prepare(event);
            if (this.eventSetting.getValue() == AutoCrystal.EventSetting.Pre) {
               this.interact(event);
            }

         }
      }
   }

   public void interact(MotionUpdateEvent event) {
      if (!(Boolean)this.noGapSwitch.getValue() || !this.isEatingGap() || this.isCaMultiTask()) {
         this.breakCrystal(event);
         int crystalSlot = InventoryUtil.getHotbarItemSlot(Items.END_CRYSTAL);
         if (crystalSlot != -1 || this.offhand) {
            boolean crystalGone = this.calcCrystal == null || this.calcCrystal.isDead || mc.world.getEntityByID(this.calcCrystal.getEntityId()) == null || this.hitCrystals.containsKey(this.calcCrystal.getEntityId());
            if (!(Boolean)this.limit.getValue() || crystalGone) {
               this.placeCrystal(event);
            }
         }

      }
   }

   private boolean isCaMultiTask() {
      return MultiTask.INSTANCE.isEnabled() && (Boolean)MultiTask.INSTANCE.crystalAura.getValue();
   }

   public void prepare(MotionUpdateEvent event) {
      this.offhand = ((ItemStack)mc.player.inventory.offHandInventory.get(0)).getItem() == Items.END_CRYSTAL;
      float beginYaw = event.getYaw();
      float beginPitch = event.getPitch();
      this.placeTimer.setDelay(((Integer)this.placeDelay.getValue()).longValue());
      this.breakTimer.setDelay(((Integer)this.breakDelay.getValue()).longValue());
      this.swapTimer.setDelay(((Integer)this.swapWaitDelay.getValue()).longValue());
      List<EntityPlayer> validTargets = (List)EntityUtil.getTargets(((Float)this.targetRange.getValue()).doubleValue()).filter((entity) -> entity instanceof EntityPlayer).map((entity) -> (EntityPlayer)entity).filter((player) -> !this.isIgnoredFreecamTarget(player)).collect(Collectors.toList());
      if (validTargets.isEmpty()) {
         this.target = null;
         this.calcCrystal = null;
         this.calcPos = null;
         this.breaking = false;
         this.breakTargetRot = null;
         this.placeTargetRot = null;
         this.committedCrystalId = -1;
         this.committedPlacePos = null;
         this.committedBreakDamage = (double)0.0F;
      } else {
         BreakResult breakResult = this.getBestGlobalBreakResult(validTargets, this.calcPos);
         PlaceResult placeResult = this.getBestGlobalPlaceResult(validTargets, breakResult.calcCrystal);
         breakResult = this.getBestGlobalBreakResult(validTargets, placeResult != null ? placeResult.calcPos : this.calcPos);
         if (placeResult != null && placeResult.target != null) {
            this.target = placeResult.target;
         } else {
            this.target = (EntityPlayer)validTargets.get(0);
         }

         Vec3d extrapPos = (Boolean)this.extrapolation.getValue() ? this.getExtrapolatedPos(this.target, (Integer)this.extrapolateTicks.getValue()) : null;
         placeResult = this.retainCurrentPlacement(this.target, breakResult.calcCrystal, extrapPos, placeResult);
         this.calcCrystal = null;
         this.calcPos = null;
         this.placeTargetRot = null;
         this.breakTargetRot = null;
         if (breakResult.calcCrystal != null && !breakResult.calcCrystal.isDead && this.canBreakCrystalOB((EntityEnderCrystal)breakResult.calcCrystal)) {
            this.calcCrystal = breakResult.calcCrystal;
            this.committedCrystalId = this.calcCrystal.getEntityId();
            this.crystalStates.putIfAbsent(this.committedCrystalId, AutoCrystal.CrystalActionState.SEEN);
            this.breakTargetRot = (Boolean)this.rotate.getValue() ? this.getCrystalRotations(this.calcCrystal) : null;
         }

         if (placeResult != null && placeResult.calcPos != null) {
            this.calcPos = placeResult.calcPos;
            this.committedPlacePos = this.calcPos;
            this.currentPlaceSide = placeResult.side;
            this.placeTargetRot = placeResult.rots;
            this.renderDMG = placeResult.bestDMG;
            this.renderPos = this.calcPos;
         } else {
            this.renderPos = null;
            this.renderDMG = (double)0.0F;
            this.currentPlaceSide = null;
            this.committedPlacePos = null;
         }

         if (this.calcCrystal != null && this.calcPos != null) {
            if (!this.stuckTimerRunning) {
               this.stuckTimer.setDelay(750L);
               this.stuckTimer.resetDelay();
               this.stuckTimerRunning = true;
            } else if (this.stuckTimer.isPassed()) {
               this.hitCrystals.remove(this.calcCrystal.getEntityId());
               this.lastInhibitedCrystalId = -1;
               this.breakTimer.setDelay(0L);
               this.stuckTimer.resetDelay();
            }
         } else {
            this.stuckTimerRunning = false;
         }

         this.lastInhibitedCrystalId = -1;
         if ((Boolean)this.rotate.getValue()) {
            if (this.placeTargetRot != null && this.breakTargetRot == null) {
               this.setRots(this.placeTargetRot, event);
            } else if (this.placeTargetRot == null && this.breakTargetRot != null) {
               this.setRots(this.breakTargetRot, event);
            } else if (this.placeTargetRot != null) {
               this.handleSequence(event);
            }

            if ((beginPitch != event.getPitch() || beginYaw != event.getYaw()) && this.smoothing.getValue() != AutoCrystal.Smoothing.None && (Boolean)this.rotate.getValue() && (this.placeTargetRot != null || this.breakTargetRot != null)) {
               float[] curRots = new float[]{RotationManager.INSTANCE.getServerYaw(), RotationManager.INSTANCE.getServerPitch()};
               float[] theseRots = new float[]{event.getYaw(), event.getPitch()};
               if (RotationUtil.hastRotatedYaw(theseRots, curRots, (Float)this.smoothDistance.getValue())) {
                  this.lastRots = new float[]{event.getYaw(), event.getPitch()};
                  return;
               }

               float yaw = this.yawMouseFilter.easeTo(theseRots[0], (Float)this.smoothSpeed.getValue());
               RotationManager.INSTANCE.requestRotation(RotationManager.RotationSource.AUTO_CRYSTAL, yaw, event.getPitch());
            }

            this.lastRots = new float[]{event.getYaw(), event.getPitch()};
         }
      }
   }

   public void handleSequence(MotionUpdateEvent event) {
      switch ((Sequence)this.sequence.getValue()) {
         case None:
            if (this.calcCrystal == null) {
               this.setRots(this.placeTargetRot, event);
            } else if (this.calcPos != null && this.calcPos.equals(this.lastPos) && mc.world.getEntitiesWithinAABB(Entity.class, new AxisAlignedBB(this.calcPos.add(0, 1, 0))).contains(this.calcCrystal) && this.calcCrystal.getPosition().down().equals(this.calcPos)) {
               this.breakFlag = true;
               this.setRots(this.placeTargetRot, event);
            } else if (!this.calcCrystal.getPosition().down().equals(this.calcPos)) {
               this.placeFlag = true;
               this.setRots(this.breakTargetRot, event);
            } else {
               this.breakFlag = true;
               this.setRots(this.placeTargetRot, event);
            }
            break;
         case Soft:
            this.softSequence(event);
            break;
         case Full:
            this.softSequence(event);
      }

   }

   public void softSequence(MotionUpdateEvent event) {
      if (this.calcCrystal != null && this.wasAttackedRecently(this.calcCrystal.getEntityId(), 30L)) {
         this.setRots(this.placeTargetRot, event);
      } else {
         this.setRots(this.breakTargetRot, event);
      }

   }

   public void setRots(float[] rots, MotionUpdateEvent event) {
      RotationManager.INSTANCE.requestRotation(RotationManager.RotationSource.AUTO_CRYSTAL, rots[0], rots[1]);
   }

   @SubscribeEvent
   public void onUpdatePost(MotionUpdateEvent event) {
      if (!fullNullCheck()) {
         if (event.getStage() == 1) {
            if (this.eventSetting.getValue() == AutoCrystal.EventSetting.Post) {
               this.interact(event);
            }

         }
      }
   }

   public Result getTargetResult() {
      switch ((TargetSorting)this.targetSorting.getValue()) {
         case Damage:
            List<Entity> targets = (List)EntityUtil.getTargets(((Float)this.targetRange.getValue()).doubleValue()).collect(Collectors.toList());
            Result bestResult = null;

            for(Entity target : targets) {
               Result currentResult = this.getResult((EntityPlayer)target);
               if (currentResult != null) {
                  if (bestResult == null) {
                     bestResult = currentResult;
                  } else if (currentResult.getDamage() > bestResult.getDamage()) {
                     bestResult = currentResult;
                  }
               }
            }

            if (bestResult == null) {
               this.reset();
               return null;
            }

            return bestResult;
         case Range:
            this.target = EntityUtil.getTarget(((Float)this.targetRange.getValue()).doubleValue());
            if (this.target == null) {
               this.reset();
               return null;
            }

            return this.getResult(this.target);
         default:
            return null;
      }
   }

   public void reset() {
      this.calcCrystal = null;
      this.calcPos = null;
      this.breaking = false;
      this.breakTargetRot = null;
      this.placeTargetRot = null;
      this.stuckTimerRunning = false;
      this.stuckBlacklistedPos = null;
      this.lastAttackTimes.clear();
      this.lastAttackTicks.clear();
      this.crystalHitCounts.clear();
      this.stuckBreakCrystalId = -1;
      this.stuckBreakStartTime = 0L;
      this.lastForcedBreakTime = 0L;
   }

   public Result getResult(EntityPlayer player) {
      if (this.isIgnoredFreecamTarget(player)) {
         return null;
      } else {
         Vec3d breakExtrap = (Boolean)this.extrapolation.getValue() ? this.getBreakExtrapolatedPos(player, (Integer)this.extrapolateTicks.getValue()) : null;
         Vec3d placeExtrap = (Boolean)this.extrapolation.getValue() ? this.getExtrapolatedPos(player, (Integer)this.extrapolateTicks.getValue()) : null;
         BreakResult breakResult = this.getBestBreakResult(player, breakExtrap);
         Entity currentCalcCrystal = breakResult.calcCrystal;
         return new Result(breakResult, this.getBestPlaceResult(player, currentCalcCrystal, placeExtrap));
      }
   }

   public void load(Result result) {
      this.target = result.target;
      boolean offhand = ((ItemStack)mc.player.inventory.offHandInventory.get(0)).getItem() == Items.END_CRYSTAL;
      if (InventoryUtil.getHotbarItemSlot(Items.END_CRYSTAL) == -1 && !offhand) {
         this.calcPos = null;
         this.renderPos = null;
         this.placeTargetRot = null;
         this.renderDMG = (double)0.0F;
      } else if (result.calcPos != null) {
         this.calcPos = result.calcPos;
         this.placeTargetRot = result.placeRots;
         this.renderDMG = result.bestPlaceDMG;
      } else if (result.bestPlaceDMG == (double)0.0F) {
         this.renderPos = null;
      }

      if (result.calcCrystal != null) {
         if (!result.calcCrystal.isDead && this.canBreakCrystalOB((EntityEnderCrystal)result.calcCrystal)) {
            this.calcCrystal = result.calcCrystal;
            this.breakTargetRot = (Boolean)this.rotate.getValue() ? RotationUtil.calculateLookAt(this.calcCrystal.posX, this.calcCrystal.posY - 1.1, this.calcCrystal.posZ) : null;
         } else {
            this.calcCrystal = null;
            this.breakTargetRot = null;
         }
      }

   }

   private BreakResult getBestGlobalBreakResult(List<EntityPlayer> targets, BlockPos preferredPlacePos) {
      EntityPlayer bestTarget = null;
      Entity bestCrystal = null;
      double bestDamage = (double)0.0F;
      double bestScore = -Double.MAX_VALUE;

      for(EntityPlayer candidate : targets) {
         Vec3d extrap = (Boolean)this.extrapolation.getValue() ? this.getBreakExtrapolatedPos(candidate, (Integer)this.extrapolateTicks.getValue()) : null;
         BreakResult current = this.getBestBreakResult(candidate, extrap, preferredPlacePos);
         if (current != null && current.calcCrystal != null) {
            double[] tracked = this.getTrackedPos(current.calcCrystal);
            double damage = extrap != null ? (double)CrystalUtil.calcPlaceDmgAt(tracked[0], tracked[1], tracked[2], candidate, extrap.x, extrap.y, extrap.z, false) : (double)CrystalUtil.calculateDamage(tracked[0], tracked[1], tracked[2], candidate, (double)0.0F);
            double score = this.getBreakScore(current.calcCrystal, damage, preferredPlacePos);
            if (this.committedCrystalId == current.calcCrystal.getEntityId()) {
               score += (double)0.35F;
            }

            if (bestCrystal == null || score > bestScore) {
               bestTarget = candidate;
               bestCrystal = current.calcCrystal;
               bestDamage = damage;
               bestScore = score;
            }
         }
      }

      float[] rotations = bestCrystal != null && (Boolean)this.rotate.getValue() ? this.getCrystalRotations(bestCrystal) : null;
      return new BreakResult(bestTarget != null ? bestTarget : (EntityPlayer)targets.get(0), bestDamage, bestCrystal, rotations);
   }

   private BreakCandidate evaluateBreakCandidate(Entity crystal, double targetDamage, float selfDamage, BlockPos preferredPlacePos) {
      float[] rots = (Boolean)this.rotate.getValue() ? this.getCrystalRotations(crystal) : null;
      float delta = rots != null ? this.getRotationDelta(rots[0], rots[1]) : 0.0F;
      boolean blocksPlace = preferredPlacePos != null && crystal.getEntityBoundingBox().intersects(getCrystalBB(preferredPlacePos));
      double score = this.getBreakScore(crystal, targetDamage, preferredPlacePos);
      return new BreakCandidate(crystal, targetDamage, selfDamage, rots, delta, blocksPlace, score);
   }

   private double getBreakScore(Entity crystal, double damage, BlockPos preferredPlacePos) {
      if (crystal == null) {
         return -Double.MAX_VALUE;
      } else {
         double score = damage;
         if (preferredPlacePos != null && crystal.getEntityBoundingBox().intersects(getCrystalBB(preferredPlacePos))) {
            score = damage + (double)1.25F;
         }

         if ((Boolean)this.rotate.getValue()) {
            float[] rots = this.getCrystalRotations(crystal);
            float delta = this.getRotationDelta(rots[0], rots[1]);
            score -= (double)(Math.min(1.0F, delta / 180.0F) * 0.75F);
            if (this.isCurrentServerRotationValid(crystal)) {
               score += (double)0.85F;
            } else if (this.hasHistoricalRotationTo(crystal)) {
               score += (double)0.25F;
            }
         }

         return score;
      }
   }

   private boolean isCurrentServerRotationValid(Entity crystal) {
      if (crystal != null && mc.world != null && mc.player != null) {
         Vec3d eye = PositionManager.INSTANCE.getEyeVec();
         AxisAlignedBB box = crystal.getEntityBoundingBox().grow(0.1);
         float yaw = RotationManager.INSTANCE.getServerYaw();
         float pitch = RotationManager.INSTANCE.getServerPitch();
         float yawRad = -yaw * ((float)Math.PI / 180F) - (float)Math.PI;
         float pitchRad = -pitch * ((float)Math.PI / 180F);
         float cosYaw = MathHelper.cos(yawRad);
         float sinYaw = MathHelper.sin(yawRad);
         float cosPitch = -MathHelper.cos(pitchRad);
         float sinPitch = MathHelper.sin(pitchRad);
         Vec3d direction = new Vec3d((double)(sinYaw * cosPitch), (double)sinPitch, (double)(cosYaw * cosPitch));
         double reach = (double)Math.max((Float)this.breakRange.getValue(), (Float)this.breakWallsRange.getValue()) + (double)0.5F;
         return box.calculateIntercept(eye, eye.add(direction.scale(reach))) != null;
      } else {
         return false;
      }
   }

   private float blendBreakDamage(Entity crystal, EntityPlayer targetPlayer, Vec3d extrapPos, float predictedDamage, boolean rotationValid, double currentBestDamage) {
      if (crystal != null && targetPlayer != null && extrapPos != null) {
         if (!rotationValid && currentBestDamage > (double)0.5F && (double)(predictedDamage + 1.5F) < currentBestDamage) {
            return predictedDamage;
         } else {
            double[] trackedPos = this.getTrackedPos(crystal);
            float currentDamage = CrystalUtil.calculateDamage(trackedPos[0], trackedPos[1], trackedPos[2], targetPlayer, (double)0.0F);
            return predictedDamage * 0.65F + currentDamage * 0.35F;
         }
      } else {
         return predictedDamage;
      }
   }

   private float getRotationDelta(float yaw, float pitch) {
      float yawDelta = Math.abs(MathHelper.wrapDegrees(yaw - RotationManager.INSTANCE.getServerYaw()));
      float pitchDelta = Math.abs(pitch - RotationManager.INSTANCE.getServerPitch());
      return (float)Math.sqrt((double)(yawDelta * yawDelta + pitchDelta * pitchDelta));
   }

   private boolean hasHistoricalRotationTo(Entity crystal) {
      if (crystal != null && !this.rotationHistory.isEmpty() && mc.world != null) {
         Vec3d eye = PositionManager.INSTANCE.getEyeVec();
         AxisAlignedBB box = crystal.getEntityBoundingBox().grow(0.1);
         long now = System.currentTimeMillis();
         double reach = (double)Math.max((Float)this.breakRange.getValue(), (Float)this.breakWallsRange.getValue()) + (double)0.5F;

         for(RotationSample sample : this.rotationHistory) {
            if (now - sample.time <= 350L) {
               float yawRad = -sample.yaw * ((float)Math.PI / 180F) - (float)Math.PI;
               float pitchRad = -sample.pitch * ((float)Math.PI / 180F);
               float cosYaw = MathHelper.cos(yawRad);
               float sinYaw = MathHelper.sin(yawRad);
               float cosPitch = -MathHelper.cos(pitchRad);
               float sinPitch = MathHelper.sin(pitchRad);
               Vec3d direction = new Vec3d((double)(sinYaw * cosPitch), (double)sinPitch, (double)(cosYaw * cosPitch));
               Vec3d end = eye.add(direction.scale(reach));
               RayTraceResult entityHit = box.calculateIntercept(eye, end);
               if (entityHit != null) {
                  RayTraceResult blockHit = mc.world.rayTraceBlocks(eye, entityHit.hitVec, false, true, false);
                  if (blockHit == null) {
                     return true;
                  }
               }
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private PlaceResult getBestGlobalPlaceResult(List<EntityPlayer> targets, Entity currentCalcCrystal) {
      PlaceResult best = null;
      List<BlockPos> sphere = new ArrayList(this.getPlaceCandidates());
      boolean miningIgnored = this.shouldIgnoreMiningForDamage();
      sphere.sort(Comparator.comparingDouble((posx) -> {
         double nearest = Double.MAX_VALUE;

         for(EntityPlayer player : targets) {
            nearest = Math.min(nearest, posx.distanceSq(player.posX, player.posY, player.posZ));
         }

         return nearest;
      }));

      for(BlockPos pos : sphere) {
         EnumFacing side = BlockUtil.getPlaceableSideCrystal(pos, (Boolean)this.strictDirection.getValue());
         if (side != null && this.isPlacementGeometryValid(pos, side, currentCalcCrystal)) {
            EntityPlayer bestTargetForPos = null;
            float bestTargetDamageForPos = 0.0F;

            for(EntityPlayer candidate : targets) {
               if (candidate != null && !candidate.isDead && !(candidate.getHealth() <= 0.0F)) {
                  Vec3d extrap = (Boolean)this.extrapolation.getValue() ? this.getExtrapolatedPos(candidate, (Integer)this.extrapolateTicks.getValue()) : null;
                  float damage = extrap != null ? CrystalUtil.calcPlaceDmgAt((double)pos.getX() + (double)0.5F, (double)pos.getY() + (double)1.0F, (double)pos.getZ() + (double)0.5F, candidate, extrap.x, extrap.y, extrap.z, miningIgnored) : CrystalUtil.calcPlaceDmg((double)pos.getX() + (double)0.5F, (double)pos.getY() + (double)1.0F, (double)pos.getZ() + (double)0.5F, candidate, (double)0.0F, miningIgnored);
                  boolean lethal = (Integer)this.lethalCrystals.getValue() != 0 && damage * ((Integer)this.lethalCrystals.getValue()).floatValue() >= candidate.getHealth() + candidate.getAbsorptionAmount();
                  boolean acceptable = (double)damage > ((Float)this.minDmg.getValue()).doubleValue() || this.isArmorBreaker(candidate) && damage >= (Float)this.minDmgArmor.getValue() || lethal;
                  if (acceptable && damage > bestTargetDamageForPos) {
                     bestTargetDamageForPos = damage;
                     bestTargetForPos = candidate;
                  }
               }
            }

            if (bestTargetForPos != null) {
               float selfDamage = CrystalUtil.calcPlaceDmg((double)pos.getX() + (double)0.5F, (double)pos.getY() + (double)1.0F, (double)pos.getZ() + (double)0.5F, mc.player, (double)0.0F, miningIgnored);
               if (!this.doesSelfDmgWork(selfDamage, bestTargetDamageForPos, (double)0.0F)) {
                  PlaceResult candidateResult = new PlaceResult(bestTargetForPos, (double)bestTargetDamageForPos, pos, this.getPlaceRot(pos), side, System.currentTimeMillis());
                  if (best == null || candidateResult.bestDMG > best.bestDMG) {
                     best = candidateResult;
                  }
               }
            }
         }
      }

      return best;
   }

   private boolean isPlacementGeometryValid(BlockPos pos, EnumFacing side, Entity currentCalcCrystal) {
      if (pos != null && side != null) {
         if (CrystalUtil.canPlaceCrystal(pos, (Boolean)this.onePointThirteen.getValue(), false) && this.handleBBCrystal(pos, this.lastPos, currentCalcCrystal)) {
            double playerDistance = this.getServerDistanceSq(pos);
            if (playerDistance >= MathUtil.square(((Float)this.placeRange.getValue()).doubleValue())) {
               return false;
            } else if (BlockUtil.placeTrace(pos) && playerDistance >= MathUtil.square(((Float)this.placeWallsRange.getValue()).doubleValue())) {
               return false;
            } else {
               double crystalDistance = PositionManager.INSTANCE.getDistanceSqEyeHeight((double)pos.getX() + (double)0.5F, (double)pos.getY() + 2.7, (double)pos.getZ() + (double)0.5F);
               if (crystalDistance > MathUtil.square(((Float)this.breakRange.getValue()).doubleValue())) {
                  return false;
               } else if (crystalDistance > MathUtil.square(((Float)this.breakWallsRange.getValue()).doubleValue()) && !this.canSeeFromServer(new Vec3d((double)pos.getX() + (double)0.5F, (double)pos.getY() + 2.7, (double)pos.getZ() + (double)0.5F))) {
                  return false;
               } else {
                  return !(Boolean)this.strictRange.getValue() || CrystalUtil.ncpCrystalCheck((double)((float)pos.getY() + 1.0F));
               }
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public PlaceResult getBestPlaceResult(EntityPlayer targetPlayer, Entity currentCalcCrystal, Vec3d extrapPos) {
      BlockPos bestPos = null;
      EnumFacing bestSide = null;
      List<BlockPos> sphere = new ArrayList(this.getPlaceCandidates());
      double tx = extrapPos != null ? extrapPos.x : targetPlayer.posX;
      double ty = extrapPos != null ? extrapPos.y : targetPlayer.posY;
      double tz = extrapPos != null ? extrapPos.z : targetPlayer.posZ;
      sphere.sort(Comparator.comparingDouble((p) -> p.distanceSq(tx, ty, tz)));
      int size = sphere.size();
      double bestDMG = (double)0.5F;

      for(int i = 0; i < size; ++i) {
         BlockPos pos = (BlockPos)sphere.get(i);
         EnumFacing side = BlockUtil.getPlaceableSideCrystal(pos, (Boolean)this.strictDirection.getValue());
         if (side != null && CrystalUtil.canPlaceCrystal(pos, (Boolean)this.onePointThirteen.getValue(), false) && this.handleBBCrystal(pos, this.lastPos, currentCalcCrystal) && !(this.getServerDistanceSq(pos) >= MathUtil.square(((Float)this.placeRange.getValue()).doubleValue()))) {
            boolean trace = BlockUtil.placeTrace(pos);
            if (!trace || !(this.getServerDistanceSq(pos) >= MathUtil.square(((Float)this.placeWallsRange.getValue()).doubleValue()))) {
               double crystalDistance = PositionManager.INSTANCE.getDistanceSqEyeHeight((double)pos.getX() + (double)0.5F, (double)pos.getY() + 2.7, (double)pos.getZ() + (double)0.5F);
               if (!(crystalDistance > MathUtil.square(((Float)this.breakRange.getValue()).doubleValue())) && (!(crystalDistance > MathUtil.square(((Float)this.breakWallsRange.getValue()).doubleValue())) || this.canSeeFromServer(new Vec3d((double)pos.getX() + (double)0.5F, (double)pos.getY() + 2.7, (double)pos.getZ() + (double)0.5F))) && (!(Boolean)this.strictRange.getValue() || CrystalUtil.ncpCrystalCheck((double)((float)pos.getY() + 1.0F)))) {
                  boolean doMiningIgnore = false;
                  switch ((Mining)this.miningIgnore.getValue()) {
                     case Ignore:
                        doMiningIgnore = true;
                        break;
                     case StrictIgnore:
                        doMiningIgnore = this.offhand || mc.player.inventory.getCurrentItem().getItem() instanceof ItemEndCrystal || !((FastBreak)Vonware.moduleManager.getModuleByClass(FastBreak.class)).isBreaking();
                  }

                  float targetDamage;
                  if (extrapPos != null) {
                     targetDamage = CrystalUtil.calcPlaceDmgAt((double)pos.getX() + (double)0.5F, (double)pos.getY() + (double)1.0F, (double)pos.getZ() + (double)0.5F, targetPlayer, extrapPos.x, extrapPos.y, extrapPos.z, doMiningIgnore);
                  } else {
                     targetDamage = CrystalUtil.calcPlaceDmg((double)pos.getX() + (double)0.5F, (double)pos.getY() + (double)1.0F, (double)pos.getZ() + (double)0.5F, targetPlayer, (double)0.0F, doMiningIgnore);
                  }

                  boolean isLethal = (Integer)this.lethalCrystals.getValue() != 0 && targetDamage * ((Integer)this.lethalCrystals.getValue()).floatValue() >= targetPlayer.getHealth() + targetPlayer.getAbsorptionAmount();
                  if ((double)targetDamage > ((Float)this.minDmg.getValue()).doubleValue() || this.isArmorBreaker(targetPlayer) && targetDamage >= (Float)this.minDmgArmor.getValue() || isLethal) {
                     float selfDamage = CrystalUtil.calcPlaceDmg((double)pos.getX() + (double)0.5F, (double)pos.getY() + (double)1.0F, (double)pos.getZ() + (double)0.5F, mc.player, (double)0.0F, doMiningIgnore);
                     if (!this.doesSelfDmgWork(selfDamage, targetDamage, bestDMG)) {
                        bestDMG = (double)targetDamage;
                        bestPos = pos;
                        bestSide = side;
                        if (targetDamage >= targetPlayer.getHealth() + targetPlayer.getAbsorptionAmount()) {
                           break;
                        }
                     }
                  }
               }
            }
         }
      }

      if (bestDMG == (double)0.5F) {
         bestDMG = (double)0.0F;
      }

      return new PlaceResult(targetPlayer, bestDMG, bestPos, this.getPlaceRot(bestPos), bestSide, System.currentTimeMillis());
   }

   private PlaceResult retainCurrentPlacement(EntityPlayer targetPlayer, Entity currentCalcCrystal, Vec3d extrapPos, PlaceResult newlyCalculated) {
      BlockPos retainedPos = this.calcPos;
      if (retainedPos != null && targetPlayer != null) {
         EnumFacing side = BlockUtil.getPlaceableSideCrystal(retainedPos, (Boolean)this.strictDirection.getValue());
         if (side != null && CrystalUtil.canPlaceCrystal(retainedPos, (Boolean)this.onePointThirteen.getValue(), false) && this.handleBBCrystal(retainedPos, this.lastPos, currentCalcCrystal)) {
            double playerDistance = this.getServerDistanceSq(retainedPos);
            if (playerDistance >= MathUtil.square(((Float)this.placeRange.getValue()).doubleValue())) {
               return newlyCalculated;
            } else if (BlockUtil.placeTrace(retainedPos) && playerDistance >= MathUtil.square(((Float)this.placeWallsRange.getValue()).doubleValue())) {
               return newlyCalculated;
            } else {
               Vec3d crystalVec = new Vec3d((double)retainedPos.getX() + (double)0.5F, (double)retainedPos.getY() + 2.7, (double)retainedPos.getZ() + (double)0.5F);
               double crystalDistance = PositionManager.INSTANCE.getDistanceSqEyeHeight(crystalVec.x, crystalVec.y, crystalVec.z);
               if (crystalDistance > MathUtil.square(((Float)this.breakRange.getValue()).doubleValue())) {
                  return newlyCalculated;
               } else if (crystalDistance > MathUtil.square(((Float)this.breakWallsRange.getValue()).doubleValue()) && !this.canSeeFromServer(crystalVec)) {
                  return newlyCalculated;
               } else if ((Boolean)this.strictRange.getValue() && !CrystalUtil.ncpCrystalCheck((double)((float)retainedPos.getY() + 1.0F))) {
                  return newlyCalculated;
               } else {
                  boolean miningIgnored = this.shouldIgnoreMiningForDamage();
                  float retainedDamage = extrapPos != null ? CrystalUtil.calcPlaceDmgAt((double)retainedPos.getX() + (double)0.5F, (double)retainedPos.getY() + (double)1.0F, (double)retainedPos.getZ() + (double)0.5F, targetPlayer, extrapPos.x, extrapPos.y, extrapPos.z, miningIgnored) : CrystalUtil.calcPlaceDmg((double)retainedPos.getX() + (double)0.5F, (double)retainedPos.getY() + (double)1.0F, (double)retainedPos.getZ() + (double)0.5F, targetPlayer, (double)0.0F, miningIgnored);
                  boolean lethal = (Integer)this.lethalCrystals.getValue() != 0 && retainedDamage * ((Integer)this.lethalCrystals.getValue()).floatValue() >= targetPlayer.getHealth() + targetPlayer.getAbsorptionAmount();
                  if (!((double)retainedDamage > ((Float)this.minDmg.getValue()).doubleValue()) && (!this.isArmorBreaker(targetPlayer) || !(retainedDamage >= (Float)this.minDmgArmor.getValue())) && !lethal) {
                     return newlyCalculated;
                  } else {
                     float selfDamage = CrystalUtil.calcPlaceDmg((double)retainedPos.getX() + (double)0.5F, (double)retainedPos.getY() + (double)1.0F, (double)retainedPos.getZ() + (double)0.5F, mc.player, (double)0.0F, miningIgnored);
                     if (this.doesSelfDmgWork(selfDamage, retainedDamage, (double)0.0F)) {
                        return newlyCalculated;
                     } else {
                        return newlyCalculated != null && newlyCalculated.calcPos != null && newlyCalculated.bestDMG > (double)retainedDamage + (double)0.5F ? newlyCalculated : new PlaceResult(targetPlayer, (double)retainedDamage, retainedPos, this.getPlaceRot(retainedPos), side, System.currentTimeMillis());
                     }
                  }
               }
            }
         } else {
            return newlyCalculated;
         }
      } else {
         return newlyCalculated;
      }
   }

   private boolean shouldIgnoreMiningForDamage() {
      switch ((Mining)this.miningIgnore.getValue()) {
         case Ignore:
            return true;
         case StrictIgnore:
            return this.offhand || mc.player.inventory.getCurrentItem().getItem() instanceof ItemEndCrystal || !((FastBreak)Vonware.moduleManager.getModuleByClass(FastBreak.class)).isBreaking();
         case None:
         default:
            return false;
      }
   }

   private boolean doesSelfDmgWork(float selfDamage, float targetDamage, double bestDMG) {
      return (double)selfDamage > ((Float)this.maxSelfDmg.getValue()).doubleValue() || selfDamage + 2.0F > mc.player.getHealth() + mc.player.getAbsorptionAmount() && (Boolean)this.noSuicide.getValue() || selfDamage >= targetDamage && (Boolean)this.noSuicide.getValue() || bestDMG > (double)targetDamage;
   }

   public boolean isArmorBreaker(EntityPlayer e) {
      if (!(Boolean)this.armorBreaker.getValue()) {
         return false;
      } else {
         for(ItemStack stack : e.inventory.armorInventory) {
            if (stack != ItemStack.EMPTY) {
               String armor = InventoryUtil.getArmorPieceName(stack);
               if (armor != null) {
                  int dura = stack.getMaxDamage() - stack.getItemDamage();
                  int scaledDurability = (int)((float)dura / (float)stack.getMaxDamage() * 100.0F);
                  if (scaledDurability <= (Integer)this.armorThreshold.getValue()) {
                     return true;
                  }
               }
            }
         }

         return false;
      }
   }

   public BreakResult getBestBreakResult(EntityPlayer targetPlayer, Vec3d extrapPos) {
      return this.getBestBreakResult(targetPlayer, extrapPos, this.committedPlacePos);
   }

   private BreakResult getBestBreakResult(EntityPlayer targetPlayer, Vec3d extrapPos, BlockPos preferredPlacePos) {
      EntityEnderCrystal sequenceCrystal = this.findSequenceCrystal();
      if (sequenceCrystal != null && !this.isCrystalUnbreakable(sequenceCrystal.getEntityId()) && this.canBreakCrystal(sequenceCrystal)) {
         double[] trackedPos = this.getTrackedPos(sequenceCrystal);
         float selfDamage = CrystalUtil.calculateDamage(trackedPos[0], trackedPos[1], trackedPos[2], mc.player, (double)0.0F);
         if ((double)selfDamage <= ((Float)this.maxSelfDmg.getValue()).doubleValue() && (!(Boolean)this.noSuicide.getValue() || selfDamage + 2.0F <= mc.player.getHealth() + mc.player.getAbsorptionAmount())) {
            float targetDamage;
            if (extrapPos != null) {
               targetDamage = CrystalUtil.calcPlaceDmgAt(trackedPos[0], trackedPos[1], trackedPos[2], targetPlayer, extrapPos.x, extrapPos.y, extrapPos.z, false);
            } else {
               targetDamage = CrystalUtil.calculateDamage(trackedPos[0], trackedPos[1], trackedPos[2], targetPlayer, (double)0.0F);
            }

            return new BreakResult(targetPlayer, (double)targetDamage, sequenceCrystal, (Boolean)this.rotate.getValue() ? this.getCrystalRotations(sequenceCrystal) : null);
         }
      }

      Entity bestCrystal = null;
      double maxDamage = (double)0.5F;
      double bestScore = -Double.MAX_VALUE;
      Entity bestTicksExisted = null;
      int size = mc.world.loadedEntityList.size();

      for(int i = 0; i < size; ++i) {
         Entity entity = (Entity)mc.world.loadedEntityList.get(i);
         if (entity instanceof EntityEnderCrystal && !this.isCrystalUnbreakable(entity.getEntityId()) && this.canBreakCrystal((EntityEnderCrystal)entity)) {
            if (bestTicksExisted == null) {
               bestTicksExisted = entity;
            }

            if (entity.ticksExisted > bestTicksExisted.ticksExisted) {
               bestTicksExisted = entity;
            }

            double[] trackedPos = this.getTrackedPos(entity);
            float predictedDamage;
            if (extrapPos != null) {
               predictedDamage = CrystalUtil.calcPlaceDmgAt(trackedPos[0], trackedPos[1], trackedPos[2], targetPlayer, extrapPos.x, extrapPos.y, extrapPos.z, false);
            } else {
               predictedDamage = CrystalUtil.calculateDamage(trackedPos[0], trackedPos[1], trackedPos[2], targetPlayer, (double)0.0F);
            }

            boolean rotationValid = (Boolean)this.rotate.getValue() && this.isCurrentServerRotationValid(entity);
            float targetDamage = this.blendBreakDamage(entity, targetPlayer, extrapPos, predictedDamage, rotationValid, maxDamage);
            if ((double)targetDamage > ((Float)this.minDmg.getValue()).doubleValue() || this.isArmorBreaker(targetPlayer) && targetDamage >= (Float)this.minDmgArmor.getValue() || (Integer)this.lethalCrystals.getValue() != 0 && targetDamage * ((Integer)this.lethalCrystals.getValue()).floatValue() >= targetPlayer.getHealth() + targetPlayer.getAbsorptionAmount()) {
               float selfDamage = CrystalUtil.calculateDamage(trackedPos[0], trackedPos[1], trackedPos[2], mc.player, (double)0.0F);
               if (!this.doesSelfDmgWork(selfDamage, targetDamage, (double)0.0F)) {
                  BreakCandidate candidate = this.evaluateBreakCandidate(entity, (double)targetDamage, selfDamage, preferredPlacePos);
                  double candidateScore = candidate.score;
                  if (entity.getEntityId() == this.committedCrystalId && this.isTargetMoving(targetPlayer)) {
                     candidateScore += (double)0.45F;
                  }

                  if ((bestCrystal == null || !(candidate.targetDamage + (double)1.5F < maxDamage)) && (bestCrystal == null || candidateScore > bestScore || candidate.targetDamage > maxDamage + (double)1.5F)) {
                     maxDamage = candidate.targetDamage;
                     bestScore = candidateScore;
                     bestCrystal = candidate.crystal;
                  }
               }
            }
         }
      }

      if ((Boolean)this.ticksExistedOptimize.getValue() && bestCrystal != null && bestTicksExisted != bestCrystal && bestTicksExisted.getDistanceSq(bestCrystal) <= (double)6.0F) {
         bestCrystal = bestTicksExisted;
      }

      float[] breakRots = null;
      if (bestCrystal != null && (Boolean)this.rotate.getValue()) {
         breakRots = this.getCrystalRotations(bestCrystal);
      }

      return new BreakResult(this.target, maxDamage, bestCrystal, breakRots);
   }

   private boolean isTargetMoving(EntityPlayer player) {
      if (player == null) {
         return false;
      } else {
         double dx = player.posX - player.prevPosX;
         double dz = player.posZ - player.prevPosZ;
         return dx * dx + dz * dz > 0.0025;
      }
   }

   private EntityEnderCrystal findSequenceCrystal() {
      if (this.lastPos != null && mc.world != null) {
         AxisAlignedBB box = getCrystalBB(this.lastPos);
         EntityEnderCrystal closest = null;
         double closestDistance = Double.MAX_VALUE;

         for(EntityEnderCrystal crystal : mc.world.getEntitiesWithinAABB(EntityEnderCrystal.class, box)) {
            if (crystal != null && !crystal.isDead && !this.isCrystalUnbreakable(crystal.getEntityId()) && crystal.getPosition().down().equals(this.lastPos)) {
               double distance = mc.player.getDistanceSq(crystal);
               if (distance < closestDistance) {
                  closestDistance = distance;
                  closest = crystal;
               }
            }
         }

         return closest;
      } else {
         return null;
      }
   }

   private float[] getCrystalRotations(Entity crystal) {
      AxisAlignedBB box = crystal.getEntityBoundingBox();
      return RotationUtil.calculateLookAt((box.minX + box.maxX) * (double)0.5F, (box.minY + box.maxY) * (double)0.5F, (box.minZ + box.maxZ) * (double)0.5F);
   }

   @SubscribeEvent
   public void onEntityAdd(EntityWorldEvent.Add event) {
      if (event.getEntity() instanceof EntityEnderCrystal && !fullNullCheck()) {
         EntityEnderCrystal crystal = (EntityEnderCrystal)event.getEntity();
         Long expires = (Long)this.pendingSpawnAttacks.remove(crystal.getEntityId());
         if (expires != null && System.currentTimeMillis() <= expires) {
            if (!crystal.isDead && this.target != null && this.canBreakCrystalOB(crystal)) {
               if ((Boolean)this.rotate.getValue()) {
                  float[] rots = this.getCrystalRotations(crystal);
                  RotationManager.INSTANCE.requestRotation(RotationManager.RotationSource.AUTO_CRYSTAL, rots[0], rots[1]);
                  this.breakTargetRot = rots;
               }

               this.calcCrystal = crystal;
               this.committedCrystalId = crystal.getEntityId();
               this.sendAttackPacket(crystal.getEntityId(), crystal);
            }
         }
      }
   }

   @SubscribeEvent
   public void onEntityRemove(EntityWorldEvent.Remove event) {
      if (event.getEntity() instanceof EntityEnderCrystal) {
         int entityId = event.getEntity().getEntityId();
         if (this.hitCrystals.containsKey(entityId)) {
            this.brokeCrystals.add(System.currentTimeMillis());
         }

         this.clearCrystalTracking(entityId);
      }
   }

   private void clearCrystalTracking(int entityId) {
      this.hitCrystals.remove(entityId);
      this.lastAttackTimes.remove(entityId);
      this.lastAttackTicks.remove(entityId);
      this.crystalHitCounts.remove(entityId);
      this.maxHitsCooldowns.remove(entityId);
      this.trackedPositions.remove(entityId);
      this.simulatedDeadUntil.remove(entityId);
      this.pendingSpawnAttacks.remove(entityId);
      this.crystalStates.put(entityId, AutoCrystal.CrystalActionState.CONFIRMED_DEAD);
      if (this.committedCrystalId == entityId) {
         this.committedCrystalId = -1;
         this.committedBreakDamage = (double)0.0F;
      }

      if (this.lastInhibitedCrystalId == entityId) {
         this.lastInhibitedCrystalId = -1;
      }

      if (this.stuckBreakCrystalId == entityId) {
         this.resetStuckBreakTracking();
      }

      if (this.calcCrystal != null && this.calcCrystal.getEntityId() == entityId) {
         this.calcCrystal = null;
      }

      this.stuckTimerRunning = false;
      this.stuckBlacklistedPos = null;
   }

   public double[] getTrackedPos(Entity e) {
      double[] tracked = (double[])this.trackedPositions.get(e.getEntityId());
      return tracked != null ? tracked : new double[]{e.posX, e.posY, e.posZ};
   }

   public boolean canBreakCrystal(EntityEnderCrystal entity) {
      if (entity == null) {
         return false;
      } else if (this.ticksExistedMode.getValue() == AutoCrystal.TicksMode.PB && ((Float)this.ticksExisted.getValue()).intValue() != 0 && entity.ticksExisted < ((Float)this.ticksExisted.getValue()).intValue()) {
         return false;
      } else {
         return PositionManager.INSTANCE.getDistanceSq(entity) < (PositionManager.INSTANCE.canEntityBeSeen(entity) ? MathUtil.square(((Float)this.breakRange.getValue()).doubleValue()) : MathUtil.square(((Float)this.breakWallsRange.getValue()).doubleValue()));
      }
   }

   public boolean canBreakCrystalOB(EntityEnderCrystal entity) {
      if (entity == null) {
         return false;
      } else {
         return PositionManager.INSTANCE.getDistanceSq(entity) < (PositionManager.INSTANCE.canEntityBeSeen(entity) ? MathUtil.square(((Float)this.breakRange.getValue()).doubleValue()) : MathUtil.square(((Float)this.breakWallsRange.getValue()).doubleValue()));
      }
   }

   public void placeCrystal(MotionUpdateEvent event) {
      if (this.placeFlag) {
         this.placeFlag = false;
      } else {
         if (this.calcPos != null) {
            if (this.smoothing.getValue() != AutoCrystal.Smoothing.None && (Boolean)this.rotate.getValue()) {
               float[] curRots = new float[]{event.getYaw(), event.getPitch()};
               if (!RotationUtil.hastRotatedYaw(this.placeTargetRot, curRots, (Float)this.smoothDistance.getValue())) {
                  return;
               }
            }

            if ((Boolean)this.rotate.getValue() && this.placeTargetRot == null) {
               return;
            }

            if (this.renderPos != null) {
               oldPlacements.removeIf((pair) -> ((BlockPos)pair.key()).equals(this.renderPos));
               oldPlacements.add(new Pair.BlockPairTime(this.renderPos));
            }

            if (this.placeTimer.isPassed()) {
               if (this.swapWait.getValue() == AutoCrystal.SwapWait.Full && !this.swapTimer.isPassed()) {
                  return;
               }

               boolean doSilent = false;
               int crystalSlot = InventoryUtil.getHotbarItemSlot(Items.END_CRYSTAL);
               if (crystalSlot == -1 && !this.offhand) {
                  this.renderPos = null;
                  return;
               }

               if (!this.offhand && crystalSlot != -1 && this.autoSwitch.getValue() == AutoCrystal.SwitchMode.Normal) {
                  if ((Boolean)this.noGapSwitch.getValue()) {
                     if (!this.isEatingGap()) {
                        int prevSlot = mc.player.inventory.currentItem;
                        InventoryUtil.switchToSlotUpdateIf(crystalSlot);
                        if (mc.player.inventory.currentItem != prevSlot) {
                           this.swapTimer.resetDelay();
                        }
                     }
                  } else {
                     int prevSlot = mc.player.inventory.currentItem;
                     InventoryUtil.switchToSlotUpdateIf(crystalSlot);
                     if (mc.player.inventory.currentItem != prevSlot) {
                        this.swapTimer.resetDelay();
                     }
                  }
               }

               if (!this.offhand && crystalSlot != -1 && (this.autoSwitch.getValue() == AutoCrystal.SwitchMode.Silent || this.autoSwitch.getValue() == AutoCrystal.SwitchMode.SilentBypass) && !(mc.player.inventory.getCurrentItem().getItem() instanceof ItemEndCrystal)) {
                  doSilent = true;
               }

               if (this.autoSwitch.getValue() != AutoCrystal.SwitchMode.Silent && this.autoSwitch.getValue() != AutoCrystal.SwitchMode.SilentBypass && !(mc.player.inventory.getCurrentItem().getItem() instanceof ItemEndCrystal) && !this.offhand) {
                  this.renderPos = null;
                  return;
               }

               int oldslot = mc.player.inventory.currentItem;
               if (doSilent) {
                  if (this.autoSwitch.getValue() == AutoCrystal.SwitchMode.SilentBypass) {
                     InventoryUtil.switchToBypass(InventoryUtil.hotbarToInventory(crystalSlot), false);
                  } else {
                     InventoryUtil.switchToSlotGhost(crystalSlot);
                  }
               }

               EnumFacing side = this.currentPlaceSide != null ? this.currentPlaceSide : BlockUtil.getPlaceableSideCrystal(this.calcPos, (Boolean)this.strictDirection.getValue());
               if (side != null) {
                  RayTraceResult ray = RayTraceUtil.rayTraceTo(this.calcPos, mc.world);
                  Vec3d hitVec = ray != null ? ray.hitVec : new Vec3d((double)this.calcPos.getX() + (double)0.5F, (double)this.calcPos.getY() + (double)0.5F, (double)this.calcPos.getZ() + (double)0.5F);
                  float[] f = RayTraceUtil.hitVecToPlaceVec(this.calcPos, hitVec);
                  if (this.doPacketRotPlace && this.placeTargetRot != null) {
                     mc.player.connection.sendPacket(new CPacketPlayer.Rotation(this.placeTargetRot[0], this.placeTargetRot[1], mc.player.onGround));
                  }

                  this.doPacketRotPlace = false;
                  mc.getConnection().sendPacket(new CPacketPlayerTryUseItemOnBlock(this.calcPos, side, this.offhand ? EnumHand.OFF_HAND : EnumHand.MAIN_HAND, f[0], f[1], f[2]));
                  if (doSilent) {
                     if (this.autoSwitch.getValue() == AutoCrystal.SwitchMode.SilentBypass) {
                        InventoryUtil.switchToBypass(InventoryUtil.hotbarToInventory(crystalSlot), true);
                     } else {
                        InventoryUtil.switchToSlotGhost(oldslot);
                     }
                  }

                  this.lastPos = this.calcPos;
                  this.lastPlaceTimestamp = System.currentTimeMillis();
                  this.expectedOwnSpawnPos = this.calcPos;
                  this.expectedOwnSpawnTime = this.lastPlaceTimestamp;
                  this.expectedOwnSpawnDamage = (float)this.renderDMG;
                  mc.player.swingArm((Boolean)this.mainhand.getValue() ? EnumHand.MAIN_HAND : (this.offhand ? EnumHand.OFF_HAND : EnumHand.MAIN_HAND));
                  this.placeTimer.resetDelay();
               }
            }

            this.renderPos = this.calcPos;
         }

      }
   }

   private boolean isIgnoredFreecamTarget(EntityPlayer player) {
      if (player != null && player != mc.player) {
         return Freecam.INSTANCE != null && Freecam.INSTANCE.isEnabled() && Freecam.INSTANCE.getCameraEntity() == player;
      } else {
         return true;
      }
   }

   private boolean wasAttackedRecently(int entityId, long windowMs) {
      Long time = (Long)this.lastAttackTimes.get(entityId);
      return time != null && System.currentTimeMillis() - time <= windowMs;
   }

   private boolean shouldFastConfirmRetry(Entity crystal) {
      if (crystal != null && !crystal.isDead) {
         int entityId = crystal.getEntityId();
         if ((Integer)this.crystalHitCounts.getOrDefault(entityId, 0) != 1) {
            return false;
         } else if (this.crystalStates.get(entityId) != AutoCrystal.CrystalActionState.ATTACK_SENT) {
            return false;
         } else {
            Long lastAttack = (Long)this.lastAttackTimes.get(entityId);
            return lastAttack != null && System.currentTimeMillis() - lastAttack >= 45L;
         }
      } else {
         return false;
      }
   }

   private boolean canSendAttack(int entityId) {
      int tick = mc.player.ticksExisted;
      Integer attackedTick = (Integer)this.lastAttackTicks.get(entityId);
      return attackedTick == null || attackedTick != tick;
   }

   private void markAttackAttempt(int entityId) {
      long now = System.currentTimeMillis();
      this.lastAttackTimes.put(entityId, now);
      this.lastAttackTicks.put(entityId, mc.player.ticksExisted);
      this.hitCrystals.put(entityId, now);
      this.crystalHitCounts.merge(entityId, 1, Integer::sum);
      this.crystalStates.put(entityId, AutoCrystal.CrystalActionState.ATTACK_SENT);
      this.simulatedDeadUntil.put(entityId, now + 125L);
   }

   private boolean isSimulatedDead(int entityId) {
      Long until = (Long)this.simulatedDeadUntil.get(entityId);
      if (until == null) {
         return false;
      } else if (System.currentTimeMillis() >= until) {
         this.simulatedDeadUntil.remove(entityId);
         return false;
      } else {
         return true;
      }
   }

   private void sendAttackPacket(int entityId, Entity crystal) {
      if (mc.getConnection() != null && this.canSendAttack(entityId)) {
         if (crystal != null) {
            mc.getConnection().sendPacket(new CPacketUseEntity(crystal));
         } else {
            CPacketUseEntity packet = new CPacketUseEntity();
            ((ICPacketUseEntity)packet).setEntityId(entityId);
            ((ICPacketUseEntity)packet).setAction(Action.ATTACK);
            mc.getConnection().sendPacket(packet);
         }

         this.markAttackAttempt(entityId);
         this.attacked = true;
         mc.player.swingArm((Boolean)this.mainhand.getValue() ? EnumHand.MAIN_HAND : (this.offhand ? EnumHand.OFF_HAND : EnumHand.MAIN_HAND));
         this.breakTimer.resetDelay();
      }
   }

   private boolean isCrystalUnbreakable(int entityId) {
      if (!(Boolean)this.unbreakableCheck.getValue()) {
         return false;
      } else if ((Integer)this.crystalHitCounts.getOrDefault(entityId, 0) < (Integer)this.maxHits.getValue()) {
         return false;
      } else {
         long now = System.currentTimeMillis();
         Long cooldownStart = (Long)this.maxHitsCooldowns.putIfAbsent(entityId, now);
         if (cooldownStart == null) {
            return true;
         } else if (now - cooldownStart < 350L) {
            return true;
         } else {
            this.crystalHitCounts.put(entityId, 0);
            this.maxHitsCooldowns.remove(entityId);
            this.lastAttackTimes.remove(entityId);
            this.lastAttackTicks.remove(entityId);
            return false;
         }
      }
   }

   private void updateStuckBreakTracking(Entity crystal) {
      if ((Boolean)this.stuckBreak.getValue() && crystal != null && !crystal.isDead) {
         int entityId = crystal.getEntityId();
         if (this.stuckBreakCrystalId != entityId) {
            this.stuckBreakCrystalId = entityId;
            this.stuckBreakStartTime = System.currentTimeMillis();
         }

      } else {
         this.resetStuckBreakTracking();
      }
   }

   private void resetStuckBreakTracking() {
      this.stuckBreakCrystalId = -1;
      this.stuckBreakStartTime = 0L;
   }

   private boolean shouldForceBreak(Entity crystal) {
      if ((Boolean)this.stuckBreak.getValue() && crystal != null && !crystal.isDead && this.stuckBreakCrystalId == crystal.getEntityId()) {
         long now = System.currentTimeMillis();
         return this.stuckBreakStartTime != 0L && now - this.stuckBreakStartTime >= (long)(Integer)this.stuckBreakTimeout.getValue() && now - this.lastForcedBreakTime >= (long)(Integer)this.stuckBreakRetryDelay.getValue();
      } else {
         return false;
      }
   }

   private void forcePacketBreak(Entity crystal) {
      if (crystal != null && !crystal.isDead && mc.getConnection() != null) {
         if ((Boolean)this.rotate.getValue()) {
            float[] rotations = this.getCrystalRotations(crystal);
            RotationManager.INSTANCE.requestRotation(RotationManager.RotationSource.AUTO_CRYSTAL, rotations[0], rotations[1]);
            this.breakTargetRot = rotations;
         }

         this.sendAttackPacket(crystal.getEntityId(), crystal);
         this.lastForcedBreakTime = System.currentTimeMillis();
         this.stuckBreakStartTime = this.lastForcedBreakTime;
      }
   }

   public boolean breakCrystal(MotionUpdateEvent event) {
      this.attacked = false;
      if (this.breakFlag) {
         this.breakFlag = false;
         return true;
      } else if (this.calcCrystal != null) {
         if (this.isCrystalUnbreakable(this.calcCrystal.getEntityId())) {
            this.resetStuckBreakTracking();
            this.calcCrystal = null;
            return true;
         } else {
            this.updateStuckBreakTracking(this.calcCrystal);
            if (this.shouldForceBreak(this.calcCrystal)) {
               this.forcePacketBreak(this.calcCrystal);
               return false;
            } else {
               if (this.smoothing.getValue() != AutoCrystal.Smoothing.None && this.breakTargetRot != null && (Boolean)this.rotate.getValue()) {
                  float[] curRots = new float[]{event.getYaw(), event.getPitch()};
                  if (!RotationUtil.hastRotatedYaw(this.breakTargetRot, curRots, (Float)this.smoothDistance.getValue())) {
                     return true;
                  }
               }

               if ((Boolean)this.rotate.getValue() && this.breakTargetRot == null) {
                  return true;
               } else if (this.calcCrystal.isDead) {
                  this.resetStuckBreakTracking();
                  this.calcCrystal = null;
                  return true;
               } else if (!this.canSendAttack(this.calcCrystal.getEntityId())) {
                  return true;
               } else {
                  boolean firstAttack = !this.lastAttackTimes.containsKey(this.calcCrystal.getEntityId());
                  if (!firstAttack && this.ticksExistedMode.getValue() == AutoCrystal.TicksMode.OB && ((Float)this.ticksExisted.getValue()).intValue() != 0 && this.calcCrystal.ticksExisted < ((Float)this.ticksExisted.getValue()).intValue()) {
                     return true;
                  } else {
                     boolean fastConfirmRetry = !firstAttack && this.shouldFastConfirmRetry(this.calcCrystal);
                     if (!firstAttack && !fastConfirmRetry && !this.breakTimer.isPassed()) {
                        return true;
                     } else if (this.swapWait.getValue() != AutoCrystal.SwapWait.None && !this.swapTimer.isPassed()) {
                        return true;
                     } else {
                        PotionEffect weaknessEffect = mc.player.getActivePotionEffect(MobEffects.WEAKNESS);
                        PotionEffect strengthEffect = mc.player.getActivePotionEffect(MobEffects.STRENGTH);
                        boolean swapBack = false;
                        int curSlot = mc.player.inventory.currentItem;
                        if (this.antiWeakness.getValue() != AutoCrystal.AntiWeakness.None && weaknessEffect != null && (strengthEffect == null || strengthEffect.getAmplifier() <= weaknessEffect.getAmplifier())) {
                           int bestWeapon = this.getBestWeapon();
                           if (bestWeapon != -1 && RotationManager.INSTANCE.getServerSlot() != bestWeapon) {
                              switch ((AntiWeakness)this.antiWeakness.getValue()) {
                                 case Normal:
                                    InventoryUtil.switchToSlotUpdateIf(bestWeapon);
                                    break;
                                 case Silent:
                                    InventoryUtil.switchToSlotGhost(bestWeapon);
                                    swapBack = true;
                              }
                           }
                        }

                        if (this.doPacketRotBreak && this.breakTargetRot != null) {
                           RotationUtil.packetRotate(this.breakTargetRot);
                           this.doPacketRotBreak = false;
                        }

                        this.sendAttackPacket(this.calcCrystal.getEntityId(), this.calcCrystal);
                        if (!mc.player.onGround) {
                           mc.effectRenderer.emitParticleAtEntity(this.calcCrystal, EnumParticleTypes.CRIT, 1);
                        }

                        if (swapBack) {
                           InventoryUtil.switchToSlotGhost(curSlot);
                        }

                        if (this.setDeadMode.getValue() == AutoCrystal.SetDeadMode.Ghost) {
                           this.calcCrystal.setDead();
                        }

                        return false;
                     }
                  }
               }
            }
         }
      } else {
         this.resetStuckBreakTracking();
         return true;
      }
   }

   public int getBestWeapon() {
      int bestSlot = -1;
      double maxDamage = (double)0.0F;

      for(int i = 0; i < 9; ++i) {
         ItemStack stack = mc.player.inventory.getStackInSlot(i);
         if (!stack.isEmpty() && stack.getItem() instanceof ItemSword) {
            double damage = (double)((ItemSword)stack.getItem()).getAttackDamage() + (double)EnchantmentHelper.getModifierForCreature(stack, EnumCreatureAttribute.UNDEFINED);
            if (damage > maxDamage) {
               maxDamage = damage;
               bestSlot = i;
            }
         }
      }

      return bestSlot;
   }

   private boolean isEatingGap() {
      return mc.player.getHeldItemMainhand().getItem() instanceof ItemAppleGold && mc.player.isHandActive();
   }

   private static AxisAlignedBB getCrystalBB(BlockPos pos) {
      return new AxisAlignedBB((double)pos.getX(), (double)(pos.getY() + 1), (double)pos.getZ(), (double)(pos.getX() + 1), (double)(pos.getY() + 3), (double)(pos.getZ() + 1));
   }

   public boolean handleBBCrystal(BlockPos pos, BlockPos lastPos, Entity currentCalcCrystal) {
      switch ((Handling)this.handlePlacements.getValue()) {
         case HitIgnore:
            for(Entity entity : mc.world.getEntitiesWithinAABB(Entity.class, getCrystalBB(pos))) {
               if (!entity.isDead && !this.isSimulatedDead(entity.getEntityId()) && (!(entity instanceof EntityEnderCrystal) || !this.hitCrystals.containsKey(entity.getEntityId()))) {
                  return false;
               }
            }
            break;
         case Ignore:
            for(Entity entity : mc.world.getEntitiesWithinAABB(Entity.class, getCrystalBB(pos))) {
               if (!entity.isDead && !this.isSimulatedDead(entity.getEntityId()) && !(entity instanceof EntityEnderCrystal)) {
                  return false;
               }
            }
            break;
         case None:
            if (lastPos != null && lastPos.equals(pos)) {
               for(Entity entity : mc.world.getEntitiesWithinAABB(Entity.class, getCrystalBB(pos))) {
                  if ((!(entity instanceof EntityEnderCrystal) || !entity.isDead && !entity.getPosition().down().equals(pos) && !entity.equals(currentCalcCrystal)) && !entity.isDead && !this.isSimulatedDead(entity.getEntityId())) {
                     return false;
                  }
               }
            } else {
               for(Entity entity : mc.world.getEntitiesWithinAABB(Entity.class, getCrystalBB(pos))) {
                  if (!entity.isDead && !this.isSimulatedDead(entity.getEntityId()) && !entity.equals(currentCalcCrystal)) {
                     return false;
                  }
               }
            }
            break;
         case RealNone:
            for(Entity entity : mc.world.getEntitiesWithinAABB(Entity.class, getCrystalBB(pos))) {
               if (!entity.isDead && !this.isSimulatedDead(entity.getEntityId())) {
                  return false;
               }
            }
      }

      return true;
   }

   private boolean matchesRecentOwnPlacement(SPacketSpawnObject packet) {
      if (packet != null && packet.getType() == 51 && this.expectedOwnSpawnPos != null && this.expectedOwnSpawnTime != 0L) {
         long elapsed = System.currentTimeMillis() - this.expectedOwnSpawnTime;
         if (elapsed >= 0L && elapsed <= 400L) {
            double dx = packet.getX() - ((double)this.expectedOwnSpawnPos.getX() + (double)0.5F);
            double dy = packet.getY() - ((double)this.expectedOwnSpawnPos.getY() + (double)1.0F);
            double dz = packet.getZ() - ((double)this.expectedOwnSpawnPos.getZ() + (double)0.5F);
            return dx * dx + dy * dy + dz * dz <= 0.16;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private void tryAttackOwnSpawnedCrystal(SPacketSpawnObject packet) {
      if ((Boolean)this.predict.getValue() && this.target != null && this.matchesRecentOwnPlacement(packet)) {
         double x = packet.getX();
         double y = packet.getY();
         double z = packet.getZ();
         boolean visible = this.canSeeFromServer(new Vec3d(x, y, z));
         double rangeSq = visible ? MathUtil.square(((Float)this.breakRange.getValue()).doubleValue()) : MathUtil.square(((Float)this.breakWallsRange.getValue()).doubleValue());
         if (!(PositionManager.INSTANCE.getDistanceSq(x, y, z) >= rangeSq)) {
            float targetDamage = this.expectedOwnSpawnDamage;
            boolean lethal = (Integer)this.lethalCrystals.getValue() != 0 && targetDamage * ((Integer)this.lethalCrystals.getValue()).floatValue() >= this.target.getHealth() + this.target.getAbsorptionAmount();
            if ((double)targetDamage > ((Float)this.minDmg.getValue()).doubleValue() || this.isArmorBreaker(this.target) && targetDamage >= (Float)this.minDmgArmor.getValue() || lethal) {
               float selfDamage = CrystalUtil.calculateDamage(x, y, z, mc.player, (double)0.0F);
               if (!this.doesSelfDmgWork(selfDamage, targetDamage, (double)0.5F) && this.canSendAttack(packet.getEntityID())) {
                  this.committedCrystalId = packet.getEntityID();
                  this.committedBreakDamage = (double)targetDamage;
                  this.crystalStates.put(packet.getEntityID(), AutoCrystal.CrystalActionState.SEEN);
                  this.pendingSpawnAttacks.put(packet.getEntityID(), System.currentTimeMillis() + 250L);
                  this.expectedOwnSpawnPos = null;
                  this.expectedOwnSpawnTime = 0L;
                  this.expectedOwnSpawnDamage = 0.0F;
               }
            }
         }
      }
   }

   private void resetAfterServerCorrection() {
      this.invalidatePlacementCache();
      this.target = null;
      this.calcPos = null;
      this.calcCrystal = null;
      this.renderPos = null;
      this.lastPos = null;
      this.renderDMG = (double)0.0F;
      this.placeTargetRot = null;
      this.breakTargetRot = null;
      this.lastRots = new float[]{RotationManager.INSTANCE.getServerYaw(), RotationManager.INSTANCE.getServerPitch()};
      this.breaking = false;
      this.placeFlag = false;
      this.breakFlag = false;
      this.swapStop = false;
      this.swapStopCrystal = null;
      this.stuckTimerRunning = false;
      this.stuckBlacklistedPos = null;
      this.stuckBreakCrystalId = -1;
      this.stuckBreakStartTime = 0L;
      this.lastForcedBreakTime = 0L;
      this.lastPlaceTimestamp = 0L;
      this.expectedOwnSpawnPos = null;
      this.expectedOwnSpawnTime = 0L;
      this.expectedOwnSpawnDamage = 0.0F;
      this.trackedPositions.clear();
      this.extrapolationHistory.clear();
      this.breakExtrapolationHistory.clear();
      this.crystalStates.clear();
      this.simulatedDeadUntil.clear();
      this.pendingSpawnAttacks.clear();
      this.rotationHistory.clear();
      this.committedCrystalId = -1;
      this.committedPlacePos = null;
      this.committedBreakDamage = (double)0.0F;
      this.lastInhibitedCrystalId = -1;
      this.placeTimer.resetDelay();
      this.breakTimer.resetDelay();
   }

   @SubscribeEvent
   public void onPacket(PacketEvent event) {
      if (!fullNullCheck()) {
         if (event instanceof PacketEvent.Post && event.getPacket() instanceof CPacketPlayer) {
            CPacketPlayer packet = (CPacketPlayer)event.getPacket();
            float yaw = packet.getYaw(RotationManager.INSTANCE.getServerYaw());
            float pitch = packet.getPitch(RotationManager.INSTANCE.getServerPitch());
            this.rotationHistory.addLast(new RotationSample(yaw, pitch, System.currentTimeMillis()));

            while(this.rotationHistory.size() > 12) {
               this.rotationHistory.removeFirst();
            }
         }

         if (event.getPacket() instanceof SPacketPlayerPosLook) {
            this.resetAfterServerCorrection();
         } else {
            if (event.getPacket() instanceof SPacketSpawnObject) {
               SPacketSpawnObject spawn = (SPacketSpawnObject)event.getPacket();
               if (spawn.getType() == 51) {
                  this.crystalStates.putIfAbsent(spawn.getEntityID(), AutoCrystal.CrystalActionState.SEEN);
               }

               this.tryAttackOwnSpawnedCrystal(spawn);
            }

            if (event.getPacket() instanceof SPacketDestroyEntities) {
               SPacketDestroyEntities packet = (SPacketDestroyEntities)event.getPacket();

               for(int entityId : packet.getEntityIDs()) {
                  this.clearCrystalTracking(entityId);
               }
            }

            if (event.getPacket() instanceof SPacketSoundEffect) {
               SPacketSoundEffect packet = (SPacketSoundEffect)event.getPacket();
               if (packet.getCategory() == SoundCategory.BLOCKS && packet.getSound() == SoundEvents.ENTITY_GENERIC_EXPLODE) {
                  for(Entity c : CrystalUtil.getLoadedCrystalsInRange(((Float)this.breakRange.getValue()).doubleValue())) {
                     if (c.getDistance(packet.getX(), packet.getY(), packet.getZ()) <= (double)6.0F && this.setDeadMode.getValue() != AutoCrystal.SetDeadMode.None) {
                        c.setDead();
                        this.clearCrystalTracking(c.getEntityId());
                     }
                  }
               }
            }

            if (event.getPacket() instanceof CPacketHeldItemChange && this.swapWait.getValue() != AutoCrystal.SwapWait.None) {
               PotionEffect strengthEffect = mc.player.getActivePotionEffect(MobEffects.STRENGTH);
               PotionEffect weaknessEffect = mc.player.getActivePotionEffect(MobEffects.WEAKNESS);
               if (this.antiWeakness.getValue() != AutoCrystal.AntiWeakness.None && weaknessEffect != null && (strengthEffect == null || strengthEffect.getAmplifier() <= weaknessEffect.getAmplifier())) {
                  return;
               }

               this.swapTimer.resetDelay();
            }

            if (event.getPacket() instanceof SPacketEntityTeleport) {
               SPacketEntityTeleport pkt = (SPacketEntityTeleport)event.getPacket();
               this.extrapolationHistory.remove(pkt.getEntityId());
               this.trackedPositions.put(pkt.getEntityId(), new double[]{pkt.getX(), pkt.getY(), pkt.getZ()});
            }

            if (event.getPacket() instanceof SPacketEntity.S15PacketEntityRelMove || event.getPacket() instanceof SPacketEntity.S17PacketEntityLookMove) {
               SPacketEntity pkt = (SPacketEntity)event.getPacket();
               int id = ((ISPacketEntity)pkt).getEntityId();
               double[] prev = (double[])this.trackedPositions.get(id);
               if (prev != null) {
                  this.trackedPositions.put(id, new double[]{prev[0] + (double)pkt.getX() / (double)4096.0F, prev[1] + (double)pkt.getY() / (double)4096.0F, prev[2] + (double)pkt.getZ() / (double)4096.0F});
               }
            }

            if (event.getPacket() instanceof SPacketSpawnPlayer) {
               SPacketSpawnPlayer pkt = (SPacketSpawnPlayer)event.getPacket();
               this.trackedPositions.put(pkt.getEntityID(), new double[]{pkt.getX(), pkt.getY(), pkt.getZ()});
            }

         }
      }
   }

   boolean canRender() {
      if ((Boolean)this.noGapSwitch.getValue() && this.isEatingGap()) {
         return false;
      } else {
         int crystalSlot = InventoryUtil.getHotbarItemSlot(Items.END_CRYSTAL);
         if (crystalSlot == -1 && !this.offhand) {
            return false;
         } else {
            return this.target != null;
         }
      }
   }

   @SubscribeEvent
   public void onRender(RenderWorldLastEvent event) {
      if (!fullNullCheck()) {
         if (this.renderMode.getValue() != AutoCrystal.RenderMode.None) {
            Color base = Global.getGlobalColorRainbowS();
            Color fillColor = ColorUtil.newAlpha(base, (Integer)this.boxAlpha.getValue());
            Color lineColor = ColorUtil.newAlpha(base, 255);
            if (this.animationType.getValue() == AutoCrystal.Animation.Fade) {
               oldPlacements.removeIf((pairx) -> System.currentTimeMillis() - (Long)pairx.value() > (long)(Integer)this.fadeTime.getValue());

               for(Pair.BlockPairTime pair : oldPlacements) {
                  if (this.renderPos == null || !((BlockPos)pair.key()).equals(this.renderPos)) {
                     float normal = (float)Math.min((double)1.0F, (double)(System.currentTimeMillis() - (Long)pair.value()) / (double)(Integer)this.fadeTime.getValue());
                     Color fadeFill = ColorUtil.newAlpha(fillColor, (int)((1.0F - normal) * (float)fillColor.getAlpha()));
                     Color fadeLine = ColorUtil.newAlpha(lineColor, (int)((1.0F - normal) * (float)lineColor.getAlpha()));
                     AxisAlignedBB fadeBB = new AxisAlignedBB((double)((BlockPos)pair.key()).getX() - mc.getRenderManager().viewerPosX, (double)((BlockPos)pair.key()).getY() - mc.getRenderManager().viewerPosY, (double)((BlockPos)pair.key()).getZ() - mc.getRenderManager().viewerPosZ, (double)(((BlockPos)pair.key()).getX() + 1) - mc.getRenderManager().viewerPosX, (double)(((BlockPos)pair.key()).getY() + 1) - mc.getRenderManager().viewerPosY, (double)(((BlockPos)pair.key()).getZ() + 1) - mc.getRenderManager().viewerPosZ);
                     RenderMethods.startRender();
                     RenderMethods.drawBox(fadeBB, fadeFill);
                     RenderMethods.drawOutline(fadeBB, (Float)this.lineWidth.getValue(), fadeLine);
                     RenderMethods.endRender();
                  }
               }
            }

            if (this.target != null && this.renderPos != null) {
               if (this.canRender()) {
                  AxisAlignedBB bb = new AxisAlignedBB((double)this.renderPos.getX() - mc.getRenderManager().viewerPosX, (double)this.renderPos.getY() - mc.getRenderManager().viewerPosY, (double)this.renderPos.getZ() - mc.getRenderManager().viewerPosZ, (double)(this.renderPos.getX() + 1) - mc.getRenderManager().viewerPosX, (double)(this.renderPos.getY() + 1) - mc.getRenderManager().viewerPosY, (double)(this.renderPos.getZ() + 1) - mc.getRenderManager().viewerPosZ);
                  RenderMethods.startRender();
                  switch ((RenderMode)this.renderMode.getValue()) {
                     case Box:
                        RenderMethods.drawBox(bb, fillColor);
                        break;
                     case Outline:
                        RenderMethods.drawOutline(bb, (Float)this.lineWidth.getValue(), lineColor);
                        break;
                     case Both:
                        RenderMethods.drawBox(bb, fillColor);
                        RenderMethods.drawOutline(bb, (Float)this.lineWidth.getValue(), lineColor);
                        break;
                     case Debug:
                        if (this.calcPos != null) {
                           AxisAlignedBB calcBB = new AxisAlignedBB((double)this.calcPos.getX() - mc.getRenderManager().viewerPosX, (double)this.calcPos.getY() - mc.getRenderManager().viewerPosY, (double)this.calcPos.getZ() - mc.getRenderManager().viewerPosZ, (double)(this.calcPos.getX() + 1) - mc.getRenderManager().viewerPosX, (double)(this.calcPos.getY() + 1) - mc.getRenderManager().viewerPosY, (double)(this.calcPos.getZ() + 1) - mc.getRenderManager().viewerPosZ);
                           RenderMethods.drawBox(calcBB, Color.GREEN);
                           RenderMethods.drawOutline(calcBB, (Float)this.lineWidth.getValue(), Color.GREEN);
                        }

                        RenderMethods.drawBox(bb, fillColor);
                        RenderMethods.drawOutline(bb, (Float)this.lineWidth.getValue(), lineColor);
                  }

                  RenderMethods.endRender();
                  if ((Boolean)this.renderText.getValue()) {
                     this.renderDamageText(this.renderPos, this.renderDMG);
                  }

               }
            }
         }
      }
   }

   private void renderDamageText(BlockPos pos, double damage) {
      float x = (float)pos.getX() + 0.5F;
      float yIn = (float)pos.getY();
      float z = (float)pos.getZ() + 0.5F;
      double y = (double)yIn + 0.6;
      String enemyDamageText = String.format("%.1f", this.renderDMG);
      String selfDamageText = null;
      if ((Boolean)this.renderSelfDmg.getValue()) {
         float selfDamage = CrystalUtil.calcPlaceDmg((double)pos.getX() + (double)0.5F, (double)pos.getY() + (double)1.0F, (double)pos.getZ() + (double)0.5F, mc.player, (double)0.0F, false);
         selfDamageText = String.format("%.1f", selfDamage);
      }

      GlStateManager.pushMatrix();
      RenderHelper.enableStandardItemLighting();
      GlStateManager.enablePolygonOffset();
      GlStateManager.doPolygonOffset(1.0F, -1500000.0F);
      GlStateManager.disableLighting();
      GlStateManager.disableDepth();
      float baseScale = 0.021666666F;
      double renderX = mc.getRenderManager().viewerPosX;
      double renderY = mc.getRenderManager().viewerPosY;
      double renderZ = mc.getRenderManager().viewerPosZ;
      GlStateManager.translate((double)x - renderX, y - renderY, (double)z - renderZ);
      GlStateManager.glNormal3f(0.0F, 1.0F, 0.0F);
      GlStateManager.rotate(-mc.player.rotationYaw, 0.0F, 1.0F, 0.0F);
      GlStateManager.rotate(mc.player.rotationPitch, mc.gameSettings.thirdPersonView == 2 ? -1.0F : 1.0F, 0.0F, 0.0F);
      GlStateManager.scale(-0.021666666F, -0.021666666F, 0.021666666F);
      int distance = (int)mc.player.getDistance((double)x, y, (double)z);
      float scaleD = (float)distance / 2.0F / 3.0F;
      if (scaleD < 1.0F) {
         scaleD = 1.0F;
      }

      GlStateManager.scale(scaleD, scaleD, scaleD);
      float tScale = (Float)this.textScale.getValue();
      int iconSize = (int)(10.0F * tScale);
      int spacing = 2;
      int enemyTextWidth = (int)((float)Vonware.textManager.getStringWidth(enemyDamageText) * tScale);
      int slashWidth = (Boolean)this.renderSelfDmg.getValue() ? (int)((float)Vonware.textManager.getStringWidth("/") * tScale) : 0;
      int selfTextWidth = selfDamageText != null ? (int)((float)Vonware.textManager.getStringWidth(selfDamageText) * tScale) : 0;
      int totalWidth;
      if ((Boolean)this.renderIcons.getValue()) {
         totalWidth = iconSize + 2 + enemyTextWidth;
         if ((Boolean)this.renderSelfDmg.getValue()) {
            totalWidth += slashWidth + iconSize + 2 + selfTextWidth;
         }
      } else {
         totalWidth = enemyTextWidth;
         if ((Boolean)this.renderSelfDmg.getValue()) {
            totalWidth = enemyTextWidth + slashWidth + selfTextWidth;
         }
      }

      GlStateManager.translate(-((double)totalWidth / (double)2.0F), (double)0.0F, (double)0.0F);
      int currentX = 0;
      if ((Boolean)this.renderIcons.getValue()) {
         GlStateManager.enableTexture2D();
         GlStateManager.enableBlend();
         mc.getTextureManager().bindTexture(new ResourceLocation("textures/combat.png"));
         GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
         this.drawTexturedRect(currentX, (int)(-2.0F * tScale), iconSize, iconSize);
         currentX += iconSize + 2;
      }

      GlStateManager.enableTexture2D();
      GlStateManager.pushMatrix();
      GlStateManager.translate((float)currentX, 0.0F, 0.0F);
      GlStateManager.scale(tScale, tScale, 1.0F);
      Vonware.textManager.drawStringWithShadow(enemyDamageText, 0.0F, 0.0F, -1);
      GlStateManager.popMatrix();
      currentX += enemyTextWidth;
      if ((Boolean)this.renderSelfDmg.getValue() && selfDamageText != null) {
         GlStateManager.pushMatrix();
         GlStateManager.translate((float)currentX, 0.0F, 0.0F);
         GlStateManager.scale(tScale, tScale, 1.0F);
         Vonware.textManager.drawStringWithShadow("/", 0.0F, 0.0F, -1);
         GlStateManager.popMatrix();
         currentX += slashWidth;
         if ((Boolean)this.renderIcons.getValue()) {
            GlStateManager.enableTexture2D();
            mc.getTextureManager().bindTexture(new ResourceLocation("textures/skull.png"));
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            this.drawTexturedRect(currentX, (int)(-2.0F * tScale), iconSize, iconSize);
            currentX += iconSize + 2;
         }

         GlStateManager.enableTexture2D();
         GlStateManager.pushMatrix();
         GlStateManager.translate((float)currentX, 0.0F, 0.0F);
         GlStateManager.scale(tScale, tScale, 1.0F);
         Vonware.textManager.drawStringWithShadow(selfDamageText, 0.0F, 0.0F, -1);
         GlStateManager.popMatrix();
      }

      GlStateManager.enableDepth();
      GlStateManager.disableBlend();
      GlStateManager.disablePolygonOffset();
      GlStateManager.doPolygonOffset(1.0F, 1500000.0F);
      GlStateManager.popMatrix();
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
   }

   private void drawTexturedRect(int x, int y, int width, int height) {
      Tessellator tessellator = Tessellator.getInstance();
      BufferBuilder buffer = tessellator.getBuffer();
      buffer.begin(7, DefaultVertexFormats.POSITION_TEX);
      buffer.pos((double)x, (double)(y + height), (double)0.0F).tex((double)0.0F, (double)1.0F).endVertex();
      buffer.pos((double)(x + width), (double)(y + height), (double)0.0F).tex((double)1.0F, (double)1.0F).endVertex();
      buffer.pos((double)(x + width), (double)y, (double)0.0F).tex((double)1.0F, (double)0.0F).endVertex();
      buffer.pos((double)x, (double)y, (double)0.0F).tex((double)0.0F, (double)0.0F).endVertex();
      tessellator.draw();
   }

   private Vec3d getBreakExtrapolatedPos(EntityPlayer player, int requestedTicks) {
      if (player != null && mc.world != null && requestedTicks > 0) {
         double dx = player.posX - player.prevPosX;
         double dy = player.posY - player.prevPosY;
         double dz = player.posZ - player.prevPosZ;
         double motionSq = dx * dx + dy * dy + dz * dz;
         if (motionSq >= (double)1.0F) {
            this.breakExtrapolationHistory.remove(player.getEntityId());
            return null;
         } else {
            MotionHistory history = (MotionHistory)this.breakExtrapolationHistory.computeIfAbsent(player.getEntityId(), (id) -> new MotionHistory());
            if (history.lastSampleTick != player.ticksExisted) {
               history.lastSampleTick = player.ticksExisted;
               history.samples.addLast(new Vec3d(dx, dy, dz));
               int maxSamples = Math.max(2, (Integer)this.extrapolationSamples.getValue());

               while(history.samples.size() > maxSamples) {
                  history.samples.removeFirst();
               }
            }

            if (history.samples.isEmpty()) {
               return null;
            } else {
               double weightedX = (double)0.0F;
               double weightedZ = (double)0.0F;
               double totalWeight = (double)0.0F;
               int weight = 1;

               for(Vec3d sample : history.samples) {
                  weightedX += sample.x * (double)weight;
                  weightedZ += sample.z * (double)weight;
                  totalWeight += (double)weight;
                  ++weight;
               }

               weightedX /= totalWeight;
               weightedZ /= totalWeight;
               Vec3d newest = (Vec3d)history.samples.peekLast();
               double newestSpeed = newest == null ? (double)0.0F : Math.sqrt(newest.x * newest.x + newest.z * newest.z);
               double weightedSpeed = Math.sqrt(weightedX * weightedX + weightedZ * weightedZ);
               int ticks = requestedTicks;
               if (newest != null && history.samples.size() >= 2) {
                  double olderX = (double)0.0F;
                  double olderZ = (double)0.0F;
                  int olderCount = 0;

                  for(Iterator<Vec3d> iterator = history.samples.iterator(); iterator.hasNext(); ++olderCount) {
                     Vec3d sample = (Vec3d)iterator.next();
                     if (!iterator.hasNext()) {
                        break;
                     }

                     olderX += sample.x;
                     olderZ += sample.z;
                  }

                  if (olderCount > 0) {
                     olderX /= (double)olderCount;
                     olderZ /= (double)olderCount;
                     double olderSpeed = Math.sqrt(olderX * olderX + olderZ * olderZ);
                     if (olderSpeed > 1.0E-4 && newestSpeed > 1.0E-4) {
                        double dot = (olderX * newest.x + olderZ * newest.z) / (olderSpeed * newestSpeed);
                        if (dot < (double)0.5F) {
                           weightedX = newest.x;
                           weightedZ = newest.z;
                           ticks = Math.min(requestedTicks, 1);
                        }
                     }

                     if (olderSpeed > 0.05 && newestSpeed < olderSpeed * 0.3) {
                        weightedX = newest.x;
                        weightedZ = newest.z;
                        ticks = Math.min(ticks, 1);
                     }
                  }
               }

               if (newestSpeed < 0.01 && weightedSpeed < 0.02) {
                  return new Vec3d(player.posX, player.posY, player.posZ);
               } else {
                  Vec3d predicted = new Vec3d(player.posX, player.posY, player.posZ);
                  double vertical = player.onGround ? (double)0.0F : dy;

                  for(int i = 0; i < ticks; ++i) {
                     Vec3d horizontalNext = predicted.add(new Vec3d(weightedX, (double)0.0F, weightedZ));
                     if (mc.world.rayTraceBlocks(predicted, horizontalNext, false, true, false) == null) {
                        predicted = horizontalNext;
                     }

                     if (!player.onGround || vertical > (double)0.0F) {
                        Vec3d verticalNext = predicted.add(new Vec3d((double)0.0F, vertical, (double)0.0F));
                        if (mc.world.rayTraceBlocks(predicted, verticalNext, false, true, false) == null) {
                           predicted = verticalNext;
                        } else {
                           vertical = (double)0.0F;
                        }

                        vertical = (vertical - 0.08) * 0.98;
                     }
                  }

                  return predicted;
               }
            }
         }
      } else {
         return null;
      }
   }

   private Vec3d getExtrapolatedPos(EntityPlayer player, int ticks) {
      if (player != null && mc.world != null && ticks > 0) {
         double dx = player.posX - player.prevPosX;
         double dy = player.posY - player.prevPosY;
         double dz = player.posZ - player.prevPosZ;
         double motionSq = dx * dx + dy * dy + dz * dz;
         if (motionSq >= (double)1.0F) {
            this.extrapolationHistory.remove(player.getEntityId());
            return null;
         } else {
            MotionHistory history = (MotionHistory)this.extrapolationHistory.computeIfAbsent(player.getEntityId(), (id) -> new MotionHistory());
            if (history.lastSampleTick != player.ticksExisted) {
               history.lastSampleTick = player.ticksExisted;
               history.samples.addLast(new Vec3d(dx, dy, dz));
               int maxSamples = (Integer)this.extrapolationSamples.getValue();

               while(history.samples.size() > maxSamples) {
                  history.samples.removeFirst();
               }
            }

            if (history.samples.isEmpty()) {
               return null;
            } else {
               double averageX = (double)0.0F;
               double averageY = (double)0.0F;
               double averageZ = (double)0.0F;

               for(Vec3d sample : history.samples) {
                  averageX += sample.x;
                  averageY += sample.y;
                  averageZ += sample.z;
               }

               double divisor = (double)history.samples.size();
               averageX /= divisor;
               averageY /= divisor;
               averageZ /= divisor;
               Vec3d predicted = new Vec3d(player.posX, player.posY, player.posZ);

               for(int i = 0; i < ticks; ++i) {
                  Vec3d next = predicted.add(new Vec3d(averageX, (double)0.0F, (double)0.0F));
                  if (mc.world.rayTraceBlocks(predicted, next, false, true, false) == null) {
                     predicted = next;
                  }

                  next = predicted.add(new Vec3d((double)0.0F, (double)0.0F, averageZ));
                  if (mc.world.rayTraceBlocks(predicted, next, false, true, false) == null) {
                     predicted = next;
                  }

                  next = predicted.add(new Vec3d((double)0.0F, averageY, (double)0.0F));
                  if (mc.world.rayTraceBlocks(predicted, next, false, true, false) == null) {
                     predicted = next;
                  }
               }

               return predicted;
            }
         }
      } else {
         return null;
      }
   }

   public String getDisplayInfo() {
      if (this.target != null && (this.calcPos != null || this.calcCrystal != null)) {
         String candidate = this.target.getName() + ", " + String.format(Locale.ROOT, "%.1f", this.renderDMG) + ", " + this.CRYSTALS_PER_SECOND;
         if (!candidate.equals(this.lastDisplayInfo)) {
            long now = System.currentTimeMillis();
            if (this.displayInfoLastChanged == 0L) {
               this.displayInfoLastChanged = now;
            } else if (now - this.displayInfoLastChanged >= 150L) {
               this.lastDisplayInfo = candidate;
               this.displayInfoLastChanged = 0L;
            }

            return this.lastDisplayInfo.isEmpty() ? candidate : this.lastDisplayInfo;
         } else {
            this.displayInfoLastChanged = 0L;
            return this.lastDisplayInfo;
         }
      } else {
         this.lastDisplayInfo = "";
         this.displayInfoLastChanged = 0L;
         return null;
      }
   }

   public float[] getPlaceRot(BlockPos pos) {
      if (pos == null) {
         return null;
      } else if (!(Boolean)this.rotate.getValue()) {
         return null;
      } else {
         return (Boolean)this.strictDirection.getValue() ? RotationUtil.getBlockRotations(pos, BlockUtil.getPlaceableSideCrystal(pos, (Boolean)this.strictDirection.getValue())) : RotationUtil.getBlockRotations(pos, EnumFacing.UP);
      }
   }

   private double getServerDistanceSq(BlockPos pos) {
      return pos == null ? Double.MAX_VALUE : PositionManager.INSTANCE.getDistanceSq((double)pos.getX() + (double)0.5F, (double)pos.getY() + (double)0.5F, (double)pos.getZ() + (double)0.5F);
   }

   private boolean canSeeFromServer(Vec3d vec) {
      if (vec != null && mc.world != null) {
         return mc.world.rayTraceBlocks(PositionManager.INSTANCE.getEyeVec(), vec, false, true, false) == null;
      } else {
         return false;
      }
   }

   private List<BlockPos> getPlaceCandidates() {
      BlockPos origin = new BlockPos(PositionManager.INSTANCE.getVec());
      float range = (Float)this.placeRange.getValue();
      if (this.cachedCandidateOrigin == null || !this.cachedCandidateOrigin.equals(origin) || Float.compare(this.cachedCandidateRange, range) != 0 || this.cachedPlaceCandidates.isEmpty()) {
         this.cachedPlaceCandidates.clear();
         this.cachedPlaceCandidates.addAll(BlockUtil.getSphere((double)(range + 1.0F), origin, true, false));
         this.cachedCandidateOrigin = origin;
         this.cachedCandidateRange = range;
      }

      return this.cachedPlaceCandidates;
   }

   private void invalidatePlacementCache() {
      this.cachedPlaceCandidates.clear();
      this.cachedCandidateOrigin = null;
      this.cachedCandidateRange = -1.0F;
      this.currentPlaceSide = null;
   }

   public static enum Animation {
      None,
      Fade;
   }

   public static enum RenderMode {
      None,
      Outline,
      Box,
      Both,
      Debug;
   }

   public static enum TargetSorting {
      Damage,
      Range;
   }

   public static enum Handling {
      HitIgnore,
      Ignore,
      None,
      RealNone;
   }

   public static enum SwitchMode {
      None,
      Normal,
      Silent,
      SilentBypass;
   }

   public static enum AntiWeakness {
      None,
      Normal,
      Silent;
   }

   public static enum SetDeadMode {
      None,
      Sound,
      Ghost;
   }

   public static enum SwapWait {
      None,
      Semi,
      Full;
   }

   public static enum Mining {
      None,
      Ignore,
      StrictIgnore;
   }

   public static enum Smoothing {
      None,
      Semi;
   }

   public static enum Sequence {
      None,
      Soft,
      Full;
   }

   public static enum EventSetting {
      Post,
      Pre;
   }

   public static enum TicksMode {
      PB,
      OB;
   }

   private static final class MotionHistory {
      private final Deque<Vec3d> samples;
      private int lastSampleTick;

      private MotionHistory() {
         this.samples = new ArrayDeque();
         this.lastSampleTick = Integer.MIN_VALUE;
      }
   }

   private static enum CrystalActionState {
      SEEN,
      ATTACK_SENT,
      CONFIRMED_DEAD;
   }

   private static final class RotationSample {
      final float yaw;
      final float pitch;
      final long time;

      RotationSample(float yaw, float pitch, long time) {
         this.yaw = yaw;
         this.pitch = pitch;
         this.time = time;
      }
   }

   private static final class BreakCandidate {
      final Entity crystal;
      final double targetDamage;
      final float selfDamage;
      final float[] rotations;
      final float rotationDelta;
      final boolean blocksPlace;
      final double score;

      BreakCandidate(Entity crystal, double targetDamage, float selfDamage, float[] rotations, float rotationDelta, boolean blocksPlace, double score) {
         this.crystal = crystal;
         this.targetDamage = targetDamage;
         this.selfDamage = selfDamage;
         this.rotations = rotations;
         this.rotationDelta = rotationDelta;
         this.blocksPlace = blocksPlace;
         this.score = score;
      }
   }
}
