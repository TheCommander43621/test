package com.github.abigailfails.helpfulhitboxes;

import com.google.gson.*;
import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagKey;
import net.minecraft.util.profiling.ProfilerFiller;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.github.abigailfails.helpfulhitboxes.HelpfulHitboxes.COMPATIBLE_BLOCKS;

public class ModConfig {
    public static final String CONFIG_FILE = getConfigDirectory().toString() + "/helpfulhitboxes.json";

    /**
     * This is the example method for demonstrating <code>@ExpectPlatform</code> but i'm keeping it because it's actually useful!
     */
    @ExpectPlatform
    public static Path getConfigDirectory() {
        // Just throw an error, the content should get replaced at runtime.
        throw new AssertionError();
    }

    /**
     * Replaces placeholder tag IDs with their elements in the compatible block list. Invoked on world start.
     * */
    public static void fillAllTagsInCompatibleBlocks() {
        fillTagsInBlockList(HelpfulHitboxes.COMPATIBLE_BLOCKS.ungroupedBlocks());
        HelpfulHitboxes.COMPATIBLE_BLOCKS.blockGroups().forEach(ModConfig::fillTagsInBlockList);
    }

    private static void fillTagsInBlockList(HashSet<String> blockList) {
        List<String> toAdd = new ArrayList<>();
        List<String> toRemove = new ArrayList<>();
        for (String string : blockList) {
            if (addTagElementsOrCache(string, toAdd))
                toRemove.add(string);
        }
        toRemove.forEach(blockList::remove);
        blockList.addAll(toAdd);
    }

    /**
     * Reads a tag ID and adds all its elements to a specified {@code HashSet}. If the tag is empty, it adds the tag
     * ID to the set instead.
     *
     * @return If true, elements from the tag were added to the set. If false, the cache placeholder was added instead
     * */
    private static boolean addTagElementsOrCache(String tag, Collection<String> toAdd) {
        HashSet<String> tagElements = new HashSet<>();
        Registry.BLOCK.getTagOrEmpty(TagKey.create(Registry.BLOCK_REGISTRY, new ResourceLocation(tag.substring(1)))).forEach(h -> {
            tagElements.add(h.value().getDescriptionId());
        });
        if (!tagElements.isEmpty()) {
            toAdd.addAll(tagElements);
            return true;
        } else toAdd.add(tag);
        return false;
    }

    public static JsonArray readConfig(ResourceManager resourceManager, ProfilerFiller profilerFiller) {
        JsonArray compatibleBlocks;
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try {
            if (!new File(CONFIG_FILE).exists()) {
                JsonArray compatibleBlocksJson = defaultCompatibleBlocksJson();
                compatibleBlocks = compatibleBlocksJson;
                FileWriter writer = new FileWriter(CONFIG_FILE);
                JsonObject config = new JsonObject();
                config.add("compatible_blocks", compatibleBlocksJson);
                gson.toJson(config, writer);
                writer.close();
            } else {
                Reader reader = Files.newBufferedReader(Paths.get(CONFIG_FILE));
                compatibleBlocks = gson.fromJson(reader, JsonObject.class).getAsJsonArray("compatible_blocks");
                reader.close();
            }
        } catch (IOException | JsonParseException e) {
            Logger.getGlobal().log(Level.SEVERE, "Cannot access HelpfulHitboxes config file or it is invalid, reverting to default value...");
            return defaultCompatibleBlocksJson();
        }
        return compatibleBlocks;
    }

    public static void applyConfig(JsonArray json, ResourceManager resourceManager, ProfilerFiller profilerFiller) {
        HashSet<String> ungrouped = new HashSet<>();
        HashSet<HashSet<String>> blockGroups = new HashSet<>();
        readBlocklistFromJson(json, ungrouped, blockGroups);
        HelpfulHitboxes.COMPATIBLE_BLOCKS = new HelpfulHitboxes.CompatibleBlockList(ungrouped, blockGroups);
    }

    private static JsonArray defaultCompatibleBlocksJson() {
        JsonArray compatibleBlocks = new JsonArray();
        compatibleBlocks.add("minecraft:chain");
        compatibleBlocks.add("minecraft:end_rod");

        JsonArray fences = new JsonArray();
        fences.add("#minecraft:fences");
        JsonObject fencesObject = new JsonObject();
        fencesObject.add("group", fences);
        compatibleBlocks.add(fencesObject);

        JsonArray panes = new JsonArray();
        panes.add("#forge:glass_panes");
        panes.add("#c:glass_panes");
        panes.add("minecraft:iron_bars");
        JsonObject panesObject = new JsonObject();
        panesObject.add("group", panes);
        compatibleBlocks.add(panesObject);

        return compatibleBlocks;
    }

    private static void readBlocklistFromJson(JsonArray array, HashSet<String> ungrouped, HashSet<HashSet<String>> blockGroups) {
        for (JsonElement element : array) {
            if (element.isJsonObject()) {
                HashSet<String> blockGroup = new HashSet<>();
                readBlocklistFromJson(element.getAsJsonObject().getAsJsonArray("group"), blockGroup, blockGroups);
                blockGroups.add(blockGroup);
            } else {
                String string = element.getAsString();
                if (string.charAt(0) == '#')
                    addTagElementsOrCache(string, ungrouped);
                else ungrouped.add(Registry.BLOCK.get(new ResourceLocation(string)).getDescriptionId());
            }
        }
    }
}