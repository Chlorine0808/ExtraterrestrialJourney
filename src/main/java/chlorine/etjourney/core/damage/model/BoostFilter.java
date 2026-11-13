package chlorine.etjourney.core.damage.model;

/** Which attacks a boost applies to; each axis is either unset, matching anything, or one value. */
public final class BoostFilter {

    /** Matches every attack. */
    public static final BoostFilter ALL = new BoostFilter(null, null, null, null, null);

    private final Element element;
    private final DamageKind kind;
    private final Reach reach;
    private final WeaponClass weapon;
    private final Boolean summoned;

    private BoostFilter(Element element, DamageKind kind, Reach reach, WeaponClass weapon, Boolean summoned) {
        this.element = element;
        this.kind = kind;
        this.reach = reach;
        this.weapon = weapon;
        this.summoned = summoned;
    }

    public static BoostFilter element(Element element) {
        return new BoostFilter(element, null, null, null, null);
    }

    public BoostFilter kind(DamageKind value) {
        return new BoostFilter(element, value, reach, weapon, summoned);
    }

    public BoostFilter reach(Reach value) {
        return new BoostFilter(element, kind, value, weapon, summoned);
    }

    public BoostFilter weapon(WeaponClass value) {
        return new BoostFilter(element, kind, reach, value, summoned);
    }

    public BoostFilter summoned(boolean value) {
        return new BoostFilter(element, kind, reach, weapon, value);
    }

    public boolean matches(Element e, AttackProfile attack) {
        return (element == null || element.equals(e)) && (kind == null || kind == attack.kind)
            && (reach == null || reach == attack.reach)
            && (weapon == null || weapon == attack.weapon)
            && (summoned == null || summoned == attack.summoned);
    }
}
