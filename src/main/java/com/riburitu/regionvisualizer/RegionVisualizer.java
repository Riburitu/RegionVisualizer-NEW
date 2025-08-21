package com.riburitu.regionvisualizer;

import com.riburitu.regionvisualizer.registry.ModItems;
import com.riburitu.regionvisualizer.commands.RegionCommands;
import com.riburitu.regionvisualizer.client.ClientEventHandler;
import com.riburitu.regionvisualizer.commands.PlayMusicCommand;
import com.riburitu.regionvisualizer.item.RegionSelectorItem;
import com.riburitu.regionvisualizer.network.NetworkHandler;
import com.riburitu.regionvisualizer.util.Region;
import com.riburitu.regionvisualizer.util.RegionManager;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.api.distmarker.Dist;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Objects;

@Mod(RegionVisualizer.MODID)
public class RegionVisualizer {
	public static final String MODID = "regionvisualizer";
	public static RegionVisualizer INSTANCE;
    private final Map<ServerPlayer, String> lastRegion = new HashMap<>();
    private final Map<ServerPlayer, Long> lastRegionMessageTime = new HashMap<>();
    private static final long MESSAGE_COOLDOWN = 1000; // 1 segundo de cooldown
    private final static RegionManager regionManager = new RegionManager(); // Primero
    private final RegionCommands regionCommands = new RegionCommands(RegionVisualizer.regionManager);
    public RegionVisualizer(FMLJavaModLoadingContext context) {
        
    	INSTANCE = this;
        IEventBus modEventBus = context.getModEventBus();

        // Registro común (server + client)
        ModItems.ITEMS.register(modEventBus);
        NetworkHandler.register();
        modEventBus.addListener(ModItems::registerCreativeTab);

        // Solo registrar eventos del cliente si estamos en el cliente
        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.addListener(this::onClientSetup);
        }

        // Eventos generales
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(RegionSelectorItem.class);
    }

    public RegionManager getRegionManager() {
        return regionManager;
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        regionCommands.region(event.getDispatcher());
        regionCommands.regedit(event.getDispatcher());
        PlayMusicCommand.playmusic(event.getDispatcher());
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
    	regionManager.loadRegions(event.getServer().overworld());
        System.out.println("[RegionVisualizer] 📂 Regiones cargadas al iniciar el servidor: " + regionManager.getRegions().size());
        for (Region region : regionManager.getRegions()) {
            System.out.println("[RegionVisualizer] 🔍 Región cargada: " + region.getName() + ", música: " + region.getMusicFile() + ", loopEnabled: " + region.isLoopEnabled() + ", fadeEnabled: " + region.isFadeEnabled());
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        if (player.level().isClientSide()) return;
        if (player.tickCount % 5 != 0) return;
        
        ServerLevel level = player.serverLevel();
        String currentRegion = getCurrentRegion(level, player.blockPosition());
        String last = lastRegion.getOrDefault(player, null);

        if (!Objects.equals(currentRegion, last)) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastRegionMessageTime.getOrDefault(player, 0L) < MESSAGE_COOLDOWN) return;
            lastRegionMessageTime.put(player, currentTime);

            if (currentRegion != null) {
                Optional<Region> regionOpt = regionManager.getRegionByName(currentRegion);
                regionOpt.ifPresent(region -> {
                    String musicFile = region.getMusicFile();
                    
                    if (isValidAudioFile(musicFile)) {
                        String musicCommand = "MUSIC:" + musicFile + ":" + region.isLoopEnabled() + ":" + region.isFadeEnabled();
                        NetworkHandler.sendPlayMusic(player, musicCommand);
                        
                        // Remover la extensión del nombre para mostrar
                        String displayName = removeFileExtension(musicFile);
                        
                        // Mensaje al jugador en la HUD superpuesta.
                        NetworkHandler.sendOverlayMessage(player, 
                        		Component.literal("Ahora sonando → ")
                        	        .withStyle(ChatFormatting.GOLD)
                        	        .append(Component.literal("♫ ")
                        	        		.withStyle(ChatFormatting.AQUA))
                        	        .append(Component.literal(displayName)
                        	            .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD))
                        	        .append(Component.literal(" ♫")
                        	            .withStyle(ChatFormatting.AQUA))
                        	);                        
                        System.out.println("[RegionVisualizer] " + player.getName().getString() + " ha entrado en " + currentRegion);
                    } else {
                        NetworkHandler.sendOverlayMessage(player, Component.literal("Formato de audio no soportado: " + musicFile).withStyle(ChatFormatting.RED));
                        System.out.println("[RegionVisualizer] Formato no válido: " + musicFile);
                    }
                });
            } else if (last != null) {
                Optional<Region> lastRegionOpt = regionManager.getRegionByName(last);
                String musicCommand = lastRegionOpt.map(region -> "STOP:" + region.isFadeEnabled()).orElse("STOP:false");
                NetworkHandler.sendPlayMusic(player, musicCommand);
                
                // Mensaje al jugador en la HUD superpuesta.
                NetworkHandler.sendOverlayMessage(player, 
                	    Component.literal("⏹ Música detenida")
                	        .withStyle(ChatFormatting.RED, ChatFormatting.BOLD)
                	);
                
                System.out.println("[RegionVisualizer] " + player.getName().getString() + " salió de la región.");
            }
            lastRegion.put(player, currentRegion);
        }
    }

    // Método para validar extensiones de audio
    private boolean isValidAudioFile(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return false;
        }
        String lowerName = fileName.toLowerCase();
        return lowerName.endsWith(".ogg") || lowerName.endsWith(".wav");
    }
    // Método para remover la extensión
    private String removeFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return fileName;
        }
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(0, lastDot) : fileName;
    }
    public static String getCurrentRegion(Level level, BlockPos pos) {
        return regionManager.getRegions().stream()
            .filter(region -> region.contains(pos))
            .map(Region::getName)
            .findFirst()
            .orElse(null);
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

    // MÉTODOS SOLO CLIENTE - Separados en una clase aparte llamada ClientEventHandler.java
    private void onClientSetup(final FMLClientSetupEvent event) {
        // Solo inicializar cliente si estamos en el lado cliente
        event.enqueueWork(() -> {
            try {
                ClientEventHandler.initializeClient();
                System.out.println("[RegionVisualizer] Cliente configurado - Renderer y Música inicializados");
            } catch (Exception e) {
                System.err.println("[RegionVisualizer] Error inicializando música en cliente: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
}