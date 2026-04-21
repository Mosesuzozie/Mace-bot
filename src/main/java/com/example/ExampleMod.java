package net.fabricmc.example;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

public class ExampleMod implements ClientModInitializer {
    // State tracking for the combo
    private boolean awaitingApex = false;
    private double lastYVelocity = 0;
    private final double SLAB_THRESHOLD = 0.65; // Distance (in blocks) to trigger the hit

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            // 1. Apex Detection (Detecting the top of your Wind Charge jump)
            double currentYVel = client.player.getVelocity().y;

            // Trigger "Awaiting Apex" mode if player uses a wind charge
            if (client.player.isUsingItem() && client.player.getMainHandStack().isOf(Items.WIND_CHARGE)) {
                awaitingApex = true;
            }

            // If we are at the peak (velocity flips from up to down), swap to Elytra
            if (awaitingApex && lastYVelocity > 0 && currentYVel <= 0) {
                int elytraIdx = findItemInInventory(client, Items.ELYTRA);
                if (elytraIdx != -1) {
                    performArmorSwap(client, elytraIdx);
                    client.options.jumpKey.setPressed(true); // Start gliding
                }
                awaitingApex = false;
            }
            lastYVelocity = currentYVel;

            // 2. The Slab Strike (Auto-Mace & Chestplate Swap)
            HitResult hit = client.crosshairTarget;
            if (hit instanceof EntityHitResult entityHit) {
                // Check vertical distance from feet to enemy head
                double distance = client.player.getY() - entityHit.getEntity().getEyeY();

                if (distance > 0 && distance <= SLAB_THRESHOLD) {
                    int maceSlot = findItemInHotbar(client, Items.MACE);
                    int chestplateIdx = findItemInInventory(client, Items.NETHERITE_CHESTPLATE);

                    // Switch to Mace
                    if (maceSlot != -1) {
                        client.player.getInventory().selectedSlot = maceSlot;
                    }

                    // Hot-swap Elytra back to Chestplate for protection/damage
                    if (chestplateIdx != -1) {
                        performArmorSwap(client, chestplateIdx);
                    }

                    // Send Attack Packet
                    client.interactionManager.attackEntity(client.player, entityHit.getEntity());
                }
            }
        });
    }

    /**
     * Internal helper to swap an inventory item into the Chest Armor Slot (Slot 6)
     */
    private void performArmorSwap(MinecraftClient client, int invSlot) {
        int syncId = client.player.playerScreenHandler.syncId;
        client.interactionManager.clickSlot(syncId, 6, invSlot, SlotActionType.SWAP, client.player);
    }

    /**
     * Finds an item anywhere in the 36-slot player inventory
     */
    private int findItemInInventory(MinecraftClient client, net.minecraft.item.Item item) {
        for (int i = 0; i < 36; i++) {
            if (client.player.getInventory().getStack(i).isOf(item)) return i;
        }
        return -1;
    }

    /**
     * Finds an item specifically in the 0-8 hotbar slots
     */
    private int findItemInHotbar(MinecraftClient client, net.minecraft.item.Item item) {
        for (int i = 0; i < 9; i++) {
            if (client.player.getInventory().getStack(i).isOf(item)) return i;
        }
        return -1;
    }
}
