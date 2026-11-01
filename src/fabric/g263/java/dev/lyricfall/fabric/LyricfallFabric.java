package dev.lyricfall.fabric;

import dev.lyricfall.LyricfallMod;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.lyricfall.compat.CanvasImpl;
import dev.lyricfall.compat.OverlayText;
import dev.lyricfall.compat.WorldTextImpl;
import dev.lyricfall.hud.HudLayers;
import dev.lyricfall.hud.WorldLyricsRenderer;
import dev.lyricfall.input.Hotkeys;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

public final class LyricfallFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        FabricLoader loader = FabricLoader.getInstance();
        LyricfallMod.init(loader.getConfigDir(), loader.getGameDir());

        for (KeyMapping km : Hotkeys.mappings()) KeyMappingHelper.registerKeyMapping(km);

        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(LyricfallMod.MOD_ID, "lyrics"), (graphics, tick) -> {
            CanvasImpl canvas = new CanvasImpl(graphics);
            // with a shader pack the world lyrics are drawn here, after the shader, so they stay crisp
            if (WorldLyricsRenderer.overlayActive()) WorldLyricsRenderer.render(new PoseStack(), new OverlayText(canvas));
            HudLayers.render(canvas);
        });
        LevelRenderEvents.COLLECT_SUBMITS.register(ctx -> {
            if (!WorldLyricsRenderer.overlayActive()) WorldLyricsRenderer.render(ctx.poseStack(), new WorldTextImpl(ctx.submitNodeCollector()));
        });

        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            LyricfallMod.onClientStarted();
            // Iris toggles shaders with K, which was also our settings key: move ours to U once
            if (loader.isModLoaded("iris")) Hotkeys.moveOffIrisKey();
        });
        boolean lights = loader.isModLoaded("lambdynlights");
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            LyricfallMod.onClientTick();
            if (lights) LyricfallLights.tick();
        });
    }
}
