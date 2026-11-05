package chlorine.etjourney.world.building;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.block.Block;
import net.minecraft.block.BlockBed;
import net.minecraft.block.BlockButton;
import net.minecraft.block.BlockChest;
import net.minecraft.block.BlockDirectional;
import net.minecraft.block.BlockDispenser;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.BlockFurnace;
import net.minecraft.block.BlockLadder;
import net.minecraft.block.BlockLever;
import net.minecraft.block.BlockPistonBase;
import net.minecraft.block.BlockPumpkin;
import net.minecraft.block.BlockRotatedPillar;
import net.minecraft.block.BlockSign;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.BlockTorch;
import net.minecraft.block.BlockTrapDoor;
import net.minecraft.init.Blocks;

/** The rotation for a placed block: by registered name first, then by its vanilla block class. */
public final class VanillaMetaRotations {

    private static final Map<Block, MetaRotation> CACHE = new ConcurrentHashMap<>();

    private VanillaMetaRotations() {}

    public static MetaRotation forBlock(Block block) {
        return CACHE.computeIfAbsent(block, VanillaMetaRotations::find);
    }

    private static MetaRotation find(Block block) {
        String name = Block.blockRegistry.getNameForObject(block);
        MetaRotation registered = name == null ? null : MetaRotations.forName(name);
        if (registered != null) return registered;
        if (block instanceof BlockStairs) return MetaRotation.STAIRS;
        if (block instanceof BlockRotatedPillar) return MetaRotation.LOG;
        if (block instanceof BlockDoor) return MetaRotation.DOOR;
        if (block instanceof BlockTrapDoor) return MetaRotation.TRAPDOOR;
        if (block instanceof BlockLever) return MetaRotation.LEVER;
        if (block instanceof BlockButton) return MetaRotation.BUTTON;
        if (block instanceof BlockTorch) return MetaRotation.TORCH;
        if (block instanceof BlockPistonBase || block instanceof BlockDispenser) return MetaRotation.PISTON_DISPENSER;
        if (block instanceof BlockChest || block instanceof BlockFurnace || block instanceof BlockLadder)
            return MetaRotation.FACING_2345;
        if (block instanceof BlockSign)
            return block == Blocks.standing_sign ? MetaRotation.SIGN_POST : MetaRotation.FACING_2345;
        if (block instanceof BlockBed || block instanceof BlockFenceGate
            || block instanceof BlockPumpkin
            || block instanceof BlockDirectional) return MetaRotation.DIRECTIONAL_0123;
        return MetaRotation.NONE;
    }
}
