package com.github.abigailfails.helpfulhitboxes.fabric;

import com.github.abigailfails.helpfulhitboxes.HelpfulHitboxes;
import com.github.abigailfails.helpfulhitboxes.ModConfig;
import com.github.abigailfails.helpfulhitboxes.ModOptions;
import com.google.gson.JsonArray;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleResourceReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class HelpfulHitboxesFabric implements ModInitializer {
    public static ResourceLocation FABRIC_ID = new ResourceLocation("helpfulhitboxes", "config_reload");
    @Override
    public void onInitialize() {
        HelpfulHitboxes.init();
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new SimpleResourceReloadListener<JsonArray>() {
            @Override
            public ResourceLocation getFabricId() {
                return FABRIC_ID;
            }

            @Override
            public CompletableFuture<JsonArray> load(ResourceManager manager, ProfilerFiller profiler, Executor executor) {
                return CompletableFuture.supplyAsync(() -> ModConfig.readConfig(manager, profiler), executor);
            }

            @Override
            public CompletableFuture<Void> apply(JsonArray data, ResourceManager manager, ProfilerFiller profiler, Executor executor) {
                return CompletableFuture.runAsync(() -> ModConfig.applyConfig(data, manager, profiler), executor);
            }
        });
        KeyBindingHelper.registerKeyBinding(ModOptions.DISABLE_BEHAVIOUR);
    }
}
