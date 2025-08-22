package com.Infinity.Nexus.Market.block.entity;

import com.Infinity.Nexus.Market.block.custom.BaseMachineBlock;
import com.Infinity.Nexus.Market.config.ModConfigs;
import com.Infinity.Nexus.Market.itemStackHandler.RestrictedItemStackHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;
import software.bernie.geckolib.util.RenderUtil;

import java.util.UUID;

public abstract class AbstractMarketBlockEntity extends BlockEntity implements MenuProvider, GeoBlockEntity {
    protected String owner;
    protected String ownerName;
    protected boolean autoEnabled = false;
    protected int autoMinAmount = 0;
    protected double autoPrice = 0.0;
    protected boolean autoNotify = false;
    protected final ContainerData data;
    protected final RestrictedItemStackHandler itemHandler;
    protected int progress = 0;
    protected int maxProgress = ModConfigs.ticksPerOperation;
    private final float speed = maxProgress > 0 ? 10f / maxProgress : 1.0f;

    public AbstractMarketBlockEntity(BlockEntityType<?> type, BlockPos pPos, BlockState pBlockState, int slots) {
        super(type, pPos, pBlockState);
        this.itemHandler = createItemHandler(slots);
        this.data = createContainerData();
    }

    protected abstract RestrictedItemStackHandler createItemHandler(int slots);

    protected ContainerData createContainerData() {
        return new ContainerData() {
            @Override
            public int get(int pIndex) {
                return switch (pIndex) {
                    case 0 -> progress;
                    case 1 -> maxProgress;
                    case 2 -> owner == null ? 0 : 1;
                    case 3 -> ownerName == null ? 0 : 1;
                    default -> 0;
                };
            }

            @Override
            public void set(int pIndex, int pValue) {
                switch (pIndex) {
                    case 0 -> progress = pValue;
                    case 1 -> maxProgress = pValue;
                    case 2 -> owner = pValue == 1 ? "" : null;
                    case 3 -> ownerName = pValue == 1 ? "" : null;
                }
            }

            @Override
            public int getCount() {
                return 4;
            }
        };
    }

    public IItemHandler getItemHandler(Direction direction) {
        return itemHandler;
    }

    public void drops() {
        SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            inventory.setItem(i, itemHandler.getStackInSlot(i));
        }
        Containers.dropContents(this.level, this.worldPosition, inventory);
    }

    @Override
    protected void saveAdditional(CompoundTag pTag, HolderLookup.Provider registries) {
        super.saveAdditional(pTag, registries);
        pTag.put("inventory", itemHandler.serializeNBT(registries));
        pTag.putString("owner", owner == null ? "" : owner);
        pTag.putString("ownerName", ownerName == null ? "" : ownerName);
        pTag.putBoolean("autoEnabled", autoEnabled);
        pTag.putInt("autoMinAmount", autoMinAmount);
        pTag.putDouble("autoPrice", autoPrice);
        pTag.putBoolean("autoNotify", autoNotify);
        pTag.putInt("progress", progress);
        pTag.putInt("maxProgress", maxProgress);
    }

    @Override
    protected void loadAdditional(CompoundTag pTag, HolderLookup.Provider registries) {
        super.loadAdditional(pTag, registries);
        itemHandler.deserializeNBT(registries, pTag.getCompound("inventory"));
        if (pTag.getString("owner").equals("")) {
            owner = null;
        } else {
            owner = pTag.getString("owner");
        }
        if (pTag.getString("ownerName").equals("")) {
            ownerName = null;
        } else {
            ownerName = pTag.getString("ownerName");
        }
        autoEnabled = pTag.getBoolean("autoEnabled");
        autoMinAmount = pTag.getInt("autoMinAmount");
        autoPrice = pTag.getDouble("autoPrice");
        autoNotify = pTag.getBoolean("autoNotify");
        progress = pTag.getInt("progress");
        maxProgress = pTag.getInt("maxProgress");
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider lookupProvider) {
        super.onDataPacket(net, pkt, lookupProvider);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithFullMetadata(registries);
    }

    public void setOwner(Player player) {
        owner = player.getStringUUID();
        ownerName = player.getName().getString();
        setChanged();
    }

    public String getOwner() {
        if (owner == null) {
            return "§4❎";
        }
        Player player = level.getPlayerByUUID(UUID.fromString(owner));
        Component displayName = player == null ? Component.empty() : player.getDisplayName();
        return "§e" + displayName.getString();
    }

    // Common auto-config methods
    public void setAutoEnabled(boolean enabled) {
        this.autoEnabled = enabled;
        setChanged();
    }

    public void setAutoMinAmount(int amount) {
        this.autoMinAmount = amount;
        setChanged();
    }

    public void setAutoPrice(double price) {
        this.autoPrice = price;
        setChanged();
    }

    public void setAutoNotify(boolean notify) {
        this.autoNotify = notify;
        setChanged();
    }

    public boolean isAutoEnabled() {
        return autoEnabled;
    }

    public int getAutoMinAmount() {
        return autoMinAmount;
    }

    public double getAutoPrice() {
        return autoPrice;
    }

    public boolean isAutoNotify() {
        return autoNotify;
    }

    protected static final RawAnimation WORK = RawAnimation.begin().thenPlay("animation.model.work").thenLoop("animation.model.off");
    protected static final RawAnimation PLACED = RawAnimation.begin().thenPlay("animation.model.start").thenLoop("animation.model.off");
    protected static final RawAnimation OFF = RawAnimation.begin().then("animation.model.off", Animation.LoopType.LOOP);

    protected AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, this::deployAnimController));
    }

    protected <E extends GeoAnimatable> PlayState deployAnimController(final AnimationState<E> state) {
        try {
            if (level.isClientSide) {
                if (this.getBlockState().getValue(BaseMachineBlock.PLACED)) {
                    state.getController().setAnimation(PLACED);
                }else if (this.getBlockState().getValue(BaseMachineBlock.WORK)) {
                    state.getController().setAnimation(WORK);
                    state.getController().setAnimationSpeed(speed);
                } else {
                    state.getController().setAnimation(OFF);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public double getTick(Object blockEntity) {
        return RenderUtil.getCurrentTick();
    }
}