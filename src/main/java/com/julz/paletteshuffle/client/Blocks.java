package com.julz.paletteshuffle.client;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.*;

public final class Blocks {
    private Blocks() {}
    public static Item item(String id) { return BuiltInRegistries.ITEM.getValue(Identifier.parse(id)); }
    public static ItemStack stack(String id) { return new ItemStack(item(id)); }
    public static String id(Item item) { return BuiltInRegistries.ITEM.getKey(item).toString(); }
    public static boolean valid(String id) { return item(id) instanceof BlockItem; }
}
