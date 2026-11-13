package chlorine.etjourney.content.area;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.potion.Potion;

import chlorine.etjourney.content.combat.Elements;
import chlorine.etjourney.core.damage.model.DamageKind;
import chlorine.etjourney.core.damage.model.Element;
import chlorine.etjourney.core.damage.model.ElementRegistry;
import chlorine.etjourney.core.damage.model.ElementalDamage;

/** The table of areas by name; the client finds an area's spec here from the name it is sent. */
public final class AreaTypes {

    private static final Map<String, AreaSpec> BY_NAME = new LinkedHashMap<>();

    /** A spore cloud that poisons, provisional until the spore enemies exist. */
    public static final AreaSpec TRIAL_SPORES = register(
        AreaSpec.builder("trial_spores")
            .shape(AreaShape.CLOUD)
            .startRadius(1)
            .radius(2.5)
            .growTicks(20)
            .height(2.5)
            .duration(200)
            .interval(20)
            .damage(damage(DamageKind.MAGIC, ElementRegistry.NEUTRAL, 1))
            .effect(AreaEffect.status(Potion.poison.id, 60, 0))
            .particle("townaura")
            .build());
    /** A sap floor that holds anyone in it, mobs included. */
    public static final AreaSpec TRIAL_SAP = register(
        AreaSpec.builder("trial_sap")
            .radius(2)
            .duration(300)
            .shrinkTicks(60)
            .slow(0.3)
            .hitsAllies(true)
            .particle("slime")
            .build());
    /** A floor of embers that burns out as it burns. */
    public static final AreaSpec TRIAL_EMBERS = register(
        AreaSpec.builder("trial_embers")
            .radius(2)
            .duration(160)
            .interval(10)
            .radiusPerUse(0.25)
            .damage(damage(DamageKind.MAGIC, Elements.FIRE, 2))
            .effect(AreaEffect.burn(2))
            .particle("flame")
            .build());


    private AreaTypes() {}
    public static synchronized AreaSpec register(AreaSpec spec) {
        if (BY_NAME.putIfAbsent(spec.name, spec) != null)
            throw new IllegalArgumentException("Area already registered: " + spec.name);
        return spec;
    }

    /** The spec of that name, or null. */
    public static synchronized AreaSpec get(String name) {
        return BY_NAME.get(name);
    }

    public static synchronized List<String> names() {
        return new ArrayList<>(BY_NAME.keySet());
    }

    private static ElementalDamage damage(DamageKind kind, Element element, float amount) {
        return ElementalDamage.builder(kind)
            .add(element, amount)
            .build();
    }

    /** Loads the class, which registers the trial areas above. */
    public static void register() {}
}
