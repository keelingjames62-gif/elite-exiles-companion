# Elite Exiles Companion — Exile HQ

**Exile HQ** is the Elite Exiles RuneLite companion: a local-first clan progression hub with an optional, explicit connection to the Elite Exiles service.

The plugin is aimed at a developing clan, not an endgame-only raid roster. It helps members see useful next steps, find people at a similar progression stage, take part in weekly competition, and stay connected to clan activity. It does **not** automate gameplay, inject input, predict attacks, choose prayers, mark future hazards, or control the game client.

## Exile HQ layout

The sidebar has four focused workspaces:

- **HQ** — member identity, EE rank/points, current session and a short **What can I do next?** list combining account progression with current clan opportunities.
- **GROUP** — simple clan grouping: pick an activity, host or jump in, the host presses **START**, then everyone presses **I'M DONE** after the activity. Discord and RuneLite use the same group record.
- **COMPETE** — Skill of the Week (SOTW), progression-friendly divisions, leaderboard and weekly missions.
- **CLAN** — Discord-link status, clan network counts, native OSRS clan-broadcast activity feed, equipped Elite Exiles identity cosmetic, Mystery Pet Hunt status, next clan event and Discord access.

Detailed OSRS guides remain on established OSRS resources. The plugin does not attempt to reproduce the OSRS Wiki or provide a general-purpose Wiki/AI chat interface.

## Local-first by default

**Elite Exiles Sync is OFF by default.** While it is disabled, the plugin makes no Elite Exiles bridge requests.

Local Mode provides the currently logged-in RSN and live RuneLite session tracking without sending Elite Exiles data to the clan service.

## Fast Discord + Elite Exiles setup

1. Enable **Elite Exiles Sync** in the RuneLite plugin settings. RuneLite shows the third-party-server warning on this setting.
2. Click **OPEN ELITE EXILES DISCORD** in Exile HQ, or visit `https://discord.gg/FTJhv48K2`.
3. Run `/runelitelink` in Elite Exiles Discord.
4. Paste the one-time 8-character code into Exile HQ while logged into the same OSRS character.
5. Click **CONNECT ELITE EXILES**.

The production HTTPS bridge is preconfigured. Members do not enter or manage a server URL.

After the one-time Discord link succeeds, Companion access is granted to that linked RuneLite account. Continued plugin access does **not** depend on Discord roles, staff approval, or current Elite Exiles clan membership. The Discord invite remains available so people can join or return to the community, but leaving the Discord or clan does not revoke the established Companion link unless the user explicitly unlinks it.

## Optional network features

When Elite Exiles Sync is enabled and used:

- linking sends the OSRS display name and one-time link code;
- GROUP hosting can send the chosen activity and optional OSRS world; joining, starting, leaving and completion confirmation send the selected clan-group identifier;
- qualifying native OSRS **clan system broadcasts** (`CLAN_MESSAGE`) can be sent to the Elite Exiles service to populate the shared clan activity feed; ordinary member clan-chat conversation (`CLAN_CHAT`) is not uploaded;
- dashboard/progression requests return linked-account access state, rank/EE progression, current progression suggestions, GROUP listings, SOTW standings, missions, recent normalized clan activity, equipped Elite Exiles cosmetic identity, community/event summary and a read-only Mystery Pet Hunt summary;
- an explicit progression refresh asks the existing Elite Exiles backend to refresh the linked account from Jagex data and update the member's SOTW baseline/current score;
- if **Live session sync** is separately enabled, the plugin sends RSN, skill levels/XP, total XP, session XP and session start time;
- the remote server receives the connecting IP address as part of ordinary HTTPS networking;
- the authenticated Elite Exiles bridge token is stored in RuneLite configuration and is not a Jagex credential.

## Progression model

Exile HQ intentionally uses a small structured clan progression model rather than a copied knowledge database. The backend combines existing Elite Exiles account recommendations, goals/missions, Jagex-visible account data, current clan groups and weekly competition state. The resulting recommendation is an Elite Exiles participation/progression suggestion, not a replacement for a full OSRS guide.

SOTW uses Jagex-refreshed skill XP. A member's weekly baseline is established on their first verified progression refresh for that SOTW week; the score is XP gained after that baseline. Division labels keep developing players visible without awarding EE Points from client-reported XP.

## Clan activity and identity safety boundary

The activity feed is passive. The plugin only considers native OSRS clan **system** broadcasts, strips game formatting, applies a small allow-list of activity-like terms, and sends at most the normalized broadcast text when Elite Exiles Sync is enabled and the Companion is linked. It does not upload normal clan conversation, private messages, public chat, opponent data or hidden player information. Unknown/irrelevant clan system messages are ignored by the server. The feed does not directly award EE Points.

Elite Exiles keeps ordinary clan chat visually native. For an Elite Exiles clan channel visible to the linked client, RuneLite only applies a local vivid-purple tint to the existing `Elite Exiles` channel label; player names and message text remain standard RuneLite/Jagex rendering. No ordinary clan-chat message is uploaded. Cosmetic purchases/equips remain managed through Discord `/eeshop`, and the CLAN workspace can display the read-only equipped identity returned by the dashboard.

## GROUP safety boundary

GROUP is coordination only. It stores/displays the chosen clan activity, optional world, participants and lifecycle state (**Waiting / Ready / In Progress**). The host explicitly starts the group; completion is only available after START and EE credit waits for every current participant to confirm. It does not scout players, read opponents, automate gameplay, send game input, or perform gameplay actions. Discord and RuneLite use the same single clan-side group record.

## Mystery Pet Hunt

Exile HQ shows a **read-only** summary of the linked member's Discord Mystery Pet Hunt progress and current clan luck/box status when available. Pet claiming/opening remains a Discord activity; the RuneLite plugin does not roll rewards or change Pet Hunt state.

## Safe diagnostics

After linking, **DIAGNOSTICS** verifies HTTPS connectivity, bridge identity/protocol, bearer authentication, linked-RSN binding and JSON routing. The diagnostic endpoints are deliberately read-only and do not award EE Points, complete goals/missions, alter registrations, modify Discord settings or write live progression data.

## Branding and RuneLite fit

The sidebar uses RuneLite/OSRS-style dark neutral surfaces with restrained Elite Exiles purple/silver accents. The UI uses lightweight Swing painting and small bundled PNG resources. There is no embedded browser, runtime-downloaded UI, animation loop, or additional runtime dependency.

## Development

This project targets Java 11 and uses RuneLite's `latest.release` client dependency. It remains a Plugin Hub `standard` build and adds no new runtime dependencies.

## Privacy

See [PRIVACY.md](PRIVACY.md).

## License

BSD 2-Clause. See [LICENSE](LICENSE).
