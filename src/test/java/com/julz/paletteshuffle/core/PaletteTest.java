package com.julz.paletteshuffle.core;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PaletteTest {
    private Palette palette(Palette.Entry... entries) { return new Palette(UUID.randomUUID(),"Stone mix",List.of(entries)); }
    @Test void validatesNineUniqueBlocksAndNames() {
        assertThrows(IllegalArgumentException.class,()->palette());
        assertThrows(IllegalArgumentException.class,()->new Palette(UUID.randomUUID(),"  ",List.of(new Palette.Entry("minecraft:stone",1))));
        assertThrows(IllegalArgumentException.class,()->palette(new Palette.Entry("minecraft:stone",1),new Palette.Entry("minecraft:stone",2)));
        assertThrows(IllegalArgumentException.class,()->new Palette.Entry("bad id",1));
        assertThrows(IllegalArgumentException.class,()->new Palette.Entry("minecraft:stone",0));
        var nine=new ArrayList<Palette.Entry>();for(int i=0;i<9;i++)nine.add(new Palette.Entry("test:block_"+i,1));
        assertEquals(9,new Palette(UUID.randomUUID(),"Nine",nine).entries().size());
        nine.add(new Palette.Entry("test:tenth",1));
        assertThrows(IllegalArgumentException.class,()->new Palette(UUID.randomUUID(),"Ten",nine));
    }
    @Test void weightedBoundariesAndMissingBlocks() {
        var p=palette(new Palette.Entry("minecraft:stone",3),new Palette.Entry("minecraft:dirt",1));
        for(int i=0;i<4;i++){
            final int roll=i;
            var rng=new Random(){@Override public int nextInt(int bound){assertEquals(4,bound);return roll;}};
            assertEquals(i<3?"minecraft:stone":"minecraft:dirt",p.pick(id->true,rng).orElseThrow().blockId());
        }
        assertEquals("minecraft:dirt",p.pick(id->id.endsWith(":dirt"),new Random(1)).orElseThrow().blockId());
        assertTrue(p.pick(id->false,new Random()).isEmpty());
    }
    @Test void copyHasIndependentIdentityAndImmutableEntries(){
        var entries=new ArrayList<>(List.of(new Palette.Entry("minecraft:stone",1)));
        var p=new Palette(UUID.randomUUID(),"Original",entries);entries.clear();
        assertEquals(1,p.entries().size());assertNotEquals(p.id(),p.duplicate().id());
        assertThrows(UnsupportedOperationException.class,()->p.entries().clear());
    }
}
