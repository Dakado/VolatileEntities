package fr.ender_griefeur99.volatileentities.policy;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * High-level policies controlling how and when a volatile entity should be removed.
 *
 * Each policy is evaluated by one or more systems in this library (or by vanilla Hytale).
 */
@Getter
@RequiredArgsConstructor
public enum VolatilePolicy {

    /**
     * Remove the entity when its chunk unloads.
     * <p>
     * Handled by {@link fr.ender_griefeur99.volatileentities.system.VolatileChunkUnloadSystem}.
     */
    CHUNK_UNLOAD(false, false, false),

    /**
     * Remove the entity when its linked entity becomes invalid (Ref no longer valid).
     * <p>
     * Handled by {@link fr.ender_griefeur99.volatileentities.system.VolatileTickSystem}.
     */
    LINKED_ENTITY_INVALID(true, false, false),

    /**
     * Remove the entity when it goes beyond a maximum distance from its linked entity.
     * <p>
     * Handled by {@link fr.ender_griefeur99.volatileentities.system.VolatileTickSystem}.
     */
    MAX_DISTANCE(true, true, false),

    /**
     * Remove (or mark for removal) the entity when its owner player disconnects.
     * <p>
     * Handled by {@link fr.ender_griefeur99.volatileentities.system.VolatileOwnerDisconnectListener}.
     */
    OWNER_DISCONNECT(false, false, false),

    /**
     * Remove the entity when it leaves the world bounds.
     * <p>
     * Not implemented in the core systems yet – reserved for custom world-bounds logic.
     */
    OUT_OF_BOUNDS(false, false, false),

    /**
     * Remove the entity after a certain number of ticks (idle timeout).
     * <p>
     * Handled by {@link fr.ender_griefeur99.volatileentities.system.VolatileTickSystem}.
     */
    IDLE_TIMEOUT(false, false, true),

    /**
     * The entity's TTL is handled exclusively by Hytale's {@link com.hypixel.hytale.server.core.modules.entity.DespawnComponent}
     * and {@link com.hypixel.hytale.server.core.modules.entity.DespawnSystem}.
     * <p>
     * When this policy is active:
     * <ul>
     *   <li>{@link fr.ender_griefeur99.volatileentities.system.VolatileDespawnKeepAliveSystem}
     *       will NOT touch the entity's DespawnComponent (no keep-alive).</li>
     *   <li>{@link fr.ender_griefeur99.volatileentities.system.VolatileTickSystem}
     *       does not perform any TTL-based removal either.</li>
     * </ul>
     */
    USE_DESPAWN_TTL(false, false, false),

    /**
     * Custom removal logic implemented as a predicate on {@link fr.ender_griefeur99.volatileentities.config.VolatileContext}.
     * <p>
     * Handled by {@link fr.ender_griefeur99.volatileentities.system.VolatileTickSystem}.
     */
    CUSTOM(false, false, false);

    /**
     * Whether this policy requires a linked entity to be present in the config.
     */
    private final boolean requiresLinkedEntity;

    /**
     * Whether this policy requires a max distance value in the config.
     */
    private final boolean requiresDistance;

    /**
     * Whether this policy requires an idle timeout value in the config.
     */
    private final boolean requiresTimeout;
}