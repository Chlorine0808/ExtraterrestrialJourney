package chlorine.etjourney.core.damage.model;

/** Tells the weapon class and reach of an attack from game-free facts about it. */
public final class WeaponClassifier {

    /** What touched the target: nothing known, the attacker itself, an arrow, or anything else. */
    public enum Direct {
        NONE,
        SELF,
        ARROW,
        OTHER
    }

    /** The kind of item in the attacker's hand, by vanilla class. */
    public enum Held {
        NONE,
        SWORD,
        AXE,
        TOOL,
        BOW,
        OTHER
    }

    /** Declared may be null; it is the class an item claims for itself. */
    public static final class Facts {

        final Direct direct;
        final Held held;
        final WeaponClass declared;
        final boolean magic;
        final boolean projectile;

        public Facts(Direct direct, Held held, WeaponClass declared, boolean magic, boolean projectile) {
            this.direct = direct;
            this.held = held;
            this.declared = declared;
            this.magic = magic;
            this.projectile = projectile;
        }
    }

    private WeaponClassifier() {}

    public static WeaponClass weapon(Facts f) {
        if (f.direct == Direct.ARROW) return WeaponClass.BOW;
        if (f.direct == Direct.SELF) return f.declared != null ? f.declared : held(f.held);
        if (f.direct == Direct.NONE) return WeaponClass.OTHER;
        if (f.magic) return WeaponClass.SPELL;
        if (f.projectile) return WeaponClass.THROWN;
        return WeaponClass.OTHER;
    }

    public static Reach reach(Facts f) {
        return f.direct == Direct.SELF ? Reach.MELEE : Reach.RANGED;
    }

    private static WeaponClass held(Held held) {
        switch (held) {
            case SWORD:
                return WeaponClass.SWORD;
            case AXE:
                return WeaponClass.AXE;
            case TOOL:
                return WeaponClass.TOOL;
            case NONE:
                return WeaponClass.UNARMED;
            default:
                return WeaponClass.OTHER_MELEE;
        }
    }
}
