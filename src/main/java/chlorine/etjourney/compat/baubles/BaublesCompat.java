package chlorine.etjourney.compat.baubles;

import baubles.api.expanded.BaubleExpandedSlots;
import chlorine.etjourney.compat.CompatModule;
import chlorine.etjourney.content.accessory.Talisman;
import chlorine.etjourney.core.accessory.WornAccessories;
import chlorine.etjourney.core.util.ModLog;
import chlorine.etjourney.world.end.debug.damagecheck.DamageCheckServer;
import cpw.mods.fml.common.registry.GameRegistry;

/** Baubles Expanded integration: ETJ's own two talisman slots, the talisman items and the worn lister. */
public final class BaublesCompat implements CompatModule {

    /** ETJ's slot type, so ETJ's accessories never compete with other mods' rings and charms. */
    static final String TALISMAN_TYPE = "etjourney_talisman";
    static final int TALISMAN_SLOTS = 2;

    /** Baubles takes new types and slots only during preInit. */
    @Override
    public void preInit() {
        if (!BaubleExpandedSlots.tryRegisterType(TALISMAN_TYPE)
            || !BaubleExpandedSlots.tryAssignSlotsUpToMinimum(TALISMAN_TYPE, TALISMAN_SLOTS))
            ModLog.LOG.warn("Baubles refused the {} slots; talismans cannot be worn", TALISMAN_TYPE);
        for (Talisman talisman : Talisman.values())
            GameRegistry.registerItem(new TalismanItem(talisman), talisman.name);
        WornAccessories.install(new BaublesWorn());
    }

    @Override
    public void init() {
        if (DamageCheckServer.requested()) AccessoryCheck.register();
    }
}
