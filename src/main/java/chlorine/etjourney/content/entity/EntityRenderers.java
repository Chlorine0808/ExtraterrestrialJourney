package chlorine.etjourney.content.entity;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;

import chlorine.etjourney.core.util.ModLog;
import cpw.mods.fml.client.registry.RenderingRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Client-side renderer registration for mod entities; only the client proxy calls it. */
@SideOnly(Side.CLIENT)
public final class EntityRenderers {

    private static final Set<Class<?>> RENDERED = new HashSet<>();

    private EntityRenderers() {}

    public static void bind(Class<? extends Entity> type, Render render) {
        RenderingRegistry.registerEntityRenderingHandler(type, render);
        RENDERED.add(type);
    }

    /** Warns about entities left without a renderer, which would show up invisible. */
    public static void init() {
        List<String> missing = ModEntities.TABLE.withoutRenderer(RENDERED);
        if (!missing.isEmpty()) ModLog.LOG.warn("Entities without a renderer: {}", missing);
    }
}
