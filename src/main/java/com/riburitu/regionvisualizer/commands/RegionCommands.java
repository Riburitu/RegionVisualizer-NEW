package com.riburitu.regionvisualizer.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import com.riburitu.regionvisualizer.util.Region;
import com.riburitu.regionvisualizer.util.RegionManager;
import com.riburitu.regionvisualizer.RegionVisualizer;
import com.riburitu.regionvisualizer.item.RegionSelectorItem;
import com.riburitu.regionvisualizer.network.NetworkHandler;
import com.riburitu.regionvisualizer.network.PacketClearSelection;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.core.BlockPos;
import net.minecraftforge.network.PacketDistributor;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@SuppressWarnings("unused")
public class RegionCommands {
    private final RegionManager regionManager;

    public RegionCommands(RegionManager regionManager) {
        this.regionManager = regionManager;
    }

    public void region(CommandDispatcher<CommandSourceStack> dispatcher) {
    	dispatcher.register(
    		    Commands.literal("region")
    		        .requires(source -> source.hasPermission(2))
    		        
    		        .then(Commands.literal("add")
    		            .then(Commands.argument("RegionTag", StringArgumentType.word())
    		                .then(Commands.argument("musicFile", StringArgumentType.string())
    		                	.suggests(RegionCommands::suggestMusicFiles)
    		                    .then(Commands.argument("loop", BoolArgumentType.bool())
    		                        .executes(ctx -> executeAddRegion(ctx, false))
    		                        .then(Commands.argument("fade", BoolArgumentType.bool())
    		                            .executes(ctx -> executeAddRegion(ctx, BoolArgumentType.getBool(ctx, "fade"))))))))
                
                .then(Commands.literal("remove")
                    .then(Commands.argument("RegionTag", StringArgumentType.word())
                    	.suggests(RegionCommands::suggestRegions)
                        .executes(ctx -> executeRemoveRegion(ctx))))
                
                .then(Commands.literal("list")
                    .executes(ctx -> executeListRegions(ctx)))
                
                .then(Commands.literal("tp")
                    .then(Commands.argument("RegionTag", StringArgumentType.word())
                    	.suggests(RegionCommands::suggestRegions)
                        .executes(ctx -> executeTeleportToRegion(ctx))))
                
                .then(Commands.literal("cancel")
                    .executes(ctx -> executeCancelSelection(ctx)))
                
                .then(Commands.literal("info")
                    .then(Commands.argument("RegionTag", StringArgumentType.word())
                    	.suggests(RegionCommands::suggestRegions)
                        .executes(ctx -> executeRegionInfo(ctx))))
                
                .then(Commands.literal("here")
                    .executes(ctx -> executeRegionHere(ctx)))
        );
    }
    public void regedit(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("regedit")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("view")
                    .then(Commands.argument("RegionTag", StringArgumentType.word())
                    	.suggests(RegionCommands::suggestRegions)
                        .executes(ctx -> executeViewRegion(ctx))))
                
                .then(Commands.literal("clear")
                    .executes(ctx -> executeClearVisualization(ctx)))
                
                .then(Commands.literal("music")
                    .then(Commands.argument("RegionTag", StringArgumentType.word())
                    	.suggests(RegionCommands::suggestRegions)
                        .then(Commands.argument("musicFile", StringArgumentType.string())
                        	.suggests(RegionCommands::suggestMusicFiles)
                            .executes(ctx -> executeEditMusic(ctx)))))
                    
                .then(Commands.literal("regname")
                    .then(Commands.argument("RegionTag", StringArgumentType.word())
                    	.suggests(RegionCommands::suggestRegions)
                        .then(Commands.argument("newRegionTag", StringArgumentType.word())
                            .executes(ctx -> executeEditName(ctx)))))
                    
                .then(Commands.literal("pos")
                    .then(Commands.argument("RegionTag", StringArgumentType.word())
                    	.suggests(RegionCommands::suggestRegions)
                        .executes(ctx -> executeEditPos(ctx))))
        );
    }
    private static CompletableFuture<Suggestions> suggestRegions(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        // Obtén el RegionManager desde la instancia global
        RegionManager regionManager = RegionVisualizer.INSTANCE.getRegionManager();
        for (Region region : regionManager.getRegions()) {
            builder.suggest(region.getName());
        }
        return builder.buildFuture();
    }
    private static CompletableFuture<Suggestions> suggestMusicFiles(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        File musicFolder;

        // Si estamos en servidor
        if (ctx.getSource().getServer() != null) {
            musicFolder = new File(ctx.getSource().getServer().getServerDirectory(), "music");
        } else {
            // Si estamos en cliente
            musicFolder = new File(Minecraft.getInstance().gameDirectory, "music");
        }

        if (musicFolder.exists() && musicFolder.isDirectory()) {
            for (File file : musicFolder.listFiles()) {
            	if (file.isFile() && (file.getName().endsWith(".ogg") || file.getName().endsWith(".wav"))) {
                    builder.suggest(file.getName());
                }
            }
        }
        return builder.buildFuture();
    }
    private int executeAddRegion(CommandContext<CommandSourceStack> ctx, boolean fade) {
    	if (regionManager == null) {
            ctx.getSource().sendFailure(Component.literal("Error: RegionManager no inicializado").withStyle(ChatFormatting.RED));
            return 0;
        }
    	try {
            CommandSourceStack source = ctx.getSource();
            ServerPlayer player = source.getPlayerOrException();
            BlockPos pos1 = RegionSelectorItem.getSelection().pos1;
            BlockPos pos2 = RegionSelectorItem.getSelection().pos2;

            if (pos1 == null || pos2 == null) {
                source.sendFailure(Component.literal("Debes seleccionar dos posiciones con el Region Selector.").withStyle(ChatFormatting.RED));
                return 0;
            }

            String name = StringArgumentType.getString(ctx, "RegionTag");
            if (regionManager.getRegionByName(name).isPresent()) {
                source.sendFailure(Component.literal("Región ya existente llamada '" + name + "'.").withStyle(ChatFormatting.RED));
                return 0;
            }

            String musicFile = StringArgumentType.getString(ctx, "musicFile");
            boolean loopEnabled = BoolArgumentType.getBool(ctx, "loop");
            System.out.println("[RegionVisualizer] 📥 Comando /region add recibido: name=" + name + ", musicFile=" + musicFile + ", loop=" + loopEnabled + ", fade=" + fade);

            Region region = new Region(name, pos1, pos2, musicFile, loopEnabled, fade);
            regionManager.addRegion(region);
            regionManager.saveRegions(player.serverLevel());

            source.sendSuccess(() -> Component.literal("Región '" + name + "' creada con música '" + musicFile + "', bucle: " + (loopEnabled ? "Activado" : "Desactivado") + ", fade: " + (fade ? "Activado" : "Desactivado")).withStyle(ChatFormatting.GREEN), true);
            NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new PacketClearSelection());

            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Error creando región: " + e.getMessage()).withStyle(ChatFormatting.RED));
            System.err.println("[RegionVisualizer] Error creando región: " + e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }

    private int executeRemoveRegion(CommandContext<CommandSourceStack> ctx) {
    	if (regionManager == null) {
            ctx.getSource().sendFailure(Component.literal("Error: RegionManager no inicializado").withStyle(ChatFormatting.RED));
            return 0;
        }
    	try {
            String name = StringArgumentType.getString(ctx, "RegionTag");
            CommandSourceStack source = ctx.getSource();

            if (regionManager.removeRegion(name)) {
                regionManager.saveRegions(source.getLevel());
                source.sendSuccess(() -> Component.literal("Región '" + name + "' eliminada.").withStyle(ChatFormatting.GREEN), true);
                return 1;
            } else {
                source.sendFailure(Component.literal("No se encontró la región '" + name + "'.").withStyle(ChatFormatting.RED));
                return 0;
            }
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Error eliminando región: " + e.getMessage()).withStyle(ChatFormatting.RED));
            System.err.println("[RegionVisualizer] Error eliminando región: " + e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }

    private int executeListRegions(CommandContext<CommandSourceStack> ctx) {
        if (regionManager == null) {
            ctx.getSource().sendFailure(Component.literal("Error: RegionManager no inicializado").withStyle(ChatFormatting.RED));
            return 0;
        }
        try {
            CommandSourceStack source = ctx.getSource();
            List<Region> regions = regionManager.getRegions();
            
            // Log de depuración para verificar el tamaño real
            System.out.println("[RegionVisualizer] Número de regiones a listar: " + regions.size());
            
            if (regions.isEmpty()) {
                source.sendSuccess(() -> Component.literal("No hay regiones definidas.").withStyle(ChatFormatting.YELLOW), false);
            } else {
                // Construir un componente compuesto para el mensaje
                MutableComponent message = Component.literal("=== Lista de Regiones ===\n").withStyle(ChatFormatting.GOLD);
                for (int i = 0; i < regions.size(); i++) {
                    Region r = regions.get(i);
                    message.append(Component.literal((i + 1) + ". " + r.getName() + "\n").withStyle(ChatFormatting.GRAY));
                    
                    // Log de depuración por región
                    System.out.println("[RegionVisualizer] Listando región " + (i + 1) + ": " + r.getName());
                }
                
                // Enviar el mensaje consolidado
                source.sendSuccess(() -> message, false);
            }
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Error listando regiones: " + e.getMessage()).withStyle(ChatFormatting.RED));
            System.err.println("[RegionVisualizer] Error listando regiones: " + e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }

    private int executeTeleportToRegion(CommandContext<CommandSourceStack> ctx) {
    	if (regionManager == null) {
            ctx.getSource().sendFailure(Component.literal("Error: RegionManager no inicializado").withStyle(ChatFormatting.RED));
            return 0;
        }
    	try {
            String name = StringArgumentType.getString(ctx, "RegionTag");
            CommandSourceStack source = ctx.getSource();
            ServerPlayer player = source.getPlayerOrException();
            Optional<Region> regionOpt = regionManager.getRegionByName(name);

            if (regionOpt.isEmpty()) {
                source.sendFailure(Component.literal("[RegionVisualizer] No se encontró la región '" + name + "'.").withStyle(ChatFormatting.RED));
                return 0;
            }

            Region region = regionOpt.get();
            BlockPos center = region.getCenter();
            player.teleportTo(center.getX() + 0.5, center.getY(), center.getZ() + 0.5);
            source.sendSuccess(() -> Component.literal("[RegionVisualizer] Teletransportado a la región '" + name + "'.").withStyle(ChatFormatting.GREEN), true);
            return 1;

        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("[RegionVisualizer] Error teletransportando: " + e.getMessage()).withStyle(ChatFormatting.RED));
            System.err.println("[RegionVisualizer] Error teletransportando: " + e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }

    private int executeCancelSelection(CommandContext<CommandSourceStack> ctx) {
        try {
            CommandSourceStack source = ctx.getSource();
            ServerPlayer player = source.getPlayerOrException();
            RegionSelectorItem.clearSelection();
            NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new PacketClearSelection());
            source.sendSuccess(() -> Component.literal("Selección cancelada.").withStyle(ChatFormatting.GREEN), true);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Error cancelando selección: " + e.getMessage()).withStyle(ChatFormatting.RED));
            System.err.println("[RegionVisualizer] Error cancelando selección: " + e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }

    private int executeRegionInfo(CommandContext<CommandSourceStack> ctx) {
    	if (regionManager == null) {
            ctx.getSource().sendFailure(Component.literal("Error: RegionManager no inicializado").withStyle(ChatFormatting.RED));
            return 0;
        }
    	try {
            String name = StringArgumentType.getString(ctx, "RegionTag");
            CommandSourceStack source = ctx.getSource();
            Optional<Region> regionOpt = regionManager.getRegionByName(name);

            if (regionOpt.isEmpty()) {
                source.sendFailure(Component.literal("[RegionVisualizer] No se encontró la región '" + name + "'.").withStyle(ChatFormatting.RED));
                return 0;
            }

            Region region = regionOpt.get();
            BlockPos pos1 = region.getPos1();
            BlockPos pos2 = region.getPos2();
            BlockPos center = region.getCenter();

            source.sendSuccess(() -> Component.literal("=== Información de Región: " + name + " ===").withStyle(ChatFormatting.GOLD), false);
            source.sendSuccess(() -> Component.literal("Posición 1: " + pos1.getX() + ", " + pos1.getY() + ", " + pos1.getZ()).withStyle(ChatFormatting.LIGHT_PURPLE), false);
            source.sendSuccess(() -> Component.literal("Posición 2: " + pos2.getX() + ", " + pos2.getY() + ", " + pos2.getZ()).withStyle(ChatFormatting.LIGHT_PURPLE), false);
            source.sendSuccess(() -> Component.literal("Centro: " + center.getX() + ", " + center.getY() + ", " + center.getZ()).withStyle(ChatFormatting.DARK_PURPLE), false);
            source.sendSuccess(() -> Component.literal("Música: " + (region.getMusicFile() != null ? region.getMusicFile() : "Ninguna")).withStyle(ChatFormatting.AQUA), false);
            source.sendSuccess(() -> Component.literal("Bucle: " + (region.isLoopEnabled() ? "Activado" : "Desactivado")).withStyle(ChatFormatting.GREEN), false);
            source.sendSuccess(() -> Component.literal("Fade: " + (region.isFadeEnabled() ? "Activado" : "Desactivado")).withStyle(ChatFormatting.GREEN), false);

            int sizeX = Math.abs(pos1.getX() - pos2.getX()) + 1;
            int sizeY = Math.abs(pos1.getY() - pos2.getY()) + 1;
            int sizeZ = Math.abs(pos1.getZ() - pos2.getZ()) + 1;
            
            source.sendSuccess(() -> Component.literal("Dimensiones: " + sizeX + "x" + sizeY + "x" + sizeZ + " bloques").withStyle(ChatFormatting.YELLOW), false);
                
            return 1;

        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("[RegionVisualizer] Error obteniendo información: " + e.getMessage()).withStyle(ChatFormatting.RED));
            System.err.println("[RegionVisualizer] Error obteniendo información: " + e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }

    private int executeRegionHere(CommandContext<CommandSourceStack> ctx) {
    	if (regionManager == null) {
            ctx.getSource().sendFailure(Component.literal("Error: RegionManager no inicializado").withStyle(ChatFormatting.RED));
            return 0;
        }
    	try {
            CommandSourceStack source = ctx.getSource();
            ServerPlayer player = source.getPlayerOrException();
            BlockPos playerPos = player.blockPosition();

            Optional<Region> regionOpt = regionManager.getRegionContaining(playerPos);
            
            if (regionOpt.isEmpty()) {
                source.sendSuccess(() -> Component.literal("[RegionVisualizer] No estás en ninguna región.").withStyle(ChatFormatting.YELLOW), false);
                return 1;
            }

            Region region = regionOpt.get();
            source.sendSuccess(() -> Component.literal("[RegionVisualizer] Estás en la región: " + region.getName()).withStyle(ChatFormatting.GREEN), false);
                
            return 1;

        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("[RegionVisualizer] Error verificando ubicación: " + e.getMessage()).withStyle(ChatFormatting.RED));
            System.err.println("[RegionVisualizer] Error verificando ubicación: " + e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }
    private int executeEditMusic(CommandContext<CommandSourceStack> ctx) {
    	if (regionManager == null) {
            ctx.getSource().sendFailure(Component.literal("Error: RegionManager no inicializado").withStyle(ChatFormatting.RED));
            return 0;
        }
    	try {
            String name = StringArgumentType.getString(ctx, "RegionTag");
            String musicFile = StringArgumentType.getString(ctx, "musicFile");
            CommandSourceStack source = ctx.getSource();
            ServerPlayer player = source.getPlayerOrException();

            Optional<Region> regionOpt = regionManager.getRegionByName(name);
            if (regionOpt.isEmpty()) {
                source.sendFailure(Component.literal("No se encontró la región '" + name + "'.").withStyle(ChatFormatting.RED));
                return 0;
            }

            Region region = regionOpt.get();
            region.setMusicFile(musicFile);
            regionManager.saveRegions(player.serverLevel());
            source.sendSuccess(() -> Component.literal("Música de la región '" + name + "' cambiada a '" + musicFile + "'.").withStyle(ChatFormatting.GREEN), true);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Error cambiando música: " + e.getMessage()).withStyle(ChatFormatting.RED));
            System.err.println("[RegionVisualizer] Error cambiando música: " + e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }

    private int executeEditName(CommandContext<CommandSourceStack> ctx) {
    	if (regionManager == null) {
            ctx.getSource().sendFailure(Component.literal("Error: RegionManager no inicializado").withStyle(ChatFormatting.RED));
            return 0;
        }
    	try {
            String name = StringArgumentType.getString(ctx, "RegionTag");
            String newName = StringArgumentType.getString(ctx, "newRegionTag");
            CommandSourceStack source = ctx.getSource();
            ServerPlayer player = source.getPlayerOrException();

            if (regionManager.getRegionByName(newName).isPresent()) {
                source.sendFailure(Component.literal("Ya existe una región llamada '" + newName + "'.").withStyle(ChatFormatting.RED));
                return 0;
            }

            Optional<Region> regionOpt = regionManager.getRegionByName(name);
            if (regionOpt.isEmpty()) {
                source.sendFailure(Component.literal("No se encontró la región '" + name + "'.").withStyle(ChatFormatting.RED));
                return 0;
            }

            Region region = regionOpt.get();
            regionManager.removeRegion(name);
            region.setName(newName);
            regionManager.addRegion(region);
            regionManager.saveRegions(player.serverLevel());
            source.sendSuccess(() -> Component.literal("Nombre de la región cambiado de '" + name + "' a '" + newName + "'.").withStyle(ChatFormatting.GREEN), true);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Error cambiando nombre: " + e.getMessage()).withStyle(ChatFormatting.RED));
            System.err.println("[RegionVisualizer] Error cambiando nombre: " + e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }

    private int executeEditPos(CommandContext<CommandSourceStack> ctx) {
    	if (regionManager == null) {
            ctx.getSource().sendFailure(Component.literal("Error: RegionManager no inicializado").withStyle(ChatFormatting.RED));
            return 0;
        }
    	try {
            String name = StringArgumentType.getString(ctx, "RegionTag");
            CommandSourceStack source = ctx.getSource();
            ServerPlayer player = source.getPlayerOrException();

            Optional<Region> regionOpt = regionManager.getRegionByName(name);
            if (regionOpt.isEmpty()) {
                source.sendFailure(Component.literal("No se encontró la región '" + name + "'.").withStyle(ChatFormatting.RED));
                return 0;
            }

            Region region = regionOpt.get();
            RegionSelectorItem.setEditingRegion(region);
            NetworkHandler.sendEditPos(player, region);
            source.sendSuccess(() -> Component.literal("Editando posiciones de la región '" + name + "'. Usa el Region Selector para establecer nuevas posiciones.").withStyle(ChatFormatting.GREEN), true);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Error iniciando edición de posiciones: " + e.getMessage()).withStyle(ChatFormatting.RED));
            System.err.println("[RegionVisualizer] Error iniciando edición de posiciones: " + e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }
    private int executeViewRegion(CommandContext<CommandSourceStack> ctx) {
    	if (regionManager == null) {
            ctx.getSource().sendFailure(Component.literal("Error: RegionManager no inicializado").withStyle(ChatFormatting.RED));
            return 0;
        }
    	try {
            String name = StringArgumentType.getString(ctx, "RegionTag");
            CommandSourceStack source = ctx.getSource();
            ServerPlayer player = source.getPlayerOrException();
            Optional<Region> regionOpt = regionManager.getRegionByName(name);

            if (regionOpt.isEmpty()) {
                source.sendFailure(Component.literal("No se encontró la región '" + name + "'.").withStyle(ChatFormatting.RED));
                return 0;
            }

            Region region = regionOpt.get();
            NetworkHandler.sendViewRegion(player, region);
            source.sendSuccess(() -> Component.literal("Visualizando región '" + name + "'.").withStyle(ChatFormatting.GREEN), true);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Error visualizando región: " + e.getMessage()).withStyle(ChatFormatting.RED));
            System.err.println("[RegionVisualizer] Error visualizando región: " + e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }
    private int executeClearVisualization(CommandContext<CommandSourceStack> ctx) {
        try {
            CommandSourceStack source = ctx.getSource();
            ServerPlayer player = source.getPlayerOrException();
            NetworkHandler.sendClearVisualization(player);
            source.sendSuccess(() -> Component.literal("Visualización de región desactivada.").withStyle(ChatFormatting.GREEN), true);
            return 1;
        } catch (Exception e) {
            ctx.getSource();
            Component.literal("Error desactivando visualización: " + e.getMessage()).withStyle(ChatFormatting.RED);
            System.err.println("[RegionVisualizer] Error desactivando visualización: " + e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }
}