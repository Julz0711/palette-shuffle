package com.julz.paletteshuffle.client;

import com.julz.paletteshuffle.core.Palette;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import java.util.*;

final class EditorScreen extends PaletteScreen {
    private final GalleryScreen parent;
    private final UUID id;
    private final List<Palette.Entry> entries=new ArrayList<>();
    private final String originalName;
    private final List<Palette.Entry> originalEntries;
    private String name;
    private int selected;
    EditorScreen(GalleryScreen parent, Palette palette){
        super(palette==null?"New palette":"Edit palette");this.parent=parent;
        id=palette==null?UUID.randomUUID():palette.id();name=palette==null?"":palette.name();
        if(palette!=null)entries.addAll(palette.entries());
        originalName=name;originalEntries=List.copyOf(entries);
    }
    @Override protected void init(){
        var nameBox=addRenderableWidget(new EditBox(font,width/2-140,34,280,20,Component.literal("Palette name")));
        nameBox.setMaxLength(48);nameBox.setValue(name);nameBox.setResponder(s->name=s);
        for(int i=0;i<9;i++){
            int index=i; var stack=i<entries.size()?Blocks.stack(entries.get(i).blockId()):ItemStack.EMPTY;
            String tip=i<entries.size()?stack.getHoverName().getString():"Add block";
            icon(width/2-36+i%3*24,68+i/3*24,stack,tip,i==selected,()->{
                selected=index;
                if(index>=entries.size())minecraft.setScreen(new PickerScreen(this));else rebuildWidgets();
            });
        }
        int y=150;
        button(width/2-142,y,90,"Replace / add",()->minecraft.setScreen(new PickerScreen(this)));
        button(width/2-48,y,62,"Remove",()->{entries.remove(selected);selected=Math.max(0,selected-1);rebuildWidgets();}).active=selected<entries.size();
        button(width/2+18,y,28,"−",()->weight(-1)).active=selected<entries.size()&&entries.get(selected).weight()>1;
        button(width/2+50,y,28,"+",()->weight(1)).active=selected<entries.size()&&entries.get(selected).weight()<100;
        button(width/2+82,y,60,"Equal mix",()->{entries.replaceAll(e->new Palette.Entry(e.blockId(),1));rebuildWidgets();});
        button(width/2-104,height-24,100,"Save",()->{
            try{
                Palette p=new Palette(id,name,entries);PaletteShuffleClient.STORE.put(p);
                var active=PaletteShuffleClient.PLACEMENT.active();
                if(active!=null&&active.id().equals(id))PaletteShuffleClient.PLACEMENT.stop();
                minecraft.setScreen(parent);
            }catch(Exception e){error=e.getMessage();}
        });
        button(width/2+4,height-24,100,"Cancel",this::onClose);
    }
    void choose(String block){
        if(entries.stream().anyMatch(e->e.blockId().equals(block))){error="That block is already in this palette.";return;}
        var e=new Palette.Entry(block,selected<entries.size()?entries.get(selected).weight():1);
        if(selected<entries.size())entries.set(selected,e);else if(entries.size()<9){selected=entries.size();entries.add(e);}
        error="";
    }
    private void weight(int change){var e=entries.get(selected);entries.set(selected,new Palette.Entry(e.blockId(),e.weight()+change));rebuildWidgets();}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float d){
        super.extractRenderState(g,mx,my,d);
        if(selected<entries.size()){
            var e=entries.get(selected);int sum=entries.stream().mapToInt(Palette.Entry::weight).sum();
            g.centeredText(font,"Mix weight: "+e.weight()+" · "+Math.round(100.0*e.weight()/sum)+"% when all blocks are available",width/2,180,0xFFCCCCCC);
        }else g.centeredText(font,"Choose up to nine blocks. Equal chances by default.",width/2,180,0xFFCCCCCC);
    }
    @Override public void onClose(){
        if(name.equals(originalName)&&entries.equals(originalEntries)){minecraft.setScreen(parent);return;}
        minecraft.setScreen(new ConfirmScreen(yes->minecraft.setScreen(yes?parent:this),Component.literal("Discard changes?"),Component.literal("Your saved palette will remain unchanged.")));
    }
}
