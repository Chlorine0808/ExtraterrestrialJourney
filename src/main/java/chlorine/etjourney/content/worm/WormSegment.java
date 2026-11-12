package chlorine.etjourney.content.worm;

import net.minecraft.entity.IEntityMultiPart;
import net.minecraft.entity.boss.EntityDragonPart;

/** One body or tail segment: a hitbox that hands its hits to the head, the way a dragon part does. */
public class WormSegment extends EntityDragonPart {

    /** Position in the body, 0 next to the head. */
    public final int index;
    public final boolean tail;

    public WormSegment(IEntityMultiPart owner, int index, boolean tail, float size) {
        super(owner, tail ? "tail" : "body", size, size);
        this.index = index;
        this.tail = tail;
    }

    /** Moves the segment, keeping the last spot and facing for interpolated drawing. */
    public void place(double x, double y, double z, float yaw, float pitch) {
        prevPosX = lastTickPosX = posX;
        prevPosY = lastTickPosY = posY;
        prevPosZ = lastTickPosZ = posZ;
        prevRotationYaw = rotationYaw;
        prevRotationPitch = rotationPitch;
        // The box is centred on the trail point; an entity's position is the bottom of its box.
        setPosition(x, y - height / 2, z);
        rotationYaw = yaw;
        rotationPitch = pitch;
    }
}
