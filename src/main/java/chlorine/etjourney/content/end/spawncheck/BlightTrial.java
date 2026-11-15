package chlorine.etjourney.content.end.spawncheck;

import net.minecraft.potion.Potion;

import chlorine.etjourney.content.rank.ability.BlightAbility;

/** blight: a player within 6 blocks of the mob has Weakness within a few seconds. */
final class BlightTrial extends AbilityTrial {

    BlightTrial() {
        super(BlightAbility.ID);
    }

    @Override
    boolean step(Ctx c, int tick) {
        if (tick == 1) {
            c.spawn(4, 0, BlightAbility.ID);
            return false;
        }
        if (tick < 70) return false;
        c.verdict(c.player.isPotionActive(Potion.weakness), name + ": the player has Weakness");
        return true;
    }
}
