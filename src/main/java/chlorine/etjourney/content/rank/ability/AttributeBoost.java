package chlorine.etjourney.content.rank.ability;

import java.util.UUID;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttribute;
import net.minecraft.entity.ai.attributes.IAttributeInstance;

/** Adds an ability's fixed attribute modifier, a part of the base value, once. */
final class AttributeBoost {

    private static final int ADD_AMOUNT = 0, ADD_PART = 1;

    private AttributeBoost() {}

    static void add(EntityLiving mob, IAttribute attribute, UUID id, double part) {
        apply(mob, attribute, id, part, ADD_PART);
    }

    /** Adds a flat amount, for an attribute whose base may be zero, as knockback resistance. */
    static void addAmount(EntityLiving mob, IAttribute attribute, UUID id, double amount) {
        apply(mob, attribute, id, amount, ADD_AMOUNT);
    }

    private static void apply(EntityLiving mob, IAttribute attribute, UUID id, double value, int operation) {
        IAttributeInstance a = mob.getEntityAttribute(attribute);
        if (a == null || a.getModifier(id) != null) return;
        a.applyModifier(new AttributeModifier(id, "etjourney rank ability", value, operation));
    }
}
