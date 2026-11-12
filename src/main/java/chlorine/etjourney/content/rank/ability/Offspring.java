package chlorine.etjourney.content.rank.ability;

import java.util.Collections;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.world.WorldServer;

import chlorine.etjourney.content.rank.Rank;
import chlorine.etjourney.content.rank.RankData;

/** Makes the unranked mobs an ability brings into the world. */
final class Offspring {

    private Offspring() {}

    /** A new mob of the parent's kind, not yet in the world; null when the kind cannot be made by name. */
    static EntityLiving sameKind(EntityLiving parent) {
        String name = EntityList.getEntityString(parent);
        Entity e = name == null ? null : EntityList.createEntityByName(name, parent.worldObj);
        return e instanceof EntityLiving ? (EntityLiving) e : null;
    }

    /** Marks the child as rolled and unranked, so it never rolls a rank of its own. */
    static void bind(EntityLiving child, EntityLiving parent, int role) {
        RankData.set(child, Rank.NORMAL, Collections.emptyList());
        RankData.setOffspring(child, role, parent.getUniqueID());
    }

    /** Server-side particles that every nearby player sees. */
    static void puff(EntityLiving at, String particle, int count) {
        if (!(at.worldObj instanceof WorldServer)) return;
        ((WorldServer) at.worldObj).func_147487_a(
            particle,
            at.posX,
            at.posY + at.height / 2,
            at.posZ,
            count,
            at.width / 2,
            at.height / 2,
            at.width / 2,
            0.02);
    }
}
