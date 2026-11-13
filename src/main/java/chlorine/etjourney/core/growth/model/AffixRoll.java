package chlorine.etjourney.core.growth.model;

/** One affix on a piece of gear: its id and where in its range it rolled, from 0 to 1. */
public final class AffixRoll {

    public final String id;
    public final float roll;

    public AffixRoll(String id, float roll) {
        this.id = id;
        this.roll = roll;
    }
}
