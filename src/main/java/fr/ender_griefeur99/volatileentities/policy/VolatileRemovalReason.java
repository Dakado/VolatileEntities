package fr.ender_griefeur99.volatileentities.policy;

import com.hypixel.hytale.component.RemoveReason;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import javax.annotation.Nonnull;

/**
 * High-level reasons used by the volatile systems when removing entities,
 * mapped down to vanilla Hytale {@link RemoveReason} values.
 */
@Getter
@RequiredArgsConstructor
public enum VolatileRemovalReason {

    CHUNK_UNLOADED("Chunk was unloaded", RemoveReason.UNLOAD),
    LINKED_ENTITY_INVALID("Linked entity became invalid", RemoveReason.REMOVE),
    MAX_DISTANCE_EXCEEDED("Maximum distance from target exceeded", RemoveReason.REMOVE),
    OWNER_DISCONNECTED("Owner player disconnected", RemoveReason.REMOVE),
    OUT_OF_BOUNDS("Entity went out of world bounds", RemoveReason.REMOVE),
    IDLE_TIMEOUT("Idle timeout expired", RemoveReason.REMOVE),
    SERVER_SHUTDOWN("Server is shutting down (or entity reloaded with volatile component)", RemoveReason.REMOVE),
    CUSTOM_CONDITION("Custom invalidation condition met", RemoveReason.REMOVE),
    MANUAL_INVALIDATION("Manually invalidated by code", RemoveReason.REMOVE);

    /**
     * Human-readable description (good for logging).
     */
    private final String description;

    /**
     * Underlying Hytale {@link RemoveReason} used with {@link com.hypixel.hytale.component.CommandBuffer#removeEntity}.
     */
    private final RemoveReason hytaleReason;

    /**
     * Convenience helper to map a {@link VolatilePolicy} to a default removal reason.
     */
    @Nonnull
    public static VolatileRemovalReason fromPolicy(@Nonnull VolatilePolicy policy) {
        return switch (policy) {
            case CHUNK_UNLOAD        -> CHUNK_UNLOADED;
            case LINKED_ENTITY_INVALID -> LINKED_ENTITY_INVALID;
            case MAX_DISTANCE        -> MAX_DISTANCE_EXCEEDED;
            case OWNER_DISCONNECT    -> OWNER_DISCONNECTED;
            case OUT_OF_BOUNDS       -> OUT_OF_BOUNDS;
            case IDLE_TIMEOUT        -> IDLE_TIMEOUT;
            case USE_DESPAWN_TTL     -> CUSTOM_CONDITION;
            case CUSTOM              -> CUSTOM_CONDITION;
        };
    }

    /**
     * Returns the vanilla Hytale {@link RemoveReason} used by CommandBuffer.
     */
    @Nonnull
    public RemoveReason toHytale() {
        return hytaleReason;
    }

    @Override
    public String toString() {
        return name() + ": " + description;
    }
}