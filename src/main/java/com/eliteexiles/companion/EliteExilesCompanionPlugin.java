package com.eliteexiles.companion;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.swing.SwingUtilities;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.clan.ClanChannel;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.StatChanged;
import net.runelite.client.chat.ChatCommandManager;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.events.ChatInput;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

@PluginDescriptor(
    name = "Elite Exiles Companion",
    description = "Exile HQ: local session tracking plus optional Elite Exiles Coach Chat, goal planning, EE progression, Pet Hunt status and clan events.",
    tags = {"clan", "progression", "goals", "discord", "coach", "stats", "qol", "planner"}
)
public class EliteExilesCompanionPlugin extends Plugin
{
    @Inject private Client client;
    @Inject private ClientToolbar clientToolbar;
    @Inject private ChatCommandManager chatCommandManager;
    @Inject private EliteExilesBridgeClient bridge;
    @Inject private EliteExilesCompanionConfig config;
    @Inject private ConfigManager configManager;
    @Inject private ScheduledExecutorService scheduler;

    private EliteExilesPanel panel;
    private NavigationButton navButton;
    private ScheduledFuture<?> backgroundTask;
    private final Map<String, LiveSkill> liveSkills = new ConcurrentHashMap<>();
    private volatile String currentRsn;
    private volatile long sessionStartedAt;
    private volatile long sessionStartXp;
    private volatile long currentTotalXp;
    private volatile boolean pendingLoginInit;
    private volatile long lastDashboardPull;
    private volatile long lastLivePush;
    private volatile long lastUiRefresh;
    private volatile long lastMembershipSync;
    private volatile String lastClanName = "";
    private volatile String lastGuestClanName = "";
    private volatile int bridgeProtocol;
    private static final String COACH_COMMAND = "!coach";
    private static final String ASK_COACH_COMMAND = "!askcoach";
    private static final long MEMBERSHIP_HEARTBEAT_MS = 5 * 60_000L;

    @Provides
    EliteExilesCompanionConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(EliteExilesCompanionConfig.class);
    }

    @Override
    protected void startUp()
    {
        migrateBridgeUrl();
        panel = new EliteExilesPanel();
        panel.setController(this);
        final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "icon.png");
        navButton = NavigationButton.builder()
            .tooltip("Elite Exiles • Exile HQ")
            .icon(icon)
            .priority(8)
            .panel(panel)
            .build();
        clientToolbar.addNavigation(navButton);
        chatCommandManager.registerCommand(COACH_COMMAND, (message, value) -> { }, this::coachCommandInput);
        chatCommandManager.registerCommand(ASK_COACH_COMMAND, (message, value) -> { }, this::coachCommandInput);

        bridgeProtocol = 0;
        if (coachEnabled())
        {
            if (bridge.isLinked())
            {
                panel.setBusy("Connecting Exile HQ…");
                pullDashboard();
            }
            else panel.setDisconnected("Coach Integration is enabled. Join Discord, run /runelitelink, then paste the code above.");
        }
        else panel.setLocalMode();

        pendingLoginInit = client.getGameState() == GameState.LOGGED_IN;
        // Fifteen-second housekeeping is enough because login/stat events update immediately.
        // This keeps idle CPU/network work low while preserving a current session clock.
        backgroundTask = scheduler.scheduleAtFixedRate(this::backgroundTick, 5, 15, TimeUnit.SECONDS);
    }

    private void migrateBridgeUrl()
    {
        String saved = configManager.getConfiguration(EliteExilesCompanionConfig.GROUP, "bridgeUrl");
        if (shouldMigrateBridgeUrl(saved))
            configManager.setConfiguration(EliteExilesCompanionConfig.GROUP, "bridgeUrl", EliteExilesCompanionConfig.PRODUCTION_BRIDGE_URL);
    }

    static boolean shouldMigrateBridgeUrl(String value) { return !isValidHttpsBridgeOrigin(value); }

    static boolean isValidHttpsBridgeOrigin(String value)
    {
        if (value == null || value.isBlank()) return false;
        try
        {
            String trimmed = value.trim();
            while (trimmed.endsWith("/")) trimmed = trimmed.substring(0, trimmed.length() - 1);
            URI uri = URI.create(trimmed);
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
            String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
            if (!"https".equals(scheme) || host.isBlank()) return false;
            if (uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null) return false;
            String path = uri.getPath();
            if (path != null && !path.isBlank() && !"/".equals(path)) return false;
            return uri.getPort() == -1 || uri.getPort() == 443;
        }
        catch (IllegalArgumentException ex) { return false; }
    }

    @Override
    protected void shutDown()
    {
        if (backgroundTask != null) { backgroundTask.cancel(true); backgroundTask = null; }
        chatCommandManager.unregisterCommand(COACH_COMMAND);
        chatCommandManager.unregisterCommand(ASK_COACH_COMMAND);
        if (navButton != null) { clientToolbar.removeNavigation(navButton); navButton = null; }
        liveSkills.clear();
        panel = null;
        currentRsn = null;
        lastClanName = "";
        lastGuestClanName = "";
        lastMembershipSync = 0L;
        bridgeProtocol = 0;
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        if (event.getGameState() == GameState.LOGGED_IN) pendingLoginInit = true;
        else if (event.getGameState() == GameState.LOGIN_SCREEN) pendingLoginInit = false;
    }

    @Subscribe
    public void onGameTick(GameTick event)
    {
        if (!pendingLoginInit) return;
        Player local = client.getLocalPlayer();
        if (local == null || local.getName() == null || local.getName().isBlank()) return;
        String name = local.getName();
        boolean changedAccount = currentRsn == null || !normalize(currentRsn).equals(normalize(name));
        currentRsn = name;
        currentTotalXp = client.getOverallExperience();
        if (changedAccount || sessionStartedAt == 0L)
        {
            sessionStartedAt = System.currentTimeMillis();
            sessionStartXp = currentTotalXp;
            liveSkills.clear();
        }
        for (Skill skill : Skill.values())
        {
            try { liveSkills.put(skillLabel(skill), new LiveSkill(client.getRealSkillLevel(skill), client.getSkillExperience(skill))); }
            catch (Exception ignored) { }
        }
        pendingLoginInit = false;
        if (panel != null)
        {
            lastUiRefresh = System.currentTimeMillis();
            panel.updateLiveSnapshot(currentRsn, Math.max(0L, currentTotalXp - sessionStartXp), snapshotLevels());
        }
        if (coachEnabled() && bridge.isLinked())
        {
            maybeSyncMembership(true);
            pushLive();
            pullDashboard();
        }
    }

    @Subscribe
    public void onStatChanged(StatChanged event)
    {
        if (event.getSkill() == null) return;
        liveSkills.put(skillLabel(event.getSkill()), new LiveSkill(event.getLevel(), event.getXp()));
        try { currentTotalXp = client.getOverallExperience(); } catch (Exception ignored) { }
        long now = System.currentTimeMillis();
        if (panel != null && now - lastUiRefresh >= 1_000L)
        {
            lastUiRefresh = now;
            panel.updateLiveSnapshot(currentRsn, Math.max(0L, currentTotalXp - sessionStartXp), snapshotLevels());
        }
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event)
    {
        if (!EliteExilesCompanionConfig.GROUP.equals(event.getGroup()) || panel == null) return;
        if (!coachEnabled()) { bridgeProtocol = 0; panel.setLocalMode(); return; }
        if (bridge.isLinked()) { bridgeProtocol = 0; panel.setBusy("Connecting Exile HQ…"); pullDashboard(); }
        else panel.setDisconnected("Coach Integration is enabled. Join Discord, run /runelitelink, then paste the code above.");
    }

    void linkFromPanel(String code)
    {
        if (!coachEnabled()) { panel.setLocalMode(); return; }
        if (currentRsn == null || currentRsn.isBlank()) { panel.setError("Log into the OSRS account you registered in Discord first."); return; }
        String cleanedCode = code == null ? "" : code.toUpperCase().replaceAll("[^A-HJ-NP-Z2-9]", "");
        if (cleanedCode.length() != 8) { panel.setError("That link code is not 8 valid characters. Run /runelitelink and paste the fresh code."); return; }
        panel.setBusy("Checking Elite Exiles bridge…");
        bridge.health(health -> {
            if (!isExpectedHealth(health)) { handleBridgeError("Bridge health response did not match the Elite Exiles protocol."); return; }
            updateBridgeProtocol(health);
            bridge.link(cleanedCode, currentRsn, response -> {
                updateBridgeProtocol(response);
                if (panel != null) panel.updateDashboard(response);
                maybeSyncMembership(true);
                pushLive();
            }, this::handleBridgeError);
        }, this::handleBridgeError);
    }

    void refreshCoachFromPanel()
    {
        if (!coachEnabled()) { panel.setLocalMode(); return; }
        if (!bridge.isLinked()) { panel.setDisconnected("Use /runelitelink in Discord to connect."); return; }
        if (!supportsExileHqProtocol()) { panel.setError(protocolUpgradeMessage()); return; }
        panel.setBusy("Refreshing Jagex + Coach…");
        bridge.refreshCoach(response -> { if (panel != null) panel.updateDashboard(response); }, this::handleBridgeError);
    }

    void askCoachFromPanel(String question, String subject)
    {
        if (!coachEnabled()) { if (panel != null) panel.showCoachError("Enable Coach Integration in plugin settings first."); return; }
        if (!bridge.isLinked()) { if (panel != null) panel.showCoachError("Link the Discord Coach first with /runelitelink."); return; }
        if (!supportsExileHqProtocol()) { if (panel != null) panel.showCoachError(protocolUpgradeMessage()); return; }
        bridge.askCoach(question, subject,
            response -> { if (panel != null) panel.showCoachAnswer(question, response); },
            message -> { if (panel != null) panel.showCoachError(message); });
    }

    void requestClanAccessFromPanel()
    {
        if (!coachEnabled()) { if (panel != null) panel.setLocalMode(); return; }
        if (!bridge.isLinked()) { if (panel != null) panel.setDisconnected("Link Discord first so the request is attached to the correct member and RSN."); return; }
        if (!supportsExileHqProtocol()) { if (panel != null) panel.setError(protocolUpgradeMessage()); return; }
        if (panel != null) panel.setBusy("Refreshing Elite Exiles membership…");
        syncMembershipNow(response -> {
            if (panel != null) panel.updateDashboard(response);
            bridge.requestClanAccess(request -> { if (panel != null) panel.updateDashboard(request); }, this::handleBridgeError);
        }, this::handleBridgeError);
    }

    private boolean coachCommandInput(ChatInput input, String value)
    {
        String raw = value == null ? "" : value.trim();
        int split = raw.indexOf(' ');
        String question = split < 0 ? "" : raw.substring(split + 1).trim();
        if (question.length() > 400) question = question.substring(0, 400);
        final String safeQuestion = question;
        SwingUtilities.invokeLater(() -> {
            if (navButton != null) clientToolbar.openPanel(navButton);
            if (panel == null) return;
            panel.openCoachPage();
            if (safeQuestion.isBlank()) panel.showCoachCommandHelp();
            else panel.askCoachFromCommand(safeQuestion);
        });
        // RuneLite's ChatCommandManager consumes the input when this callback returns true,
        // so !coach questions stay local and are never posted to the game chat channel.
        return true;
    }

    void checkInFromPanel()
    {
        if (!coachEnabled()) { panel.setLocalMode(); return; }
        if (!bridge.isLinked()) { panel.setDisconnected("Use /runelitelink in Discord to connect."); return; }
        if (!supportsExileHqProtocol()) { panel.setError(protocolUpgradeMessage()); return; }
        panel.setBusy("Verifying goals + missions…");
        pushLive();
        bridge.checkIn(response -> { if (panel != null) panel.showCheckinResult(response); }, this::handleBridgeError);
    }

    void runDiagnosticsFromPanel()
    {
        if (!coachEnabled()) { panel.setLocalMode(); return; }
        if (!bridge.isLinked()) { panel.setDisconnected("Link the Discord Coach first."); return; }
        if (currentRsn == null || currentRsn.isBlank()) { panel.setError("Log into the linked OSRS account before running diagnostics."); return; }
        final long started = System.nanoTime();
        final String nonce = "ee-" + Long.toUnsignedString(System.nanoTime(), 36);
        panel.setBusy("Running safe diagnostics…");
        bridge.health(health -> {
            if (!isExpectedHealth(health)) { handleBridgeError("Bridge health response did not match the Elite Exiles protocol."); return; }
            bridge.diagnostics(diagnostic -> {
                if (!isSafeDiagnosticResponse(diagnostic)) { handleBridgeError("Authenticated diagnostics failed the no-mutation safety contract."); return; }
                if (!normalize(currentRsn).equals(normalize(stringValue(diagnostic, "rsn")))) { handleBridgeError("Diagnostic RSN did not match the logged-in RuneLite account."); return; }
                bridge.diagnosticsEcho(nonce, echo -> {
                    if (!isSafeDiagnosticResponse(echo) || !nonce.equals(stringValue(echo, "echo"))) { handleBridgeError("Diagnostic POST/JSON echo failed."); return; }
                    long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
                    if (panel != null) panel.showDiagnosticsResult(stringValue(diagnostic, "bridgeId"), intValue(diagnostic, "protocol", 0), elapsedMs);
                }, this::handleBridgeError);
            }, this::handleBridgeError);
        }, this::handleBridgeError);
    }

    void unlinkFromPanel()
    {
        if (!coachEnabled()) { panel.setLocalMode(); return; }
        if (!bridge.isLinked()) { panel.setDisconnected("Already unlinked."); return; }
        panel.setBusy("Revoking Companion link…");
        bridge.unlink(response -> { bridgeProtocol = 0; if (panel != null) panel.setDisconnected("Link revoked. Use /runelitelink to reconnect."); }, this::handleBridgeError);
    }

    private void backgroundTick()
    {
        if (panel == null) return;
        long now = System.currentTimeMillis();
        // Keep the live duration feeling current without rebuilding Swing cards every scheduler tick.
        // StatChanged/login events still update immediately; this is only a low-frequency idle refresh.
        if (now - lastUiRefresh >= 15_000L)
        {
            lastUiRefresh = now;
            panel.updateLiveSnapshot(currentRsn, Math.max(0L, currentTotalXp - sessionStartXp), snapshotLevels());
        }
        if (!coachEnabled() || !bridge.isLinked()) return;
        int refreshSeconds = Math.max(30, config.refreshSeconds());
        maybeSyncMembership(false);
        if (config.autoSync() && currentRsn != null && now - lastLivePush >= 30_000L) pushLive();
        if (now - lastDashboardPull >= refreshSeconds * 1000L) pullDashboard();
    }

    private void maybeSyncMembership(boolean force)
    {
        if (!coachEnabled() || !bridge.isLinked() || currentRsn == null || currentRsn.isBlank()) return;
        if (!supportsExileHqProtocol()) return;
        String clanName = currentClanName(false);
        String guestClanName = currentClanName(true);
        long now = System.currentTimeMillis();
        boolean changed = !normalize(clanName).equals(normalize(lastClanName))
            || !normalize(guestClanName).equals(normalize(lastGuestClanName));
        if (!force && !changed && now - lastMembershipSync < MEMBERSHIP_HEARTBEAT_MS) return;
        lastMembershipSync = now;
        lastClanName = clanName;
        lastGuestClanName = guestClanName;
        bridge.syncMembership(currentRsn, clanName, guestClanName,
            response -> { if (panel != null) panel.updateDashboard(response); },
            message -> { if (panel != null && changed) panel.setError(message); });
    }

    private void syncMembershipNow(java.util.function.Consumer<JsonObject> success, java.util.function.Consumer<String> failure)
    {
        if (!supportsExileHqProtocol()) { failure.accept(protocolUpgradeMessage()); return; }
        if (currentRsn == null || currentRsn.isBlank()) { failure.accept("Log into the registered OSRS account first."); return; }
        String clanName = currentClanName(false);
        String guestClanName = currentClanName(true);
        lastMembershipSync = System.currentTimeMillis();
        lastClanName = clanName;
        lastGuestClanName = guestClanName;
        bridge.syncMembership(currentRsn, clanName, guestClanName, success, failure);
    }

    private String currentClanName(boolean guest)
    {
        try
        {
            ClanChannel channel = guest ? client.getGuestClanChannel() : client.getClanChannel();
            return channel == null || channel.getName() == null ? "" : channel.getName().trim();
        }
        catch (Exception ignored) { return ""; }
    }

    private void pullDashboard()
    {
        if (!coachEnabled() || !bridge.isLinked()) return;
        lastDashboardPull = System.currentTimeMillis();
        bridge.getDashboard(response -> { updateBridgeProtocol(response); if (panel != null) panel.updateDashboard(response); }, this::handleBridgeError);
    }

    private void pushLive()
    {
        if (!coachEnabled() || !config.autoSync() || !bridge.isLinked() || currentRsn == null || currentRsn.isBlank()) return;
        lastLivePush = System.currentTimeMillis();
        JsonObject body = new JsonObject();
        body.addProperty("rsn", currentRsn);
        body.addProperty("sessionStartedAt", sessionStartedAt);
        body.addProperty("totalXp", currentTotalXp);
        body.addProperty("sessionXp", Math.max(0L, currentTotalXp - sessionStartXp));
        JsonArray skills = new JsonArray();
        for (Map.Entry<String, LiveSkill> entry : liveSkills.entrySet())
        {
            JsonObject row = new JsonObject();
            row.addProperty("name", entry.getKey());
            row.addProperty("level", entry.getValue().level);
            row.addProperty("xp", entry.getValue().xp);
            skills.add(row);
        }
        body.add("skills", skills);
        bridge.sendLive(body, ignored -> { }, this::handleBridgeError);
    }

    private Map<String, Integer> snapshotLevels()
    {
        Map<String, Integer> out = new LinkedHashMap<>();
        for (Map.Entry<String, LiveSkill> entry : liveSkills.entrySet()) out.put(entry.getKey(), entry.getValue().level);
        return out;
    }

    private void updateBridgeProtocol(JsonObject response)
    {
        if (response == null) return;
        int protocol = intValue(response, "protocol", 0);
        if (protocol <= 0 && response.has("dashboard") && response.get("dashboard").isJsonObject())
            protocol = intValue(response.getAsJsonObject("dashboard"), "protocol", 0);
        if (protocol > 0) bridgeProtocol = protocol;
    }

    private boolean supportsExileHqProtocol()
    {
        return bridgeProtocol >= 5;
    }

    private String protocolUpgradeMessage()
    {
        return bridgeProtocol > 0
            ? "Exile HQ features need bridge protocol 5; the connected bridge is protocol " + bridgeProtocol + ". Refresh again after the server update."
            : "Exile HQ is still confirming the Coach bridge version. Wait a moment and refresh before using this feature.";
    }

    private void handleBridgeError(String message)
    {
        if (panel == null) return;
        if (!coachEnabled()) { panel.setLocalMode(); return; }
        if (!bridge.isLinked()) panel.setDisconnected(message + " Generate a new code with /runelitelink.");
        else panel.setError(message);
    }

    private boolean coachEnabled() { return config != null && config.coachIntegration(); }

    private static boolean isExpectedHealth(JsonObject response)
    {
        return response != null && booleanValue(response, "ok", false)
            && "elite-exiles-runelite-bridge".equals(stringValue(response, "service"))
            && intValue(response, "protocol", 0) >= 4;
    }

    private static boolean isSafeDiagnosticResponse(JsonObject response)
    {
        return response != null && booleanValue(response, "ok", false)
            && booleanValue(response, "authenticated", false)
            && booleanValue(response, "readOnly", false)
            && !booleanValue(response, "stateMutation", true)
            && "elite-exiles-runelite-bridge".equals(stringValue(response, "service"))
            && intValue(response, "protocol", 0) >= 4;
    }

    private static boolean booleanValue(JsonObject object, String key, boolean fallback)
    {
        try { return object != null && object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsBoolean() : fallback; }
        catch (Exception ignored) { return fallback; }
    }

    private static String stringValue(JsonObject object, String key)
    {
        try { return object != null && object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsString() : ""; }
        catch (Exception ignored) { return ""; }
    }

    private static int intValue(JsonObject object, String key, int fallback)
    {
        try { return object != null && object.has(key) ? object.get(key).getAsInt() : fallback; }
        catch (Exception ignored) { return fallback; }
    }

    private static String normalize(String s) { return s == null ? "" : s.toLowerCase().replaceAll("[^a-z0-9]", ""); }

    private static String skillLabel(Skill skill)
    {
        String raw = skill.name().toLowerCase().replace('_', ' ');
        StringBuilder out = new StringBuilder();
        boolean cap = true;
        for (char c : raw.toCharArray())
        {
            if (cap && Character.isLetter(c)) { out.append(Character.toUpperCase(c)); cap = false; }
            else out.append(c);
            if (c == ' ') cap = true;
        }
        return out.toString();
    }

    private static final class LiveSkill
    {
        private final int level;
        private final int xp;
        private LiveSkill(int level, int xp) { this.level = level; this.xp = xp; }
    }
}
