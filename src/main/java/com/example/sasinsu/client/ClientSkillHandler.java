package com.example.sasinsu.client;

import com.example.sasinsu.SasinSuMod;
import com.example.sasinsu.network.SasinSuNetwork;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = SasinSuMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClientSkillHandler {
    private static long lastSneakPress = 0L;
    private static long lastSpecialSent = 0L;

    private ClientSkillHandler() {}

    @SubscribeEvent
    public static void onMouseButton(InputEvent.MouseButton.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null || !minecraft.player.isCrouching()
                || event.getAction() != GLFW.GLFW_PRESS) return;

        int skill = event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT ? 2
                : event.getButton() == GLFW.GLFW_MOUSE_BUTTON_RIGHT ? 3 : 0;
        if (skill != 0) {
            SasinSuNetwork.sendSkill(skill);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onKey(InputEvent.Key event) {
        if (event.getAction() != GLFW.GLFW_PRESS
                || (event.getKey() != GLFW.GLFW_KEY_LEFT_SHIFT
                && event.getKey() != GLFW.GLFW_KEY_RIGHT_SHIFT)) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null) return;
        long now = System.currentTimeMillis();
        if (now - lastSneakPress <= 360L && now - lastSpecialSent > 500L) {
            SasinSuNetwork.sendSkill(4);
            lastSpecialSent = now;
            lastSneakPress = 0L;
        } else {
            lastSneakPress = now;
        }
    }
}
