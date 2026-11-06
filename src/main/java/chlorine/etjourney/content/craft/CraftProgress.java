package chlorine.etjourney.content.craft;

/**
 * How far a device has worked on the recipe it currently matches. The recipe itself is not saved, so a reloaded
 * device starts its work over.
 */
public final class CraftProgress {

    private Recipe recipe;
    private int done;

    /**
     * Adds work towards the matched recipe, or starts over when it differs from the last one.
     *
     * @return true when the recipe is finished; the progress is then cleared
     */
    public boolean advance(Recipe matched, int work) {
        if (matched != recipe) {
            recipe = matched;
            done = 0;
        }
        if (matched == null) return false;
        done += work;
        if (done < matched.duration) return false;
        recipe = null;
        done = 0;
        return true;
    }

    public int done() {
        return done;
    }
}
