package com.example.sasinsu.network;

import com.example.sasinsu.SasinSuMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class SasinSuNetwork {
    private static final String VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(SasinSuMod.MODID, "main"),
            () -> VERSION, VERSION::equals, VERSION::equals);
    private static boolean registered = false;

    private SasinSuNetwork() {}

    public static void register() {
        if (registered) return;
        registered = true;
        CHANNEL.registerMessage(0, SkillPacket.class, SkillPacket::encode,
                SkillPacket::decode, SkillPacket::handle);
    }

    public static void sendSkill(int skill) {
        CHANNEL.sendToServer(new SkillPacket(skill));
    }
}
