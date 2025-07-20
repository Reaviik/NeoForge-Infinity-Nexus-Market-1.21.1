package com.Infinity.Nexus.Utils.compat;

import com.Infinity.Nexus.Utils.block.custom.Terraform;
import com.Infinity.Nexus.Utils.compat.jade.TerraformOwner;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class JadeModPlugin implements IWailaPlugin {
    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(TerraformOwner.INSTANCE, Terraform.class);
    }

    @Override
    public void register(IWailaCommonRegistration registration) {
    }
}