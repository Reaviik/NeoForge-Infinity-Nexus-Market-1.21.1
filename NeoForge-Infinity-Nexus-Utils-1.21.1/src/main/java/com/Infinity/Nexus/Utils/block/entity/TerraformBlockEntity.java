package com.Infinity.Nexus.Utils.block.entity;

import com.Infinity.Nexus.Core.block.entity.common.SetUpgradeLevel;
import com.Infinity.Nexus.Core.component.CoreDataComponents;
import com.Infinity.Nexus.Core.itemStackHandler.RestrictedItemStackHandler;
import com.Infinity.Nexus.Core.items.ModItems;
import com.Infinity.Nexus.Core.renderer.area.AreaRenderable;
import com.Infinity.Nexus.Core.utils.*;
import com.Infinity.Nexus.Utils.block.entity.terraform.TerraformClear;
import com.Infinity.Nexus.Utils.block.entity.terraform.TerraformMiner;
import com.Infinity.Nexus.Utils.block.entity.terraform.TerraformPlacer;
import com.Infinity.Nexus.Utils.component.UtilsDataComponents;
import com.Infinity.Nexus.Utils.config.ModConfigs;
import com.Infinity.Nexus.Utils.item.ModItemsUtils;
import com.Infinity.Nexus.Utils.screen.terraform.TerraformMenu;
import com.Infinity.Nexus.Utils.utils.ModUtilsUtils;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class TerraformBlockEntity extends BlockEntity implements MenuProvider, AreaRenderable {
    private final RestrictedItemStackHandler itemHandler = new RestrictedItemStackHandler(13) {
        @Override
        protected void onContentsChanged(int slot) {
            if(slot >= 7 && slot <= 10) {
                if(itemHandler.getStackInSlot(slot).is(ModItemsUtils.TERRAFORM_RANGE_UPGRADE.get())) {
                    terraformingEnabled = false;
                }
            }
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return switch (slot) {
                case 0 -> ModUtils.isComponent(stack);
                case 1,2,3,4,5,6 -> !ModUtils.isComponent(stack) || !ModUtils.isUpgrade(stack) || stack.is(ModItems.LINKING_TOOL.get());
                case 7 -> stack.getBurnTime(RecipeType.SMELTING) > 0;
                case 8 -> stack.is(ModItems.LINKING_TOOL.get());
                case 9,10,11, 12 -> ModUtils.isUpgrade(stack);
                default -> super.isItemValid(slot, stack);
            };
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate, boolean fromAutomation) {
            if (slot >= 1 && slot <= 6) {
                return super.extractItem(slot, amount, simulate, false);
            }
            return super.extractItem(slot, amount, simulate, fromAutomation);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (slot > 0) {
                return stack;
            }
            return super.insertItem(slot, stack, simulate);
        }
    };
    private static final int[] OUTPUT_SLOTS = {1,2,3,4,5,6};
    private static final int FUEL_SLOT = 7;
    private static final int LINK_SLOT = 8;
    private static final int[] UPGRADE_SLOTS = {9,10,11};
    private static final int ENERGY_CAPACITY = ModConfigs.terraformEnergyCapacity;
    private static final int ENERGY_TRANSFER = ModConfigs.terraformEnergyTransfer;

    private final ModEnergyStorage ENERGY_STORAGE = createEnergyStorage();
    private ModEnergyStorage createEnergyStorage() {
        return new ModEnergyStorage(ENERGY_CAPACITY, ENERGY_TRANSFER) {
            @Override
            public void onEnergyChanged() {
                setChanged();
                getLevel().sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 4);
            }
        };
    }

    protected final ContainerData data;
    private int progress = 0;
    private int maxProgress = ModConfigs.terraformOperationTicks;
    private int sizeY = 0;
    private int sizeX = 0;
    private int sizeZ = 0;
    private int offsetX = 0;
    private int offsetY = 0;
    private int offsetZ = 0;
    private boolean showArea = false;
    private boolean terraformingEnabled = false;
    private int workX = 0, workY = 0, workZ = 0;
    private boolean lastRedstonePowered = false;
    private String owner;


    public TerraformBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(ModBlockEntities.TERRAFORM_BE.get(), pPos, pBlockState);
        this.data = new ContainerData() {
            @Override
            public int get(int pIndex) {
                return switch (pIndex) {
                    case 0 -> TerraformBlockEntity.this.progress;
                    case 1 -> TerraformBlockEntity.this.maxProgress;
                    case 2 -> TerraformBlockEntity.this.sizeY;
                    case 3 -> TerraformBlockEntity.this.sizeX;
                    case 4 -> TerraformBlockEntity.this.offsetX;
                    case 5 -> TerraformBlockEntity.this.offsetY;
                    case 6 -> TerraformBlockEntity.this.sizeZ;
                    case 7 -> TerraformBlockEntity.this.offsetZ;
                    case 8 -> TerraformBlockEntity.this.owner == null ? 0 : 1;
                    default -> 0;
                };
            }

            @Override
            public void set(int pIndex, int pValue) {
                switch (pIndex) {
                    case 0 -> TerraformBlockEntity.this.progress = pValue;
                    case 1 -> TerraformBlockEntity.this.maxProgress = pValue;
                    case 2 -> TerraformBlockEntity.this.sizeY = pValue;
                    case 3 -> TerraformBlockEntity.this.sizeX = pValue;
                    case 4 -> TerraformBlockEntity.this.offsetX = pValue;
                    case 5 -> TerraformBlockEntity.this.offsetY = pValue;
                    case 6 -> TerraformBlockEntity.this.sizeZ = pValue;
                    case 7 -> TerraformBlockEntity.this.offsetZ = pValue;
                    case 8 -> TerraformBlockEntity.this.owner = pValue == 1 ? "" : null;
                }
            }

            @Override
            public int getCount() {
                return 9;
            }
        };

    }

    public IItemHandler getItemHandler(Direction direction) {
        return itemHandler;
    }
    public IEnergyStorage getEnergyStorage(@Nullable Direction direction) {
        return ENERGY_STORAGE;
    }


    public void drops() {
        SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            inventory.setItem(i, itemHandler.getStackInSlot(i));
        }
        Containers.dropContents(this.level, this.worldPosition, inventory);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.infinity_nexus_utils.terraform");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
        return new TerraformMenu(pContainerId, pPlayerInventory,this, this.data, this.itemHandler);
    }


    @Override
    protected void saveAdditional(CompoundTag pTag, HolderLookup.Provider registries) {
        try {
            pTag.put("inventory", itemHandler.serializeNBT(registries));
            pTag.putInt("terraform.progress", progress);
            pTag.putInt("terraform.energy", ENERGY_STORAGE.getEnergyStored());
            pTag.putInt("terraform.sizeX", sizeX);
            pTag.putInt("terraform.sizeY", sizeY);
            pTag.putInt("terraform.sizeZ", sizeZ);
            pTag.putInt("terraform.offsetX", offsetX);
            pTag.putInt("terraform.offsetY", offsetY);
            pTag.putInt("terraform.offsetZ", offsetZ);
            pTag.putInt("terraform.workX", workX);
            pTag.putInt("terraform.workY", workY);
            pTag.putInt("terraform.workZ", workZ);
            pTag.putBoolean("terraform.enabled", terraformingEnabled);
            pTag.putString("owner", owner == null ? "" : owner);
            super.saveAdditional(pTag, registries);
        } catch (Exception e) {
            System.out.println("Failed to save TerraformBlockEntity data at " + this.worldPosition);
            e.printStackTrace();
        }
    }

    @Override
    protected void loadAdditional(CompoundTag pTag, HolderLookup.Provider registries) {
        super.loadAdditional(pTag, registries);
        itemHandler.deserializeNBT(registries, pTag.getCompound("inventory"));
        progress = pTag.getInt("terraform.progress");
        ENERGY_STORAGE.setEnergy(pTag.getInt("terraform.energy"));
        sizeX = pTag.getInt("terraform.sizeX");
        sizeY = pTag.getInt("terraform.sizeY");
        sizeZ = pTag.getInt("terraform.sizeZ");
        offsetX = pTag.getInt("terraform.offsetX");
        offsetY = pTag.getInt("terraform.offsetY");
        offsetZ = pTag.getInt("terraform.offsetZ");
        workX = pTag.getInt("terraform.workX");
        workY = pTag.getInt("terraform.workY");
        workZ = pTag.getInt("terraform.workZ");
        terraformingEnabled = pTag.getBoolean("terraform.enabled");
        if (pTag.getString("owner").equals("")) {
            owner = null;
        } else {
            owner = pTag.getString("owner");
        }
    }

    public <T extends BlockEntity> void tick(Level pLevel, BlockPos pPos, BlockState pState) {
        if (pLevel.isClientSide) {
            return;
        }

        // Toggle por pulso de redstone
        boolean redstonePowered = isRedstonePowered(pPos);
        if (redstonePowered && !lastRedstonePowered) {
            setTerraformingEnabled(!isTerraformingEnabled());
        }
        lastRedstonePowered = redstonePowered;

        if (!terraformingEnabled) {
            return;
        }

        if (!hasProgressFinished()) {
            increaseCraftingProgress();
            return;
        }

        if(!hasEnoughEnergy()){
            verifySolidFuel();
            return;
        }

        boolean finished = processNextBlock();
        extractEnergy();
        resetProgress();
        setMaxProgress();
        sendItems(pLevel);
        if (finished) {
            setTerraformingEnabled(false);
        }
    }

    private void sendItems(Level level) {
        BlockPos pos = itemHandler.getStackInSlot(LINK_SLOT).getOrDefault(CoreDataComponents.LINKINGTOOL_COORDS.get(), new BlockPos(0, 0, 0));
        if(pos.getY() == 0 && pos.getX() == 0 && pos.getZ() == 0) {
            ModUtils.ejectItemsWhePusher(worldPosition, UPGRADE_SLOTS, OUTPUT_SLOTS, itemHandler, level);
            return;
        }
        if(hasEmptySlot() || ModUtilsUtils.getUpgradeCount(itemHandler, UPGRADE_SLOTS, ModItemsUtils.TERRAFORM_PLACER_UPGRADE.get()) != 0) {
            return;
        }
        IItemHandler beItemHandler = ItemStackHandlerUtils.getBlockCapabilityItemHandler(level, pos, Direction.UP);
        if(beItemHandler == null) return;
        for (int slot : OUTPUT_SLOTS) {
            for (int i = 0; i < beItemHandler.getSlots(); i++) {
                if(ModUtils.canPlaceItemInContainer(itemHandler.getStackInSlot(slot), i, beItemHandler)) {
                    beItemHandler.insertItem(i, itemHandler.getStackInSlot(slot), false);
                    ItemStackHandlerUtils.extractItem(slot, itemHandler.getStackInSlot(slot).getCount(), false, itemHandler);
                }
            }
        }
    }
    private void setMaxProgress() {
        maxProgress = ModConfigs.terraformOperationTicks;
    }

    private boolean processNextBlock() {
        if (level == null || level.isClientSide()) return false;
        if(ModUtilsUtils.getUpgradeCount(itemHandler, UPGRADE_SLOTS, ModItemsUtils.TERRAFORM_RANGE_UPGRADE.get()) == 0) {
            for (int slot : UPGRADE_SLOTS) {
                ItemStack stack = itemHandler.getStackInSlot(slot);
                if (stack.is(ModItemsUtils.TERRAFORM_RANGE_UPGRADE.get()) && stack.has(UtilsDataComponents.TERRAFORM_RANGE_CARD_RANGE.get())) {
                    int range = stack.getOrDefault(UtilsDataComponents.TERRAFORM_RANGE_CARD_RANGE.get(), ModConfigs.terraformDefaultArea);
                    if (range > 0) {
                        setTerraformingEnabled(false);
                        return true;
                    }
                }
            }
            return false;
        }
        int placerCount = ModUtilsUtils.getUpgradeCount(itemHandler, UPGRADE_SLOTS, ModItemsUtils.TERRAFORM_PLACER_UPGRADE.get());
        int minerCount = ModUtilsUtils.getUpgradeCount(itemHandler, UPGRADE_SLOTS, ModItemsUtils.TERRAFORM_MINER_UPGRADE.get());
        int clearCount = ModUtilsUtils.getUpgradeCount(itemHandler, UPGRADE_SLOTS, ModItemsUtils.TERRAFORM_CLEAR_UPGRADE.get());
        boolean hasPlacer = placerCount > 0;
        boolean hasMiner = minerCount > 0;
        boolean hasClear = clearCount > 0;
        BlockPos min = getAreaMin();
        BlockPos max = getAreaMax();
        int areaX = max.getX() - min.getX() + 1;
        int areaY = max.getY() - min.getY() + 1;
        int areaZ = max.getZ() - min.getZ() + 1;

        // Posição da máquina
        BlockPos machinePos = getBlockPos();

        // Processa de cima para baixo (Y decrescente)
        while (workY >= 0) {
            int x = min.getX() + workX;
            int y = max.getY() - workY; // Começa do Y máximo e vai diminuindo
            int z = min.getZ() + workZ;
            BlockPos target = new BlockPos(x, y, z);

            // Impede quebrar a própria máquina e a área 3x3x3 ao redor (logo no início)
            if (Math.abs(target.getX() - machinePos.getX()) <= 1 &&
                Math.abs(target.getY() - machinePos.getY()) <= 1 &&
                Math.abs(target.getZ() - machinePos.getZ()) <= 1) {
                // Incrementa Z primeiro (menor para maior)
                workZ++;
                if (workZ >= areaZ) {
                    workZ = 0;
                    // Depois incrementa X (menor para maior)
                    workX++;
                    if (workX >= areaX) {
                        workX = 0;
                        // Por último, incrementa Y (de cima para baixo)
                        workY++;
                    }
                }
                // Verifica se terminou (Y chegou ao máximo)
                if (workY >= areaY) {
                    workX = workY = workZ = 0;
                    return true;
                }
                continue;
            }

            boolean processed = false;
            if (level.isLoaded(target)) {
                BlockState state = level.getBlockState(target);
                String blockId = state.getBlock().builtInRegistryHolder().key().location().toString();
                if (!ModConfigs.terraformBlocksBlacklist.contains(blockId)) {
                    boolean skipBlockEntities = ModConfigs.terraformSkipBlockEntities;
                    boolean airCostDurability = ModConfigs.terraformAirBlocksCostDurability;
                    FakePlayer player = GetFakePlayer.get((ServerLevel) level);
                    // Lógica de tipos do Range Card
                    int rangeCardType = 0;
                    for (int slot : UPGRADE_SLOTS) {
                        ItemStack stackType = itemHandler.getStackInSlot(slot);
                        if (stackType.is(ModItemsUtils.TERRAFORM_RANGE_UPGRADE.get()) && stackType.has(UtilsDataComponents.TERRAFORM_RANGE_CARD_TYPE.get())) {
                            rangeCardType = stackType.getOrDefault(UtilsDataComponents.TERRAFORM_RANGE_CARD_TYPE.get(), 0);
                            break;
                        }
                    }
                    // Tipo 1: pular BlockEntities
                    if ((skipBlockEntities || rangeCardType == 1) && level.getBlockEntity(target) != null) {
                        processed = false;
                    // Tipo 2: pular fluidos
                    } else if (rangeCardType == 2 && state.getFluidState() != null && !state.getFluidState().isEmpty()) {
                        processed = false;
                    // Tipo 3: só fluidos
                    } else if (rangeCardType == 3 && (state.getFluidState() == null || state.getFluidState().isEmpty())) {
                        processed = false;
                    } else if (level.isEmptyBlock(target)) {
                        if (hasPlacer && hasBlockInSlots()) {
                            if (airCostDurability) {
                                ModUtilsUtils.damageRangeCard(itemHandler, UPGRADE_SLOTS);
                            }
                            processed = TerraformPlacer.placeBlock(this, target, OUTPUT_SLOTS, UPGRADE_SLOTS, player);
                        }
                    } else {
                        if (hasMiner) {
                            if (hasEmptySlot()) {
                                ModUtilsUtils.damageRangeCard(itemHandler, UPGRADE_SLOTS);
                                processed = TerraformMiner.mineBlock(this, target, OUTPUT_SLOTS, UPGRADE_SLOTS, player);
                            } else {
                                // Não há slot livre: PAUSA, não avança workX/Y/Z
                                return false;
                            }
                        } else if (hasClear) {
                            ModUtilsUtils.damageRangeCard(itemHandler, UPGRADE_SLOTS);
                            processed = TerraformClear.clearBlock(this, target, UPGRADE_SLOTS, player);
                        }
                    }
                }
            }

            // Incrementa Z primeiro (menor para maior)
            workZ++;
            if (workZ >= areaZ) {
                workZ = 0;
                // Depois incrementa X (menor para maior)
                workX++;
                if (workX >= areaX) {
                    workX = 0;
                    // Por último, incrementa Y (de cima para baixo)
                    workY++;
                }
            }

            // Verifica se terminou (Y chegou ao máximo)
            if (workY >= areaY) {
                workX = workY = workZ = 0;
                return true;
            }

            if (processed) {
                return false;
            }
        }
        workX = workY = workZ = 0;
        return true;
    }

    private boolean hasBlockInSlots() {
        for (int slot = 0; slot < OUTPUT_SLOTS.length; slot++) {
            if ((itemHandler.getStackInSlot(OUTPUT_SLOTS[slot]).getItem() instanceof BlockItem)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasEmptySlot() {
        for (int slot = 0; slot < OUTPUT_SLOTS.length; slot++) {
            if (itemHandler.getStackInSlot(OUTPUT_SLOTS[slot]).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private void extractEnergy() {
        int energy = ModConfigs.terraformEnergyCostPerOperation;
        int speed = Math.max(ModUtils.getSpeed(itemHandler, UPGRADE_SLOTS), 2);
        int strength = (ModUtils.getStrength(itemHandler, UPGRADE_SLOTS) * 10);

        int var1 = energy * speed;

        int extractEnergy = var1 + strength;
        EnergyUtils.extractEnergy(ENERGY_STORAGE, extractEnergy, false);
    }

    private boolean hasEnoughEnergy() {
        int energy = ModConfigs.terraformEnergyCostPerOperation;
        int speed = Math.max(ModUtils.getSpeed(itemHandler, UPGRADE_SLOTS), 2);
        int strength = (ModUtils.getStrength(itemHandler, UPGRADE_SLOTS) * 10);

        int var1 = energy * speed;

        int extractEnergy = var1 + strength;
        return ENERGY_STORAGE.getEnergyStored() >= extractEnergy;
    }

    private void resetProgress() {
        progress = 0;
    }

    private boolean isRedstonePowered(BlockPos pPos) {
        return this.level.hasNeighborSignal(pPos);
    }

    private boolean hasProgressFinished() {
        return progress >= maxProgress;
    }

    private void increaseCraftingProgress() {
        progress += ModUtilsUtils.getUpgradeCount(itemHandler, UPGRADE_SLOTS, ModItems.SPEED_UPGRADE.get()) + 1;
    }

    private void verifySolidFuel(){
        ItemStack slotItem = itemHandler.getStackInSlot(FUEL_SLOT);
        int burnTime = slotItem.getBurnTime(RecipeType.SMELTING);
        if(burnTime > 1){
            while(itemHandler.getStackInSlot(FUEL_SLOT).getCount() > 0 && this.getEnergyStorage(null).getEnergyStored() + burnTime < this.getEnergyStorage(null).getMaxEnergyStored()){
                this.getEnergyStorage(null).receiveEnergy(burnTime, false);
                ItemStackHandlerUtils.extractItem(FUEL_SLOT, 1, false, itemHandler);
            }
        }
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

    public void setUpgradeLevel(ItemStack itemStack, Player player) {
        SetUpgradeLevel.setUpgradeLevel(itemStack, player, this, UPGRADE_SLOTS, this.itemHandler);
        setChanged();
    }

    public boolean hasRangeCard() {
        return ModUtilsUtils.getUpgradeCount(itemHandler, UPGRADE_SLOTS, ModItemsUtils.TERRAFORM_RANGE_UPGRADE.get()) > 0;
    }

    public void setOwner(Player player) {
        owner = player.getStringUUID();
        setChanged();
    }

    //---------------------------------------Jade----------------------------------------//
    public String getOwner() {
        if (owner == null) {
            return "§4❎";
        }
        Player player = level.getPlayerByUUID(UUID.fromString(owner));
        Component displayName = player == null ? Component.empty() : player.getDisplayName();

        return "§e"+ displayName.getString();
    }
    //---------------------------------------Render Area Manager----------------------------------------//

    public void setShowArea(boolean show) {
        this.showArea = show;
        this.setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    public boolean shouldShowArea() {
        return this.showArea;
    }

    public void setTerraformingEnabled(boolean enabled) {
        if (enabled) {
            int currentVolume = sizeX * sizeY * sizeZ;
            int maxVolume = getMaxVolume();

            if (currentVolume > maxVolume) {
                adjustAreaToFitVolume(maxVolume);
            }
        }

        this.terraformingEnabled = enabled;
        this.setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    private void adjustAreaToFitVolume(int maxVolume) {
        // Calcula um fator de escala para reduzir proporcionalmente
        double scale = Math.cbrt((double) maxVolume / (sizeX * sizeY * sizeZ));

        // Aplica a escala mantendo valores mínimos de 1
        sizeX = Math.max(1, (int) (sizeX * scale));
        sizeY = Math.max(1, (int) (sizeY * scale));
        sizeZ = Math.max(1, (int) (sizeZ * scale));

        // Garante que o volume final não exceda o máximo
        while (sizeX * sizeY * sizeZ > maxVolume) {
            // Reduz o maior eixo primeiro
            if (sizeX >= sizeY && sizeX >= sizeZ) {
                sizeX = Math.max(1, sizeX - 1);
            } else if (sizeY >= sizeX && sizeY >= sizeZ) {
                sizeY = Math.max(1, sizeY - 1);
            } else {
                sizeZ = Math.max(1, sizeZ - 1);
            }
        }

        setChanged();
    }

    public boolean isTerraformingEnabled() {
        return this.terraformingEnabled;
    }

    public BlockPos getAreaMin() {
        return new BlockPos(
                getBlockPos().getX() + offsetX - sizeX / 2,
                getBlockPos().getY() + 1 + offsetY,
                getBlockPos().getZ() + offsetZ - sizeZ / 2
        );
    }

    public BlockPos getAreaMax() {
        return getAreaMin().offset(sizeX - 1, sizeY - 1, sizeZ - 1);
    }

    public void moveArea(String axis, int delta) {
        switch (axis) {
            case "x" -> this.sizeX = Math.max(0, this.sizeX + delta);
            case "y" -> this.sizeY = Math.max(0, this.sizeY + delta);
            case "z" -> this.sizeZ = Math.max(0, this.sizeZ + delta);
            case "offsetX" -> this.offsetX += delta;
            case "offsetY" -> this.offsetY += delta;
            case "offsetZ" -> this.offsetZ += delta;
        }
        setChanged();
    }

    public void setAreaSize(String axis, int value) {
        int x = axis.equals("x") ? value : this.sizeX;
        int y = axis.equals("y") ? value : this.sizeY;
        int z = axis.equals("z") ? value : this.sizeZ;

        int maxVolume = getMaxVolume();
        int volume = x * y * z;
        if (volume > maxVolume) {
            while (x * y * z > maxVolume && value > 0) {
                value--;
                if (axis.equals("x")) x = value;
                if (axis.equals("y")) y = value;
                if (axis.equals("z")) z = value;
            }
        }

        switch (axis) {
            case "x" -> this.sizeX = Math.max(0, x);
            case "y" -> this.sizeY = Math.max(0, y);
            case "z" -> this.sizeZ = Math.max(0, z);
            case "offsetX" -> this.offsetX = value;
            case "offsetY" -> this.offsetY = value;
            case "offsetZ" -> this.offsetZ = value;
        }
        setChanged();
    }

    public int getSizeX() { return sizeX; }
    public int getSizeY() { return sizeY; }
    public int getSizeZ() { return sizeZ; }

    public int getOffsetX() { return offsetX; }
    public int getOffsetY() { return offsetY; }
    public int getOffsetZ() { return offsetZ; }

    public int getMaxVolume() {
        for (int slot : UPGRADE_SLOTS) {
            ItemStack stack = itemHandler.getStackInSlot(slot);
            if (stack.is(ModItemsUtils.TERRAFORM_RANGE_UPGRADE.get()) && stack.has(UtilsDataComponents.TERRAFORM_RANGE_CARD_RANGE.get())) {
                Integer range = stack.get(UtilsDataComponents.TERRAFORM_RANGE_CARD_RANGE.get());
                if (range != null && range > 0) {
                    return range;
                }
            }
        }
        return ModConfigs.terraformDefaultArea;
    }

    @Override
    public float[] getAreaColor() {
        return new float[]{0.0f, 1.0f, 0.0f, 1.0f};
    }
}