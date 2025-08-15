package com.Infinity.Nexus.Market.utils;

import com.Infinity.Nexus.Market.InfinityNexusMarket;
import com.Infinity.Nexus.Market.events.ModEvents;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class LotteryManager {
    private static final Path LOTTERY_FILE = Paths.get("config/infinity_nexus_market/lottery_participants.json");
    private static final List<String> participants = new ArrayList<>();
    private static final Gson gson = new Gson();

    static {
        loadParticipants();
    }

    private static void loadParticipants() {
        try {
            if (Files.exists(LOTTERY_FILE)) {
                String json = new String(Files.readAllBytes(LOTTERY_FILE));
                List<String> loaded = gson.fromJson(json, new TypeToken<List<String>>(){}.getType());
                if (loaded != null) {
                    participants.clear();
                    participants.addAll(loaded);
                }
            }
        } catch (IOException e) {
            InfinityNexusMarket.LOGGER.error("Failed to load lottery participants", e);
        }
    }

    private static void saveParticipants() {
        try {
            Files.createDirectories(LOTTERY_FILE.getParent());
            String json = gson.toJson(participants);
            Files.write(LOTTERY_FILE, json.getBytes());
        } catch (IOException e) {
            InfinityNexusMarket.LOGGER.error("Failed to save lottery participants", e);
        }
    }

    public static void addParticipant(ServerPlayer player) {
        if(!ModEvents.isDay){
            player.sendSystemMessage(Component.literal("§cA loteria não esta aberta hoje!"));
            return;
        }
        if (!participants.contains(player.getName().getString())) {
            participants.add(player.getName().getString());
            saveParticipants();
            player.sendSystemMessage(Component.literal("§aVocê foi adicionado ao sorteio semanal!"));
        } else {
            player.sendSystemMessage(Component.literal("§cVocê já está participando do sorteio!"));
        }
    }

    public static List<String> getParticipants() {
        return new ArrayList<>(participants);
    }

    public static void clearParticipants() {
        participants.clear();
        try {
            Files.deleteIfExists(LOTTERY_FILE);
        } catch (IOException e) {
            InfinityNexusMarket.LOGGER.error("Failed to clear lottery participants", e);
        }
    }
}