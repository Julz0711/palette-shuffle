package com.julz.paletteshuffle.mixin;

import com.julz.paletteshuffle.client.PaletteShuffleClient;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public abstract class BlockItemMixin {
    @Inject(method = "place", at = @At("RETURN"))
    private void afterPlacement(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (context.getLevel().isClientSide() && context.getPlayer() == Minecraft.getInstance().player
                && context.getHand() == InteractionHand.MAIN_HAND && cir.getReturnValue().consumesAction())
            PaletteShuffleClient.PLACEMENT.placed();
    }
}
