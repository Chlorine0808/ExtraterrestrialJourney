package chlorine.etjourney.content.area;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;

import chlorine.etjourney.content.projectile.Faction;
import chlorine.etjourney.core.damage.ElementalDamageSource;
import chlorine.etjourney.core.damage.ElementalDamages;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.registry.IEntityAdditionalSpawnData;
import io.netty.buffer.ByteBuf;

/**
 * Every kind of lingering area; its spec decides its size, life and what it does to those inside. A floor's
 * position is its base, a cloud's its centre. Never saved with the world.
 */
public class EntityLingeringArea extends Entity implements IEntityAdditionalSpawnData {

    private static final int RADIUS_WATCHER = 17;
    private static final int MAX_PARTICLES = 12;

    private AreaSpec spec;
    private EntityLivingBase owner;
    private Faction faction = Faction.ENEMY;
    private PulseClock clock;
    private int age, uses;

    /** For the client and the entity registry; an area without a spec dies on its first tick. */
    public EntityLingeringArea(World world) {
        super(world);
        setSize(0.5F, 0.5F);
        noClip = true;
        isImmuneToFire = true;
        renderDistanceWeight = 10;
    }

    EntityLingeringArea(World world, AreaSpec spec, EntityLivingBase owner, double x, double y, double z) {
        this(world);
        this.spec = spec;
        this.owner = owner;
        faction = Faction.of(owner instanceof EntityPlayer);
        clock = new PulseClock(spec.interval);
        setPosition(x, y, z);
        dataWatcher.updateObject(RADIUS_WATCHER, (float) spec.size.radius(0, 0));
    }

    @Override
    protected void entityInit() {
        dataWatcher.addObject(RADIUS_WATCHER, 0F);
    }

    public AreaSpec spec() {
        return spec;
    }

    /** Who left it; null for an enemy area with no one to blame. */
    public EntityLivingBase owner() {
        return owner;
    }

    public Faction faction() {
        return faction;
    }

    /** The radius now, on either side. */
    public double radius() {
        return dataWatcher.getWatchableObjectFloat(RADIUS_WATCHER);
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (spec == null) {
            setDead();
            return;
        }
        if (worldObj.isRemote) {
            scatter();
            return;
        }
        age++;
        if (!spec.size.alive(age, uses)) {
            setDead();
            return;
        }
        double r = spec.size.radius(age, uses);
        if ((float) r != radius()) dataWatcher.updateObject(RADIUS_WATCHER, (float) r);
        work(r);
    }

    private void work(double r) {
        double bottom = spec.shape.bottom(posY, spec.height), top = spec.shape.top(posY, spec.height);
        AxisAlignedBB box = AxisAlignedBB.getBoundingBox(posX - r, bottom, posZ - r, posX + r, top, posZ + r)
            .expand(1, 1, 1);
        int worked = 0;
        for (Object o : worldObj.getEntitiesWithinAABB(EntityLivingBase.class, box)) {
            EntityLivingBase e = (EntityLivingBase) o;
            if (e.isDead || e.getHealth() <= 0) continue;
            if (!AreaTargets.affects(faction, spec.hitsAllies, e instanceof EntityPlayer, e == owner)) continue;
            AxisAlignedBB b = e.boundingBox;
            if (!spec.shape.overlaps(
                r,
                spec.height,
                posX,
                posY,
                posZ,
                (b.minX + b.maxX) / 2,
                (b.minZ + b.maxZ) / 2,
                (b.maxX - b.minX) / 2,
                b.minY,
                b.maxY)) continue;
            if (spec.slow < 1) AreaSlow.mark(e, spec.slow);
            if (clock.due(e.getEntityId(), age)) {
                pulse(e);
                worked++;
            }
        }
        uses += worked;
    }

    private void pulse(EntityLivingBase e) {
        boolean hurt = false;
        if (spec.damage != null) hurt = ElementalDamages.deal(e, new ElementalDamageSource(spec.damage, this, owner));
        for (AreaEffect effect : spec.effects) effect.onPulse(this, e, hurt);
    }

    /** Particles over the area, more for a wider one. */
    private void scatter() {
        double r = radius();
        if (r <= 0) return;
        int n = (int) Math.min(MAX_PARTICLES, Math.ceil(r * r * 1.5));
        double bottom = spec.shape.bottom(posY, spec.height);
        for (int i = 0; i < n; i++) {
            double a = rand.nextDouble() * Math.PI * 2, d = Math.sqrt(rand.nextDouble()) * r;
            worldObj.spawnParticle(
                spec.particle,
                posX + Math.cos(a) * d,
                bottom + rand.nextDouble() * spec.height,
                posZ + Math.sin(a) * d,
                0,
                0.02,
                0);
        }
    }

    @Override
    public boolean writeToNBTOptional(NBTTagCompound tag) {
        return false;
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound tag) {}

    @Override
    protected void writeEntityToNBT(NBTTagCompound tag) {}

    @Override
    public void writeSpawnData(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, spec != null ? spec.name : "");
    }

    @Override
    public void readSpawnData(ByteBuf buf) {
        spec = AreaTypes.get(ByteBufUtils.readUTF8String(buf));
    }
}
