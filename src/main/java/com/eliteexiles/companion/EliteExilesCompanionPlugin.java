package com.eliteexiles.companion;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.inject.Provides;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
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
        panel = new EliteExilesPanel();
        panel.setController(this);

        navButton = NavigationButton.builder()
            .tooltip("Elite Exiles Companion")
            .icon(makeIcon())
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

    private static BufferedImage makeIcon()
    {
        BufferedImage image = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try
        {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(10, 12, 17));
            g.fillOval(1, 1, 30, 30);
            g.setColor(new Color(217, 180, 74));
            g.drawOval(2, 2, 28, 28);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
            g.drawString("EE", 7, 21);
            g.setColor(new Color(141, 32, 40));
            g.fillRect(7, 24, 18, 2);
        }
        finally
        {
            g.dispose();
        }
        return image;
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
