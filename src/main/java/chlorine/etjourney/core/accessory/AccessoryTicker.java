package chlorine.etjourney.core.accessory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.BaseAttributeMap;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import chlorine.etjourney.core.accessory.model.Cadence;
import chlorine.etjourney.core.accessory.model.ModifierPlan;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/** Runs the accessory effects on the server: matches modifiers every half second and ticks each accessory. */
public final class AccessoryTicker {

    /** Wear and sync cadences per player. */
    private final Map<EntityPlayer, Cadence[]> cadences = new WeakHashMap<>();

    private AccessoryTicker() {}

    public static void register() {
        FMLCommonHandler.instance()
            .bus()
            .register(new AccessoryTicker());
    }

    /**
     * Counts world time rather than the player's age: right after joining, a server player's tick event fired
     * several times on one age and never on others, which would wear an accessory several times a second.
     */
    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.worldObj.isRemote) return;
        Cadence[] c = cadences.computeIfAbsent(event.player, p -> new Cadence[] { new Cadence(20), new Cadence(10) });
        long now = event.player.worldObj.getTotalWorldTime();
        if (c[0].due(now)) tickWorn(event.player);
        if (c[1].due(now)) syncModifiers(event.player);
    }

    private static void tickWorn(EntityLivingBase living) {
        for (ItemStack stack : WornAccessories.of(living)) {
            ItemStack after = ((Accessory) stack.getItem()).tickWorn(stack, living);
            if (after != stack) WornAccessories.replace(living, stack, after);
        }
    }

    private static void syncModifiers(EntityLivingBase living) {
        AccessoryModifiers wanted = new AccessoryModifiers();
        for (ItemStack stack : WornAccessories.of(living)) {
            wanted.begin(key(stack));
            ((Accessory) stack.getItem()).addModifiers(stack, living, wanted);
        }
        BaseAttributeMap map = living.getAttributeMap();
        ModifierPlan plan = ModifierPlan.between(current(map), wanted.entries());
        for (ModifierPlan.Entry e : plan.removals) {
            IAttributeInstance instance = map.getAttributeInstanceByName(e.attribute);
            AttributeModifier modifier = instance == null ? null : instance.getModifier(e.id);
            if (modifier != null) instance.removeModifier(modifier);
        }
        for (ModifierPlan.Entry e : plan.additions) {
            IAttributeInstance instance = map.getAttributeInstanceByName(e.attribute);
            if (instance != null)
                instance.applyModifier(new AttributeModifier(e.id, AccessoryModifiers.NAME, e.amount, e.operation));
        }
        if (living.getHealth() > living.getMaxHealth()) living.setHealth(living.getMaxHealth());
    }

    /** The accessory modifiers the creature carries now, including any left from a save. */
    @SuppressWarnings("unchecked")
    private static List<ModifierPlan.Entry> current(BaseAttributeMap map) {
        List<ModifierPlan.Entry> found = new ArrayList<>();
        for (IAttributeInstance instance : (Iterable<IAttributeInstance>) map.getAllAttributes()) {
            String attribute = instance.getAttribute()
                .getAttributeUnlocalizedName();
            for (AttributeModifier m : (Iterable<AttributeModifier>) instance.func_111122_c()) {
                if (AccessoryModifiers.NAME.equals(m.getName()))
                    found.add(new ModifierPlan.Entry(m.getID(), attribute, m.getAmount(), m.getOperation()));
            }
        }
        return found;
    }

    private static String key(ItemStack stack) {
        Item item = stack.getItem();
        String name = Item.itemRegistry.getNameForObject(item);
        return item.getHasSubtypes() ? name + "@" + stack.getItemDamage() : name;
    }
}
