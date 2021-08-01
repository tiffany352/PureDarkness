package com.tiffnix.puredarkness;

import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class PureDarkness extends JavaPlugin implements Listener {
    public static class SpawnRule {
        public final EntityType entityType;
        public final CreatureSpawnEvent.SpawnReason spawnReason;
        public final int lightLevel;

        public SpawnRule(EntityType entityType, CreatureSpawnEvent.SpawnReason spawnReason, int lightLevel) {
            this.entityType = entityType;
            this.spawnReason = spawnReason;
            this.lightLevel = lightLevel;
        }
    }

    private HashMap<CreatureSpawnEvent.SpawnReason, HashMap<EntityType, Integer>> rulesLookup;
    private boolean enableLogging = false;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        loadConfigInternal();

        getServer().getPluginManager().registerEvents(this, this);
    }

    private void loadConfigInternal() {
        rulesLookup = new HashMap<>();
        enableLogging = getConfig().getBoolean("enable-logging");

        final List<Map<?, ?>> rules = getConfig().getMapList("rules");
        for (Map<?, ?> object : rules) {
            try {
                final EntityType entityType = EntityType.valueOf((String) object.get("mob"));

                final String spawnReasonStr = (String) object.get("reason");
                final CreatureSpawnEvent.SpawnReason spawnReason = spawnReasonStr != null ? CreatureSpawnEvent.SpawnReason.valueOf(spawnReasonStr) : CreatureSpawnEvent.SpawnReason.NATURAL;

                final Object lightLevelObj = object.get("light-level");
                final int lightLevel = lightLevelObj != null ? (int) lightLevelObj : 0;

                final HashMap<EntityType, Integer> lightLevelMap = rulesLookup.computeIfAbsent(spawnReason, k -> new HashMap<>());
                lightLevelMap.put(entityType, lightLevel);
            } catch (IllegalArgumentException err) {
                getLogger().severe("Invalid rule in config: " + err);
            }
        }

    }

    @Override
    public void onDisable() {
    }

    @EventHandler
    public void onCreatureSpawnEvent(CreatureSpawnEvent event) {
        final HashMap<EntityType, Integer> lightLevelMap = rulesLookup.get(event.getSpawnReason());
        if (lightLevelMap == null) {
            if (enableLogging)
                getLogger().info("no entry for spawn reason " + event.getSpawnReason().name());
            return;
        }

        final Integer requiredLightLevel = lightLevelMap.get(event.getEntityType());
        if (requiredLightLevel == null) {
            if (enableLogging)
                getLogger().info("no entry for mob " + event.getEntityType().name() + " with spawn reason " + event.getSpawnReason().name());
            return;
        }

        final int lightLevel = event.getLocation().getBlock().getLightFromBlocks();
        if (lightLevel > requiredLightLevel) {
            if (enableLogging)
                getLogger().info("stopping " + event.getEntityType() + " from spawning: " + lightLevel + " > " + requiredLightLevel);
            event.setCancelled(true);
        } else {
            if (enableLogging)
                getLogger().info("allowed " + event.getEntityType() + " to spawn");
        }
    }
}
