package com.eliteexiles.companion;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Rectangle;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.text.NumberFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import net.runelite.client.ui.PluginPanel;

/**
 * Shared Exile HQ state, rendering primitives, JSON helpers, and bounded Swing components.
 * Split from EliteExilesPanel without changing runtime behavior.
 */
abstract class EliteExilesPanelSupport extends PluginPanel
{
    protected static final String DISCORD_INVITE_URL = "https://discord.gg/FTJhv48K2";

    // RuneLite/OSRS-native neutral base with restrained Elite Exiles branding.
    // Purple is reserved for identity/selection instead of tinting every surface.
    protected static final Color BG = new Color(18, 16, 13);
    protected static final Color PANEL = new Color(29, 26, 22);
    protected static final Color PANEL_2 = new Color(36, 32, 27);
    protected static final Color PANEL_3 = new Color(43, 38, 31);
    protected static final Color BORDER = new Color(91, 78, 59);
    protected static final Color BRONZE = new Color(112, 88, 54);
    protected static final Color GOLD = new Color(206, 161, 82);
    protected static final Color PURPLE = new Color(108, 68, 145);
    protected static final Color PURPLE_LIGHT = new Color(168, 132, 198);
    protected static final Color SILVER = new Color(216, 212, 204);
    protected static final Color WHITE = new Color(239, 233, 220);
    protected static final Color MUTED = new Color(168, 157, 139);
    protected static final Color GREEN = new Color(104, 166, 103);
    protected static final Color AMBER = GOLD;
    protected static final Color RED = new Color(168, 77, 72);
    protected static final Color BLUE = new Color(100, 116, 142);
    protected static final Color PANEL_HIGHLIGHT = new Color(43, 38, 32);
    protected static final Color DEEP_PURPLE = new Color(58, 41, 73);
    protected static final Color SHADOW = new Color(0, 0, 0, 125);

    protected EliteExilesCompanionPlugin controller;

    protected final JLabel connection = text("LOCAL MODE", 11, GREEN, Font.BOLD);
    protected final JTextArea statusDetail = wrap("Local RuneLite tracking is ready.", MUTED, 11);
    protected final JPanel linkCard = card(PURPLE);
    protected final JTextField linkCode = new JTextField();
    protected final JButton joinDiscordButton = button("JOIN ELITE EXILES DISCORD", PURPLE);
    protected final JButton linkButton = button("CONNECT ELITE EXILES", PURPLE);

    protected final JPanel profileHost = transparent(new BorderLayout());
    protected final JPanel contentHost = transparent(new CardLayout());
    protected final Map<String, JButton> navButtons = new LinkedHashMap<>();
    protected final Map<String, JPanel> pages = new LinkedHashMap<>();
    protected final Map<String, JScrollPane> pageScrolls = new LinkedHashMap<>();

    protected final JPanel hq = vertical();
    protected final JPanel coach = vertical();
    protected final JPanel plan = vertical();
    protected final JPanel clan = vertical();
    protected final JComboBox<String> groupActivityInput = new JComboBox<>(new String[] {
        "Barrows", "Moons of Peril", "King Black Dragon", "Dagannoth Kings", "God Wars Learners",
        "Entry-mode ToA", "Pest Control", "Tempoross", "Wintertodt", "Guardians of the Rift",
        "Clan Skilling Session", "Slayer / Wilderness Slayer", "LMS / PK Learners", "Wilderness Trip", "Quest / Unlock Help"
    });
    protected final JTextField groupWorldInput = new JTextField();
    protected final JButton hostGroupButton = button("HOST THIS", PURPLE);

    protected final JButton refreshButton = button("REFRESH", PURPLE);
    protected final JButton checkinButton = button("CHECK-IN", GREEN);
    protected final JButton diagnosticsButton = button("DIAGNOSTICS", BLUE);
    protected final JButton unlinkButton = button("UNLINK", RED);

    protected JsonObject lastDashboard;
    protected final Map<String, Integer> liveSkillLevels = new LinkedHashMap<>();
    protected String localRsn = "";
    protected long localSessionXp = 0L;
    protected long sessionStartedAt = System.currentTimeMillis();
    protected String sessionRsn = "";

    protected EliteExilesPanelSupport(boolean wrap)
    {
        super(wrap);
    }

    protected JPanel infoCard(String title, String body, Color accent)
    {
        return infoCard(title, body, accent, glyphFor((title == null ? "" : title) + " " + (body == null ? "" : body)));
    }

    protected JPanel infoCard(String title, String body, Color accent, String glyph)
    {
        JPanel c = card(accent);
        c.setLayout(new BorderLayout(8, 0));
        c.add(new GlyphTile(glyph, accent, 34), BorderLayout.WEST);
        JPanel copy = transparent();
        copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
        String safeTitle = title == null || title.isBlank() ? "—" : title;
        JTextArea titleArea = wrapStyled(safeTitle, WHITE, 12, Font.BOLD);
        titleArea.setRows(rowsFor(safeTitle, 23, 2));
        copy.add(titleArea);
        if (body != null && !body.isBlank())
        {
            copy.add(vGap(3));
            JTextArea bodyArea = wrap(body, MUTED, 11);
            bodyArea.setRows(rowsFor(body, 29, 5));
            copy.add(bodyArea);
        }
        copy.setMinimumSize(new Dimension(0, 0));
        c.add(copy, BorderLayout.CENTER);
        return c;
    }

    protected JPanel setupStep(int number, String title, String body, String glyph, Color accent)
    {
        JPanel row = new RoundedPanel(PANEL_2, new Color(BRONZE.getRed(), BRONZE.getGreen(), BRONZE.getBlue(), 190), 6);
        row.setLayout(new BorderLayout(7, 0));
        row.setBorder(BorderFactory.createEmptyBorder(6, 7, 6, 7));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setPreferredSize(new Dimension(205, 86));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 92));
        row.add(new StepBadge(number, accent), BorderLayout.WEST);
        JPanel copy = transparent();
        copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
        JTextArea heading = wrapStyled(title, WHITE, 12, Font.BOLD);
        heading.setRows(rowsFor(title, 22, 2));
        heading.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        copy.add(heading);
        copy.add(vGap(2));
        JTextArea details = wrap(body, SILVER, 10);
        details.setRows(3);
        copy.add(details);
        copy.setMinimumSize(new Dimension(0, 0));
        row.add(copy, BorderLayout.CENTER);
        return row;
    }

    protected static String glyphFor(String text)
    {
        String s = text == null ? "" : text.toLowerCase();
        if (s.contains("coach") || s.contains("ask")) return "coach";
        if (s.contains("pet") || s.contains("hunt")) return "pet";
        if (s.contains("event") || s.contains("boss") || s.contains("rsvp")) return "event";
        if (s.contains("discord") || s.contains("clan") || s.contains("member")) return "clan";
        if (s.contains("goal") || s.contains("blocker")) return "target";
        if (s.contains("next") || s.contains("roadmap")) return "next";
        if (s.contains("session") || s.contains("minute") || s.contains("xp")) return "session";
        if (s.contains("plan")) return "plan";
        if (s.contains("check")) return "check";
        if (s.contains("refresh")) return "refresh";
        if (s.contains("diagnostic")) return "diagnostics";
        if (s.contains("unlink")) return "unlink";
        if (s.contains("setting") || s.contains("enable")) return "settings";
        if (s.contains("link") || s.contains("connect")) return "link";
        if (s.equals("hq") || s.contains("local")) return "home";
        if (s.contains("rank") || s.contains("elite")) return "rank";
        return "spark";
    }

    protected JPanel stat(String label, int value, Color accent)
    {
        JPanel c = card(accent);
        c.setLayout(new BoxLayout(c, BoxLayout.Y_AXIS));
        JLabel v = text(format(value), 14, WHITE, Font.BOLD); v.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel l = text(label, 9, MUTED, Font.BOLD); l.setAlignmentX(Component.CENTER_ALIGNMENT);
        c.add(Box.createVerticalGlue()); c.add(v); c.add(vGap(2)); c.add(l); c.add(Box.createVerticalGlue());
        return c;
    }

    protected JPanel section(String title, String source)
    {
        JPanel row = transparent(new BorderLayout(5, 0));
        JLabel heading = text(title, 12, WHITE, Font.BOLD);
        heading.setIcon(new GlyphIcon(glyphFor(title), GOLD, 12));
        heading.setIconTextGap(6);
        row.add(heading, BorderLayout.WEST);
        String sourceTag = compactSource(source);
        // Sidebar width is narrow. The source badge is decorative, so never let it collide
        // with a readable section title; omit it when the combined label would be crowded.
        if (title == null || title.length() + sourceTag.length() <= 16)
        {
            row.add(pill(sourceTag, BRONZE), BorderLayout.EAST);
        }
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        return row;
    }

    protected static JPanel card(Color accent)
    {
        JPanel c = new EliteCard(accent);
        c.setBorder(BorderFactory.createEmptyBorder(8, 9, 8, 9));
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        c.setMaximumSize(new Dimension(Integer.MAX_VALUE, 280));
        return c;
    }

    protected static Component vGap(int height)
    {
        Box.Filler gap = new Box.Filler(
            new Dimension(0, height),
            new Dimension(0, height),
            new Dimension(1000, height));
        gap.setAlignmentX(Component.LEFT_ALIGNMENT);
        return gap;
    }

    protected static JPanel fullWidth(JComponent component, int height)
    {
        JPanel host = transparent(new BorderLayout());
        host.setAlignmentX(Component.LEFT_ALIGNMENT);
        host.setPreferredSize(new Dimension(205, height));
        host.setMaximumSize(new Dimension(1000, height));
        component.setPreferredSize(new Dimension(0, height));
        host.add(component, BorderLayout.CENTER);
        return host;
    }

    protected static JPanel transparent()
    {
        JPanel p = new JPanel(); p.setOpaque(false); return p;
    }

    protected static JPanel transparent(java.awt.LayoutManager layout)
    {
        JPanel p = new JPanel(layout); p.setOpaque(false); return p;
    }

    protected static JPanel vertical()
    {
        return new VerticalPanel();
    }

    protected static JLabel text(String value, int size, Color color, int style)
    {
        JLabel l = new JLabel(value == null ? "" : value);
        l.setForeground(color);
        l.setFont(l.getFont().deriveFont(style, (float) size));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    protected static JTextArea wrap(String value, Color color, int size)
    {
        return wrapStyled(value, color, size, Font.PLAIN);
    }

    protected static JTextArea wrapStyled(String value, Color color, int size, int style)
    {
        JTextArea a = new JTextArea(value == null ? "" : value);
        a.setOpaque(false);
        a.setEditable(false);
        a.setFocusable(false);
        a.setLineWrap(true);
        a.setWrapStyleWord(true);
        a.setForeground(color);
        a.setFont(a.getFont().deriveFont(style, (float) size));
        a.setBorder(null);
        a.setAlignmentX(Component.LEFT_ALIGNMENT);
        return a;
    }

    protected static JLabel pill(String value, Color accent)
    {
        Color labelColor = accent.equals(GREEN) || accent.equals(RED) ? accent : (accent.equals(PURPLE) ? PURPLE_LIGHT : GOLD);
        JLabel l = text(value, 9, labelColor, Font.BOLD);
        l.setOpaque(true);
        l.setBackground(PANEL_3);
        l.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BRONZE), BorderFactory.createEmptyBorder(2, 5, 2, 5)));
        return l;
    }

    protected static JButton button(String title, Color accent)
    {
        return new PremiumButton(title, accent, glyphFor(title));
    }

    protected static JButton navButton(String title)
    {
        // COMPETE is the only tab whose full label plus glyph can exceed the narrow
        // RuneLite sidebar cell. Keep the exact same font and button styling, but
        // render COMPETE text-only so the full word remains legible at normal width.
        String glyph = "COMPETE".equals(title) ? "" : glyphFor(title);
        PremiumButton b = new PremiumButton(title, PURPLE, glyph);
        b.setFont(b.getFont().deriveFont(Font.BOLD, 10.5f));
        return b;
    }

    protected static JsonObject object(JsonObject o, String key)
    {
        try { return o != null && o.has(key) && o.get(key).isJsonObject() ? o.getAsJsonObject(key) : new JsonObject(); }
        catch (Exception e) { return new JsonObject(); }
    }

    protected static JsonArray array(JsonObject o, String key)
    {
        try { return o != null && o.has(key) && o.get(key).isJsonArray() ? o.getAsJsonArray(key) : new JsonArray(); }
        catch (Exception e) { return new JsonArray(); }
    }

    protected static String str(JsonObject o, String key, String fallback)
    {
        try { return o != null && o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : fallback; }
        catch (Exception e) { return fallback; }
    }

    protected static int integer(JsonObject o, String key, int fallback)
    {
        try { return o != null && o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsInt() : fallback; }
        catch (Exception e) { return fallback; }
    }

    protected static long longValue(JsonObject o, String key, long fallback)
    {
        try { return o != null && o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsLong() : fallback; }
        catch (Exception e) { return fallback; }
    }

    protected static boolean booleanValue(JsonObject o, String key, boolean fallback)
    {
        try { return o != null && o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsBoolean() : fallback; }
        catch (Exception e) { return fallback; }
    }

    protected static String firstTitle(JsonArray rows, String fallback)
    {
        for (JsonElement e : rows)
        {
            if (e.isJsonObject())
            {
                String title = str(e.getAsJsonObject(), "title", "");
                if (!title.isBlank()) return title;
            }
        }
        return fallback;
    }

    protected static String compactSource(String source)
    {
        if (source == null) return "";
        switch (source)
        {
            case "30-SECOND SETUP": return "SETUP";
            case "ACCOUNT AWARE": return "ACCOUNT";
            case "RUNELITE LIVE": return "LIVE";
            case "NO AUTOMATION": return "SAFE";
            case "TRUSTED OSRS RESEARCH": return "TRUSTED";
            case "GOAL → NEXT STEP": return "GOAL→STEP";
            case "USER-REQUESTED COACH PLAN": return "ON DEMAND";
            case "NOW → NEXT → LATER": return "ROADMAP";
            case "JAGEX DATA": return "JAGEX";
            case "DISCORD ↔ RUNELITE": return "SYNCED";
            case "DISCORD PET LOG": return "PET LOG";
            default: return source.length() > 14 ? source.substring(0, 13) + "…" : source;
        }
    }

    protected static int rowsFor(String value, int charsPerLine, int maxRows)
    {
        String v = value == null ? "" : value;
        int rows = 0;
        String[] lines = v.split("\\n", -1);
        for (String line : lines)
        {
            rows += Math.max(1, (line.length() + Math.max(1, charsPerLine) - 1) / Math.max(1, charsPerLine));
        }
        return Math.max(1, Math.min(maxRows, rows));
    }

    protected static String fitText(String value, FontMetrics fm, int maxWidth)
    {
        String text = value == null ? "" : value;
        if (fm == null || fm.stringWidth(text) <= maxWidth) return text;
        final String ellipsis = "…";
        int limit = Math.max(0, maxWidth - fm.stringWidth(ellipsis));
        int end = text.length();
        while (end > 0 && fm.stringWidth(text.substring(0, end)) > limit) end--;
        return end <= 0 ? ellipsis : text.substring(0, end).trim() + ellipsis;
    }

    protected static String format(long value)
    {
        return NumberFormat.getIntegerInstance().format(Math.max(0L, value));
    }

    protected static String duration(long millis)
    {
        long total = Math.max(0L, millis / 1000L);
        long h = total / 3600L;
        long m = (total % 3600L) / 60L;
        return h > 0 ? h + "h " + m + "m" : m + "m";
    }

    protected static String countdown(long at)
    {
        if (at <= 0L) return "time TBD";
        long delta = at - System.currentTimeMillis();
        if (delta <= 0L) return "starting now";
        long minutes = delta / 60_000L;
        long hours = minutes / 60L;
        long days = hours / 24L;
        if (days > 0) return "in " + days + "d " + (hours % 24) + "h";
        if (hours > 0) return "in " + hours + "h " + (minutes % 60) + "m";
        return "in " + Math.max(1, minutes) + "m";
    }

    protected static String timeAgo(long at)
    {
        if (at <= 0L) return "recently";
        long delta = Math.max(0L, System.currentTimeMillis() - at);
        long minutes = delta / 60_000L;
        long hours = minutes / 60L;
        long days = hours / 24L;
        if (days > 0) return days + "d ago";
        if (hours > 0) return hours + "h ago";
        if (minutes > 0) return minutes + "m ago";
        return "just now";
    }


    protected static final class GlyphIcon implements Icon
    {
        private final String kind;
        private final Color color;
        private final int size;

        GlyphIcon(String kind, Color color, int size)
        {
            this.kind = kind == null ? "spark" : kind;
            this.color = color == null ? PURPLE_LIGHT : color;
            this.size = Math.max(9, size);
        }

        @Override public int getIconWidth() { return size; }
        @Override public int getIconHeight() { return size; }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y)
        {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.translate(x, y);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(Math.max(1.2f, size / 10f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int w = size, h = size;
            switch (kind)
            {
                case "home":
                    g2.drawLine(2, h / 2, w / 2, 2); g2.drawLine(w / 2, 2, w - 2, h / 2);
                    g2.drawRoundRect(4, h / 2 - 1, w - 8, h / 2 - 3, 2, 2); break;
                case "coach":
                    g2.drawRoundRect(2, 2, w - 4, h - 6, 4, 4);
                    g2.drawLine(w / 3, h - 4, w / 3 - 2, h - 1); g2.drawLine(w / 3 - 2, h - 1, w / 2, h - 4); break;
                case "plan":
                    g2.drawRoundRect(3, 3, w - 6, h - 5, 2, 2);
                    g2.drawLine(6, 7, w - 6, 7); g2.drawLine(6, 11, w - 8, 11); break;
                case "clan":
                    g2.fillOval(w / 2 - 2, 2, 5, 5); g2.fillOval(2, 5, 4, 4); g2.fillOval(w - 6, 5, 4, 4);
                    g2.drawArc(w / 2 - 5, 7, 10, 7, 0, 180); g2.drawArc(0, 9, 8, 5, 0, 180); g2.drawArc(w - 8, 9, 8, 5, 0, 180); break;
                case "target":
                    g2.drawOval(2, 2, w - 4, h - 4); g2.drawOval(5, 5, w - 10, h - 10); g2.fillOval(w / 2 - 1, h / 2 - 1, 3, 3); break;
                case "next":
                    g2.drawLine(2, h / 2, w - 4, h / 2); g2.drawLine(w - 7, h / 2 - 4, w - 3, h / 2); g2.drawLine(w - 3, h / 2, w - 7, h / 2 + 4); break;
                case "session":
                    g2.drawOval(2, 2, w - 4, h - 4); g2.drawLine(w / 2, 4, w / 2, h / 2); g2.drawLine(w / 2, h / 2, w - 5, h / 2 + 2); break;
                case "pet":
                    g2.fillOval(w / 2 - 3, h / 2 + 1, 7, 6); g2.fillOval(2, 4, 4, 4); g2.fillOval(w / 2 - 2, 2, 4, 4); g2.fillOval(w - 6, 4, 4, 4); break;
                case "event":
                    g2.drawRoundRect(2, 4, w - 4, h - 6, 2, 2); g2.drawLine(2, 8, w - 2, 8); g2.drawLine(5, 2, 5, 6); g2.drawLine(w - 5, 2, w - 5, 6); break;
                case "settings":
                    g2.drawOval(4, 4, w - 8, h - 8); g2.fillOval(w / 2 - 2, h / 2 - 2, 4, 4);
                    for (int i = 0; i < 8; i++) { double a = Math.PI * i / 4; int x1=(int)(w/2+Math.cos(a)*(w/2-3)); int y1=(int)(h/2+Math.sin(a)*(h/2-3)); int x2=(int)(w/2+Math.cos(a)*(w/2-1)); int y2=(int)(h/2+Math.sin(a)*(h/2-1)); g2.drawLine(x1,y1,x2,y2); } break;
                case "link":
                    g2.drawOval(1, h / 2 - 4, w / 2 + 2, 8); g2.drawOval(w / 2 - 3, h / 2 - 4, w / 2 + 2, 8); g2.drawLine(w / 3, h / 2, w * 2 / 3, h / 2); break;
                case "check":
                    g2.drawLine(2, h / 2, w / 2 - 1, h - 3); g2.drawLine(w / 2 - 1, h - 3, w - 2, 3); break;
                case "refresh":
                    g2.drawArc(2, 2, w - 4, h - 4, 35, 285); g2.drawLine(w - 3, 3, w - 3, 8); g2.drawLine(w - 3, 3, w - 8, 3); break;
                case "diagnostics":
                    g2.drawRect(3, 3, w - 6, h - 6); g2.drawLine(5, h - 5, 8, h - 8); g2.drawLine(8, h - 8, 11, h - 5); break;
                case "unlink":
                    g2.drawLine(3, 3, w - 3, h - 3); g2.drawLine(w - 3, 3, 3, h - 3); break;
                case "rank":
                    int[] xs={w/2,w-3,w-5,w/2,5,3}; int[] ys={2,5,h/2,h-2,h/2,5}; g2.drawPolygon(xs,ys,6); break;
                default:
                    g2.drawLine(w / 2, 1, w / 2, h - 1); g2.drawLine(1, h / 2, w - 1, h / 2); g2.drawLine(3, 3, w - 3, h - 3); g2.drawLine(w - 3, 3, 3, h - 3);
            }
            g2.dispose();
        }
    }

    protected static final class GlyphTile extends JComponent
    {
        private final GlyphIcon icon;
        private final Color accent;
        private final int size;
        GlyphTile(String kind, Color accent, int size)
        {
            this.icon = new GlyphIcon(kind, accent, Math.max(12, size / 2));
            this.accent = accent;
            this.size = size;
            setPreferredSize(new Dimension(size, size));
            setMaximumSize(new Dimension(size, size));
        }
        @Override protected void paintComponent(Graphics g)
        {
            Graphics2D g2=(Graphics2D)g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setPaint(new GradientPaint(0,0,PANEL_3,size,size,PANEL_2));
            g2.fillRoundRect(1,1,size-2,size-2,6,6);
            g2.setColor(BRONZE);
            g2.drawRoundRect(1,1,size-2,size-2,6,6);
            g2.setColor(new Color(255,255,255,20));
            g2.drawLine(5,2,Math.max(5,size-6),2);
            int ix=(size-icon.getIconWidth())/2, iy=(size-icon.getIconHeight())/2;
            icon.paintIcon(this,g2,ix,iy);
            g2.dispose();
        }
    }

    protected static final class StepBadge extends JComponent
    {
        private final int number; private final Color accent;
        StepBadge(int number, Color accent) { this.number=number; this.accent=accent; setPreferredSize(new Dimension(27,27)); }
        @Override protected void paintComponent(Graphics g)
        {
            Graphics2D g2=(Graphics2D)g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setPaint(new GradientPaint(0,0,new Color(accent.getRed(),accent.getGreen(),accent.getBlue(),150),getWidth(),getHeight(),PANEL_3));
            g2.fillRoundRect(1,1,getWidth()-2,getHeight()-2,6,6); g2.setColor(GOLD); g2.drawRoundRect(1,1,getWidth()-2,getHeight()-2,6,6);
            String n=String.valueOf(number); g2.setFont(getFont().deriveFont(Font.BOLD,11f)); FontMetrics fm=g2.getFontMetrics();
            g2.drawString(n,(getWidth()-fm.stringWidth(n))/2,(getHeight()+fm.getAscent()-fm.getDescent())/2); g2.dispose();
        }
    }

    protected static final class CrestFrame extends JPanel
    {
        private final ImageIcon logo;
        CrestFrame(ImageIcon logo) { this.logo=logo; setOpaque(false); }
        @Override protected void paintComponent(Graphics g)
        {
            Graphics2D g2=(Graphics2D)g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int s=Math.min(getWidth(),getHeight())-3;
            g2.setPaint(new GradientPaint(0,0,new Color(84,64,45),s,s,new Color(26,22,18)));
            g2.fillRoundRect(1,1,s,s,8,8);
            g2.setColor(BRONZE); g2.drawRoundRect(1,1,s,s,8,8);
            g2.setColor(new Color(PURPLE_LIGHT.getRed(),PURPLE_LIGHT.getGreen(),PURPLE_LIGHT.getBlue(),85));
            g2.drawRoundRect(4,4,Math.max(1,s-6),Math.max(1,s-6),6,6);
            if (logo != null && logo.getIconWidth() > 0) logo.paintIcon(this,g2,(getWidth()-logo.getIconWidth())/2,(getHeight()-logo.getIconHeight())/2);
            g2.dispose();
        }
    }

    protected static final class PremiumButton extends JButton
    {
        private final Color accent; private final String glyph; private boolean active;
        PremiumButton(String title, Color accent, String glyph)
        {
            super(title); this.accent=accent; this.glyph=glyph;
            setFocusPainted(false); setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); setForeground(WHITE);
            setFont(getFont().deriveFont(Font.BOLD,11f)); setBorder(BorderFactory.createEmptyBorder(5,6,5,6));
            setMargin(new Insets(3,4,3,4)); setOpaque(false); setContentAreaFilled(false); setRolloverEnabled(true);
        }
        void setActive(boolean active) { this.active=active; repaint(); }
        @Override protected void paintComponent(Graphics g)
        {
            Graphics2D g2=(Graphics2D)g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            boolean hover=getModel().isRollover(), pressed=getModel().isPressed();
            Color top = active ? new Color(67,50,78) : hover ? new Color(51,45,37) : new Color(41,36,30);
            Color bottom = active ? new Color(39,30,47) : pressed ? new Color(27,24,20) : new Color(30,27,23);
            g2.setPaint(new GradientPaint(0,0,top,0,getHeight(),bottom));
            g2.fillRoundRect(0,0,getWidth()-1,getHeight()-1,6,6);
            Color border = active ? PURPLE_LIGHT : (hover ? GOLD : BRONZE);
            g2.setColor(border); g2.drawRoundRect(0,0,getWidth()-1,getHeight()-1,6,6);
            g2.setColor(new Color(255,255,255,18));
            g2.drawLine(5,1,Math.max(5,getWidth()-6),1);
            if (isEnabled())
            {
                boolean showGlyph = glyph != null && !glyph.isEmpty();
                GlyphIcon icon = showGlyph ? new GlyphIcon(glyph, active?WHITE:(accent.equals(PURPLE)?PURPLE_LIGHT:accent),11) : null;
                FontMetrics fm=g2.getFontMetrics(getFont());
                int textW=fm.stringWidth(getText());
                int iconW=showGlyph ? icon.getIconWidth() : 0;
                int gap=showGlyph ? 5 : 0;
                int total=iconW+gap+textW;
                int x=Math.max(4,(getWidth()-total)/2);
                if (showGlyph)
                {
                    int y=(getHeight()-icon.getIconHeight())/2;
                    icon.paintIcon(this,g2,x,y);
                }
                g2.setFont(getFont()); g2.setColor(getForeground());
                g2.drawString(getText(),x+iconW+gap,(getHeight()+fm.getAscent()-fm.getDescent())/2);
            }
            else
            {
                g2.setFont(getFont()); g2.setColor(new Color(MUTED.getRed(),MUTED.getGreen(),MUTED.getBlue(),100)); FontMetrics fm=g2.getFontMetrics();
                g2.drawString(getText(),Math.max(4,(getWidth()-fm.stringWidth(getText()))/2),(getHeight()+fm.getAscent()-fm.getDescent())/2);
            }
            g2.dispose();
        }
    }

    protected static final class EliteCard extends RoundedPanel
    {
        private final Color accent;
        EliteCard(Color accent) { super(PANEL, BRONZE, 7); this.accent=accent; }
        @Override protected void paintComponent(Graphics g)
        {
            Graphics2D g2=(Graphics2D)g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(SHADOW); g2.fillRoundRect(2,3,getWidth()-4,getHeight()-3,7,7);
            g2.setPaint(new GradientPaint(0,0,PANEL_HIGHLIGHT,getWidth(),getHeight(),PANEL));
            g2.fillRoundRect(0,0,getWidth()-2,getHeight()-2,7,7);
            g2.setColor(BRONZE); g2.drawRoundRect(0,0,getWidth()-2,getHeight()-2,7,7);
            Color rail = accent.equals(BORDER) ? GOLD : accent;
            g2.setColor(new Color(rail.getRed(),rail.getGreen(),rail.getBlue(),210));
            g2.fillRoundRect(1,3,3,Math.max(1,getHeight()-7),3,3);
            g2.setColor(new Color(255,255,255,22)); g2.drawLine(7,1,Math.max(7,getWidth()-8),1);
            g2.dispose();
        }
    }

    protected static final class VerticalPanel extends JPanel implements Scrollable
    {
        VerticalPanel()
        {
            setOpaque(false);
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        }

        @Override
        public Dimension getPreferredScrollableViewportSize()
        {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction)
        {
            return 18;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction)
        {
            return Math.max(18, visibleRect.height - 18);
        }

        @Override
        public boolean getScrollableTracksViewportWidth()
        {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight()
        {
            return false;
        }
    }

    protected static class RoundedPanel extends JPanel
    {
        private final Color fill;
        private final Color stroke;
        private final int arc;

        RoundedPanel(Color fill, Color stroke, int arc)
        {
            this.fill = fill;
            this.stroke = stroke;
            this.arc = arc;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g)
        {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(fill);
            g2.fill(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1f, getHeight() - 1f, arc, arc));
            g2.setColor(stroke);
            g2.setStroke(new BasicStroke(1f));
            g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1f, getHeight() - 1f, arc, arc));
            g2.dispose();
            super.paintComponent(g);
        }
    }

    protected static final class GradientCard extends RoundedPanel
    {
        private final Color from;
        private final Color to;
        private final Color stroke;

        GradientCard(Color from, Color to, Color stroke)
        {
            super(to, stroke, 14);
            this.from = from;
            this.to = to;
            this.stroke = stroke;
        }

        @Override
        protected void paintComponent(Graphics g)
        {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setPaint(new GradientPaint(0, 0, from, getWidth(), getHeight(), to));
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
            g2.setColor(stroke);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
            g2.setColor(new Color(255,255,255,22));
            g2.drawLine(8,1,Math.max(8,getWidth()-9),1);
            g2.dispose();
        }
    }

    protected static final class ProgressBar extends JComponent
    {
        private final int percent;
        private final Color accent;
        private String caption = "";

        ProgressBar(int percent, Color accent)
        {
            this.percent = Math.max(0, Math.min(100, percent));
            this.accent = accent;
            setPreferredSize(new Dimension(190, 31));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 31));
            setAlignmentX(Component.LEFT_ALIGNMENT);
        }

        void setCaption(String caption) { this.caption = caption == null ? "" : caption; repaint(); }

        @Override
        protected void paintComponent(Graphics g)
        {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int y = 4;
            int h = 8;
            g2.setColor(new Color(22,20,17));
            g2.fillRoundRect(0, y, getWidth(), h, 5, 5);
            g2.setColor(BRONZE);
            g2.drawRoundRect(0, y, Math.max(0,getWidth()-1), h, 5, 5);
            int w = (int) Math.round(getWidth() * (percent / 100.0));
            if (w > 0)
            {
                Color end = accent.equals(PURPLE) ? PURPLE_LIGHT : accent.brighter();
                g2.setPaint(new GradientPaint(0, y, accent, Math.max(1, w), y, end));
                g2.fillRoundRect(1, y + 1, Math.max(1,w - 2), Math.max(1,h - 1), 4, 4);
            }
            g2.setFont(getFont().deriveFont(Font.BOLD, 10f));
            g2.setColor(MUTED);
            FontMetrics fm = g2.getFontMetrics();
            String shown = fitText(caption, fm, Math.max(1, getWidth()));
            g2.drawString(shown, 0, 26);
            g2.dispose();
        }
    }

    protected static final class RankBadge extends JComponent
    {
        private final String rank;
        private final int percent;

        RankBadge(String rank, int percent)
        {
            this.rank = rank == null ? "R" : rank;
            this.percent = Math.max(0, Math.min(100, percent));
            setPreferredSize(new Dimension(52, 56));
        }

        @Override
        protected void paintComponent(Graphics g)
        {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w=getWidth(), h=getHeight();
            g2.setColor(new Color(0,0,0,95));
            g2.fillOval(2,3,w-4,w-4);
            g2.setColor(BRONZE);
            g2.setStroke(new BasicStroke(3f));
            g2.drawArc(3,3,w-7,w-7,90,-360);
            g2.setColor(PURPLE_LIGHT);
            g2.drawArc(3,3,w-7,w-7,90,-(int)Math.round(360*percent/100.0));

            Path2D shield = new Path2D.Float();
            shield.moveTo(w/2.0,8); shield.lineTo(w-12,14); shield.lineTo(w-14,34);
            shield.lineTo(w/2.0,h-7); shield.lineTo(14,34); shield.lineTo(12,14); shield.closePath();
            g2.setPaint(new GradientPaint(12,8,new Color(133,101,157),w-12,h-8,DEEP_PURPLE));
            g2.fill(shield); g2.setColor(GOLD); g2.setStroke(new BasicStroke(1.2f)); g2.draw(shield);
            g2.setColor(new Color(255,255,255,50)); g2.drawLine(18,16,w-18,16);

            String mark = rank.isBlank() ? "R" : rank.substring(0, 1).toUpperCase();
            g2.setFont(getFont().deriveFont(Font.BOLD, 17f));
            FontMetrics fm = g2.getFontMetrics();
            g2.setColor(WHITE);
            g2.drawString(mark, (w - fm.stringWidth(mark)) / 2, 31);
            g2.dispose();
        }
    }
}
