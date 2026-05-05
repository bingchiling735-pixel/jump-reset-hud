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
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

public class JumpResetHudClient implements ClientModInitializer {
    private static final long RESET_AFTER_MS = 1000;
    private static final int BAR_HALF_WIDTH = 60;
    private static final int BAR_HEIGHT = 6;
    private static final int MAX_MS_FOR_BAR = 200;

    private HudConfig config;
    private long lastHitTime = -1;
    private long lastActionTime = -1;
    private Integer currentDelay = null;
    private int previousHurtTime = 0;
    private boolean lastJumpPressed = false;
    private boolean dragging = false;
    private boolean lastMousePressed = false;
    private int dragOffsetX;
    private int dragOffsetY;

    private KeyBinding toggleHud;

    @Override
    public void onInitializeClient() {
        config = ConfigManager.load();

        toggleHud = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.jumpresethud.toggle",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_H,
            "category.jumpresethud"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
        HudRenderCallback.EVENT.register(this::renderHud);
    }

    private void onTick(MinecraftClient client) {
        while (toggleHud.wasPressed()) {
            config.enabled = !config.enabled;
            ConfigManager.save(config);
        }

        if (!config.enabled || client.player == null) {
            return;
        }

        long now = System.currentTimeMillis();
        trackHit(client.player, now);
        trackJump(client, now);
        updateAutoReset(now);
        updateDragging(client);
    }

    private void trackHit(PlayerEntity player, long now) {
        int hurtTime = player.hurtTime;
        if (hurtTime > 0 && previousHurtTime <= 0) {
            lastHitTime = now;
            lastActionTime = now;
        }
        previousHurtTime = hurtTime;
    }

    private void trackJump(MinecraftClient client, long now) {
        boolean jumpPressed = client.options.jumpKey.isPressed();
        if (jumpPressed && !lastJumpPressed && lastHitTime > 0) {
            currentDelay = (int) (now - lastHitTime);
            lastActionTime = now;
        }
        lastJumpPressed = jumpPressed;
    }

    private void updateAutoReset(long now) {
        if (lastActionTime > 0 && now - lastActionTime >= RESET_AFTER_MS) {
            currentDelay = null;
            lastHitTime = -1;
            lastActionTime = -1;
        }
    }

    private void updateDragging(MinecraftClient client) {
        if (client.currentScreen == null) {
            dragging = false;
            lastMousePressed = false;
            return;
        }

        long window = client.getWindow().getHandle();
        boolean mouseDown = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        double mouseX = client.mouse.getX() * client.getWindow().getScaledWidth() / client.getWindow().getWidth();
        double mouseY = client.mouse.getY() * client.getWindow().getScaledHeight() / client.getWindow().getHeight();

        int x = config.finalX();
        int y = config.finalY();
        int width = 140;
        int height = 24;

        if (mouseDown && !lastMousePressed && inside(mouseX, mouseY, x, y, width, height)) {
            dragging = true;
            dragOffsetX = (int) mouseX - x;
            dragOffsetY = (int) mouseY - y;
        }

        if (!mouseDown) {
            dragging = false;
        }

        if (dragging) {
            config.posX = (int) mouseX - dragOffsetX - config.offsetX;
            config.posY = (int) mouseY - dragOffsetY - config.offsetY;
            ConfigManager.save(config);
        }

        lastMousePressed = mouseDown;
    }

    private boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private void renderHud(DrawContext context, net.minecraft.client.render.RenderTickCounter tickCounter) {
        if (!config.enabled) {
            return;
        }

        int x = config.finalX();
        int y = config.finalY();

        String status;
        int color;
        if (currentDelay == null) {
            status = "Waiting...";
            color = 0xFFAAAAAA;
        } else if (currentDelay <= 50) {
            status = "Perfect";
            color = 0xFF55FF55;
        } else if (currentDelay <= 120) {
            status = "Good";
            color = 0xFFFFFF55;
        } else {
            status = "Bad";
            color = 0xFFFF5555;
        }

        String text = currentDelay == null ? "JR: Waiting..." : "JR: " + status + " (" + currentDelay + "ms)";
        context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, text, x, y, color);
        drawTimingBar(context, x, y + 12);

        if (dragging) {
            context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, Text.literal("Dragging").formatted(Formatting.DARK_GRAY), x, y + 20, 0xFF666666);
        }
    }

    private void drawTimingBar(DrawContext context, int x, int y) {
        int left = x;
        int right = x + BAR_HALF_WIDTH * 2;
        int center = x + BAR_HALF_WIDTH;

        context.fill(left, y, right, y + BAR_HEIGHT, 0xAA222222);
        context.fill(center - 1, y - 1, center + 1, y + BAR_HEIGHT + 1, 0xFF00FF00);

        if (currentDelay != null) {
            int clamped = Math.min(MAX_MS_FOR_BAR, Math.abs(currentDelay));
            int direction = currentDelay >= 0 ? 1 : -1;
            int markerX = center + direction * (BAR_HALF_WIDTH * clamped / MAX_MS_FOR_BAR);
            context.fill(markerX - 1, y - 2, markerX + 1, y + BAR_HEIGHT + 2, 0xFFFFFFFF);
        }
    }
}
