package com.example.sasinsu;

import com.example.sasinsu.network.SasinSuNetwork;
import com.example.sasinsu.registry.ModItems;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(SasinSuMod.MODID)
public class SasinSuMod {
    public static final String MODID = "sasinsu";

    public SasinSuMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModItems.register(bus);
        bus.addListener(ModItems::addCreative);
        SasinSuNetwork.register();
    }
}
