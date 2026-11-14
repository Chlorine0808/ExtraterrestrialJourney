package chlorine.etjourney.content.rank.ability;

import java.lang.reflect.Method;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;

import chlorine.etjourney.content.rank.RankData;
import cpw.mods.fml.relauncher.ReflectionHelper;

/** Scales the hitbox of a mob from the size it was born with, kept with the ability setting. */
final class Resize {

    private static Method setSize;

    private Resize() {}

    /** Safe to call again: the base size is saved on the first call, so a reload does not scale twice. */
    static void apply(EntityLiving self, String ability, float factor) {
        String saved = RankData.setting(self, ability);
        if (saved.isEmpty()) {
            saved = self.width + "," + self.height;
            RankData.setSetting(self, ability, saved);
        }
        float[] base = parse(saved);
        try {
            if (setSize == null) setSize = ReflectionHelper
                .findMethod(Entity.class, null, new String[] { "func_70105_a", "setSize" }, float.class, float.class);
            setSize.invoke(self, base[0] * factor, base[1] * factor);
        } catch (ReflectiveOperationException | RuntimeException e) {
            // Without a resize the mob keeps its size; the render scale still applies.
        }
    }

    static float[] parse(String saved) {
        int comma = saved.indexOf(',');
        return new float[] { Float.parseFloat(saved.substring(0, comma)),
            Float.parseFloat(saved.substring(comma + 1)) };
    }
}
