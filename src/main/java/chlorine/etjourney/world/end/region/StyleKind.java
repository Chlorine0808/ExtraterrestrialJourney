package chlorine.etjourney.world.end.region;

/** What a style may be in a region. */
public enum StyleKind {
    /** A base with a continent. */
    BASE_LAND,
    /** A base without a continent. */
    BASE_VOID,
    /** Only ever laid over a base. */
    OVERLAY,
    /** A land base that may also be laid over other bases. */
    BOTH
}
