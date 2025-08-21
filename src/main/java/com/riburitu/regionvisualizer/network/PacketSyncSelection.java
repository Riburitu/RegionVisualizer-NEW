package com.riburitu.regionvisualizer.network;

import com.riburitu.regionvisualizer.item.RegionSelectorItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketSyncSelection {
    private final BlockPos pos1;
    private final BlockPos pos2;

    public PacketSyncSelection(BlockPos pos1, BlockPos pos2) {
        this.pos1 = pos1;
        this.pos2 = pos2;
    }

    public static void encode(PacketSyncSelection msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.pos1 != null);
        if (msg.pos1 != null) {
            buf.writeBlockPos(msg.pos1);
        }
        
        buf.writeBoolean(msg.pos2 != null);
        if (msg.pos2 != null) {
            buf.writeBlockPos(msg.pos2);
        }
    }

    public static PacketSyncSelection decode(FriendlyByteBuf buf) {
        BlockPos pos1 = buf.readBoolean() ? buf.readBlockPos() : null;
        BlockPos pos2 = buf.readBoolean() ? buf.readBlockPos() : null;
        return new PacketSyncSelection(pos1, pos2);
    }

    public static void handle(PacketSyncSelection msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (ctx.get().getDirection().getReceptionSide().isClient()) {
                RegionSelectorItem.syncSelectionFromServer(msg.pos1, msg.pos2);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}