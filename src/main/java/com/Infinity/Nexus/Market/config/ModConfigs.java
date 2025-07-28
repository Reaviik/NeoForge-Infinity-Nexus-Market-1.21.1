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

    //-----------------------------------------------Vending-----------------------------------------------//
    //Prefix
    private static final ModConfigSpec.ConfigValue<String> PREFIX = BUILDER.comment("Defines the message prefix").define("prefix", "§f[§bMarket§f]: ");
    //ENERGY Vending
    private static final ModConfigSpec.IntValue VENDING_ENERGY = BUILDER.comment("Defines the amount of energy that the Vending will store").defineInRange("vending_energy_capacity", 150000, 1, Integer.MAX_VALUE);
    private static final ModConfigSpec.IntValue VENDING_ENERGY_TRANSFER = BUILDER.comment("Defines the amount of energy that the Vending will transfer").defineInRange("vending_energy_transfer", 100000, 1, Integer.MAX_VALUE);
    private static final ModConfigSpec.IntValue VENDING_ENERGY_COST_PER_OPERATION = BUILDER.comment("Defines the amount of energy that the Vending will consume per operation").defineInRange("vending_energy_cost_per_operation", 100, 1, Integer.MAX_VALUE);
    private static final ModConfigSpec.IntValue VENDING_TICKS_PER_OPERATION = BUILDER.comment("Defines how many ticks the vending machine will make each operation").defineInRange("vending_ticks_per_operation", 100, 1, Integer.MAX_VALUE);
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
    private static final ModConfigSpec.IntValue BUYING_TICKS_PER_OPERATION = BUILDER.comment("Defines how many ticks the buying machine will make each operation").defineInRange("buying_ticks_per_operation", 100, 1, Integer.MAX_VALUE);

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

    // BACKUP AUTOMÁTICO
    private static final ModConfigSpec.BooleanValue BACKUP_ENABLED = BUILDER.comment("Ativa/desativa o backup automático das databases do market").define("backup_enabled", true);
    private static final ModConfigSpec.IntValue BACKUP_INTERVAL_MIN = BUILDER.comment("Intervalo do backup automático em minutos").defineInRange("backup_interval_min", 30, 1, 1440);

    // LIMPEZA AUTOMÁTICA
    private static final ModConfigSpec.BooleanValue CLEANUP_ENABLED = BUILDER.comment("Ativa/desativa a limpeza automática de vendas expiradas").define("cleanup_enabled", true);
    private static final ModConfigSpec.IntValue CLEANUP_INTERVAL_HOURS = BUILDER.comment("Intervalo da limpeza automática em horas").defineInRange("cleanup_interval_hours", 24, 1, 168);
    private static final ModConfigSpec.IntValue CLEANUP_EXPIRE_DAYS = BUILDER.comment("Dias para considerar uma venda como expirada").defineInRange("cleanup_expire_days", 7, 1, 365);

    public static final ModConfigSpec SPEC = BUILDER.build();

    //-----------------------------------------------Prefix-----------------------------------------------//
    public static String prefix;
    //-----------------------------------------------Vending-----------------------------------------------//
    public static int vendingEnergyCapacity;
    public static int vendingEnergyTransfer;
    public static int vendingEnergyCostPerOperation;
    public static int vendingTicksPerOperation;
    public static List<String> vendingItemBlacklist = new ArrayList<>();
    //-----------------------------------------------Buying-----------------------------------------------//
    public static int buyingEnergyCapacity;
    public static int buyingEnergyTransfer;
    public static int buyingEnergyCostPerOperation;
    public static int buyingTicksPerOperation;
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

    //-----------------------------------------------Backup-----------------------------------------------//
    public static boolean backupEnabled;
    public static int backupIntervalMin;

    //-----------------------------------------------Cleanup-----------------------------------------------//
    public static boolean cleanupEnabled;
    public static int cleanupIntervalHours;
    public static int cleanupExpireDays;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent.Loading event) {
        //-----------------------------------------------Prefix-----------------------------------------------//
        prefix = PREFIX.get();
        //-----------------------------------------------Vending-----------------------------------------------//
        vendingEnergyCapacity = VENDING_ENERGY.get();
        vendingEnergyTransfer = VENDING_ENERGY_TRANSFER.get();
        vendingEnergyCostPerOperation = VENDING_ENERGY_COST_PER_OPERATION.get();
        vendingTicksPerOperation = VENDING_TICKS_PER_OPERATION.get();
        vendingItemBlacklist = new ArrayList<>(VENDING_ITEM_BLACKLIST.get());
        //-----------------------------------------------Buying-----------------------------------------------//
        buyingEnergyCapacity = BUYING_ENERGY.get();
        buyingEnergyTransfer = BUYING_ENERGY_TRANSFER.get();
        buyingEnergyCostPerOperation = BUYING_ENERGY_COST_PER_OPERATION.get();
        buyingTicksPerOperation = BUYING_TICKS_PER_OPERATION.get();
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
        //-----------------------------------------------Backup-----------------------------------------------//
        backupEnabled = BACKUP_ENABLED.get();
        backupIntervalMin = BACKUP_INTERVAL_MIN.get();

        //-----------------------------------------------Cleanup-----------------------------------------------//
        cleanupEnabled = CLEANUP_ENABLED.get();
        cleanupIntervalHours = CLEANUP_INTERVAL_HOURS.get();
        cleanupExpireDays = CLEANUP_EXPIRE_DAYS.get();
    }
}