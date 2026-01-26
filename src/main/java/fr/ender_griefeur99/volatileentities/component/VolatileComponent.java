package fr.ender_griefeur99.volatileentities.component;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.ender_griefeur99.volatileentities.config.VolatileConfig;
import fr.ender_griefeur99.volatileentities.config.VolatileContext;
import fr.ender_griefeur99.volatileentities.policy.VolatilePolicy;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Core component marking an entity as "volatile".
 *
 * Responsibilities:
 * <ul>
 *     <li>Defines volatile policies via a {@link VolatileConfig}.</li>
 *     <li>Tracks an idle timeout (if enabled).</li>
 *     <li>Tracks whether the entity was loaded from disk (expiredOnLoad).</li>
 * </ul>
 *
 * Kill-on-load:
 * <ul>
 *     <li>Runtime instances are created with {@link #VolatileComponent(VolatileConfig)} => expiredOnLoad = false.</li>
 *     <li>Instances loaded via {@link #CODEC} use the no-arg constructor and set expiredOnLoad = true.</li>
 *     <li>{@link fr.ender_griefeur99.volatileentities.system.VolatileTickSystem} removes
 *         entities with expiredOnLoad == true on the first tick after a restart.</li>
 * </ul>
 */
@Getter
public final class VolatileComponent implements Component<EntityStore> {

    /**
     * Component type, registered by the plugin at startup.
     */
    public static ComponentType<EntityStore, VolatileComponent> TYPE;

    /**
     * Codec used to serialize VolatileComponent to disk.
     * <p>
     * We only persist a timestamp and mark expiredOnLoad when read back.
     */
    @Nonnull
    public static final BuilderCodec<VolatileComponent> CODEC =
            BuilderCodec.builder(VolatileComponent.class, VolatileComponent::new)
                    .append(
                            new KeyedCodec<>("Timestamp", Codec.LONG),
                            (c, v) -> {
                                c.timestamp = v;
                                c.expiredOnLoad = true; // any entity loaded from disk is considered expired
                            },
                            c -> c.timestamp
                    )
                    .add()
                    .build();

    // ----- Backing config -----

    @Nonnull
    private final VolatileConfig config;

    /**
     * Remaining idle ticks before timeout (for IDLE_TIMEOUT).
     */
    @Setter
    private int idleTicksRemaining;

    /**
     * Whether this entity has been marked for removal externally.
     * If true, {@link fr.ender_griefeur99.volatileentities.system.VolatileTickSystem}
     * will remove it on the next tick.
     */
    @Setter
    private boolean markedForRemoval;

    /**
     * Optional reason string for removal (useful for logging or debugging).
     */
    @Setter
    @Nullable
    private String removalReason;

    /**
     * Creation or last-update timestamp, persisted by the codec.
     */
    public long timestamp;

    /**
     * True when this component was created via the codec (i.e. loaded from disk),
     * false for runtime instances.
     */
    @Setter
    private boolean expiredOnLoad;

    // ----- Constructors -----

    /**
     * Constructor used by the codec only.
     * <p>
     * It creates a config that always despawns on the next tick (CUSTOM that always returns true).
     * The real timestamp is injected by the codec.
     */
    public VolatileComponent() {
        Predicate<VolatileContext> killAlways = ctx -> true;
        this.config = VolatileConfig.custom(killAlways);
        this.idleTicksRemaining = 0;
        this.markedForRemoval = false;
        this.removalReason = null;
        this.timestamp = 0L;
        this.expiredOnLoad = true;
    }

    /**
     * Runtime constructor. This is what you should use in code to create
     * new volatile entities.
     */
    public VolatileComponent(@Nonnull VolatileConfig config) {
        config.validate();
        this.config = config;
        this.idleTicksRemaining = config.getIdleTimeoutTicks();
        this.markedForRemoval = false;
        this.removalReason = null;
        this.timestamp = System.currentTimeMillis();
        this.expiredOnLoad = false;
    }

    // ----- Convenience factories -----

    @Nonnull
    public static VolatileComponent chunkBound() {
        return new VolatileComponent(VolatileConfig.chunkBound());
    }

    @Nonnull
    public static VolatileComponent linkedTo(@Nonnull Ref<EntityStore> entity) {
        return new VolatileComponent(VolatileConfig.linkedTo(entity));
    }

    @Nonnull
    public static VolatileComponent withinDistance(@Nonnull Ref<EntityStore> entity, float distance) {
        return new VolatileComponent(VolatileConfig.withinDistance(entity, distance));
    }

    @Nonnull
    public static VolatileComponent ownedBy(@Nonnull UUID playerUuid) {
        return new VolatileComponent(VolatileConfig.ownedBy(playerUuid));
    }

    @Nonnull
    public static VolatileComponent withTimeout(int ticks) {
        return new VolatileComponent(VolatileConfig.withTimeout(ticks));
    }

    @Nonnull
    public static VolatileComponent withTimeoutSeconds(float seconds, int tickRate) {
        return withTimeout((int) (seconds * tickRate));
    }

    @Nonnull
    public static VolatileComponent worldBounded() {
        return new VolatileComponent(VolatileConfig.worldBounded());
    }

    @Nonnull
    public static VolatileComponent projectile(@Nonnull Ref<EntityStore> shooter, float maxDistance) {
        return new VolatileComponent(VolatileConfig.projectile(shooter, maxDistance));
    }

    @Nonnull
    public static VolatileComponent temporaryEffect(int durationTicks) {
        return new VolatileComponent(VolatileConfig.temporaryEffect(durationTicks));
    }

    /**
     * Config that delegates TTL handling exclusively to DespawnComponent.
     */
    @Nonnull
    public static VolatileComponent useDespawnTtl() {
        return new VolatileComponent(VolatileConfig.useDespawnTtl());
    }

    /**
     * "Restart only" volatile: no runtime TTL, but will be removed on restart.
     */
    @Nonnull
    public static VolatileComponent restartOnly() {
        // CUSTOM that never returns true (no runtime removal),
        // but the kill-on-load behavior still applies on restart.
        return new VolatileComponent(VolatileConfig.custom(ctx -> false));
    }

    @Nonnull
    public static VolatileComponent of(@Nonnull VolatileConfig config) {
        return new VolatileComponent(config);
    }

    // ----- Config delegates -----

    public boolean hasPolicy(@Nonnull VolatilePolicy policy) {
        return config.hasPolicy(policy);
    }

    @Nullable
    public Ref<EntityStore> getLinkedEntity() {
        return config.getLinkedEntity();
    }

    @Nullable
    public UUID getOwnerUuid() {
        return config.getOwnerUuid();
    }

    public float getMaxDistanceSquared() {
        return config.getMaxDistanceSquared();
    }

    public int getIdleTimeoutTicks() {
        return config.getIdleTimeoutTicks();
    }

    // ----- Idle timeout handling -----

    /**
     * Decrements the idle timeout (if enabled).
     *
     * @return true if the timeout has expired.
     */
    public boolean tickIdle() {
        if (idleTicksRemaining > 0) {
            idleTicksRemaining--;
        }
        return isIdleTimedOut();
    }

    public void resetIdleTicks() {
        idleTicksRemaining = config.getIdleTimeoutTicks();
    }

    public boolean isIdleTimedOut() {
        return config.hasPolicy(VolatilePolicy.IDLE_TIMEOUT) && idleTicksRemaining <= 0;
    }

    public float getTimeRemainingSeconds(int tickRate) {
        return (float) idleTicksRemaining / tickRate;
    }

    // ----- Removal / invalidation -----

    public void markForRemoval(@Nullable String reason) {
        this.markedForRemoval = true;
        this.removalReason = reason;
    }

    public void markForRemoval() {
        markForRemoval(null);
    }

    @Nonnull
    public static ComponentType<EntityStore, VolatileComponent> getComponentType() {
        if (TYPE == null) {
            throw new IllegalStateException(
                    "VolatileComponent.TYPE not initialized. Is VolatileEntitiesPlugin loaded?"
            );
        }
        return TYPE;
    }

    // ----- Cloning -----

    @Nonnull
    @Override
    public VolatileComponent clone() {
        VolatileComponent clone = new VolatileComponent(this.config);
        clone.idleTicksRemaining = this.idleTicksRemaining;
        clone.markedForRemoval = this.markedForRemoval;
        clone.removalReason = this.removalReason;
        clone.timestamp = this.timestamp;
        clone.expiredOnLoad = this.expiredOnLoad;
        return clone;
    }

    @Override
    public String toString() {
        return "VolatileComponent{" +
                "policies=" + config.getPolicies() +
                ", idleTicksRemaining=" + idleTicksRemaining +
                ", markedForRemoval=" + markedForRemoval +
                ", expiredOnLoad=" + expiredOnLoad +
                ", timestamp=" + timestamp +
                '}';
    }
}