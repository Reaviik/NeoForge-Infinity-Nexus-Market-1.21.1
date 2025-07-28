package com.Infinity.Nexus.Market.compat;

import com.Infinity.Nexus.Market.block.custom.BuyingMachine;
import com.Infinity.Nexus.Market.block.custom.MarketMachine;
import com.Infinity.Nexus.Market.block.custom.VendingMachine;
import com.Infinity.Nexus.Market.compat.jade.MachineOwner;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class JadeModPlugin implements IWailaPlugin {
    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(MachineOwner.INSTANCE, VendingMachine.class);
        registration.registerBlockComponent(MachineOwner.INSTANCE, BuyingMachine.class);
        registration.registerBlockComponent(MachineOwner.INSTANCE, MarketMachine.class);
    }

    @Override
    public void register(IWailaCommonRegistration registration) {
    }
}