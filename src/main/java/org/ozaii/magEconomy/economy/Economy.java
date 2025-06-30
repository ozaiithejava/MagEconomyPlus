package org.ozaii.magEconomy.economy;

import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.java.JavaPlugin;
import org.ozaii.magEconomy.economy.services.PlayerEconomyService;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

public class Economy implements net.milkbowl.vault.economy.Economy {

    private static Economy instance;
    private PlayerEconomyService economyService;
    private JavaPlugin plugin;
    private boolean enabled = false;

    /**
     * Economy singleton instance'ını döndürür
     * @return Economy instance
     */
    public static Economy getInstance() {
        if (instance == null) {
            instance = new Economy();
        }
        return instance;
    }

    /**
     * Economy'yi başlatır
     * @param plugin JavaPlugin instance
     */
    public void initialize(JavaPlugin plugin) {
        this.plugin = plugin;
        this.economyService = PlayerEconomyService.getInstance();
        this.enabled = true;
        plugin.getLogger().info("Economy sistemi başlatıldı!");
    }

    /**
     * CompletableFuture'ı senkron olarak bekler
     * @param future CompletableFuture
     * @param defaultValue Varsayılan değer
     * @param <T> Tip
     * @return Sonuç
     */
    private <T> T waitForResult(CompletableFuture<T> future, T defaultValue) {
        try {
            return future.get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            if (plugin != null) {
                plugin.getLogger().log(Level.WARNING, "Economy işlemi zaman aşımına uğradı", e);
            }
            return defaultValue;
        }
    }

    /**
     * EconomyResponse oluşturur
     * @param amount Miktar
     * @param balance Bakiye
     * @param type Response tipi
     * @param errorMessage Hata mesajı
     * @return EconomyResponse
     */
    private EconomyResponse createResponse(double amount, double balance, EconomyResponse.ResponseType type, String errorMessage) {
        return new EconomyResponse(amount, balance, type, errorMessage);
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public String getName() {
        return "MagEconomy";
    }

    @Override
    public boolean hasBankSupport() {
        return false; // Şimdilik bank desteği yok
    }

    @Override
    public int fractionalDigits() {
        return economyService != null ? economyService.getFractionalDigits() : 2;
    }

    @Override
    public String format(double amount) {
        return economyService != null ? economyService.format(amount) : String.format("%.2f", amount);
    }

    @Override
    public String currencyNamePlural() {
        return economyService != null ? economyService.getCurrencyNamePlural() : "Coins";
    }

    @Override
    public String currencyNameSingular() {
        return economyService != null ? economyService.getCurrencyName() : "Coin";
    }

    @Override
    public boolean hasAccount(String playerName) {
        // PlayerName ile hesap kontrolü - önce OfflinePlayer bulmalıyız
        // Bu method deprecated olduğu için basit bir implementasyon
        return false;
    }

    @Override
    public boolean hasAccount(OfflinePlayer player) {
        if (economyService == null || player == null) return false;
        return waitForResult(economyService.hasAccount(player), false);
    }

    @Override
    public boolean hasAccount(String playerName, String worldName) {
        // World desteği şimdilik yok
        return hasAccount(playerName);
    }

    @Override
    public boolean hasAccount(OfflinePlayer player, String worldName) {
        // World desteği şimdilik yok
        return hasAccount(player);
    }

    @Override
    public double getBalance(String playerName) {
        // PlayerName ile bakiye alma - deprecated
        return 0;
    }

    @Override
    public double getBalance(OfflinePlayer player) {
        if (economyService == null || player == null) return 0;
        return waitForResult(economyService.getBalance(player), 0.0);
    }

    @Override
    public double getBalance(String playerName, String world) {
        // World desteği şimdilik yok
        return getBalance(playerName);
    }

    @Override
    public double getBalance(OfflinePlayer player, String world) {
        // World desteği şimdilik yok
        return getBalance(player);
    }

    @Override
    public boolean has(String playerName, double amount) {
        // PlayerName ile kontrol - deprecated
        return false;
    }

    @Override
    public boolean has(OfflinePlayer player, double amount) {
        if (economyService == null || player == null) return false;
        return waitForResult(economyService.has(player, amount), false);
    }

    @Override
    public boolean has(String playerName, String worldName, double amount) {
        // World desteği şimdilik yok
        return has(playerName, amount);
    }

    @Override
    public boolean has(OfflinePlayer player, String worldName, double amount) {
        // World desteği şimdilik yok
        return has(player, amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(String playerName, double amount) {
        // PlayerName ile withdraw - deprecated
        return createResponse(amount, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED,
                "PlayerName ile işlemler desteklenmiyor");
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, double amount) {
        if (economyService == null || player == null) {
            return createResponse(amount, 0, EconomyResponse.ResponseType.FAILURE,
                    "Economy service mevcut değil");
        }

        if (amount < 0) {
            return createResponse(amount, getBalance(player), EconomyResponse.ResponseType.FAILURE,
                    "Negatif miktar giremezsiniz");
        }

        boolean success = waitForResult(economyService.withdraw(player, amount), false);
        double newBalance = getBalance(player);

        if (success) {
            return createResponse(amount, newBalance, EconomyResponse.ResponseType.SUCCESS, "");
        } else {
            return createResponse(amount, newBalance, EconomyResponse.ResponseType.FAILURE,
                    "Yetersiz bakiye veya işlem başarısız");
        }
    }

    @Override
    public EconomyResponse withdrawPlayer(String playerName, String worldName, double amount) {
        // World desteği şimdilik yok
        return withdrawPlayer(playerName, amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, String worldName, double amount) {
        // World desteği şimdilik yok
        return withdrawPlayer(player, amount);
    }

    @Override
    public EconomyResponse depositPlayer(String playerName, double amount) {
        // PlayerName ile deposit - deprecated
        return createResponse(amount, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED,
                "PlayerName ile işlemler desteklenmiyor");
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, double amount) {
        if (economyService == null || player == null) {
            return createResponse(amount, 0, EconomyResponse.ResponseType.FAILURE,
                    "Economy service mevcut değil");
        }

        if (amount < 0) {
            return createResponse(amount, getBalance(player), EconomyResponse.ResponseType.FAILURE,
                    "Negatif miktar giremezsiniz");
        }

        boolean success = waitForResult(economyService.deposit(player, amount), false);
        double newBalance = getBalance(player);

        if (success) {
            return createResponse(amount, newBalance, EconomyResponse.ResponseType.SUCCESS, "");
        } else {
            return createResponse(amount, newBalance, EconomyResponse.ResponseType.FAILURE,
                    "Maksimum bakiye aşıldı veya işlem başarısız");
        }
    }

    @Override
    public EconomyResponse depositPlayer(String playerName, String worldName, double amount) {
        // World desteği şimdilik yok
        return depositPlayer(playerName, amount);
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, String worldName, double amount) {
        // World desteği şimdilik yok
        return depositPlayer(player, amount);
    }

    // Bank işlemleri - şimdilik desteklenmiyor

    @Override
    public EconomyResponse createBank(String name, String player) {
        return createResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED,
                "Bank sistemi henüz desteklenmiyor");
    }

    @Override
    public EconomyResponse createBank(String name, OfflinePlayer player) {
        return createResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED,
                "Bank sistemi henüz desteklenmiyor");
    }

    @Override
    public EconomyResponse deleteBank(String name) {
        return createResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED,
                "Bank sistemi henüz desteklenmiyor");
    }

    @Override
    public EconomyResponse bankBalance(String name) {
        return createResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED,
                "Bank sistemi henüz desteklenmiyor");
    }

    @Override
    public EconomyResponse bankHas(String name, double amount) {
        return createResponse(amount, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED,
                "Bank sistemi henüz desteklenmiyor");
    }

    @Override
    public EconomyResponse bankWithdraw(String name, double amount) {
        return createResponse(amount, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED,
                "Bank sistemi henüz desteklenmiyor");
    }

    @Override
    public EconomyResponse bankDeposit(String name, double amount) {
        return createResponse(amount, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED,
                "Bank sistemi henüz desteklenmiyor");
    }

    @Override
    public EconomyResponse isBankOwner(String name, String playerName) {
        return createResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED,
                "Bank sistemi henüz desteklenmiyor");
    }

    @Override
    public EconomyResponse isBankOwner(String name, OfflinePlayer player) {
        return createResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED,
                "Bank sistemi henüz desteklenmiyor");
    }

    @Override
    public EconomyResponse isBankMember(String name, String playerName) {
        return createResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED,
                "Bank sistemi henüz desteklenmiyor");
    }

    @Override
    public EconomyResponse isBankMember(String name, OfflinePlayer player) {
        return createResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED,
                "Bank sistemi henüz desteklenmiyor");
    }

    @Override
    public List<String> getBanks() {
        return List.of(); // Boş liste döndür
    }

    @Override
    public boolean createPlayerAccount(String playerName) {
        // PlayerName ile hesap oluşturma - deprecated
        return false;
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer player) {
        if (economyService == null || player == null) return false;
        return waitForResult(economyService.createAccount(player), false);
    }

    @Override
    public boolean createPlayerAccount(String playerName, String worldName) {
        // World desteği şimdilik yok
        return createPlayerAccount(playerName);
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer player, String worldName) {
        // World desteği şimdilik yok
        return createPlayerAccount(player);
    }

    /**
     * Economy'yi kapatır
     */
    public void shutdown() {
        enabled = false;
        if (economyService != null) {
            economyService.shutdown();
        }
        plugin.getLogger().info("Economy sistemi kapatıldı!");
    }
}