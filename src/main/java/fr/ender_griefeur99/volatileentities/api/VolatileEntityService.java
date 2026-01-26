package fr.ender_griefeur99.volatileentities.api;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.ender_griefeur99.volatileentities.component.VolatileComponent;
import lombok.RequiredArgsConstructor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Service API for interacting with volatile entities.
 *
 * <p>Volatile entities are temporary entities that can be automatically
 * removed after a period of inactivity or when explicitly invalidated.</p>
 *
 * <p>This service provides methods to:</p>
 * <ul>
 *   <li>Check if an entity is volatile</li>
 *   <li>Retrieve volatile component data</li>
 *   <li>Invalidate entities for removal</li>
 *   <li>Manage idle timers</li>
 * </ul>
 */
@RequiredArgsConstructor
public class VolatileEntityService {

    private static final int DEFAULT_TICK_RATE = 20;

    /**
     * Checks whether an entity is volatile.
     *
     * @param store the entity store containing the entity
     * @param ref   the reference to the entity
     * @return {@code true} if the entity has a volatile component, {@code false} otherwise
     */
    public boolean isVolatile(
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref
    ) {
        if (!ref.isValid()) {
            return false;
        }
        return store.getComponent(ref, VolatileComponent.getComponentType()) != null;
    }

    /**
     * Retrieves the {@link VolatileComponent} of an entity.
     *
     * @param store the entity store containing the entity
     * @param ref   the reference to the entity
     * @return the volatile component, or {@code null} if not found or ref is invalid
     */
    @Nullable
    public VolatileComponent getComponent(
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref
    ) {
        if (!ref.isValid()) {
            return null;
        }
        return store.getComponent(ref, VolatileComponent.getComponentType());
    }

    /**
     * Invalidates a volatile entity, marking it for removal on the next tick.
     *
     * @param store the entity store containing the entity
     * @param ref   the reference to the entity
     */
    public void invalidate(
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref
    ) {
        invalidate(store, ref, null);
    }

    /**
     * Invalidates a volatile entity with a specified reason.
     *
     * @param store  the entity store containing the entity
     * @param ref    the reference to the entity
     * @param reason the reason for invalidation (for logging/debugging), may be {@code null}
     */
    public void invalidate(
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nullable String reason
    ) {
        VolatileComponent vc = getComponent(store, ref);
        if (vc != null) {
            vc.markForRemoval(reason);
        }
    }

    /**
     * Resets the idle timer of a volatile entity.
     *
     * <p>This prevents the entity from being removed due to inactivity timeout.</p>
     *
     * @param store the entity store containing the entity
     * @param ref   the reference to the entity
     */
    public void resetIdleTimer(
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref
    ) {
        VolatileComponent vc = getComponent(store, ref);
        if (vc != null) {
            vc.resetIdleTicks();
        }
    }

    /**
     * Checks whether a volatile entity is marked for removal.
     *
     * @param store the entity store containing the entity
     * @param ref   the reference to the entity
     * @return {@code true} if the entity is marked for removal, {@code false} otherwise
     */
    public boolean isMarkedForRemoval(
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref
    ) {
        VolatileComponent vc = getComponent(store, ref);
        return vc != null && vc.isMarkedForRemoval();
    }

    /**
     * Returns the remaining ticks before the entity times out.
     *
     * @param store the entity store containing the entity
     * @param ref   the reference to the entity
     * @return the number of remaining ticks, or {@code -1} if not a volatile entity
     */
    public int getIdleTicksRemaining(
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref
    ) {
        VolatileComponent vc = getComponent(store, ref);
        return vc != null ? vc.getIdleTicksRemaining() : -1;
    }

    /**
     * Returns the remaining time in seconds before the entity times out.
     *
     * <p>Uses the default tick rate of 20 ticks per second.</p>
     *
     * @param store the entity store containing the entity
     * @param ref   the reference to the entity
     * @return the remaining time in seconds, or {@code 0} if expired or not volatile
     */
    public float getTimeRemainingSeconds(
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref
    ) {
        return getTimeRemainingSeconds(store, ref, DEFAULT_TICK_RATE);
    }

    /**
     * Returns the remaining time in seconds before the entity times out.
     *
     * @param store    the entity store containing the entity
     * @param ref      the reference to the entity
     * @param tickRate the server tick rate (ticks per second)
     * @return the remaining time in seconds, or {@code 0} if expired or not volatile
     */
    public float getTimeRemainingSeconds(
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            int tickRate
    ) {
        int ticks = getIdleTicksRemaining(store, ref);
        return ticks > 0 ? (float) ticks / tickRate : 0f;
    }
}