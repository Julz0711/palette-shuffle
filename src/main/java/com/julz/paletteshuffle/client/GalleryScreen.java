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
    public GalleryScreen(Screen parent){
        super("Palette Shuffle");this.parent=parent;
        var c=PaletteShuffleClient.PLACEMENT;
        var mc=net.minecraft.client.Minecraft.getInstance();
        if(c.active()!=null){selected=c.active().id();slot=c.slot();}
        else if(mc.player!=null)slot=mc.player.getInventory().getSelectedSlot();
    }
    void select(UUID id){selected=id;}
    @Override protected void init(){
        var palettes=PaletteShuffleClient.STORE.all();
        if(current()==null&&!palettes.isEmpty())selected=palettes.getFirst().id();
        if(PaletteShuffleClient.STORE.loadError()!=null)error=PaletteShuffleClient.STORE.loadError();
        int cols=Math.max(1,Math.min(3,(width-20)/108));
        int rows=Math.max(1,(height-144)/72);int perPage=cols*rows;
        page=Math.min(page,Math.max(0,(palettes.size()-1)/perPage));
        int left=(width-cols*108)/2;
        for(int i=page*perPage;i<Math.min(palettes.size(),(page+1)*perPage);i++){
            var p=palettes.get(i);int index=i-page*perPage;
            var card=new Button(left+index%cols*108,36+index/cols*72,104,70,Component.literal(p.name()),b->{selected=p.id();error="";rebuildWidgets();},s->s.get()){
                @Override public void extractContents(GuiGraphicsExtractor g,int mx,int my,float d){
                    var active=PaletteShuffleClient.PLACEMENT.active();
                    g.fill(getX(),getY(),getX()+104,getY()+70,0xEE282828);
                    g.outline(getX(),getY(),104,70,p.id().equals(selected)?0xFFB7DD82:0xFF888888);
                    int color=active!=null&&active.id().equals(p.id())?0xFFB7DD82:0xFFFFFFFF;
                    g.centeredText(font,font.plainSubstrByWidth(p.name(),96),getX()+52,getY()+4,color);
                    for(int j=0;j<9;j++){
                        int x=getX()+26+j%3*18,y=getY()+15+j/3*18;
                        g.fill(x,y,x+17,y+17,0xFF484848);
                        if(j<p.entries().size())g.item(Blocks.stack(p.entries().get(j).blockId()),x,y);
                    }
                }
            };addRenderableWidget(card);
        }
        int y=height-104;
        button(width/2-155,y,70,"New palette",()->minecraft.gui.setScreen(new EditorScreen(this,null)));
        button(width/2-81,y,42,"Edit",()->minecraft.gui.setScreen(new EditorScreen(this,current()))).active=current()!=null;
        button(width/2-35,y,68,"Duplicate",()->{try{var p=current().duplicate();PaletteShuffleClient.STORE.put(p);selected=p.id();rebuildWidgets();}catch(Exception e){error=e.getMessage();}}).active=current()!=null;
        button(width/2+37,y,54,"Delete",this::confirmDelete).active=current()!=null;
        button(width/2+95,y,26,"<",()->{page--;rebuildWidgets();}).active=page>0;
        button(width/2+125,y,26,">",()->{page++;rebuildWidgets();}).active=(page+1)*perPage<palettes.size();
        for(int i=0;i<9;i++){
            int target=i;
            slot(width/2-108+i*24,height-68,()->minecraft.player==null?net.minecraft.world.item.ItemStack.EMPTY:minecraft.player.getInventory().getItem(target),
                    "Hotbar slot "+(i+1),i==slot,""+(i+1),()->{slot=target;error="";rebuildWidgets();});
        }
        boolean on=PaletteShuffleClient.PLACEMENT.active()!=null;
        button(width/2-155,height-24,202,on?"Disable palette":"Enable palette",()->{
            var c=PaletteShuffleClient.PLACEMENT;
            if(c.active()!=null){c.stop();error="";}
            else if(current()!=null&&!c.start(current(),slot))error=c.status();
            else error="";
            rebuildWidgets();
        }).active=on||current()!=null&&minecraft.player!=null;
        button(width/2+51,height-24,104,"Back to game",this::onClose);
    }
    private Palette current(){return PaletteShuffleClient.STORE.all().stream().filter(p->p.id().equals(selected)).findFirst().orElse(null);}
    private void confirmDelete(){
        var palette=current();minecraft.gui.setScreen(new ConfirmScreen(yes->{
            if(yes)try{
                PaletteShuffleClient.STORE.delete(palette.id());
                var active=PaletteShuffleClient.PLACEMENT.active();if(active!=null&&active.id().equals(palette.id()))PaletteShuffleClient.PLACEMENT.stop();
                selected=null;
            }catch(Exception e){error=e.getMessage();}
            minecraft.gui.setScreen(this);
        },Component.literal("Delete "+palette.name()+"?"),Component.literal("This removes the saved palette. Your blocks are kept.")));
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float d){
        super.extractRenderState(g,mx,my,d);
        var c=PaletteShuffleClient.PLACEMENT;
        g.centeredText(font,font.plainSubstrByWidth(c.status(),width-20),width/2,24,c.active()!=null?0xFFB7DD82:0xFFCCCCCC);
        if(PaletteShuffleClient.STORE.all().isEmpty())g.centeredText(font,"Create a palette, then select its blocks.",width/2,60,0xFFCCCCCC);
        g.centeredText(font,"Your hotbar — palette slot "+(slot+1),width/2,height-80,0xFFFFFFFF);
        if(error.isEmpty()){
            String tip=minecraft.player!=null&&minecraft.player.getAbilities().instabuild?"Creative: blocks supplied automatically":"Survival: palette blocks must be in your inventory";
            g.centeredText(font,tip,width/2,height-37,0xFFCCCCCC);
        }
    }
    @Override public void onClose(){minecraft.gui.setScreen(parent);}
}
