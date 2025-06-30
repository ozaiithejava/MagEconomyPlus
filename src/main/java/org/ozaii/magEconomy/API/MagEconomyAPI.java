package org.ozaii.magEconomy.API;

import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.java.JavaPlugin;
import org.ozaii.magEconomy.economy.models.PlayerEconomy;
import org.ozaii.magEconomy.economy.services.PlayerEconomyService;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * Thread-Safe ve güvenli MagEconomy API
 * Enum Singleton Pattern kullanarak thread safety sağlar
 */
public enum MagEconomyAPI {
    INSTANCE;

    private volatile PlayerEconomyService economyService;
    private volatile boolean initialized = false;
    private Logger logger;

    /**
     * API instance'ını alır
     * @return MagEconomyAPI instance
     */
    public static MagEconomyAPI getInstance() {
        return INSTANCE;
    }

    /**
     * API'yi başlatır (Sadece MagEconomy eklentisi tarafından çağrılmalı)
     * @param economyService PlayerEconomyService instance
     * @param plugin MagEconomy plugin instance
     * @throws IllegalArgumentException eğer parametreler null ise
     * @throws IllegalStateException eğer API zaten başlatılmış ise
     */
    public synchronized void initialize(PlayerEconomyService economyService, JavaPlugin plugin) {
        if (economyService == null) {
            throw new IllegalArgumentException("EconomyService null olamaz!");
        }
        if (plugin == null) {
            throw new IllegalArgumentException("Plugin null olamaz!");
        }
        if (this.initialized) {
            throw new IllegalStateException("API zaten başlatılmış!");
        }

        this.economyService = economyService;
        this.logger = plugin.getLogger();
        this.initialized = true;

        logger.info("MagEconomy API başarıyla başlatıldı!");
    }

    /**
     * API'yi kapatır ve temizler
     */
    public synchronized void shutdown() {
        if (initialized) {
            this.initialized = false;
            this.economyService = null;
            this.logger = null;
            if (logger != null) {
                logger.info("MagEconomy API kapatıldı.");
            }
        }
    }

    /**
     * API'nin hazır olup olmadığını kontrol eder
     * @return boolean
     */
    public boolean isReady() {
        return initialized && economyService != null;
    }

    // === TEMEL EKONOMİ İŞLEMLERİ ===

    /**
     * Oyuncu hesabının var olup olmadığını kontrol eder
     */
    public CompletableFuture<Boolean> hasAccount(OfflinePlayer player) {
        checkInitialized();
        validatePlayer(player);

        return economyService.hasAccount(player)
                .exceptionally(throwable -> {
                    logError("Hesap kontrolü hatası", throwable);
                    return false;
                });
    }

    public CompletableFuture<Boolean> hasAccount(UUID playerUUID) {
        checkInitialized();
        validateUUID(playerUUID);

        return economyService.hasAccount(playerUUID)
                .exceptionally(throwable -> {
                    logError("Hesap kontrolü hatası", throwable);
                    return false;
                });
    }

    /**
     * Oyuncu hesabı oluşturur
     */
    public CompletableFuture<Boolean> createAccount(OfflinePlayer player) {
        checkInitialized();
        validatePlayer(player);

        return economyService.createAccount(player)
                .exceptionally(throwable -> {
                    logError("Hesap oluşturma hatası", throwable);
                    return false;
                });
    }

    public CompletableFuture<Boolean> createAccount(UUID playerUUID, String playerName) {
        checkInitialized();
        validateUUID(playerUUID);
        validatePlayerName(playerName);

        return economyService.createAccount(playerUUID, playerName)
                .exceptionally(throwable -> {
                    logError("Hesap oluşturma hatası", throwable);
                    return false;
                });
    }

    /**
     * Oyuncunun bakiyesini getirir
     */
    public CompletableFuture<Double> getBalance(OfflinePlayer player) {
        checkInitialized();
        validatePlayer(player);

        return economyService.getBalance(player)
                .exceptionally(throwable -> {
                    logError("Bakiye getirme hatası", throwable);
                    return 0.0;
                });
    }

    public CompletableFuture<Double> getBalance(UUID playerUUID) {
        checkInitialized();
        validateUUID(playerUUID);

        return economyService.getBalance(playerUUID)
                .exceptionally(throwable -> {
                    logError("Bakiye getirme hatası", throwable);
                    return 0.0;
                });
    }

    /**
     * Oyuncunun yeterli parası olup olmadığını kontrol eder
     */
    public CompletableFuture<Boolean> has(OfflinePlayer player, double amount) {
        checkInitialized();
        validatePlayer(player);
        validateAmount(amount);

        return economyService.has(player, amount)
                .exceptionally(throwable -> {
                    logError("Para kontrolü hatası", throwable);
                    return false;
                });
    }

    public CompletableFuture<Boolean> has(UUID playerUUID, double amount) {
        checkInitialized();
        validateUUID(playerUUID);
        validateAmount(amount);

        return economyService.has(playerUUID, amount)
                .exceptionally(throwable -> {
                    logError("Para kontrolü hatası", throwable);
                    return false;
                });
    }

    /**
     * Oyuncunun bakiyesinden para çıkarır
     */
    public CompletableFuture<Boolean> withdraw(OfflinePlayer player, double amount) {
        checkInitialized();
        validatePlayer(player);
        validatePositiveAmount(amount);

        return economyService.withdraw(player, amount)
                .exceptionally(throwable -> {
                    logError("Para çekme hatası", throwable);
                    return false;
                });
    }

    public CompletableFuture<Boolean> withdraw(UUID playerUUID, double amount) {
        checkInitialized();
        validateUUID(playerUUID);
        validatePositiveAmount(amount);

        return economyService.withdraw(playerUUID, amount)
                .exceptionally(throwable -> {
                    logError("Para çekme hatası", throwable);
                    return false;
                });
    }

    /**
     * Oyuncunun bakiyesine para ekler
     */
    public CompletableFuture<Boolean> deposit(OfflinePlayer player, double amount) {
        checkInitialized();
        validatePlayer(player);
        validatePositiveAmount(amount);

        return economyService.deposit(player, amount)
                .exceptionally(throwable -> {
                    logError("Para yatırma hatası", throwable);
                    return false;
                });
    }

    public CompletableFuture<Boolean> deposit(UUID playerUUID, double amount) {
        checkInitialized();
        validateUUID(playerUUID);
        validatePositiveAmount(amount);

        return economyService.deposit(playerUUID, amount)
                .exceptionally(throwable -> {
                    logError("Para yatırma hatası", throwable);
                    return false;
                });
    }

    /**
     * Oyuncunun bakiyesini ayarlar
     */
    public CompletableFuture<Boolean> setBalance(UUID playerUUID, double balance) {
        checkInitialized();
        validateUUID(playerUUID);
        validateAmount(balance);

        return economyService.setBalance(playerUUID, balance)
                .exceptionally(throwable -> {
                    logError("Bakiye ayarlama hatası", throwable);
                    return false;
                });
    }

    /**
     * İki oyuncu arasında para transferi yapar
     */
    public CompletableFuture<Boolean> transfer(UUID fromUUID, UUID toUUID, double amount) {
        checkInitialized();
        validateUUID(fromUUID);
        validateUUID(toUUID);
        validatePositiveAmount(amount);

        if (fromUUID.equals(toUUID)) {
            throw new IllegalArgumentException("Gönderici ve alıcı aynı olamaz!");
        }

        return economyService.transfer(fromUUID, toUUID, amount)
                .exceptionally(throwable -> {
                    logError("Transfer hatası", throwable);
                    return false;
                });
    }

    // === FORMAT VE PARA BİRİMİ ===

    /**
     * Para miktarını formatlar
     */
    public String format(double amount) {
        checkInitialized();
        validateAmount(amount);

        try {
            return economyService.format(amount);
        } catch (Exception e) {
            logError("Format hatası", e);
            return String.format("%.2f", amount);
        }
    }

    /**
     * Para biriminin tekil adını döndürür
     */
    public String getCurrencyName() {
        checkInitialized();
        try {
            return economyService.getCurrencyName();
        } catch (Exception e) {
            logError("Para birimi adı alma hatası", e);
            return "Coin";
        }
    }

    /**
     * Para biriminin çoğul adını döndürür
     */
    public String getCurrencyNamePlural() {
        checkInitialized();
        try {
            return economyService.getCurrencyNamePlural();
        } catch (Exception e) {
            logError("Para birimi çoğul adı alma hatası", e);
            return "Coins";
        }
    }

    /**
     * Ondalık basamak sayısını döndürür
     */
    public int getFractionalDigits() {
        checkInitialized();
        try {
            return economyService.getFractionalDigits();
        } catch (Exception e) {
            logError("Ondalık basamak sayısı alma hatası", e);
            return 2;
        }
    }

    // === LİMİTLER VE AYARLAR ===

    /**
     * Başlangıç bakiyesini döndürür
     */
    public double getStartingBalance() {
        checkInitialized();
        try {
            return economyService.getStartingBalance();
        } catch (Exception e) {
            logError("Başlangıç bakiyesi alma hatası", e);
            return 0.0;
        }
    }

    /**
     * Maksimum bakiyeyi döndürür
     */
    public double getMaxBalance() {
        checkInitialized();
        try {
            return economyService.getMaxBalance();
        } catch (Exception e) {
            logError("Maksimum bakiye alma hatası", e);
            return Double.MAX_VALUE;
        }
    }

    /**
     * Minimum bakiyeyi döndürür
     */
    public double getMinBalance() {
        checkInitialized();
        try {
            return economyService.getMinBalance();
        } catch (Exception e) {
            logError("Minimum bakiye alma hatası", e);
            return 0.0;
        }
    }

    // === İSTATİSTİKLER ===

    /**
     * En zengin oyuncuları getirir
     */
    public CompletableFuture<List<PlayerEconomy>> getTopPlayers(int limit) {
        checkInitialized();
        if (limit <= 0) {
            throw new IllegalArgumentException("Limit pozitif olmalıdır!");
        }
        if (limit > 1000) {
            throw new IllegalArgumentException("Limit 1000'den büyük olamaz!");
        }

        return economyService.getTopPlayers(limit)
                .exceptionally(throwable -> {
                    logError("Top oyuncular getirme hatası", throwable);
                    return List.of();
                });
    }

    /**
     * Toplam oyuncu sayısını döndürür
     */
    public CompletableFuture<Long> getTotalPlayers() {
        checkInitialized();
        return economyService.getTotalPlayers()
                .exceptionally(throwable -> {
                    logError("Toplam oyuncu sayısı alma hatası", throwable);
                    return 0L;
                });
    }

    /**
     * Toplam ekonomik değeri döndürür
     */
    public CompletableFuture<Double> getTotalEconomicValue() {
        checkInitialized();
        return economyService.getTotalEconomicValue()
                .exceptionally(throwable -> {
                    logError("Toplam ekonomik değer alma hatası", throwable);
                    return 0.0;
                });
    }

    /**
     * Belirtilen bakiye aralığındaki oyuncuları getirir
     */
    public CompletableFuture<List<PlayerEconomy>> getPlayersByBalanceRange(double minBalance, double maxBalance) {
        checkInitialized();
        validateAmount(minBalance);
        validateAmount(maxBalance);

        if (minBalance > maxBalance) {
            throw new IllegalArgumentException("Minimum bakiye maksimum bakiyeden büyük olamaz!");
        }

        return economyService.getPlayersByBalanceRange(minBalance, maxBalance)
                .exceptionally(throwable -> {
                    logError("Bakiye aralığı oyuncular getirme hatası", throwable);
                    return List.of();
                });
    }

    // === VALİDASYON METODLARİ ===

    private void checkInitialized() {
        if (!isReady()) {
            throw new IllegalStateException("MagEconomy API henüz başlatılmadı! MagEconomy eklentisinin yüklü olduğundan emin olun.");
        }
    }

    private void validatePlayer(OfflinePlayer player) {
        if (player == null) {
            throw new IllegalArgumentException("Player null olamaz!");
        }
    }

    private void validateUUID(UUID uuid) {
        if (uuid == null) {
            throw new IllegalArgumentException("UUID null olamaz!");
        }
    }

    private void validatePlayerName(String playerName) {
        if (playerName == null || playerName.trim().isEmpty()) {
            throw new IllegalArgumentException("Oyuncu adı null veya boş olamaz!");
        }
        if (playerName.length() > 16) {
            throw new IllegalArgumentException("Oyuncu adı 16 karakterden uzun olamaz!");
        }
    }

    private void validateAmount(double amount) {
        if (Double.isNaN(amount) || Double.isInfinite(amount)) {
            throw new IllegalArgumentException("Miktar geçersiz! (NaN veya Infinite)");
        }
    }

    private void validatePositiveAmount(double amount) {
        validateAmount(amount);
        if (amount < 0) {
            throw new IllegalArgumentException("Miktar negatif olamaz!");
        }
        if (amount == 0) {
            throw new IllegalArgumentException("Miktar sıfır olamaz!");
        }
    }

    private void logError(String message, Throwable throwable) {
        if (logger != null) {
            logger.severe(message + ": " + throwable.getMessage());
        }
    }
}