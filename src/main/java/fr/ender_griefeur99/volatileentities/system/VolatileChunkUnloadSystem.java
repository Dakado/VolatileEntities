package fr.ender_griefeur99.volatileentities.system;

import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.universe.world.events.ecs.ChunkUnloadEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.ender_griefeur99.volatileentities.component.VolatileComponent;
import fr.ender_griefeur99.volatileentities.policy.VolatilePolicy;
import fr.ender_griefeur99.volatileentities.policy.VolatileRemovalReason;

import javax.annotation.Nonnull;

/**
 * ECS event system for the CHUNK_UNLOAD policy.
 *
 * When a {@link ChunkUnloadEvent} occurs, this system is invoked for all entities
 * in the affected chunk that match the query; if they have a VolatileComponent
 * with {@link VolatilePolicy#CHUNK_UNLOAD}, they are removed.
 */
public final class VolatileChunkUnloadSystem extends EntityEventSystem<EntityStore, ChunkUnloadEvent> {

    private static final Archetype<EntityStore> QUERY = Archetype.of(
            VolatileComponent.getComponentType()
    );

    public VolatileChunkUnloadSystem() {
        super(ChunkUnloadEvent.class);
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return QUERY;
    }

    @Override
    public void handle(
            int index,
            @Nonnull ArchetypeChunk<EntityStore> chunk,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer,
            @Nonnull ChunkUnloadEvent event
    ) {
        VolatileComponent vc = chunk.getComponent(index, VolatileComponent.getComponentType());
        if (vc == null) return;

        // Only entities explicitly marked for CHUNK_UNLOAD are handled here.
        if (!vc.hasPolicy(VolatilePolicy.CHUNK_UNLOAD)) return;

        Ref<EntityStore> ref = chunk.getReferenceTo(index);
        if (ref == null || !ref.isValid()) return;

        // We assume this event is dispatched only for entities in the chunk that unloads,
        // so we can safely remove the entity here.
        commandBuffer.removeEntity(ref, VolatileRemovalReason.CHUNK_UNLOADED.toHytale());
    }
}