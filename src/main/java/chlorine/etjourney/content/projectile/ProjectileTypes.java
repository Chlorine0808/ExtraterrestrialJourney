package chlorine.etjourney.content.projectile;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.potion.Potion;

import chlorine.etjourney.content.combat.Elements;
import chlorine.etjourney.core.damage.model.DamageKind;
import chlorine.etjourney.core.damage.model.Element;
import chlorine.etjourney.core.damage.model.ElementRegistry;
import chlorine.etjourney.core.damage.model.ElementalDamage;

/** The table of shots by name; the client finds a shot's spec here from the name it is sent. */
public final class ProjectileTypes {

    private static final Map<String, ProjectileSpec> BY_NAME = new LinkedHashMap<>();

    /** HIGHLAND's thrown rock, provisional until the rock thrower exists. */
    public static final ProjectileSpec TRIAL_ROCK = register(
        ProjectileSpec.builder("trial_rock", damage(DamageKind.PHYSICAL, ElementRegistry.NEUTRAL, 6))
            .speed(0.9)
            .gravity(0.05)
            .aim(Aim.LOB)
            .arcHeight(3)
            .lifetime(120)
            .size(0.5F)
            .look("flint")
            .build());
    /** Nebula's and Celestial's seeking shot. */
    public static final ProjectileSpec TRIAL_SEEKER = register(
        ProjectileSpec.builder("trial_seeker", damage(DamageKind.MAGIC, Elements.LIGHT, 4))
            .speed(0.5)
            .homing(new Homing(0.15, 5, 60, 24))
            .lifetime(130)
            .look("ender_eye")
            .trail("portal")
            .build());
    /** Nebula's beam that slows through walls. */
    public static final ProjectileSpec TRIAL_BEAM = register(
        ProjectileSpec.builder("trial_beam", damage(DamageKind.MAGIC, Elements.ICE, 3))
            .speed(1.5)
            .piercesWalls(true)
            .lifetime(45)
            .effect(ImpactEffect.potion(Potion.moveSlowdown.id, 60, 1))
            .look("snowball")
            .trail("snowshovel")
            .build());

    private ProjectileTypes() {}

    public static synchronized ProjectileSpec register(ProjectileSpec spec) {
        if (BY_NAME.putIfAbsent(spec.name, spec) != null)
            throw new IllegalArgumentException("Projectile already registered: " + spec.name);
        return spec;
    }

    /** The spec of that name, or null. */
    public static synchronized ProjectileSpec get(String name) {
        return BY_NAME.get(name);
    }

    public static synchronized List<String> names() {
        return new ArrayList<>(BY_NAME.keySet());
    }

    private static ElementalDamage damage(DamageKind kind, Element element, float amount) {
        return ElementalDamage.builder(kind)
            .add(element, amount)
            .build();
    }

    /** Loads the class, which registers the trial shots above. */
    public static void register() {}
}
