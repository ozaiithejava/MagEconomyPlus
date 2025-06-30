package org.ozaii.magEconomy.placeholder.impl;

import io.github.miniplaceholders.api.Expansion;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.Tag;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.java.JavaPlugin;
import org.ozaii.magEconomy.placeholder.core.PlaceholderBase;

public class EconomyMiniPlaceholdersExpansion extends PlaceholderBase {

    private Expansion expansion;

    public EconomyMiniPlaceholdersExpansion(JavaPlugin plugin) {
        super(plugin);
    }

    @Override
    public void register() {
        if (registered) {
            plugin.getLogger().warning("MiniPlaceholders expansion zaten kayıtlı!");
            return;
        }

        try {
            expansion = Expansion.builder("mageconomy")
                    .author(plugin.getDescription().getAuthors().toString())
                    .version(plugin.getDescription().getVersion())

                    // Balance placeholders
                    .audiencePlaceholder("balance", (audience, queue, ctx) -> {
                        if (!(audience instanceof OfflinePlayer)) {
                            return Tag.selfClosingInserting(Component.text(""));
                        }
                        OfflinePlayer player = (OfflinePlayer) audience;
                        String balance = processPlaceholder(player, "balance");
                        return Tag.selfClosingInserting(Component.text(balance));
                    })

                    .audiencePlaceholder("balance_formatted", (audience, queue, ctx) -> {
                        if (!(audience instanceof OfflinePlayer)) {
                            return Tag.selfClosingInserting(Component.text(""));
                        }
                        OfflinePlayer player = (OfflinePlayer) audience;
                        String balance = processPlaceholder(player, "balance_formatted");
                        return Tag.selfClosingInserting(Component.text(balance));
                    })

                    .audiencePlaceholder("balance_short", (audience, queue, ctx) -> {
                        if (!(audience instanceof OfflinePlayer)) {
                            return Tag.selfClosingInserting(Component.text(""));
                        }
                        OfflinePlayer player = (OfflinePlayer) audience;
                        String balance = processPlaceholder(player, "balance_short");
                        return Tag.selfClosingInserting(Component.text(balance));
                    })

                    // Currency info placeholders
                    .globalPlaceholder("currency_name", (queue, ctx) -> {
                        String currencyName = processPlaceholder(null, "currency_name");
                        return Tag.selfClosingInserting(Component.text(currencyName));
                    })

                    .globalPlaceholder("currency_name_plural", (queue, ctx) -> {
                        String currencyNamePlural = processPlaceholder(null, "currency_name_plural");
                        return Tag.selfClosingInserting(Component.text(currencyNamePlural));
                    })

                    // Economy limits placeholders
                    .globalPlaceholder("max_balance", (queue, ctx) -> {
                        String maxBalance = processPlaceholder(null, "max_balance");
                        return Tag.selfClosingInserting(Component.text(maxBalance));
                    })

                    .globalPlaceholder("min_balance", (queue, ctx) -> {
                        String minBalance = processPlaceholder(null, "min_balance");
                        return Tag.selfClosingInserting(Component.text(minBalance));
                    })

                    .globalPlaceholder("starting_balance", (queue, ctx) -> {
                        String startingBalance = processPlaceholder(null, "starting_balance");
                        return Tag.selfClosingInserting(Component.text(startingBalance));
                    })

                    .build();

            expansion.register();
            registered = true;
            plugin.getLogger().info("MiniPlaceholders expansion başarıyla kaydedildi");

        } catch (Exception e) {
            plugin.getLogger().severe("MiniPlaceholders expansion kaydedilemedi: " + e.getMessage());
        }
    }

    @Override
    public void unregister() {
        if (!registered) {
            return;
        }

        try {
            if (expansion != null) {
                expansion.unregister();
            }
            registered = false;
            plugin.getLogger().info("MiniPlaceholders expansion başarıyla kaldırıldı");
        } catch (Exception e) {
            plugin.getLogger().warning("MiniPlaceholders expansion kaldırılırken hata: " + e.getMessage());
        }
    }
}

