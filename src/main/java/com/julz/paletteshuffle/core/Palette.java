package com.julz.paletteshuffle.core;

import java.util.*;
import java.util.function.Predicate;
import java.util.random.RandomGenerator;

/** Independent of Minecraft; saved identifiers remain portable across worlds. */
public record Palette(UUID id, String name, List<Entry> entries) {
    public static final int MAX_BLOCKS = 9;
    public record Entry(String blockId, int weight) {
        public Entry {
            if (blockId == null || !blockId.matches("[a-z0-9_.-]+:[a-z0-9_/.-]+"))
                throw new IllegalArgumentException("Invalid block identifier");
            if (weight < 1 || weight > 100) throw new IllegalArgumentException("Weight must be 1–100");
        }
    }
    public Palette {
        Objects.requireNonNull(id);
        name = Objects.requireNonNull(name).strip();
        if (name.isEmpty() || name.length() > 48) throw new IllegalArgumentException("Name must be 1–48 characters");
        entries = List.copyOf(entries);
        if (entries.isEmpty() || entries.size() > MAX_BLOCKS)
            throw new IllegalArgumentException("Choose 1–9 blocks");
        if (entries.stream().map(Entry::blockId).distinct().count() != entries.size())
            throw new IllegalArgumentException("Each block can appear only once");
    }
    public Palette duplicate() {
        return new Palette(UUID.randomUUID(), name.substring(0, Math.min(41, name.length())) + " (copy)", entries);
    }
    public Optional<Entry> pick(Predicate<String> available, RandomGenerator random) {
        var candidates = entries.stream().filter(e -> available.test(e.blockId())).toList();
        int total = candidates.stream().mapToInt(Entry::weight).sum();
        if (total == 0) return Optional.empty();
        int roll = random.nextInt(total);
        for (var entry : candidates) {
            roll -= entry.weight();
            if (roll < 0) return Optional.of(entry);
        }
        throw new AssertionError("Unreachable weighted selection");
    }
}
