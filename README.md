# Elite Exiles Companion

Elite Exiles Companion is a local-first RuneLite progression sidebar for Elite Exiles. It tracks the currently logged-in account's live skill levels and session XP inside RuneLite and can optionally connect to the Elite Exiles Discord coach for clan goals, missions, roadmaps, EE Points, and account-aware recommendations.

## Local-first by default

The plugin works without the Elite Exiles service. **Coach Integration is OFF by default**, and the plugin makes no Elite Exiles bridge requests while that option is disabled.

Local features include:

- Live RuneLite RSN display
- Live skill-level dashboard
- Session XP tracking
- XP/hour session view
- Readable graphical sidebar
- Core combat/Slayer level strip

## Optional Coach Integration

Users who want Elite Exiles clan features explicitly enable **Enable Coach Integration** in the plugin settings. RuneLite shows its third-party-server warning before that feature is enabled.

The production Elite Exiles bridge is preconfigured as an HTTPS origin, so normal Plugin Hub users do not need to copy or edit a server URL.

Normal linking flow:

1. Enable **Coach Integration**.
2. If needed, click **JOIN ELITE EXILES DISCORD** in the unlinked coach card. This opens the fixed official invite in the user's browser.
3. Run `/runelitelink` in Elite Exiles Discord.
4. Paste the unique one-time code into the RuneLite sidebar while logged into the same OSRS character.
5. Click **LINK COACH**.

The Discord button is user-click only. It never auto-opens Discord and the invite URL contains no RSN, link code, bridge token, or tracking parameters.

When Coach Integration is enabled and the user links an account:

- Linking sends the OSRS display name and one-time link code to the Elite Exiles bridge.
- If **Live session sync** is also enabled, the plugin sends skill levels, skill XP, total XP, session XP, and session start time.
- The user's IP address is visible to the remote server as part of the network connection.
- The Companion requires HTTPS and standard HTTPS port 443 for Elite Exiles bridge connections.
- The authenticated bridge token is used only as a bearer token for the Elite Exiles bridge and is not a Jagex/RuneScape credential.

No Elite Exiles bridge data is sent while **Enable Coach Integration** is off.

## Safe diagnostics

After linking, **RUN SAFE DIAGNOSTICS** verifies the same bridge path used by normal operation:

- HTTPS connectivity
- Elite Exiles bridge identity and protocol version
- bearer-token authentication
- linked-RSN binding
- authenticated GET routing
- authenticated POST + JSON routing
- response parsing and latency

The diagnostic endpoints are deliberately read-only. They execute before member-progression state is touched on the VPS and do not award EE Points, complete goals or missions, alter registrations, change Discord configuration, or write RuneLite live-session state.

## Coach features after linking

- Elite Exiles rank and EE Points
- Rank-specific visual insignia
- Goals and missions
- Session plan
- NOW / NEXT / LATER progression roadmap
- Coach recommendations
- Verified check-in workflow

## Branding

The Plugin Hub listing, RuneLite sidebar icon, and Companion header use the current Elite Exiles purple / silver / black crest. The UI uses the same palette while keeping green, amber, and red status colors where they convey connection or action state.

## Development

This project targets Java 11 and uses RuneLite's `latest.release` client dependency for local development.

Run the Gradle `run` task to launch a RuneLite developer client with the plugin loaded. A live in-game validation still has to be performed by the user; the plugin does not automate game input.

## Plugin Hub

This repository is structured for RuneLite Plugin Hub `standard` builds. The plugin uses RuneLite-injected OkHttp and Gson for network/JSON work and RuneLite's `ImageUtil` and `LinkBrowser` utilities for resources and user-clicked external links.

## Privacy

See [PRIVACY.md](PRIVACY.md).

## License

BSD 2-Clause. See [LICENSE](LICENSE).
