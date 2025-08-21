package com.riburitu.regionvisualizer.client;

import com.riburitu.regionvisualizer.client.RegionRenderer;
import com.riburitu.regionvisualizer.client.sound.MusicConfigScreen;
import com.riburitu.regionvisualizer.client.sound.MusicManager;

import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.SoundOptionsScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

@OnlyIn(Dist.CLIENT)
public class ClientEventHandler {
    
    public static void initializeClient() {
        // Registrar eventos del cliente
        MinecraftForge.EVENT_BUS.register(new ClientEventHandler());
        MinecraftForge.EVENT_BUS.addListener(RegionRenderer::onRenderWorld);
        
        // Inicializar MusicManager
        MusicManager.initialize();
    }

    @SubscribeEvent
    public void onScreenInit(ScreenEvent.Init event) {
        if (event.getScreen() instanceof SoundOptionsScreen screen) {
            int centerX = screen.width / 2;
            int centerY = screen.height / 2;

            Button configButton = Button.builder(
                Component.literal("Configuración de RegionVisualizer"),
                b -> screen.getMinecraft().setScreen(new MusicConfigScreen(screen))
            ).bounds(
                centerX - 100,
                centerY + 80, // Posicionado debajo de los controles de sonido
                200,
                20
            ).build();

            event.addListener(configButton);
        }
    }

    @SubscribeEvent
    public void onScreenOpen(ScreenEvent.Opening event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.isSingleplayer() && event.getScreen() instanceof PauseScreen && mc.player != null) {
            System.out.println("[RegionVisualizer] 🔍 Menú de pausa abierto en singleplayer, pausando música");
            MusicManager.pauseMusic(true);
        }
    }

    @SubscribeEvent
    public void onScreenClose(ScreenEvent.Closing event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.isSingleplayer() && event.getScreen() instanceof PauseScreen && mc.player != null) {
            System.out.println("[RegionVisualizer] 🔍 Cerrando menú de pausa en singleplayer, reanudando música");
            MusicManager.resumeMusic((LocalPlayer) mc.player);
        }
    }

    @SubscribeEvent
    public void onLevelUnload(LevelEvent.Unload event) {
        // Detectar salida del mundo en singleplayer
        if (Minecraft.getInstance().isSingleplayer()) {
            System.out.println("[RegionVisualizer] 🔍 Mundo descargado en singleplayer, deteniendo música");
            MusicManager.stop(true); // Usar fade-out
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
        // Limpiar recursos al desconectarse (singleplayer o multiplayer)
        MusicManager.onPlayerLoggedOut();
    }
    @SubscribeEvent
    public void onScreenOpen(ScreenEvent event) {
        if (event.getScreen() instanceof TitleScreen) {
            // Jugador llegó al menú principal
            MusicManager.onMainMenuOpened();
        }
    }
}