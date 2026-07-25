package net.rainbowfurry.moderationManager.models;

import java.util.UUID;

public class Punishment {

    public enum Type { BAN, TEMPBAN, KICK, MUTE, TEMPMUTE, WARN, UNBAN, UNMUTE }

    private final long id;
    private final Type type;
    private final UUID targetUUID;
    private final String targetName;
    private final UUID operatorUUID;
    private final String operatorName;
    private final String reason;
    private final long createdAt;
    private final long endAt;
    private boolean active;
    private String server;

    public Punishment(long id, Type type, UUID targetUUID, String targetName, UUID operatorUUID,
                      String operatorName, String reason, long createdAt, long endAt, boolean active, String server) {
        this.id = id;
        this.type = type;
        this.targetUUID = targetUUID;
        this.targetName = targetName;
        this.operatorUUID = operatorUUID;
        this.operatorName = operatorName;
        this.reason = reason;
        this.createdAt = createdAt;
        this.endAt = endAt;
        this.active = active;
        this.server = server;
    }

    // Vereinfachter Konstruktor für GUI-Integration (Operator-Name wird als String übergeben)
    public Punishment(long id, UUID targetUUID, String targetName, Type type,
                      long dummy, String reason, String operatorName,
                      long createdAt, long endAt, boolean active) {
        this.id = id;
        this.type = type;
        this.targetUUID = targetUUID;
        this.targetName = targetName;
        this.operatorUUID = null;
        this.operatorName = operatorName;
        this.reason = reason;
        this.createdAt = createdAt;
        this.endAt = endAt;
        this.active = active;
        this.server = null;
    }

    public boolean isExpired() {
        return endAt > 0 && System.currentTimeMillis() > endAt;
    }

    public long getId() { return id; }
    public Type getType() { return type; }
    public UUID getTargetUUID() { return targetUUID; }
    public String getTargetName() { return targetName; }
    public UUID getOperatorUUID() { return operatorUUID; }
    public String getOperatorName() { return operatorName; }
    public String getReason() { return reason; }
    public long getCreatedAt() { return createdAt; }
    public long getStart() { return createdAt; }
    public long getEndAt() { return endAt; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public String getServer() { return server; }

    public String getTypeDisplayName() {
        return switch (type) {
            case BAN, TEMPBAN -> "Ban";
            case KICK -> "Kick";
            case MUTE, TEMPMUTE -> "Mute";
            case WARN -> "Warn";
            case UNBAN -> "Unban";
            case UNMUTE -> "Unmute";
        };
    }
}
