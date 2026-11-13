package chlorine.etjourney.core.damage;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IEntityOwnable;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemTool;
import net.minecraft.util.DamageSource;

import chlorine.etjourney.core.damage.model.AttackProfile;
import chlorine.etjourney.core.damage.model.DamageKind;
import chlorine.etjourney.core.damage.model.WeaponClass;
import chlorine.etjourney.core.damage.model.WeaponClassifier;
import chlorine.etjourney.core.damage.model.WeaponClassifier.Direct;
import chlorine.etjourney.core.damage.model.WeaponClassifier.Held;

/** Reads the shape of an attack from its damage source and the attacker's hand. */
public final class AttackProfiles {

    private AttackProfiles() {}

    public static AttackProfile of(DamageSource source) {
        Entity attacker = source.getEntity();
        Entity direct = source.getSourceOfDamage();
        ItemStack held = attacker instanceof EntityLivingBase ? ((EntityLivingBase) attacker).getHeldItem() : null;
        WeaponClass declared = held != null && held.getItem() instanceof WeaponClassified
            ? ((WeaponClassified) held.getItem()).weaponClass(held)
            : null;
        boolean magic = source.isMagicDamage();
        WeaponClassifier.Facts facts = new WeaponClassifier.Facts(
            direct(attacker, direct),
            held(held),
            declared,
            magic,
            source.isProjectile());
        DamageKind kind = source instanceof ElementalDamageSource ? ((ElementalDamageSource) source).damage()
            .kind() : magic ? DamageKind.MAGIC : DamageKind.PHYSICAL;
        boolean summoned = attacker instanceof IEntityOwnable && ((IEntityOwnable) attacker).getOwner() != null;
        return new AttackProfile(kind, WeaponClassifier.reach(facts), WeaponClassifier.weapon(facts), summoned);
    }

    private static Direct direct(Entity attacker, Entity direct) {
        if (attacker == null) return Direct.NONE;
        if (direct == null || direct == attacker) return Direct.SELF;
        return direct instanceof EntityArrow ? Direct.ARROW : Direct.OTHER;
    }

    private static Held held(ItemStack stack) {
        if (stack == null) return Held.NONE;
        if (stack.getItem() instanceof ItemSword) return Held.SWORD;
        if (stack.getItem() instanceof ItemAxe) return Held.AXE;
        if (stack.getItem() instanceof ItemTool) return Held.TOOL;
        if (stack.getItem() instanceof ItemBow) return Held.BOW;
        return Held.OTHER;
    }
}
