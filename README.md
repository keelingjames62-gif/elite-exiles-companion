# Elite Exiles Companion — Exile HQ

**Exile HQ** is the Elite Exiles RuneLite companion: a local-first progression command center with an optional, explicit connection to the Elite Exiles Discord Coach.

It is designed to complement RuneLite and Jagex's own activity tools rather than duplicate them. It does **not** automate gameplay, inject input, predict attacks, choose prayers, mark future hazards, or control the game client.

## Exile HQ layout

The sidebar is deliberately kept to four useful workspaces:

- **HQ** — player identity, EE rank progress, active goal, next step, live session XP/rate and quick actions.
- **Coach** — account-aware OSRS questions and follow-up questions through the linked Elite Exiles Coach, including the local `!coach question` shortcut.
- **Plan** — current goal, session plan, NOW / NEXT / LATER roadmap and nearby milestones.
- **Clan** — Elite Exiles community status, EE progression context, Mystery Pet Hunt progress, next clan event and Discord access.

## Local-first by default

**Coach Integration is OFF by default.** While it is disabled, the plugin makes no Elite Exiles bridge requests.

Local mode provides the currently logged-in RSN and live RuneLite session tracking without sending Elite Exiles data to the clan service.

## Fast Coach + Discord setup

The sidebar keeps the official Discord link and Coach connection flow together:

1. Enable **Coach Integration** in the RuneLite plugin settings. RuneLite shows the third-party-server warning on this setting.
2. Click **OPEN DISCORD** in Exile HQ, or visit `https://discord.gg/FTJhv48K2`.
3. Run `/runelitelink` in Elite Exiles Discord.
4. Paste the one-time 8-character code into Exile HQ while logged into the same OSRS character.
5. Click **CONNECT COACH**.

The production HTTPS bridge is preconfigured. Members do not enter or manage a server URL.

After linking, the **Clan** tab provides an official **REQUEST CLAN ACCESS** flow. The request is tied to the linked Discord member and logged-in RSN. Staff approves the request in Discord, a clan officer completes the normal OSRS clan invite manually, and RuneLite detects current Elite Exiles clan membership automatically. Full protected Exile HQ/Coach actions require all three checks: Discord membership, staff approval, and current in-game Elite Exiles clan membership.

## Optional network features

When Coach Integration is enabled and used:

- linking sends the OSRS display name and the one-time link code;
- Coach Chat sends only the OSRS question text the member explicitly types; `!coach` / `!askcoach` are consumed locally by RuneLite and open the Coach panel rather than posting the question into game chat;
- membership verification sends the names of the member's current normal and guest clan channels so the Elite Exiles server can verify whether the linked RSN is presently in the expected clan;
- dashboard/Coach requests return the member's Elite Exiles goals, rank/EE progression, roadmap, membership state, clan/event summary and read-only Mystery Pet Hunt summary;
- if **Live session sync** is separately enabled, the plugin sends RSN, skill levels/XP, total XP, session XP and session start time;
- the remote server receives the connecting IP address as part of ordinary HTTPS networking;
- the authenticated Elite Exiles bridge token is stored in RuneLite configuration and is not a Jagex credential.

## Coach safety boundary

Coach Chat is informational and planning-oriented. It may explain OSRS mechanics, requirements, training, preparation, items, money making and account goals. It does not react to combat in order to predict or automate gameplay and does not send input to RuneScape.

The Coach uses the existing Elite Exiles trusted OSRS research service. The server is designed to answer from recent Coach context, bounded caches, and deterministic trusted-source research first. Optional paid AI assistance is a server-side last-mile fallback only when the free answer is not good enough and the request passes relevance, rate, and hard-budget gates. The RuneLite plugin never receives or stores the owner's OpenAI API key. If trusted evidence is insufficient, the Coach can fail closed rather than inventing an answer.

## Mystery Pet Hunt

Exile HQ shows a **read-only** summary of the linked member's Discord Mystery Pet Hunt progress and current clan luck/box status when available. Pet claiming/opening remains a Discord activity; the RuneLite plugin does not roll rewards or change Pet Hunt state.

## Safe diagnostics

After linking, **DIAGNOSTICS** verifies HTTPS connectivity, bridge identity/protocol, bearer authentication, linked-RSN binding and JSON routing. The diagnostic endpoints are deliberately read-only and do not award EE Points, complete goals/missions, alter registrations, modify Discord settings or write live progression data.

## Branding and RuneLite fit

The sidebar deliberately follows RuneLite/OSRS visual proportions and dark neutral surfaces so it does not look out of place beside the game. Elite Exiles branding is layered in through the clan crest plus restrained purple/silver accents for selected tabs, rank/progression and identity. Green/amber/red remain reserved for status meaning.

The polished look is produced with lightweight Swing painting and small bundled PNG resources. There is no embedded browser, runtime-downloaded UI, animation loop, or additional runtime dependency.

## Development

This project targets Java 11 and uses RuneLite's `latest.release` client dependency. It remains a Plugin Hub `standard` build and adds no new runtime dependencies.

## Privacy

See [PRIVACY.md](PRIVACY.md).

## License

BSD 2-Clause. See [LICENSE](LICENSE).
