package chlorine.etjourney.content.craft;

import java.util.Arrays;

/**
 * The recipes of each device. Every recipe here is a placeholder that only proves the devices work; the real ones
 * are still to be decided. Write recipes with more conditions first, since the first match wins.
 */
final class CraftRecipes {

    static final RecipeBook FORGE = new RecipeBook(
        Arrays.asList(
            // The memo says Ender Gold can be turned into gold.
            Recipe.builder(new ItemOutput("minecraft:gold_ingot", 0, 1), 5)
                .input(ItemInput.ore("ingotEnderGold", 2))
                .build(),
            // Talismans: three of the early metal bound with Ender Gold; Rhenium is rare, so one ingot will do.
            Recipe.builder(new ItemOutput("etjourney:sakura_gold_talisman", 0, 1), 8)
                .input(ItemInput.ore("ingotSakuraGold", 3))
                .input(ItemInput.ore("ingotEnderGold", 1))
                .build(),
            Recipe.builder(new ItemOutput("etjourney:golden_palladium_talisman", 0, 1), 8)
                .input(ItemInput.ore("ingotGoldenPalladium", 3))
                .input(ItemInput.ore("ingotEnderGold", 1))
                .build(),
            Recipe.builder(new ItemOutput("etjourney:rhenium_talisman", 0, 1), 8)
                .input(ItemInput.ore("ingotRhenium", 1))
                .input(ItemInput.ore("ingotEnderGold", 2))
                .build()));

    static final RecipeBook ATTUNE = new RecipeBook(
        Arrays.asList(
            Recipe.builder(new ItemOutput("minecraft:diamond", 0, 1), 200)
                .center(ItemInput.ore("ingotEnderPlatinum", 1))
                .input(ItemInput.ore("ingotEnderGold", 2))
                .condition(new BiomeCondition(CraftWorld.endBiomeName("grove")))
                .build(),
            // Drift is a high-up gem for moving, so its talisman takes the open sky.
            Recipe.builder(new ItemOutput("etjourney:drift_talisman", 0, 1), 200)
                .center(ItemInput.ore("ingotEnderGold", 1))
                .input(ItemInput.ore("gemDrift", 2))
                .condition(new SkyCondition())
                .build(),
            Recipe.builder(new ItemOutput("minecraft:ender_pearl", 0, 1), 200)
                .center(ItemInput.ore("ingotEnderGold", 1))
                .condition(new SkyCondition())
                .condition(new NightCondition())
                .build()));

    private CraftRecipes() {}
}
