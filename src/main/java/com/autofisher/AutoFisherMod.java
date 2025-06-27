package com.autofisher;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

public class AutoFisherMod implements ClientModInitializer {
    private static final int MIN_REEL_DELAY   = 2;   // 100 ms
    private static final int MAX_REEL_DELAY   = 5;   // 250 ms
    private static final int MIN_RECAST_DELAY = 20;  // 1 s
    private static final int MAX_RECAST_DELAY = 40;  // 2 s

    private static final CopyOnWriteArrayList<Task> TASKS = new CopyOnWriteArrayList<>();
    private static final Random RNG = new Random();

    private KeyBinding toggleKey;
    private boolean     enabled = false;

    @Override
    public void onInitializeClient() {
        // bind F to toggle on/off
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.autofisher.toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F,
                "category.autofisher"
        ));

        // every tick: toggle, run tasks, auto-cast if needed
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (toggleKey.wasPressed() && client.player != null) {
                enabled = !enabled;
                client.player.sendMessage(
                        Text.of("AutoFisher " + (enabled ? "ENABLED" : "DISABLED")),
                        true
                );
            }
            if (!enabled || client.player == null || client.world == null) return;

            // run scheduled reels & recasts
            TASKS.removeIf(t -> {
                if (t.delay-- <= 0) {
                    t.action.run();
                    return true;
                }
                return false;
            });

            // if you don’t have a bobber in‐world, cast immediately
            PlayerEntity me = client.player;
            Box scanArea = me.getBoundingBox().expand(64.0);
            List<FishingBobberEntity> bobbers = client.world.getEntitiesByClass(
                    FishingBobberEntity.class,
                    scanArea,
                    b -> {
                        PlayerEntity owner = b.getPlayerOwner();
                        return owner != null && owner == me;
                    }
            );
            if (bobbers.isEmpty()) {
                schedule(() -> castRod(client), 0);
            }
        });
    }

    /** Called by our Mixin on the exact tick a fish becomes catchable. */
    public static void onBobberBite(FishingBobberEntity bobber) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        // only for your line
        PlayerEntity owner = bobber.getPlayerOwner();
        if (owner == null || owner != mc.player) return;

        // human-like reaction delay
        int reelTicks = RNG.nextInt(MAX_REEL_DELAY - MIN_REEL_DELAY + 1) + MIN_REEL_DELAY;
        schedule(() -> {
            if (mc.interactionManager != null) {
                mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                mc.player.swingHand(Hand.MAIN_HAND);
            }
            // then recast in 1–2s
            int recast = RNG.nextInt(MAX_RECAST_DELAY - MIN_RECAST_DELAY + 1) + MIN_RECAST_DELAY;
            schedule(() -> castRod(mc), recast);
        }, reelTicks);
    }

    private static void castRod(MinecraftClient client) {
        if (client.player != null && client.interactionManager != null) {
            client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
            client.player.swingHand(Hand.MAIN_HAND);
        }
    }

    private static void schedule(Runnable action, int delay) {
        TASKS.add(new Task(delay, action));
    }

    private static class Task {
        int delay;
        Runnable action;
        Task(int delay, Runnable action) {
            this.delay = delay;
            this.action = action;
        }
    }
}
