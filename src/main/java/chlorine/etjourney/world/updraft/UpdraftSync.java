package chlorine.etjourney.world.updraft;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import chlorine.etjourney.world.updraft.model.Updraft;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/** Server to client: every updraft of one dimension, replacing what the client knew. */
public final class UpdraftSync implements IMessage {

    int dimension;
    List<Updraft> updrafts = new ArrayList<>();

    public UpdraftSync() {}

    UpdraftSync(int dimension, Collection<Updraft> updrafts) {
        this.dimension = dimension;
        this.updrafts = new ArrayList<>(updrafts);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(dimension);
        buf.writeInt(updrafts.size());
        for (Updraft u : updrafts) {
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
        updrafts = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            double[] v = new double[11];
            for (int k = 0; k < v.length; k++) v[k] = buf.readDouble();
            updrafts.add(new Updraft(v[0], v[1], v[2], v[3], v[4], v[5], v[6], v[7], v[8], v[9], v[10]));
        }
    }

    /** Runs on the network thread, so it only swaps the client's snapshot. */
    public static final class Handler implements IMessageHandler<UpdraftSync, IMessage> {

        @Override
        public IMessage onMessage(UpdraftSync message, MessageContext ctx) {
            UpdraftView.set(message.dimension, message.updrafts);
            return null;
        }
    }
}
