package com.Infinity.Nexus.Utils.block.custom.common;

import com.Infinity.Nexus.Core.utils.ModUtils;
import com.Infinity.Nexus.Utils.block.entity.TerraformBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class CommonUpgrades {

    public static void setUpgrades(Level pLevel, BlockPos pPos, Player pPlayer) {
        BlockEntity entity = pLevel.getBlockEntity(pPos);
        ItemStack stack = pPlayer.getMainHandItem().copy();
        boolean component = ModUtils.isComponent(stack);
        boolean upgrade = ModUtils.isUpgrade(stack);
        if (pPlayer instanceof ServerPlayer serverPlayer) {
            if (upgrade) {
                if (entity instanceof TerraformBlockEntity be) {
                    be.setUpgradeLevel(stack, pPlayer);
                }
            } else {
                try {
                    serverPlayer.openMenu((MenuProvider) entity, pPos);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}