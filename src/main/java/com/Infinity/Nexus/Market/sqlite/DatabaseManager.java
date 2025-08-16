package com.Infinity.Nexus.Market.sqlite;

import com.Infinity.Nexus.Market.InfinityNexusMarket;
import com.Infinity.Nexus.Market.config.ModConfigs;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import java.sql.*;
import java.util.*;
import java.util.function.Supplier;

public class DatabaseManager {
    private static final Logger LOGGER = InfinityNexusMarket.LOGGER;
    public static final UUID SERVER_UUID = UUID.fromString("00000000-0000-0000-0000-00000000c0de");
    private static Connection conn = null;
    private static boolean isInitialized = false;
    private static boolean usingShadowDriver = false;
    public static final Map<String, Double> SERVER_ITEM_PRICE_CACHE = new HashMap<>();
    private static long lastCacheUpdate = 0;
    private static final long CACHE_TTL = 5 * 60 * 1000;

    // Métodos auxiliares de tempo
    private static <R> R logExecutionTime(String methodName, Supplier<R> method) {
        if (!isInitialized) return null;
        long startTime = System.nanoTime();
        R result = method.get();
        if(ModConfigs.debug) {
            long endTime = System.nanoTime();
            System.out.println("[Debug] " + methodName + " executado em " + ((endTime - startTime) / 1_000_000.0) + " ms");
        }
        return result;
    }

    private static void logExecutionTime(String methodName, Runnable method) {
        if (!isInitialized) return;
        long startTime = System.nanoTime();
        method.run();
        if(ModConfigs.debug) {
            long endTime = System.nanoTime();
            System.out.println("[Debug] " + methodName + " executado em " + ((endTime - startTime) / 1_000_000.0) + " ms");
        }
    }

    static {
        loadDriverWithFallback();
    }

    private static void loadDriverWithFallback() {
        try {
            Class.forName("com.infinity.nexus.shadow.mysql.cj.jdbc.Driver");
            usingShadowDriver = true;
            LOGGER.info("Driver MySQL (shadow) carregado com sucesso");
        } catch (ClassNotFoundException e) {
            LOGGER.warn("Driver MySQL (shadow) não encontrado, tentando versão padrão...");
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                usingShadowDriver = false;
                LOGGER.info("Driver MySQL (padrão) carregado com sucesso");
            } catch (ClassNotFoundException ex) {
                LOGGER.error("Falha ao carregar ambos os drivers MySQL (shadow e padrão)");
            }
        }

        try {
            Driver driver = DriverManager.getDriver("jdbc:mysql://dummy");
            LOGGER.debug("Driver registrado: {}", driver.getClass().getName());
        } catch (SQLException e) {
            LOGGER.error("Falha ao verificar driver registrado", e);
        }
    }

    public static void initialize() throws SQLException {
        if (isInitialized) return;

        try {
            LOGGER.debug("Estabelecendo conexão com o banco MySQL...");
            conn = DataSourceManager.getConnection();
            conn.setAutoCommit(false);

            try (Statement stmt = conn.createStatement()) {
                stmt.execute("SET SESSION sql_mode='STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION'");
            }

            createAllTables(conn);
            updateServerItemPriceCache();
            isInitialized = true;
            LOGGER.info("Banco de dados MySQL inicializado com sucesso");
        } catch (SQLException e) {
            LOGGER.error("Falha ao conectar ao banco MySQL", e);
            isInitialized = false;
        }
    }

    public static Connection getConnection() {
        if (!isInitialized) return null;
        try {
            return DataSourceManager.getConnection();
        } catch (SQLException e) {
            LOGGER.error("Falha ao obter conexão com o banco de dados", e);
            return null;
        }
    }

    private static void createAllTables(Connection conn) throws SQLException {
        if (!isInitialized) return;
        logExecutionTime("createAllTables", () -> {
            String createPlayerBalances = "CREATE TABLE IF NOT EXISTS player_balances (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "player_name VARCHAR(64) NOT NULL, " +
                    "balance DOUBLE DEFAULT 0.0, " +
                    "total_gasto DOUBLE DEFAULT 0.0, " +
                    "total_ganho DOUBLE DEFAULT 0.0, " +
                    "total_vendas INT DEFAULT 0, " +
                    "total_compras INT DEFAULT 0, " +
                    "max_sales INT DEFAULT 5, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
                    ") ENGINE=InnoDB;";

            String createMarketItems = "CREATE TABLE IF NOT EXISTS market_items (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "entry_id VARCHAR(36), " +
                    "type ENUM('server', 'player') NOT NULL, " +
                    "seller_uuid VARCHAR(36), " +
                    "seller_name VARCHAR(64), " +
                    "item_stack_nbt TEXT NOT NULL, " +
                    "quantity INT NOT NULL, " +
                    "base_price DOUBLE NOT NULL, " +
                    "current_price DOUBLE NOT NULL, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                    "is_active TINYINT(1) DEFAULT 1" +
                    ") ENGINE=InnoDB;";

            String createSalesHistory = "CREATE TABLE IF NOT EXISTS sales_history (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "item_stack_nbt TEXT NOT NULL, " +
                    "quantity INT NOT NULL, " +
                    "price DOUBLE NOT NULL, " +
                    "seller_uuid VARCHAR(36), " +
                    "seller_name VARCHAR(64), " +
                    "transaction_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ") ENGINE=InnoDB;";

            String createInflationTable = "CREATE TABLE IF NOT EXISTS item_inflation (" +
                    "item_nbt TEXT NOT NULL, " +
                    "base_price DOUBLE NOT NULL, " +
                    "current_price DOUBLE NOT NULL, " +
                    "purchases_last_period INT DEFAULT 0, " +
                    "last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
                    ") ENGINE=InnoDB;";

            try (Statement stmt = conn.createStatement()) {
                stmt.execute(createPlayerBalances);
                stmt.execute(createMarketItems);
                stmt.execute(createSalesHistory);
                stmt.execute(createInflationTable);
            } catch (SQLException e) {
                LOGGER.error("Erro ao criar tabelas no banco de dados", e);
            }
        });
    }

    public static void reload() {
        if (!isInitialized) return;
        try {
            conn.close();
            isInitialized = false;
            initialize();
        } catch (SQLException e) {
            LOGGER.error("Erro ao recarregar o banco de dados", e);
        }
    }

    public static double getPlayerBalance(String uuid) {
        if (!isInitialized) return 0.0;
        return logExecutionTime("getPlayerBalance", () -> {
            try (Connection conn = getConnection();
                 PreparedStatement pstmt = conn.prepareStatement("SELECT balance FROM player_balances WHERE uuid = ?")) {
                pstmt.setString(1, uuid);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    return rs.getDouble("balance");
                } else {
                    setPlayerBalance(uuid, "", 0.0);
                    return 0.0;
                }
            } catch (SQLException e) {
                LOGGER.error("Erro ao buscar saldo do jogador", e);
                return 0.0;
            }
        });
    }

    public static void addPlayerBalance(String uuid, String name, double amount) {
        if (!isInitialized) return;
        logExecutionTime("addPlayerBalance", () -> {
            try (Connection conn = getConnection()) {
                String updateSql = "INSERT INTO player_balances (uuid, player_name, balance) " +
                        "VALUES (?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE " +
                        "player_name = VALUES(player_name), " +
                        "balance = balance + VALUES(balance)";

                try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
                    pstmt.setString(1, uuid);
                    pstmt.setString(2, name == null ? "" : name);
                    pstmt.setDouble(3, amount);
                    pstmt.executeUpdate();
                }
            } catch (SQLException e) {
                LOGGER.error("Erro ao adicionar saldo", e);
            }
        });
    }

    public static void setPlayerBalance(String uuid, String playerName, double balance) {
        if (!isInitialized) return;
        logExecutionTime("setPlayerBalance", () -> {
            try (Connection conn = getConnection()) {
                String updateSql = "INSERT INTO player_balances (uuid, player_name, balance, max_sales) " +
                        "VALUES (?, ?, ?, COALESCE((SELECT max_sales FROM player_balances WHERE uuid = ?), 5)) " +
                        "ON DUPLICATE KEY UPDATE " +
                        "player_name = VALUES(player_name), " +
                        "balance = VALUES(balance)";

                try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
                    pstmt.setString(1, uuid);
                    pstmt.setString(2, playerName == null ? "" : playerName);
                    pstmt.setDouble(3, balance);
                    pstmt.setString(4, uuid);
                    pstmt.executeUpdate();
                }
            } catch (SQLException e) {
                LOGGER.error("Erro ao definir saldo do jogador", e);
            }
        });
    }

    public static Map<String, Double> getAllPlayerBalances() {
        if (!isInitialized) return new HashMap<>();
        return logExecutionTime("getAllPlayerBalances", () -> {
            Map<String, Double> balances = new HashMap<>();
            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT player_name, balance FROM player_balances")) {
                while (rs.next()) {
                    balances.put(rs.getString("player_name"), rs.getDouble("balance"));
                }
            } catch (SQLException e) {
                LOGGER.error("Erro ao buscar saldos", e);
            }
            return balances;
        });
    }

    public static MarketItemEntry getMarketItemByStackAndPrice(ItemStack itemStack, double price, String sellerName, boolean randomSeller, String ownerName) {
        if (!isInitialized) return null;
        return logExecutionTime("getMarketItemByStackAndPrice", () -> {
            if (getAllMarketItems().isEmpty()) {
                return null;
            }
            String itemNbt = serializeItemStack(itemStack);
            if (itemNbt == null || itemNbt.isEmpty()) {
                return null;
            }
            return getAllMarketItems().stream()
                    .filter(entry -> entry != null)
                    .filter(entry -> entry.itemNbt.equals(itemNbt))
                    .filter(entry -> randomSeller ? !entry.sellerName.equals(ownerName) : (entry.sellerName.equals(sellerName) && !entry.sellerName.equals(ownerName)))
                    .filter(entry -> entry.currentPrice <= price)
                    .filter(entry -> entry.quantity >= 1)
                    .findFirst()
                    .orElse(null);
        });
    }

    public static void applyFirstAccountAmount(ServerPlayer serverPlayer) {
        Map<String, Double> accounts = getAllPlayerBalances();
        if(accounts.isEmpty()){
            return;
        }

        String playerName = serverPlayer.getName().getString();
        if(accounts.containsKey(playerName)){
            return;
        }
        String uuid = serverPlayer.getStringUUID();
        InfinityNexusMarket.LOGGER.info("§bGiving a first amount of coins to {}", serverPlayer.getName().getString());
        addPlayerBalance(uuid, playerName, ModConfigs.startPlayerAmount);
    }

    public static class MarketItemEntry {
        public int id;
        public String entryId;
        public String type;
        public String sellerUUID;
        public String sellerName;
        public String itemId;
        public String itemName;
        public String itemNbt;
        public int quantity;
        public double basePrice;
        public double currentPrice;
        public boolean isActive;
        public String createdAt;
        public String lastUpdated;
    }

    public static boolean addOrUpdateMarketItem(String entryId, String type, String sellerUUID, String sellerName, ItemStack item, int quantity, double basePrice, double currentPrice) {
        if (!isInitialized) return false;
        return logExecutionTime("addOrUpdateMarketItem", () -> {
            try (Connection conn = getConnection()) {
                try (PreparedStatement checkStmt = conn.prepareStatement(
                        "SELECT entry_id, quantity FROM market_items WHERE item_stack_nbt = ? AND type = ? AND is_active = 1 LIMIT 1")) {
                    ItemStack copy = item.copy();
                    copy.setCount(1);
                    String itemNbt = serializeItemStack(copy);
                    checkStmt.setString(1, itemNbt);
                    checkStmt.setString(2, type);
                    ResultSet rs = checkStmt.executeQuery();
                    if (rs.next()) {
                        String existingEntryId = rs.getString("entry_id");
                        if ("server".equals(type)) {
                            try (PreparedStatement updateStmt = conn.prepareStatement(
                                    "UPDATE market_items SET base_price = ?, current_price = ?, last_updated = CURRENT_TIMESTAMP WHERE entry_id = ?")) {
                                updateStmt.setDouble(1, basePrice);
                                updateStmt.setDouble(2, currentPrice);
                                updateStmt.setString(3, existingEntryId);
                                return updateStmt.executeUpdate() > 0;
                            }
                        } else if ("player".equals(type)) {
                            try (PreparedStatement checkPlayerStmt = conn.prepareStatement(
                                    "SELECT entry_id, quantity FROM market_items WHERE entry_id = ? AND seller_uuid = ? AND base_price = ? AND current_price = ?")) {
                                checkPlayerStmt.setString(1, existingEntryId);
                                checkPlayerStmt.setString(2, sellerUUID);
                                checkPlayerStmt.setDouble(3, basePrice);
                                checkPlayerStmt.setDouble(4, currentPrice);
                                ResultSet playerRs = checkPlayerStmt.executeQuery();
                                if (playerRs.next()) {
                                    int existingQuantity = playerRs.getInt("quantity");
                                    try (PreparedStatement updateStmt = conn.prepareStatement(
                                            "UPDATE market_items SET quantity = ?, last_updated = CURRENT_TIMESTAMP WHERE entry_id = ?")) {
                                        updateStmt.setInt(1, existingQuantity + quantity);
                                        updateStmt.setString(2, existingEntryId);
                                        return updateStmt.executeUpdate() > 0;
                                    }
                                }
                            }
                        }
                    }
                    String upsert = "INSERT INTO market_items (entry_id, type, seller_uuid, seller_name, item_stack_nbt, quantity, base_price, current_price, last_updated) VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)";
                    try (PreparedStatement pstmt = conn.prepareStatement(upsert)) {
                        pstmt.setString(1, entryId);
                        pstmt.setString(2, type);
                        pstmt.setString(3, "server".equals(type) ? SERVER_UUID.toString() : sellerUUID);
                        pstmt.setString(4, "server".equals(type) ? "Server" : sellerName);
                        pstmt.setString(5, itemNbt);
                        pstmt.setInt(6, "server".equals(type) ? 64 : quantity);
                        pstmt.setDouble(7, basePrice);
                        pstmt.setDouble(8, currentPrice);
                        return pstmt.executeUpdate() > 0;
                    }
                }
            } catch (SQLException e) {
                LOGGER.error("Erro ao adicionar/atualizar item", e);
                return false;
            }
        });
    }

    public static List<MarketItemEntry> getMarketItems(String typeFilter) {
        if (!isInitialized) return new ArrayList<>();
        return logExecutionTime("getMarketItems", () -> {
            List<MarketItemEntry> items = new ArrayList<>();
            try (Connection conn = getConnection()) {
                String query = "SELECT * FROM market_items WHERE is_active = TRUE" + (typeFilter != null ? " AND type = ?" : "") + " ORDER BY last_updated DESC";
                try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                    if (typeFilter != null) pstmt.setString(1, typeFilter);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        while (rs.next()) {
                            MarketItemEntry entry = new MarketItemEntry();
                            entry.id = rs.getInt("id");
                            entry.entryId = rs.getString("entry_id");
                            entry.type = rs.getString("type");
                            entry.sellerUUID = rs.getString("seller_uuid");
                            entry.sellerName = rs.getString("seller_name");
                            entry.itemNbt = rs.getString("item_stack_nbt");
                            entry.quantity = rs.getInt("quantity");
                            entry.basePrice = rs.getDouble("base_price");
                            entry.currentPrice = rs.getDouble("current_price");
                            entry.isActive = rs.getBoolean("is_active");
                            entry.createdAt = rs.getString("created_at");
                            entry.lastUpdated = rs.getString("last_updated");
                            items.add(entry);
                        }
                    }
                }
            } catch (SQLException e) {
                LOGGER.error("Erro ao buscar itens", e);
            }
            return items;
        });
    }

    public static MarketItemEntry getMarketItemByEntryId(String entryId) {
        if (!isInitialized) return null;
        return logExecutionTime("getMarketItemByEntryId", () -> {
            try (Connection conn = getConnection()) {
                String query = "SELECT * FROM market_items WHERE entry_id = ? AND is_active = 1 LIMIT 1;";
                try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                    pstmt.setString(1, entryId);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next()) {
                            MarketItemEntry entry = new MarketItemEntry();
                            entry.id = rs.getInt("id");
                            entry.entryId = rs.getString("entry_id");
                            entry.type = rs.getString("type");
                            entry.sellerUUID = rs.getString("seller_uuid");
                            entry.sellerName = rs.getString("seller_name");
                            entry.itemId = rs.getString("item_stack_nbt");
                            entry.itemName = "";
                            entry.itemNbt = rs.getString("item_stack_nbt");
                            entry.quantity = rs.getInt("quantity");
                            entry.basePrice = rs.getDouble("base_price");
                            entry.currentPrice = rs.getDouble("current_price");
                            entry.isActive = rs.getInt("is_active") == 1;
                            entry.createdAt = rs.getString("created_at");
                            entry.lastUpdated = rs.getString("last_updated");
                            return entry;
                        }
                    }
                }
            } catch (SQLException e) {
                LOGGER.error("Erro ao buscar item por entry_id", e);
            }
            return null;
        });
    }

    public static boolean removeMarketItem(String entryId) {
        if (!isInitialized) return false;
        return logExecutionTime("removeMarketItem", () -> {
            try (Connection conn = getConnection()) {
                try (PreparedStatement pstmt = conn.prepareStatement("UPDATE market_items SET is_active = 0 WHERE entry_id = ?")) {
                    pstmt.setString(1, entryId);
                    return pstmt.executeUpdate() > 0;
                }
            } catch (SQLException e) {
                LOGGER.error("Erro ao remover item", e);
                return false;
            }
        });
    }

    public static boolean addPlayerSale(String sellerUUID, String sellerName, ItemStack item, int quantity, double price) {
        if (!isInitialized) return false;
        return logExecutionTime("addPlayerSale", () -> {
            if (!canPlayerAddMoreSales(sellerUUID)) {
                InfinityNexusMarket.LOGGER.warn("Jogador {} atingiu o limite máximo de vendas ({}/{})", sellerName, getPlayerCurrentSalesCount(sellerUUID), getPlayerMaxSales(sellerUUID));
                return false;
            }
            return addOrUpdateMarketItem(UUID.randomUUID().toString(), "player", sellerUUID, sellerName, item, quantity, price, price);
        });
    }

    public static boolean addOrUpdateServerItem(String entryId, ItemStack item, double basePrice, double currentPrice) {
        if (!isInitialized) return false;
        return logExecutionTime("addOrUpdateServerItem", () -> {
            return addOrUpdateMarketItem(entryId, "server", SERVER_UUID.toString(), "Server", item, 64, basePrice, currentPrice);
        });
    }

    public static List<MarketItemEntry> getAllPlayerSales() {
        if (!isInitialized) return new ArrayList<>();
        return logExecutionTime("getAllPlayerSales", () -> getMarketItems("player"));
    }

    public static List<MarketItemEntry> getAllServerItems() {
        if (!isInitialized) return new ArrayList<>();
        return logExecutionTime("getAllServerItems", () -> getMarketItems("server"));
    }

    public static List<MarketItemEntry> getAllMarketItems() {
        if (!isInitialized) return new ArrayList<>();
        return logExecutionTime("getAllMarketItems", () -> getMarketItems(null));
    }

    public static boolean addSalesHistory(String itemId, int quantity, double price, String sellerUUID, String sellerName) {
        if (!isInitialized) return false;
        return logExecutionTime("addSalesHistory", () -> {
            try (Connection conn = getConnection()) {
                try (PreparedStatement pstmt = conn.prepareStatement(
                        "INSERT INTO sales_history (item_stack_nbt, quantity, price, seller_uuid, seller_name) VALUES (?, ?, ?, ?, ?)")) {
                    pstmt.setString(1, itemId);
                    pstmt.setInt(2, quantity);
                    pstmt.setDouble(3, price);
                    pstmt.setString(4, sellerUUID);
                    pstmt.setString(5, sellerName);
                    boolean success = pstmt.executeUpdate() > 0;
                    applyServerInflation(itemId, quantity);
                    return success;
                }
            } catch (SQLException e) {
                LOGGER.error("Erro ao adicionar histórico", e);
                return false;
            }
        });
    }

    private static boolean isServerItem(String itemNbt) {
        if (!isInitialized) return false;
        return logExecutionTime("isServerItem", () -> {
            try (Connection conn = getConnection()) {
                try (PreparedStatement pstmt = conn.prepareStatement("SELECT 1 FROM market_items WHERE item_stack_nbt = ? AND type = 'server' LIMIT 1")) {
                    pstmt.setString(1, itemNbt);
                    return pstmt.executeQuery().next();
                }
            } catch (SQLException e) {
                LOGGER.error("Erro ao verificar item do servidor", e);
                return false;
            }
        });
    }

    public static Map<String, Integer> getSalesHistoryAndReset() {
        if (!isInitialized) return new HashMap<>();
        return logExecutionTime("getSalesHistoryAndReset", () -> {
            Map<String, Integer> sales = new HashMap<>();
            try (Connection conn = getConnection()) {
                try (ResultSet rs = conn.createStatement().executeQuery("SELECT item_stack_nbt, SUM(quantity) as total_quantity FROM sales_history GROUP BY item_stack_nbt")) {
                    while (rs.next()) {
                        sales.put(rs.getString("item_stack_nbt"), rs.getInt("total_quantity"));
                    }
                }
                conn.createStatement().execute("DELETE FROM sales_history;");
            } catch (SQLException e) {
                LOGGER.error("Erro ao buscar histórico", e);
            }
            return sales;
        });
    }

    public static String serializeItemStack(ItemStack item) {
        if (!isInitialized) return "";
        return logExecutionTime("serializeItemStack", () -> {
            if (item == null || item.isEmpty()) return "";
            try {
                JsonElement json = ItemStack.CODEC.encodeStart(
                        RegistryOps.create(JsonOps.INSTANCE, InfinityNexusMarket.serverLevel.registryAccess()),
                        item
                ).getOrThrow();
                return json.toString();
            } catch (Exception e) {
                LOGGER.error("Erro ao serializar ItemStack {}", e.getMessage());
                return "";
            }
        });
    }

    public static ItemStack deserializeItemStack(String jsonString) {
        if (!isInitialized) return ItemStack.EMPTY;
        return logExecutionTime("deserializeItemStack", () -> {
            try {
                if (jsonString == null || jsonString.isEmpty()) return ItemStack.EMPTY;
                return ItemStack.CODEC.parse(
                        RegistryOps.create(JsonOps.INSTANCE, InfinityNexusMarket.serverLevel.registryAccess()),
                        JsonParser.parseString(jsonString)
                ).getOrThrow();
            } catch (Exception e) {
                LOGGER.error("Erro ao deserializar ItemStack: {}", e.getMessage());
                String id = StringUtils.substringBetween(jsonString, "\"id\":\"", "\"");
                if (id != null){
                    String parts[] = id.split(":");
                    LOGGER.error("Considere executar o comando /market remove <Mod {}: ou Item {}> id {}", parts[0], id, e.getMessage());
                }
                return ItemStack.EMPTY;
            }
        });
    }

    public static void cleanCorruptedData() {
        if (!isInitialized) return;
        logExecutionTime("cleanCorruptedData", () -> {
            try (Connection conn = getConnection()) {
                int deleted = conn.createStatement().executeUpdate(
                        "DELETE FROM market_items WHERE entry_id NOT LIKE '%-%-%-%-%' OR entry_id LIKE 'player_%' OR entry_id LIKE 'server_%'");
                if (deleted > 0) {
                    InfinityNexusMarket.LOGGER.info("Removidos " + deleted + " registros corrompidos");
                }
            } catch (SQLException e) {
                LOGGER.error("Erro ao limpar dados", e);
            }
        });
    }

    public static int cleanExpiredSales(int daysToExpire) {
        if (!isInitialized) return 0;
        return logExecutionTime("cleanExpiredSales", () -> {
            try (Connection conn = getConnection()) {
                int deleted = conn.createStatement().executeUpdate(
                        "DELETE FROM market_items WHERE type = 'player' AND is_active = 1 AND created_at < datetime('now', '-" + daysToExpire + " days');");
                if (deleted > 0) {
                    InfinityNexusMarket.LOGGER.info("Removidas " + deleted + " vendas expiradas");
                }
                return deleted;
            } catch (SQLException e) {
                LOGGER.error("Erro ao limpar vendas", e);
                return 0;
            }
        });
    }

    public static void compactDatabase() {
        if (!isInitialized) return;
        logExecutionTime("compactDatabase", () -> {
            try (Connection conn = getConnection()) {
                conn.createStatement().execute("VACUUM;");
                InfinityNexusMarket.LOGGER.info("Database compactada");
            } catch (SQLException e) {
                LOGGER.error("Erro ao compactar", e);
            }
        });
    }

    public static String getSellerNameByEntryId(String entryId) {
        if (!isInitialized) return null;
        return logExecutionTime("getSellerNameByEntryId", () -> {
            try (Connection conn = getConnection()) {
                try (PreparedStatement pstmt = conn.prepareStatement("SELECT seller_name FROM market_items WHERE entry_id = ? LIMIT 1")) {
                    pstmt.setString(1, entryId);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next()) return rs.getString("seller_name");
                    }
                }
            } catch (SQLException e) {
                LOGGER.error("Erro ao buscar vendedor", e);
            }
            return null;
        });
    }

    public static String getSellerNameByEntryUUID(String entryId) {
        if (!isInitialized) return null;
        return logExecutionTime("getSellerNameByEntryUUID", () -> {
            try (Connection conn = getConnection()) {
                try (PreparedStatement pstmt = conn.prepareStatement("SELECT seller_name FROM market_items WHERE seller_uuid = ? LIMIT 1")) {
                    pstmt.setString(1, entryId);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next()) return rs.getString("seller_name");
                    }
                }
            } catch (SQLException e) {
                LOGGER.error("Erro ao buscar vendedor", e);
            }
            return null;
        });
    }

    public static void incrementPlayerStats(String uuid, double gasto, double ganho, int vendas, int compras) {
        if (!isInitialized) return;
        logExecutionTime("incrementPlayerStats", () -> {
            try (Connection conn = getConnection()) {
                try (PreparedStatement pstmt = conn.prepareStatement(
                        "UPDATE player_balances SET total_gasto = total_gasto + ?, total_ganho = total_ganho + ?, total_vendas = total_vendas + ?, total_compras = total_compras + ?, last_updated = CURRENT_TIMESTAMP WHERE uuid = ?")) {
                    pstmt.setDouble(1, gasto);
                    pstmt.setDouble(2, ganho);
                    pstmt.setInt(3, vendas);
                    pstmt.setInt(4, compras);
                    pstmt.setString(5, uuid);
                    pstmt.executeUpdate();
                }
            } catch (SQLException e) {
                LOGGER.error("Erro ao atualizar estatísticas", e);
            }
        });
    }

    public static int getPlayerMaxSales(String uuid) {
        if (!isInitialized) return 5;
        return logExecutionTime("getPlayerMaxSales", () -> {
            if (uuid.equals(SERVER_UUID.toString())) {
                return 500;
            }
            try (Connection conn = getConnection();
                 PreparedStatement pstmt = conn.prepareStatement("SELECT max_sales FROM player_balances WHERE uuid = ?")) {
                pstmt.setString(1, uuid);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) return rs.getInt("max_sales");
                }
            } catch (SQLException e) {
                LOGGER.error("Erro ao buscar limite de vendas", e);
            }
            return 5;
        });
    }

    public static void setPlayerMaxSales(String uuid, int maxSales) {
        if (!isInitialized) return;
        logExecutionTime("setPlayerMaxSales", () -> {
            try (Connection conn = getConnection()) {
                setPlayerBalance(uuid, null, getPlayerBalance(uuid));
                try (PreparedStatement pstmt = conn.prepareStatement("UPDATE player_balances SET max_sales = ? WHERE uuid = ?")) {
                    pstmt.setInt(1, maxSales);
                    pstmt.setString(2, uuid);
                    pstmt.executeUpdate();
                }
            } catch (SQLException e) {
                LOGGER.error("Erro ao definir limite de vendas", e);
            }
        });
    }

    public static int getPlayerCurrentSalesCount(String uuid) {
        if (!isInitialized) return 0;
        return logExecutionTime("getPlayerCurrentSalesCount", () -> {
            try (Connection conn = getConnection();
                 PreparedStatement pstmt = conn.prepareStatement("SELECT COUNT(*) as count FROM market_items WHERE type = 'player' AND seller_uuid = ? AND is_active = 1")) {
                pstmt.setString(1, uuid);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) return rs.getInt("count");
                }
            } catch (SQLException e) {
                LOGGER.error("Erro ao contar vendas do jogador", e);
            }
            return 0;
        });
    }

    public static boolean canPlayerAddMoreSales(String uuid) {
        if (!isInitialized) return false;
        return logExecutionTime("canPlayerAddMoreSales", () -> {
            int current = getPlayerCurrentSalesCount(uuid);
            int max = getPlayerMaxSales(uuid);
            return current < max;
        });
    }

    public static void applyServerInflation(String itemNbt, int quantityPurchased) {
        if (!isInitialized) return;
        logExecutionTime("applyServerInflation", () -> {
            try (Connection conn = getConnection()) {
                String baseItemId = extractBaseItemId(itemNbt);
                if (!isServerItemByBaseId(baseItemId))
                    return;

                String query = "INSERT INTO item_inflation (item_nbt, base_price, current_price, purchases_last_period, last_updated) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP) ON DUPLICATE KEY UPDATE purchases_last_period = purchases_last_period + ?";
                double basePrice = getBasePriceForItemByBaseId(baseItemId);
                double currentPrice = getCurrentPriceForItem(baseItemId);

                try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                    pstmt.setString(1, baseItemId);
                    pstmt.setDouble(2, basePrice);
                    pstmt.setDouble(3, currentPrice);
                    pstmt.setInt(4, quantityPurchased);
                    pstmt.setInt(5, quantityPurchased);
                    pstmt.executeUpdate();
                }
            } catch (SQLException e) {
                LOGGER.error("Erro ao registrar compra para inflação", e);
            }
        });
    }

    private static boolean isServerItemByBaseId(String baseItemId) {
        if (!isInitialized) return false;
        return logExecutionTime("isServerItemByBaseId", () -> {
            try (Connection conn = getConnection();
                 PreparedStatement pstmt = conn.prepareStatement("SELECT 1 FROM market_items WHERE item_stack_nbt LIKE ? AND type = 'server' LIMIT 1")) {
                pstmt.setString(1, "%\"" + baseItemId + "\"%");
                return pstmt.executeQuery().next();
            } catch (SQLException e) {
                LOGGER.error("Erro ao verificar item do servidor", e);
                return false;
            }
        });
    }

    private static double getBasePriceForItemByBaseId(String baseItemId) {
        if (!isInitialized) return 0.0;
        return logExecutionTime("getBasePriceForItemByBaseId", () -> {
            try (Connection conn =  getConnection()) {
                PreparedStatement pstmt = conn.prepareStatement(
                        "SELECT MIN(base_price) FROM market_items WHERE item_stack_nbt LIKE ? AND type = 'server';");
                pstmt.setString(1, "%\"" + baseItemId + "\"%");
                ResultSet rs = pstmt.executeQuery();
                return rs.next() ? rs.getDouble(1) : 0.0;
            } catch (SQLException e) {
                InfinityNexusMarket.LOGGER.error("Erro ao buscar preço base", e);
                return 0.0;
            }
        });
    }

    public static double getCurrentPriceForItem(String itemNbt) {
        if (!isInitialized) return 0.0;
        return logExecutionTime("getCurrentPriceForItem", () -> {
            try (Connection conn =  getConnection()) {
                PreparedStatement pstmt = conn.prepareStatement(
                        "SELECT current_price FROM item_inflation WHERE item_nbt = ?;");
                pstmt.setString(1, itemNbt);
                ResultSet rs = pstmt.executeQuery();

                if (rs.next()) {
                    return rs.getDouble("current_price");
                } else {
                    return getBasePriceForItem(itemNbt);
                }
            } catch (SQLException e) {
                InfinityNexusMarket.LOGGER.error("Erro ao buscar preço atual", e);
                return 0.0;
            }
        });
    }

    public static void updateInflationPrices() {
        if (!isInitialized) return;
        logExecutionTime("updateInflationPrices", () -> {
            try (Connection conn = getConnection()) {
                ResultSet rs = conn.createStatement().executeQuery(
                        "SELECT item_nbt, base_price, current_price, purchases_last_period FROM item_inflation");

                while (rs.next()) {
                    String itemNbt = rs.getString("item_nbt");
                    double basePrice = rs.getDouble("base_price");
                    double currentPrice = rs.getDouble("current_price");
                    int purchases = rs.getInt("purchases_last_period");

                    double variation = 0;
                    if (purchases >= 1000) {
                        variation = 0.0005;
                    } else if (purchases == 0) {
                        variation = -0.0005;
                    }

                    double newPrice = currentPrice * (1 + variation);
                    newPrice = Math.max(basePrice * 0.8, Math.min(basePrice * 1.2, newPrice));

                    try (PreparedStatement pstmt = conn.prepareStatement(
                            "UPDATE item_inflation SET current_price = ?, purchases_last_period = 0 WHERE item_nbt = ?")) {
                        pstmt.setDouble(1, newPrice);
                        pstmt.setString(2, itemNbt);
                        pstmt.executeUpdate();
                    }

                    SERVER_ITEM_PRICE_CACHE.put(itemNbt, newPrice);
                    updateMarketItemPrice(itemNbt, newPrice);
                }
                updateServerItemPriceCache();
            } catch (SQLException e) {
                LOGGER.error("Erro ao atualizar inflação", e);
            }
        });
    }

    private static String extractBaseItemId(String itemNbt) {
        if (!isInitialized) return itemNbt;
        return logExecutionTime("extractBaseItemId", () -> {
            try {
                JsonElement json = JsonParser.parseString(itemNbt);
                if (json.isJsonObject()) {
                    JsonElement idElement = json.getAsJsonObject().get("id");
                    if (idElement != null) {
                        return idElement.getAsString();
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Erro ao extrair ID base do item", e);
            }
            return itemNbt;
        });
    }

    private static double getBasePriceForItem(String itemNbt) {
        if (!isInitialized) return 0.0;
        return logExecutionTime("getBasePriceForItem", () -> {
            try (Connection conn =  getConnection()) {
                PreparedStatement pstmt = conn.prepareStatement(
                        "SELECT base_price FROM market_items WHERE item_stack_nbt = ? AND type = 'server' LIMIT 1;");
                pstmt.setString(1, itemNbt);
                ResultSet rs = pstmt.executeQuery();

                if (rs.next()) {
                    return rs.getDouble("base_price");
                } else {
                    return 0.0;
                }
            } catch (SQLException e) {
                InfinityNexusMarket.LOGGER.error("Erro ao buscar preço base", e);
                return 0.0;
            }
        });
    }

    private static void updateMarketItemPrice(String baseItemId, double newPrice) {
        if (!isInitialized) return;
        logExecutionTime("updateMarketItemPrice", () -> {
            try (Connection conn = getConnection()) {
                PreparedStatement pstmt = conn.prepareStatement("UPDATE market_items SET current_price = ? WHERE item_stack_nbt LIKE ? AND type = 'server'");
                pstmt.setDouble(1, newPrice);
                pstmt.setString(2, "%\"" + baseItemId + "\"%");
                pstmt.executeUpdate();
            } catch (SQLException e) {
                LOGGER.error("Erro ao atualizar preço no mercado", e);
            }
        });
    }

    public static int getUpdatedItemsCount() {
        if (!isInitialized) return 0;
        return logExecutionTime("getUpdatedItemsCount", () -> {
            try (Connection conn = getConnection()) {
                ResultSet rs = conn.createStatement().executeQuery("SELECT COUNT(*) FROM item_inflation WHERE purchases_last_period > 0");
                return rs.next() ? rs.getInt(1) : 0;
            } catch (SQLException e) {
                LOGGER.error("Erro ao contar itens atualizados", e);
                return 0;
            }
        });
    }

    public static void updateServerItemPriceCache() {
        if (!isInitialized) return;

        long now = System.currentTimeMillis();
        if (now - lastCacheUpdate < CACHE_TTL && !SERVER_ITEM_PRICE_CACHE.isEmpty()) {
            return;
        }

        SERVER_ITEM_PRICE_CACHE.clear();
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT item_stack_nbt, current_price FROM market_items WHERE type = 'server'")) {

            while (rs.next()) {
                String itemNbt = rs.getString("item_stack_nbt");
                double price = rs.getDouble("current_price");
                SERVER_ITEM_PRICE_CACHE.put(itemNbt, price);
            }
            lastCacheUpdate = now;
        } catch (SQLException e) {
            LOGGER.error("Erro ao atualizar cache de preços do servidor", e);
        }
    }

    public static int removeItemsByModNamespace(String modNamespace) {
        if (!isInitialized) return 0;
        return logExecutionTime("removeItemsByModNamespace", () -> {
            try (Connection conn = getConnection()) {
                // Remove itens onde o item_stack_nbt contém o namespace do mod
                String sql = "DELETE FROM market_items WHERE item_stack_nbt LIKE ?";
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, "%\"id\":\"" + modNamespace + "%");
                    return pstmt.executeUpdate();
                }
            } catch (SQLException e) {
                LOGGER.error("Erro ao remover itens por namespace do mod", e);
                return 0;
            }
        });
    }

    public static int removeItemsByExactId(String fullItemId) {
        if (!isInitialized) return 0;
        return logExecutionTime("removeItemsByExactId", () -> {
            try (Connection conn = getConnection()) {
                // Remove itens onde o item_stack_nbt contém o ID exato do item
                String sql = "DELETE FROM market_items WHERE item_stack_nbt LIKE ?";
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, "%\"id\":\"" + fullItemId + "\"%");
                    return pstmt.executeUpdate();
                }
            } catch (SQLException e) {
                LOGGER.error("Erro ao remover itens por ID completo", e);
                return 0;
            }
        });
    }

    public static int clearAllPlayerBalances() {
        if (!isInitialized) return 0;
        return logExecutionTime("clearAllPlayerBalances", () -> {
            try (Connection conn = getConnection()) {
                try (Statement stmt = conn.createStatement()) {
                    int count = stmt.executeUpdate("DELETE FROM player_balances");
                    conn.commit();
                    LOGGER.info("Removed all player balances ({} records)", count);
                    return count;
                }
            } catch (SQLException e) {
                LOGGER.error("Error clearing player balances", e);
                return 0;
            }
        });
    }
}