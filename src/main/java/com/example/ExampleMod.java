package net.fabricmc.example;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.lwjgl.glfw.GLFW;

public class ExampleMod implements ClientModInitializer {
    private static KeyBinding toggleKey;
    private boolean enabled = false;
    private boolean awaitingApex = false;
    private double lastYVelocity = 0;

    @Override
    public void onInitializeClient() {
        // Register the Toggle Key (Default is 'K')
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.automace.toggle", 
                InputUtil.Type.KEYSYM, 
                GLFW.GLFW_KEY_K, 
                "category.automace"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            // Handle Toggle Press
            while (toggleKey.wasPressed()) {
                enabled = !enabled;
                String state = enabled ? "§aENABLED" : "§cDISABLED";
                client.player.sendMessage(Text.literal("AutoMace is now " + state), true);
            }

            if (!enabled) return;

            double currentYVel = client.player.getVelocity().y;

            // 1. Apex Detection for Elytra
            if (client.player.getMainHandStack().isOf(Items.WIND_CHARGE)) awaitingApex = true;
            
            if (awaitingApex && lastYVelocity > 0 && currentYVel <= 0) {
                int elytraIdx = findItem(client, Items.ELYTRA);
                if (elytraIdx != -1) {
                    swapItem(client, 6, elytraIdx);
                    client.options.jumpKey.setPressed(true);
                }
                awaitingApex = false;
            }
            lastYVelocity = currentYVel;

            // 2. The Slab Strike (Mace & Chestplate Swap)
            HitResult hit = client.crosshairTarget;
            if (hit instanceof EntityHitResult entityHit) {
                double dist = client.player.getY() - entityHit.getEntity().getEyeY();

                if (dist > 0 && dist <= 0.65) {
                    int maceSlot = findItemInHotbar(client, Items.MACE);
                    int plateIdx = findItem(client, Items.NETHERITE_CHESTPLATE);

                    if (maceSlot != -1) client.player.getInventory().selectedSlot = maceSlot;
                    if (plateIdx != -1) swapItem(client, 6, plateIdx);
                    
                    client.interactionManager.attackEntity(client.player, entityHit.getEntity());
                }
            }
        });
    }

    private void swapItem(MinecraftClient client, int slot, int invIdx) {
        client.interactionManager.clickSlot(client.player.playerScreenHandler.syncId, slot, invIdx, SlotActionType.SWAP, client.player);
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
