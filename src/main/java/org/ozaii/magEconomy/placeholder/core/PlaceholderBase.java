package org.ozaii.magEconomy.placeholder.core;


import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.java.JavaPlugin;
import org.ozaii.magEconomy.economy.services.PlayerEconomyService;

import java.text.DecimalFormat;
import java.util.concurrent.TimeUnit;

/**
 * Tüm placeholder sınıfları için temel abstract sınıf
 */
public abstract class PlaceholderBase implements PlaceholderService {

    protected final JavaPlugin plugin;
    protected final PlayerEconomyService economyService;
    protected final DecimalFormat decimalFormat;
    protected boolean registered = false;

    // Desteklenen parametreler
    protected static final String[] SUPPORTED_PARAMETERS = {
            "balance",
            "balance_formatted",
            "balance_short",
            "currency_name",
            "currency_name_plural",
            "max_balance",
            "min_balance",
            "starting_balance"
    };

    public PlaceholderBase(JavaPlugin plugin) {
        this.plugin = plugin;
        this.economyService = PlayerEconomyService.getInstance();
        this.decimalFormat = new DecimalFormat("#,##0.00");
    }

    @Override
    public String getIdentifier() {
        return "mageconomy";
    }

    @Override
    public String[] getSupportedParameters() {
        return SUPPORTED_PARAMETERS.clone();
    }

    @Override
    public boolean isRegistered() {
        return registered;
    }

    @Override
    public String processPlaceholder(OfflinePlayer player, String parameter) {
        if (parameter == null) {
            return "";
        }

        try {
            switch (parameter.toLowerCase()) {
                case "balance":
                    return player != null ? getBalanceSync(player) : "";

                case "balance_formatted":
                    return player != null ? getBalanceFormattedSync(player) : "";

                case "balance_short":
                    return player != null ? getBalanceShortSync(player) : "";

                case "currency_name":
                    return economyService.getCurrencyName();

                case "currency_name_plural":
                    return economyService.getCurrencyNamePlural();

                case "max_balance":
                    return decimalFormat.format(economyService.getMaxBalance());

                case "min_balance":
                    return decimalFormat.format(economyService.getMinBalance());

                case "starting_balance":
                    return decimalFormat.format(economyService.getStartingBalance());

                default:
                    return "";
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Placeholder işleme hatası (" + parameter + "): " + e.getMessage());
            return getDefaultValue(parameter);
        }
    }

    /**
     * Hata durumunda varsayılan değer döndür
     */
    protected String getDefaultValue(String parameter) {
        switch (parameter.toLowerCase()) {
            case "balance":
            case "balance_formatted":
                return "0.00";
            case "balance_short":
                return "0";
            case "currency_name":
                return "Coin";
            case "currency_name_plural":
                return "Coins";
            case "max_balance":
            case "min_balance":
            case "starting_balance":
                return "0.00";
            default:
                return "";
        }
    }

    /**
     * Oyuncunun bakiyesini senkron olarak al
     */
    protected String getBalanceSync(OfflinePlayer player) {
        try {
            Double balance = economyService.getBalance(player).get(5, TimeUnit.SECONDS);
            return decimalFormat.format(balance);
        } catch (Exception e) {
            plugin.getLogger().warning("Balance sync hatası: " + e.getMessage());
            return "0.00";
        }
    }

    /**
     * Oyuncunun formatlanmış bakiyesini senkron olarak al
     */
    protected String getBalanceFormattedSync(OfflinePlayer player) {
        try {
            Double balance = economyService.getBalance(player).get(5, TimeUnit.SECONDS);
            return economyService.format(balance);
        } catch (Exception e) {
            plugin.getLogger().warning("Balance formatted sync hatası: " + e.getMessage());
            return economyService.format(0.0);
        }
    }

    /**
     * Oyuncunun kısa formatlanmış bakiyesini senkron olarak al
     */
    protected String getBalanceShortSync(OfflinePlayer player) {
        try {
            Double balance = economyService.getBalance(player).get(5, TimeUnit.SECONDS);
            return formatShort(balance);
        } catch (Exception e) {
            plugin.getLogger().warning("Balance short sync hatası: " + e.getMessage());
            return "0";
        }
    }

    /**
     * Sayıyı kısa formatta formatla (K, M, B)
     */
    protected String formatShort(double amount) {
        if (amount >= 1_000_000_000) {
            return String.format("%.1fB", amount / 1_000_000_000);
        } else if (amount >= 1_000_000) {
            return String.format("%.1fM", amount / 1_000_000);
        } else if (amount >= 1_000) {
            return String.format("%.1fK", amount / 1_000);
        } else {
            return decimalFormat.format(amount);
        }
    }
}