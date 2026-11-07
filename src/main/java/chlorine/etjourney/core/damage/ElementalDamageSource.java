package chlorine.etjourney.core.damage;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.DamageSource;
import net.minecraft.util.IChatComponent;

import chlorine.etjourney.core.ModInfo;
import chlorine.etjourney.core.damage.model.DamageKind;
import chlorine.etjourney.core.damage.model.ElementalDamage;

/** A damage source that carries an elemental attack; its type is etjourney.<dominant element>. */
public class ElementalDamageSource extends DamageSource {

    private final ElementalDamage damage;
    private final Entity direct;
    private final Entity attacker;

    /** Both entities may be null: direct is what touched the target, attacker who is to blame. */
    public ElementalDamageSource(ElementalDamage damage, Entity direct, Entity attacker) {
        super(
            ModInfo.MODID + "."
                + damage.dominant()
                    .name());
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

    @Override
    public IChatComponent func_151519_b(EntityLivingBase victim) {
        String key = "death.attack." + damageType;
        if (attacker == null) return new ChatComponentTranslation(key, victim.func_145748_c_());
        return new ChatComponentTranslation(key + ".player", victim.func_145748_c_(), attacker.func_145748_c_());
    }
}
