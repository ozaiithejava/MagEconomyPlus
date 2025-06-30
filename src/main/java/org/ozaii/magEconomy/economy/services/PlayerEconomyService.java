package org.ozaii.magEconomy.economy.services;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.ozaii.magEconomy.API.events.MoneyDepositEvent;
import org.ozaii.magEconomy.API.events.MoneyTransferEvent;
import org.ozaii.magEconomy.API.events.MoneyWithdrawEvent;
import org.ozaii.magEconomy.config.ConfigManager;
import org.ozaii.magEconomy.database.DatabaseManager;
import org.ozaii.magEconomy.economy.daos.PlayerEconomyDao;
import org.ozaii.magEconomy.economy.models.PlayerEconomy;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class PlayerEconomyService {

    private static PlayerEconomyService instance;
    private JavaPlugin plugin;
    private PlayerEconomyDao playerEconomyDao;
    private ConfigManager configManager;

    // Cache sistemi
    private final ConcurrentHashMap<UUID, PlayerEconomy> playerCache;
    private boolean cacheEnabled;
    private long cacheExpireTime;
    private final ConcurrentHashMap<UUID, Long> cacheTimestamps;

    // Ekonomi ayarları
    private double startingBalance;
    private double maxBalance;
    private double minBalance;
    private String currencyName;
    private String currencyNamePlural;
    private int fractionalDigits;

    private PlayerEconomyService() {
        this.playerCache = new ConcurrentHashMap<>();
        this.cacheTimestamps = new ConcurrentHashMap<>();
    }

    /**
     * PlayerEconomyService singleton instance'ını döndürür
     *
     * @return PlayerEconomyService instance
     */
    public static PlayerEconomyService getInstance() {
        if (instance == null) {
            instance = new PlayerEconomyService();
        }
        return instance;
    }

    /**
     * PlayerEconomyService'i başlatır
     *
     * @param plugin JavaPlugin instance
     */
    public void initialize(JavaPlugin plugin) {
        this.plugin = plugin;
        this.configManager = ConfigManager.getInstance();
        this.playerEconomyDao = new PlayerEconomyDao(plugin);

        // Economy config'ini oluştur ve ayarları yükle
        setupEconomyConfig();
        loadEconomySettings();

        // Tabloları oluştur
        DatabaseManager.getInstance().createTable(PlayerEconomy.class)
                .thenRun(() -> plugin.getLogger().info("PlayerEconomy tablosu hazır!"));

        plugin.getLogger().info("PlayerEconomyService başlatıldı!");
    }

    /**
     * Economy config dosyasını ayarlar
     */
    private void setupEconomyConfig() {
        configManager.createConfig("economy");
        FileConfiguration economyConfig = configManager.getConfig("economy");

        if (!economyConfig.contains("settings.starting-balance")) {
            economyConfig.set("settings.starting-balance", 1000.0);
            economyConfig.set("settings.max-balance", 1000000000.0);
            economyConfig.set("settings.min-balance", 0.0);
            economyConfig.set("settings.fractional-digits", 2);

            economyConfig.set("currency.singular", "MagCoin");
            economyConfig.set("currency.plural", "MagCoins");

            economyConfig.set("cache.enabled", false);
            economyConfig.set("cache.expire-time-minutes", 30);

            economyConfig.set("####### author", "ozaii1337");

            configManager.saveConfig("economy");
        }
    }

    /**
     * Ekonomi ayarlarını yükler
     */
    private void loadEconomySettings() {
        FileConfiguration economyConfig = configManager.getConfig("economy");

        this.startingBalance = economyConfig.getDouble("settings.starting-balance", 1000.0);
        this.maxBalance = economyConfig.getDouble("settings.max-balance", 1000000000.0);
        this.minBalance = economyConfig.getDouble("settings.min-balance", 0.0);
        this.fractionalDigits = economyConfig.getInt("settings.fractional-digits", 2);

        this.currencyName = economyConfig.getString("currency.singular", "Coin");
        this.currencyNamePlural = economyConfig.getString("currency.plural", "Coins");

        this.cacheEnabled = economyConfig.getBoolean("cache.enabled", true);
        this.cacheExpireTime = economyConfig.getLong("cache.expire-time-minutes", 30) * 60 * 1000; // Dakikayı milisaniyeye çevir
    }

    /**
     * Oyuncu hesabı oluşturur
     *
     * @param player OfflinePlayer
     * @return CompletableFuture<Boolean>
     */
    public CompletableFuture<Boolean> createAccount(OfflinePlayer player) {
        return createAccount(player.getUniqueId(), player.getName());
    }

    /**
     * Oyuncu hesabı oluşturur
     *
     * @param playerUUID Oyuncunun UUID'si
     * @param playerName Oyuncunun adı
     * @return CompletableFuture<Boolean>
     */
    public CompletableFuture<Boolean> createAccount(UUID playerUUID, String playerName) {
        return hasAccount(playerUUID).thenCompose(exists -> {
            if (exists) {
                return CompletableFuture.completedFuture(false);
            }

            PlayerEconomy playerEconomy = new PlayerEconomy(playerUUID, playerName, startingBalance);

            return playerEconomyDao.createAccount(playerEconomy).thenApply(success -> {
                if (success && cacheEnabled) {
                    updateCache(playerEconomy);
                }
                return success;
            });
        });
    }

    /**
     * Oyuncu hesabının var olup olmadığını kontrol eder
     *
     * @param player OfflinePlayer
     * @return CompletableFuture<Boolean>
     */
    public CompletableFuture<Boolean> hasAccount(OfflinePlayer player) {
        return hasAccount(player.getUniqueId());
    }

    /**
     * Oyuncu hesabının var olup olmadığını kontrol eder
     *
     * @param playerUUID Oyuncunun UUID'si
     * @return CompletableFuture<Boolean>
     */
    public CompletableFuture<Boolean> hasAccount(UUID playerUUID) {
        if (cacheEnabled && isInCache(playerUUID)) {
            return CompletableFuture.completedFuture(true);
        }
        return playerEconomyDao.exists(playerUUID);
    }

    /**
     * Oyuncunun bakiyesini getirir
     *
     * @param player OfflinePlayer
     * @return CompletableFuture<Double>
     */
    public CompletableFuture<Double> getBalance(OfflinePlayer player) {
        return getBalance(player.getUniqueId());
    }

    /**
     * Oyuncunun bakiyesini getirir
     *
     * @param playerUUID Oyuncunun UUID'si
     * @return CompletableFuture<Double>
     */
    public CompletableFuture<Double> getBalance(UUID playerUUID) {
        if (cacheEnabled && isInCache(playerUUID)) {
            PlayerEconomy cached = playerCache.get(playerUUID);
            return CompletableFuture.completedFuture(cached.getBalance());
        }

        return getPlayerEconomy(playerUUID).thenApply(playerEconomy -> {
            if (playerEconomy != null) {
                if (cacheEnabled) {
                    updateCache(playerEconomy);
                }
                return playerEconomy.getBalance();
            }
            return 0.0;
        });
    }

    /**
     * Oyuncunun yeterli parası olup olmadığını kontrol eder
     *
     * @param player OfflinePlayer
     * @param amount Kontrol edilecek miktar
     * @return CompletableFuture<Boolean>
     */
    public CompletableFuture<Boolean> has(OfflinePlayer player, double amount) {
        return has(player.getUniqueId(), amount);
    }

    /**
     * Oyuncunun yeterli parası olup olmadığını kontrol eder
     *
     * @param playerUUID Oyuncunun UUID'si
     * @param amount     Kontrol edilecek miktar
     * @return CompletableFuture<Boolean>
     */
    public CompletableFuture<Boolean> has(UUID playerUUID, double amount) {
        return getBalance(playerUUID).thenApply(balance -> balance >= amount);
    }

    /**
     * Oyuncunun bakiyesinden para çıkarır
     *
     * @param player OfflinePlayer
     * @param amount Çıkarılacak miktar
     * @return CompletableFuture<Boolean>
     */
    public CompletableFuture<Boolean> withdraw(OfflinePlayer player, double amount) {
        return withdraw(player.getUniqueId(), amount);
    }

    /**
     * Oyuncunun bakiyesinden para çıkarır
     *
     * @param playerUUID Oyuncunun UUID'si
     * @param amount     Çıkarılacak miktar
     * @return CompletableFuture<Boolean>
     */
    public CompletableFuture<Boolean> withdraw(UUID playerUUID, double amount) {
        if (amount < 0) return CompletableFuture.completedFuture(false);

        return getPlayerEconomy(playerUUID).thenCompose(playerEconomy -> {
            if (playerEconomy == null || !playerEconomy.hasBalance(amount)) {
                return CompletableFuture.completedFuture(false);
            }

            double currentBalance = playerEconomy.getBalance();
            double newBalance = currentBalance - amount;
            if (newBalance < minBalance) {
                return CompletableFuture.completedFuture(false);
            }

            return setBalance(playerUUID, newBalance).thenApply(success -> {
                if (success) {
                    // Event'i çağır
                    MoneyWithdrawEvent event = new MoneyWithdrawEvent(playerUUID, plugin, amount, newBalance);
                    Bukkit.getPluginManager().callEvent(event);
                }
                return success;
            });
        });
    }

    /**
     * Oyuncunun bakiyesine para ekler
     *
     * @param player OfflinePlayer
     * @param amount Eklenecek miktar
     * @return CompletableFuture<Boolean>
     */
    public CompletableFuture<Boolean> deposit(OfflinePlayer player, double amount) {
        return deposit(player.getUniqueId(), amount);
    }

    /**
     * Oyuncunun bakiyesine para ekler
     *
     * @param playerUUID Oyuncunun UUID'si
     * @param amount     Eklenecek miktar
     * @return CompletableFuture<Boolean>
     */
    public CompletableFuture<Boolean> deposit(UUID playerUUID, double amount) {
        if (amount < 0) return CompletableFuture.completedFuture(false);

        return getBalance(playerUUID).thenCompose(currentBalance -> {
            double newBalance = currentBalance + amount;
            if (newBalance > maxBalance) {
                return CompletableFuture.completedFuture(false);
            }

            return setBalance(playerUUID, newBalance).thenApply(success -> {
                if (success) {
                    // Event'i çağır
                    MoneyDepositEvent event = new MoneyDepositEvent(playerUUID, plugin, amount, newBalance);
                    Bukkit.getPluginManager().callEvent(event);
                }
                return success;
            });
        });
    }

    /**
     * Oyuncunun bakiyesini ayarlar
     *
     * @param playerUUID Oyuncunun UUID'si
     * @param balance    Yeni bakiye
     * @return CompletableFuture<Boolean>
     */
    public CompletableFuture<Boolean> setBalance(UUID playerUUID, double balance) {
        if (balance < minBalance || balance > maxBalance) {
            return CompletableFuture.completedFuture(false);
        }

        return playerEconomyDao.updateBalance(playerUUID, balance).thenApply(success -> {
            if (success && cacheEnabled && playerCache.containsKey(playerUUID)) {
                PlayerEconomy cached = playerCache.get(playerUUID);
                cached.setBalance(balance);
                updateCache(cached);
            }
            return success;
        });
    }

    /**
     * PlayerEconomy nesnesini getirir
     *
     * @param playerUUID Oyuncunun UUID'si
     * @return CompletableFuture<PlayerEconomy>
     */
    public CompletableFuture<PlayerEconomy> getPlayerEconomy(UUID playerUUID) {
        if (cacheEnabled && isInCache(playerUUID)) {
            return CompletableFuture.completedFuture(playerCache.get(playerUUID));
        }

        return playerEconomyDao.getByUUID(playerUUID).thenApply(playerEconomy -> {
            if (playerEconomy != null && cacheEnabled) {
                updateCache(playerEconomy);
            }
            return playerEconomy;
        });
    }

    /**
     * En zengin oyuncuları getirir
     *
     * @param limit Limit
     * @return CompletableFuture<List < PlayerEconomy>>
     */
    public CompletableFuture<List<PlayerEconomy>> getTopPlayers(int limit) {
        return playerEconomyDao.getTopPlayers(limit);
    }

    /**
     * Para formatlar
     *
     * @param amount Miktar
     * @return Formatlanmış string
     */
    public String format(double amount) {
        if (fractionalDigits == 0) {
            return String.format("%.0f %s", amount, amount == 1 ? currencyName : currencyNamePlural);
        } else {
            return String.format("%." + fractionalDigits + "f %s", amount, amount == 1 ? currencyName : currencyNamePlural);
        }
    }

    // Cache yönetimi metodları

    private boolean isInCache(UUID playerUUID) {
        if (!playerCache.containsKey(playerUUID)) {
            return false;
        }

        Long timestamp = cacheTimestamps.get(playerUUID);
        if (timestamp == null) {
            return false;
        }

        // Cache süresi dolmuş mu kontrol et
        if (System.currentTimeMillis() - timestamp > cacheExpireTime) {
            playerCache.remove(playerUUID);
            cacheTimestamps.remove(playerUUID);
            return false;
        }

        return true;
    }

    private void updateCache(PlayerEconomy playerEconomy) {
        if (cacheEnabled) {
            playerCache.put(playerEconomy.getPlayerUUIDAsUUID(), playerEconomy);
            cacheTimestamps.put(playerEconomy.getPlayerUUIDAsUUID(), System.currentTimeMillis());
        }
    }

    /**
     * Cache'i temizler
     */
    public void clearCache() {
        playerCache.clear();
        cacheTimestamps.clear();
        plugin.getLogger().info("Player cache temizlendi.");
    }

    /**
     * Belirtilen oyuncuyu cache'den kaldırır
     *
     * @param playerUUID Oyuncunun UUID'si
     */
    public void removeFromCache(UUID playerUUID) {
        playerCache.remove(playerUUID);
        cacheTimestamps.remove(playerUUID);
    }

    /**
     * Cache istatistiklerini döndürür
     *
     * @return Cache boyutu
     */
    public int getCacheSize() {
        return playerCache.size();
    }

    // Getter metodları

    public double getStartingBalance() {
        return startingBalance;
    }

    public double getMaxBalance() {
        return maxBalance;
    }

    public double getMinBalance() {
        return minBalance;
    }

    public String getCurrencyName() {
        return currencyName;
    }

    public String getCurrencyNamePlural() {
        return currencyNamePlural;
    }

    public int getFractionalDigits() {
        return fractionalDigits;
    }

    /**
     * Ayarları yeniden yükler
     */
    public void reloadSettings() {
        configManager.reloadConfig("economy");
        loadEconomySettings();
        clearCache(); // Cache'i temizle çünkü ayarlar değişmiş olabilir
        plugin.getLogger().info("Economy ayarları yeniden yüklendi!");
    }

    /**
     * Toplam oyuncu sayısını döndürür
     *
     * @return CompletableFuture<Long>
     */
    public CompletableFuture<Long> getTotalPlayers() {
        return playerEconomyDao.getTotalPlayers();
    }

    /**
     * Toplam ekonomik değeri döndürür
     *
     * @return CompletableFuture<Double>
     */
    public CompletableFuture<Double> getTotalEconomicValue() {
        return playerEconomyDao.getTotalEconomicValue();
    }

    /**
     * Oyuncu hesabını siler
     *
     * @param playerUUID Oyuncunun UUID'si
     * @return CompletableFuture<Boolean>
     */
    public CompletableFuture<Boolean> deleteAccount(UUID playerUUID) {
        return playerEconomyDao.delete(playerUUID).thenApply(success -> {
            if (success) {
                removeFromCache(playerUUID);
            }
            return success;
        });
    }

    /**
     * İki oyuncu arasında para transferi yapar
     *
     * @param fromUUID Gönderen oyuncu UUID
     * @param toUUID   Alan oyuncu UUID
     * @param amount   Transfer miktarı
     * @return CompletableFuture<Boolean>
     */
    public CompletableFuture<Boolean> transfer(UUID fromUUID, UUID toUUID, double amount) {
        if (amount <= 0) {
            return CompletableFuture.completedFuture(false);
        }

        return withdraw(fromUUID, amount).thenCompose(withdrawSuccess -> {
            if (!withdrawSuccess) {
                return CompletableFuture.completedFuture(false);
            }

            return deposit(toUUID, amount).thenCompose(depositSuccess -> {
                if (!depositSuccess) {
                    // Deposit başarısız olursa parayı geri ver
                    return deposit(fromUUID, amount).thenApply(rollbackSuccess -> {
                        if (!rollbackSuccess) {
                            plugin.getLogger().severe("Transfer rollback başarısız! " +
                                    "Oyuncu: " + fromUUID + ", Miktar: " + amount);
                        }
                        return false;
                    });
                } else {
                    // Transfer başarılı, event'i çağır
                    MoneyTransferEvent event = new MoneyTransferEvent(fromUUID, toUUID, plugin, amount);
                    Bukkit.getPluginManager().callEvent(event);
                }
                return CompletableFuture.completedFuture(true);
            });
        });
    }

    /**
     * Belirtilen bakiye aralığındaki oyuncuları getirir
     *
     * @param minBalance Minimum bakiye
     * @param maxBalance Maximum bakiye
     * @return CompletableFuture<List < PlayerEconomy>>
     */
    public CompletableFuture<List<PlayerEconomy>> getPlayersByBalanceRange(double minBalance, double maxBalance) {
        return playerEconomyDao.getPlayersByBalanceRange(minBalance, maxBalance);
    }

    /**
     * Service'i kapatır ve temizlik yapar
     */
    public void shutdown() {
        clearCache();
        plugin.getLogger().info("PlayerEconomyService kapatıldı.");
    }
}