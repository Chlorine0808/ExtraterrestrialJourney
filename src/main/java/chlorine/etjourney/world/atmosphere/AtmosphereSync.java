package chlorine.etjourney.world.atmosphere;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/** Server to client: the atmosphere grid around the player, replacing the one the client had. */
public final class AtmosphereSync implements IMessage {

    int dimension;
    AtmosphereGrid grid;

    public AtmosphereSync() {}

    AtmosphereSync(int dimension, AtmosphereGrid grid) {
        this.dimension = dimension;
        this.grid = grid;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(dimension);
        buf.writeInt(grid.originX());
        buf.writeInt(grid.originZ());
        buf.writeBytes(grid.ids());
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        dimension = buf.readInt();
        int ox = buf.readInt(), oz = buf.readInt();
        byte[] ids = new byte[AtmosphereGrid.SIZE * AtmosphereGrid.SIZE];
        buf.readBytes(ids);
        grid = new AtmosphereGrid(ox, oz, ids);
    }

    /** Runs on the network thread, so it only swaps the client's snapshot. */
    public static final class Handler implements IMessageHandler<AtmosphereSync, IMessage> {

        @Override
        public IMessage onMessage(AtmosphereSync message, MessageContext ctx) {
            AtmosphereView.set(message.dimension, message.grid);
            return null;
        }
    }
}
