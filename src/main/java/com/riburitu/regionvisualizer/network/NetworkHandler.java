package com.riburitu.regionvisualizer.network;

import com.riburitu.regionvisualizer.RegionVisualizer;
import com.riburitu.regionvisualizer.client.RegionRenderer;
import com.riburitu.regionvisualizer.item.RegionSelectorItem;
import com.riburitu.regionvisualizer.util.Region;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.ChatFormatting;

import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;
import java.util.function.Supplier;

public class NetworkHandler {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(RegionVisualizer.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++, PacketClearSelection.class,
                PacketClearSelection::encode,
                PacketClearSelection::decode,
                PacketClearSelection::handle);
        CHANNEL.registerMessage(id++, PlayMusicPacket.class,
                PlayMusicPacket::encode,
                PlayMusicPacket::decode,
                PlayMusicPacket::handle);
        CHANNEL.registerMessage(id++, PacketViewRegion.class,
                PacketViewRegion::encode,
                PacketViewRegion::decode,
                PacketViewRegion::handle);
        CHANNEL.registerMessage(id++, PacketClearVisualization.class,
                PacketClearVisualization::encode,
                PacketClearVisualization::decode,
                PacketClearVisualization::handle);
        CHANNEL.registerMessage(id++, PacketEditPos.class,
                PacketEditPos::encode,
                PacketEditPos::decode,
                PacketEditPos::handle);
        CHANNEL.registerMessage(id++, PacketOverlayMessage.class,
                PacketOverlayMessage::encode,
                PacketOverlayMessage::decode,
                PacketOverlayMessage::handle);
        // Nuevo mensaje para solicitud de región
        CHANNEL.registerMessage(id++, RegionRequestPacket.class,
                RegionRequestPacket::encode,
                RegionRequestPacket::decode,
                RegionRequestPacket::handle);
        // Detección de la selección en lado cliente
        CHANNEL.registerMessage(id++, PacketSyncSelection.class,
                PacketSyncSelection::encode,
                PacketSyncSelection::decode,
                PacketSyncSelection::handle);
    }

    public static void sendPlayMusic(ServerPlayer player, String filename) {
        CHANNEL.sendTo(new PlayMusicPacket(filename), player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
    }

    public static void sendViewRegion(ServerPlayer player, Region region) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new PacketViewRegion(region));
    }

    public static void sendSyncSelection(ServerPlayer player, BlockPos pos1, BlockPos pos2) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new PacketSyncSelection(pos1, pos2));
    }
    public static void sendClearVisualization(ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new PacketClearVisualization());
    }

    public static void sendEditPos(ServerPlayer player, Region region) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new PacketEditPos(region));
    }

    public static void sendOverlayMessage(ServerPlayer player, Component message) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new PacketOverlayMessage(message));
    }

    public static void sendRegionRequest(BlockPos pos) {
        CHANNEL.sendToServer(new RegionRequestPacket(pos));
    }
}

class PacketViewRegion {
    private final Region region;

    public PacketViewRegion(Region region) {
        this.region = region;
    }

    public static void encode(PacketViewRegion msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.region.getName());
        buf.writeBlockPos(msg.region.getPos1());
        buf.writeBlockPos(msg.region.getPos2());
        buf.writeUtf(msg.region.getMusicFile() != null ? msg.region.getMusicFile() : "");
        buf.writeBoolean(msg.region.isLoopEnabled());
        buf.writeBoolean(msg.region.isFadeEnabled());
    }

    public static PacketViewRegion decode(FriendlyByteBuf buf) {
        String name = buf.readUtf();
        BlockPos pos1 = buf.readBlockPos();
        BlockPos pos2 = buf.readBlockPos();
        String musicFile = buf.readUtf();
        boolean loop = buf.readBoolean();
        boolean fade = buf.readBoolean();
        return new PacketViewRegion(new Region(name, pos1, pos2, musicFile.isEmpty() ? null : musicFile, loop, fade));
    }

    public static void handle(PacketViewRegion msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            RegionRenderer.setViewedRegion(msg.region);
        });
        ctx.get().setPacketHandled(true);
    }
}

class PacketClearVisualization {
    public PacketClearVisualization() {}

    public static void encode(PacketClearVisualization msg, FriendlyByteBuf buf) {}

    public static PacketClearVisualization decode(FriendlyByteBuf buf) {
        return new PacketClearVisualization();
    }

    public static void handle(PacketClearVisualization msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            RegionRenderer.clearViewedRegion();
        });
        ctx.get().setPacketHandled(true);
    }
}

class PacketEditPos {
    private final Region region;

    public PacketEditPos(Region region) {
        this.region = region;
    }

    public static void encode(PacketEditPos msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.region.getName());
        buf.writeBlockPos(msg.region.getPos1());
        buf.writeBlockPos(msg.region.getPos2());
        buf.writeUtf(msg.region.getMusicFile() != null ? msg.region.getMusicFile() : "");
        buf.writeBoolean(msg.region.isLoopEnabled());
        buf.writeBoolean(msg.region.isFadeEnabled());
    }

    public static PacketEditPos decode(FriendlyByteBuf buf) {
        String name = buf.readUtf();
        BlockPos pos1 = buf.readBlockPos();
        BlockPos pos2 = buf.readBlockPos();
        String musicFile = buf.readUtf();
        boolean loop = buf.readBoolean();
        boolean fade = buf.readBoolean();
        return new PacketEditPos(new Region(name, pos1, pos2, musicFile.isEmpty() ? null : musicFile, loop, fade));
    }

    public static void handle(PacketEditPos msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            RegionSelectorItem.setEditingRegion(msg.region);
            RegionRenderer.setViewedRegion(msg.region);
        });
        ctx.get().setPacketHandled(true);
    }
}

class PacketOverlayMessage {
    private final Component message;

    public PacketOverlayMessage(Component message) {
        this.message = message;
    }

    public static void encode(PacketOverlayMessage msg, FriendlyByteBuf buf) {
        buf.writeComponent(msg.message);
    }

    public static PacketOverlayMessage decode(FriendlyByteBuf buf) {
        return new PacketOverlayMessage(buf.readComponent());
    }

    public static void handle(PacketOverlayMessage msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (ctx.get().getDirection().getReceptionSide().isClient()) {
                net.minecraft.client.Minecraft.getInstance().gui.setOverlayMessage(msg.message, false);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}

class RegionRequestPacket {
    private final BlockPos pos;

    public RegionRequestPacket(BlockPos pos) {
        this.pos = pos;
    }

    public static void encode(RegionRequestPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
    }

    public static RegionRequestPacket decode(FriendlyByteBuf buf) {
        return new RegionRequestPacket(buf.readBlockPos());
    }

    public static void handle(RegionRequestPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                System.err.println("[RegionVisualizer] ⚠️ Jugador nulo al manejar RegionRequestPacket");
                // Determinar la región en el servidor
                String regionName = RegionVisualizer.INSTANCE.getCurrentRegion(player.level(), msg.pos);
                if (regionName != null) {
                    Optional<Region> regionOpt = RegionVisualizer.INSTANCE.getRegionManager().getRegionByName(regionName);
                    if (regionOpt.isPresent()) {
                        Region region = regionOpt.get();
                        String musicCommand = "MUSIC:" + region.getMusicFile() + ":" + region.isLoopEnabled() + ":" + region.isFadeEnabled();
                        NetworkHandler.sendPlayMusic(player, musicCommand);
                        NetworkHandler.sendOverlayMessage(player, Component.literal("Reproduciendo --> " + region.getMusicFile()).withStyle(ChatFormatting.GREEN));
                        System.out.println("[RegionVisualizer] " + player.getName().getString() + " ha entrado en " + regionName);
                    }
                } else {
                    NetworkHandler.sendPlayMusic(player, "STOP:false");
                    NetworkHandler.sendOverlayMessage(player, Component.literal("Música detenida").withStyle(ChatFormatting.YELLOW));
                    System.out.println("[RegionVisualizer] " + player.getName().getString() + " no está en ninguna región.");
                }
                return;
            } else {
                System.err.println("[RegionVisualizer] ⚠️ Jugador nulo al manejar RegionRequestPacket");
            }
            
        });
        ctx.get().setPacketHandled(true);
    }
}