package chlorine.etjourney.content.projectile;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.MovingObjectPosition;

/** What a shot does when it lands, beyond its damage; status effects and lingering fields plug in here. */
public interface ImpactEffect {

    /** After the damage; hurt is whether health or absorption dropped. */
    default void onEntity(EntityEtjProjectile shot, EntityLivingBase target, boolean hurt) {}

    /** When a shot that stops at walls runs into a block. */
    default void onBlock(EntityEtjProjectile shot, MovingObjectPosition hit) {}

    /** When it runs out of life without landing. */
    default void onExpire(EntityEtjProjectile shot) {}

    /** Gives whatever it hurts a potion effect. */
    static ImpactEffect potion(int potionId, int ticks, int amplifier) {
        return new ImpactEffect() {

            @Override
            public void onEntity(EntityEtjProjectile shot, EntityLivingBase target, boolean hurt) {
                if (hurt) target.addPotionEffect(new PotionEffect(potionId, ticks, amplifier));
            }
        };
    }
}
