package com.Infinity.Nexus.Market.sqlite;

import com.Infinity.Nexus.Market.InfinityNexusMarket;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;
import org.h2.Driver;

import java.io.File;
import java.sql.*;
import java.util.*;

public class DatabaseManager {

    private static final String DB_NAME = "market";
    private static final String DB_USERNAME = "sa";
    private static final String DB_PASSWORD = "sa";
    private static String DB_URL;
    private static final UUID SERVER_UUID = UUID.fromString("00000000-0000-0000-0000-00000000c0de");
    private static boolean isInitialized = false;

    public static void initialize() {
        if (isInitialized) return;

        try {
            // Carregar o driver diretamente
            Driver driver = (Driver) Class.forName("org.h2.Driver").newInstance();
            DriverManager.registerDriver(new DriverShim(driver));
        } catch (Exception e) {
            System.err.println("[InfinityNexus] FATAL: Failed to load H2 driver");
            return;
        }

        File configDir = new File("config/infinity_nexus_market/");
        if (!configDir.exists()) configDir.mkdirs();
        DB_URL = "jdbc:h2:file:" + new File(configDir, DB_NAME).getAbsolutePath();

        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            createAllTables(conn);
            isInitialized = true;
            System.out.println("[InfinityNexus] H2 Database initialized with market.db");
        } catch (SQLException e) {
            if (e.getErrorCode() == 28000) {
                System.err.println("[InfinityNexus] FATAL: Database authentication failed. Please check credentials.");
            } else {
                System.err.println("[InfinityNexus] Failed to initialize database: " + e.getMessage());
            }
        }
    }


    private static void createAllTables(Connection conn) throws SQLException {
        // 1. Tabela de saldos dos jogadores
        String createPlayerBalances = "CREATE TABLE IF NOT EXISTS player_balances (" +
                "uuid TEXT PRIMARY KEY, " +
                "player_name TEXT NOT NULL, " +
                "balance REAL DEFAULT 0.0, " +
                "total_gasto REAL DEFAULT 0.0, " +
                "total_ganho REAL DEFAULT 0.0, " +
                "total_vendas INTEGER DEFAULT 0, " +
                "total_compras INTEGER DEFAULT 0, " +
                "max_sales INTEGER DEFAULT 5, " +
                "last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ");";

        // 2. Tabela de market
        String createMarketItems = "CREATE TABLE IF NOT EXISTS market_items (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "entry_id TEXT UNIQUE NOT NULL, " +
                "type TEXT NOT NULL CHECK (type IN ('server', 'player')), " +
                "seller_uuid TEXT, " +
                "seller_name TEXT, " +
                "item_stack_nbt TEXT NOT NULL, " +
                "quantity INT NOT NULL, " +
                "base_price REAL NOT NULL, " +
                "current_price REAL NOT NULL, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "is_active BOOLEAN DEFAULT TRUE" +
                ");";

        // 3. Histórico de vendas
        String createSalesHistory = "CREATE TABLE IF NOT EXISTS sales_history (" +
                "id IDENTITY PRIMARY KEY, " +
                "item_stack_nbt TEXT NOT NULL, " +
                "quantity INT NOT NULL, " +
                "price REAL NOT NULL, " +
                "seller_uuid TEXT, " +
                "seller_name TEXT, " +
                "transaction_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ");";

        // 4. Tabela de inflação
        String createInflationTable = "CREATE TABLE IF NOT EXISTS item_inflation (" +
                "item_nbt TEXT PRIMARY KEY, " +
                "base_price REAL NOT NULL, " +
                "current_price REAL NOT NULL, " +
                "purchases_last_period INT DEFAULT 0, " +
                "last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ");";

        Statement stmt = conn.createStatement();
        stmt.execute(createPlayerBalances);
        stmt.execute(createMarketItems);
        stmt.execute(createSalesHistory);
        stmt.execute(createInflationTable);
        stmt.close();


    }

    // ========== MÉTODOS PARA PLAYER BALANCES ==========
    public static double getPlayerBalance(String uuid) {
        return getAllPlayerBalances().getOrDefault(UUID.fromString(uuid), 0.0);
    }
    public static void addPlayerBalance(String uuid, String name, double amount) {
        if (!isInitialized) initialize();
        try {
            double currentBalance = DatabaseManager.getPlayerBalance(uuid);
            DatabaseManager.setPlayerBalance(uuid, name, currentBalance + amount);
        } catch (Exception e) {
        }
    }

    public static void setPlayerBalance(String uuid, String playerName, double balance) {
        if (!isInitialized) initialize();
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String upsert = "MERGE INTO player_balances USING (VALUES(?, ?, ?, COALESCE((SELECT max_sales FROM player_balances WHERE uuid = ?), 5), CURRENT_TIMESTAMP)) " +
                    "AS vals(uuid, player_name, balance, max_sales, last_updated) " +
                    "ON player_balances.uuid = vals.uuid " +
                    "WHEN MATCHED THEN UPDATE SET player_name = vals.player_name, balance = vals.balance, max_sales = vals.max_sales, last_updated = vals.last_updated " +
                    "WHEN NOT MATCHED THEN INSERT (uuid, player_name, balance, max_sales, last_updated) VALUES (vals.uuid, vals.player_name, vals.balance, vals.max_sales, vals.last_updated);";
            PreparedStatement pstmt = conn.prepareStatement(upsert);
            pstmt.setString(1, uuid);
            pstmt.setString(2, playerName == null ? "" : playerName);
            pstmt.setDouble(3, balance);
            pstmt.setString(4, uuid);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            InfinityNexusMarket.LOGGER.error("Erro ao definir saldo do jogador", e);
        }
    }

    public static Map<UUID, Double> getAllPlayerBalances() {
        if (!isInitialized) initialize();
        Map<UUID, Double> balances = new HashMap<>();
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String select = "SELECT uuid, balance FROM player_balances;";
            ResultSet rs = conn.createStatement().executeQuery(select);
            while (rs.next()) {
                try {
                    balances.put(UUID.fromString(rs.getString("uuid")), rs.getDouble("balance"));
                } catch (IllegalArgumentException e) {
                    InfinityNexusMarket.LOGGER.warn("UUID inválido encontrado: " + rs.getString("uuid"));
                }
            }
        } catch (SQLException e) {
            InfinityNexusMarket.LOGGER.error("Erro ao buscar saldos", e);
        }
        return balances;
    }

    public static MarketItemEntry getMarketItemByStackAndPrice(ItemStack itemStack, double price, String sellerName, boolean randomSeller, String ownerName) {
        if (!isInitialized) initialize();

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
    }


    // ========== MÉTODOS UNIFICADOS PARA MARKET ITEMS ==========

    public static class MarketItemEntry {
        public int id;
        public String entryId;
        public String type; // "player" ou "server"
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

    /**
     * Adiciona ou atualiza um item no mercado
     */
    public static boolean addOrUpdateMarketItem(String entryId, String type, String sellerUUID, String sellerName, ItemStack item, int quantity, double basePrice, double currentPrice) {
        if (!isInitialized) initialize();
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            // Serializa o item para comparar NBT
            ItemStack copy = item.copy();
            copy.setCount(1);
            String itemNbt = serializeItemStack(copy);

            // Verifica se já existe um item ativo com o mesmo NBT E MESMO TIPO
            PreparedStatement checkStmt = conn.prepareStatement(
                    "SELECT entry_id, quantity FROM market_items " +
                            "WHERE item_stack_nbt = ? AND type = ? AND is_active = TRUE LIMIT 1");
            checkStmt.setString(1, itemNbt);
            checkStmt.setString(2, type);
            ResultSet rs = checkStmt.executeQuery();

            if (rs.next()) {
                String existingEntryId = rs.getString("entry_id");

                // Se for um item do servidor, atualiza o preço
                if ("server".equals(type)) {
                    PreparedStatement updateStmt = conn.prepareStatement(
                            "UPDATE market_items SET base_price = ?, current_price = ?, last_updated = CURRENT_TIMESTAMP " +
                                    "WHERE entry_id = ?");
                    updateStmt.setDouble(1, basePrice);
                    updateStmt.setDouble(2, currentPrice);
                    updateStmt.setString(3, existingEntryId);
                    return updateStmt.executeUpdate() > 0;
                }
                // Se for um item de player com mesmo vendedor e preço, soma a quantidade
                else if ("player".equals(type)) {
                    // Verifica se é do mesmo vendedor e mesmo preço
                    PreparedStatement checkPlayerStmt = conn.prepareStatement(
                            "SELECT entry_id, quantity FROM market_items " +
                                    "WHERE entry_id = ? AND seller_uuid = ? " +
                                    "AND base_price = ? AND current_price = ?");
                    checkPlayerStmt.setString(1, existingEntryId);
                    checkPlayerStmt.setString(2, sellerUUID);
                    checkPlayerStmt.setDouble(3, basePrice);
                    checkPlayerStmt.setDouble(4, currentPrice);
                    ResultSet playerRs = checkPlayerStmt.executeQuery();

                    if (playerRs.next()) {
                        int existingQuantity = playerRs.getInt("quantity");
                        PreparedStatement updateStmt = conn.prepareStatement(
                                "UPDATE market_items SET quantity = ?, last_updated = CURRENT_TIMESTAMP " +
                                        "WHERE entry_id = ?");
                        updateStmt.setInt(1, existingQuantity + quantity);
                        updateStmt.setString(2, existingEntryId);
                        return updateStmt.executeUpdate() > 0;
                    }
                }
            }

            // Nenhum item igual encontrado ou não atende aos critérios de merge, insere novo
            String upsert = "INSERT INTO market_items " +
                    "(entry_id, type, seller_uuid, seller_name, item_stack_nbt, quantity, base_price, current_price, last_updated) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)";

            PreparedStatement pstmt = conn.prepareStatement(upsert);
            pstmt.setString(1, entryId);
            pstmt.setString(2, type);
            pstmt.setString(3, "server".equals(type) ? SERVER_UUID.toString() : sellerUUID);
            pstmt.setString(4, "server".equals(type) ? "Server" : sellerName);
            pstmt.setString(5, itemNbt);
            pstmt.setInt(6, "server".equals(type) ? 64 : quantity);
            pstmt.setDouble(7, basePrice);
            pstmt.setDouble(8, currentPrice);

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            InfinityNexusMarket.LOGGER.error("Erro ao adicionar/atualizar item", e);
            return false;
        }
    }

    /**
     * Obtém todos os itens do mercado, com filtro opcional por tipo
     */
    public static List<MarketItemEntry> getMarketItems(String typeFilter) {
        if (!isInitialized) initialize();
        List<MarketItemEntry> items = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String query = "SELECT * FROM market_items WHERE is_active = TRUE";
            if (typeFilter != null) {
                query += " AND type = '" + typeFilter + "'";
            }
            query += " ORDER BY last_updated DESC;";

            ResultSet rs = conn.createStatement().executeQuery(query);
            while (rs.next()) {
                MarketItemEntry entry = new MarketItemEntry();
                entry.id = rs.getInt("id");
                entry.entryId = rs.getString("entry_id");
                entry.type = rs.getString("type");
                entry.sellerUUID = rs.getString("seller_uuid");
                entry.sellerName = rs.getString("seller_name");
                entry.itemId = rs.getString("item_stack_nbt");
                entry.itemName = ""; // Preencher depois com o nome real do item
                entry.itemNbt = rs.getString("item_stack_nbt");
                entry.quantity = rs.getInt("quantity");
                entry.basePrice = rs.getDouble("base_price");
                entry.currentPrice = rs.getDouble("current_price");
                entry.isActive = rs.getBoolean("is_active");
                entry.createdAt = rs.getString("created_at");
                entry.lastUpdated = rs.getString("last_updated");
                items.add(entry);
            }
        } catch (SQLException e) {
            InfinityNexusMarket.LOGGER.error("Erro ao buscar itens", e);
        }
        return items;
    }
    /**
     * Obtém um item do mercado pelo seu entry_id
     */
    public static MarketItemEntry getMarketItemByEntryId(String entryId) {
        if (!isInitialized) initialize();
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String query = "SELECT * FROM market_items WHERE entry_id = ? AND is_active = TRUE LIMIT 1;";
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setString(1, entryId);

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                MarketItemEntry entry = new MarketItemEntry();
                entry.id = rs.getInt("id");
                entry.entryId = rs.getString("entry_id");
                entry.type = rs.getString("type");
                entry.sellerUUID = rs.getString("seller_uuid");
                entry.sellerName = rs.getString("seller_name");
                entry.itemId = rs.getString("item_stack_nbt");
                entry.itemName = ""; // Preencher depois com o nome real do item
                entry.itemNbt = rs.getString("item_stack_nbt");
                entry.quantity = rs.getInt("quantity");
                entry.basePrice = rs.getDouble("base_price");
                entry.currentPrice = rs.getDouble("current_price");
                entry.isActive = rs.getBoolean("is_active");
                entry.createdAt = rs.getString("created_at");
                entry.lastUpdated = rs.getString("last_updated");
                return entry;
            }
        } catch (SQLException e) {
            InfinityNexusMarket.LOGGER.error("Erro ao buscar item por entry_id", e);
        }
        return null;
    }

    /**
     * Remove um item do mercado (marca como inativo)
     */
    public static boolean removeMarketItem(String entryId) {
        if (!isInitialized) initialize();
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            PreparedStatement pstmt = conn.prepareStatement(
                    "UPDATE market_items SET is_active = FALSE WHERE entry_id = ?;");
            pstmt.setString(1, entryId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            InfinityNexusMarket.LOGGER.error("Erro ao remover item", e);
            return false;
        }
    }

    // ========== MÉTODOS DE CONVENIÊNCIA ==========

    public static boolean addPlayerSale(String sellerUUID, String sellerName,
                                        ItemStack item, int quantity, double price) {
        if (!canPlayerAddMoreSales(sellerUUID)) {
            InfinityNexusMarket.LOGGER.warn("Jogador {} atingiu o limite máximo de vendas ({}/{})",
                    sellerName, getPlayerCurrentSalesCount(sellerUUID), getPlayerMaxSales(sellerUUID));
            return false;
        }

        return addOrUpdateMarketItem(
                UUID.randomUUID().toString(),
                "player",
                sellerUUID,
                sellerName,
                item,
                quantity,
                price,
                price
        );
    }

    public static boolean addOrUpdateServerItem(String entryId, ItemStack item, double basePrice, double currentPrice) {
        return addOrUpdateMarketItem(
                entryId,
                "server",
                SERVER_UUID.toString(),
                "Server",
                item,
                64,
                basePrice,
                currentPrice
        );
    }

    public static List<MarketItemEntry> getAllPlayerSales() {
        return getMarketItems("player");
    }

    public static List<MarketItemEntry> getAllServerItems() {
        return getMarketItems("server");
    }

    public static List<MarketItemEntry> getAllMarketItems() {
        return getMarketItems(null);
    }

    // ========== MÉTODOS PARA SALES HISTORY ==========
    public static boolean addSalesHistory(String itemId, int quantity, double price, String sellerUUID, String sellerName) {
        if (!isInitialized) initialize();
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            // 1. Registra a transação no histórico
            PreparedStatement pstmt = conn.prepareStatement(
                    "INSERT INTO sales_history (item_stack_nbt, quantity, price, seller_uuid, seller_name) " +
                            "VALUES (?, ?, ?, ?, ?);");
            pstmt.setString(1, itemId);
            pstmt.setInt(2, quantity);
            pstmt.setDouble(3, price);
            pstmt.setString(4, sellerUUID);
            pstmt.setString(5, sellerName);
            boolean success = pstmt.executeUpdate() > 0;
            applyServerInflation(itemId, quantity);
            return success;
        } catch (SQLException e) {
            InfinityNexusMarket.LOGGER.error("Erro ao adicionar histórico", e);
            return false;
        }
    }

    private static boolean isServerItem(String itemNbt) {
        if (!isInitialized) initialize();
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            PreparedStatement pstmt = conn.prepareStatement(
                    "SELECT 1 FROM market_items WHERE item_stack_nbt = ? AND type = 'server' LIMIT 1;");
            pstmt.setString(1, itemNbt);
            return pstmt.executeQuery().next();
        } catch (SQLException e) {
            InfinityNexusMarket.LOGGER.error("Erro ao verificar item do servidor", e);
            return false;
        }
    }

    public static Map<String, Integer> getSalesHistoryAndReset() {
        if (!isInitialized) initialize();
        Map<String, Integer> sales = new HashMap<>();
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            ResultSet rs = conn.createStatement().executeQuery(
                    "SELECT item_stack_nbt, SUM(quantity) as total_quantity FROM sales_history GROUP BY item_stack_nbt;");
            while (rs.next()) {
                sales.put(rs.getString("item_stack_nbt"), rs.getInt("total_quantity"));
            }
            conn.createStatement().execute("DELETE FROM sales_history;");
        } catch (SQLException e) {
            InfinityNexusMarket.LOGGER.error("Erro ao buscar histórico", e);
        }
        return sales;
    }

    // ========== MÉTODOS DE UTILIDADE ==========

    // (Mantidos iguais ao original)
    public static String serializeItemStack(ItemStack item) {
        if (item == null || item.isEmpty()) return "";
        JsonElement json = ItemStack.CODEC.encodeStart(RegistryOps.create(JsonOps.INSTANCE, InfinityNexusMarket.serverLevel.registryAccess()), item).getOrThrow();
        return json.toString();
    }

    public static ItemStack deserializeItemStack(String jsonString) {
        try {
            if (jsonString == null || jsonString.isEmpty()) return ItemStack.EMPTY;
            return ItemStack.CODEC.parse(RegistryOps.create(JsonOps.INSTANCE, InfinityNexusMarket.serverLevel.registryAccess()), JsonParser.parseString(jsonString)).getOrThrow();
        } catch (Exception e) {
            InfinityNexusMarket.LOGGER.error("Erro ao deserializar ItemStack", e);
            return ItemStack.EMPTY;
        }
    }

    // ========== OUTROS MÉTODOS ==========
    // (Mantidos iguais ao original)
    public static void cleanCorruptedData() {
        if (!isInitialized) initialize();
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            int deleted = conn.createStatement().executeUpdate(
                    "DELETE FROM market_items WHERE entry_id NOT LIKE '%-%-%-%-%' OR entry_id LIKE 'player_%' OR entry_id LIKE 'server_%';");
            if (deleted > 0) {
                InfinityNexusMarket.LOGGER.info("Removidos " + deleted + " registros corrompidos");
            }
        } catch (SQLException e) {
            InfinityNexusMarket.LOGGER.error("Erro ao limpar dados", e);
        }
    }

    public static int cleanExpiredSales(int daysToExpire) {
        if (!isInitialized) initialize();
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            int deleted = conn.createStatement().executeUpdate(
                    "DELETE FROM market_items WHERE ... AND created_at < DATEADD('DAY', -" + daysToExpire + ", CURRENT_TIMESTAMP)");

            if (deleted > 0) {
                InfinityNexusMarket.LOGGER.info("Removidas " + deleted + " vendas expiradas");
            }
            return deleted;
        } catch (SQLException e) {
            InfinityNexusMarket.LOGGER.error("Erro ao limpar vendas", e);
            return 0;
        }
    }

    public static void compactDatabase() {
        if (!isInitialized) initialize();
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            conn.createStatement().execute("SHUTDOWN DEFRAG;");
            InfinityNexusMarket.LOGGER.info("Database compactada");
        } catch (SQLException e) {
            InfinityNexusMarket.LOGGER.error("Erro ao compactar", e);
        }
    }

    public static String getSellerNameByEntryId(String entryId) {
        if (!isInitialized) initialize();
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            PreparedStatement pstmt = conn.prepareStatement(
                    "SELECT seller_name FROM market_items WHERE entry_id = ? LIMIT 1;");
            pstmt.setString(1, entryId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getString("seller_name");
        } catch (SQLException e) {
            InfinityNexusMarket.LOGGER.error("Erro ao buscar vendedor", e);
        }
        return null;
    }
    public static String getSellerNameByEntryUUID(String entryId) {
        if (!isInitialized) initialize();
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            PreparedStatement pstmt = conn.prepareStatement(
                    "SELECT seller_name FROM market_items WHERE seller_uuid = ? LIMIT 1;");
            pstmt.setString(1, entryId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getString("seller_name");
        } catch (SQLException e) {
            InfinityNexusMarket.LOGGER.error("Erro ao buscar vendedor", e);
        }
        return null;
    }

    public static void incrementPlayerStats(String uuid, double gasto, double ganho, int vendas, int compras) {
        if (!isInitialized) initialize();
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            PreparedStatement pstmt = conn.prepareStatement(
                    "UPDATE player_balances SET total_gasto = total_gasto + ?, total_ganho = total_ganho + ?, " +
                            "total_vendas = total_vendas + ?, total_compras = total_compras + ?, last_updated = CURRENT_TIMESTAMP " +
                            "WHERE uuid = ?;");
            pstmt.setDouble(1, gasto);
            pstmt.setDouble(2, ganho);
            pstmt.setInt(3, vendas);
            pstmt.setInt(4, compras);
            pstmt.setString(5, uuid);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            InfinityNexusMarket.LOGGER.error("Erro ao atualizar estatísticas", e);
        }
    }
    // ========== MÉTODOS PARA LIMITE DE VENDAS ==========

    public static int getPlayerMaxSales(String uuid) {
        if(uuid.equals(SERVER_UUID.toString())){
            return 500;
        }
        if (!isInitialized) initialize();
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            PreparedStatement pstmt = conn.prepareStatement(
                    "SELECT max_sales FROM player_balances WHERE uuid = ?");
            pstmt.setString(1, uuid);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getInt("max_sales");
        } catch (SQLException e) {
            InfinityNexusMarket.LOGGER.error("Erro ao buscar limite de vendas", e);
        }
        return 5; // Valor padrão caso não encontre
    }

    public static void setPlayerMaxSales(String uuid, int maxSales) {
        if (!isInitialized) initialize();
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            // Garante que o jogador existe na tabela
            setPlayerBalance(uuid, null, getPlayerBalance(uuid));

            PreparedStatement pstmt = conn.prepareStatement(
                    "UPDATE player_balances SET max_sales = ? WHERE uuid = ?");
            pstmt.setInt(1, maxSales);
            pstmt.setString(2, uuid);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            InfinityNexusMarket.LOGGER.error("Erro ao definir limite de vendas", e);
        }
    }

    public static int getPlayerCurrentSalesCount(String uuid) {
        if (!isInitialized) initialize();
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            PreparedStatement pstmt = conn.prepareStatement(
                    "SELECT COUNT(*) as count FROM market_items " +
                            "WHERE type = 'player' AND seller_uuid = ? AND is_active = TRUE");
            pstmt.setString(1, uuid);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getInt("count");
        } catch (SQLException e) {
            InfinityNexusMarket.LOGGER.error("Erro ao contar vendas do jogador", e);
        }
        return 0;
    }

    public static boolean canPlayerAddMoreSales(String uuid) {
        int current = getPlayerCurrentSalesCount(uuid);
        int max = getPlayerMaxSales(uuid);
        return current < max;
    }

    public static void applyServerInflation(String itemNbt, int quantityPurchased) {
        if (!isInitialized) initialize();
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String baseItemId = extractBaseItemId(itemNbt);
            if (!isServerItemByBaseId(baseItemId)) return;

            // Restante do método permanece igual, usando baseItemId em vez de itemNbt
            String query = "INSERT INTO item_inflation (item_nbt, base_price, current_price, purchases_last_period, last_updated) " +
                    "VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP) " +
                    "ON CONFLICT(item_nbt) DO UPDATE SET " +
                    "purchases_last_period = purchases_last_period + ?;";

            double basePrice = getBasePriceForItemByBaseId(baseItemId);
            double currentPrice = getCurrentPriceForItem(baseItemId);

            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setString(1, baseItemId);
            pstmt.setDouble(2, basePrice);
            pstmt.setDouble(3, currentPrice);
            pstmt.setInt(4, quantityPurchased);
            pstmt.setInt(5, quantityPurchased);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            InfinityNexusMarket.LOGGER.error("Erro ao registrar compra para inflação", e);
        }
    }
    private static boolean isServerItemByBaseId(String baseItemId) {
        if (!isInitialized) initialize();
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            PreparedStatement pstmt = conn.prepareStatement(
                    "SELECT 1 FROM market_items WHERE " +
                            "item_stack_nbt LIKE ? AND type = 'server' LIMIT 1;");
            pstmt.setString(1, "%" + baseItemId + "%");
            return pstmt.executeQuery().next();
        } catch (SQLException e) {
            InfinityNexusMarket.LOGGER.error("Erro ao verificar item do servidor", e);
            return false;
        }
    }

    private static double getBasePriceForItemByBaseId(String baseItemId) {
        if (!isInitialized) initialize();
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            PreparedStatement pstmt = conn.prepareStatement(
                    "SELECT MIN(base_price) FROM market_items " +
                            "WHERE item_stack_nbt LIKE ? AND type = 'server';");
            pstmt.setString(1, "%\"" + baseItemId + "\"%");
            ResultSet rs = pstmt.executeQuery();
            return rs.next() ? rs.getDouble(1) : 0;
        } catch (SQLException e) {
            InfinityNexusMarket.LOGGER.error("Erro ao buscar preço base", e);
            return 0;
        }
    }
    /**
     * Obtém o preço ATUAL de um item (com inflação aplicada)
     */
    public static double getCurrentPriceForItem(String itemNbt) {
        if (!isInitialized) initialize();
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            // 1. Tenta buscar da tabela de inflação (se o item já teve variação)
            PreparedStatement pstmt = conn.prepareStatement(
                    "SELECT current_price FROM item_inflation WHERE item_nbt = ?;");
            pstmt.setString(1, itemNbt);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getDouble("current_price");
            }

            // 2. Se não existir na inflação, busca o preço base do mercado
            return getBasePriceForItem(itemNbt);

        } catch (SQLException e) {
            InfinityNexusMarket.LOGGER.error("Erro ao buscar preço atual", e);
            return 0; // Valor padrão de fallback
        }
    }

    public static void updateInflationPrices() {
        if (!isInitialized) initialize();
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            // 1. Processa todos os itens com inflação
            ResultSet rs = conn.createStatement().executeQuery(
                    "SELECT item_nbt, base_price, current_price, purchases_last_period FROM item_inflation;");

            while (rs.next()) {
                String itemNbt = rs.getString("item_nbt");
                double basePrice = rs.getDouble("base_price");
                double currentPrice = rs.getDouble("current_price");
                int purchases = rs.getInt("purchases_last_period");

                // 2. Calcula variação
                double variation = 0;
                if (purchases >= 1000) {
                    variation = 0.0005; // +0.05%
                } else if (purchases == 0) {
                    variation = -0.0005; // -0.05%
                }

                // 3. Aplica variação com limites
                double newPrice = currentPrice * (1 + variation);
                newPrice = Math.max(basePrice * 0.8, Math.min(basePrice * 1.2, newPrice));

                // 4. Atualiza no banco
                PreparedStatement pstmt = conn.prepareStatement(
                        "UPDATE item_inflation SET current_price = ?, purchases_last_period = 0 WHERE item_nbt = ?;");
                pstmt.setDouble(1, newPrice);
                pstmt.setString(2, itemNbt);
                pstmt.executeUpdate();

                // 5. Atualiza no mercado
                updateMarketItemPrice(itemNbt, newPrice);
            }
        } catch (SQLException e) {
            InfinityNexusMarket.LOGGER.error("Erro ao atualizar inflação", e);
        }
    }
    private static String extractBaseItemId(String itemNbt) {
        try {
            JsonElement json = JsonParser.parseString(itemNbt);
            if (json.isJsonObject()) {
                JsonElement idElement = json.getAsJsonObject().get("id");
                if (idElement != null) {
                    return idElement.getAsString(); // Retorna "minecraft:item_id"
                }
            }
        } catch (Exception e) {
            InfinityNexusMarket.LOGGER.error("Erro ao extrair ID base do item", e);
        }
        return itemNbt;
    }
    private static double getBasePriceForItem(String itemNbt) {
        if (!isInitialized) initialize();
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            PreparedStatement pstmt = conn.prepareStatement(
                    "SELECT base_price FROM market_items " +
                            "WHERE item_stack_nbt = ? AND type = 'server' LIMIT 1;");
            pstmt.setString(1, itemNbt);
            ResultSet rs = pstmt.executeQuery();
            return rs.next() ? rs.getDouble("base_price") : 0;
        } catch (SQLException e) {
            InfinityNexusMarket.LOGGER.error("Erro ao buscar preço base", e);
            return 0;
        }
    }

    private static void updateMarketItemPrice(String baseItemId, double newPrice) {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            PreparedStatement pstmt = conn.prepareStatement(
                    "UPDATE market_items SET current_price = ? " +
                            "WHERE item_stack_nbt LIKE ? AND type = 'server';");
            pstmt.setDouble(1, newPrice);
            pstmt.setString(2, "%\"" + baseItemId + "\"%");
            pstmt.executeUpdate();
        } catch (SQLException e) {
            InfinityNexusMarket.LOGGER.error("Erro ao atualizar preço no mercado", e);
        }
    }

    public static int getUpdatedItemsCount() {
        if (!isInitialized) initialize();
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            ResultSet rs = conn.createStatement().executeQuery(
                    "SELECT COUNT(*) FROM item_inflation WHERE purchases_last_period > 0;");
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            InfinityNexusMarket.LOGGER.error("Erro ao contar itens atualizados", e);
            return 0;
        }
    }
}