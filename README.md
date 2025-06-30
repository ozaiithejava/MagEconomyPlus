# MagEconomy Plugin - Detaylı Wiki ve Tanıtım

## 📋 İçindekiler
- [Genel Bakış](#genel-bakış)
- [Özellikler](#özellikler)
- [Gereksinimler](#gereksinimler)
- [Kurulum](#kurulum)
- [API Kullanımı](#api-kullanımı)
- [Yapılandırma](#yapılandırma)
- [Event Sistemi](#event-sistemi)
- [Cache Sistemi](#cache-sistemi)
- [Veritabanı](#veritabanı)
- [PlaceholderAPI Desteği](#placeholderapi-desteği)
- [Güvenlik ve Performans](#güvenlik-ve-performans)
- [Troubleshooting](#troubleshooting)
- [SSS](#sss)

---

## 🎯 Genel Bakış

**MagEconomy**, Minecraft Bukkit/Spigot sunucuları için geliştirilmiş modern, güvenli ve performanslı bir ekonomi eklentisidir. Thread-safe tasarımı, kapsamlı API desteği ve Vault entegrasyonu ile sunucunuza güçlü bir ekonomi sistemi sağlar.

### ⭐ Neden MagEconomy?

- **🔒 Thread-Safe Tasarım**: Çok işlemcili ortamlarda güvenli çalışma
- **⚡ Yüksek Performans**: Optimize edilmiş cache sistemi ve asenkron işlemler
- **🛡️ Güvenlik Odaklı**: Kapsamlı validasyon ve hata yönetimi
- **🔧 Kolay Entegrasyon**: Vault uyumluluğu ve zengin API
- **📊 Gelişmiş İstatistikler**: Detaylı ekonomi analizi ve raporlama

---

## 🚀 Özellikler

### 💰 Temel Ekonomi İşlemleri
- **Hesap Yönetimi**: Otomatik hesap oluşturma ve doğrulama
- **Bakiye İşlemleri**: Para yatırma, çekme, kontrol etme
- **Transfer Sistemi**: Oyuncular arası güvenli para transferi
- **Bakiye Limitleri**: Minimum/maksimum bakiye kontrolü

### 🔧 Gelişmiş Özellikler
- **Cache Sistemi**: Yapılandırılabilir cache ile hızlı erişim
- **Asenkron İşlemler**: Veritabanı işlemleri için CompletableFuture
- **Event Sistemi**: Para işlemleri için özel event'ler
- **Version Kontrolü**: API uyumluluk kontrolü
- **Memory Leak Koruması**: Akıllı cache yönetimi

### 📈 İstatistik ve Analiz
- **Top Players**: En zengin oyuncu listesi
- **Ekonomi İstatistikleri**: Toplam değer, oyuncu sayısı
- **Bakiye Aralığı**: Belirli bakiye aralığındaki oyuncular
- **Debug Araçları**: Detaylı sistem durumu raporları

---

## 📋 Gereksinimler

### Zorunlu
- **Minecraft Server**: Bukkit/Spigot 1.16+
- **Java**: JDK 11 veya üzeri
- **Vault**: Economy API desteği için

### Opsiyonel
- **PlaceholderAPI**: Placeholder desteği için
- **MySQL/MariaDB**: Gelişmiş veritabanı desteği için

---

## 🛠️ Kurulum

### 1. Eklenti Kurulumu
```bash
# 1. MagEconomy.jar dosyasını plugins klasörüne kopyalayın
# 2. Sunucuyu yeniden başlatın
# 3. Yapılandırma dosyaları otomatik oluşturulacaktır
```

### 2. Vault Kurulumu
```bash
# Vault eklentisini indirin ve plugins klasörüne yerleştirin
# Sunucuyu yeniden başlatın
# MagEconomy otomatik olarak Vault ile entegre olacaktır
```

### 3. İlk Yapılandırma
```yaml
# plugins/MagEconomy/economy.yml
settings:
  starting-balance: 1000.0
  max-balance: 1000000000.0
  min-balance: 0.0
  fractional-digits: 2

currency:
  singular: "MagCoin"
  plural: "MagCoins"

cache:
  enabled: true
  expire-time-minutes: 30
```

---

## 🔧 API Kullanımı

### Temel Kullanım

```java
// Plugin'inizin onEnable metodunda
@Override
public void onEnable() {
    // MagEconomy API'sine erişim
    MagEconomyAPI api = MagEconomyProvider.getAPI(this);
    if (api == null) {
        getLogger().warning("MagEconomy bulunamadı!");
        return;
    }
    
    // API hazır mı kontrol et
    if (!api.isReady()) {
        getLogger().warning("MagEconomy API henüz hazır değil!");
        return;
    }
    
    getLogger().info("MagEconomy API başarıyla yüklendi!");
}
```

### Version Kontrolü

```java
// Belirli bir versiyon gerektiren kullanım
MagEconomyAPI api = MagEconomyProvider.getAPI(this, "1.0.0");
if (api == null) {
    getLogger().warning("MagEconomy 1.0.0 veya üzeri gerekli!");
    return;
}
```

### Bakiye İşlemleri

```java
public class EconomyExample {
    private MagEconomyAPI api;
    
    public void economyOperations(Player player) {
        UUID playerUUID = player.getUniqueId();
        
        // Hesap var mı kontrol et
        api.hasAccount(playerUUID).thenAccept(hasAccount -> {
            if (!hasAccount) {
                // Hesap oluştur
                api.createAccount(playerUUID, player.getName());
            }
        });
        
        // Bakiye kontrol et
        api.getBalance(playerUUID).thenAccept(balance -> {
            player.sendMessage("Bakiyeniz: " + api.format(balance));
        });
        
        // Para yatır
        api.deposit(playerUUID, 100.0).thenAccept(success -> {
            if (success) {
                player.sendMessage("100 " + api.getCurrencyName() + " yatırıldı!");
            } else {
                player.sendMessage("Para yatırma başarısız!");
            }
        });
        
        // Para çek
        api.withdraw(playerUUID, 50.0).thenAccept(success -> {
            if (success) {
                player.sendMessage("50 " + api.getCurrencyName() + " çekildi!");
            } else {
                player.sendMessage("Yetersiz bakiye!");
            }
        });
        
        // Yeterli para var mı kontrol et
        api.has(playerUUID, 200.0).thenAccept(hasMoney -> {
            if (hasMoney) {
                player.sendMessage("200 " + api.getCurrencyName() + " için yeterli bakiye var!");
            } else {
                player.sendMessage("Yetersiz bakiye!");
            }
        });
    }
}
```

### Transfer İşlemleri

```java
public void transferMoney(UUID fromPlayer, UUID toPlayer, double amount) {
    api.transfer(fromPlayer, toPlayer, amount).thenAccept(success -> {
        if (success) {
            // Transfer başarılı
            Player from = Bukkit.getPlayer(fromPlayer);
            Player to = Bukkit.getPlayer(toPlayer);
            
            if (from != null) {
                from.sendMessage("Transfer başarılı! " + api.format(amount) + " gönderildi.");
            }
            if (to != null) {
                to.sendMessage("Transfer alındı! " + api.format(amount) + " aldınız.");
            }
        } else {
            // Transfer başarısız
            Player from = Bukkit.getPlayer(fromPlayer);
            if (from != null) {
                from.sendMessage("Transfer başarısız! Yetersiz bakiye veya hata.");
            }
        }
    });
}
```

### İstatistik ve Analiz

```java
public void showStatistics(CommandSender sender) {
    // En zengin oyuncular
    api.getTopPlayers(10).thenAccept(topPlayers -> {
        sender.sendMessage("=== En Zengin 10 Oyuncu ===");
        for (int i = 0; i < topPlayers.size(); i++) {
            PlayerEconomy player = topPlayers.get(i);
            sender.sendMessage((i + 1) + ". " + player.getPlayerName() + 
                             ": " + api.format(player.getBalance()));
        }
    });
    
    // Toplam istatistikler
    api.getTotalPlayers().thenAccept(totalPlayers -> {
        sender.sendMessage("Toplam Oyuncu: " + totalPlayers);
    });
    
    api.getTotalEconomicValue().thenAccept(totalValue -> {
        sender.sendMessage("Toplam Ekonomik Değer: " + api.format(totalValue));
    });
    
    // Belirli bakiye aralığındaki oyuncular
    api.getPlayersByBalanceRange(1000.0, 10000.0).thenAccept(players -> {
        sender.sendMessage("1000-10000 arası bakiyeli oyuncu sayısı: " + players.size());
    });
}
```

---

## ⚙️ Yapılandırma

### Economy Ayarları (`economy.yml`)

```yaml
# Temel ekonomi ayarları
settings:
  starting-balance: 1000.0      # Başlangıç bakiyesi
  max-balance: 1000000000.0     # Maksimum bakiye
  min-balance: 0.0              # Minimum bakiye
  fractional-digits: 2          # Ondalık basamak sayısı

# Para birimi ayarları
currency:
  singular: "MagCoin"           # Tekil isim
  plural: "MagCoins"            # Çoğul isim

# Cache ayarları
cache:
  enabled: true                 # Cache aktif/pasif
  expire-time-minutes: 30       # Cache süresi (dakika)

# Geliştirici bilgisi
"####### author": "ozaii1337"
```

### Veritabanı Ayarları (`config.yml`)

```yaml
# SQLite (Varsayılan)
database:
  type: "sqlite"
  filename: "economy.db"

# MySQL/MariaDB (Gelişmiş)
database:
  type: "mysql"
  host: "localhost"
  port: 3306
  database: "mageconomy"
  username: "root"
  password: "password"
  
  # Connection Pool Ayarları
  pool:
    maximum-pool-size: 10
    minimum-idle: 5
    connection-timeout: 30000
    idle-timeout: 600000
    max-lifetime: 1800000
```

---

## 🎯 Event Sistemi

MagEconomy, ekonomi işlemleri için özel event'ler sağlar:

### Event Türleri

```java
// Para yatırma event'i
@EventHandler
public void onMoneyDeposit(MoneyDepositEvent event) {
    UUID playerUUID = event.getPlayerUUID();
    double amount = event.getAmount();
    double newBalance = event.getNewBalance();
    
    // Para yatırma işlemi gerçekleşti
    Player player = Bukkit.getPlayer(playerUUID);
    if (player != null) {
        player.sendMessage("Para yatırıldı: " + amount);
    }
}

// Para çekme event'i
@EventHandler
public void onMoneyWithdraw(MoneyWithdrawEvent event) {
    UUID playerUUID = event.getPlayerUUID();
    double amount = event.getAmount();
    double newBalance = event.getNewBalance();
    
    // Para çekme işlemi gerçekleşti
    // Log kaydı, istatistik güncelleme vb.
}

// Para transferi event'i
@EventHandler
public void onMoneyTransfer(MoneyTransferEvent event) {
    UUID fromUUID = event.getFromPlayerUUID();
    UUID toUUID = event.getToPlayerUUID();
    double amount = event.getAmount();
    
    // Transfer işlemi gerçekleşti
    // İki oyuncuya da bildirim gönder
}
```

### Event Kullanım Örnekleri

```java
public class EconomyListener implements Listener {
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onMoneyDeposit(MoneyDepositEvent event) {
        // VIP oyuncular için bonus
        Player player = Bukkit.getPlayer(event.getPlayerUUID());
        if (player != null && player.hasPermission("vip.bonus")) {
            double bonus = event.getAmount() * 0.05; // %5 bonus
            MagEconomyAPI.getInstance().deposit(player.getUniqueId(), bonus);
            player.sendMessage("VIP bonusu: " + bonus);
        }
    }
    
    @EventHandler
    public void onLargeTransaction(MoneyWithdrawEvent event) {
        // Büyük para hareketlerini logla
        if (event.getAmount() > 10000) {
            Bukkit.getLogger().info("Büyük para hareketi: " + 
                event.getPlayerUUID() + " - " + event.getAmount());
        }
    }
}
```

---

## 💾 Cache Sistemi

### Cache Nasıl Çalışır?

MagEconomy, performansı artırmak için akıllı bir cache sistemi kullanır:

1. **Otomatik Cache**: Sık erişilen oyuncu verileri otomatik cache'lenir
2. **Expiration**: Belirli süre sonra cache temizlenir
3. **Memory-Safe**: Memory leak'leri önlemek için kontrollü boyut
4. **Thread-Safe**: Çoklu thread ortamında güvenli

### Cache Yönetimi

```java
public class CacheManager {
    
    public void managePlayers() {
        PlayerEconomyService service = PlayerEconomyService.getInstance();
        
        // Cache boyutunu kontrol et
        int cacheSize = service.getCacheSize();
        System.out.println("Mevcut cache boyutu: " + cacheSize);
        
        // Belirli oyuncuyu cache'den kaldır
        UUID playerUUID = UUID.fromString("...");
        service.removeFromCache(playerUUID);
        
        // Tüm cache'i temizle
        service.clearCache();
    }
}
```

### Cache Ayarları

```yaml
cache:
  enabled: true                 # true/false
  expire-time-minutes: 30       # Cache süresi
  max-size: 1000               # Maksimum cache boyutu (opsiyonel)
```

---

## 🗄️ Veritabanı

### Desteklenen Veritabanları

#### SQLite (Varsayılan)
- **Avantajlar**: Kolay kurulum, dosya tabanlı
- **Dezavantajlar**: Sınırlı eşzamanlılık
- **Kullanım**: Küçük-orta sunucular için ideal

#### MySQL/MariaDB
- **Avantajlar**: Yüksek performans, eşzamanlılık
- **Dezavantajlar**: Ayrı kurulum gerekir
- **Kullanım**: Büyük sunucular için önerilen

### Veritabanı Yapısı

```sql
CREATE TABLE player_economies (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    player_uuid VARCHAR(36) UNIQUE NOT NULL,
    player_name VARCHAR(16) NOT NULL,
    balance DECIMAL(20, 2) NOT NULL DEFAULT 0.00,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Index'ler performans için
CREATE INDEX idx_player_uuid ON player_economies(player_uuid);
CREATE INDEX idx_balance ON player_economies(balance);
CREATE INDEX idx_player_name ON player_economies(player_name);
```

### Veritabanı Yönetimi

```java
public class DatabaseExample {
    
    public void databaseOperations() {
        DatabaseManager dbManager = DatabaseManager.getInstance();
        
        // Bağlantı durumunu kontrol et
        boolean isConnected = dbManager.isConnected();
        System.out.println("Veritabanı bağlı: " + isConnected);
        
        // Tablolar hazır mı kontrol et
        boolean tablesReady = dbManager.areTablesReady();
        System.out.println("Tablolar hazır: " + tablesReady);
        
        // Manuel tablo oluşturma
        dbManager.createTable(PlayerEconomy.class)
            .thenRun(() -> System.out.println("Tablo oluşturuldu"));
    }
}
```

---

## 🏷️ PlaceholderAPI Desteği

### Mevcut Placeholder'lar

```yaml
# Oyuncu bakiyesi
%mageconomy_balance%           # Formatlanmış bakiye
%mageconomy_balance_raw%       # Ham bakiye değeri

# İstatistikler
%mageconomy_total_players%     # Toplam oyuncu sayısı
%mageconomy_total_value%       # Toplam ekonomik değer
%mageconomy_rank%              # Oyuncunun zenginlik sırası

# Para birimi
%mageconomy_currency%          # Para birimi adı
%mageconomy_currency_plural%   # Para birimi çoğul adı

# Sıralama
%mageconomy_top_<sıra>_name%   # En zengin oyuncu adı
%mageconomy_top_<sıra>_balance% # En zengin oyuncu bakiyesi
```

### Kullanım Örnekleri

```yaml
# Scoreboard'da kullanım
scoreboard:
  title: "&6&lSUNUCU EKONOMİSİ"
  lines:
    - "&7Bakiyeniz: &a%mageconomy_balance%"
    - "&7Sıranız: &e#%mageconomy_rank%"
    - "&7Toplam Oyuncu: &b%mageconomy_total_players%"
    - ""
    - "&6En Zengin:"
    - "&71. &f%mageconomy_top_1_name%"
    - "&7   &a%mageconomy_top_1_balance%"

# Chat formatında kullanım
chat-format: "&7[&a%mageconomy_balance%&7] &f%player_name%: %message%"

# Tab listesinde kullanım
tab-format: "&f%player_name% &7[&a%mageconomy_balance%&7]"
```

---

## 🛡️ Güvenlik ve Performans

### Güvenlik Özellikleri

#### Thread-Safety
```java
// MagEconomy tüm operasyonları thread-safe olarak tasarlar
public class ThreadSafeOperations {
    
    public void multiThreadExample() {
        MagEconomyAPI api = MagEconomyAPI.getInstance();
        UUID playerUUID = UUID.fromString("...");
        
        // Aynı anda birden fazla thread bu operasyonu çalıştırabilir
        CompletableFuture.allOf(
            api.deposit(playerUUID, 100.0),
            api.withdraw(playerUUID, 50.0),
            api.getBalance(playerUUID)
        ).thenRun(() -> {
            System.out.println("Tüm operasyonlar tamamlandı");
        });
    }
}
```

#### Validasyon Sistemleri
```java
// Otomatik input validasyonu
api.deposit(playerUUID, -100.0);    // IllegalArgumentException
api.withdraw(playerUUID, Double.NaN); // IllegalArgumentException
api.transfer(playerUUID, playerUUID, 100.0); // IllegalArgumentException (aynı oyuncu)
```

#### Memory Leak Koruması
```java
// Akıllı cache boyut yönetimi
public class MemoryProtection {
    // Warned plugins cache'i memory leak'i önler
    private static final Set<String> warnedPlugins = ConcurrentHashMap.newKeySet();
    
    // Cache expiration ile eski veriler otomatik temizlenir
    private static final long CACHE_DURATION = 5000; // 5 saniye
}
```

### Performans Optimizasyonları

#### Asenkron İşlemler
```java
// Tüm veritabanı işlemleri asenkron
public CompletableFuture<Double> getBalance(UUID playerUUID) {
    return CompletableFuture.supplyAsync(() -> {
        // Veritabanı işlemi ana thread'i bloklamaz
        return fetchBalanceFromDatabase(playerUUID);
    });
}
```

#### Akıllı Cache Sistemi
```java
// Double-checked locking ile performans
public static boolean isAvailable() {
    long currentTime = System.currentTimeMillis();
    
    // Cache kontrolü (thread-safe)
    if (currentTime - lastAvailabilityCheck < CACHE_DURATION) {
        return lastAvailabilityResult;
    }
    
    synchronized (LOCK) {
        // Double-checked locking pattern
        if (currentTime - lastAvailabilityCheck < CACHE_DURATION) {
            return lastAvailabilityResult;
        }
        
        // Yeniden hesapla
        boolean available = checkAvailability();
        lastAvailabilityCheck = currentTime;
        lastAvailabilityResult = available;
        
        return available;
    }
}
```

#### Batch Operations
```java
// Toplu işlemler için optimize edilmiş metodlar
public CompletableFuture<List<PlayerEconomy>> getTopPlayers(int limit) {
    // Tek sorgu ile birden fazla oyuncu getir
    return playerEconomyDao.getTopPlayers(limit);
}
```

---

## 🔧 Troubleshooting

### Yaygın Sorunlar ve Çözümleri

#### 1. API Null Dönüyor
```java
// Problem
MagEconomyAPI api = MagEconomyProvider.getAPI(this);
if (api == null) {
    // MagEconomy yüklü değil veya henüz hazır değil
}

// Çözüm
public void onEnable() {
    // Server tamamen yüklendikten sonra dene
    Bukkit.getScheduler().runTaskLater(this, () -> {
        MagEconomyAPI api = MagEconomyProvider.getAPI(this);
        if (api != null && api.isReady()) {
            // API hazır
            initializeEconomyFeatures();
        }
    }, 20L); // 1 saniye bekle
}
```

#### 2. Cache Problemleri
```java
// Cache'i manuel temizle
public void fixCacheIssues() {
    PlayerEconomyService service = PlayerEconomyService.getInstance();
    
    // Tüm cache'i temizle
    service.clearCache();
    
    // Specific player cache'ini temizle
    UUID problemPlayer = UUID.fromString("...");
    service.removeFromCache(problemPlayer);
}
```

#### 3. Veritabanı Bağlantı Sorunları
```java
// Veritabanı durumunu kontrol et
public void checkDatabaseHealth() {
    DatabaseManager db = DatabaseManager.getInstance();
    
    if (!db.isConnected()) {
        getLogger().warning("Veritabanı bağlantısı yok!");
        
        // Yeniden bağlan
        db.reconnect().thenRun(() -> {
            getLogger().info("Veritabanı yeniden bağlandı");
        });
    }
}
```

#### 4. Memory Leaks
```java
// Plugin kapatılırken temizlik yap
@Override
public void onDisable() {
    // Cache'leri temizle
    MagEconomyProvider.clearAllWarningCache();
    
    // API'yi kapat
    MagEconomyAPI.getInstance().shutdown();
    
    // Service'leri kapat
    PlayerEconomyService.getInstance().shutdown();
}
```

### Debug Araçları

#### Health Check
```java
public void performHealthCheck() {
    MagEconomy plugin = MagEconomy.getInstance();
    
    // Genel sağlık durumu
    boolean healthy = plugin.isHealthy();
    System.out.println("Plugin Healthy: " + healthy);
    
    // Detaylı durum raporu
    String status = plugin.getHealthStatus();
    System.out.println(status);
}
```

#### Provider İstatistikleri
```java
public void showProviderStats() {
    String stats = MagEconomyProvider.getProviderStats();
    System.out.println(stats);
    
    int cacheSize = MagEconomyProvider.getCacheSize();
    System.out.println("Provider Cache Size: " + cacheSize);
}
```

---

## ❓ SSS

### Genel Sorular

**S: MagEconomy diğer ekonomi eklentileriyle uyumlu mu?**
A: MagEconomy Vault API'sini kullanır, bu nedenle Vault destekli tüm eklentilerle uyumludur.

**S: Var olan ekonomi verilerimi aktarabilir miyim?**
A: Evet, Vault API üzerinden diğer ekonomi eklentilerinden veri aktarabilirsiniz.

**S: Cache sistemi ne kadar bellek kullanır?**
A: Cache boyutu konfigüre edilebilir ve otomatik olarak yönetilir. Ortalama bir oyuncu için ~1KB bellek kullanır.

### Teknik Sorular

**S: API thread-safe mi?**
A: Evet, MagEconomy API'si tamamen thread-safe tasarlanmıştır.

**S: Asenkron işlemler nasıl çalışır?**
A: Tüm veritabanı işlemleri CompletableFuture ile asenkron olarak çalışır.

**S: Hangi veritabanlarını destekler?**
A: SQLite (varsayılan) ve MySQL/MariaDB desteklenir.

### Performans Soruları

**S: Büyük sunucularda performans nasıl?**
A: MagEconomy 1000+ oyunculu sunucularda test edilmiş ve optimize edilmiştir.

**S: Cache'siz kullanabilir miyim?**
A: Evet, cache'i devre dışı bırakabilirsiniz ancak performans düşer.

**S: Veritabanı sorguları optimize mi?**
A: Evet, tüm sorgular optimize edilmiş ve index'ler eklenmiştir.

### Geliştirici Soruları

**S: API nasıl kullanmaya başlarım?**
A: `MagEconomyProvider.getAPI(this)` ile API'ye erişin ve örnekleri inceleyin.

**S: Custom event'ler yazabilir miyim?**
A: Evet, MagEconomy event'lerini dinleyerek custom logic ekleyebilirsiniz.

**S: PlaceholderAPI nasıl kullanırım?**
A: MagEconomy otomatik olarak placeholder'ları kaydeder, sadece kullanmanız yeterli.

---

## 📞 Destek ve İletişim

### Destek Kanalları
- **GitHub Issues**: Bug raporları ve feature istekleri
- **Discord**: Anlık destek ve topluluk
- **Wiki**: Detaylı dokümantasyon

### Geliştirici
- **Geliştirici**: ozaii1337
- **GitHub**: [MagEconomy Repository]
- **Discord**: [MagEconomy Discord Server]

### Katkıda Bulunma
MagEconomy açık kaynak bir projedir. Katkılarınızı bekliyoruz:
- Bug raporları
- Feature önerileri
- Code contributions
- Dokümantasyon iyileştirmeleri

---

## 📄 Lisans

MagEconomy, [MIT Lisansı] altında dağıtılmaktadır.

---

## 🔄 Güncellemeler

### v1.0.0 (Mevcut)
- ✅ Thread-safe API
- ✅ Vault entegrasyonu
- ✅ Cache sistemi
- ✅ PlaceholderAPI desteği
- ✅ MySQL/SQLite desteği

### Gelecek Güncellemeler
- 🔄 Web dashboard
- 🔄 Advanced statistics
- 🔄 Multi-currency support
- 🔄 Bank system
- 🔄 Loan system

---

Bu wiki, MagEconomy'nin tüm özelliklerini kapsamlı şekilde açıklamaktadır. Daha fazla bilgi için GitHub repository'sini ziyaret edin ve toplulukla etkileşime geçin!
