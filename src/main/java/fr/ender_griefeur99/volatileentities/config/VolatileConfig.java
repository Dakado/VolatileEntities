package fr.ender_griefeur99.volatileentities.config;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.ender_griefeur99.volatileentities.policy.VolatilePolicy;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Immutable configuration object describing how a volatile entity behaves.
 *
 * It combines a set of {@link VolatilePolicy} flags with supporting data (linked entity,
 * max distance, idle timeout, custom condition, etc.).
 */
@Getter
@Builder(toBuilder = true)
public final class VolatileConfig {

    /**
     * Active policies for this volatile entity.
     */
    @Singular
    @Nonnull
    private final Set<VolatilePolicy> policies;

    /**
     * Linked entity (for LINKED_ENTITY_INVALID / MAX_DISTANCE / OWNER_DISCONNECT use-cases).
     */
    @Nullable
    private final Ref<EntityStore> linkedEntity;

    /**
     * UUID of the owning player (for OWNER_DISCONNECT).
     */
    @Nullable
    private final UUID ownerUuid;

    /**
     * Maximum allowed distance from the linked entity (for MAX_DISTANCE).
     */
    @Builder.Default
    private final float maxDistance = 0f;

    /**
     * Idle timeout in ticks before removal (for IDLE_TIMEOUT).
     */
    @Builder.Default
    private final int idleTimeoutTicks = 0;

    /**
     * Custom condition for removal (for CUSTOM).
     */
    @Nullable
    private final Predicate<VolatileContext> customCondition;

    /**
     * Whether the entity should be immediately deleted when a policy condition is met,
     * or just marked as invalid and left for other systems to handle.
     */
    @Builder.Default
    private final boolean removeOnInvalid = true;

    public float getMaxDistanceSquared() {
        return maxDistance * maxDistance;
    }

    public boolean hasPolicy(@Nonnull VolatilePolicy policy) {
        return policies != null && policies.contains(policy);
    }

    /**
     * Validate this config at construction time.
     *
     * @throws IllegalStateException if required fields are missing for active policies.
     */
    public void validate() {
        if (policies == null || policies.isEmpty()) {
            throw new IllegalStateException("At least one VolatilePolicy must be specified");
        }

        for (VolatilePolicy policy : policies) {
            validatePolicy(policy);
        }
    }

    private void validatePolicy(@Nonnull VolatilePolicy policy) {
        if (policy.isRequiresLinkedEntity() && linkedEntity == null && ownerUuid == null) {
            throw new IllegalStateException(
                    "Policy " + policy + " requires a linked entity or owner UUID"
            );
        }
        if (policy.isRequiresDistance() && maxDistance <= 0) {
            throw new IllegalStateException(
                    "Policy " + policy + " requires a positive max distance, got: " + maxDistance
            );
        }
        if (policy.isRequiresTimeout() && idleTimeoutTicks <= 0) {
            throw new IllegalStateException(
                    "Policy " + policy + " requires a positive idle timeout, got: " + idleTimeoutTicks
            );
        }
        if (policy == VolatilePolicy.CUSTOM && customCondition == null) {
            throw new IllegalStateException(
                    "Policy CUSTOM requires a customCondition predicate"
            );
        }
    }

    // ---------- Presets ----------

    /**
     * Simple config: entity is removed when its chunk unloads (CHUNK_UNLOAD).
     */
    @Nonnull
    public static VolatileConfig chunkBound() {
        return VolatileConfig.builder()
                .policy(VolatilePolicy.CHUNK_UNLOAD)
                .build();
    }

    /**
     * Config: removed when the linked entity becomes invalid (LINKED_ENTITY_INVALID).
     */
    @Nonnull
    public static VolatileConfig linkedTo(@Nonnull Ref<EntityStore> entity) {
        return VolatileConfig.builder()
                .policy(VolatilePolicy.LINKED_ENTITY_INVALID)
                .linkedEntity(entity)
                .build();
    }

    /**
     * Config: removed when further than {@code distance} units from the linked entity (MAX_DISTANCE).
     */
    @Nonnull
    public static VolatileConfig withinDistance(@Nonnull Ref<EntityStore> entity, float distance) {
        return VolatileConfig.builder()
                .policy(VolatilePolicy.MAX_DISTANCE)
                .linkedEntity(entity)
                .maxDistance(distance)
                .build();
    }

    /**
     * Config: removed when the owning player disconnects (OWNER_DISCONNECT).
     */
    @Nonnull
    public static VolatileConfig ownedBy(@Nonnull UUID playerUuid) {
        return VolatileConfig.builder()
                .policy(VolatilePolicy.OWNER_DISCONNECT)
                .ownerUuid(playerUuid)
                .build();
    }

    /**
     * Config: removed after {@code ticks} of idle time (IDLE_TIMEOUT).
     */
    @Nonnull
    public static VolatileConfig withTimeout(int ticks) {
        return VolatileConfig.builder()
                .policy(VolatilePolicy.IDLE_TIMEOUT)
                .idleTimeoutTicks(ticks)
                .build();
    }

    @Nonnull
    public static VolatileConfig withTimeoutSeconds(float seconds, int tickRate) {
        return withTimeout((int) (seconds * tickRate));
    }

    /**
     * Config: removal when leaving world bounds (OUT_OF_BOUNDS).
     */
    @Nonnull
    public static VolatileConfig worldBounded() {
        return VolatileConfig.builder()
                .policy(VolatilePolicy.OUT_OF_BOUNDS)
                .build();
    }

    /**
     * Typical projectile config (MAX_DISTANCE + LINKED_ENTITY_INVALID).
     */
    @Nonnull
    public static VolatileConfig projectile(@Nonnull Ref<EntityStore> shooter, float maxDistance) {
        return VolatileConfig.builder()
                .policy(VolatilePolicy.MAX_DISTANCE)
                .policy(VolatilePolicy.LINKED_ENTITY_INVALID)
                .linkedEntity(shooter)
                .maxDistance(maxDistance)
                .build();
    }

    /**
     * Simple temporary effect config: removed after {@code durationTicks}.
     */
    @Nonnull
    public static VolatileConfig temporaryEffect(int durationTicks) {
        return VolatileConfig.builder()
                .policy(VolatilePolicy.IDLE_TIMEOUT)
                .idleTimeoutTicks(durationTicks)
                .build();
    }

    /**
     * Config that delegates TTL handling exclusively to Hytale's DespawnComponent.
     */
    @Nonnull
    public static VolatileConfig useDespawnTtl() {
        return VolatileConfig.builder()
                .policy(VolatilePolicy.USE_DESPAWN_TTL)
                .build();
    }

    /**
     * Config that uses a custom predicate for removal.
     */
    @Nonnull
    public static VolatileConfig custom(@Nonnull Predicate<VolatileContext> condition) {
        return VolatileConfig.builder()
                .policy(VolatilePolicy.CUSTOM)
                .customCondition(condition)
                .build();
    }
}