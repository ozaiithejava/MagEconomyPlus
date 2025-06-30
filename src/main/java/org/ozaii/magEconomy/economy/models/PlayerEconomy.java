package org.ozaii.magEconomy.economy.models;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.Objects;
import java.util.UUID;

@DatabaseTable(tableName = "player_economy")
public class PlayerEconomy {

    @DatabaseField(id = true, columnName = "player_uuid")
    private String playerUUID;

    @DatabaseField(columnName = "player_name")
    private String playerName;

    @DatabaseField(columnName = "balance")
    private double balance;

    @DatabaseField(columnName = "created_at")
    private long createdAt;

    @DatabaseField(columnName = "updated_at")
    private long updatedAt;

    // Boş constructor (ORMLite için gerekli)
    public PlayerEconomy() {
    }

    public PlayerEconomy(UUID playerUUID, String playerName, double balance) {
        this.playerUUID = playerUUID.toString();
        this.playerName = playerName;
        this.balance = balance;
        long now = System.currentTimeMillis();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public PlayerEconomy(UUID playerUUID, String playerName) {
        this(playerUUID, playerName, 0.0);
    }

    public String getPlayerUUID() {
        return playerUUID;
    }

    public UUID getPlayerUUIDAsUUID() {
        return UUID.fromString(playerUUID);
    }

    public void setPlayerUUID(UUID uuid) {
        this.playerUUID = uuid.toString();
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
        this.updatedAt = System.currentTimeMillis();
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
        this.updatedAt = System.currentTimeMillis();
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void addBalance(double amount) {
        this.balance += amount;
        this.updatedAt = System.currentTimeMillis();
    }

    public boolean subtractBalance(double amount) {
        if (this.balance >= amount) {
            this.balance -= amount;
            this.updatedAt = System.currentTimeMillis();
            return true;
        }
        return false;
    }

    public boolean hasBalance(double amount) {
        return this.balance >= amount;
    }

    @Override
    public String toString() {
        return "PlayerEconomy{" +
                "playerUUID='" + playerUUID + '\'' +
                ", playerName='" + playerName + '\'' +
                ", balance=" + balance +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlayerEconomy)) return false;
        PlayerEconomy that = (PlayerEconomy) o;
        return Objects.equals(playerUUID, that.playerUUID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerUUID);
    }
}
