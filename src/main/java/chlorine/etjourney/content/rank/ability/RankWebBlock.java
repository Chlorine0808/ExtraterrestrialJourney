package chlorine.etjourney.content.rank.ability;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.BlockWeb;
import net.minecraft.item.Item;
import net.minecraft.world.World;

import chlorine.etjourney.core.registry.RegistryNames;
import cpw.mods.fml.common.registry.GameRegistry;

/** A cobweb that a web shot leaves; it drops nothing and fades after 100 ticks, even across a reload. */
public final class RankWebBlock extends BlockWeb {

    public static final String NAME = "rank_web";
    static final int LIFETIME = 100;
    private static Block instance;

    private RankWebBlock() {
        setHardness(4.0F);
        setLightOpacity(1);
        setBlockName(RegistryNames.unlocalized(NAME));
        setBlockTextureName("web");
    }

    /** Registers the block; the IDs must match on both sides, so it runs whatever the config says. */
    public static void register() {
        instance = new RankWebBlock();
        GameRegistry.registerBlock(instance, NAME);
    }

    public static Block instance() {
        return instance;
    }

    /** Puts a web at the spot if it holds air; true when it did. */
    static boolean place(World world, int x, int y, int z) {
        if (instance == null || !world.isAirBlock(x, y, z)) return false;
        return world.setBlock(x, y, z, instance);
    }

    @Override
    public void onBlockAdded(World world, int x, int y, int z) {
        if (!world.isRemote) world.scheduleBlockUpdate(x, y, z, this, LIFETIME);
    }

    @Override
    public void updateTick(World world, int x, int y, int z, Random random) {
        world.setBlockToAir(x, y, z);
    }

    @Override
    public Item getItemDropped(int meta, Random random, int fortune) {
        return null;
    }

    @Override
    public int quantityDropped(Random random) {
        return 0;
    }
}
