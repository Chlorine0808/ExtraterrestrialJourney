package chlorine.etjourney.content.rank;

import net.minecraft.entity.Entity;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/** Server to client: how one mob shows its rank, sent when a player starts tracking it. */
public final class RankSync implements IMessage {

    int entityId, rank;
    boolean small;

    public RankSync() {}

    RankSync(Entity e) {
        entityId = e.getEntityId();
        rank = RankData.rank(e)
            .ordinal();
        small = RankData.role(e) == RankData.SPLIT_CHILD;
    }

    /** Only mobs that look different are sent. */
    static boolean worthSending(Entity e) {
        return RankData.ranked(e) || RankData.role(e) == RankData.SPLIT_CHILD;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeByte(rank);
        buf.writeBoolean(small);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        entityId = buf.readInt();
        rank = buf.readByte();
        small = buf.readBoolean();
    }

    /** Runs on the network thread, so it only fills the client's map. */
    public static final class Handler implements IMessageHandler<RankSync, IMessage> {

        @Override
        public IMessage onMessage(RankSync message, MessageContext ctx) {
            RankViews.put(message.entityId, Rank.of(message.rank), message.small);
            return null;
        }
    }
}
