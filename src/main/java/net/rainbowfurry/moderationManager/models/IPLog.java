package net.rainbowfurry.moderationManager.models;

import java.util.UUID;

public class IPLog {

    private final UUID playerUUID;
    private final String playerName;
    private final String ip;
    private final long firstSeen;
    private final long lastSeen;
    private boolean isVpn;

    public IPLog(UUID playerUUID, String playerName, String ip, long firstSeen, long lastSeen, boolean isVpn) {
        this.playerUUID = playerUUID;
        this.playerName = playerName;
        this.ip = ip;
        this.firstSeen = firstSeen;
        this.lastSeen = lastSeen;
        this.isVpn = isVpn;
    }

    public UUID getPlayerUUID() { return playerUUID; }
    public String getPlayerName() { return playerName; }
    public String getIp() { return ip; }
    public long getFirstSeen() { return firstSeen; }
    public long getLastSeen() { return lastSeen; }
    public boolean isVpn() { return isVpn; }
    public void setVpn(boolean vpn) { isVpn = vpn; }
}
