package com.jumpresethud;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class JumpResetHudClient implements ClientModInitializer {
    private static final long RESET_MS = 1_000L;

    private HudConfig config;

    private long lastHitTime = -1;
    private long lastActionTime = -1;
    private long lastDelay = -1;
    private int lastHurtTime = 0;
    private boolean dragging = false;
    private int dragOffsetX = 0;
    private int dragOffsetY = 0;

    private KeyBinding toggleHudKey;

    @Override
    public void onInitializeClient() {
        config = HudConfig.load();
        toggleHudKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.jumpresethud.toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                "category.jumpresethud"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
        HudRenderCallback.EVENT.register(this::renderHud);
    }

    private void onTick(MinecraftClient client) {
        while (toggleHudKey.wasPressed()) {
            config.enabled = !config.enabled;
            config.save();
        }

        if (client.player == null) {
            return;
        }

        updateHitTracking(client.player);
        updateJumpTracking(client);
        updateAutoReset();
        updateDragging(client);
    }

    private void updateHitTracking(PlayerEntity player) {
        if (player.hurtTime > 0 && lastHurtTime == 0) {
            lastHitTime = System.currentTimeMillis();
            lastActionTime = lastHitTime;
        }
        lastHurtTime = player.hurtTime;
    }

    private void updateJumpTracking(MinecraftClient client) {
        if (client.options.jumpKey.wasPressed() && lastHitTime > 0) {
            long now = System.currentTimeMillis();
            lastDelay = now - lastHitTime;
            lastActionTime = now;
        }
    }

    private void updateAutoReset() {
        if (lastActionTime <= 0) {
            return;
        }

        if (System.currentTimeMillis() - lastActionTime >= RESET_MS) {
            lastDelay = -1;
            lastHitTime = -1;
            lastActionTime = -1;
        }
    }

    private void renderHud(DrawContext context, float tickDelta) {
        if (!config.enabled) {
            return;
        }

        int x = config.finalX();
        int y = config.finalY();

        context.getMatrices().push();
        context.getMatrices().scale(config.scale, config.scale, 1.0f);

        int drawX = (int) (x / config.scale);
        int drawY = (int) (y / config.scale);

        String label;
        int color;
        if (lastDelay < 0) {
            label = "JR: Waiting...";
            color = 0xFFFFFF;
        } else if (lastDelay <= 50) {
            label = "JR: Perfect (" + lastDelay + "ms)";
            color = 0x00FF00;
        } else if (lastDelay <= 120) {
            label = "JR: Good (" + lastDelay + "ms)";
            color = 0xFFFF00;
        } else {
            label = "JR: Bad (" + lastDelay + "ms)";
            color = 0xFF4444;
        }

        context.drawText(MinecraftClient.getInstance().textRenderer, Text.literal(label), drawX, drawY, color, true);
        drawTimingBar(context, drawX, drawY + 14);

        context.getMatrices().pop();
    }

    private void drawTimingBar(DrawContext context, int x, int y) {
        int width = 120;
        int height = 8;
        int center = x + width / 2;

        context.fill(x, y, x + width, y + height, 0xAA222222);
        context.fill(center - 1, y, center + 1, y + height, 0xFFFFFFFF);

        if (lastDelay >= 0) {
            int clamped = (int) Math.min(240, lastDelay);
            int markerOffset = (int) (((float) clamped / 240.0f) * (width / 2f));
            int markerX = center + markerOffset;
            context.fill(markerX - 1, y - 2, markerX + 1, y + height + 2, 0xFF00BFFF);
        }
    }

    private void updateDragging(MinecraftClient client) {
        if (client.currentScreen != null) {
            dragging = false;
            return;
        }

        long window = client.getWindow().getHandle();
        boolean leftDown = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;

        int mouseX = (int) (client.mouse.getX() * client.getWindow().getScaledWidth() / client.getWindow().getWidth());
        int mouseY = (int) (client.mouse.getY() * client.getWindow().getScaledHeight() / client.getWindow().getHeight());

        int hudX = config.finalX();
        int hudY = config.finalY();
        int width = (int) (130 * config.scale);
        int height = (int) (24 * config.scale);

        if (leftDown && !dragging && mouseX >= hudX && mouseX <= hudX + width && mouseY >= hudY && mouseY <= hudY + height) {
            dragging = true;
            dragOffsetX = mouseX - config.posX;
            dragOffsetY = mouseY - config.posY;
        }

        if (!leftDown && dragging) {
            dragging = false;
            config.save();
        }

        if (dragging) {
            config.posX = mouseX - dragOffsetX;
            config.posY = mouseY - dragOffsetY;
        }
    }
}
