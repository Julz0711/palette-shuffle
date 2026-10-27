package com.julz.paletteshuffle.client;

import com.julz.paletteshuffle.core.Palette;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.network.chat.Component;
import java.util.*;

public final class GalleryScreen extends PaletteScreen {
    private final Screen parent;
    private UUID selected;
    private int page;
    private int slot;
    public GalleryScreen(Screen parent) { super("Palette Shuffle"); this.parent = parent; }
    @Override protected void init() {
        var palettes = PaletteShuffleClient.STORE.all();
        if (PaletteShuffleClient.STORE.loadError() != null) error = PaletteShuffleClient.STORE.loadError();
        int cols = Math.max(1,Math.min(3,(width-20)/108));
        int rows = Math.max(1,(height-166)/80);
        int perPage=cols*rows;
        page = Math.min(page, Math.max(0,(palettes.size()-1)/perPage));
        int left=(width-cols*108)/2;
        for (int i=page*perPage;i<Math.min(palettes.size(),(page+1)*perPage);i++) {
            Palette p=palettes.get(i); int index=i-page*perPage;
            Button card = new Button(left+index%cols*108,34+index/cols*80,104,76,
                    Component.literal(p.name()), b->{selected=p.id();rebuildWidgets();},Button.DEFAULT_NARRATION) {
                @Override public void extractContents(GuiGraphicsExtractor g,int mx,int my,float delta) {
                    var active=PaletteShuffleClient.PLACEMENT.active();
                    int color=active!=null&&active.id().equals(p.id())?0xFFB7DD82:0xFFFFFFFF;
                    g.centeredText(font,font.plainSubstrByWidth(p.name(),96),getX()+52,getY()+5,color);
                    for(int j=0;j<p.entries().size();j++) g.item(Blocks.stack(p.entries().get(j).blockId()),getX()+26+j%3*18,getY()+19+j/3*18);
                    if(p.id().equals(selected))g.outline(getX(),getY(),104,76,0xFFB7DD82);
                }
            };
            addRenderableWidget(card);
        }
        int y=height-116;
        button(width/2-155,y,70,"New palette",()->minecraft.setScreen(new EditorScreen(this,null)));
        button(width/2-81,y,42,"Edit",()->edit()).active=current()!=null;
        button(width/2-35,y,68,"Duplicate",()->{
            try{PaletteShuffleClient.STORE.put(current().duplicate());rebuildWidgets();}catch(Exception e){error=e.getMessage();}
        }).active=current()!=null;
        button(width/2+37,y,54,"Delete",()->confirmDelete()).active=current()!=null;
        button(width/2+95,y,26,"<",()->{page--;rebuildWidgets();}).active=page>0;
        button(width/2+125,y,26,">",()->{page++;rebuildWidgets();}).active=(page+1)*perPage<palettes.size();
        for(int i=0;i<9;i++){
            int target=i;
            var stack=minecraft.player==null?net.minecraft.world.item.ItemStack.EMPTY:minecraft.player.getInventory().getItem(i);
            icon(width/2-108+i*24,height-80,stack,"Working slot "+(i+1),i==slot,()->{slot=target;rebuildWidgets();});
        }
        button(width/2-155,height-24,98,"Use palette",()->{
            PaletteShuffleClient.PLACEMENT.start(current(),slot);
            minecraft.player.getInventory().setSelectedSlot(slot);
            minecraft.setScreen(null);
        }).active=current()!=null&&minecraft.player!=null;
        button(width/2-53,height-24,108,"Stop using palette",()->{PaletteShuffleClient.PLACEMENT.stop();rebuildWidgets();})
                .active=PaletteShuffleClient.PLACEMENT.active()!=null;
        button(width/2+59,height-24,96,"Done",this::onClose);
    }
    private Palette current(){return PaletteShuffleClient.STORE.all().stream().filter(p->p.id().equals(selected)).findFirst().orElse(null);}
    private void edit(){minecraft.setScreen(new EditorScreen(this,current()));}
    private void confirmDelete(){
        var palette=current();
        minecraft.setScreen(new ConfirmScreen(yes->{
            if(yes)try{
                PaletteShuffleClient.STORE.delete(palette.id());
                var active=PaletteShuffleClient.PLACEMENT.active();
                if(active!=null&&active.id().equals(palette.id()))PaletteShuffleClient.PLACEMENT.stop();
                selected=null;
            }catch(Exception e){error=e.getMessage();}
            minecraft.setScreen(this);
        },Component.literal("Delete "+palette.name()+"?"),Component.literal("This removes the saved palette. Your blocks are kept.")));
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float d){
        super.extractRenderState(g,mx,my,d);
        if(PaletteShuffleClient.STORE.all().isEmpty())g.centeredText(font,"Create your first palette to start building.",width/2,55,0xFFCCCCCC);
        g.centeredText(font,"Choose the hotbar slot to build with",width/2,height-92,0xFFCCCCCC);
    }
    @Override public void onClose(){minecraft.setScreen(parent);}
}
