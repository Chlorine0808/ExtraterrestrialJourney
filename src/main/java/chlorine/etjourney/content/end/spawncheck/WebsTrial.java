package chlorine.etjourney.content.end.spawncheck;

import net.minecraft.entity.EntityLiving;
import net.minecraft.util.MathHelper;

import chlorine.etjourney.content.rank.ability.RankShots;
import chlorine.etjourney.content.rank.ability.RankWebBlock;
import chlorine.etjourney.content.rank.ability.WebsAbility;

/** webs: the mob fires a web shot, and a web set in the air by hand is gone after 100 ticks. */
final class WebsTrial extends AbilityTrial {

    private static final int CHECK_AT = 125;
    private boolean fired, placed;
    private int x, y, z;

    WebsTrial() {
        super(WebsAbility.ID);
    }

    @Override
    boolean step(Ctx c, int tick) {
        if (tick == 1) {
            EntityLiving mob = c.spawn(6, 0, WebsAbility.ID);
            TrialSupport.harmless(mob);
            mob.setAttackTarget(c.player);
            x = MathHelper.floor_double(c.player.posX);
            z = MathHelper.floor_double(c.player.posZ) - 6;
            y = MathHelper.floor_double(c.player.posY);
            placed = c.end.isAirBlock(x, y, z) && c.end.setBlock(x, y, z, RankWebBlock.instance());
            return false;
        }
        if (TrialSupport.shotExists(c.end, RankShots.WEB_SHOT.name)) fired = true;
        if (tick == 50)
            c.verdict(placed && c.end.getBlock(x, y, z) == RankWebBlock.instance(), name + ": the web stands");
        if (tick < CHECK_AT) return false;
        c.verdict(placed && c.end.isAirBlock(x, y, z), name + ": the placed web faded after 100 ticks");
        c.verdict(fired, name + ": a web shot was fired");
        return true;
    }
}
