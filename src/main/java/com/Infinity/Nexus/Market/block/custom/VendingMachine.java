package com.Infinity.Nexus.Market.block.custom;

import com.Infinity.Nexus.Market.block.entity.ModBlockEntities;
import com.Infinity.Nexus.Market.block.entity.VendingBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class VendingMachine extends BaseMachineBlock {
    public static final MapCodec<BaseEntityBlock> CODEC = simpleCodec(VendingMachine::new);

    public VendingMachine(Properties pProperties) {
        super(pProperties, CODEC, null, null, null);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new VendingBlockEntity(pPos, pState);
    }


    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.VENDING_MACHINE_BE.get(),
                (pLevel1, pPos, pState1, pBlockEntity) -> pBlockEntity.tick(pLevel1, pPos, pState1));
    }

    @Override
    protected void dropContents(Level level, BlockPos pos, BlockEntity blockEntity) {
        if (blockEntity instanceof VendingBlockEntity) {
            ((VendingBlockEntity) blockEntity).drops();
        }
    }
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        BlockEntity entity = level.getBlockEntity(pos);
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu((MenuProvider) entity, pos);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (placer instanceof Player) {
            Player player = (Player) placer;
            VendingBlockEntity vendingMachineBlockEntity = (VendingBlockEntity) level.getBlockEntity(pos);
            vendingMachineBlockEntity.setOwner(player);
        }
        super.setPlacedBy(level, pos, state, placer, stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> components, TooltipFlag tooltipFlag) {
        if (Screen.hasShiftDown()) {
            components.add(Component.translatable("item.infinity_nexus_market.vending_machine_description"));
        } else {
            components.add(Component.translatable("tooltip.infinity_nexus_market.pressShift"));
        }
        super.appendHoverText(stack, context, components, tooltipFlag);
    }
}