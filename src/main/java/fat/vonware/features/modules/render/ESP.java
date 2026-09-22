package fat.vonware.features.modules.render;

import fat.vonware.Global;
import fat.vonware.Vonware;
import fat.vonware.event.events.PacketEvent;
import fat.vonware.event.events.Render3DEvent;
import fat.vonware.features.gui.components.items.buttons.render.ColorPickerConfig;
import fat.vonware.features.gui.newgui.newcomponents.NewComponent;
import fat.vonware.features.gui.newgui.newcomponents.preview.IPreviewable;
import fat.vonware.features.modules.Module;
import fat.vonware.features.modules.client.ClickGui;
import fat.vonware.features.modules.client.Colors;
import fat.vonware.features.modules.client.Media;
import fat.vonware.features.setting.ColorSetting;
import fat.vonware.features.setting.Setting;
import fat.vonware.util.EntityUtil;
import fat.vonware.util.Interpolation;
import fat.vonware.util.Animations.render.ColorUtil;
import fat.vonware.util.Animations.render.RenderMethods;
import fat.vonware.util.Animations.render.RenderUtil;
import fat.vonware.util.ESP.RenderPosition;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.GlStateManager.DestFactor;
import net.minecraft.client.renderer.GlStateManager.SourceFactor;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityEnderPearl;
import net.minecraft.entity.item.EntityExpBottle;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Enchantments;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.SPacketBlockBreakAnim;
import net.minecraft.network.play.server.SPacketSoundEffect;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityBrewingStand;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityDispenser;
import net.minecraft.tileentity.TileEntityEnderChest;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.tileentity.TileEntityHopper;
import net.minecraft.tileentity.TileEntityShulkerBox;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

public class ESP extends Module implements IPreviewable {
   public final Setting<RenderMode> mode;
   public Setting<Boolean> items;
   public Setting<Boolean> xpBottles;
   public Setting<Boolean> pearls;
   private final Setting<Integer> boxAlpha;
   private final Setting<Float> lineWidth;
   public Setting<Boolean> chorus;
   public Setting<Boolean> pearlNametags;
   public Setting<Float> pearlSize;
   public Setting<Boolean> itemNametags;
   public Setting<Boolean> itemBackground;
   public Setting<Boolean> smartScale;
   public Setting<Float> factor;
   public Setting<Float> itemSize;
   public Setting<Boolean> burrowESP;
   public Setting<BurrowRenderMode> burrowMode;
   public Setting<Integer> burrowRange;
   public Setting<Boolean> burrowSelf;
   public Setting<Integer> burrowAlpha;
   public Setting<Integer> burrowOutlineAlpha;
   public Setting<Boolean> clipESP;
   public Setting<Integer> clipRange;
   public Setting<Integer> clipAlpha;
   public Setting<Float> clipLineW;
   private final ColorSetting clipColor;
   public Setting<ClipRenderMode> clipRenderMode;
   public final Setting<Boolean> storageESP;
   public final Setting<Boolean> chests;
   public final Setting<Boolean> trappedChests;
   public final Setting<Boolean> enderChests;
   public final Setting<Boolean> shulkers;
   public final Setting<Boolean> storageMisc;
   public final Setting<StorageRenderMode> storageMode;
   public final Setting<Integer> storageRange;
   public final Setting<Integer> storageAlpha;
   public final Setting<Float> storageLineW;
   public final Setting<Boolean> cityESP;
   public final Setting<Float> cityEnemyRange;
   public final Setting<StorageRenderMode> cityRenderMode;
   public final Setting<Integer> cityAlpha;
   public final Setting<Float> cityLineW;
   private final ColorSetting cityColor;
   public final Setting<Boolean> breakESP;
   public final Setting<Boolean> breakShowProgress;
   public final Setting<RenderMode> breakRenderMode;
   public final Setting<Integer> breakAlpha;
   public final Setting<Float> breakLineW;
   private final ColorSetting breakColor;
   private final ColorSetting breakNameColor;
   private static final int BREAK_RANGE = 12;
   private static final int BREAK_PLAYER_RANGE = 12;
   private final ColorSetting customColor;
   public Setting<Integer> burrowColor;
   private final ColorSetting chestColor;
   private final ColorSetting enderColor;
   private final ColorSetting shulkerColor;
   private final ColorSetting miscColor;
   private final CopyOnWriteArrayList<BlockPos> cityPosList;
   protected final List<RenderPosition> positions;
   public static CopyOnWriteArrayList<ChorusPos> chorusPosList = new CopyOnWriteArrayList();
   private final Map<EntityEnderPearl, String> pearlThrowers;
   private final List<BlockPos> burrowPosList;
   private final CopyOnWriteArrayList<StorageEntry> storageList;
   private final CopyOnWriteArrayList<ClipEntry> clipList;
   private final Map<EntityPlayer, BreakPos> breakList;
   private final RenderUtil renderUtil;

   public ESP() {
      super("ESP", "Draws ESP on stuff", Module.Category.RENDER, true, false, false);
      this.mode = new Setting<RenderMode>("Mode", ESP.RenderMode.Both);
      this.items = new Setting<Boolean>("Items", true, (v) -> this.mode.getValue() != ESP.RenderMode.None);
      this.xpBottles = new Setting<Boolean>("XP-Bottles", true, (v) -> this.mode.getValue() != ESP.RenderMode.None);
      this.pearls = new Setting<Boolean>("Pearls", false, (v) -> this.mode.getValue() != ESP.RenderMode.None);
      this.boxAlpha = new Setting<Integer>("Alpha", 85, 0, 255, (v) -> this.mode.getValue() != ESP.RenderMode.None);
      this.lineWidth = new Setting<Float>("LineWidth", 1.0F, 0.1F, 4.0F, (v) -> this.mode.getValue() != ESP.RenderMode.None);
      this.chorus = new Setting<Boolean>("ChorusESP", true);
      this.pearlNametags = new Setting<Boolean>("PearlNametags", false);
      this.pearlSize = new Setting<Float>("Pearl Size", 2.5F, 0.1F, 3.0F, (v) -> (Boolean)this.pearlNametags.getValue());
      this.itemNametags = new Setting<Boolean>("ItemNametags", false);
      this.itemBackground = new Setting<Boolean>("Background", true, (v) -> (Boolean)this.itemNametags.getValue());
      this.smartScale = new Setting<Boolean>("SmartScale", true, (v) -> (Boolean)this.itemNametags.getValue());
      this.factor = new Setting<Float>("Factor", 1.0F, 0.1F, 1.0F, (v) -> (Boolean)this.itemNametags.getValue());
      this.itemSize = new Setting<Float>("Item Size", 3.0F, 0.1F, 4.0F, (v) -> (Boolean)this.itemNametags.getValue());
      this.burrowESP = new Setting<Boolean>("BurrowESP", false);
      this.burrowMode = new Setting<BurrowRenderMode>("RenderMode", ESP.BurrowRenderMode.Box, (v) -> (Boolean)this.burrowESP.getValue());
      this.burrowRange = new Setting<Integer>("Range", 20, 5, 50, (v) -> (Boolean)this.burrowESP.getValue());
      this.burrowSelf = new Setting<Boolean>("Self", true, (v) -> (Boolean)this.burrowESP.getValue());
      this.burrowAlpha = new Setting<Integer>("B-Alpha", 30, 0, 255, (v) -> (Boolean)this.burrowESP.getValue() && this.burrowMode.getValue() == ESP.BurrowRenderMode.Box);
      this.burrowOutlineAlpha = new Setting<Integer>("B-LineAlpha", 120, 0, 255, (v) -> (Boolean)this.burrowESP.getValue());
      this.clipESP = new Setting<Boolean>("ClipESP", false);
      this.clipRange = new Setting<Integer>("ClipRange", 20, 5, 50, (v) -> (Boolean)this.clipESP.getValue());
      this.clipAlpha = new Setting<Integer>("Clip-Alpha", 60, 0, 255, (v) -> (Boolean)this.clipESP.getValue());
      this.clipLineW = new Setting<Float>("Clip-LineWidth", 1.5F, 0.1F, 4.0F, (v) -> (Boolean)this.clipESP.getValue());
      this.clipColor = new ColorSetting("ClipColor", 16729156, false, (v) -> (Boolean)this.clipESP.getValue());
      this.clipRenderMode = new Setting<ClipRenderMode>("Clip-Mode", ESP.ClipRenderMode.Face, (v) -> (Boolean)this.clipESP.getValue());
      this.storageESP = new Setting<Boolean>("StorageESP", false);
      this.chests = new Setting<Boolean>("Chests", true, (v) -> (Boolean)this.storageESP.getValue());
      this.trappedChests = new Setting<Boolean>("TrappedChests", true, (v) -> (Boolean)this.storageESP.getValue());
      this.enderChests = new Setting<Boolean>("EnderChests", true, (v) -> (Boolean)this.storageESP.getValue());
      this.shulkers = new Setting<Boolean>("Shulkers", true, (v) -> (Boolean)this.storageESP.getValue());
      this.storageMisc = new Setting<Boolean>("Misc", false, (v) -> (Boolean)this.storageESP.getValue());
      this.storageMode = new Setting<StorageRenderMode>("StorageRender", ESP.StorageRenderMode.Both, (v) -> (Boolean)this.storageESP.getValue());
      this.storageRange = new Setting<Integer>("Range", 50, 10, 100, (v) -> (Boolean)this.storageESP.getValue());
      this.storageAlpha = new Setting<Integer>("S-Alpha", 60, 0, 255, (v) -> (Boolean)this.storageESP.getValue());
      this.storageLineW = new Setting<Float>("S-LineWidth", 1.5F, 0.1F, 4.0F, (v) -> (Boolean)this.storageESP.getValue());
      this.cityESP = new Setting<Boolean>("CityESP", false);
      this.cityEnemyRange = new Setting<Float>("CityRange", 7.5F, 1.0F, 12.0F, (v) -> (Boolean)this.cityESP.getValue());
      this.cityRenderMode = new Setting<StorageRenderMode>("CityRender", ESP.StorageRenderMode.Both, (v) -> (Boolean)this.cityESP.getValue());
      this.cityAlpha = new Setting<Integer>("C-Alpha", 60, 0, 255, (v) -> (Boolean)this.cityESP.getValue());
      this.cityLineW = new Setting<Float>("C-LineWidth", 1.5F, 0.1F, 4.0F, (v) -> (Boolean)this.cityESP.getValue());
      this.cityColor = new ColorSetting("CityColor", 16729156, false, (v) -> (Boolean)this.cityESP.getValue());
      this.breakESP = new Setting<Boolean>("BreakESP", false);
      this.breakShowProgress = new Setting<Boolean>("Brk-ShowProgress", true, (v) -> (Boolean)this.breakESP.getValue());
      this.breakRenderMode = new Setting<RenderMode>("Brk-Mode", ESP.RenderMode.Both, (v) -> (Boolean)this.breakESP.getValue());
      this.breakAlpha = new Setting<Integer>("Brk-Alpha", 60, 0, 255, (v) -> (Boolean)this.breakESP.getValue());
      this.breakLineW = new Setting<Float>("Brk-LineWidth", 1.5F, 0.1F, 4.0F, (v) -> (Boolean)this.breakESP.getValue());
      this.breakColor = new ColorSetting("BreakColor", 65280, false, (v) -> (Boolean)this.breakESP.getValue());
      this.breakNameColor = new ColorSetting("BreakNameColor", 16777215, false, (v) -> (Boolean)this.breakESP.getValue());
      this.customColor = new ColorSetting("ESPColor", 16777215, false);
      this.burrowColor = new ColorSetting("BurrowColor", 9533665, false, (v) -> (Boolean)this.burrowESP.getValue());
      this.chestColor = new ColorSetting("ChestColor", 16371065, false, (v) -> (Boolean)this.storageESP.getValue() && ((Boolean)this.chests.getValue() || (Boolean)this.trappedChests.getValue()));
      this.enderColor = new ColorSetting("EnderColor", 8871935, false, (v) -> (Boolean)this.storageESP.getValue() && (Boolean)this.enderChests.getValue());
      this.shulkerColor = new ColorSetting("ShulkerColor", 16733695, false, (v) -> (Boolean)this.storageESP.getValue() && (Boolean)this.shulkers.getValue());
      this.miscColor = new ColorSetting("MiscColor", 5636095, false, (v) -> (Boolean)this.storageESP.getValue() && (Boolean)this.storageMisc.getValue());
      this.cityPosList = new CopyOnWriteArrayList();
      this.pearlThrowers = new HashMap();
      this.burrowPosList = new ArrayList();
      this.storageList = new CopyOnWriteArrayList();
      this.clipList = new CopyOnWriteArrayList();
      this.breakList = new HashMap();
      this.renderUtil = new RenderUtil();
      this.register(this.mode);
      this.register(this.items);
      this.register(this.xpBottles);
      this.register(this.pearls);
      this.register(this.chorus);
      this.register(this.customColor);
      this.register(this.boxAlpha);
      this.register(this.lineWidth);
      this.register(this.pearlNametags);
      this.register(this.pearlSize);
      this.register(this.itemNametags);
      this.register(this.itemBackground);
      this.register(this.smartScale);
      this.register(this.factor);
      this.register(this.itemSize);
      this.register(this.burrowESP);
      this.register(this.burrowMode);
      this.register(this.burrowRange);
      this.register(this.burrowSelf);
      this.register(this.burrowColor);
      this.register(this.burrowAlpha);
      this.register(this.burrowOutlineAlpha);
      this.register(this.clipESP);
      this.register(this.clipRange);
      this.register(this.clipAlpha);
      this.register(this.clipLineW);
      this.register(this.clipColor);
      this.register(this.clipRenderMode);
      this.register(this.cityESP);
      this.register(this.cityEnemyRange);
      this.register(this.cityRenderMode);
      this.register(this.cityAlpha);
      this.register(this.cityLineW);
      this.register(this.cityColor);
      this.register(this.storageESP);
      this.register(this.chests);
      this.register(this.trappedChests);
      this.register(this.enderChests);
      this.register(this.shulkers);
      this.register(this.storageMisc);
      this.register(this.storageMode);
      this.register(this.storageRange);
      this.register(this.storageAlpha);
      this.register(this.storageLineW);
      this.register(this.chestColor);
      this.register(this.enderColor);
      this.register(this.shulkerColor);
      this.register(this.miscColor);
      this.register(this.breakESP);
      this.register(this.breakShowProgress);
      this.register(this.breakRenderMode);
      this.register(this.breakAlpha);
      this.register(this.breakLineW);
      this.register(this.breakColor);
      this.register(this.breakNameColor);
      this.positions = new ArrayList();
   }

   public IPreviewable.PreviewType getPreviewType() {
      return IPreviewable.PreviewType.ESP;
   }

   public void onDisable() {
      chorusPosList.clear();
      this.pearlThrowers.clear();
      this.burrowPosList.clear();
      this.storageList.clear();
      this.cityPosList.clear();
      this.clipList.clear();
      this.breakList.clear();
   }

   public void onTick() {
      if (mc.world != null && mc.player != null) {
         if ((Boolean)this.burrowESP.getValue()) {
            this.burrowPosList.clear();

            for(EntityPlayer player : mc.world.playerEntities) {
               BlockPos blockPos = new BlockPos(Math.floor(player.posX), Math.floor(player.posY + 0.2), Math.floor(player.posZ));
               if ((mc.world.getBlockState(blockPos).getBlock() == Blocks.ENDER_CHEST || mc.world.getBlockState(blockPos).getBlock() == Blocks.OBSIDIAN) && blockPos.distanceSq(mc.player.posX, mc.player.posY, mc.player.posZ) <= (double)(Integer)this.burrowRange.getValue() && (!(blockPos.distanceSq(mc.player.posX, mc.player.posY, mc.player.posZ) <= (double)1.5F) || (Boolean)this.burrowSelf.getValue())) {
                  this.burrowPosList.add(blockPos);
               }
            }
         }

         if ((Boolean)this.storageESP.getValue()) {
            this.storageList.clear();
            int r = (Integer)this.storageRange.getValue();
            BlockPos playerPos = mc.player.getPosition();

            for(TileEntity te : mc.world.loadedTileEntityList) {
               BlockPos pos = te.getPos();
               if (Math.abs(pos.getX() - playerPos.getX()) <= r && Math.abs(pos.getY() - playerPos.getY()) <= r && Math.abs(pos.getZ() - playerPos.getZ()) <= r) {
                  Block block = mc.world.getBlockState(pos).getBlock();
                  StorageType type = this.getStorageType(te, block);
                  if (type != null) {
                     this.storageList.add(new StorageEntry(pos, type));
                  }
               }
            }
         }

         if ((Boolean)this.clipESP.getValue()) {
            this.clipList.clear();

            for(EntityPlayer player : mc.world.playerEntities) {
               if (player != mc.player && !(mc.player.getDistance(player) > (float)(Integer)this.clipRange.getValue()) && EntityUtil.isPartialPhasing(player)) {
                  double xOff = player.posX - Math.floor(player.posX);
                  double zOff = player.posZ - Math.floor(player.posZ);
                  BlockPos playerBlock = new BlockPos(Math.floor(player.posX), Math.floor(player.posY), Math.floor(player.posZ));

                  for(EnumFacing facing : EnumFacing.HORIZONTALS) {
                     boolean nearEdge = false;
                     switch (facing) {
                        case WEST:
                           nearEdge = xOff >= 0.235 && xOff <= 0.245;
                           break;
                        case EAST:
                           nearEdge = xOff >= 0.755 && xOff <= 0.765;
                           break;
                        case NORTH:
                           nearEdge = zOff >= 0.235 && zOff <= 0.245;
                           break;
                        case SOUTH:
                           nearEdge = zOff >= 0.755 && zOff <= 0.765;
                     }

                     if (nearEdge) {
                        BlockPos adjacent = playerBlock.offset(facing);
                        IBlockState state = mc.world.getBlockState(adjacent);
                        if (state.isFullBlock()) {
                           this.clipList.add(new ClipEntry(player, adjacent, facing.getOpposite()));
                        }
                     }
                  }
               }
            }
         }

         if ((Boolean)this.cityESP.getValue()) {
            this.cityPosList.clear();

            for(EntityPlayer player : mc.world.playerEntities) {
               if (player != mc.player && !(mc.player.getDistance(player) > (Float)this.cityEnemyRange.getValue())) {
                  BlockPos targetPos = new BlockPos((int)Math.floor(player.posX), (int)Math.floor(player.posY), (int)Math.floor(player.posZ));
                  if (this.isCityHole(targetPos)) {
                     for(EnumFacing facing : EnumFacing.HORIZONTALS) {
                        BlockPos side = targetPos.offset(facing);
                        if (mc.world.getBlockState(side).getBlock() == Blocks.OBSIDIAN || mc.world.getBlockState(side).getBlock() == Blocks.ENDER_CHEST) {
                           BlockPos exit = side.offset(facing);
                           BlockPos exitDown = exit.down();
                           boolean placeableFloor = mc.world.getBlockState(exitDown).getBlock() == Blocks.OBSIDIAN || mc.world.getBlockState(exitDown).getBlock() == Blocks.BEDROCK;
                           boolean exitClear = mc.world.getBlockState(exit).getBlock() == Blocks.AIR && mc.world.getBlockState(exit.up()).getBlock() == Blocks.AIR;
                           if (placeableFloor && exitClear && !this.cityPosList.contains(side)) {
                              this.cityPosList.add(side);
                           }
                        }
                     }

                     for(EnumFacing facing : EnumFacing.HORIZONTALS) {
                        BlockPos side = targetPos.offset(facing);
                        if (mc.world.getBlockState(side).getBlock() == Blocks.OBSIDIAN && mc.world.getBlockState(side.up()).getBlock() == Blocks.AIR && !this.cityPosList.contains(side)) {
                           this.cityPosList.add(side);
                        }
                     }
                  }
               }
            }
         }

         if ((Boolean)this.breakESP.getValue() && !this.breakList.isEmpty()) {
            Iterator<Map.Entry<EntityPlayer, BreakPos>> it = this.breakList.entrySet().iterator();

            while(it.hasNext()) {
               Map.Entry<EntityPlayer, BreakPos> entry = (Map.Entry)it.next();
               EntityPlayer player = (EntityPlayer)entry.getKey();
               BreakPos bp = (BreakPos)entry.getValue();
               if (!player.isDead && mc.world.playerEntities.contains(player) && bp.pos != null) {
                  IBlockState state = mc.world.getBlockState(bp.pos);
                  if (state.getBlock() != Blocks.AIR && !(state.getBlockHardness(mc.world, bp.pos) < 0.0F)) {
                     bp.time = this.calcBreakTime(bp.pos);
                     if (player.getDistanceSq(bp.pos) > (double)144.0F) {
                        it.remove();
                     }
                  } else {
                     it.remove();
                  }
               } else {
                  it.remove();
               }
            }
         }

      }
   }

   @SubscribeEvent
   public void onPacketReceive(PacketEvent.Receive event) {
      if ((Boolean)this.chorus.getValue() && event.getPacket() instanceof SPacketSoundEffect) {
         SPacketSoundEffect packet = (SPacketSoundEffect)event.getPacket();
         if (packet.getSound() == SoundEvents.ITEM_CHORUS_FRUIT_TELEPORT) {
            double soundX = packet.getX();
            double soundY = packet.getY();
            double soundZ = packet.getZ();
            boolean isNearPlayer = false;

            for(EntityPlayer player : mc.world.playerEntities) {
               double distance = Math.sqrt(Math.pow(player.posX - soundX, (double)2.0F) + Math.pow(player.posY - soundY, (double)2.0F) + Math.pow(player.posZ - soundZ, (double)2.0F));
               if (distance < (double)1.0F) {
                  isNearPlayer = true;
                  break;
               }
            }

            if (!isNearPlayer) {
               chorusPosList.add(new ChorusPos(System.currentTimeMillis(), soundX, soundY, soundZ));
            }
         }
      }

      if ((Boolean)this.breakESP.getValue() && event.getPacket() instanceof SPacketBlockBreakAnim) {
         if (mc.world == null || mc.player == null) {
            return;
         }

         SPacketBlockBreakAnim packet = (SPacketBlockBreakAnim)event.getPacket();
         BlockPos blockPos = packet.getPosition();
         if (mc.player.getDistanceSq(blockPos) <= (double)144.0F) {
            Entity breaker = mc.world.getEntityByID(packet.getBreakerId());
            if (breaker instanceof EntityPlayer && breaker != mc.player) {
               EntityPlayer player = (EntityPlayer)breaker;
               long time = this.calcBreakTime(blockPos);
               BreakPos existing = (BreakPos)this.breakList.get(player);
               if (existing != null) {
                  if (existing.pos == null || !existing.pos.equals(blockPos)) {
                     existing.update(blockPos, time);
                  }
               } else {
                  this.breakList.put(player, new BreakPos(blockPos, time));
               }
            }
         }
      }

   }

   @SubscribeEvent
   public void onEntityJoinWorld(EntityJoinWorldEvent event) {
      if ((Boolean)this.pearlNametags.getValue()) {
         if (mc.world != null && mc.player != null) {
            if (event.getEntity() instanceof EntityEnderPearl) {
               EntityEnderPearl pearl = (EntityEnderPearl)event.getEntity();
               EntityPlayer thrower = this.findPearlThrower(pearl);
               if (thrower != null) {
                  this.pearlThrowers.put(pearl, thrower.getName());
               }
            }

         }
      }
   }

   @SubscribeEvent
   public void onRender3D(Render3DEvent event) {
      Entity renderEntity = RenderMethods.getEntity();
      Frustum frustum = Interpolation.createFrustum(renderEntity);
      List<Entity> entityList = mc.world.loadedEntityList;
      entityList.sort(Comparator.comparing((entityx) -> mc.player.getDistance(entityx)));
      RenderMethods.startRender();

      for(Entity entity2 : entityList) {
         AxisAlignedBB bb = entity2.getEntityBoundingBox();
         Vec3d vec = Interpolation.interpolateEntity(entity2);
         if (frustum.isBoundingBoxInFrustum(bb)) {
            if (this.isValid(entity2) && this.mode.getValue() != ESP.RenderMode.None) {
               Color color = this.getColor();
               this.render((new AxisAlignedBB((double)0.0F, (double)0.0F, (double)0.0F, (double)entity2.width, (double)entity2.height, (double)entity2.width)).offset(vec.x - (double)(entity2.width / 2.0F), vec.y + 0.05, vec.z - (double)(entity2.width / 2.0F)).grow(0.05), color);
            }

            for(RenderPosition pos : this.positions) {
               AxisAlignedBB box = Interpolation.interpolatePos(pos.getPos());
               switch ((RenderMode)this.mode.getValue()) {
                  case Box:
                     RenderMethods.drawBox(box, this.getBoxColor());
                     break;
                  case Outline:
                     RenderMethods.drawOutline(box, (Float)this.lineWidth.getValue(), this.getColor());
                     break;
                  case Both:
                     RenderMethods.drawBox(box, this.getBoxColor());
                     RenderMethods.drawOutline(box, (Float)this.lineWidth.getValue(), this.getColor());
               }
            }
         }
      }

      RenderMethods.endRender();
      if ((Boolean)this.itemNametags.getValue() && mc.player != null) {
         GlStateManager.enableBlend();
         GlStateManager.tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ZERO);
         GlStateManager.enableTexture2D();
         GlStateManager.disableDepth();
         float partialTicks = mc.getRenderPartialTicks();

         for(Entity entity : entityList) {
            if (entity instanceof EntityItem && frustum.isBoundingBoxInFrustum(entity.getRenderBoundingBox())) {
               double x = this.interpolate(entity.lastTickPosX, entity.posX, partialTicks) - mc.renderManager.renderPosX;
               double y = this.interpolate(entity.lastTickPosY, entity.posY, partialTicks) - mc.renderManager.renderPosY;
               double z = this.interpolate(entity.lastTickPosZ, entity.posZ, partialTicks) - mc.renderManager.renderPosZ;
               this.renderItemNametag((EntityItem)entity, x, y, z, partialTicks);
            }
         }

         GlStateManager.enableDepth();
         GlStateManager.disableBlend();
      }

      if ((Boolean)this.pearlNametags.getValue() && mc.player != null) {
         for(EntityEnderPearl pearl : mc.world.getEntitiesWithinAABB(EntityEnderPearl.class, new AxisAlignedBB(mc.player.posX - (double)50.0F, mc.player.posY - (double)50.0F, mc.player.posZ - (double)50.0F, mc.player.posX + (double)50.0F, mc.player.posY + (double)50.0F, mc.player.posZ + (double)50.0F))) {
            if (pearl != null && !pearl.isDead) {
               String throwerName = (String)this.pearlThrowers.get(pearl);
               if (throwerName == null) {
                  EntityPlayer thrower = this.findPearlThrower(pearl);
                  if (thrower != null) {
                     throwerName = thrower.getName();
                     this.pearlThrowers.put(pearl, throwerName);
                  }
               }

               if (throwerName != null) {
                  this.renderPearlNametag(pearl, throwerName, event.getPartialTicks());
               }
            }
         }
      }

      if ((Boolean)this.chorus.getValue() && !chorusPosList.isEmpty()) {
         for(ChorusPos pos : chorusPosList) {
            long elapsed = System.currentTimeMillis() - pos.startTime;
            if (elapsed > 10000L) {
               chorusPosList.remove(pos);
            } else if (!(mc.player.getDistance(pos.x, pos.y, pos.z) > (double)70.0F)) {
               this.renderChorusText(pos.x, pos.y, pos.z, "*Player Teleports*");
            }
         }
      }

      if ((Boolean)this.burrowESP.getValue() && !this.burrowPosList.isEmpty()) {
         Color baseColor = this.getBurrowColor();

         for(BlockPos blockPos : this.burrowPosList) {
            if (this.burrowMode.getValue() == ESP.BurrowRenderMode.Text) {
               Color textColor = new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), 255);
               this.renderUtil.drawText(blockPos, "BURROW", textColor);
            } else {
               Color boxColor = new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), (Integer)this.burrowOutlineAlpha.getValue());
               RenderUtil.drawBoxESP(blockPos, boxColor, 1.5F, true, true, (Integer)this.burrowAlpha.getValue());
            }
         }
      }

      if ((Boolean)this.clipESP.getValue() && !this.clipList.isEmpty()) {
         GlStateManager.pushMatrix();
         GlStateManager.enableBlend();
         GlStateManager.tryBlendFuncSeparate(770, 771, 0, 1);
         GlStateManager.disableTexture2D();
         GlStateManager.disableDepth();
         GlStateManager.depthMask(false);
         GlStateManager.disableCull();
         GL11.glEnable(2848);
         GL11.glHint(3154, 4354);
         GL11.glLineWidth((Float)this.clipLineW.getValue());
         Color col = this.getClipColor();
         float r = (float)col.getRed() / 255.0F;
         float g = (float)col.getGreen() / 255.0F;
         float b = (float)col.getBlue() / 255.0F;
         float fillA = (float)(Integer)this.clipAlpha.getValue() / 255.0F;
         float lineA = Math.min(1.0F, fillA * 2.0F);

         for(ClipEntry entry : this.clipList) {
            EntityPlayer player = entry.player;
            if (player != null && !player.isDead) {
               float partialTicks = mc.getRenderPartialTicks();
               double var10000 = player.lastTickPosX + (player.posX - player.lastTickPosX) * (double)partialTicks;
               double py = player.lastTickPosY + (player.posY - player.lastTickPosY) * (double)partialTicks;
               var10000 = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * (double)partialTicks;
               BlockPos adjBlock = entry.adjacentBlock;
               IBlockState adjState = mc.world.getBlockState(adjBlock);
               AxisAlignedBB adjBB = adjState.getBoundingBox(mc.world, adjBlock);
               if (this.clipRenderMode.getValue() == ESP.ClipRenderMode.Block) {
                  double bx = (double)adjBlock.getX() - mc.getRenderManager().viewerPosX + adjBB.minX;
                  double by = (double)adjBlock.getY() - mc.getRenderManager().viewerPosY + adjBB.minY;
                  double bz = (double)adjBlock.getZ() - mc.getRenderManager().viewerPosZ + adjBB.minZ;
                  double bx2 = (double)adjBlock.getX() - mc.getRenderManager().viewerPosX + adjBB.maxX;
                  double by2 = (double)adjBlock.getY() - mc.getRenderManager().viewerPosY + adjBB.maxY;
                  double bz2 = (double)adjBlock.getZ() - mc.getRenderManager().viewerPosZ + adjBB.maxZ;
                  AxisAlignedBB bb = new AxisAlignedBB(bx, by, bz, bx2, by2, bz2);
                  RenderGlobal.renderFilledBox(bb, r, g, b, fillA);
                  RenderUtil.drawBlockOutline(bb, new Color(col.getRed(), col.getGreen(), col.getBlue(), (int)(lineA * 255.0F)));
               } else {
                  double height = adjBB.maxY;
                  double bx = (double)adjBlock.getX() - mc.getRenderManager().viewerPosX;
                  double by = py - mc.getRenderManager().viewerPosY;
                  double bz = (double)adjBlock.getZ() - mc.getRenderManager().viewerPosZ;
                  double z1;
                  double x0;
                  double z0;
                  double x1;
                  switch (entry.faceDir) {
                     case WEST:
                        x0 = bx;
                        z0 = bz;
                        x1 = bx;
                        z1 = bz + (double)1.0F;
                        break;
                     case EAST:
                        double faceX = bx + (double)1.0F;
                        x0 = faceX;
                        z0 = bz;
                        x1 = faceX;
                        z1 = bz + (double)1.0F;
                        break;
                     case NORTH:
                     default:
                        x0 = bx;
                        z0 = bz;
                        x1 = bx + (double)1.0F;
                        z1 = bz;
                        break;
                     case SOUTH:
                        double faceZ = bz + (double)1.0F;
                        x0 = bx;
                        z0 = faceZ;
                        x1 = bx + (double)1.0F;
                        z1 = faceZ;
                  }

                  double yTop = by + height;
                  Tessellator tess = Tessellator.getInstance();
                  BufferBuilder buf = tess.getBuffer();
                  buf.begin(7, DefaultVertexFormats.POSITION_COLOR);
                  buf.pos(x0, yTop, z0).color(r, g, b, fillA).endVertex();
                  buf.pos(x1, yTop, z1).color(r, g, b, fillA).endVertex();
                  buf.pos(x1, by, z1).color(r, g, b, fillA).endVertex();
                  buf.pos(x0, by, z0).color(r, g, b, fillA).endVertex();
                  tess.draw();
                  buf.begin(2, DefaultVertexFormats.POSITION_COLOR);
                  buf.pos(x0, yTop, z0).color(r, g, b, lineA).endVertex();
                  buf.pos(x1, yTop, z1).color(r, g, b, lineA).endVertex();
                  buf.pos(x1, by, z1).color(r, g, b, lineA).endVertex();
                  buf.pos(x0, by, z0).color(r, g, b, lineA).endVertex();
                  tess.draw();
               }
            }
         }

         GL11.glDisable(2848);
         GlStateManager.depthMask(true);
         GlStateManager.enableDepth();
         GlStateManager.enableCull();
         GlStateManager.enableTexture2D();
         GlStateManager.disableBlend();
         GlStateManager.popMatrix();
      }

      if ((Boolean)this.storageESP.getValue() && !this.storageList.isEmpty()) {
         GlStateManager.pushMatrix();
         GlStateManager.enableBlend();
         GlStateManager.tryBlendFuncSeparate(770, 771, 0, 1);
         GlStateManager.disableTexture2D();
         GlStateManager.disableDepth();
         GlStateManager.depthMask(false);
         GlStateManager.disableCull();
         GL11.glEnable(2848);
         GL11.glHint(3154, 4354);
         GL11.glLineWidth((Float)this.storageLineW.getValue());

         for(StorageEntry entry : this.storageList) {
            Color color = this.getStorageColor(entry.type);
            double x = (double)entry.pos.getX() - mc.getRenderManager().viewerPosX;
            double y = (double)entry.pos.getY() - mc.getRenderManager().viewerPosY;
            double z = (double)entry.pos.getZ() - mc.getRenderManager().viewerPosZ;
            AxisAlignedBB bb = new AxisAlignedBB(x, y, z, x + (double)1.0F, y + (double)1.0F, z + (double)1.0F);
            boolean doBox = this.storageMode.getValue() == ESP.StorageRenderMode.Box || this.storageMode.getValue() == ESP.StorageRenderMode.Both;
            boolean doOutline = this.storageMode.getValue() == ESP.StorageRenderMode.Outline || this.storageMode.getValue() == ESP.StorageRenderMode.Both;
            if (doBox) {
               RenderGlobal.renderFilledBox(bb, (float)color.getRed() / 255.0F, (float)color.getGreen() / 255.0F, (float)color.getBlue() / 255.0F, (float)(Integer)this.storageAlpha.getValue() / 255.0F);
            }

            if (doOutline) {
               RenderUtil.drawBlockOutline(bb, color);
            }
         }

         GL11.glDisable(2848);
         GlStateManager.depthMask(true);
         GlStateManager.enableDepth();
         GlStateManager.enableCull();
         GlStateManager.enableTexture2D();
         GlStateManager.disableBlend();
         GlStateManager.popMatrix();
      }

      if ((Boolean)this.cityESP.getValue() && !this.cityPosList.isEmpty()) {
         GlStateManager.pushMatrix();
         GlStateManager.enableBlend();
         GlStateManager.tryBlendFuncSeparate(770, 771, 0, 1);
         GlStateManager.disableTexture2D();
         GlStateManager.disableDepth();
         GlStateManager.depthMask(false);
         GlStateManager.disableCull();
         GL11.glEnable(2848);
         GL11.glHint(3154, 4354);
         GL11.glLineWidth((Float)this.cityLineW.getValue());
         Color col = this.getCityColor();
         boolean doBox = this.cityRenderMode.getValue() == ESP.StorageRenderMode.Box || this.cityRenderMode.getValue() == ESP.StorageRenderMode.Both;
         boolean doOutline = this.cityRenderMode.getValue() == ESP.StorageRenderMode.Outline || this.cityRenderMode.getValue() == ESP.StorageRenderMode.Both;

         for(BlockPos pos : this.cityPosList) {
            double x = (double)pos.getX() - mc.getRenderManager().viewerPosX;
            double y = (double)pos.getY() - mc.getRenderManager().viewerPosY;
            double z = (double)pos.getZ() - mc.getRenderManager().viewerPosZ;
            AxisAlignedBB bb = new AxisAlignedBB(x, y, z, x + (double)1.0F, y + (double)1.0F, z + (double)1.0F);
            if (doBox) {
               RenderGlobal.renderFilledBox(bb, (float)col.getRed() / 255.0F, (float)col.getGreen() / 255.0F, (float)col.getBlue() / 255.0F, (float)(Integer)this.cityAlpha.getValue() / 255.0F);
            }

            if (doOutline) {
               RenderUtil.drawBlockOutline(bb, col);
            }
         }

         GL11.glDisable(2848);
         GlStateManager.depthMask(true);
         GlStateManager.enableDepth();
         GlStateManager.enableCull();
         GlStateManager.enableTexture2D();
         GlStateManager.disableBlend();
         GlStateManager.popMatrix();
      }

      if ((Boolean)this.breakESP.getValue() && !this.breakList.isEmpty()) {
         Color boxColor = this.getBreakColor();
         Color nameColor = this.getBreakNameColor();

         for(Map.Entry<EntityPlayer, BreakPos> entry : this.breakList.entrySet()) {
            BreakPos bp = (BreakPos)entry.getValue();
            if (bp.pos != null) {
               this.renderBreakBlock(bp.pos, bp.start, bp.time, ((EntityPlayer)entry.getKey()).getName(), boxColor, nameColor);
            }
         }
      }

   }

   private void renderBreakBlock(BlockPos pos, long start, long time, String name, Color boxColor, Color nameColor) {
      double x = (double)pos.getX() - mc.getRenderManager().viewerPosX;
      double y = (double)pos.getY() - mc.getRenderManager().viewerPosY;
      double z = (double)pos.getZ() - mc.getRenderManager().viewerPosZ;
      AxisAlignedBB bb = new AxisAlignedBB(x, y, z, x + (double)1.0F, y + (double)1.0F, z + (double)1.0F);
      float progress = time > 0L ? MathHelper.clamp((float)(System.currentTimeMillis() - start) / (float)time, 0.0F, 1.0F) : 0.0F;
      GlStateManager.pushMatrix();
      GlStateManager.enableBlend();
      GlStateManager.tryBlendFuncSeparate(770, 771, 0, 1);
      GlStateManager.disableTexture2D();
      GlStateManager.disableDepth();
      GlStateManager.depthMask(false);
      GlStateManager.disableCull();
      GL11.glEnable(2848);
      GL11.glHint(3154, 4354);
      GL11.glLineWidth((Float)this.breakLineW.getValue());
      boolean doBox = this.breakRenderMode.getValue() == ESP.RenderMode.Box || this.breakRenderMode.getValue() == ESP.RenderMode.Both;
      boolean doOutline = this.breakRenderMode.getValue() == ESP.RenderMode.Outline || this.breakRenderMode.getValue() == ESP.RenderMode.Both;
      if (doBox) {
         RenderGlobal.renderFilledBox(bb, (float)boxColor.getRed() / 255.0F, (float)boxColor.getGreen() / 255.0F, (float)boxColor.getBlue() / 255.0F, (float)(Integer)this.breakAlpha.getValue() / 255.0F);
      }

      if (doOutline) {
         RenderUtil.drawBlockOutline(bb, boxColor);
      }

      GL11.glDisable(2848);
      GlStateManager.depthMask(true);
      GlStateManager.enableDepth();
      GlStateManager.enableCull();
      GlStateManager.enableTexture2D();
      GlStateManager.disableBlend();
      GlStateManager.popMatrix();
      if ((Boolean)this.breakShowProgress.getValue()) {
         String progressText = String.format("%.0f%%", progress * 100.0F);
         this.renderBreakText((double)pos.getX() + (double)0.5F, (double)pos.getY() + (double)0.75F, (double)pos.getZ() + (double)0.5F, name, nameColor);
         this.renderBreakText((double)pos.getX() + (double)0.5F, (double)pos.getY() + 0.55, (double)pos.getZ() + (double)0.5F, progressText, nameColor);
      } else {
         this.renderBreakText((double)pos.getX() + (double)0.5F, (double)pos.getY() + 0.65, (double)pos.getZ() + (double)0.5F, name, nameColor);
      }

   }

   private void renderBreakText(double x, double y, double z, String text, Color color) {
      GlStateManager.pushMatrix();
      double renderX = x - mc.getRenderManager().viewerPosX;
      double renderY = y - mc.getRenderManager().viewerPosY;
      double renderZ = z - mc.getRenderManager().viewerPosZ;
      GlStateManager.translate(renderX, renderY, renderZ);
      GlStateManager.glNormal3f(0.0F, 1.0F, 0.0F);
      GlStateManager.rotate(-mc.getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);
      GlStateManager.rotate(mc.getRenderManager().playerViewX, 1.0F, 0.0F, 0.0F);
      GlStateManager.scale(-0.02F, -0.02F, 0.02F);
      GlStateManager.disableDepth();
      GlStateManager.disableLighting();
      GlStateManager.enableBlend();
      GlStateManager.tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ZERO);
      GlStateManager.enableTexture2D();
      int textWidth = Vonware.textManager.getStringWidth(text);
      int argb = -16777216 | color.getRed() << 16 | color.getGreen() << 8 | color.getBlue();
      GlStateManager.enablePolygonOffset();
      GlStateManager.doPolygonOffset(1.0F, -1500000.0F);
      Vonware.textManager.drawStringWithShadow(text, (float)(-textWidth) / 2.0F, 0.0F, argb);
      GlStateManager.doPolygonOffset(1.0F, 1500000.0F);
      GlStateManager.disablePolygonOffset();
      GlStateManager.enableDepth();
      GlStateManager.disableBlend();
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      GlStateManager.popMatrix();
   }

   private long calcBreakTime(BlockPos pos) {
      if (pos != null && mc.world != null) {
         IBlockState state = mc.world.getBlockState(pos);
         float hardness = state.getBlockHardness(mc.world, pos);
         if (hardness < 0.0F) {
            return -1L;
         } else {
            float speed = this.getBestBreakSpeed(state);
            if (speed <= 0.0F) {
               return -1L;
            } else {
               float relativeDamage = speed / hardness / 30.0F;
               if (relativeDamage <= 0.0F) {
                  return -1L;
               } else {
                  int ticks = (int)Math.ceil((double)(0.7F / relativeDamage));
                  return (long)ticks * 50L;
               }
            }
         }
      } else {
         return -1L;
      }
   }

   private float getBestBreakSpeed(IBlockState state) {
      float best = 1.0F;

      for(int i = 0; i < 36; ++i) {
         ItemStack stack = mc.player.inventory.getStackInSlot(i);
         float str = stack.getDestroySpeed(state);
         if (str > 1.0F) {
            int eff = EnchantmentHelper.getEnchantmentLevel(Enchantments.EFFICIENCY, stack);
            if (eff > 0) {
               str += (float)(eff * eff) + 1.0F;
            }
         }

         if (str > best) {
            best = str;
         }
      }

      return best;
   }

   private Color getBreakColor() {
      if (ColorPickerConfig.getState(this.breakColor).useGlobalColor) {
         return Global.getGlobalColorRainbowS();
      } else {
         int c = (Integer)this.breakColor.getValue();
         return new Color(c >> 16 & 255, c >> 8 & 255, c & 255);
      }
   }

   private Color getBreakNameColor() {
      if (ColorPickerConfig.getState(this.breakNameColor).useGlobalColor) {
         return Global.getGlobalColorRainbowS();
      } else {
         int c = (Integer)this.breakNameColor.getValue();
         return new Color(c >> 16 & 255, c >> 8 & 255, c & 255);
      }
   }

   private boolean isCityHole(BlockPos pos) {
      Block below = mc.world.getBlockState(pos.down()).getBlock();
      if (below != Blocks.BEDROCK && below != Blocks.OBSIDIAN) {
         return false;
      } else {
         int solidSides = 0;

         for(EnumFacing facing : EnumFacing.HORIZONTALS) {
            Block side = mc.world.getBlockState(pos.offset(facing)).getBlock();
            if (side == Blocks.BEDROCK || side == Blocks.OBSIDIAN) {
               ++solidSides;
            }
         }

         return solidSides >= 3;
      }
   }

   private Color getCityColor() {
      if (ColorPickerConfig.getState(this.cityColor).useGlobalColor) {
         return Global.getGlobalColorRainbowS();
      } else {
         int c = (Integer)this.cityColor.getValue();
         return new Color(c >> 16 & 255, c >> 8 & 255, c & 255);
      }
   }

   private void renderItemNametag(EntityItem item, double x, double y, double z, float delta) {
      Entity camera = mc.getRenderViewEntity();
      if (camera != null) {
         double originalPositionX = camera.posX;
         double originalPositionY = camera.posY;
         double originalPositionZ = camera.posZ;
         camera.posX = this.interpolate(camera.prevPosX, camera.posX, delta);
         camera.posY = this.interpolate(camera.prevPosY, camera.posY, delta);
         camera.posZ = this.interpolate(camera.prevPosZ, camera.posZ, delta);
         String displayTag = item.getItem().getItem().getItemStackDisplayName(item.getItem()) + " x" + item.getItem().stackSize;
         double distance = camera.getDistance(x + mc.getRenderManager().viewerPosX, y + mc.getRenderManager().viewerPosY, z + mc.getRenderManager().viewerPosZ);
         int width = Vonware.textManager.getStringWidth(displayTag) / 2;
         if (distance <= (double)8.0F && (Boolean)this.smartScale.getValue()) {
            distance = (double)8.0F;
         }

         double scale = (0.0018 + (double)(Float)this.itemSize.getValue() * distance * (double)(Float)this.factor.getValue()) / (double)1000.0F;
         GlStateManager.pushMatrix();
         GlStateManager.translate((float)x, (float)y, (float)z);
         GlStateManager.rotate(-mc.getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);
         float var10001 = mc.gameSettings.thirdPersonView == 2 ? -1.0F : 1.0F;
         GlStateManager.rotate(mc.getRenderManager().playerViewX, var10001, 0.0F, 0.0F);
         GlStateManager.scale(-scale, -scale, scale);
         if ((Boolean)this.itemBackground.getValue()) {
            this.quickDrawRect((float)(-width - 2), -10.0F, (float)(width + 2), 2.0F, 1677721600, false);
         }

         Vonware.textManager.drawStringWithShadow(displayTag, (float)(-width), -8.0F, 16777215);
         GlStateManager.popMatrix();
         camera.posX = originalPositionX;
         camera.posY = originalPositionY;
         camera.posZ = originalPositionZ;
      }
   }

   private EntityPlayer findPearlThrower(EntityEnderPearl pearl) {
      if (mc.world != null && mc.player != null) {
         float partialTicks = mc.getRenderPartialTicks();
         EntityPlayer closestPlayer = null;
         float distance = Float.MAX_VALUE;

         for(EntityPlayer player : mc.world.playerEntities) {
            float newDistance = (float)pearl.getPositionVector().distanceTo(player.getPositionEyes(partialTicks));
            if (newDistance < distance) {
               closestPlayer = player;
               distance = newDistance;
            }
         }

         return closestPlayer;
      } else {
         return null;
      }
   }

   private void renderPearlNametag(EntityEnderPearl pearl, String name, float partialTicks) {
      if (pearl != null && !pearl.isDead) {
         Vec3d interpolatedPos = Interpolation.interpolateEntity(pearl);
         if (interpolatedPos != null) {
            Entity camera = mc.getRenderViewEntity();
            if (camera != null) {
               double originalPositionX = camera.posX;
               double originalPositionY = camera.posY;
               double originalPositionZ = camera.posZ;
               camera.posX = this.interpolate(camera.prevPosX, camera.posX, partialTicks);
               camera.posY = this.interpolate(camera.prevPosY, camera.posY, partialTicks);
               camera.posZ = this.interpolate(camera.prevPosZ, camera.posZ, partialTicks);
               double distance = camera.getDistance(interpolatedPos.x + mc.getRenderManager().viewerPosX, interpolatedPos.y + mc.getRenderManager().viewerPosY, interpolatedPos.z + mc.getRenderManager().viewerPosZ);
               if (distance <= (double)8.0F) {
                  distance = (double)8.0F;
               }

               if (Vonware.moduleManager.getModuleByName("Media").isEnabled() && name.equals(mc.player.getName())) {
                  name = ((Media)Vonware.moduleManager.getModuleByName("Media")).newName.getValue();
               }

               boolean customFont = Vonware.moduleManager.getModuleByName("Fonts").isEnabled();
               int width = customFont ? Vonware.textManager.getStringWidth(name) / 2 : mc.fontRenderer.getStringWidth(name) / 2;
               double scale = (0.0018 + (double)(Float)this.pearlSize.getValue() * distance * (double)1.0F) / (double)1000.0F;
               GlStateManager.pushMatrix();
               GlStateManager.translate(interpolatedPos.x, interpolatedPos.y + (double)0.5F, interpolatedPos.z);
               GlStateManager.rotate(-mc.getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);
               float var10001 = mc.gameSettings.thirdPersonView == 2 ? -1.0F : 1.0F;
               GlStateManager.rotate(mc.getRenderManager().playerViewX, var10001, 0.0F, 0.0F);
               GlStateManager.scale(-scale, -scale, scale);
               GlStateManager.disableLighting();
               GlStateManager.disableDepth();
               GlStateManager.enableBlend();
               GlStateManager.tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ZERO);
               this.drawNameTagRect((float)(-width - 1), -9.0F, (float)(width + 1), 1.0F, 0, 0, 1.0F);
               if (customFont) {
                  Vonware.textManager.drawStringWithShadow(name, (float)(-Vonware.textManager.getStringWidth(name)) / 2.0F, -8.0F, -1);
               } else {
                  mc.fontRenderer.drawStringWithShadow(name, (float)(-mc.fontRenderer.getStringWidth(name)) / 2.0F, -8.0F, -1);
               }

               GlStateManager.enableDepth();
               GlStateManager.disableBlend();
               GlStateManager.enableLighting();
               GlStateManager.popMatrix();
               camera.posX = originalPositionX;
               camera.posY = originalPositionY;
               camera.posZ = originalPositionZ;
            }
         }
      }
   }

   private double interpolate(double prev, double current, float delta) {
      return prev + (current - prev) * (double)delta;
   }

   private void drawNameTagRect(float left, float top, float right, float bottom, int color, int border, float width) {
      this.quickDrawRect(left, top, right, bottom, color, false);
      GL11.glLineWidth(width);
      this.quickDrawRect(left, top, right, bottom, border, true);
   }

   private void quickDrawRect(float x, float y, float x2, float y2, int color, boolean line) {
      float a = (float)(color >> 24 & 255) / 255.0F;
      float r = (float)(color >> 16 & 255) / 255.0F;
      float g = (float)(color >> 8 & 255) / 255.0F;
      float b = (float)(color & 255) / 255.0F;
      Tessellator tessellator = Tessellator.getInstance();
      BufferBuilder bufferbuilder = tessellator.getBuffer();
      GlStateManager.enableBlend();
      GlStateManager.disableTexture2D();
      GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
      bufferbuilder.begin(line ? 2 : 7, DefaultVertexFormats.POSITION_COLOR);
      bufferbuilder.pos((double)x, (double)y2, (double)0.0F).color(r, g, b, a).endVertex();
      bufferbuilder.pos((double)x2, (double)y2, (double)0.0F).color(r, g, b, a).endVertex();
      bufferbuilder.pos((double)x2, (double)y, (double)0.0F).color(r, g, b, a).endVertex();
      bufferbuilder.pos((double)x, (double)y, (double)0.0F).color(r, g, b, a).endVertex();
      tessellator.draw();
      GlStateManager.enableTexture2D();
      GlStateManager.disableBlend();
   }

   private void renderChorusText(double x, double y, double z, String text) {
      GlStateManager.pushMatrix();
      double renderX = x - mc.getRenderManager().viewerPosX;
      double renderY = y - mc.getRenderManager().viewerPosY;
      double renderZ = z - mc.getRenderManager().viewerPosZ;
      GlStateManager.translate(renderX, renderY + (double)0.5F, renderZ);
      GlStateManager.glNormal3f(0.0F, 1.0F, 0.0F);
      GlStateManager.rotate(-mc.getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);
      GlStateManager.rotate(mc.getRenderManager().playerViewX, 1.0F, 0.0F, 0.0F);
      GlStateManager.scale(-0.02F, -0.02F, 0.02F);
      GlStateManager.disableDepth();
      GlStateManager.disableLighting();
      GlStateManager.enableBlend();
      GlStateManager.tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ZERO);
      GlStateManager.enableTexture2D();
      int textWidth = Vonware.textManager.getStringWidth(text);
      GlStateManager.enablePolygonOffset();
      GlStateManager.doPolygonOffset(1.0F, -1500000.0F);
      Vonware.textManager.drawStringWithShadow(text, (float)(-textWidth) / 2.0F, 0.0F, -1);
      GlStateManager.doPolygonOffset(1.0F, 1500000.0F);
      GlStateManager.disablePolygonOffset();
      GlStateManager.enableDepth();
      GlStateManager.disableBlend();
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      GlStateManager.popMatrix();
   }

   private StorageType getStorageType(TileEntity te, Block block) {
      if (te instanceof TileEntityChest) {
         if (block == Blocks.CHEST && (Boolean)this.chests.getValue()) {
            return ESP.StorageType.CHEST;
         }

         if (block == Blocks.TRAPPED_CHEST && (Boolean)this.trappedChests.getValue()) {
            return ESP.StorageType.CHEST;
         }
      }

      if (te instanceof TileEntityEnderChest && (Boolean)this.enderChests.getValue()) {
         return ESP.StorageType.ENDER_CHEST;
      } else if (te instanceof TileEntityShulkerBox && (Boolean)this.shulkers.getValue()) {
         return ESP.StorageType.SHULKER;
      } else {
         if ((Boolean)this.storageMisc.getValue()) {
            if (te instanceof TileEntityFurnace) {
               return ESP.StorageType.MISC;
            }

            if (te instanceof TileEntityHopper) {
               return ESP.StorageType.MISC;
            }

            if (te instanceof TileEntityBrewingStand) {
               return ESP.StorageType.MISC;
            }

            if (te instanceof TileEntityDispenser && (block == Blocks.DISPENSER || block == Blocks.DROPPER)) {
               return ESP.StorageType.MISC;
            }
         }

         return null;
      }
   }

   private Color getStorageColor(StorageType type) {
      switch (type) {
         case CHEST:
            return this.resolveStorageColor(this.chestColor);
         case ENDER_CHEST:
            return this.resolveStorageColor(this.enderColor);
         case SHULKER:
            return this.resolveStorageColor(this.shulkerColor);
         default:
            return this.resolveStorageColor(this.miscColor);
      }
   }

   private Color resolveStorageColor(ColorSetting setting) {
      if (ColorPickerConfig.getState(setting).useGlobalColor) {
         return Global.getGlobalColorRainbowS();
      } else {
         int c = (Integer)setting.getValue();
         return new Color(c >> 16 & 255, c >> 8 & 255, c & 255);
      }
   }

   public Color getColor() {
      if (ColorPickerConfig.getState(this.customColor).useGlobalColor) {
         return Global.getGlobalColorRainbowS();
      } else {
         int colorInt = (Integer)this.customColor.getValue();
         return new Color(colorInt >> 16 & 255, colorInt >> 8 & 255, colorInt & 255);
      }
   }

   public Color getBoxColor() {
      return ColorUtil.changeAlpha(this.getColor(), (Integer)this.boxAlpha.getValue());
   }

   protected boolean isValid(Entity entity) {
      if (entity instanceof EntityItem && (Boolean)this.items.getValue()) {
         return true;
      } else if (entity instanceof EntityExpBottle && (Boolean)this.xpBottles.getValue()) {
         return true;
      } else {
         return entity instanceof EntityEnderPearl && (Boolean)this.pearls.getValue();
      }
   }

   protected void render(AxisAlignedBB bb, Color color) {
      switch ((RenderMode)this.mode.getValue()) {
         case Box:
            this.renderBox(bb);
            break;
         case Outline:
            this.renderOutline(bb, color);
            break;
         case Both:
            this.renderBox(bb);
            this.renderOutline(bb, color);
      }

   }

   protected void renderBox(AxisAlignedBB bb) {
      RenderMethods.drawBox(bb, this.getBoxColor());
   }

   public void renderOutline(AxisAlignedBB bb, Color color) {
      RenderMethods.drawOutline(bb, (Float)this.lineWidth.getValue(), color);
   }

   private Color getBurrowColor() {
      if (ColorPickerConfig.getState(this.burrowColor).useGlobalColor) {
         Colors colors = Colors.getInstance();
         if ((Boolean)colors.rainbow.getValue()) {
            return colors.rainbowMode.getValue() == Colors.RainbowMode.Static ? ColorUtil.rainbow(0) : ColorUtil.rainbow((int)((float)NewComponent.counter1[0] * (Float)colors.rainbowHue.getValue()));
         } else {
            return (Boolean)ClickGui.getInstance().pulseGui.getValue() ? Global.getPulseColor(colors.getGuiColor(), 0) : colors.getCurrentColor();
         }
      } else {
         int colorInt = (Integer)this.burrowColor.getValue();
         return new Color(colorInt >> 16 & 255, colorInt >> 8 & 255, colorInt & 255);
      }
   }

   private Color getClipColor() {
      if (ColorPickerConfig.getState(this.clipColor).useGlobalColor) {
         return Global.getGlobalColorRainbowS();
      } else {
         int c = (Integer)this.clipColor.getValue();
         return new Color(c >> 16 & 255, c >> 8 & 255, c & 255);
      }
   }

   public static enum StorageRenderMode {
      Box,
      Outline,
      Both;
   }

   public static enum StorageType {
      CHEST,
      ENDER_CHEST,
      SHULKER,
      MISC;
   }

   public static enum BurrowRenderMode {
      Text,
      Box;
   }

   public static enum RenderMode {
      Box,
      Outline,
      Both,
      None;
   }

   public static enum ClipRenderMode {
      Face,
      Block;
   }

   private static class StorageEntry {
      final BlockPos pos;
      final StorageType type;

      StorageEntry(BlockPos pos, StorageType type) {
         this.pos = pos;
         this.type = type;
      }
   }

   private static class ClipEntry {
      final EntityPlayer player;
      final BlockPos adjacentBlock;
      final EnumFacing faceDir;

      ClipEntry(EntityPlayer player, BlockPos adjacentBlock, EnumFacing faceDir) {
         this.player = player;
         this.adjacentBlock = adjacentBlock;
         this.faceDir = faceDir;
      }
   }

   private static class BreakPos {
      BlockPos pos;
      long start;
      long time;

      BreakPos(BlockPos pos, long time) {
         this.pos = pos;
         this.start = System.currentTimeMillis();
         this.time = time;
      }

      void update(BlockPos pos, long time) {
         this.pos = pos;
         this.start = System.currentTimeMillis();
         this.time = time;
      }
   }

   public static class ChorusPos {
      public final long startTime;
      public final double x;
      public final double y;
      public final double z;

      public ChorusPos(long startTime, double x, double y, double z) {
         this.startTime = startTime;
         this.x = x;
         this.y = y;
         this.z = z;
      }
   }
}
