package org.ozaii.magEconomy.placeholder.impl;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.java.JavaPlugin;
import org.ozaii.magEconomy.placeholder.core.PlaceholderBase;

public class EconomyPlaceholderApiExpansion extends PlaceholderBase {

    private PlaceholderApiWrapper wrapper;

    public EconomyPlaceholderApiExpansion(JavaPlugin plugin) {
        super(plugin);
        this.wrapper = new PlaceholderApiWrapper();
    }

    @Override
    public void register() {
        if (registered) {
            plugin.getLogger().warning("PlaceholderAPI expansion zaten kayıtlı!");
            return;
        }

        try {
            wrapper.register();
            registered = true;
            plugin.getLogger().info("PlaceholderAPI expansion başarıyla kaydedildi");
        } catch (Exception e) {
            plugin.getLogger().severe("PlaceholderAPI expansion kaydedilemedi: " + e.getMessage());
        }
    }

    @Override
    public void unregister() {
        if (!registered) {
            return;
        }

        try {
            if (wrapper != null) {
                wrapper.unregister();
            }
            registered = false;
            plugin.getLogger().info("PlaceholderAPI expansion başarıyla kaldırıldı");
        } catch (Exception e) {
            plugin.getLogger().warning("PlaceholderAPI expansion kaldırılırken hata: " + e.getMessage());
        }
    }

    /**
     * PlaceholderAPI için wrapper sınıf
     */
    private class PlaceholderApiWrapper extends PlaceholderExpansion {

        @Override
        public String getIdentifier() {
            return EconomyPlaceholderApiExpansion.this.getIdentifier();
        }

        @Override
        public String getAuthor() {
            return plugin.getDescription().getAuthors().toString();
        }

        @Override
        public String getVersion() {
            return plugin.getDescription().getVersion();
        }

        @Override
        public boolean persist() {
            return true;
        }

        @Override
        public boolean canRegister() {
            return true;
        }

        @Override
        public String onRequest(OfflinePlayer player, String params) {
            return EconomyPlaceholderApiExpansion.this.processPlaceholder(player, params);
        }
    }
}
