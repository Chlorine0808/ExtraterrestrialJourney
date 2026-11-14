package chlorine.etjourney.content.rank.ability;

import net.minecraft.entity.EntityLivingBase;

import chlorine.etjourney.content.combat.Elements;
import chlorine.etjourney.content.projectile.EntityEtjProjectile;
import chlorine.etjourney.content.projectile.ImpactEffect;
import chlorine.etjourney.content.projectile.ProjectileSpec;
import chlorine.etjourney.content.projectile.ProjectileTypes;
import chlorine.etjourney.core.damage.model.DamageKind;
import chlorine.etjourney.core.damage.model.Element;
import chlorine.etjourney.core.damage.model.ElementRegistry;
import chlorine.etjourney.core.damage.model.ElementalDamage;

/** The shots the rank abilities fire; the client finds them by name, so they register on both sides. */
public final class RankShots {

    /** Sets what it hits on fire; it never lights blocks. */
    public static final ProjectileSpec FIREBALL = ProjectileTypes.register(
        ProjectileSpec.builder("rank_fireball", damage(DamageKind.MAGIC, Elements.FIRE, 5))
            .speed(0.8)
            .lifetime(80)
            .look("fire_charge")
            .trail("flame")
            .effect(new ImpactEffect() {

                @Override
                public void onEntity(EntityEtjProjectile shot, EntityLivingBase target, boolean hurt) {
                    target.setFire(3);
                }
            })
            .build());

    /** Falls straight down on a warned spot. */
    public static final ProjectileSpec ICICLE = ProjectileTypes.register(
        ProjectileSpec.builder("rank_icicle", damage(DamageKind.PHYSICAL, ElementRegistry.NEUTRAL, 5))
            .speed(0.4)
            .gravity(0.08)
            .lifetime(80)
            .size(0.5F)
            .look("flint")
            .trail("snowshovel")
            .build());

    private RankShots() {}

    private static ElementalDamage damage(DamageKind kind, Element element, float amount) {
        return ElementalDamage.builder(kind)
            .add(element, amount)
            .build();
    }

    /** Loads the class, which registers the shots above. */
    public static void register() {}
}
