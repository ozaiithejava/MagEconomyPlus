package org.ozaii.magEconomy.placeholder.core;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.ozaii.magEconomy.placeholder.core.PlaceholderService;
import org.ozaii.magEconomy.placeholder.impl.EconomyMiniPlaceholdersExpansion;
import org.ozaii.magEconomy.placeholder.impl.EconomyPlaceholderApiExpansion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Placeholder'ları yöneten registry sınıfı
 */
public class PlaceholderRegistry {

    private static PlaceholderRegistry instance;
    private final JavaPlugin plugin;
    private final Map<String, PlaceholderService> registeredPlaceholders;
    private final List<String> availableExpansions;

    private PlaceholderRegistry(JavaPlugin plugin) {
        this.plugin = plugin;
        this.registeredPlaceholders = new HashMap<>();
        this.availableExpansions = new ArrayList<>();
        detectAvailableExpansions();
    }

    /**
     * Registry'yi initialize et
     */
    public static void initialize(JavaPlugin plugin) {
        if (instance != null) {
            plugin.getLogger().warning("PlaceholderRegistry zaten initialize edilmiş!");
            return;
        }
        instance = new PlaceholderRegistry(plugin);
        plugin.getLogger().info("PlaceholderRegistry başarıyla initialize edildi");
    }

    /**
     * Registry instance'ını al
     */
    public static PlaceholderRegistry getInstance(JavaPlugin plugin) {
        if (instance == null) {
            initialize(plugin);
        }
        return instance;
    }

    /**
     * Registry instance'ını al (plugin parametresi olmadan)
     */
    public static PlaceholderRegistry getInstance() {
        if (instance == null) {
            throw new IllegalStateException("PlaceholderRegistry henüz initialize edilmedi!");
        }
        return instance;
    }

    /**
     * Mevcut placeholder expansion'larını tespit et
     */
    private void detectAvailableExpansions() {
        availableExpansions.clear();

        // PlaceholderAPI kontrolü
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            availableExpansions.add("PlaceholderAPI");
            plugin.getLogger().info("PlaceholderAPI tespit edildi");
        }

        // MiniPlaceholders kontrolü
        try {
            Class.forName("io.github.miniplaceholders.api.Expansion");
            availableExpansions.add("MiniPlaceholders");
            plugin.getLogger().info("MiniPlaceholders tespit edildi");
        } catch (ClassNotFoundException e) {
            plugin.getLogger().info("MiniPlaceholders bulunamadı");
        }

        if (availableExpansions.isEmpty()) {
            plugin.getLogger().warning("Hiçbir placeholder expansion bulunamadı!");
        }
    }

    /**
     * Tüm mevcut placeholder'ları kaydet
     */
    public void registerAll() {
        plugin.getLogger().info("Placeholder'lar kaydediliyor...");

        int registeredCount = 0;

        // PlaceholderAPI kaydı
        if (availableExpansions.contains("PlaceholderAPI")) {
            try {
                EconomyPlaceholderApiExpansion placeholderAPI = new EconomyPlaceholderApiExpansion(plugin);
                placeholderAPI.register();
                registeredPlaceholders.put("PlaceholderAPI", placeholderAPI);
                registeredCount++;
                plugin.getLogger().info("✓ PlaceholderAPI expansion kaydedildi");
            } catch (Exception e) {
                plugin.getLogger().severe("✗ PlaceholderAPI expansion kaydedilemedi: " + e.getMessage());
            }
        }

        // MiniPlaceholders kaydı
        if (availableExpansions.contains("MiniPlaceholders")) {
            try {
                EconomyMiniPlaceholdersExpansion miniPlaceholders = new EconomyMiniPlaceholdersExpansion(plugin);
                miniPlaceholders.register();
                registeredPlaceholders.put("MiniPlaceholders", miniPlaceholders);
                registeredCount++;
                plugin.getLogger().info("✓ MiniPlaceholders expansion kaydedildi");
            } catch (Exception e) {
                plugin.getLogger().severe("✗ MiniPlaceholders expansion kaydedilemedi: " + e.getMessage());
            }
        }

        plugin.getLogger().info(registeredCount + " placeholder expansion kaydedildi");
    }

    /**
     * Tüm placeholder'ları kaldır
     */
    public void unregisterAll() {
        plugin.getLogger().info("Placeholder'lar kaldırılıyor...");

        int unregisteredCount = 0;

        for (Map.Entry<String, PlaceholderService> entry : registeredPlaceholders.entrySet()) {
            try {
                entry.getValue().unregister();
                unregisteredCount++;
                plugin.getLogger().info("✓ " + entry.getKey() + " expansion kaldırıldı");
            } catch (Exception e) {
                plugin.getLogger().warning("✗ " + entry.getKey() + " expansion kaldırılamadı: " + e.getMessage());
            }
        }

        registeredPlaceholders.clear();
        plugin.getLogger().info(unregisteredCount + " placeholder expansion kaldırıldı");
    }

    /**
     * Belirli bir placeholder'ı kaydet
     */
    public boolean register(String expansionName) {
        if (registeredPlaceholders.containsKey(expansionName)) {
            plugin.getLogger().warning(expansionName + " zaten kayıtlı!");
            return false;
        }

        if (!availableExpansions.contains(expansionName)) {
            plugin.getLogger().warning(expansionName + " mevcut değil!");
            return false;
        }

        try {
            PlaceholderService service = null;

            switch (expansionName) {
                case "PlaceholderAPI":
                    service = new EconomyPlaceholderApiExpansion(plugin);
                    break;
                case "MiniPlaceholders":
                    service = new EconomyMiniPlaceholdersExpansion(plugin);
                    break;
            }

            if (service != null) {
                service.register();
                registeredPlaceholders.put(expansionName, service);
                plugin.getLogger().info("✓ " + expansionName + " kaydedildi");
                return true;
            }
        } catch (Exception e) {
            plugin.getLogger().severe("✗ " + expansionName + " kaydedilemedi: " + e.getMessage());
        }

        return false;
    }

    /**
     * Belirli bir placeholder'ı kaldır
     */
    public boolean unregister(String expansionName) {
        PlaceholderService service = registeredPlaceholders.get(expansionName);
        if (service == null) {
            plugin.getLogger().warning(expansionName + " kayıtlı değil!");
            return false;
        }

        try {
            service.unregister();
            registeredPlaceholders.remove(expansionName);
            plugin.getLogger().info("✓ " + expansionName + " kaldırıldı");
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("✗ " + expansionName + " kaldırılamadı: " + e.getMessage());
            return false;
        }
    }

    /**
     * Kayıtlı placeholder'ları al
     */
    public Map<String, PlaceholderService> getRegisteredPlaceholders() {
        return new HashMap<>(registeredPlaceholders);
    }

    /**
     * Mevcut expansion'ları al
     */
    public List<String> getAvailableExpansions() {
        return new ArrayList<>(availableExpansions);
    }

    /**
     * Belirli bir expansion'ın kayıtlı olup olmadığını kontrol et
     */
    public boolean isRegistered(String expansionName) {
        return registeredPlaceholders.containsKey(expansionName);
    }

    /**
     * Belirli bir expansion'ın mevcut olup olmadığını kontrol et
     */
    public boolean isAvailable(String expansionName) {
        return availableExpansions.contains(expansionName);
    }

    /**
     * Registry durumunu yazdır
     */
    public void printStatus() {
        plugin.getLogger().info("=== Placeholder Registry Durumu ===");
        plugin.getLogger().info("Mevcut expansions: " + availableExpansions);
        plugin.getLogger().info("Kayıtlı expansions: " + registeredPlaceholders.keySet());

        for (Map.Entry<String, PlaceholderService> entry : registeredPlaceholders.entrySet()) {
            String status = entry.getValue().isRegistered() ? "✓ Aktif" : "✗ Pasif";
            plugin.getLogger().info("  " + entry.getKey() + ": " + status);
        }
    }

    /**
     * Registry'yi temizle (plugin disable edilirken)
     */
    public void shutdown() {
        plugin.getLogger().info("PlaceholderRegistry kapatılıyor...");
        unregisterAll();
        instance = null;
        plugin.getLogger().info("PlaceholderRegistry başarıyla kapatıldı");
    }

    /**
     * Registry'nin initialize edilip edilmediğini kontrol et
     */
    public static boolean isInitialized() {
        return instance != null;
    }
}