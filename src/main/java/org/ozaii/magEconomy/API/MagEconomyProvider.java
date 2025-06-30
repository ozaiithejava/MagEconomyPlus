package org.ozaii.magEconomy.API;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * MagEconomy API Provider - Memory leak ve spam logging sorunları düzeltilmiş
 * Thread-safe implementasyon ile güvenli erişim sağlar
 */
public class MagEconomyProvider {

    // Memory leak'i önlemek için ConcurrentHashMap kullanıyoruz
    private static final Set<String> warnedPlugins = ConcurrentHashMap.newKeySet();
    private static final Object LOCK = new Object();

    // Cache için son kontrol zamanları
    private static volatile long lastAvailabilityCheck = 0;
    private static volatile boolean lastAvailabilityResult = false;
    private static final long CACHE_DURATION = 5000; // 5 saniye cache

    /**
     * MagEconomy API'sine erişim sağlar
     * @param plugin API'yi kullanacak eklenti
     * @return MagEconomyAPI instance veya null
     * @throws IllegalArgumentException eğer plugin null ise
     */
    public static MagEconomyAPI getAPI(Plugin plugin) {
        if (plugin == null) {
            throw new IllegalArgumentException("Plugin null olamaz!");
        }

        return getAPI(plugin, null);
    }

    /**
     * MagEconomy API'sine version kontrolü ile erişim sağlar
     * @param plugin API'yi kullanacak eklenti
     * @param requiredVersion Gerekli minimum versiyon (null ise kontrol yapılmaz)
     * @return MagEconomyAPI instance veya null
     * @throws IllegalArgumentException eğer plugin null ise
     */
    public static MagEconomyAPI getAPI(Plugin plugin, String requiredVersion) {
        if (plugin == null) {
            throw new IllegalArgumentException("Plugin null olamaz!");
        }

        synchronized (LOCK) {
            Plugin magEconomy = Bukkit.getPluginManager().getPlugin("MagEconomy");
            Logger logger = plugin.getLogger();
            String pluginName = plugin.getName();

            // MagEconomy yüklü mü kontrol et
            if (magEconomy == null || !magEconomy.isEnabled()) {
                // İlk kez uyarı ver (spam önleme)
                if (warnedPlugins.add(pluginName)) {
                    logger.warning("MagEconomy eklentisi bulunamadı veya aktif değil!");
                    logger.info("MagEconomy eklentisini indirip server'a yükleyiniz.");
                }
                return null;
            }

            // Version kontrolü
            if (requiredVersion != null && !isVersionCompatible(magEconomy, requiredVersion)) {
                String currentVersion = magEconomy.getDescription().getVersion();
                if (warnedPlugins.add(pluginName + "_version")) {
                    logger.warning(String.format(
                            "MagEconomy versiyonu uyumlu değil! Gerekli: %s, Mevcut: %s",
                            requiredVersion, currentVersion
                    ));
                }
                return null;
            }

            // API hazır mı kontrol et
            MagEconomyAPI api = MagEconomyAPI.getInstance();
            if (!api.isReady()) {
                if (warnedPlugins.add(pluginName + "_not_ready")) {
                    logger.warning("MagEconomy API henüz hazır değil! Lütfen server'ın tamamen başlamasını bekleyiniz.");
                }
                return null;
            }

            // Başarılı erişim log'u (sadece ilk kez)
            if (warnedPlugins.add(pluginName + "_success")) {
                logger.info("MagEconomy API'sine başarıyla bağlanıldı!");
            }

            return api;
        }
    }

    /**
     * MagEconomy'nin yüklü ve hazır olup olmadığını kontrol eder
     * Cache mekanizması ile performans optimizasyonu sağlar
     * @return boolean
     */
    public static boolean isAvailable() {
        long currentTime = System.currentTimeMillis();

        // Cache kontrolü
        if (currentTime - lastAvailabilityCheck < CACHE_DURATION) {
            return lastAvailabilityResult;
        }

        synchronized (LOCK) {
            // Double-checked locking
            if (currentTime - lastAvailabilityCheck < CACHE_DURATION) {
                return lastAvailabilityResult;
            }

            Plugin magEconomy = Bukkit.getPluginManager().getPlugin("MagEconomy");
            boolean available = magEconomy != null &&
                    magEconomy.isEnabled() &&
                    MagEconomyAPI.getInstance().isReady();

            lastAvailabilityCheck = currentTime;
            lastAvailabilityResult = available;

            return available;
        }
    }

    /**
     * Belirtilen plugin için uyarı cache'ini temizler
     * Plugin yeniden yüklendiğinde çağrılabilir
     * @param plugin Temizlenecek plugin
     */
    public static void clearWarningCache(Plugin plugin) {
        if (plugin == null) return;

        String pluginName = plugin.getName();
        warnedPlugins.remove(pluginName);
        warnedPlugins.remove(pluginName + "_version");
        warnedPlugins.remove(pluginName + "_not_ready");
        warnedPlugins.remove(pluginName + "_success");
    }

    /**
     * Tüm uyarı cache'ini temizler
     * Maintenance veya test amaçlı kullanılabilir
     */
    public static void clearAllWarningCache() {
        warnedPlugins.clear();
        lastAvailabilityCheck = 0;
        lastAvailabilityResult = false;
    }

    /**
     * Mevcut cache boyutunu döndürür (debug amaçlı)
     * @return cache boyutu
     */
    public static int getCacheSize() {
        return warnedPlugins.size();
    }

    /**
     * Version uyumluluğunu kontrol eder
     * Semantic versioning desteği (x.y.z formatı)
     * @param magEconomyPlugin MagEconomy plugin instance
     * @param requiredVersion Gerekli minimum versiyon
     * @return boolean uyumluluk durumu
     */
    private static boolean isVersionCompatible(Plugin magEconomyPlugin, String requiredVersion) {
        if (requiredVersion == null || requiredVersion.trim().isEmpty()) {
            return true;
        }

        try {
            String currentVersion = magEconomyPlugin.getDescription().getVersion();
            return compareVersions(currentVersion, requiredVersion) >= 0;
        } catch (Exception e) {
            // Version parse hatası durumunda false döndür
            return false;
        }
    }

    /**
     * İki versiyonu karşılaştırır (semantic versioning)
     * @param version1 Mevcut versiyon
     * @param version2 Karşılaştırılacak versiyon
     * @return int (-1: version1 < version2, 0: eşit, 1: version1 > version2)
     */
    private static int compareVersions(String version1, String version2) {
        if (version1 == null || version2 == null) {
            return 0;
        }

        // Sadece sayısal kısmı al (ör: "1.2.3-SNAPSHOT" -> "1.2.3")
        version1 = version1.split("-")[0];
        version2 = version2.split("-")[0];

        String[] parts1 = version1.split("\\.");
        String[] parts2 = version2.split("\\.");

        int maxLength = Math.max(parts1.length, parts2.length);

        for (int i = 0; i < maxLength; i++) {
            int v1 = i < parts1.length ? parseVersionPart(parts1[i]) : 0;
            int v2 = i < parts2.length ? parseVersionPart(parts2[i]) : 0;

            if (v1 < v2) return -1;
            if (v1 > v2) return 1;
        }

        return 0;
    }

    /**
     * Version parçasını integer'a çevirir
     * @param part Version parçası
     * @return int değer
     */
    private static int parseVersionPart(String part) {
        try {
            return Integer.parseInt(part);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Provider istatistiklerini string olarak döndürür (debug amaçlı)
     * @return String istatistikler
     */
    public static String getProviderStats() {
        return String.format(
                "MagEconomyProvider Stats - Cached Plugins: %d, Last Check: %d ms ago, Available: %s",
                warnedPlugins.size(),
                System.currentTimeMillis() - lastAvailabilityCheck,
                lastAvailabilityResult
        );
    }
}