package com.eliteexiles.companion;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import java.net.URI;
import java.util.Locale;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.StatChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

@PluginDescriptor(
    name = "Elite Exiles Companion",
    description = "Local RuneLite progression dashboard with live skill and session tracking, plus an optional opt-in Elite Exiles Discord coach integration.",
    tags = {"clan", "progression", "goals", "missions", "discord", "coach", "stats", "qol"}
)
public class EliteExilesCompanionPlugin extends Plugin
{
    @Inject private Client client;
    @Inject private ClientToolbar clientToolbar;
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
            .tooltip("Elite Exiles Companion")
            .icon(icon)
            .priority(8)
            .panel(panel)
            .build();
        clientToolbar.addNavigation(navButton);

        if (coachEnabled())
        {
            if (bridge.isLinked())
            {
                panel.setBusy("● DISCORD COACH • CONNECTING");
                pullDashboard();
            }
            else
            {
                panel.setDisconnected("Coach Integration is enabled. Use /runelitelink in Discord to connect.");
            }
        }
        else
        {
            panel.setLocalMode();
        }

        pendingLoginInit = client.getGameState() == GameState.LOGGED_IN;
        backgroundTask = scheduler.scheduleAtFixedRate(this::backgroundTick, 3, 3, TimeUnit.SECONDS);
    }

    private void migrateBridgeUrl()
    {
        String saved = configManager.getConfiguration(EliteExilesCompanionConfig.GROUP, "bridgeUrl");
        if (shouldMigrateBridgeUrl(saved))
        {
            configManager.setConfiguration(
                EliteExilesCompanionConfig.GROUP,
                "bridgeUrl",
                EliteExilesCompanionConfig.PRODUCTION_BRIDGE_URL);
        }
    }

    static boolean shouldMigrateBridgeUrl(String value)
    {
        return !isValidHttpsBridgeOrigin(value);
    }

    static boolean isValidHttpsBridgeOrigin(String value)
    {
        if (value == null || value.isBlank())
        {
            return false;
        }

        try
        {
            String trimmed = value.trim();
            while (trimmed.endsWith("/"))
            {
                trimmed = trimmed.substring(0, trimmed.length() - 1);
            }

            URI uri = URI.create(trimmed);
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
            String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
            if (!"https".equals(scheme) || host.isBlank())
            {
                return false;
            }
            if (uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null)
            {
                return false;
            }
            String path = uri.getPath();
            if (path != null && !path.isBlank() && !"/".equals(path))
            {
                return false;
            }
            return uri.getPort() == -1 || uri.getPort() == 443;
        }
        catch (IllegalArgumentException ex)
        {
            return false;
        }
    }

    @Override
    protected void shutDown()
    {
        if (backgroundTask != null)
        {
            backgroundTask.cancel(true);
            backgroundTask = null;
        }
        if (navButton != null)
        {
            clientToolbar.removeNavigation(navButton);
            navButton = null;
        }
        liveSkills.clear();
        panel = null;
        currentRsn = null;
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        if (event.getGameState() == GameState.LOGGED_IN)
        {
            pendingLoginInit = true;
        }
        else if (event.getGameState() == GameState.LOGIN_SCREEN)
        {
            pendingLoginInit = false;
        }
    }

    @Subscribe
    public void onGameTick(GameTick event)
    {
        if (!pendingLoginInit)
        {
            return;
        }

        Player local = client.getLocalPlayer();
        if (local == null || local.getName() == null || local.getName().isBlank())
        {
            return;
        }

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
            try
            {
                liveSkills.put(skillLabel(skill), new LiveSkill(client.getRealSkillLevel(skill), client.getSkillExperience(skill)));
            }
            catch (Exception ignored)
            {
                // RuneLite occasionally contains non-trainable enum entries; simply skip them.
            }
        }
        pendingLoginInit = false;
        if (panel != null)
        {
            panel.updateLiveSnapshot(currentRsn, Math.max(0L, currentTotalXp - sessionStartXp), snapshotLevels());
        }
        if (coachEnabled() && bridge.isLinked())
        {
            pushLive();
            pullDashboard();
        }
    }

    @Subscribe
    public void onStatChanged(StatChanged event)
    {
        if (event.getSkill() == null)
        {
            return;
        }
        liveSkills.put(skillLabel(event.getSkill()), new LiveSkill(event.getLevel(), event.getXp()));
        try
        {
            currentTotalXp = client.getOverallExperience();
        }
        catch (Exception ignored)
        {
        }
        if (panel != null)
        {
            panel.updateLiveSnapshot(currentRsn, Math.max(0L, currentTotalXp - sessionStartXp), snapshotLevels());
        }
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event)
    {
        if (!EliteExilesCompanionConfig.GROUP.equals(event.getGroup()) || panel == null)
        {
            return;
        }

        if (!coachEnabled())
        {
            panel.setLocalMode();
            return;
        }

        if (bridge.isLinked())
        {
            panel.setBusy("● DISCORD COACH • CONNECTING");
            pullDashboard();
        }
        else
        {
            panel.setDisconnected("Coach Integration is enabled. Use /runelitelink in Discord to connect.");
        }
    }

    void linkFromPanel(String code)
    {
        if (!coachEnabled())
        {
            panel.setLocalMode();
            return;
        }

        if (currentRsn == null || currentRsn.isBlank())
        {
            panel.setError("Log into the OSRS account you registered in Discord first.");
            return;
        }

        // Be forgiving about Discord copy/paste formatting, spaces, hyphens and
        // zero-width characters. Current bridge link codes are exactly 8 characters.
        String cleanedCode = code == null ? "" : code.toUpperCase().replaceAll("[^A-HJ-NP-Z2-9]", "");
        if (cleanedCode.length() != 8)
        {
            panel.setError("That link code is not 8 valid characters. Copy a fresh code from /runelitelink and paste it here.");
            return;
        }

        panel.setBusy("CHECKING DISCORD COACH BRIDGE…");
        bridge.health(
            health -> {
                String bridgeId = health != null && health.has("bridgeId")
                    ? health.get("bridgeId").getAsString()
                    : "UNKNOWN";
                if (panel != null)
                {
                    panel.setBusy("BRIDGE " + bridgeId + " ONLINE • LINKING…");
                }

                bridge.link(cleanedCode, currentRsn,
                    response -> {
                        if (panel != null) panel.updateDashboard(response);
                        pushLive();
                    },
                    this::handleBridgeError);
            },
            message -> {
                if (panel != null)
                {
                    panel.setError(message + " Make sure the current Elite Exiles Discord bot window is running and owns port 47621.");
                }
            });
    }

    void refreshCoachFromPanel()
    {
        if (!coachEnabled())
        {
            panel.setLocalMode();
            return;
        }
        if (!bridge.isLinked())
        {
            panel.setDisconnected("Use /runelitelink in Discord to connect.");
            return;
        }
        panel.setBusy("REFRESHING JAGEX + COACH…");
        bridge.refreshCoach(
            response -> { if (panel != null) panel.updateDashboard(response); },
            this::handleBridgeError);
    }

    void checkInFromPanel()
    {
        if (!coachEnabled())
        {
            panel.setLocalMode();
            return;
        }
        if (!bridge.isLinked())
        {
            panel.setDisconnected("Use /runelitelink in Discord to connect.");
            return;
        }
        panel.setBusy("VERIFYING GOALS + MISSIONS…");
        pushLive();
        bridge.checkIn(
            response -> { if (panel != null) panel.showCheckinResult(response); },
            this::handleBridgeError);
    }

    void runDiagnosticsFromPanel()
    {
        if (!coachEnabled())
        {
            panel.setLocalMode();
            return;
        }
        if (!bridge.isLinked())
        {
            panel.setDisconnected("Link the Discord Coach first. Safe diagnostics verify the authenticated connection without changing progression state.");
            return;
        }
        if (currentRsn == null || currentRsn.isBlank())
        {
            panel.setError("Log into the Discord-linked OSRS account before running diagnostics so the RSN binding can be verified.");
            return;
        }

        final long started = System.nanoTime();
        final String nonce = "ee-" + Long.toUnsignedString(System.nanoTime(), 36);
        panel.setBusy("RUNNING SAFE READ-ONLY DIAGNOSTICS…");

        bridge.health(
            health -> {
                if (!isExpectedHealth(health))
                {
                    handleBridgeError("Bridge health response did not match the Elite Exiles protocol.");
                    return;
                }

                bridge.diagnostics(
                    diagnostic -> {
                        if (!isSafeDiagnosticResponse(diagnostic))
                        {
                            handleBridgeError("Authenticated diagnostics failed the no-mutation safety contract.");
                            return;
                        }
                        String diagnosticRsn = stringValue(diagnostic, "rsn");
                        if (!normalize(currentRsn).equals(normalize(diagnosticRsn)))
                        {
                            handleBridgeError("Authenticated diagnostic RSN did not match the logged-in RuneLite account.");
                            return;
                        }

                        bridge.diagnosticsEcho(nonce,
                            echo -> {
                                if (!isSafeDiagnosticResponse(echo) || !nonce.equals(stringValue(echo, "echo")))
                                {
                                    handleBridgeError("Diagnostic POST/JSON echo failed the safety contract.");
                                    return;
                                }
                                long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
                                String bridgeId = stringValue(diagnostic, "bridgeId");
                                int protocol = intValue(diagnostic, "protocol", 0);
                                if (panel != null)
                                {
                                    panel.showDiagnosticsResult(bridgeId, protocol, elapsedMs);
                                }
                            },
                            this::handleBridgeError);
                    },
                    this::handleBridgeError);
            },
            this::handleBridgeError);
    }

    void unlinkFromPanel()
    {
        if (!coachEnabled())
        {
            panel.setLocalMode();
            return;
        }
        if (!bridge.isLinked())
        {
            panel.setDisconnected("Already unlinked.");
            return;
        }
        panel.setBusy("REVOKING COMPANION LINK…");
        bridge.unlink(
            response -> {
                if (panel != null) panel.setDisconnected("Link revoked. Use /runelitelink to reconnect.");
            },
            this::handleBridgeError);
    }

    private void backgroundTick()
    {
        if (panel == null)
        {
            return;
        }
        panel.updateLiveSnapshot(currentRsn, Math.max(0L, currentTotalXp - sessionStartXp), snapshotLevels());
        if (!coachEnabled() || !bridge.isLinked())
        {
            return;
        }
        long now = System.currentTimeMillis();
        int refreshSeconds = Math.max(30, config.refreshSeconds());
        if (config.autoSync() && currentRsn != null && now - lastLivePush >= 30_000L)
        {
            pushLive();
        }
        if (now - lastDashboardPull >= refreshSeconds * 1000L)
        {
            pullDashboard();
        }
    }

    private void pullDashboard()
    {
        if (!coachEnabled() || !bridge.isLinked())
        {
            return;
        }
        lastDashboardPull = System.currentTimeMillis();
        bridge.getDashboard(
            response -> { if (panel != null) panel.updateDashboard(response); },
            this::handleBridgeError);
    }

    private void pushLive()
    {
        if (!coachEnabled() || !config.autoSync() || !bridge.isLinked() || currentRsn == null || currentRsn.isBlank())
        {
            return;
        }
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
        for (Map.Entry<String, LiveSkill> entry : liveSkills.entrySet())
        {
            out.put(entry.getKey(), entry.getValue().level);
        }
        return out;
    }

    private void handleBridgeError(String message)
    {
        if (panel == null)
        {
            return;
        }
        if (!coachEnabled())
        {
            panel.setLocalMode();
            return;
        }
        if (!bridge.isLinked())
        {
            panel.setDisconnected(message + " Generate a new code with /runelitelink.");
        }
        else
        {
            panel.setError(message);
        }
    }

    private boolean coachEnabled()
    {
        return config != null && config.coachIntegration();
    }

    private static boolean isExpectedHealth(JsonObject response)
    {
        return response != null
            && booleanValue(response, "ok", false)
            && "elite-exiles-runelite-bridge".equals(stringValue(response, "service"))
            && intValue(response, "protocol", 0) >= 4;
    }

    private static boolean isSafeDiagnosticResponse(JsonObject response)
    {
        return response != null
            && booleanValue(response, "ok", false)
            && booleanValue(response, "authenticated", false)
            && booleanValue(response, "readOnly", false)
            && !booleanValue(response, "stateMutation", true)
            && "elite-exiles-runelite-bridge".equals(stringValue(response, "service"))
            && intValue(response, "protocol", 0) >= 4;
    }

    private static boolean booleanValue(JsonObject object, String key, boolean fallback)
    {
        try
        {
            return object != null && object.has(key) && !object.get(key).isJsonNull()
                ? object.get(key).getAsBoolean()
                : fallback;
        }
        catch (Exception ignored)
        {
            return fallback;
        }
    }

    private static String stringValue(JsonObject object, String key)
    {
        try
        {
            return object != null && object.has(key) && !object.get(key).isJsonNull()
                ? object.get(key).getAsString()
                : "";
        }
        catch (Exception ignored)
        {
            return "";
        }
    }

    private static int intValue(JsonObject object, String key, int fallback)
    {
        try
        {
            return object != null && object.has(key) ? object.get(key).getAsInt() : fallback;
        }
        catch (Exception ignored)
        {
            return fallback;
        }
    }

    private static String normalize(String s)
    {
        return s == null ? "" : s.toLowerCase().replaceAll("[^a-z0-9]", "");
    }

    private static String skillLabel(Skill skill)
    {
        String raw = skill.name().toLowerCase().replace('_', ' ');
        StringBuilder out = new StringBuilder();
        boolean cap = true;
        for (char c : raw.toCharArray())
        {
            if (cap && Character.isLetter(c))
            {
                out.append(Character.toUpperCase(c));
                cap = false;
            }
            else
            {
                out.append(c);
            }
            if (c == ' ') cap = true;
        }
        return out.toString();
    }

    private static final class LiveSkill
    {
        private final int level;
        private final int xp;

        private LiveSkill(int level, int xp)
        {
            this.level = level;
            this.xp = xp;
        }
    }
}
