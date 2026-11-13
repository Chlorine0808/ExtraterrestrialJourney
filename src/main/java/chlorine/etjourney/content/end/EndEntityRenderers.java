package chlorine.etjourney.content.end;

import net.minecraft.client.model.ModelZombie;
import net.minecraft.client.renderer.entity.RenderBiped;
import net.minecraft.entity.EntityLiving;
import net.minecraft.util.ResourceLocation;

import chlorine.etjourney.content.entity.EntityRenderers;
import chlorine.etjourney.content.worm.RenderWorm;
import chlorine.etjourney.core.ModInfo;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Client-side renderers of the End's mobs. */
@SideOnly(Side.CLIENT)
public final class EndEntityRenderers {

    private EndEntityRenderers() {}

    public static void init() {
        EntityRenderers.bind(EntitySporeZombie.class, new TexturedBiped("spore_zombie"));
        EntityRenderers.bind(
            EntityFlameWorm.class,
            new RenderWorm(vanillaBlock("glowstone"), vanillaBlock("netherrack"), vanillaBlock("nether_brick")));
    }

    private static ResourceLocation vanillaBlock(String name) {
        return new ResourceLocation("textures/blocks/" + name + ".png");
    }

    /** A zombie-armed biped wearing one texture from textures/entity. */
    private static final class TexturedBiped extends RenderBiped {

        private final ResourceLocation texture;

        TexturedBiped(String name) {
            super(new ModelZombie(), 0.5F);
            texture = new ResourceLocation(ModInfo.MODID, "textures/entity/" + name + ".png");
        }

        @Override
        protected ResourceLocation getEntityTexture(EntityLiving entity) {
            return texture;
        }
    }
}
