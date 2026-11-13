package chlorine.etjourney.core.accessory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.entity.ai.attributes.IAttribute;

import chlorine.etjourney.core.accessory.model.ModifierPlan;

/** Collects the attribute modifiers of the counted accessories; each accessory gets its own stable ids. */
public final class AccessoryModifiers {

    /** The name every accessory modifier carries, so the loop can tell its own apart. */
    public static final String NAME = "etjourney.accessory";

    private final Map<UUID, ModifierPlan.Entry> entries = new LinkedHashMap<>();
    private String accessory;

    /** Names the accessory whose modifiers the following adds belong to. */
    void begin(String accessory) {
        this.accessory = accessory;
    }

    /** Operation as in vanilla: 0 adds, 1 multiplies the base, 2 multiplies the total. */
    public AccessoryModifiers add(IAttribute attribute, double amount, int operation) {
        String name = attribute.getAttributeUnlocalizedName();
        UUID id = UUID.nameUUIDFromBytes(
            (NAME + ":" + accessory + ":" + name + ":" + operation).getBytes(StandardCharsets.UTF_8));
        ModifierPlan.Entry old = entries.get(id);
        double total = old == null ? amount : old.amount + amount;
        entries.put(id, new ModifierPlan.Entry(id, name, total, operation));
        return this;
    }

    List<ModifierPlan.Entry> entries() {
        return new ArrayList<>(entries.values());
    }
}
