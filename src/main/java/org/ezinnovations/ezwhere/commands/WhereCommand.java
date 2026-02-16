package org.ezinnovations.ezwhere.commands;

import org.ezinnovations.ezwhere.EZWhere;
import org.ezinnovations.ezwhere.storage.HistoryService;
import org.ezinnovations.ezwhere.storage.WhereSnapshot;
import org.ezinnovations.ezwhere.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class WhereCommand implements CommandExecutor, TabCompleter {
    private final EZWhere plugin;
    private final HistoryService historyService;

    public WhereCommand(EZWhere plugin, HistoryService historyService) {
        this.plugin = plugin;
        this.historyService = historyService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.sendMessage(sender, plugin.getMessages(), "only-player");
            return true;
        }

        if (args.length == 0) {
            handleWhere(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("history")) {
            handleHistory(player, args);
            return true;
        }

        return false;
    }

    private void handleWhere(Player player) {
        Location location = player.getLocation();
        WhereSnapshot snapshot = WhereSnapshot.fromLocation(location, plugin.shouldStoreYawPitch());
        historyService.addSnapshot(player.getUniqueId(), snapshot);

        MessageUtil.sendMessage(player, plugin.getMessages(), "current-location", Map.of(
                "x", String.valueOf(snapshot.x()),
                "y", String.valueOf(snapshot.y()),
                "z", String.valueOf(snapshot.z()),
                "world", snapshot.world()
        ));
    }

    private void handleHistory(Player player, String[] args) {
        UUID targetUuid = player.getUniqueId();
        String targetName = player.getName();

        if (args.length > 1) {
            if (!player.hasPermission("ezwhere.history.others")) {
                MessageUtil.sendMessage(player, plugin.getMessages(), "no-permission");
                return;
            }

            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                MessageUtil.sendMessage(player, plugin.getMessages(), "player-not-found", Map.of("player", args[1]));
                return;
            }

            targetUuid = target.getUniqueId();
            targetName = target.getName();
        }

        MessageUtil.sendMessage(player, plugin.getMessages(), "history-loading");
        String finalTargetName = targetName;
        historyService.loadHistory(targetUuid).thenAccept(history ->
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    plugin.getHistoryGui().open(player, history, 1);
                    if (history.isEmpty()) {
                        MessageUtil.sendMessage(player, plugin.getMessages(), "empty-history");
                    } else {
                        MessageUtil.sendMessage(player, plugin.getMessages(), "history-opened", Map.of("player", finalTargetName));
                    }
                })
        );
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            if ("history".startsWith(args[0].toLowerCase())) {
                return List.of("history");
            }
            return Collections.emptyList();
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("history") && sender.hasPermission("ezwhere.history.others")) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .toList();
        }

        return Collections.emptyList();
    }
}
