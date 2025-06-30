package org.ozaii.magEconomy.API.events;

import org.bukkit.plugin.Plugin;

import java.util.UUID;

// Para transfer eventi
public class MoneyTransferEvent extends MagEconomyEvent {
    private final UUID toPlayerUUID;
    private final double amount;

    public MoneyTransferEvent(UUID fromPlayerUUID, UUID toPlayerUUID, Plugin plugin, double amount) {
        super(fromPlayerUUID, plugin);
        this.toPlayerUUID = toPlayerUUID;
        this.amount = amount;
    }

    public UUID getToPlayerUUID() { return toPlayerUUID; }
    public double getAmount() { return amount; }
}
