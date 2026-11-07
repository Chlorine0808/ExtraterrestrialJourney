package chlorine.etjourney.content.end;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;

import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import cpw.mods.fml.client.registry.RenderingRegistry;

/** Client only: draws a fur as crossed planes like a plant, turned to grow away from the leaf it clings to. */
public final class FurRenderer implements ISimpleBlockRenderingHandler {

    /** Half the width of a plane, as vanilla's crossed squares. */
    private static final double HALF = 0.45;

    private final int id;

    private FurRenderer(int id) {
        this.id = id;
    }

    public static void init() {
        FurRenderer renderer = new FurRenderer(RenderingRegistry.getNextAvailableRenderId());
        EndFurBlock.renderType = renderer.id;
        RenderingRegistry.registerBlockHandler(renderer);
    }

    @Override
    public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId,
        RenderBlocks renderer) {
        Block leaves = ((EndFurBlock) block).leaves();
        int[] out = FurFacing.outward((dx, dy, dz) -> world.getBlock(x + dx, y + dy, z + dz) == leaves);
        // Two axes across the growth direction; a fur on a side spreads its planes vertically.
        int[] a = out[1] != 0 ? new int[] { 1, 0, 0 } : new int[] { 0, 1, 0 };
        int[] b = { out[1] * a[2] - out[2] * a[1], out[2] * a[0] - out[0] * a[2], out[0] * a[1] - out[1] * a[0] };

        Tessellator t = Tessellator.instance;
        t.setBrightness(block.getMixedBrightnessForBlock(world, x, y, z));
        int color = block.colorMultiplier(world, x, y, z);
        t.setColorOpaque_F((color >> 16 & 255) / 255.0F, (color >> 8 & 255) / 255.0F, (color & 255) / 255.0F);
        IIcon icon = renderer.getBlockIcon(block, world, x, y, z, 0);
        double u0 = icon.getMinU(), u1 = icon.getMaxU(), v0 = icon.getMinV(), v1 = icon.getMaxV();

        // Each plane in local units: across from -HALF to HALF on (a, b) diagonals, along out from the leaf side up.
        for (int plane = 0; plane < 2; plane++) {
            double s = plane == 0 ? 1 : -1;
            double[] p0 = corner(x, y, z, out, a, b, -HALF, -s * HALF, 0),
                p1 = corner(x, y, z, out, a, b, HALF, s * HALF, 0);
            double[] q0 = corner(x, y, z, out, a, b, -HALF, -s * HALF, 1),
                q1 = corner(x, y, z, out, a, b, HALF, s * HALF, 1);
            // Both windings, so the plane shows from either side.
            vertex(t, q0, u0, v0);
            vertex(t, p0, u0, v1);
            vertex(t, p1, u1, v1);
            vertex(t, q1, u1, v0);
            vertex(t, q1, u0, v0);
            vertex(t, p1, u0, v1);
            vertex(t, p0, u1, v1);
            vertex(t, q0, u1, v0);
        }
        return true;
    }

    /** A point at (across a, across b) from the block's centre line and along out from the leaf side (0) to 1. */
    private static double[] corner(int x, int y, int z, int[] out, int[] a, int[] b, double alongA, double alongB,
        double along) {
        double[] p = new double[3];
        int[] base = { x, y, z };
        for (int i = 0; i < 3; i++) p[i] = base[i] + 0.5 + (along - 0.5) * out[i] + alongA * a[i] + alongB * b[i];
        return p;
    }

    private static void vertex(Tessellator t, double[] p, double u, double v) {
        t.addVertexWithUV(p[0], p[1], p[2], u, v);
    }

    @Override
    public void renderInventoryBlock(Block block, int metadata, int modelId, RenderBlocks renderer) {}

    @Override
    public boolean shouldRender3DInInventory(int modelId) {
        return false;
    }

    @Override
    public int getRenderId() {
        return id;
    }
}
