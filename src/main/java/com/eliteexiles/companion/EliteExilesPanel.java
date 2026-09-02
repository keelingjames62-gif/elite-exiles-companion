package com.eliteexiles.companion;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.geom.Path2D;
import java.text.NumberFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.text.DefaultCaret;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.LinkBrowser;

/**
 * Narrow-sidebar graphical UI for the Elite Exiles RuneLite Companion.
 *
 * Local RuneLite tracking is always available. Everything marked DISCORD COACH
 * belongs to the optional, user-enabled Coach Integration. Everything marked
 * RUNELITE LIVE is read from the currently logged-in RuneLite client. This keeps
 * the visual UI explicit about which system supplied each value.
 */
public class EliteExilesPanel extends PluginPanel
{
    private static final String DISCORD_INVITE_URL = "https://discord.gg/FTJhv48K2";
    // Current Elite Exiles branding: black / purple / silver.
    // Legacy constant names are retained to keep this UI-only diff narrow and reviewable.
    private static final Color BG = new Color(8, 7, 12);
    private static final Color PANEL = new Color(17, 15, 23);
    private static final Color PANEL_2 = new Color(25, 22, 33);
    private static final Color PANEL_3 = new Color(34, 30, 45);
    private static final Color BORDER = new Color(76, 70, 91);
    private static final Color GOLD = new Color(142, 82, 214);
    private static final Color GOLD_LIGHT = new Color(221, 216, 230);
    private static final Color RED = new Color(171, 67, 91);
    private static final Color GREEN = new Color(72, 194, 126);
    private static final Color BLUE = new Color(124, 92, 196);
    private static final Color ORANGE = new Color(204, 145, 79);
    private static final Color WHITE = new Color(242, 240, 246);
    private static final Color MUTED = new Color(184, 180, 194);

    private EliteExilesCompanionPlugin controller;

    private final JLabel connection = label("LOCAL MODE", 11, GREEN, Font.BOLD);
    private final JTextArea statusDetail = wrap("Optional Coach Integration is disabled.", MUTED, 11);
    private final JTextField linkCode = new JTextField();
    private final JButton joinDiscordButton = actionButton("JOIN ELITE EXILES DISCORD", GOLD);
    private final JButton linkButton = actionButton("LINK COACH", GOLD);
    private final JButton refreshButton = actionButton("REFRESH", GOLD);
    private final JButton checkinButton = actionButton("CHECK-IN", GREEN);
    private final JButton diagnosticsButton = actionButton("RUN SAFE DIAGNOSTICS", BLUE);
    private final JButton unlinkButton = actionButton("UNLINK", RED);

    private final JPanel linkCard = roundedCard(GOLD);
    private final JPanel profileHost = new JPanel(new BorderLayout());
    private final JPanel contentHost = new JPanel(new CardLayout());
    private final Map<String, JPanel> pages = new LinkedHashMap<>();
    private final Map<String, JButton> navButtons = new LinkedHashMap<>();
    private final Map<String, JScrollPane> pageScrolls = new LinkedHashMap<>();
    private JScrollPane outerScroll;

    private final JPanel home = verticalPanel();
    private final JPanel skills = verticalPanel();
    private final JPanel today = verticalPanel();
    private final JPanel goals = verticalPanel();
    private final JPanel missions = verticalPanel();
    private final JPanel roadmap = verticalPanel();
    private final JPanel tips = verticalPanel();


    private JsonObject lastDashboard;
    private final Map<String, Integer> liveSkillLevels = new LinkedHashMap<>();
    private String localRsn = "";
    private long localSessionXp = 0L;
    private long liveSessionStartedAt = System.currentTimeMillis();
    private String liveSessionRsn = "";

    public EliteExilesPanel()
    {
        super(false);
        setLayout(new BorderLayout());
        setBackground(BG);

        JPanel root = verticalPanel();
        root.setBorder(BorderFactory.createEmptyBorder(7, 7, 10, 7));

        root.add(buildHero());
        root.add(Box.createVerticalStrut(7));

        buildLinkCard();
        root.add(linkCard);
        root.add(Box.createVerticalStrut(7));

        profileHost.setOpaque(false);
        profileHost.setAlignmentX(Component.LEFT_ALIGNMENT);
        profileHost.setMaximumSize(new Dimension(Integer.MAX_VALUE, 210));
        root.add(profileHost);
        root.add(Box.createVerticalStrut(7));

        root.add(buildNavigation());
        root.add(Box.createVerticalStrut(6));

        pages.put("HOME", home);
        pages.put("SKILLS", skills);
        pages.put("TODAY", today);
        pages.put("GOALS", goals);
        pages.put("MISSIONS", missions);
        pages.put("ROADMAP", roadmap);
        pages.put("TIPS", tips);
        for (Map.Entry<String, JPanel> entry : pages.entrySet())
        {
            JScrollPane pageScroll = scroll(entry.getValue());
            pageScrolls.put(entry.getKey(), pageScroll);
            contentHost.add(pageScroll, entry.getKey());
        }
        contentHost.setOpaque(false);
        contentHost.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentHost.setPreferredSize(new Dimension(205, 405));
        contentHost.setMaximumSize(new Dimension(Integer.MAX_VALUE, 430));
        root.add(contentHost);
        root.add(Box.createVerticalStrut(7));

        JPanel actionRow = new JPanel(new GridLayout(1, 2, 5, 0));
        actionRow.setOpaque(false);
        actionRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        actionRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        actionRow.add(refreshButton);
        actionRow.add(checkinButton);
        root.add(actionRow);
        root.add(Box.createVerticalStrut(5));
        diagnosticsButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        diagnosticsButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 31));
        root.add(diagnosticsButton);
        root.add(Box.createVerticalStrut(5));
        unlinkButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        unlinkButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 31));
        root.add(unlinkButton);

        outerScroll = new JScrollPane(root);
        outerScroll.setBorder(null);
        outerScroll.getViewport().setBackground(BG);
        outerScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        outerScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        outerScroll.getVerticalScrollBar().setUnitIncrement(18);
        outerScroll.getVerticalScrollBar().setBlockIncrement(72);
        outerScroll.setWheelScrollingEnabled(true);
        add(outerScroll, BorderLayout.CENTER);

        joinDiscordButton.addActionListener(e -> LinkBrowser.browse(DISCORD_INVITE_URL));
        linkButton.addActionListener(e -> { if (controller != null) controller.linkFromPanel(linkCode.getText()); });
        refreshButton.addActionListener(e -> { if (controller != null) controller.refreshCoachFromPanel(); });
        checkinButton.addActionListener(e -> { if (controller != null) controller.checkInFromPanel(); });
        diagnosticsButton.addActionListener(e -> { if (controller != null) controller.runDiagnosticsFromPanel(); });
        unlinkButton.addActionListener(e -> { if (controller != null) controller.unlinkFromPanel(); });

        setLocalMode();
        selectPage("HOME");
    }

    private void preserveScrollPositions(Runnable update)
    {
        int outerPosition = scrollPosition(outerScroll);
        Map<String, Integer> pagePositions = new LinkedHashMap<>();
        for (Map.Entry<String, JScrollPane> entry : pageScrolls.entrySet())
        {
            pagePositions.put(entry.getKey(), scrollPosition(entry.getValue()));
        }

        update.run();

        restoreScrollPositions(outerPosition, pagePositions);
        SwingUtilities.invokeLater(() -> restoreScrollPositions(outerPosition, pagePositions));
    }

    private void restoreScrollPositions(int outerPosition, Map<String, Integer> pagePositions)
    {
        restoreScrollPosition(outerScroll, outerPosition);
        for (Map.Entry<String, Integer> entry : pagePositions.entrySet())
        {
            restoreScrollPosition(pageScrolls.get(entry.getKey()), entry.getValue());
        }
    }

    private static int scrollPosition(JScrollPane pane)
    {
        return pane == null ? 0 : pane.getVerticalScrollBar().getValue();
    }

    private static void restoreScrollPosition(JScrollPane pane, int value)
    {
        if (pane == null)
        {
            return;
        }

        int max = Math.max(0,
            pane.getVerticalScrollBar().getMaximum() - pane.getVerticalScrollBar().getVisibleAmount());
        pane.getVerticalScrollBar().setValue(Math.max(0, Math.min(value, max)));
    }

    private JPanel buildHero()
    {
        GradientCard hero = new GradientCard(new Color(34, 24, 47), new Color(10, 9, 14), GOLD);
        hero.setLayout(new BorderLayout(7, 0));
        hero.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 9));
        hero.setAlignmentX(Component.LEFT_ALIGNMENT);
        hero.setMaximumSize(new Dimension(Integer.MAX_VALUE, 88));

        JLabel crest = new JLabel(new ImageIcon(ImageUtil.loadImageResource(getClass(), "header_logo.png")));
        crest.setPreferredSize(new Dimension(50, 50));
        crest.setHorizontalAlignment(SwingConstants.CENTER);
        hero.add(crest, BorderLayout.WEST);

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.add(label("ELITE EXILES", 17, GOLD_LIGHT, Font.BOLD));
        JLabel subtitle = label("RUNELITE COMPANION", 9, MUTED, Font.BOLD);
        subtitle.setFont(subtitle.getFont().deriveFont(Font.BOLD, 9.5f));
        text.add(subtitle);
        text.add(Box.createVerticalStrut(4));
        text.add(connection);
        hero.add(text, BorderLayout.CENTER);
        return hero;
    }

    private void buildLinkCard()
    {
        linkCard.setLayout(new BoxLayout(linkCard, BoxLayout.Y_AXIS));
        linkCard.add(sourcePill("DISCORD COACH • OPTIONAL", GOLD));
        linkCard.add(Box.createVerticalStrut(6));
        linkCard.add(wrap("New to Elite Exiles? Join the Discord first. Already a member? Run /runelitelink to get your one-time code.", MUTED, 10));
        linkCard.add(Box.createVerticalStrut(6));

        joinDiscordButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        joinDiscordButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        joinDiscordButton.setToolTipText("Open the official Elite Exiles Discord invite in your browser");
        linkCard.add(joinDiscordButton);

        linkCard.add(Box.createVerticalStrut(8));
        linkCard.add(wrap("1  Run /runelitelink in Discord\n2  Paste the 8-character code below\n3  Link while logged into the same OSRS character", WHITE, 10));
        linkCard.add(Box.createVerticalStrut(7));

        linkCode.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        linkCode.setBackground(PANEL_3);
        linkCode.setForeground(WHITE);
        linkCode.setCaretColor(WHITE);
        linkCode.setHorizontalAlignment(SwingConstants.CENTER);
        linkCode.setFont(linkCode.getFont().deriveFont(Font.BOLD, 14f));
        linkCode.setToolTipText("Paste the one-time /runelitelink code from Elite Exiles Discord");
        linkCode.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(GOLD),
            BorderFactory.createEmptyBorder(4, 6, 4, 6)));
        linkCard.add(linkCode);

        linkCard.add(Box.createVerticalStrut(6));
        linkButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        linkButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        linkCard.add(linkButton);

        linkCard.add(Box.createVerticalStrut(6));
        statusDetail.setAlignmentX(Component.LEFT_ALIGNMENT);
        linkCard.add(statusDetail);
    }

    private JPanel buildNavigation()
    {
        JPanel nav = new JPanel(new GridLayout(3, 3, 4, 4));
        nav.setOpaque(false);
        nav.setAlignmentX(Component.LEFT_ALIGNMENT);
        nav.setMaximumSize(new Dimension(Integer.MAX_VALUE, 96));
        String[] names = {"HOME", "SKILLS", "TODAY", "GOALS", "MISSIONS", "ROADMAP", "TIPS"};
        for (String name : names)
        {
            JButton b = navButton(name);
            b.addActionListener(e -> selectPage(name));
            navButtons.put(name, b);
            nav.add(b);
        }
        return nav;
    }

    private void selectPage(String page)
    {
        CardLayout layout = (CardLayout) contentHost.getLayout();
        layout.show(contentHost, page);
        for (Map.Entry<String, JButton> e : navButtons.entrySet())
        {
            boolean active = e.getKey().equals(page);
            JButton b = e.getValue();
            b.setForeground(active ? BG : MUTED);
            b.setBackground(active ? GOLD : PANEL_2);
            b.setBorder(BorderFactory.createLineBorder(active ? GOLD_LIGHT : BORDER));
        }
    }

    public void setController(EliteExilesCompanionPlugin controller)
    {
        this.controller = controller;
    }

    /** Update purely local, live RuneLite values. No reward logic is run here. */
    public void updateLiveSnapshot(String rsn, long sessionXp, Map<String, Integer> skillLevels)
    {
        SwingUtilities.invokeLater(() -> preserveScrollPositions(() -> {
            String incomingRsn = rsn == null ? "" : rsn;
            long incomingXp = Math.max(0L, sessionXp);
            if (!incomingRsn.equalsIgnoreCase(liveSessionRsn) || incomingXp < localSessionXp)
            {
                liveSessionStartedAt = System.currentTimeMillis();
                liveSessionRsn = incomingRsn;
            }
            localRsn = incomingRsn;
            localSessionXp = incomingXp;
            liveSkillLevels.clear();
            if (skillLevels != null) liveSkillLevels.putAll(skillLevels);
            rebuildSkills();
            if (lastDashboard != null)
            {
                rebuildHome(lastDashboard);
            }
            else
            {
                rebuildUnlinkedProfile();
                rebuildLocalHome();
            }
        }));
    }

    public void setLocalRsn(String name)
    {
        updateLiveSnapshot(name, localSessionXp, new LinkedHashMap<>(liveSkillLevels));
    }

    public void setLocalMode()
    {
        SwingUtilities.invokeLater(() -> preserveScrollPositions(() -> {
            lastDashboard = null;
            connection.setText("LOCAL MODE");
            connection.setForeground(GREEN);
            statusDetail.setText("Coach Integration is disabled. Local RuneLite tracking remains active.");
            statusDetail.setForeground(MUTED);
            linkCard.setVisible(false);
            refreshButton.setVisible(false);
            checkinButton.setVisible(false);
            diagnosticsButton.setVisible(false);
            unlinkButton.setVisible(false);
            refreshButton.setEnabled(false);
            checkinButton.setEnabled(false);
            diagnosticsButton.setEnabled(false);
            unlinkButton.setEnabled(false);
            rebuildUnlinkedProfile();
            rebuildLocalHome();
            rebuildSkills();
            clearPanel(today, "Optional Coach Integration is off. Enable it in the plugin settings to use session plans.");
            clearPanel(goals, "Optional Coach Integration is off. Enable it in the plugin settings to use synced goals.");
            clearPanel(missions, "Optional Coach Integration is off. Enable it in the plugin settings to use synced missions.");
            clearPanel(roadmap, "Optional Coach Integration is off. Enable it in the plugin settings to use the coach roadmap.");
            clearPanel(tips, "Optional Coach Integration is off. Enable it in the plugin settings to use coach suggestions.");
        }));
    }

    public void setBusy(String text)
    {
        SwingUtilities.invokeLater(() -> preserveScrollPositions(() -> {
            connection.setText(shortText(text, 24));
            connection.setForeground(GOLD_LIGHT);
            statusDetail.setText(text == null ? "" : text);
        }));
    }

    public void setError(String message)
    {
        SwingUtilities.invokeLater(() -> preserveScrollPositions(() -> {
            connection.setText("ACTION NEEDED");
            connection.setForeground(ORANGE);
            statusDetail.setText(message == null ? "Unknown coach error." : message);
            statusDetail.setForeground(ORANGE);
        }));
    }

    public void setDisconnected(String message)
    {
        SwingUtilities.invokeLater(() -> preserveScrollPositions(() -> {
            lastDashboard = null;
            connection.setText("COACH NOT LINKED");
            connection.setForeground(GOLD_LIGHT);
            statusDetail.setText(message == null ? "Use /runelitelink in Discord to connect." : message);
            statusDetail.setForeground(MUTED);
            linkCard.setVisible(true);
            refreshButton.setVisible(true);
            checkinButton.setVisible(true);
            diagnosticsButton.setVisible(true);
            unlinkButton.setVisible(true);
            refreshButton.setEnabled(false);
            checkinButton.setEnabled(false);
            diagnosticsButton.setEnabled(false);
            unlinkButton.setEnabled(false);
            rebuildUnlinkedProfile();
            rebuildLocalHome();
            rebuildSkills();
            clearPanel(today, "Link the optional Discord Coach to load your session plan.");
            clearPanel(goals, "Link the optional Discord Coach to load synced goals.");
            clearPanel(missions, "Link the optional Discord Coach to load synced missions.");
            clearPanel(roadmap, "Link the optional Discord Coach to load your NOW → NEXT → LATER roadmap.");
            clearPanel(tips, "Link the optional Discord Coach to load coach suggestions.");
        }));
    }

    public void updateDashboard(JsonObject response)
    {
        SwingUtilities.invokeLater(() -> preserveScrollPositions(() -> {
            JsonObject d = response.has("dashboard") && response.get("dashboard").isJsonObject()
                ? response.getAsJsonObject("dashboard") : response;
            if (d == null || !d.has("member"))
            {
                setError("Coach dashboard was missing member data.");
                return;
            }
            lastDashboard = d.deepCopy();

            JsonObject rl = object(d, "runelite");
            long lastSeen = longValue(rl, "lastSeenAt", 0L);
            boolean fresh = lastSeen > 0 && System.currentTimeMillis() - lastSeen < 120_000L;
            connection.setText(fresh ? "COACH LIVE" : "COACH LINKED");
            connection.setForeground(fresh ? GREEN : GOLD_LIGHT);
            statusDetail.setText(fresh
                ? "RuneLite Live ↔ Discord Coach are synchronized."
                : "Discord Coach is linked. Press Refresh to pull the latest profile.");
            statusDetail.setForeground(fresh ? GREEN : MUTED);
            linkCard.setVisible(false);
            refreshButton.setVisible(true);
            checkinButton.setVisible(true);
            diagnosticsButton.setVisible(true);
            unlinkButton.setVisible(true);
            refreshButton.setEnabled(true);
            checkinButton.setEnabled(true);
            diagnosticsButton.setEnabled(true);
            unlinkButton.setEnabled(true);

            rebuildProfile(d, fresh);
            rebuildHome(d);
            rebuildSkills();
            rebuildToday(today, object(d, "plan"), array(d, "milestones"));
            rebuildRoadmap(roadmap, object(d, "roadmap"));
            rebuildTasks(goals, array(d, "goals"), GOLD, "No active goals yet. Use /goals or a verified check-in.", "DISCORD GOAL");
            JsonObject missionObj = object(d, "missions");
            JsonArray missionRows = new JsonArray();
            for (JsonElement e : array(missionObj, "daily")) missionRows.add(e);
            for (JsonElement e : array(missionObj, "weekly")) missionRows.add(e);
            rebuildTasks(missions, missionRows, BLUE, "No missions generated yet. Run a verified check-in.", "DISCORD MISSION");
            rebuildTips(tips, array(object(d, "coach"), "recommendations"));
        }));
    }

    public void showCheckinResult(JsonObject response)
    {
        updateDashboard(response);
        SwingUtilities.invokeLater(() -> preserveScrollPositions(() -> {
            int g = array(response, "completedGoals").size();
            int m = array(response, "completedMissions").size();
            connection.setText(g + m > 0 ? "CHECK-IN VERIFIED" : "CHECK-IN COMPLETE");
            connection.setForeground(GREEN);
            statusDetail.setText(g + m > 0
                ? (g + m) + " verified completion" + (g + m == 1 ? "" : "s") + " detected. Discord progression was refreshed."
                : "Jagex verification completed. No new rewarded completions were detected.");
            statusDetail.setForeground(GREEN);
        }));
    }

    public void showDiagnosticsResult(String bridgeId, int protocol, long elapsedMs)
    {
        SwingUtilities.invokeLater(() -> preserveScrollPositions(() -> {
            connection.setText("DIAGNOSTICS PASS");
            connection.setForeground(GREEN);
            statusDetail.setText(
                "Safe read-only diagnostics passed. Bridge " + shortText(bridgeId, 16)
                    + " • protocol " + protocol
                    + " • HTTPS/auth/GET/POST/JSON/RSN checks OK"
                    + " • " + Math.max(0L, elapsedMs) + " ms. No points, goals, missions, registrations, or Discord settings were changed.");
            statusDetail.setForeground(GREEN);
        }));
    }

    private void rebuildUnlinkedProfile()
    {
        profileHost.removeAll();
        JPanel card = roundedCard(BORDER);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.add(sourcePill("RUNELITE LIVE", BLUE));
        card.add(Box.createVerticalStrut(6));
        card.add(label(localRsn.isBlank() ? "LOG INTO OSRS" : localRsn, 16, WHITE, Font.BOLD));
        card.add(Box.createVerticalStrut(3));
        card.add(label("Session  +" + format(localSessionXp) + " XP", 11, GREEN, Font.BOLD));
        card.add(Box.createVerticalStrut(5));
        card.add(wrap("RuneLite tracking is active locally. Coach Integration is optional and remains off until you enable it in plugin settings.", MUTED, 10));
        profileHost.add(card, BorderLayout.CENTER);
        profileHost.revalidate();
        profileHost.repaint();
    }

    private void rebuildLocalHome()
    {
        home.removeAll();
        home.add(sectionTitle("LOCAL DASHBOARD", "RUNELITE"));
        home.add(Box.createVerticalStrut(5));

        int totalLevel = 0;
        int skillCount = 0;
        for (Map.Entry<String, Integer> entry : liveSkillLevels.entrySet())
        {
            if ("Overall".equalsIgnoreCase(entry.getKey()))
            {
                continue;
            }
            int level = Math.max(0, entry.getValue());
            if (level > 0)
            {
                totalLevel += level;
                skillCount++;
            }
        }

        long elapsed = Math.max(1L, System.currentTimeMillis() - liveSessionStartedAt);
        long xpHour = Math.round(localSessionXp * 3_600_000.0 / elapsed);

        JPanel stats = new JPanel(new GridLayout(2, 2, 5, 5));
        stats.setOpaque(false);
        stats.setAlignmentX(Component.LEFT_ALIGNMENT);
        stats.add(statTile("TOTAL LEVEL", format(totalLevel), "TL", GOLD));
        stats.add(statTile("SESSION XP", format(localSessionXp), "XP", GREEN));
        stats.add(statTile("XP / HOUR", format(xpHour), "RATE", BLUE));
        stats.add(statTile("SKILLS", String.valueOf(skillCount), "LIVE", ORANGE));
        stats.setMaximumSize(new Dimension(Integer.MAX_VALUE, 118));
        home.add(stats);
        home.add(Box.createVerticalStrut(8));

        home.add(sectionTitle("LIVE SESSION", "RUNELITE"));
        home.add(Box.createVerticalStrut(5));
        SessionGauge session = new SessionGauge(localSessionXp, xpHour);
        session.setAlignmentX(Component.LEFT_ALIGNMENT);
        session.setMaximumSize(new Dimension(Integer.MAX_VALUE, 82));
        home.add(session);
        home.add(Box.createVerticalStrut(8));

        home.add(sectionTitle("CORE LEVELS", "RUNELITE"));
        home.add(Box.createVerticalStrut(5));
        home.add(coreSkillStrip());
        home.add(Box.createVerticalStrut(8));

        JPanel privacy = roundedCard(BLUE);
        privacy.setLayout(new BoxLayout(privacy, BoxLayout.Y_AXIS));
        privacy.add(sourcePill("LOCAL-FIRST", BLUE));
        privacy.add(Box.createVerticalStrut(5));
        privacy.add(wrap("No Elite Exiles network requests are made in Local Mode. Coach Integration only starts after you explicitly enable it in the plugin settings.", MUTED, 10));
        home.add(privacy);

        home.revalidate();
        home.repaint();
    }

    private void rebuildProfile(JsonObject d, boolean fresh)
    {
        profileHost.removeAll();
        JsonObject member = object(d, "member");
        JsonObject r = object(d, "rank");
        JsonObject c = object(d, "coach");
        JsonObject streak = object(d, "streak");

        JPanel card = roundedCard(GOLD);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.add(buildSyncStrip(fresh));
        card.add(Box.createVerticalStrut(7));

        JPanel row = new JPanel(new BorderLayout(7, 0));
        row.setOpaque(false);
        RankEmblem emblem = new RankEmblem(str(r, "name", "Recruit"), GOLD);
        emblem.setPreferredSize(new Dimension(46, 54));
        row.add(emblem, BorderLayout.WEST);
        JPanel identity = new JPanel();
        identity.setOpaque(false);
        identity.setLayout(new BoxLayout(identity, BoxLayout.Y_AXIS));
        identity.add(label(str(member, "rsn", localRsn.isBlank() ? "Unknown" : localRsn), 15, WHITE, Font.BOLD));
        identity.add(label(str(member, "mode", "main").toUpperCase() + " • " + str(c, "stage", "learning").toUpperCase(), 9, MUTED, Font.BOLD));
        identity.add(Box.createVerticalStrut(3));
        identity.add(label(str(r, "name", "Recruit").toUpperCase(), 9, GOLD_LIGHT, Font.BOLD));
        row.add(identity, BorderLayout.CENTER);
        RingMeter coverage = new RingMeter(integer(c, "coveragePercent", 0), BLUE, "DATA");
        coverage.setPreferredSize(new Dimension(46, 54));
        row.add(coverage, BorderLayout.EAST);
        card.add(row);
        card.add(Box.createVerticalStrut(8));

        card.add(label(format(integer(r, "points", 0)) + " EE POINTS", 11, GOLD_LIGHT, Font.BOLD));
        FancyBar rankBar = new FancyBar(integer(r, "percent", 0), GOLD);
        String next = str(r, "next", "TOP RANK");
        int remaining = integer(r, "remaining", 0);
        rankBar.setCaption(next.equals("TOP RANK") || next.equals("null")
            ? "TOP EARNABLE RANK"
            : integer(r, "percent", 0) + "% → " + next + " • " + format(remaining) + " EE left");
        card.add(Box.createVerticalStrut(4));
        card.add(rankBar);
        card.add(Box.createVerticalStrut(7));
        card.add(label("Streak  " + integer(streak, "current", 0) + "d   •   Focus  " + str(c, "focus", "balanced").toUpperCase(), 9, MUTED, Font.BOLD));

        profileHost.add(card, BorderLayout.CENTER);
        profileHost.revalidate();
        profileHost.repaint();
    }

    private JPanel buildSyncStrip(boolean fresh)
    {
        JPanel strip = new JPanel(new BorderLayout(4, 0));
        strip.setOpaque(false);
        strip.add(sourcePill("RUNELITE LIVE", BLUE), BorderLayout.WEST);
        JLabel bridge = label("↔", 13, fresh ? GREEN : GOLD, Font.BOLD);
        bridge.setHorizontalAlignment(SwingConstants.CENTER);
        strip.add(bridge, BorderLayout.CENTER);
        strip.add(sourcePill("DISCORD COACH", GOLD), BorderLayout.EAST);
        strip.setMaximumSize(new Dimension(Integer.MAX_VALUE, 23));
        return strip;
    }

    private void rebuildHome(JsonObject d)
    {
        home.removeAll();
        JsonObject jagex = object(d, "jagex");
        JsonObject readiness = object(d, "readiness");
        JsonObject streak = object(d, "streak");
        JsonObject coach = object(d, "coach");
        JsonObject rl = object(d, "runelite");
        JsonObject rank = object(d, "rank");
        JsonArray goalRows = array(d, "goals");
        JsonObject missionObj = object(d, "missions");
        int missionCount = array(missionObj, "daily").size() + array(missionObj, "weekly").size();

        home.add(sectionTitle("ACCOUNT DASHBOARD", "SYNCED"));
        home.add(Box.createVerticalStrut(5));

        JPanel stats = new JPanel(new GridLayout(2, 2, 5, 5));
        stats.setOpaque(false);
        stats.setAlignmentX(Component.LEFT_ALIGNMENT);
        stats.add(statTile("TOTAL", format(integer(jagex, "totalLevel", 0)), "TL", GOLD));
        stats.add(statTile("COMBAT", String.valueOf(integer(jagex, "combat", 0)), "CB", RED));
        stats.add(statTile("BOSS KC", format(integer(jagex, "totalBossKc", 0)), "KC", BLUE));
        stats.add(statTile("STREAK", integer(streak, "current", 0) + "d", "ST", GREEN));
        stats.setMaximumSize(new Dimension(Integer.MAX_VALUE, 118));
        home.add(stats);
        home.add(Box.createVerticalStrut(8));

        JPanel momentum = roundedCard(GOLD);
        momentum.setLayout(new BorderLayout(7, 0));
        MomentumBadge badge = new MomentumBadge(goalRows.size(), missionCount, integer(rank, "percent", 0));
        badge.setPreferredSize(new Dimension(54, 62));
        momentum.add(badge, BorderLayout.WEST);
        JPanel mText = new JPanel();
        mText.setOpaque(false);
        mText.setLayout(new BoxLayout(mText, BoxLayout.Y_AXIS));
        mText.add(sourcePill("PROGRESS", GOLD));
        mText.add(Box.createVerticalStrut(4));
        mText.add(label(goalRows.size() + (goalRows.size() == 1 ? " goal" : " goals") + "  •  " + missionCount + (missionCount == 1 ? " mission" : " missions"), 9, WHITE, Font.BOLD));
        mText.add(label(integer(rank, "percent", 0) + "% to next rank", 9, MUTED, Font.PLAIN));
        momentum.add(mText, BorderLayout.CENTER);
        home.add(momentum);
        home.add(Box.createVerticalStrut(8));

        home.add(sectionTitle("LIVE SESSION", "RUNELITE"));
        home.add(Box.createVerticalStrut(5));
        long sessionXp = Math.max(localSessionXp, longValue(rl, "sessionXp", 0L));
        long elapsed = Math.max(1L, System.currentTimeMillis() - liveSessionStartedAt);
        long xpHour = Math.round(sessionXp * 3_600_000.0 / elapsed);
        SessionGauge session = new SessionGauge(sessionXp, xpHour);
        session.setAlignmentX(Component.LEFT_ALIGNMENT);
        session.setMaximumSize(new Dimension(Integer.MAX_VALUE, 82));
        home.add(session);
        home.add(Box.createVerticalStrut(8));

        home.add(sectionTitle("ACCOUNT READINESS", "COACH"));
        home.add(Box.createVerticalStrut(5));
        JPanel rings = new JPanel(new GridLayout(1, 4, 3, 0));
        rings.setOpaque(false);
        rings.setAlignmentX(Component.LEFT_ALIGNMENT);
        rings.add(new RingMeter(integer(readiness, "combat", 0), RED, "CBT"));
        rings.add(new RingMeter(integer(readiness, "skilling", 0), GREEN, "SKL"));
        rings.add(new RingMeter(integer(readiness, "pvm", 0), GOLD, "PVM"));
        rings.add(new RingMeter(integer(readiness, "pvp", 0), BLUE, "PVP"));
        rings.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
        home.add(rings);
        home.add(Box.createVerticalStrut(8));

        home.add(sectionTitle("LIVE LEVELS", "RUNELITE"));
        home.add(Box.createVerticalStrut(5));
        home.add(coreSkillStrip());
        home.add(Box.createVerticalStrut(8));

        JsonArray recs = array(coach, "recommendations");
        if (recs.size() > 0 && recs.get(0).isJsonObject())
        {
            JsonObject top = recs.get(0).getAsJsonObject();
            Color conf = confidenceColor(str(top, "confidence", "suggested"));
            JPanel next = roundedCard(conf);
            next.setLayout(new BorderLayout(8, 0));
            CoachGlyph glyph = new CoachGlyph(conf);
            glyph.setPreferredSize(new Dimension(42, 48));
            next.add(glyph, BorderLayout.WEST);
            JPanel nt = new JPanel();
            nt.setOpaque(false);
            nt.setLayout(new BoxLayout(nt, BoxLayout.Y_AXIS));
            nt.add(sourcePill("COACH NEXT MOVE", conf));
            nt.add(Box.createVerticalStrut(4));
            nt.add(label(shortText(str(top, "title", "Next step"), 28), 11, WHITE, Font.BOLD));
            String nextStep = str(top, "nextStep", "");
            if (!nextStep.isEmpty())
            {
                nt.add(Box.createVerticalStrut(2));
                nt.add(wrap(nextStep, MUTED, 9));
            }
            next.add(nt, BorderLayout.CENTER);
            home.add(next);
        }

        home.revalidate();
        home.repaint();
    }

    private JPanel coreSkillStrip()
    {
        JPanel grid = new JPanel(new GridLayout(2, 4, 4, 4));
        grid.setOpaque(false);
        grid.setAlignmentX(Component.LEFT_ALIGNMENT);
        String[] names = {"Attack", "Strength", "Defence", "Hitpoints", "Ranged", "Prayer", "Magic", "Slayer"};
        for (String name : names)
        {
            grid.add(miniSkill(name, liveSkillLevels.getOrDefault(name, 0)));
        }
        grid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 92));
        return grid;
    }

    private void rebuildSkills()
    {
        skills.removeAll();
        skills.add(sectionTitle("LIVE SKILL LEVELS", "RUNELITE LIVE"));
        skills.add(Box.createVerticalStrut(5));
        if (liveSkillLevels.isEmpty())
        {
            skills.add(wrap("Log into OSRS in this RuneLite client. Your current levels will populate here automatically.", MUTED, 10));
        }
        else
        {
            JPanel grid = new JPanel(new GridLayout(0, 2, 5, 5));
            grid.setOpaque(false);
            grid.setAlignmentX(Component.LEFT_ALIGNMENT);
            for (Map.Entry<String, Integer> entry : liveSkillLevels.entrySet())
            {
                grid.add(skillTile(entry.getKey(), entry.getValue()));
            }
            skills.add(grid);
        }
        skills.revalidate();
        skills.repaint();
    }

    private void rebuildToday(JPanel target, JsonObject plan, JsonArray milestones)
    {
        target.removeAll();
        target.add(sectionTitle("TODAY'S PLAN", "DISCORD COACH"));
        target.add(Box.createVerticalStrut(5));
        JsonArray tasks = array(plan, "tasks");
        if (tasks.size() == 0)
        {
            target.add(wrap("Refresh Coach to generate a session plan from your current goals, missions and Jagex stats.", MUTED, 10));
        }
        int i = 1;
        for (JsonElement element : tasks)
        {
            if (!element.isJsonPrimitive()) continue;
            TimelineStepCard step = new TimelineStepCard(i, stripMarkdown(element.getAsString()), i == 1 ? GOLD : BLUE, i == 1);
            target.add(step);
            target.add(Box.createVerticalStrut(6));
            if (++i > 4) break;
        }
        if (milestones.size() > 0)
        {
            target.add(Box.createVerticalStrut(2));
            target.add(sectionTitle("NEARBY MILESTONES", "JAGEX VERIFIED"));
            target.add(Box.createVerticalStrut(5));
            int shown = 0;
            for (JsonElement element : milestones)
            {
                if (!element.isJsonObject()) continue;
                JsonObject m = element.getAsJsonObject();
                MilestoneCard c = new MilestoneCard(
                    str(m, "skill", "Skill"),
                    integer(m, "level", 1),
                    integer(m, "target", 1),
                    str(m, "description", "")
                );
                target.add(c);
                target.add(Box.createVerticalStrut(5));
                if (++shown >= 4) break;
            }
        }
        target.revalidate();
        target.repaint();
    }

    private void rebuildRoadmap(JPanel target, JsonObject roadmapObj)
    {
        target.removeAll();
        target.add(sectionTitle("PROGRESSION ROADMAP", "DISCORD COACH"));
        target.add(Box.createVerticalStrut(5));
        addRoadmapSection(target, "NOW", array(roadmapObj, "quick"), GREEN);
        addRoadmapSection(target, "NEXT", array(roadmapObj, "next"), GOLD);
        addRoadmapSection(target, "LATER", array(roadmapObj, "long"), BLUE);
        target.revalidate();
        target.repaint();
    }

    private void addRoadmapSection(JPanel target, String name, JsonArray rows, Color accent)
    {
        target.add(new RoadmapLanePanel(name, rows, accent));
        target.add(Box.createVerticalStrut(6));
    }

    private void rebuildTasks(JPanel target, JsonArray rows, Color accent, String empty, String source)
    {
        target.removeAll();
        target.add(sectionTitle(source.contains("MISSION") ? "ACTIVE MISSIONS" : "ACTIVE GOALS", "DISCORD COACH"));
        target.add(Box.createVerticalStrut(5));
        if (rows.size() == 0)
        {
            target.add(wrap(empty, MUTED, 10));
        }
        for (JsonElement element : rows)
        {
            if (!element.isJsonObject()) continue;
            JsonObject t = element.getAsJsonObject();
            String tag = str(t, "period", "").isEmpty() ? str(t, "category", "progression") : str(t, "period", "task");
            TaskCard card = new TaskCard(
                source,
                str(t, "title", "Task"),
                tag,
                integer(t, "points", 0),
                integer(t, "percent", 0),
                str(t, "description", ""),
                accent
            );
            target.add(card);
            target.add(Box.createVerticalStrut(6));
        }
        target.revalidate();
        target.repaint();
    }

    private void rebuildTips(JPanel target, JsonArray rows)
    {
        target.removeAll();
        target.add(sectionTitle("PRECISION TIPS", "DISCORD COACH"));
        target.add(Box.createVerticalStrut(5));
        if (rows.size() == 0)
        {
            target.add(wrap("Press Refresh Coach after linking. Suggestions use Jagex stats plus your Discord unlock audit.", MUTED, 10));
        }
        int index = 1;
        for (JsonElement element : rows)
        {
            if (!element.isJsonObject()) continue;
            JsonObject t = element.getAsJsonObject();
            String confidenceName = str(t, "confidence", "suggested");
            Color confidence = confidenceColor(confidenceName);
            TipCard card = new TipCard(
                index,
                str(t, "title", "Next step"),
                str(t, "nextStep", ""),
                str(t, "tip", ""),
                confidenceName,
                confidence
            );
            target.add(card);
            target.add(Box.createVerticalStrut(6));
            if (++index > 6) break;
        }
        target.revalidate();
        target.repaint();
    }

    private static JPanel sectionTitle(String title, String source)
    {
        JPanel row = new JPanel(new BorderLayout(4, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        row.add(label(title, 10, GOLD_LIGHT, Font.BOLD), BorderLayout.WEST);
        row.add(label(source, 7, MUTED, Font.BOLD), BorderLayout.EAST);
        return row;
    }

    private static JPanel statTile(String title, String value, String source, Color accent)
    {
        return new MetricTile(title, value, source, accent);
    }

    private static JPanel miniSkill(String name, int level)
    {
        return new MiniSkillTile(name, level, skillAccent(name));
    }

    private static JPanel skillTile(String name, int level)
    {
        return new FullSkillTile(name, level, skillAccent(name));
    }

    private static JPanel roundedCard(Color accent)
    {
        RoundedPanel p = new RoundedPanel(PANEL, accent == null ? BORDER : accent);
        p.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        return p;
    }

    private static JLabel sourcePill(String text, Color accent)
    {
        PillLabel l = new PillLabel(text, accent);
        l.setFont(l.getFont().deriveFont(Font.BOLD, 10f));
        l.setForeground(accent);
        l.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
        return l;
    }

    private static JButton navButton(String text)
    {
        JButton b = new JButton(text);
        b.setFont(b.getFont().deriveFont(Font.BOLD, 10.5f));
        b.setForeground(MUTED);
        b.setBackground(PANEL_2);
        b.setFocusPainted(false);
        b.setMargin(new Insets(2, 2, 2, 2));
        b.setBorder(BorderFactory.createLineBorder(BORDER));
        return b;
    }

    private static JButton actionButton(String text, Color accent)
    {
        JButton b = new JButton(text);
        b.setFont(b.getFont().deriveFont(Font.BOLD, 10.5f));
        b.setForeground(WHITE);
        b.setBackground(PANEL_2);
        b.setFocusPainted(false);
        b.setMargin(new Insets(4, 5, 4, 5));
        b.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(accent.darker()),
            BorderFactory.createEmptyBorder(3, 5, 3, 5)));
        return b;
    }

    private static JPanel verticalPanel()
    {
        JPanel p = new ViewportWidthPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(BG);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        return p;
    }

    private static JScrollPane scroll(JPanel panel)
    {
        JScrollPane s = new JScrollPane(panel);
        s.setBorder(BorderFactory.createEmptyBorder(5, 2, 5, 2));
        s.getViewport().setBackground(BG);
        s.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        s.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        s.getVerticalScrollBar().setUnitIncrement(16);
        s.getVerticalScrollBar().setBlockIncrement(64);
        s.setWheelScrollingEnabled(true);
        return s;
    }

    private static JLabel label(String text, int size, Color color, int style)
    {
        JLabel l = new JLabel(text == null ? "" : text);
        l.setForeground(color);
        float readableSize = Math.max(10.5f, size + (size <= 10 ? 1.5f : 1.0f));
        l.setFont(l.getFont().deriveFont(style, readableSize));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private static JTextArea wrap(String text, Color color, int size)
    {
        JTextArea a = new JTextArea(text == null ? "" : text);
        a.setWrapStyleWord(true);
        a.setLineWrap(true);
        a.setEditable(false);
        a.setFocusable(false);
        a.setOpaque(false);
        a.setForeground(color);
        float readableSize = Math.max(11f, size + 1.5f);
        a.setFont(a.getFont().deriveFont(Font.PLAIN, readableSize));
        a.setBorder(null);
        a.setMargin(new Insets(0, 0, 0, 0));
        a.setAlignmentX(Component.LEFT_ALIGNMENT);
        a.setColumns(1);
        if (a.getCaret() instanceof DefaultCaret)
        {
            ((DefaultCaret) a.getCaret()).setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
        }
        a.setCaretPosition(0);
        a.setMinimumSize(new Dimension(0, 16));
        a.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1000));
        return a;
    }

    private static void clearPanel(JPanel p, String message)
    {
        p.removeAll();
        JPanel c = roundedCard(BORDER);
        c.setLayout(new BoxLayout(c, BoxLayout.Y_AXIS));
        c.add(wrap(message, MUTED, 10));
        p.add(c);
        p.revalidate();
        p.repaint();
    }

    private static JsonObject object(JsonObject parent, String key)
    {
        return parent != null && parent.has(key) && parent.get(key).isJsonObject()
            ? parent.getAsJsonObject(key) : new JsonObject();
    }

    private static JsonArray array(JsonObject parent, String key)
    {
        return parent != null && parent.has(key) && parent.get(key).isJsonArray()
            ? parent.getAsJsonArray(key) : new JsonArray();
    }

    private static String str(JsonObject obj, String key, String fallback)
    {
        try { return obj != null && obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsString() : fallback; }
        catch (Exception e) { return fallback; }
    }

    private static int integer(JsonObject obj, String key, int fallback)
    {
        try { return obj != null && obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsInt() : fallback; }
        catch (Exception e) { return fallback; }
    }

    private static long longValue(JsonObject obj, String key, long fallback)
    {
        try { return obj != null && obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsLong() : fallback; }
        catch (Exception e) { return fallback; }
    }

    private static String format(long n)
    {
        return NumberFormat.getIntegerInstance().format(Math.max(0L, n));
    }

    private static String stripMarkdown(String text)
    {
        return text == null ? "" : text.replace("**", "").replace("`", "");
    }

    private static String shortText(String value, int max)
    {
        String s = value == null ? "" : value.replaceAll("\\s+", " ").trim();
        return s.length() <= max ? s : s.substring(0, Math.max(0, max - 1)) + "…";
    }

    private static String shortSkill(String name)
    {
        if (name == null || name.isEmpty()) return "?";
        switch (name.toLowerCase())
        {
            case "attack": return "ATK";
            case "strength": return "STR";
            case "defence": return "DEF";
            case "hitpoints": return "HP";
            case "ranged": return "RNG";
            case "prayer": return "PRY";
            case "magic": return "MAG";
            case "cooking": return "CK";
            case "woodcutting": return "WC";
            case "fletching": return "FLT";
            case "fishing": return "FSH";
            case "firemaking": return "FM";
            case "crafting": return "CRF";
            case "smithing": return "SMT";
            case "mining": return "MIN";
            case "herblore": return "HERB";
            case "agility": return "AGI";
            case "thieving": return "THV";
            case "slayer": return "SLY";
            case "farming": return "FRM";
            case "runecraft": return "RC";
            case "hunter": return "HNT";
            case "construction": return "CON";
            default: return name.length() <= 3 ? name.toUpperCase() : name.substring(0, 3).toUpperCase();
        }
    }

    private static Color skillAccent(String name)
    {
        String n = name == null ? "" : name.toLowerCase();
        if (n.matches("attack|strength|defence|hitpoints|ranged|prayer|magic|slayer")) return RED;
        if (n.matches("mining|smithing|fishing|cooking|woodcutting|firemaking|farming|hunter")) return GREEN;
        if (n.matches("agility|thieving|runecraft|crafting|fletching|herblore|construction")) return BLUE;
        return GOLD;
    }

    private static Color confidenceColor(String confidence)
    {
        String c = confidence == null ? "" : confidence.toLowerCase();
        if (c.contains("live")) return GREEN;
        if (c.contains("member")) return BLUE;
        if (c.contains("infer")) return ORANGE;
        return GOLD;
    }

    private static final class ViewportWidthPanel extends JPanel implements Scrollable
    {
        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) { return 18; }
        @Override public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) { return Math.max(72, visibleRect.height - 36); }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }

    private static class RoundedPanel extends JPanel
    {
        private final Color fill;
        private final Color accent;
        RoundedPanel(Color fill, Color accent)
        {
            this.fill = fill;
            this.accent = accent;
            setOpaque(false);
        }
        @Override protected void paintComponent(Graphics g)
        {
            Graphics2D g2 = (Graphics2D) g.create();
            try
            {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(fill);
                g2.fill(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1f, getHeight() - 1f, 10f, 10f));
                g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 125));
                g2.setStroke(new BasicStroke(1f));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1f, getHeight() - 1f, 10f, 10f));
                g2.setColor(accent);
                g2.fillRoundRect(0, 8, 2, Math.max(0, getHeight() - 16), 2, 2);
            }
            finally { g2.dispose(); }
            super.paintComponent(g);
        }
    }

    private static final class GradientCard extends RoundedPanel
    {
        private final Color top;
        private final Color bottom;
        GradientCard(Color top, Color bottom, Color accent)
        {
            super(bottom, accent);
            this.top = top;
            this.bottom = bottom;
        }
        @Override protected void paintComponent(Graphics g)
        {
            Graphics2D g2 = (Graphics2D) g.create();
            try
            {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, top, getWidth(), getHeight(), bottom));
                g2.fill(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1f, getHeight() - 1f, 12f, 12f));
                g2.setColor(new Color(GOLD.getRed(), GOLD.getGreen(), GOLD.getBlue(), 150));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1f, getHeight() - 1f, 12f, 12f));
            }
            finally { g2.dispose(); }
        }
    }

    private static final class PillLabel extends JLabel
    {
        private final Color accent;
        PillLabel(String text, Color accent)
        {
            super(text);
            this.accent = accent;
            setOpaque(false);
        }
        @Override protected void paintComponent(Graphics g)
        {
            Graphics2D g2 = (Graphics2D) g.create();
            try
            {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 28));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 90));
                g2.drawRoundRect(0, 0, Math.max(0, getWidth() - 1), Math.max(0, getHeight() - 1), getHeight(), getHeight());
            }
            finally { g2.dispose(); }
            super.paintComponent(g);
        }
    }

    private static final class FancyBar extends JComponent
    {
        private int value;
        private final Color accent;
        private String caption = "";
        FancyBar(int value, Color accent)
        {
            this.value = Math.max(0, Math.min(100, value));
            this.accent = accent;
            setPreferredSize(new Dimension(180, 18));
            setMinimumSize(new Dimension(40, 18));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));
        }
        void setCaption(String caption) { this.caption = caption == null ? "" : caption; repaint(); }
        @Override protected void paintComponent(Graphics g)
        {
            Graphics2D g2 = (Graphics2D) g.create();
            try
            {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int h = getHeight();
                int w = getWidth();
                g2.setColor(PANEL_3);
                g2.fillRoundRect(0, 2, w, Math.max(1, h - 4), h, h);
                int fill = (int) Math.round(w * (value / 100.0));
                g2.setColor(accent);
                g2.fillRoundRect(0, 2, Math.max(0, fill), Math.max(1, h - 4), h, h);
                g2.setFont(getFont().deriveFont(Font.BOLD, 9.5f));
                FontMetrics fm = g2.getFontMetrics();
                String text = caption.isEmpty() ? value + "%" : caption;
                int x = Math.max(3, (w - fm.stringWidth(text)) / 2);
                int y = (h - fm.getHeight()) / 2 + fm.getAscent();
                g2.setColor(WHITE);
                g2.drawString(text, x, y);
            }
            finally { g2.dispose(); }
        }
    }

    private static final class RingMeter extends JComponent
    {
        private final int value;
        private final Color accent;
        private final String label;
        RingMeter(int value, Color accent, String label)
        {
            this.value = Math.max(0, Math.min(100, value));
            this.accent = accent;
            this.label = label;
            setPreferredSize(new Dimension(48, 64));
            setMinimumSize(new Dimension(40, 60));
        }
        @Override protected void paintComponent(Graphics g)
        {
            Graphics2D g2 = (Graphics2D) g.create();
            try
            {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int diameter = Math.min(getWidth() - 6, getHeight() - 20);
                int x = (getWidth() - diameter) / 2;
                int y = 1;
                g2.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.setColor(PANEL_3);
                g2.draw(new Arc2D.Float(x, y, diameter, diameter, 90, -360, Arc2D.OPEN));
                g2.setColor(accent);
                g2.draw(new Arc2D.Float(x, y, diameter, diameter, 90, -(360f * value / 100f), Arc2D.OPEN));
                g2.setFont(getFont().deriveFont(Font.BOLD, 11f));
                String pct = value + "%";
                FontMetrics fm = g2.getFontMetrics();
                g2.setColor(WHITE);
                g2.drawString(pct, (getWidth() - fm.stringWidth(pct)) / 2, y + diameter / 2 + fm.getAscent() / 3);
                g2.setFont(getFont().deriveFont(Font.BOLD, 9f));
                fm = g2.getFontMetrics();
                g2.setColor(MUTED);
                g2.drawString(label, (getWidth() - fm.stringWidth(label)) / 2, getHeight() - 3);
            }
            finally { g2.dispose(); }
        }
    }

    private static final class MetricTile extends RoundedPanel
    {
        MetricTile(String title, String value, String glyph, Color accent)
        {
            super(PANEL, accent);
            setBorder(BorderFactory.createEmptyBorder(6, 5, 6, 5));
            setLayout(new BorderLayout(4, 0));
            setAlignmentX(Component.LEFT_ALIGNMENT);

            MetricGlyph icon = new MetricGlyph(glyph, accent);
            icon.setPreferredSize(new Dimension(27, 31));
            add(icon, BorderLayout.WEST);

            JPanel text = new JPanel();
            text.setOpaque(false);
            text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
            text.add(label(title, 7, MUTED, Font.BOLD));
            text.add(label(value, 13, WHITE, Font.BOLD));
            add(text, BorderLayout.CENTER);
        }
    }

    private static final class MetricGlyph extends JComponent
    {
        private final String glyph;
        private final Color accent;
        MetricGlyph(String glyph, Color accent) { this.glyph = glyph; this.accent = accent; }
        @Override protected void paintComponent(Graphics g)
        {
            Graphics2D g2 = (Graphics2D) g.create();
            try
            {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int s = Math.min(getWidth(), getHeight()) - 2;
                int x = (getWidth() - s) / 2;
                int y = (getHeight() - s) / 2;
                g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 32));
                g2.fillOval(x, y, s, s);
                g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 150));
                g2.setStroke(new BasicStroke(1.4f));
                g2.drawOval(x, y, s, s);
                g2.setFont(getFont().deriveFont(Font.BOLD, glyph.length() > 2 ? 9f : 9.5f));
                FontMetrics fm = g2.getFontMetrics();
                g2.setColor(accent);
                g2.drawString(glyph, x + (s - fm.stringWidth(glyph)) / 2, y + s / 2 + fm.getAscent() / 3);
            }
            finally { g2.dispose(); }
        }
    }

    private static final class MiniSkillTile extends RoundedPanel
    {
        MiniSkillTile(String name, int level, Color accent)
        {
            super(PANEL, accent);
            setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
            setLayout(new BorderLayout(3, 0));
            LevelOrb orb = new LevelOrb(level, accent);
            orb.setPreferredSize(new Dimension(29, 29));
            add(orb, BorderLayout.WEST);
            JLabel n = label(shortSkill(name), 7, MUTED, Font.BOLD);
            n.setHorizontalAlignment(SwingConstants.CENTER);
            add(n, BorderLayout.CENTER);
        }
    }

    private static final class FullSkillTile extends RoundedPanel
    {
        FullSkillTile(String name, int level, Color accent)
        {
            super(PANEL, accent);
            setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
            setLayout(new BorderLayout(5, 0));
            SkillBadge badge = new SkillBadge(shortSkill(name), accent);
            badge.setPreferredSize(new Dimension(31, 31));
            add(badge, BorderLayout.WEST);

            JPanel body = new JPanel();
            body.setOpaque(false);
            body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
            body.add(label(shortText(name, 12), 8, MUTED, Font.BOLD));
            JPanel valueRow = new JPanel(new BorderLayout(3, 0));
            valueRow.setOpaque(false);
            valueRow.add(label(level <= 0 ? "—" : String.valueOf(level), 14, WHITE, Font.BOLD), BorderLayout.WEST);
            MiniLevelBar bar = new MiniLevelBar(level, accent);
            bar.setPreferredSize(new Dimension(38, 8));
            valueRow.add(bar, BorderLayout.CENTER);
            body.add(valueRow);
            add(body, BorderLayout.CENTER);
        }
    }

    private static final class LevelOrb extends JComponent
    {
        private final int level;
        private final Color accent;
        LevelOrb(int level, Color accent) { this.level = Math.max(0, level); this.accent = accent; }
        @Override protected void paintComponent(Graphics g)
        {
            Graphics2D g2 = (Graphics2D) g.create();
            try
            {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int s = Math.min(getWidth(), getHeight()) - 2;
                int x = (getWidth() - s) / 2;
                int y = (getHeight() - s) / 2;
                g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 30));
                g2.fillOval(x, y, s, s);
                g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.setColor(PANEL_3);
                g2.draw(new Arc2D.Float(x + 2, y + 2, s - 4, s - 4, 90, -360, Arc2D.OPEN));
                int pct = Math.min(100, Math.max(0, (int)Math.round(level / 99.0 * 100)));
                g2.setColor(accent);
                g2.draw(new Arc2D.Float(x + 2, y + 2, s - 4, s - 4, 90, -(360f * pct / 100f), Arc2D.OPEN));
                String t = level <= 0 ? "-" : String.valueOf(level);
                g2.setFont(getFont().deriveFont(Font.BOLD, t.length() > 2 ? 9f : 9.5f));
                FontMetrics fm = g2.getFontMetrics();
                g2.setColor(WHITE);
                g2.drawString(t, x + (s - fm.stringWidth(t)) / 2, y + s / 2 + fm.getAscent() / 3);
            }
            finally { g2.dispose(); }
        }
    }

    private static final class MiniLevelBar extends JComponent
    {
        private final int level;
        private final Color accent;
        MiniLevelBar(int level, Color accent) { this.level = Math.max(0, Math.min(99, level)); this.accent = accent; }
        @Override protected void paintComponent(Graphics g)
        {
            Graphics2D g2 = (Graphics2D) g.create();
            try
            {
                int y = Math.max(0, getHeight() / 2 - 2);
                g2.setColor(PANEL_3);
                g2.fillRoundRect(0, y, getWidth(), 4, 4, 4);
                int w = (int)Math.round(getWidth() * (level / 99.0));
                g2.setColor(accent);
                g2.fillRoundRect(0, y, Math.max(0, w), 4, 4, 4);
            }
            finally { g2.dispose(); }
        }
    }

    private static final class RankEmblem extends JComponent
    {
        private final String rank;
        private final Color accent;

        RankEmblem(String rank, Color accent)
        {
            this.rank = rank == null ? "Recruit" : rank;
            this.accent = accent;
        }

        private int tier()
        {
            String r = rank.toLowerCase();
            String[] ladder = {"recruit", "corporal", "sergeant", "lieutenant", "captain", "major", "colonel", "brigadier", "general", "marshal", "king"};
            for (int i = 0; i < ladder.length; i++)
            {
                if (r.contains(ladder[i]))
                {
                    return i;
                }
            }
            return 0;
        }

        @Override
        protected void paintComponent(Graphics g)
        {
            Graphics2D g2 = (Graphics2D) g.create();
            try
            {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();
                int cx = w / 2;
                int t = tier();

                g2.setColor(PANEL_3);
                g2.fillRoundRect(2, 2, Math.max(1, w - 5), Math.max(1, h - 5), 12, 12);
                g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 85));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(2, 2, Math.max(1, w - 5), Math.max(1, h - 5), 12, 12);

                g2.setColor(GOLD_LIGHT);
                g2.fillRoundRect(9, 7, Math.max(6, w - 18), 3, 3, 3);

                if (t <= 2)
                {
                    drawChevrons(g2, cx, 20, t + 1);
                }
                else if (t == 3 || t == 4)
                {
                    int bars = t == 3 ? 1 : 2;
                    for (int i = 0; i < bars; i++)
                    {
                        int x = cx - ((bars - 1) * 6) + i * 12 - 3;
                        g2.setColor(GOLD_LIGHT);
                        g2.fillRoundRect(x, 17, 6, 21, 3, 3);
                        g2.setColor(accent);
                        g2.drawRoundRect(x, 17, 6, 21, 3, 3);
                    }
                }
                else if (t == 5 || t == 6)
                {
                    Polygon diamond = new Polygon();
                    diamond.addPoint(cx, 15);
                    diamond.addPoint(cx + 11, 27);
                    diamond.addPoint(cx, 39);
                    diamond.addPoint(cx - 11, 27);
                    g2.setColor(t == 6 ? GOLD_LIGHT : accent);
                    g2.fillPolygon(diamond);
                    g2.setColor(t == 6 ? accent : GOLD_LIGHT);
                    g2.drawPolygon(diamond);
                    if (t == 6)
                    {
                        g2.drawLine(cx - 18, 27, cx - 11, 27);
                        g2.drawLine(cx + 11, 27, cx + 18, 27);
                    }
                }
                else if (t >= 7 && t <= 9)
                {
                    int stars = t - 6;
                    int spacing = 13;
                    int startX = cx - ((stars - 1) * spacing) / 2;
                    for (int i = 0; i < stars; i++)
                    {
                        drawStar(g2, startX + i * spacing, 27, 6, i == stars - 1 ? GOLD_LIGHT : accent);
                    }
                }
                else
                {
                    drawCrown(g2, cx, 27);
                }

                String shortRank = rankShort(rank);
                Font baseFont = getFont() == null ? new Font(Font.SANS_SERIF, Font.PLAIN, 12) : getFont();
                g2.setFont(baseFont.deriveFont(Font.BOLD, 9f));
                FontMetrics fm = g2.getFontMetrics();
                g2.setColor(MUTED);
                g2.drawString(shortRank, Math.max(3, cx - fm.stringWidth(shortRank) / 2), h - 7);
            }
            finally
            {
                g2.dispose();
            }
        }

        private static void drawChevrons(Graphics2D g2, int cx, int startY, int count)
        {
            g2.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int i = 0; i < count; i++)
            {
                int y = startY + i * 7;
                g2.setColor(i == count - 1 ? GOLD_LIGHT : GOLD);
                g2.drawLine(cx - 11, y, cx, y + 6);
                g2.drawLine(cx, y + 6, cx + 11, y);
            }
        }

        private static void drawStar(Graphics2D g2, int cx, int cy, int radius, Color color)
        {
            Polygon star = new Polygon();
            for (int i = 0; i < 10; i++)
            {
                double angle = -Math.PI / 2 + i * Math.PI / 5;
                double r = (i & 1) == 0 ? radius : radius * 0.43;
                star.addPoint((int)Math.round(cx + Math.cos(angle) * r), (int)Math.round(cy + Math.sin(angle) * r));
            }
            g2.setColor(color);
            g2.fillPolygon(star);
        }

        private static void drawCrown(Graphics2D g2, int cx, int cy)
        {
            Polygon crown = new Polygon();
            crown.addPoint(cx - 15, cy + 8);
            crown.addPoint(cx - 12, cy - 7);
            crown.addPoint(cx - 4, cy);
            crown.addPoint(cx, cy - 11);
            crown.addPoint(cx + 4, cy);
            crown.addPoint(cx + 12, cy - 7);
            crown.addPoint(cx + 15, cy + 8);
            g2.setColor(GOLD_LIGHT);
            g2.fillPolygon(crown);
            g2.setColor(GOLD);
            g2.drawPolygon(crown);
        }

        private static String rankShort(String value)
        {
            if (value == null || value.isBlank())
            {
                return "REC";
            }
            String upper = value.trim().toUpperCase();
            return upper.length() <= 4 ? upper : upper.substring(0, 4);
        }
    }

    private static final class MomentumBadge extends JComponent
    {
        private final int goals;
        private final int missions;
        private final int rankPercent;
        MomentumBadge(int goals, int missions, int rankPercent)
        {
            this.goals = Math.max(0, goals);
            this.missions = Math.max(0, missions);
            this.rankPercent = Math.max(0, Math.min(100, rankPercent));
        }
        @Override protected void paintComponent(Graphics g)
        {
            Graphics2D g2 = (Graphics2D) g.create();
            try
            {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int d = Math.min(getWidth(), getHeight()) - 8;
                int x = (getWidth() - d) / 2, y = (getHeight() - d) / 2;
                g2.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.setColor(PANEL_3);
                g2.draw(new Arc2D.Float(x, y, d, d, 90, -360, Arc2D.OPEN));
                g2.setColor(GOLD);
                g2.draw(new Arc2D.Float(x, y, d, d, 90, -(360f * rankPercent / 100f), Arc2D.OPEN));
                String value = String.valueOf(goals + missions);
                g2.setFont(getFont().deriveFont(Font.BOLD, 15f));
                FontMetrics fm = g2.getFontMetrics();
                g2.setColor(WHITE);
                g2.drawString(value, x + (d - fm.stringWidth(value)) / 2, y + d / 2 + fm.getAscent() / 3);
                g2.setFont(getFont().deriveFont(Font.BOLD, 9f));
                String txt = "ACTIVE";
                fm = g2.getFontMetrics();
                g2.setColor(MUTED);
                g2.drawString(txt, (getWidth() - fm.stringWidth(txt)) / 2, y + d + 7);
            }
            finally { g2.dispose(); }
        }
    }

    private static final class SessionGauge extends RoundedPanel
    {
        SessionGauge(long xp, long xpHour)
        {
            super(PANEL, BLUE);
            setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
            setLayout(new BorderLayout(8, 0));
            SessionPulse pulse = new SessionPulse(BLUE);
            pulse.setPreferredSize(new Dimension(48, 48));
            add(pulse, BorderLayout.WEST);
            JPanel center = new JPanel();
            center.setOpaque(false);
            center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
            center.add(sourcePill("RUNELITE LIVE", BLUE));
            center.add(Box.createVerticalStrut(4));
            center.add(label("+" + format(xp) + " XP", 17, GREEN, Font.BOLD));
            center.add(label(format(xpHour) + " XP/hr", 9, MUTED, Font.BOLD));
            add(center, BorderLayout.CENTER);
        }
    }

    private static final class SessionPulse extends JComponent
    {
        private final Color accent;
        SessionPulse(Color accent) { this.accent = accent; }
        @Override protected void paintComponent(Graphics g)
        {
            Graphics2D g2 = (Graphics2D) g.create();
            try
            {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 25));
                g2.fillOval(3, 3, w - 6, h - 6);
                g2.setColor(accent);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawOval(3, 3, w - 7, h - 7);
                int cy = h / 2;
                int[] xs = {8, 14, 19, 24, 29, 35, 40};
                int[] ys = {cy, cy, cy - 7, cy + 8, cy - 4, cy, cy};
                for (int i = 0; i < xs.length - 1; i++)
                    g2.drawLine(xs[i], ys[i], xs[i + 1], ys[i + 1]);
            }
            finally { g2.dispose(); }
        }
    }

    private static final class CoachGlyph extends JComponent
    {
        private final Color accent;
        CoachGlyph(Color accent) { this.accent = accent; }
        @Override protected void paintComponent(Graphics g)
        {
            Graphics2D g2 = (Graphics2D) g.create();
            try
            {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int cx = getWidth() / 2, cy = getHeight() / 2;
                int r = Math.min(getWidth(), getHeight()) / 2 - 4;
                g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 26));
                g2.fillOval(cx - r, cy - r, r * 2, r * 2);
                g2.setColor(accent);
                g2.drawOval(cx - r, cy - r, r * 2, r * 2);
                Polygon p = new Polygon();
                p.addPoint(cx, cy - r + 5);
                p.addPoint(cx + 5, cy - 2);
                p.addPoint(cx + r - 5, cy);
                p.addPoint(cx + 5, cy + 2);
                p.addPoint(cx, cy + r - 5);
                p.addPoint(cx - 5, cy + 2);
                p.addPoint(cx - r + 5, cy);
                p.addPoint(cx - 5, cy - 2);
                g2.setColor(GOLD_LIGHT);
                g2.fillPolygon(p);
                g2.setColor(RED);
                g2.fillOval(cx - 2, cy - 2, 4, 4);
            }
            finally { g2.dispose(); }
        }
    }

    private static final class TimelineStepCard extends RoundedPanel
    {
        TimelineStepCard(int step, String text, Color accent, boolean primary)
        {
            super(PANEL, accent);
            setBorder(BorderFactory.createEmptyBorder(8, 7, 8, 7));
            setLayout(new BorderLayout(8, 0));
            StepMarker marker = new StepMarker(step, accent, primary);
            marker.setPreferredSize(new Dimension(35, 48));
            add(marker, BorderLayout.WEST);
            JPanel body = new JPanel();
            body.setOpaque(false);
            body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
            body.add(sourcePill(primary ? "PRIMARY" : "STEP " + step, accent));
            body.add(Box.createVerticalStrut(4));
            body.add(wrap(text, WHITE, 10));
            add(body, BorderLayout.CENTER);
        }
    }

    private static final class StepMarker extends JComponent
    {
        private final int step;
        private final Color accent;
        private final boolean primary;
        StepMarker(int step, Color accent, boolean primary) { this.step = step; this.accent = accent; this.primary = primary; }
        @Override protected void paintComponent(Graphics g)
        {
            Graphics2D g2 = (Graphics2D) g.create();
            try
            {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int cx = getWidth() / 2;
                g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 45));
                g2.fillOval(cx - 12, 4, 24, 24);
                g2.setColor(accent);
                g2.setStroke(new BasicStroke(primary ? 2f : 1.2f));
                g2.drawOval(cx - 12, 4, 24, 24);
                String n = String.valueOf(step);
                g2.setFont(getFont().deriveFont(Font.BOLD, 11f));
                FontMetrics fm = g2.getFontMetrics();
                g2.setColor(WHITE);
                g2.drawString(n, cx - fm.stringWidth(n) / 2, 20);
                g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 100));
                g2.drawLine(cx, 30, cx, getHeight());
            }
            finally { g2.dispose(); }
        }
    }

    private static final class MilestoneCard extends RoundedPanel
    {
        MilestoneCard(String skill, int level, int target, String description)
        {
            super(PANEL, BLUE);
            setBorder(BorderFactory.createEmptyBorder(7, 7, 7, 7));
            setLayout(new BorderLayout(7, 0));
            SkillBadge badge = new SkillBadge(shortSkill(skill), skillAccent(skill));
            badge.setPreferredSize(new Dimension(35, 35));
            add(badge, BorderLayout.WEST);
            JPanel body = new JPanel();
            body.setOpaque(false);
            body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
            body.add(label(shortText(skill, 18) + "  " + level + " → " + target, 10, WHITE, Font.BOLD));
            int pct = target <= 0 ? 0 : Math.max(0, Math.min(100, (int)Math.round(level * 100.0 / target)));
            FancyBar bar = new FancyBar(pct, skillAccent(skill));
            bar.setCaption(level + " / " + target);
            body.add(Box.createVerticalStrut(3));
            body.add(bar);
            if (description != null && !description.isBlank())
            {
                body.add(Box.createVerticalStrut(3));
                body.add(wrap(description, MUTED, 9));
            }
            add(body, BorderLayout.CENTER);
        }
    }

    private static final class RoadmapLanePanel extends RoundedPanel
    {
        RoadmapLanePanel(String name, JsonArray rows, Color accent)
        {
            super(PANEL, accent);
            setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            JPanel header = new JPanel(new BorderLayout(4, 0));
            header.setOpaque(false);
            header.add(sourcePill(name, accent), BorderLayout.WEST);
            header.add(label(name.equals("NOW") ? "FIRST" : name.equals("NEXT") ? "QUEUE" : "LATER", 9, MUTED, Font.BOLD), BorderLayout.EAST);
            add(header);
            add(Box.createVerticalStrut(6));
            if (rows.size() == 0)
            {
                add(wrap("Refresh Coach to build this roadmap lane.", MUTED, 9));
                return;
            }
            int i = 0;
            for (JsonElement element : rows)
            {
                if (!element.isJsonPrimitive()) continue;
                RoadmapNode node = new RoadmapNode(++i, stripMarkdown(element.getAsString()), accent, i == 1);
                add(node);
                if (i >= 4) break;
                add(Box.createVerticalStrut(4));
            }
        }
    }

    private static final class RoadmapNode extends JPanel
    {
        RoadmapNode(int number, String text, Color accent, boolean first)
        {
            setOpaque(false);
            setLayout(new BorderLayout(6, 0));
            JLabel node = new JLabel(String.valueOf(number), SwingConstants.CENTER);
            node.setOpaque(true);
            node.setBackground(first ? accent : PANEL_3);
            node.setForeground(first ? BG : accent);
            node.setFont(node.getFont().deriveFont(Font.BOLD, 9f));
            node.setBorder(BorderFactory.createLineBorder(accent.darker()));
            node.setPreferredSize(new Dimension(22, 22));
            add(node, BorderLayout.WEST);
            add(wrap(text, WHITE, 9), BorderLayout.CENTER);
        }
    }

    private static final class TaskCard extends RoundedPanel
    {
        TaskCard(String source, String title, String tag, int points, int percent, String description, Color accent)
        {
            super(PANEL, accent);
            setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            JPanel top = new JPanel(new BorderLayout(4, 0));
            top.setOpaque(false);
            top.add(sourcePill(source, accent), BorderLayout.WEST);
            top.add(label("+" + points + " EE", 9, GOLD_LIGHT, Font.BOLD), BorderLayout.EAST);
            add(top);
            add(Box.createVerticalStrut(5));

            JPanel center = new JPanel(new BorderLayout(7, 0));
            center.setOpaque(false);
            ProgressOrb orb = new ProgressOrb(percent, accent);
            orb.setPreferredSize(new Dimension(48, 48));
            center.add(orb, BorderLayout.WEST);
            JPanel text = new JPanel();
            text.setOpaque(false);
            text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
            text.add(label(shortText(title, 31), 11, WHITE, Font.BOLD));
            text.add(label(tag.toUpperCase(), 9, MUTED, Font.BOLD));
            center.add(text, BorderLayout.CENTER);
            add(center);
            add(Box.createVerticalStrut(5));
            FancyBar bar = new FancyBar(percent, accent);
            bar.setCaption(percent + "% COMPLETE");
            add(bar);
            if (description != null && !description.isBlank())
            {
                add(Box.createVerticalStrut(5));
                add(wrap(description, MUTED, 9));
            }
        }
    }

    private static final class ProgressOrb extends JComponent
    {
        private final int percent;
        private final Color accent;
        ProgressOrb(int percent, Color accent) { this.percent = Math.max(0, Math.min(100, percent)); this.accent = accent; }
        @Override protected void paintComponent(Graphics g)
        {
            Graphics2D g2 = (Graphics2D) g.create();
            try
            {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int d = Math.min(getWidth(), getHeight()) - 6;
                int x = (getWidth() - d) / 2, y = (getHeight() - d) / 2;
                g2.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.setColor(PANEL_3);
                g2.draw(new Arc2D.Float(x, y, d, d, 90, -360, Arc2D.OPEN));
                g2.setColor(accent);
                g2.draw(new Arc2D.Float(x, y, d, d, 90, -(360f * percent / 100f), Arc2D.OPEN));
                String value = percent + "%";
                g2.setFont(getFont().deriveFont(Font.BOLD, 9f));
                FontMetrics fm = g2.getFontMetrics();
                g2.setColor(WHITE);
                g2.drawString(value, x + (d - fm.stringWidth(value)) / 2, y + d / 2 + fm.getAscent() / 3);
            }
            finally { g2.dispose(); }
        }
    }

    private static final class TipCard extends RoundedPanel
    {
        TipCard(int index, String title, String next, String tip, String confidence, Color accent)
        {
            super(PANEL, accent);
            setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
            setLayout(new BorderLayout(8, 0));
            CoachGlyph glyph = new CoachGlyph(accent);
            glyph.setPreferredSize(new Dimension(40, 44));
            add(glyph, BorderLayout.WEST);
            JPanel body = new JPanel();
            body.setOpaque(false);
            body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
            JPanel top = new JPanel(new BorderLayout(4, 0));
            top.setOpaque(false);
            top.add(sourcePill(confidence.toUpperCase(), accent), BorderLayout.WEST);
            top.add(label("#" + index, 9, MUTED, Font.BOLD), BorderLayout.EAST);
            body.add(top);
            body.add(Box.createVerticalStrut(4));
            body.add(label(shortText(title, 28), 11, WHITE, Font.BOLD));
            if (next != null && !next.isBlank())
            {
                body.add(Box.createVerticalStrut(3));
                body.add(wrap(next, WHITE, 9));
            }
            if (tip != null && !tip.isBlank())
            {
                body.add(Box.createVerticalStrut(4));
                body.add(wrap("TIP  " + tip, MUTED, 9));
            }
            add(body, BorderLayout.CENTER);
        }
    }

    private static final class SkillBadge extends JComponent
    {
        private final String text;
        private final Color accent;
        SkillBadge(String text, Color accent) { this.text = text; this.accent = accent; }
        @Override protected void paintComponent(Graphics g)
        {
            Graphics2D g2 = (Graphics2D) g.create();
            try
            {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int s = Math.min(getWidth(), getHeight()) - 2;
                int x = (getWidth() - s) / 2;
                int y = (getHeight() - s) / 2;
                g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 35));
                g2.fillRoundRect(x, y, s, s, 8, 8);
                g2.setColor(accent);
                g2.drawRoundRect(x, y, s, s, 8, 8);
                g2.setFont(getFont().deriveFont(Font.BOLD, text.length() > 3 ? 9f : 9.5f));
                FontMetrics fm = g2.getFontMetrics();
                g2.setColor(WHITE);
                g2.drawString(text, x + (s - fm.stringWidth(text)) / 2, y + s / 2 + fm.getAscent() / 3);
            }
            finally { g2.dispose(); }
        }
    }
}
