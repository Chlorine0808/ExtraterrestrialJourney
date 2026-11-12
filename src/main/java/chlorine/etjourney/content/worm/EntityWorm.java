package chlorine.etjourney.content.worm;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IEntityMultiPart;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.boss.EntityDragonPart;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import chlorine.etjourney.content.worm.WormPhases.Medium;
import chlorine.etjourney.content.worm.WormPhases.Phase;

/**
 * A worm's head, which is the whole worm as far as health, ranks and AI go. The body and tail are
 * {@link WormSegment}s that follow the head's trail and pass their hits to it.
 */
public abstract class EntityWorm extends EntityMob implements IEntityMultiPart {

    private static final int PHASE_WATCHER = 16;
    /** How far up from the head the warning looks for the ground's surface. */
    private static final int SURFACE_SCAN = 16;
    /** Ticks between new wander goals when there is no one to chase. */
    private static final int WANDER_TICKS = 100;

    public final WormSpec spec;
    private final WormSegment[] segments;
    private final WormTrail trail;
    private final WormPhases phases;
    private Phase shownPhase = Phase.ROAM;
    private int shownElapsed;
    private double wanderX, wanderY, wanderZ;
    private int wanderLeft;

    protected EntityWorm(World world, WormSpec spec) {
        super(world);
        this.spec = spec;
        float head = spec.size * spec.headScale;
        setSize(head, head);
        // Parts are made right after the head so their IDs follow its own, which the spawn packet relies on.
        segments = new WormSegment[spec.bodySegments + 1];
        for (int i = 0; i < segments.length; i++) {
            boolean tail = i == spec.bodySegments;
            segments[i] = new WormSegment(this, i, tail, tail ? spec.size * spec.tailScale : spec.size);
        }
        trail = new WormTrail(spec.length() + spec.spacing, spec.spacing * 8);
        phases = spec.newPhases();
        noClip = true;
        ignoreFrustumCheck = true;
        experienceValue = 20;
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataWatcher.addObject(PHASE_WATCHER, (byte) 0);
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.knockbackResistance).setBaseValue(1.0D);
    }

    @Override
    public Entity[] getParts() {
        return segments;
    }

    @Override
    public World func_82194_d() {
        return worldObj;
    }

    @Override
    public boolean attackEntityFromPart(EntityDragonPart part, DamageSource source, float amount) {
        return attackEntityFrom(source, amount * spec.partDamageFactor);
    }

    /** The phase as the client last heard it, or the real one on the server. */
    public Phase phase() {
        return worldObj.isRemote ? Phase.values()[dataWatcher.getWatchableObjectByte(PHASE_WATCHER)] : phases.phase();
    }

    /** Centre of the head, which the trail follows. */
    public double centreY() {
        return posY + height / 2;
    }

    @Override
    public void onLivingUpdate() {
        if (worldObj.isRemote) interpolate();
        else if (getHealth() > 0) think();
        renderYawOffset = rotationYawHead = rotationYaw;
        followTrail();
        if (worldObj.isRemote) showEffects();
        else if (getHealth() > 0) touch();
    }

    // ---- server: where the head goes ----

    private void think() {
        EntityLivingBase target = pickTarget();
        double hDist = target == null ? 0 : Math.hypot(target.posX - posX, target.posZ - posZ);
        boolean buried = isSolid(posX, centreY(), posZ);
        boolean leapt = phases.tick(target != null, hDist, buried, motionY < 0);
        Phase phase = phases.phase();
        if (phase.ordinal() != dataWatcher.getWatchableObjectByte(PHASE_WATCHER)) {
            dataWatcher.updateObject(PHASE_WATCHER, (byte) phase.ordinal());
            if (phase == Phase.WARN) onWarn();
        }
        if (leapt) launch(target, hDist);
        else if (phase == Phase.LEAP) {
            if (spec.medium == Medium.GROUND) motionY -= spec.gravity;
        } else if (spec.medium == Medium.GROUND && !buried && phase == Phase.ROAM) {
            motionY -= spec.gravity;
            motionX *= 0.98;
            motionZ *= 0.98;
        } else {
            double slow = phase == Phase.WARN ? 0.3 : 1.0;
            steer(goal(target), spec.speed * slow);
        }
        setPosition(posX + motionX, posY + motionY, posZ + motionZ);
        face(motionX, motionY, motionZ);
    }

    private EntityLivingBase pickTarget() {
        Entity current = entityToAttack;
        if (current instanceof EntityLivingBase && current.isEntityAlive()
            && current.dimension == dimension
            && getDistanceSqToEntity(current) < spec.followRange * spec.followRange
            && !(current instanceof EntityPlayer && ((EntityPlayer) current).capabilities.disableDamage)) {
            return (EntityLivingBase) current;
        }
        EntityPlayer player = worldObj.getClosestVulnerablePlayerToEntity(this, spec.followRange);
        entityToAttack = player;
        return player;
    }

    /** Where the head swims towards: under (or over) the target, or a wander spot. */
    private double[] goal(EntityLivingBase target) {
        double sign = spec.medium == Medium.GROUND ? -1 : 1;
        if (target != null) {
            if (spec.medium == Medium.AIR && phases.phase() == Phase.ROAM) {
                // Circle over the target rather than hover on top of it.
                double a = ticksExisted * 0.05;
                return new double[] { target.posX + Math.cos(a) * 8, target.posY + spec.depth,
                    target.posZ + Math.sin(a) * 8 };
            }
            return new double[] { target.posX, target.posY + sign * spec.depth, target.posZ };
        }
        if (--wanderLeft <= 0 || getDistanceSq(wanderX, wanderY, wanderZ) < 4) {
            wanderLeft = WANDER_TICKS;
            wanderX = posX + (rand.nextDouble() - 0.5) * 32;
            wanderZ = posZ + (rand.nextDouble() - 0.5) * 32;
            wanderY = posY + (rand.nextDouble() - 0.5) * 6;
        }
        return new double[] { wanderX, wanderY, wanderZ };
    }

    /** Turns the velocity towards the goal, never faster than the turn rate. */
    private void steer(double[] goal, double speed) {
        double dx = goal[0] - posX, dy = goal[1] - centreY(), dz = goal[2] - posZ;
        double n = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (n < 1e-6) return;
        double r = spec.turnRate;
        motionX += (dx / n * speed - motionX) * r;
        motionY += (dy / n * speed - motionY) * r;
        motionZ += (dz / n * speed - motionZ) * r;
    }

    private void launch(EntityLivingBase target, double hDist) {
        if (target == null) return;
        double dx = target.posX - posX, dz = target.posZ - posZ;
        if (spec.medium == Medium.GROUND) {
            double rise = Math.max(0, surfaceY() - centreY());
            double[] v = LeapArc.launch(dx, dz, hDist + spec.overshoot, spec.leapApex + rise, spec.gravity);
            motionX = v[0];
            motionY = v[1];
            motionZ = v[2];
        } else {
            double dy = target.posY + target.height / 2 - centreY();
            double n = Math.max(1e-6, Math.sqrt(dx * dx + dy * dy + dz * dz));
            motionX = dx / n * spec.dashSpeed;
            motionY = dy / n * spec.dashSpeed;
            motionZ = dz / n * spec.dashSpeed;
        }
        onLeap();
    }

    /** Called on the server as a warning starts. */
    protected void onWarn() {
        Block ground = surfaceBlock();
        if (ground != null) {
            worldObj.playSoundEffect(posX, surfaceY(), posZ, ground.stepSound.getBreakSound(), 1.5F, 0.6F);
        }
    }

    /** Called on the server as a leap starts. */
    protected void onLeap() {}

    /** Called on the server when the head or a segment touches a player. */
    protected void onTouch(EntityPlayer player) {
        attackEntityAsMob(player);
    }

    private void face(double mx, double my, double mz) {
        double h = Math.sqrt(mx * mx + mz * mz);
        if (h < 1e-6 && Math.abs(my) < 1e-6) return;
        rotationYaw = (float) (Math.atan2(-mx, mz) * 180 / Math.PI);
        rotationPitch = (float) (-Math.atan2(my, h) * 180 / Math.PI);
    }

    // ---- both sides: the body ----

    private void followTrail() {
        double cy = centreY();
        if (trail.size() == 0) {
            float yaw = rotationYaw * (float) Math.PI / 180;
            trail.lay(posX, cy, posZ, -MathHelper.sin(yaw), 0, MathHelper.cos(yaw));
        }
        trail.record(posX, cy, posZ);
        for (WormSegment s : segments) {
            double back = (s.index + 1) * spec.spacing;
            double[] at = trail.at(back);
            double[] ahead = trail.at(back - spec.spacing);
            double dx = ahead[0] - at[0], dy = ahead[1] - at[1], dz = ahead[2] - at[2];
            float yaw = (float) (Math.atan2(-dx, dz) * 180 / Math.PI);
            float pitch = (float) (-Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)) * 180 / Math.PI);
            s.place(at[0], at[1], at[2], yaw, pitch);
        }
    }

    private void touch() {
        touch(boundingBox);
        for (WormSegment s : segments) touch(s.boundingBox);
    }

    private void touch(AxisAlignedBB box) {
        List<?> near = worldObj.getEntitiesWithinAABB(EntityPlayer.class, box.expand(0.2, 0.2, 0.2));
        for (Object o : near) onTouch((EntityPlayer) o);
    }

    // ---- client: interpolation and effects ----

    private void interpolate() {
        if (newPosRotationIncrements <= 0) return;
        double x = posX + (newPosX - posX) / newPosRotationIncrements;
        double y = posY + (newPosY - posY) / newPosRotationIncrements;
        double z = posZ + (newPosZ - posZ) / newPosRotationIncrements;
        double dYaw = MathHelper.wrapAngleTo180_double(newRotationYaw - rotationYaw);
        rotationYaw = (float) (rotationYaw + dYaw / newPosRotationIncrements);
        rotationPitch = (float) (rotationPitch + (newRotationPitch - rotationPitch) / newPosRotationIncrements);
        newPosRotationIncrements--;
        setPosition(x, y, z);
        setRotation(rotationYaw, rotationPitch);
    }

    private void showEffects() {
        Phase now = phase();
        if (now != shownPhase) {
            shownPhase = now;
            shownElapsed = 0;
        } else shownElapsed++;
        if (now == Phase.WARN) showWarning(WormPhases.warnDust(shownElapsed, spec.warnTicks));
        breach();
        ambient();
    }

    /** Draws the warning; by default dust where the head will break out, or sparks round a flying head. */
    protected void showWarning(int puffs) {
        if (spec.medium == Medium.AIR) {
            for (int i = 0; i < puffs; i++) {
                worldObj.spawnParticle(
                    "fireworksSpark",
                    posX + jitter(1),
                    centreY() + jitter(1),
                    posZ + jitter(1),
                    0,
                    0,
                    0);
            }
            return;
        }
        Block ground = surfaceBlock();
        if (ground == null) return;
        double y = surfaceY();
        int meta = worldObj.getBlockMetadata(
            MathHelper.floor_double(posX),
            MathHelper.floor_double(y) - 1,
            MathHelper.floor_double(posZ));
        String crack = "blockcrack_" + Block.getIdFromBlock(ground) + "_" + meta;
        for (int i = 0; i < puffs * 3; i++) {
            worldObj
                .spawnParticle(crack, posX + jitter(1.5), y + 0.1, posZ + jitter(1.5), jitter(0.2), 0.3, jitter(0.2));
        }
        worldObj.spawnParticle("largesmoke", posX + jitter(1), y + 0.1, posZ + jitter(1), 0, 0.05 * puffs, 0);
    }

    /** Kicks up block bits where the body crosses the ground's surface, so it looks like it swims through. */
    private void breach() {
        boolean inside = isSolid(posX, centreY(), posZ);
        for (WormSegment s : segments) {
            double cy = s.posY + s.height / 2;
            boolean now = isSolid(s.posX, cy, s.posZ);
            if (now != inside && rand.nextInt(3) == 0) {
                Block b = worldObj.getBlock(
                    MathHelper.floor_double(s.posX),
                    MathHelper.floor_double(cy) - (now ? 0 : 1),
                    MathHelper.floor_double(s.posZ));
                if (b.getMaterial()
                    .isSolid()) {
                    String crack = "blockcrack_" + Block.getIdFromBlock(b) + "_0";
                    worldObj.spawnParticle(crack, s.posX, cy, s.posZ, jitter(0.3), 0.2, jitter(0.3));
                }
            }
            inside = now;
        }
    }

    /** Particles a kind of worm gives off all the time. */
    protected void ambient() {}

    protected double jitter(double spread) {
        return (rand.nextDouble() - 0.5) * 2 * spread;
    }

    // ---- terrain ----

    private boolean isSolid(double x, double y, double z) {
        Block b = worldObj.getBlock(MathHelper.floor_double(x), MathHelper.floor_double(y), MathHelper.floor_double(z));
        return b.getMaterial()
            .isSolid();
    }

    /** The y of the first open block above the head, or the head's own height when none is near. */
    private double surfaceY() {
        int x = MathHelper.floor_double(posX), z = MathHelper.floor_double(posZ);
        int y = MathHelper.floor_double(centreY());
        for (int i = 0; i < SURFACE_SCAN; i++, y++) {
            if (!worldObj.getBlock(x, y, z)
                .getMaterial()
                .isSolid()) return y;
        }
        return centreY();
    }

    /** The block the surface above the head is made of, or null when the head is not under ground. */
    private Block surfaceBlock() {
        double y = surfaceY();
        Block b = worldObj
            .getBlock(MathHelper.floor_double(posX), MathHelper.floor_double(y) - 1, MathHelper.floor_double(posZ));
        return b.getMaterial()
            .isSolid() ? b : null;
    }

    // ---- vanilla behaviour a burrowing body must not have ----

    @Override
    public boolean isEntityInsideOpaqueBlock() {
        return false;
    }

    @Override
    protected void fall(float distance) {}

    @Override
    public boolean canBePushed() {
        return false;
    }

    @Override
    protected void collideWithEntity(Entity entity) {}

    @Override
    public void knockBack(Entity attacker, float strength, double dx, double dz) {}

    @Override
    protected boolean isAIEnabled() {
        return false;
    }

    @Override
    public boolean isInRangeToRenderDist(double distanceSq) {
        double reach = 64 + spec.length();
        return distanceSq < reach * reach;
    }

    @Override
    protected boolean canDespawn() {
        return false;
    }
}
