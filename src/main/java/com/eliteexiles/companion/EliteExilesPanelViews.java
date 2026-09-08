package com.eliteexiles.companion;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.LinkBrowser;

/**
 * Exile HQ view construction and dashboard rendering.
 * Kept separate from controller/event handling to make Plugin Hub review easier.
 */
abstract class EliteExilesPanelViews extends EliteExilesPanelSupport
{
    protected EliteExilesPanelViews(boolean wrap)
    {
        super(wrap);
    }

    protected JPanel buildHero()
    {
        GradientCard hero = new GradientCard(new Color(44, 37, 29), new Color(20, 18, 15), BRONZE);
        hero.setLayout(new BorderLayout(9, 0));
        hero.setBorder(BorderFactory.createEmptyBorder(9, 9, 9, 10));
        hero.setAlignmentX(Component.LEFT_ALIGNMENT);
        hero.setMaximumSize(new Dimension(Integer.MAX_VALUE, 92));

        ImageIcon logo = new ImageIcon(ImageUtil.loadImageResource(getClass(), "header_logo.png"));
        JPanel crestFrame = new CrestFrame(logo);
        crestFrame.setPreferredSize(new Dimension(58, 58));
        hero.add(crestFrame, BorderLayout.WEST);

        JPanel copy = transparent();
        copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
        copy.add(text("ELITE EXILES", 18, WHITE, Font.BOLD));
        copy.add(text("EXILE HQ", 12, GOLD, Font.BOLD));
        copy.add(vGap(1));
        copy.add(text("MORE THAN A CLAN", 10, MUTED, Font.BOLD));
        copy.add(vGap(5));
        copy.add(connection);
        hero.add(copy, BorderLayout.CENTER);
        return hero;
    }

    protected void buildLinkCard()
    {
        // The onboarding card is intentionally taller than ordinary information cards.
        // Do not squeeze the setup instructions into the normal 280 px card cap.
        linkCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 640));
        linkCard.setLayout(new BoxLayout(linkCard, BoxLayout.Y_AXIS));
        linkCard.add(section("CONNECT ELITE EXILES", "30-SECOND SETUP"));
        linkCard.add(vGap(5));
        JTextArea setupIntro = wrap("One-time setup. No server address, RSN entry, API key, or token handling.", SILVER, 11);
        setupIntro.setRows(2);
        setupIntro.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        linkCard.add(setupIntro);
        linkCard.add(vGap(6));

        JPanel firstSteps = transparent(new GridLayout(2, 1, 0, 5));
        firstSteps.setAlignmentX(Component.LEFT_ALIGNMENT);
        firstSteps.setMaximumSize(new Dimension(1000, 184));
        firstSteps.add(setupStep(1, "ENABLE ELITE EXILES SYNC",
            "RuneLite Settings ⚙ → Elite Exiles Companion → Enable Elite Exiles Sync.", "settings", PURPLE));
        firstSteps.add(setupStep(2, "OPEN DISCORD",
            "Click the purple Discord button below. It opens the official Elite Exiles invite.", "discord", GOLD));
        linkCard.add(firstSteps);
        linkCard.add(vGap(5));

        joinDiscordButton.setText("OPEN ELITE EXILES DISCORD");
        joinDiscordButton.setToolTipText(DISCORD_INVITE_URL);
        linkCard.add(fullWidth(joinDiscordButton, 36));
        linkCard.add(vGap(3));
        JLabel invite = text(DISCORD_INVITE_URL, 10, PURPLE_LIGHT, Font.PLAIN);
        invite.setHorizontalAlignment(SwingConstants.CENTER);
        linkCard.add(fullWidth(invite, 18));
        linkCard.add(vGap(5));

        JPanel lastSteps = transparent(new GridLayout(2, 1, 0, 5));
        lastSteps.setAlignmentX(Component.LEFT_ALIGNMENT);
        lastSteps.setMaximumSize(new Dimension(1000, 184));
        lastSteps.add(setupStep(3, "RUN /runelitelink",
            "In Discord, type /runelitelink and copy the 8-character code it gives you.", "link", PURPLE));
        lastSteps.add(setupStep(4, "PASTE CODE + CONNECT",
            "Paste the code below while logged into that OSRS character, then click Connect Elite Exiles.", "check", GREEN));
        linkCard.add(lastSteps);
        linkCard.add(vGap(6));

        JLabel codeLabel = text("8-CHARACTER LINK CODE", 10, GOLD, Font.BOLD);
        codeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        linkCard.add(codeLabel);
        linkCard.add(vGap(3));

        linkCode.setBackground(PANEL_3);
        linkCode.setForeground(WHITE);
        linkCode.setCaretColor(WHITE);
        linkCode.setHorizontalAlignment(SwingConstants.CENTER);
        linkCode.setFont(linkCode.getFont().deriveFont(Font.BOLD, 16f));
        linkCode.setToolTipText("Paste the one-time code from /runelitelink");
        linkCode.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BRONZE, 1),
            BorderFactory.createEmptyBorder(5, 7, 5, 7)));
        linkCard.add(fullWidth(linkCode, 38));
        linkCard.add(vGap(6));
        linkCard.add(fullWidth(linkButton, 36));
        linkCard.add(vGap(6));
        statusDetail.setRows(2);
        statusDetail.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        statusDetail.setAlignmentX(Component.LEFT_ALIGNMENT);
        linkCard.add(statusDetail);
    }

    protected JPanel buildNavigation()
    {
        // COMPETE is longer than the other tab labels. Give it a little more of
        // the same row instead of shrinking/truncating the text or reducing the font.
        JPanel nav = transparent(new GridBagLayout());
        nav.setAlignmentX(Component.LEFT_ALIGNMENT);
        nav.setMaximumSize(new Dimension(Integer.MAX_VALUE, 31));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;

        String[] names = {"HQ", "GROUP", "COMPETE", "CLAN"};
        for (int i = 0; i < names.length; i++)
        {
            String name = names[i];
            JButton b = navButton(name);
            b.addActionListener(e -> selectPage(name));
            navButtons.put(name, b);

            gbc.gridx = i;
            gbc.weightx = "COMPETE".equals(name) ? 1.35 : ("GROUP".equals(name) ? 1.0 : ("CLAN".equals(name) ? 0.85 : 0.70));
            gbc.insets = new Insets(0, 0, 0, i < names.length - 1 ? 4 : 0);
            nav.add(b, gbc);
        }
        return nav;
    }

    protected void selectPage(String page)
    {
        ((CardLayout) contentHost.getLayout()).show(contentHost, page);
        for (Map.Entry<String, JButton> entry : navButtons.entrySet())
        {
            boolean active = entry.getKey().equals(page);
            JButton b = entry.getValue();
            if (b instanceof PremiumButton) ((PremiumButton) b).setActive(active);
            b.setForeground(active ? WHITE : MUTED);
        }
    }


    protected void rebuildLocalProfile()
    {
        profileHost.removeAll();
        JPanel c = card(BORDER);
        c.setLayout(new BoxLayout(c, BoxLayout.Y_AXIS));
        c.add(pill("RUNELITE LIVE", BLUE));
        c.add(vGap(5));
        c.add(text(localRsn.isBlank() ? "LOG INTO OSRS" : localRsn, 16, WHITE, Font.BOLD));
        c.add(vGap(2));
        c.add(text("Session +" + format(localSessionXp) + " XP", 11, GREEN, Font.BOLD));
        c.add(vGap(4));
        c.add(wrap("Local tracking stays available even when Elite Exiles Sync is disabled.", MUTED, 10));
        profileHost.add(c, BorderLayout.CENTER);
        profileHost.revalidate();
        profileHost.repaint();
    }

    protected void rebuildProfile(JsonObject d)
    {
        profileHost.removeAll();
        JsonObject member = object(d, "member");
        JsonObject rank = object(d, "rank");
        JsonObject coachObj = object(d, "coach");

        JPanel c = card(PURPLE);
        c.setLayout(new BoxLayout(c, BoxLayout.Y_AXIS));
        JPanel top = transparent(new BorderLayout(8, 0));
        top.add(new RankBadge(str(rank, "name", "Recruit"), integer(rank, "percent", 0)), BorderLayout.WEST);
        JPanel identity = transparent();
        identity.setLayout(new BoxLayout(identity, BoxLayout.Y_AXIS));
        identity.add(text(str(member, "rsn", localRsn), 16, WHITE, Font.BOLD));
        identity.add(text(str(rank, "name", "Recruit").toUpperCase(), 10, PURPLE_LIGHT, Font.BOLD));
        identity.add(text(str(member, "mode", "main").toUpperCase() + " • " + str(coachObj, "stage", "learning").toUpperCase(), 9, MUTED, Font.BOLD));
        top.add(identity, BorderLayout.CENTER);
        c.add(top);
        c.add(vGap(7));
        c.add(text(format(integer(rank, "points", 0)) + " EE POINTS", 11, SILVER, Font.BOLD));
        JsonObject eeIdentity = object(d, "identity");
        JsonObject eeTheme = object(eeIdentity, "theme");
        if (eeIdentity.size() > 0)
        {
            c.add(vGap(2));
            c.add(text(str(eeTheme, "name", "Classic Exile").toUpperCase() + " • " + format(integer(eeIdentity, "shopBalance", 0)) + " EE SHOP BALANCE", 9, PURPLE_LIGHT, Font.BOLD));
        }
        c.add(vGap(4));
        ProgressBar bar = new ProgressBar(integer(rank, "percent", 0), PURPLE);
        String next = str(rank, "next", "Top rank");
        bar.setCaption(integer(rank, "percent", 0) + "% → " + next + (integer(rank, "remaining", 0) > 0 ? " • " + format(integer(rank, "remaining", 0)) + " EE" : ""));
        c.add(bar);
        profileHost.add(c, BorderLayout.CENTER);
        profileHost.revalidate();
        profileHost.repaint();
    }

    protected void rebuildLocalHq()
    {
        hq.removeAll();
        hq.add(section("LOCAL SESSION", "RUNELITE"));
        hq.add(vGap(5));
        hq.add(sessionCard());
        hq.add(vGap(7));
        hq.add(infoCard("UNLOCK FULL HQ", "Enable Elite Exiles Sync in settings, then use the Discord link card above. Your local session data remains private until you opt in.", PURPLE, "link"));
        hq.revalidate();
        hq.repaint();
    }

    protected void rebuildHq(JsonObject d)
    {
        hq.removeAll();
        JsonObject rank = object(d, "rank");
        JsonObject progression = object(d, "progression");
        JsonArray moves = array(progression, "nextMoves");

        hq.add(section("MY EXILE HQ", "PROGRESS TOGETHER"));
        hq.add(vGap(5));
        hq.add(infoCard(
            str(object(d, "member"), "rsn", localRsn) + " • " + str(rank, "name", "Recruit"),
            format(integer(rank, "points", 0)) + " EE Points" + (str(rank, "next", "").isBlank() ? "" : " • " + format(integer(rank, "remaining", 0)) + " EE to " + str(rank, "next", "")),
            PURPLE, "rank"));

        hq.add(vGap(7));
        hq.add(section("WHAT CAN I DO NEXT?", "ELITE EXILES"));
        hq.add(vGap(5));
        if (moves.size() == 0)
        {
            hq.add(infoCard("Refresh progression", "Pull a current Jagex snapshot so HQ can combine your account progress with live clan groups and competitions.", AMBER));
        }
        int shown = 0;
        for (JsonElement e : moves)
        {
            if (!e.isJsonObject()) continue;
            JsonObject move = e.getAsJsonObject();
            String type = str(move, "type", "NEXT");
            Color accent = "GROUP".equals(type) ? GREEN : ("COMPETE".equals(type) ? GOLD : ("MISSION".equals(type) ? BLUE : PURPLE));
            hq.add(infoCard(str(move, "title", "Next move"), str(move, "detail", "Useful Elite Exiles progression step."), accent));
            hq.add(vGap(4));
            if (++shown >= 4) break;
        }

        hq.add(vGap(3));
        hq.add(section("CURRENT SESSION", "RUNELITE LIVE"));
        hq.add(vGap(5));
        hq.add(sessionCard());

        hq.add(vGap(7));
        hq.add(section("ELITE EXILES", "MORE THAN A CLAN"));
        hq.revalidate();
        hq.repaint();
    }

    protected JPanel sessionCard()
    {
        long elapsed = Math.max(1L, System.currentTimeMillis() - sessionStartedAt);
        long perHour = Math.round(localSessionXp * 3_600_000.0 / elapsed);
        JPanel c = card(BLUE);
        c.setLayout(new BorderLayout(8, 0));
        c.add(new GlyphTile("session", BLUE, 38), BorderLayout.WEST);
        JPanel copy = transparent();
        copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
        copy.add(text("+" + format(localSessionXp) + " XP", 13, WHITE, Font.BOLD));
        copy.add(vGap(2));
        copy.add(text(format(perHour) + " XP/hr  •  " + duration(elapsed), 10, MUTED, Font.BOLD));
        c.add(copy, BorderLayout.CENTER);
        c.add(pill("LIVE", GREEN), BorderLayout.EAST);
        return c;
    }

    protected void rebuildCoachUnlinked(String message)
    {
        coach.removeAll();
        coach.add(section("GROUP", "PROGRESS TOGETHER"));
        coach.add(vGap(5));
        coach.add(infoCard("Clan group finder", message, GREEN));
        coach.add(vGap(7));
        coach.add(discordCard());
        coach.revalidate();
        coach.repaint();
    }

    protected void rebuildCoach(JsonObject d)
    {
        coach.removeAll();
        coach.add(section("GROUP", "FIND PEOPLE • PLAY • GET CREDIT"));
        coach.add(vGap(5));

        JsonObject progression = object(d, "progression");
        JsonObject groupObj = object(progression, "group");
        JsonArray groups = array(groupObj, "groups");

        // GROUP uses a full-width text card rather than a glyph/info-card column.
        // This intentionally matches the usable COMPETE card footprint while giving
        // the instructions the entire sidebar width and normal 12/11pt typography.
        JPanel quickGuide = card(GREEN);
        quickGuide.setLayout(new BorderLayout());
        JPanel guideCopy = transparent();
        guideCopy.setLayout(new BoxLayout(guideCopy, BoxLayout.Y_AXIS));
        JTextArea guideTitle = wrapStyled("GROUP IS SIMPLE", WHITE, 12, Font.BOLD);
        guideTitle.setRows(1);
        guideCopy.add(guideTitle);
        guideCopy.add(vGap(4));
        JTextArea guideSteps = wrap("1  Pick an activity\n2  Host or join\n3  Host presses START\n4  Everyone taps I'M DONE", SILVER, 11);
        guideSteps.setRows(5);
        guideCopy.add(guideSteps);
        guideCopy.setMinimumSize(new Dimension(0, 0));
        quickGuide.add(guideCopy, BorderLayout.CENTER);
        quickGuide.setMinimumSize(new Dimension(0, 106));
        quickGuide.setPreferredSize(new Dimension(205, 106));
        quickGuide.setMaximumSize(new Dimension(Integer.MAX_VALUE, 106));
        coach.add(fullWidth(quickGuide, 106));
        coach.add(vGap(7));
        coach.add(section("MAKE A GROUP", "PICK IT • HOST IT"));
        coach.add(vGap(5));

        groupActivityInput.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        // Keep every direct child on the same LEFT alignment line. With BoxLayout,
        // JComboBox/JTextField default CENTER alignment can shift the whole GROUP
        // page inward and make otherwise full-width cards look half-width.
        groupActivityInput.setAlignmentX(Component.LEFT_ALIGNMENT);
        groupActivityInput.setBackground(PANEL_3);
        groupActivityInput.setForeground(WHITE);
        groupActivityInput.setFont(groupActivityInput.getFont().deriveFont(Font.BOLD, 11f));
        groupActivityInput.setToolTipText("Pick the activity you want to do with the clan");
        coach.add(text("WHAT ARE WE DOING?", 11, MUTED, Font.BOLD));
        coach.add(vGap(2));
        coach.add(groupActivityInput);
        coach.add(vGap(5));

        groupWorldInput.setBackground(PANEL_3);
        groupWorldInput.setForeground(WHITE);
        groupWorldInput.setCaretColor(WHITE);
        groupWorldInput.setFont(groupWorldInput.getFont().deriveFont(Font.BOLD, 11f));
        groupWorldInput.setToolTipText("Optional OSRS world, 301-599");
        groupWorldInput.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BRONZE), BorderFactory.createEmptyBorder(4, 6, 4, 6)));
        groupWorldInput.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        groupWorldInput.setAlignmentX(Component.LEFT_ALIGNMENT);
        coach.add(text("WORLD (OPTIONAL)", 11, MUTED, Font.BOLD));
        coach.add(vGap(2));
        coach.add(groupWorldInput);
        coach.add(vGap(5));

        hostGroupButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        hostGroupButton.setFont(hostGroupButton.getFont().deriveFont(Font.BOLD, 11f));
        hostGroupButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        coach.add(hostGroupButton);

        int openCount = integer(groupObj, "openCount", 0);
        int activeCount = integer(groupObj, "activeCount", 0);
        coach.add(vGap(8));
        coach.add(section("CLAN GROUPS", openCount + " OPEN • " + activeCount + " ACTIVE"));
        coach.add(vGap(5));
        if (groups.size() == 0)
        {
            JPanel empty = card(BORDER);
            empty.setLayout(new BorderLayout());
            JPanel emptyCopy = transparent();
            emptyCopy.setLayout(new BoxLayout(emptyCopy, BoxLayout.Y_AXIS));
            JTextArea emptyTitle = wrapStyled("NO GROUPS YET", WHITE, 12, Font.BOLD);
            emptyTitle.setRows(1);
            emptyCopy.add(emptyTitle);
            emptyCopy.add(vGap(3));
            JTextArea emptyBody = wrap("Pick an activity above and hit HOST THIS. Your crew appears here and in Discord GROUP immediately.", SILVER, 11);
            emptyBody.setRows(4);
            emptyCopy.add(emptyBody);
            emptyCopy.setMinimumSize(new Dimension(0, 0));
            empty.add(emptyCopy, BorderLayout.CENTER);
            empty.setMinimumSize(new Dimension(0, 108));
            empty.setPreferredSize(new Dimension(205, 108));
            empty.setMaximumSize(new Dimension(Integer.MAX_VALUE, 108));
            coach.add(fullWidth(empty, 108));
        }

        int shown = 0;
        for (JsonElement e : groups)
        {
            if (!e.isJsonObject()) continue;
            JsonObject row = e.getAsJsonObject();
            boolean mine = booleanValue(row, "mine", false);
            boolean host = booleanValue(row, "host", false);
            String phase = str(row, "phase", "WAITING");
            String phaseLabel = "ACTIVE".equals(phase) ? "⚔ IN PROGRESS" : ("READY".equals(phase) ? "✓ READY TO START" : "… WAITING FOR SOMEONE");

            JPanel c = card(mine ? PURPLE : ("ACTIVE".equals(phase) ? GOLD : GREEN));
            c.setLayout(new BoxLayout(c, BoxLayout.Y_AXIS));
            c.add(text(str(row, "activity", "Clan activity"), 12, WHITE, Font.BOLD));
            c.add(vGap(3));
            String meta = phaseLabel + " • " + integer(row, "memberCount", 1) + "/" + integer(row, "maxSize", 5);
            if (integer(row, "world", 0) > 0) meta += " • W" + integer(row, "world", 0);
            meta += " • Host: " + str(row, "hostRsn", "Clan member");
            c.add(wrapStyled(meta, MUTED, 11, Font.BOLD));
            JsonArray memberRsns = array(row, "memberRsns");
            if (memberRsns.size() > 1)
            {
                StringBuilder crew = new StringBuilder();
                for (JsonElement member : memberRsns)
                {
                    if (crew.length() > 0) crew.append(" • ");
                    crew.append(member.getAsString());
                }
                c.add(vGap(3));
                c.add(wrap("Crew: " + crew, SILVER, 11));
            }
            String note = str(row, "note", "");
            if (!note.isBlank()) { c.add(vGap(3)); c.add(wrap(note, SILVER, 11)); }
            c.add(vGap(5));
            String id = str(row, "id", "");

            if (mine)
            {
                JPanel actions = transparent(new GridLayout(1, 2, 4, 0));
                actions.setAlignmentX(Component.LEFT_ALIGNMENT);
                JButton primary;
                if ("ACTIVE".equals(phase))
                {
                    boolean confirmed = booleanValue(row, "confirmedByMe", false);
                    primary = button(confirmed ? "DONE ✓" : "I'M DONE", GREEN);
                    primary.setEnabled(!confirmed && booleanValue(row, "canConfirm", false));
                    primary.addActionListener(ev -> { if (controller != null) controller.completeGroupFromPanel(id); });
                }
                else if (host && booleanValue(row, "canStart", false))
                {
                    primary = button("START", GREEN);
                    primary.addActionListener(ev -> { if (controller != null) controller.startGroupFromPanel(id); });
                }
                else
                {
                    primary = button("READY".equals(phase) ? "HOST STARTS" : "WAITING…", BORDER);
                    primary.setEnabled(false);
                }
                JButton leave = button(host ? "CANCEL" : "LEAVE", RED);
                leave.addActionListener(ev -> { if (controller != null) controller.leaveGroupFromPanel(id); });
                actions.add(primary);
                actions.add(leave);
                actions.setMaximumSize(new Dimension(Integer.MAX_VALUE, 31));
                c.add(actions);

                if ("ACTIVE".equals(phase))
                {
                    c.add(vGap(3));
                    c.add(wrapStyled(integer(row, "confirmedCount", 0) + "/" + integer(row, "memberCount", 1) + " done • EE credit pays after the whole crew confirms", MUTED, 10, Font.BOLD));
                }
            }
            else
            {
                boolean joinable = booleanValue(row, "joinable", false);
                JButton action = button(joinable ? "JUMP IN" : ("ACTIVE".equals(phase) ? "IN PROGRESS" : "FULL"), joinable ? GREEN : BORDER);
                action.setEnabled(joinable);
                action.addActionListener(ev -> { if (controller != null) controller.joinGroupFromPanel(id); });
                action.setMaximumSize(new Dimension(Integer.MAX_VALUE, 31));
                action.setAlignmentX(Component.LEFT_ALIGNMENT);
                c.add(action);
            }
            coach.add(c);
            coach.add(vGap(5));
            if (++shown >= 10) break;
        }
        coach.revalidate();
        coach.repaint();
    }

    protected void rebuildPlanUnlinked(String message)
    {
        plan.removeAll();
        plan.add(section("COMPETE", "PROGRESSION COUNTS"));
        plan.add(vGap(5));
        plan.add(infoCard("Friendly competition", message, GOLD));
        plan.revalidate();
        plan.repaint();
    }

    protected void rebuildPlan(JsonObject d)
    {
        plan.removeAll();
        JsonObject progression = object(d, "progression");
        JsonObject competeObj = object(progression, "compete");
        JsonObject sotw = object(competeObj, "sotw");
        JsonObject me = object(sotw, "me");
        JsonArray leaderboard = array(sotw, "leaderboard");

        plan.add(section("SKILL OF THE WEEK", str(sotw, "skill", "SOTW").toUpperCase()));
        plan.add(vGap(5));
        if (me.size() > 0)
        {
            String division = str(me, "division", "DEVELOPING");
            plan.add(infoCard("#" + integer(me, "rank", 0) + " overall • #" + integer(me, "divisionRank", 0) + " " + division, "+" + format(longValue(me, "gain", 0L)) + " XP this week • Level " + integer(me, "level", 1), GOLD, "trophy"));
        }
        else
        {
            plan.add(infoCard("Establish your weekly baseline", "Press Refresh once. Your SOTW score then measures XP gained after that verified Jagex refresh.", GOLD, "trophy"));
        }
        plan.add(vGap(4));
        plan.add(wrap(str(sotw, "scoringNote", "Progression-friendly weekly scoring."), MUTED, 10));

        plan.add(vGap(7));
        plan.add(section("LEADERBOARD", integer(sotw, "participantCount", 0) + " PARTICIPANTS"));
        plan.add(vGap(5));
        if (leaderboard.size() == 0)
        {
            plan.add(infoCard("No scored gains yet", "The board fills as members refresh during the week. Small-clan participation matters more than pretending everyone is maxed.", BORDER));
        }
        int shown = 0;
        for (JsonElement e : leaderboard)
        {
            if (!e.isJsonObject()) continue;
            JsonObject row = e.getAsJsonObject();
            String title = "#" + integer(row, "rank", shown + 1) + "  " + str(row, "rsn", "Member");
            String division = str(row, "division", "DEVELOPING");
            String body = "+" + format(longValue(row, "gain", 0L)) + " XP • #" + integer(row, "divisionRank", 0) + " in " + division + " • Lvl " + integer(row, "level", 1);
            plan.add(infoCard(title, body, shown == 0 ? GOLD : BORDER, "trophy"));
            plan.add(vGap(4));
            if (++shown >= 8) break;
        }

        JsonObject missions = object(d, "missions");
        JsonArray weekly = array(missions, "weekly");
        plan.add(vGap(4));
        plan.add(section("WEEKLY MISSIONS", "EVERYONE CAN CONTRIBUTE"));
        plan.add(vGap(5));
        if (weekly.size() == 0) plan.add(infoCard("No weekly mission loaded", "Refresh HQ after the Discord mission cycle updates.", BLUE));
        shown = 0;
        for (JsonElement e : weekly)
        {
            if (!e.isJsonObject()) continue;
            JsonObject mission = e.getAsJsonObject();
            plan.add(infoCard(str(mission, "title", "Weekly mission"), integer(mission, "percent", 0) + "% complete • " + integer(mission, "points", 0) + " EE Points", BLUE));
            plan.add(vGap(4));
            if (++shown >= 3) break;
        }
        plan.revalidate();
        plan.repaint();
    }

    protected void rebuildClanUnlinked()
    {
        clan.removeAll();
        clan.add(section("ELITE EXILES", "COMMUNITY"));
        clan.add(vGap(5));
        clan.add(discordCard());
        clan.add(vGap(7));
        clan.add(infoCard("Mystery Pet Hunt", "Join Discord to catch Mystery Pet Boxes, build your Pet Log and flex your equipped pet.", PURPLE));
        clan.revalidate();
        clan.repaint();
    }

    protected void rebuildClan(JsonObject d)
    {
        clan.removeAll();
        JsonObject community = object(d, "community");
        JsonObject pet = object(d, "petHunt");

        clan.add(section("COMPANION ACCESS", "DISCORD LINK"));
        clan.add(vGap(5));
        clan.add(infoCard("LINK ACTIVE", "This RuneLite account is linked to Elite Exiles. Continued plugin access does not depend on Discord roles, staff approval, or current clan membership.", GREEN, "link"));

        JsonObject progression = object(d, "progression");
        JsonObject groupPulse = object(progression, "group");
        JsonObject sotwPulse = object(object(progression, "compete"), "sotw");
        clan.add(vGap(7));
        clan.add(section("PROGRESSION PULSE", "RIGHT NOW"));
        clan.add(vGap(5));
        clan.add(infoCard(integer(groupPulse, "openCount", 0) + " open groups", "SOTW: " + str(sotwPulse, "skill", "—") + " • " + integer(sotwPulse, "participantCount", 0) + " participants", GREEN));

        JsonObject identityObj = object(d, "identity");
        JsonObject themeObj = object(identityObj, "theme");
        clan.add(vGap(7));
        clan.add(section("EXILE IDENTITY", "RUNIC COSMETICS"));
        clan.add(vGap(5));
        clan.add(infoCard(str(themeObj, "name", "Classic Exile"), "Spendable EE: " + format(integer(identityObj, "shopBalance", 0)) + " • Unlock/equip permanent chat styles with /eeshop in Discord.", PURPLE));

        JsonArray activityFeed = array(progression, "activityFeed");
        clan.add(vGap(7));
        clan.add(section("CLAN ACTIVITY", "NATIVE OSRS BROADCASTS"));
        clan.add(vGap(5));
        if (activityFeed.size() == 0)
        {
            clan.add(infoCard("No recent broadcast captured", "Your existing OSRS clan broadcast settings remain the source of truth. Qualifying loot, pets, Collection Log, PB, CA, level, quest and PvP broadcasts appear here when a synced member receives them.", BORDER));
        }
        else
        {
            int activityShown = 0;
            for (JsonElement eventElement : activityFeed)
            {
                if (!eventElement.isJsonObject()) continue;
                JsonObject eventRow = eventElement.getAsJsonObject();
                clan.add(infoCard(str(eventRow, "category", "CLAN").replace('_', ' '), str(eventRow, "message", "Clan activity") + " • " + timeAgo(longValue(eventRow, "at", 0L)), PURPLE));
                clan.add(vGap(4));
                if (++activityShown >= 8) break;
            }
        }

        clan.add(vGap(7));
        clan.add(section("CLAN NETWORK", "DISCORD ↔ RUNELITE"));
        clan.add(vGap(5));
        JPanel stats = transparent(new GridLayout(1, 3, 4, 0));
        stats.setAlignmentX(Component.LEFT_ALIGNMENT);
        stats.add(stat("MEMBERS", integer(community, "memberCount", 0), PURPLE));
        stats.add(stat("REGISTERED", integer(community, "registeredCount", 0), BLUE));
        stats.add(stat("ONLINE", integer(community, "onlineCount", 0), GREEN));
        stats.setMaximumSize(new Dimension(Integer.MAX_VALUE, 66));
        clan.add(stats);

        clan.add(vGap(7));
        clan.add(section("MYSTERY PET HUNT", "DISCORD PET LOG"));
        clan.add(vGap(5));
        if (pet.size() > 0)
        {
            int unique = integer(pet, "unique", 0);
            int total = Math.max(1, integer(pet, "total", 72));
            JPanel petCard = card(PURPLE);
            petCard.setLayout(new BoxLayout(petCard, BoxLayout.Y_AXIS));
            petCard.add(text(unique + " / " + total + " UNIQUE PETS", 12, WHITE, Font.BOLD));
            petCard.add(vGap(4));
            ProgressBar progress = new ProgressBar((int) Math.round(unique * 100.0 / total), PURPLE);
            progress.setCaption((int) Math.round(unique * 100.0 / total) + "% of Pet Log complete");
            petCard.add(progress);
            petCard.add(vGap(5));
            petCard.add(wrapStyled("Clan luck: Tier " + integer(pet, "luckTier", 0) + "/3 • " + integer(pet, "luckSignals", 0) + " recent signals", MUTED, 10, Font.BOLD));
            petCard.add(vGap(3));
            petCard.add(wrapStyled(booleanValue(pet, "activeBox", false) ? "● MYSTERY PET BOX LIVE" : "No Mystery Pet Box live right now", booleanValue(pet, "activeBox", false) ? GREEN : MUTED, 10, Font.BOLD));
            clan.add(petCard);
        }
        else
        {
            clan.add(infoCard("Mystery Pet Hunt", "Pet Hunt is active in Discord. Refresh after the server-side Exile HQ bridge update to show live Pet Log progress here.", PURPLE));
        }

        clan.add(vGap(7));
        JsonObject event = object(community, "nextEvent");
        clan.add(section("NEXT CLAN EVENT", "DISCORD"));
        clan.add(vGap(5));
        if (event.size() > 0)
        {
            long at = longValue(event, "startAt", 0L);
            clan.add(infoCard(str(event, "name", "Clan Event"), str(event, "type", "Clan Event") + " • " + countdown(at) + (str(event, "location", "").isBlank() ? "" : "\n" + str(event, "location", "")), BLUE, "event"));
        }
        else
        {
            clan.add(infoCard("No scheduled event found", "Discord events will appear here automatically when one is scheduled.", BORDER));
        }
        clan.add(vGap(7));
        clan.add(discordCard());
        clan.revalidate();
        clan.repaint();
    }


    protected JPanel discordCard()
    {
        JPanel c = card(PURPLE);
        c.setLayout(new BoxLayout(c, BoxLayout.Y_AXIS));
        c.add(text("ELITE EXILES DISCORD", 11, WHITE, Font.BOLD));
        c.add(vGap(3));
        c.add(wrap("Events, Pet Hunt, registration, goals and /runelitelink all live here.", MUTED, 10));
        c.add(vGap(6));
        JButton open = button("OPEN DISCORD", PURPLE);
        open.setAlignmentX(Component.LEFT_ALIGNMENT);
        open.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        open.addActionListener(e -> LinkBrowser.browse(DISCORD_INVITE_URL));
        c.add(open);
        c.add(vGap(3));
        c.add(text(DISCORD_INVITE_URL, 8, PURPLE_LIGHT, Font.PLAIN));
        return c;
    }

    protected void addRoadmap(JPanel target, String label, JsonArray rows, Color accent)
    {
        if (rows.size() == 0) return;
        int shown = 0;
        for (JsonElement e : rows)
        {
            String value = e.isJsonPrimitive() ? e.getAsString() : e.toString();
            target.add(infoCard(label, value, accent));
            target.add(vGap(4));
            if (++shown >= 2) break;
        }
    }

    protected JPanel stepCard(int number, String value, Color accent)
    {
        JPanel c = card(accent);
        c.setLayout(new BorderLayout(7, 0));
        JLabel num = text(String.valueOf(number), 15, accent, Font.BOLD);
        num.setHorizontalAlignment(SwingConstants.CENTER);
        num.setPreferredSize(new Dimension(26, 36));
        c.add(num, BorderLayout.WEST);
        c.add(wrap(value, WHITE, 11), BorderLayout.CENTER);
        return c;
    }

}
