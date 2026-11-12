package chlorine.etjourney.content.worm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.entity.IEntityMultiPart;
import net.minecraft.entity.boss.EntityDragonPart;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

import org.junit.jupiter.api.Test;

/** A hit on any segment is handed whole to the worm that owns it. */
class WormSegmentTest {

    private static final class Owner implements IEntityMultiPart {

        EntityDragonPart hitPart;
        float hitAmount;

        @Override
        public World func_82194_d() {
            return null;
        }

        @Override
        public boolean attackEntityFromPart(EntityDragonPart part, DamageSource source, float amount) {
            hitPart = part;
            hitAmount = amount;
            return true;
        }
    }

    @Test
    void hittingASegmentHandsTheDamageToItsOwner() {
        Owner owner = new Owner();
        WormSegment tail = new WormSegment(owner, 10, true, 1.0F);
        assertTrue(tail.attackEntityFrom(DamageSource.generic, 6));
        assertSame(tail, owner.hitPart);
        assertEquals(6, owner.hitAmount, 1e-6);
    }

    @Test
    void movingASegmentKeepsWhereItWasForSmoothDrawing() {
        WormSegment body = new WormSegment(new Owner(), 0, false, 1.0F);
        body.place(1, 2, 3, 90, 10);
        body.place(2, 2, 3, 80, 0);
        assertEquals(1, body.prevPosX, 1e-9);
        assertEquals(2, body.posX, 1e-9);
        assertEquals(90, body.prevRotationYaw, 1e-6);
        assertEquals(80, body.rotationYaw, 1e-6);
    }

    @Test
    void theFirstPlacingAndALongJumpDoNotSweepAcross() {
        WormSegment body = new WormSegment(new Owner(), 0, false, 1.0F);
        body.place(100, 64, 100, 0, 0);
        assertEquals(100, body.prevPosX, 1e-9);
        body.place(150, 64, 100, 0, 0);
        assertEquals(150, body.prevPosX, 1e-9);
    }
}
