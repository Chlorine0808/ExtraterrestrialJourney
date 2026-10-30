package chlorine.etjourney.content.end;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import chlorine.etjourney.world.end.EndPalette;
import chlorine.etjourney.world.end.feature.plant.CrystalGrowth;
import chlorine.etjourney.world.end.modifier.EndBlock;

/** The crystal that grows buds on its faces: each random tick may start a bud on one face or advance the one there. */
final class BuddingCrystalBlock extends CrystalBlock {

    private static final EndBlock[] BUDS = { EndBlock.CRYSTAL_BUD_SMALL, EndBlock.CRYSTAL_BUD_MEDIUM,
        EndBlock.CRYSTAL_BUD_LARGE, EndBlock.CRYSTAL_CLUSTER };

    BuddingCrystalBlock(LargePlantBlockTable.Entry entry) {
        super(entry);
        setTickRandomly(true);
    }

    @Override
    public void updateTick(World world, int x, int y, int z, Random random) {
        if (world.isRemote || random.nextDouble() >= CrystalGrowth.GROW_CHANCE) return;
        int face = random.nextInt(6);
        ForgeDirection side = ForgeDirection.getOrientation(face);
        int bx = x + side.offsetX, by = y + side.offsetY, bz = z + side.offsetZ;
        // A random tick must not load the chunk across a border.
        if (by < 0 || by > 255 || !world.blockExists(bx, by, bz)) return;
        Block there = world.getBlock(bx, by, bz);
        if (world.isAirBlock(bx, by, bz)) {
            setBud(world, bx, by, bz, CrystalGrowth.next(null), face);
            return;
        }
        // Only a bud pointing away from this face grows; anything else on the face is left alone.
        if (world.getBlockMetadata(bx, by, bz) != face) return;
        EndBlock current = budKind(there);
        EndBlock next = current == null ? null : CrystalGrowth.next(current);
        if (next != null) setBud(world, bx, by, bz, next, face);
    }

    private static EndBlock budKind(Block block) {
        for (EndBlock kind : BUDS) {
            if (EndPalette.block(kind) == block) return kind;
        }
        return null;
    }

    private static void setBud(World world, int x, int y, int z, EndBlock kind, int facing) {
        world.setBlock(x, y, z, EndPalette.block(kind), facing, 3);
    }
}
