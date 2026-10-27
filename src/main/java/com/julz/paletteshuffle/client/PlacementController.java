package com.julz.paletteshuffle.client;

import com.julz.paletteshuffle.core.Palette;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import java.util.Random;

/** All survival changes use normal container inputs. Activation belongs to one player session. */
public final class PlacementController {
    private Palette active;
    private int slot;
    private boolean needsBlock;
    private int delay;
    private String expected;
    private LocalPlayer owner;
    private ItemStack original=ItemStack.EMPTY;
    private boolean creativeSession;
    private String status="Off — normal block placement";
    private final Random random=new Random();
    public Palette active(){return active;}
    public int slot(){return slot;}
    public String status(){return status;}
    public boolean start(Palette palette,int target){
        if(target<0||target>8)throw new IllegalArgumentException("Hotbar slot must be 0–8");
        var mc=Minecraft.getInstance();
        if(mc.player==null||mc.gameMode==null||mc.player.isSpectator())return fail("Join a world in survival or creative first.");
        if(mc.player.containerMenu!=mc.player.inventoryMenu||!mc.player.inventoryMenu.getCarried().isEmpty())return fail("Close your inventory and put down the carried item first.");
        if(palette.entries().stream().noneMatch(e->available(mc,e.blockId())))return fail("No palette blocks in your inventory. Add supplies first.");
        if(active!=null)stop();
        active=palette;slot=target;owner=mc.player;creativeSession=owner.getAbilities().instabuild;
        original=owner.getInventory().getItem(slot).copy();needsBlock=true;delay=0;expected=null;
        owner.getInventory().setSelectedSlot(slot);
        prepare(mc);
        status="On — "+palette.name()+" · slot "+(slot+1);message(status);
        return true;
    }
    private boolean fail(String text){status=text;message(text);return false;}
    /** Explicit disable: release placement immediately, then restore the previous slot when safe. */
    public void stop(){
        var mc=Minecraft.getInstance();
        boolean restore=active!=null&&owner==mc.player&&mc.player!=null&&mc.gameMode!=null
                &&mc.player.containerMenu==mc.player.inventoryMenu&&mc.player.inventoryMenu.getCarried().isEmpty();
        active=null;needsBlock=false;delay=0;
        if(restore && expected!=null && Blocks.id(mc.player.getInventory().getItem(slot).getItem()).equals(expected)){
            if(creativeSession&&mc.player.getAbilities().instabuild){
                var stack=original.copy();mc.player.getInventory().setItem(slot,stack);
                mc.gameMode.handleCreativeModeItemAdd(stack,36+slot);
            }else if(!creativeSession&&!original.isEmpty()){
                for(int i=0;i<36;i++)if(ItemStack.isSameItemSameComponents(mc.player.getInventory().getItem(i),original)){
                    if(i!=slot)swap(mc,i);break;
                }
            }
        }
        reset();message(status);
    }
    /** Disconnect/join must never restore items into a different player or world. */
    public void reset(){active=null;expected=null;needsBlock=false;delay=0;owner=null;original=ItemStack.EMPTY;status="Off — normal block placement";}
    public void placed(){
        var mc=Minecraft.getInstance();
        if(active!=null&&owner==mc.player&&mc.player.getInventory().getSelectedSlot()==slot){needsBlock=true;delay=1;}
    }
    public boolean ready(){return active==null||(!needsBlock&&delay==0);}
    public void tick(Minecraft mc){
        if(mc.player==null||mc.level==null||active!=null&&owner!=mc.player){reset();return;}
        if(active==null)return;
        if(!mc.player.isAlive()||mc.player.isSpectator()||mc.player.getAbilities().instabuild!=creativeSession){reset();return;}
        if(mc.gui.screen()!=null||mc.isPaused()||mc.gameMode==null||mc.player.containerMenu!=mc.player.inventoryMenu||!mc.player.inventoryMenu.getCarried().isEmpty())return;
        if(delay>0){delay--;return;}
        if(mc.player.getInventory().getSelectedSlot()!=slot)return;
        if(needsBlock){prepare(mc);return;}
        var held=mc.player.getInventory().getItem(slot);
        if(expected!=null&&!Blocks.id(held.getItem()).equals(expected)){
            // Refilling an exhausted stack should not silently disable the palette.
            if(held.isEmpty()){needsBlock=true;prepare(mc);}
            else{reset();status="Off — working slot was changed";message(status);}
        }
    }
    private void prepare(Minecraft mc){
        var next=active.pick(id->available(mc,id),random);
        if(next.isEmpty()){reset();status="Off — out of palette blocks. Add supplies, then enable again.";message(status);return;}
        String id=next.get().blockId();
        if(creativeSession){
            ItemStack stack=Blocks.stack(id);stack.setCount(stack.getMaxStackSize());
            mc.player.getInventory().setItem(slot,stack);mc.gameMode.handleCreativeModeItemAdd(stack,36+slot);
        }else{
            int source=find(mc,id);if(source<0)return;
            if(source!=slot)swap(mc,source);
        }
        expected=id;needsBlock=false;delay=1;
    }
    private void swap(Minecraft mc,int source){
        mc.gameMode.handleContainerInput(mc.player.inventoryMenu.containerId,source<9?36+source:source,slot,ContainerInput.SWAP,mc.player);
    }
    public static boolean available(Minecraft mc,String id){return Blocks.valid(id)&&mc.player!=null&&(mc.player.getAbilities().instabuild||find(mc,id)>=0);}
    public static int find(Minecraft mc,String id){
        if(mc.player==null)return -1;
        // Prefer the working/selected stack, avoiding needless swaps between identical stacks.
        int selected=mc.player.getInventory().getSelectedSlot();
        var held=mc.player.getInventory().getItem(selected);
        if(!held.isEmpty()&&Blocks.id(held.getItem()).equals(id))return selected;
        for(int i=0;i<36;i++){var stack=mc.player.getInventory().getItem(i);if(!stack.isEmpty()&&Blocks.id(stack.getItem()).equals(id))return i;}
        return -1;
    }
    private void message(String text){var p=Minecraft.getInstance().player;if(p!=null)p.sendOverlayMessage(Component.literal(text));}
}
