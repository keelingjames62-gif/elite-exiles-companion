# Elite Exiles Companion

Elite Exiles Companion is a local-first RuneLite progression sidebar. It tracks the currently logged-in account's live skill levels and session XP inside RuneLite and can optionally connect to the Elite Exiles Discord coach for clan goals, missions, roadmaps, points, and account-aware recommendations.

## Local-first by default

The plugin works without the Elite Exiles service. **Coach Integration is OFF by default**, and the plugin makes no Elite Exiles network requests while that option is disabled.

Local features include:

- Live RuneLite RSN display
- Live skill-level dashboard
- Session XP tracking
- XP/hour session view
- Readable graphical sidebar
- Core combat/Slayer level strip

## Optional Coach Integration

Users who want the Elite Exiles clan features can explicitly enable **Enable Coach Integration** in the plugin settings. RuneLite shows a warning on that setting before it is enabled.

When Coach Integration is enabled and the user links an account:

- Linking sends the OSRS display name and one-time link code to the configured Elite Exiles bridge.
- If **Live session sync** is also enabled, the plugin sends skill levels, skill XP, total XP, session XP, and session start time.
- The user's IP address is visible to the remote server as part of the network connection.
- Remote bridge addresses must use HTTPS. HTTP is accepted only for localhost development/testing.

No Elite Exiles data is sent while **Enable Coach Integration** is off.

## Coach features after linking

- Elite Exiles rank and EE Points
- Goals and missions
- Session plan
- NOW / NEXT / LATER progression roadmap
- Coach recommendations
- Verified check-in workflow

## Development

This project targets Java 11 and uses RuneLite's `latest.release` client dependency for local development.

Run the Gradle `run` task to launch a RuneLite developer client with the plugin loaded.

## Plugin Hub

This repository is structured for RuneLite Plugin Hub `standard` builds and does not add third-party Gradle dependencies beyond dependencies already provided by RuneLite.

## Privacy

See [PRIVACY.md](PRIVACY.md) for a concise description of Local Mode and the optional network integration.

## License

BSD 2-Clause. See [LICENSE](LICENSE).
