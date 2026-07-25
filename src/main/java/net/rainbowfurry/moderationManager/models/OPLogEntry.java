package net.rainbowfurry.moderationManager.models;

import java.util.UUID;

public class OPLogEntry {

    private final long id;
    private final UUID targetUUID;
    private final String targetName;
    private final UUID operatorUUID;
    private final String operatorName;
    private final String action;
    private final String detail;
    private final long timestamp;

    public OPLogEntry(long id, UUID targetUUID, String targetName, UUID operatorUUID,
                      String operatorName, String action, String detail, long timestamp) {
        this.id = id;
        this.targetUUID = targetUUID;
        this.targetName = targetName;
        this.operatorUUID = operatorUUID;
        this.operatorName = operatorName;
        this.action = action;
        this.detail = detail;
        this.timestamp = timestamp;
    }

    public long getId() { return id; }
    public UUID getTargetUUID() { return targetUUID; }
    public String getTargetName() { return targetName; }
    public UUID getOperatorUUID() { return operatorUUID; }
    public String getOperatorName() { return operatorName; }
    public String getAction() { return action; }
    public String getDetail() { return detail; }
    public long getTimestamp() { return timestamp; }
}
