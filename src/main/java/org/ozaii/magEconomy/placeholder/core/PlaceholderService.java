package org.ozaii.magEconomy.placeholder.core;

import org.bukkit.OfflinePlayer;

/**
 * Placeholder servisi için temel interface
 */
public interface PlaceholderService {

    /**
     * Placeholder'ı kaydet
     */
    void register();

    /**
     * Placeholder'ı kaldır
     */
    void unregister();

    /**
     * Placeholder'ın kayıtlı olup olmadığını kontrol et
     * @return kayıtlı ise true
     */
    boolean isRegistered();

    /**
     * Placeholder'ın adını döndür
     * @return placeholder adı
     */
    String getIdentifier();

    /**
     * Placeholder'ın desteklediği parametreleri döndür
     * @return desteklenen parametreler
     */
    String[] getSupportedParameters();

    /**
     * Placeholder değerini hesapla
     * @param player oyuncu
     * @param parameter parametre
     * @return hesaplanan değer
     */
    String processPlaceholder(OfflinePlayer player, String parameter);
}