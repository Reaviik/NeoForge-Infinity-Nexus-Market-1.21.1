package com.Infinity.Nexus.Market.config;

import com.Infinity.Nexus.Market.InfinityNexusMarket;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = InfinityNexusMarket.MOD_ID)
public class ModConfigs {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    //-----------------------------------------------MARKET-----------------------------------------------//
    //Prefix
    private static final ModConfigSpec.ConfigValue<String> PREFIX = BUILDER.comment("Defines the message prefix").define("prefix", "§f[§bMarket§f]: ");
    //Misc
    private static final ModConfigSpec.ConfigValue<Boolean> DEBUG = BUILDER.comment("Defines if the mod is in debug mode").define("debug_mode",false);
    private static final ModConfigSpec.ConfigValue<Boolean> NOTIFY_BALL_TOP = BUILDER.comment("Defines if send notification to the top of the ball change").define("notify_ball_top",false);
    private static final ModConfigSpec.DoubleValue MIN_PRICE_PERCENTAGE = BUILDER.comment("Define the minimum price percentage of the item prince can player sell above server items price (ex: 0.5 = 50%), 1.0 same as server, this value make server auto buy player items if match").defineInRange("minimum_price_percentage", 0.5, 0.001, 1.0);
    private static final ModConfigSpec.ConfigValue<Boolean> LOTTERY = BUILDER.comment("Defines if the lottery is enabled, this occurs once a week at 15:50 in Sanday").define("lottery_enabled",true);
    private static final ModConfigSpec.IntValue TOP_BALANCE_INTERVAL = BUILDER.comment("Defines the interval in minutes to check the top balances. Set to 0 to disable").defineInRange("top_balance_interval", 15, 1, 120);
    //Database
    private static final ModConfigSpec.ConfigValue<String> DB_IP = BUILDER.comment("IP do banco de dados").define("db_ip", "127.0.0.1");
    private static final ModConfigSpec.IntValue DB_PORT = BUILDER.comment("Porta do banco de dados").defineInRange("db_port", 3306, 1, 65535);
    private static final ModConfigSpec.ConfigValue<String> DB_NAME = BUILDER.comment("Nome do banco de dados").define("db_name", "s4_loja");
    private static final ModConfigSpec.ConfigValue<String> DB_USERNAME = BUILDER.comment("Usuário do banco de dados").define("db_username", "usuario");
    private static final ModConfigSpec.ConfigValue<String> DB_PASSWORD = BUILDER.comment("Senha do banco de dados").define("db_password", "senha");
    //ENERGY Vending
    private static final ModConfigSpec.IntValue VENDING_ENERGY = BUILDER.comment("Defines the amount of energy that the Vending will store").defineInRange("vending_energy_capacity", 150000, 1, Integer.MAX_VALUE);
    private static final ModConfigSpec.IntValue VENDING_ENERGY_TRANSFER = BUILDER.comment("Defines the amount of energy that the Vending will transfer").defineInRange("vending_energy_transfer", 100000, 1, Integer.MAX_VALUE);
    private static final ModConfigSpec.IntValue VENDING_ENERGY_COST_PER_OPERATION = BUILDER.comment("Defines the amount of energy that the Vending will consume per operation").defineInRange("vending_energy_cost_per_operation", 100, 1, Integer.MAX_VALUE);
    //Deny List
    private static final ModConfigSpec.ConfigValue<List<? extends String>> VENDING_ITEM_BLACKLIST = BUILDER
            .comment("List of items/tags that can't be sold by the vending machine")
            .defineList("vending_id_blacklist", List.of("minecraft:air","#c:shulker_boxes"),
                    o -> o instanceof String
            );

    //ENERGY Buying
    private static final ModConfigSpec.IntValue BUYING_ENERGY = BUILDER.comment("Defines the amount of energy that the Buying will store").defineInRange("buying_energy_capacity", 150000, 1, Integer.MAX_VALUE);
    private static final ModConfigSpec.IntValue BUYING_ENERGY_TRANSFER = BUILDER.comment("Defines the amount of energy that the Buying will transfer").defineInRange("buying_energy_transfer", 100000, 1, Integer.MAX_VALUE);
    private static final ModConfigSpec.IntValue BUYING_ENERGY_COST_PER_OPERATION = BUILDER.comment("Defines the amount of energy that the Buying will consume per operation").defineInRange("buying_energy_cost_per_operation", 100, 1, Integer.MAX_VALUE);
    // TICK
    private static final ModConfigSpec.IntValue TICKS_PER_OPERATION = BUILDER.comment("Defines how many ticks the buying/vending machine will make each operation").defineInRange("ticks_per_operation", 30, 1, Integer.MAX_VALUE);

    // INFLAÇÃO DINÂMICA SERVER MARKET
    private static final ModConfigSpec.BooleanValue INFLATION_ENABLED = BUILDER.comment("Ativa/desativa o sistema de inflação dinâmica do market do server").define("inflation_enabled", true);
    private static final ModConfigSpec.BooleanValue INFLATION_NOTIFY = BUILDER.comment("Ativa/desativa notificações de inflação para jogadores").define("inflation_notify", true);
    private static final ModConfigSpec.IntValue INFLATION_INTERVAL_MIN = BUILDER.comment("Intervalo do ciclo de inflação em minutos").defineInRange("inflation_interval_min", 30, 1, 1440);
    private static final ModConfigSpec.DoubleValue INFLATION_MAX_VARIATION = BUILDER.comment("Variação máxima por ciclo (ex: 0.2 = 20%)").defineInRange("inflation_max_variation", 0.2, 0.01, 1.0);
    private static final ModConfigSpec.DoubleValue INFLATION_MIN_MULT = BUILDER.comment("Mínimo multiplicador do preço base (ex: 0.5 = 50%)").defineInRange("inflation_min_mult", 0.5, 0.01, 1.0);
    private static final ModConfigSpec.DoubleValue INFLATION_MAX_MULT = BUILDER.comment("Máximo multiplicador do preço base (ex: 2.0 = 200%)").defineInRange("inflation_max_mult", 2.0, 1.0, 10.0);
    private static final ModConfigSpec.DoubleValue INFLATION_NOTIFY_PERCENT = BUILDER.comment("Percentual mínimo de variação para notificação (ex: 0.05 = 5%)").defineInRange("inflation_notify_percent", 0.05, 0.0, 1.0);
    private static final ModConfigSpec.IntValue INFLATION_MAX_NOTIFICATIONS = BUILDER.comment("Máximo de itens notificados por ciclo").defineInRange("inflation_max_notifications", 5, 1, 100);
    private static final ModConfigSpec.IntValue INFLATION_AVG_SALES = BUILDER.comment("Média de vendas usada como referência para ajuste de preço").defineInRange("inflation_avg_sales", 10, 1, 10000);
    private static final ModConfigSpec.IntValue INFLATION_TIMEOUT = BUILDER.comment("Tempo em minutos para o ciclo de atualização da inflação").defineInRange("inflation_timeout", 10, 1, 600);

    // LIMPEZA AUTOMÁTICA
    private static final ModConfigSpec.BooleanValue CLEANUP_ENABLED = BUILDER.comment("Ativa/desativa a limpeza automática de vendas expiradas").define("cleanup_enabled", true);
    private static final ModConfigSpec.IntValue CLEANUP_INTERVAL_HOURS = BUILDER.comment("Intervalo da limpeza automática em horas").defineInRange("cleanup_interval_hours", 24, 1, 168);
    private static final ModConfigSpec.IntValue CLEANUP_EXPIRE_DAYS = BUILDER.comment("Dias para considerar uma venda como expirada").defineInRange("cleanup_expire_days", 7, 1, 365);

    public static final ModConfigSpec SPEC = BUILDER.build();

    //-----------------------------------------------Prefix-----------------------------------------------//
    public static String prefix;
    //-----------------------------------------------Misc-----------------------------------------------//
    public static boolean debug;
    public static boolean notifyBallTop;
    public static double minimumPricePercentage;
    public static boolean lotteryEnabled;
    public static int topBalanceInterval;
    //-----------------------------------------------Database-----------------------------------------------//
    public static String dbIp;
    public static int dbPort;
    public static String dbName;
    public static String dbUsername;
    public static String dbPassword;
    //-----------------------------------------------Vending-----------------------------------------------//
    public static int vendingEnergyCapacity;
    public static int vendingEnergyTransfer;
    public static int vendingEnergyCostPerOperation;
    public static List<String> vendingItemBlacklist = new ArrayList<>();
    //-----------------------------------------------Buying-----------------------------------------------//
    public static int buyingEnergyCapacity;
    public static int buyingEnergyTransfer;
    public static int buyingEnergyCostPerOperation;
    //-----------------------------------------------Tick-----------------------------------------------//
    public static int ticksPerOperation;
    //-----------------------------------------------Inflation-----------------------------------------------//
    public static boolean inflationEnabled;
    public static boolean inflationNotify;
    public static int inflationIntervalMin;
    public static double inflationMaxVariation;
    public static double inflationMinMult;
    public static double inflationMaxMult;
    public static double inflationNotifyPercent;
    public static int inflationMaxNotifications;
    public static int inflationAvgSales;
    public static int inflationTimeout;

    //-----------------------------------------------Cleanup-----------------------------------------------//
    public static boolean cleanupEnabled;
    public static int cleanupIntervalHours;
    public static int cleanupExpireDays;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent.Loading event) {
        //-----------------------------------------------Prefix-----------------------------------------------//
        prefix = PREFIX.get();
        //-----------------------------------------------Misc-----------------------------------------------//
        debug = DEBUG.get();
        notifyBallTop = NOTIFY_BALL_TOP.get();
        minimumPricePercentage = MIN_PRICE_PERCENTAGE.get();
        lotteryEnabled = LOTTERY.get();
        topBalanceInterval = TOP_BALANCE_INTERVAL.get();
        //-----------------------------------------------Database-----------------------------------------------//
        dbIp = DB_IP.get();
        dbPort = DB_PORT.get();
        dbName = DB_NAME.get();
        dbUsername = DB_USERNAME.get();
        dbPassword = DB_PASSWORD.get();
        //-----------------------------------------------Vending-----------------------------------------------//
        vendingEnergyCapacity = VENDING_ENERGY.get();
        vendingEnergyTransfer = VENDING_ENERGY_TRANSFER.get();
        vendingEnergyCostPerOperation = VENDING_ENERGY_COST_PER_OPERATION.get();
        vendingItemBlacklist = new ArrayList<>(VENDING_ITEM_BLACKLIST.get());
        //-----------------------------------------------Buying-----------------------------------------------//
        buyingEnergyCapacity = BUYING_ENERGY.get();
        buyingEnergyTransfer = BUYING_ENERGY_TRANSFER.get();
        buyingEnergyCostPerOperation = BUYING_ENERGY_COST_PER_OPERATION.get();
        //-----------------------------------------------Tick-----------------------------------------------//
        ticksPerOperation = TICKS_PER_OPERATION.get();
        //-----------------------------------------------Inflation-----------------------------------------------//
        inflationEnabled = INFLATION_ENABLED.get();
        inflationNotify = INFLATION_NOTIFY.get();
        inflationIntervalMin = INFLATION_INTERVAL_MIN.get();
        inflationMaxVariation = INFLATION_MAX_VARIATION.get();
        inflationMinMult = INFLATION_MIN_MULT.get();
        inflationMaxMult = INFLATION_MAX_MULT.get();
        inflationNotifyPercent = INFLATION_NOTIFY_PERCENT.get();
        inflationMaxNotifications = INFLATION_MAX_NOTIFICATIONS.get();
        inflationAvgSales = INFLATION_AVG_SALES.get();
        inflationTimeout = INFLATION_TIMEOUT.get();
        //-----------------------------------------------Cleanup-----------------------------------------------//
        cleanupEnabled = CLEANUP_ENABLED.get();
        cleanupIntervalHours = CLEANUP_INTERVAL_HOURS.get();
        cleanupExpireDays = CLEANUP_EXPIRE_DAYS.get();
    }
}