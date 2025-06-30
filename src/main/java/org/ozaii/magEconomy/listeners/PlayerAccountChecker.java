package org.ozaii.magEconomy.listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.ozaii.magEconomy.economy.services.PlayerEconomyService;

public class PlayerAccountChecker implements Listener {

    private static PlayerAccountChecker instance;
    private JavaPlugin plugin;

    private PlayerAccountChecker() {
        // private constructor
    }

    public static PlayerAccountChecker getInstance() {
        if (instance == null) {
            instance = new PlayerAccountChecker();
        }
        return instance;
    }

    public void initialize(JavaPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        plugin.getLogger().info("PlayerAccountChecker başarıyla başlatıldı.");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        PlayerEconomyService.getInstance().hasAccount(player).thenAccept(hasAccount -> {
            if (!hasAccount) {
                PlayerEconomyService.getInstance().createAccount(player.getUniqueId(), player.getName());
                plugin.getLogger().info(player.getName() + " için yeni hesap oluşturuldu.");
            }
        });
    }
}
