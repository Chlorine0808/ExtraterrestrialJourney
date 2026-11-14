package chlorine.etjourney.content.projectile;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.function.UnaryOperator;

import org.junit.jupiter.api.Test;

import chlorine.etjourney.core.damage.model.DamageKind;
import chlorine.etjourney.core.damage.model.ElementRegistry;
import chlorine.etjourney.core.damage.model.ElementalDamage;

/** A shot's row in the table refuses values that would make it misbehave. */
class ProjectileSpecTest {

    private static final ElementalDamage HIT = ElementalDamage.builder(DamageKind.PHYSICAL)
        .add(ElementRegistry.NEUTRAL, 2)
        .build();

    private static void rejects(UnaryOperator<ProjectileSpec.Builder> change) {
        assertThrows(
            IllegalArgumentException.class,
            () -> change.apply(ProjectileSpec.builder("shot", HIT))
                .build());
    }

    @Test
    void theDefaultsMakeAShot() {
        assertDoesNotThrow(
            () -> ProjectileSpec.builder("shot", HIT)
                .build());
    }

    @Test
    void rejectsBadMotion() {
        rejects(b -> b.speed(0));
        rejects(b -> b.gravity(-0.1));
        rejects(b -> b.drag(0));
        rejects(b -> b.drag(1.1));
        rejects(b -> b.lifetime(0));
        rejects(b -> b.size(0));
        rejects(b -> b.piercesEntities(-1));
    }

    @Test
    void aLobNeedsGravityAndAnArc() {
        rejects(b -> b.aim(Aim.LOB));
        rejects(
            b -> b.aim(Aim.LOB)
                .gravity(0.05)
                .arcHeight(0));
        assertDoesNotThrow(
            () -> ProjectileSpec.builder("shot", HIT)
                .aim(Aim.LOB)
                .gravity(0.05)
                .build());
    }

    @Test
    void needsANameAndDamage() {
        assertThrows(IllegalArgumentException.class, () -> ProjectileSpec.builder("Bad Name", HIT));
        assertThrows(IllegalArgumentException.class, () -> ProjectileSpec.builder("shot", null));
    }
}
