package com.julz.paletteshuffle.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import java.util.function.Supplier;

abstract class PaletteScreen extends Screen {
    protected String error = "";
    protected PaletteScreen(String title) { super(Component.literal(title)); }
    protected Button button(int x, int y, int w, String label, Runnable action) {
        return addRenderableWidget(Button.builder(Component.literal(label), b -> action.run()).bounds(x,y,w,20).build());
    }
    protected Button icon(int x, int y, ItemStack stack, String tip, boolean selected, Runnable action) {
        return slot(x,y,()->stack,tip,selected,"",action);
    }
    protected Button slot(int x,int y,Supplier<ItemStack> item,String tip,boolean selected,String label,Runnable action) {
        Button b = new Button(x,y,24,24,Component.literal(tip),ignored->action.run(),supplier->supplier.get()) {
            @Override public void extractContents(GuiGraphicsExtractor g,int mx,int my,float delta) {
                int sx=getX(),sy=getY();
                g.fill(sx,sy,sx+24,sy+24,0xFF282828);
                g.outline(sx,sy,24,24,selected?0xFFB7DD82:isHoveredOrFocused()?0xFFFFFFFF:0xFF8B8B8B);
                g.fill(sx+2,sy+2,sx+22,sy+22,0xFF474747);
                ItemStack stack=item.get();
                if(!stack.isEmpty()) {
                    g.item(stack,sx+4,sy+4);
                    g.itemDecorations(font,stack,sx+4,sy+4);
                } else if(label.isEmpty()) g.centeredText(font,"+",sx+12,sy+8,0xFFAAAAAA);
                if(!label.isEmpty())g.text(font,label,sx+2,sy+1,0xFFCCCCCC);
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
