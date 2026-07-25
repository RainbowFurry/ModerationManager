package net.rainbowfurry.moderationManager;

import net.rainbowfurry.moderationManager.commands.CommandManager;
import net.rainbowfurry.moderationManager.listeners.ChatListener;
import net.rainbowfurry.moderationManager.listeners.ConnectionListener;
import net.rainbowfurry.moderationManager.listeners.MenuInventoryListener;
import net.rainbowfurry.moderationManager.listeners.VanishListener;
import net.rainbowfurry.moderationManager.managers.*;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class ModerationManager extends JavaPlugin {

    private static ModerationManager instance;

    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private PunishmentManager punishmentManager;
    private AltAccountManager altAccountManager;
    private ChatManager chatManager;
    private ProtectionManager protectionManager;
    private StaffManager staffManager;
    private CommandManager commandManager;
    private ConnectionListener connectionListener;
    private GuiManager guiManager;
    private AutoModManager autoModManager;

    @Override
    public void onEnable() {
        instance = this;

        long start = System.currentTimeMillis();

        // 1. Config laden
        this.configManager = new ConfigManager(this);

        // 2. Datenbank verbinden
        this.databaseManager = new DatabaseManager(this);
        this.databaseManager.connect();

        // 3. Manager initialisieren
        this.guiManager = new GuiManager();
        this.autoModManager = new AutoModManager(this);
        this.punishmentManager = new PunishmentManager(this);
        this.altAccountManager = new AltAccountManager(this);
        this.chatManager = new ChatManager(this);
        this.protectionManager = new ProtectionManager(this);
        this.staffManager = new StaffManager(this);
        this.commandManager = new CommandManager(this);

        // 4. Events registrieren
        PluginManager pm = Bukkit.getPluginManager();
        this.connectionListener = new ConnectionListener(this);
        pm.registerEvents(this.connectionListener, this);
        pm.registerEvents(new ChatListener(this), this);
        pm.registerEvents(new VanishListener(this), this);
        pm.registerEvents(new MenuInventoryListener(this), this);

        // 5. Commands registrieren
        this.commandManager.registerAll();

        // 6. bStats
        if (configManager.isBstats()) {
            try {
                new Metrics(this, 23045); // Dummy ID, bitte bei Release ersetzen
            } catch (Exception ignored) {}
        }

        // 7. Sicherstellen dass bei allen online Spielern OP Abuse gecheckt wird (für /reload)
        Bukkit.getScheduler().runTask(this, () -> {
            for (var p : Bukkit.getOnlinePlayers()) {
                protectionManager.checkAndFixOpAbuse(p);
                staffManager.applyVanishForNewPlayer(p);
            }
        });

        long time = System.currentTimeMillis() - start;
        getLogger().info("================================================");
        getLogger().info("  ModerationManager v" + getDescription().getVersion() + " erfolgreich geladen!");
        getLogger().info("  Ladezeit: " + time + "ms");
        getLogger().info("  © RainbowFurry - rainbowfurry.com");
        getLogger().info("================================================");
    }

    @Override
    public void onDisable() {
        // Alle offenen GUIs schließen
        if (guiManager != null) guiManager.closeAll();
        // Spielzeiten final speichern
        if (databaseManager != null) {
            for (var p : Bukkit.getOnlinePlayers()) {
                var profile = databaseManager.getPlayerProfile(p.getUniqueId());
                if (profile != null) {
                    long now = System.currentTimeMillis();
                    profile.setLastLogout(now);
                    long session = now - profile.getLastLogin();
                    if (session > 0) profile.addPlaytime(session);
                    databaseManager.savePlayerProfile(profile);
                }
            }
            databaseManager.disconnect();
        }

        getLogger().info("ModerationManager deaktiviert. Auf Wiedersehen!");
    }

    // ====== Getter ======
    public static ModerationManager getInstance() { return instance; }

    public ConfigManager getConfigManager() { return configManager; }
    public DatabaseManager getDatabaseManager() { return databaseManager; }
    public PunishmentManager getPunishmentManager() { return punishmentManager; }
    public AltAccountManager getAltAccountManager() { return altAccountManager; }
    public ChatManager getChatManager() { return chatManager; }
    public ProtectionManager getProtectionManager() { return protectionManager; }
    public StaffManager getStaffManager() { return staffManager; }
    public CommandManager getCommandManager() { return commandManager; }
    public ConnectionListener getConnectionListener() { return connectionListener; }
    public GuiManager getGuiManager() { return guiManager; }
    public AutoModManager getAutoModManager() { return autoModManager; }
}
