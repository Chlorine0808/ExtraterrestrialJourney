package chlorine.etjourney.content.rank.client;

import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.common.MinecraftForge;

import org.lwjgl.opengl.GL11;

import chlorine.etjourney.content.rank.Rank;
import chlorine.etjourney.content.rank.RankViews;
import chlorine.etjourney.core.ModInfo;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/** Shows ranks on the client: a coloured name over the mob and motes of light around its body. */
public final class RankClient {

    private static final double NAME_RANGE = 24, MOTE_RANGE = 32;
    /** Ticks a view waits for its entity before it is dropped; the packet may beat the spawn. */
    private static final int MISSING_TICKS = 100;
    private static final float LABEL_SCALE = 0.016666668F * 1.6F, SMALL = 0.75F;

    private final Random random = new Random();
    private World lastWorld;
    private int ticks;

    private RankClient() {}

    public static void init() {
        RankClient client = new RankClient();
        MinecraftForge.EVENT_BUS.register(client);
        FMLCommonHandler.instance()
            .bus()
            .register(client);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        World world = mc.theWorld;
        if (world != lastWorld) {
            RankViews.VIEWS.clear();
            lastWorld = world;
        }
        if (world == null || mc.isGamePaused()) return;
        ticks++;
        Iterator<Map.Entry<Integer, RankViews.View>> it = RankViews.VIEWS.entrySet()
            .iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, RankViews.View> entry = it.next();
            Entity e = world.getEntityByID(entry.getKey());
            RankViews.View view = entry.getValue();
            if (e == null || e.isDead) {
                if (++view.missing > MISSING_TICKS) it.remove();
                continue;
            }
            view.missing = 0;
            if (mc.thePlayer != null && e.getDistanceToEntity(mc.thePlayer) < MOTE_RANGE) mote(world, e, view.rank);
        }
    }

    /** Rare mobs give a mote every other tick, higher ranks every tick. */
    private void mote(World world, Entity e, Rank rank) {
        if (rank == Rank.NORMAL || rank == Rank.RARE && ticks % 2 != 0) return;
        double x = e.posX + (random.nextDouble() - 0.5) * e.width * 1.2;
        double y = e.boundingBox.minY + random.nextDouble() * e.height;
        double z = e.posZ + (random.nextDouble() - 0.5) * e.width * 1.2;
        // reddust reads its colour from the velocity arguments, and zero means full red.
        world.spawnParticle("reddust", x, y, z, part(rank.rgb >> 16), part(rank.rgb >> 8), part(rank.rgb));
    }

    private static double part(int channel) {
        return Math.max(0.01, (channel & 0xFF) / 255.0);
    }

    /** Lowest and never on a cancelled render, so Post always pops what Pre pushed. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onRenderPre(RenderLivingEvent.Pre event) {
        RankViews.View view = RankViews.get(event.entity.getEntityId());
        if (view == null || !view.small) return;
        GL11.glPushMatrix();
        GL11.glTranslated(event.x, event.y, event.z);
        GL11.glScalef(SMALL, SMALL, SMALL);
        GL11.glTranslated(-event.x, -event.y, -event.z);
    }

    @SubscribeEvent
    public void onRenderPost(RenderLivingEvent.Post event) {
        RankViews.View view = RankViews.get(event.entity.getEntityId());
        if (view != null && view.small) GL11.glPopMatrix();
    }

    @SubscribeEvent
    public void onSpecials(RenderLivingEvent.Specials.Post event) {
        RankViews.View view = RankViews.get(event.entity.getEntityId());
        if (view == null || view.rank == Rank.NORMAL) return;
        EntityLivingBase e = event.entity;
        Minecraft mc = Minecraft.getMinecraft();
        RenderManager rm = RenderManager.instance;
        if (!Minecraft.isGuiEnabled() || e.isInvisibleToPlayer(mc.thePlayer) || e.riddenByEntity != null) return;
        if (rm.livingPlayer == null || e.getDistanceSqToEntity(rm.livingPlayer) > NAME_RANGE * NAME_RANGE) return;
        String rank = I18n.format(
            ModInfo.MODID + ".rank."
                + view.rank.name()
                    .toLowerCase(Locale.ROOT));
        String text = "§" + view.rank.color + rank + " " + e.getCommandSenderName();
        // Above the vanilla name tag when the mob has one.
        float lift = e.height + 0.5F + (e instanceof EntityLiving && ((EntityLiving) e).hasCustomNameTag() ? 0.3F : 0);
        label(rm, text, event.x, event.y + lift, event.z);
    }

    private static void label(RenderManager rm, String text, double x, double y, double z) {
        FontRenderer font = rm.getFontRenderer();
        if (font == null) return;
        GL11.glPushMatrix();
        GL11.glTranslated(x, y, z);
        GL11.glNormal3f(0.0F, 1.0F, 0.0F);
        GL11.glRotatef(-rm.playerViewY, 0.0F, 1.0F, 0.0F);
        GL11.glRotatef(rm.playerViewX, 1.0F, 0.0F, 0.0F);
        GL11.glScalef(-LABEL_SCALE, -LABEL_SCALE, LABEL_SCALE);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDepthMask(false);
        GL11.glEnable(GL11.GL_BLEND);
        OpenGlHelper.glBlendFunc(770, 771, 1, 0);
        int half = font.getStringWidth(text) / 2;
        Tessellator t = Tessellator.instance;
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        t.startDrawingQuads();
        t.setColorRGBA_F(0.0F, 0.0F, 0.0F, 0.25F);
        t.addVertex(-half - 1, -1.0D, 0.0D);
        t.addVertex(-half - 1, 8.0D, 0.0D);
        t.addVertex(half + 1, 8.0D, 0.0D);
        t.addVertex(half + 1, -1.0D, 0.0D);
        t.draw();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDepthMask(true);
        font.drawString(text, -half, 0, -1);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glPopMatrix();
    }
}
