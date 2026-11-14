package chlorine.etjourney.core.status;

import net.minecraft.potion.Potion;

import chlorine.etjourney.core.status.model.PotionSlots;
import chlorine.etjourney.core.util.ModLog;

/** A potion of ETJ's own; it knows whether it is harmful on the server too. */
public class StatusPotion extends Potion {

    /** The first ID above vanilla's. */
    private static final int FIRST_FREE = 24;

    final boolean harmful;

    /** The name doubles as the translation key. The icon is a cell of vanilla's inventory sheet. */
    public StatusPotion(int id, String name, boolean harmful, int color, int iconX, int iconY) {
        super(id, harmful, color);
        this.harmful = harmful;
        setPotionName(name);
        setIconIndex(iconX, iconY);
    }

    /** The preferred ID when free, else the first free one, or -1 when the table is full. */
    public static int claimId(String name, int preferred) {
        int id = PotionSlots.claim(Potion.potionTypes, preferred, FIRST_FREE);
        if (id < 0) ModLog.LOG.warn("No free potion ID for {}; the effect is left out", name);
        else if (id != preferred) ModLog.LOG.warn("Potion ID {} is taken; {} uses {} instead", preferred, name, id);
        return id;
    }
}
