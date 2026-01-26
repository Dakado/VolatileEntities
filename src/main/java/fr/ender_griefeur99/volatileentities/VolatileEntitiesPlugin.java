package fr.ender_griefeur99.volatileentities;

import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import fr.ender_griefeur99.volatileentities.api.VolatileEntityService;
import fr.ender_griefeur99.volatileentities.component.VolatileComponent;
import fr.ender_griefeur99.volatileentities.policy.VolatilePolicy;
import fr.ender_griefeur99.volatileentities.system.VolatileChunkUnloadSystem;
import fr.ender_griefeur99.volatileentities.system.VolatileDespawnKeepAliveSystem;
import fr.ender_griefeur99.volatileentities.system.VolatileOwnerDisconnectListener;
import fr.ender_griefeur99.volatileentities.system.VolatileTickSystem;
import lombok.Getter;

import javax.annotation.Nonnull;

/**
 * Main plugin entry point for the VolatileEntities framework.
 *
 * This plugin:
 * <ul>
 *   <li>Registers {@link VolatileComponent} with a codec (so it can be persisted and flagged on load).</li>
 *   <li>Registers all ECS systems needed to process volatile entities:
 *     <ul>
 *       <li>{@link VolatileTickSystem} – applies volatile policies every tick (kill-on-load, distance, timeout, etc.).</li>
 *       <li>{@link VolatileDespawnKeepAliveSystem} – keeps DespawnComponent "alive" for uninstall safety.</li>
 *       <li>{@link VolatileChunkUnloadSystem} – handles CHUNK_UNLOAD policy on chunk unload events.</li>
 *     </ul>
 *   </li>
 *   <li>Registers a plugin-level listener for {@link PlayerDisconnectEvent} to handle OWNER_DISCONNECT policy.</li>
 * </ul>
 */
public class VolatileEntitiesPlugin extends JavaPlugin {

    @Getter
    private static VolatileEntitiesPlugin instance;

    /**
     * Optional high-level service wrapper for other plugins.
     * It’s not used internally yet, but exposed for convenience.
     */
    @Getter
    private VolatileEntityService service;

    /**
     * Sliding TTL in seconds for the uninstall cleanup safety net.
     * <p>
     * As long as this plugin is running, most volatile entities get their DespawnComponent
     * constantly reset to now + DESPAWN_KEEPALIVE_SECONDS by {@link VolatileDespawnKeepAliveSystem}.
     * After uninstall, DespawnSystem (vanilla) will eventually remove them once this TTL is reached.
     *
     * For production you might want a higher value (e.g. 3600f = 1h, 86400f = 24h).
     * For testing, 5 seconds is convenient.
     */
    private static final float DESPAWN_KEEPALIVE_SECONDS = 5f;

    public VolatileEntitiesPlugin(@Nonnull JavaPluginInit init) {
        super(init);
        instance = this;
    }

    @Override
    protected void setup() {
        try {
            getLogger().atInfo().log("[VolatileEntities] Initializing...");

            // 1) Component registration
            registerComponents();

            // 2) ECS systems + plugin listeners
            registerSystemsAndListeners();

            // 3) Optional service API for other plugins
            service = new VolatileEntityService();

            getLogger().atInfo().log(
                    "[VolatileEntities] Initialized. %d policies.",
                    VolatilePolicy.values().length
            );
        } catch (Exception e) {
            getLogger().atSevere().log("[VolatileEntities] Failed to initialize: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    /**
     * Register VolatileComponent in the ECS registry with its codec.
     * <p>
     * We set {@link VolatileComponent#TYPE} here so that static calls to
     * {@link VolatileComponent#getComponentType()} work everywhere else.
     */
    private void registerComponents() {
        VolatileComponent.TYPE = getEntityStoreRegistry()
                .registerComponent(
                        VolatileComponent.class,
                        "VolatileEntities:Volatile",
                        VolatileComponent.CODEC
                );

        getLogger().atFine().log("[VolatileEntities] Registered VolatileComponent with CODEC");
    }

    /**
     * Register all systems and listeners that drive the volatile behavior.
     */
    private void registerSystemsAndListeners() {
        // 1) Main volatile system: kill-on-load, distance, idle timeout, custom conditions…
        getEntityStoreRegistry().registerSystem(new VolatileTickSystem(this));

        // 2) DespawnComponent keep-alive (handles uninstall cleanup for most volatile entities)
        getEntityStoreRegistry().registerSystem(new VolatileDespawnKeepAliveSystem(DESPAWN_KEEPALIVE_SECONDS));

        // 3) ECS system listening for ChunkUnloadEvent, handling CHUNK_UNLOAD policy
        getEntityStoreRegistry().registerSystem(new VolatileChunkUnloadSystem());

        // 4) Plugin event listener for PlayerDisconnectEvent, handling OWNER_DISCONNECT policy
        getEventRegistry().registerGlobal(
                PlayerDisconnectEvent.class,
                VolatileOwnerDisconnectListener::onPlayerDisconnect
        );

        getLogger().atFine().log("[VolatileEntities] Registered systems and listeners");
    }

    @Override
    protected void shutdown() {
        getLogger().atInfo().log("[VolatileEntities] Shutting down...");
        service = null;
        instance = null;
        getLogger().atInfo().log("[VolatileEntities] Shutdown complete.");
    }

    /**
     * Simple helper to check if the plugin has been initialized.
     */
    public static boolean isInitialized() {
        return instance != null;
    }
}