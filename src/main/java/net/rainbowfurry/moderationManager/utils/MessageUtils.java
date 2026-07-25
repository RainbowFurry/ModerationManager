package net.rainbowfurry.moderationManager.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.rainbowfurry.moderationManager.ModerationManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

public class MessageUtils {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacySection();

    public static Component parse(String text) {
        if (text == null || text.isEmpty()) return Component.empty();
        return MINI_MESSAGE.deserialize(text);
    }

    public static Component parsePrefix(String text) {
        String prefix = ModerationManager.getInstance().getConfigManager().getPrefix();
        return parse(prefix + text);
    }

    public static String applyPercentPlaceholders(String template, Map<String, String> placeholders) {
        if (template == null || template.isEmpty()) return "";
        String result = template;
        for (Map.Entry<String, String> e : placeholders.entrySet()) {
            String key = e.getKey();
            String value = e.getValue() != null ? e.getValue() : "";
            result = result.replace("%" + key + "%", value);
        }
        return result;
    }

    public static String applyPercentPlaceholders(String template, String... pairs) {
        if (template == null || template.isEmpty()) return "";
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            map.put(pairs[i], pairs[i + 1] != null ? pairs[i + 1] : "");
        }
        return applyPercentPlaceholders(template, map);
    }

    public static Component formatPunishment(String template, String reason, String operator, String id, String duration, String unbanDate) {
        String resolved = applyPercentPlaceholders(template,
                "reason", reason != null ? reason : "Kein Grund",
                "operator", operator != null ? operator : "System",
                "id", id != null ? id : "-",
                "duration", duration != null ? duration : "Permanent",
                "unban_date", unbanDate != null ? unbanDate : "Niemals",
                "unmute_date", unbanDate != null ? unbanDate : "Niemals",
                "player", "-",
                "message", "-"
        );
        TagResolver resolver = TagResolver.resolver(
                Placeholder.parsed("reason", reason != null ? reason : "Kein Grund"),
                Placeholder.parsed("operator", operator != null ? operator : "System"),
                Placeholder.parsed("id", id != null ? id : "-"),
                Placeholder.parsed("duration", duration != null ? duration : "Permanent"),
                Placeholder.parsed("unban_date", unbanDate != null ? unbanDate : "Niemals"),
                Placeholder.parsed("unmute_date", unbanDate != null ? unbanDate : "Niemals")
        );
        return MINI_MESSAGE.deserialize(resolved, resolver);
    }

    public static void sendMessage(CommandSender sender, String message) {
        sender.sendMessage(parsePrefix(message));
    }

    public static void sendRaw(CommandSender sender, String message) {
        sender.sendMessage(parse(message));
    }

    public static void kickPlayer(Player player, String screenTemplate, String reason, String operator, String id, String duration, String unbanDate) {
        Component component = formatPunishment(screenTemplate, reason, operator, id, duration, unbanDate);
        player.kick(component);
    }

    public static String toLegacy(Component component) {
        return LEGACY_SERIALIZER.serialize(component);
    }

    public static String formatDate(long timestamp) {
        if (timestamp <= 0) return "Permanent";
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        sdf.setTimeZone(TimeZone.getTimeZone("Europe/Berlin"));
        return sdf.format(new Date(timestamp));
    }

    public static String formatDate(Date date) {
        return formatDate(date.getTime());
    }

    public static String joinArgs(String[] args, int startIndex) {
        StringBuilder sb = new StringBuilder();
        for (int i = startIndex; i < args.length; i++) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(args[i]);
        }
        return sb.toString();
    }

    public static String joinStringList(List<String> list, String separator) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(separator);
            sb.append(list.get(i));
        }
        return sb.toString();
    }

    public static String gradient(String text, String startColor, String endColor) {
        return "<gradient:" + startColor + ":" + endColor + ">" + text + "</gradient>";
    }

    public static String boldGradient(String text, String startColor, String endColor) {
        return "<gradient:" + startColor + ":" + endColor + "><bold>" + text + "</bold></gradient>";
    }
}
