package chlorine.etjourney.content.worm;

import net.minecraft.entity.IEntityMultiPart;
import net.minecraft.entity.boss.EntityDragonPart;

/** One hitbox of a worm (head, body or tail) that hands its hits to the head entity, the way a dragon part does. */
public class WormSegment extends EntityDragonPart {

    /** The index of the head's own hitbox. */
    public static final int HEAD = -1;

    /** Position in the body, 0 next to the head, or {@link #HEAD}. */
    public final int index;
    public final boolean tail;

    public WormSegment(IEntityMultiPart owner, int index, boolean tail, float size) {
        super(owner, index == HEAD ? "head" : tail ? "tail" : "body", size, size);
        this.index = index;
        this.tail = tail;
    }

    /** A move longer than this is drawn as a jump, not swept across. */
    private static final double SNAP = 4;
    private boolean placed;

    /**
     * Moves the segment, keeping the last spot and facing for interpolated drawing. The first placing and a long
     * jump start afresh, so the segment is not drawn sweeping in from the world origin or across a teleport.
     */
    public void place(double x, double y, double z, float yaw, float pitch) {
        double by = y - height / 2;
        if (!placed || Math.abs(x - posX) + Math.abs(by - posY) + Math.abs(z - posZ) > SNAP) {
            posX = x;
            posY = by;
            posZ = z;
            rotationYaw = yaw;
            rotationPitch = pitch;
            placed = true;
        }
        prevPosX = lastTickPosX = posX;
        prevPosY = lastTickPosY = posY;
        prevPosZ = lastTickPosZ = posZ;
        prevRotationYaw = rotationYaw;
        prevRotationPitch = rotationPitch;
        // The box is centred on the trail point; an entity's position is the bottom of its box.
        setPosition(x, by, z);
        rotationYaw = yaw;
        rotationPitch = pitch;
    }
}
