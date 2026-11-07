package chlorine.etjourney.world.wind;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import chlorine.etjourney.world.wind.model.Wind;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/** Server to client: every wind of one dimension, replacing what the client knew. */
public final class WindSync implements IMessage {

    int dimension;
    List<Wind> winds = new ArrayList<>();

    public WindSync() {}

    WindSync(int dimension, Collection<Wind> winds) {
        this.dimension = dimension;
        this.winds = new ArrayList<>(winds);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(dimension);
        buf.writeInt(winds.size());
        for (Wind u : winds) {
            buf.writeDouble(u.minX);
            buf.writeDouble(u.minY);
            buf.writeDouble(u.minZ);
            buf.writeDouble(u.maxX);
            buf.writeDouble(u.maxY);
            buf.writeDouble(u.maxZ);
            buf.writeDouble(u.dirX);
            buf.writeDouble(u.dirY);
            buf.writeDouble(u.dirZ);
            buf.writeDouble(u.lift);
            buf.writeDouble(u.maxSpeed);
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        dimension = buf.readInt();
        int n = buf.readInt();
        winds = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            double[] v = new double[11];
            for (int k = 0; k < v.length; k++) v[k] = buf.readDouble();
            winds.add(new Wind(v[0], v[1], v[2], v[3], v[4], v[5], v[6], v[7], v[8], v[9], v[10]));
        }
    }

    /** Runs on the network thread, so it only swaps the client's snapshot. */
    public static final class Handler implements IMessageHandler<WindSync, IMessage> {

        @Override
        public IMessage onMessage(WindSync message, MessageContext ctx) {
            WindView.set(message.dimension, message.winds);
            return null;
        }
    }
}
