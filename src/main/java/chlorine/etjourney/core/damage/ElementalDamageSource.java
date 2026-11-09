package chlorine.etjourney.core.damage;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EntityDamageSource;
import net.minecraft.util.IChatComponent;

import chlorine.etjourney.core.ModInfo;
import chlorine.etjourney.core.damage.model.DamageKind;
import chlorine.etjourney.core.damage.model.ElementalDamage;

/**
 * A damage source that carries an elemental attack; its type is etjourney.<dominant element>. An entity source, so
 * players keep the server PvP setting and team friendly fire.
 */
public class ElementalDamageSource extends EntityDamageSource {

    private final ElementalDamage damage;
    private final Entity direct;
    private final Entity attacker;

    /** Both entities may be null: direct is what touched the target, attacker who is to blame. */
    public ElementalDamageSource(ElementalDamage damage, Entity direct, Entity attacker) {
        super(
            ModInfo.MODID + "."
                + damage.dominant()
                    .name(),
            attacker);
        this.damage = damage;
        this.direct = direct;
        this.attacker = attacker;
        if (damage.kind() == DamageKind.MAGIC) {
            setMagicDamage();
            setDamageBypassesArmor();
        }
        if (damage.isPiercing()) {
            setDamageBypassesArmor();
            setDamageIsAbsolute();
        }
    }

    public ElementalDamage damage() {
        return damage;
    }

    @Override
    public Entity getSourceOfDamage() {
        return direct;
    }

    @Override
    public Entity getEntity() {
        return attacker;
    }

    /** Never scaled by difficulty: an enemy's strength is set by its attack. */
    @Override
    public boolean isDifficultyScaled() {
        return false;
    }

    @Override
    public IChatComponent func_151519_b(EntityLivingBase victim) {
        String key = "death.attack." + damageType;
        if (attacker == null) return new ChatComponentTranslation(key, victim.func_145748_c_());
        return new ChatComponentTranslation(key + ".player", victim.func_145748_c_(), attacker.func_145748_c_());
    }
}
