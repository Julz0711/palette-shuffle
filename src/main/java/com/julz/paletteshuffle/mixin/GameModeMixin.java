package com.julz.paletteshuffle.mixin;

import com.julz.paletteshuffle.client.PaletteShuffleClient;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public abstract class GameModeMixin {
    // Gate before vanilla emits a placement packet, not inside the client prediction.
    @Inject(method="useItemOn",at=@At("HEAD"),cancellable=true)
    private void waitForSwap(LocalPlayer player, InteractionHand hand, BlockHitResult hit,
                             CallbackInfoReturnable<InteractionResult> cir){
        var c=PaletteShuffleClient.PLACEMENT;
        if(hand==InteractionHand.MAIN_HAND&&c.active()!=null&&player.getInventory().getSelectedSlot()==c.slot()&&!c.ready())
            cir.setReturnValue(InteractionResult.FAIL);
    }
}
