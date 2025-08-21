// RegionSelectorItem.java

package com.riburitu.regionvisualizer.item;

import com.riburitu.regionvisualizer.RegionVisualizer;
import com.riburitu.regionvisualizer.network.NetworkHandler;
import com.riburitu.regionvisualizer.network.PacketClearSelection;
import com.riburitu.regionvisualizer.util.Region;
import com.riburitu.regionvisualizer.util.RegionSelection;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.List;

public class RegionSelectorItem extends Item {
    private static final RegionSelection selection = new RegionSelection();
    private static BlockPos lastPos1 = null;
    private static BlockPos lastPos2 = null;
    private static Region editingRegion = null;
    private static RegionSelection positionRegion = null;

    public static void syncSelectionFromServer(BlockPos pos1, BlockPos pos2) {
        if (pos1 != null) {
            selection.setPos1(pos1);
            lastPos1 = pos1;
        }
        if (pos2 != null) {
            selection.setPos2(pos2);
            lastPos2 = pos2;
        }
        System.out.println("[RegionVisualizer] Selección sincronizada desde servidor: pos1=" + pos1 + ", pos2=" + pos2);
    }
    
    public RegionSelectorItem(Properties props) {
        super(props);
    }
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        BlockPos clicked = context.getClickedPos();
        if (player == null) return InteractionResult.FAIL;

        if (level.isClientSide()) {
            // Opcional: Feedback local en cliente, pero no lógica de servidor
            return InteractionResult.SUCCESS;
        }
        
     // Ahora en servidor
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.FAIL;
        try {
            boolean wasComplete = selection.isComplete();
            Component message;
            if (editingRegion != null) {
                if (player.isShiftKeyDown()) {
                    if (!clicked.equals(lastPos2)) {
                        selection.setPos2(clicked);
                        lastPos2 = clicked;
                        message = Component.literal("Pos2 establecida para región '" + editingRegion.getName() + "': " + formatPos(clicked))
                            .withStyle(ChatFormatting.LIGHT_PURPLE);
                        NetworkHandler.sendOverlayMessage((ServerPlayer) player, message);
                        System.out.println("[RegionVisualizer] " + player.getName().getString() + " estableció Pos2 para región '" + editingRegion.getName() + "': " + clicked);
                    } else {
                        return InteractionResult.PASS;
                    }
                } else {
                    if (!clicked.equals(lastPos1)) {
                        selection.setPos1(clicked);
                        lastPos1 = clicked;
                        message = Component.literal("Pos1 establecida para región '" + editingRegion.getName() + "': " + formatPos(clicked))
                            .withStyle(ChatFormatting.LIGHT_PURPLE);
                        NetworkHandler.sendOverlayMessage((ServerPlayer) player, message);
                        System.out.println("[RegionVisualizer] " + player.getName().getString() + " estableció Pos1 para región '" + editingRegion.getName() + "': " + clicked);
                    } else {
                        return InteractionResult.PASS;
                    }
                }
                if (selection.isComplete()) {
                    editingRegion.setPos1(selection.pos1);
                    editingRegion.setPos2(selection.pos2);
                    RegionVisualizer.INSTANCE.getRegionManager().updateRegion(editingRegion);
                    RegionVisualizer.INSTANCE.getRegionManager().saveRegions(player.getServer().overworld());
                    message = Component.literal("Posiciones de la región '" + editingRegion.getName() + "' actualizadas.")
                        .withStyle(ChatFormatting.GREEN);
                    NetworkHandler.sendOverlayMessage((ServerPlayer) player, message);
                    System.out.println("[RegionVisualizer] Posiciones actualizadas para región '" + editingRegion.getName() + "': pos1=" + selection.pos1 + ", pos2=" + selection.pos2);
                    editingRegion = null;
                    clearSelection();
                    NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) player), new PacketClearSelection());
                }
            } else {
                if (player.isShiftKeyDown()) {
                    if (!clicked.equals(lastPos2)) {
                        selection.setPos2(clicked);
                        lastPos2 = clicked;
                        message = Component.literal("Pos2 establecida: " + formatPos(clicked))
                            .withStyle(ChatFormatting.LIGHT_PURPLE);
                        NetworkHandler.sendOverlayMessage((ServerPlayer) player, message);
                        System.out.println("[RegionVisualizer] " + player.getName().getString() + " estableció Pos2: " + clicked);
                    } else {
                        return InteractionResult.PASS;
                    }
                } else {
                    if (!clicked.equals(lastPos1)) {
                        selection.setPos1(clicked);
                        lastPos1 = clicked;
                        message = Component.literal("Pos1 establecida: " + formatPos(clicked))
                            .withStyle(ChatFormatting.LIGHT_PURPLE);
                        NetworkHandler.sendOverlayMessage((ServerPlayer) player, message);
                        System.out.println("[RegionVisualizer] " + player.getName().getString() + " estableció Pos1: " + clicked);
                    } else {
                        return InteractionResult.PASS;
                    }
                }
                if (!wasComplete && selection.isComplete()) {
                    int sizeX = Math.abs(selection.pos1.getX() - selection.pos2.getX()) + 1;
                    int sizeY = Math.abs(selection.pos1.getY() - selection.pos2.getY()) + 1;
                    int sizeZ = Math.abs(selection.pos1.getZ() - selection.pos2.getZ()) + 1;
                    message = Component.literal("Selección completa - Tamaño: " + sizeX + "x" + sizeY + "x" + sizeZ + " bloques")
                        .withStyle(ChatFormatting.GREEN);
                    NetworkHandler.sendOverlayMessage((ServerPlayer) player, message);
                    System.out.println("[RegionVisualizer] Selección completa para " + player.getName().getString() + ": pos1=" + selection.pos1 + ", pos2=" + selection.pos2);
                }
            }
        } catch (Exception e) {
            System.err.println("[RegionVisualizer] Error en RegionSelectorItem: " + e.getMessage());
            e.printStackTrace();
            Component message = Component.literal("Error al seleccionar posición").withStyle(ChatFormatting.RED);
            NetworkHandler.sendOverlayMessage(serverPlayer, message);
            return InteractionResult.FAIL;
        }
        // Después del código anterior... Sincronizamos la selección.
        NetworkHandler.sendSyncSelection(serverPlayer, selection.pos1, selection.pos2);
        if (editingRegion == null && selection.isComplete()) {
            // Después de completar selección...
            NetworkHandler.sendSyncSelection(serverPlayer, selection.pos1, selection.pos2);
        }
        return InteractionResult.SUCCESS;
    }
    public static void setEditingRegion(Region region) {
        editingRegion = region;
        selection.setPos1(region.getPos1());
        selection.setPos2(region.getPos2());
        System.out.println("[RegionVisualizer] Modo edición activado para región: " + region.getName());
    }
    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Selector de Regiones").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("Click izquierdo: Establecer Pos1").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("Shift + Click izquierdo: Establecer Pos2").withStyle(ChatFormatting.LIGHT_PURPLE));
        
        if (selection.pos1 != null) {
            tooltip.add(Component.literal("Pos1: " + formatPos(selection.pos1)).withStyle(ChatFormatting.GRAY));
        }
        
        if (selection.pos2 != null) {
            tooltip.add(Component.literal("Pos2: " + formatPos(selection.pos2)).withStyle(ChatFormatting.GRAY));
        }
        
        if (selection.isComplete()) {
            int sizeX = Math.abs(selection.pos1.getX() - selection.pos2.getX()) + 1;
            int sizeY = Math.abs(selection.pos1.getY() - selection.pos2.getY()) + 1;
            int sizeZ = Math.abs(selection.pos1.getZ() - selection.pos2.getZ()) + 1;
            tooltip.add(Component.literal("Tamaño: " + sizeX + "x" + sizeY + "x" + sizeZ).withStyle(ChatFormatting.GREEN));
        } else {
            tooltip.add(Component.literal("Selección incompleta").withStyle(ChatFormatting.YELLOW));
        }
    }

    public static RegionSelection getSelection() {
        return selection;
    }

    public static void clearSelection() {
        selection.setPos1(null);
        selection.setPos2(null);
        lastPos1 = null;
        lastPos2 = null;
        editingRegion = null;
        System.out.println("[RegionVisualizer] Selección limpiada.");
    }
    private static String formatPos(BlockPos pos) {
        return pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        clearSelection();
        System.out.println("[RegionVisualizer] Selección limpiada al salir el jugador: " + event.getEntity().getName().getString());
    }

    @SubscribeEvent  
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        // Limpiar selección cuando un jugador se conecta nuevamente para evitar conflictos.
        clearSelection();
        System.out.println("[RegionVisualizer] Selección reiniciada para nuevo jugador: " + event.getEntity().getName().getString());
    }
}