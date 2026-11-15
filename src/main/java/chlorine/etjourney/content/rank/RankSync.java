package chlorine.etjourney.content.rank;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;

import chlorine.etjourney.content.rank.ability.RankAbilities;
import chlorine.etjourney.content.rank.ability.RankAbility;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/** Server to client: how one mob shows its rank, sent when a player starts tracking it. */
public final class RankSync implements IMessage {

    static final float SPLIT_CHILD_SCALE = 0.75f;

    int entityId, rank;
    float scale;

    public RankSync() {}

    RankSync(Entity e) {
        entityId = e.getEntityId();
        rank = RankData.rank(e)
            .ordinal();
        scale = scaleOf(e);
    }

    /** Only mobs that look different are sent. */
    static boolean worthSending(Entity e) {
        return RankData.ranked(e) || RankData.role(e) == RankData.SPLIT_CHILD;
    }

    /** The factor a mob is drawn at: the split child's shrink times each ability's own. */
    static float scaleOf(Entity e) {
        List<Float> parts = new ArrayList<>();
        if (e instanceof EntityLiving)
            for (RankAbility a : RankAbilities.of((EntityLiving) e)) parts.add(a.renderScale());
        return combine(RankData.role(e) == RankData.SPLIT_CHILD, parts);
    }

    static float combine(boolean splitChild, List<Float> parts) {
        float scale = splitChild ? SPLIT_CHILD_SCALE : 1;
        for (float part : parts) scale *= part;
        return scale;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeByte(rank);
        buf.writeFloat(scale);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        entityId = buf.readInt();
        rank = buf.readByte();
        scale = buf.readFloat();
    }

    /** Runs on the network thread, so it only fills the client's map. */
    public static final class Handler implements IMessageHandler<RankSync, IMessage> {

        @Override
        public IMessage onMessage(RankSync message, MessageContext ctx) {
            RankViews.put(message.entityId, Rank.of(message.rank), message.scale);
            return null;
        }
    }
}
