package com.julz.paletteshuffle.client;

import com.julz.paletteshuffle.core.Palette;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import java.util.List;
import java.util.UUID;

public class PaletteClientTest implements FabricClientGameTest {
    private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    private static void click(ClientGameTestContext context,String label){
        context.runOnClient(mc->{
            var button=mc.gui.screen().children().stream().filter(w->w instanceof Button b&&b.getMessage().getString().equals(label)).map(w->(Button)w).findFirst().orElseThrow();
            check(button.active,"Button disabled: "+label);button.onPress(new KeyEvent(InputConstants.KEY_RETURN,0,0));
        });
    }
    @Override public void runTest(ClientGameTestContext context){
        var palette=new Palette(UUID.randomUUID(),"Stone test",List.of(new Palette.Entry("minecraft:stone",1)));
        context.runOnClient(mc->PaletteShuffleClient.STORE.put(palette));
        try(var world=context.worldBuilder().adjustSettings(s->s.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE)).create()){
            world.getConnection().waitForChunksRender();
            world.getServer().runCommand("tp @p 0 100 0");
            world.getServer().runCommand("fill -4 98 -4 4 98 4 minecraft:stone");
            world.getServer().runCommand("item replace entity @p hotbar.0 with minecraft:diamond_pickaxe");
            context.waitFor(mc->mc.player.getInventory().getItem(0).is(Items.DIAMOND_PICKAXE));
            context.runOnClient(mc->mc.player.getInventory().setSelectedSlot(0));
            context.getInput().resizeWindow(1080,648);
            context.runOnClient(mc->{mc.options.guiScale().set(3);mc.resizeGui();});
            context.getInput().holdKeyFor(InputConstants.KEY_B,1);
            context.waitForScreen(GalleryScreen.class);context.waitTicks(3);
            context.takeScreenshot("gallery-off-hotbar");
            click(context,"Edit");context.waitTicks(3);context.takeScreenshot("editor-visible-slots");
            click(context,"Cancel");context.waitForScreen(GalleryScreen.class);
            click(context,"Enable palette");
            context.runOnClient(mc->{check(PaletteShuffleClient.PLACEMENT.active()!=null,"Enable should activate immediately");check(mc.player.getMainHandItem().is(Items.STONE),"Enable should prepare a block immediately");});
            context.waitTicks(3);context.takeScreenshot("gallery-on-hotbar");
            click(context,"Back to game");context.waitFor(mc->PaletteShuffleClient.PLACEMENT.ready());
            // A real gameMode placement must reach the server and reroll via the mixin.
            BlockPos support=new BlockPos(1,98,0);
            context.runOnClient(mc->{
                var result=mc.gameMode.useItemOn(mc.player,InteractionHand.MAIN_HAND,new BlockHitResult(new Vec3(1.5,99,0.5),Direction.UP,support,false));
                check(result.consumesAction(),"Creative placement failed");
                check(!PaletteShuffleClient.PLACEMENT.ready(),"Successful placement did not queue the next block");
            });
            context.waitFor(mc->mc.level.getBlockState(support.above()).is(net.minecraft.world.level.block.Blocks.STONE));
            world.getConnection().waitForServerboundPackets();
            world.getServer().runOnServer(server->check(world.getConnection().getServerLevel().getBlockState(support.above()).is(net.minecraft.world.level.block.Blocks.STONE),"Server never received creative block"));
            context.waitFor(mc->PaletteShuffleClient.PLACEMENT.ready());
            context.getInput().holdKeyFor(InputConstants.KEY_B,1);context.waitForScreen(GalleryScreen.class);
            click(context,"Disable palette");
            context.runOnClient(mc->{check(PaletteShuffleClient.PLACEMENT.active()==null&&PaletteShuffleClient.PLACEMENT.ready(),"Disable left placement gated");check(mc.player.getMainHandItem().is(Items.DIAMOND_PICKAXE),"Creative disable did not restore previous item");});
            context.waitTicks(3);context.takeScreenshot("gallery-disabled-restored");
            click(context,"Back to game");
            world.getServer().runCommand("gamemode survival @p");world.getServer().runCommand("clear @p");
            context.waitFor(mc->!mc.player.getAbilities().instabuild&&mc.player.getInventory().isEmpty());
            context.setScreen(()->new GalleryScreen(null));click(context,"Enable palette");
            context.runOnClient(mc->{check(PaletteShuffleClient.PLACEMENT.active()==null,"Empty survival inventory activated");check(PaletteShuffleClient.PLACEMENT.status().contains("No palette blocks"),"Missing supply explanation absent");});
            context.waitTicks(3);context.takeScreenshot("survival-no-supplies");
            context.setScreen(()->null);
            world.getServer().runCommand("item replace entity @p hotbar.0 with minecraft:diamond_pickaxe");
            world.getServer().runCommand("item replace entity @p inventory.0 with minecraft:stone 2");
            context.waitFor(mc->mc.player.getInventory().getItem(9).is(Items.STONE));
            context.setScreen(()->new GalleryScreen(null));click(context,"Enable palette");click(context,"Back to game");
            context.waitFor(mc->PaletteShuffleClient.PLACEMENT.ready()&&mc.player.getMainHandItem().is(Items.STONE));
            BlockPos second=new BlockPos(2,98,0);
            context.runOnClient(mc->check(mc.gameMode.useItemOn(mc.player,InteractionHand.MAIN_HAND,new BlockHitResult(new Vec3(2.5,99,0.5),Direction.UP,second,false)).consumesAction(),"Survival placement failed"));
            world.getConnection().waitForServerboundPackets();world.getConnection().waitForClientboundPackets();
            context.waitFor(mc->PaletteShuffleClient.PLACEMENT.ready());
            context.runOnClient(mc->check(mc.player.getMainHandItem().getCount()==1,"Survival placement did not consume exactly one block"));
            context.setScreen(()->new GalleryScreen(null));click(context,"Disable palette");
            context.runOnClient(mc->{check(PaletteShuffleClient.PLACEMENT.active()==null,"Survival disable failed");check(mc.player.getMainHandItem().is(Items.DIAMOND_PICKAXE),"Survival previous item not restored");check(PlacementController.find(mc,"minecraft:stone")>=0,"Survival supplies lost on disable");});
            context.setScreen(()->null);
        }
        context.runOnClient(mc->check(PaletteShuffleClient.PLACEMENT.active()==null,"Activation leaked across disconnect"));
    }
}
