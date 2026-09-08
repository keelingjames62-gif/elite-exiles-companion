## 2.1.0 — COMPETE label legibility

- Kept the existing 10.5pt navigation font and single-row layout.
- COMPETE now renders text-only in the top navigation, freeing the glyph width so the full `COMPETE` label fits instead of clipping to `COMPE`.
- HQ/GROUP/CLAN keep their existing glyphs and behavior.
- No GROUP, access, server, timer, network, dependency, gameplay, input, combat, PvP, Slayer, or scroll behavior changed.

## 2.1.0 — GROUP full-width legibility finalization

- GROUP guide/empty-state text now uses the full card body width with COMPETE-matched 12pt titles and 11pt body text instead of the smaller 11/10 treatment.
- GROUP activity/world controls and host action use larger 11pt text.
- Existing live GROUP cards use larger supporting text for member/host/crew/completion details.
- Full-width LEFT alignment fix is retained so GROUP content consumes the available sidebar width instead of collapsing inward.
- Scroll behavior is unchanged: dashboard rebuilds capture and restore the active page/outer scroll positions; no automatic bottom-scroll call is used.
- No protocol, access, server, networking, timer, dependency, gameplay, input, combat, PvP, or Slayer behavior changed.

## 2.1.0 — linked-access + GROUP width finalization

- Replaced the old staff-approval/current-clan access gate with a one-time Discord-link access model. The link itself grants Companion access until the user explicitly unlinks it.
- Removed RuneLite membership-sync/request traffic and the related membership gating UI. Existing server compatibility endpoints can remain during rollout for older approved clients, but the 2.1 client no longer uses them.
- Preserved local stat/XP collection and progression refresh paths used for account progression context, SOTW, and emblem/rank presentation.
- Fixed the GROUP quick-guide and empty-state cards with a full-width BorderLayout host so Swing BoxLayout cannot collapse them to half-width; the empty title is shortened to `NO GROUPS YET`.
- Preserved low-energy scheduling: 30-second stat-to-panel cadence, fixed-delay housekeeping, dashboard overlap guard, optional 60-second Live session push, and 90-second default HQ refresh.
- Ordinary `CLAN_CHAT` remains local-only; only the existing Elite Exiles clan-channel tag receives the vivid-purple local tint.

# Changelog

## 2.1.0 — Progress Together rework

- Reworked the visible sidebar from **HQ / Coach / Plan / Clan** to **HQ / GROUP / COMPETE / CLAN**.
- Removed the RuneLite-side general Coach/Wiki/AI chat and local `!coach` / `!askcoach` commands.
- Simplified GROUP into a visible **Host → Join → Start → Done** flow with a fixed activity picker and optional world instead of exposed matching/readiness jargon.
- Discord **PLAY WITH SOMEONE** and RuneLite GROUP now use one shared group record: Discord joins an existing matching crew or hosts one automatically instead of creating a second request/matcher.
- Added SOTW display, progression-friendly divisions, leaderboard and weekly mission display under COMPETE.
- Added HQ **What can I do next?** cards combining existing account progression state with clan groups, competition and missions.
- Added CLAN progression pulse for open groups and SOTW participation.
- Added explicit host-only **START** plus unanimous **GROUP completion confirmation**; completion cannot be confirmed before START, requires at least two verified members, and rewards are awarded server-side only after every current participant confirms.
- Added a bounded **CLAN activity feed** sourced only from qualifying native OSRS clan system broadcasts while Elite Exiles Sync is enabled; ordinary clan-chat conversation is not uploaded.
- Added read-only **Exile Identity** and EE shop balance display; equipped cosmetics remain managed through Discord `/eeshop`.
- Expanded the third-party-data disclosure to cover native clan broadcast sharing, GROUP completion confirmation, and cosmetic identity data.
- Keeps the existing opt-in network boundary, HTTPS-only production bridge, membership verification, EE Points, Pet Hunt, diagnostics, session tracking and scroll-stability fix.
- Refined the GROUP quick-start card into four compact, sidebar-safe steps so the flow remains readable at normal RuneLite sidebar width.
- Simplified clan-chat identity after live testing: normal RuneLite/Jagex usernames and message text are left untouched; only the existing `Elite Exiles` clan-channel label receives a vivid local purple tint. Ordinary clan-chat text is never uploaded.
- Fixed the GROUP empty-state card so its text uses the full sidebar width instead of being squeezed beside a glyph column.
- Corrected the remaining GROUP width regression: the manual quick-guide and empty-state cards now use the same 205px sidebar card footprint as the established HQ cards, so only their text changes while the box size stays consistent.
- Removed redundant HQ quick-action navigation buttons and replaced them with the static existing brand line **ELITE EXILES • MORE THAN A CLAN**, adding no background work.
- Reduced client-side churn further: routine stat events now update only the lightweight session model, full HQ Swing trees rebuild only on session/account changes or dashboard/user actions, stat-to-panel handoff is capped at 30 seconds, and overlapping dashboard pulls are suppressed.
- No new runtime dependencies and no gameplay/input automation.

## 2.0.0 — Exile HQ

- Rebuilt the sidebar into four focused workspaces: **HQ**, **Coach**, **Plan**, and **Clan**.
- Added a RuneLite/OSRS-native visual treatment with restrained Elite Exiles purple/silver branding, the clan crest, rank progress, compact status chips, and lightweight custom Swing cards.
- Added a visible first-run Coach setup card with exact in-client instructions, the fixed Elite Exiles Discord invite, `/runelitelink` guidance, one-time code entry, and **CONNECT COACH**.
- Added bounded Coach Chat UI for account-aware OSRS questions and follow-up subject continuity. Questions are capped to 400 characters and the local visible thread is capped to 12 bubbles.
- Added RuneLite-native `!coach question` and `!askcoach question` shortcuts. RuneLite consumes the command locally, opens Exile HQ, and sends the explicit question to Coach without posting it into game chat.
- Added official clan-access onboarding: linked Discord identity + logged-in RSN + staff approval + current Elite Exiles in-game clan detection gate protected HQ/Coach actions. Staff still performs the normal OSRS clan invite manually; no game input is automated.
- Added answer-mode labels so members can see whether a Coach reply used trusted sources, recent context/cache, or paid AI assistance.
- Added goal-focused quick actions for Coach questions, blocker analysis, and user-requested session planning.
- Consolidated goals, missions, roadmap, milestones, and session planning into the **Plan** workspace instead of duplicating RuneLite/Jagex skill interfaces.
- Added read-only **Clan** workspace support for EE progression, Mystery Pet Hunt summary, next clan event, and user-clicked Discord access when supplied by the Elite Exiles bridge.
- Kept Mystery Pet Hunt claims, rolls, rewards, and state changes server/Discord-side only.
- Kept Coach Integration and Live session sync opt-in and OFF by default.
- Kept the production bridge preconfigured, HTTPS-only on standard port 443, with no member-entered server URL.
- Kept bounded network behavior: finite connection/read/write/call timeouts and a 256 KiB maximum response body.
- Reduced idle client work further: background sync now uses a 30-second fixed-delay task, dashboard refresh defaults to 90 seconds (60-second minimum), and local stat-driven Swing refreshes are capped at once per 5 seconds.
- Removed idle full-card refreshes and avoids rebuilding the linked profile card for local stat changes; rank/identity/profile data still updates on dashboard responses.
- Kept the Plugin Hub `standard` build with no new runtime dependencies.
- Added no gameplay automation, input injection, attack/prayer prediction, future-hazard guidance, PvP scouting, or crowdsourced player telemetry.

## 1.8.1

- Preconfigured the production Elite Exiles HTTPS coach bridge for normal Plugin Hub users.
- Hid the managed bridge URL from the normal RuneLite configuration UI.
- Added a startup migration that repairs blank, legacy localhost, malformed, non-HTTPS, and non-standard-port bridge values while preserving valid HTTPS origins.
- Added **JOIN ELITE EXILES DISCORD** to the unlinked coach card using RuneLite's `LinkBrowser`; the fixed invite opens only after a user click and carries no player/link/token data.
- Updated Plugin Hub, RuneLite sidebar, and Companion header branding to the current purple / silver / black Elite Exiles crest.
- Added authenticated **RUN SAFE DIAGNOSTICS** for the same Plugin Hub code path used in normal operation.
- Hardened the RuneLite HTTP client with finite timeouts and a 1 MiB response-body safety cap.
- Tightened bridge URL validation: HTTPS only, port 443 only, no embedded credentials, query, fragment, or extra path.
- Aligned the opt-in third-party-server warning with RuneLite guidance.
- Kept Coach Integration and Live session sync disabled by default.
- No new runtime dependencies and no gameplay/input automation changes.

## 1.8.0

- Changed the plugin to local-first operation.
- Coach Integration is disabled by default.
- Added the RuneLite third-party-server warning to the opt-in Coach Integration toggle.
- Added defense-in-depth network guards so Elite Exiles requests cannot be created while Coach Integration is disabled.
- Live session sync is disabled by default.
- Added a local dashboard with total level, session XP, XP/hour, skill count, and core levels.
- Hid Coach action controls while Local Mode is active.
- Chat features remain removed.

### GROUP full-width alignment correction
- Fixed the remaining BoxLayout alignment mismatch that could shift the GROUP page inward: the activity picker and world field now use LEFT alignment like the rest of the page, allowing guide, inputs, host action and group cards to consume the full available sidebar width.
- No GROUP protocol, server, access, ranking, stat tracking, networking, or performance cadence changes.
