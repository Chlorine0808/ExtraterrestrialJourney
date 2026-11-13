package chlorine.etjourney.content.area;

import chlorine.etjourney.content.projectile.Faction;

/** Whom an area works on: as its faction's shots would hit, plus fellow mobs when it takes allies along. */
public final class AreaTargets {

    private AreaTargets() {}

    public static boolean affects(Faction faction, boolean hitsAllies, boolean targetIsPlayer, boolean targetIsOwner) {
        if (targetIsOwner) return false;
        return faction.hits(targetIsPlayer, false) || hitsAllies;
    }
}
