package org.ozaii.magEconomy.database;


import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.ozaii.magEconomy.config.ConfigManager;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class DatabaseManager {

    private static DatabaseManager instance;
    private JavaPlugin plugin;
    private HikariDataSource dataSource;
    private ConnectionSource connectionSource;
    private DatabaseType currentDatabaseType;
    private Map<Class<?>, Dao<?, ?>> daoCache;


    public enum DatabaseType {
        MYSQL, SQLITE
    }

    private DatabaseManager() {
        daoCache = new HashMap<>();
        // HikariCP loglamasını minimize et
        System.setProperty("hikaricp.configurationFile", "");
        System.setProperty("com.zaxxer.hikari.housekeeping.periodMs", "60000");
        disableHikariLogging();

    }

    /**
     * DatabaseManager singleton instance'ını döndürür
     * @return DatabaseManager instance
     */
    public static DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    /**
     * DatabaseManager'ı başlatır
     * @param plugin JavaPlugin instance
     */
    public void initialize(JavaPlugin plugin) {
        this.plugin = plugin;

        // Database config'ini oluştur
        ConfigManager configManager = ConfigManager.getInstance();
        configManager.createConfig("database");

        // Default database config'ini ayarla
        setupDefaultDatabaseConfig();

        // Veritabanı bağlantısını başlat
        initializeDatabase();
    }

    /**
     * Default database config ayarlarını oluşturur
     */
    private void setupDefaultDatabaseConfig() {
        FileConfiguration dbConfig = ConfigManager.getInstance().getConfig("database");

        // MySQL ayarları
        if (!dbConfig.contains("mysql")) {
            dbConfig.set("mysql.enabled", true);
            dbConfig.set("mysql.host", "localhost");
            dbConfig.set("mysql.port", 3306);
            dbConfig.set("mysql.database", "minecraft");
            dbConfig.set("mysql.username", "root");
            dbConfig.set("mysql.password", "");
            dbConfig.set("mysql.useSSL", false);
            dbConfig.set("mysql.autoReconnect", true);
            dbConfig.set("mysql.maxPoolSize", 10);
            dbConfig.set("mysql.minPoolSize", 2);
            dbConfig.set("mysql.maxLifetime", 1800000); // 30 dakika
            dbConfig.set("mysql.connectionTimeout", 5000); // 5 saniye
        }

        // SQLite ayarları
        if (!dbConfig.contains("sqlite")) {
            dbConfig.set("sqlite.filename", "database.db");
        }

        ConfigManager.getInstance().saveConfig("database");
    }

    /**
     * Veritabanı bağlantısını başlatır (önce MySQL, başarısız olursa SQLite)
     */
    /**
     * Veritabanı bağlantısını başlatır (önce MySQL, başarısız olursa SQLite)
     */
    private void initializeDatabase() {
        FileConfiguration dbConfig = ConfigManager.getInstance().getConfig("database");

        // Önce MySQL'i dene
        if (dbConfig.getBoolean("mysql.enabled", true)) {
            if (initializeMySQL()) {
                plugin.getLogger().info("MySQL veritabanına başarıyla bağlanıldı!");
                currentDatabaseType = DatabaseType.MYSQL;
                return;
            } else {
                // Sadece SQLite'a geçiş mesajı göster
                plugin.getLogger().info("SQLite veritabanına geçiliyor...");
            }
        }

        // MySQL başarısız olursa SQLite'ı başlat
        if (initializeSQLite()) {
            plugin.getLogger().info("SQLite veritabanı başarıyla başlatıldı!");
            currentDatabaseType = DatabaseType.SQLITE;
        } else {
            plugin.getLogger().severe("Hiçbir veritabanı türü başlatılamadı!");
            throw new RuntimeException("Veritabanı başlatılamadı!");
        }
    }

    private void disableHikariLogging() {
        try {
            // HikariCP'nin kendi logger'ını kapat
            java.util.logging.Logger hikariLogger = java.util.logging.Logger.getLogger("com.zaxxer.hikari");
            hikariLogger.setLevel(java.util.logging.Level.OFF);
            hikariLogger.setUseParentHandlers(false);

            // HikariDataSource logger'ını kapat
            java.util.logging.Logger hikariDataSourceLogger = java.util.logging.Logger.getLogger("com.zaxxer.hikari.HikariDataSource");
            hikariDataSourceLogger.setLevel(java.util.logging.Level.OFF);
            hikariDataSourceLogger.setUseParentHandlers(false);

            // HikariPool logger'ını kapat
            java.util.logging.Logger hikariPoolLogger = java.util.logging.Logger.getLogger("com.zaxxer.hikari.pool.HikariPool");
            hikariPoolLogger.setLevel(java.util.logging.Level.OFF);
            hikariPoolLogger.setUseParentHandlers(false);

            // Sistem özelliklerini ayarla
            System.setProperty("com.zaxxer.hikari.alwaysCreateNewConnection", "false");
            System.setProperty("hikaricp.configurationFile", "");
        } catch (Exception e) {
            // Sessizce geç
        }
    }

    /**
     * MySQL bağlantısını başlatır
     * @return Başarılı ise true
     */
    private boolean initializeMySQL() {
        FileConfiguration dbConfig = ConfigManager.getInstance().getConfig("database");
        String host = dbConfig.getString("mysql.host");
        int port = dbConfig.getInt("mysql.port");
        String database = dbConfig.getString("mysql.database");
        String username = dbConfig.getString("mysql.username");
        String password = dbConfig.getString("mysql.password");

        String jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&serverTimezone=UTC&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false";

        try {
            // Önce loglama sistemini kapat
            disableHikariLogging();

            HikariConfig hikariConfig = new HikariConfig();
            hikariConfig.setJdbcUrl(jdbcUrl);
            hikariConfig.setUsername(username);
            hikariConfig.setPassword(password);
            hikariConfig.setMaximumPoolSize(dbConfig.getInt("mysql.maxPoolSize", 10));
            hikariConfig.setMinimumIdle(dbConfig.getInt("mysql.minPoolSize", 2));
            hikariConfig.setConnectionTimeout(dbConfig.getLong("mysql.connectionTimeout", 5000));
            hikariConfig.setIdleTimeout(60000);
            hikariConfig.setMaxLifetime(dbConfig.getLong("mysql.maxLifetime", 1800000));

            // HikariCP'nin kendi loglama sistemini kapat
            hikariConfig.setRegisterMbeans(false);
            hikariConfig.setLeakDetectionThreshold(0);

            // Test bağlantısını kapat (bu da log üretebilir)
            hikariConfig.setInitializationFailTimeout(-1);

            this.dataSource = new HikariDataSource(hikariConfig);

            // Test bağlantısı yap ama sessizce
            try (Connection testConnection = this.dataSource.getConnection()) {
                if (testConnection.isValid(3)) {
                    this.connectionSource = new JdbcConnectionSource(jdbcUrl, username, password);
                    return true;
                }
            }

            return false;

        } catch (Exception e) {
            // Tüm hatalar sessizce geçilsin
            try {
                if (this.dataSource != null && !this.dataSource.isClosed()) {
                    this.dataSource.close();
                }
            } catch (Exception closeException) {
                // Sessizce geç
            }
            return false;
        }
    }
    /**
     * SQLite bağlantısını başlatır
     * @return Başarılı ise true
     */
    private boolean initializeSQLite() {
        try {
            FileConfiguration dbConfig = ConfigManager.getInstance().getConfig("database");
            String filename = dbConfig.getString("sqlite.filename", "database.db");

            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }

            File dbFile = new File(dataFolder, filename);
            String jdbcUrl = "jdbc:sqlite:" + dbFile.getAbsolutePath();

            HikariConfig config = new HikariConfig();
            config.setDriverClassName("org.sqlite.JDBC");
            config.setJdbcUrl(jdbcUrl);
            config.setMaximumPoolSize(1); // SQLite için tek bağlantı yeterli
            config.setPoolName("SQLite-Pool");

            dataSource = new HikariDataSource(config);
            connectionSource = new JdbcConnectionSource(jdbcUrl);

            return true;

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "SQLite bağlantısı kurulamadı: " + e.getMessage(), e);
        }

        return false;
    }

    /**
     * Tablo oluşturur
     * @param clazz Entity sınıfı
     * @param <T> Entity tipi
     */
    public <T> CompletableFuture<Void> createTable(Class<T> clazz) {
        return CompletableFuture.runAsync(() -> {
            try {
                TableUtils.createTableIfNotExists(connectionSource, clazz);
                plugin.getLogger().info(clazz.getSimpleName() + " tablosu oluşturuldu/kontrol edildi.");
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Tablo oluşturulamadı: " + clazz.getSimpleName(), e);
            }
        });
    }

    /**
     * DAO nesnesini döndürür (cache'li)
     * @param clazz Entity sınıfı
     * @param <T> Entity tipi
     * @param <ID> ID tipi
     * @return Dao nesnesi
     */
    @SuppressWarnings("unchecked")
    public <T, ID> Dao<T, ID> getDao(Class<T> clazz) {
        try {
            if (!daoCache.containsKey(clazz)) {
                Dao<T, ID> dao = DaoManager.createDao(connectionSource, clazz);
                daoCache.put(clazz, dao);
            }
            return (Dao<T, ID>) daoCache.get(clazz);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "DAO oluşturulamadı: " + clazz.getSimpleName(), e);
            return null;
        }
    }

    /**
     * Ham bağlantı döndürür (dikkatli kullanın)
     * @return Connection
     * @throws SQLException
     */
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    /**
     * ConnectionSource döndürür
     * @return ConnectionSource
     */
    public ConnectionSource getConnectionSource() {
        return connectionSource;
    }

    /**
     * Şu anki veritabanı türünü döndürür
     * @return DatabaseType
     */
    public DatabaseType getCurrentDatabaseType() {
        return currentDatabaseType;
    }

    /**
     * Veritabanı bağlantısının aktif olup olmadığını kontrol eder
     * @return boolean
     */
    public boolean isConnected() {
        try {
            if (dataSource != null && !dataSource.isClosed()) {
                try (Connection connection = dataSource.getConnection()) {
                    return connection.isValid(3);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Bağlantı kontrolü başarısız", e);
        }
        return false;
    }

    /**
     * Veritabanı bağlantısını yeniden başlatır
     */
    public CompletableFuture<Boolean> reconnect() {
        return CompletableFuture.supplyAsync(() -> {
            plugin.getLogger().info("Veritabanı yeniden bağlanıyor...");
            close();
            daoCache.clear();

            try {
                Thread.sleep(1000); // 1 saniye bekle
                initializeDatabase();
                return isConnected();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        });
    }

    /**
     * Tüm bağlantıları kapatır
     */
    public void close() {
        try {
            if (connectionSource != null) {
                connectionSource.close();
            }
            if (dataSource != null && !dataSource.isClosed()) {
                dataSource.close();
            }
            daoCache.clear();
            plugin.getLogger().info("Veritabanı bağlantıları kapatıldı.");
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Veritabanı kapatılırken hata oluştu", e);
        }
    }

    /**
     * Veritabanı yapılandırmasını yeniden yükler ve bağlantıyı yeniden başlatır
     * @return Reload işleminin başarılı olup olmadığını döndüren CompletableFuture
     */
    public CompletableFuture<Boolean> reload() {
        return CompletableFuture.supplyAsync(() -> {
            plugin.getLogger().info("Veritabanı yapılandırması yeniden yükleniyor...");

            try {
                // Mevcut bağlantıları kapat
                close();

                // DAO cache'ini temizle
                daoCache.clear();

                // Config'i yeniden yükle
                ConfigManager.getInstance().reloadConfig("database");

                // Kısa bir bekleme süresi
                Thread.sleep(500);

                // Veritabanı bağlantısını yeniden başlat
                initializeDatabase();

                // Bağlantı durumunu kontrol et
                boolean connected = isConnected();

                if (connected) {
                    plugin.getLogger().info("Veritabanı yapılandırması başarıyla yeniden yüklendi!");
                } else {
                    plugin.getLogger().warning("Veritabanı yeniden yüklendi ancak bağlantı kurulamadı!");
                }

                return connected;

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                plugin.getLogger().severe("Veritabanı reload işlemi kesildi!");
                return false;
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Veritabanı reload işlemi başarısız: " + e.getMessage(), e);
                return false;
            }
        });
    }

    /**
     * Veritabanı yapılandırmasını senkron olarak yeniden yükler
     * Ana thread'de çağrılmamalı - async işlemler için reload() metodunu kullanın
     * @return Reload işleminin başarılı olup olmadığı
     */
    public boolean reloadSync() {
        plugin.getLogger().info("Veritabanı yapılandırması senkron olarak yeniden yükleniyor...");

        try {
            // Mevcut bağlantıları kapat
            close();

            // DAO cache'ini temizle
            daoCache.clear();

            // Config'i yeniden yükle
            ConfigManager.getInstance().reloadConfig("database");

            // Veritabanı bağlantısını yeniden başlat
            initializeDatabase();

            // Bağlantı durumunu kontrol et
            boolean connected = isConnected();

            if (connected) {
                plugin.getLogger().info("Veritabanı yapılandırması başarıyla yeniden yüklendi!");
            } else {
                plugin.getLogger().warning("Veritabanı yeniden yüklendi ancak bağlantı kurulamadı!");
            }

            return connected;

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Veritabanı reload işlemi başarısız: " + e.getMessage(), e);
            return false;
        }
    }



}