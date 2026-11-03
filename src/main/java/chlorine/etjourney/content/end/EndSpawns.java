package chlorine.etjourney.content.end;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;

import chlorine.etjourney.content.entity.EntityTable;
import chlorine.etjourney.content.entity.ModEntities;
import chlorine.etjourney.content.entity.SpawnRule;
import chlorine.etjourney.content.entity.SpawnSite;
import chlorine.etjourney.core.config.ETJConfig;
import chlorine.etjourney.world.end.EndTerrain;
import chlorine.etjourney.world.end.feature.EndBiome;
import chlorine.etjourney.world.end.noise.Distance;
import cpw.mods.fml.common.eventhandler.Event;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.registry.EntityRegistry;

/** Lists ruled entities in the End's spawn list and turns away natural spawns their rule refuses. */
public final class EndSpawns {

    private static final int END = 1;

    private EndSpawns() {}

    public static void register() {
        if (!ETJConfig.naturalSpawns) return;
        // The End biome's own list, so a spawn the rule refuses is denied in CheckSpawn rather than kept off the list.
        for (EntityTable.Entry e : ModEntities.TABLE.entries()) {
            if (e.rule == null) continue;
            @SuppressWarnings("unchecked")
            Class<? extends EntityLiving> type = (Class<? extends EntityLiving>) e.type;
            EntityRegistry.addSpawn(
                type,
                e.rule.weight,
                e.rule.groupMin,
                e.rule.groupMax,
                EnumCreatureType.monster,
                BiomeGenBase.sky);
        }
        MinecraftForge.EVENT_BUS.register(new EndSpawns());
    }

    @SubscribeEvent
    public void onCheckSpawn(LivingSpawnEvent.CheckSpawn event) {
        World world = event.world;
        if (world.isRemote || world.provider.dimensionId != END) return;
        EntityTable.Entry entry = ModEntities.TABLE.byType(event.entityLiving.getClass());
        if (entry == null || entry.rule == null) return;
        Long seed = EndTerrain.seed();
        if (seed == null || !entry.rule.allows(site(world, seed, entry.rule, event))) {
            event.setResult(Event.Result.DENY);
        }
    }

    private static SpawnSite site(World world, long seed, SpawnRule rule, LivingSpawnEvent.CheckSpawn event) {
        int x = MathHelper.floor_double(event.x), y = MathHelper.floor_double(event.y),
            z = MathHelper.floor_double(event.z);
        int r = rule.capRadius;
        AxisAlignedBB box = AxisAlignedBB.getBoundingBox(x - r, y - r, z - r, x + r + 1, y + r + 1, z + r + 1);
        int nearby = world.getEntitiesWithinAABB(event.entityLiving.getClass(), box)
            .size();
        return new SpawnSite(
            Distance.planar(x + 0.5, z + 0.5) >= EndTerrain.TERRAIN_START,
            EndBiome.at(seed, x + 0.5, z + 0.5),
            y,
            world.getBlockLightValue(x, y, z),
            nearby);
    }
}
