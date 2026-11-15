package chlorine.etjourney.world.atmosphere;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/** The grid crosses the wire unchanged, negative origins included. */
class AtmosphereSyncTest {

    @Test
    void roundTrips() {
        byte[] ids = new byte[AtmosphereGrid.SIZE * AtmosphereGrid.SIZE];
        for (int i = 0; i < ids.length; i++) ids[i] = (byte) (i % 6);
        ByteBuf buf = Unpooled.buffer();
        new AtmosphereSync(1, new AtmosphereGrid(-7, 12, ids)).toBytes(buf);

        AtmosphereSync read = new AtmosphereSync();
        read.fromBytes(buf);
        assertEquals(1, read.dimension);
        assertEquals(-7, read.grid.originX());
        assertEquals(12, read.grid.originZ());
        assertArrayEquals(ids, read.grid.ids());
    }
}
