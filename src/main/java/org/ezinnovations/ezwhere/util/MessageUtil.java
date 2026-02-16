package org.ezinnovations.ezwhere.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Map;

public final class MessageUtil {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private MessageUtil() {
    }

    public static void sendMessage(CommandSender sender, FileConfiguration messages, String path) {
        sendMessage(sender, messages, path, Map.of());
    }

    public static void sendMessage(CommandSender sender, FileConfiguration messages, String path, Map<String, String> placeholders) {
        String raw = messages.getString(path, "<red>Missing message: " + path + "</red>");
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            raw = raw.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        Component component = MINI_MESSAGE.deserialize(raw);
        sender.sendMessage(component);
    }

    public static Component component(FileConfiguration messages, String path, Map<String, String> placeholders) {
        String raw = messages.getString(path, "<red>Missing message: " + path + "</red>");
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            raw = raw.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return MINI_MESSAGE.deserialize(raw);
    }
}
