package net.rainbowfurry.moderationManager.utils;

import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DurationUtils {

    private static final Pattern DURATION_PATTERN = Pattern.compile("(\\d+)(mo|[smhdw])", Pattern.CASE_INSENSITIVE);

    public static long parseDuration(String input) {
        if (input == null || input.isEmpty()) return -1;
        if ("perm".equalsIgnoreCase(input) || "permanent".equalsIgnoreCase(input) || "-1".equals(input)) return -1;

        long totalMillis = 0;
        Matcher matcher = DURATION_PATTERN.matcher(input);
        boolean found = false;

        while (matcher.find()) {
            found = true;
            long amount = Long.parseLong(matcher.group(1));
            String unit = matcher.group(2).toLowerCase();

            switch (unit) {
                case "mo" -> totalMillis += TimeUnit.DAYS.toMillis(amount * 30);
                case "w" -> totalMillis += TimeUnit.DAYS.toMillis(amount * 7);
                case "d" -> totalMillis += TimeUnit.DAYS.toMillis(amount);
                case "h" -> totalMillis += TimeUnit.HOURS.toMillis(amount);
                case "m" -> totalMillis += TimeUnit.MINUTES.toMillis(amount);
                case "s" -> totalMillis += TimeUnit.SECONDS.toMillis(amount);
            }
        }

        if (!found) return -1;
        return totalMillis;
    }

    public static String formatDuration(long millis) {
        if (millis <= 0) return "Permanent";

        long seconds = Math.abs(millis) / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        long weeks = days / 7;
        long months = days / 30;

        StringBuilder sb = new StringBuilder();

        if (months > 0) {
            sb.append(months).append(" Monat").append(months > 1 ? "e" : "").append(" ");
            days %= 30;
        }
        if (weeks > 0 && months == 0) {
            sb.append(weeks).append(" Woche").append(weeks > 1 ? "n" : "").append(" ");
            days %= 7;
        }
        if (days > 0) {
            sb.append(days).append(" Tag").append(days > 1 ? "e" : "").append(" ");
        }
        hours %= 24;
        if (hours > 0) {
            sb.append(hours).append(" Stunde").append(hours > 1 ? "n" : "").append(" ");
        }
        minutes %= 60;
        if (minutes > 0) {
            sb.append(minutes).append(" Minute").append(minutes > 1 ? "n" : "").append(" ");
        }
        seconds %= 60;
        if (seconds > 0 && sb.length() == 0) {
            sb.append(seconds).append(" Sekunde").append(seconds > 1 ? "n" : "");
        }

        return sb.toString().trim();
    }

    public static String formatRemaining(long endTimestamp) {
        if (endTimestamp <= 0) return "Permanent";
        long remaining = endTimestamp - System.currentTimeMillis();
        if (remaining <= 0) return "Abgelaufen";
        return formatDuration(remaining);
    }

    public static boolean isExpired(long endTimestamp) {
        return endTimestamp > 0 && System.currentTimeMillis() > endTimestamp;
    }
}
