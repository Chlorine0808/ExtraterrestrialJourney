package chlorine.etjourney.content.end.spawncheck;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import chlorine.etjourney.content.rank.ability.BlastAbility;

/** blast: a plain zombie beside the corpse is hurt by the blast, and an item lying there is not lost. */
final class BlastTrial extends AbilityTrial {

    private EntityLiving neighbour;
    private EntityItem item;
    private float before;

    BlastTrial() {
        super(BlastAbility.ID);
    }

    @Override
    boolean step(Ctx c, int tick) {
        if (tick == 1) {
            EntityLiving mob = c.spawn(5, 0, BlastAbility.ID);
            neighbour = c.plain(5, 1);
            TrialSupport.harmless(neighbour);
            before = neighbour.getHealth();
            item = new EntityItem(c.end, mob.posX, mob.posY + 0.5, mob.posZ, new ItemStack(Items.stick));
            c.end.spawnEntityInWorld(item);
            c.made.add(item);
            mob.attackEntityFrom(c.byPlayer(), 1000);
            return false;
        }
        if (tick < 45) return false;
        c.verdict(neighbour.getHealth() < before, name + ": the neighbouring zombie was hurt");
        c.verdict(!item.isDead, name + ": the item lying there survived");
        return true;
    }
}
