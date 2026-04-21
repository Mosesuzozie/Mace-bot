package net.fabricmc.example;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

public class ExampleMod implements ClientModInitializer {
    private boolean awaitingApex = false;
    private double lastYVelocity = 0;
    private final double ATTACK_RANGE = 0.65; // The "Slab" distance

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            double currentYVel = client.player.getVelocity().y;

            // 1. APEX DETECTION (Peak of the Wind Charge)
            // Check if player just used a Wind Charge and reached the peak height
            if (client.player.getMainHandStack().isOf(Items.WIND_CHARGE)) {
                awaitingApex = true; 
            }

            if (awaitingApex && lastYVelocity > 0 && currentYVel <= 0) {
                handleElytraSwap(client);
                awaitingApex = false;
            }
            lastYVelocity = currentYVel;

            // 2. THE STRIKE (Look down, check distance, swap and hit)
            processMaceStrike(client);
        });
    }

    private void handleElytraSwap(MinecraftClient client) {
        int elytra = findItem(client, Items.ELYTRA);
        if (elytra != -1) {
            // Swap to armor slot 6 (Chest)
            client.interactionManager.clickSlot(client.player.playerScreenHandler.syncId, 6, elytra, SlotActionType.SWAP, client.player);
            // Engage glide
            client.options.jumpKey.setPressed(true);
        }
    }

    private void processMaceStrike(MinecraftClient client) {
        HitResult hit = client.crosshairTarget;
        if (hit instanceof EntityHitResult entityHit) {
            // Calculate distance between feet and enemy head
            double dist = client.player.getY() - entityHit.getEntity().getEyeY();

            if (dist > 0 && dist <= ATTACK_RANGE) {
                int mace = findItemInHotbar(client, Items.MACE);
                int plate = findItem(client, Items.NETHERITE_CHESTPLATE);

                // Execution: Swap Mace -> Swap Chestplate -> Attack
                if (mace != -1) client.player.getInventory().selectedSlot = mace;
                if (plate != -1) {
                    client.interactionManager.clickSlot(client.player.playerScreenHandler.syncId, 6, plate, SlotActionType.SWAP, client.player);
                }
                
                // Final Mace Smash
                client.interactionManager.attackEntity(client.player, entityHit.getEntity());
            }
        }
    }

    private int findItem(MinecraftClient client, net.minecraft.item.Item item) {
        for (int i = 0; i < 36; i++) {
            if (client.player.getInventory().getStack(i).isOf(item)) return i;
        }
        return -1;
    }

    private int findItemInHotbar(MinecraftClient client, net.minecraft.item.Item item) {
        for (int i = 0; i < 9; i++) {
            if (client.player.getInventory().getStack(i).isOf(item)) return i;
        }
        return -1;
    }
}
