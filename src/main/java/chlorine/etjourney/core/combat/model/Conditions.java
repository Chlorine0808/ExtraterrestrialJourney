package chlorine.etjourney.core.combat.model;

import java.util.Arrays;
import java.util.List;

import chlorine.etjourney.core.damage.model.DamageKind;
import chlorine.etjourney.core.damage.model.Reach;

/** The condition parts and the ways to combine them. */
public final class Conditions {

    public static final Condition ALWAYS = s -> true;

    private Conditions() {}

    public static Condition night() {
        return Situation::isNight;
    }

    public static Condition healthAtMost(float fraction) {
        return s -> s.healthFraction() <= fraction;
    }

    public static Condition healthAtLeast(float fraction) {
        return s -> s.healthFraction() >= fraction;
    }

    public static Condition reach(Reach reach) {
        return s -> s.attack().reach == reach;
    }

    public static Condition kind(DamageKind kind) {
        return s -> s.attack().kind == kind;
    }

    public static Condition all(Condition... parts) {
        List<Condition> list = Arrays.asList(parts);
        return s -> list.stream()
            .allMatch(c -> c.test(s));
    }

    public static Condition any(Condition... parts) {
        List<Condition> list = Arrays.asList(parts);
        return s -> list.stream()
            .anyMatch(c -> c.test(s));
    }

    public static Condition not(Condition part) {
        return s -> !part.test(s);
    }
}
