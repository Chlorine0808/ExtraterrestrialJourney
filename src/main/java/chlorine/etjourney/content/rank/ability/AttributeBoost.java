package chlorine.etjourney.content.rank.ability;

import java.util.UUID;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttribute;
import net.minecraft.entity.ai.attributes.IAttributeInstance;

/** Adds an ability's fixed attribute modifier, a part of the base value, once. */
final class AttributeBoost {

    private static final int ADD_PART = 1;

    private AttributeBoost() {}

    static void add(EntityLiving mob, IAttribute attribute, UUID id, double part) {
        IAttributeInstance a = mob.getEntityAttribute(attribute);
        if (a == null || a.getModifier(id) != null) return;
        a.applyModifier(new AttributeModifier(id, "etjourney rank ability", part, ADD_PART));
    }
}
