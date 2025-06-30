package org.ozaii.magEconomy.command;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.ozaii.magEconomy.database.DatabaseManager;
import org.ozaii.magEconomy.economy.models.PlayerEconomy;
import org.ozaii.magEconomy.economy.services.PlayerEconomyService;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class EconomyCommand implements CommandExecutor, TabCompleter {

    private final JavaPlugin plugin;
    private final PlayerEconomyService economyService;

    public EconomyCommand(JavaPlugin plugin) {
        this.plugin = plugin;
        this.economyService = PlayerEconomyService.getInstance();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "info":
                handleInfo(sender);
                break;
            case "admin":
                handleAdmin(sender, args);
                break;
            case "help":
                sendHelp(sender);
                break;
            case "top":
                handleTop(sender, args);
                break;
            case "balance":
            case "bal":
                handleBalance(sender, args);
                break;
            default:
                sender.sendMessage("§cBilinmeyen komut! §e/eco help §cyazarak yardım alabilirsin.");
                break;
        }

        return true;
    }

    /**
     * Admin yetkisi kontrolü yapar
     * @param sender Komut gönderen
     * @return Admin yetkisi var mı?
     */
    private boolean hasAdminPermission(CommandSender sender) {
        return sender.hasPermission("mageconomy.admin") || sender.isOp();
    }

    /**
     * Admin yetkisi kontrol eder ve mesaj gönderir
     * @param sender Komut gönderen
     * @return Admin yetkisi var mı?
     */
    private boolean checkAdminPermission(CommandSender sender) {
        if (!hasAdminPermission(sender)) {
            sender.sendMessage("§cBu komutu kullanma yetkin yok! Admin yetkisi gerekli.");
            return false;
        }
        return true;
    }

    private void handleInfo(CommandSender sender) {
        sender.sendMessage("§6§l=== MagEconomy Bilgileri ===");

        CompletableFuture<Long> totalPlayersFuture = economyService.getTotalPlayers();
        CompletableFuture<Double> totalValueFuture = economyService.getTotalEconomicValue();

        CompletableFuture.allOf(totalPlayersFuture, totalValueFuture).thenRun(() -> {
            try {
                Long totalPlayers = totalPlayersFuture.get();
                Double totalValue = totalValueFuture.get();

                sender.sendMessage("§aToplam Oyuncu Hesabı: §e" + totalPlayers);
                sender.sendMessage("§aToplam Ekonomik Değer: §e" + economyService.format(totalValue));
                sender.sendMessage("§aPara Birimi: §e" + economyService.getCurrencyName() + " / " + economyService.getCurrencyNamePlural());
                sender.sendMessage("§aMinimum Bakiye: §e" + economyService.format(economyService.getMinBalance()));
                sender.sendMessage("§aMaksimum Bakiye: §e" + economyService.format(economyService.getMaxBalance()));
                sender.sendMessage("§aBaşlangıç Bakiyesi: §e" + economyService.format(economyService.getStartingBalance()));
                sender.sendMessage("§aCache Boyutu: §e" + economyService.getCacheSize());
            } catch (Exception e) {
                sender.sendMessage("§cBilgiler alınırken hata oluştu!");
                plugin.getLogger().warning("Economy info komutu hatası: " + e.getMessage());
            }
        });
    }

    private void handleAdmin(CommandSender sender, String[] args) {
        if (!checkAdminPermission(sender)) {
            return;
        }

        if (args.length < 2) {
            sendAdminHelp(sender);
            return;
        }

        String adminCommand = args[1].toLowerCase();

        switch (adminCommand) {
            case "add":
                handleAdminAdd(sender, args);
                break;
            case "remove":
                handleAdminRemove(sender, args);
                break;
            case "set":
                handleAdminSet(sender, args);
                break;
            case "reset":
                handleAdminReset(sender, args);
                break;
            case "see":
                handleAdminSee(sender, args);
                break;
            case "reload":
                handleAdminReload(sender);
                break;
            case "check":
                handleAdminCheck(sender, args);
                break;
            default:
                sendAdminHelp(sender);
                break;
        }
    }

    private void handleAdminAdd(CommandSender sender, String[] args) {
        if (!checkAdminPermission(sender)) return;

        if (args.length < 4) {
            sender.sendMessage("§cKullanım: /eco admin add <oyuncu> <miktar>");
            return;
        }

        String playerName = args[2];
        double amount;

        try {
            amount = Double.parseDouble(args[3]);
            if (amount <= 0) {
                sender.sendMessage("§cMiktar pozitif bir sayı olmalıdır!");
                return;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage("§cGeçersiz miktar!");
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage("§cOyuncu bulunamadı!");
            return;
        }

        economyService.deposit(target, amount).thenAccept(success -> {
            if (success) {
                sender.sendMessage("§a" + target.getName() + " adlı oyuncuya " + economyService.format(amount) + " eklendi!");
                if (target.isOnline()) {
                    target.getPlayer().sendMessage("§aHesabınıza " + economyService.format(amount) + " eklendi!");
                }
            } else {
                sender.sendMessage("§cPara eklenirken hata oluştu!");
            }
        });
    }

    private void handleAdminRemove(CommandSender sender, String[] args) {
        if (!checkAdminPermission(sender)) return;

        if (args.length < 4) {
            sender.sendMessage("§cKullanım: /eco admin remove <oyuncu> <miktar>");
            return;
        }

        String playerName = args[2];
        double amount;

        try {
            amount = Double.parseDouble(args[3]);
            if (amount <= 0) {
                sender.sendMessage("§cMiktar pozitif bir sayı olmalıdır!");
                return;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage("§cGeçersiz miktar!");
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage("§cOyuncu bulunamadı!");
            return;
        }

        economyService.withdraw(target, amount).thenAccept(success -> {
            if (success) {
                sender.sendMessage("§a" + target.getName() + " adlı oyuncudan " + economyService.format(amount) + " çıkarıldı!");
                if (target.isOnline()) {
                    target.getPlayer().sendMessage("§cHesabınızdan " + economyService.format(amount) + " çıkarıldı!");
                }
            } else {
                sender.sendMessage("§cPara çıkarılırken hata oluştu! (Yetersiz bakiye veya hata)");
            }
        });
    }

    private void handleAdminSet(CommandSender sender, String[] args) {
        if (!checkAdminPermission(sender)) return;

        if (args.length < 4) {
            sender.sendMessage("§cKullanım: /eco admin set <oyuncu> <miktar>");
            return;
        }

        String playerName = args[2];
        double amount;

        try {
            amount = Double.parseDouble(args[3]);
            if (amount < 0) {
                sender.sendMessage("§cMiktar negatif olamaz!");
                return;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage("§cGeçersiz miktar!");
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage("§cOyuncu bulunamadı!");
            return;
        }

        economyService.setBalance(target.getUniqueId(), amount).thenAccept(success -> {
            if (success) {
                sender.sendMessage("§a" + target.getName() + " adlı oyuncunun bakiyesi " + economyService.format(amount) + " olarak ayarlandı!");
                if (target.isOnline()) {
                    target.getPlayer().sendMessage("§eBakiyeniz " + economyService.format(amount) + " olarak ayarlandı!");
                }
            } else {
                sender.sendMessage("§cBakiye ayarlanırken hata oluştu!");
            }
        });
    }

    private void handleAdminReset(CommandSender sender, String[] args) {
        if (!checkAdminPermission(sender)) return;

        if (args.length < 3) {
            sender.sendMessage("§cKullanım: /eco admin reset <oyuncu>");
            return;
        }

        String playerName = args[2];
        OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);

        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage("§cOyuncu bulunamadı!");
            return;
        }

        double startingBalance = economyService.getStartingBalance();
        economyService.setBalance(target.getUniqueId(), startingBalance).thenAccept(success -> {
            if (success) {
                sender.sendMessage("§a" + target.getName() + " adlı oyuncunun bakiyesi sıfırlandı! Yeni bakiye: " + economyService.format(startingBalance));
                if (target.isOnline()) {
                    target.getPlayer().sendMessage("§eBakiyeniz sıfırlandı! Yeni bakiye: " + economyService.format(startingBalance));
                }
            } else {
                sender.sendMessage("§cBakiye sıfırlanırken hata oluştu!");
            }
        });
    }

    private void handleAdminSee(CommandSender sender, String[] args) {
        if (!checkAdminPermission(sender)) return;

        if (args.length < 3) {
            sender.sendMessage("§cKullanım: /eco admin see <oyuncu>");
            return;
        }

        String playerName = args[2];
        OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);

        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage("§cOyuncu bulunamadı!");
            return;
        }

        economyService.getBalance(target).thenAccept(balance -> {
            sender.sendMessage("§6" + target.getName() + " §aadlı oyuncunun bakiyesi: §e" + economyService.format(balance));
        });
    }

    /**
     * Admin check komutu - Oyuncunun ekonomi hesap durumunu kontrol eder
     */
    private void handleAdminCheck(CommandSender sender, String[] args) {
        if (!checkAdminPermission(sender)) return;

        if (args.length < 3) {
            sender.sendMessage("§cKullanım: /eco admin check <oyuncu>");
            return;
        }

        String playerName = args[2];
        OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);

        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage("§cOyuncu bulunamadı!");
            return;
        }

        UUID playerUUID = target.getUniqueId();

        sender.sendMessage("§6§l=== " + target.getName() + " Hesap Kontrolü ===");
        sender.sendMessage("§eOyuncu UUID: §f" + playerUUID.toString());
        sender.sendMessage("§eOyuncu Adı: §f" + target.getName());
        sender.sendMessage("§eÇevrimiçi Durumu: " + (target.isOnline() ? "§aÇevrimiçi" : "§cÇevrimdışı"));

        // Hesap varlığını kontrol et
        economyService.hasAccount(target).thenAccept(hasAccount -> {
            sender.sendMessage("§eHesap Durumu: " + (hasAccount ? "§aMevcut" : "§cMevcut değil"));

            if (hasAccount) {
                // Bakiye bilgisi
                economyService.getBalance(target).thenAccept(balance -> {
                    sender.sendMessage("§eMevcut Bakiye: §a" + economyService.format(balance));

                    // Limit kontrolleri
                    double minBalance = economyService.getMinBalance();
                    double maxBalance = economyService.getMaxBalance();

                    sender.sendMessage("§eLimit Durumu:");
                    sender.sendMessage("  §7Minimum: §e" + economyService.format(minBalance) +
                            (balance >= minBalance ? " §a✓" : " §c✗"));
                    sender.sendMessage("  §7Maksimum: §e" + economyService.format(maxBalance) +
                            (balance <= maxBalance ? " §a✓" : " §c✗"));

                    // Son işlem tarihi (eğer mevcut ise)
                    sender.sendMessage("§eSon Güncelleme: §7" + new java.util.Date().toString());
                });
            } else {
                sender.sendMessage("§eÖnerilen Eylem: §7/eco admin reset " + target.getName() + " §ekomutu ile hesap oluşturun");
            }
        }).exceptionally(throwable -> {
            sender.sendMessage("§cHesap kontrolü sırasında hata oluştu: " + throwable.getMessage());
            return null;
        });
    }

    private void handleAdminReload(CommandSender sender) {
        if (!checkAdminPermission(sender)) return;

        sender.sendMessage("§eYeniden yükleme başlatılıyor...");

        DatabaseManager.getInstance().reload().thenAccept(success -> {
            if (success) {
                sender.sendMessage("§aVeritabanı başarıyla yeniden yüklendi!");
            } else {
                sender.sendMessage("§cVeritabanı yeniden yüklenemedi!");
            }
        });

        economyService.reloadSettings();
        sender.sendMessage("§aEconomy ayarları yeniden yüklendi!");
    }

    private void handleTop(CommandSender sender, String[] args) {
        int limit = 10;
        if (args.length > 1) {
            try {
                limit = Integer.parseInt(args[1]);
                if (limit < 1 || limit > 50) {
                    limit = 10;
                }
            } catch (NumberFormatException e) {
                limit = 10;
            }
        }

        int finalLimit = limit;
        economyService.getTopPlayers(limit).thenAccept(topPlayers -> {
            sender.sendMessage("§6§l=== En Zengin " + finalLimit + " Oyuncu ===");

            if (topPlayers.isEmpty()) {
                sender.sendMessage("§cHenüz hiç oyuncu yok!");
                return;
            }

            for (int i = 0; i < topPlayers.size(); i++) {
                PlayerEconomy playerEconomy = topPlayers.get(i);
                String rank = String.valueOf(i + 1);
                String name = playerEconomy.getPlayerName();
                String balance = economyService.format(playerEconomy.getBalance());

                sender.sendMessage("§e" + rank + ". §a" + name + " §7- §e" + balance);
            }
        });
    }

    private void handleBalance(CommandSender sender, String[] args) {
        if (args.length > 1) {
            // Başka oyuncunun bakiyesini görme (yetki gerekli)
            if (!sender.hasPermission("mageconomy.balance.others")) {
                sender.sendMessage("§cBaşka oyuncuların bakiyesini görme yetkin yok!");
                return;
            }

            String playerName = args[1];
            OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);

            if (!target.hasPlayedBefore() && !target.isOnline()) {
                sender.sendMessage("§cOyuncu bulunamadı!");
                return;
            }

            economyService.getBalance(target).thenAccept(balance -> {
                sender.sendMessage("§6" + target.getName() + " §aadlı oyuncunun bakiyesi: §e" + economyService.format(balance));
            });
        } else {
            // Kendi bakiyesini görme
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cBu komutu sadece oyuncular kullanabilir!");
                return;
            }

            Player player = (Player) sender;
            economyService.getBalance(player).thenAccept(balance -> {
                player.sendMessage("§aBakiyeniz: §e" + economyService.format(balance));
            });
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6§l=== MagEconomy Komutları ===");
        sender.sendMessage("§e/eco balance [oyuncu] §7- Bakiye görüntüle");
        sender.sendMessage("§e/eco info §7- Economy bilgilerini görüntüle");
        sender.sendMessage("§e/eco top [sayı] §7- En zengin oyuncuları listele");
        sender.sendMessage("§e/eco help §7- Bu yardımı görüntüle");

        if (hasAdminPermission(sender)) {
            sender.sendMessage("§c§l=== Admin Komutları ===");
            sender.sendMessage("§c/eco admin add <oyuncu> <miktar> §7- Para ekle");
            sender.sendMessage("§c/eco admin remove <oyuncu> <miktar> §7- Para çıkar");
            sender.sendMessage("§c/eco admin set <oyuncu> <miktar> §7- Bakiye ayarla");
            sender.sendMessage("§c/eco admin reset <oyuncu> §7- Bakiyeyi sıfırla");
            sender.sendMessage("§c/eco admin see <oyuncu> §7- Bakiye görüntüle");
            sender.sendMessage("§c/eco admin check <oyuncu> §7- Hesap durumu kontrol et");
            sender.sendMessage("§c/eco admin reload §7- Ayarları yenile");
        }
    }

    private void sendAdminHelp(CommandSender sender) {
        sender.sendMessage("§c§l=== Admin Komutları ===");
        sender.sendMessage("§c/eco admin add <oyuncu> <miktar> §7- Para ekle");
        sender.sendMessage("§c/eco admin remove <oyuncu> <miktar> §7- Para çıkar");
        sender.sendMessage("§c/eco admin set <oyuncu> <miktar> §7- Bakiye ayarla");
        sender.sendMessage("§c/eco admin reset <oyuncu> §7- Bakiyeyi sıfırla");
        sender.sendMessage("§c/eco admin see <oyuncu> §7- Bakiye görüntüle");
        sender.sendMessage("§c/eco admin check <oyuncu> §7- Hesap durumu kontrol et");
        sender.sendMessage("§c/eco admin reload §7- Ayarları yenile");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("info", "balance", "top", "help"));
            if (hasAdminPermission(sender)) {
                completions.add("admin");
            }
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("admin") && hasAdminPermission(sender)) {
                completions.addAll(Arrays.asList("add", "remove", "set", "reset", "see", "check", "reload"));
            } else if (args[0].equalsIgnoreCase("balance") && sender.hasPermission("mageconomy.balance.others")) {
                // Online oyuncuları öner
                Bukkit.getOnlinePlayers().forEach(player -> completions.add(player.getName()));
            } else if (args[0].equalsIgnoreCase("top")) {
                completions.addAll(Arrays.asList("5", "10", "15", "20"));
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("admin") && hasAdminPermission(sender)) {
                String subCmd = args[1].toLowerCase();
                if (subCmd.equals("add") || subCmd.equals("remove") || subCmd.equals("set") ||
                        subCmd.equals("reset") || subCmd.equals("see") || subCmd.equals("check")) {
                    // Online oyuncuları öner
                    Bukkit.getOnlinePlayers().forEach(player -> completions.add(player.getName()));
                }
            }
        } else if (args.length == 4) {
            if (args[0].equalsIgnoreCase("admin") && hasAdminPermission(sender)) {
                String subCmd = args[1].toLowerCase();
                if (subCmd.equals("add") || subCmd.equals("remove") || subCmd.equals("set")) {
                    completions.addAll(Arrays.asList("100", "1000", "10000", "100000"));
                }
            }
        }

        // Girilen metinle başlayanları filtrele
        return completions.stream()
                .filter(completion -> completion.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }
}