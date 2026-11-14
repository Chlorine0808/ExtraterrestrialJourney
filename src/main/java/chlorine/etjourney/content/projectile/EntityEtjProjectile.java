package chlorine.etjourney.content.projectile;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import chlorine.etjourney.core.damage.ElementalDamageSource;
import chlorine.etjourney.core.damage.ElementalDamages;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.registry.IEntityAdditionalSpawnData;
import io.netty.buffer.ByteBuf;

/** Every kind of shot; its spec decides how it flies and what it does. Never saved with the world. */
public class EntityEtjProjectile extends Entity implements IEntityAdditionalSpawnData {

    private static final int TARGET_WATCHER = 16;
    private static final int RETARGET_TICKS = 10;
    private static final double VOID_Y = -64;

    private ProjectileSpec spec;
    private EntityLivingBase shooter;
    private Faction faction = Faction.ENEMY;
    private Entity target;
    private int age;
    private final Set<Integer> passed = new HashSet<>();

    /** For the client and the entity registry; a shot without a spec dies on its first tick. */
    public EntityEtjProjectile(World world) {
        super(world);
        setSize(0.25F, 0.25F);
        renderDistanceWeight = 10;
    }

    EntityEtjProjectile(World world, ProjectileSpec spec, EntityLivingBase shooter, double x, double y, double z,
        double[] velocity, Entity target) {
        this(world);
        this.spec = spec;
        this.shooter = shooter;
        faction = Faction.of(shooter instanceof EntityPlayer);
        this.target = target;
        setSize(spec.size, spec.size);
        setPosition(x, y, z);
        motionX = velocity[0];
        motionY = velocity[1];
        motionZ = velocity[2];
        if (target != null) dataWatcher.updateObject(TARGET_WATCHER, target.getEntityId());
    }

    @Override
    protected void entityInit() {
        dataWatcher.addObject(TARGET_WATCHER, -1);
    }

    public ProjectileSpec spec() {
        return spec;
    }

    public EntityLivingBase shooter() {
        return shooter;
    }

    public Faction faction() {
        return faction;
    }

    public int age() {
        return age;
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (spec == null) {
            setDead();
            return;
        }
        age++;
        if (!worldObj.isRemote && (age > spec.lifetime || posY < VOID_Y)) {
            for (ImpactEffect e : spec.effects) e.onExpire(this);
            setDead();
            return;
        }
        Vec3 from = Vec3.createVectorHelper(posX, posY, posZ);
        Vec3 to = Vec3.createVectorHelper(posX + motionX, posY + motionY, posZ + motionZ);
        MovingObjectPosition wall = spec.piercesWalls ? null
            : worldObj
                .func_147447_a(from, Vec3.createVectorHelper(to.xCoord, to.yCoord, to.zCoord), false, true, false);
        if (wall != null) to = wall.hitVec;
        if (!worldObj.isRemote && hitAlong(from, to)) return;
        if (wall != null) {
            setPosition(to.xCoord, to.yCoord, to.zCoord);
            if (!worldObj.isRemote) {
                for (ImpactEffect e : spec.effects) e.onBlock(this, wall);
                setDead();
            }
            return;
        }
        setPosition(to.xCoord, to.yCoord, to.zCoord);
        if (worldObj.isRemote) trail();
        steer();
    }

    private void steer() {
        double[] toTarget = null;
        if (spec.homing != null) {
            Entity t = homingTarget();
            if (t != null) toTarget = new double[] { t.posX - posX, t.posY + t.height / 2 - posY, t.posZ - posZ };
        }
        double[] v = ProjectileMotion
            .step(new double[] { motionX, motionY, motionZ }, spec.gravity, spec.drag, spec.homing, age, toTarget);
        motionX = v[0];
        motionY = v[1];
        motionZ = v[2];
    }

    /** The server picks the target and tells the client its ID. */
    private Entity homingTarget() {
        if (worldObj.isRemote) return worldObj.getEntityByID(dataWatcher.getWatchableObjectInt(TARGET_WATCHER));
        boolean lost = target == null || target.isDead;
        if (lost && (age % RETARGET_TICKS == 1 || target != null)) {
            target = nearestTarget(spec.homing.range);
            dataWatcher.updateObject(TARGET_WATCHER, target != null ? target.getEntityId() : -1);
        }
        return target;
    }

    private Entity nearestTarget(double range) {
        AxisAlignedBB box = boundingBox.expand(range, range, range);
        Entity best = null;
        double bestSq = range * range;
        for (Object o : worldObj.getEntitiesWithinAABBExcludingEntity(this, box)) {
            if (!(o instanceof EntityLivingBase) || !canHit((Entity) o)) continue;
            double d = getDistanceSqToEntity((Entity) o);
            if (d < bestSq) {
                bestSq = d;
                best = (Entity) o;
            }
        }
        return best;
    }

    private boolean canHit(Entity e) {
        if (!(e instanceof EntityLivingBase) || e.isDead || !e.canBeCollidedWith()) return false;
        return faction.hits(e instanceof EntityPlayer, e == shooter);
    }

    /** Hits what lies on the segment, nearest first; true once the shot is spent. */
    private boolean hitAlong(Vec3 from, Vec3 to) {
        double pad = spec.size / 2;
        AxisAlignedBB sweep = boundingBox.addCoord(to.xCoord - posX, to.yCoord - posY, to.zCoord - posZ)
            .expand(1, 1, 1);
        List<double[]> order = new ArrayList<>();
        List<EntityLivingBase> found = new ArrayList<>();
        for (Object o : worldObj.getEntitiesWithinAABBExcludingEntity(this, sweep)) {
            Entity e = (Entity) o;
            if (!canHit(e) || passed.contains(e.getEntityId())) continue;
            AxisAlignedBB box = e.boundingBox.expand(pad, pad, pad);
            double d;
            if (box.isVecInside(from)) d = 0;
            else {
                MovingObjectPosition m = box.calculateIntercept(from, to);
                if (m == null) continue;
                d = from.distanceTo(m.hitVec);
            }
            order.add(new double[] { d, found.size() });
            found.add((EntityLivingBase) e);
        }
        order.sort((a, b) -> Double.compare(a[0], b[0]));
        for (double[] o : order) {
            EntityLivingBase e = found.get((int) o[1]);
            hit(e);
            passed.add(e.getEntityId());
            if (passed.size() > spec.piercesEntities) {
                setDead();
                return true;
            }
        }
        return false;
    }

    private void hit(EntityLivingBase e) {
        ElementalDamageSource source = new ElementalDamageSource(spec.damage, this, shooter);
        source.setProjectile();
        boolean hurt = ElementalDamages.deal(e, source);
        for (ImpactEffect effect : spec.effects) effect.onEntity(this, e, hurt);
    }

    private void trail() {
        if (spec.trail.isEmpty()) return;
        for (int i = 0; i < 2; i++) {
            double f = i / 2.0;
            worldObj.spawnParticle(spec.trail, posX - motionX * f, posY - motionY * f, posZ - motionZ * f, 0, 0, 0);
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
        buf.writeDouble(motionX);
        buf.writeDouble(motionY);
        buf.writeDouble(motionZ);
    }

    @Override
    public void readSpawnData(ByteBuf buf) {
        spec = ProjectileTypes.get(ByteBufUtils.readUTF8String(buf));
        motionX = buf.readDouble();
        motionY = buf.readDouble();
        motionZ = buf.readDouble();
        if (spec != null) setSize(spec.size, spec.size);
    }
}
