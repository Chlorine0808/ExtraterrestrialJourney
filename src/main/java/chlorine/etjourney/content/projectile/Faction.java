package chlorine.etjourney.content.projectile;

/** Whose shot it is, which decides what it may hit. */
public enum Faction {

    /** Hits anything alive but the player who fired; other players by the server's PvP rule. */
    PLAYER,
    /** Hits players only, passing through other mobs so enemies never thin each other out. */
    ENEMY;

    public static Faction of(boolean shooterIsPlayer) {
        return shooterIsPlayer ? PLAYER : ENEMY;
    }

    public boolean hits(boolean targetIsPlayer, boolean targetIsShooter) {
        if (targetIsShooter) return false;
        return this == PLAYER || targetIsPlayer;
    }
}
