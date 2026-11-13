package chlorine.etjourney.content.accessory;

/** How a single-use accessory wears out: one second of its lifetime per worn second. */
final class WearOut {

    /** The accessory has no time left and breaks. */
    static final int SPENT = -1;

    private WearOut() {}

    /** A lifetime of 0 or less means the accessory never wears. */
    static int afterOneSecond(int used, int lifetime) {
        if (lifetime <= 0) return used;
        int next = used + 1;
        return next >= lifetime ? SPENT : next;
    }
}
