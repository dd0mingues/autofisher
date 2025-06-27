// Define the package for your mod. This should match your folder structure.
package com.autofisher;

// Import necessary Fabric API and Minecraft classes.
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

/**
 * The main class for the AutoFisher mod.
 * Implements ClientModInitializer, which is the entry point for client-side Fabric mods.
 */
public class AutoFisherMod implements ClientModInitializer {

    // Define a constant for the bobber's vertical velocity threshold.
    // When a fish bites, the bobber is pulled down, so its Y-velocity becomes negative.
    private static final double BOBBER_PULL_THRESHOLD = -0.05;

    // This method is called when the client-side mod is initialized by Fabric.
    @Override
    public void onInitializeClient() {
        // Register an event listener that gets called at the end of every client tick.
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Check if the player and world are not null.
            if (client.player == null || client.world == null) {
                return; // If not ready, do nothing.
            }

            // Check if the player is currently holding a fishing rod in their main hand.
            if (client.player.getStackInHand(Hand.MAIN_HAND).getItem() == Items.FISHING_ROD) {

                // Attempt to find the player's active fishing bobber.
                // The PlayerEntity class has a direct reference to their fishHook entity.
                FishingBobberEntity bobber = client.player.fishHook;

                // If a bobber is found (meaning the player has cast their line)
                if (bobber != null) {
                    // Check the bobber's vertical motion (delta movement on the Y-axis).
                    // This is the corrected line using getVelocity().
                    if (bobber.getVelocity().y < BOBBER_PULL_THRESHOLD) {
                        System.out.println("[AutoFisher] Fish detected! Reeling in...");

                        // Perform the right-click action to reel in the fish.
                        performRightClick(client);
                    }
                }
            }
        });
    }

    /**
     * Helper method to simulate a right-click action in Minecraft.
     *
     * @param client The MinecraftClient instance.
     */
    private void performRightClick(MinecraftClient client) {
        // Ensure the interaction manager is not null before attempting to use it.
        if (client.interactionManager != null) {
            // interactItem sends a packet to the server, simulating a right-click.
            client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
        }
    }
}