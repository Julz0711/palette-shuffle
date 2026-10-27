package com.julz.paletteshuffle.config;

import com.google.gson.*;
import com.julz.paletteshuffle.core.Palette;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/** Atomic saves; a damaged file is retained and never silently replaced. */
public final class PaletteStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path path;
    private List<Palette> palettes = new ArrayList<>();
    private String loadError;
    public PaletteStore(Path path) { this.path = path; }
    public List<Palette> all() { return List.copyOf(palettes); }
    public String loadError() { return loadError; }
    public void load() {
        if (!Files.exists(path)) return;
        try {
            JsonObject root = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
            if (root.get("version").getAsInt() != 1) throw new IllegalArgumentException("Unsupported palette file version");
            List<Palette> loaded = new ArrayList<>();
            for (var node : root.getAsJsonArray("palettes")) {
                var o = node.getAsJsonObject();
                List<Palette.Entry> entries = new ArrayList<>();
                for (var e : o.getAsJsonArray("entries")) {
                    var item = e.getAsJsonObject();
                    entries.add(new Palette.Entry(item.get("blockId").getAsString(), item.get("weight").getAsInt()));
                }
                var p = new Palette(UUID.fromString(o.get("id").getAsString()), o.get("name").getAsString(), entries);
                if (loaded.stream().anyMatch(x -> x.id().equals(p.id()))) throw new IllegalArgumentException("Duplicate palette ID");
                loaded.add(p);
            }
            palettes = loaded;
            loadError = null;
        } catch (IOException | RuntimeException e) {
            loadError = "Cannot read palettes. Original file preserved: " + path.getFileName();
        }
    }
    public void put(Palette palette) throws IOException {
        var next = new ArrayList<>(palettes);
        next.removeIf(p -> p.id().equals(palette.id())); next.add(palette);
        save(next);
    }
    public void delete(UUID id) throws IOException {
        var next = new ArrayList<>(palettes); next.removeIf(p -> p.id().equals(id)); save(next);
    }
    private void save(List<Palette> next) throws IOException {
        if (loadError != null) throw new IOException(loadError);
        Files.createDirectories(path.getParent());
        var root = new JsonObject(); root.addProperty("version", 1); root.add("palettes", GSON.toJsonTree(next));
        Path temp = Files.createTempFile(path.getParent(), "palette-shuffle-", ".tmp");
        try {
            Files.writeString(temp, GSON.toJson(root));
            try { Files.move(temp, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException e) { Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING); }
            palettes = next;
        } finally { Files.deleteIfExists(temp); }
    }
}
