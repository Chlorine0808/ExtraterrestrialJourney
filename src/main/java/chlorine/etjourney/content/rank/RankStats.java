package chlorine.etjourney.content.rank;

import java.util.UUID;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttribute;
import net.minecraft.entity.ai.attributes.IAttributeInstance;

/** Raises a ranked mob's attributes with fixed modifiers; the attributes save them with the mob. */
public final class RankStats {

    private static final UUID HEALTH = UUID.fromString("5d4b0c1e-7a0e-4f7c-9a51-1c2a3b9e0d01"),
        ATTACK = UUID.fromString("5d4b0c1e-7a0e-4f7c-9a51-1c2a3b9e0d02"),
        SPEED = UUID.fromString("5d4b0c1e-7a0e-4f7c-9a51-1c2a3b9e0d03"),
        KNOCKBACK = UUID.fromString("5d4b0c1e-7a0e-4f7c-9a51-1c2a3b9e0d04");
    /** Operation 1 adds a part of the base value; 0 adds the amount as is. */
    private static final int ADD = 0, ADD_PART = 1;

    private RankStats() {}

    public static void apply(EntityLivingBase mob, Rank rank) {
        if (rank == Rank.NORMAL) return;
        modify(mob, SharedMonsterAttributes.maxHealth, HEALTH, rank.health, ADD_PART);
        modify(mob, SharedMonsterAttributes.attackDamage, ATTACK, rank.attack, ADD_PART);
        modify(mob, SharedMonsterAttributes.movementSpeed, SPEED, rank.speed, ADD_PART);
        modify(mob, SharedMonsterAttributes.knockbackResistance, KNOCKBACK, rank.knockback, ADD);
        mob.setHealth(mob.getMaxHealth());
    }

    /** Weakens a split child or a minion by the given parts of its health and attack. */
    public static void weaken(EntityLivingBase mob, double health, double attack) {
        modify(mob, SharedMonsterAttributes.maxHealth, HEALTH, -health, ADD_PART);
        modify(mob, SharedMonsterAttributes.attackDamage, ATTACK, -attack, ADD_PART);
        mob.setHealth(mob.getMaxHealth());
    }

    private static void modify(EntityLivingBase mob, IAttribute attribute, UUID id, double amount, int operation) {
        IAttributeInstance a = mob.getEntityAttribute(attribute);
        if (a == null || amount == 0 || a.getModifier(id) != null) return;
        a.applyModifier(new AttributeModifier(id, "etjourney rank", amount, operation));
    }
}
