package com.Infinity.Nexus.Market.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public abstract class BaseMachineBlock extends BaseEntityBlock {
    public static IntegerProperty LIT = IntegerProperty.create("lit", 0, 1);
    public static BooleanProperty WORK = BooleanProperty.create("work");
    public static BooleanProperty PLACED = BooleanProperty.create("placed");
    protected VoxelShape SHAPE = Block.box(0, 0, 0, 16, 16, 16);
    public static DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    protected final MapCodec<BaseEntityBlock> CODEC;

    protected BaseMachineBlock(Properties properties, MapCodec<BaseEntityBlock> codec, IntegerProperty lit, DirectionProperty facing, VoxelShape shape) {
        super(properties);
        this.CODEC = codec;
        LIT = lit == null ? LIT : lit;
        FACING = facing == null ? FACING : facing;
        this.SHAPE = shape == null ? SHAPE : shape;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }


    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public @NotNull BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public @NotNull BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT, WORK, PLACED);
    }
    @Override
    public @NotNull VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return this.SHAPE;
    }

    @Override
    public @NotNull RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }


    @Override
    public void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pIsMoving) {
        if (pState.getBlock() != pNewState.getBlock()) {
            dropContents(pLevel, pPos, pLevel.getBlockEntity(pPos));
        }
        super.onRemove(pState, pLevel, pPos, pNewState, pIsMoving);
    }

    protected abstract void dropContents(Level level, BlockPos pos, BlockEntity blockEntity);

    @Nullable
    public abstract BlockEntity newBlockEntity(BlockPos pos, BlockState state);

    @Nullable
    public abstract <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType);

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> components, TooltipFlag flag) {
        if (getDescription() != null) {
            if(!Screen.hasShiftDown()){
                components.add(Component.translatable("tooltip.infinity_nexus_market.pressShift"));
            }else{
                String[] component = getDescription();
                for(String s : component){
                    components.add(Component.translatable("gui.infinity_nexus_market.tooltip.base_machine", s));
                }
            }
        }
        super.appendHoverText(stack, context, components, flag);
    }

    protected String[] getDescription() {
        return null;
    }
}
