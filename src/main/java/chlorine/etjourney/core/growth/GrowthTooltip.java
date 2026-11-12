package chlorine.etjourney.core.growth;

import java.util.List;

import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;

import chlorine.etjourney.core.growth.model.AffixRoll;
import chlorine.etjourney.core.growth.model.GrowthData;
import chlorine.etjourney.core.growth.model.Leveling;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/** Colours the name by rarity and lists level and affixes under it; client only. */
public final class GrowthTooltip {

    private GrowthTooltip() {}

    public static void register() {
        MinecraftForge.EVENT_BUS.register(new GrowthTooltip());
    }

    @SubscribeEvent
    public void onTooltip(ItemTooltipEvent event) {
        GrowthData data = GrowthNbt.read(event.itemStack);
        List<String> lines = event.toolTip;
        if (data == null || lines.isEmpty()) return;
        String color = "§" + data.rarity.color;
        lines.set(0, color + lines.get(0));
        String rarity = StatCollector.translateToLocal("etjourney.rarity." + data.rarity.id);
        int level = data.level();
        int[] progress = Leveling.progress(data.xp);
        String lv = level >= Leveling.MAX_LEVEL
            ? StatCollector.translateToLocalFormatted("etjourney.growth.level_max", level)
            : StatCollector.translateToLocalFormatted("etjourney.growth.level", level, progress[0], progress[1]);
        int at = 1;
        lines.add(at++, color + rarity + EnumChatFormatting.GRAY + " " + lv);
        for (AffixRoll roll : data.affixes) {
            Affix affix = Affixes.get(roll.id);
            if (affix != null) lines.add(at++, EnumChatFormatting.DARK_AQUA + affix.describe(affix.value(roll.roll)));
        }
    }
}
