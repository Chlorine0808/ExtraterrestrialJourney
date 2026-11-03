package chlorine.etjourney.content.end;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.world.IBlockAccess;

import chlorine.etjourney.core.registry.RegistryNames;

/**
 * An End ore or gem; it drops its raw ore or crystal, more with Fortune, and itself with Silk Touch. A blastproof one
 * stands up to explosions, the Wither and the dragon.
 */
final class OreBlock extends Block {

    private static final float PLAIN_RESISTANCE = 15.0F;
    /** What bedrock sets. */
    private static final float BLASTPROOF_RESISTANCE = 6000000.0F;

    private final OreBlockTable.Entry entry;
    private final Item drop;

    OreBlock(OreBlockTable.Entry entry, Item drop) {
        super(Material.rock);
        this.entry = entry;
        this.drop = drop;
        setHardness(entry.hardness);
        setResistance(entry.blastproof ? BLASTPROOF_RESISTANCE : PLAIN_RESISTANCE);
        setStepSound(soundTypePiston);
        setHarvestLevel("pickaxe", entry.harvestLevel);
        setCreativeTab(CreativeTabs.tabBlock);
        setBlockName(RegistryNames.unlocalized(entry.name));
        setBlockTextureName(RegistryNames.texture(entry.name));
    }

    @Override
    public Item getItemDropped(int meta, Random random, int fortune) {
        return drop;
    }

    @Override
    public int quantityDropped(int meta, int fortune, Random random) {
        return OreDrops.count(entry.minDrops, entry.maxDrops, fortune, random);
    }

    @Override
    public boolean canEntityDestroy(IBlockAccess world, int x, int y, int z, Entity entity) {
        return !entry.blastproof && super.canEntityDestroy(world, x, y, z, entity);
    }
}
