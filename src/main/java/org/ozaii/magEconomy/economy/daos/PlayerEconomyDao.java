package org.ozaii.magEconomy.economy.daos;


import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.UpdateBuilder;
import org.bukkit.plugin.java.JavaPlugin;
import org.ozaii.magEconomy.database.DatabaseManager;
import org.ozaii.magEconomy.economy.models.PlayerEconomy;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class PlayerEconomyDao {

    private final JavaPlugin plugin;
    private final Dao<PlayerEconomy, String> dao;
    private final DatabaseManager databaseManager;

    public PlayerEconomyDao(JavaPlugin plugin) {
        this.plugin = plugin;
        this.databaseManager = DatabaseManager.getInstance();
        this.dao = databaseManager.getDao(PlayerEconomy.class);
    }

    /**
     * Oyuncu hesabını oluşturur
     * @param playerEconomy PlayerEconomy nesnesi
     * @return CompletableFuture<Boolean>
     */
    public CompletableFuture<Boolean> createAccount(PlayerEconomy playerEconomy) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                dao.create(playerEconomy);
                return true;
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Oyuncu hesabı oluşturulamadı: " + playerEconomy.getPlayerUUID(), e);
                return false;
            }
        });
    }

    /**
     * UUID ile oyuncu bilgilerini getirir
     * @param playerUUID Oyuncunun UUID'si
     * @return CompletableFuture<PlayerEconomy>
     */
    public CompletableFuture<PlayerEconomy> getByUUID(UUID playerUUID) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return dao.queryForId(playerUUID.toString());
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Oyuncu verileri getirilemedi: " + playerUUID, e);
                return null;
            }
        });
    }

    /**
     * Oyuncu adı ile oyuncu bilgilerini getirir
     * @param playerName Oyuncunun adı
     * @return CompletableFuture<PlayerEconomy>
     */
    public CompletableFuture<PlayerEconomy> getByName(String playerName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                QueryBuilder<PlayerEconomy, String> queryBuilder = dao.queryBuilder();
                queryBuilder.where().eq("player_name", playerName);
                return queryBuilder.queryForFirst();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Oyuncu verileri getirilemedi: " + playerName, e);
                return null;
            }
        });
    }

    /**
     * Oyuncu hesabının var olup olmadığını kontrol eder
     * @param playerUUID Oyuncunun UUID'si
     * @return CompletableFuture<Boolean>
     */
    public CompletableFuture<Boolean> exists(UUID playerUUID) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return dao.idExists(playerUUID.toString());
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Oyuncu varlık kontrolü başarısız: " + playerUUID, e);
                return false;
            }
        });
    }

    /**
     * Oyuncu bilgilerini günceller
     * @param playerEconomy Güncellenecek PlayerEconomy nesnesi
     * @return CompletableFuture<Boolean>
     */
    public CompletableFuture<Boolean> update(PlayerEconomy playerEconomy) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                playerEconomy.setUpdatedAt(System.currentTimeMillis());
                dao.update(playerEconomy);
                return true;
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Oyuncu verileri güncellenemedi: " + playerEconomy.getPlayerUUID(), e);
                return false;
            }
        });
    }

    /**
     * Oyuncu bakiyesini günceller (performans için optimize edilmiş)
     * @param playerUUID Oyuncunun UUID'si
     * @param newBalance Yeni bakiye
     * @return CompletableFuture<Boolean>
     */
    public CompletableFuture<Boolean> updateBalance(UUID playerUUID, double newBalance) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                UpdateBuilder<PlayerEconomy, String> updateBuilder = dao.updateBuilder();
                updateBuilder.where().eq("player_uuid", playerUUID.toString());
                updateBuilder.updateColumnValue("balance", newBalance);
                updateBuilder.updateColumnValue("updated_at", System.currentTimeMillis());

                int result = updateBuilder.update();
                return result > 0;
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Oyuncu bakiyesi güncellenemedi: " + playerUUID, e);
                return false;
            }
        });
    }

    /**
     * Oyuncu hesabını siler
     * @param playerUUID Oyuncunun UUID'si
     * @return CompletableFuture<Boolean>
     */
    public CompletableFuture<Boolean> delete(UUID playerUUID) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                int result = dao.deleteById(playerUUID.toString());
                return result > 0;
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Oyuncu hesabı silinemedi: " + playerUUID, e);
                return false;
            }
        });
    }

    /**
     * En zengin oyuncuları getirir
     * @param limit Limit (varsayılan 10)
     * @return CompletableFuture<List<PlayerEconomy>>
     */
    public CompletableFuture<List<PlayerEconomy>> getTopPlayers(int limit) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                QueryBuilder<PlayerEconomy, String> queryBuilder = dao.queryBuilder();
                queryBuilder.orderBy("balance", false); // DESC
                queryBuilder.limit((long) limit);
                return queryBuilder.query();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Top oyuncular getirilemedi", e);
                return List.of();
            }
        });
    }

    /**
     * Toplam oyuncu sayısını döndürür
     * @return CompletableFuture<Long>
     */
    public CompletableFuture<Long> getTotalPlayers() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return dao.countOf();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Toplam oyuncu sayısı alınamadı", e);
                return 0L;
            }
        });
    }

    /**
     * Belirtilen bakiye aralığındaki oyuncuları getirir
     * @param minBalance Minimum bakiye
     * @param maxBalance Maximum bakiye
     * @return CompletableFuture<List<PlayerEconomy>>
     */
    public CompletableFuture<List<PlayerEconomy>> getPlayersByBalanceRange(double minBalance, double maxBalance) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                QueryBuilder<PlayerEconomy, String> queryBuilder = dao.queryBuilder();
                queryBuilder.where()
                        .ge("balance", minBalance)
                        .and()
                        .le("balance", maxBalance);
                return queryBuilder.query();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Bakiye aralığındaki oyuncular getirilemedi", e);
                return List.of();
            }
        });
    }

    /**
     * Toplam ekonomik değeri hesaplar
     * @return CompletableFuture<Double>
     */
    public CompletableFuture<Double> getTotalEconomicValue() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                QueryBuilder<PlayerEconomy, String> queryBuilder = dao.queryBuilder();
                queryBuilder.selectRaw("SUM(balance)");

                List<PlayerEconomy> result = queryBuilder.query();
                if (!result.isEmpty()) {
                    // Bu örnekte basit bir toplama işlemi yapıyoruz
                    // Gerçek uygulamada SQL SUM fonksiyonunu kullanabilirsiniz
                    return dao.queryForAll().stream()
                            .mapToDouble(PlayerEconomy::getBalance)
                            .sum();
                }
                return 0.0;
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Toplam ekonomik değer hesaplanamadı", e);
                return 0.0;
            }
        });
    }

    /**
     * Tüm oyuncuları getirir (dikkatli kullanın!)
     * @return CompletableFuture<List<PlayerEconomy>>
     */
    public CompletableFuture<List<PlayerEconomy>> getAllPlayers() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return dao.queryForAll();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Tüm oyuncular getirilemedi", e);
                return List.of();
            }
        });
    }

    /**
     * Belirtilen süre öncesinden aktif olmayan oyuncuları getirir
     * @param inactiveTimeMillis Aktif olmama süresi (milisaniye)
     * @return CompletableFuture<List<PlayerEconomy>>
     */
    public CompletableFuture<List<PlayerEconomy>> getInactivePlayers(long inactiveTimeMillis) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                long cutoffTime = System.currentTimeMillis() - inactiveTimeMillis;
                QueryBuilder<PlayerEconomy, String> queryBuilder = dao.queryBuilder();
                queryBuilder.where().lt("updated_at", cutoffTime);
                return queryBuilder.query();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Aktif olmayan oyuncular getirilemedi", e);
                return List.of();
            }
        });
    }
}