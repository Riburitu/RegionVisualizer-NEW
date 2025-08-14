package com.riburitu.regionvisualizer;

import com.riburitu.regionvisualizer.registry.ModItems;
import com.riburitu.regionvisualizer.client.RegionRenderer;
import com.riburitu.regionvisualizer.client.sound.MusicConfigScreen;
import com.riburitu.regionvisualizer.client.sound.MusicManager;
import com.riburitu.regionvisualizer.commands.RegionCommands;
import com.riburitu.regionvisualizer.commands.PlayMusicCommand;
import com.riburitu.regionvisualizer.item.RegionSelectorItem;
import com.riburitu.regionvisualizer.network.NetworkHandler;
import com.riburitu.regionvisualizer.util.Region;
import com.riburitu.regionvisualizer.util.RegionManager;

import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.SoundOptionsScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Objects;

@Mod(RegionVisualizer.MODID)
public class RegionVisualizer {
    public static final String MODID = "regionvisualizer";
    
    public static RegionVisualizer INSTANCE;
    private static final RegionManager regionManager = new RegionManager();
    private final RegionCommands regionCommands = new RegionCommands(regionManager);
    private final Map<ServerPlayer, String> lastRegion = new HashMap<>();
    
    public RegionVisualizer(FMLJavaModLoadingContext context) {
        INSTANCE = this;
        IEventBus modEventBus = context.getModEventBus();

        ModItems.ITEMS.register(modEventBus);
        NetworkHandler.register();

        modEventBus.addListener(ModItems::registerCreativeTab);
        modEventBus.addListener(this::onClientSetup);

        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(RegionSelectorItem.class);
    }

    public RegionManager getRegionManager() {
        return regionManager;
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        regionCommands.register(event.getDispatcher());
        PlayMusicCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        regionManager.loadRegions(event.getServer().overworld());
        System.out.println("[RegionVisualizer] 📂 Regiones cargadas al iniciar el servidor: " + regionManager.getRegions().size());
        for (Region region : regionManager.getRegions()) {
            System.out.println("[RegionVisualizer] 📍 Región cargada: " + region.getName() + ", música: " + region.getMusicFile() + ", loopEnabled: " + region.isLoopEnabled() + ", fadeEnabled: " + region.isFadeEnabled());
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        if (player.tickCount % 20 != 0) return; // Check every 20 ticks
        ServerLevel level = player.serverLevel();
        String currentRegion = getCurrentRegion(level, player.blockPosition());
        String last = lastRegion.getOrDefault(player, null);
        if (!Objects.equals(currentRegion, last)) {
            if (currentRegion != null) {
                Optional<Region> regionOpt = regionManager.getRegionByName(currentRegion);
                regionOpt.ifPresent(region -> {
                    String musicCommand = "MUSIC:" + region.getMusicFile() + ":" + region.isLoopEnabled() + ":" + region.isFadeEnabled();
                    NetworkHandler.sendPlayMusic(player, musicCommand);
                    player.sendSystemMessage(Component.literal("[RegionVisualizer] " + player.getName().getString() + " ha entrado en " + currentRegion).withStyle(ChatFormatting.YELLOW));
                    System.out.println("[RegionVisualizer] " + player.getName().getString() + " ha entrado en " + currentRegion);
                    System.out.println("[RegionVisualizer] 📦 Enviando comando de música: " + musicCommand + " para " + player.getName().getString());
                    System.out.println("[RegionVisualizer] 📍 Región: " + region.getName() + ", música: " + region.getMusicFile() + ", loopEnabled: " + region.isLoopEnabled() + ", fadeEnabled: " + region.isFadeEnabled());
                });
            } else if (last != null) {
                Optional<Region> lastRegionOpt = regionManager.getRegionByName(last);
                String musicCommand = lastRegionOpt.map(region -> "STOP:" + region.isFadeEnabled()).orElse("STOP:false");
                NetworkHandler.sendPlayMusic(player, musicCommand);
                player.sendSystemMessage(Component.literal("[RegionVisualizer] " + player.getName().getString() + " se alejó de la región.").withStyle(ChatFormatting.YELLOW));
                System.out.println("[RegionVisualizer] " + player.getName().getString() + " se alejó de la región.");
                lastRegion.remove(player);
            }
            lastRegion.put(player, currentRegion);
        }
    }

    public static String getCurrentRegion(Level level, BlockPos pos) {
        Optional<Region> regionOpt = regionManager.getRegionContaining(pos);
        return regionOpt.map(Region::getName).orElse(null);
    }
    
    private void onClientSetup(final FMLClientSetupEvent event) {
        MinecraftForge.EVENT_BUS.addListener(RegionRenderer::onRenderWorld);
        
        event.enqueueWork(() -> {
            try {
                MusicManager.initialize();
                System.out.println("[RegionVisualizer] Cliente configurado - Renderer y Música inicializados");
            } catch (Exception e) {
                System.err.println("[RegionVisualizer] Error inicializando música en cliente: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
    
    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            try {
                NetworkHandler.sendPlayMusic(player, "INIT");
                System.out.println("[RegionVisualizer] 🎵 Sistema de música inicializado para: " + player.getName().getString());
            } catch (Exception e) {
                System.err.println("[RegionVisualizer] Error inicializando música para " + player.getName().getString() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
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

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public void onScreenOpen(ScreenEvent.Opening event) {
        // Detectar apertura del menú de pausa en singleplayer
        if (Minecraft.getInstance().isSingleplayer() && event.getScreen() instanceof PauseScreen) {
            System.out.println("[RegionVisualizer] 🔍 Menú de pausa abierto en singleplayer, pausando música");
            MusicManager.pauseMusic(true); // Usar fade-out
        }
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public void onScreenClose(ScreenEvent.Closing event) {
        // Detectar cierre del menú de pausa en singleplayer
        if (Minecraft.getInstance().isSingleplayer() && event.getScreen() instanceof PauseScreen) {
            System.out.println("[RegionVisualizer] 🔍 Cerrando menú de pausa en singleplayer, reanudando música");
            if (Minecraft.getInstance().player instanceof LocalPlayer) {
                LocalPlayer player = (LocalPlayer) Minecraft.getInstance().player;
                MusicManager.resumeMusic(player);
            } else {
                System.out.println("[RegionVisualizer] ⚠️ No se encontró LocalPlayer al cerrar el menú de pausa");
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public void onLevelUnload(LevelEvent.Unload event) {
        // Detectar salida del mundo en singleplayer
        if (Minecraft.getInstance().isSingleplayer()) {
            System.out.println("[RegionVisualizer] 🔍 Mundo descargado en singleplayer, deteniendo música");
            MusicManager.stop(true); // Usar fade-out
        }
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public void onPlayerLoggedOut(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
        // Limpiar recursos al desconectarse (singleplayer o multiplayer)
        MusicManager.onPlayerLoggedOut();
    }
}