package fr.ender_griefeur99.volatileentities.api;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.ender_griefeur99.volatileentities.component.VolatileComponent;
import lombok.RequiredArgsConstructor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Service API pour interagir avec les entités volatiles.
 */
@RequiredArgsConstructor
public class VolatileEntityService {

    private static final int DEFAULT_TICK_RATE = 20;

    /**
     * Vérifie si une entité est volatile.
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
     * Récupère le VolatileComponent d'une entité.
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
     * Invalide une entité volatile (sera supprimée au prochain tick).
     */
    public void invalidate(
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref
    ) {
        invalidate(store, ref, null);
    }

    /**
     * Invalide une entité volatile avec une raison.
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
     * Reset le timer d'inactivité d'une entité volatile.
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
     * Vérifie si une entité volatile est marquée pour suppression.
     */
    public boolean isMarkedForRemoval(
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref
    ) {
        VolatileComponent vc = getComponent(store, ref);
        return vc != null && vc.isMarkedForRemoval();
    }

    /**
     * Retourne les ticks restants avant timeout.
     */
    public int getIdleTicksRemaining(
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref
    ) {
        VolatileComponent vc = getComponent(store, ref);
        return vc != null ? vc.getIdleTicksRemaining() : -1;
    }

    /**
     * Retourne le temps restant en secondes.
     */
    public float getTimeRemainingSeconds(
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref
    ) {
        return getTimeRemainingSeconds(store, ref, DEFAULT_TICK_RATE);
    }

    /**
     * Retourne le temps restant en secondes avec un tick rate personnalisé.
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