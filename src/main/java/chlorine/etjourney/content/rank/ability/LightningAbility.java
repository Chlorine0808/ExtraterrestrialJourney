package chlorine.etjourney.content.rank.ability;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;

import chlorine.etjourney.content.combat.Elements;
import chlorine.etjourney.content.rank.Cooldown;
import chlorine.etjourney.content.rank.Rank;
import chlorine.etjourney.content.rank.RankBolt;
import chlorine.etjourney.content.rank.RankData;
import chlorine.etjourney.core.damage.ElementalDamageSource;
import chlorine.etjourney.core.damage.ElementalDamages;
import chlorine.etjourney.core.damage.model.DamageKind;
import chlorine.etjourney.core.damage.model.ElementalDamage;
import chlorine.etjourney.core.network.ETJNetwork;
import cpw.mods.fml.common.network.NetworkRegistry;

/** Every 8 seconds, the spot under a target within 24 blocks is warned, then a bolt strikes it for electric damage. */
public final class LightningAbility implements RankAbility {

    public static final String ID = "lightning";
    static final int COOLDOWN = 160, WARNING = 20;
    static final double REACH = 24, RADIUS = 1.5, SEEN_WITHIN = 128;
    private static final ElementalDamage DAMAGE = ElementalDamage.builder(DamageKind.PHYSICAL)
        .add(Elements.ELECTRIC, 6)
        .build();

    @Override
    public String id() {
        return ID;
    }

    @Override
    public Rank least() {
        return Rank.EPIC;
    }

    @Override
    public void onSecond(EntityLiving self) {
        EntityPlayer player = Near.playerTarget(self, REACH);
        if (player == null) return;
        long now = self.worldObj.getTotalWorldTime();
        if (!Cooldown.ready(now, RankData.lastUsed(self, ID), COOLDOWN)) return;
        RankData.markUsed(self, ID, now);
        double x = player.posX, y = player.posY, z = player.posZ;
        Warning.at(
            self.worldObj,
            "fireworksSpark",
            x,
            y + 0.1,
            z,
            WARNING,
            () -> { if (self.isEntityAlive()) strike(self, x, y, z); });
    }

    /** Draws the bolt on the clients and hurts what stands within the radius, the mob itself excepted. */
    private static void strike(EntityLiving self, double x, double y, double z) {
        World world = self.worldObj;
        ETJNetwork.channel()
            .sendToAllAround(
                new RankBolt(x, y, z),
                new NetworkRegistry.TargetPoint(world.provider.dimensionId, x, y, z, SEEN_WITHIN));
        double r = RADIUS + 1;
        AxisAlignedBB box = AxisAlignedBB.getBoundingBox(x - r, y - r, z - r, x + r, y + r, z + r);
        for (Object o : world.getEntitiesWithinAABB(EntityLivingBase.class, box)) {
            EntityLivingBase e = (EntityLivingBase) o;
            if (e == self || !e.isEntityAlive() || e.getDistanceSq(x, y, z) > RADIUS * RADIUS) continue;
            ElementalDamages.deal(e, new ElementalDamageSource(DAMAGE, null, self));
        }
    }
}
