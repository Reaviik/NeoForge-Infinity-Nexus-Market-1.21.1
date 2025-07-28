package com.Infinity.Nexus.Market.market;

import com.Infinity.Nexus.Market.InfinityNexusMarket;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

public class SQLiteManager {
    private static final UUID SERVER_UUID = UUID.fromString("00000000-0000-0000-0000-00000000c0de");
    private static final Map<UUID, Double> playerBalancesCache = new HashMap<>();
    private static final List<MarketItemEntry> marketItemsCache = new ArrayList<>();
    private static long lastCacheUpdate = 0;
    private static final long CACHE_TTL = 5 * 60 * 1000; // 5 minutos

    private static final String DB_URL = "jdbc:sqlite:" + new File("config/infinity_nexus_market/market.db").getAbsolutePath();
    private static boolean isInitialized = false;

    public static void initialize() {
        if (isInitialized) {
            return;
        }

        InfinityNexusMarket.LOGGER.info("Inicializando SQLite database...");

        try {
            // Garante que o diretório existe
            new File("config/infinity_nexus_market").mkdirs();

            // Carrega o driver SQLite
            try {
                Class.forName("org.sqlite.JDBC").getDeclaredConstructor().newInstance();
                InfinityNexusMarket.LOGGER.info("SQLite driver carregado com sucesso!");
            } catch (Exception e) {
                InfinityNexusMarket.LOGGER.error("Falha ao carregar o driver SQLite. Verifique se a dependência sqlite-jdbc está no classpath.", e);
                return;
            }

            // Testa a conexão e cria as tabelas
            try (Connection conn = DriverManager.getConnection(DB_URL)) {
                InfinityNexusMarket.LOGGER.info("Conectado ao banco SQLite: {}", DB_URL);
                createAllTables(conn);
                isInitialized = true;
                InfinityNexusMarket.LOGGER.info("Todas as tabelas criadas/verificadas com sucesso!");
            } catch (SQLException e) {
                InfinityNexusMarket.LOGGER.error("Falha ao conectar ou criar tabelas no banco SQLite", e);
            }

        } catch (Exception e) {
            InfinityNexusMarket.LOGGER.error("Erro durante inicialização do SQLite", e);
        }
    }

    private static void createAllTables(Connection conn) throws SQLException {
        // 1. Tabela de saldos dos jogadores (agora com estatísticas)
        String createPlayerBalances = "CREATE TABLE IF NOT EXISTS player_balances (" +
                "uuid TEXT PRIMARY KEY, " +
                "player_name TEXT NOT NULL, " +
                "balance REAL DEFAULT 0.0, " +
                "total_gasto REAL DEFAULT 0.0, " +
                "total_ganho REAL DEFAULT 0.0, " +
                "total_vendas INTEGER DEFAULT 0, " +
                "total_compras INTEGER DEFAULT 0, " +
                "max_sales INTEGER DEFAULT 5, " +
                "last_updated DATETIME DEFAULT CURRENT_TIMESTAMP" +
                ");";

        // 2. Tabela unificada de market (server + player)
        String createMarketItems = "CREATE TABLE IF NOT EXISTS market_items (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "entry_id TEXT UNIQUE NOT NULL, " +
                "type TEXT NOT NULL CHECK (type IN ('server', 'player')), " +
                "seller_uuid TEXT, " + // NULL para server, UUID do jogador para player
                "seller_name TEXT, " + // NULL para server, nome do jogador para player
                "item_stack_nbt TEXT NOT NULL, " +
                "quantity INTEGER NOT NULL, " + // 0 para server (estoque infinito), quantidade real para player
                "base_price REAL NOT NULL, " +
                "current_price REAL NOT NULL, " +
                "created_at DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                "last_updated DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                "is_active INTEGER DEFAULT 1" +
                ");";

        // 3. Tabela de histórico de vendas
        String createSalesHistory = "CREATE TABLE IF NOT EXISTS sales_history (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "item_stack_nbt TEXT NOT NULL, " +
                "quantity INTEGER NOT NULL, " +
                "price REAL NOT NULL, " +
                "seller_uuid TEXT, " +
                "seller_name TEXT, " +
                "transaction_date DATETIME DEFAULT CURRENT_TIMESTAMP" +
                ");";

        // Executa todas as criações
        conn.createStatement().execute(createPlayerBalances);
        conn.createStatement().execute(createMarketItems);
        conn.createStatement().execute(createSalesHistory);

        // Verifica e adiciona colunas faltantes (migrações)
        try {
            // Verifica se a coluna max_sales existe
            conn.createStatement().executeQuery("SELECT max_sales FROM player_balances LIMIT 1");
        } catch (SQLException e) {
            // Se não existir, adiciona
            conn.createStatement().execute("ALTER TABLE player_balances ADD COLUMN max_sales INTEGER DEFAULT 5");
            InfinityNexusMarket.LOGGER.info("Adicionada coluna max_sales à tabela player_balances");
        }

        // Adicione verificações semelhantes para outras colunas que você pode adicionar no futuro
        try {
            conn.createStatement().executeQuery("SELECT seller_uuid FROM sales_history LIMIT 1");
        } catch (SQLException e) {
            conn.createStatement().execute("ALTER TABLE sales_history ADD COLUMN seller_uuid TEXT");
        }

        try {
            conn.createStatement().executeQuery("SELECT seller_name FROM sales_history LIMIT 1");
        } catch (SQLException e) {
            conn.createStatement().execute("ALTER TABLE sales_history ADD COLUMN seller_name TEXT");
        }
    }

    private static void updateCacheIfNeeded() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCacheUpdate > CACHE_TTL) {
            refreshAllCaches();
            lastCacheUpdate = currentTime;
        }
    }
    private static void refreshAllCaches() {
        playerBalancesCache.clear();
        playerBalancesCache.putAll(getAllPlayerBalances());

        marketItemsCache.clear();
        marketItemsCache.addAll(getAllMarketItems());
    }


    public static List<MarketItemEntry> getCachedMarketItems(String typeFilter) {
        updateCacheIfNeeded();
        if (typeFilter == null) {
            return new ArrayList<>(marketItemsCache);
        }
        return marketItemsCache.stream()
                .filter(e -> typeFilter.equals(e.type))
                .collect(Collectors.toList());
    }

    // ========== MÉTODOS PARA PLAYER BALANCES ==========
    public static double getPlayerBalance(String uuid) {
        updateCacheIfNeeded();
        return playerBalancesCache.getOrDefault(UUID.fromString(uuid), 0.0);
    }

    public static void setPlayerBalance(String uuid, String playerName, double balance) {
        if (!isInitialized) initialize();
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String upsert = "INSERT OR REPLACE INTO player_balances (uuid, player_name, balance, max_sales, last_updated) VALUES (?, ?, ?, COALESCE((SELECT max_sales FROM player_balances WHERE uuid = ?), 5), CURRENT_TIMESTAMP);";
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
    public static boolean addOrUpdateMarketItem(String entryId, String type,
                                                String sellerUUID, String sellerName,
                                                ItemStack item, int quantity,
                                                double basePrice, double currentPrice,
                                                ServerLevel serverLevel) {
        if (!isInitialized) initialize();
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            // Serializa o item para comparar NBT
            String itemNbt = serializeItemStack(item, serverLevel);

            // Verifica se já existe um item ativo com o mesmo NBT
            PreparedStatement checkStmt = conn.prepareStatement(
                    "SELECT entry_id, type FROM market_items " +
                            "WHERE item_stack_nbt = ? AND is_active = 1 LIMIT 1");
            checkStmt.setString(1, itemNbt);
            ResultSet rs = checkStmt.executeQuery();

            if (rs.next()) {
                String existingType = rs.getString("type");
                String existingEntryId = rs.getString("entry_id");

                // Se for um item do servidor e já existir um igual, atualiza o preço
                if ("server".equals(type) && "server".equals(existingType)) {
                    PreparedStatement updateStmt = conn.prepareStatement(
                            "UPDATE market_items SET base_price = ?, current_price = ?, last_updated = CURRENT_TIMESTAMP " +
                                    "WHERE entry_id = ?");
                    updateStmt.setDouble(1, basePrice);
                    updateStmt.setDouble(2, currentPrice);
                    updateStmt.setString(3, existingEntryId);
                    return updateStmt.executeUpdate() > 0;
                }
                // Se for um item de player com mesmo NBT, preço e vendedor, soma a quantidade
                else if ("player".equals(type) && "player".equals(existingType)) {
                    // Verifica se é do mesmo vendedor e tem mesmo preço
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

            // Se não encontrou um item igual ou não é para mesclar, insere normalmente
            String upsert = "INSERT INTO market_items " +
                    "(entry_id, type, seller_uuid, seller_name, item_stack_nbt, quantity, base_price, current_price, last_updated) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP);";

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
            String query = "SELECT * FROM market_items WHERE is_active = 1";
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
                entry.isActive = rs.getInt("is_active") == 1;
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
     * Remove um item do mercado (marca como inativo)
     */
    public static boolean removeMarketItem(String entryId) {
        if (!isInitialized) initialize();
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            PreparedStatement pstmt = conn.prepareStatement(
                    "UPDATE market_items SET is_active = 0 WHERE entry_id = ?;");
            pstmt.setString(1, entryId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            InfinityNexusMarket.LOGGER.error("Erro ao remover item", e);
            return false;
        }
    }

    // ========== MÉTODOS DE CONVENIÊNCIA ==========

    public static boolean addPlayerSale(String sellerUUID, String sellerName,
                                        ItemStack item, int quantity, double price,
                                        ServerLevel serverLevel) {
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
                price,
                serverLevel
        );
    }

    public static boolean addOrUpdateServerItem(String entryId, ItemStack item,
                                                double basePrice, double currentPrice,
                                                ServerLevel serverLevel) {
        return addOrUpdateMarketItem(
                entryId,
                "server",
                SERVER_UUID.toString(),
                "Server",
                item,
                0,
                basePrice,
                currentPrice,
                serverLevel
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
    // (Mantidos iguais ao original)
    public static boolean addSalesHistory(String itemId, int quantity, double price, String sellerUUID, String sellerName) {
        if (!isInitialized) initialize();
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            PreparedStatement pstmt = conn.prepareStatement(
                    "INSERT INTO sales_history (item_stack_nbt, quantity, price, seller_uuid, seller_name) " +
                            "VALUES (?, ?, ?, ?, ?);");
            pstmt.setString(1, itemId);
            pstmt.setInt(2, quantity);
            pstmt.setDouble(3, price);
            pstmt.setString(4, sellerUUID);
            pstmt.setString(5, sellerName);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            InfinityNexusMarket.LOGGER.error("Erro ao adicionar histórico", e);
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
    private static String serializeItemStack(ItemStack item, ServerLevel level) {
        if (item == null || item.isEmpty()) return "";
        JsonElement json = ItemStack.CODEC.encodeStart(
                RegistryOps.create(JsonOps.INSTANCE, level.registryAccess()),
                item
        ).getOrThrow();
        return json.toString();
    }

    public static ItemStack deserializeItemStack(String jsonString, ServerLevel level) {
        try {
            if (jsonString == null || jsonString.isEmpty()) return ItemStack.EMPTY;
            return ItemStack.CODEC.parse(
                    RegistryOps.create(JsonOps.INSTANCE, level.registryAccess()),
                    JsonParser.parseString(jsonString)
            ).getOrThrow();
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
                    "DELETE FROM market_items WHERE type = 'player' AND is_active = 1 AND created_at < datetime('now', '-" + daysToExpire + " days');");
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
            conn.createStatement().execute("VACUUM;");
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
                            "WHERE type = 'player' AND seller_uuid = ? AND is_active = 1");
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
}