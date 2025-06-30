package org.ozaii.magEconomy;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.ozaii.magEconomy.API.MagEconomyAPI;
import org.ozaii.magEconomy.command.EconomyCommand;
import org.ozaii.magEconomy.config.ConfigManager;
import org.ozaii.magEconomy.database.DatabaseManager;
import org.ozaii.magEconomy.economy.services.PlayerEconomyService;
import org.ozaii.magEconomy.listeners.PlayerAccountChecker;
import org.ozaii.magEconomy.placeholder.core.PlaceholderRegistry;

import java.util.logging.Logger;
import java.util.logging.Level;

public final class MagEconomy extends JavaPlugin {

    private static MagEconomy instance;
    private Economy econ = null;
    private org.ozaii.magEconomy.economy.Economy customEconomy;
    private boolean shutdownInProgress = false;

    /* close hikari logging */
    static {
        Logger hikariLogger = Logger.getLogger("com.zaxxer.hikari");
        hikariLogger.setLevel(Level.OFF);

        for (var handler : hikariLogger.getHandlers()) {
            handler.setLevel(Level.OFF);
        }
        hikariLogger.setUseParentHandlers(false);
    }

    @Override
    public void onEnable() {
        instance = this;

        try {
            getLogger().info("MagEconomy başlatılıyor...");

            if (!initializeCore()) {
                disablePlugin("Core servisler başlatılamadı!");
                return;
            }

            if (!initializeEconomyServices()) {
                disablePlugin("Economy servisleri başlatılamadı!");
                return;
            }

            if (!setupEconomy()) {
                disablePlugin("Vault economy kurulamadı!");
                return;
            }

            if (!initializeCommandsAndListeners()) {
                disablePlugin("Commands ve Listeners başlatılamadı!");
                return;
            }

            if (!initializeAPI()) {
                disablePlugin("MagEconomy API başlatılamadı!");
                return;
            }

            initializePlaceholders();

            getLogger().info("MagEconomy başarıyla yüklendi!");

            getLogger().info("MagEconomy Kontrol: " + getHealthStatus());

        } catch (Exception e) {
            getLogger().severe("MagEconomy başlatma sırasında hata oluştu: " + e.getMessage());
            e.printStackTrace();
            disablePlugin("Beklenmeyen hata!");
        }
    }

    @Override
    public void onDisable() {
        if (shutdownInProgress) return;
        shutdownInProgress = true;

        getLogger().info("MagEconomy kapatılıyor...");

        try {

            shutdownAPI();

            shutdownPlaceholders();

            shutdownDatabase();

            instance = null;

            getLogger().info("MagEconomy başarıyla kapatıldı!");

        } catch (Exception e) {
            getLogger().severe("MagEconomy kapatma sırasında hata oluştu: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Core servisleri başlatır (Config, Database, PlayerEconomyService)
     */
    private boolean initializeCore() {
        try {
            getLogger().info("Core servisler başlatılıyor...");

            ConfigManager.getInstance().initialize(this);
            getLogger().fine("ConfigManager başlatıldı");

            DatabaseManager.getInstance().initialize(this);
            getLogger().fine("DatabaseManager başlatıldı");

            PlayerEconomyService.getInstance().initialize(this);
            getLogger().fine("PlayerEconomyService başlatıldı");

            return true;
        } catch (Exception e) {
            getLogger().severe("Core servisler başlatılırken hata: " + e.getMessage());
            return false;
        }
    }

    /**
     * Economy servisleri başlatır
     */
    private boolean initializeEconomyServices() {
        try {
            getLogger().info("Economy servisleri başlatılıyor...");

            customEconomy = org.ozaii.magEconomy.economy.Economy.getInstance();
            customEconomy.initialize(this);
            getLogger().fine("Custom Economy başlatıldı");

            return true;
        } catch (Exception e) {
            getLogger().severe("Economy servisleri başlatılırken hata: " + e.getMessage());
            return false;
        }
    }

    /**
     * Commands ve Listeners başlatır
     */
    private boolean initializeCommandsAndListeners() {
        try {
            getLogger().info("Commands ve Listeners başlatılıyor...");

            EconomyCommand economyCommand = new EconomyCommand(this);
            getCommand("eco").setExecutor(economyCommand);
            getCommand("eco").setTabCompleter(economyCommand);
            getLogger().fine("Economy Command başlatıldı");

            PlayerAccountChecker.getInstance().initialize(this);
            getLogger().fine("PlayerAccountChecker başlatıldı");

            return true;
        } catch (Exception e) {
            getLogger().severe("Commands ve Listeners başlatılırken hata: " + e.getMessage());
            return false;
        }
    }

    /**
     * MagEconomy API'sini başlatır
     */
    private boolean initializeAPI() {
        try {
            getLogger().info("MagEconomy API başlatılıyor...");

            MagEconomyAPI.getInstance().initialize(
                    PlayerEconomyService.getInstance(),
                    this
            );

            getLogger().info("MagEconomy API başarıyla başlatıldı!");
            return true;

        } catch (IllegalStateException e) {
            getLogger().severe("API zaten başlatılmış: " + e.getMessage());
            return false;
        } catch (IllegalArgumentException e) {
            getLogger().severe("API başlatma parametreleri geçersiz: " + e.getMessage());
            return false;
        } catch (Exception e) {
            getLogger().severe("API başlatılırken beklenmeyen hata: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * PlaceholderAPI entegrasyonunu başlatır (opsiyonel)
     */
    private void initializePlaceholders() {
        try {
            if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
                getLogger().info("PlaceholderAPI entegrasyonu başlatılıyor...");

                PlaceholderRegistry.initialize(this);
                PlaceholderRegistry.getInstance().registerAll();

                getLogger().info("PlaceholderAPI entegrasyonu başarıyla başlatıldı!");
            } else {
                getLogger().info("PlaceholderAPI bulunamadı, placeholder desteği atlanıyor.");
            }
        } catch (Exception e) {
            getLogger().warning("PlaceholderAPI başlatılırken hata (opsiyonel): " + e.getMessage());
        }
    }

    /**
     * Vault Economy kurulumu
     */
    private boolean setupEconomy() {
        try {
            getLogger().info("Vault Economy kurulumu yapılıyor...");

            if (getServer().getPluginManager().getPlugin("Vault") == null) {
                getLogger().severe("Vault eklentisi bulunamadı! Vault gereklidir.");
                return false;
            }

            getServer().getServicesManager().register(
                    Economy.class,
                    customEconomy,
                    this,
                    ServicePriority.Highest
            );

            RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager()
                    .getRegistration(Economy.class);

            if (rsp == null) {
                getLogger().severe("Economy servisi kaydedilemedi!");
                return false;
            }

            econ = rsp.getProvider();
            if (econ == null) {
                getLogger().severe("Economy provider alınamadı!");
                return false;
            }

            getLogger().info("Vault Economy başarıyla kuruldu!");
            return true;

        } catch (Exception e) {
            getLogger().severe("Vault Economy kurulumunda hata: " + e.getMessage());
            return false;
        }
    }

    /**
     * API'yi güvenli şekilde kapatır
     */
    private void shutdownAPI() {
        try {
            if (MagEconomyAPI.getInstance().isReady()) {
                MagEconomyAPI.getInstance().shutdown();
                getLogger().info("MagEconomy API kapatıldı");
            }
        } catch (Exception e) {
            getLogger().warning("API kapatılırken hata: " + e.getMessage());
        }
    }

    /**
     * PlaceholderAPI'yi güvenli şekilde kapatır
     */
    private void shutdownPlaceholders() {
        try {
            if (PlaceholderRegistry.isInitialized()) {
                PlaceholderRegistry.getInstance().shutdown();
                getLogger().fine("PlaceholderRegistry kapatıldı");
            }
        } catch (Exception e) {
            getLogger().warning("PlaceholderRegistry kapatılırken hata: " + e.getMessage());
        }
    }

    /**
     * Database'i güvenli şekilde kapatır
     */
    private void shutdownDatabase() {
        try {
            if (DatabaseManager.getInstance() != null) {
                DatabaseManager.getInstance().close();
                getLogger().fine("DatabaseManager kapatıldı");
            }
        } catch (Exception e) {
            getLogger().warning("DatabaseManager kapatılırken hata: " + e.getMessage());
        }
    }

    /**
     * Plugin'i hata ile kapatır
     */
    private void disablePlugin(String reason) {
        getLogger().severe("Plugin kapatılıyor: " + reason);
        getServer().getPluginManager().disablePlugin(this);
    }

    /**
     * Thread-safe instance getter
     */
    public static MagEconomy getInstance() {
        MagEconomy currentInstance = instance;
        if (currentInstance == null) {
            throw new IllegalStateException("Plugin henüz yüklenmedi veya kapatıldı!");
        }
        return currentInstance;
    }

    /**
     * Vault Economy getter
     */
    public Economy getEconomy() {
        return econ;
    }

    /**
     * Custom Economy getter
     */
    public org.ozaii.magEconomy.economy.Economy getCustomEconomy() {
        return customEconomy;
    }

    /**
     * Plugin'in sağlıklı çalışıp çalışmadığını kontrol eder
     */
    public boolean isHealthy() {
        return !shutdownInProgress &&
                instance != null &&
                isEnabled() &&
                MagEconomyAPI.getInstance().isReady() &&
                econ != null;
    }

    /**
     * Plugin durumu hakkında detaylı bilgi döndürür
     */
    public String getHealthStatus() {
        StringBuilder status = new StringBuilder();
        status.append("MagEconomy Health Status:\n");
        status.append("- Plugin Enabled: ").append(isEnabled()).append("\n");
        status.append("- Shutdown in Progress: ").append(shutdownInProgress).append("\n");
        status.append("- API Ready: ").append(MagEconomyAPI.getInstance().isReady()).append("\n");
        status.append("- Vault Economy: ").append(econ != null ? "OK" : "NULL").append("\n");
        status.append("- Database: ").append(DatabaseManager.getInstance() != null ? "OK" : "NULL").append("\n");
        status.append("- PlaceholderAPI: ").append(PlaceholderRegistry.isInitialized() ? "OK" : "NOT_LOADED").append("\n");
        return status.toString();
    }
}