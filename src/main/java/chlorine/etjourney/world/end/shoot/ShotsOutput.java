package chlorine.etjourney.world.end.shoot;

import java.io.File;

/** Where a screenshot run writes: the root from -Detj.shotsDir, one folder per run named by time and branch. */
public final class ShotsOutput {

    private ShotsOutput() {}

    /** The folder holding every run; a relative property resolves against the game folder. */
    public static File root(String property, File gameDir) {
        String dir = property == null ? "" : property.trim();
        if (dir.isEmpty()) return new File(gameDir, "etj-shots");
        File f = new File(dir);
        return f.isAbsolute() ? f : new File(gameDir, dir);
    }

    /** The run's folder name: the stamp, then the branch with anything unsafe in a file name turned into '-'. */
    public static String folder(String stamp, String branch) {
        String tag = branch == null ? "" : branch.trim();
        if (tag.isEmpty()) return stamp;
        return stamp + "_" + tag.replaceAll("[^A-Za-z0-9._-]", "-");
    }
}
