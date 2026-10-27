package com.julz.paletteshuffle.config;
import com.julz.paletteshuffle.core.Palette;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class PaletteStoreTest {
    @TempDir Path dir;
    @Test void roundTripEditDelete() throws Exception {
        var file=dir.resolve("palettes.json");var store=new PaletteStore(file);
        var p=new Palette(UUID.randomUUID(),"Stone",List.of(new Palette.Entry("minecraft:stone",5)));
        store.put(p);store=new PaletteStore(file);store.load();assertEquals(List.of(p),store.all());
        var edited=new Palette(p.id(),"New name",p.entries());store.put(edited);
        store.load();assertEquals(List.of(edited),store.all());store.delete(p.id());store.load();assertTrue(store.all().isEmpty());
    }
    @Test void corruptDataCannotBeOverwritten() throws Exception {
        var file=dir.resolve("palettes.json");Files.writeString(file,"{ broken");
        var store=new PaletteStore(file);store.load();assertNotNull(store.loadError());
        assertThrows(java.io.IOException.class,()->store.put(new Palette(UUID.randomUUID(),"Test",List.of(new Palette.Entry("minecraft:stone",1)))));
        assertEquals("{ broken",Files.readString(file));
    }
    @Test void futureFileVersionIsPreserved() throws Exception {
        var file=dir.resolve("palettes.json");var data="{\"version\":2,\"palettes\":[]}";Files.writeString(file,data);
        var store=new PaletteStore(file);store.load();assertNotNull(store.loadError());
        assertThrows(java.io.IOException.class,()->store.delete(UUID.randomUUID()));assertEquals(data,Files.readString(file));
    }
}
