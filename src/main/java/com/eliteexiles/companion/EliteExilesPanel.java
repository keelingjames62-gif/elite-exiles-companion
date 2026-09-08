package com.eliteexiles.companion;

import com.google.gson.JsonObject;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import net.runelite.client.util.LinkBrowser;

/**
 * Elite Exiles Companion 2.0 — Exile HQ.
 *
 * This panel is intentionally informational/planning focused. It does not inject
 * game input, mark tiles, predict attacks, alter menus, or automate gameplay.
 * Optional network features remain behind the existing explicit Elite Exiles Sync
 * setting and use only the authenticated Elite Exiles HTTPS bridge.
 */

public class EliteExilesPanel extends EliteExilesPanelViews
{
    private final JScrollPane outerScroll = new JScrollPane();

    private static final class ScrollState
    {
        private final int outer;
        private final Map<String, Integer> pages;

        private ScrollState(int outer, Map<String, Integer> pages)
        {
            this.outer = outer;
            this.pages = pages;
        }
    }

    public EliteExilesPanel()
    {
        super(false);
        setLayout(new BorderLayout());
        setBackground(BG);

        JPanel root = vertical();
        root.setBorder(BorderFactory.createEmptyBorder(7, 7, 12, 7));
        root.add(buildHero());
        root.add(vGap(7));

        buildLinkCard();
        root.add(linkCard);
        root.add(vGap(7));

        profileHost.setAlignmentX(Component.LEFT_ALIGNMENT);
        profileHost.setMaximumSize(new Dimension(Integer.MAX_VALUE, 170));
        root.add(profileHost);
        root.add(vGap(7));

        root.add(buildNavigation());
        root.add(vGap(6));

        pages.put("HQ", hq);
        pages.put("GROUP", coach);
        pages.put("COMPETE", plan);
        pages.put("CLAN", clan);
        for (Map.Entry<String, JPanel> entry : pages.entrySet())
        {
            JScrollPane scroll = new JScrollPane(entry.getValue());
            scroll.setBorder(null);
            scroll.getViewport().setBackground(BG);
            scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            scroll.getVerticalScrollBar().setUnitIncrement(18);
            pageScrolls.put(entry.getKey(), scroll);
            contentHost.add(scroll, entry.getKey());
        }
        contentHost.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentHost.setPreferredSize(new Dimension(225, 360));
        contentHost.setMaximumSize(new Dimension(Integer.MAX_VALUE, 480));
        root.add(contentHost);
        root.add(vGap(7));

        JPanel actions = transparent(new GridLayout(2, 2, 5, 5));
        actions.setAlignmentX(Component.LEFT_ALIGNMENT);
        actions.setMaximumSize(new Dimension(Integer.MAX_VALUE, 67));
        actions.add(refreshButton);
        actions.add(checkinButton);
        actions.add(diagnosticsButton);
        actions.add(unlinkButton);
        root.add(actions);

        outerScroll.setViewportView(root);
        outerScroll.setBorder(null);
        outerScroll.getViewport().setBackground(BG);
        outerScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        outerScroll.getVerticalScrollBar().setUnitIncrement(18);
        add(outerScroll, BorderLayout.CENTER);

        joinDiscordButton.addActionListener(e -> LinkBrowser.browse(DISCORD_INVITE_URL));
        linkButton.addActionListener(e -> submitLinkCode());
        linkCode.addActionListener(e -> submitLinkCode());
        refreshButton.addActionListener(e -> { if (controller != null) controller.refreshProgressionFromPanel(); });
        checkinButton.addActionListener(e -> { if (controller != null) controller.checkInFromPanel(); });
        diagnosticsButton.addActionListener(e -> { if (controller != null) controller.runDiagnosticsFromPanel(); });
        unlinkButton.addActionListener(e -> { if (controller != null) controller.unlinkFromPanel(); });
        hostGroupButton.addActionListener(e -> submitGroupHost());

        selectPage("HQ");
        setLocalMode();
    }

    public void setController(EliteExilesCompanionPlugin controller)
    {
        this.controller = controller;
    }

    public void openGroupPage()
    {
        selectPage("GROUP");
    }

    private void submitLinkCode()
    {
        if (controller == null || !linkButton.isEnabled())
        {
            return;
        }
        linkButton.setEnabled(false);
        controller.linkFromPanel(linkCode.getText());
    }

    private void submitGroupHost()
    {
        if (controller == null || !hostGroupButton.isEnabled()) return;
        String activity = String.valueOf(groupActivityInput.getSelectedItem()).trim();
        if (activity.length() < 2)
        {
            setError("Choose the activity you want to host first.");
            return;
        }
        int world = 0;
        String worldText = groupWorldInput.getText() == null ? "" : groupWorldInput.getText().trim();
        if (!worldText.isBlank())
        {
            try { world = Integer.parseInt(worldText); }
            catch (NumberFormatException ex) { setError("World must be a number from 301 to 599, or left blank."); return; }
            if (world < 301 || world > 599) { setError("World must be from 301 to 599, or left blank."); return; }
        }
        hostGroupButton.setEnabled(false);
        controller.hostGroupFromPanel(activity, world);
    }

    private ScrollState captureScrollState()
    {
        Map<String, Integer> values = new LinkedHashMap<>();
        for (Map.Entry<String, JScrollPane> entry : pageScrolls.entrySet())
        {
            values.put(entry.getKey(), entry.getValue().getVerticalScrollBar().getValue());
        }
        return new ScrollState(outerScroll.getVerticalScrollBar().getValue(), values);
    }

    private static void restoreScrollBar(JScrollBar bar, int value)
    {
        int min = bar.getMinimum();
        int max = Math.max(min, bar.getMaximum() - bar.getVisibleAmount());
        bar.setValue(Math.max(min, Math.min(max, value)));
    }

    private void restoreScrollState(ScrollState state)
    {
        if (state == null) return;
        SwingUtilities.invokeLater(() -> {
            restoreScrollBar(outerScroll.getVerticalScrollBar(), state.outer);
            for (Map.Entry<String, Integer> entry : state.pages.entrySet())
            {
                JScrollPane scroll = pageScrolls.get(entry.getKey());
                if (scroll != null) restoreScrollBar(scroll.getVerticalScrollBar(), entry.getValue());
            }
        });
    }

    private void scrollPageToBottom(String page)
    {
        JScrollPane scroll = pageScrolls.get(page);
        if (scroll == null)
        {
            return;
        }
        SwingUtilities.invokeLater(() -> {
            int max = scroll.getVerticalScrollBar().getMaximum();
            scroll.getVerticalScrollBar().setValue(max);
        });
    }

    public void updateLiveSnapshot(String rsn, long sessionXp, Map<String, Integer> skillLevels)
    {
        SwingUtilities.invokeLater(() -> {
            String incoming = rsn == null ? "" : rsn;
            long xp = Math.max(0L, sessionXp);
            boolean sessionChanged = !incoming.equalsIgnoreCase(sessionRsn) || xp < localSessionXp;
            if (sessionChanged)
            {
                sessionStartedAt = System.currentTimeMillis();
                sessionRsn = incoming;
            }
            localRsn = incoming;
            localSessionXp = xp;
            liveSkillLevels.clear();
            if (skillLevels != null) liveSkillLevels.putAll(skillLevels);

            // Routine XP/stat events only update this lightweight model. Full HQ/profile
            // Swing trees are rebuilt on account/session change or dashboard/user actions,
            // not continuously while the player is skilling.
            if (sessionChanged)
            {
                ScrollState scrollState = captureScrollState();
                if (lastDashboard != null) rebuildHq(lastDashboard);
                else
                {
                    rebuildLocalProfile();
                    rebuildLocalHq();
                }
                restoreScrollState(scrollState);
            }
        });
    }

    public void setLocalRsn(String name)
    {
        updateLiveSnapshot(name, localSessionXp, new LinkedHashMap<>(liveSkillLevels));
    }

    public void setLocalMode()
    {
        SwingUtilities.invokeLater(() -> {
            lastDashboard = null;
            connection.setText("LOCAL MODE");
            connection.setForeground(GREEN);
            statusDetail.setText("Elite Exiles Sync is off. Enable it in RuneLite plugin settings, then use /runelitelink below.");
            statusDetail.setForeground(MUTED);
            linkCard.setVisible(true);
            linkCode.setEnabled(false);
            linkButton.setText("STEP 1: ENABLE SYNC FIRST");
            linkButton.setEnabled(false);
            refreshButton.setEnabled(false);
            checkinButton.setEnabled(false);
            diagnosticsButton.setEnabled(false);
            unlinkButton.setEnabled(false);
            hostGroupButton.setEnabled(false);
            refreshButton.setVisible(false);
            checkinButton.setVisible(false);
            diagnosticsButton.setVisible(false);
            unlinkButton.setVisible(false);
            rebuildLocalProfile();
            rebuildLocalHq();
            rebuildCoachUnlinked("Enable Elite Exiles Sync to use clan groups, competition, and synced progression.");
            rebuildPlanUnlinked("Link Elite Exiles to load SOTW, weekly missions, and progression competition.");
            rebuildClanUnlinked();
        });
    }

    public void setBusy(String message)
    {
        SwingUtilities.invokeLater(() -> {
            connection.setText("WORKING…");
            connection.setForeground(PURPLE_LIGHT);
            statusDetail.setText(message == null ? "Working…" : message);
        });
    }

    public void setError(String message)
    {
        SwingUtilities.invokeLater(() -> {
            connection.setText("ACTION NEEDED");
            connection.setForeground(AMBER);
            statusDetail.setText(message == null ? "Unknown Companion error." : message);
            statusDetail.setForeground(AMBER);
            if (linkCard.isVisible() && linkCode.isEnabled())
            {
                linkButton.setText("CONNECT ELITE EXILES");
                linkButton.setEnabled(true);
            }
        });
    }

    public void setDisconnected(String message)
    {
        SwingUtilities.invokeLater(() -> {
            lastDashboard = null;
            connection.setText("ELITE EXILES NOT LINKED");
            connection.setForeground(PURPLE_LIGHT);
            statusDetail.setText(message == null ? "Run /runelitelink in Discord, paste the code, then connect Elite Exiles." : message);
            statusDetail.setForeground(MUTED);
            linkCard.setVisible(true);
            linkCode.setEnabled(true);
            linkButton.setText("CONNECT ELITE EXILES");
            linkButton.setEnabled(true);
            refreshButton.setEnabled(false);
            checkinButton.setEnabled(false);
            diagnosticsButton.setEnabled(false);
            unlinkButton.setEnabled(false);
            hostGroupButton.setEnabled(false);
            refreshButton.setVisible(false);
            checkinButton.setVisible(false);
            diagnosticsButton.setVisible(false);
            unlinkButton.setVisible(false);
            rebuildLocalProfile();
            rebuildLocalHq();
            rebuildCoachUnlinked("Connect above to host and join progression-focused clan groups.");
            rebuildPlanUnlinked("Connect above to load SOTW, weekly missions, and clan competition.");
            rebuildClanUnlinked();
        });
    }

    public void updateDashboard(JsonObject response)
    {
        SwingUtilities.invokeLater(() -> {
            ScrollState scrollState = captureScrollState();
            JsonObject d = response != null && response.has("dashboard") && response.get("dashboard").isJsonObject()
                ? response.getAsJsonObject("dashboard") : response;
            if (d == null || !d.has("member"))
            {
                setError("Elite Exiles dashboard was missing member data.");
                return;
            }
            lastDashboard = d.deepCopy();
            JsonObject rl = object(d, "runelite");
            long lastSeen = longValue(rl, "lastSeenAt", 0L);
            boolean fresh = lastSeen > 0 && System.currentTimeMillis() - lastSeen < 120_000L;
            connection.setText(fresh ? "DISCORD LINKED • LIVE" : "DISCORD LINKED");
            connection.setForeground(GREEN);
            statusDetail.setText(fresh
                ? "Companion access active. Your Discord link stays valid without a clan-rank or membership gate."
                : "Companion access active. Refresh for the newest account snapshot.");
            statusDetail.setForeground(GREEN);
            linkCard.setVisible(false);
            linkCode.setText("");
            linkCode.setEnabled(false);
            linkButton.setText("CONNECT ELITE EXILES");
            linkButton.setEnabled(false);
            refreshButton.setEnabled(true);
            checkinButton.setEnabled(true);
            diagnosticsButton.setEnabled(true);
            unlinkButton.setEnabled(true);
            hostGroupButton.setEnabled(true);
            refreshButton.setVisible(true);
            checkinButton.setVisible(true);
            diagnosticsButton.setVisible(true);
            unlinkButton.setVisible(true);
            rebuildProfile(d);
            rebuildHq(d);
            rebuildCoach(d);
            rebuildPlan(d);
            rebuildClan(d);
            restoreScrollState(scrollState);
        });
    }

    public void showCheckinResult(JsonObject response)
    {
        updateDashboard(response);
        SwingUtilities.invokeLater(() -> {
            connection.setText("CHECK-IN COMPLETE");
            connection.setForeground(GREEN);
            statusDetail.setText("Jagex verification complete. Goals, missions and rank progress were refreshed.");
            statusDetail.setForeground(GREEN);
        });
    }

    public void showDiagnosticsResult(String bridgeId, int protocol, long elapsedMs)
    {
        SwingUtilities.invokeLater(() -> {
            connection.setText("DIAGNOSTICS PASS");
            connection.setForeground(GREEN);
            statusDetail.setText("HTTPS/auth/RSN/JSON checks passed • protocol " + protocol + " • " + elapsedMs + " ms • bridge " + bridgeId);
            statusDetail.setForeground(GREEN);
        });
    }

}
