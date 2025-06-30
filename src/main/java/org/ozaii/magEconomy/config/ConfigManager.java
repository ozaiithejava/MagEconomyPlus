package org.ozaii.magEconomy.config;


import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class ConfigManager {

    private static ConfigManager instance;
    private JavaPlugin plugin;
    private Map<String, FileConfiguration> configs;
    private Map<String, File> configFiles;

    private ConfigManager() {
        configs = new HashMap<>();
        configFiles = new HashMap<>();
    }

    /**
     * ConfigManager singleton instance'ını döndürür
     * @return ConfigManager instance
     */
    public static ConfigManager getInstance() {
        if (instance == null) {
            instance = new ConfigManager();
        }
        return instance;
    }

    /**
     * ConfigManager'ı başlatır
     * @param plugin JavaPlugin instance
     */
    public void initialize(JavaPlugin plugin) {
        this.plugin = plugin;


       // createConfig("config");
    }

    /**
     * Yeni bir config dosyası oluşturur veya yükler
     * @param configName Config dosyasının adı (.yml uzantısı olmadan)
     */
    public void createConfig(String configName) {
        if (plugin == null) {
            throw new IllegalStateException("ConfigManager henüz initialize edilmedi!");
        }

        File configFile = new File(plugin.getDataFolder(), configName + ".yml");

        // Dosya yoksa oluştur
        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();

            // Plugin'in resources klasöründen kopyala
            if (plugin.getResource(configName + ".yml") != null) {
                plugin.saveResource(configName + ".yml", false);
            } else {
                try {
                    configFile.createNewFile();
                } catch (IOException e) {
                    plugin.getLogger().log(Level.SEVERE, "Config dosyası oluşturulamadı: " + configName, e);
                    return;
                }
            }
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        configs.put(configName, config);
        configFiles.put(configName, configFile);

        plugin.getLogger().info(configName + ".yml yüklendi.");
    }

    /**
     * Belirtilen config'i döndürür
     * @param configName Config adı
     * @return FileConfiguration
     */
    public FileConfiguration getConfig(String configName) {
        if (!configs.containsKey(configName)) {
            createConfig(configName);
        }
        return configs.get(configName);
    }

    /**
     * Ana config.yml'i döndürür
     * @return FileConfiguration
     */
    public FileConfiguration getConfig() {
        return getConfig("config");
    }

    /**
     * Belirtilen config'i kaydet
     * @param configName Config adı
     */
    public void saveConfig(String configName) {
        if (!configs.containsKey(configName) || !configFiles.containsKey(configName)) {
            plugin.getLogger().warning("Config bulunamadı: " + configName);
            return;
        }

        try {
            configs.get(configName).save(configFiles.get(configName));
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Config kaydedilemedi: " + configName, e);
        }
    }

    /**
     * Ana config.yml'i kaydet
     */
    public void saveConfig() {
        saveConfig("config");
    }

    /**
     * Belirtilen config'i yeniden yükle
     * @param configName Config adı
     */
    public void reloadConfig(String configName) {
        if (!configFiles.containsKey(configName)) {
            plugin.getLogger().warning("Config bulunamadı: " + configName);
            return;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(configFiles.get(configName));
        configs.put(configName, config);
        plugin.getLogger().info(configName + ".yml yeniden yüklendi.");
    }

    /**
     * Ana config.yml'i yeniden yükle
     */
    public void reloadConfig() {
        reloadConfig("config");
    }

    /**
     * Tüm config'leri kaydet
     */
    public void saveAllConfigs() {
        for (String configName : configs.keySet()) {
            saveConfig(configName);
        }
    }

    /**
     * Tüm config'leri yeniden yükle
     */
    public void reloadAllConfigs() {
        for (String configName : configs.keySet()) {
            reloadConfig(configName);
        }
    }

    /**
     * Belirtilen config'in var olup olmadığını kontrol et
     * @param configName Config adı
     * @return boolean
     */
    public boolean hasConfig(String configName) {
        return configs.containsKey(configName);
    }

    /**
     * Yüklü tüm config isimlerini döndür
     * @return String array
     */
    public String[] getLoadedConfigs() {
        return configs.keySet().toArray(new String[0]);
    }
}