package fr.ender_griefeur99.volatileentities.config;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.ender_griefeur99.volatileentities.component.VolatileComponent;
import lombok.Builder;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Evaluation context passed to CUSTOM policies in {@link VolatileConfig}.
 *
 * It exposes:
 * <ul>
 *     <li>The current Store and ArchetypeChunk</li>
 *     <li>The current entity's index and reference</li>
 *     <li>The entity's VolatileComponent</li>
 *     <li>The current delta time</li>
 * </ul>
 */
@Getter
@Builder
public final class VolatileContext {

    @Nonnull
    private final Store<EntityStore> store;

    @Nonnull
    private final ArchetypeChunk<EntityStore> chunk;

    private final int index;

    @Nonnull
    private final Ref<EntityStore> entityRef;

    @Nonnull
    private final VolatileComponent volatileComponent;

    private final float deltaTime;

    /**
     * Read a component from the current entity.
     */
    @Nullable
    public <T extends Component<EntityStore>> T getComponent(
            @Nonnull ComponentType<EntityStore, T> componentType
    ) {
        return chunk.getComponent(index, componentType);
    }

    /**
     * Check if the current entity has a given component.
     */
    public boolean hasComponent(@Nonnull ComponentType<EntityStore, ?> componentType) {
        return chunk.getArchetype().contains(componentType);
    }

    /**
     * Read a component from another entity.
     */
    @Nullable
    public <T extends Component<EntityStore>> T getComponentFrom(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull ComponentType<EntityStore, T> componentType
    ) {
        if (!ref.isValid()) {
            return null;
        }
        return store.getComponent(ref, componentType);
    }

    /**
     * Check if a reference is still valid in the ECS store.
     */
    public boolean isValid(@Nonnull Ref<EntityStore> ref) {
        return ref.isValid();
    }
}