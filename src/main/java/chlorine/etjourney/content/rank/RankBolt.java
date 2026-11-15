package chlorine.etjourney.content.rank;

import chlorine.etjourney.content.rank.client.RankBoltClient;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/** Server to client: a lightning bolt to draw; the server never makes one itself, so nothing burns. */
public final class RankBolt implements IMessage {

    double x, y, z;

    public RankBolt() {}

    public RankBolt(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        x = buf.readDouble();
        y = buf.readDouble();
        z = buf.readDouble();
    }

    /** Runs on the network thread, so the client class hands the bolt to the client thread. */
    public static final class Handler implements IMessageHandler<RankBolt, IMessage> {

        @Override
        public IMessage onMessage(RankBolt message, MessageContext ctx) {
            RankBoltClient.strike(message.x, message.y, message.z);
            return null;
        }
    }
}
