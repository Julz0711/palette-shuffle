package com.julz.paletteshuffle.client;

import com.julz.paletteshuffle.config.PaletteStore;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public final class PaletteShuffleClient implements ClientModInitializer {
    public static final PaletteStore STORE = new PaletteStore(FabricLoader.getInstance().getConfigDir().resolve("palette-shuffle.json"));
    public static final PlacementController PLACEMENT = new PlacementController();
    @Override public void onInitializeClient() {
        STORE.load();
        var category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("palette-shuffle", "general"));
        var open = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.palette-shuffle.open",
                InputConstants.Type.KEYBOARD, GLFW.GLFW_KEY_B, category));
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> PLACEMENT.stop());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> PLACEMENT.stop());
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (open.consumeClick()) if (client.screen == null) client.setScreen(new GalleryScreen(null));
            PLACEMENT.tick(client);
        });
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("palette-shuffle", "status"), (g, ticks) -> {
            var mc = net.minecraft.client.Minecraft.getInstance();
            var palette = PLACEMENT.active();
            if (palette == null || mc.player == null || mc.options.hideGui) return;
            int x = g.guiWidth() / 2 - 91 + PLACEMENT.slot() * 20;
            g.outline(x, g.guiHeight() - 23, 22, 22, 0xFFB7DD82);
            if (mc.player.getInventory().getSelectedSlot() == PLACEMENT.slot())
                g.centeredText(mc.font, "Palette: " + palette.name(), g.guiWidth()/2, g.guiHeight()-65, 0xFFB7DD82);
        });
    }
}
