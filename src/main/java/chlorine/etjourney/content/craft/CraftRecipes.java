package chlorine.etjourney.content.craft;

import java.util.Arrays;
import java.util.Collections;

/**
 * The recipes of each device. Every recipe here is a placeholder that only proves the devices work; the real ones
 * are still to be decided. Write recipes with more conditions first, since the first match wins.
 */
final class CraftRecipes {

    static final RecipeBook FORGE = new RecipeBook(
        Collections.singletonList(
            // The memo says Ender Gold can be turned into gold.
            Recipe.builder(new ItemOutput("minecraft:gold_ingot", 0, 1), 5)
                .input(ItemInput.ore("ingotEnderGold", 2))
                .build()));

    static final RecipeBook ATTUNE = new RecipeBook(
        Arrays.asList(
            Recipe.builder(new ItemOutput("minecraft:diamond", 0, 1), 200)
                .center(ItemInput.ore("ingotEnderPlatinum", 1))
                .input(ItemInput.ore("ingotEnderGold", 2))
                .condition(new BiomeCondition(CraftWorld.endBiomeName("grove")))
                .build(),
            Recipe.builder(new ItemOutput("minecraft:ender_pearl", 0, 1), 200)
                .center(ItemInput.ore("ingotEnderGold", 1))
                .condition(new SkyCondition())
                .condition(new NightCondition())
                .build()));

    private CraftRecipes() {}
}
