package chlorine.etjourney.content.rank.ability;

import java.util.List;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;

import chlorine.etjourney.content.rank.Cooldown;
import chlorine.etjourney.content.rank.RankData;
import chlorine.etjourney.core.status.StatusEffects;

/** Every 3 seconds, players near the mob get Weakness I and Slowness I for 4 seconds. */
public final class BlightAbility implements RankAbility {

    public static final String ID = "blight";
    static final int COOLDOWN = 60, DURATION = 80;
    private static final double REACH = 6;

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void onSecond(EntityLiving self) {
        long now = self.worldObj.getTotalWorldTime();
        if (!Cooldown.ready(now, RankData.lastUsed(self, ID), COOLDOWN)) return;
        List<EntityPlayer> players = Near.players(self, REACH);
        if (players.isEmpty()) return;
        for (EntityPlayer p : players) {
            StatusEffects.apply(p, new PotionEffect(Potion.weakness.id, DURATION, 0), self, false);
            StatusEffects.apply(p, new PotionEffect(Potion.moveSlowdown.id, DURATION, 0), self, false);
        }
        RankData.markUsed(self, ID, now);
        Offspring.puff(self, "witchMagic", 16);
    }
}
