package com.riburitu.regionvisualizer.commands;

import java.io.File;
import java.util.concurrent.CompletableFuture;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import com.riburitu.regionvisualizer.network.NetworkHandler;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;

public class PlayMusicCommand {
	public static void playmusic(CommandDispatcher<CommandSourceStack> dispatcher) {
	    dispatcher.register(
	        Commands.literal("playmusic")
	            .requires(source -> source.hasPermission(2))
	            // playmusic <player> start <filename>
	            .then(Commands.argument("player", EntityArgument.player())
	                .then(Commands.literal("start")
	                    .then(Commands.argument("musicFile", StringArgumentType.string())
	                    	.suggests(PlayMusicCommand::suggestMusicFiles)	// <--- autocompletado
	                        .executes(ctx -> executePlayMusic(ctx)))))

	            // playmusic <player> volume <volumen>
	            .then(Commands.argument("player", EntityArgument.player())
	                .then(Commands.literal("volume")
	                    .then(Commands.argument("volume", FloatArgumentType.floatArg(0.0f, 1.0f))
	                        .executes(ctx -> executeSetVolume(ctx)))))

	            // playmusic <player> stop
	            .then(Commands.argument("player", EntityArgument.player())
	                .then(Commands.literal("stop")
	                    .executes(ctx -> executeStopMusic(ctx))))

	            // playmusic <player> list
	            .then(Commands.argument("player", EntityArgument.player())
	                .then(Commands.literal("list")
	                    .executes(ctx -> executeListMusic(ctx))))

	            // playmusic <player> getvolume
	            .then(Commands.argument("player", EntityArgument.player())
	                .then(Commands.literal("getvolume")
	                    .executes(ctx -> executeGetVolume(ctx))))
//	            .then(Commands.literal("config")
//	            	.executes(ctx -> executeConfig()))
//	            Comando a futuro, para administradores.
	    );
	}

//    public static void playcache(CommandDispatcher<CommandSourceStack> dispatcher) {
//    	dispatcher.register(
//    			Commands.literal("playcache")
//    			.requires(source -> source.hasPermission(2))
//    			.then(Commands.literal("cache_stats")
//    					.executes(ctx -> executeCacheStats(ctx)))
//    			.then(Commands.literal("cache_status")
//    					.executes(ctx -> executeCacheStatus(ctx)))
//    			.then(Commands.literal("cache_info")
//    					.executes(ctx -> executeCacheInfo(ctx)))
//    			.then(Commands.literal("cache_clear")
//    					.executes(ctx -> executeCacheClear(ctx)))
//    			);
//    }
// Descartado por el menu de configuración.
	
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

    private static int executePlayMusic(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
            String filename = StringArgumentType.getString(ctx, "filename");

            NetworkHandler.sendPlayMusic(player, filename);
            ctx.getSource().sendSuccess(() -> Component.literal("Reproduciendo música '" + filename + "' para " + player.getName().getString()).withStyle(ChatFormatting.GREEN), true);
            System.out.println("[RegionVisualizer] Enviado comando play: " + filename + " para " + player.getName().getString());
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Error reproduciendo música: " + e.getMessage()).withStyle(ChatFormatting.RED));
            System.err.println("[RegionVisualizer] Error en comando playmusic: " + e.getMessage());
            return 0;
        }
    }

    private static int executeSetVolume(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
            float volume = FloatArgumentType.getFloat(ctx, "volume");

            NetworkHandler.sendPlayMusic(player, "VOLUME:" + volume);
            ctx.getSource().sendSuccess(() -> Component.literal("Volumen establecido a " + Math.round(volume * 100) + "% para " + player.getName().getString()).withStyle(ChatFormatting.GREEN), true);
            System.out.println("[RegionVisualizer] Enviado comando volume: " + volume + " para " + player.getName().getString());
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Error estableciendo volumen: " + e.getMessage()).withStyle(ChatFormatting.RED));
            System.err.println("[RegionVisualizer] Error en comando volume: " + e.getMessage());
            return 0;
        }
    }

    private static int executeStopMusic(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(ctx, "player");

            NetworkHandler.sendPlayMusic(player, "STOP");
            ctx.getSource().sendSuccess(() -> Component.literal("Música detenida para " + player.getName().getString()).withStyle(ChatFormatting.YELLOW), true);
            System.out.println("[RegionVisualizer] Enviado comando stop para " + player.getName().getString());
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Error deteniendo música: " + e.getMessage()).withStyle(ChatFormatting.RED));
            System.err.println("[RegionVisualizer] Error en comando stop: " + e.getMessage());
            return 0;
        }
    }

    private static int executeListMusic(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(ctx, "player");

            NetworkHandler.sendPlayMusic(player, "LIST");
            ctx.getSource().sendSuccess(() -> Component.literal("Lista de música enviada a " + player.getName().getString()).withStyle(ChatFormatting.GREEN), true);
            System.out.println("[RegionVisualizer] Enviado comando list para " + player.getName().getString());
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Error listando música: " + e.getMessage()).withStyle(ChatFormatting.RED));
            System.err.println("[RegionVisualizer] Error en comando list: " + e.getMessage());
            return 0;
        }
    }

    private static int executeGetVolume(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(ctx, "player");

            NetworkHandler.sendPlayMusic(player, "GET_VOLUME");
            ctx.getSource().sendSuccess(() -> Component.literal("Solicitado volumen actual para " + player.getName().getString()).withStyle(ChatFormatting.GREEN), true);
            System.out.println("[RegionVisualizer] Enviado comando getvolume para " + player.getName().getString());
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Error obteniendo volumen: " + e.getMessage()).withStyle(ChatFormatting.RED));
            System.err.println("[RegionVisualizer] Error en comando getvolume: " + e.getMessage());
            return 0;
        }
    }
}