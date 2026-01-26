package fr.ender_griefeur99.volatileentities.system;

import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.modules.entity.DespawnComponent;
import com.hypixel.hytale.server.core.modules.time.TimeResource;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.ender_griefeur99.volatileentities.component.VolatileComponent;
import fr.ender_griefeur99.volatileentities.policy.VolatilePolicy;

import javax.annotation.Nonnull;

/**
 * Ticking system that keeps DespawnComponent "alive" for most volatile entities.
 *
 * The goal is:
 * - while this plugin is installed, most volatile entities never despawn via DespawnSystem
 *   (we constantly reset their despawn time to now + keepAliveSeconds),
 * - once the plugin is removed, this system no longer runs and the last Despawn time
 *   will eventually be reached, letting the vanilla DespawnSystem clean them up.
 *
 * Entities marked with {@link VolatilePolicy#USE_DESPAWN_TTL} are ignored here:
 * their TTL is handled exclusively by vanilla {@link DespawnComponent}.
 */
public final class VolatileDespawnKeepAliveSystem extends EntityTickingSystem<EntityStore> {

    /**
     * We only care about entities that have a VolatileComponent.
     */
    private static final Archetype<EntityStore> QUERY = Archetype.of(
            VolatileComponent.getComponentType()
    );

    /**
     * Sliding TTL in seconds used for the "uninstall cleanup" safety net.
     */
    private final float keepAliveSeconds;

    public VolatileDespawnKeepAliveSystem(float keepAliveSeconds) {
        this.keepAliveSeconds = keepAliveSeconds;
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return QUERY;
    }

    @Override
    public boolean isParallel(int archetypeChunkSize, int taskCount) {
        return EntityTickingSystem.maybeUseParallel(archetypeChunkSize, taskCount);
    }

    @Override
    public void tick(
            float dt,
            int index,
            @Nonnull ArchetypeChunk<EntityStore> chunk,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        Ref<EntityStore> ref = chunk.getReferenceTo(index);
        if (ref == null || !ref.isValid()) return;

        VolatileComponent vc = chunk.getComponent(index, VolatileComponent.getComponentType());
        if (vc == null) return;

        // If this entity explicitly uses DespawnComponent as TTL, we do not touch it here.
        if (vc.hasPolicy(VolatilePolicy.USE_DESPAWN_TTL)) {
            return;
        }

        TimeResource time = store.getResource(TimeResource.getResourceType());
        if (time == null) return;

        DespawnComponent despawn = chunk.getComponent(index, DespawnComponent.getComponentType());

        // Either add a DespawnComponent or update its despawn time to now + keepAliveSeconds
        DespawnComponent.trySetDespawn(
                commandBuffer,
                time,
                ref,
                despawn,
                keepAliveSeconds
        );
    }
}