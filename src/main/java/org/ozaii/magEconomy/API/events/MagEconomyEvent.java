package org.ozaii.magEconomy.API.events;


import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;

import java.util.UUID;

public abstract class MagEconomyEvent extends Event {

    private static final HandlerList handlers = new HandlerList();
    protected final UUID playerUUID;
    protected final Plugin plugin;

    public MagEconomyEvent(UUID playerUUID, Plugin plugin) {
        this.playerUUID = playerUUID;
        this.plugin = plugin;
    }

    public UUID getPlayerUUID() {
        return playerUUID;
    }

    public Plugin getPlugin() {
        return plugin;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}

