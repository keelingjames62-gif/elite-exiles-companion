# Privacy

## Local Mode

Elite Exiles Companion runs in Local Mode by default. While **Enable Coach Integration** is disabled, the plugin does not make requests to the Elite Exiles bridge and does not send Elite Exiles player/session data or Coach questions to that service.

## Optional Coach Integration

Coach Integration is an opt-in feature. RuneLite displays a third-party-server warning on the setting that enables it.

The production Elite Exiles HTTPS bridge is preconfigured for normal Plugin Hub users so members do not have to paste a server address.

When Coach Integration is enabled and used:

- Linking sends the OSRS display name and one-time link code.
- Coach Chat sends the OSRS question text that the member explicitly types. Follow-up requests may also include the previously resolved OSRS subject so the Coach can retain subject continuity; the RuneLite client does not send the full prior conversation as hidden data. Questions entered with `!coach` / `!askcoach` are consumed locally by RuneLite and are not posted into RuneScape chat.
- Membership verification sends the current normal clan-channel name and guest clan-channel name reported by RuneLite, together with the already linked RSN, so the Elite Exiles service can determine whether the account is presently in the expected in-game clan.
- Dashboard and Coach responses can include the linked member's Elite Exiles membership state, rank/EE progression, goals, missions, roadmap, community/event summary and a read-only Mystery Pet Hunt summary.
- If Live session sync is enabled, the plugin can send the current RSN, skill levels, skill XP, total XP, session XP, and session start time.
- The remote server necessarily receives the connecting IP address as part of the network connection.
- The plugin requires HTTPS for Elite Exiles bridge connections and permits only standard HTTPS port 443.
- A randomly generated Elite Exiles bridge bearer token is stored in RuneLite configuration after linking so later bridge requests can be authenticated. It is not a Jagex credential.

Disabling Coach Integration stops new Elite Exiles bridge requests from being created by the plugin.

## Discord join buttons

Exile HQ includes user-clicked **JOIN ELITE EXILES DISCORD** / **OPEN DISCORD** buttons. They open this fixed invite in the user's normal browser:

`https://discord.gg/FTJhv48K2`

The plugin does not append an RSN, link code, bridge token, question text, query string, fragment, or tracking parameter to that invite. The button never auto-opens.

## Mystery Pet Hunt

The RuneLite sidebar only receives and displays a read-only summary for the linked Elite Exiles member (for example unique-pet progress and current clan box/luck status). Pet rolls, claims, rewards and collection changes remain Discord/server-side actions and are not performed by RuneLite.

## Safe diagnostics

The **DIAGNOSTICS** action is available only after the user has opted into Coach Integration and linked the Companion. It sends the existing bearer token to the configured HTTPS Elite Exiles bridge and performs read-only health/authentication and JSON echo checks. The diagnostics do not award points, complete goals/missions, alter registrations, change Discord settings, or write live RuneLite progression/session data.

## RuneScape credentials

The plugin does not request, read, transmit, or store Jagex/RuneScape passwords, Jagex Account credentials, bank PINs, or authenticator secrets.

## Paid AI boundary

Any optional OpenAI/API integration is server-side only. The RuneLite plugin does not contain, request, transmit, or store the owner's OpenAI API key. Coach questions may be answered without paid AI from recent context, caches, or trusted-source research. If the server elects to use paid AI, that decision and its spending limits are enforced server-side.
