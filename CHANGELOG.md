# Changelog

## 1.8.1

- Preconfigured the production Elite Exiles HTTPS coach bridge for normal Plugin Hub users.
- Hid the managed bridge URL from the normal RuneLite configuration UI.
- Added a startup migration that repairs blank, legacy localhost, malformed, non-HTTPS, and non-standard-port bridge values while preserving valid HTTPS origins.
- Added **JOIN ELITE EXILES DISCORD** to the unlinked coach card using RuneLite's `LinkBrowser`; the fixed invite opens only after a user click and carries no player/link/token data.
- Updated Plugin Hub, RuneLite sidebar, and Companion header branding to the current purple / silver / black Elite Exiles crest.
- Refreshed the panel palette and spacing while preserving status colors for success/warning/destructive actions.
- Replaced the generic rank shield with tier-specific insignia: chevrons, officer bars, diamonds, stars, and crown progression.
- Added authenticated **RUN SAFE DIAGNOSTICS** for the same Plugin Hub code path used in normal operation.
- Added read-only VPS diagnostic GET and POST/JSON echo contracts with explicit `stateMutation=false` responses.
- Hardened the RuneLite HTTP client with 5s connect, 10s read/write, and 15s total call timeouts.
- Added a 1 MiB response-body safety cap.
- Tightened bridge URL validation: HTTPS only, port 443 only, no embedded credentials, query, fragment, or extra path.
- Aligned the opt-in third-party-server warning with RuneLite's current example-plugin guidance.
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
