package com.eliteexiles.companion;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.JTextArea;
import net.runelite.client.util.LinkBrowser;

/**
 * Elite Exiles Companion 2.0 — Exile HQ.
 *
 * This panel is intentionally informational/planning focused. It does not inject
 * game input, mark tiles, predict attacks, alter menus, or automate gameplay.
 * Optional network features remain behind the existing explicit Coach Integration
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
        pages.put("COACH", coach);
        pages.put("PLAN", plan);
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
        refreshButton.addActionListener(e -> { if (controller != null) controller.refreshCoachFromPanel(); });
        checkinButton.addActionListener(e -> { if (controller != null) controller.checkInFromPanel(); });
        diagnosticsButton.addActionListener(e -> { if (controller != null) controller.runDiagnosticsFromPanel(); });
        unlinkButton.addActionListener(e -> { if (controller != null) controller.unlinkFromPanel(); });
        askCoachButton.addActionListener(e -> submitCoachQuestion());
        coachInput.addActionListener(e -> submitCoachQuestion());

        selectPage("HQ");
        setLocalMode();
    }

    public void setController(EliteExilesCompanionPlugin controller)
    {
        this.controller = controller;
    }

    public void openCoachPage()
    {
        selectPage("COACH");
    }

    public void askCoachFromCommand(String question)
    {
        sendCoachQuestion(question, currentGoalSubject());
    }

    public void showCoachCommandHelp()
    {
        SwingUtilities.invokeLater(() -> {
            selectPage("COACH");
            addThreadBubble("COACH", "Type !coach followed by a specific OSRS question. The command is consumed locally and the answer appears here; it is not posted into game chat.", new Color(38, 33, 28), SILVER);
        });
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

    private void submitCoachQuestion()
    {
        String question = coachInput.getText() == null ? "" : coachInput.getText().trim();
        sendCoachQuestion(question, coachSubject);
    }

    private void sendCoachQuestion(String question, String subject)
    {
        String clean = question == null ? "" : question.trim();
        if (clean.length() < 2)
        {
            setError("Type a specific OSRS question first.");
            return;
        }
        if (controller == null)
        {
            return;
        }
        selectPage("COACH");
        if (lastDashboard != null && !booleanValue(object(lastDashboard, "membership"), "verified", false))
        {
            addThreadBubble("COACH", "Coach access unlocks after Discord membership, staff approval, and current Elite Exiles in-game clan membership are verified. Use REQUEST CLAN ACCESS in the Clan tab.", PANEL_2, GOLD);
            return;
        }
        askCoachButton.setEnabled(false);
        coachInput.setEnabled(false);
        addThreadBubble("YOU", clean, PANEL_3, WHITE);
        controller.askCoachFromPanel(clean, subject == null ? "" : subject);
    }

    private String currentGoalSubject()
    {
        if (lastDashboard == null) return coachSubject;
        JsonObject coachObj = object(lastDashboard, "coach");
        String goal = str(coachObj, "personalGoal", "");
        if (!goal.isBlank()) return goal;
        return coachSubject;
    }

    @Override
    protected void rebuildCoach(JsonObject d)
    {
        super.rebuildCoach(d);
        coachInput.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        coachInput.setFont(coachInput.getFont().deriveFont(Font.PLAIN, 16f));
    }

    @Override
    protected void requestSessionPlan(int minutes)
    {
        sendCoachQuestion(
            "Build me a " + minutes + "-minute session plan for my current goal using my current stats. "
                + "Prioritize the closest useful blocker or next step and keep the plan practical.",
            currentGoalSubject());
    }

    @Override
    protected void requestBlockerAnalysis()
    {
        sendCoachQuestion(
            "What blockers or missing requirements should I handle for my current goal? "
                + "Use my current stats, separate hard requirements from recommendations, and tell me the best next step.",
            currentGoalSubject());
    }

    public void showCoachAnswer(String question, JsonObject response)
    {
        SwingUtilities.invokeLater(() -> {
            String answer = str(response, "answer", "The Coach returned no answer text.");
            coachSubject = str(response, "primaryEntity", coachSubject);
            String answerMode = str(response, "mode", "").toLowerCase();
            boolean aiUsed = booleanValue(response, "aiUsed", answerMode.contains("ai"));
            boolean cached = booleanValue(response, "cached", false);
            String coachLabel = aiUsed ? "COACH • AI ASSISTED" : (answerMode.contains("recent") ? "COACH • CONTEXT" : (cached ? "COACH • CACHED" : "COACH • TRUSTED SOURCES"));
            addThreadBubble(coachLabel, answer, new Color(38, 33, 28), SILVER);
            String note = str(response, "researchNote", "");
            if (!note.isBlank()) addThreadBubble("SOURCE NOTE", note, PANEL_2, AMBER);
            JsonArray sources = array(response, "sources");
            if (sources.size() > 0)
            {
                StringBuilder sb = new StringBuilder("Sources: ");
                int count = 0;
                for (JsonElement e : sources)
                {
                    if (!e.isJsonObject()) continue;
                    if (count++ > 0) sb.append(" • ");
                    sb.append(str(e.getAsJsonObject(), "title", "OSRS source"));
                    if (count >= 3) break;
                }
                addThreadBubble("RESEARCH", sb.toString(), PANEL_2, MUTED);
            }
            coachInput.setText("");
            coachInput.setEnabled(true);
            askCoachButton.setEnabled(true);
            connection.setText("COACH READY");
            connection.setForeground(GREEN);
            coach.revalidate();
            coach.repaint();
        });
    }

    public void showCoachError(String message)
    {
        SwingUtilities.invokeLater(() -> {
            addThreadBubble("COACH", message == null ? "Coach request failed." : message, PANEL_2, AMBER);
            coachInput.setEnabled(true);
            askCoachButton.setEnabled(true);
            connection.setText("COACH ISSUE");
            connection.setForeground(AMBER);
        });
    }

    @Override
    protected void addThreadBubble(String who, String message, Color background, Color foreground)
    {
        JPanel bubble = new RoundedPanel(background, who.equals("YOU") ? PURPLE : BORDER, 12);
        bubble.setLayout(new BoxLayout(bubble, BoxLayout.Y_AXIS));
        bubble.add(text(who, 12, who.equals("YOU") ? PURPLE_LIGHT : GOLD, Font.BOLD));
        bubble.add(vGap(5));
        JTextArea messageArea = wrap(message, foreground, 16);
        messageArea.setRows(rowsFor(message, 22, 120));
        bubble.add(messageArea);
        bubble.setAlignmentX(Component.LEFT_ALIGNMENT);
        bubble.setMaximumSize(new Dimension(Integer.MAX_VALUE, 2200));
        coachThread.add(bubble);
        coachThread.add(vGap(9));
        while (coachThread.getComponentCount() > MAX_COACH_THREAD_COMPONENTS)
        {
            coachThread.remove(0);
            if (coachThread.getComponentCount() > 0) coachThread.remove(0);
        }
        coachThread.revalidate();
        coachThread.repaint();
        scrollPageToBottom("COACH");
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
            ScrollState scrollState = captureScrollState();
            String incoming = rsn == null ? "" : rsn;
            long xp = Math.max(0L, sessionXp);
            if (!incoming.equalsIgnoreCase(sessionRsn) || xp < localSessionXp)
            {
                sessionStartedAt = System.currentTimeMillis();
                sessionRsn = incoming;
            }
            localRsn = incoming;
            localSessionXp = xp;
            liveSkillLevels.clear();
            if (skillLevels != null) liveSkillLevels.putAll(skillLevels);
            if (lastDashboard != null)
            {
                rebuildProfile(lastDashboard);
                rebuildHq(lastDashboard);
            }
            else
            {
                rebuildLocalProfile();
                rebuildLocalHq();
            }
            restoreScrollState(scrollState);
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
            statusDetail.setText("Coach Integration is off. Enable it in RuneLite plugin settings, then use /runelitelink below.");
            statusDetail.setForeground(MUTED);
            linkCard.setVisible(true);
            linkCode.setEnabled(false);
            linkButton.setText("STEP 1: ENABLE COACH FIRST");
            linkButton.setEnabled(false);
            refreshButton.setEnabled(false);
            checkinButton.setEnabled(false);
            diagnosticsButton.setEnabled(false);
            unlinkButton.setEnabled(false);
            refreshButton.setVisible(false);
            checkinButton.setVisible(false);
            diagnosticsButton.setVisible(false);
            unlinkButton.setVisible(false);
            rebuildLocalProfile();
            rebuildLocalHq();
            rebuildCoachUnlinked("Enable Coach Integration in plugin settings to use Coach Chat and synced recommendations.");
            rebuildPlanUnlinked("Link Coach to turn your Elite Exiles goals into a clean NOW → NEXT → LATER plan.");
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
                linkButton.setText("CONNECT COACH");
                linkButton.setEnabled(true);
            }
        });
    }

    public void setDisconnected(String message)
    {
        SwingUtilities.invokeLater(() -> {
            lastDashboard = null;
            connection.setText("COACH NOT LINKED");
            connection.setForeground(PURPLE_LIGHT);
            statusDetail.setText(message == null ? "Run /runelitelink in Discord, paste the code, then Connect Coach." : message);
            statusDetail.setForeground(MUTED);
            linkCard.setVisible(true);
            linkCode.setEnabled(true);
            linkButton.setText("CONNECT COACH");
            linkButton.setEnabled(true);
            refreshButton.setEnabled(false);
            checkinButton.setEnabled(false);
            diagnosticsButton.setEnabled(false);
            unlinkButton.setEnabled(false);
            refreshButton.setVisible(false);
            checkinButton.setVisible(false);
            diagnosticsButton.setVisible(false);
            unlinkButton.setVisible(false);
            rebuildLocalProfile();
            rebuildLocalHq();
            rebuildCoachUnlinked("Connect Coach above to ask account-aware OSRS questions directly inside RuneLite.");
            rebuildPlanUnlinked("Connect Coach above to load goals, blockers, session plan, missions and roadmap.");
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
                setError("Coach dashboard was missing member data.");
                return;
            }
            lastDashboard = d.deepCopy();
            JsonObject rl = object(d, "runelite");
            JsonObject membership = object(d, "membership");
            long lastSeen = longValue(rl, "lastSeenAt", 0L);
            boolean fresh = lastSeen > 0 && System.currentTimeMillis() - lastSeen < 120_000L;
            boolean verified = booleanValue(membership, "verified", false);
            connection.setText(verified ? (fresh ? "VERIFIED EXILE • LIVE" : "VERIFIED EXILE") : "CLAN ACCESS CHECK");
            connection.setForeground(verified ? GREEN : GOLD);
            statusDetail.setText(verified
                ? (fresh ? "Discord + Elite Exiles in-game membership verified." : "Membership verified. Refresh for the newest account snapshot.")
                : membershipStatusLine(membership));
            statusDetail.setForeground(verified ? GREEN : GOLD);
            linkCard.setVisible(false);
            linkCode.setText("");
            linkCode.setEnabled(false);
            linkButton.setText("CONNECT COACH");
            linkButton.setEnabled(false);
            refreshButton.setEnabled(verified);
            checkinButton.setEnabled(verified);
            diagnosticsButton.setEnabled(true);
            unlinkButton.setEnabled(true);
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
