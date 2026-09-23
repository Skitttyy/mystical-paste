package fat.vonware.util.BlockPlaceModules;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture.Type;
import fat.vonware.Global;
import fat.vonware.Vonware;
import fat.vonware.features.gui.newgui.newcomponents.NewComponent;
import fat.vonware.features.modules.client.ClickGui;
import fat.vonware.features.modules.client.Colors;
import fat.vonware.util.Animations.AnimationUtil;
import fat.vonware.util.Animations.ScrollBarUtil;
import fat.vonware.util.Animations.draw.DismantleUtil;
import fat.vonware.util.Animations.render.ColorUtil;
import fat.vonware.util.Animations.render.RenderUtil;
import fat.vonware.util.Chat.ThreadUtil;
import fat.vonware.util.OtherRenderShit.Render.GlowTextUtil;
import fat.vonware.util.OtherRenderShit.gui.SoundUtil;
import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.GlStateManager.DestFactor;
import net.minecraft.client.renderer.GlStateManager.SourceFactor;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Mouse;

public class WelcomeScreen extends GuiScreen {
    private static boolean welcomed = false;
    private static final String WELCOMED_FILE = "vonware/welcomed.txt";
    private boolean tosAccepted = false;
    private int tosCheckX;
    private int tosCheckY;
    private int tosCheckSize;
    private int tosViewX;
    private int tosViewY;
    private int tosViewW;
    private int tosViewH;
    private boolean showTosPopup = false;
    private int tosPopupCloseX;
    private int tosPopupCloseY;
    private static final int tosPopupCloseSize = 12;
    private int tosPopupX;
    private int tosPopupY;
    private int tosPopupW;
    private int tosPopupH;
    private final AnimationUtil.DeltaAnimation tosCheckAnim = new AnimationUtil.DeltaAnimation(0.0F, 125.0F);
    private final ScrollBarUtil tosScrollBar = new ScrollBarUtil();
    private static final String[] TOS_LINES = new String[]{"Mystic - Terms of Service", "", "1. License & Usage", "   This client is licensed to a single user. Redistribution, resale,", "   or sharing of your license is strictly prohibited.", "", "2. Prohibited Activities", "   You may not decompile, reverse-engineer, crack, or modify this", "   client in any way. Any attempt to bypass the authentication", "   system will result in permanent revocation of your license.", "", "3. Account Responsibility", "   You are solely responsible for any actions performed with this", "   client on your Minecraft account. Mystic is not liable for bans", "   or penalties issued by Mojang or any server.", "", "4. Modifications", "   These terms may be updated at any time. Continued use of the", "   client constitutes acceptance of the current terms.", "", "By checking the box you confirm you have read and agree to", "these terms in full."};
    private static final int TOS_LINE_HEIGHT = 10;
    private float fadeIn = 0.0F;
    private float buttonHoverAnim = 0.0F;
    private long lastUpdateTime = System.currentTimeMillis();
    private long openTime = 0L;
    private int buttonX;
    private int buttonY;
    private int buttonW;
    private int buttonH;
    private ResourceLocation playerSkinLocation;
    private boolean skinRequested = false;
    private final DismantleUtil dismantle = new DismantleUtil();
    private final Random random = new Random();
    private final List<VoidStar> voidStars = new ArrayList();
    private boolean starsInitialized = false;
    private final List<CursedParticle> cursedParticles = new ArrayList();
    private final List<BlueOrb> blueOrbs = new ArrayList();
    private float sixEyesRotation = 0.0F;
    private final List<PurpleStreak> purpleStreaks = new ArrayList();
    private long lastPurpleSpawn = 0L;
    private final List<InfinityRipple> infinityRipples = new ArrayList();
    private long lastRippleSpawn = 0L;
    private static boolean startupPlayed = false;

    public WelcomeScreen() {
        this.openTime = System.currentTimeMillis();
    }

    public static boolean isFirstLaunch() {
        if (welcomed) {
            return false;
        } else {
            File file = new File("vonware/welcomed.txt");
            if (file.exists()) {
                try {
                    BufferedReader reader = new BufferedReader(new FileReader(file));
                    Throwable var2 = null;

                    boolean var4;
                    try {
                        String line = reader.readLine();
                        if (!"true".equalsIgnoreCase(line != null ? line.trim() : "")) {
                            return true;
                        }

                        welcomed = true;
                        var4 = false;
                    } catch (Throwable var15) {
                        var2 = var15;
                        throw var15;
                    } finally {
                        if (reader != null) {
                            if (var2 != null) {
                                try {
                                    reader.close();
                                } catch (Throwable var14) {
                                    var2.addSuppressed(var14);
                                }
                            } else {
                                reader.close();
                            }
                        }

                    }

                    return var4;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            return true;
        }
    }

    private static void setWelcomed() {
        welcomed = true;
        File dir = new File("vonware");
        if (!dir.exists()) {
            dir.mkdirs();
        }

        try {
            FileWriter writer = new FileWriter("vonware/welcomed.txt");
            Throwable var2 = null;

            try {
                writer.write("true");
            } catch (Throwable var12) {
                var2 = var12;
                throw var12;
            } finally {
                if (writer != null) {
                    if (var2 != null) {
                        try {
                            writer.close();
                        } catch (Throwable var11) {
                            var2.addSuppressed(var11);
                        }
                    } else {
                        writer.close();
                    }
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void initGui() {
        this.openTime = System.currentTimeMillis();
        this.fadeIn = 0.0F;
        this.starsInitialized = false;
        this.voidStars.clear();
        this.cursedParticles.clear();
        this.blueOrbs.clear();
        this.purpleStreaks.clear();
        this.infinityRipples.clear();
        this.skinRequested = false;
        this.playerSkinLocation = null;
        this.tosScrollBar.resetScroll();
        this.dismantle.reset();
        if (!startupPlayed) {
            SoundUtil.playStartup();
            startupPlayed = true;
        }

    }

    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        long currentTime = System.currentTimeMillis();
        float deltaTime = (float)(currentTime - this.lastUpdateTime) / 1000.0F;
        this.lastUpdateTime = currentTime;
        this.dismantle.tickProgress(deltaTime);
        if (this.fadeIn < 1.0F) {
            this.fadeIn = Math.min(1.0F, this.fadeIn + deltaTime * 1.65F);
        }

        float easedFade = this.easeOutCubic(this.fadeIn);
        ScaledResolution sr = new ScaledResolution(this.mc);
        int screenWidth = sr.getScaledWidth();
        int screenHeight = sr.getScaledHeight();

        // --- GLITCH MATRIX TRANSFORM: CHROMATIC & SLICE DISPLACEMENT ---
        GlStateManager.pushMatrix();
        if (this.random.nextFloat() < 0.45F) {
            float glitchX = (this.random.nextFloat() - 0.5F) * 18.0F;
            float glitchY = (this.random.nextFloat() - 0.5F) * 6.0F;
            GlStateManager.translate(glitchX, glitchY, 0.0F);
        }

        drawRect(0, 0, screenWidth, screenHeight, (new Color(3, 3, 8, (int)(255.0F * easedFade))).getRGB());
        if (!this.starsInitialized) {
            this.initVoidStars(screenWidth, screenHeight);
            this.initBlueOrbs(screenWidth, screenHeight);
            this.starsInitialized = true;
        }

        int panelW = isFirstLaunch() ? 320 : 240;
        int panelH = isFirstLaunch() ? 240 : 135;
        int panelX = (screenWidth - panelW) / 2;
        int panelY = (screenHeight - panelH) / 2;
        float slideOffset = (1.0F - easedFade) * 30.0F;
        panelY += (int)slideOffset;

        // Jitter panel position randomly for hardware crash feel
        if (this.random.nextFloat() < 0.25F) {
            panelX += (this.random.nextInt(11) - 5);
        }

        float panelCenterX = (float)panelX + (float)panelW / 2.0F;
        float panelCenterY = (float)panelY + (float)panelH / 2.0F;
        this.drawVoidStars(screenWidth, screenHeight, deltaTime, easedFade);
        this.drawSixEyesRings(panelCenterX, panelCenterY, deltaTime, easedFade);
        this.updateAndDrawBlueOrbs(screenWidth, screenHeight, deltaTime, easedFade);
        this.spawnCursedParticles(screenWidth, screenHeight, panelCenterX, panelCenterY);
        this.updateAndDrawCursedParticles(panelCenterX, panelCenterY, deltaTime, easedFade);
        if (currentTime - this.lastRippleSpawn > 1800L && easedFade > 0.5F) {
            this.infinityRipples.add(new InfinityRipple(panelCenterX, panelCenterY));
            this.lastRippleSpawn = currentTime;
        }

        this.updateAndDrawInfinityRipples(deltaTime, easedFade);
        if (currentTime - this.lastPurpleSpawn > (long)(4000 + this.random.nextInt(3000)) && easedFade > 0.7F) {
            this.purpleStreaks.add(new PurpleStreak(screenWidth, screenHeight, this.random));
            this.lastPurpleSpawn = currentTime;
        }

        this.updateAndDrawPurpleStreaks(deltaTime, easedFade);
        if (this.dismantle.exitAnimating && !this.dismantle.exitInitialized) {
            this.dismantle.initIfNeeded(screenWidth, screenHeight, panelX, panelY, panelW, panelH, this.getThemeColor());
        }

        if (this.dismantle.exitAnimating && this.dismantle.exitProgress >= 1.24F) {
            this.dismantle.drawExitShatter(screenWidth, screenHeight);
            if (this.dismantle.exitProgress >= 2.2F) {
                setWelcomed();
                Vonware.screenTransitionManager.beginFadeIn();
                this.mc.displayGuiScreen((GuiScreen)null);
            }

            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        } else {
            Color themeColor = this.getThemeColor();
            int panelAlpha = (int)(230.0F * easedFade);
            this.drawPanelGlow(panelX, panelY, panelW, panelH, themeColor, easedFade);
            RenderUtil.drawGradientRect((float)panelX, (float)panelY, (float)(panelX + panelW), (float)(panelY + panelH), (new Color(25, 25, 30, panelAlpha)).getRGB(), (new Color(15, 15, 18, panelAlpha)).getRGB());
            RenderUtil.drawRect((float)panelX, (float)panelY, (float)(panelX + panelW), (float)(panelY + 4), (new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), panelAlpha)).getRGB());
            RenderUtil.drawGradientRect((float)panelX, (float)(panelY + 4), (float)(panelX + panelW), (float)(panelY + 16), (new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), (int)(50.0F * easedFade))).getRGB(), (new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), 0)).getRGB());
            this.drawOutlineRect((float)panelX, (float)panelY, (float)(panelX + panelW), (float)(panelY + panelH), (new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), (int)(100.0F * easedFade))).getRGB());
            this.drawOutlineRect((float)(panelX + 1), (float)(panelY + 1), (float)(panelX + panelW - 1), (float)(panelY + panelH - 1), (new Color(255, 255, 255, (int)(10.0F * easedFade))).getRGB());
            int textAlpha = (int)(255.0F * easedFade);
            int textWhite = (new Color(255, 255, 255, textAlpha)).getRGB();
            int textGray = (new Color(190, 190, 195, textAlpha)).getRGB();
            int textDimGray = (new Color(120, 120, 130, textAlpha)).getRGB();
            String owner = BlockPlaceUtil.getWelcomer();
            Color goldBase = Global.getGlobalColorRainbowS();
            if (owner != null) {
                String welcomeLabel = "Welcome, ";
                float welcomeLabelW = (float)Vonware.textManager.getStringWidth(welcomeLabel);
                float totalW = welcomeLabelW + (float)Vonware.textManager.getStringWidth(owner);
                float startX = (float)panelX + ((float)panelW - totalW) / 2.0F;
                float centerY = (float)(panelY + 30);
                Vonware.textManager.drawStringWithShadow(welcomeLabel, startX, centerY, textWhite);
                GlowTextUtil.drawGlowString(owner, startX + welcomeLabelW, centerY, goldBase, 16777215, textAlpha);
            }

            String playerName = this.mc.getSession().getUsername();
            float headSize = 16.0F;
            float headNameGap = 5.0F;
            float nameW = (float)Vonware.textManager.getStringWidth(playerName);
            float totalSubW = headSize + headNameGap + nameW;
            float subStartX = (float)panelX + ((float)panelW - totalSubW) / 2.0F;
            float subY = (float)(panelY + 42);
            GlStateManager.enableTexture2D();
            GlStateManager.enableBlend();
            GlStateManager.color(1.0F, 1.0F, 1.0F, easedFade);
            this.mc.getTextureManager().bindTexture(this.getPlayerSkinLocation());
            float u1 = 0.125F;
            float v1 = 0.125F;
            float u2 = 0.25F;
            float v2 = 0.25F;
            Tessellator tessHead = Tessellator.getInstance();
            BufferBuilder bufHead = tessHead.getBuffer();
            bufHead.begin(7, DefaultVertexFormats.POSITION_TEX);
            bufHead.pos((double)subStartX, (double)(subY + headSize), (double)0.0F).tex((double)u1, (double)v2).endVertex();
            bufHead.pos((double)(subStartX + headSize), (double)(subY + headSize), (double)0.0F).tex((double)u2, (double)v2).endVertex();
            bufHead.pos((double)(subStartX + headSize), (double)subY, (double)0.0F).tex((double)u2, (double)v1).endVertex();
            bufHead.pos((double)subStartX, (double)subY, (double)0.0F).tex((double)u1, (double)v1).endVertex();
            tessHead.draw();
            float hu1 = 0.625F;
            float hv1 = 0.125F;
            float hu2 = 0.75F;
            float hv2 = 0.25F;
            float hatPad = 1.0F;
            bufHead.begin(7, DefaultVertexFormats.POSITION_TEX);
            bufHead.pos((double)(subStartX - hatPad), (double)(subY + headSize + hatPad), (double)0.0F).tex((double)hu1, (double)hv2).endVertex();
            bufHead.pos((double)(subStartX + headSize + hatPad), (double)(subY + headSize + hatPad), (double)0.0F).tex((double)hu2, (double)hv2).endVertex();
            bufHead.pos((double)(subStartX + headSize + hatPad), (double)(subY - hatPad), (double)0.0F).tex((double)hu2, (double)hv1).endVertex();
            bufHead.pos((double)(subStartX - hatPad), (double)(subY - hatPad), (double)0.0F).tex((double)hu1, (double)hv1).endVertex();
            tessHead.draw();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            Vonware.textManager.drawStringWithShadow(playerName, subStartX + headSize + headNameGap, subY + (headSize - 8.0F) / 2.0F, textWhite);
            if (!isFirstLaunch()) {
                this.drawSeparator((float)(panelX + 20), (float)(panelX + panelW - 20), (float)(panelY + 62), themeColor, easedFade);
            }

            if (isFirstLaunch()) {
                this.drawSeparator((float)(panelX + 30), (float)(panelX + panelW - 30), (float)(panelY + 62), themeColor, easedFade);
                this.drawInfoPair("ClickGui Bind", "L", (float)(panelX + 24), (float)(panelY + 76), textDimGray, textWhite);
                this.drawInfoPair("Command Prefix", ",  (Comma)", (float)(panelX + 24), (float)(panelY + 91), textDimGray, textWhite);
                this.drawInfoPair("Search Bar", "Ctrl + F", (float)(panelX + 24), (float)(panelY + 106), textDimGray, textWhite);
            }

            if (isFirstLaunch()) {
                int tosRowY = panelY + panelH - 62;
                this.tosCheckSize = 9;
                this.tosCheckX = panelX + 14;
                this.tosCheckY = tosRowY;
                this.drawOutlineRect((float)this.tosCheckX, (float)this.tosCheckY, (float)(this.tosCheckX + this.tosCheckSize), (float)(this.tosCheckY + this.tosCheckSize), (new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), (int)(160.0F * easedFade))).getRGB());
                float easedCheck = this.tosCheckAnim.tickEased(this.tosAccepted);
                if (easedCheck > 0.0F) {
                    RenderUtil.drawRect((float)(this.tosCheckX + 1), (float)(this.tosCheckY + 1), (float)(this.tosCheckX + this.tosCheckSize - 1), (float)(this.tosCheckY + this.tosCheckSize - 1), (new Color(Math.min(255, themeColor.getRed() + 20), Math.min(255, themeColor.getGreen() + 20), Math.min(255, themeColor.getBlue() + 20), (int)(70.0F * easedCheck * easedFade))).getRGB());
                    GlStateManager.enableBlend();
                    GlStateManager.disableTexture2D();
                    GlStateManager.tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ZERO);
                    float ckX1 = (float)this.tosCheckX + 2.0F;
                    float ckY1 = (float)this.tosCheckY + (float)this.tosCheckSize * 0.5F;
                    float ckMX = (float)this.tosCheckX + (float)this.tosCheckSize * 0.38F;
                    float ckMY = (float)(this.tosCheckY + this.tosCheckSize) - 2.0F;
                    float ckX2 = (float)(this.tosCheckX + this.tosCheckSize) - 1.5F;
                    float ckY2 = (float)this.tosCheckY + 2.0F;
                    float seg1End = 0.45F;
                    int ckR = Math.min(255, themeColor.getRed() + 20);
                    int ckG = Math.min(255, themeColor.getGreen() + 20);
                    int ckB = Math.min(255, themeColor.getBlue() + 20);
                    float ckA = easedCheck * easedFade;
                    Tessellator tessCk = Tessellator.getInstance();
                    BufferBuilder bufCk = tessCk.getBuffer();
                    float tipAX;
                    float tipAY;
                    float tipBX;
                    float tipBY;
                    boolean seg1Done;
                    if (easedCheck <= seg1End) {
                        float t = easedCheck / seg1End;
                        tipAX = ckX1 + (ckMX - ckX1) * t;
                        tipAY = ckY1 + (ckMY - ckY1) * t;
                        tipBX = tipAX;
                        tipBY = tipAY;
                        seg1Done = false;
                    } else {
                        tipAX = ckMX;
                        tipAY = ckMY;
                        float t = (easedCheck - seg1End) / (1.0F - seg1End);
                        tipBX = ckMX + (ckX2 - ckMX) * t;
                        tipBY = ckMY + (ckY2 - ckMY) * t;
                        seg1Done = true;
                    }

                    float cr = (float)ckR / 255.0F;
                    float cg = (float)ckG / 255.0F;
                    float cb = (float)ckB / 255.0F;
                    this.drawLine(bufCk, tessCk, ckX1, ckY1, tipAX, tipAY, 5.5F, cr, cg, cb, ckA * 0.12F);
                    if (seg1Done) {
                        this.drawLine(bufCk, tessCk, ckMX, ckMY, tipBX, tipBY, 5.5F, cr, cg, cb, ckA * 0.12F);
                    }

                    this.drawLine(bufCk, tessCk, ckX1, ckY1, tipAX, tipAY, 3.0F, cr, cg, cb, ckA * 0.28F);
                    if (seg1Done) {
                        this.drawLine(bufCk, tessCk, ckMX, ckMY, tipBX, tipBY, 3.0F, cr, cg, cb, ckA * 0.28F);
                    }

                    this.drawLine(bufCk, tessCk, ckX1, ckY1, tipAX, tipAY, 1.2F, cr, cg, cb, ckA * 0.95F);
                    if (seg1Done) {
                        this.drawLine(bufCk, tessCk, ckMX, ckMY, tipBX, tipBY, 1.2F, cr, cg, cb, ckA * 0.95F);
                    }

                    GlStateManager.enableTexture2D();
                }

                float tosTextX = (float)(this.tosCheckX + this.tosCheckSize + 5);
                float tosTextY = (float)this.tosCheckY + (float)(this.tosCheckSize - 8) / 2.0F;
                String agreePrefix = "I agree to the ";
                Vonware.textManager.drawStringWithShadow(agreePrefix, tosTextX, tosTextY, textGray);
                float prefixW = (float)Vonware.textManager.getStringWidth(agreePrefix);
                String tosLink = "Terms of Service";
                float tosLinkW = (float)Vonware.textManager.getStringWidth(tosLink);
                this.tosViewX = (int)(tosTextX + prefixW);
                this.tosViewY = (int)tosTextY;
                this.tosViewW = (int)tosLinkW;
                this.tosViewH = 8;
                boolean hoveringTosLink = mouseX >= this.tosViewX && mouseX <= this.tosViewX + this.tosViewW && mouseY >= this.tosViewY && mouseY <= this.tosViewY + this.tosViewH;
                int tosLinkColor = hoveringTosLink ? (new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), textAlpha)).getRGB() : (new Color(150, 170, 210, textAlpha)).getRGB();
                Vonware.textManager.drawStringWithShadow(tosLink, tosTextX + prefixW, tosTextY, tosLinkColor);
                RenderUtil.drawRect(tosTextX + prefixW, tosTextY + 9.0F, tosTextX + prefixW + tosLinkW, tosTextY + 9.5F, tosLinkColor);
            }

            this.buttonW = 100;
            this.buttonH = 20;
            this.buttonX = panelX + (panelW - this.buttonW) / 2;
            this.buttonY = panelY + panelH - 38;
            boolean buttonEnabled = !this.dismantle.exitAnimating && (!isFirstLaunch() || this.tosAccepted);
            boolean hovering = buttonEnabled && mouseX >= this.buttonX && mouseX <= this.buttonX + this.buttonW && mouseY >= this.buttonY && mouseY <= this.buttonY + this.buttonH;
            if (hovering) {
                this.buttonHoverAnim = Math.min(1.0F, this.buttonHoverAnim + deltaTime * 6.0F);
            } else {
                this.buttonHoverAnim = Math.max(0.0F, this.buttonHoverAnim - deltaTime * 4.0F);
            }

            float easedHover = this.easeOutCubic(this.buttonHoverAnim);
            if (buttonEnabled) {
                if (easedHover > 0.01F) {
                    int ga = (int)(30.0F * easedHover * easedFade);
                    RenderUtil.drawGradientRect((float)(this.buttonX - 4), (float)(this.buttonY - 2), (float)(this.buttonX + this.buttonW + 4), (float)(this.buttonY + this.buttonH + 2), (new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), ga)).getRGB(), (new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), 0)).getRGB());
                }

                int ba = (int)((70.0F + 60.0F * easedHover) * easedFade);
                RenderUtil.drawGradientRect((float)this.buttonX, (float)this.buttonY, (float)(this.buttonX + this.buttonW), (float)(this.buttonY + this.buttonH), (new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), ba)).getRGB(), (new Color(Math.max(0, themeColor.getRed() - 30), Math.max(0, themeColor.getGreen() - 30), Math.max(0, themeColor.getBlue() - 30), ba)).getRGB());
                RenderUtil.drawGradientRect((float)this.buttonX, (float)this.buttonY, (float)(this.buttonX + this.buttonW), (float)(this.buttonY + 2), (new Color(255, 255, 255, (int)(35.0F * easedFade))).getRGB(), (new Color(255, 255, 255, 0)).getRGB());
                this.drawOutlineRect((float)this.buttonX, (float)this.buttonY, (float)(this.buttonX + this.buttonW), (float)(this.buttonY + this.buttonH), (new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), (int)((70.0F + 80.0F * easedHover) * easedFade))).getRGB());
            } else {
                RenderUtil.drawGradientRect((float)this.buttonX, (float)this.buttonY, (float)(this.buttonX + this.buttonW), (float)(this.buttonY + this.buttonH), (new Color(50, 50, 55, (int)(120.0F * easedFade))).getRGB(), (new Color(35, 35, 40, (int)(120.0F * easedFade))).getRGB());
                this.drawOutlineRect((float)this.buttonX, (float)this.buttonY, (float)(this.buttonX + this.buttonW), (float)(this.buttonY + this.buttonH), (new Color(80, 80, 85, (int)(100.0F * easedFade))).getRGB());
            }

            float btnTW = (float)Vonware.textManager.getStringWidth("Continue");
            Vonware.textManager.drawStringWithShadow("Continue", (float)this.buttonX + ((float)this.buttonW - btnTW) / 2.0F, (float)this.buttonY + (float)(this.buttonH - 8) / 2.0F, buttonEnabled ? textWhite : (new Color(100, 100, 110, (int)(180.0F * easedFade))).getRGB());
            Vonware.textManager.drawStringWithShadow("", (float)(panelX + 10), (float)(panelY + panelH - 14), textDimGray);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            if (this.dismantle.exitAnimating && this.dismantle.exitProgress >= 0.55F && this.dismantle.exitProgress < 1.24F) {
                this.dismantle.drawSlashCrackOverlay(screenWidth, screenHeight);
            }

            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            if (this.showTosPopup && !this.dismantle.exitAnimating) {
                this.drawTosPopup(screenWidth, screenHeight, mouseX, mouseY, easedFade, themeColor);
            }

        }

        // --- HARDWARE STATIC OVERLAYS & OVERLAY ENGINE ---
        this.drawGlitchStaticOverlay(screenWidth, screenHeight);
        this.drawCrackedDogshitOverlay(screenWidth, screenHeight, currentTime);

        GlStateManager.popMatrix();
    }

    // --- RENDER HEAVY NOISE & CORRUPTED SCREEN OVERLAYS ---
    private void drawGlitchStaticOverlay(int sw, int sh) {
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE, SourceFactor.ONE, DestFactor.ZERO);
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();

        // Corrupted RAM Noise Quads
        buf.begin(7, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i < 60; i++) {
            float rx = this.random.nextFloat() * sw;
            float ry = this.random.nextFloat() * sh;
            float rw = 5.0F + this.random.nextFloat() * 45.0F;
            float rh = 1.0F + this.random.nextFloat() * 3.0F;
            float r = this.random.nextFloat();
            float g = this.random.nextFloat();
            float b = this.random.nextFloat();
            float a = 0.15F + this.random.nextFloat() * 0.35F;

            buf.pos((double)rx, (double)(ry + rh), 0.0).color(r, g, b, a).endVertex();
            buf.pos((double)(rx + rw), (double)(ry + rh), 0.0).color(r, g, b, a).endVertex();
            buf.pos((double)(rx + rw), (double)ry, 0.0).color(r, g, b, a).endVertex();
            buf.pos((double)rx, (double)ry, 0.0).color(r, g, b, a).endVertex();
        }
        tess.draw();

        GlStateManager.enableTexture2D();
    }

    // --- RENDER "CRACKED DOGSHIT" BITMAP OVERLAYS ---
    private void drawCrackedDogshitOverlay(int sw, int sh, long time) {
        String text = "CRACKED DOGSHIT";
        GlStateManager.enableBlend();

        // Scrolling Top Banner Overlay
        float scrollX1 = (float)((time * 0.2) % (sw + 300)) - 150.0F;
        int rainbowColor1 = Color.HSBtoRGB((time % 2000L) / 2000.0F, 0.9F, 1.0F);
        Vonware.textManager.drawStringWithShadow(text, scrollX1, 15.0F, rainbowColor1);

        // Center High Intensity Multi-Glitch Duplicate
        if (this.random.nextFloat() < 0.8F) {
            float glitchCenterX = (sw - Vonware.textManager.getStringWidth(text)) / 2.0F + (this.random.nextFloat() - 0.5F) * 12.0F;
            float glitchCenterY = sh / 2.0F + (this.random.nextFloat() - 0.5F) * 12.0F;
            int rainbowColor2 = Color.HSBtoRGB(((time + 500L) % 1500L) / 1500.0F, 1.0F, 1.0F);
            Vonware.textManager.drawStringWithShadow(text, glitchCenterX, glitchCenterY, rainbowColor2);
        }

        // Scrolling Bottom Reverse Banner
        float scrollX2 = sw - (float)((time * 0.25) % (sw + 300));
        int rainbowColor3 = Color.HSBtoRGB(((time + 1000L) % 2500L) / 2500.0F, 0.8F, 1.0F);
        Vonware.textManager.drawStringWithShadow(text, scrollX2, sh - 25.0F, rainbowColor3);
    }

    private void drawTosPopup(int sw, int sh, int mouseX, int mouseY, float alpha, Color themeColor) {
        drawRect(0, 0, sw, sh, (new Color(0, 0, 0, (int)(160.0F * alpha))).getRGB());
        this.tosPopupW = 340;
        this.tosPopupH = 220;
        this.tosPopupX = (sw - this.tosPopupW) / 2;
        this.tosPopupY = (sh - this.tosPopupH) / 2;
        int popAlpha = (int)(240.0F * alpha);
        int textAlpha = (int)(255.0F * alpha);
        int textWhite = (new Color(255, 255, 255, textAlpha)).getRGB();
        RenderUtil.drawGradientRect((float)this.tosPopupX, (float)this.tosPopupY, (float)(this.tosPopupX + this.tosPopupW), (float)(this.tosPopupY + this.tosPopupH), (new Color(20, 20, 25, popAlpha)).getRGB(), (new Color(12, 12, 15, popAlpha)).getRGB());
        this.drawOutlineRect((float)this.tosPopupX, (float)this.tosPopupY, (float)(this.tosPopupX + this.tosPopupW), (float)(this.tosPopupY + this.tosPopupH), (new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), (int)(130.0F * alpha))).getRGB());
        RenderUtil.drawRect((float)this.tosPopupX, (float)this.tosPopupY, (float)(this.tosPopupX + this.tosPopupW), (float)(this.tosPopupY + 3), (new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), popAlpha)).getRGB());
        String title = "Terms of Service";
        Vonware.textManager.drawStringWithShadow(title, (float)this.tosPopupX + (float)(this.tosPopupW - Vonware.textManager.getStringWidth(title)) / 2.0F, (float)(this.tosPopupY + 8), textWhite);
        this.tosPopupCloseX = this.tosPopupX + this.tosPopupW - 12 - 6;
        this.tosPopupCloseY = this.tosPopupY + 4;
        boolean hoverClose = mouseX >= this.tosPopupCloseX && mouseX <= this.tosPopupCloseX + 12 && mouseY >= this.tosPopupCloseY && mouseY <= this.tosPopupCloseY + 12;
        Vonware.textManager.drawStringWithShadow("x", (float)(this.tosPopupCloseX + 2), (float)(this.tosPopupCloseY + 1), hoverClose ? (new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), textAlpha)).getRGB() : (new Color(150, 150, 160, textAlpha)).getRGB());
        this.drawSeparator((float)(this.tosPopupX + 10), (float)(this.tosPopupX + this.tosPopupW - 10), (float)(this.tosPopupY + 20), themeColor, alpha);
        int contentX = this.tosPopupX + 12;
        int contentY = this.tosPopupY + 26;
        int contentH = this.tosPopupH - 50;
        int totalTextH = TOS_LINES.length * 10;
        this.tosScrollBar.clampToContent((float)totalTextH, (float)contentH);
        float scrollOff = this.tosScrollBar.getScrollOffset();

        for(int i = 0; i < TOS_LINES.length; ++i) {
            float lineY = (float)(contentY + i * 10) - scrollOff;
            if (!(lineY < (float)(contentY - 10))) {
                if (lineY > (float)(contentY + contentH)) {
                    break;
                }

                String line = TOS_LINES[i];
                int lc = line.equals("Mystic - Terms of Service") ? textWhite : (line.matches("\\d+\\..*") ? (new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), textAlpha)).getRGB() : (new Color(180, 180, 190, textAlpha)).getRGB());
                Vonware.textManager.drawStringWithShadow(line, (float)contentX, lineY, lc);
            }
        }

        this.tosScrollBar.draw((float)(this.tosPopupX + this.tosPopupW - 6), (float)contentY, (float)contentH, (float)totalTextH, (float)contentH, themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), mouseX, mouseY);
        int aw = 100;
        int ah = 18;
        int ax = this.tosPopupX + (this.tosPopupW - aw) / 2;
        int ay = this.tosPopupY + this.tosPopupH - 26;
        boolean hoverAccept = mouseX >= ax && mouseX <= ax + aw && mouseY >= ay && mouseY <= ay + ah;
        int aba = (int)((float)(hoverAccept ? 130 : 80) * alpha);
        RenderUtil.drawGradientRect((float)ax, (float)ay, (float)(ax + aw), (float)(ay + ah), (new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), aba)).getRGB(), (new Color(Math.max(0, themeColor.getRed() - 30), Math.max(0, themeColor.getGreen() - 30), Math.max(0, themeColor.getBlue() - 30), aba)).getRGB());
        this.drawOutlineRect((float)ax, (float)ay, (float)(ax + aw), (float)(ay + ah), (new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), (int)(110.0F * alpha))).getRGB());
        String acceptTxt = this.tosAccepted ? "Accepted" : "Accept";
        Vonware.textManager.drawStringWithShadow(acceptTxt, (float)ax + (float)(aw - Vonware.textManager.getStringWidth(acceptTxt)) / 2.0F, (float)ay + (float)(ah - 8) / 2.0F, textWhite);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private void initVoidStars(int w, int h) {
        this.voidStars.clear();

        for(int i = 0; i < 200; ++i) {
            this.voidStars.add(new VoidStar(this.random.nextFloat() * (float)w, this.random.nextFloat() * (float)h, 0.3F + this.random.nextFloat() * 1.5F, this.random.nextFloat() * 255.0F, 0.5F + this.random.nextFloat() * 2.0F, this.random.nextFloat() * (float)Math.PI * 2.0F));
        }

    }

    private void drawVoidStars(int w, int h, float delta, float alpha) {
        if (!(alpha < 0.01F)) {
            GlStateManager.enableBlend();
            GlStateManager.disableTexture2D();
            GlStateManager.tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ZERO);
            Tessellator tess = Tessellator.getInstance();
            BufferBuilder buf = tess.getBuffer();

            for(VoidStar star : this.voidStars) {
                star.phase += delta * star.speed;
                float pulse = (float)(Math.sin((double)star.phase) * (double)0.5F + (double)0.5F);
                float sa = star.brightness * (0.3F + 0.7F * pulse) * alpha / 255.0F;
                if (!(sa < 0.02F)) {
                    float r;
                    float g;
                    float b;
                    if (star.size > 1.2F) {
                        r = 0.6F + 0.2F * pulse;
                        g = 0.75F + 0.2F * pulse;
                        b = 1.0F;
                    } else {
                        r = 0.85F + 0.15F * pulse;
                        g = 0.88F + 0.12F * pulse;
                        b = 0.95F + 0.05F * pulse;
                    }

                    DismantleUtil.drawFilledCircle(buf, tess, star.x, star.y, star.size, r, g, b, sa);
                }
            }

            GlStateManager.enableTexture2D();
        }
    }

    private void drawSixEyesRings(float cx, float cy, float delta, float alpha) {
        if (!(alpha < 0.05F)) {
            this.sixEyesRotation += delta * 15.0F;
            GlStateManager.enableBlend();
            GlStateManager.disableTexture2D();
            GlStateManager.tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE, SourceFactor.ONE, DestFactor.ZERO);
            Tessellator tess = Tessellator.getInstance();
            BufferBuilder buf = tess.getBuffer();
            float[] radii = new float[]{100.0F, 130.0F, 165.0F, 210.0F};
            float[] speeds = new float[]{1.0F, -0.7F, 0.5F, -0.35F};
            float[] alphas = new float[]{0.08F, 0.06F, 0.04F, 0.025F};
            int[] segs = new int[]{6, 8, 10, 12};

            for(int ring = 0; ring < radii.length; ++ring) {
                float rot = this.sixEyesRotation * speeds[ring];
                float segArc = (float)((Math.PI * 2D) / (double)segs[ring]);

                for(int seg = 0; seg < segs[ring]; ++seg) {
                    float sa = (float)Math.toRadians((double)rot) + (float)seg * segArc;
                    this.drawArc(buf, tess, cx, cy, radii[ring], sa, sa + segArc * 0.7F, 1.2F, 0.3F, 0.6F, 1.0F, alphas[ring] * alpha);
                }
            }

            float breathe = (float)(Math.sin((double)System.currentTimeMillis() * 0.002) * 0.3 + 0.7);

            for(float r = 60.0F; r > 0.0F; r -= 4.0F) {
                this.drawRing(buf, tess, cx, cy, r, r + 3.0F, 0.25F, 0.55F, 1.0F, 0.03F * alpha * breathe * (1.0F - r / 60.0F), 48);
            }

            GlStateManager.tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ZERO);
            GlStateManager.enableTexture2D();
        }
    }

    private void initBlueOrbs(int w, int h) {
        this.blueOrbs.clear();

        for(int i = 0; i < 3 + this.random.nextInt(2); ++i) {
            this.blueOrbs.add(new BlueOrb(30.0F + this.random.nextFloat() * (float)(w - 60), 30.0F + this.random.nextFloat() * (float)(h - 60), 0.3F + this.random.nextFloat() * 0.4F, this.random.nextFloat() * (float)Math.PI * 2.0F));
        }

    }

    private void updateAndDrawBlueOrbs(int w, int h, float delta, float alpha) {
        if (!(alpha < 0.05F)) {
            GlStateManager.enableBlend();
            GlStateManager.disableTexture2D();
            GlStateManager.tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE, SourceFactor.ONE, DestFactor.ZERO);
            Tessellator tess = Tessellator.getInstance();
            BufferBuilder buf = tess.getBuffer();

            for(BlueOrb orb : this.blueOrbs) {
                orb.phase += delta * orb.speed;
                orb.x += (float)(Math.cos((double)orb.phase * 0.7) * (double)8.0F * (double)delta);
                orb.y += (float)(Math.sin((double)orb.phase * (double)0.5F) * (double)6.0F * (double)delta);
                if (orb.x < -30.0F) {
                    orb.x = (float)(w + 30);
                }

                if (orb.x > (float)(w + 30)) {
                    orb.x = -30.0F;
                }

                if (orb.y < -30.0F) {
                    orb.y = (float)(h + 30);
                }

                if (orb.y > (float)(h + 30)) {
                    orb.y = -30.0F;
                }

                float pulse = (float)(Math.sin((double)(orb.phase * 2.0F)) * 0.3 + 0.7);

                for(float r = 25.0F; r > 0.0F; r -= 2.5F) {
                    DismantleUtil.drawFilledCircle(buf, tess, orb.x, orb.y, r, 0.1F, 0.4F, 1.0F, 0.04F * (1.0F - r / 25.0F) * pulse * alpha);
                }

                DismantleUtil.drawFilledCircle(buf, tess, orb.x, orb.y, 4.0F, 0.4F, 0.7F, 1.0F, 0.25F * pulse * alpha);
                DismantleUtil.drawFilledCircle(buf, tess, orb.x, orb.y, 2.0F, 0.7F, 0.9F, 1.0F, 0.375F * pulse * alpha);
            }

            GlStateManager.tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ZERO);
            GlStateManager.enableTexture2D();
        }
    }

    private void spawnCursedParticles(int w, int h, float cx, float cy) {
        if (this.cursedParticles.size() < 80 && this.random.nextFloat() < 0.3F) {
            float angle = this.random.nextFloat() * (float)Math.PI * 2.0F;
            float dist = 200.0F + this.random.nextFloat() * 150.0F;
            this.cursedParticles.add(new CursedParticle(Math.max(-20.0F, Math.min((float)(w + 20), cx + (float)Math.cos((double)angle) * dist)), Math.max(-20.0F, Math.min((float)(h + 20), cy + (float)Math.sin((double)angle) * dist)), 0.8F + this.random.nextFloat() * 1.5F, 3.0F + this.random.nextFloat() * 4.0F));
        }

    }

    private void updateAndDrawCursedParticles(float cx, float cy, float delta, float alpha) {
        if (!(alpha < 0.05F)) {
            GlStateManager.enableBlend();
            GlStateManager.disableTexture2D();
            GlStateManager.tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE, SourceFactor.ONE, DestFactor.ZERO);
            Tessellator tess = Tessellator.getInstance();
            BufferBuilder buf = tess.getBuffer();
            Iterator<CursedParticle> it = this.cursedParticles.iterator();

            while(it.hasNext()) {
                CursedParticle p = (CursedParticle)it.next();
                float dx = cx - p.x;
                float dy = cy - p.y;
                float dist = (float)Math.sqrt((double)(dx * dx + dy * dy));
                if (dist < 20.0F) {
                    it.remove();
                } else {
                    float sm = Math.min(1.0F, dist / 150.0F);
                    float angle = (float)Math.atan2((double)dy, (double)dx) + 0.3F * (1.0F - sm);
                    p.x += (float)Math.cos((double)angle) * 35.0F * sm * delta;
                    p.y += (float)Math.sin((double)angle) * 35.0F * sm * delta;
                    p.life -= delta * 0.15F;
                    if (p.life <= 0.0F) {
                        it.remove();
                    } else {
                        float pa = Math.min(1.0F, p.life) * 0.4F * alpha * sm;
                        float t = 1.0F - sm;
                        DismantleUtil.drawFilledCircle(buf, tess, p.x, p.y, p.size, 0.2F + 0.6F * t, 0.5F + 0.4F * t, 1.0F, pa);
                        DismantleUtil.drawFilledCircle(buf, tess, p.x, p.y, p.size * 0.4F, 0.8F, 0.9F, 1.0F, pa * 2.0F);
                    }
                }
            }

            GlStateManager.tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ZERO);
            GlStateManager.enableTexture2D();
        }
    }

    private void updateAndDrawInfinityRipples(float delta, float alpha) {
        if (!(alpha < 0.05F)) {
            GlStateManager.enableBlend();
            GlStateManager.disableTexture2D();
            GlStateManager.tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE, SourceFactor.ONE, DestFactor.ZERO);
            Tessellator tess = Tessellator.getInstance();
            BufferBuilder buf = tess.getBuffer();
            Iterator<InfinityRipple> it = this.infinityRipples.iterator();

            while(it.hasNext()) {
                InfinityRipple r = (InfinityRipple)it.next();
                r.radius += delta * 60.0F;
                r.life -= delta * 0.35F;
                if (r.life <= 0.0F) {
                    it.remove();
                } else {
                    this.drawRing(buf, tess, r.x, r.y, r.radius, r.radius + 1.0F, 0.3F, 0.6F, 1.0F, r.life * 0.12F * alpha, 64);
                }
            }

            GlStateManager.tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ZERO);
            GlStateManager.enableTexture2D();
        }
    }

    private void updateAndDrawPurpleStreaks(float delta, float alpha) {
        if (!this.purpleStreaks.isEmpty()) {
            GlStateManager.enableBlend();
            GlStateManager.disableTexture2D();
            GlStateManager.tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE, SourceFactor.ONE, DestFactor.ZERO);
            Tessellator tess = Tessellator.getInstance();
            BufferBuilder buf = tess.getBuffer();
            Iterator<PurpleStreak> it = this.purpleStreaks.iterator();

            while(it.hasNext()) {
                PurpleStreak s = (PurpleStreak)it.next();
                s.progress += delta * s.speed;
                s.life -= delta * 0.8F;
                if (s.life <= 0.0F) {
                    it.remove();
                } else {
                    float sa = Math.min(1.0F, s.life) * 0.15F * alpha;
                    float hx = s.startX + s.dirX * s.progress * 400.0F;
                    float hy = s.startY + s.dirY * s.progress * 400.0F;
                    float tx = hx - s.dirX * 80.0F;
                    float ty = hy - s.dirY * 80.0F;
                    this.drawLine(buf, tess, tx, ty, hx, hy, 2.0F, 0.6F, 0.15F, 1.0F, sa);
                    this.drawLine(buf, tess, tx, ty, hx, hy, 6.0F, 0.4F, 0.1F, 0.8F, sa * 0.3F);
                    this.drawLine(buf, tess, tx, ty, hx, hy, 14.0F, 0.35F, 0.05F, 0.6F, sa * 0.08F);
                }
            }

            GlStateManager.tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ZERO);
            GlStateManager.enableTexture2D();
        }
    }

    private ResourceLocation getPlayerSkinLocation() {
        if (!this.skinRequested) {
            this.skinRequested = true;
            GameProfile profile = this.mc.getSession().getProfile();
            this.playerSkinLocation = DefaultPlayerSkin.getDefaultSkin(EntityPlayer.getUUID(profile));
            this.mc.getSkinManager().loadProfileTextures(profile, (type, location, texture) -> {
                if (type == Type.SKIN) {
                    this.playerSkinLocation = location;
                }

            }, true);
        }

        return this.playerSkinLocation;
    }

    private void drawRing(BufferBuilder buf, Tessellator tess, float cx, float cy, float innerR, float outerR, float r, float g, float b, float a, int segs) {
        if (!(a < 0.005F)) {
            a = Math.min(1.0F, a);
            buf.begin(8, DefaultVertexFormats.POSITION_COLOR);

            for(int i = 0; i <= segs; ++i) {
                double ang = (Math.PI * 2D) * (double)i / (double)segs;
                float cos = (float)Math.cos(ang);
                float sin = (float)Math.sin(ang);
                buf.pos((double)(cx + cos * innerR), (double)(cy + sin * innerR), (double)0.0F).color(r, g, b, a).endVertex();
                buf.pos((double)(cx + cos * outerR), (double)(cy + sin * outerR), (double)0.0F).color(r, g, b, a * 0.3F).endVertex();
            }

            tess.draw();
        }
    }

    private void drawArc(BufferBuilder buf, Tessellator tess, float cx, float cy, float radius, float startAngle, float endAngle, float thickness, float r, float g, float b, float a) {
        if (!(a < 0.005F)) {
            a = Math.min(1.0F, a);
            int steps = 20;
            float step = (endAngle - startAngle) / (float)steps;
            buf.begin(8, DefaultVertexFormats.POSITION_COLOR);

            for(int i = 0; i <= steps; ++i) {
                float ang = startAngle + step * (float)i;
                float cos = (float)Math.cos((double)ang);
                float sin = (float)Math.sin((double)ang);
                float ed = (float)Math.min(i, steps - i) / (float)steps;
                float sa = a * (ed < 0.15F ? ed / 0.15F : 1.0F);
                buf.pos((double)(cx + cos * (radius - thickness)), (double)(cy + sin * (radius - thickness)), (double)0.0F).color(r, g, b, sa * 0.5F).endVertex();
                buf.pos((double)(cx + cos * (radius + thickness)), (double)(cy + sin * (radius + thickness)), (double)0.0F).color(r, g, b, sa).endVertex();
            }

            tess.draw();
        }
    }

    private void drawLine(BufferBuilder buf, Tessellator tess, float x1, float y1, float x2, float y2, float width, float r, float g, float b, float a) {
        if (!(a < 0.005F)) {
            a = Math.min(1.0F, a);
            float dx = x2 - x1;
            float dy = y2 - y1;
            float len = (float)Math.sqrt((double)(dx * dx + dy * dy));
            if (!(len < 0.01F)) {
                float nx = -dy / len * width * 0.5F;
                float ny = dx / len * width * 0.5F;
                buf.begin(7, DefaultVertexFormats.POSITION_COLOR);
                buf.pos((double)(x1 + nx), (double)(y1 + ny), (double)0.0F).color(r, g, b, 0.0F).endVertex();
                buf.pos((double)(x1 - nx), (double)(y1 - ny), (double)0.0F).color(r, g, b, 0.0F).endVertex();
                buf.pos((double)(x2 - nx), (double)(y2 - ny), (double)0.0F).color(r, g, b, a).endVertex();
                buf.pos((double)(x2 + nx), (double)(y2 + ny), (double)0.0F).color(r, g, b, a).endVertex();
                tess.draw();
            }
        }
    }

    private void drawInfoPair(String label, String value, float x, float y, int lc, int vc) {
        String full = label + ": ";
        Vonware.textManager.drawStringWithShadow(full, x, y, lc);
        Vonware.textManager.drawStringWithShadow(value, x + (float)Vonware.textManager.getStringWidth(full), y, vc);
    }

    private void drawSeparator(float x1, float x2, float y, Color tc, float alpha) {
        float mid = (x1 + x2) / 2.0F;
        int ca = (int)(70.0F * alpha);
        int c = (new Color(tc.getRed(), tc.getGreen(), tc.getBlue(), ca)).getRGB();
        int t = (new Color(tc.getRed(), tc.getGreen(), tc.getBlue(), 0)).getRGB();
        RenderUtil.drawGradientSideways(x1, y, mid, y + 0.5F, t, c);
        RenderUtil.drawGradientSideways(mid, y, x2, y + 0.5F, c, t);
    }

    private void drawPanelGlow(int px, int py, int pw, int ph, Color tc, float alpha) {
        int gs = 10;
        int ga = (int)(20.0F * alpha);
        Color g = new Color(tc.getRed(), tc.getGreen(), tc.getBlue(), ga);
        Color t = new Color(tc.getRed(), tc.getGreen(), tc.getBlue(), 0);
        Color c = new Color(tc.getRed(), tc.getGreen(), tc.getBlue(), (int)(10.0F * alpha));
        RenderUtil.drawGradientRect((float)px, (float)(py - gs), (float)(px + pw), (float)py, t.getRGB(), g.getRGB());
        RenderUtil.drawGradientRect((float)px, (float)(py + ph), (float)(px + pw), (float)(py + ph + gs), g.getRGB(), t.getRGB());
        RenderUtil.drawGradientSideways((float)(px - gs), (float)py, (float)px, (float)(py + ph), t.getRGB(), g.getRGB());
        RenderUtil.drawGradientSideways((float)(px + pw), (float)py, (float)(px + pw + gs), (float)(py + ph), g.getRGB(), t.getRGB());
        RenderUtil.drawGradientRect((float)(px - gs), (float)(py - gs), (float)px, (float)py, t.getRGB(), c.getRGB());
        RenderUtil.drawGradientRect((float)(px + pw), (float)(py - gs), (float)(px + pw + gs), (float)py, t.getRGB(), c.getRGB());
        RenderUtil.drawGradientRect((float)(px - gs), (float)(py + ph), (float)px, (float)(py + ph + gs), c.getRGB(), t.getRGB());
        RenderUtil.drawGradientRect((float)(px + pw), (float)(py + ph), (float)(px + pw + gs), (float)(py + ph + gs), c.getRGB(), t.getRGB());
    }

    private Color getThemeColor() {
        if ((Boolean)Colors.getInstance().rainbow.getValue()) {
            return Colors.getInstance().rainbowMode.getValue() == Colors.RainbowMode.Static ? ColorUtil.rainbow(0) : ColorUtil.rainbow((int)((float)NewComponent.counter1[0] * (Float)Colors.getInstance().rainbowHue.getValue()));
        } else {
            return (Boolean)ClickGui.getInstance().pulseGui.getValue() ? Global.getPulseColor(Colors.getInstance().getGuiColor(), 0) : Colors.getInstance().getGuiColor();
        }
    }

    private void drawOutlineRect(float x1, float y1, float x2, float y2, int color) {
        RenderUtil.drawRect(x1, y1, x2, y1 + 0.5F, color);
        RenderUtil.drawRect(x1, y2 - 0.5F, x2, y2, color);
        RenderUtil.drawRect(x1, y1, x1 + 0.5F, y2, color);
        RenderUtil.drawRect(x2 - 0.5F, y1, x2, y2, color);
    }

    private float easeOutCubic(float t) {
        return 1.0F - (float)Math.pow((double)(1.0F - t), (double)3.0F);
    }

    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseButton == 0) {
            if (this.dismantle.exitAnimating) {
                return;
            }

            if (this.showTosPopup) {
                this.tosScrollBar.mouseClicked(mouseX, mouseY, (float)(this.tosPopupX + this.tosPopupW - 6), (float)(this.tosPopupY + 26), (float)(this.tosPopupH - 50), (float)(TOS_LINES.length * 10), (float)(this.tosPopupH - 50));
                if (mouseX >= this.tosPopupCloseX && mouseX <= this.tosPopupCloseX + 12 && mouseY >= this.tosPopupCloseY && mouseY <= this.tosPopupCloseY + 12) {
                    this.showTosPopup = false;
                    return;
                }

                int aw = 100;
                int ah = 18;
                int ax = this.tosPopupX + (this.tosPopupW - aw) / 2;
                int ay = this.tosPopupY + this.tosPopupH - 26;
                if (mouseX >= ax && mouseX <= ax + aw && mouseY >= ay && mouseY <= ay + ah) {
                    this.tosAccepted = true;
                    this.showTosPopup = false;
                    return;
                }

                if (mouseX < this.tosPopupX || mouseX > this.tosPopupX + this.tosPopupW || mouseY < this.tosPopupY || mouseY > this.tosPopupY + this.tosPopupH) {
                    this.showTosPopup = false;
                }

                return;
            }

            if (mouseX >= this.tosCheckX && mouseX <= this.tosCheckX + this.tosCheckSize && mouseY >= this.tosCheckY && mouseY <= this.tosCheckY + this.tosCheckSize) {
                this.tosAccepted = !this.tosAccepted;
                return;
            }

            if (mouseX >= this.tosViewX && mouseX <= this.tosViewX + this.tosViewW && mouseY >= this.tosViewY && mouseY <= this.tosViewY + this.tosViewH) {
                this.showTosPopup = true;
                this.tosScrollBar.resetScroll();
                return;
            }

            if ((!isFirstLaunch() || this.tosAccepted) && mouseX >= this.buttonX && mouseX <= this.buttonX + this.buttonW && mouseY >= this.buttonY && mouseY <= this.buttonY + this.buttonH) {
                this.dismantle.exitAnimating = true;
                SoundUtil.playDismantleSay();
                ThreadUtil.startDaemonDelayed("Dismantle-1", 550L, () -> SoundUtil.playDismantle(1.0F));
                ThreadUtil.startDaemonDelayed("Dismantle-2", 618L, () -> SoundUtil.playDismantle(0.92F));
                ThreadUtil.startDaemonDelayed("Dismantle-3", 680L, () -> SoundUtil.playDismantle(1.08F));
                ThreadUtil.startDaemonDelayed("Dismantle-4", 770L, () -> SoundUtil.playDismantle(0.96F));
                ThreadUtil.startDaemonDelayed("Dismantle-5", 882L, () -> SoundUtil.playDismantle(1.05F));
            }
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    protected void mouseReleased(int mouseX, int mouseY, int state) {
        this.tosScrollBar.mouseReleased();
        super.mouseReleased(mouseX, mouseY, state);
    }

    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (this.showTosPopup && !this.dismantle.exitAnimating) {
            this.tosScrollBar.mouseDragged(mouseY, (float)(this.tosPopupH - 50), (float)(TOS_LINES.length * 10), (float)(this.tosPopupH - 50));
        }

        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
    }

    public void handleMouseInput() throws IOException {
        if (this.showTosPopup && !this.dismantle.exitAnimating) {
            int wheel = Mouse.getEventDWheel();
            if (wheel != 0) {
                this.tosScrollBar.scroll(wheel > 0 ? 1 : -1, (float)(TOS_LINES.length * 10), (float)(this.tosPopupH - 50));
            }
        }

        super.handleMouseInput();
    }

    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == 1 && this.showTosPopup && !this.dismantle.exitAnimating) {
            this.showTosPopup = false;
        }

    }

    public boolean doesGuiPauseGame() {
        return false;
    }

    private static class VoidStar {
        float x;
        float y;
        float size;
        float brightness;
        float speed;
        float phase;

        VoidStar(float x, float y, float size, float brightness, float speed, float phase) {
            this.x = x;
            this.y = y;
            this.size = size;
            this.brightness = brightness;
            this.speed = speed;
            this.phase = phase;
        }
    }

    private static class CursedParticle {
        float x;
        float y;
        float size;
        float life;

        CursedParticle(float x, float y, float size, float life) {
            this.x = x;
            this.y = y;
            this.size = size;
            this.life = life;
        }
    }

    private static class BlueOrb {
        float x;
        float y;
        float speed;
        float phase;

        BlueOrb(float x, float y, float speed, float phase) {
            this.x = x;
            this.y = y;
            this.speed = speed;
            this.phase = phase;
        }
    }

    private static class PurpleStreak {
        float startX;
        float startY;
        float dirX;
        float dirY;
        float progress;
        float life;
        float speed;

        PurpleStreak(int sw, int sh, Random rand) {
            int edge = rand.nextInt(4);
            switch (edge) {
                case 0:
                    this.startX = -20.0F;
                    this.startY = rand.nextFloat() * (float)sh;
                    break;
                case 1:
                    this.startX = (float)(sw + 20);
                    this.startY = rand.nextFloat() * (float)sh;
                    break;
                case 2:
                    this.startX = rand.nextFloat() * (float)sw;
                    this.startY = -20.0F;
                    break;
                default:
                    this.startX = rand.nextFloat() * (float)sw;
                    this.startY = (float)(sh + 20);
            }

            float tx = (float)sw / 2.0F + (rand.nextFloat() - 0.5F) * (float)sw * 0.6F;
            float ty = (float)sh / 2.0F + (rand.nextFloat() - 0.5F) * (float)sh * 0.6F;
            float dx = tx - this.startX;
            float dy = ty - this.startY;
            float len = (float)Math.sqrt((double)(dx * dx + dy * dy));
            this.dirX = dx / len;
            this.dirY = dy / len;
            this.progress = 0.0F;
            this.life = 1.0F + rand.nextFloat() * 0.5F;
            this.speed = 1.5F + rand.nextFloat();
        }
    }

    private static class InfinityRipple {
        float x;
        float y;
        float radius;
        float life;

        InfinityRipple(float x, float y) {
            this.x = x;
            this.y = y;
            this.radius = 5.0F;
            this.life = 1.0F;
        }
    }
}
