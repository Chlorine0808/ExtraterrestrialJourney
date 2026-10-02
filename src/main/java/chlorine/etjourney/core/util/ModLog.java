package chlorine.etjourney.core.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import chlorine.etjourney.core.ModInfo;

/** Shared logger for the whole mod. */
public final class ModLog {

    public static final Logger LOG = LogManager.getLogger(ModInfo.MODID);

    private ModLog() {}
}
