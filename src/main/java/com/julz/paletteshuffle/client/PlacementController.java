package com.julz.paletteshuffle.client;

import com.julz.paletteshuffle.core.Palette;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import java.util.Random;

/** Uses ordinary container swap inputs; never creates survival inventory items. */
public final class PlacementController {
    private Palette active;
    private int slot;
    private boolean needsBlock;
    private int delay;
    private String expected;
    private final Random random = new Random();
    public Palette active() { return active; }
    public int slot() { return slot; }
    public void start(Palette palette, int slot) {
        if (slot < 0 || slot > 8) throw new IllegalArgumentException("Hotbar slot must be 0–8");
        this.active = palette; this.slot = slot; needsBlock = true; expected = null; delay = 0;
        message("Palette: " + palette.name() + " · slot " + (slot + 1));
    }
    public void stop() { active = null; expected = null; needsBlock = false; }
    public void placed() {
        var mc = Minecraft.getInstance();
        if (active != null && mc.player != null && mc.player.getInventory().getSelectedSlot() == slot) {
            needsBlock = true; delay = 2;
        }
    }
    public boolean ready() { return active == null || (!needsBlock && delay == 0); }
    public void tick(Minecraft mc) {
        if (mc.player == null || mc.level == null) { stop(); return; }
        if (active == null) return;
        if (!mc.player.isAlive() || mc.player.isSpectator()) { stop(); return; }
        if (mc.gui.screen() != null || mc.gameMode == null || mc.player.containerMenu != mc.player.inventoryMenu
                || !mc.player.inventoryMenu.getCarried().isEmpty()) return;
        if (delay > 0) { delay--; return; }
        var inventory = mc.player.getInventory();
        if (inventory.getSelectedSlot() != slot) return;
        boolean creative = mc.player.getAbilities().instabuild;
        if (!needsBlock) {
            if (expected != null && !Blocks.id(inventory.getItem(slot).getItem()).equals(expected)) {
                stop(); message("Palette stopped: working slot changed.");
            }
            return;
        }
        var next = active.pick(id -> Blocks.valid(id) && (creative || find(mc, id) >= 0), random);
        if (next.isEmpty()) { stop(); message("Palette stopped: no available blocks."); return; }
        String id = next.get().blockId();
        if (creative) {
            ItemStack stack = Blocks.stack(id);
            stack.setCount(stack.getMaxStackSize());
            inventory.setItem(slot, stack);
            mc.gameMode.handleCreativeModeItemAdd(stack, 36 + slot);
        } else {
            int source = find(mc, id);
            if (source < 0) return;
            if (source != slot) {
                int menuSlot = source < 9 ? 36 + source : source;
                mc.gameMode.handleContainerInput(mc.player.inventoryMenu.containerId, menuSlot, slot,
                        ContainerInput.SWAP, mc.player);
            }
        }
        expected = id; needsBlock = false;
        // Allow the swap to be sent before the next placement packet.
        delay = 2;
    }
    public static int find(Minecraft mc, String id) {
        if (mc.player == null) return -1;
        for (int i = 0; i < 36; i++) {
            var stack = mc.player.getInventory().getItem(i);
            if (!stack.isEmpty() && Blocks.id(stack.getItem()).equals(id)) return i;
        }
        return -1;
    }
    private void message(String text) {
        var p = Minecraft.getInstance().player;
        if (p != null) p.sendOverlayMessage(Component.literal(text));
    }
}
