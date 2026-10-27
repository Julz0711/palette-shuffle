package com.julz.paletteshuffle.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

abstract class PaletteScreen extends Screen {
    protected String error = "";
    protected PaletteScreen(String title) { super(Component.literal(title)); }
    protected Button button(int x, int y, int w, String label, Runnable action) {
        return addRenderableWidget(Button.builder(Component.literal(label), b -> action.run()).bounds(x,y,w,20).build());
    }
    protected Button icon(int x, int y, ItemStack stack, String tip, boolean selected, Runnable action) {
        Button b = new Button(x,y,24,24, Component.literal(tip), ignored -> action.run(), supplier -> supplier.get()) {
            @Override public void extractContents(GuiGraphicsExtractor g, int mx, int my, float delta) {
                g.item(stack, getX()+4, getY()+4);
                if (selected) g.outline(getX(), getY(), getWidth(), getHeight(), 0xFFB7DD82);
            }
        };
        b.setTooltip(Tooltip.create(Component.literal(tip)));
        return addRenderableWidget(b);
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        // Screen.extractRenderStateWithTooltipAndSubtitles already draws the background.
        // Requesting it here again would schedule a second blur and crash in 26.3.
        super.extractRenderState(g,mx,my,delta);
        g.centeredText(font,title,width/2,12,0xFFFFFFFF);
        if (!error.isEmpty()) g.centeredText(font,error,width/2,height-34,0xFFFF9999);
    }
    @Override public boolean isPauseScreen() { return false; }
}
