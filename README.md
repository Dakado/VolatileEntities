# VolatileEntities

VolatileEntities is an ECS-based framework for Hytale that lets you create and manage **non-persistent** or **time-controlled** entities, without leaving “garbage” in the world.

It is designed to:

* give you fine-grained control over entity lifetime (distance, timeout, owner disconnect, etc.),
* automatically clean up entities on server restart,
* and even ensure cleanup after your plugin is uninstalled.

---

## Key Features

### Automatic non-persistence on restart

Any entity with a `VolatileComponent` is automatically removed on the very **first tick after a server restart**, thanks to:

* the `expiredOnLoad` flag handled by the component’s codec (`VolatileComponent.CODEC`),
* and the `VolatileTickSystem` that kills “loaded-from-disk” volatile entities.

This guarantees that volatile entities **never survive a reboot**, even if they were saved to disk.

---

### Flexible runtime TTL (while the plugin is running)

Using `VolatileConfig` and `VolatilePolicy`, you can precisely control how long an entity should live at runtime:

* `IDLE_TIMEOUT` – despawn after X ticks or seconds of activity,
* `MAX_DISTANCE` – despawn when too far from a linked entity,
* `LINKED_ENTITY_INVALID` – despawn if the linked entity becomes invalid,
* `CUSTOM` – fully custom despawn logic via a predicate on `VolatileContext`.

All of these are evaluated by `VolatileTickSystem` while your plugin is loaded.

---

### Policy combination

Policies are **not exclusive**.

A single volatile entity can combine multiple policies at the same time.
The entity will be removed as soon as **any policy condition is met**.

For example, an entity can:
- despawn after 10 seconds of inactivity (`IDLE_TIMEOUT`),
- OR despawn if it moves too far from a target (`MAX_DISTANCE`),
- OR be removed when its owner disconnects (`OWNER_DISCONNECT`).

This allows you to express complex lifecycles without custom code.
```java
VolatileEntities.builder()
    .withinDistance(targetRef, 30f)
    .idleTimeoutSeconds(10f)
    .ownedBy(playerUuid)
    .spawn(commandBuffer);
```

---

# Combining Volatile Policies (Example)

This example demonstrates how **multiple `VolatilePolicy` rules can be combined**
inside a single `VolatileConfig`.

Policies are **not exclusive**: an entity can have several lifecycle rules applied
at the same time.

The entity will be removed as soon as **any policy condition is met**.

---

## Example: Chunk-bound entity linked to another entity

```java
VolatileConfig.VolatileConfigBuilder configBuilder = VolatileConfig.builder()
        // Automatically remove the entity when its chunk unloads
        .policy(VolatilePolicy.CHUNK_UNLOAD)

        // Remove immediately as soon as a policy condition becomes invalid
        .removeOnInvalid(true);

// Optionally link this volatile entity to another one (e.g. a mob or NPC)
if (linkedEntity != null) {
        configBuilder
        .policy(VolatilePolicy.LINKED_ENTITY_INVALID)
        .linkedEntity(linkedEntity);
}

VolatileConfig config = configBuilder.build();
return new VolatileComponent(config);
```

---

### Absolute TTL using Hytale’s `DespawnComponent` (chunk unload/reload safe)

VolatileEntities integrates cleanly with vanilla Hytale systems:

* `DespawnComponent` + `DespawnSystem` handle absolute, real-time TTL,
* the `USE_DESPAWN_TTL` policy tells VolatileEntities to **delegate TTL handling entirely** to `DespawnComponent`.

This means:

* TTL is based on real time (`TimeResource`), not ticks,
* chunk unloads / reloads are safe,
* if the despawn time is already in the past when the chunk reloads, the entity is removed immediately.

You can therefore choose between:

* **runtime, online-only TTL** (`IDLE_TIMEOUT`), or
* **absolute TTL** that continues counting even while unloaded (`USE_DESPAWN_TTL` + `DespawnComponent`).

---

### Cleanup after plugin uninstall

To solve the classic “entities left behind after removing a mod” problem, VolatileEntities uses a keep-alive pattern:

* `VolatileDespawnKeepAliveSystem` continuously refreshes a vanilla `DespawnComponent` on volatile entities
  (except those using `USE_DESPAWN_TTL`),
* while your plugin is installed, the despawn deadline is constantly pushed forward,
* once your plugin is removed, the last despawn deadline is eventually reached.

At that point, Hytale’s core `DespawnSystem` automatically cleans up **all remaining volatile entities**, even though your plugin is no longer present.

No manual world scans. No orphaned entities.

---

### Advanced policies

In addition to core TTL policies, the framework supports:

* `CHUNK_UNLOAD` – automatically remove volatile entities when their chunk unloads
  (handled by `VolatileChunkUnloadSystem`, an `EntityEventSystem<ChunkUnloadEvent>`).

* `OWNER_DISCONNECT` – mark or remove volatile entities owned by a player when they disconnect
  (handled by `VolatileOwnerDisconnectListener`, listening to `PlayerDisconnectEvent`).

Combined together, these policies form a robust lifecycle toolkit for temporary entities.

---

## Typical Use Cases

Use VolatileEntities to mark:

* holograms,
* projectiles,
* visual effects,
* temporary NPCs,
* helper or marker entities

as volatile, so that they:

* despawn on restart,
* optionally follow runtime rules (timeout, distance, ownership, custom logic),
* and are still guaranteed to be cleaned up after plugin uninstall.

---

## Usage Examples

### 1) Marking an existing entity as volatile (restart-only)

If you already spawn an entity manually (for example a hologram), you can simply attach a `VolatileComponent` to make it non-persistent.

```java
holder.addComponent(
    VolatileComponent.getComponentType(),
    VolatileComponent.restartOnly()
);
```

**Result:**

* the entity lives as long as the server runs,
* it is removed on the first tick after a restart,
* uninstall safety cleanup still applies.

---

### 2) Absolute TTL (10 seconds real-time)

To despawn an entity after a fixed amount of real time, even across chunk unloads, combine `DespawnComponent` with `USE_DESPAWN_TTL`:

```java
holder.addComponent(
    DespawnComponent.getComponentType(),
    DespawnComponent.despawnInSeconds(time, 10f)
);

VolatileConfig cfg = VolatileConfig.builder()
    .policy(VolatilePolicy.USE_DESPAWN_TTL)
    .build();

holder.addComponent(
    VolatileComponent.getComponentType(),
    new VolatileComponent(cfg)
);
```

Here:

* Hytale handles the TTL itself,
* VolatileEntities does not refresh despawn time,
* restart and uninstall cleanup are still guaranteed.

---

### 3) Mark an already existing entity as volatile

You can mark an entity as volatile after it already exists:

```java
store.putComponent(
    ref,
    VolatileComponent.getComponentType(),
    VolatileComponent.restartOnly()
);
```

No respawn or manual despawn logic required.

---

## API Reference (Quick Overview)

### `VolatileComponent` – factory methods

* `restartOnly()` – runtime-only, removed on restart
* `withTimeout(int ticks)` / `withTimeoutSeconds(float seconds, int tickRate)`
* `withinDistance(Ref<EntityStore> target, float distance)`
* `linkedTo(Ref<EntityStore> target)`
* `ownedBy(UUID playerUuid)`
* `useDespawnTtl()`

---

### `VolatileConfig`

Presets and builders for advanced control:

* `chunkBound()`
* `linkedTo(ref)`
* `withinDistance(ref, distance)`
* `ownedBy(playerUuid)`
* `withTimeout(ticks)`
* `useDespawnTtl()`
* `custom(ctx -> { ... })`

---

## `VolatileEntities.builder()`

For convenience, the builder API provides a fluent way to spawn volatile entities:

```java
VolatileEntities.builder()
    .at(new Vector3d(x, y, z))
    .with(MyComponent.TYPE, new MyComponent(...))
    .withinDistance(targetRef, 50f)
    .idleTimeoutSeconds(10f)
    .spawn(commandBuffer);
```

---

## Installation

1. Drop the **VolatileEntities** JAR into your server’s plugin folder.

2. Declare a dependency in your plugin manifest:

```json
{
  "Group": "Ender_Griefeur99",
  "Name": "MyPlugin",
  "Version": "1.0.0",
  "Main": "fr.ender_griefeur99.myplugin.MyPlugin",
  "ServerVersion": "*",
  "IncludesAssetPack": false,
  "Dependencies": {
    "Ender_Griefeur99:VolatileEntities": "*"
  }
}
```
