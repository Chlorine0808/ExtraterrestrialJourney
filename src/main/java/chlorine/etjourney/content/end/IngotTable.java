package chlorine.etjourney.content.end;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Registry names of the ingots, their ore dictionary names, and the ore each one smelts from. */
final class IngotTable {

    static final class Entry {

        final String name;
        final String oreName;
        /** Registry name of the ore whose block and raw drop smelt into this ingot, or null when none does. */
        final String ore;
        final float experience;

        Entry(String name, String oreName, String ore, float experience) {
            this.name = name;
            this.oreName = oreName;
            this.ore = ore;
            this.experience = experience;
        }
    }

    // Experience follows vanilla iron (0.7) and gold (1.0); the later stages give more.
    static final List<Entry> ENTRIES = Collections.unmodifiableList(
        Arrays.asList(
            new Entry("ingot_ender_gold", "ingotEnderGold", "ender_gold_ore", 0.7F),
            new Entry("ingot_ender_platinum", "ingotEnderPlatinum", "ender_platinum_ore", 1.0F),
            new Entry("ingot_sakura_gold", "ingotSakuraGold", "sakura_gold_ore", 1.0F),
            new Entry("ingot_golden_palladium", "ingotGoldenPalladium", "golden_palladium_ore", 1.0F),
            new Entry("ingot_rhenium", "ingotRhenium", "rhenium_ore", 1.0F),
            new Entry("ingot_osmium", "ingotOsmium", "osmium_ore", 1.5F),
            new Entry("ingot_iridium", "ingotIridium", "iridium_ore", 1.5F),
            new Entry("ingot_arceodymium", "ingotArceodymium", "arceodymium_ore", 1.5F),
            new Entry("ingot_ktangphor", "ingotKtangphor", "ktangphor_ore", 1.5F),
            new Entry("ingot_tantalum", "ingotTantalum", null, 1.5F),
            new Entry("ingot_niobium", "ingotNiobium", null, 1.5F),
            // The endgame ingots carry the modifier of their mod-specific name, so other mods do not take them
            // for their own americium or actinium.
            new Entry("ingot_semistable_americium", "ingotSemistableAmericium", "americium_ore", 2.0F),
            new Entry("ingot_semistable_actinium", "ingotSemistableActinium", "actinium_ore", 2.0F),
            new Entry("ingot_inactive_neutron", "ingotInactiveNeutron", "neutron_ore", 2.0F)));

    private IngotTable() {}
}
