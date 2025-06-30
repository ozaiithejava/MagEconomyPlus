package org.ozaii.magEconomy.API.events;

import org.bukkit.plugin.Plugin;

import java.util.UUID;


public class MoneyDepositEvent extends MagEconomyEvent {
    private final double amount;
    private final double newBalance;

    public MoneyDepositEvent(UUID playerUUID, Plugin plugin, double amount, double newBalance) {
        super(playerUUID, plugin);
        this.amount = amount;
        this.newBalance = newBalance;
    }

    public double getAmount() { return amount; }
    public double getNewBalance() { return newBalance; }
}
