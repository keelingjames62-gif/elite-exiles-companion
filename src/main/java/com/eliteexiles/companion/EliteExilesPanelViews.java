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
import java.awt.GridLayout;
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

    protected abstract void requestSessionPlan(int minutes);
    protected abstract void requestBlockerAnalysis();
    protected abstract void addThreadBubble(String who, String message, Color background, Color foreground);

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
        linkCard.add(section("CONNECT YOUR COACH", "30-SECOND SETUP"));
        linkCard.add(vGap(5));
        JTextArea setupIntro = wrap("One-time setup. No server address, RSN entry, API key, or token handling.", SILVER, 11);
        setupIntro.setRows(2);
        setupIntro.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        linkCard.add(setupIntro);
        linkCard.add(vGap(6));

        JPanel firstSteps = transparent(new GridLayout(2, 1, 0, 5));
        firstSteps.setAlignmentX(Component.LEFT_ALIGNMENT);
        firstSteps.setMaximumSize(new Dimension(1000, 184));
        firstSteps.add(setupStep(1, "ENABLE COACH",
            "RuneLite Settings ⚙ → Elite Exiles Companion → Enable Coach Integration.", "settings", PURPLE));
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
            "Paste the code below while logged into that OSRS character, then click Connect Coach.", "check", GREEN));
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
        JPanel nav = transparent(new GridLayout(1, 4, 4, 0));
        nav.setAlignmentX(Component.LEFT_ALIGNMENT);
        nav.setMaximumSize(new Dimension(Integer.MAX_VALUE, 31));
        for (String name : new String[] {"HQ", "COACH", "PLAN", "CLAN"})
        {
            JButton b = navButton(name);
            b.addActionListener(e -> selectPage(name));
            navButtons.put(name, b);
            nav.add(b);
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
        c.add(wrap("Local tracking stays available even when Coach Integration is disabled.", MUTED, 10));
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
        hq.add(infoCard("UNLOCK FULL HQ", "Enable Coach Integration in settings, then use the Discord link card above. Your local session data remains private until you opt in.", PURPLE, "link"));
        hq.revalidate();
        hq.repaint();
    }

    protected void rebuildHq(JsonObject d)
    {
        hq.removeAll();
        JsonObject coachObj = object(d, "coach");
        JsonArray goals = array(d, "goals");
        JsonArray recs = array(coachObj, "recommendations");
        JsonObject membership = object(d, "membership");
        if (!booleanValue(membership, "verified", false))
        {
            hq.add(section("VERIFIED EXILE", "ACCESS"));
            hq.add(vGap(5));
            hq.add(membershipCard(membership));
            hq.add(vGap(7));
            hq.add(section("LOCAL SESSION", "RUNELITE"));
            hq.add(vGap(5));
            hq.add(sessionCard());
            hq.revalidate();
            hq.repaint();
            return;
        }

        hq.add(section("ACTIVE GOAL", "COACH"));
        hq.add(vGap(5));
        String personal = str(coachObj, "personalGoal", "");
        String goal = !personal.isBlank() ? personal : firstTitle(goals, "No personal goal set yet");
        String status = str(coachObj, "personalGoalStatus", "");
        hq.add(infoCard(goal, status.isBlank() ? "Use /setgoal in Discord to choose a personal goal." : status, PURPLE, "target"));

        hq.add(vGap(7));
        hq.add(section("NEXT STEP", "ACCOUNT AWARE"));
        hq.add(vGap(5));
        if (recs.size() > 0 && recs.get(0).isJsonObject())
        {
            JsonObject rec = recs.get(0).getAsJsonObject();
            hq.add(infoCard(str(rec, "title", "Next move"), str(rec, "nextStep", str(rec, "why", "Refresh Coach for a recommendation.")), GREEN, "next"));
        }
        else
        {
            hq.add(infoCard("Refresh Coach", "Pull a current Jagex snapshot to generate your next useful step.", AMBER));
        }

        hq.add(vGap(7));
        hq.add(section("CURRENT SESSION", "RUNELITE LIVE"));
        hq.add(vGap(5));
        hq.add(sessionCard());

        hq.add(vGap(7));
        hq.add(section("QUICK ACTIONS", "NO AUTOMATION"));
        hq.add(vGap(5));
        JPanel quick = transparent(new GridLayout(2, 2, 5, 5));
        quick.setAlignmentX(Component.LEFT_ALIGNMENT);
        JButton ask = button("ASK COACH", PURPLE); ask.addActionListener(e -> selectPage("COACH"));
        JButton planner = button("PLAN 30M", BLUE); planner.addActionListener(e -> requestSessionPlan(30));
        JButton blockers = button("BLOCKERS", AMBER); blockers.addActionListener(e -> requestBlockerAnalysis());
        JButton check = button("CHECK-IN", GREEN); check.addActionListener(e -> { if (controller != null) controller.checkInFromPanel(); });
        quick.add(ask); quick.add(planner); quick.add(blockers); quick.add(check);
        quick.setMaximumSize(new Dimension(Integer.MAX_VALUE, 66));
        hq.add(quick);
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
        coach.add(section("COACH CHAT", "OPTIONAL"));
        coach.add(vGap(5));
        coach.add(infoCard("Account-aware OSRS help", message, PURPLE));
        coach.add(vGap(7));
        coach.add(discordCard());
        coach.revalidate();
        coach.repaint();
    }

    protected void rebuildCoach(JsonObject d)
    {
        coach.removeAll();
        coach.add(section("COACH CHAT", "TRUSTED OSRS RESEARCH"));
        coach.add(vGap(5));

        JsonObject membership = object(d, "membership");
        if (!booleanValue(membership, "verified", false))
        {
            coach.add(membershipCard(membership));
            coach.add(vGap(7));
            coach.add(infoCard("Coach locked until verified", "Coach unlocks after your Discord link, staff approval, and current Elite Exiles in-game membership are verified. Use REQUEST CLAN ACCESS above.", GOLD));
            coach.revalidate();
            coach.repaint();
            return;
        }

        JsonObject coachObj = object(d, "coach");
        String goal = str(coachObj, "personalGoal", "No personal goal set");
        coach.add(infoCard("Current context", "Goal: " + goal + "\nUses your linked account stats and trusted OSRS sources. No gameplay input is automated.", PURPLE));
        coach.add(vGap(7));

        coachThread.setAlignmentX(Component.LEFT_ALIGNMENT);
        if (coachThread.getComponentCount() == 0)
        {
            addThreadBubble("COACH", "Ask me about gear preparation, quest requirements, training, items, money making, goals, or general OSRS mechanics. I will use your linked account context when it matters.", new Color(38, 33, 28), SILVER);
        }
        coach.add(coachThread);
        coach.add(vGap(7));

        coach.add(text("ASK A QUESTION OR FOLLOW-UP", 11, WHITE, Font.BOLD));
        coach.add(vGap(3));
        coachInput.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        coachInput.setBackground(PANEL_3);
        coachInput.setForeground(WHITE);
        coachInput.setCaretColor(WHITE);
        coachInput.setFont(coachInput.getFont().deriveFont(Font.PLAIN, 13f));
        coachInput.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BRONZE), BorderFactory.createEmptyBorder(6, 8, 6, 8)));
        coachInput.setToolTipText("Type an OSRS question. Press Enter or click Ask Coach.");
        coach.add(coachInput);
        coach.add(vGap(3));
        coach.add(text("Press Enter to send", 9, MUTED, Font.PLAIN));
        coach.add(vGap(5));
        askCoachButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        askCoachButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        coach.add(askCoachButton);
        coach.revalidate();
        coach.repaint();
    }

    protected void rebuildPlanUnlinked(String message)
    {
        plan.removeAll();
        plan.add(section("PLAN", "GOAL → NEXT STEP"));
        plan.add(vGap(5));
        plan.add(infoCard("Plan is ready when you link", message, BLUE));
        plan.revalidate();
        plan.repaint();
    }

    protected void rebuildPlan(JsonObject d)
    {
        plan.removeAll();
        JsonObject membership = object(d, "membership");
        if (!booleanValue(membership, "verified", false))
        {
            plan.add(section("PLAN", "ACCESS"));
            plan.add(vGap(5));
            plan.add(membershipCard(membership));
            plan.add(vGap(7));
            plan.add(infoCard("Planner locked until verified", "Your local session stays available. Discord link + staff approval + current Elite Exiles in-game membership unlock the account-aware planner.", BLUE));
            plan.revalidate();
            plan.repaint();
            return;
        }
        JsonObject coachObj = object(d, "coach");
        JsonObject planObj = object(d, "plan");
        JsonObject roadmap = object(d, "roadmap");
        JsonArray goals = array(d, "goals");
        JsonArray milestones = array(d, "milestones");

        plan.add(section("CURRENT GOAL", "COACH"));
        plan.add(vGap(5));
        String goal = str(coachObj, "personalGoal", firstTitle(goals, "No personal goal set"));
        plan.add(infoCard(goal, str(coachObj, "personalGoalStatus", "Set a goal with /setgoal in Discord."), PURPLE, "target"));

        plan.add(vGap(7));
        plan.add(section("SESSION PLANNER", "USER-REQUESTED COACH PLAN"));
        plan.add(vGap(5));
        JPanel planButtons = transparent(new GridLayout(1, 3, 4, 0));
        planButtons.setAlignmentX(Component.LEFT_ALIGNMENT);
        JButton p15 = button("15 MIN", BORDER); p15.addActionListener(e -> requestSessionPlan(15));
        JButton p30 = button("30 MIN", PURPLE); p30.addActionListener(e -> requestSessionPlan(30));
        JButton p60 = button("60 MIN", BLUE); p60.addActionListener(e -> requestSessionPlan(60));
        planButtons.add(p15); planButtons.add(p30); planButtons.add(p60);
        planButtons.setMaximumSize(new Dimension(Integer.MAX_VALUE, 31));
        plan.add(planButtons);
        plan.add(vGap(7));
        plan.add(section("CURRENT COACH PLAN", "NOW"));
        plan.add(vGap(5));
        JsonArray tasks = array(planObj, "tasks");
        if (tasks.size() == 0) plan.add(infoCard("No session tasks yet", "Press Refresh to rebuild the plan from your goals and current stats.", AMBER));
        int shown = 0;
        for (JsonElement e : tasks)
        {
            if (!e.isJsonPrimitive()) continue;
            plan.add(stepCard(++shown, e.getAsString(), shown == 1 ? GREEN : BLUE));
            plan.add(vGap(5));
            if (shown >= 4) break;
        }

        plan.add(vGap(3));
        plan.add(section("ROADMAP", "NOW → NEXT → LATER"));
        plan.add(vGap(5));
        addRoadmap(plan, "NOW", array(roadmap, "quick"), GREEN);
        addRoadmap(plan, "NEXT", array(roadmap, "next"), PURPLE);
        addRoadmap(plan, "LATER", array(roadmap, "long"), BLUE);

        if (milestones.size() > 0)
        {
            plan.add(vGap(5));
            plan.add(section("NEARBY MILESTONES", "JAGEX DATA"));
            plan.add(vGap(5));
            int n = 0;
            for (JsonElement e : milestones)
            {
                if (!e.isJsonObject()) continue;
                JsonObject m = e.getAsJsonObject();
                String title = str(m, "skill", "Skill") + " " + integer(m, "level", 0) + " → " + integer(m, "target", 0);
                plan.add(infoCard(title, str(m, "description", "Nearby milestone"), BORDER));
                plan.add(vGap(4));
                if (++n >= 3) break;
            }
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
        JsonObject membership = object(d, "membership");

        clan.add(section("VERIFIED EXILE", "MEMBERSHIP"));
        clan.add(vGap(5));
        clan.add(membershipCard(membership));
        if (!booleanValue(membership, "verified", false))
        {
            clan.add(vGap(7));
            clan.add(discordCard());
            clan.revalidate();
            clan.repaint();
            return;
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

    protected JPanel membershipCard(JsonObject membership)
    {
        boolean discord = booleanValue(membership, "discordPresent", false);
        boolean inGame = booleanValue(membership, "inGameClan", false);
        boolean guest = booleanValue(membership, "guestClan", false);
        boolean approved = booleanValue(membership, "staffApproved", false);
        boolean verified = booleanValue(membership, "verified", false);
        String requestStatus = str(membership, "requestStatus", "none");

        JPanel c = card(verified ? GREEN : GOLD);
        c.setLayout(new BoxLayout(c, BoxLayout.Y_AXIS));
        c.add(text(verified ? "✓ VERIFIED EXILE" : "ELITE EXILES ACCESS", 13, verified ? GREEN : WHITE, Font.BOLD));
        c.add(vGap(5));
        c.add(statusRow("Discord", discord, discord ? "LINKED" : "NOT LINKED"));
        c.add(statusRow("Staff", approved, approved ? "APPROVED" : ("pending".equalsIgnoreCase(requestStatus) ? "PENDING" : "NEEDED")));
        c.add(statusRow("In-game", inGame, inGame ? "MEMBER" : (guest ? "GUEST" : "NOT DETECTED")));
        c.add(vGap(6));
        String membershipNoteText = membershipStatusLine(membership);
        JTextArea membershipNote = wrap(membershipNoteText, verified ? SILVER : MUTED, 11);
        membershipNote.setRows(rowsFor(membershipNoteText, 30, verified ? 2 : 4));
        c.add(membershipNote);
        if (!verified)
        {
            c.add(vGap(7));
            JButton request = button("pending".equalsIgnoreCase(requestStatus) ? "ACCESS REQUEST PENDING" : "REQUEST CLAN ACCESS", PURPLE);
            request.setAlignmentX(Component.LEFT_ALIGNMENT);
            request.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
            request.setEnabled(!"pending".equalsIgnoreCase(requestStatus));
            request.addActionListener(e -> { if (controller != null) controller.requestClanAccessFromPanel(); });
            c.add(request);
            c.add(vGap(4));
            JTextArea guestNote = wrap("Guest CC: Elite Exiles • staff handles the actual in-game invite", MUTED, 10);
            guestNote.setRows(2);
            c.add(guestNote);
        }
        return c;
    }

    protected JPanel statusRow(String label, boolean good, String value)
    {
        JPanel row = transparent(new BorderLayout(6, 0));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        row.add(text((good ? "✓ " : "• ") + label, 10, good ? GREEN : SILVER, Font.BOLD), BorderLayout.WEST);
        JLabel right = text(value, 9, good ? GREEN : GOLD, Font.BOLD);
        right.setHorizontalAlignment(SwingConstants.RIGHT);
        row.add(right, BorderLayout.EAST);
        return row;
    }

    protected String membershipStatusLine(JsonObject membership)
    {
        boolean discord = booleanValue(membership, "discordPresent", false);
        boolean inGame = booleanValue(membership, "inGameClan", false);
        boolean guest = booleanValue(membership, "guestClan", false);
        boolean approved = booleanValue(membership, "staffApproved", false);
        boolean verified = booleanValue(membership, "verified", false);
        String requestStatus = str(membership, "requestStatus", "none");
        if (verified) return "Full Exile HQ unlocked.";
        if (!discord) return "Join/link the Elite Exiles Discord first. Your RuneLite link keeps the request attached to the correct Discord member and RSN.";
        if ("denied".equalsIgnoreCase(requestStatus)) return "The previous access request was declined. You can submit a new request when the issue is resolved.";
        if (!approved && "pending".equalsIgnoreCase(requestStatus)) return "Your official clan-access request is waiting for staff review. No DM or manual RSN lookup is needed.";
        if (!approved) return "Request clan access. Staff receives your linked Discord member, RSN, and current clan/guest-channel status in one review card.";
        if (!inGame && guest) return "Staff approved you for access and the Elite Exiles guest channel is detected. A clan officer still needs to complete the normal in-game clan invite.";
        if (!inGame) return "Staff approved you. Join Elite Exiles in-game when the officer invite is completed; RuneLite will detect it automatically.";
        return "In-game clan detected. Waiting for the remaining Discord/staff verification step.";
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
