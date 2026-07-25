package net.rainbowfurry.moderationManager.managers;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.rainbowfurry.moderationManager.ModerationManager;
import net.rainbowfurry.moderationManager.models.IPLog;
import net.rainbowfurry.moderationManager.models.OPLogEntry;
import net.rainbowfurry.moderationManager.models.PlayerProfile;
import net.rainbowfurry.moderationManager.models.Punishment;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class DatabaseManager {

    private final ModerationManager plugin;
    private HikariDataSource dataSource;

    public DatabaseManager(ModerationManager plugin) {
        this.plugin = plugin;
    }

    public void connect() {
        try {
            Class.forName("org.sqlite.JDBC");
            File dbFile = new File(plugin.getDataFolder(), plugin.getConfigManager().getDatabaseFilename());
            if (!dbFile.getParentFile().exists()) dbFile.getParentFile().mkdirs();
            if (!dbFile.exists()) dbFile.createNewFile();

            HikariConfig config = new HikariConfig();
            config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
            config.setMaximumPoolSize(10);
            config.setConnectionTimeout(30_000);
            config.setIdleTimeout(600_000);
            config.setMaxLifetime(1_800_000);
            dataSource = new HikariDataSource(config);

            createTables();
            plugin.getLogger().info("Datenbankverbindung hergestellt.");
        } catch (Exception e) {
            plugin.getLogger().severe("Datenbankverbindung fehlgeschlagen: " + e.getMessage());
        }
    }

    private Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    private void createTables() throws SQLException {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS players (
                    uuid TEXT PRIMARY KEY,
                    player_name TEXT NOT NULL,
                    current_ip TEXT,
                    first_join INTEGER NOT NULL,
                    last_login INTEGER,
                    last_logout INTEGER,
                    playtime_millis INTEGER DEFAULT 0,
                    total_bans INTEGER DEFAULT 0,
                    total_mutes INTEGER DEFAULT 0,
                    total_warns INTEGER DEFAULT 0,
                    total_kicks INTEGER DEFAULT 0
                )
            """);
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS punishments (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    type TEXT NOT NULL,
                    target_uuid TEXT NOT NULL,
                    target_name TEXT,
                    operator_uuid TEXT,
                    operator_name TEXT,
                    reason TEXT,
                    created_at INTEGER NOT NULL,
                    end_at INTEGER DEFAULT 0,
                    active INTEGER DEFAULT 1,
                    server TEXT
                )
            """);
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS ip_logs (
                    player_uuid TEXT NOT NULL,
                    player_name TEXT,
                    ip TEXT NOT NULL,
                    first_seen INTEGER NOT NULL,
                    last_seen INTEGER NOT NULL,
                    is_vpn INTEGER DEFAULT 0,
                    PRIMARY KEY (player_uuid, ip)
                )
            """);
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS op_logs (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    target_uuid TEXT,
                    target_name TEXT,
                    operator_uuid TEXT,
                    operator_name TEXT,
                    action TEXT,
                    detail TEXT,
                    timestamp INTEGER NOT NULL
                )
            """);
        }
    }

    public void disconnect() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    // ============ PLAYER PROFILE ============

    public void savePlayerProfile(PlayerProfile profile) {
        CompletableFuture.runAsync(() -> {
            try (Connection conn = getConnection()) {
                PreparedStatement ps = conn.prepareStatement("""
                    INSERT INTO players(uuid, player_name, current_ip, first_join, last_login, last_logout,
                     playtime_millis, total_bans, total_mutes, total_warns, total_kicks)
                    VALUES(?,?,?,?,?,?,?,?,?,?,?)
                    ON CONFLICT(uuid) DO UPDATE SET
                        player_name=excluded.player_name,
                        current_ip=excluded.current_ip,
                        last_login=excluded.last_login,
                        last_logout=excluded.last_logout,
                        playtime_millis=excluded.playtime_millis,
                        total_bans=excluded.total_bans,
                        total_mutes=excluded.total_mutes,
                        total_warns=excluded.total_warns,
                        total_kicks=excluded.total_kicks
                """);
                ps.setString(1, profile.getUuid().toString());
                ps.setString(2, profile.getPlayerName());
                ps.setString(3, profile.getCurrentIp());
                ps.setLong(4, profile.getFirstJoin());
                ps.setLong(5, profile.getLastLogin());
                ps.setLong(6, profile.getLastLogout());
                ps.setLong(7, profile.getPlaytimeMillis());
                ps.setInt(8, profile.getTotalBans());
                ps.setInt(9, profile.getTotalMutes());
                ps.setInt(10, profile.getTotalWarns());
                ps.setInt(11, profile.getTotalKicks());
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Error saving player: " + e.getMessage());
            }
        });
    }

    public PlayerProfile getPlayerProfile(UUID uuid) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM players WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new PlayerProfile(
                        UUID.fromString(rs.getString("uuid")),
                        rs.getString("player_name"),
                        rs.getString("current_ip"),
                        rs.getLong("first_join"),
                        rs.getLong("last_login"),
                        rs.getLong("last_logout"),
                        rs.getLong("playtime_millis"),
                        rs.getInt("total_bans"),
                        rs.getInt("total_mutes"),
                        rs.getInt("total_warns"),
                        rs.getInt("total_kicks")
                );
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error loading player: " + e.getMessage());
        }
        return null;
    }

    public PlayerProfile getPlayerProfileByName(String name) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM players WHERE player_name = ? COLLATE NOCASE LIMIT 1")) {
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new PlayerProfile(
                        UUID.fromString(rs.getString("uuid")),
                        rs.getString("player_name"),
                        rs.getString("current_ip"),
                        rs.getLong("first_join"),
                        rs.getLong("last_login"),
                        rs.getLong("last_logout"),
                        rs.getLong("playtime_millis"),
                        rs.getInt("total_bans"),
                        rs.getInt("total_mutes"),
                        rs.getInt("total_warns"),
                        rs.getInt("total_kicks")
                );
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error loading player by name: " + e.getMessage());
        }
        return null;
    }

    // ============ PUNISHMENTS ============

    public long savePunishment(Punishment p) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO punishments(type, target_uuid, target_name, operator_uuid, operator_name, reason, created_at, end_at, active, server)
                VALUES(?,?,?,?,?,?,?,?,?,?)
             """, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.getType().name());
            ps.setString(2, p.getTargetUUID().toString());
            ps.setString(3, p.getTargetName());
            ps.setString(4, p.getOperatorUUID() != null ? p.getOperatorUUID().toString() : null);
            ps.setString(5, p.getOperatorName());
            ps.setString(6, p.getReason());
            ps.setLong(7, p.getCreatedAt());
            ps.setLong(8, p.getEndAt());
            ps.setInt(9, p.isActive() ? 1 : 0);
            ps.setString(10, p.getServer());
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) return keys.getLong(1);
        } catch (SQLException e) {
            plugin.getLogger().severe("Error saving punishment: " + e.getMessage());
        }
        return -1;
    }

    public void updatePunishment(Punishment p) {
        CompletableFuture.runAsync(() -> {
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement("UPDATE punishments SET active = ? WHERE id = ?")) {
                ps.setInt(1, p.isActive() ? 1 : 0);
                ps.setLong(2, p.getId());
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Error updating punishment: " + e.getMessage());
            }
        });
    }

    public Punishment getActiveBan(UUID uuid) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("""
                SELECT * FROM punishments
                WHERE target_uuid = ? AND (type = 'BAN' OR type = 'TEMPBAN') AND active = 1
                ORDER BY created_at DESC LIMIT 1
             """)) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            return resultSetToPunishment(rs);
        } catch (SQLException e) {
            plugin.getLogger().severe("Error getting active ban: " + e.getMessage());
        }
        return null;
    }

    public Punishment getActiveBanByName(String name) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("""
                SELECT * FROM punishments
                WHERE target_name = ? COLLATE NOCASE AND (type = 'BAN' OR type = 'TEMPBAN') AND active = 1
                ORDER BY created_at DESC LIMIT 1
             """)) {
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            return resultSetToPunishment(rs);
        } catch (SQLException e) {
            plugin.getLogger().severe("Error getting active ban by name: " + e.getMessage());
        }
        return null;
    }

    public Punishment getActiveMute(UUID uuid) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("""
                SELECT * FROM punishments
                WHERE target_uuid = ? AND (type = 'MUTE' OR type = 'TEMPMUTE') AND active = 1
                ORDER BY created_at DESC LIMIT 1
             """)) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            return resultSetToPunishment(rs);
        } catch (SQLException e) {
            plugin.getLogger().severe("Error getting active mute: " + e.getMessage());
        }
        return null;
    }

    public List<Punishment> getPunishmentHistory(UUID uuid, int limit) {
        List<Punishment> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("""
                SELECT * FROM punishments WHERE target_uuid = ? ORDER BY created_at DESC LIMIT ?
             """)) {
            ps.setString(1, uuid.toString());
            ps.setInt(2, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(resultSetToPunishmentFromRS(rs));
        } catch (SQLException e) {
            plugin.getLogger().severe("Error loading punishments: " + e.getMessage());
        }
        return list;
    }

    public void unbanAllFor(UUID targetUUID, String operatorName, UUID operatorUUID) {
        try (Connection conn = getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement("""
                UPDATE punishments SET active = 0
                WHERE target_uuid = ? AND (type = 'BAN' OR type = 'TEMPBAN') AND active = 1
             """)) {
                ps.setString(1, targetUUID.toString());
                ps.executeUpdate();
            }
            Punishment unban = new Punishment(-1, Punishment.Type.UNBAN, targetUUID, null,
                    operatorUUID, operatorName, "Entbannt", System.currentTimeMillis(), 0, false, null);
            savePunishment(unban);
        } catch (SQLException e) {
            plugin.getLogger().severe("Error unbanning: " + e.getMessage());
        }
    }

    public void unmuteAllFor(UUID targetUUID, String operatorName, UUID operatorUUID) {
        try (Connection conn = getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement("""
                UPDATE punishments SET active = 0
                WHERE target_uuid = ? AND (type = 'MUTE' OR type = 'TEMPMUTE') AND active = 1
             """)) {
                ps.setString(1, targetUUID.toString());
                ps.executeUpdate();
            }
            Punishment unmute = new Punishment(-1, Punishment.Type.UNMUTE, targetUUID, null,
                    operatorUUID, operatorName, "Entmutet", System.currentTimeMillis(), 0, false, null);
            savePunishment(unmute);
        } catch (SQLException e) {
            plugin.getLogger().severe("Error unmuting: " + e.getMessage());
        }
    }

    public int countActiveWarns(UUID uuid) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("""
                SELECT COUNT(*) FROM punishments WHERE target_uuid = ? AND type = 'WARN' AND active = 1
             """)) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            plugin.getLogger().severe("Error counting warns: " + e.getMessage());
        }
        return 0;
    }

    public boolean deactivatePunishmentById(long id) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE punishments SET active = 0 WHERE id = ?")) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().severe("Error deactivating punishment: " + e.getMessage());
            return false;
        }
    }

    public Punishment getPunishmentById(long id) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM punishments WHERE id = ?")) {
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            return resultSetToPunishment(rs);
        } catch (SQLException e) {
            plugin.getLogger().severe("Error getting punishment by id: " + e.getMessage());
            return null;
        }
    }

    public boolean deletePunishmentById(long id) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM punishments WHERE id = ?")) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().severe("Error deleting punishment: " + e.getMessage());
            return false;
        }
    }

    public Punishment resultSetToPunishment(ResultSet rs) throws SQLException {
        if (rs.next()) return resultSetToPunishmentFromRS(rs);
        return null;
    }

    private Punishment resultSetToPunishmentFromRS(ResultSet rs) throws SQLException {
        return new Punishment(
                rs.getLong("id"),
                Punishment.Type.valueOf(rs.getString("type")),
                UUID.fromString(rs.getString("target_uuid")),
                rs.getString("target_name"),
                rs.getString("operator_uuid") != null ? UUID.fromString(rs.getString("operator_uuid")) : null,
                rs.getString("operator_name"),
                rs.getString("reason"),
                rs.getLong("created_at"),
                rs.getLong("end_at"),
                rs.getInt("active") == 1,
                rs.getString("server")
        );
    }

    // ============ IP LOGS ============

    public void logIp(UUID playerUUID, String playerName, String ip, boolean isVpn) {
        CompletableFuture.runAsync(() -> {
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement("""
                    INSERT INTO ip_logs(player_uuid, player_name, ip, first_seen, last_seen, is_vpn)
                    VALUES(?,?,?,?,?,?)
                    ON CONFLICT(player_uuid, ip) DO UPDATE SET
                        last_seen=excluded.last_seen,
                        player_name=excluded.player_name,
                        is_vpn=excluded.is_vpn
                 """)) {
                long now = System.currentTimeMillis();
                ps.setString(1, playerUUID.toString());
                ps.setString(2, playerName);
                ps.setString(3, ip);
                ps.setLong(4, now);
                ps.setLong(5, now);
                ps.setInt(6, isVpn ? 1 : 0);
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Error logging IP: " + e.getMessage());
            }
        });
    }

    public List<IPLog> findPlayersByIp(String ip) {
        List<IPLog> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM ip_logs WHERE ip = ?")) {
            ps.setString(1, ip);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new IPLog(
                        UUID.fromString(rs.getString("player_uuid")),
                        rs.getString("player_name"),
                        rs.getString("ip"),
                        rs.getLong("first_seen"),
                        rs.getLong("last_seen"),
                        rs.getInt("is_vpn") == 1
                ));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error finding players by IP: " + e.getMessage());
        }
        return list;
    }

    public List<IPLog> findAllIpLogs() {
        List<IPLog> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT player_uuid, player_name, ip, MIN(first_seen) first_seen, MAX(last_seen) last_seen, MAX(is_vpn) is_vpn " +
                     "FROM ip_logs GROUP BY player_uuid, ip ORDER BY last_seen DESC LIMIT 10000")) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new IPLog(
                        UUID.fromString(rs.getString("player_uuid")),
                        rs.getString("player_name"),
                        rs.getString("ip"),
                        rs.getLong("first_seen"),
                        rs.getLong("last_seen"),
                        rs.getInt("is_vpn") == 1
                ));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error loading all IP logs: " + e.getMessage());
        }
        return list;
    }

    public List<IPLog> getPlayerIpLogs(UUID uuid) {
        List<IPLog> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM ip_logs WHERE player_uuid = ? ORDER BY last_seen DESC")) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new IPLog(
                        UUID.fromString(rs.getString("player_uuid")),
                        rs.getString("player_name"),
                        rs.getString("ip"),
                        rs.getLong("first_seen"),
                        rs.getLong("last_seen"),
                        rs.getInt("is_vpn") == 1
                ));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error loading IP logs: " + e.getMessage());
        }
        return list;
    }

    // ============ OP LOGS ============

    public void logOpAction(OPLogEntry entry) {
        CompletableFuture.runAsync(() -> {
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement("""
                    INSERT INTO op_logs(target_uuid, target_name, operator_uuid, operator_name, action, detail, timestamp)
                    VALUES(?,?,?,?,?,?,?)
                 """)) {
                ps.setString(1, entry.getTargetUUID() != null ? entry.getTargetUUID().toString() : null);
                ps.setString(2, entry.getTargetName());
                ps.setString(3, entry.getOperatorUUID() != null ? entry.getOperatorUUID().toString() : null);
                ps.setString(4, entry.getOperatorName());
                ps.setString(5, entry.getAction());
                ps.setString(6, entry.getDetail());
                ps.setLong(7, entry.getTimestamp());
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Error logging OP action: " + e.getMessage());
            }
        });
    }

    public List<OPLogEntry> getOpLogsFor(UUID targetUUID, int limit) {
        List<OPLogEntry> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("""
                SELECT * FROM op_logs WHERE target_uuid = ? ORDER BY timestamp DESC LIMIT ?
             """)) {
            ps.setString(1, targetUUID.toString());
            ps.setInt(2, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(rsToOpLog(rs));
        } catch (SQLException e) {
            plugin.getLogger().severe("Error loading OP logs: " + e.getMessage());
        }
        return list;
    }

    private OPLogEntry rsToOpLog(ResultSet rs) throws SQLException {
        return new OPLogEntry(
                rs.getLong("id"),
                rs.getString("target_uuid") != null ? UUID.fromString(rs.getString("target_uuid")) : null,
                rs.getString("target_name"),
                rs.getString("operator_uuid") != null ? UUID.fromString(rs.getString("operator_uuid")) : null,
                rs.getString("operator_name"),
                rs.getString("action"),
                rs.getString("detail"),
                rs.getLong("timestamp")
        );
    }
}
