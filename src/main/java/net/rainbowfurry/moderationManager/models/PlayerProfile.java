package net.rainbowfurry.moderationManager.models;

import java.util.UUID;

public class PlayerProfile {

    private final UUID uuid;
    private String playerName;
    private String currentIp;
    private long firstJoin;
    private long lastLogin;
    private long lastLogout;
    private long playtimeMillis;
    private int totalBans;
    private int totalMutes;
    private int totalWarns;
    private int totalKicks;

    public PlayerProfile(UUID uuid, String playerName) {
        this.uuid = uuid;
        this.playerName = playerName;
        this.firstJoin = System.currentTimeMillis();
        this.lastLogin = System.currentTimeMillis();
    }

    public PlayerProfile(UUID uuid, String playerName, String currentIp, long firstJoin, long lastLogin,
                         long lastLogout, long playtimeMillis, int totalBans, int totalMutes, int totalWarns, int totalKicks) {
        this.uuid = uuid;
        this.playerName = playerName;
        this.currentIp = currentIp;
        this.firstJoin = firstJoin;
        this.lastLogin = lastLogin;
        this.lastLogout = lastLogout;
        this.playtimeMillis = playtimeMillis;
        this.totalBans = totalBans;
        this.totalMutes = totalMutes;
        this.totalWarns = totalWarns;
        this.totalKicks = totalKicks;
    }

    public UUID getUuid() { return uuid; }
    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }
    public String getCurrentIp() { return currentIp; }
    public void setCurrentIp(String currentIp) { this.currentIp = currentIp; }
    public long getFirstJoin() { return firstJoin; }
    public void setFirstJoin(long firstJoin) { this.firstJoin = firstJoin; }
    public long getLastLogin() { return lastLogin; }
    public void setLastLogin(long lastLogin) { this.lastLogin = lastLogin; }
    public long getLastLogout() { return lastLogout; }
    public void setLastLogout(long lastLogout) { this.lastLogout = lastLogout; }
    public long getPlaytimeMillis() { return playtimeMillis; }
    public void setPlaytimeMillis(long playtimeMillis) { this.playtimeMillis = playtimeMillis; }
    public void addPlaytime(long millis) { this.playtimeMillis += millis; }
    public int getTotalBans() { return totalBans; }
    public void incTotalBans() { this.totalBans++; }
    public int getTotalMutes() { return totalMutes; }
    public void incTotalMutes() { this.totalMutes++; }
    public int getTotalWarns() { return totalWarns; }
    public void incTotalWarns() { this.totalWarns++; }
    public int getTotalKicks() { return totalKicks; }
    public void incTotalKicks() { this.totalKicks++; }
}
