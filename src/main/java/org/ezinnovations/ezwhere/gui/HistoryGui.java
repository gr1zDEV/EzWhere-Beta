package org.ezinnovations.ezwhere.gui;

import org.ezinnovations.ezwhere.storage.WhereSnapshot;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class HistoryGui implements Listener {
    private static final int INVENTORY_SIZE = 54;
    private static final int ENTRIES_PER_PAGE = 44;

    private final String title;
    private final DateTimeFormatter formatter;
    private final boolean includeYawPitch;

    public HistoryGui(String title, String dateTimeFormat, boolean includeYawPitch) {
        this.title = title;
        this.formatter = DateTimeFormatter.ofPattern(dateTimeFormat).withZone(ZoneId.systemDefault());
        this.includeYawPitch = includeYawPitch;
    }

    public void open(Player viewer, List<WhereSnapshot> history, int page) {
        int totalPages = Math.max(1, (int) Math.ceil(history.size() / (double) ENTRIES_PER_PAGE));
        int targetPage = Math.max(1, Math.min(page, totalPages));

        Inventory inventory = Bukkit.createInventory(new HistoryHolder(history, targetPage), INVENTORY_SIZE, title);
        fillWithGrayPanes(inventory);

        if (history.isEmpty()) {
            inventory.setItem(22, noHistoryItem());
        } else {
            int startIndex = (targetPage - 1) * ENTRIES_PER_PAGE;
            int endIndex = Math.min(startIndex + ENTRIES_PER_PAGE, history.size());

            for (int i = startIndex; i < endIndex; i++) {
                int slot = i - startIndex;
                inventory.setItem(slot, createHistoryItem(i + 1, history.get(i)));
            }
        }

        inventory.setItem(49, pageItem(targetPage, totalPages));
        if (targetPage > 1) {
            inventory.setItem(45, navigationItem(Material.ARROW, "§aPrevious Page"));
        }
        if (targetPage < totalPages) {
            inventory.setItem(53, navigationItem(Material.ARROW, "§aNext Page"));
        }

        viewer.openInventory(inventory);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof HistoryHolder holder)) {
            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        int clicked = event.getRawSlot();
        if (clicked == 45 && holder.page > 1) {
            open(player, holder.history, holder.page - 1);
        } else if (clicked == 53) {
            int maxPage = Math.max(1, (int) Math.ceil(holder.history.size() / (double) ENTRIES_PER_PAGE));
            if (holder.page < maxPage) {
                open(player, holder.history, holder.page + 1);
            }
        }
    }

    private void fillWithGrayPanes(Inventory inventory) {
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = filler.getItemMeta();
        meta.setDisplayName(" ");
        filler.setItemMeta(meta);

        for (int i = 0; i < INVENTORY_SIZE; i++) {
            inventory.setItem(i, filler);
        }
    }

    private ItemStack createHistoryItem(int index, WhereSnapshot snapshot) {
        ItemStack item = new ItemStack(Material.GREEN_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§a#" + index + " " + snapshot.world());

        List<String> lore = new ArrayList<>();
        lore.add("§7Time: §f" + formatter.format(snapshot.timestamp()));
        lore.add("§7XYZ: §f" + snapshot.x() + " " + snapshot.y() + " " + snapshot.z());
        if (includeYawPitch && snapshot.yaw() != null && snapshot.pitch() != null) {
            lore.add(String.format("§7Yaw/Pitch: §f%.2f / %.2f", snapshot.yaw(), snapshot.pitch()));
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack noHistoryItem() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§cNo history yet");
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack pageItem(int page, int totalPages) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§ePage " + page + " / " + totalPages);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack navigationItem(Material material, String displayName) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(displayName);
        item.setItemMeta(meta);
        return item;
    }

    private record HistoryHolder(List<WhereSnapshot> history, int page) implements InventoryHolder {

        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
