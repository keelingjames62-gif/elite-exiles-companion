# Changelog

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
- Reduced background housekeeping to a lightweight 15-second cadence; stat changes still update live values event-by-event.
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
