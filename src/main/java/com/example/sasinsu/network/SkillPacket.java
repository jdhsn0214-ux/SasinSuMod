package com.example.sasinsu.network;

import com.example.sasinsu.item.DivineWeaponItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SkillPacket {
    private final int skill;

    public SkillPacket(int skill) {
        this.skill = skill;
    }

    public static void encode(SkillPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.skill);
    }

    public static SkillPacket decode(FriendlyByteBuf buffer) {
        return new SkillPacket(buffer.readVarInt());
    }

    public static void handle(SkillPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || packet.skill < 2 || packet.skill > 4) return;
            ItemStack stack = player.getMainHandItem();
            if (!(stack.getItem() instanceof DivineWeaponItem weapon)) {
                stack = player.getOffhandItem();
                if (!(stack.getItem() instanceof DivineWeaponItem)) return;
                weapon = (DivineWeaponItem) stack.getItem();
            }
            weapon.activateSkill(player, packet.skill, stack);
        });
        context.setPacketHandled(true);
    }
}
