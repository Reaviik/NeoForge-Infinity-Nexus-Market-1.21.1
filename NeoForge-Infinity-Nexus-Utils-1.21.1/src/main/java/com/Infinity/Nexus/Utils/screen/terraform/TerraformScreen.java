package com.Infinity.Nexus.Utils.screen.terraform;

import com.Infinity.Nexus.Core.renderer.EnergyInfoArea;
import com.Infinity.Nexus.Core.renderer.InfoArea;
import com.Infinity.Nexus.Core.renderer.RenderScreenTooltips;
import com.Infinity.Nexus.Core.utils.MouseUtil;
import com.Infinity.Nexus.Utils.InfinityNexusUtils;
import com.Infinity.Nexus.Utils.networking.ModMessages;
import com.Infinity.Nexus.Utils.networking.packet.ShowTerraformAreaC2SPacket;
import com.Infinity.Nexus.Utils.networking.packet.ToggleAreaC2SPacket;
import com.Infinity.Nexus.Utils.networking.packet.ToggleTerraformingC2SPacket;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import com.Infinity.Nexus.Utils.config.ModConfigs;

import java.util.Optional;

public class TerraformScreen extends AbstractContainerScreen<TerraformMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(InfinityNexusUtils.MOD_ID, "textures/gui/terraform_gui.png");

    private EnergyInfoArea energyInfoArea;
    private static boolean showArea;
    private static boolean terraformingEnabled;
    private Button areaButton;
    private Button terraformingButton;
    private Button xButton, yButton, zButton, offsetXButton, offsetYButton, offsetZButton;
    private EditBox xEditBox, yEditBox, zEditBox;
    private EditBox offsetXEditBox, offsetYEditBox, offsetZEditBox;
    private boolean lastHasRangeCard = false;

    public TerraformScreen(TerraformMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
    }
    @Override
    protected void init() {
        super.init();
        this.inventoryLabelY = 10000;
        this.titleLabelY = 10000;
        assignEnergyInfoArea();
        initializeAreaButton();
        initializeTerraformingButton();
        initializeMoveButtons();
        initializeEditBoxes();
        if (!menu.hasRangeCard()) {
            xEditBox.setEditable(false);
            yEditBox.setEditable(false);
            zEditBox.setEditable(false);
            offsetXEditBox.setEditable(false);
            offsetYEditBox.setEditable(false);
            offsetZEditBox.setEditable(false);
        }
    }
    @Override
    public void containerTick() {
        super.containerTick();
        boolean hasRangeCard = menu.hasRangeCard();
        if (xEditBox != null) xEditBox.setEditable(hasRangeCard);
        if (yEditBox != null) yEditBox.setEditable(hasRangeCard);
        if (zEditBox != null) zEditBox.setEditable(hasRangeCard);
        if (offsetXEditBox != null) offsetXEditBox.setEditable(hasRangeCard);
        if (offsetYEditBox != null) offsetYEditBox.setEditable(hasRangeCard);
        if (offsetZEditBox != null) offsetZEditBox.setEditable(hasRangeCard);
        lastHasRangeCard = hasRangeCard;
    }
    private void initializeAreaButton() {
        showArea = menu.blockEntity.shouldShowArea();
        this.areaButton = addRenderableWidget(
                new Button.Builder(
                        Component.literal(" "),
                        this::onAreaButtonClick)
                        .tooltip(Tooltip.create(Component.translatable((showArea ? "gui.infinity_nexus_utils.terraform.hide_area" : "gui.infinity_nexus_utils.terraform.show_area"))))
                        .bounds(this.leftPos +32, this.topPos +14, 10, 10)
                        .size(10, 10)
                        .build()
        );
        this.areaButton.setAlpha(0.2F);
    }

    private void initializeTerraformingButton() {
        terraformingEnabled = menu.blockEntity.isTerraformingEnabled();
        this.terraformingButton = addRenderableWidget(
                new Button.Builder(
                        Component.literal(" "),
                        this::onTerraformingButtonClick)
                        .tooltip(Tooltip.create(Component.translatable((terraformingEnabled ? "gui.infinity_nexus_utils.terraform.disable_terraforming" : "gui.infinity_nexus_utils.terraform.enable_terraforming"))))
                        .bounds(this.leftPos + 32, this.topPos + 32, 10, 10)
                        .size(10, 10)
                        .build()
        );
        this.terraformingButton.setAlpha(0.2F);
    }

    private void initializeMoveButtons() {
        int xBtnX = 79, xBtnY = 14; // -9
        int yBtnX = 79, yBtnY = 32; // -9
        int zBtnX = 79, zBtnY = 50; // -9
        int offsetBtnX = 69, offsetBtnY = 14; // -9
        int offsetYBtnY = 32;
        int offsetZBtnY = 50;
        int btnSize = 10;
        // X
        this.xButton = addRenderableWidget(new Button.Builder(Component.literal("X"), b -> handleAxisButtonClick("x", 1))
            .tooltip(Tooltip.create(Component.literal("Clique esquerdo: aumentar X\nClique direito: diminuir X\nShift: ±10\nCtrl: ±50")))
            .bounds(this.leftPos + xBtnX, this.topPos + xBtnY, btnSize, btnSize).size(btnSize, btnSize).build());
        this.xButton.setAlpha(0.2F);
        // Y
        this.yButton = addRenderableWidget(new Button.Builder(Component.literal("Y"), b -> handleAxisButtonClick("y", 1))
            .tooltip(Tooltip.create(Component.literal("Clique esquerdo: aumentar Y\nClique direito: diminuir Y\nShift: ±10\nCtrl: ±50")))
            .bounds(this.leftPos + yBtnX, this.topPos + yBtnY, btnSize, btnSize).size(btnSize, btnSize).build());
        this.yButton.setAlpha(0.2F);
        // Z
        this.zButton = addRenderableWidget(new Button.Builder(Component.literal("Z"), b -> handleAxisButtonClick("z", 1))
            .tooltip(Tooltip.create(Component.literal("Clique esquerdo: aumentar Z\nClique direito: diminuir Z\nShift: ±10\nCtrl: ±50")))
            .bounds(this.leftPos + zBtnX, this.topPos + zBtnY, btnSize, btnSize).size(btnSize, btnSize).build());
        this.zButton.setAlpha(0.2F);
        // Offset X
        this.offsetXButton = addRenderableWidget(new Button.Builder(Component.literal("O"), b -> handleAxisButtonClick("offsetX", 1))
            .tooltip(Tooltip.create(Component.literal("Clique esquerdo: aumentar offset X\nClique direito: diminuir offset X\nShift: ±10\nCtrl: ±50\nLimite: ±250")))
            .bounds(this.leftPos + offsetBtnX, this.topPos + offsetBtnY, btnSize, btnSize).size(btnSize, btnSize).build());
        this.offsetXButton.setAlpha(0.2F);
        // Offset Y
        this.offsetYButton = addRenderableWidget(new Button.Builder(Component.literal("O"), b -> handleAxisButtonClick("offsetY", 1))
            .tooltip(Tooltip.create(Component.literal("Clique esquerdo: aumentar offset Y\nClique direito: diminuir offset Y\nShift: ±10\nCtrl: ±50\nLimite: ±250")))
            .bounds(this.leftPos + offsetBtnX, this.topPos + offsetYBtnY, btnSize, btnSize).size(btnSize, btnSize).build());
        this.offsetYButton.setAlpha(0.2F);
        // Offset Z
        this.offsetZButton = addRenderableWidget(new Button.Builder(Component.literal("O"), b -> handleAxisButtonClick("offsetZ", 1))
            .tooltip(Tooltip.create(Component.literal("Clique esquerdo: aumentar offset Z\nClique direito: diminuir offset Z\nShift: ±10\nCtrl: ±50\nLimite: ±250")))
            .bounds(this.leftPos + offsetBtnX, this.topPos + offsetZBtnY, btnSize, btnSize).size(btnSize, btnSize).build());
        this.offsetZButton.setAlpha(0.2F);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 0 = esquerdo, 1 = direito
        if (xButton != null && xButton.isMouseOver(mouseX, mouseY)) {
            handleAxisButtonClick("x", button == 0 ? 1 : -1);
            return true;
        }
        if (yButton != null && yButton.isMouseOver(mouseX, mouseY)) {
            handleAxisButtonClick("y", button == 0 ? 1 : -1);
            return true;
        }
        if (zButton != null && zButton.isMouseOver(mouseX, mouseY)) {
            handleAxisButtonClick("z", button == 0 ? 1 : -1);
            return true;
        }
        if (offsetXButton != null && offsetXButton.isMouseOver(mouseX, mouseY)) {
            handleAxisButtonClick("offsetX", button == 0 ? 1 : -1);
            return true;
        }
        if (offsetYButton != null && offsetYButton.isMouseOver(mouseX, mouseY)) {
            handleAxisButtonClick("offsetY", button == 0 ? 1 : -1);
            return true;
        }
        if (offsetZButton != null && offsetZButton.isMouseOver(mouseX, mouseY)) {
            handleAxisButtonClick("offsetZ", button == 0 ? 1 : -1);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void handleAxisButtonClick(String axis, int delta) {
        if (!menu.hasRangeCard()) return;
        int newValue;
        
        switch (axis) {
            case "x", "y", "z" -> {
                int max = getMaxValueForAxis(axis); // 100
                newValue = Math.max(0, Math.min(parseIntSafe(getEditBoxForAxis(axis).getValue()) + delta * getDeltaMultiplier(), max));
                getEditBoxForAxis(axis).setValue(String.valueOf(newValue));
                onEditBoxChanged(axis, String.valueOf(newValue));
            }
            case "offsetX", "offsetY", "offsetZ" -> {
                // Offset fixo de ±250 blocks
                int maxOffset = 250;
                newValue = Math.max(-maxOffset, Math.min(parseIntSafe(getEditBoxForAxis(axis).getValue()) + delta * getDeltaMultiplier(), maxOffset));
                getEditBoxForAxis(axis).setValue(String.valueOf(newValue));
                onEditBoxChanged(axis, String.valueOf(newValue));
            }
        }
    }

    private int parseIntSafe(String s) {
        try { return Integer.parseInt(s); } catch (Exception e) { return 0; }
    }
    private int getDeltaMultiplier() {
        if (Screen.hasControlDown()) return 50;
        if (Screen.hasShiftDown()) return 10;
        return 1;
    }
    
    private EditBox getEditBoxForAxis(String axis) {
        return switch (axis) {
            case "x" -> xEditBox;
            case "y" -> yEditBox;
            case "z" -> zEditBox;
            case "offsetX" -> offsetXEditBox;
            case "offsetY" -> offsetYEditBox;
            case "offsetZ" -> offsetZEditBox;
            default -> throw new IllegalArgumentException("Eixo inválido: " + axis);
        };
    }

    private void initializeEditBoxes() {
        int xBtnX = 79, xBtnY = 14; // -9
        int yBtnX = 79, yBtnY = 32; // -9
        int zBtnX = 79, zBtnY = 50; // -9
        int offsetBtnX = 69, offsetBtnY = 14; // -9
        int offsetYBtnY = 32;
        int offsetZBtnY = 50;
        int boxWidth = 26, boxHeight = 10, boxOffset = 10;
        // X
        xEditBox = new EditBox(this.font, this.leftPos + xBtnX + boxOffset, this.topPos + xBtnY, boxWidth, boxHeight, Component.literal("X"));
        xEditBox.setValue(String.valueOf(menu.blockEntity.getSizeX()));
        xEditBox.setMaxLength(4);
        xEditBox.setFilter(s -> s.isEmpty() || s.matches("\\d{1,4}"));
        xEditBox.setTooltip(Tooltip.create(Component.literal("Definir valor de X")));
        xEditBox.setBordered(true);
        xEditBox.setEditable(true);
        xEditBox.setFocused(false);
        xEditBox.setVisible(true);
        xEditBox.setResponder(val -> onEditBoxChanged("x", val));
        this.addRenderableWidget(xEditBox);
        // Y
        yEditBox = new EditBox(this.font, this.leftPos + yBtnX + boxOffset, this.topPos + yBtnY, boxWidth, boxHeight, Component.literal("Y"));
        yEditBox.setValue(String.valueOf(menu.blockEntity.getSizeY()));
        yEditBox.setMaxLength(4);
        yEditBox.setFilter(s -> s.isEmpty() || s.matches("\\d{1,4}"));
        yEditBox.setTooltip(Tooltip.create(Component.literal("Definir valor de Y")));
        yEditBox.setBordered(true);
        yEditBox.setEditable(true);
        yEditBox.setFocused(false);
        yEditBox.setVisible(true);
        yEditBox.setResponder(val -> onEditBoxChanged("y", val));
        this.addRenderableWidget(yEditBox);
        // Z
        zEditBox = new EditBox(this.font, this.leftPos + zBtnX + boxOffset, this.topPos + zBtnY, boxWidth, boxHeight, Component.literal("Z"));
        zEditBox.setValue(String.valueOf(menu.blockEntity.getSizeZ()));
        zEditBox.setMaxLength(4);
        zEditBox.setFilter(s -> s.isEmpty() || s.matches("\\d{1,4}"));
        zEditBox.setTooltip(Tooltip.create(Component.literal("Definir valor de Z")));
        zEditBox.setBordered(true);
        zEditBox.setEditable(true);
        zEditBox.setFocused(false);
        zEditBox.setVisible(true);
        zEditBox.setResponder(val -> onEditBoxChanged("z", val));
        this.addRenderableWidget(zEditBox);
        // Offset X
        offsetXEditBox = new EditBox(this.font, this.leftPos + offsetBtnX - boxWidth, this.topPos + offsetBtnY, boxWidth, boxHeight, Component.literal("offsetX"));
        offsetXEditBox.setValue(String.valueOf(menu.blockEntity.getOffsetX()));
        offsetXEditBox.setMaxLength(4);
        offsetXEditBox.setFilter(s -> s.isEmpty() || s.equals("-") || s.matches("-?\\d{1,4}"));
        offsetXEditBox.setTooltip(Tooltip.create(Component.literal("Definir offset X")));
        offsetXEditBox.setBordered(true);
        offsetXEditBox.setEditable(true);
        offsetXEditBox.setFocused(false);
        offsetXEditBox.setVisible(true);
        offsetXEditBox.setResponder(val -> onEditBoxChanged("offsetX", val));
        this.addRenderableWidget(offsetXEditBox);
        // Offset Y
        offsetYEditBox = new EditBox(this.font, this.leftPos + offsetBtnX - boxWidth, this.topPos + offsetYBtnY, boxWidth, boxHeight, Component.literal("offsetY"));
        offsetYEditBox.setValue(String.valueOf(menu.blockEntity.getOffsetY()));
        offsetYEditBox.setMaxLength(4);
        offsetYEditBox.setFilter(s -> s.isEmpty() || s.equals("-") || s.matches("-?\\d{1,4}"));
        offsetYEditBox.setTooltip(Tooltip.create(Component.literal("Definir offset Y")));
        offsetYEditBox.setBordered(true);
        offsetYEditBox.setEditable(true);
        offsetYEditBox.setFocused(false);
        offsetYEditBox.setVisible(true);
        offsetYEditBox.setResponder(val -> onEditBoxChanged("offsetY", val));
        this.addRenderableWidget(offsetYEditBox);
        // Offset Z
        offsetZEditBox = new EditBox(this.font, this.leftPos + offsetBtnX - boxWidth, this.topPos + offsetZBtnY, boxWidth, boxHeight, Component.literal("offsetZ"));
        offsetZEditBox.setValue(String.valueOf(menu.blockEntity.getOffsetZ()));
        offsetZEditBox.setMaxLength(4);
        offsetZEditBox.setFilter(s -> s.isEmpty() || s.equals("-") || s.matches("-?\\d{1,4}"));
        offsetZEditBox.setTooltip(Tooltip.create(Component.literal("Definir offset Z")));
        offsetZEditBox.setBordered(true);
        offsetZEditBox.setEditable(true);
        offsetZEditBox.setFocused(false);
        offsetZEditBox.setVisible(true);
        offsetZEditBox.setResponder(val -> onEditBoxChanged("offsetZ", val));
        this.addRenderableWidget(offsetZEditBox);
    }

    private void onEditBoxChanged(String axis, String value) {
        try {
            int v = Integer.parseInt(value);
            
            switch (axis) {
                case "x", "y", "z" -> {
                    int max = getMaxValueForAxis(axis);
                    v = Math.max(0, Math.min(v, max));
                    
                    // Verifica o volume total
                    int x = axis.equals("x") ? v : parseIntSafe(xEditBox.getValue());
                    int y = axis.equals("y") ? v : parseIntSafe(yEditBox.getValue());
                    int z = axis.equals("z") ? v : parseIntSafe(zEditBox.getValue());
                    
                    int maxVolume = menu.blockEntity.getMaxVolume();
                    int volume = x * y * z;
                    if (volume > maxVolume) {
                        // Reduz o valor atual até o volume caber no range card
                        while (x * y * z > maxVolume && v > 0) {
                            v--;
                            if (axis.equals("x")) x = v;
                            if (axis.equals("y")) y = v;
                            if (axis.equals("z")) z = v;
                        }
                    }
                    
                    // Atualiza o valor na caixa de texto se necessário
                    EditBox editBox = getEditBoxForAxis(axis);
                    if (!editBox.getValue().equals(String.valueOf(v))) {
                        editBox.setValue(String.valueOf(v));
                    }
                }
                case "offsetX", "offsetY", "offsetZ" -> {
                    // Offset fixo de ±250 blocks
                    int maxOffset = 250;
                    v = Math.max(-maxOffset, Math.min(v, maxOffset));
                    
                    // Atualiza o valor na caixa de texto se necessário
                    EditBox editBox = getEditBoxForAxis(axis);
                    if (!editBox.getValue().equals(String.valueOf(v))) {
                        editBox.setValue(String.valueOf(v));
                    }
                }
            }
            
            // Envia o pacote para o servidor
            if (menu.blockEntity != null && menu.blockEntity.getLevel() != null && menu.blockEntity.getLevel().isClientSide()) {
                ModMessages.sendToServer(new com.Infinity.Nexus.Utils.networking.packet.SetTerraformAreaSizeC2SPacket(
                        menu.blockEntity.getBlockPos(), axis, v
                ));
            }
        } catch (NumberFormatException ignored) {}
    }

    private void sendMovePacket(String axis, int delta) {
        int finalDelta = delta;
        if (Screen.hasControlDown()) {
            finalDelta = delta > 0 ? 50 : -50;
        } else if (Screen.hasShiftDown()) {
            finalDelta = delta > 0 ? 10 : -10;
        }
        if (menu.blockEntity != null && menu.blockEntity.getLevel() != null && menu.blockEntity.getLevel().isClientSide()) {
            ModMessages.sendToServer(new com.Infinity.Nexus.Utils.networking.packet.MoveTerraformAreaC2SPacket(
                    menu.blockEntity.getBlockPos(), axis, finalDelta
            ));
        }
    }

    private void onAreaButtonClick(Button button) {
        showArea = !showArea;
        button.setTooltip(Tooltip.create(Component.translatable(
                showArea ? "gui.infinity_nexus_utils.terraform.hide_area"
                        : "gui.infinity_nexus_utils.terraform.show_area"
        )));

        if (menu.blockEntity != null && menu.blockEntity.getLevel() != null && menu.blockEntity.getLevel().isClientSide()) {
            ModMessages.sendToServer(new ToggleAreaC2SPacket(
                    menu.blockEntity.getBlockPos(),
                    showArea
            ));
        }
    }
    private void assignEnergyInfoArea() {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        energyInfoArea = new EnergyInfoArea(x + 159, y + 6, menu.getEnergyStorage());
    }
    @Override
    protected void renderLabels(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        pGuiGraphics.drawString(this.font,this.playerInventoryTitle,8,74,0XFFFFFF);
        pGuiGraphics.drawString(this.font,this.title,8,-9,0XFFFFFF);

        // Renderiza o valor do volume da área
        int sizeX = menu.blockEntity.getSizeX();
        int sizeY = menu.blockEntity.getSizeY();
        int sizeZ = menu.blockEntity.getSizeZ();
        int volume = sizeX * sizeY * sizeZ;
        int maxVolumeCard = menu.blockEntity.getMaxVolume();
        int maxVolumeConfig = ModConfigs.terraformMaxArea;
        int maxVolume = Math.min(maxVolumeCard, maxVolumeConfig);
        pGuiGraphics.drawString(this.font, Component.literal("Área: " + volume + "/" + maxVolume), 80, 74, 0xFFFFFF);

        renderEnergyAreaTooltips(pGuiGraphics,pMouseX,pMouseY, x, y);

        InfoArea.draw(pGuiGraphics);
        super.renderLabels(pGuiGraphics, pMouseX, pMouseY);
    }
    private void handleExampleButton(AbstractButton button) {
        if (menu.blockEntity != null && menu.blockEntity.getLevel() != null && menu.blockEntity.getLevel().isClientSide()) {
            ModMessages.sendToServer(new ShowTerraformAreaC2SPacket(menu.blockEntity.getBlockPos()));
        }
    }
    private void renderEnergyAreaTooltips(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, int x, int y) {
        if(isMouseAboveArea(pMouseX, pMouseY, x, y, 159,  6, 6, 62)) {
            pGuiGraphics.renderTooltip(this.font, energyInfoArea.getTooltips(), Optional.empty(), pMouseX - x, pMouseY - y);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float pPartialTick, int pMouseX, int pMouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        guiGraphics.blit(TEXTURE, x + 2, y-14, 2, 167, 174, 64);
        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        if(Screen.hasShiftDown()){
            RenderScreenTooltips.renderComponentSlotTooltip(guiGraphics, TEXTURE, x - 15, y + 10, 193, 84, 18, 131);
        }else{
            RenderScreenTooltips.renderComponentSlotTooltip(guiGraphics, TEXTURE, x - 3, y + 10, 193, 84, 4, 131);
        }

        renderProgressArrow(guiGraphics, x, y);
        energyInfoArea.render(guiGraphics);
    }

    private void renderProgressArrow(GuiGraphics guiGraphics, int x, int y) {
        if(menu.isCrafting()) {
            guiGraphics.blit(TEXTURE, x + 210, y + 68 , 240, 62,  -64, -menu.getScaledProgress());

        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        boolean enabled = menu.blockEntity.isTerraformingEnabled();
        if (terraformingEnabled != enabled) {
            terraformingEnabled = enabled;
            if (terraformingButton != null) {
                terraformingButton.setTooltip(Tooltip.create(Component.translatable(
                        terraformingEnabled ? "gui.infinity_nexus_utils.terraform.disable_terraforming"
                                : "gui.infinity_nexus_utils.terraform.enable_terraforming"
                )));
            }
        }
        super.render(guiGraphics, mouseX, mouseY, delta);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }


    private boolean isMouseAboveArea(int pMouseX, int pMouseY, int x, int y, int offsetX, int offsetY, int width, int height) {
        return MouseUtil.isMouseOver(pMouseX, pMouseY, x + offsetX, y + offsetY, width, height);
    }

    private int getMaxValueForAxis(String axis) {
        return switch (axis) {
            case "x", "y", "z" -> 100;
            case "offsetX", "offsetY", "offsetZ" -> 250;
            default -> 0;
        };
    }

    private void onTerraformingButtonClick(Button button) {
        terraformingEnabled = !terraformingEnabled;
        button.setTooltip(Tooltip.create(Component.translatable(
                terraformingEnabled ? "gui.infinity_nexus_utils.terraform.disable_terraforming"
                        : "gui.infinity_nexus_utils.terraform.enable_terraforming"
        )));

        if (menu.blockEntity != null && menu.blockEntity.getLevel() != null && menu.blockEntity.getLevel().isClientSide()) {
            ModMessages.sendToServer(new ToggleTerraformingC2SPacket(
                    menu.blockEntity.getBlockPos(),
                    terraformingEnabled
            ));
        }
    }
}