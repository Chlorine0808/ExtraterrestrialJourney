package chlorine.etjourney.core.damage.model;

/** The shape of one attack: its kind, reach and weapon class, and whether a summoned creature made it. */
public final class AttackProfile {

    /** An attack nothing is known about: physical, ranged, other, not summoned. */
    public static final AttackProfile UNKNOWN = new AttackProfile(
        DamageKind.PHYSICAL,
        Reach.RANGED,
        WeaponClass.OTHER,
        false);

    public final DamageKind kind;
    public final Reach reach;
    public final WeaponClass weapon;
    public final boolean summoned;

    public AttackProfile(DamageKind kind, Reach reach, WeaponClass weapon, boolean summoned) {
        this.kind = kind;
        this.reach = reach;
        this.weapon = weapon;
        this.summoned = summoned;
    }

    @Override
    public String toString() {
        return kind + "/" + reach + "/" + weapon + (summoned ? "/summoned" : "");
    }
}
