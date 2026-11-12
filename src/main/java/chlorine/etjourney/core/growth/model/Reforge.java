package chlorine.etjourney.core.growth.model;

import java.util.List;
import java.util.Random;

/** Draws a piece's affixes again; its rarity and experience stay. */
public final class Reforge {

    private Reforge() {}

    /** A null piece has not been rolled yet, so its rarity is drawn as well. */
    public static GrowthData reroll(GrowthData piece, List<AffixDraw.Candidate> pool, Random random) {
        if (piece == null) return GrowthData.roll(pool, random);
        Rarity r = piece.rarity;
        return piece.withAffixes(AffixDraw.draw(pool, r.affixes, r.floor, random));
    }
}
