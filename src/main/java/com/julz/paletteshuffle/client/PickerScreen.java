package com.julz.paletteshuffle.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.*;
import net.minecraft.network.chat.Component;
import java.util.*;

final class PickerScreen extends PaletteScreen {
    private final EditorScreen parent;
    private boolean inventoryOnly;
    private String query="";
    private int page;
    private List<Item> results=List.of();
    PickerScreen(EditorScreen parent){super("Choose a block");this.parent=parent;inventoryOnly=net.minecraft.client.Minecraft.getInstance().player!=null;}
    @Override protected void init(){
        button(width/2-104,32,100,inventoryOnly?"[Inventory]":"Inventory",()->{inventoryOnly=true;page=0;rebuildWidgets();});
        button(width/2+4,32,100,!inventoryOnly?"[All blocks]":"All blocks",()->{inventoryOnly=false;page=0;rebuildWidgets();});
        var search=addRenderableWidget(new EditBox(font,width/2-140,60,210,20,Component.literal("Search blocks")));
        search.setValue(query);search.setResponder(s->query=s);
        button(width/2+74,60,66,"Search",()->{page=0;rebuildWidgets();});
        List<Item> items=new ArrayList<>();
        for(var item:BuiltInRegistries.ITEM)if(item instanceof BlockItem
                && (!inventoryOnly || PlacementController.find(minecraft,Blocks.id(item))>=0)
                && (Blocks.id(item).contains(query.toLowerCase(Locale.ROOT)) || new ItemStack(item).getHoverName().getString().toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT))))items.add(item);
        items.sort(Comparator.comparing(i->new ItemStack(i).getHoverName().getString()));results=items;
        int cols=Math.max(1,Math.min(12,(width-28)/24));int rows=Math.max(1,(height-140)/24);int perPage=cols*rows;
        page=Math.min(page,Math.max(0,(items.size()-1)/perPage));
        for(int i=page*perPage;i<Math.min(items.size(),(page+1)*perPage);i++){
            var item=items.get(i);var stack=new ItemStack(item);int index=i-page*perPage;
            icon((width-cols*24)/2+index%cols*24,90+index/cols*24,stack,stack.getHoverName().getString()+" · "+Blocks.id(item),false,()->{parent.choose(Blocks.id(item));minecraft.setScreen(parent);});
        }
        button(width/2-104,height-24,40,"<",()->{page--;rebuildWidgets();}).active=page>0;
        button(width/2-60,height-24,120,"Back to palette",this::onClose);
        button(width/2+64,height-24,40,">",()->{page++;rebuildWidgets();}).active=(page+1)*perPage<items.size();
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float d){
        super.extractRenderState(g,mx,my,d);
        if(results.isEmpty())g.centeredText(font,"No blocks found. Try All blocks or another search.",width/2,100,0xFFCCCCCC);
    }
    @Override public void onClose(){minecraft.setScreen(parent);}
}
