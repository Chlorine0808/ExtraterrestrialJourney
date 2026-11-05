package chlorine.etjourney.world.building;

/** One position of a template: left alone, air, or a weighted choice of blocks. */
public final class Cell {

    public static final Cell KEEP = new Cell(null, null);
    public static final Cell AIR = new Cell(null, null);

    private final BlockRef[] choices;
    /** Running totals of the weights, normalised to end at 1. */
    private final double[] cumulative;

    private Cell(BlockRef[] choices, double[] cumulative) {
        this.choices = choices;
        this.cumulative = cumulative;
    }

    public static Cell of(BlockRef block) {
        return new Cell(new BlockRef[] { block }, new double[] { 1 });
    }

    public static Cell of(BlockRef[] choices, double[] weights) {
        double total = 0;
        for (double w : weights) total += w;
        double[] cumulative = new double[weights.length];
        double sum = 0;
        for (int i = 0; i < weights.length; i++) {
            sum += weights[i];
            cumulative[i] = sum / total;
        }
        return new Cell(choices.clone(), cumulative);
    }

    public boolean isKeep() {
        return this == KEEP;
    }

    public boolean isAir() {
        return this == AIR;
    }

    /** The block chosen by u in [0, 1); null for KEEP and AIR. */
    public BlockRef pick(double u) {
        if (choices == null) return null;
        for (int i = 0; i < choices.length - 1; i++) if (u < cumulative[i]) return choices[i];
        return choices[choices.length - 1];
    }
}
