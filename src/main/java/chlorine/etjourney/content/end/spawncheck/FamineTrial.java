package chlorine.etjourney.content.end.spawncheck;

import chlorine.etjourney.content.rank.Rank;
import chlorine.etjourney.content.rank.ability.FamineAbility;

/** famine: with the mob near, the player's health does not rise on healing. */
final class FamineTrial extends AbilityTrial {

    FamineTrial() {
        super(FamineAbility.ID);
    }

    @Override
    boolean step(Ctx c, int tick) {
        if (tick == 1) {
            c.spawn(Rank.EPIC, 4, 0, FamineAbility.ID);
            return false;
        }
        if (tick < 25) return false;
        c.player.setHealth(10);
        c.player.heal(4);
        c.verdict(c.player.getHealth() == 10, name + ": no healing while marked (10 -> " + c.player.getHealth() + ")");
        return true;
    }
}
